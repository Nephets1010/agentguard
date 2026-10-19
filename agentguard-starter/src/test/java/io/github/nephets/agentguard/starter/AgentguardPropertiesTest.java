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

import io.github.nephets.agentguard.core.Severity;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AgentguardPropertiesTest {

    @Test
    void 默认值符合约定() {
        AgentguardProperties properties = new AgentguardProperties();
        assertThat(properties.getTenantHeader()).isEqualTo("X-Tenant-Id");
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getBlockOnErrorSeverity()).isEqualTo(Severity.ERROR);
        assertThat(properties.getBudget().getMaxTokens()).isEqualTo(100_000L);
        assertThat(properties.getBudget().getMaxCostMicros()).isEqualTo(5_000_000L);
        assertThat(properties.getBudget().getMaxCalls()).isEqualTo(100);
        assertThat(properties.getRateLimit().getPermits()).isEqualTo(60);
        assertThat(properties.getRateLimit().getWindow()).isEqualTo(Duration.ofMinutes(1));
        assertThat(properties.getRedaction().isEnabled()).isTrue();
    }

    @Test
    void 脱敏开关默认全开() {
        AgentguardProperties.RedactionProperties redaction =
                new AgentguardProperties().getRedaction();
        assertThat(redaction.isPhone()).isTrue();
        assertThat(redaction.isIdCard()).isTrue();
        assertThat(redaction.isBankCard()).isTrue();
        assertThat(redaction.isEmail()).isTrue();
        assertThat(redaction.isIp()).isTrue();
    }

    @Test
    void 赋值后可读取() {
        AgentguardProperties properties = new AgentguardProperties();
        properties.setTenantHeader("X-Org");
        properties.setEnabled(false);
        properties.setBlockOnErrorSeverity(Severity.WARN);

        AgentguardProperties.BudgetProperties budget = new AgentguardProperties.BudgetProperties();
        budget.setMaxTokens(10L);
        budget.setMaxCostMicros(20L);
        budget.setMaxCalls(3);
        properties.setBudget(budget);

        AgentguardProperties.RateLimitProperties rateLimit =
                new AgentguardProperties.RateLimitProperties();
        rateLimit.setPermits(5);
        rateLimit.setWindow(Duration.ofSeconds(30));
        properties.setRateLimit(rateLimit);

        AgentguardProperties.RedactionProperties redaction =
                new AgentguardProperties.RedactionProperties();
        redaction.setEnabled(false);
        redaction.setPhone(false);
        redaction.setIdCard(false);
        redaction.setBankCard(false);
        redaction.setEmail(false);
        redaction.setIp(false);
        properties.setRedaction(redaction);

        assertThat(properties.getTenantHeader()).isEqualTo("X-Org");
        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getBlockOnErrorSeverity()).isEqualTo(Severity.WARN);
        assertThat(properties.getBudget().getMaxTokens()).isEqualTo(10L);
        assertThat(properties.getBudget().getMaxCostMicros()).isEqualTo(20L);
        assertThat(properties.getBudget().getMaxCalls()).isEqualTo(3);
        assertThat(properties.getRateLimit().getPermits()).isEqualTo(5);
        assertThat(properties.getRateLimit().getWindow()).isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.getRedaction().isEnabled()).isFalse();
        assertThat(properties.getRedaction().isPhone()).isFalse();
        assertThat(properties.getRedaction().isIdCard()).isFalse();
        assertThat(properties.getRedaction().isBankCard()).isFalse();
        assertThat(properties.getRedaction().isEmail()).isFalse();
        assertThat(properties.getRedaction().isIp()).isFalse();
    }
}
