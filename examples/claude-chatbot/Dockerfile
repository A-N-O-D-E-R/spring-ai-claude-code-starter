# Build context = repository root:  docker build -f examples/claude-chatbot/Dockerfile -t claude-chatbot .

# ---- build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -B -pl examples/claude-chatbot -am package -DskipTests

# ---- runtime: JRE + Node (for the Claude Code CLI) ----
FROM eclipse-temurin:21-jre
ARG NODE_MAJOR=22
ARG CLAUDE_CODE_VERSION=latest

RUN apt-get update \
 && apt-get install -y --no-install-recommends curl ca-certificates git ripgrep \
 && curl -fsSL https://deb.nodesource.com/setup_${NODE_MAJOR}.x | bash - \
 && apt-get install -y --no-install-recommends nodejs \
 && npm install -g @anthropic-ai/claude-code@${CLAUDE_CODE_VERSION} \
 && apt-get clean && rm -rf /var/lib/apt/lists/*

# Claude Code must not run as root (and the agent can execute tools: keep it unprivileged)
RUN useradd --create-home --uid 10001 app \
 && mkdir -p /workspace /home/app/.claude \
 && chown -R app:app /workspace /home/app
USER app
WORKDIR /app

COPY --from=build --chown=app:app /src/examples/claude-chatbot/target/claude-chatbot-example-*.jar /app/app.jar

# Inside the container the app must listen on all interfaces; compose publishes it on 127.0.0.1 only.
ENV SERVER_ADDRESS=0.0.0.0 \
    SPRING_AI_CLAUDE_CODE_WORKING_DIRECTORY=/workspace \
    SPRING_AI_CLAUDE_CODE_EXECUTABLE=claude

EXPOSE 8080
VOLUME ["/workspace"]
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
