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

class RateLimiterEdgeTest {

    /** 可在测试里推进的时钟。 */
    private static final class MutableClock implements Clock {

        private long millis;

        @Override
        public long millis() {
            return millis;
        }
    }

    @Test
    void 刚好放行一个配额时边界精确() {
        RateLimiter limiter = new RateLimiter(1, Duration.ofSeconds(60), Clock.fixed(0L));
        assertThat(limiter.tryAcquire("acme")).isTrue();
        assertThat(limiter.tryAcquire("acme")).isFalse();
        assertThat(limiter.availablePermits("acme")).isZero();
    }

    @Test
    void 多个配额恰好用尽后拒绝() {
        RateLimiter limiter = new RateLimiter(5, Duration.ofSeconds(60), Clock.fixed(0L));
        for (int i = 0; i < 5; i++) {
            assertThat(limiter.tryAcquire("acme")).isTrue();
        }
        assertThat(limiter.tryAcquire("acme")).isFalse();
        assertThat(limiter.availablePermits("acme")).isZero();
    }

    @Test
    void 窗口边界上差一毫秒不过期到达即过期() {
        MutableClock clock = new MutableClock();
        RateLimiter limiter = new RateLimiter(1, Duration.ofSeconds(60), clock);
        assertThat(limiter.tryAcquire("acme")).isTrue();
        clock.millis = 59_999L;
        assertThat(limiter.tryAcquire("acme")).isFalse();
        clock.millis = 60_000L;
        assertThat(limiter.tryAcquire("acme")).isTrue();
    }

    @Test
    void 多个键的配额互不影响() {
        RateLimiter limiter = new RateLimiter(1, Duration.ofSeconds(60), Clock.fixed(0L));
        assertThat(limiter.tryAcquire("acme")).isTrue();
        assertThat(limiter.tryAcquire("acme")).isFalse();
        assertThat(limiter.tryAcquire("globex")).isTrue();
        assertThat(limiter.availablePermits("globex")).isZero();
        assertThat(limiter.availablePermits("acme")).isZero();
    }

    @Test
    void 空键归一化为默认键() {
        RateLimiter limiter = new RateLimiter(1, Duration.ofSeconds(60), Clock.fixed(0L));
        assertThat(limiter.tryAcquire(null)).isTrue();
        assertThat(limiter.tryAcquire("default")).isFalse();
        assertThat(limiter.availablePermits(null)).isZero();
        limiter.reset(null);
        assertThat(limiter.tryAcquire("default")).isTrue();
    }

    @Test
    void 未知键的剩余配额为满额() {
        RateLimiter limiter = new RateLimiter(3, Duration.ofSeconds(60), Clock.fixed(0L));
        assertThat(limiter.availablePermits("never-seen")).isEqualTo(3);
    }

    @Test
    void 窗口与配额访问器一致() {
        RateLimiter limiter = new RateLimiter(7, Duration.ofMinutes(2));
        assertThat(limiter.permits()).isEqualTo(7);
        assertThat(limiter.windowMillis()).isEqualTo(120_000L);
        assertThat(limiter.window()).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void 拒绝异常带上键与窗口信息() {
        RateLimiter limiter = new RateLimiter(1, Duration.ofSeconds(30), Clock.fixed(0L));
        limiter.acquire("acme");
        assertThatThrownBy(() -> limiter.acquire("acme"))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("acme")
                .hasMessageContaining("permits=1")
                .hasMessageContaining("windowMillis=30000");
    }

    @Test
    void 单键重置不影响其他键() {
        RateLimiter limiter = new RateLimiter(1, Duration.ofSeconds(60), Clock.fixed(0L));
        limiter.tryAcquire("acme");
        limiter.tryAcquire("globex");
        limiter.reset("acme");
        assertThat(limiter.tryAcquire("acme")).isTrue();
        assertThat(limiter.tryAcquire("globex")).isFalse();
    }

    @Test
    void 并发多键各自不超过配额() throws Exception {
        RateLimiter limiter = new RateLimiter(10, Duration.ofSeconds(60), Clock.fixed(0L));
        int keys = 4;
        int threads = 8;
        AtomicInteger[] granted = new AtomicInteger[keys];
        for (int k = 0; k < keys; k++) {
            granted[k] = new AtomicInteger();
        }
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                final int key = t % keys;
                futures.add(
                        pool.submit(
                                () -> {
                                    for (int i = 0; i < 5; i++) {
                                        if (limiter.tryAcquire("k" + key)) {
                                            granted[key].incrementAndGet();
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
        for (int k = 0; k < keys; k++) {
            assertThat(granted[k].get()).isEqualTo(10);
        }
    }
}
