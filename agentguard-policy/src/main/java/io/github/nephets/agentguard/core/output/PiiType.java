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

/** 支持的个人信息类型。 */
public enum PiiType {

    /** 手机号（11 位中国大陆号码）。 */
    PHONE,

    /** 身份证号（18 位）。 */
    ID_CARD,

    /** 银行卡号（16 到 19 位）。 */
    BANK_CARD,

    /** 电子邮箱。 */
    EMAIL,

    /** IPv4 地址。 */
    IP
}
