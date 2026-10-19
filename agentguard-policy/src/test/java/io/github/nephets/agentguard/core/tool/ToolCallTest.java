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

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ToolCallTest {

    @Test
    void 缺省调用方为匿名() {
        ToolCall call = ToolCall.builder("search.doc").build();
        assertThat(call.toolName()).isEqualTo("search.doc");
        assertThat(call.principal()).isEqualTo("anonymous");
        assertThat(call.roles()).isEmpty();
        assertThat(call.arguments()).isEmpty();
    }

    @Test
    void 快速构造保留全部字段() {
        ToolCall call = ToolCall.of("db.write", Map.of("table", "orders"), "alice", Set.of("user"));
        assertThat(call.toolName()).isEqualTo("db.write");
        assertThat(call.arguments()).containsEntry("table", "orders");
        assertThat(call.principal()).isEqualTo("alice");
        assertThat(call.roles()).containsExactly("user");
        assertThat(call.toString()).contains("db.write");
    }
}
