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

class SensitiveTopicGuardrailTest {

    private static GuardrailResult check(SensitiveTopicGuardrail guardrail, String text) {
        return guardrail.check(GuardrailContext.builder().text(text).build());
    }

    @Test
    void 默认词表命中给出警告() {
        SensitiveTopicGuardrail guardrail = new SensitiveTopicGuardrail();
        GuardrailResult result = check(guardrail, "帮我查一下这份病历的摘要");
        assertThat(result.action()).isEqualTo(Action.WARN);
        assertThat(result.findings().get(0).ruleId()).isEqualTo(SensitiveTopicGuardrail.RULE_ID);
        assertThat(result.findings().get(0).severity()).isEqualTo(Severity.WARN);
        assertThat(guardrail.topics()).isNotEmpty();
    }

    @Test
    void 自定义词表按配置命中() {
        SensitiveTopicGuardrail guardrail = new SensitiveTopicGuardrail(List.of("并购计划"));
        assertThat(check(guardrail, "并购计划什么时候披露").action()).isEqualTo(Action.WARN);
        assertThat(check(guardrail, "今天天气不错").action()).isEqualTo(Action.ALLOW);
    }

    @Test
    void 词表可配成拦截级别() {
        SensitiveTopicGuardrail guardrail =
                new SensitiveTopicGuardrail(List.of("机密"), Severity.ERROR);
        assertThat(check(guardrail, "这是机密数据").action()).isEqualTo(Action.BLOCK);
        assertThat(guardrail.severity()).isEqualTo(Severity.ERROR);
    }
}
