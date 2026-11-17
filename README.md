# Spring AI Claude Code Starter (prototype)

A small Spring Boot auto-configuration that launches the locally installed Claude Code CLI and exposes:
- `ClaudeCodeAgent` for native Claude Code agent runs (filesystem, terminal, permissions, CLAUDE.md, MCP, etc.).
- `ChatModel` adapter so Spring AI `ChatClient` can use Claude Code for text-oriented prompts.

## Requirements
- Java 21+
- Maven 3.9+
- Spring Boot 4.0.x / Spring AI 2.0.x (versions are properties in `pom.xml`)
- Claude Code CLI installed and authenticated on the machine/container running the app.

Install/authenticate Claude Code separately, then verify `claude --version` and `claude auth status`.

## Modules

| Module | Purpose |
|---|---|
| `spring-ai-claude-dependencies` | BOM (imports Spring AI BOM + manages all module versions) |
| `spring-ai-claude-core` | `ClaudeCodeAgent`, `ClaudeCodeRequest/Result/Config`, CLI process runner (no Spring) |
| `spring-ai-claude-chat` | Spring AI `ChatModel` adapter (`ClaudeCodeChatModel`) |
| `spring-ai-claude-autoconfigure` | Spring Boot auto-configuration + `spring.ai.claude-code.*` properties |
| `spring-ai-claude-starters/spring-boot-starter-ai-claude` | Starter: agent API |
| `spring-ai-claude-starters/spring-boot-starter-ai-claude-chat` | Starter: agent API + Spring AI `ChatModel` |

Build everything with `mvn verify`.

## Add the starter
Publish to your internal Maven repository, import the BOM and add a starter:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.anode.ai</groupId>
      <artifactId>spring-ai-claude-dependencies</artifactId>
      <version>0.1.0-SNAPSHOT</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependency>
  <groupId>com.anode.ai</groupId>
  <artifactId>spring-boot-starter-ai-claude-chat</artifactId> <!-- or spring-boot-starter-ai-claude -->
</dependency>
```

## Configure

```yaml
spring:
  ai:
    claude-code:
      enabled: true
      executable: claude
      model: sonnet
      working-directory: ./workspace
      timeout: 10m
      permission-mode: default
      max-turns: 30
      expose-chat-model: true
```

`permission-mode` is passed to the Claude Code CLI. Keep `default` unless you have deliberately designed a trusted, isolated execution environment. Do not expose a server that can run arbitrary prompts against a developer's workspace without authentication and authorization.

## Use the native agent API

```java
@Service
class RefactoringService {
    private final ClaudeCodeAgent agent;

    RefactoringService(ClaudeCodeAgent agent) {
        this.agent = agent;
    }

    ClaudeCodeResult refactor(String instruction) {
        return agent.run(ClaudeCodeRequest.of(instruction));
    }
}
```

For session continuation, pass the returned session ID in the next request:

```java
ClaudeCodeResult first = agent.run(ClaudeCodeRequest.of("Inspect this project and summarize its architecture."));
ClaudeCodeResult next = agent.run(new ClaudeCodeRequest(
    "Now add health checks.", first.sessionId(), null, null, null));
```

## Use through Spring AI ChatClient

```java
@Bean
ChatClient claudeChatClient(ChatModel chatModel) {
    return ChatClient.builder(chatModel).build();
}
```

```java
String answer = claudeChatClient.prompt()
    .user("Inspect this repository and identify the highest priority bug.")
    .call()
    .content();
```

The adapter is intentionally text-only. Claude Code performs its own agent loop and native tool execution. Spring AI `@Tool` callbacks are **not** automatically converted into Claude Code tools in this prototype. Use `ClaudeCodeAgent` for coding-agent tasks; use a normal Spring AI provider when you need Spring AI-managed tool calling.

## How it works

`Spring application -> ClaudeCodeAgent -> ProcessBuilder -> claude -p --output-format json -> Claude Code -> Anthropic`

The process is launched without a shell, so prompt content is passed as a single argument (not interpolated into a shell command). The CLI inherits the app process environment and Claude Code authentication.

## Current prototype limitations / next steps
- Blocking process execution; add a bounded executor for high concurrency.
- Streaming is not implemented yet. A production version should consume `--output-format stream-json` events and expose `Flux<AgentEvent>` / SSE.
- ChatModel adapter flattens Spring messages into text and does not preserve structured tool calls, images, or token usage.
- Session persistence is caller-managed; store session IDs by authenticated conversation/user.
- Add integration tests against a stub executable, health indicator, process concurrency limits, cancellation, audit logging, and workspace allow-listing.
- Verify CLI flags against the installed Claude Code version; Anthropic can evolve CLI options.

## Security
This starter can execute commands and modify files in its configured working directory. Run it as a low-privilege OS user, isolate workspaces, authenticate API callers, enforce per-user workspace authorization, set timeouts and concurrency limits, and never enable permission bypass for untrusted input.

## Example chatbot

`examples/claude-chatbot` is a runnable Spring Boot app built on `spring-boot-starter-ai-claude-chat` (modelled on `atsia-igor`):
- `ChatClient` + `MessageChatMemoryAdvisor` (in-memory conversation history) on top of the Claude Code `ChatModel`.
- `POST /api/chat` stateful JSON chat, and a small web page at `/`.
- `POST /api/v1/chat/completions` and `GET /api/v1/models`: stateless OpenAI-compatible endpoints (streaming is emulated as a single chunk), usable from Open WebUI.

```bash
mvn -q install -DskipTests
cd examples/claude-chatbot && mvn spring-boot:run   # then open http://localhost:8080
```

It binds to `127.0.0.1` and uses `permission-mode: default`; add authentication before exposing it.

### Docker

`examples/claude-chatbot/Dockerfile` builds the app and installs the Claude Code CLI (Node 22 + `@anthropic-ai/claude-code`) in a non-root image. Authenticate from the web UI with an OAuth token (`claude setup-token`) or a Console API key. It is stored in the `claude-home` volume and can also be set through the config API (`PUT /api/config/token` with `{"token":"..."}`, `GET` for status, `DELETE` to remove; the token is write-only):

```bash
cd examples/claude-chatbot
docker compose up --build   # http://localhost:8080, then enter your token in the page
```

The port is published on `127.0.0.1` only, the agent works in the `/workspace` volume, and capabilities are dropped. Pin the CLI with `--build-arg CLAUDE_CODE_VERSION=x.y.z`.
