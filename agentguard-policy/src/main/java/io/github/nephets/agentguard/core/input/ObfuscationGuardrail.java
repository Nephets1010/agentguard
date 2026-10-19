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
package io.github.nephets.agentguard.core.input;

import io.github.nephets.agentguard.core.Finding;
import io.github.nephets.agentguard.core.Guardrail;
import io.github.nephets.agentguard.core.GuardrailContext;
import io.github.nephets.agentguard.core.GuardrailResult;
import io.github.nephets.agentguard.core.Severity;
import io.github.nephets.agentguard.core.TextSearch;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 检测绕过检测的手段：base64 长串、零宽字符、百分号编码、控制字符。
 *
 * <p>base64 串若能解码且解码内容里带注入短语，判定升级为 ERROR。
 */
public final class ObfuscationGuardrail implements Guardrail {

    /** 零宽 / 不可见字符的规则标识。 */
    public static final String RULE_ZERO_WIDTH = "input.obfuscation.zero-width";

    /** base64 长串的规则标识。 */
    public static final String RULE_BASE64 = "input.obfuscation.base64";

    /** 百分号编码的规则标识。 */
    public static final String RULE_PERCENT = "input.obfuscation.percent-encoding";

    /** 控制字符的规则标识。 */
    public static final String RULE_CONTROL_CHAR = "input.obfuscation.control-char";

    /** base64 串的最小长度。 */
    public static final int MIN_BASE64_LENGTH = 32;

    private static final Pattern ZERO_WIDTH =
            Pattern.compile("[\\u00AD\\u200B\\u200C\\u200D\\u2060\\uFEFF\\u202A-\\u202E]");

    private static final Pattern BASE64_TOKEN =
            Pattern.compile("[A-Za-z0-9+/]{" + MIN_BASE64_LENGTH + ",}={0,2}");

    private static final Pattern PERCENT_ENCODED = Pattern.compile("(?:%[0-9A-Fa-f]{2}){4,}");

    private static final Pattern CONTROL_CHAR =
            Pattern.compile("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]");

    private static final List<String> INJECTION_MARKERS =
            List.of(
                    "ignore previous",
                    "ignore all previous",
                    "system prompt",
                    "忽略以上",
                    "绕过权限",
                    "drop table");

    /** 使用默认配置构造。 */
    public ObfuscationGuardrail() {}

    @Override
    public GuardrailResult check(GuardrailContext ctx) {
        if (ctx == null) {
            return GuardrailResult.allow();
        }
        String text = ctx.text() == null ? "" : ctx.text();
        List<Finding> findings = new ArrayList<>();
        collectZeroWidth(text, findings);
        collectBase64(text, findings);
        collectPercent(text, findings);
        collectControlChars(text, findings);
        return GuardrailResult.fromFindings(findings);
    }

    private void collectZeroWidth(String text, List<Finding> findings) {
        Matcher matcher = ZERO_WIDTH.matcher(text);
        int count = 0;
        int first = -1;
        while (matcher.find()) {
            count++;
            if (first < 0) {
                first = matcher.start();
            }
        }
        if (count > 0) {
            findings.add(
                    Finding.of(
                            RULE_ZERO_WIDTH,
                            Severity.WARN,
                            "含零宽或不可见字符 " + count + " 个",
                            TextSearch.snippet(text, first, first + 1),
                            first,
                            first + 1));
        }
    }

    private void collectBase64(String text, List<Finding> findings) {
        Matcher matcher = BASE64_TOKEN.matcher(text);
        while (matcher.find()) {
            String token = matcher.group();
            String decoded = tryDecode(token);
            Severity severity = Severity.WARN;
            String detail = "疑似 base64 长串";
            if (decoded != null && containsInjection(decoded)) {
                severity = Severity.ERROR;
                detail = "base64 解码后含注入指令";
            }
            findings.add(
                    Finding.of(
                            RULE_BASE64,
                            severity,
                            detail,
                            TextSearch.snippet(text, matcher.start(), matcher.start() + 16),
                            matcher.start(),
                            matcher.end()));
        }
    }

    private void collectPercent(String text, List<Finding> findings) {
        Matcher matcher = PERCENT_ENCODED.matcher(text);
        if (matcher.find()) {
            findings.add(
                    Finding.of(
                            RULE_PERCENT,
                            Severity.WARN,
                            "含百分号编码串",
                            TextSearch.snippet(text, matcher.start(), matcher.end()),
                            matcher.start(),
                            matcher.end()));
        }
    }

    private void collectControlChars(String text, List<Finding> findings) {
        Matcher matcher = CONTROL_CHAR.matcher(text);
        if (matcher.find()) {
            findings.add(
                    Finding.of(
                            RULE_CONTROL_CHAR,
                            Severity.WARN,
                            "含控制字符",
                            TextSearch.snippet(text, matcher.start(), matcher.end()),
                            matcher.start(),
                            matcher.end()));
        }
    }

    private static String tryDecode(String token) {
        try {
            byte[] bytes = Base64.getDecoder().decode(token);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean containsInjection(String decoded) {
        for (String marker : INJECTION_MARKERS) {
            if (TextSearch.containsIgnoreCase(decoded, marker)) {
                return true;
            }
        }
        return false;
    }
}
