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

/** 某个预算键上的已用量与剩余量。 */
public final class BudgetUsage {

    private final Budget budget;
    private final long tokens;
    private final long costMicros;
    private final int calls;

    /**
     * @param budget 预算上限
     * @param tokens 已用 token
     * @param costMicros 已用成本，单位为微元
     * @param calls 已用调用次数
     */
    public BudgetUsage(Budget budget, long tokens, long costMicros, int calls) {
        this.budget = budget;
        this.tokens = Math.max(0L, tokens);
        this.costMicros = Math.max(0L, costMicros);
        this.calls = Math.max(0, calls);
    }

    /**
     * @return 预算上限
     */
    public Budget budget() {
        return budget;
    }

    /**
     * @return 已用 token
     */
    public long tokens() {
        return tokens;
    }

    /**
     * @return 已用成本，单位为微元
     */
    public long costMicros() {
        return costMicros;
    }

    /**
     * @return 已用调用次数
     */
    public int calls() {
        return calls;
    }

    /**
     * @return 剩余 token，不会为负
     */
    public long remainingTokens() {
        return Math.max(0L, budget.maxTokens() - tokens);
    }

    /**
     * @return 剩余成本，单位为微元
     */
    public long remainingCostMicros() {
        return Math.max(0L, budget.maxCostMicros() - costMicros);
    }

    /**
     * @return 剩余调用次数
     */
    public int remainingCalls() {
        return Math.max(0, budget.maxCalls() - calls);
    }

    /**
     * @return 任一维度已超出预算时为 true
     */
    public boolean isExceeded() {
        return tokens > budget.maxTokens()
                || costMicros > budget.maxCostMicros()
                || calls > budget.maxCalls();
    }

    @Override
    public String toString() {
        return "BudgetUsage{tokens="
                + tokens
                + ", costMicros="
                + costMicros
                + ", calls="
                + calls
                + ", exceeded="
                + isExceeded()
                + "}";
    }
}
