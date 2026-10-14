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

import io.github.nephets.agentguard.core.Finding;
import io.github.nephets.agentguard.core.Severity;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ParamValidatorTest {

    private static ParamValidator validator() {
        return ParamValidator.of(
                "db.query",
                ParamRule.builder("sql")
                        .kind(ParamRule.Kind.STRING)
                        .required(true)
                        .maxLength(20)
                        .build(),
                ParamRule.builder("limit")
                        .kind(ParamRule.Kind.INTEGER)
                        .required(true)
                        .min(1)
                        .max(100)
                        .build(),
                ParamRule.builder("format")
                        .kind(ParamRule.Kind.STRING)
                        .allowedValues(Set.of("json", "csv"))
                        .build(),
                ParamRule.builder("debug").kind(ParamRule.Kind.BOOLEAN).build(),
                ParamRule.builder("ratio").kind(ParamRule.Kind.DOUBLE).min(0).max(1).build(),
                ParamRule.builder("offset").kind(ParamRule.Kind.LONG).min(0).build(),
                ParamRule.builder("code")
                        .kind(ParamRule.Kind.STRING)
                        .pattern("^[A-Z]{3}$")
                        .minLength(3)
                        .build());
    }

    private static List<Finding> validate(Map<String, Object> args) {
        return validator().validate("db.query", args);
    }

    @Test
    void 参数齐全且合法时没有命中() {
        assertThat(
                        validate(
                                Map.of(
                                        "sql",
                                        "select 1",
                                        "limit",
                                        10,
                                        "format",
                                        "json",
                                        "debug",
                                        true,
                                        "ratio",
                                        0.5,
                                        "offset",
                                        0L,
                                        "code",
                                        "ABC")))
                .isEmpty();
    }

    @Test
    void 缺少必填参数被记录() {
        List<Finding> findings = validate(Map.of("limit", 10));
        assertThat(findings).anySatisfy(finding -> assertThat(finding.message()).contains("sql"));
    }

    @Test
    void 类型不符被记录() {
        List<Finding> findings = validate(Map.of("sql", "select 1", "limit", "ten"));
        assertThat(findings)
                .anySatisfy(
                        finding -> {
                            assertThat(finding.ruleId()).isEqualTo(ParamValidator.RULE_ID);
                            assertThat(finding.severity()).isEqualTo(Severity.ERROR);
                            assertThat(finding.message()).contains("整数");
                        });
    }

    @Test
    void 数值越界被记录() {
        assertThat(validate(Map.of("sql", "select 1", "limit", 500)))
                .anySatisfy(finding -> assertThat(finding.message()).contains("超过上限"));
        assertThat(validate(Map.of("sql", "select 1", "limit", 0)))
                .anySatisfy(finding -> assertThat(finding.message()).contains("小于下限"));
        assertThat(validate(Map.of("sql", "select 1", "limit", 10, "ratio", 2.0)))
                .anySatisfy(finding -> assertThat(finding.message()).contains("ratio"));
    }

    @Test
    void 正则不匹配被记录() {
        assertThat(validate(Map.of("sql", "select 1", "limit", 1, "code", "abc")))
                .anySatisfy(finding -> assertThat(finding.message()).contains("格式"));
    }

    @Test
    void 未登记规则的工具直接通过() {
        assertThat(validator().validate("other.tool", Map.of("a", 1))).isEmpty();
        assertThat(validator().rules("other.tool")).isEmpty();
        assertThat(validator().validate((ToolCall) null)).isEmpty();
    }
}
