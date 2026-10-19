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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 预算台账：按「租户 + 运行」隔离记账，超出任一度量即抛 {@link BudgetExceededException}。
 *
 * <p>不带参数的方法取当前 {@link AgentguardTenant} 的值，运行标识用 default。
 */
public final class BudgetTracker {

    private static final Logger log = LoggerFactory.getLogger(BudgetTracker.class);

    private final Budget budget;
    private final Map<BudgetKey, long[]> usages = new HashMap<>();

    /** 被拒过的键：一旦某次记账因超限被拒，该键即被置位为「已超支」，isExceeded 随之为真。 */
    private final Map<BudgetKey, Boolean> exceeded = new HashMap<>();

    /**
     * @param budget 预算上限
     */
    public BudgetTracker(Budget budget) {
        this.budget = budget == null ? Budget.unlimited() : budget;
    }

    /**
     * @return 预算上限
     */
    public Budget budget() {
        return budget;
    }

    /**
     * @param key 预算键
     * @return 该键上的用量
     */
    public synchronized BudgetUsage usage(BudgetKey key) {
        long[] counters = usages.get(key);
        if (counters == null) {
            return new BudgetUsage(budget, 0L, 0L, 0);
        }
        return new BudgetUsage(budget, counters[0], counters[1], (int) counters[2]);
    }

    /**
     * @param key 预算键
     * @return 该键上的用量，与 {@link #usage(BudgetKey)} 相同
     */
    public BudgetUsage remaining(BudgetKey key) {
        return usage(key);
    }

    /**
     * @param key 预算键
     * @return 是否已超出预算
     */
    public boolean isExceeded(BudgetKey key) {
        synchronized (this) {
            if (exceeded.getOrDefault(key, Boolean.FALSE)) {
                return true;
            }
            long[] counters = usages.get(key);
            if (counters == null) {
                return false;
            }
            return new BudgetUsage(budget, counters[0], counters[1], (int) counters[2])
                    .isExceeded();
        }
    }

    /**
     * @return 当前租户是否已超出预算
     */
    public boolean isExceeded() {
        return isExceeded(currentKey());
    }

    /**
     * 记一次调用消耗，超预算时抛异常。
     *
     * @param key 预算键
     * @param tokens 本次消耗的 token
     * @param costMicros 本次消耗的成本，单位为微元
     * @throws IllegalArgumentException tokens 或 costMicros 为负时抛出
     * @throws BudgetExceededException 记账后超出预算时抛出
     */
    public void consume(BudgetKey key, long tokens, long costMicros) {
        if (tokens < 0 || costMicros < 0) {
            throw new IllegalArgumentException("消耗量不能为负");
        }
        synchronized (this) {
            long[] counters = usages.computeIfAbsent(key, k -> new long[3]);
            // 先算「本次记账后」的快照再决定落账：判定与落账必须在同一把锁内，
            // 否则并发下会放出超过上限的请求。超限则只置位「已超支」标记并抛异常，
            // 不落账——被拒请求不占预算、调用计数恰好停在上限，同时 isExceeded 因标记仍为真。
            BudgetUsage snapshot =
                    new BudgetUsage(
                            budget,
                            counters[0] + tokens,
                            counters[1] + costMicros,
                            (int) counters[2] + 1);
            if (snapshot.isExceeded()) {
                exceeded.put(key, Boolean.TRUE);
                log.warn(
                        "budget exceeded: tenant={} run={} tokens={} costMicros={} calls={}",
                        key.tenantId(),
                        key.runId(),
                        snapshot.tokens(),
                        snapshot.costMicros(),
                        snapshot.calls());
                throw new BudgetExceededException(
                        "预算已用尽: "
                                + key.tenantId()
                                + "/"
                                + key.runId()
                                + " tokens="
                                + snapshot.tokens()
                                + " costMicros="
                                + snapshot.costMicros()
                                + " calls="
                                + snapshot.calls());
            }
            counters[0] += tokens;
            counters[1] += costMicros;
            counters[2] += 1;
            log.debug(
                    "consume tenant={} run={} tokens={} costMicros={} calls={}",
                    key.tenantId(),
                    key.runId(),
                    snapshot.tokens(),
                    snapshot.costMicros(),
                    snapshot.calls());
        }
    }

    /**
     * 记一次调用消耗，只计次数。
     *
     * @param key 预算键
     */
    public void consume(BudgetKey key) {
        consume(key, 0L, 0L);
    }

    /**
     * @return 当前租户在默认运行标识上的用量
     */
    public BudgetUsage usage() {
        return usage(currentKey());
    }

    /**
     * @return 当前租户在默认运行标识上的用量
     */
    public BudgetUsage remaining() {
        return usage();
    }

    /**
     * 在当前租户上记一次消耗。
     *
     * @param tokens 本次消耗的 token
     * @param costMicros 本次消耗的成本，单位为微元
     */
    public void consume(long tokens, long costMicros) {
        consume(currentKey(), tokens, costMicros);
    }

    /**
     * 清空指定键的记账。
     *
     * @param key 预算键
     */
    public synchronized void reset(BudgetKey key) {
        usages.remove(key);
        exceeded.remove(key);
    }

    /** 清空全部记账。 */
    public synchronized void reset() {
        usages.clear();
        exceeded.clear();
    }

    private static BudgetKey currentKey() {
        return BudgetKey.of(AgentguardTenant.currentOrElse(BudgetKey.DEFAULT_TENANT_ID));
    }
}
