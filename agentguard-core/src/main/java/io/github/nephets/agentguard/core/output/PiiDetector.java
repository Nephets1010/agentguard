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

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 用正则检测文本里的手机号、身份证号、银行卡号、邮箱与 IPv4 地址。
 *
 * <p>命中区间重叠时保留先匹配到的类型，顺序为邮箱、身份证、银行卡、手机号、IP。
 */
public final class PiiDetector {

    private static final Pattern EMAIL =
            Pattern.compile("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}");

    private static final Pattern ID_CARD = Pattern.compile("(?<![0-9])[0-9]{17}[0-9Xx](?![0-9])");

    private static final Pattern BANK_CARD = Pattern.compile("(?<![0-9])[0-9]{16,19}(?![0-9])");

    private static final Pattern PHONE = Pattern.compile("(?<![0-9])1[3-9][0-9]{9}(?![0-9])");

    private static final Pattern IPV4 =
            Pattern.compile("(?<![0-9.])(?:[0-9]{1,3}\\.){3}[0-9]{1,3}(?![0-9.])");

    private final Set<PiiType> enabled;

    /** 检测全部类型。 */
    public PiiDetector() {
        this(EnumSet.allOf(PiiType.class));
    }

    /**
     * @param types 需要检测的类型，为空时表示全部关闭
     */
    public PiiDetector(Collection<PiiType> types) {
        if (types == null || types.isEmpty()) {
            this.enabled = EnumSet.noneOf(PiiType.class);
        } else {
            this.enabled = EnumSet.copyOf(types);
        }
    }

    /**
     * @param types 需要检测的类型
     * @return 只检测指定类型的检测器
     */
    public static PiiDetector of(PiiType... types) {
        if (types == null) {
            return new PiiDetector(List.of());
        }
        return new PiiDetector(List.of(types));
    }

    /**
     * @param type 类型
     * @return 该类型是否参与检测
     */
    public boolean isEnabled(PiiType type) {
        return enabled.contains(type);
    }

    /**
     * @return 参与检测的类型
     */
    public Set<PiiType> enabledTypes() {
        return Set.copyOf(enabled);
    }

    /**
     * @param text 待检测文本
     * @return 全部命中，按在文本中的位置升序
     */
    public List<PiiMatch> detect(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        List<PiiMatch> matches = new ArrayList<>();
        collect(text, EMAIL, PiiType.EMAIL, matches);
        collect(text, ID_CARD, PiiType.ID_CARD, matches);
        collect(text, BANK_CARD, PiiType.BANK_CARD, matches);
        collect(text, PHONE, PiiType.PHONE, matches);
        collect(text, IPV4, PiiType.IP, matches);
        matches.sort((a, b) -> Integer.compare(a.start(), b.start()));
        return List.copyOf(matches);
    }

    /**
     * @param text 待检测文本
     * @return 是否含任意已开启类型的信息
     */
    public boolean hasPii(String text) {
        return !detect(text).isEmpty();
    }

    private void collect(String text, Pattern pattern, PiiType type, List<PiiMatch> matches) {
        if (!enabled.contains(type)) {
            return;
        }
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String value = matcher.group();
            if (type == PiiType.IP && !isIpv4(value)) {
                continue;
            }
            if (overlaps(matches, matcher.start(), matcher.end())) {
                continue;
            }
            matches.add(new PiiMatch(type, matcher.start(), matcher.end(), value));
        }
    }

    private static boolean overlaps(List<PiiMatch> matches, int start, int end) {
        for (PiiMatch match : matches) {
            if (start < match.end() && end > match.start()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isIpv4(String value) {
        String[] parts = value.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            int octet;
            try {
                octet = Integer.parseInt(part);
            } catch (NumberFormatException e) {
                return false;
            }
            if (octet < 0 || octet > 255) {
                return false;
            }
        }
        return true;
    }
}
