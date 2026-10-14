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
package io.github.nephets.agentguard.core;

import java.util.Objects;

/** 一条命中记录：哪条规则命中、严重级别、说明、证据片段与在原文本中的位置。 */
public final class Finding {

    /** 位置未知时的占位值。 */
    public static final int UNKNOWN_POSITION = -1;

    private final String ruleId;
    private final Severity severity;
    private final String message;
    private final String evidence;
    private final int start;
    private final int end;

    /**
     * @param ruleId 规则标识，不能为空
     * @param severity 严重级别，不能为 null
     * @param message 说明文字
     * @param evidence 命中的证据片段，可为空
     * @param start 证据在原文本中的起始下标，未知时传 {@link #UNKNOWN_POSITION}
     * @param end 证据在原文本中的结束下标（不含），未知时传 {@link #UNKNOWN_POSITION}
     */
    public Finding(
            String ruleId, Severity severity, String message, String evidence, int start, int end) {
        this.ruleId = requireNonBlank(ruleId, "ruleId");
        this.severity = Objects.requireNonNull(severity, "severity");
        this.message = message == null ? "" : message;
        this.evidence = evidence == null ? "" : evidence;
        this.start = start;
        this.end = end;
    }

    /** 创建一条不带位置信息的命中记录。 */
    public static Finding of(String ruleId, Severity severity, String message) {
        return new Finding(ruleId, severity, message, "", UNKNOWN_POSITION, UNKNOWN_POSITION);
    }

    /** 创建一条带证据与位置的命中记录。 */
    public static Finding of(
            String ruleId, Severity severity, String message, String evidence, int start, int end) {
        return new Finding(ruleId, severity, message, evidence, start, end);
    }

    /** 创建 INFO 级别的命中记录。 */
    public static Finding info(String ruleId, String message) {
        return of(ruleId, Severity.INFO, message);
    }

    /** 创建 WARN 级别的命中记录。 */
    public static Finding warn(String ruleId, String message) {
        return of(ruleId, Severity.WARN, message);
    }

    /** 创建 ERROR 级别的命中记录。 */
    public static Finding error(String ruleId, String message) {
        return of(ruleId, Severity.ERROR, message);
    }

    /**
     * @return 规则标识
     */
    public String ruleId() {
        return ruleId;
    }

    /**
     * @return 严重级别
     */
    public Severity severity() {
        return severity;
    }

    /**
     * @return 说明文字
     */
    public String message() {
        return message;
    }

    /**
     * @return 证据片段，可能为空字符串
     */
    public String evidence() {
        return evidence;
    }

    /**
     * @return 起始下标，未知时为 {@link #UNKNOWN_POSITION}
     */
    public int start() {
        return start;
    }

    /**
     * @return 结束下标（不含），未知时为 {@link #UNKNOWN_POSITION}
     */
    public int end() {
        return end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Finding other)) {
            return false;
        }
        return start == other.start
                && end == other.end
                && ruleId.equals(other.ruleId)
                && severity == other.severity
                && message.equals(other.message)
                && evidence.equals(other.evidence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleId, severity, message, evidence, start, end);
    }

    @Override
    public String toString() {
        return ruleId + "[" + severity + "] " + message;
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value;
    }
}
