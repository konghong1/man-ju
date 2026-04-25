package org.kh.manju.qa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kh.manju.llm.AnthropicLlmClient;
import org.kh.manju.llm.ChatRequest;
import org.kh.manju.llm.GeminiLlmClient;
import org.kh.manju.llm.OpenAiLlmClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "manju.storage-dir=target/test-chaos-projects",
        "manju.job-storage-dir=target/test-chaos-jobs",
        "manju.llm-circuit-failure-threshold=999"
})
class FailoverChaosIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @SpyBean
    private OpenAiLlmClient openAiLlmClient;

    @SpyBean
    private AnthropicLlmClient anthropicLlmClient;

    @SpyBean
    private GeminiLlmClient geminiLlmClient;

    @BeforeEach
    void cleanup() throws IOException {
        reset(openAiLlmClient, anthropicLlmClient, geminiLlmClient);
        resetDir("target/test-chaos-projects");
        resetDir("target/test-chaos-jobs");
    }

    @Test
    void shouldFallbackToAnthropicWhenOpenAiProviderThrows() throws Exception {
        doThrow(new IllegalStateException("chaos-openai")).when(openAiLlmClient).chat(any(ChatRequest.class));

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectPayload("chaos-openai")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.generationTrace[1].provider").value("anthropic"));
    }

    @Test
    void shouldFallbackToInternalWhenAllExternalProvidersThrow() throws Exception {
        doThrow(new IllegalStateException("chaos-openai")).when(openAiLlmClient).chat(any(ChatRequest.class));
        doThrow(new IllegalStateException("chaos-anthropic")).when(anthropicLlmClient).chat(any(ChatRequest.class));
        doThrow(new IllegalStateException("chaos-gemini")).when(geminiLlmClient).chat(any(ChatRequest.class));

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectPayload("chaos-all")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.generationTrace[1].provider").value("internal"))
                .andExpect(jsonPath("$.generationTrace[2].provider").value("internal"));
    }

    private String projectPayload(String title) {
        return """
                {
                  "title": "%s",
                  "genre": "sci-fi",
                  "tone": "tense",
                  "targetAudience": "youth",
                  "episodeLength": "SHORT",
                  "premise": "chaos test",
                  "protagonist": "tester",
                  "conflict": "provider fault",
                  "visualStyle": "mono",
                  "language": "en"
                }
                """.formatted(title);
    }

    private void resetDir(String pathText) throws IOException {
        Path dir = Path.of(pathText);
        if (Files.exists(dir)) {
            try (var paths = Files.walk(dir)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
        Files.createDirectories(dir);
    }
}
