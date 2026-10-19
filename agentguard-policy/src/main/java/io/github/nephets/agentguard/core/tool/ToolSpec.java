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
package io.github.nephets.agentguard.core.tool;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** 一个工具的注册信息：风险等级、允许的角色、是否需要审批、单次运行可调用次数。 */
public final class ToolSpec {

    private final String name;
    private final ToolRisk risk;
    private final Set<String> allowedRoles;
    private final boolean requiresApproval;
    private final int maxCallsPerRun;

    private ToolSpec(Builder builder) {
        this.name = builder.name;
        this.risk = builder.risk;
        this.allowedRoles = Collections.unmodifiableSet(new LinkedHashSet<>(builder.allowedRoles));
        this.requiresApproval = builder.requiresApproval;
        this.maxCallsPerRun = builder.maxCallsPerRun;
    }

    /**
     * @return 新的 Builder
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    /**
     * 快速构造：只读、不限角色、不需审批、不限次数。
     *
     * @param name 工具名
     * @return 工具注册信息
     */
    public static ToolSpec readOnly(String name) {
        return builder(name).build();
    }

    /**
     * @return 工具名
     */
    public String name() {
        return name;
    }

    /**
     * @return 风险等级
     */
    public ToolRisk risk() {
        return risk;
    }

    /**
     * @return 允许的角色，空集合表示不限制角色
     */
    public Set<String> allowedRoles() {
        return allowedRoles;
    }

    /**
     * @return 是否需要审批
     */
    public boolean requiresApproval() {
        return requiresApproval;
    }

    /**
     * @return 单次运行允许调用的次数，小于等于 0 表示不限制
     */
    public int maxCallsPerRun() {
        return maxCallsPerRun;
    }

    @Override
    public String toString() {
        return "ToolSpec{name="
                + name
                + ", risk="
                + risk
                + ", roles="
                + allowedRoles
                + ", requiresApproval="
                + requiresApproval
                + ", maxCallsPerRun="
                + maxCallsPerRun
                + "}";
    }

    /** ToolSpec 的构造器。 */
    public static final class Builder {

        private final String name;
        private ToolRisk risk = ToolRisk.READ;
        private Set<String> allowedRoles = new LinkedHashSet<>();
        private boolean requiresApproval = false;
        private int maxCallsPerRun = 0;

        /**
         * @param name 工具名，不能为空
         */
        public Builder(String name) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("工具名不能为空");
            }
            this.name = name;
        }

        /**
         * @param risk 风险等级
         */
        public Builder risk(ToolRisk risk) {
            this.risk = risk == null ? ToolRisk.READ : risk;
            return this;
        }

        /**
         * @param roles 允许的角色
         */
        public Builder allowedRoles(Collection<String> roles) {
            this.allowedRoles.clear();
            if (roles != null) {
                this.allowedRoles.addAll(roles);
            }
            return this;
        }

        /**
         * @param role 追加一个允许的角色
         */
        public Builder allowedRole(String role) {
            if (role != null && !role.isBlank()) {
                this.allowedRoles.add(role);
            }
            return this;
        }

        /**
         * @param requiresApproval 是否需要审批
         */
        public Builder requiresApproval(boolean requiresApproval) {
            this.requiresApproval = requiresApproval;
            return this;
        }

        /**
         * @param maxCallsPerRun 单次运行允许调用的次数，小于等于 0 表示不限制
         */
        public Builder maxCallsPerRun(int maxCallsPerRun) {
            this.maxCallsPerRun = maxCallsPerRun;
            return this;
        }

        /**
         * @return 构造好的工具注册信息
         */
        public ToolSpec build() {
            return new ToolSpec(this);
        }
    }
}
