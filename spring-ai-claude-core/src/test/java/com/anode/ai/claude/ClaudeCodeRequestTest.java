package com.anode.ai.claude;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ClaudeCodeRequestTest {
    @Test void rejectsBlankPrompt() {
        assertThrows(IllegalArgumentException.class, () -> ClaudeCodeRequest.of(" "));
    }
    @Test void createsSimpleRequest() {
        assertEquals("hello", ClaudeCodeRequest.of("hello").prompt());
    }
}
