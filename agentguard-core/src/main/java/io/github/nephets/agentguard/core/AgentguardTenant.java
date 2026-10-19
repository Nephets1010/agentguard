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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/** 租户上下文：用 InheritableThreadLocal 存放当前租户标识，子线程自动继承。 */
public final class AgentguardTenant {

    private static final Logger log = LoggerFactory.getLogger(AgentguardTenant.class);

    private static final InheritableThreadLocal<String> CURRENT = new InheritableThreadLocal<>();

    private AgentguardTenant() {}

    /**
     * 设置当前租户标识。
     *
     * @param tenantId 租户标识，为 null 或空白时等同于 {@link #clear()}
     */
    public static void set(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            log.debug("clear tenant context (blank or null id)");
            clear();
            return;
        }
        log.debug("set tenant context: tenant={}", tenantId);
        CURRENT.set(tenantId);
    }

    /**
     * @return 当前租户标识，未设置时为 null
     */
    public static String current() {
        return CURRENT.get();
    }

    /**
     * @param fallback 未设置时的返回值
     * @return 当前租户标识，未设置时返回 fallback
     */
    public static String currentOrElse(String fallback) {
        String value = CURRENT.get();
        return value == null ? fallback : value;
    }

    /**
     * @return 当前租户标识
     * @throws IllegalStateException 未设置租户时抛出
     */
    public static String require() {
        String value = CURRENT.get();
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("未设置租户上下文，请先调用 AgentguardTenant.set(tenantId)");
        }
        return value;
    }

    /** 清除当前线程的租户标识。 */
    public static void clear() {
        log.debug("clear tenant context");
        CURRENT.remove();
    }

    /**
     * 在指定租户上下文中执行一段逻辑，执行完恢复原上下文。
     *
     * @param tenantId 租户标识
     * @param action 要执行的逻辑
     */
    public static void runWith(String tenantId, Runnable action) {
        log.debug("runWith tenant={}", tenantId);
        String previous = CURRENT.get();
        try {
            set(tenantId);
            action.run();
        } finally {
            restore(previous);
        }
    }

    /**
     * 在指定租户上下文中执行一段有返回值的逻辑，执行完恢复原上下文。
     *
     * @param tenantId 租户标识
     * @param action 要执行的逻辑
     * @param <T> 返回值类型
     * @return 执行结果
     */
    public static <T> T runWith(String tenantId, Supplier<T> action) {
        log.debug("runWith tenant={}", tenantId);
        String previous = CURRENT.get();
        try {
            set(tenantId);
            return action.get();
        } finally {
            restore(previous);
        }
    }

    private static void restore(String previous) {
        if (previous == null) {
            clear();
        } else {
            CURRENT.set(previous);
        }
    }
}
