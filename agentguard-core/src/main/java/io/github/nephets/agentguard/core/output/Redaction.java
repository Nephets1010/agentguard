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

import java.util.List;

/** 一次脱敏的结果：改写后的文本与对应的命中记录。 */
public record Redaction(String text, List<Finding> findings) {

    /**
     * @param text 改写后的文本
     * @param findings 命中记录，里面的证据是脱敏后的值，不含原文
     */
    public Redaction {
        if (text == null) {
            throw new IllegalArgumentException("text 不能为空");
        }
        if (findings == null) {
            findings = List.of();
        }
        findings = List.copyOf(findings);
    }
}
