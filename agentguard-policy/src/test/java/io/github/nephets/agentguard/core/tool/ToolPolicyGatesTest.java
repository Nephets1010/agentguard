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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 三道闸（未注册 / 白名单 / 高危审批）的边界与并发行为。 */
class ToolPolicyGatesTest {

    private static ToolPolicy policy() {
        return ToolPolicy.of(
                ToolSpec.builder("search.doc").risk(ToolRisk.READ).build(),
                ToolSpec.builder("db.write")
                        .risk(ToolRisk.WRITE)
                        .allowedRoles(Set.of("admin"))
                        .maxCallsPerRun(3)
                        .build(),
                ToolSpec.builder("db.drop")
                        .risk(ToolRisk.DESTRUCTIVE)
                        .allowedRoles(Set.of("admin"))
                        .build(),
                ToolSpec.builder("report.export")
                        .risk(ToolRisk.WRITE)
                        .requiresApproval(true)
                        .build());
    }

    // ---------- 第一道闸：未注册不可调 ----------

    @Test
    void 未注册工具查无此spec且次数为无穷() {
        ToolPolicy policy = policy();
        assertThat(policy.spec("ghost.tool")).isEmpty();
        assertThat(policy.remainingCalls("alice", "ghost.tool")).isEqualTo(Integer.MAX_VALUE);
        assertThat(policy.recordedCalls("alice", "ghost.tool")).isZero();
    }

    @Test
    void 未注册工具的拒绝不留下任何计数() {
        ToolPolicy policy = policy();
        policy.authorize(ToolCall.builder("db.write").roles(Set.of("user")).build());
        policy.authorize(ToolCall.builder("ghost.tool").principal("alice").build());
        assertThat(policy.recordedCalls("alice", "db.write")).isZero();
    }

    @Test
    void 未注册工具强制校验抛异常且不带调用方也能拒() {
        ToolPolicy policy = policy();
        assertThatThrownBy(() -> policy.require(ToolCall.builder("ghost.tool").build()))
                .isInstanceOf(ToolNotAllowedException.class)
                .hasMessageContaining("ghost.tool");
    }

    @Test
    void 空策略拒绝一切调用() {
        ToolPolicy policy = ToolPolicy.of();
        assertThat(policy.toolNames()).isEmpty();
        assertThat(policy.authorize(ToolCall.builder("any.tool").build()))
                .isEqualTo(ToolDecision.DENY);
    }

    // ---------- 第二道闸：角色白名单 ----------

    @Test
    void 未配白名单时不限制角色() {
        ToolPolicy policy = ToolPolicy.of(ToolSpec.readOnly("search.doc"));
        assertThat(
                        policy.authorize(
                                ToolCall.builder("search.doc").roles(Set.of("anything")).build()))
                .isEqualTo(ToolDecision.ALLOW);
        assertThat(policy.authorize(ToolCall.builder("search.doc").build()))
                .isEqualTo(ToolDecision.ALLOW); // 无角色也放行
    }

    @Test
    void 白名单只要求角色有交集() {
        ToolPolicy policy = policy();
        assertThat(
                        policy.authorize(
                                ToolCall.builder("db.write")
                                        .roles(Set.of("user", "admin"))
                                        .build()))
                .isEqualTo(ToolDecision.ALLOW);
        assertThat(
                        policy.authorize(
                                ToolCall.builder("db.write")
                                        .roles(Set.of("user", "guest"))
                                        .build()))
                .isEqualTo(ToolDecision.DENY);
    }

    @Test
    void 角色匹配大小写敏感() {
        ToolPolicy policy = policy();
        assertThat(policy.authorize(ToolCall.builder("db.write").roles(Set.of("Admin")).build()))
                .isEqualTo(ToolDecision.DENY);
    }

    @Test
    void 角色不足时不计调用次数() {
        ToolPolicy policy = policy();
        policy.authorize(ToolCall.builder("db.write").roles(Set.of("user")).build());
        assertThat(policy.recordedCalls("anonymous", "db.write")).isZero();
    }

    // ---------- 第三道闸：高危需审批 ----------

    @Test
    void 破坏性与显式审批都走审批且不消耗次数() {
        ToolPolicy policy = policy();
        for (int i = 0; i < 3; i++) {
            assertThat(policy.authorize(ToolCall.builder("db.drop").roles(Set.of("admin")).build()))
                    .isEqualTo(ToolDecision.REQUIRE_APPROVAL);
            assertThat(
                            policy.authorize(
                                    ToolCall.builder("report.export").principal("alice").build()))
                    .isEqualTo(ToolDecision.REQUIRE_APPROVAL);
        }
        assertThat(policy.recordedCalls("anonymous", "db.drop")).isZero();
        assertThat(policy.recordedCalls("alice", "report.export")).isZero();
    }

