package com.anode.ai.claude.autoconfigure;

import com.anode.ai.claude.ClaudeCodeAgent;
import com.anode.ai.claude.chat.ClaudeCodeChatModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@AutoConfigureAfter(ClaudeCodeAutoConfiguration.class)
@ConditionalOnClass({ChatModel.class, ClaudeCodeChatModel.class})
@ConditionalOnBean(ClaudeCodeAgent.class)
@ConditionalOnProperty(prefix = "spring.ai.claude-code", name = "expose-chat-model", havingValue = "true", matchIfMissing = true)
public class ClaudeCodeChatModelAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ChatModel.class)
    public ChatModel claudeCodeChatModel(ClaudeCodeAgent agent) {
        return new ClaudeCodeChatModel(agent);
    }
}
