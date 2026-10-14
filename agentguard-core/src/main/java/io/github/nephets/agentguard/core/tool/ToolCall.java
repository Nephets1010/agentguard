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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** 一次工具调用请求：工具名、参数、调用方标识与角色。 */
public final class ToolCall {

    private final String toolName;
    private final Map<String, Object> arguments;
    private final String principal;
    private final Set<String> roles;

    private ToolCall(Builder builder) {
        this.toolName = builder.toolName;
        this.arguments = Collections.unmodifiableMap(new LinkedHashMap<>(builder.arguments));
        this.principal = builder.principal;
        this.roles = Collections.unmodifiableSet(new LinkedHashSet<>(builder.roles));
    }

    /**
     * @return 新的 Builder
     */
    public static Builder builder(String toolName) {
        return new Builder(toolName);
    }

    /**
     * 快速构造。
     *
     * @param toolName 工具名
     * @param arguments 参数
     * @param principal 调用方标识
     * @param roles 调用方角色
     * @return 工具调用请求
     */
    public static ToolCall of(
            String toolName,
            Map<String, Object> arguments,
            String principal,
            Collection<String> roles) {
        return builder(toolName).arguments(arguments).principal(principal).roles(roles).build();
    }

    /**
     * @return 工具名
     */
    public String toolName() {
        return toolName;
    }

    /**
     * @return 参数，不可变视图
     */
    public Map<String, Object> arguments() {
        return arguments;
    }

    /**
     * @return 调用方标识
     */
    public String principal() {
        return principal;
    }

    /**
     * @return 调用方角色，不可变视图
     */
    public Set<String> roles() {
        return roles;
    }

    @Override
    public String toString() {
        return "ToolCall{tool="
                + toolName
                + ", principal="
                + principal
                + ", roles="
                + roles
                + ", arguments="
                + arguments.keySet()
                + "}";
    }

    /** ToolCall 的构造器。 */
    public static final class Builder {

        private final String toolName;
        private Map<String, Object> arguments = new LinkedHashMap<>();
        private String principal = "anonymous";
        private Set<String> roles = new LinkedHashSet<>();

        /**
         * @param toolName 工具名，不能为空
         */
        public Builder(String toolName) {
            if (toolName == null || toolName.isBlank()) {
                throw new IllegalArgumentException("工具名不能为空");
            }
            this.toolName = toolName;
        }

        /**
         * @param arguments 参数
         */
        public Builder arguments(Map<String, Object> arguments) {
            this.arguments.clear();
            if (arguments != null) {
                this.arguments.putAll(arguments);
            }
            return this;
        }

        /**
         * @param name 参数名
         * @param value 参数值
         */
        public Builder argument(String name, Object value) {
            this.arguments.put(name, value);
            return this;
        }

        /**
         * @param principal 调用方标识，为空时用 anonymous
         */
        public Builder principal(String principal) {
            this.principal = principal == null || principal.isBlank() ? "anonymous" : principal;
            return this;
        }

        /**
         * @param roles 调用方角色
         */
        public Builder roles(Collection<String> roles) {
            this.roles.clear();
            if (roles != null) {
                this.roles.addAll(roles);
            }
            return this;
        }

        /**
         * @param role 追加一个角色
         */
        public Builder role(String role) {
            if (role != null && !role.isBlank()) {
                this.roles.add(role);
            }
            return this;
        }

        /**
         * @return 构造好的工具调用请求
         */
        public ToolCall build() {
            return new ToolCall(this);
        }
    }
}
