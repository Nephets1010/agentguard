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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 滑动窗口限流：每个键在窗口内最多放行 permits 次。
 *
 * <p>分桶存储有界：键只建不收会让 {@code hits} 随「租户 + 工具」这类高基数键空间单调增长， 因此按操作计数周期性清扫窗口内已无记录的空桶（见 {@link
 * #sweepEmptyBuckets}）。
 */
public final class RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    /** 每累积这么多次读写操作清扫一次空桶；摊薄到每次操作是 O(1)。 */
    private static final int SWEEP_INTERVAL = 128;

    private final int permits;
    private final long windowMillis;
    private final Clock clock;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();
    private final AtomicInteger opsSinceSweep = new AtomicInteger();

    /**
     * @param permits 窗口内允许的次数
     * @param window 窗口长度
     */
    public RateLimiter(int permits, Duration window) {
        this(permits, window, Clock.system());
    }

    /**
     * @param permits 窗口内允许的次数
     * @param window 窗口长度
     * @param clock 时钟，测试里可注入固定时钟
     */
    public RateLimiter(int permits, Duration window, Clock clock) {
        if (permits < 1) {
            throw new IllegalArgumentException("permits 至少为 1");
        }
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("窗口长度必须为正");
        }
        this.permits = permits;
        this.windowMillis = window.toMillis();
        this.clock = clock == null ? Clock.system() : clock;
    }

    /**
     * @return 窗口内允许的次数
     */
    public int permits() {
        return permits;
    }

    /**
     * @return 窗口长度，单位为毫秒
     */
    public long windowMillis() {
        return windowMillis;
    }

    /**
     * @return 窗口长度
     */
    public Duration window() {
        return Duration.ofMillis(windowMillis);
    }

    /**
     * 尝试获取一次配额。
     *
     * @param key 限流键，例如租户标识或「租户 + 工具」
     * @return 获取成功返回 true
     */
    public boolean tryAcquire(String key) {
        maybeSweep();
        String normalized = key == null ? "default" : key;
        long now = clock.millis();
        Deque<Long> bucket = hits.computeIfAbsent(normalized, k -> new ArrayDeque<>());
        synchronized (bucket) {
            evictExpired(bucket, now);
            if (bucket.size() < permits) {
                bucket.addLast(now);
                log.debug(
                        "tryAcquire key={} -> acquired (used {}/{})",
                        normalized,
                        bucket.size(),
                        permits);
                return true;
            }
            log.debug(
                    "tryAcquire key={} -> rejected (used {}/{})",
                    normalized,
                    bucket.size(),
                    permits);
            return false;
        }
    }

    /**
     * 获取一次配额，取不到时抛异常。
     *
     * @param key 限流键
     * @throws RateLimitExceededException 窗口内配额用尽时抛出
     */
    public void acquire(String key) {
        if (!tryAcquire(key)) {
            log.warn(
                    "rate limit triggered: key={} permits={} windowMillis={}",
                    key,
                    permits,
                    windowMillis);
            throw new RateLimitExceededException(
                    "限流触发: key=" + key + " permits=" + permits + " windowMillis=" + windowMillis);
        }
    }

    /**
     * @param key 限流键
     * @return 当前窗口内剩余的配额
     */
    public int availablePermits(String key) {
        maybeSweep();
        String normalized = key == null ? "default" : key;
        long now = clock.millis();
        Deque<Long> bucket = hits.get(normalized);
        if (bucket == null) {
            return permits;
        }
        synchronized (bucket) {
            evictExpired(bucket, now);
            return Math.max(0, permits - bucket.size());
        }
    }

    /**
     * 清空某个键的记录。
     *
     * @param key 限流键
     */
    public void reset(String key) {
        hits.remove(key == null ? "default" : key);
    }

    /** 清空全部记录。 */
    public void reset() {
        hits.clear();
    }

    /**
     * @return 当前持有的键数量（含窗口内记录已全部过期的桶），供测试与诊断使用
     */
    int trackedKeys() {
        return hits.size();
    }

    private void maybeSweep() {
        if (opsSinceSweep.incrementAndGet() >= SWEEP_INTERVAL) {
            opsSinceSweep.set(0);
            sweepEmptyBuckets(clock.millis());
        }
    }

    /**
     * 回收窗口内已无记录的空桶。
     *
     * <p>没有这一步，一个键只要出现过，它的桶（连同滑出窗口的陈旧时间戳）就会永久留在 map 里 —— 长跑多租户进程的 {@code hits} 单调增长，正是「存储有界」要消除的问题。
     *
     * <p>回收是行为等价的：桶内记录本就全部滑出了窗口，下次 {@code tryAcquire} 会 {@code computeIfAbsent} 重建空桶，结果与桶还在时完全一致。用
     * {@code remove(key, bucket)} 仅移除 仍映射到当前桶的条目，避免删掉并发刚建的新桶。注意这里在释放桶监视器之后才做 map 移除， 与 {@code
     * tryAcquire}「先 CHM bin 锁、后桶锁」的顺序不冲突，不会死锁。
     */
    private void sweepEmptyBuckets(long now) {
        int reclaimed = 0;
        for (Map.Entry<String, Deque<Long>> entry : hits.entrySet()) {
            Deque<Long> bucket = entry.getValue();
            synchronized (bucket) {
                evictExpired(bucket, now);
                if (!bucket.isEmpty()) {
                    continue;
                }
            }
            if (hits.remove(entry.getKey(), bucket)) {
                reclaimed++;
            }
        }
        if (reclaimed > 0) {
            log.debug("回收 {} 个空限流桶（窗口内无记录，按需重建）", reclaimed);
        }
    }

    private void evictExpired(Deque<Long> bucket, long now) {
        while (!bucket.isEmpty() && now - bucket.peekFirst() >= windowMillis) {
            bucket.pollFirst();
        }
    }
}
