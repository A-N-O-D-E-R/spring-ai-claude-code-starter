package com.anode.ai.claude;

public class ClaudeCodeException extends RuntimeException {
    public ClaudeCodeException(String message) { super(message); }
    public ClaudeCodeException(String message, Throwable cause) { super(message, cause); }
}
