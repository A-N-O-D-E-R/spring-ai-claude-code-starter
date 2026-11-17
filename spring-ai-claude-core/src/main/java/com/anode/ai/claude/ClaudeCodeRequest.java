package com.anode.ai.claude;

import java.nio.file.Path;

public record ClaudeCodeRequest(
        String prompt,
        String sessionId,
        Path workingDirectory,
        String model,
        String systemPrompt) {
    public ClaudeCodeRequest {
        if (prompt == null || prompt.isBlank()) throw new IllegalArgumentException("prompt must not be blank");
    }
    public static ClaudeCodeRequest of(String prompt) {
        return new ClaudeCodeRequest(prompt, null, null, null, null);
    }
}
