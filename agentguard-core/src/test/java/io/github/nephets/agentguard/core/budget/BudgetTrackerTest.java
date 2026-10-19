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

import io.github.nephets.agentguard.core.AgentguardTenant;
import io.github.nephets.agentguard.core.BudgetExceededException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BudgetTrackerTest {

    private static final BudgetKey KEY = BudgetKey.of("acme", "run-1");

    @AfterEach
    void 清理租户() {
        AgentguardTenant.clear();
    }

    @Test
    void 初始用量为零() {
        BudgetTracker tracker = new BudgetTracker(Budget.of(10L, 100L, 2));
        BudgetUsage usage = tracker.usage(KEY);
        assertThat(usage.tokens()).isZero();
        assertThat(usage.costMicros()).isZero();
        assertThat(usage.calls()).isZero();
        assertThat(usage.isExceeded()).isFalse();
        assertThat(usage.budget().maxTokens()).isEqualTo(10L);
        assertThat(tracker.budget().maxCalls()).isEqualTo(2);
    }

    @Test
    void 记账后剩余量减少() {
        BudgetTracker tracker = new BudgetTracker(Budget.of(10L, 100L, 2));
        tracker.consume(KEY, 4L, 30L);
        BudgetUsage usage = tracker.usage(KEY);
        assertThat(usage.tokens()).isEqualTo(4L);
        assertThat(usage.costMicros()).isEqualTo(30L);
        assertThat(usage.calls()).isEqualTo(1);
        assertThat(usage.remainingTokens()).isEqualTo(6L);
        assertThat(usage.remainingCostMicros()).isEqualTo(70L);
        assertThat(usage.remainingCalls()).isEqualTo(1);
        assertThat(tracker.remaining(KEY).tokens()).isEqualTo(4L);
        assertThat(usage.toString()).contains("tokens=4");
    }

    @Test
    void 次数超预算时抛异常() {
        BudgetTracker tracker = new BudgetTracker(Budget.of(100L, 1000L, 2));
        tracker.consume(KEY);
        tracker.consume(KEY);
        assertThat(tracker.isExceeded(KEY)).isFalse();
        assertThatThrownBy(() -> tracker.consume(KEY))
                .isInstanceOf(BudgetExceededException.class)
                .hasMessageContaining("acme");
        assertThat(tracker.isExceeded(KEY)).isTrue();
    }

    @Test
    void token超预算时抛异常() {
        BudgetTracker tracker = new BudgetTracker(Budget.of(10L, 1000L, 100));
        assertThatThrownBy(() -> tracker.consume(KEY, 11L, 0L))
                .isInstanceOf(BudgetExceededException.class);
    }

    @Test
    void 成本超预算时抛异常() {
        BudgetTracker tracker = new BudgetTracker(Budget.of(100L, 100L, 100));
        assertThatThrownBy(() -> tracker.consume(KEY, 1L, 101L))
                .isInstanceOf(BudgetExceededException.class);
    }

    @Test
    void 负的消耗量报错() {
        BudgetTracker tracker = new BudgetTracker(Budget.unlimited());
        assertThatThrownBy(() -> tracker.consume(KEY, -1L, 0L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tracker.consume(KEY, 0L, -1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 不带键的方法取当前租户() {
        BudgetTracker tracker = new BudgetTracker(Budget.of(100L, 1000L, 5));
        AgentguardTenant.set("globex");
        tracker.consume(3L, 10L);
        assertThat(tracker.usage().tokens()).isEqualTo(3L);
        assertThat(tracker.remaining().calls()).isEqualTo(1);
        assertThat(tracker.remaining().remainingCalls()).isEqualTo(4);
        assertThat(tracker.isExceeded()).isFalse();
        AgentguardTenant.clear();
        tracker.consume(1L, 1L);
        assertThat(tracker.usage(BudgetKey.of("default")).tokens()).isEqualTo(1L);
    }
}
