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
package io.github.nephets.agentguard.sample;

import io.github.nephets.agentguard.core.tool.ToolPolicy;
import io.github.nephets.agentguard.core.tool.ToolRisk;
import io.github.nephets.agentguard.core.tool.ToolSpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Set;

/** 示例应用：注册几个示例工具，启动后跑一遍演示。 */
@SpringBootApplication
public class AgentguardSampleApplication {

    private static final Logger log = LoggerFactory.getLogger(AgentguardSampleApplication.class);

    public static void main(String[] args) {
        log.info("starting agentguard sample application");
        SpringApplication.run(AgentguardSampleApplication.class, args);
    }

    /**
     * 示例工具策略，会覆盖 starter 里的默认（空）策略。
     *
     * @return 工具策略
     */
    @Bean
    public ToolPolicy toolPolicy() {
        ToolPolicy policy =
                ToolPolicy.of(
                        ToolSpec.builder("search.doc").risk(ToolRisk.READ).build(),
                        ToolSpec.builder("db.write")
                                .risk(ToolRisk.WRITE)
                                .allowedRoles(Set.of("admin"))
                                .maxCallsPerRun(3)
                                .build(),
                        ToolSpec.builder("db.drop")
                                .risk(ToolRisk.DESTRUCTIVE)
                                .allowedRoles(Set.of("admin"))
                                .build());
        log.info("registered sample ToolPolicy: {}", policy.toolNames());
        return policy;
    }

    /**
     * @param demo 演示流程
     * @return 启动后执行演示的 runner
     */
    @Bean
    public CommandLineRunner demoRunner(AgentguardDemo demo) {
        log.info("sample demo runner registered");
        return args -> demo.run();
    }
}
