package org.kh.manju.api;

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
import java.time.Instant;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "manju.storage-dir=target/test-metrics-projects",
        "manju.job-storage-dir=target/test-metrics-jobs"
})
class LlmMetricsApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void cleanup() throws IOException {
        resetDir("target/test-metrics-projects");
        resetDir("target/test-metrics-jobs");
    }

    @Test
    void shouldQueryMetricsByProviderModelAndStep() throws Exception {
        String payload = """
                {
                  "title": "metrics-test",
                  "genre": "sci-fi",
                  "tone": "tense",
                  "targetAudience": "youth",
                  "episodeLength": "SHORT",
                  "premise": "metrics aggregation",
                  "protagonist": "tester",
                  "conflict": "monitoring",
                  "visualStyle": "mono",
                  "language": "en"
                }
                """;

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        String from = Instant.now().minusSeconds(600).toString();
        String to = Instant.now().plusSeconds(600).toString();

        String response = mockMvc.perform(get("/api/llm/metrics")
                        .queryParam("from", from)
                        .queryParam("to", to))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics.length()").isNotEmpty())
                .andExpect(jsonPath("$.metrics[0].provider").exists())
                .andExpect(jsonPath("$.metrics[0].model").exists())
                .andExpect(jsonPath("$.metrics[0].step").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).contains("openai");
        assertThat(response).contains("S2_STORY_PLAN");
    }

    @Test
    void shouldReturnEmptyMetricsForFutureWindow() throws Exception {
        String from = Instant.now().plusSeconds(3600).toString();
        String to = Instant.now().plusSeconds(7200).toString();

        mockMvc.perform(get("/api/llm/metrics")
                        .queryParam("from", from)
                        .queryParam("to", to))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics.length()").value(0));
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
