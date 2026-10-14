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

import io.github.nephets.agentguard.core.ToolNotAllowedException;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolPolicyTest {

    private static ToolPolicy policy() {
        return ToolPolicy.of(
                ToolSpec.builder("search.doc").risk(ToolRisk.READ).build(),
                ToolSpec.builder("db.write")
                        .risk(ToolRisk.WRITE)
                        .allowedRoles(Set.of("admin"))
                        .maxCallsPerRun(2)
                        .build(),
                ToolSpec.builder("db.drop")
                        .risk(ToolRisk.DESTRUCTIVE)
                        .allowedRoles(Set.of("admin"))
                        .build(),
                ToolSpec.builder("report.export")
                        .risk(ToolRisk.WRITE)
                        .requiresApproval(true)
                        .build());
    }

    @Test
    void 未注册的工具默认拒绝() {
        ToolPolicy policy = policy();
        assertThat(policy.authorize(ToolCall.builder("unknown.tool").build()))
                .isEqualTo(ToolDecision.DENY);
        assertThat(policy.toolNames())
                .contains("search.doc", "db.write", "db.drop", "report.export");
    }

    @Test
    void 角色不足被拒绝() {
        ToolPolicy policy = policy();
        ToolCall call =
                ToolCall.builder("db.write").principal("alice").roles(Set.of("user")).build();
        assertThat(policy.authorize(call)).isEqualTo(ToolDecision.DENY);
    }

    @Test
    void 角色匹配时放行并记账() {
        ToolPolicy policy = policy();
        ToolCall call =
                ToolCall.builder("db.write").principal("root").roles(Set.of("admin")).build();
        assertThat(policy.authorize(call)).isEqualTo(ToolDecision.ALLOW);
        assertThat(policy.recordedCalls("root", "db.write")).isEqualTo(1);
        assertThat(policy.remainingCalls("root", "db.write")).isEqualTo(1);
    }

    @Test
    void 次数用尽后被拒绝() {
        ToolPolicy policy = policy();
        ToolCall call =
                ToolCall.builder("db.write").principal("root").roles(Set.of("admin")).build();
        assertThat(policy.authorize(call)).isEqualTo(ToolDecision.ALLOW);
        assertThat(policy.authorize(call)).isEqualTo(ToolDecision.ALLOW);
        assertThat(policy.authorize(call)).isEqualTo(ToolDecision.DENY);
    }

    @Test
    void 破坏性工具需要审批() {
        ToolPolicy policy = policy();
        ToolCall call =
                ToolCall.builder("db.drop").principal("root").roles(Set.of("admin")).build();
        assertThat(policy.authorize(call)).isEqualTo(ToolDecision.REQUIRE_APPROVAL);
        assertThat(policy.recordedCalls("root", "db.drop")).isZero();
    }

    @Test
    void 被拒绝时强制校验抛异常() {
        ToolPolicy policy = policy();
        ToolCall call = ToolCall.builder("unknown.tool").principal("alice").build();
        assertThatThrownBy(() -> policy.require(call))
                .isInstanceOf(ToolNotAllowedException.class)
                .hasMessageContaining("unknown.tool");
    }

    @Test
    void 清空记录后可继续调用() {
        ToolPolicy policy = policy();
        ToolCall call = ToolCall.builder("db.write").roles(Set.of("admin")).build();
        policy.authorize(call);
        policy.authorize(call);
        policy.resetRun();
        assertThat(policy.authorize(call)).isEqualTo(ToolDecision.ALLOW);
    }
}
