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
package io.github.nephets.agentguard.core.rate;

import io.github.nephets.agentguard.core.RateLimitExceededException;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimiterTest {

    /** 可在测试里推进的时钟。 */
    private static final class MutableClock implements Clock {

        private long millis;

        @Override
        public long millis() {
            return millis;
        }
    }

    @Test
    void 配额用尽后拒绝() {
        RateLimiter limiter = new RateLimiter(2, Duration.ofSeconds(60), Clock.fixed(0L));
        assertThat(limiter.tryAcquire("acme")).isTrue();
        assertThat(limiter.tryAcquire("acme")).isTrue();
        assertThat(limiter.tryAcquire("acme")).isFalse();
        assertThat(limiter.permits()).isEqualTo(2);
        assertThat(limiter.windowMillis()).isEqualTo(60_000L);
    }

    @Test
    void 窗口滑动后恢复配额() {
        MutableClock clock = new MutableClock();
        RateLimiter limiter = new RateLimiter(1, Duration.ofSeconds(60), clock);
        assertThat(limiter.tryAcquire("acme")).isTrue();
        assertThat(limiter.tryAcquire("acme")).isFalse();
        clock.millis = 60_000L;
        assertThat(limiter.tryAcquire("acme")).isTrue();
    }

    @Test
    void 剩余配额可查询() {
        MutableClock clock = new MutableClock();
        RateLimiter limiter = new RateLimiter(3, Duration.ofSeconds(60), clock);
        assertThat(limiter.availablePermits("acme")).isEqualTo(3);
        limiter.tryAcquire("acme");
        assertThat(limiter.availablePermits("acme")).isEqualTo(2);
        assertThat(limiter.availablePermits("globex")).isEqualTo(3);
    }

    @Test
    void 取不到配额时抛异常() {
        RateLimiter limiter = new RateLimiter(1, Duration.ofSeconds(60), Clock.fixed(0L));
        limiter.acquire("acme");
        assertThatThrownBy(() -> limiter.acquire("acme"))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("acme");
    }

    @Test
    void 清空记录后重新开始() {
        RateLimiter limiter = new RateLimiter(1, Duration.ofSeconds(60), Clock.fixed(0L));
        limiter.tryAcquire("acme");
        limiter.reset("acme");
        assertThat(limiter.tryAcquire("acme")).isTrue();
        limiter.reset();
        assertThat(limiter.availablePermits("acme")).isEqualTo(1);
    }

    @Test
    void 过期键的桶会被清扫回收_存储有界() {
        MutableClock clock = new MutableClock();
        RateLimiter limiter = new RateLimiter(1, Duration.ofSeconds(60), clock);
        limiter.tryAcquire("acme");
        limiter.tryAcquire("globex");
        assertThat(limiter.trackedKeys()).isEqualTo(2);

        clock.millis = 60_000L; // 两个键的记录都滑出窗口
        for (int i = 0; i < 300; i++) {
            limiter.availablePermits("probe"); // 只读路径不建桶，累计到阈值触发清扫
        }
        assertThat(limiter.trackedKeys()).isZero();

        // 清扫不影响限流语义：窗口已滑过，重新放行，且新桶只按实际使用建立
        assertThat(limiter.tryAcquire("acme")).isTrue();
        assertThat(limiter.trackedKeys()).isEqualTo(1);
    }

    @Test
    void 未过期的桶不会被清扫() {
        MutableClock clock = new MutableClock();
        RateLimiter limiter = new RateLimiter(2, Duration.ofSeconds(60), clock);
        limiter.tryAcquire("acme");
        for (int i = 0; i < 300; i++) {
            limiter.availablePermits("probe");
        }
        assertThat(limiter.trackedKeys()).isEqualTo(1);
        assertThat(limiter.availablePermits("acme")).isEqualTo(1);
    }

    @Test
    void 并发获取不超过窗口配额() throws Exception {
        RateLimiter limiter = new RateLimiter(50, Duration.ofSeconds(60), Clock.fixed(0L));
        int threads = 8;
        int perThread = 20;
        AtomicInteger granted = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                futures.add(
                        pool.submit(
                                () -> {
                                    for (int i = 0; i < perThread; i++) {
                                        if (limiter.tryAcquire("acme")) {
                                            granted.incrementAndGet();
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
        assertThat(granted.get()).isEqualTo(50);
    }

    @Test
    void 非法参数报错() {
        assertThatThrownBy(() -> new RateLimiter(0, Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RateLimiter(1, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RateLimiter(1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
