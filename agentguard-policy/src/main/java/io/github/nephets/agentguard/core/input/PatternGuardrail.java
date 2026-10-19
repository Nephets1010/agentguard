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
package io.github.nephets.agentguard.core.input;

import io.github.nephets.agentguard.core.Finding;
import io.github.nephets.agentguard.core.Guardrail;
import io.github.nephets.agentguard.core.GuardrailContext;
import io.github.nephets.agentguard.core.GuardrailResult;
import io.github.nephets.agentguard.core.TextSearch;

import java.util.ArrayList;
import java.util.List;

/**
 * 短语匹配规则的公共实现：在文本中忽略大小写查找一组短语，命中即生成 {@link Finding}。
 *
 * <p>子类只需提供规则标识、说明文字与短语表。
 */
public abstract class PatternGuardrail implements Guardrail {

    private final String ruleId;
    private final String description;
    private final List<Phrase> phrases;

    /**
     * @param ruleId 规则标识
     * @param description 说明文字，会出现在命中记录里
     * @param phrases 短语表
     */
    protected PatternGuardrail(String ruleId, String description, List<Phrase> phrases) {
        if (ruleId == null || ruleId.isBlank()) {
            throw new IllegalArgumentException("ruleId 不能为空");
        }
        this.ruleId = ruleId;
        this.description = description == null ? "" : description;
        this.phrases = phrases == null ? List.of() : List.copyOf(phrases);
    }

    /**
     * @return 规则标识
     */
    public final String ruleId() {
        return ruleId;
    }

    /**
     * @return 短语表，不可变列表
     */
    public final List<Phrase> phrases() {
        return phrases;
    }

    @Override
    public final GuardrailResult check(GuardrailContext ctx) {
        if (ctx == null) {
            return GuardrailResult.allow();
        }
        String text = ctx.text() == null ? "" : ctx.text();
        List<Finding> raw = new ArrayList<>();
        for (Phrase phrase : phrases) {
            int start = TextSearch.indexOfIgnoreCase(text, phrase.text());
            if (start >= 0) {
                int end = start + phrase.text().length();
                raw.add(
                        Finding.of(
                                ruleId,
                                phrase.severity(),
                                description + "：" + phrase.text(),
                                TextSearch.snippet(text, start, end),
                                start,
                                end));
            }
        }
        // 去重：若两条命中区间重叠，只保留更具体（文本更长）的一条，
        // 避免「你现在是」与「你现在是一名」这种包含关系被同时报告。
        List<Finding> findings = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            Finding a = raw.get(i);
            int lenA = a.end() - a.start();
            boolean redundant = false;
            for (int j = 0; j < raw.size(); j++) {
                if (i == j) {
                    continue;
                }
                Finding b = raw.get(j);
                boolean overlap = a.start() < b.end() && b.start() < a.end();
                if (overlap) {
                    int lenB = b.end() - b.start();
                    if (lenA < lenB || (lenA == lenB && i > j)) {
                        redundant = true;
                        break;
                    }
                }
            }
            if (!redundant) {
                findings.add(a);
            }
        }
        return GuardrailResult.fromFindings(findings);
    }
}
