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

class ToolRiskTest {

    @Test
    void 三个等级从只读到破坏性排列() {
        assertThat(ToolRisk.values())
                .containsExactly(ToolRisk.READ, ToolRisk.WRITE, ToolRisk.DESTRUCTIVE);
    }

    @Test
    void 破坏性等级是三道闸里唯一强制审批的风险级() {
        // ToolPolicy.authorize 只对 DESTRUCTIVE 与 requiresApproval 强制审批；
        // 这里固定契约：DESTRUCTIVE 必须是独立取值，不能被当成 WRITE 的别名。
        assertThat(ToolRisk.DESTRUCTIVE).isNotEqualTo(ToolRisk.WRITE);
        assertThat(ToolRisk.valueOf("DESTRUCTIVE")).isEqualTo(ToolRisk.DESTRUCTIVE);
    }
}
