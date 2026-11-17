package com.anode.ai.claude.example;

import com.anode.ai.claude.ClaudeCodeEnvironmentProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Holds the Claude credential entered through the UI and injects it into every CLI process.
 * Persisted (owner-only file) so it survives restarts; the value is never returned or logged in full.
 */
@Component
class CredentialStore implements ClaudeCodeEnvironmentProvider {
    private static final Logger log = LoggerFactory.getLogger(CredentialStore.class);

    private final Path file;
    private volatile String token;

    CredentialStore(@Value("${chatbot.token-file:${CLAUDE_CONFIG_DIR:${user.home}/.claude}/chatbot-token}") Path file) {
        this.file = file;
        try {
            if (Files.isRegularFile(file)) token = Files.readString(file, StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            log.warn("Could not read stored token from {}: {}", file, e.getMessage());
        }
    }

    boolean isSet() { return token != null && !token.isEmpty(); }

    /** Console API keys use ANTHROPIC_API_KEY; everything else (OAuth tokens) CLAUDE_CODE_OAUTH_TOKEN. */
    String kind() {
        if (!isSet()) return null;
        return token.startsWith("sk-ant-api") ? "api_key" : "oauth_token";
    }

    String hint() {
        return !isSet() ? null : token.substring(0, Math.min(10, token.length())) + "...";
    }

    synchronized void set(String value) {
        String cleaned = value.strip();
        if (cleaned.length() >= 2 && (cleaned.startsWith("'") && cleaned.endsWith("'")
                || cleaned.startsWith("\"") && cleaned.endsWith("\""))) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).strip();
        }
        if (cleaned.isEmpty() || cleaned.chars().anyMatch(Character::isWhitespace))
            throw new IllegalArgumentException("token must be a single value without spaces or line breaks");
        this.token = cleaned;
        persist();
    }

    synchronized void clear() {
        this.token = null;
        try { Files.deleteIfExists(file); } catch (IOException e) { log.warn("Could not delete {}: {}", file, e.getMessage()); }
    }

    private void persist() {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            if (!Files.exists(file)) {
                try {
                    Files.createFile(file, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
                } catch (UnsupportedOperationException e) {
                    Files.createFile(file);
                }
            }
            Files.writeString(file, token, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Token kept in memory only, could not persist to {}: {}", file, e.getMessage());
        }
    }

    @Override
    public Map<String, String> environment() {
        Map<String, String> env = new HashMap<>();
        if (!isSet()) return env; // nothing entered: the CLI falls back to the inherited environment / ~/.claude login
        boolean apiKey = "api_key".equals(kind());
        env.put("ANTHROPIC_API_KEY", apiKey ? token : null);
        env.put("CLAUDE_CODE_OAUTH_TOKEN", apiKey ? null : token);
        return env;
    }
}
