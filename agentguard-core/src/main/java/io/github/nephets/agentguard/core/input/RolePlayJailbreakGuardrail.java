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

import java.util.ArrayList;
import java.util.List;

/** 检测角色扮演越狱：DAN / 不受限制的 AI 等明确标记判定为 ERROR， 单纯的角色扮演开头（pretend you are）只给 WARN。 */
public final class RolePlayJailbreakGuardrail extends PatternGuardrail {

    /** 规则标识。 */
    public static final String RULE_ID = "input.roleplay-jailbreak";

    /** 默认短语表。 */
    public static final List<Phrase> DEFAULT_PHRASES;

    static {
        List<Phrase> phrases = new ArrayList<>();
        phrases.add(Phrase.of("dan mode", Severity.ERROR));
        phrases.add(Phrase.of("do anything now", Severity.ERROR));
        phrases.add(Phrase.of("act as an unrestricted", Severity.ERROR));
        phrases.add(Phrase.of("you have no restrictions", Severity.ERROR));
        phrases.add(Phrase.of("ignore your guidelines", Severity.ERROR));
        phrases.add(Phrase.of("jailbreak", Severity.ERROR));
        phrases.add(Phrase.of("开发者模式", Severity.ERROR));
        phrases.add(Phrase.of("不受任何限制", Severity.ERROR));
        phrases.add(Phrase.of("无限制模式", Severity.ERROR));
        phrases.add(Phrase.of("pretend you are", Severity.WARN));
        phrases.add(Phrase.of("act as", Severity.WARN));
        phrases.add(Phrase.of("roleplay as", Severity.WARN));
        phrases.add(Phrase.of("扮演", Severity.WARN));
        phrases.add(Phrase.of("假设你是", Severity.WARN));
        DEFAULT_PHRASES = List.copyOf(phrases);
    }

    /** 使用默认短语表构造。 */
    public RolePlayJailbreakGuardrail() {
        this(DEFAULT_PHRASES);
    }

    /**
     * @param phrases 自定义短语表
     */
    public RolePlayJailbreakGuardrail(List<Phrase> phrases) {
        super(RULE_ID, "检测到角色扮演越狱", phrases);
    }
}
