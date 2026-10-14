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
package io.github.nephets.agentguard.core.output;

import io.github.nephets.agentguard.core.Guardrail;
import io.github.nephets.agentguard.core.GuardrailContext;
import io.github.nephets.agentguard.core.GuardrailResult;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 输出阶段规则：检测到个人信息就把文本改写成脱敏版本，动作记为 REDACT。
 *
 * <p>默认只在 OUTPUT 阶段生效，输入侧原文不做改写。
 */
public final class PiiRedactionGuardrail implements Guardrail {

    /** 默认生效的阶段。 */
    public static final Set<String> DEFAULT_STAGES = Set.of(GuardrailContext.STAGE_OUTPUT);

    private final PiiDetector detector;
    private final Redactor redactor;
    private final boolean enabled;
    private final Set<String> stages;

    /** 默认构造，脱敏全部类型，只在输出阶段生效。 */
    public PiiRedactionGuardrail() {
        this(new PiiDetector(), new Redactor(), true, DEFAULT_STAGES);
    }

    /**
     * @param detector 检测器
     * @param redactor 脱敏器
     */
    public PiiRedactionGuardrail(PiiDetector detector, Redactor redactor) {
        this(detector, redactor, true, DEFAULT_STAGES);
    }

    /**
     * @param detector 检测器
     * @param redactor 脱敏器
     * @param enabled 是否启用，关闭时始终放行
     */
    public PiiRedactionGuardrail(PiiDetector detector, Redactor redactor, boolean enabled) {
        this(detector, redactor, enabled, DEFAULT_STAGES);
    }

    /**
     * @param detector 检测器
     * @param redactor 脱敏器
     * @param enabled 是否启用
     * @param stages 生效的阶段，为空时表示全部阶段
     */
    public PiiRedactionGuardrail(
            PiiDetector detector, Redactor redactor, boolean enabled, Collection<String> stages) {
        this.detector = detector == null ? new PiiDetector() : detector;
        this.redactor = redactor == null ? new Redactor(this.detector) : redactor;
        this.enabled = enabled;
        this.stages = stages == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(stages));
    }

    /**
     * @return 是否启用
     */
    public boolean enabled() {
        return enabled;
    }

    /**
     * @return 生效的阶段，空集合表示全部阶段
     */
    public Set<String> stages() {
        return stages;
    }

    @Override
    public GuardrailResult check(GuardrailContext ctx) {
        if (ctx == null || !enabled) {
            return GuardrailResult.allow();
        }
        if (!stages.isEmpty() && !stages.contains(ctx.stage())) {
            return GuardrailResult.allow();
        }
        String text = ctx.text() == null ? "" : ctx.text();
        if (text.isEmpty()) {
            return GuardrailResult.allow();
        }
        Redaction redaction = redactor.redactWithFindings(text);
        if (redaction.findings().isEmpty()) {
            return GuardrailResult.allow();
        }
        if (redaction.text().equals(text)) {
            return GuardrailResult.allow(redaction.findings());
        }
        return GuardrailResult.redact(redaction.text(), redaction.findings());
    }

    /**
     * @return 使用的检测器
     */
    public PiiDetector detector() {
        return detector;
    }
}
