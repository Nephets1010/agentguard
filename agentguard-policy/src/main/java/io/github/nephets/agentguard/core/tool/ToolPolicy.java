/*
 * Copyright 2026 The agentguard Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.nephets.agentguard.core.tool;

import io.github.nephets.agentguard.core.ToolNotAllowedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具权限策略：默认拒绝未注册的工具（deny-by-default）。
 *
 * <p>判定顺序：工具是否注册 → 角色是否允许 → 次数是否用尽 → 风险等级与是否要求审批。
 *
 * <p><b>为什么默认拒绝而不是默认放行：</b>Agent 可用的工具集合是开放集（模型可以编造工具名、 提示词注入可以诱导调用不存在的工具），而注册表是封闭集。只有注册表里的工具才经过过审，
 * 因此「未注册」必须落在拒绝侧；放行一个未注册工具等于把处置权交给不可信的输入。
 *
 * <p><b>为什么高危工具在注册期声明、授权期强制审批：</b>风险等级（{@link ToolRisk#DESTRUCTIVE} 与 {@code
 * requiresApproval}）是工具的静态属性，写在注册期就意味着「要不要审批」在工具上线前 就已决定，运行期没有任何路径可以绕过——授权期对高危工具一律返回 {@link
 * ToolDecision#REQUIRE_APPROVAL}，既不静默放行、也不计入调用次数（审批中的调用不消耗额度）。
 */
public final class ToolPolicy {

    private static final Logger log = LoggerFactory.getLogger(ToolPolicy.class);

    private final Map<String, ToolSpec> specs = new ConcurrentHashMap<>();
    private final Map<String, Integer> callCounts = new ConcurrentHashMap<>();

    /**
     * @param specs 初始注册的工具
     * @return 策略实例
     */
    public static ToolPolicy of(ToolSpec... specs) {
        ToolPolicy policy = new ToolPolicy();
        if (specs != null) {
            for (ToolSpec spec : specs) {
                policy.register(spec);
            }
        }
        return policy;
    }

    /**
     * 注册一个工具。重复注册同名工具时后者生效（便于热更新策略）。
     *
     * <p>为什么白名单要 fail-fast 而不是静默淘汰：静默丢弃（拒绝注册但不报错）会让调用方 以为工具已上线，故障被推迟到第一次授权时才以 DENY 的形式暴露，排查时要翻注册日志；
     * 这里对非法 spec 立即抛错，把问题留在装配期。
     *
     * @param spec 工具注册信息
     * @return 当前策略，便于链式调用
     */
    public ToolPolicy register(ToolSpec spec) {
        Objects.requireNonNull(spec, "spec");
        specs.put(spec.name(), spec);
        return this;
    }

    /**
     * 批量注册工具。
     *
     * @param specs 工具注册信息
     * @return 当前策略，便于链式调用
     */
    public ToolPolicy register(ToolSpec... specs) {
        if (specs != null) {
            for (ToolSpec spec : specs) {
                register(spec);
            }
        }
        return this;
    }

    /**
     * @param toolName 工具名
     * @return 工具注册信息，未注册时为空
     */
    public Optional<ToolSpec> spec(String toolName) {
        return Optional.ofNullable(specs.get(toolName));
    }

