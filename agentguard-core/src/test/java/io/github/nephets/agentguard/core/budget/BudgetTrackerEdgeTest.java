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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BudgetTrackerEdgeTest {

    @Test
    void 恰好用满不超预算() {
        BudgetTracker tracker = new BudgetTracker(Budget.of(100L, 1_000L, 3));
        BudgetKey key = BudgetKey.of("acme", "run-1");
        tracker.consume(key, 50L, 500L);
        tracker.consume(key, 50L, 500L);
        assertThat(tracker.usage(key).isExceeded()).isFalse();
        assertThat(tracker.usage(key).tokens()).isEqualTo(100L);
        assertThat(tracker.usage(key).calls()).isEqualTo(2);
    }

    @Test
    void 每个维度各超一个都拒绝() {
        BudgetTracker tokens = new BudgetTracker(Budget.of(10L, Long.MAX_VALUE, Integer.MAX_VALUE));
        assertThatThrownBy(() -> tokens.consume(BudgetKey.of("t"), 11L, 0L))
                .isInstanceOf(BudgetExceededException.class);

        BudgetTracker cost = new BudgetTracker(Budget.of(Long.MAX_VALUE, 10L, Integer.MAX_VALUE));
        assertThatThrownBy(() -> cost.consume(BudgetKey.of("t"), 0L, 11L))
                .isInstanceOf(BudgetExceededException.class);

        BudgetTracker calls = new BudgetTracker(Budget.of(Long.MAX_VALUE, Long.MAX_VALUE, 2));
        calls.consume(BudgetKey.of("t"));
        calls.consume(BudgetKey.of("t"));
        assertThatThrownBy(() -> calls.consume(BudgetKey.of("t")))
                .isInstanceOf(BudgetExceededException.class);
    }

    @Test
    void 重置指定键后恢复记账() {
        BudgetTracker tracker = new BudgetTracker(Budget.of(10L, 10L, 2));
        BudgetKey key = BudgetKey.of("acme", "run-1");
        tracker.consume(key);
        tracker.consume(key);
        tracker.reset(key);
        assertThat(tracker.usage(key).calls()).isZero();
        assertThat(tracker.usage(key).isExceeded()).isFalse();
        tracker.consume(key);
        assertThat(tracker.usage(key).calls()).isEqualTo(1);
    }

    @Test
    void 全量重置清空所有键() {
        BudgetTracker tracker = new BudgetTracker(Budget.of(10L, 10L, 2));
        tracker.consume(BudgetKey.of("a"));
        tracker.consume(BudgetKey.of("b"));
        tracker.reset();
        assertThat(tracker.usage(BudgetKey.of("a")).calls()).isZero();
        assertThat(tracker.usage(BudgetKey.of("b")).calls()).isZero();
    }

    @Test
    void 不同租户与运行的记账互相隔离() {
        BudgetTracker tracker = new BudgetTracker(Budget.of(10L, 10L, 1));
        BudgetKey acme = BudgetKey.of("acme", "run-1");
        BudgetKey acme2 = BudgetKey.of("acme", "run-2");
        BudgetKey globex = BudgetKey.of("globex", "run-1");
        tracker.consume(acme);
        assertThat(tracker.usage(acme).calls()).isEqualTo(1);
        assertThat(tracker.usage(acme2).calls()).isZero();
        assertThat(tracker.usage(globex).calls()).isZero();
        assertThatThrownBy(() -> tracker.consume(acme)).isInstanceOf(BudgetExceededException.class);
        tracker.consume(acme2); // 另一个运行不受影响
        tracker.consume(globex); // 另一个租户不受影响
    }

    @Test
    void 并发扣减不超过调用上限() throws Exception {
        int maxCalls = 50;
        BudgetTracker tracker =
                new BudgetTracker(Budget.of(Long.MAX_VALUE, Long.MAX_VALUE, maxCalls));
        BudgetKey key = BudgetKey.of("acme", "run-1");
        int threads = 8;
        int perThread = 20;
        AtomicInteger granted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                futures.add(
                        pool.submit(
                                () -> {
                                    for (int i = 0; i < perThread; i++) {
                                        try {
                                            tracker.consume(key);
                                            granted.incrementAndGet();
                                        } catch (BudgetExceededException e) {
                                            rejected.incrementAndGet();
                                        }
                                    }
                                }));
            }
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdown();
        }
        assertThat(granted.get()).isEqualTo(maxCalls);
        assertThat(rejected.get()).isEqualTo(threads * perThread - maxCalls);
        assertThat(tracker.usage(key).calls()).isEqualTo(maxCalls);
    }

    @Test
    void 剩余量永不为负() {
        BudgetTracker tracker = new BudgetTracker(Budget.of(10L, 10L, 1));
        tracker.consume(BudgetKey.of("t"), 10L, 10L);
        BudgetUsage usage = tracker.usage(BudgetKey.of("t"));
        assertThat(usage.remainingTokens()).isZero();
        assertThat(usage.remainingCostMicros()).isZero();
        assertThat(usage.remainingCalls()).isZero();
        assertThat(usage.isExceeded()).isFalse();
    }

    @Test
    void 无参方法落在当前租户上下文上() {
        BudgetTracker tracker = new BudgetTracker(Budget.of(10L, 10L, 1));
        AgentguardTenant.runWith(
                "acme",
                () -> {
                    tracker.consume(1L, 1L);
                    assertThat(tracker.usage().tokens()).isEqualTo(1L);
                    assertThat(tracker.remaining().remainingTokens()).isEqualTo(9L);
                    assertThat(tracker.isExceeded()).isFalse();
                });
        // 上下文退出后回到 default 租户，看不到 acme 的记账
        assertThat(tracker.usage().tokens()).isZero();
    }

    @Test
    void 预算键把空标识归一化为默认值() {
        assertThat(BudgetKey.of(null, null)).isEqualTo(BudgetKey.of("default", "default"));
        assertThat(BudgetKey.of("  ", "  ")).isEqualTo(BudgetKey.of("default", "default"));
        assertThat(BudgetKey.of("acme")).isEqualTo(BudgetKey.of("acme", "default"));
        assertThat(BudgetKey.of("acme", "run-1").tenantId()).isEqualTo("acme");
    }

    @Test
    void 不限量的预算永不超支() {
        BudgetTracker tracker = new BudgetTracker(Budget.unlimited());
        BudgetKey key = BudgetKey.of("acme");
        tracker.consume(key, Long.MAX_VALUE - 1, Long.MAX_VALUE - 1);
        assertThat(tracker.usage(key).isExceeded()).isFalse();
    }

    @Test
    void 构造器容忍空预算按不限量处理() {
        BudgetTracker tracker = new BudgetTracker(null);
        assertThat(tracker.budget()).isEqualTo(Budget.unlimited());
        tracker.consume(BudgetKey.of("t"), 1L, 1L);
        assertThat(tracker.usage(BudgetKey.of("t")).isExceeded()).isFalse();
    }

    @Test
    void 用量快照逐字段可读() {
        BudgetUsage usage = new BudgetUsage(Budget.of(1L, 2L, 3), 1L, 2L, 3);
        assertThat(usage.tokens()).isEqualTo(1L);
        assertThat(usage.costMicros()).isEqualTo(2L);
        assertThat(usage.calls()).isEqualTo(3);
        assertThat(usage.remainingTokens()).isZero();
        assertThat(usage.budget().maxCalls()).isEqualTo(3);
        assertThat(usage.toString()).contains("tokens=1");
    }
}
