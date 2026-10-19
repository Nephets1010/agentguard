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
package io.github.nephets.agentguard.core.budget;

/** 预算的隔离维度：租户标识 + 运行标识。 */
public record BudgetKey(String tenantId, String runId) {

    /** 默认运行标识。 */
    public static final String DEFAULT_RUN_ID = "default";

    /** 默认租户标识。 */
    public static final String DEFAULT_TENANT_ID = "default";

    /**
     * @param tenantId 租户标识，为空时记为 default
     * @param runId 运行标识，为空时记为 default
     */
    public BudgetKey {
        tenantId = tenantId == null || tenantId.isBlank() ? DEFAULT_TENANT_ID : tenantId;
        runId = runId == null || runId.isBlank() ? DEFAULT_RUN_ID : runId;
    }

    /**
     * @param tenantId 租户标识
     * @param runId 运行标识
     * @return 预算键
     */
    public static BudgetKey of(String tenantId, String runId) {
        return new BudgetKey(tenantId, runId);
    }

    /**
     * @param tenantId 租户标识
     * @return 使用默认运行标识的预算键
     */
    public static BudgetKey of(String tenantId) {
        return new BudgetKey(tenantId, DEFAULT_RUN_ID);
    }
}
