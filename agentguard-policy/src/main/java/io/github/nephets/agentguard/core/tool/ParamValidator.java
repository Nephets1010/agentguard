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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** 按工具维度登记参数规则并做校验，返回命中记录而不抛异常。 */
public final class ParamValidator {

    /** 规则标识。 */
    public static final String RULE_ID = "tool.param";

    private final Map<String, List<ParamRule>> rulesByTool = new LinkedHashMap<>();

    /**
     * @param toolName 工具名
     * @param rules 该工具的参数规则
     * @return 校验器实例
     */
    public static ParamValidator of(String toolName, ParamRule... rules) {
        ParamValidator validator = new ParamValidator();
        validator.register(toolName, rules);
        return validator;
    }

    /**
     * 登记一个工具的参数规则。
     *
     * @param toolName 工具名
     * @param rules 参数规则
     * @return 当前校验器，便于链式调用
     */
    public ParamValidator register(String toolName, ParamRule... rules) {
        if (toolName == null || toolName.isBlank() || rules == null) {
            return this;
        }
        List<ParamRule> bucket = rulesByTool.computeIfAbsent(toolName, key -> new ArrayList<>());
        for (ParamRule rule : rules) {
            if (rule != null) {
                bucket.add(rule);
            }
        }
        return this;
    }

    /**
     * @param toolName 工具名
     * @return 该工具登记的规则，未登记时返回空列表
     */
    public List<ParamRule> rules(String toolName) {
        return Collections.unmodifiableList(rulesByTool.getOrDefault(toolName, List.of()));
    }

    /**
     * 校验一次工具调用的参数。
     *
     * @param call 工具调用请求
     * @return 命中记录，全部通过时为空列表
     */
    public List<Finding> validate(ToolCall call) {
        if (call == null) {
            return List.of();
        }
        return validate(call.toolName(), call.arguments());
    }

    /**
     * 校验一组参数。
     *
     * @param toolName 工具名
     * @param arguments 参数
     * @return 命中记录，全部通过时为空列表
     */
    public List<Finding> validate(String toolName, Map<String, Object> arguments) {
        List<ParamRule> rules = rules(toolName);
        if (rules.isEmpty()) {
            return List.of();
        }
        // 参数为 null 视为「调用负载缺失，无从校验」，与 validate(null ToolCall) 的空调用安全语义一致；
        // 注意：空 Map 仍会被正常校验（必填项会报缺失），二者合同不同。
        if (arguments == null) {
            return List.of();
        }
        Map<String, Object> args = arguments;
        List<Finding> findings = new ArrayList<>();
        for (ParamRule rule : rules) {
            Object value = args.get(rule.name());
            if (value == null) {
                if (rule.required()) {
                    findings.add(error(rule, "缺少必填参数"));
                }
                continue;
            }
            switch (rule.kind()) {
                case STRING -> checkString(rule, value, findings);
                case INTEGER, LONG -> checkInteger(rule, value, findings);
                case DOUBLE -> checkDouble(rule, value, findings);
                case BOOLEAN -> checkBoolean(rule, value, findings);
                default -> findings.add(error(rule, "不支持的参数类型"));
            }
        }
        return List.copyOf(findings);
    }

    private void checkString(ParamRule rule, Object value, List<Finding> findings) {
        if (!(value instanceof String text)) {
            findings.add(error(rule, "参数类型应为字符串，实际为 " + value.getClass().getSimpleName()));
            return;
        }
        if (rule.minLength() >= 0 && text.length() < rule.minLength()) {
            findings.add(error(rule, "长度 " + text.length() + " 小于下限 " + rule.minLength()));
        }
        if (rule.maxLength() >= 0 && text.length() > rule.maxLength()) {
            findings.add(error(rule, "长度 " + text.length() + " 超过上限 " + rule.maxLength()));
        }
        if (!rule.allowedValues().isEmpty() && !rule.allowedValues().contains(text)) {
            findings.add(error(rule, "取值不在允许范围内"));
        }
        if (rule.pattern() != null && !rule.pattern().isEmpty()) {
            try {
                if (!Pattern.compile(rule.pattern()).matcher(text).find()) {
                    findings.add(error(rule, "不匹配要求的格式"));
                }
            } catch (PatternSyntaxException e) {
                findings.add(error(rule, "规则的正则表达式无法编译"));
            }
        }
    }

    private void checkInteger(ParamRule rule, Object value, List<Finding> findings) {
        if (!(value instanceof Integer) && !(value instanceof Long)) {
            findings.add(error(rule, "参数类型应为整数，实际为 " + value.getClass().getSimpleName()));
            return;
        }
        checkRange(rule, ((Number) value).doubleValue(), findings);
    }

    private void checkDouble(ParamRule rule, Object value, List<Finding> findings) {
        if (!(value instanceof Number)) {
            findings.add(error(rule, "参数类型应为数值，实际为 " + value.getClass().getSimpleName()));
            return;
        }
        checkRange(rule, ((Number) value).doubleValue(), findings);
    }

    private void checkBoolean(ParamRule rule, Object value, List<Finding> findings) {
        if (!(value instanceof Boolean)) {
            findings.add(error(rule, "参数类型应为布尔值，实际为 " + value.getClass().getSimpleName()));
        }
    }

    private void checkRange(ParamRule rule, double actual, List<Finding> findings) {
        if (rule.min() != null && actual < rule.min()) {
            findings.add(error(rule, "取值 " + actual + " 小于下限 " + rule.min()));
        }
        if (rule.max() != null && actual > rule.max()) {
            findings.add(error(rule, "取值 " + actual + " 超过上限 " + rule.max()));
        }
    }

    private static Finding error(ParamRule rule, String detail) {
        return Finding.of(RULE_ID, Severity.ERROR, "参数 " + rule.name() + "：" + detail);
    }
}
