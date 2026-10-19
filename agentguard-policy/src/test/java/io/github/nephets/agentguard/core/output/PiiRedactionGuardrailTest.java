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

import io.github.nephets.agentguard.core.Action;
import io.github.nephets.agentguard.core.GuardrailContext;
import io.github.nephets.agentguard.core.GuardrailResult;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PiiRedactionGuardrailTest {

    private static GuardrailResult check(
            PiiRedactionGuardrail guardrail, String stage, String text) {
        return guardrail.check(GuardrailContext.builder().stage(stage).text(text).build());
    }

    @Test
    void 输出阶段命中后改写文本() {
        PiiRedactionGuardrail guardrail = new PiiRedactionGuardrail();
        GuardrailResult result = check(guardrail, GuardrailContext.STAGE_OUTPUT, "手机号 13812345678");
        assertThat(result.action()).isEqualTo(Action.REDACT);
        assertThat(result.sanitizedText()).isEqualTo("手机号 138****5678");
        assertThat(result.findings()).hasSize(1);
    }

    @Test
    void 输入阶段默认不改写() {
        PiiRedactionGuardrail guardrail = new PiiRedactionGuardrail();
        GuardrailResult result = check(guardrail, GuardrailContext.STAGE_INPUT, "手机号 13812345678");
        assertThat(result.action()).isEqualTo(Action.ALLOW);
        assertThat(result.sanitizedText()).isNull();
        assertThat(guardrail.stages()).isEqualTo(Set.of(GuardrailContext.STAGE_OUTPUT));
    }

    @Test
    void 关闭后始终放行() {
        PiiRedactionGuardrail guardrail =
                new PiiRedactionGuardrail(new PiiDetector(), new Redactor(), false);
        assertThat(guardrail.enabled()).isFalse();
        assertThat(check(guardrail, GuardrailContext.STAGE_OUTPUT, "手机号 13812345678").action())
                .isEqualTo(Action.ALLOW);
    }

    @Test
    void 空文本与空上下文放行() {
        PiiRedactionGuardrail guardrail = new PiiRedactionGuardrail();
        assertThat(check(guardrail, GuardrailContext.STAGE_OUTPUT, "").action())
                .isEqualTo(Action.ALLOW);
        assertThat(guardrail.check(null).action()).isEqualTo(Action.ALLOW);
        assertThat(guardrail.detector()).isNotNull();
    }
}
