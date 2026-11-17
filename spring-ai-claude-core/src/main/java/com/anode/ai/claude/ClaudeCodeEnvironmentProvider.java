package com.anode.ai.claude;

import java.util.Map;

/**
 * Supplies extra environment variables for each Claude Code CLI process, evaluated at every run
 * (e.g. credentials entered at runtime). A {@code null} value removes the variable from the child process.
 */
@FunctionalInterface
public interface ClaudeCodeEnvironmentProvider {
    Map<String, String> environment();
}
