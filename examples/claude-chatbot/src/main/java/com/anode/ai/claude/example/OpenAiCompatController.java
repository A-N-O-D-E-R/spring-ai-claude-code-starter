package com.anode.ai.claude.example;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal OpenAI-compatible, stateless endpoint (same contract as atsia-igor's
 * /api/v1/chat/completions) so Open WebUI or any OpenAI client can use Claude Code as a backend.
 * The client resends the whole history on every call. Streaming is emulated: one content chunk then [DONE].
 */
@RestController
@RequestMapping("/api/v1")
class OpenAiCompatController {

    static final String MODEL_ID = "claude-code";

    record ChatMessage(String role, Object content) {}

    record CompletionRequest(String model, List<ChatMessage> messages, Boolean stream) {}

    private final ChatModel chatModel;

    OpenAiCompatController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @GetMapping("/models")
    Map<String, Object> models() {
        return Map.of("object", "list", "data", List.of(Map.of("id", MODEL_ID, "object", "model", "owned_by", "claude-code")));
    }

    @PostMapping("/chat/completions")
    ResponseEntity<?> completion(@RequestBody CompletionRequest request) {
        if (request.messages() == null || request.messages().isEmpty())
            throw new IllegalArgumentException("At least one message is required");

        List<Message> messages = new ArrayList<>();
        for (ChatMessage m : request.messages()) {
            String text = textOf(m.content());
            switch (m.role() == null ? "user" : m.role()) {
                case "system" -> messages.add(new SystemMessage(text));
                case "assistant" -> messages.add(new AssistantMessage(text));
                default -> messages.add(new UserMessage(text));
            }
        }
        String answer = chatModel.call(new Prompt(messages)).getResult().getOutput().getText();
        String id = "chatcmpl-" + UUID.randomUUID();
        long created = System.currentTimeMillis() / 1000;

        if (Boolean.TRUE.equals(request.stream())) {
            String body = "data: " + json(chunk(id, created, Map.of("role", "assistant", "content", answer), null)) + "\n\n"
                    + "data: " + json(chunk(id, created, Map.of(), "stop")) + "\n\n"
                    + "data: [DONE]\n\n";
            return ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(body);
        }

        Map<String, Object> choice = new LinkedHashMap<>();
        choice.put("index", 0);
        choice.put("message", Map.of("role", "assistant", "content", answer));
        choice.put("finish_reason", "stop");
        return ResponseEntity.ok(Map.of("id", id, "object", "chat.completion", "created", created,
                "model", MODEL_ID, "choices", List.of(choice)));
    }

    private static Map<String, Object> chunk(String id, long created, Map<String, Object> delta, String finish) {
        Map<String, Object> choice = new LinkedHashMap<>();
        choice.put("index", 0);
        choice.put("delta", delta);
        choice.put("finish_reason", finish);
        return Map.of("id", id, "object", "chat.completion.chunk", "created", created,
                "model", MODEL_ID, "choices", List.of(choice));
    }

    // OpenAI "content" is either a string or an array of parts ({type:"text", text:"..."}).
    @SuppressWarnings("unchecked")
    private static String textOf(Object content) {
        if (content == null) return "";
        if (content instanceof String s) return s;
        if (content instanceof List<?> parts) {
            StringBuilder sb = new StringBuilder();
            for (Object part : parts) {
                if (part instanceof Map<?, ?> map && "text".equals(map.get("type")) && map.get("text") != null)
                    sb.append(map.get("text"));
            }
            return sb.toString();
        }
        return content.toString();
    }

    private static String json(Map<String, Object> map) {
        return new tools.jackson.databind.json.JsonMapper().writeValueAsString(map);
    }
}
