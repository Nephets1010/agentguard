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

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ClockTest {

    @Test
    void 固定时钟恒定返回注入值() {
        Clock clock = Clock.fixed(1_725_000_000_000L);
        assertThat(clock.millis()).isEqualTo(1_725_000_000_000L);
        assertThat(clock.millis()).isEqualTo(1_725_000_000_000L);
    }

    @Test
    void 系统时钟返回毫秒级时间戳() {
        long before = System.currentTimeMillis();
        long now = Clock.system().millis();
        long after = System.currentTimeMillis();
        assertThat(now).isBetween(before, after);
    }

    @Test
    void 注入固定时钟后限流窗口完全可预期() {
        // Clock 注入是限流可测性的前提：固定时钟下窗口永不滑动。
        RateLimiter limiter = new RateLimiter(1, Duration.ofSeconds(60), Clock.fixed(0L));
        assertThat(limiter.tryAcquire("acme")).isTrue();
        assertThat(limiter.tryAcquire("acme")).isFalse();
        assertThat(limiter.tryAcquire("acme")).isFalse();
    }
}
