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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuardrailResultTest {

    @Test
    void 放行结果没有命中记录() {
        GuardrailResult result = GuardrailResult.allow();
        assertThat(result.action()).isEqualTo(Action.ALLOW);
        assertThat(result.findings()).isEmpty();
        assertThat(result.sanitizedText()).isNull();
        assertThat(result.isBlocked()).isFalse();
        assertThat(result.hasSanitizedText()).isFalse();
    }

    @Test
    void 告警结果只带警告级记录() {
        GuardrailResult result = GuardrailResult.warn(List.of(Finding.warn("r", "m")));
        assertThat(result.action()).isEqualTo(Action.WARN);
        assertThat(result.reason()).contains("r");
        assertThat(result.toString()).contains("WARN");
    }

    @Test
    void 拦截结果标记被拦截() {
        GuardrailResult result = GuardrailResult.block(List.of(Finding.error("r", "m")), "命中规则: r");
        assertThat(result.action()).isEqualTo(Action.BLOCK);
        assertThat(result.isBlocked()).isTrue();
        assertThat(result.reason()).isEqualTo("命中规则: r");
    }

    @Test
    void 脱敏结果带改写后的文本() {
        GuardrailResult result =
                GuardrailResult.redact("138****5678", List.of(Finding.warn("r", "m")));
        assertThat(result.action()).isEqualTo(Action.REDACT);
        assertThat(result.sanitizedText()).isEqualTo("138****5678");
        assertThat(result.hasSanitizedText()).isTrue();
        assertThatThrownBy(() -> GuardrailResult.redact(null, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 按命中记录推导时错误级优先于警告级() {
        GuardrailResult blocked =
                GuardrailResult.fromFindings(
                        List.of(Finding.warn("w", "m"), Finding.error("e", "m")));
        assertThat(blocked.action()).isEqualTo(Action.BLOCK);

        GuardrailResult warned = GuardrailResult.fromFindings(List.of(Finding.warn("w", "m")));
        assertThat(warned.action()).isEqualTo(Action.WARN);

        GuardrailResult allowed = GuardrailResult.fromFindings(List.of());
        assertThat(allowed.action()).isEqualTo(Action.ALLOW);
    }
}
