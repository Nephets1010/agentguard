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

import io.github.nephets.agentguard.core.GuardrailChain;
import io.github.nephets.agentguard.core.LlmClient;
import io.github.nephets.agentguard.core.budget.Budget;
import io.github.nephets.agentguard.core.budget.BudgetTracker;
import io.github.nephets.agentguard.core.input.InstructionOverrideGuardrail;
import io.github.nephets.agentguard.core.input.ObfuscationGuardrail;
import io.github.nephets.agentguard.core.input.RolePlayJailbreakGuardrail;
import io.github.nephets.agentguard.core.input.SensitiveTopicGuardrail;
import io.github.nephets.agentguard.core.input.SystemPromptLeakGuardrail;
import io.github.nephets.agentguard.core.input.ToolAbuseGuardrail;
import io.github.nephets.agentguard.core.output.OutputSchemaGuardrail;
import io.github.nephets.agentguard.core.output.PiiDetector;
import io.github.nephets.agentguard.core.output.PiiRedactionGuardrail;
import io.github.nephets.agentguard.core.output.PiiType;
import io.github.nephets.agentguard.core.output.Redactor;
import io.github.nephets.agentguard.core.rate.RateLimiter;
import io.github.nephets.agentguard.core.tool.ToolPolicy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;

import java.util.EnumSet;
import java.util.Set;

/** agentguard 的自动装配：注册规则链、工具策略、预算、限流与脱敏 Bean。 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AgentguardProperties.class)
@ConditionalOnProperty(
        prefix = "agentguard",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AgentguardAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AgentguardAutoConfiguration.class);

    /** 默认输出长度上限。 */
    public static final int DEFAULT_MAX_OUTPUT_LENGTH = 4096;

    @Bean
    @ConditionalOnMissingBean
    public PiiDetector piiDetector(AgentguardProperties properties) {
        Set<PiiType> types = EnumSet.noneOf(PiiType.class);
        AgentguardProperties.RedactionProperties redaction = properties.getRedaction();
        if (redaction.isPhone()) {
            types.add(PiiType.PHONE);
        }
        if (redaction.isIdCard()) {
            types.add(PiiType.ID_CARD);
        }
        if (redaction.isBankCard()) {
            types.add(PiiType.BANK_CARD);
        }
        if (redaction.isEmail()) {
            types.add(PiiType.EMAIL);
        }
        if (redaction.isIp()) {
            types.add(PiiType.IP);
        }
        log.info("configured PiiDetector with types={}", types);
        return new PiiDetector(types);
    }

    @Bean
    @ConditionalOnMissingBean
    public Redactor redactor(PiiDetector piiDetector) {
        log.info("configured Redactor");
        return new Redactor(piiDetector);
    }

    @Bean
    @ConditionalOnMissingBean
    public GuardrailChain guardrailChain(
            PiiDetector piiDetector, Redactor redactor, AgentguardProperties properties) {
        boolean redactionEnabled = properties.getRedaction().isEnabled();
        GuardrailChain chain =
                GuardrailChain.builder()
                        .add(new InstructionOverrideGuardrail())
                        .add(new SystemPromptLeakGuardrail())
                        .add(new RolePlayJailbreakGuardrail())
                        .add(new ObfuscationGuardrail())
                        .add(new ToolAbuseGuardrail())
                        .add(new SensitiveTopicGuardrail())
                        .add(new PiiRedactionGuardrail(piiDetector, redactor, redactionEnabled))
                        .add(
                                OutputSchemaGuardrail.builder()
                                        .maxLength(DEFAULT_MAX_OUTPUT_LENGTH)
                                        .build())
                        .build();
        log.info(
                "configured GuardrailChain with {} guardrails (redactionEnabled={})",
                chain.size(),
                redactionEnabled);
        return chain;
    }

    @Bean
    @ConditionalOnMissingBean
    public ToolPolicy toolPolicy() {
        log.info("configured ToolPolicy (deny-by-default)");
        return new ToolPolicy();
    }

    @Bean
    @ConditionalOnMissingBean
    public BudgetTracker budgetTracker(AgentguardProperties properties) {
        AgentguardProperties.BudgetProperties budget = properties.getBudget();
        BudgetTracker tracker =
                new BudgetTracker(
                        Budget.of(
                                budget.getMaxTokens(),
                                budget.getMaxCostMicros(),
                                budget.getMaxCalls()));
        log.info(
                "configured BudgetTracker maxTokens={} maxCostMicros={} maxCalls={}",
                budget.getMaxTokens(),
                budget.getMaxCostMicros(),
                budget.getMaxCalls());
        return tracker;
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimiter rateLimiter(AgentguardProperties properties) {
        AgentguardProperties.RateLimitProperties rateLimit = properties.getRateLimit();
        RateLimiter limiter = new RateLimiter(rateLimit.getPermits(), rateLimit.getWindow());
        log.info(
                "configured RateLimiter permits={} window={}",
                rateLimit.getPermits(),
                rateLimit.getWindow());
        return limiter;
    }

    @Bean
    @ConditionalOnMissingBean
    public LlmClient llmClient(GuardrailChain guardrailChain) {
        RuleBasedLlmClient client = new RuleBasedLlmClient(guardrailChain);
        log.info("configured LlmClient (rule-based)");
        return client;
    }

    /** Web 环境下的控制器装配。 */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(DispatcherServlet.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public static class WebConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public AgentguardController agentguardController(
                GuardrailChain guardrailChain,
                ToolPolicy toolPolicy,
                AgentguardProperties properties) {
            log.info("configured AgentguardController (web)");
            return new AgentguardController(guardrailChain, toolPolicy, properties);
        }
    }
}
