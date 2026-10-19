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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PatternGuardrailTest {

    /** 暴露受保护构造器的具体子类。 */
    private static final class TestPatternGuardrail extends PatternGuardrail {

        private TestPatternGuardrail(String ruleId, String description, List<Phrase> phrases) {
            super(ruleId, description, phrases);
        }
    }

    @Test
    void 规则标识与短语表可读且短语表不可变() {
        PatternGuardrail guardrail =
                new TestPatternGuardrail(
                        "test.rule", "说明", List.of(Phrase.of("hit", Severity.ERROR)));
        assertThat(guardrail.ruleId()).isEqualTo("test.rule");
        assertThat(guardrail.phrases()).hasSize(1);
        assertThatThrownBy(() -> guardrail.phrases().add(Phrase.of("x")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void 命中时说明文字与短语进入消息() {
        PatternGuardrail guardrail =
                new TestPatternGuardrail(
                        "test.rule", "测试说明", List.of(Phrase.of("boom", Severity.ERROR)));
        GuardrailResult result =
                guardrail.check(GuardrailContext.builder().text("a boom here").build());
        assertThat(result.action()).isEqualTo(Action.BLOCK);
        assertThat(result.findings().get(0).message()).isEqualTo("测试说明：boom");
    }

    @Test
    void 空短语表不命中任何文本() {
        PatternGuardrail guardrail = new TestPatternGuardrail("test.rule", "", List.of());
        assertThat(guardrail.check(GuardrailContext.builder().text("anything").build()).action())
                .isEqualTo(Action.ALLOW);
    }

    @Test
    void 空规则标识构造报错() {
        assertThatThrownBy(() -> new TestPatternGuardrail("  ", "", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ruleId");
        assertThatThrownBy(() -> new TestPatternGuardrail(null, "", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 空描述按空串处理() {
        PatternGuardrail guardrail =
                new TestPatternGuardrail("test.rule", null, List.of(Phrase.of("x")));
        assertThat(guardrail.check(GuardrailContext.builder().text("x").build()).findings())
                .hasSize(1);
    }
}
