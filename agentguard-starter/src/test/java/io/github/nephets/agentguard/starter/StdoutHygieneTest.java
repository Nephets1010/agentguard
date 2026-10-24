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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * 输出卫生守卫：主源码不得直接写标准流。
 *
 * <p>把「src/main/java 里没有 System.out / err / console，也没有 printStackTrace」从一次性的 grep 结论
 * 固化成每次构建都跑的机器判据。只扫主源码，不扫测试源码：守卫自己就住在 src/test 下，把测试源码也纳入扫描 就得给它开白名单，那是一整类一改就坏的脆弱断言。
 *
 * <p>禁模式逐条罗列，不用 {@code System\.} 兜底。兜底会把 {@code System.currentTimeMillis}、{@code
 * System.nanoTime}、{@code System.exit}、{@code System.getProperty}、{@code System.getenv} 这些正常用法
 * 一次全报成违规，逼人去改没问题的代码。前两条用例就是为此存在的：一条保证禁模式还咬得住违例样例，一条保证 它们咬不住合法样例。没有这两条闸，正则被改坏后守卫会静默永远绿 ——
 * 看着在守，实际什么都没查。
 *
 * <p>为什么没有白名单：本仓主源码里不存在「只能出 stdout」的机器解析出口。原先唯一的 stdout 来源是 sample 模块的 demo 叙述，CI
 * 不跑它、不比对它的输出，README 里没有期望输出块，测试也没有对 stdout 做断言，所以 它是给人读的叙述，可以整体并入 SLF4J。规则因此可以没有例外。
 */
class StdoutHygieneTest {

    /** 需要扫描的 5 个模块目录名。 */
    private static final List<String> MODULES =
            List.of(
                    "agentguard-contract",
                    "agentguard-core",
                    "agentguard-policy",
                    "agentguard-starter",
                    "agentguard-sample");

    /**
     * 判定仓库根的标志路径：三个模块的主源码目录必须同时存在。
     *
     * <p>取 contract / core / policy 而不取 sample：前三个是被 CI 的 architecture-boundaries job
     * 显式引用的模块，结构最稳；sample 可能随示例调整而改名。
     */
    private static final List<String> REPO_ROOT_MARKERS =
            List.of(
                    "agentguard-contract/src/main/java",
                    "agentguard-core/src/main/java",
                    "agentguard-policy/src/main/java");

    /** 扫描文件总数下限：现状 48 个，留 4 个余量。低于此值说明漏扫，守卫会空转。 */
    private static final int MIN_TOTAL_FILES = 44;

    /** 6 条禁模式，改完后 5 个模块的 src/main/java 命中为 0。 */
    private static final List<Pattern> FORBIDDEN_PATTERNS =
            List.of(
                    Pattern.compile("System\\s*\\.\\s*out\\b"),
                    Pattern.compile("System\\s*\\.\\s*err\\b"),
                    Pattern.compile("System\\s*\\.\\s*console\\b"),
                    Pattern.compile("System\\s*\\.\\s*setOut\\b"),
                    Pattern.compile("System\\s*\\.\\s*setErr\\b"),
                    Pattern.compile("printStackTrace\\s*\\("));

    /** 与禁模式一一对应的违例样例，用于负向自检：正则被改坏时这里先红。 */
    private static final List<String> FORBIDDEN_SAMPLES =
            List.of(
                    "System.out.println(\"x\");",
                    "System.err.println(\"x\");",
                    "System.console().printf(\"x\");",
                    "System.setOut(out);",
                    "System.setErr(err);",
                    "e.printStackTrace();");

    /** 合法代码样例：一条都不许命中，用来挡住把正则放宽成 {@code System\.} 之类的兜底写法。 */
    private static final List<String> ALLOWED_SAMPLES =
            List.of(
                    "log.info(\"a={}\", b);",
                    "long t0 = System.nanoTime();",
                    "long now = System.currentTimeMillis();",
                    "System.exit(1);",
                    "String os = System.getProperty(\"os.name\");",
                    "String home = System.getenv(\"HOME\");",
                    "String sep = System.lineSeparator();");

    @Test
    void 每条禁模式都咬得住自己的违例样例() {
        assertThat(FORBIDDEN_SAMPLES)
                .as("禁模式与违例样例必须一一对应，改动其中一边时另一边要跟着改")
                .hasSameSizeAs(FORBIDDEN_PATTERNS);
        for (int i = 0; i < FORBIDDEN_PATTERNS.size(); i++) {
            int index = i;
            assertThat(FORBIDDEN_PATTERNS.get(index).matcher(FORBIDDEN_SAMPLES.get(index)).find())
                    .as(
                            "第 %d 条禁模式 %s 没有命中样例 %s：正则可能被改坏，守卫将静默失效",
                            index + 1,
                            FORBIDDEN_PATTERNS.get(index).pattern(),
                            FORBIDDEN_SAMPLES.get(index))
                    .isTrue();
        }
    }

    @Test
    void 禁模式不得误判合法代码() {
        List<String> misfires = new ArrayList<>();
        for (Pattern pattern : FORBIDDEN_PATTERNS) {
            for (String sample : ALLOWED_SAMPLES) {
                if (pattern.matcher(sample).find()) {
                    misfires.add(pattern.pattern() + " 误判了合法代码 " + sample);
                }
            }
        }
        assertThat(misfires).as("正则过宽，会逼人去改没问题的代码").isEmpty();
    }

