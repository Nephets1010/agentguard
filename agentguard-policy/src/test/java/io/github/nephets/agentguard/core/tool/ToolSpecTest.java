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

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ToolSpecTest {

    @Test
    void 默认值为只读且不限制() {
        ToolSpec spec = ToolSpec.readOnly("search.doc");
        assertThat(spec.name()).isEqualTo("search.doc");
        assertThat(spec.risk()).isEqualTo(ToolRisk.READ);
        assertThat(spec.allowedRoles()).isEmpty();
        assertThat(spec.requiresApproval()).isFalse();
        assertThat(spec.maxCallsPerRun()).isZero();
    }

    @Test
    void 构造器赋值后可读() {
        ToolSpec spec =
                ToolSpec.builder("db.write")
                        .risk(ToolRisk.WRITE)
                        .allowedRoles(Set.of("admin"))
                        .allowedRole("dba")
                        .requiresApproval(true)
                        .maxCallsPerRun(5)
                        .build();
        assertThat(spec.risk()).isEqualTo(ToolRisk.WRITE);
        assertThat(spec.allowedRoles()).containsExactly("admin", "dba");
        assertThat(spec.requiresApproval()).isTrue();
        assertThat(spec.maxCallsPerRun()).isEqualTo(5);
        assertThat(spec.toString()).contains("db.write");
    }
}
