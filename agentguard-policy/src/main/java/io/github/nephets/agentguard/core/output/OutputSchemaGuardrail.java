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
package io.github.nephets.agentguard.core.output;

import io.github.nephets.agentguard.core.Finding;
import io.github.nephets.agentguard.core.Guardrail;
import io.github.nephets.agentguard.core.GuardrailContext;
import io.github.nephets.agentguard.core.GuardrailResult;
import io.github.nephets.agentguard.core.Severity;
import io.github.nephets.agentguard.core.TextSearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** 输出阶段规则：长度上限、必填字段、禁止出现的内容（密钥格式、私钥头）。 */
public final class OutputSchemaGuardrail implements Guardrail {

    /** 长度超限的规则标识。 */
    public static final String RULE_LENGTH = "output.schema.length";

    /** 缺少必填字段的规则标识。 */
    public static final String RULE_REQUIRED = "output.schema.required";

    /** 命中禁止内容的规则标识。 */
    public static final String RULE_FORBIDDEN = "output.schema.forbidden";

    /** 默认禁止出现的正则：OpenAI 风格的密钥与 PEM 私钥头。 */
    public static final List<String> DEFAULT_FORBIDDEN_PATTERNS =
            List.of("sk-[A-Za-z0-9]{16,}", "-----BEGIN [A-Z ]*PRIVATE KEY-----");

    private final int maxLength;
    private final Set<String> requiredFields;
    private final List<Pattern> forbidden;

    private OutputSchemaGuardrail(Builder builder) {
        this.maxLength = builder.maxLength;
        this.requiredFields = new LinkedHashSet<>(builder.requiredFields);
        List<Pattern> compiled = new ArrayList<>();
        for (String regex : builder.forbiddenPatterns) {
            try {
                compiled.add(Pattern.compile(regex));
            } catch (PatternSyntaxException e) {
                compiled.add(Pattern.compile(Pattern.quote(regex)));
            }
        }
        this.forbidden = List.copyOf(compiled);
    }

    /**
     * @return 新的 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return 长度上限，小于等于 0 表示不限制
     */
    public int maxLength() {
        return maxLength;
    }

    /**
     * @return 必填字段名
     */
    public Set<String> requiredFields() {
        return Set.copyOf(requiredFields);
    }

    /**
     * @return 禁止内容的正则原文
     */
    public List<String> forbiddenPatterns() {
        List<String> patterns = new ArrayList<>();
        for (Pattern pattern : forbidden) {
            patterns.add(pattern.pattern());
        }
        return List.copyOf(patterns);
    }

    @Override
    public GuardrailResult check(GuardrailContext ctx) {
        if (ctx == null) {
            return GuardrailResult.allow();
        }
        String text = ctx.text() == null ? "" : ctx.text();
        List<Finding> findings = new ArrayList<>();
        if (maxLength > 0 && text.length() > maxLength) {
            findings.add(
                    Finding.of(
                            RULE_LENGTH,
                            Severity.ERROR,
                            "输出长度 " + text.length() + " 超过上限 " + maxLength));
        }
        for (String field : requiredFields) {
            if (ctx.attributes().get(field) == null) {
                findings.add(Finding.of(RULE_REQUIRED, Severity.ERROR, "缺少必填字段：" + field));
            }
        }
        for (Pattern pattern : forbidden) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                findings.add(
                        Finding.of(
                                RULE_FORBIDDEN,
                                Severity.ERROR,
                                "输出含禁止内容：" + pattern.pattern(),
                                TextSearch.snippet(text, matcher.start(), matcher.end()),
                                matcher.start(),
                                matcher.end()));
            }
        }
        return GuardrailResult.fromFindings(findings);
    }

    /** OutputSchemaGuardrail 的构造器。 */
    public static final class Builder {

        private int maxLength = 0;
        private Set<String> requiredFields = new LinkedHashSet<>();
        private List<String> forbiddenPatterns = new ArrayList<>(DEFAULT_FORBIDDEN_PATTERNS);

        /**
         * @param maxLength 长度上限，小于等于 0 表示不限制
         */
        public Builder maxLength(int maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        /**
         * @param fields 必填字段名
         */
        public Builder requiredFields(Collection<String> fields) {
            this.requiredFields.clear();
            if (fields != null) {
                this.requiredFields.addAll(fields);
            }
            return this;
        }

        /**
         * @param field 追加一个必填字段名
         */
        public Builder requiredField(String field) {
            if (field != null && !field.isBlank()) {
                this.requiredFields.add(field);
            }
            return this;
        }

        /**
         * @param patterns 禁止内容的正则
         */
        public Builder forbiddenPatterns(Collection<String> patterns) {
            this.forbiddenPatterns.clear();
            if (patterns != null) {
                this.forbiddenPatterns.addAll(patterns);
            }
            return this;
        }

        /**
         * @return 构造好的规则
         */
        public OutputSchemaGuardrail build() {
            return new OutputSchemaGuardrail(this);
        }
    }
}
