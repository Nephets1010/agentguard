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
package io.github.nephets.agentguard.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 一次检查的结果：处置动作、命中记录、改写后的文本与原因。 */
public final class GuardrailResult {

    private final Action action;
    private final List<Finding> findings;
    private final String sanitizedText;
    private final String reason;

    private GuardrailResult(
            Action action, List<Finding> findings, String sanitizedText, String reason) {
        this.action = action;
        this.findings = Collections.unmodifiableList(new ArrayList<>(findings));
        this.sanitizedText = sanitizedText;
        this.reason = reason == null ? "" : reason;
    }

    /**
     * @return 放行，无命中记录
     */
    public static GuardrailResult allow() {
        return new GuardrailResult(Action.ALLOW, List.of(), null, "");
    }

    /**
     * @return 放行，附带命中记录
     */
    public static GuardrailResult allow(List<Finding> findings) {
        return new GuardrailResult(Action.ALLOW, copy(findings), null, "");
    }

    /**
     * @return 放行但提示，附带命中记录
     */
    public static GuardrailResult warn(List<Finding> findings) {
        return warn(findings, describe(findings));
    }

    /**
     * @return 放行但提示，附带命中记录与原因
     */
    public static GuardrailResult warn(List<Finding> findings, String reason) {
        return new GuardrailResult(Action.WARN, copy(findings), null, reason);
    }

    /**
     * @return 拦截，附带命中记录，原因由命中记录生成
     */
    public static GuardrailResult block(List<Finding> findings) {
        return block(findings, describe(findings));
    }

    /**
     * @return 拦截，附带命中记录与原因
     */
    public static GuardrailResult block(List<Finding> findings, String reason) {
        return new GuardrailResult(Action.BLOCK, copy(findings), null, reason);
    }

    /**
     * @return 改写文本后放行
     */
    public static GuardrailResult redact(String sanitizedText, List<Finding> findings) {
        return redact(sanitizedText, findings, describe(findings));
    }

    /**
     * @return 改写文本后放行，附带原因
     */
    public static GuardrailResult redact(
            String sanitizedText, List<Finding> findings, String reason) {
        if (sanitizedText == null) {
            throw new IllegalArgumentException("sanitizedText 不能为空");
        }
        return new GuardrailResult(Action.REDACT, copy(findings), sanitizedText, reason);
    }

    /**
     * @return 需要审批
     */
    public static GuardrailResult requireApproval(List<Finding> findings, String reason) {
        return new GuardrailResult(Action.REQUIRE_APPROVAL, copy(findings), null, reason);
    }

    /**
     * 按命中记录推导结果：存在 ERROR 则拦截，存在 WARN 则提示，否则放行。
     *
     * @param findings 命中记录
     * @return 推导出的结果
     */
    public static GuardrailResult fromFindings(List<Finding> findings) {
        List<Finding> copy = copy(findings);
        if (copy.isEmpty()) {
            return allow();
        }
        boolean hasError = copy.stream().anyMatch(f -> f.severity() == Severity.ERROR);
        if (hasError) {
            return block(copy);
        }
        boolean hasWarn = copy.stream().anyMatch(f -> f.severity() == Severity.WARN);
        if (hasWarn) {
            return warn(copy);
        }
        return allow(copy);
    }

    /**
     * @return 处置动作
     */
    public Action action() {
        return action;
    }

    /**
     * @return 命中记录，不可变列表
     */
    public List<Finding> findings() {
        return findings;
    }

    /**
     * @return 改写后的文本，未改写时为 null
     */
    public String sanitizedText() {
        return sanitizedText;
    }

    /**
     * @return 原因，可能为空白
     */
    public String reason() {
        return reason;
    }

    /**
     * @return 是否被拦截
     */
    public boolean isBlocked() {
        return action == Action.BLOCK;
    }

    /**
     * @return 是否改写了文本
     */
    public boolean hasSanitizedText() {
        return sanitizedText != null;
    }

    @Override
    public String toString() {
        return "GuardrailResult{action="
                + action
                + ", findings="
                + findings.size()
                + ", reason="
                + reason
                + "}";
    }

    private static List<Finding> copy(List<Finding> findings) {
        if (findings == null) {
            return List.of();
        }
        return List.copyOf(findings);
    }

    private static String describe(List<Finding> findings) {
        if (findings == null || findings.isEmpty()) {
            return "";
        }
        Set<String> ruleIds = new LinkedHashSet<>();
        for (Finding finding : findings) {
            if (finding.severity() != Severity.INFO) {
                ruleIds.add(finding.ruleId());
            }
        }
        if (ruleIds.isEmpty()) {
            for (Finding finding : findings) {
                ruleIds.add(finding.ruleId());
            }
        }
        return "命中规则: " + String.join(", ", ruleIds);
    }
}
