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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "manju.storage-dir=target/test-e2e-projects",
        "manju.job-storage-dir=target/test-e2e-jobs"
})
class EndToEndWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanup() throws IOException {
        resetDir("target/test-e2e-projects");
        resetDir("target/test-e2e-jobs");
    }

    @Test
    void shouldPassCreateGenerateEditExportWorkflow() throws Exception {
        String createResponse = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").exists())
                .andExpect(jsonPath("$.latestJobId").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String projectId = objectMapper.readTree(createResponse).get("projectId").asText();

        mockMvc.perform(put("/api/llm/routes/project/{projectId}", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "routes": {
                                    "S2_STORY_PLAN": "gemini"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.S2_STORY_PLAN").value("gemini"));

        mockMvc.perform(post("/api/projects/{projectId}/jobs", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generationTrace[1].provider").value("gemini"));

        String jsonExport = mockMvc.perform(get("/api/projects/{projectId}/export", projectId)
                        .queryParam("format", "json"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(jsonExport).contains("\"projectId\"");
        assertThat(jsonExport).contains(projectId);

        String markdownExport = mockMvc.perform(get("/api/projects/{projectId}/export", projectId)
                        .queryParam("format", "markdown"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(markdownExport).contains("# e2e-workflow-test");
        assertThat(markdownExport).contains("## Synopsis");

        String promptExport = mockMvc.perform(get("/api/projects/{projectId}/export", projectId)
                        .queryParam("format", "prompt"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(promptExport).contains("E1-S1-P1:");
    }

    private String projectPayload() {
        return """
                {
                  "title": "e2e-workflow-test",
                  "genre": "sci-fi",
                  "tone": "tense",
                  "targetAudience": "youth",
                  "episodeLength": "SHORT",
                  "premise": "end to end workflow",
                  "protagonist": "tester",
                  "conflict": "delivery pressure",
                  "visualStyle": "mono",
                  "language": "en"
                }
                """;
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
