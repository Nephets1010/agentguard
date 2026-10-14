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

/** 预算（token / 成本 / 调用次数）用尽时抛出的异常。 */
public class BudgetExceededException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message 说明，通常包含租户、运行标识与已用量
     */
    public BudgetExceededException(String message) {
        super(message);
    }
}
