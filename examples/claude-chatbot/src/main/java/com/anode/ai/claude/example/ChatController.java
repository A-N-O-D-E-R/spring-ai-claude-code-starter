package com.anode.ai.claude.example;

import java.util.UUID;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Stateful chat: the server keeps the history per conversationId (used by the bundled web page). */
@RestController
@RequestMapping("/api/chat")
class ChatController {

    record ChatRequest(String conversationId, String message) {}

    record ChatReply(String conversationId, String reply) {}

    private final ChatClient chatClient;

    ChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping
    ChatReply chat(@RequestBody ChatRequest request) {
        if (request.message() == null || request.message().isBlank())
            throw new IllegalArgumentException("message is required");
        String id = request.conversationId() == null || request.conversationId().isBlank()
                ? UUID.randomUUID().toString() : request.conversationId();
        String reply = chatClient.prompt()
                .user(request.message())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, id))
                .call()
                .content();
        return new ChatReply(id, reply);
    }
}
