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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 规则链：按注册顺序执行 {@link Guardrail}。
 *
 * <ul>
 *   <li>任一步返回 BLOCK 或出现 ERROR 级别命中，立即短路返回 BLOCK；
 *   <li>REQUIRE_APPROVAL 比 WARN / REDACT 更严格，会体现在最终动作上；
 *   <li>REDACT 会把改写后的文本传给后续规则，最终返回改写结果。
 * </ul>
 */
public final class GuardrailChain implements Guardrail {

    private static final Logger log = LoggerFactory.getLogger(GuardrailChain.class);

    private final List<Guardrail> guardrails;

    /**
     * @return 空规则链的 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** 构造一条空规则链，之后可用 {@link #add(Guardrail)} 追加。 */
    public GuardrailChain() {
        this.guardrails = new ArrayList<>();
    }

    /**
     * @param guardrails 按顺序执行的规则，null 元素会被跳过
     */
    public GuardrailChain(List<Guardrail> guardrails) {
        this.guardrails = new ArrayList<>();
        if (guardrails != null) {
            for (Guardrail guardrail : guardrails) {
                if (guardrail != null) {
                    this.guardrails.add(guardrail);
                }
            }
        }
    }

    /**
     * 追加一条规则。
     *
     * @param guardrail 规则，为 null 时忽略
     * @return 当前规则链，便于链式调用
     */
    public GuardrailChain add(Guardrail guardrail) {
        if (guardrail != null) {
            this.guardrails.add(guardrail);
        }
        return this;
    }

    /**
     * @return 已注册的规则，不可变列表
     */
    public List<Guardrail> guardrails() {
        return List.copyOf(guardrails);
    }

    /**
     * @return 已注册规则的数量
     */
    public int size() {
        return guardrails.size();
    }

    @Override
    public GuardrailResult check(GuardrailContext ctx) {
        Objects.requireNonNull(ctx, "ctx");
        log.debug(
                "check invoked: stage={} tenant={} guardrails={}",
                ctx.stage(),
                ctx.tenantId(),
                guardrails.size());
        GuardrailContext current = ctx;
        String sanitized = null;
        boolean redactionRequested = false;
        Action action = Action.ALLOW;
        String reason = "";
        List<Finding> findings = new ArrayList<>();

        for (Guardrail guardrail : guardrails) {
            GuardrailResult result = guardrail.check(current);
            if (result == null) {
                continue;
            }
            findings.addAll(result.findings());
            if (!result.reason().isEmpty()) {
                reason = result.reason();
            }
            if (result.action() == Action.BLOCK || hasError(result)) {
                GuardrailResult blocked =
                        GuardrailResult.block(
                                findings,
                                reason.isEmpty()
                                        ? GuardrailResult.fromFindings(findings).reason()
                                        : reason);
                log.info(
                        "check result=BLOCK stage={} tenant={} findings={}",
                        ctx.stage(),
                        ctx.tenantId(),
                        blocked.findings().size());
                return blocked;
            }
            if (result.action() == Action.REDACT) {
                redactionRequested = true;
                action = action.mostSevere(Action.REDACT);
            }
            if (result.hasSanitizedText() && !result.sanitizedText().equals(current.text())) {
                sanitized = result.sanitizedText();
                current = current.withText(sanitized);
                redactionRequested = true;
                action = action.mostSevere(Action.REDACT);
            }
            action = action.mostSevere(strictOf(result.action()));
        }
        GuardrailResult result;
        if (redactionRequested) {
            String text = sanitized == null ? ctx.text() : sanitized;
            result = GuardrailResult.redact(text, findings, reason);
        } else if (action == Action.WARN) {
            result = GuardrailResult.warn(findings, reason);
        } else if (action == Action.REQUIRE_APPROVAL) {
            result = GuardrailResult.requireApproval(findings, reason);
        } else {
            result = GuardrailResult.allow(findings);
        }
        log.debug(
                "check result={} stage={} tenant={} findings={}",
                result.action(),
                ctx.stage(),
                ctx.tenantId(),
                result.findings().size());
        return result;
    }

    private static boolean hasError(GuardrailResult result) {
        return result.findings().stream().anyMatch(f -> f.severity() == Severity.ERROR);
    }

    private static Action strictOf(Action action) {
        return action == null ? Action.ALLOW : action;
    }

    /** GuardrailChain 的构造器。 */
    public static final class Builder {

        private final List<Guardrail> guardrails = new ArrayList<>();

        /** 追加一条规则。 */
        public Builder add(Guardrail guardrail) {
            if (guardrail != null) {
                guardrails.add(guardrail);
            }
            return this;
        }

        /**
         * @return 构造好的规则链
         */
        public GuardrailChain build() {
            return new GuardrailChain(guardrails);
        }
    }
}