    /**
     * @return 已注册的工具名
     */
    public Set<String> toolNames() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(specs.keySet()));
    }

    /**
     * 判定一次工具调用。放行时会计入该调用方对该工具的调用次数。
     *
     * @param call 工具调用请求
     * @return 判定结果
     */
    public ToolDecision authorize(ToolCall call) {
        Objects.requireNonNull(call, "call");
        ToolSpec spec = specs.get(call.toolName());
        if (spec == null) {
            log.info(
                    "authorize tool={} principal={} -> DENY (unregistered)",
                    call.toolName(),
                    call.principal());
            return ToolDecision.DENY;
        }
        if (!rolesAllowed(spec, call.roles())) {
            log.info(
                    "authorize tool={} principal={} -> DENY (roles not allowed)",
                    call.toolName(),
                    call.principal());
            return ToolDecision.DENY;
        }
        String key = countKey(call.principal(), call.toolName());
        // 次数闸先于风险闸：次数用尽时连高危工具也直接拒绝（与既有判定顺序一致）。
        if (spec.maxCallsPerRun() > 0 && callCounts.getOrDefault(key, 0) >= spec.maxCallsPerRun()) {
            log.info(
                    "authorize tool={} principal={} -> DENY (calls exhausted {}/{})",
                    call.toolName(),
                    call.principal(),
                    callCounts.getOrDefault(key, 0),
                    spec.maxCallsPerRun());
            return ToolDecision.DENY;
        }
        if (spec.risk() == ToolRisk.DESTRUCTIVE || spec.requiresApproval()) {
            log.info(
                    "authorize tool={} principal={} -> REQUIRE_APPROVAL (risk={})",
                    call.toolName(),
                    call.principal(),
                    spec.risk());
            return ToolDecision.REQUIRE_APPROVAL;
        }
        // 只有放行才计数。判定与计数必须在同一个原子步骤里完成：先读后写的
        // get-then-put 在并发下会让两个线程同时读到 used = max - 1 并双双放行，
        // 次数闸被超发。ConcurrentHashMap.compute 对同一个 key 持有 bin 锁，
        // 「读取 + 条件递增」不可分割；函数内再检一次次数，兜住竞态窗口。
        boolean[] admitted = {false};
        callCounts.compute(
                key,
                (k, current) -> {
                    int used = current == null ? 0 : current;
                    if (spec.maxCallsPerRun() > 0 && used >= spec.maxCallsPerRun()) {
                        return current;
                    }
                    admitted[0] = true;
                    return used + 1;
                });
        if (!admitted[0]) {
            log.info(
                    "authorize tool={} principal={} -> DENY (calls exhausted {}/{})",
                    call.toolName(),
                    call.principal(),
                    callCounts.getOrDefault(key, 0),
                    spec.maxCallsPerRun());
            return ToolDecision.DENY;
        }
        log.debug("authorize tool={} principal={} -> ALLOW", call.toolName(), call.principal());
        return ToolDecision.ALLOW;
    }

    /**
     * 判定一次工具调用，被拒绝时抛异常。
     *
     * @param call 工具调用请求
     * @return 判定结果
     * @throws ToolNotAllowedException 判定为 DENY 时抛出
     */
    public ToolDecision require(ToolCall call) {
        ToolDecision decision = authorize(call);
        if (decision == ToolDecision.DENY) {
            log.error("tool {} denied for principal {}", call.toolName(), call.principal());
            throw new ToolNotAllowedException(
                    "工具 " + call.toolName() + " 不允许调用方 " + call.principal() + " 执行");
        }
        return decision;
    }

    /**
     * @param principal 调用方标识
     * @param toolName 工具名
     * @return 该调用方在该工具上已记录的调用次数
     */
    public int recordedCalls(String principal, String toolName) {
        return callCounts.getOrDefault(countKey(principal, toolName), 0);
    }

    /**
     * @param principal 调用方标识
     * @param toolName 工具名
     * @return 剩余可调用次数，不限制时返回 {@link Integer#MAX_VALUE}
     */
    public int remainingCalls(String principal, String toolName) {
        ToolSpec spec = specs.get(toolName);
        if (spec == null || spec.maxCallsPerRun() <= 0) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, spec.maxCallsPerRun() - recordedCalls(principal, toolName));
    }

    /** 清空所有调用次数记录，用于开启新一轮运行。 */
    public void resetRun() {
        callCounts.clear();
    }

    /**
     * 清空指定调用方在指定工具上的次数记录。
     *
     * @param principal 调用方标识
     * @param toolName 工具名
     */
    public void reset(String principal, String toolName) {
        callCounts.remove(countKey(principal, toolName));
    }

    private static boolean rolesAllowed(ToolSpec spec, Set<String> roles) {
        if (spec.allowedRoles().isEmpty()) {
            return true;
        }
        Set<String> actual = roles == null ? Set.of() : roles;
        return !Collections.disjoint(actual, spec.allowedRoles());
    }

    private static String countKey(String principal, String toolName) {
        return (principal == null ? "anonymous" : principal) + "|" + toolName;
    }

    /**
     * @return 已注册工具名到注册信息的副本
     */
    public Map<String, ToolSpec> specs() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(specs));
    }
}
