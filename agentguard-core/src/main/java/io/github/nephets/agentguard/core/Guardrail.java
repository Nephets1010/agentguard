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

/** 单条安全检查规则。实现需要是无状态的，可在多线程下复用。 */
public interface Guardrail {

    /**
     * 对给定上下文做一次检查。
     *
     * @param ctx 检查上下文，不能为 null
     * @return 检查结果，包含处置动作、命中记录与可能的改写后文本
     */
    GuardrailResult check(GuardrailContext ctx);
}
