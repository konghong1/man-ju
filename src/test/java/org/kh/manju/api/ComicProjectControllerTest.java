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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
                .andExpect(jsonPath("$.latestJobId").exists())
                .andExpect(jsonPath("$.generationTrace.length()").value(7))
                .andExpect(jsonPath("$.generationTrace[1].provider").value("openai"))
                .andExpect(jsonPath("$.generationTrace[2].provider").value("anthropic"))
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

    @Test
    void shouldApplyProjectLevelRouteWhenRerunning() throws Exception {
        String payload = """
                {
                  "title": "路由测试",
                  "genre": "科幻",
                  "tone": "紧张",
                  "targetAudience": "青年",
                  "episodeLength": "SHORT",
                  "premise": "测试步骤路由",
                  "protagonist": "路由员",
                  "conflict": "模型切换",
                  "visualStyle": "黑白网点",
                  "language": "zh-CN"
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

        String routePayload = """
                {
                  "routes": {
                    "S2_STORY_PLAN": "gemini"
                  }
                }
                """;

        mockMvc.perform(put("/api/llm/routes/project/{projectId}", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(routePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.S2_STORY_PLAN").value("gemini"));

        mockMvc.perform(post("/api/projects/{projectId}/jobs", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.generationTrace[1].provider").value("gemini"));
    }

    @Test
    void shouldFallbackToInternalWhenProviderDisabled() throws Exception {
        mockMvc.perform(patch("/api/llm/providers/{provider}", "openai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openai").value(false));

        String payload = """
                {
                  "title": "provider-disable-test",
                  "genre": "sci-fi",
                  "tone": "tense",
                  "targetAudience": "youth",
                  "episodeLength": "SHORT",
                  "premise": "route fallback",
                  "protagonist": "tester",
                  "conflict": "provider unavailable",
                  "visualStyle": "mono",
                  "language": "en"
                }
                """;

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.generationTrace[1].provider").value("internal"));
    }

    @Test
    void shouldSupportAzureCompatibleAndOllamaRoutes() throws Exception {
        String payload = """
                {
                  "title": "multi-provider-route-test",
                  "genre": "sci-fi",
                  "tone": "tense",
                  "targetAudience": "youth",
                  "episodeLength": "SHORT",
                  "premise": "provider switching",
                  "protagonist": "tester",
                  "conflict": "route matrix",
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

        String[] providers = {"azure-openai", "openai-compatible", "ollama"};
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
}
