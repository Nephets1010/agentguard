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

import io.github.nephets.agentguard.core.Severity;

import java.util.List;

/** 检测要求绕过权限审批、删库、执行任意命令的输入。 */
public final class ToolAbuseGuardrail extends PatternGuardrail {

    /** 规则标识。 */
    public static final String RULE_ID = "input.tool-abuse";

    /** 默认短语表。 */
    public static final List<Phrase> DEFAULT_PHRASES =
            List.of(
                    Phrase.of("bypass permission", Severity.ERROR),
                    Phrase.of("bypass authorization", Severity.ERROR),
                    Phrase.of("绕过权限", Severity.ERROR),
                    Phrase.of("绕过审批", Severity.ERROR),
                    Phrase.of("without approval", Severity.ERROR),
                    Phrase.of("skip approval", Severity.ERROR),
                    Phrase.of("drop table", Severity.ERROR),
                    Phrase.of("delete from", Severity.ERROR),
                    Phrase.of("rm -rf", Severity.ERROR),
                    Phrase.of("format c:", Severity.ERROR),
                    Phrase.of("删库", Severity.ERROR),
                    Phrase.of("删除数据库", Severity.ERROR),
                    Phrase.of("run arbitrary command", Severity.ERROR),
                    Phrase.of("执行任意命令", Severity.ERROR),
                    Phrase.of("exfiltrate", Severity.ERROR),
                    Phrase.of("disable logging", Severity.ERROR),
                    Phrase.of("关闭审计", Severity.ERROR),
                    Phrase.of("泄露密钥", Severity.ERROR),
                    Phrase.of("ignore safety", Severity.ERROR),
                    Phrase.of("run as root", Severity.WARN),
                    Phrase.of("使用管理员权限", Severity.WARN));

    /** 使用默认短语表构造。 */
    public ToolAbuseGuardrail() {
        this(DEFAULT_PHRASES);
    }

    /**
     * @param phrases 自定义短语表
     */
    public ToolAbuseGuardrail(List<Phrase> phrases) {
        super(RULE_ID, "检测到危险操作指令", phrases);
    }
}
