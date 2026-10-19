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
package io.github.nephets.agentguard.core.output;

import io.github.nephets.agentguard.core.Finding;
import io.github.nephets.agentguard.core.Severity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedactorTest {

    private final Redactor redactor = new Redactor();

    @Test
    void 手机号保留前三位后四位() {
        assertThat(redactor.redact("手机号 13812345678")).isEqualTo("手机号 138****5678");
    }

    @Test
    void 身份证号保留前六位后四位() {
        assertThat(redactor.redact("身份证 110101199003071234")).isEqualTo("身份证 110101********1234");
    }

    @Test
    void 银行卡号保留前四位后四位() {
        assertThat(redactor.redact("卡号 6222020200112345678")).isEqualTo("卡号 6222***********5678");
    }

    @Test
    void 邮箱保留首字母与域名() {
        assertThat(redactor.redact("邮箱 zhangsan@example.com")).isEqualTo("邮箱 z***@example.com");
    }

    @Test
    void 没有敏感信息时原样返回() {
        assertThat(redactor.redact("这是一段普通文本")).isEqualTo("这是一段普通文本");
        assertThat(redactor.redactWithFindings("这是一段普通文本").findings()).isEmpty();
    }

    @Test
    void 命中记录里不含原文() {
        Redaction redaction = redactor.redactWithFindings("手机号 13812345678");
        assertThat(redaction.findings()).hasSize(1);
        Finding finding = redaction.findings().get(0);
        assertThat(finding.ruleId()).isEqualTo("output.pii.phone");
        assertThat(finding.severity()).isEqualTo(Severity.WARN);
        assertThat(finding.evidence()).isEqualTo("138****5678");
        assertThat(finding.message()).contains("手机号");
    }
}
