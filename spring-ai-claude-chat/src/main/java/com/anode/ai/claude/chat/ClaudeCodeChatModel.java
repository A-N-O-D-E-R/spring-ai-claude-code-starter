package com.anode.ai.claude.chat;

import com.anode.ai.claude.ClaudeCodeAgent;
import com.anode.ai.claude.ClaudeCodeResult;

import java.util.stream.Collectors;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Text-only Spring AI adapter. Claude Code owns its native tools and agent loop;
 * this adapter intentionally does not translate Spring AI ToolCallbacks into Claude tools.
 */
public final class ClaudeCodeChatModel implements ChatModel {
    private final ClaudeCodeAgent agent;
    public ClaudeCodeChatModel(ClaudeCodeAgent agent) { this.agent = agent; }

    @Override
    public ChatResponse call(Prompt prompt) {
        String conversation = prompt.getInstructions().stream()
                .map(ClaudeCodeChatModel::format)
                .collect(Collectors.joining("\n\n"));
        ClaudeCodeResult result = agent.run(conversation);
        return new ChatResponse(java.util.List.of(new Generation(new AssistantMessage(result.text()))));
    }

    private static String format(Message message) {
        String role = switch (message.getMessageType()) {
            case SYSTEM -> "System";
            case USER -> "User";
            case ASSISTANT -> "Assistant";
            default -> message.getMessageType().name();
        };
        return role + ":\n" + message.getText();
    }
}
