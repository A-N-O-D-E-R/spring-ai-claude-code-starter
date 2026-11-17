package com.anode.ai.claude.autoconfigure.properties;

import java.time.Duration;
import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spring.ai.claude-code")
public class ClaudeCodeProperties {
    private boolean enabled = true;
    private String executable = "claude";
    private String model = "sonnet";
    private Path workingDirectory = Path.of(".");
    private Duration timeout = Duration.ofMinutes(10);
    private String permissionMode = "default";
    private Integer maxTurns = 30;
    private String systemPrompt;
    private boolean exposeChatModel = true;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { enabled = v; }
    public String getExecutable() { return executable; }
    public void setExecutable(String v) { executable = v; }
    public String getModel() { return model; }
    public void setModel(String v) { model = v; }
    public Path getWorkingDirectory() { return workingDirectory; }
    public void setWorkingDirectory(Path v) { workingDirectory = v; }
    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration v) { timeout = v; }
    public String getPermissionMode() { return permissionMode; }
    public void setPermissionMode(String v) { permissionMode = v; }
    public Integer getMaxTurns() { return maxTurns; }
    public void setMaxTurns(Integer v) { maxTurns = v; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String v) { systemPrompt = v; }
    public boolean isExposeChatModel() { return exposeChatModel; }
    public void setExposeChatModel(boolean v) { exposeChatModel = v; }
}
