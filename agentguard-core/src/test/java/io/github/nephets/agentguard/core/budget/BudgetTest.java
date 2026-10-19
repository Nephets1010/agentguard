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
package io.github.nephets.agentguard.core.budget;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BudgetTest {

    @Test
    void 快速构造保留三个上限() {
        Budget budget = Budget.of(100L, 200L, 3);
        assertThat(budget.maxTokens()).isEqualTo(100L);
        assertThat(budget.maxCostMicros()).isEqualTo(200L);
        assertThat(budget.maxCalls()).isEqualTo(3);
        assertThat(budget.toString()).contains("100");
    }

    @Test
    void 默认构造不限制() {
        Budget budget = Budget.builder().build();
        assertThat(budget.maxTokens()).isEqualTo(Long.MAX_VALUE);
        assertThat(budget.maxCostMicros()).isEqualTo(Long.MAX_VALUE);
        assertThat(budget.maxCalls()).isEqualTo(Integer.MAX_VALUE);
        assertThat(Budget.unlimited().maxTokens()).isEqualTo(Long.MAX_VALUE);
    }
}
