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

/** 一次运行的预算上限：token 数、成本（微元）与调用次数。 */
public final class Budget {

    private final long maxTokens;
    private final long maxCostMicros;
    private final int maxCalls;

    private Budget(Builder builder) {
        this.maxTokens = builder.maxTokens;
        this.maxCostMicros = builder.maxCostMicros;
        this.maxCalls = builder.maxCalls;
    }

    /**
     * @return 新的 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @param maxTokens token 上限
     * @param maxCostMicros 成本上限，单位为微元（百万分之一元）
     * @param maxCalls 调用次数上限
     * @return 预算
     */
    public static Budget of(long maxTokens, long maxCostMicros, int maxCalls) {
        return builder()
                .maxTokens(maxTokens)
                .maxCostMicros(maxCostMicros)
                .maxCalls(maxCalls)
                .build();
    }

    /**
     * @return 不限制任何维度的预算
     */
    public static Budget unlimited() {
        return of(Long.MAX_VALUE, Long.MAX_VALUE, Integer.MAX_VALUE);
    }

    /**
     * @return token 上限
     */
    public long maxTokens() {
        return maxTokens;
    }

    /**
     * @return 成本上限，单位为微元
     */
    public long maxCostMicros() {
        return maxCostMicros;
    }

    /**
     * @return 调用次数上限
     */
    public int maxCalls() {
        return maxCalls;
    }

    @Override
    public String toString() {
        return "Budget{maxTokens="
                + maxTokens
                + ", maxCostMicros="
                + maxCostMicros
                + ", maxCalls="
                + maxCalls
                + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Budget other)) {
            return false;
        }
        return maxTokens == other.maxTokens
                && maxCostMicros == other.maxCostMicros
                && maxCalls == other.maxCalls;
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(maxTokens);
        result = 31 * result + Long.hashCode(maxCostMicros);
        result = 31 * result + maxCalls;
        return result;
    }

    /** Budget 的构造器。 */
    public static final class Builder {

        private long maxTokens = Long.MAX_VALUE;
        private long maxCostMicros = Long.MAX_VALUE;
        private int maxCalls = Integer.MAX_VALUE;

        /**
         * @param maxTokens token 上限，小于 1 时视为 0
         */
        public Builder maxTokens(long maxTokens) {
            this.maxTokens = Math.max(0L, maxTokens);
            return this;
        }

        /**
         * @param maxCostMicros 成本上限，小于 1 时视为 0
         */
        public Builder maxCostMicros(long maxCostMicros) {
            this.maxCostMicros = Math.max(0L, maxCostMicros);
            return this;
        }

        /**
         * @param maxCalls 调用次数上限，小于 1 时视为 0
         */
        public Builder maxCalls(int maxCalls) {
            this.maxCalls = Math.max(0, maxCalls);
            return this;
        }

        /**
         * @return 构造好的预算
         */
        public Budget build() {
            return new Budget(this);
        }
    }
}
