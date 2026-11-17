package com.anode.ai.claude.example;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OpenAiCompatControllerTest {

    private final List<Prompt> prompts = new ArrayList<>();
    private final ChatModel model = prompt -> {
        prompts.add(prompt);
        return new ChatResponse(List.of(new Generation(new AssistantMessage("pong"))));
    };
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new OpenAiCompatController(model), new ApiExceptionHandler()).build();

    @Test void listsModel() throws Exception {
        mvc.perform(get("/api/v1/models")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("claude-code"));
    }

    @Test void completes() throws Exception {
        mvc.perform(post("/api/v1/chat/completions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"messages\":[{\"role\":\"system\",\"content\":\"be brief\"},{\"role\":\"user\",\"content\":[{\"type\":\"text\",\"text\":\"ping\"}]}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.choices[0].message.content").value("pong"))
                .andExpect(jsonPath("$.choices[0].finish_reason").value("stop"));
    }

    @Test void streamsSingleChunk() throws Exception {
        var result = mvc.perform(post("/api/v1/chat/completions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stream\":true,\"messages\":[{\"role\":\"user\",\"content\":\"ping\"}]}"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data: [DONE]")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("pong")));
    }

    @Test void rejectsEmpty() throws Exception {
        mvc.perform(post("/api/v1/chat/completions").contentType(MediaType.APPLICATION_JSON).content("{\"messages\":[]}"))
                .andExpect(status().isBadRequest());
    }
}
