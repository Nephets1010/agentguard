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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuardrailChainEdgeTest {

    /** 记录执行顺序的探针规则。 */
    private static final class ProbeGuardrail implements Guardrail {

        private final String name;
        private final GuardrailResult result;
        private final List<String> order;

        private ProbeGuardrail(String name, GuardrailResult result, List<String> order) {
            this.name = name;
            this.result = result;
            this.order = order;
        }

        @Override
        public GuardrailResult check(GuardrailContext ctx) {
            order.add(name + ":" + ctx.text());
            return result;
        }
    }

    @Test
    void 规则按注册顺序执行() {
        List<String> order = new ArrayList<>();
        GuardrailChain chain =
                new GuardrailChain(
                        List.of(
                                new ProbeGuardrail("a", GuardrailResult.allow(), order),
                                new ProbeGuardrail("b", GuardrailResult.allow(), order),
                                new ProbeGuardrail("c", GuardrailResult.allow(), order)));
        chain.check(GuardrailContext.builder().text("hi").build());
        assertThat(order).containsExactly("a:hi", "b:hi", "c:hi");
    }

    @Test
    void 拦截后后续规则不再执行() {
        List<String> order = new ArrayList<>();
        GuardrailChain chain =
                new GuardrailChain(
                        List.of(
                                new ProbeGuardrail("a", GuardrailResult.allow(), order),
                                new ProbeGuardrail(
                                        "b",
                                        GuardrailResult.block(List.of(Finding.error("r", "boom"))),
                                        order),
                                new ProbeGuardrail("c", GuardrailResult.allow(), order)));
        GuardrailResult result = chain.check(GuardrailContext.builder().text("hi").build());
        assertThat(result.action()).isEqualTo(Action.BLOCK);
        assertThat(order).containsExactly("a:hi", "b:hi");
    }

    @Test
    void 脱敏改写后后续规则看到的是新文本() {
        List<String> order = new ArrayList<>();
        GuardrailChain chain =
                new GuardrailChain(
                        List.of(
                                new ProbeGuardrail(
                                        "redact",
                                        GuardrailResult.redact(
                                                "clean", List.of(Finding.warn("r", "masked"))),
                                        order),
                                new ProbeGuardrail("after", GuardrailResult.allow(), order)));
        GuardrailResult result = chain.check(GuardrailContext.builder().text("dirty").build());
        assertThat(result.action()).isEqualTo(Action.REDACT);
        assertThat(result.sanitizedText()).isEqualTo("clean");
        assertThat(order).containsExactly("redact:dirty", "after:clean");
    }

    @Test
    void 空规则链与空原因按命中记录推导() {
        GuardrailChain chain = new GuardrailChain();
        assertThat(chain.size()).isZero();
        GuardrailResult result = chain.check(GuardrailContext.builder().text("x").build());
        assertThat(result.action()).isEqualTo(Action.ALLOW);
        assertThat(result.reason()).isEmpty();
    }

    @Test
    void 拦截原因缺省时由命中记录生成() {
        GuardrailChain chain =
                new GuardrailChain(
                        List.of(
                                (Guardrail)
                                        ctx ->
                                                GuardrailResult.block(
                                                        List.of(Finding.error("input.x", "boom")),
                                                        "")));
        GuardrailResult result = chain.check(GuardrailContext.builder().text("x").build());
        assertThat(result.action()).isEqualTo(Action.BLOCK);
        assertThat(result.reason()).contains("input.x");
    }

    @Test
    void 规则返回空结果被跳过() {
        List<String> order = new ArrayList<>();
        GuardrailChain chain =
                new GuardrailChain(
                        List.of(
                                new ProbeGuardrail("a", null, order),
                                new ProbeGuardrail("b", GuardrailResult.allow(), order)));
        assertThat(chain.check(GuardrailContext.builder().text("x").build()).action())
                .isEqualTo(Action.ALLOW);
        assertThat(order).containsExactly("a:x", "b:x");
    }

    @Test
    void 注册视图不可变且追加不影响已构建链() {
        GuardrailChain chain = GuardrailChain.builder().add(ctx -> GuardrailResult.allow()).build();
        List<Guardrail> guardrails = chain.guardrails();
        assertThat(guardrails).hasSize(1);
        assertThatThrownBy(() -> guardrails.add(ctx -> GuardrailResult.allow()))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(chain.size()).isEqualTo(1);
    }

    @Test
    void 追加规则返回自身便于链式调用() {
        GuardrailChain chain = new GuardrailChain();
        assertThat(chain.add(ctx -> GuardrailResult.allow())).isSameAs(chain);
        assertThat(chain.add(null)).isSameAs(chain);
        assertThat(chain.size()).isEqualTo(1);
    }
}
