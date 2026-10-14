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

import io.github.nephets.agentguard.core.AgentguardTenant;
import io.github.nephets.agentguard.core.Finding;
import io.github.nephets.agentguard.core.GuardrailChain;
import io.github.nephets.agentguard.core.GuardrailContext;
import io.github.nephets.agentguard.core.GuardrailResult;
import io.github.nephets.agentguard.core.tool.ToolCall;
import io.github.nephets.agentguard.core.tool.ToolDecision;
import io.github.nephets.agentguard.core.tool.ToolPolicy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/** 对外暴露检查与工具鉴权端点，按租户头隔离上下文。 */
@RestController
public class AgentguardController {

    private static final Logger log = LoggerFactory.getLogger(AgentguardController.class);

    /** 检查端点路径。 */
    public static final String CHECK_PATH = "/agentguard/check";

    /** 工具鉴权端点路径。 */
    public static final String AUTHORIZE_PATH = "/agentguard/tools/authorize";

    private final GuardrailChain chain;
    private final ToolPolicy toolPolicy;
    private final AgentguardProperties properties;

    /**
     * @param chain 规则链
     * @param toolPolicy 工具策略
     * @param properties 配置
     */
    public AgentguardController(
            GuardrailChain chain, ToolPolicy toolPolicy, AgentguardProperties properties) {
        this.chain = chain;
        this.toolPolicy = toolPolicy;
        this.properties = properties;
    }

    /**
     * 对一段文本跑一次规则链。
     *
     * @param request 入参，text 为待检查文本，stage 为空时按输入阶段处理
     * @param headers 请求头，用于取租户标识
     * @return 处置动作、命中记录与改写后的文本
     */
    @PostMapping(
            path = CHECK_PATH,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public CheckResponse check(
            @RequestBody CheckRequest request,
            @RequestHeader(required = false) Map<String, String> headers) {
        String tenant = resolveTenant(headers);
        String stage =
                request.stage() == null || request.stage().isBlank()
                        ? GuardrailContext.STAGE_INPUT
                        : request.stage();
        log.info(
                "check request: tenant={} stage={} textLength={}",
                tenant,
                stage,
                request.text() == null ? 0 : request.text().length());
        GuardrailContext ctx =
                GuardrailContext.builder()
                        .tenantId(tenant)
                        .stage(stage)
                        .text(request.text())
                        .build();
        GuardrailResult result =
                AgentguardTenant.runWith(
                        tenant, (Supplier<GuardrailResult>) () -> chain.check(ctx));
        log.info("check result: tenant={} stage={} action={}", tenant, stage, result.action());
        return new CheckResponse(
                result.action().name(),
                toViews(result.findings()),
                result.sanitizedText(),
                result.reason());
    }

    /**
     * 判定一次工具调用。
     *
     * @param request 入参
     * @param headers 请求头，用于取租户标识
     * @return 判定结果
     */
    @PostMapping(
            path = AUTHORIZE_PATH,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ToolDecisionResponse authorize(
            @RequestBody ToolCallRequest request,
            @RequestHeader(required = false) Map<String, String> headers) {
        String tenant = resolveTenant(headers);
        String toolName = request.toolName() == null ? "" : request.toolName();
        log.info(
                "authorize request: tenant={} toolName={} principal={}",
                tenant,
                toolName,
                request.principal());
        ToolCall call =
                ToolCall.builder(toolName)
                        .arguments(request.arguments())
                        .principal(request.principal())
                        .roles(request.roles())
                        .build();
        ToolDecision decision =
                AgentguardTenant.runWith(
                        tenant, (Supplier<ToolDecision>) () -> toolPolicy.authorize(call));
        log.info("authorize result: tenant={} toolName={} decision={}", tenant, toolName, decision);
        return new ToolDecisionResponse(call.toolName(), decision.name(), tenant);
    }

    private String resolveTenant(Map<String, String> headers) {
        if (headers != null && !headers.isEmpty()) {
            String header = headers.get(properties.getTenantHeader());
            if (header != null && !header.isBlank()) {
                return header;
            }
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getKey() != null
                        && entry.getKey().equalsIgnoreCase(properties.getTenantHeader())
                        && entry.getValue() != null
                        && !entry.getValue().isBlank()) {
                    return entry.getValue();
                }
            }
        }
        return AgentguardTenant.currentOrElse("default");
    }

    private static List<FindingView> toViews(List<Finding> findings) {
        List<FindingView> views = new ArrayList<>();
        for (Finding finding : findings) {
            views.add(
                    new FindingView(
                            finding.ruleId(),
                            finding.severity().name(),
                            finding.message(),
                            finding.evidence(),
                            finding.start(),
                            finding.end()));
        }
        return List.copyOf(views);
    }

    /** /agentguard/check 的入参。 */
    public record CheckRequest(String text, String stage) {}

    /** /agentguard/check 的出参。 */
    public record CheckResponse(
            String action, List<FindingView> findings, String sanitizedText, String reason) {}

    /** 命中记录的传输结构，避免直接序列化 core 的值对象。 */
    public record FindingView(
            String ruleId, String severity, String message, String evidence, int start, int end) {}

    /** /agentguard/tools/authorize 的入参。 */
    public record ToolCallRequest(
            String toolName, Map<String, Object> arguments, String principal, Set<String> roles) {}

    /** /agentguard/tools/authorize 的出参。 */
    public record ToolDecisionResponse(String toolName, String decision, String tenantId) {}
}
