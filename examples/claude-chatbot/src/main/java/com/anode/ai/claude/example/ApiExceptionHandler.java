package com.anode.ai.claude.example;

import com.anode.ai.claude.ClaudeCodeException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(ClaudeCodeException.class)
    ResponseEntity<Map<String, String>> claude(ClaudeCodeException e) {
        log.error("Claude Code failed", e);
        String m = String.valueOf(e.getMessage());
        String error = m.contains("401") || m.contains("authenticate") || m.contains("Not logged in")
                ? "Claude token missing or invalid: set a valid token in the \"Claude token\" panel."
                : "Claude Code failed, check that the CLI is installed and authenticated.";
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", error));
    }
}
