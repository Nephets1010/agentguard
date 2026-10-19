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
package io.github.nephets.agentguard.starter;

import io.github.nephets.agentguard.core.Action;
import io.github.nephets.agentguard.core.GuardrailChain;
import io.github.nephets.agentguard.core.GuardrailContext;
import io.github.nephets.agentguard.core.GuardrailResult;
import io.github.nephets.agentguard.core.LlmClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link LlmClient} 的本地实现：不调用任何外部模型，直接用规则链判定。
 *
 * <p>想换成真实模型时，注册一个自己的 {@link LlmClient} Bean 即可覆盖。
 */
public class RuleBasedLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(RuleBasedLlmClient.class);

    private final GuardrailChain chain;

    /**
     * @param chain 规则链
     */
    public RuleBasedLlmClient(GuardrailChain chain) {
        this.chain = chain == null ? new GuardrailChain() : chain;
    }

    /**
     * @return 使用的规则链
     */
    public GuardrailChain chain() {
        return chain;
    }

    @Override
    public boolean looksUnsafe(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        GuardrailResult result =
                chain.check(
                        GuardrailContext.builder()
                                .stage(GuardrailContext.STAGE_INPUT)
                                .text(text)
                                .build());
        boolean unsafe = result.action() != Action.ALLOW;
        log.debug("looksUnsafe inputLength={} -> unsafe={}", text.length(), unsafe);
        return unsafe;
    }
}
