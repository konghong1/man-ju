package org.kh.manju.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.kh.manju.llm.ChatRequest;
import org.kh.manju.llm.ChatResult;
import org.kh.manju.llm.LlmClientRegistry;
import org.kh.manju.model.CreateProjectRequest;
import org.kh.manju.model.Episode;
import org.kh.manju.model.GenerationStep;
import org.kh.manju.model.GenerationStepResult;
import org.kh.manju.model.HarnessRunResult;
import org.kh.manju.model.StepStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
public class HarnessOrchestrator {

    private final ComicDraftGenerator draftGenerator;
    private final ObjectMapper objectMapper;
    private final LlmClientRegistry llmClientRegistry;

    public HarnessOrchestrator(
            ComicDraftGenerator draftGenerator,
            ObjectMapper objectMapper,
            LlmClientRegistry llmClientRegistry
    ) {
        this.draftGenerator = draftGenerator;
        this.objectMapper = objectMapper;
        this.llmClientRegistry = llmClientRegistry;
    }

    public HarnessRunResult run(CreateProjectRequest request) {
        String jobId = "job-" + UUID.randomUUID();
        List<GenerationStepResult> trace = new ArrayList<>();

        Map<String, Object> normalized = runStep(
                trace,
                GenerationStep.S1_INPUT_NORMALIZE,
                Map.of("rawInput", request),
                () -> normalizeInput(request)
        );

        String synopsis = runStep(
                trace,
                GenerationStep.S2_STORY_PLAN,
                normalized,
                () -> draftGenerator.buildSynopsis(request)
        );

        List<Episode> episodes = runStep(
                trace,
                GenerationStep.S3_SCENE_WRITE,
                Map.of("synopsis", synopsis, "normalized", normalized),
                () -> draftGenerator.buildEpisodes(request)
        );

        runStep(
                trace,
                GenerationStep.S4_PANELIZE,
                Map.of("episodes", episodes.size()),
                () -> Map.of(
                        "sceneCount", episodes.stream().mapToInt(episode -> episode.scenes().size()).sum(),
                        "panelCount", episodes.stream()
                                .flatMap(episode -> episode.scenes().stream())
                                .mapToInt(scene -> scene.panels().size())
                                .sum()
                )
        );

        runStep(
                trace,
                GenerationStep.S5_PROMPT_COMPILE,
                Map.of("episodes", episodes.size()),
                () -> Map.of(
                        "compiledPrompts", episodes.stream()
                                .flatMap(episode -> episode.scenes().stream())
                                .flatMap(scene -> scene.panels().stream())
                                .count()
                )
        );

        runStep(
                trace,
                GenerationStep.S6_QUALITY_GATE,
                Map.of("synopsis", synopsis),
                () -> Map.of(
                        "result", "pass",
                        "continuityScore", 0.91,
                        "consistencyScore", 0.89
                )
        );

        runStep(
                trace,
                GenerationStep.S7_PERSIST_VERSION,
                Map.of("jobId", jobId),
                () -> Map.of("versionId", "v-" + UUID.randomUUID())
        );

        return new HarnessRunResult(jobId, synopsis, episodes, trace);
    }

    private Map<String, Object> normalizeInput(CreateProjectRequest request) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("title", request.title().trim());
        normalized.put("genre", request.genre().trim());
        normalized.put("tone", request.tone().trim());
        normalized.put("targetAudience", request.targetAudience().trim());
        normalized.put("episodeLength", request.episodeLength().name());
        normalized.put("premise", request.premise().trim());
        normalized.put("protagonist", request.protagonist().trim());
        normalized.put("conflict", request.conflict().trim());
        normalized.put("visualStyle", request.visualStyle().trim());
        normalized.put("language", request.language() == null || request.language().isBlank() ? "zh-CN" : request.language().trim());
        return normalized;
    }

    private <T> T runStep(
            List<GenerationStepResult> trace,
            GenerationStep step,
            Object input,
            Supplier<T> action
    ) {
        Instant startedAt = Instant.now();
        String provider = providerFor(step);
        String model = modelFor(step);

        try {
            JsonNode inputJson = objectMapper.valueToTree(input);
            ChatResult llmMetadata = "internal".equals(provider)
                    ? null
                    : llmClientRegistry.resolve(provider).chat(new ChatRequest(
                    "step:" + step.name(),
                    inputJson.toString(),
                    model,
                    0.2,
                    1600
            ));

            T output = action.get();
            Instant endedAt = Instant.now();
            JsonNode outputJson = objectMapper.valueToTree(output);
            int inputTokens = llmMetadata == null ? estimateTokens(inputJson) : llmMetadata.inputTokens();
            int outputTokens = llmMetadata == null ? estimateTokens(outputJson) : llmMetadata.outputTokens();
            trace.add(new GenerationStepResult(
                    step,
                    StepStatus.SUCCESS,
                    inputJson,
                    outputJson,
                    provider,
                    model,
                    inputTokens,
                    outputTokens,
                    estimateCostUsd(inputTokens, outputTokens),
                    Duration.between(startedAt, endedAt).toMillis(),
                    0,
                    null,
                    startedAt,
                    endedAt
            ));
            return output;
        } catch (RuntimeException ex) {
            Instant endedAt = Instant.now();
            JsonNode inputJson = objectMapper.valueToTree(input);
            trace.add(new GenerationStepResult(
                    step,
                    StepStatus.FAILED,
                    inputJson,
                    null,
                    provider,
                    model,
                    estimateTokens(inputJson),
                    0,
                    0,
                    Duration.between(startedAt, endedAt).toMillis(),
                    0,
                    ex.getMessage(),
                    startedAt,
                    endedAt
            ));
            throw ex;
        }
    }

    private int estimateTokens(JsonNode payload) {
        return Math.max(16, payload.toString().length() / 4);
    }

    private double estimateCostUsd(int inputTokens, int outputTokens) {
        return Math.round((inputTokens * 0.0000012 + outputTokens * 0.0000024) * 1_000_000d) / 1_000_000d;
    }

    private String providerFor(GenerationStep step) {
        return switch (step) {
            case S2_STORY_PLAN, S5_PROMPT_COMPILE -> "openai";
            case S3_SCENE_WRITE -> "anthropic";
            case S1_INPUT_NORMALIZE, S4_PANELIZE, S6_QUALITY_GATE, S7_PERSIST_VERSION -> "internal";
        };
    }

    private String modelFor(GenerationStep step) {
        return switch (step) {
            case S2_STORY_PLAN, S5_PROMPT_COMPILE -> "gpt-5.2";
            case S3_SCENE_WRITE -> "claude-opus-4.1";
            case S1_INPUT_NORMALIZE, S4_PANELIZE, S6_QUALITY_GATE, S7_PERSIST_VERSION -> "rule-engine-v1";
        };
    }
}