    @Test
    void 五个模块的主源码里不得出现任何一条禁模式() throws IOException {
        Path repoRoot = locateRepoRoot();
        Map<String, List<Path>> perModule = new LinkedHashMap<>();
        for (String module : MODULES) {
            perModule.put(module, mainJavaFiles(repoRoot.resolve(module)));
        }
        扫描覆盖度足够(perModule);

        List<String> violations = new ArrayList<>();
        for (Map.Entry<String, List<Path>> entry : perModule.entrySet()) {
            for (Path file : entry.getValue()) {
                violations.addAll(scan(repoRoot, file));
            }
        }
        if (!violations.isEmpty()) {
            fail(
                    "安全组件与 demo 代码不得直接写标准流，命中 "
                            + violations.size()
                            + " 处：\n"
                            + String.join("\n", violations)
                            + "\n请改用 SLF4J：LoggerFactory.getLogger(Xxx.class)。");
        }
    }

    /**
     * 定位仓库根：从当前工作目录逐级向上，找第一个同时含 {@link #REPO_ROOT_MARKERS} 全部标志路径的目录。
     *
     * <p>标志只认目录结构，不认 {@code .git}：构建一定发生在无 .git 的 ASCII 副本里（中文路径跑不了 Maven），把 .git
     * 写进判定条件，守卫就会在唯一真正跑构建的地方必然失败，门禁形同虚设。
     *
     * <p>不写死 {@code ..}：surefire 的工作目录是本模块目录，写死的父目录会随模块不同指到别处；扫不到任何 文件时的零违规是一条骗人的绿。
     *
     * <p>三个标志是合取条件，遍历到第一个满足的目录就返回，本身已经确定，不需要再叠一层层数上限。
     */
    private static Path locateRepoRoot() {
        Path start = Path.of("").toAbsolutePath().normalize();
        Path cursor = start;
        while (cursor != null) {
            boolean allPresent = true;
            for (String marker : REPO_ROOT_MARKERS) {
                allPresent &= Files.isDirectory(cursor.resolve(marker));
            }
            if (allPresent) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        return fail(
                "定位不到仓库根：从 "
                        + start
                        + " 逐级向上直到文件系统顶层，都没有同时含 "
                        + String.join("、", REPO_ROOT_MARKERS)
                        + " 这三个目录的目录。");
    }

    /** 覆盖度闸：每个模块的 src/main/java 都得扫到至少 1 个 .java，总数不低于下限。 */
    private static void 扫描覆盖度足够(Map<String, List<Path>> perModule) {
        List<String> problems = new ArrayList<>();
        int total = 0;
        for (Map.Entry<String, List<Path>> entry : perModule.entrySet()) {
            int count = entry.getValue().size();
            total += count;
            if (count < 1) {
                problems.add(entry.getKey() + "：扫到 0 个 .java（src/main/java 可能被改名、移动或删除）");
            }
        }
        if (total < MIN_TOTAL_FILES) {
            problems.add("扫描总数 " + total + " 低于下限 " + MIN_TOTAL_FILES + "，疑似漏扫");
        }
        assertThat(problems).as("扫描覆盖度不足，守卫不能算通过。各模块实际文件数：%s", describe(perModule)).isEmpty();
    }

    /** 收集一个模块 src/main/java 下的 .java。 */
    private static List<Path> mainJavaFiles(Path moduleDir) throws IOException {
        Path sourceRoot = moduleDir.resolve("src").resolve("main").resolve("java");
        if (!Files.isDirectory(sourceRoot)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(sourceRoot)) {
            return walk.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .filter(StdoutHygieneTest::isSourceFile)
                    .sorted()
                    .toList();
        }
    }

    /** 是否是要扫的源码文件：排除构建产物目录，且目录段统一按正斜杠判断，免得 Windows 反斜杠让判断失效。 */
    private static boolean isSourceFile(Path path) {
        String unix = path.toString().replace('\\', '/');
        return !unix.contains("/target/") && !unix.contains("/.git/") && !unix.contains("/.mvn/");
    }

    /** 逐行匹配禁模式，命中信息格式为「文件相对路径:行号: 原行内容」。 */
    private static List<String> scan(Path repoRoot, Path file) throws IOException {
        // 显式 UTF-8：Windows 默认 GBK 会把中文注释读成乱码，行号与内容都对不上。
        String body = Files.readString(file, StandardCharsets.UTF_8);
        String[] lines = body.split("\\R", -1);
        String relative = repoRoot.relativize(file).toString().replace('\\', '/');
        List<String> hits = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            for (Pattern pattern : FORBIDDEN_PATTERNS) {
                if (pattern.matcher(line).find()) {
                    hits.add(relative + ":" + (i + 1) + ": " + line.trim());
                    break;
                }
            }
        }
        return hits;
    }

    private static String describe(Map<String, List<Path>> perModule) {
        StringBuilder text = new StringBuilder();
        for (Map.Entry<String, List<Path>> entry : perModule.entrySet()) {
            if (text.length() > 0) {
                text.append("，");
            }
            text.append(entry.getKey()).append("=").append(entry.getValue().size());
        }
        return text.toString();
    }
}
