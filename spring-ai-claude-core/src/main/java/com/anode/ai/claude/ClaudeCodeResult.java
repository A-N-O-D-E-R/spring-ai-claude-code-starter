package com.anode.ai.claude;

import com.fasterxml.jackson.databind.JsonNode;

public record ClaudeCodeResult(
        String text,
        String sessionId,
        String model,
        boolean isError,
        JsonNode raw) {}
