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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuardrailChainTest {

    private static final String TEXT = "原始文本";

    private static GuardrailContext ctx() {
        return GuardrailContext.builder().text(TEXT).build();
    }

    private static Guardrail fixed(GuardrailResult result) {
        return ctx -> result;
    }

    @Test
    void 空规则链放行() {
        GuardrailChain chain = new GuardrailChain();
        assertThat(chain.check(ctx()).action()).isEqualTo(Action.ALLOW);
        assertThat(chain.size()).isZero();
    }

    @Test
    void 构造注入与追加规则都生效() {
        GuardrailChain chain =
                new GuardrailChain(
                        List.of(
                                fixed(GuardrailResult.allow()),
                                fixed(GuardrailResult.warn(List.of()))));
        chain.add(fixed(GuardrailResult.allow()));
        assertThat(chain.size()).isEqualTo(3);
        assertThat(chain.guardrails()).hasSize(3);
    }

    @Test
    void 遇到拦截立即短路() {
        AtomicInteger calls = new AtomicInteger();
        GuardrailChain chain =
                new GuardrailChain(
                        List.of(
                                fixed(GuardrailResult.block(List.of(Finding.error("r", "命中")))),
                                ctx -> {
                                    calls.incrementAndGet();
                                    return GuardrailResult.allow();
                                }));
        GuardrailResult result = chain.check(ctx());
        assertThat(result.action()).isEqualTo(Action.BLOCK);
        assertThat(calls.get()).isZero();
    }

    @Test
    void 出现错误级命中也短路() {
        GuardrailChain chain =
                new GuardrailChain(
                        List.of(fixed(GuardrailResult.allow(List.of(Finding.error("r", "命中"))))));
        assertThat(chain.check(ctx()).action()).isEqualTo(Action.BLOCK);
    }

    @Test
    void 只有警告时聚合为警告() {
        GuardrailChain chain =
                new GuardrailChain(
                        List.of(
                                fixed(GuardrailResult.allow()),
                                fixed(GuardrailResult.warn(List.of(Finding.warn("w", "警告"))))));
        GuardrailResult result = chain.check(ctx());
        assertThat(result.action()).isEqualTo(Action.WARN);
        assertThat(result.findings()).hasSize(1);
    }

    @Test
    void 审批动作比警告更严格() {
        GuardrailChain chain =
                new GuardrailChain(
                        List.of(
                                fixed(GuardrailResult.warn(List.of(Finding.warn("w", "警告")))),
                                fixed(GuardrailResult.requireApproval(List.of(), "需要审批"))));
        GuardrailResult result = chain.check(ctx());
        assertThat(result.action()).isEqualTo(Action.REQUIRE_APPROVAL);
        assertThat(result.reason()).isEqualTo("需要审批");
    }

    @Test
    void 脱敏后文本传给后续规则() {
        List<String> seen = new ArrayList<>();
        GuardrailChain chain =
                new GuardrailChain(
                        List.of(
                                fixed(
                                        GuardrailResult.redact(
                                                "已脱敏", List.of(Finding.warn("pii", "命中")))),
                                ctx -> {
                                    seen.add(ctx.text());
                                    return GuardrailResult.allow();
                                }));
        GuardrailResult result = chain.check(ctx());
        assertThat(result.action()).isEqualTo(Action.REDACT);
        assertThat(result.sanitizedText()).isEqualTo("已脱敏");
        assertThat(seen).containsExactly("已脱敏");
    }

    @Test
    void 上下文为空时报错() {
        GuardrailChain chain = new GuardrailChain();
        assertThatThrownBy(() -> chain.check(null)).isInstanceOf(NullPointerException.class);
    }
}
