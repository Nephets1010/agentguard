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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentguardTenantTest {

    @AfterEach
    void 清理上下文() {
        AgentguardTenant.clear();
    }

    @Test
    void 设置后可读取() {
        AgentguardTenant.set("acme");
        assertThat(AgentguardTenant.current()).isEqualTo("acme");
        assertThat(AgentguardTenant.currentOrElse("fallback")).isEqualTo("acme");
    }

    @Test
    void 未设置时回退到默认值() {
        assertThat(AgentguardTenant.current()).isNull();
        assertThat(AgentguardTenant.currentOrElse("fallback")).isEqualTo("fallback");
    }

    @Test
    void 未设置时强制取值报错() {
        assertThatThrownBy(AgentguardTenant::require).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 带返回值的执行块内可读到租户() {
        String value =
                AgentguardTenant.runWith(
                        "globex",
                        (java.util.function.Supplier<String>) () -> AgentguardTenant.current());
        assertThat(value).isEqualTo("globex");
    }

    @Test
    void 执行块结束后恢复原上下文() {
        AgentguardTenant.set("outer");
        AgentguardTenant.runWith(
                "inner",
                () -> {
                    assertThat(AgentguardTenant.current()).isEqualTo("inner");
                });
        assertThat(AgentguardTenant.current()).isEqualTo("outer");
    }
}
