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

/** 规则链判定为 BLOCK 时抛出的异常。 */
public class GuardrailBlockedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient GuardrailResult result;

    /**
     * @param result 被拦截的检查结果
     */
    public GuardrailBlockedException(GuardrailResult result) {
        super("请求被安全规则拦截: " + (result == null ? "" : result.reason()));
        this.result = result;
    }

    /**
     * @param message 说明
     */
    public GuardrailBlockedException(String message) {
        super(message);
        this.result = null;
    }

    /**
     * @return 被拦截的检查结果，用 {@link #GuardrailBlockedException(String)} 构造时为 null
     */
    public GuardrailResult result() {
        return result;
    }
}
