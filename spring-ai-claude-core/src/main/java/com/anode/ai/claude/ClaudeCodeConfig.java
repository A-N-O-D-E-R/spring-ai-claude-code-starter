package com.anode.ai.claude;

import java.nio.file.Path;
import java.time.Duration;

/** Plain runtime settings for {@link DefaultClaudeCodeAgent}; independent of Spring. */
public record ClaudeCodeConfig(
        String executable,
        String model,
        Path workingDirectory,
        Duration timeout,
        String permissionMode,
        Integer maxTurns,
        String systemPrompt) {

    public static ClaudeCodeConfig defaults() {
        return new ClaudeCodeConfig("claude", "sonnet", Path.of("."), Duration.ofMinutes(10), "default", 30, null);
    }
}
