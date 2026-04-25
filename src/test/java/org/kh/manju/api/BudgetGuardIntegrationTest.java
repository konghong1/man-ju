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
import java.util.Comparator;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "manju.storage-dir=target/test-budget-projects",
        "manju.job-storage-dir=target/test-budget-jobs",
        "manju.llm-project-budget-usd=0.000001"
})
class BudgetGuardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void cleanup() throws IOException {
        resetDir("target/test-budget-projects");
        resetDir("target/test-budget-jobs");
    }

    @Test
    void shouldDowngradeToInternalProviderWhenBudgetExceeded() throws Exception {
        String payload = """
                {
                  "title": "budget-guard-test",
                  "genre": "sci-fi",
                  "tone": "tense",
                  "targetAudience": "youth",
                  "episodeLength": "SHORT",
                  "premise": "budget guard",
                  "protagonist": "tester",
                  "conflict": "cost spike",
                  "visualStyle": "mono",
                  "language": "en"
                }
                """;

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.generationTrace[0].provider").value("internal"))
                .andExpect(jsonPath("$.generationTrace[1].provider").value("internal"));
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