    @Test
    void 审批类工具的剩余次数不受限() {
        ToolPolicy policy = policy();
        assertThat(policy.remainingCalls("alice", "db.drop")).isEqualTo(Integer.MAX_VALUE);
        assertThat(policy.remainingCalls("alice", "report.export")).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void 强制校验对审批结果放行() {
        ToolPolicy policy = policy();
        assertThat(policy.require(ToolCall.builder("db.drop").roles(Set.of("admin")).build()))
                .isEqualTo(ToolDecision.REQUIRE_APPROVAL);
    }

    @Test
    void 角色闸先于风险闸() {
        ToolPolicy policy = policy();
        assertThat(policy.authorize(ToolCall.builder("db.drop").roles(Set.of("user")).build()))
                .isEqualTo(ToolDecision.DENY);
    }

    // ---------- 次数闸与重置 ----------

    @Test
    void 次数用尽边界与不限次数的两种极值() {
        ToolPolicy policy =
                ToolPolicy.of(
                        ToolSpec.builder("once").maxCallsPerRun(1).build(),
                        ToolSpec.builder("many").maxCallsPerRun(0).build(),
                        ToolSpec.builder("neg").maxCallsPerRun(-5).build());
        assertThat(policy.authorize(ToolCall.builder("once").build()))
                .isEqualTo(ToolDecision.ALLOW);
        assertThat(policy.authorize(ToolCall.builder("once").build())).isEqualTo(ToolDecision.DENY);
        for (int i = 0; i < 10; i++) {
            assertThat(policy.authorize(ToolCall.builder("many").build()))
                    .isEqualTo(ToolDecision.ALLOW);
            assertThat(policy.authorize(ToolCall.builder("neg").build()))
                    .isEqualTo(ToolDecision.ALLOW);
        }
    }

    @Test
    void 不同调用方的次数互相隔离() {
        ToolPolicy policy = ToolPolicy.of(ToolSpec.builder("db.write").maxCallsPerRun(2).build());
        assertThat(policy.authorize(ToolCall.builder("db.write").principal("alice").build()))
                .isEqualTo(ToolDecision.ALLOW);
        assertThat(policy.authorize(ToolCall.builder("db.write").principal("alice").build()))
                .isEqualTo(ToolDecision.ALLOW);
        assertThat(policy.authorize(ToolCall.builder("db.write").principal("alice").build()))
                .isEqualTo(ToolDecision.DENY);
        assertThat(policy.authorize(ToolCall.builder("db.write").principal("bob").build()))
                .isEqualTo(ToolDecision.ALLOW); // bob 的额度独立
        assertThat(policy.recordedCalls("alice", "db.write")).isEqualTo(2);
        assertThat(policy.recordedCalls("bob", "db.write")).isEqualTo(1);
    }

    @Test
    void 单键重置只清指定调用方与工具() {
        ToolPolicy policy = ToolPolicy.of(ToolSpec.builder("db.write").maxCallsPerRun(1).build());
        policy.authorize(ToolCall.builder("db.write").principal("alice").build());
        policy.reset("alice", "db.write");
        assertThat(policy.authorize(ToolCall.builder("db.write").principal("alice").build()))
                .isEqualTo(ToolDecision.ALLOW);
        policy.authorize(ToolCall.builder("db.write").principal("bob").build());
        policy.resetRun();
        assertThat(policy.recordedCalls("bob", "db.write")).isZero();
    }

    // ---------- 注册期行为与并发 ----------

    @Test
    void 重复注册同名工具后者生效() {
        ToolPolicy policy = ToolPolicy.of();
        policy.register(ToolSpec.readOnly("t"));
        policy.register(ToolSpec.builder("t").risk(ToolRisk.DESTRUCTIVE).build());
        assertThat(policy.spec("t").orElseThrow().risk()).isEqualTo(ToolRisk.DESTRUCTIVE);
        assertThat(policy.authorize(ToolCall.builder("t").build()))
                .isEqualTo(ToolDecision.REQUIRE_APPROVAL);
    }

    @Test
    void 注册期拒绝空spec() {
        ToolPolicy policy = ToolPolicy.of();
        assertThatThrownBy(() -> policy.register((ToolSpec) null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> policy.authorize(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void 注册与查询视图不可变() {
        ToolPolicy policy = ToolPolicy.of(ToolSpec.readOnly("t"));
        assertThatThrownBy(() -> policy.toolNames().add("u"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> policy.specs().put("u", ToolSpec.readOnly("u")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(policy.specs()).containsOnlyKeys("t");
    }

    @Test
    void 并发授权不超过次数上限() throws Exception {
        int max = 30;
        ToolPolicy policy = ToolPolicy.of(ToolSpec.builder("db.write").maxCallsPerRun(max).build());
        int threads = 8;
        int perThread = 10;
        AtomicInteger allowed = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                futures.add(
                        pool.submit(
                                () -> {
                                    start.await();
                                    for (int i = 0; i < perThread; i++) {
                                        if (policy.authorize(ToolCall.builder("db.write").build())
                                                == ToolDecision.ALLOW) {
                                            allowed.incrementAndGet();
                                        }
                                    }
                                    return null;
                                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdown();
        }
        assertThat(allowed.get()).isEqualTo(max);
        assertThat(policy.recordedCalls("anonymous", "db.write")).isEqualTo(max);
    }
}
