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

/**
 * 模型侧辅助判定的接口，定义在 core 里是为了让规则链可以互换实现。
 *
 * <p>core 不提供任何模型 SDK 依赖；starter 里注册的是基于规则的本地实现。
 */
public interface LlmClient {

    /**
     * 判断一段文本看上去是否不安全。
     *
     * @param text 待判断文本
     * @return 不安全返回 true
     */
    boolean looksUnsafe(String text);
}
