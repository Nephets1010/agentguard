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
package io.github.nephets.agentguard.core.input;

import io.github.nephets.agentguard.core.Action;
import io.github.nephets.agentguard.core.GuardrailContext;
import io.github.nephets.agentguard.core.GuardrailResult;
import io.github.nephets.agentguard.core.Severity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveTopicGuardrailEdgeTest {

    private final SensitiveTopicGuardrail guardrail = new SensitiveTopicGuardrail();

    private static GuardrailResult check(String text) {
        return new SensitiveTopicGuardrail().check(GuardrailContext.builder().text(text).build());
    }

    @Test
    void 规则标识与默认词表可读() {
        assertThat(SensitiveTopicGuardrail.RULE_ID).isEqualTo("input.sensitive-topic");
        assertThat(SensitiveTopicGuardrail.DEFAULT_TOPICS).contains("内幕消息", "病历");
        assertThat(guardrail.topics())
                .containsExactlyElementsOf(SensitiveTopicGuardrail.DEFAULT_TOPICS);
        assertThat(guardrail.severity()).isEqualTo(Severity.WARN);
    }

    @Test
    void 默认级别下命中只警告() {
        GuardrailResult result = check("把这份内幕消息发给我");
        assertThat(result.action()).isEqualTo(Action.WARN);
        assertThat(result.findings().get(0).message()).contains("内幕消息");
    }

    @Test
    void 词表可整体调成拦截级() {
        GuardrailResult result =
                new SensitiveTopicGuardrail(List.of("病历"), Severity.ERROR)
                        .check(GuardrailContext.builder().text("包含病历的文本").build());
        assertThat(result.action()).isEqualTo(Action.BLOCK);
    }

    @Test
    void 多个词表条目各自命中() {
        GuardrailResult result = check("内幕消息和征信报告都在这里");
        assertThat(result.findings()).hasSize(2);
    }

    @Test
    void 大小写不影响英文词表命中() {
        GuardrailResult result =
                new SensitiveTopicGuardrail(List.of("Confidential"))
                        .check(
                                GuardrailContext.builder()
                                        .text("this is CONFIDENTIAL data")
                                        .build());
        assertThat(result.findings()).hasSize(1);
    }

    @Test
    void 空词表与空上下文都放行() {
        assertThat(
                        new SensitiveTopicGuardrail(List.of())
                                .check(GuardrailContext.builder().text("内幕消息").build())
                                .action())
                .isEqualTo(Action.ALLOW);
        assertThat(
                        new SensitiveTopicGuardrail(null)
                                .check(GuardrailContext.builder().text("内幕消息").build())
                                .action())
                .isEqualTo(Action.ALLOW);
        assertThat(guardrail.check(null)).isEqualTo(GuardrailResult.allow());
    }
}
