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

/** 单条命中结果的严重级别。 */
public enum Severity {

    /** 仅记录，不影响判定结果。 */
    INFO,

    /** 可疑，需要提示但不拦截。 */
    WARN,

    /** 确定违规，规则链会按此级别短路为 BLOCK。 */
    ERROR
}
