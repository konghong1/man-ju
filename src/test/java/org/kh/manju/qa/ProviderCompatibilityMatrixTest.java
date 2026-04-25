package org.kh.manju.qa;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "manju.storage-dir=target/test-provider-matrix-projects",
        "manju.job-storage-dir=target/test-provider-matrix-jobs"
})
class ProviderCompatibilityMatrixTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanup() throws IOException {
        resetDir("target/test-provider-matrix-projects");
        resetDir("target/test-provider-matrix-jobs");
    }

    @Test
    void shouldExecuteSmokeScenarioForAllProviders() throws Exception {
        String payload = """
                {
                  "title": "provider-matrix-test",
                  "genre": "sci-fi",
                  "tone": "tense",
                  "targetAudience": "youth",
                  "episodeLength": "SHORT",
                  "premise": "provider matrix",
                  "protagonist": "tester",
                  "conflict": "compatibility",
                  "visualStyle": "mono",
                  "language": "en"
                }
                """;

        String createResponse = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String projectId = objectMapper.readTree(createResponse).get("projectId").asText();

        String[] providers = {"openai", "anthropic", "gemini", "azure-openai", "openai-compatible", "ollama", "internal"};
        for (String provider : providers) {
            String routePayload = """
                    {
                      "routes": {
                        "S2_STORY_PLAN": "%s"
                      }
                    }
                    """.formatted(provider);

            mockMvc.perform(put("/api/llm/routes/project/{projectId}", projectId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(routePayload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.S2_STORY_PLAN").value(provider));

            mockMvc.perform(post("/api/projects/{projectId}/jobs", projectId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.generationTrace[1].provider").value(provider));
        }
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
