package com.anode.ai.claude;

public interface ClaudeCodeAgent {
    ClaudeCodeResult run(ClaudeCodeRequest request);
    default ClaudeCodeResult run(String prompt) { return run(ClaudeCodeRequest.of(prompt)); }
}
