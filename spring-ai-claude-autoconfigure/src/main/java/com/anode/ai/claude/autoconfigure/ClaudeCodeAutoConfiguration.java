package com.anode.ai.claude.autoconfigure;

import com.anode.ai.claude.ClaudeCodeAgent;
import com.anode.ai.claude.ClaudeCodeConfig;
import com.anode.ai.claude.ClaudeCodeEnvironmentProvider;
import com.anode.ai.claude.DefaultClaudeCodeAgent;
import com.anode.ai.claude.autoconfigure.properties.ClaudeCodeProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(ClaudeCodeAgent.class)
@EnableConfigurationProperties(ClaudeCodeProperties.class)
@ConditionalOnProperty(prefix = "spring.ai.claude-code", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ClaudeCodeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ClaudeCodeAgent claudeCodeAgent(ClaudeCodeProperties p, ObjectProvider<ObjectMapper> objectMapper,
                                           ObjectProvider<ClaudeCodeEnvironmentProvider> environmentProvider) {
        ClaudeCodeConfig config = new ClaudeCodeConfig(p.getExecutable(), p.getModel(), p.getWorkingDirectory(),
                p.getTimeout(), p.getPermissionMode(), p.getMaxTurns(), p.getSystemPrompt());
        return new DefaultClaudeCodeAgent(config, objectMapper.getIfAvailable(ObjectMapper::new),
                environmentProvider.getIfAvailable());
    }
}
