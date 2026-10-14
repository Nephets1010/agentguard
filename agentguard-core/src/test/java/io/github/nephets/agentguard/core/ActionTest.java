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

import static org.assertj.core.api.Assertions.assertThat;

class ActionTest {

    @Test
    void 五个动作从放行到拦截排列() {
        assertThat(Action.values())
                .containsExactly(
                        Action.ALLOW,
                        Action.WARN,
                        Action.REDACT,
                        Action.REQUIRE_APPROVAL,
                        Action.BLOCK);
    }

    @Test
    void 取更严格动作时保留拦截() {
        assertThat(Action.BLOCK.mostSevere(Action.ALLOW)).isEqualTo(Action.BLOCK);
        assertThat(Action.ALLOW.mostSevere(Action.REQUIRE_APPROVAL))
                .isEqualTo(Action.REQUIRE_APPROVAL);
    }
}
