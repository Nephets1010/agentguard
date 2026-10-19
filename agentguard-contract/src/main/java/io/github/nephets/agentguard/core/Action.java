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

/** 规则链给出的处置动作，严重程度从低到高排列。 */
public enum Action {

    /** 放行。 */
    ALLOW,

    /** 放行，但附带 WARN 级别的命中记录。 */
    WARN,

    /** 文本被改写（脱敏）后放行。 */
    REDACT,

    /** 需要人工或上游审批后才能执行。 */
    REQUIRE_APPROVAL,

    /** 直接拦截。 */
    BLOCK;

    /**
     * 返回两个动作中更严格的一个。
     *
     * @param other 另一个动作
     * @return 更严格的动作
     */
    public Action mostSevere(Action other) {
        if (other == null || compareTo(other) >= 0) {
            return this;
        }
        return other;
    }
}
