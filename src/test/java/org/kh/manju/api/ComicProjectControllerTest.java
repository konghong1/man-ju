package org.kh.manju.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kh.manju.llm.ProviderStateService;
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
        "manju.storage-dir=target/test-projects",
        "manju.job-storage-dir=target/test-jobs"
})
class ComicProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProviderStateService providerStateService;

    @BeforeEach
    void cleanup() throws IOException {
        resetDir("target/test-projects");
        resetDir("target/test-jobs");
        providerStateService.states().keySet().forEach(provider -> providerStateService.update(provider, true));
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
                .andExpect(jsonPath("$.versions.length()").value(1))
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
    void shouldExposeJobAndVersionAndEventApis() throws Exception {
        String payload = """
                {
                  "title": "job-api-test",
                  "genre": "sci-fi",
                  "tone": "tense",
                  "targetAudience": "youth",
                  "episodeLength": "SHORT",
                  "premise": "job api coverage",
                  "protagonist": "tester",
                  "conflict": "event replay",
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

        JsonNode projectNode = objectMapper.readTree(createResponse);
        String projectId = projectNode.get("projectId").asText();
        String jobId = projectNode.get("latestJobId").asText();

        mockMvc.perform(get("/api/jobs/{jobId}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.trace.length()").value(7));

        String versionsResponse = mockMvc.perform(get("/api/projects/{projectId}/versions", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(versionsResponse).contains(jobId);

        String sseResponse = mockMvc.perform(get("/api/jobs/{jobId}/events", jobId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(sseResponse).contains("event:status");
        assertThat(sseResponse).contains("event:step");
        assertThat(sseResponse).contains(jobId);
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
    void shouldFallbackToSecondaryWhenPrimaryProviderDisabled() throws Exception {
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
                .andExpect(jsonPath("$.generationTrace[1].provider").value("anthropic"));
    }

    @Test
    void shouldFallbackToInternalWhenAllExternalProvidersDisabled() throws Exception {
        String[] providers = {"openai", "anthropic", "gemini"};
        for (String provider : providers) {
            mockMvc.perform(patch("/api/llm/providers/{provider}", provider)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "enabled": false
                                    }
                                    """))
                    .andExpect(status().isOk());
        }

        String payload = """
                {
                  "title": "all-disabled-fallback-test",
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

    @Test
    void shouldRetryJobFromSpecificStepAndMarkSkippedTrace() throws Exception {
        String payload = """
                {
                  "title": "retry-step-test",
                  "genre": "sci-fi",
                  "tone": "tense",
                  "targetAudience": "youth",
                  "episodeLength": "SHORT",
                  "premise": "retry behavior",
                  "protagonist": "tester",
                  "conflict": "step resume",
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

        JsonNode projectNode = objectMapper.readTree(createResponse);
        String projectId = projectNode.get("projectId").asText();
        String sourceJobId = projectNode.get("latestJobId").asText();

        String retryResponse = mockMvc.perform(post("/api/jobs/{jobId}/retry", sourceJobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromStep": "S4_PANELIZE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retriedFromJobId").value(sourceJobId))
                .andExpect(jsonPath("$.resumeFromStep").value("S4_PANELIZE"))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.trace[0].status").value("SKIPPED"))
                .andExpect(jsonPath("$.trace[1].status").value("SKIPPED"))
                .andExpect(jsonPath("$.trace[2].status").value("SKIPPED"))
                .andExpect(jsonPath("$.trace[3].status").value("SUCCESS"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String retriedJobId = objectMapper.readTree(retryResponse).get("jobId").asText();
        mockMvc.perform(get("/api/projects/{projectId}", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latestJobId").value(retriedJobId))
                .andExpect(jsonPath("$.versions.length()").value(2));
    }

    @Test
    void shouldRollbackToSpecifiedVersion() throws Exception {
        String payload = """
                {
                  "title": "rollback-test",
                  "genre": "sci-fi",
                  "tone": "tense",
                  "targetAudience": "youth",
                  "episodeLength": "SHORT",
                  "premise": "rollback behavior",
                  "protagonist": "tester",
                  "conflict": "version control",
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
        JsonNode projectNode = objectMapper.readTree(createResponse);
        String projectId = projectNode.get("projectId").asText();
        String firstJobId = projectNode.get("latestJobId").asText();

        String versionsResponse = mockMvc.perform(get("/api/projects/{projectId}/versions", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String firstVersionId = objectMapper.readTree(versionsResponse).get(0).get("versionId").asText();

        mockMvc.perform(post("/api/projects/{projectId}/jobs", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versions.length()").value(2));

        mockMvc.perform(post("/api/projects/{projectId}/rollback", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versionId": "%s"
                                }
                                """.formatted(firstVersionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latestJobId").value(firstJobId))
                .andExpect(jsonPath("$.versions.length()").value(2));
    }

    @Test
    void shouldRejectInvalidProjectListLimit() throws Exception {
        mockMvc.perform(get("/api/projects")
                        .queryParam("limit", "0"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/projects")
                        .queryParam("limit", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidProjectRoutes() throws Exception {
        mockMvc.perform(put("/api/llm/routes/project/{projectId}", "proj-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "routes": {}
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/llm/routes/project/{projectId}", "proj-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "routes": {
                                    "S2_STORY_PLAN": "missing-provider"
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest());
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
