package com.anode.ai.claude.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.anode.ai.claude.ClaudeCodeAgent;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ClaudeCodeAutoConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ClaudeCodeAutoConfiguration.class, ClaudeCodeChatModelAutoConfiguration.class));

    @Test void registersAgentAndChatModel() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(ClaudeCodeAgent.class).hasSingleBean(ChatModel.class));
    }

    @Test void canDisableChatModel() {
        runner.withPropertyValues("spring.ai.claude-code.expose-chat-model=false")
                .run(ctx -> assertThat(ctx).hasSingleBean(ClaudeCodeAgent.class).doesNotHaveBean(ChatModel.class));
    }

    @Test void canDisableEverything() {
        runner.withPropertyValues("spring.ai.claude-code.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(ClaudeCodeAgent.class));
    }
}
