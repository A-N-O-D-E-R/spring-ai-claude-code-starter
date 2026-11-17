package com.anode.ai.claude.example;

import static org.assertj.core.api.Assertions.assertThat;

import com.anode.ai.claude.ClaudeCodeAgent;
import com.anode.ai.claude.ClaudeCodeRequest;
import com.anode.ai.claude.ClaudeCodeResult;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(ChatbotApplicationTest.StubAgentConfig.class)
class ChatbotApplicationTest {

    static final List<String> PROMPTS = new ArrayList<>();

    @TestConfiguration
    static class StubAgentConfig {
        @Bean
        ClaudeCodeAgent claudeCodeAgent() {
            return new ClaudeCodeAgent() {
                @Override public ClaudeCodeResult run(ClaudeCodeRequest request) {
                    PROMPTS.add(request.prompt());
                    return new ClaudeCodeResult("stub-reply", "s1", "stub", false, null);
                }
            };
        }
    }

    @Autowired ChatClient chatClient;
    @Autowired ChatController controller;

    @Test void conversationKeepsHistoryPerId() {
        var first = controller.chat(new ChatController.ChatRequest(null, "My name is Ada"));
        controller.chat(new ChatController.ChatRequest(first.conversationId(), "What is my name?"));

        assertThat(first.reply()).isEqualTo("stub-reply");
        assertThat(PROMPTS.getLast()).contains("My name is Ada").contains("What is my name?");
    }

    @Test void otherConversationDoesNotLeak() {
        controller.chat(new ChatController.ChatRequest(null, "secret-one"));
        controller.chat(new ChatController.ChatRequest(null, "hello"));
        assertThat(PROMPTS.getLast()).doesNotContain("secret-one");
    }
}
