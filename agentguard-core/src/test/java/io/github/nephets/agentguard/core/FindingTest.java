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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FindingTest {

    @Test
    void 构造器保留全部字段() {
        Finding finding = new Finding("r1", Severity.WARN, "说明", "证据", 3, 7);
        assertThat(finding.ruleId()).isEqualTo("r1");
        assertThat(finding.severity()).isEqualTo(Severity.WARN);
        assertThat(finding.message()).isEqualTo("说明");
        assertThat(finding.evidence()).isEqualTo("证据");
        assertThat(finding.start()).isEqualTo(3);
        assertThat(finding.end()).isEqualTo(7);
    }

    @Test
    void 不带位置的工厂方法用占位值() {
        Finding finding = Finding.of("r1", Severity.INFO, "说明");
        assertThat(finding.start()).isEqualTo(Finding.UNKNOWN_POSITION);
        assertThat(finding.end()).isEqualTo(Finding.UNKNOWN_POSITION);
        assertThat(finding.evidence()).isEmpty();
    }

    @Test
    void 字段相同即相等() {
        Finding left = new Finding("r", Severity.ERROR, "m", "e", 0, 2);
        Finding right = new Finding("r", Severity.ERROR, "m", "e", 0, 2);
        assertThat(left).isEqualTo(right).hasSameHashCodeAs(right);
        assertThat(left.toString()).contains("r").contains("ERROR");
    }

    @Test
    void 规则标识为空时报错() {
        assertThatThrownBy(() -> new Finding(" ", Severity.INFO, "m", "", 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Finding("r", null, "m", "", 0, 0))
                .isInstanceOf(NullPointerException.class);
    }
}
