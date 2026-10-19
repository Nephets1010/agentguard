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
package io.github.nephets.agentguard.core.tool;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ToolDecisionTest {

    @Test
    void 三个判定取值完备() {
        assertThat(ToolDecision.values())
                .containsExactly(
                        ToolDecision.ALLOW, ToolDecision.DENY, ToolDecision.REQUIRE_APPROVAL);
    }

    @Test
    void 判定可被序列化端按名字还原() {
        // starter 的 ToolDecisionResponse 直接用 name() 传输，反序列化依赖 valueOf。
        assertThat(ToolDecision.valueOf(ToolDecision.REQUIRE_APPROVAL.name()))
                .isEqualTo(ToolDecision.REQUIRE_APPROVAL);
    }
}
