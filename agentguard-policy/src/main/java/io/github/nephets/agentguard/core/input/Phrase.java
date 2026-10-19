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

import io.github.nephets.agentguard.core.Severity;

/** 一条待匹配短语及其命中后的严重级别。 */
public record Phrase(String text, Severity severity) {

    /**
     * @param text 短语，不能为空
     * @param severity 命中后的严重级别，不能为 null
     */
    public Phrase {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("短语不能为空");
        }
        if (severity == null) {
            throw new IllegalArgumentException("severity 不能为空");
        }
    }

    /**
     * @return 默认 WARN 级别的短语
     */
    public static Phrase of(String text) {
        return new Phrase(text, Severity.WARN);
    }

    /**
     * @return 指定级别的短语
     */
    public static Phrase of(String text, Severity severity) {
        return new Phrase(text, severity);
    }
}
