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

/** 一处个人信息的命中位置。 */
public record PiiMatch(PiiType type, int start, int end, String value) {

    /**
     * @param type 类型
     * @param start 起始下标（含）
     * @param end 结束下标（不含）
     * @param value 命中的原文
     */
    public PiiMatch {
        if (type == null) {
            throw new IllegalArgumentException("type 不能为空");
        }
        if (value == null) {
            throw new IllegalArgumentException("value 不能为空");
        }
    }
}
