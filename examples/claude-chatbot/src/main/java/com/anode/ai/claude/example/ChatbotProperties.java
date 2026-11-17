package com.anode.ai.claude.example;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("chatbot")
public record ChatbotProperties(Integer memoryMaxMessages) {
    public ChatbotProperties {
        if (memoryMaxMessages == null || memoryMaxMessages < 2) memoryMaxMessages = 40;
    }
}
