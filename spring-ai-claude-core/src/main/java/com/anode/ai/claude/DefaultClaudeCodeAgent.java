package com.anode.ai.claude;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DefaultClaudeCodeAgent implements ClaudeCodeAgent {
    private static final Logger log = LoggerFactory.getLogger(DefaultClaudeCodeAgent.class);
    private final ClaudeCodeConfig config;
    private final ObjectMapper mapper;

    private final ClaudeCodeEnvironmentProvider environmentProvider;

    public DefaultClaudeCodeAgent(ClaudeCodeConfig config, ObjectMapper mapper) {
        this(config, mapper, null);
    }

    public DefaultClaudeCodeAgent(ClaudeCodeConfig config, ObjectMapper mapper,
                                  ClaudeCodeEnvironmentProvider environmentProvider) {
        this.config = config;
        this.mapper = mapper;
        this.environmentProvider = environmentProvider;
    }

    private static String mask(String value) {
        if (value == null) return "<not set>";
        if (value.isEmpty()) return "<set but EMPTY>";
        return value.substring(0, Math.min(12, value.length())) + "... (length=" + value.length() + ")";
    }

    // Diagnostic: shows which credentials the spawned CLI inherits (masked, never the full secret).
    private void logAuthEnv(java.util.Map<String, String> env) {
        String apiKey = env.get("ANTHROPIC_API_KEY");
        String oauth = env.get("CLAUDE_CODE_OAUTH_TOKEN");
        log.info("Claude auth env: ANTHROPIC_API_KEY={}, CLAUDE_CODE_OAUTH_TOKEN={}", mask(apiKey), mask(oauth));
        String used = apiKey != null && !apiKey.isEmpty() ? "ANTHROPIC_API_KEY (takes precedence)"
                : oauth != null && !oauth.isEmpty() ? "CLAUDE_CODE_OAUTH_TOKEN"
                : "none (CLI falls back to ~/.claude credentials)";
        log.info("Claude auth likely used: {}", used);
    }

    // With --output-format json the CLI reports failures (e.g. "Not logged in") on stdout, not stderr.
    private String describeFailure(String stdout, String stderr) {
        StringBuilder sb = new StringBuilder();
        if (stderr != null && !stderr.isBlank()) sb.append(stderr.strip());
        if (stdout != null && !stdout.isBlank()) {
            String message = stdout.strip();
            try {
                JsonNode json = mapper.readTree(stdout);
                String result = json.path("result").asText("");
                if (!result.isBlank()) message = result;
            } catch (Exception ignored) {
                // not JSON: keep the raw output
            }
            if (sb.length() > 0) sb.append(" | ");
            sb.append(message.length() > 2000 ? message.substring(0, 2000) + "..." : message);
        }
        return sb.length() == 0 ? "(no output)" : sb.toString();
    }

    @Override
    public ClaudeCodeResult run(ClaudeCodeRequest request) {
        Path cwd = request.workingDirectory() != null ? request.workingDirectory() : config.workingDirectory();
        cwd = cwd.toAbsolutePath().normalize();
        if (!Files.isDirectory(cwd)) throw new ClaudeCodeException("Claude Code working directory does not exist: " + cwd);

        List<String> command = new ArrayList<>();
        command.add(config.executable());
        command.add("-p");
        command.add(request.prompt());
        command.add("--output-format");
        command.add("json");
        command.add("--permission-mode");
        command.add(config.permissionMode());

        String model = request.model() != null ? request.model() : config.model();
        if (model != null && !model.isBlank()) { command.add("--model"); command.add(model); }

        String systemPrompt = request.systemPrompt() != null ? request.systemPrompt() : config.systemPrompt();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            command.add("--append-system-prompt");
            command.add(systemPrompt);
        }
        if (config.maxTurns() != null && config.maxTurns() > 0) {
            command.add("--max-turns");
            command.add(config.maxTurns().toString());
        }
        if (request.sessionId() != null && !request.sessionId().isBlank()) {
            command.add("--resume");
            command.add(request.sessionId());
        }

        Process process = null;
        try {
            ProcessBuilder builder = new ProcessBuilder(command).directory(cwd.toFile());
            builder.redirectErrorStream(false);
            if (environmentProvider != null) {
                var extra = environmentProvider.environment();
                if (extra != null) extra.forEach((k, v) -> {
                    if (v == null) builder.environment().remove(k);
                    else builder.environment().put(k, v);
                });
            }
            logAuthEnv(builder.environment());
            process = builder.start();
            // The prompt is passed as an argument; close stdin so the CLI doesn't wait for piped input.
            process.getOutputStream().close();
            Process running = process;
            var stderrFuture = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try { return new String(running.getErrorStream().readAllBytes(), StandardCharsets.UTF_8); }
                catch (IOException e) { return "Could not read Claude Code stderr: " + e.getMessage(); }
            });
            boolean finished = process.waitFor(config.timeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new ClaudeCodeException("Claude Code timed out after " + config.timeout());
            }
            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = stderrFuture.get(5, TimeUnit.SECONDS);
            if (process.exitValue() != 0) {
                throw new ClaudeCodeException("Claude Code exited with code " + process.exitValue() + ": "
                        + describeFailure(stdout, stderr));
            }
            JsonNode json = mapper.readTree(stdout);
            String text = json.path("result").asText("");
            if (text.isBlank() && json.has("content")) text = json.path("content").asText("");
            return new ClaudeCodeResult(text, json.path("session_id").asText(null),
                    json.path("model").asText(model), json.path("is_error").asBoolean(false), json);
        } catch (ClaudeCodeException e) {
            throw e;
        } catch (Exception e) {
            if (process != null) process.destroyForcibly();
            throw new ClaudeCodeException("Failed to execute Claude Code. Check that the CLI is installed and authenticated.", e);
        }
    }
}
