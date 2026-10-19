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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PiiDetectorTest {

    private final PiiDetector detector = new PiiDetector();

    @Test
    void 手机号被识别() {
        List<PiiMatch> matches = detector.detect("联系我 13812345678 谢谢");
        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).type()).isEqualTo(PiiType.PHONE);
        assertThat(matches.get(0).value()).isEqualTo("13812345678");
        assertThat("联系我 13812345678 谢谢".substring(matches.get(0).start(), matches.get(0).end()))
                .isEqualTo("13812345678");
    }

    @Test
    void 身份证号被识别() {
        List<PiiMatch> matches = detector.detect("身份证 110101199003071234");
        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).type()).isEqualTo(PiiType.ID_CARD);
    }

    @Test
    void 邮箱被识别() {
        List<PiiMatch> matches = detector.detect("邮箱 zhangsan@example.com");
        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).type()).isEqualTo(PiiType.EMAIL);
        assertThat(matches.get(0).value()).isEqualTo("zhangsan@example.com");
    }

    @Test
    void 地址被识别() {
        List<PiiMatch> matches = detector.detect("来自 192.168.1.20 的请求");
        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).type()).isEqualTo(PiiType.IP);
    }

    @Test
    void 关闭类型后不再检测() {
        PiiDetector onlyPhone = PiiDetector.of(PiiType.PHONE);
        assertThat(onlyPhone.isEnabled(PiiType.PHONE)).isTrue();
        assertThat(onlyPhone.isEnabled(PiiType.EMAIL)).isFalse();
        assertThat(onlyPhone.detect("邮箱 a@b.com 手机 13812345678")).hasSize(1);
        assertThat(onlyPhone.enabledTypes()).containsExactly(PiiType.PHONE);
    }

    @Test
    void 全部关闭时为空() {
        PiiDetector none = new PiiDetector(List.of());
        assertThat(none.hasPii("13812345678")).isFalse();
    }
}
