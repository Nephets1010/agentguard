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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParamValidatorEdgeTest {

    private static ParamValidator stringRule() {
        return ParamValidator.of(
                "db.query",
                ParamRule.builder("sql")
                        .kind(ParamRule.Kind.STRING)
                        .required(true)
                        .maxLength(200)
                        .build(),
                ParamRule.builder("limit").kind(ParamRule.Kind.INTEGER).min(1).max(100).build(),
                ParamRule.builder("dry").kind(ParamRule.Kind.BOOLEAN).build());
    }

    @Test
    void 规则标识固定() {
        assertThat(ParamValidator.RULE_ID).isEqualTo("tool.param");
    }

    @Test
    void 必填缺失与可选缺失分别处理() {
        ParamValidator validator =
                ParamValidator.of(
                        "t",
                        ParamRule.builder("need").required(true).build(),
                        ParamRule.builder("opt").required(false).build());
        List<Finding> findings = validator.validate("t", Map.of());
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).message()).contains("need").contains("必填");
        assertThat(findings.get(0).severity()).isEqualTo(Severity.ERROR);
    }

    @Test
    void 空参数表与空map都按缺失处理() {
        ParamValidator validator =
                ParamValidator.of("t", ParamRule.builder("need").required(true).build());
        assertThat(validator.validate("t", Map.of())).hasSize(1);
        assertThat(validator.validate("t", new HashMap<>())).hasSize(1);
    }

    @Test
    void 字符串规则拒绝非字符串值() {
        ParamValidator validator =
                ParamValidator.of("t", ParamRule.builder("s").kind(ParamRule.Kind.STRING).build());
        List<Finding> findings = validator.validate("t", Map.of("s", 42));
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).message()).contains("字符串");
    }

    @Test
    void 整数规则接受整数与长整数() {
        ParamValidator validator =
                ParamValidator.of(
                        "t",
                        ParamRule.builder("i").kind(ParamRule.Kind.INTEGER).build(),
                        ParamRule.builder("l").kind(ParamRule.Kind.LONG).build());
        assertThat(validator.validate("t", Map.of("i", 7, "l", 8L))).isEmpty();
        assertThat(validator.validate("t", Map.of("i", 7.5))).hasSize(1);
        assertThat(validator.validate("t", Map.of("l", "8"))).hasSize(1);
    }

    @Test
    void 数值范围边界值本身合法() {
        ParamValidator validator =
                ParamValidator.of(
                        "t",
                        ParamRule.builder("n")
                                .kind(ParamRule.Kind.INTEGER)
                                .min(1)
                                .max(100)
                                .build());
        assertThat(validator.validate("t", Map.of("n", 1))).isEmpty();
        assertThat(validator.validate("t", Map.of("n", 100))).isEmpty();
        assertThat(validator.validate("t", Map.of("n", 0))).hasSize(1);
        assertThat(validator.validate("t", Map.of("n", 101))).hasSize(1);
    }

    @Test
    void 浮点规则接受整数也接受浮点() {
        ParamValidator validator =
                ParamValidator.of(
                        "t",
                        ParamRule.builder("d")
                                .kind(ParamRule.Kind.DOUBLE)
                                .min(0.5)
                                .max(1.5)
                                .build());
        assertThat(validator.validate("t", Map.of("d", 1))).isEmpty();
        assertThat(validator.validate("t", Map.of("d", 1.0))).isEmpty();
        assertThat(validator.validate("t", Map.of("d", 0.4))).hasSize(1);
        assertThat(validator.validate("t", Map.of("d", "1.0"))).hasSize(1);
    }

    @Test
    void 布尔规则只接受布尔值() {
        ParamValidator validator =
                ParamValidator.of("t", ParamRule.builder("b").kind(ParamRule.Kind.BOOLEAN).build());
        assertThat(validator.validate("t", Map.of("b", true))).isEmpty();
        assertThat(validator.validate("t", Map.of("b", "true"))).hasSize(1);
        assertThat(validator.validate("t", Map.of("b", 1))).hasSize(1);
    }

    @Test
    void 字符串长度上下限边界精确() {
        ParamValidator validator =
                ParamValidator.of(
                        "t",
                        ParamRule.builder("s")
                                .kind(ParamRule.Kind.STRING)
                                .minLength(2)
                                .maxLength(4)
                                .build());
        assertThat(validator.validate("t", Map.of("s", "ab"))).isEmpty();
        assertThat(validator.validate("t", Map.of("s", "abcd"))).isEmpty();
        assertThat(validator.validate("t", Map.of("s", "a"))).hasSize(1);
        assertThat(validator.validate("t", Map.of("s", "abcde"))).hasSize(1);
    }

    @Test
    void 枚举取值命中与未命中() {
        ParamValidator validator =
                ParamValidator.of(
                        "t",
                        ParamRule.builder("m")
                                .kind(ParamRule.Kind.STRING)
                                .allowedValues(List.of("a", "b"))
                                .build());
        assertThat(validator.validate("t", Map.of("m", "a"))).isEmpty();
        assertThat(validator.validate("t", Map.of("m", "c"))).hasSize(1);
    }

    @Test
    void 正则规则命中未命中与非法规则() {
        ParamValidator validator =
                ParamValidator.of(
                        "t",
                        ParamRule.builder("ok").pattern("[0-9]{3}").build(),
                        ParamRule.builder("bad").pattern("([a-").build());
        assertThat(validator.validate("t", Map.of("ok", "x123y"))).isEmpty();
        assertThat(validator.validate("t", Map.of("ok", "abc"))).hasSize(1);
        assertThat(validator.validate("t", Map.of("bad", "anything"))).hasSize(1);
    }

    @Test
    void 多个违规累积为多条命中() {
        ParamValidator validator =
                ParamValidator.of(
                        "t",
                        ParamRule.builder("s")
                                .kind(ParamRule.Kind.STRING)
                                .minLength(5)
                                .maxLength(6)
                                .build(),
                        ParamRule.builder("n").kind(ParamRule.Kind.INTEGER).max(3).build());
        List<Finding> findings = validator.validate("t", Map.of("s", "abcdefg", "n", 9));
        assertThat(findings).hasSize(2);
        assertThat(findings)
                .allSatisfy(f -> assertThat(f.ruleId()).isEqualTo(ParamValidator.RULE_ID));
    }

    @Test
    void 未登记的额外字段不报错() {
        // 只校验登记的规则；调用方多传字段不视为违规（宽松策略，避免规则表跟不上参数演进）。
        assertThat(stringRule().validate("db.query", Map.of("sql", "select 1", "extra", "x")))
                .isEmpty();
    }

    @Test
    void 未登记规则的工具直接通过() {
        assertThat(stringRule().validate("other.tool", Map.of("sql", 1))).isEmpty();
        assertThat(stringRule().rules("other.tool")).isEmpty();
    }

    @Test
    void 登记忽略空工具名与空规则() {
        ParamValidator validator = new ParamValidator();
        validator.register("  ", ParamRule.builder("s").build());
        validator.register("t", (ParamRule[]) null);
        validator.register("t", (ParamRule) null);
        assertThat(validator.rules("t")).isEmpty();
    }

    @Test
    void 规则视图不可变() {
        ParamValidator validator = stringRule();
        assertThatThrownBy(() -> validator.rules("db.query").add(ParamRule.builder("x").build()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void 调用对象与散装参数等价且空调用安全() {
        ParamValidator validator = stringRule();
        ToolCall call =
                ToolCall.builder("db.query")
                        .argument("sql", "select 1")
                        .argument("limit", 5)
                        .build();
        assertThat(validator.validate(call)).isEmpty();
        assertThat(validator.validate("db.query", call.arguments())).isEmpty();
        assertThat(validator.validate((ToolCall) null)).isEmpty();
        assertThat(validator.validate("db.query", (Map<String, Object>) null)).isEmpty();
    }

    @Test
    void 超大参数表也能正确校验() {
        ParamValidator validator =
                ParamValidator.of(
                        "big",
                        ParamRule.builder("s").kind(ParamRule.Kind.STRING).maxLength(10).build());
        Map<String, Object> args = new HashMap<>();
        args.put("s", "x".repeat(50_000));
        List<Finding> findings = validator.validate("big", args);
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).message()).contains("超过上限");
    }

    @Test
    void 嵌套容器值按类型不符处理() {
        ParamValidator validator =
                ParamValidator.of("t", ParamRule.builder("s").kind(ParamRule.Kind.STRING).build());
        assertThat(validator.validate("t", Map.of("s", Map.of("k", "v")))).hasSize(1);
        assertThat(validator.validate("t", Map.of("s", List.of(1, 2)))).hasSize(1);
    }

    @Test
    void 链式登记返回自身() {
        ParamValidator validator = new ParamValidator();
        assertThat(validator.register("a", ParamRule.builder("x").build())).isSameAs(validator);
    }
}
