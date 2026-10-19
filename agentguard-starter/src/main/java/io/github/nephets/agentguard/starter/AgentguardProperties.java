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

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** agentguard.* 配置项。 */
@ConfigurationProperties(prefix = "agentguard")
public class AgentguardProperties {

    /** 默认租户请求头。 */
    public static final String DEFAULT_TENANT_HEADER = "X-Tenant-Id";

    private String tenantHeader = DEFAULT_TENANT_HEADER;
    private boolean enabled = true;
    private Severity blockOnErrorSeverity = Severity.ERROR;
    private BudgetProperties budget = new BudgetProperties();
    private RateLimitProperties rateLimit = new RateLimitProperties();
    private RedactionProperties redaction = new RedactionProperties();

    /**
     * @return 读取租户标识的请求头名
     */
    public String getTenantHeader() {
        return tenantHeader;
    }

    /**
     * @param tenantHeader 读取租户标识的请求头名
     */
    public void setTenantHeader(String tenantHeader) {
        this.tenantHeader = tenantHeader;
    }

    /**
     * @return 是否启用自动装配
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled 是否启用自动装配
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return 达到该级别即判为拦截
     */
    public Severity getBlockOnErrorSeverity() {
        return blockOnErrorSeverity;
    }

    /**
     * @param blockOnErrorSeverity 达到该级别即判为拦截
     */
    public void setBlockOnErrorSeverity(Severity blockOnErrorSeverity) {
        this.blockOnErrorSeverity = blockOnErrorSeverity;
    }

    /**
     * @return 预算配置
     */
    public BudgetProperties getBudget() {
        return budget;
    }

    /**
     * @param budget 预算配置
     */
    public void setBudget(BudgetProperties budget) {
        this.budget = budget;
    }

    /**
     * @return 限流配置
     */
    public RateLimitProperties getRateLimit() {
        return rateLimit;
    }

    /**
     * @param rateLimit 限流配置
     */
    public void setRateLimit(RateLimitProperties rateLimit) {
        this.rateLimit = rateLimit;
    }

    /**
     * @return 脱敏配置
     */
    public RedactionProperties getRedaction() {
        return redaction;
    }

    /**
     * @param redaction 脱敏配置
     */
    public void setRedaction(RedactionProperties redaction) {
        this.redaction = redaction;
    }

    /** agentguard.budget.* */
    public static class BudgetProperties {

        private long maxTokens = 100_000L;
        private long maxCostMicros = 5_000_000L;
        private int maxCalls = 100;

        /**
         * @return token 上限
         */
        public long getMaxTokens() {
            return maxTokens;
        }

        /**
         * @param maxTokens token 上限
         */
        public void setMaxTokens(long maxTokens) {
            this.maxTokens = maxTokens;
        }

        /**
         * @return 成本上限，单位为微元
         */
        public long getMaxCostMicros() {
            return maxCostMicros;
        }

        /**
         * @param maxCostMicros 成本上限，单位为微元
         */
        public void setMaxCostMicros(long maxCostMicros) {
            this.maxCostMicros = maxCostMicros;
        }

        /**
         * @return 调用次数上限
         */
        public int getMaxCalls() {
            return maxCalls;
        }

        /**
         * @param maxCalls 调用次数上限
         */
        public void setMaxCalls(int maxCalls) {
            this.maxCalls = maxCalls;
        }
    }

    /** agentguard.rate-limit.* */
    public static class RateLimitProperties {

        private int permits = 60;
        private Duration window = Duration.ofMinutes(1);

        /**
         * @return 窗口内允许的次数
         */
        public int getPermits() {
            return permits;
        }

        /**
         * @param permits 窗口内允许的次数
         */
        public void setPermits(int permits) {
            this.permits = permits;
        }

        /**
         * @return 窗口长度
         */
        public Duration getWindow() {
            return window;
        }

        /**
         * @param window 窗口长度
         */
        public void setWindow(Duration window) {
            this.window = window;
        }
    }

    /** agentguard.redaction.* */
    public static class RedactionProperties {

        private boolean enabled = true;
        private boolean phone = true;
        private boolean idCard = true;
        private boolean bankCard = true;
        private boolean email = true;
        private boolean ip = true;

        /**
         * @return 是否启用脱敏
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * @param enabled 是否启用脱敏
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * @return 是否脱敏手机号
         */
        public boolean isPhone() {
            return phone;
        }

        /**
         * @param phone 是否脱敏手机号
         */
        public void setPhone(boolean phone) {
            this.phone = phone;
        }

        /**
         * @return 是否脱敏身份证号
         */
        public boolean isIdCard() {
            return idCard;
        }

        /**
         * @param idCard 是否脱敏身份证号
         */
        public void setIdCard(boolean idCard) {
            this.idCard = idCard;
        }

        /**
         * @return 是否脱敏银行卡号
         */
        public boolean isBankCard() {
            return bankCard;
        }

        /**
         * @param bankCard 是否脱敏银行卡号
         */
        public void setBankCard(boolean bankCard) {
            this.bankCard = bankCard;
        }

        /**
         * @return 是否脱敏邮箱
         */
        public boolean isEmail() {
            return email;
        }

        /**
         * @param email 是否脱敏邮箱
         */
        public void setEmail(boolean email) {
            this.email = email;
        }

        /**
         * @return 是否脱敏 IP
         */
        public boolean isIp() {
            return ip;
        }

        /**
         * @param ip 是否脱敏 IP
         */
        public void setIp(boolean ip) {
            this.ip = ip;
        }
    }
}
