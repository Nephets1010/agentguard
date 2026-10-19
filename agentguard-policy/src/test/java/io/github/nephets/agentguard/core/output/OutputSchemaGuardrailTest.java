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

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OutputSchemaGuardrailTest {

    private static GuardrailResult check(
            OutputSchemaGuardrail guardrail, String text, GuardrailContext.Builder builder) {
        GuardrailContext ctx =
                builder == null
                        ? GuardrailContext.builder().text(text).build()
                        : builder.text(text).build();
        return guardrail.check(ctx);
    }

    @Test
    void 超过长度上限被拦截() {
        OutputSchemaGuardrail guardrail = OutputSchemaGuardrail.builder().maxLength(5).build();
        GuardrailResult result = check(guardrail, "123456", null);
        assertThat(result.action()).isEqualTo(Action.BLOCK);
        assertThat(result.findings().get(0).ruleId()).isEqualTo(OutputSchemaGuardrail.RULE_LENGTH);
        assertThat(guardrail.maxLength()).isEqualTo(5);
    }

    @Test
    void 缺少必填字段被拦截() {
        OutputSchemaGuardrail guardrail =
                OutputSchemaGuardrail.builder().requiredFields(Set.of("answer")).build();
        GuardrailResult result = check(guardrail, "结果", null);
        assertThat(result.action()).isEqualTo(Action.BLOCK);
        assertThat(result.findings().get(0).ruleId())
                .isEqualTo(OutputSchemaGuardrail.RULE_REQUIRED);
        assertThat(guardrail.requiredFields()).containsExactly("answer");
    }

    @Test
    void 输出含密钥格式被拦截() {
        OutputSchemaGuardrail guardrail = OutputSchemaGuardrail.builder().build();
        GuardrailResult result = check(guardrail, "key = sk-abcdefghijklmnopqrst", null);
        assertThat(result.action()).isEqualTo(Action.BLOCK);
        assertThat(result.findings().get(0).ruleId())
                .isEqualTo(OutputSchemaGuardrail.RULE_FORBIDDEN);
        assertThat(result.findings().get(0).evidence()).contains("sk-");
    }

    @Test
    void 输出含私钥头被拦截() {
        OutputSchemaGuardrail guardrail = OutputSchemaGuardrail.builder().build();
        assertThat(check(guardrail, "-----BEGIN RSA PRIVATE KEY-----", null).action())
                .isEqualTo(Action.BLOCK);
    }

    @Test
    void 非法正则按字面量处理() {
        OutputSchemaGuardrail guardrail =
                OutputSchemaGuardrail.builder().forbiddenPatterns(List.of("[")).build();
        assertThat(check(guardrail, "a[b", null).action()).isEqualTo(Action.BLOCK);
    }

    @Test
    void 空上下文放行() {
        assertThat(OutputSchemaGuardrail.builder().build().check(null).action())
                .isEqualTo(Action.ALLOW);
    }
}
