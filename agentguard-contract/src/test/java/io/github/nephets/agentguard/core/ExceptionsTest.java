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

class ExceptionsTest {

    @Test
    void 拦截异常带检查结果() {
        GuardrailResult result = GuardrailResult.block(java.util.List.of(), "命中规则: r");
        GuardrailBlockedException exception = new GuardrailBlockedException(result);
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception.getMessage()).contains("命中规则: r");
        assertThat(exception.result()).isSameAs(result);
    }

    @Test
    void 拦截异常支持纯文本构造() {
        GuardrailBlockedException exception = new GuardrailBlockedException("被拦截");
        assertThat(exception.getMessage()).isEqualTo("被拦截");
        assertThat(exception.result()).isNull();
    }
}
