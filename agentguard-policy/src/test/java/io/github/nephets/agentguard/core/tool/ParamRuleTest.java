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

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ParamRuleTest {

    @Test
    void 默认值为字符串且不限制() {
        ParamRule rule = ParamRule.builder("name").build();
        assertThat(rule.name()).isEqualTo("name");
        assertThat(rule.kind()).isEqualTo(ParamRule.Kind.STRING);
        assertThat(rule.required()).isFalse();
        assertThat(rule.minLength()).isEqualTo(-1);
        assertThat(rule.maxLength()).isEqualTo(-1);
        assertThat(rule.min()).isNull();
        assertThat(rule.max()).isNull();
        assertThat(rule.allowedValues()).isEmpty();
        assertThat(rule.pattern()).isNull();
    }

    @Test
    void 构造器赋值后可读() {
        ParamRule rule =
                ParamRule.builder("age")
                        .kind(ParamRule.Kind.INTEGER)
                        .required(true)
                        .min(1)
                        .max(120)
                        .allowedValues(Set.of("1"))
                        .pattern("\\d+")
                        .minLength(1)
                        .maxLength(3)
                        .build();
        assertThat(rule.kind()).isEqualTo(ParamRule.Kind.INTEGER);
        assertThat(rule.required()).isTrue();
        assertThat(rule.min()).isEqualTo(1.0);
        assertThat(rule.max()).isEqualTo(120.0);
        assertThat(rule.allowedValues()).containsExactly("1");
        assertThat(rule.pattern()).isEqualTo("\\d+");
        assertThat(rule.minLength()).isEqualTo(1);
        assertThat(rule.maxLength()).isEqualTo(3);
    }
}
