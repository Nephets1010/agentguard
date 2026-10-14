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

import java.time.Instant;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class GuardrailContextTest {

    @Test
    void 默认构造给输入阶段与默认租户() {
        GuardrailContext ctx = GuardrailContext.builder().build();
        assertThat(ctx.stage()).isEqualTo(GuardrailContext.STAGE_INPUT);
        assertThat(ctx.text()).isEmpty();
        assertThat(ctx.tenantId()).isEqualTo("default");
        assertThat(ctx.attributes()).isEmpty();
        assertThat(ctx.now()).isNotNull();
    }

    @Test
    void 显式赋值后逐字段可读() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        GuardrailContext ctx =
                GuardrailContext.builder()
                        .tenantId("acme")
                        .stage(GuardrailContext.STAGE_OUTPUT)
                        .text("你好")
                        .attribute("k", 1)
                        .now(now)
                        .build();
        assertThat(ctx.tenantId()).isEqualTo("acme");
        assertThat(ctx.stage()).isEqualTo(GuardrailContext.STAGE_OUTPUT);
        assertThat(ctx.text()).isEqualTo("你好");
        assertThat(ctx.attributes()).containsEntry("k", 1);
        assertThat(ctx.now()).isEqualTo(now);
    }

    @Test
    void 租户为空时取线程上下文() {
        GuardrailContext ctx =
                AgentguardTenant.runWith(
                        "globex",
                        (Supplier<GuardrailContext>) () -> GuardrailContext.builder().build());
        assertThat(ctx.tenantId()).isEqualTo("globex");
    }

    @Test
    void 文本与阶段副本只改对应字段() {
        GuardrailContext ctx = GuardrailContext.builder().tenantId("acme").text("原文").build();
        GuardrailContext withText = ctx.withText("新文");
        assertThat(withText.text()).isEqualTo("新文");
        assertThat(withText.tenantId()).isEqualTo("acme");
        assertThat(ctx.text()).isEqualTo("原文");

        GuardrailContext withStage = ctx.withStage(GuardrailContext.STAGE_TOOL);
        assertThat(withStage.stage()).isEqualTo(GuardrailContext.STAGE_TOOL);
        assertThat(withStage.text()).isEqualTo("原文");
    }

    @Test
    void 追加属性生成新副本() {
        GuardrailContext ctx = GuardrailContext.builder().attributes(Map.of("a", 1)).build();
        GuardrailContext copy = ctx.withAttribute("b", 2);
        assertThat(ctx.attributes()).containsExactly(Map.entry("a", 1));
        assertThat(copy.attributes()).containsEntry("a", 1).containsEntry("b", 2);
    }
}
