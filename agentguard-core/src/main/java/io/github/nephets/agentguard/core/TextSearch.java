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

/** 文本查找工具：忽略大小写定位短语，并截取证据片段。 */
public final class TextSearch {

    /** 证据片段的最大长度。 */
    public static final int MAX_SNIPPET = 48;

    private TextSearch() {}

    /**
     * 忽略大小写查找短语首次出现的位置。
     *
     * @param text 待查找文本
     * @param phrase 短语
     * @return 首次出现的下标，未找到返回 -1
     */
    public static int indexOfIgnoreCase(String text, String phrase) {
        if (text == null || phrase == null || phrase.isEmpty() || text.length() < phrase.length()) {
            return -1;
        }
        int limit = text.length() - phrase.length();
        for (int i = 0; i <= limit; i++) {
            if (text.regionMatches(true, i, phrase, 0, phrase.length())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 忽略大小写判断是否包含短语。
     *
     * @param text 待查找文本
     * @param phrase 短语
     * @return 包含返回 true
     */
    public static boolean containsIgnoreCase(String text, String phrase) {
        return indexOfIgnoreCase(text, phrase) >= 0;
    }

    /**
     * 截取证据片段，长度限制在 {@link #MAX_SNIPPET} 以内，并去掉换行。
     *
     * @param text 原文本
     * @param start 起始下标（含）
     * @param end 结束下标（不含）
     * @return 证据片段
     */
    public static String snippet(String text, int start, int end) {
        if (text == null || start < 0 || end <= start) {
            return "";
        }
        int from = Math.max(0, Math.min(start, text.length()));
        int to = Math.max(from, Math.min(end, text.length()));
        if (to - from > MAX_SNIPPET) {
            to = from + MAX_SNIPPET;
        }
        return text.substring(from, to).replace('\n', ' ').replace('\r', ' ');
    }
}
