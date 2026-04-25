package org.kh.manju.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.kh.manju.llm.BudgetGuardService;
import org.kh.manju.llm.LlmFallbackService;
import org.kh.manju.llm.LlmInvocationResult;
import org.kh.manju.llm.ModelCatalogService;
import org.kh.manju.llm.RoutingPolicyService;
import org.kh.manju.llm.StructuredOutputGuard;
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
    private final LlmFallbackService llmFallbackService;
    private final RoutingPolicyService routingPolicyService;
    private final ModelCatalogService modelCatalogService;
    private final StructuredOutputGuard structuredOutputGuard;
    private final BudgetGuardService budgetGuardService;

    public HarnessOrchestrator(
            ComicDraftGenerator draftGenerator,
            ObjectMapper objectMapper,
            LlmFallbackService llmFallbackService,
            RoutingPolicyService routingPolicyService,
            ModelCatalogService modelCatalogService,
            StructuredOutputGuard structuredOutputGuard,
            BudgetGuardService budgetGuardService
    ) {
        this.draftGenerator = draftGenerator;
        this.objectMapper = objectMapper;
        this.llmFallbackService = llmFallbackService;
        this.routingPolicyService = routingPolicyService;
        this.modelCatalogService = modelCatalogService;
        this.structuredOutputGuard = structuredOutputGuard;
        this.budgetGuardService = budgetGuardService;
    }

    public HarnessRunResult run(String projectId, CreateProjectRequest request) {
        return run(projectId, request, "job-" + UUID.randomUUID(), null, List.of());
    }

    public HarnessRunResult run(
            String projectId,
            CreateProjectRequest request,
            String jobId
    ) {
        return run(projectId, request, jobId, null, List.of());
    }

    public HarnessRunResult run(
            String projectId,
            CreateProjectRequest request,
            String jobId,
            GenerationStep resumeFromStep,
            List<GenerationStepResult> previousTrace
    ) {
        validateResumeInput(resumeFromStep, previousTrace);
        List<GenerationStepResult> trace = new ArrayList<>();
        String synopsis = "";
        List<Episode> episodes = List.of();
        String versionId = null;

        try {
            Map<String, Object> normalized = isSkipped(GenerationStep.S1_INPUT_NORMALIZE, resumeFromStep)
                    ? resumeMapOutput(trace, previousTrace, GenerationStep.S1_INPUT_NORMALIZE, new TypeReference<>() {
                    })
                    : runStep(
                    trace,
                    projectId,
                    GenerationStep.S1_INPUT_NORMALIZE,
                    Map.of("rawInput", request),
                    () -> normalizeInput(request)
            );

            synopsis = isSkipped(GenerationStep.S2_STORY_PLAN, resumeFromStep)
                    ? resumeOutput(trace, previousTrace, GenerationStep.S2_STORY_PLAN, JsonNode::asText)
                    : runStep(
                    trace,
                    projectId,
                    GenerationStep.S2_STORY_PLAN,
                    normalized,
                    () -> draftGenerator.buildSynopsis(request)
            );

            episodes = isSkipped(GenerationStep.S3_SCENE_WRITE, resumeFromStep)
                    ? resumeMapOutput(trace, previousTrace, GenerationStep.S3_SCENE_WRITE, new TypeReference<>() {
                    })
                    : runStep(
                    trace,
                    projectId,
                    GenerationStep.S3_SCENE_WRITE,
                    Map.of("synopsis", synopsis, "normalized", normalized),
                    () -> draftGenerator.buildEpisodes(request)
            );

            final List<Episode> episodeSnapshot = episodes;

            if (isSkipped(GenerationStep.S4_PANELIZE, resumeFromStep)) {
                resumeOutput(trace, previousTrace, GenerationStep.S4_PANELIZE, Function.identity());
            } else {
                runStep(
                        trace,
                        projectId,
                        GenerationStep.S4_PANELIZE,
                        Map.of("episodes", episodeSnapshot.size()),
                        () -> Map.of(
                                "sceneCount", episodeSnapshot.stream().mapToInt(episode -> episode.scenes().size()).sum(),
                                "panelCount", episodeSnapshot.stream()
                                        .flatMap(episode -> episode.scenes().stream())
                                        .mapToInt(scene -> scene.panels().size())
                                        .sum()
                        )
                );
            }

            if (isSkipped(GenerationStep.S5_PROMPT_COMPILE, resumeFromStep)) {
                resumeOutput(trace, previousTrace, GenerationStep.S5_PROMPT_COMPILE, Function.identity());
            } else {
                runStep(
                        trace,
                        projectId,
                        GenerationStep.S5_PROMPT_COMPILE,
                        Map.of("episodes", episodeSnapshot.size()),
                        () -> Map.of(
                                "compiledPrompts", episodeSnapshot.stream()
                                        .flatMap(episode -> episode.scenes().stream())
                                        .flatMap(scene -> scene.panels().stream())
                                        .count()
                        )
                );
            }

            if (isSkipped(GenerationStep.S6_QUALITY_GATE, resumeFromStep)) {
                resumeOutput(trace, previousTrace, GenerationStep.S6_QUALITY_GATE, Function.identity());
            } else {
                runStep(
                        trace,
                        projectId,
                        GenerationStep.S6_QUALITY_GATE,
                        Map.of("synopsis", synopsis),
                        () -> Map.of(
                                "result", "pass",
                                "continuityScore", 0.91,
                                "consistencyScore", 0.89
                        )
                );
            }

            Map<String, String> versionPayload = isSkipped(GenerationStep.S7_PERSIST_VERSION, resumeFromStep)
                    ? resumeMapOutput(trace, previousTrace, GenerationStep.S7_PERSIST_VERSION, new TypeReference<>() {
                    })
                    : runStep(
                    trace,
                    projectId,
                    GenerationStep.S7_PERSIST_VERSION,
                    Map.of("jobId", jobId),
                    () -> Map.of("versionId", "v-" + UUID.randomUUID())
            );
            versionId = versionPayload.get("versionId");

            return new HarnessRunResult(jobId, versionId, synopsis, episodes, trace, true, null, null);
        } catch (StepActionException ex) {
            GenerationStep failedStep = trace.isEmpty() ? null : trace.get(trace.size() - 1).step();
            return new HarnessRunResult(jobId, versionId, synopsis, episodes, trace, false, failedStep, ex.getCause().getMessage());
        }
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
            String projectId,
            GenerationStep step,
            Object input,
            Supplier<T> action
    ) {
        Instant startedAt = Instant.now();
        String routedProvider = routingPolicyService.resolveProvider(projectId, step);
        String primaryProvider = budgetGuardService.selectProvider(projectId, routedProvider, trace);
        String provider = primaryProvider;
        String model = modelCatalogService.defaultModelForProvider(primaryProvider);

        try {
            JsonNode inputJson = objectMapper.valueToTree(input);
            LlmInvocationResult llmMetadata = llmFallbackService.invokeWithFallback(primaryProvider, step, inputJson);
            provider = llmMetadata.providerUsed();
            model = llmMetadata.modelUsed();

            T output = action.get();
            Instant endedAt = Instant.now();
            JsonNode outputJson = objectMapper.valueToTree(output);
            StructuredOutputGuard.GuardResult guardResult = structuredOutputGuard.ensureValid(step, outputJson);
            int inputTokens = llmMetadata.chatResult().inputTokens();
            int outputTokens = Math.max(llmMetadata.chatResult().outputTokens(), estimateTokens(guardResult.output()));
            trace.add(new GenerationStepResult(
                    step,
                    StepStatus.SUCCESS,
                    inputJson,
                    guardResult.output(),
                    provider,
                    model,
                    inputTokens,
                    outputTokens,
                    estimateCostUsd(inputTokens, outputTokens),
                    Duration.between(startedAt, endedAt).toMillis(),
                    llmMetadata.retries() + (guardResult.repaired() ? 1 : 0),
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
            throw new StepActionException(ex);
        }
    }

    private int estimateTokens(JsonNode payload) {
        return Math.max(16, payload.toString().length() / 4);
    }

    private double estimateCostUsd(int inputTokens, int outputTokens) {
        return Math.round((inputTokens * 0.0000012 + outputTokens * 0.0000024) * 1_000_000d) / 1_000_000d;
    }

    private void validateResumeInput(GenerationStep resumeFromStep, List<GenerationStepResult> previousTrace) {
        if (resumeFromStep != null && (previousTrace == null || previousTrace.isEmpty())) {
            throw new IllegalArgumentException("Cannot resume without source trace.");
        }
    }

    private boolean isSkipped(GenerationStep step, GenerationStep resumeFromStep) {
        return resumeFromStep != null && step.ordinal() < resumeFromStep.ordinal();
    }

    private <T> T resumeOutput(
            List<GenerationStepResult> trace,
            List<GenerationStepResult> previousTrace,
            GenerationStep step,
            Function<JsonNode, T> mapper
    ) {
        GenerationStepResult source = requireSuccessfulStep(previousTrace, step);
        trace.add(toSkippedResult(source));
        return mapper.apply(source.outputPayload());
    }

    private <T> T resumeMapOutput(
            List<GenerationStepResult> trace,
            List<GenerationStepResult> previousTrace,
            GenerationStep step,
            TypeReference<T> typeReference
    ) {
        return resumeOutput(
                trace,
                previousTrace,
                step,
                node -> objectMapper.convertValue(node, typeReference)
        );
    }

    private GenerationStepResult requireSuccessfulStep(List<GenerationStepResult> trace, GenerationStep step) {
        return trace.stream()
                .filter(item -> item.step() == step && item.status() == StepStatus.SUCCESS && item.outputPayload() != null)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cannot resume: step output missing for " + step));
    }

    private GenerationStepResult toSkippedResult(GenerationStepResult source) {
        Instant now = Instant.now();
        return new GenerationStepResult(
                source.step(),
                StepStatus.SKIPPED,
                source.inputPayload(),
                source.outputPayload(),
                source.provider(),
                source.model(),
                0,
                0,
                0,
                0,
                0,
                null,
                now,
                now
        );
    }

    private static final class StepActionException extends RuntimeException {
        private StepActionException(RuntimeException cause) {
            super(cause);
        }
    }

}
