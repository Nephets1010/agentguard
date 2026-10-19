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
import io.github.nephets.agentguard.core.Severity;
import io.github.nephets.agentguard.core.TextSearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** 按可配置的敏感词 / 合规词表做匹配，命中即给出对应级别的记录。 */
public final class SensitiveTopicGuardrail implements Guardrail {

    /** 规则标识。 */
    public static final String RULE_ID = "input.sensitive-topic";

    /** 默认词表。 */
    public static final List<String> DEFAULT_TOPICS =
            List.of("内幕消息", "征信报告", "病历", "未公开财报", "客户名单", "薪酬数据");

    private final List<String> topics;
    private final Severity severity;

    /** 使用默认词表与 WARN 级别构造。 */
    public SensitiveTopicGuardrail() {
        this(DEFAULT_TOPICS, Severity.WARN);
    }

    /**
     * @param topics 自定义词表
     */
    public SensitiveTopicGuardrail(Collection<String> topics) {
        this(topics, Severity.WARN);
    }

    /**
     * @param topics 自定义词表
     * @param severity 命中后的严重级别
     */
    public SensitiveTopicGuardrail(Collection<String> topics, Severity severity) {
        this.topics = topics == null ? List.of() : List.copyOf(topics);
        this.severity = severity == null ? Severity.WARN : severity;
    }

    /**
     * @return 当前词表，不可变列表
     */
    public List<String> topics() {
        return topics;
    }

    /**
     * @return 命中后的严重级别
     */
    public Severity severity() {
        return severity;
    }

    @Override
    public GuardrailResult check(GuardrailContext ctx) {
        if (ctx == null) {
            return GuardrailResult.allow();
        }
        String text = ctx.text() == null ? "" : ctx.text();
        List<Finding> findings = new ArrayList<>();
        for (String topic : topics) {
            int start = TextSearch.indexOfIgnoreCase(text, topic);
            if (start >= 0) {
                int end = start + topic.length();
                findings.add(
                        Finding.of(
                                RULE_ID,
                                severity,
                                "命中敏感词：" + topic,
                                TextSearch.snippet(text, start, end),
                                start,
                                end));
            }
        }
        return GuardrailResult.fromFindings(findings);
    }
}
