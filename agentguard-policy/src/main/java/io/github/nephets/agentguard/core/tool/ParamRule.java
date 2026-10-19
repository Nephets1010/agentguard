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
package io.github.nephets.agentguard.core.tool;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** 单个参数的校验规则：类型、是否必填、长度、数值范围、枚举取值与正则表达式。 */
public final class ParamRule {

    /** 参数类型。 */
    public enum Kind {
        /** 字符串。 */
        STRING,
        /** 整数。 */
        INTEGER,
        /** 长整数。 */
        LONG,
        /** 浮点数。 */
        DOUBLE,
        /** 布尔值。 */
        BOOLEAN
    }

    private final String name;
    private final Kind kind;
    private final boolean required;
    private final int minLength;
    private final int maxLength;
    private final Double min;
    private final Double max;
    private final Set<String> allowedValues;
    private final String pattern;

    private ParamRule(Builder builder) {
        this.name = builder.name;
        this.kind = builder.kind;
        this.required = builder.required;
        this.minLength = builder.minLength;
        this.maxLength = builder.maxLength;
        this.min = builder.min;
        this.max = builder.max;
        this.allowedValues =
                Collections.unmodifiableSet(new LinkedHashSet<>(builder.allowedValues));
        this.pattern = builder.pattern;
    }

    /**
     * @param name 参数名
     * @return 新的 Builder
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    /**
     * @return 参数名
     */
    public String name() {
        return name;
    }

    /**
     * @return 参数类型
     */
    public Kind kind() {
        return kind;
    }

    /**
     * @return 是否必填
     */
    public boolean required() {
        return required;
    }

    /**
     * @return 字符串最小长度，小于 0 表示不限制
     */
    public int minLength() {
        return minLength;
    }

    /**
     * @return 字符串最大长度，小于 0 表示不限制
     */
    public int maxLength() {
        return maxLength;
    }

    /**
     * @return 数值下限，为 null 表示不限制
     */
    public Double min() {
        return min;
    }

    /**
     * @return 数值上限，为 null 表示不限制
     */
    public Double max() {
        return max;
    }

    /**
     * @return 允许的取值，空集合表示不限制
     */
    public Set<String> allowedValues() {
        return allowedValues;
    }

    /**
     * @return 字符串需要匹配的正则，为 null 表示不限制
     */
    public String pattern() {
        return pattern;
    }

    /** ParamRule 的构造器。 */
    public static final class Builder {

        private final String name;
        private Kind kind = Kind.STRING;
        private boolean required = false;
        private int minLength = -1;
        private int maxLength = -1;
        private Double min = null;
        private Double max = null;
        private Set<String> allowedValues = new LinkedHashSet<>();
        private String pattern = null;

        /**
         * @param name 参数名，不能为空
         */
        public Builder(String name) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("参数名不能为空");
            }
            this.name = name;
        }

        /**
         * @param kind 参数类型
         */
        public Builder kind(Kind kind) {
            this.kind = kind == null ? Kind.STRING : kind;
            return this;
        }

        /**
         * @param required 是否必填
         */
        public Builder required(boolean required) {
            this.required = required;
            return this;
        }

        /**
         * @param length 字符串最小长度
         */
        public Builder minLength(int length) {
            this.minLength = length;
            return this;
        }

        /**
         * @param length 字符串最大长度
         */
        public Builder maxLength(int length) {
            this.maxLength = length;
            return this;
        }

        /**
         * @param min 数值下限
         */
        public Builder min(double min) {
            this.min = min;
            return this;
        }

        /**
         * @param max 数值上限
         */
        public Builder max(double max) {
            this.max = max;
            return this;
        }

        /**
         * @param values 允许的取值
         */
        public Builder allowedValues(Collection<String> values) {
            this.allowedValues.clear();
            if (values != null) {
                this.allowedValues.addAll(values);
            }
            return this;
        }

        /**
         * @param pattern 字符串需要匹配的正则
         */
        public Builder pattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        /**
         * @return 构造好的校验规则
         */
        public ParamRule build() {
            return new ParamRule(this);
        }
    }
}
