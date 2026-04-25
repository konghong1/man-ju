package org.kh.manju.api;

import com.fasterxml.jackson.databind.JsonNode;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "manju.storage-dir=target/test-projects"
})
class ComicProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanup() throws IOException {
        Path dir = Path.of("target/test-projects");
        if (!Files.exists(dir)) {
            return;
        }

        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        Files.createDirectories(dir);
    }

    @Test
    void shouldCreateAndFetchProject() throws Exception {
        String payload = """
                {
                  "title": "测试漫剧",
                  "genre": "科幻",
                  "tone": "紧张",
                  "targetAudience": "青年",
                  "episodeLength": "SHORT",
                  "premise": "主角要在夜晚找回被篡改的证据",
                  "protagonist": "苏离",
                  "conflict": "反派控制了城市监控系统",
                  "visualStyle": "黑白网点",
                  "language": "zh-CN"
                }
                """;

        String createResponse = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").exists())
                .andExpect(jsonPath("$.episodes[0].scenes").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode node = objectMapper.readTree(createResponse);
        String projectId = node.get("projectId").asText();
        assertThat(projectId).startsWith("proj-");

        mockMvc.perform(get("/api/projects/{projectId}", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.episodes[0].scenes[0].panels[0].imagePrompt").exists());
    }

    @Test
    void shouldExposeHealthAndWelcomePage() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
    }
}
