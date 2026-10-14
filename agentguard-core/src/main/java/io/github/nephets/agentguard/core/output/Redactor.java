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
import io.github.nephets.agentguard.core.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 按类型把个人信息替换成掩码：手机号保留前 3 后 4，身份证保留前 6 后 4， 银行卡保留前 4 后 4，邮箱保留首字母与域名，IP 保留前两段。 */
public final class Redactor {

    /** 命中记录的规则标识前缀。 */
    public static final String RULE_PREFIX = "output.pii.";

    private final PiiDetector detector;

    /** 默认构造，检测并脱敏全部类型。 */
    public Redactor() {
        this(new PiiDetector());
    }

    /**
     * @param detector 检测器，决定检测哪些类型
     */
    public Redactor(PiiDetector detector) {
        this.detector = detector == null ? new PiiDetector() : detector;
    }

    /**
     * @return 使用的检测器
     */
    public PiiDetector detector() {
        return detector;
    }

    /**
     * @param text 原文本
     * @return 脱敏后的文本
     */
    public String redact(String text) {
        return redactWithFindings(text).text();
    }

    /**
     * @param text 原文本
     * @return 脱敏后的文本与命中记录
     */
    public Redaction redactWithFindings(String text) {
        if (text == null || text.isEmpty()) {
            return new Redaction(text == null ? "" : text, List.of());
        }
        List<PiiMatch> matches = detector.detect(text);
        if (matches.isEmpty()) {
            return new Redaction(text, List.of());
        }
        List<Finding> findings = new ArrayList<>();
        StringBuilder builder = new StringBuilder(text.length());
        int cursor = 0;
        for (PiiMatch match : matches) {
            if (match.start() < cursor) {
                continue;
            }
            String masked = mask(match);
            builder.append(text, cursor, match.start()).append(masked);
            cursor = match.end();
            findings.add(
                    Finding.of(
                            RULE_PREFIX + match.type().name().toLowerCase(Locale.ROOT),
                            Severity.WARN,
                            "检测到" + label(match.type()) + "，已脱敏",
                            masked,
                            match.start(),
                            match.end()));
        }
        builder.append(text.substring(cursor));
        return new Redaction(builder.toString(), findings);
    }

    /**
     * @param match 一处命中
     * @return 掩码替换后的文本
     */
    public String mask(PiiMatch match) {
        String value = match.value();
        return switch (match.type()) {
            case PHONE -> keepHeadTail(value, 3, 4);
            case ID_CARD -> keepHeadTail(value, 6, 4);
            case BANK_CARD -> keepHeadTail(value, 4, 4);
            case EMAIL -> maskEmail(value);
            case IP -> maskIp(value);
        };
    }

    private static String keepHeadTail(String value, int head, int tail) {
        int length = value.length();
        if (length <= head + tail) {
            return "*".repeat(length);
        }
        return value.substring(0, head)
                + "*".repeat(length - head - tail)
                + value.substring(length - tail);
    }

    private static String maskEmail(String value) {
        int at = value.lastIndexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = value.substring(0, at);
        String domain = value.substring(at);
        return local.substring(0, 1) + "***" + domain;
    }

    private static String maskIp(String value) {
        String[] parts = value.split("\\.");
        if (parts.length != 4) {
            return "***";
        }
        return parts[0] + "." + parts[1] + ".*.*";
    }

    private static String label(PiiType type) {
        return switch (type) {
            case PHONE -> "手机号";
            case ID_CARD -> "身份证号";
            case BANK_CARD -> "银行卡号";
            case EMAIL -> "邮箱";
            case IP -> "IP 地址";
        };
    }
}
