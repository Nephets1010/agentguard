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

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 一次检查的输入：租户、阶段、文本与附加属性。不可变，用 Builder 构造。 */
public final class GuardrailContext {

    /** 输入阶段：用户或上游传给 Agent 的提示词。 */
    public static final String STAGE_INPUT = "INPUT";

    /** 输出阶段：Agent 产生的回复。 */
    public static final String STAGE_OUTPUT = "OUTPUT";

    /** 工具阶段：Agent 准备调用的工具与参数。 */
    public static final String STAGE_TOOL = "TOOL";

    private final String tenantId;
    private final String stage;
    private final String text;
    private final Map<String, Object> attributes;
    private final Instant now;

    private GuardrailContext(Builder builder) {
        this.tenantId =
                builder.tenantId != null
                        ? builder.tenantId
                        : AgentguardTenant.currentOrElse("default");
        this.stage = builder.stage;
        this.text = builder.text;
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<>(builder.attributes));
        this.now = builder.now;
    }

    /**
     * @return 新的 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** 返回一个文本被替换为 {@code text} 的副本，其余字段不变。 */
    public GuardrailContext withText(String text) {
        return new GuardrailContext(this, text == null ? "" : text, null);
    }

    /** 返回一个阶段被替换为 {@code stage} 的副本。 */
    public GuardrailContext withStage(String stage) {
        return new GuardrailContext(this, null, stage);
    }

    /** 返回一个追加了属性 {@code key} = {@code value} 的副本。 */
    public GuardrailContext withAttribute(String key, Object value) {
        Builder builder =
                builder()
                        .tenantId(tenantId)
                        .stage(stage)
                        .text(text)
                        .attributes(attributes)
                        .now(now);
        builder.attribute(key, value);
        return builder.build();
    }

    private GuardrailContext(GuardrailContext source, String text, String stage) {
        this.tenantId = source.tenantId;
        this.stage = stage == null ? source.stage : stage;
        this.text = text == null ? source.text : text;
        this.attributes = source.attributes;
        this.now = source.now;
    }

    /**
     * @return 租户标识
     */
    public String tenantId() {
        return tenantId;
    }

    /**
     * @return 阶段，取值为 {@link #STAGE_INPUT} / {@link #STAGE_OUTPUT} / {@link #STAGE_TOOL}
     */
    public String stage() {
        return stage;
    }

    /**
     * @return 待检查文本，不会为 null
     */
    public String text() {
        return text;
    }

    /**
     * @return 附加属性，不可变视图
     */
    public Map<String, Object> attributes() {
        return attributes;
    }

    /**
     * @return 检查发生的时间
     */
    public Instant now() {
        return now;
    }

    /** GuardrailContext 的构造器。 */
    public static final class Builder {

        private String tenantId;
        private String stage = STAGE_INPUT;
        private String text = "";
        private Map<String, Object> attributes = new LinkedHashMap<>();
        private Instant now = Instant.now();

        /**
         * @param tenantId 租户标识，为空时回退到 {@link AgentguardTenant#currentOrElse(String)}
         */
        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        /**
         * @param stage 阶段，建议使用 {@link #STAGE_INPUT} 等常量
         */
        public Builder stage(String stage) {
            this.stage = stage == null || stage.isBlank() ? STAGE_INPUT : stage;
            return this;
        }

        /**
         * @param text 待检查文本，null 视为空串
         */
        public Builder text(String text) {
            this.text = text == null ? "" : text;
            return this;
        }

        /** 追加单个属性。 */
        public Builder attribute(String key, Object value) {
            this.attributes.put(key, value);
            return this;
        }

        /** 批量追加属性。 */
        public Builder attributes(Map<String, Object> attributes) {
            if (attributes != null) {
                this.attributes.putAll(attributes);
            }
            return this;
        }

        /**
         * @param now 检查时间
         */
        public Builder now(Instant now) {
            this.now = now == null ? Instant.now() : now;
            return this;
        }

        /**
         * @return 构造好的上下文
         */
        public GuardrailContext build() {
            return new GuardrailContext(this);
        }
    }
}
