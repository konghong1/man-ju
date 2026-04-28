package org.kh.manju.llm;

import org.kh.manju.model.GenerationStep;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoutingPolicyService {

    private final Map<String, Map<GenerationStep, String>> projectRoutes = new ConcurrentHashMap<>();
    private final Map<GenerationStep, String> defaultRoutes = new EnumMap<>(GenerationStep.class);
    private final LlmClientRegistry llmClientRegistry;

    public RoutingPolicyService(LlmClientRegistry llmClientRegistry) {
        this.llmClientRegistry = llmClientRegistry;

        defaultRoutes.put(GenerationStep.S1_INPUT_NORMALIZE, "internal");
        defaultRoutes.put(GenerationStep.S2_STORY_PLAN, "openai");
        defaultRoutes.put(GenerationStep.S3_SCENE_WRITE, "anthropic");
        defaultRoutes.put(GenerationStep.S4_PANELIZE, "internal");
        defaultRoutes.put(GenerationStep.S5_PROMPT_COMPILE, "openai");
        defaultRoutes.put(GenerationStep.S6_QUALITY_GATE, "internal");
        defaultRoutes.put(GenerationStep.S7_PERSIST_VERSION, "internal");
    }

    public String resolveProvider(String projectId, GenerationStep step) {
        Map<GenerationStep, String> projectPolicy = projectRoutes.get(projectId);
        String provider = projectPolicy == null ? null : projectPolicy.get(step);
        if (provider == null || !llmClientRegistry.hasProvider(provider)) {
            provider = defaultRoutes.getOrDefault(step, "internal");
        }
        return provider;
    }

    public Map<GenerationStep, String> getEffectiveRoutes(String projectId) {
        Map<GenerationStep, String> merged = new EnumMap<>(defaultRoutes);
        Map<GenerationStep, String> projectPolicy = projectRoutes.get(projectId);
        if (projectPolicy != null) {
            merged.putAll(projectPolicy);
        }
        return Map.copyOf(merged);
    }

    public void setProjectRoutes(String projectId, Map<GenerationStep, String> routes) {
        if (routes.isEmpty()) {
            throw new IllegalArgumentException("routes must not be empty");
        }

        Map<GenerationStep, String> filtered = new EnumMap<>(GenerationStep.class);
        for (Map.Entry<GenerationStep, String> entry : routes.entrySet()) {
            if (entry.getKey() == null) {
                throw new IllegalArgumentException("route step must not be null");
            }
            String provider = entry.getValue();
            if (provider == null || provider.isBlank()) {
                throw new IllegalArgumentException("route provider must not be blank");
            }
            if (!llmClientRegistry.hasProvider(provider)) {
                throw new IllegalArgumentException("Unknown provider: " + provider);
            }
            filtered.put(entry.getKey(), provider);
        }
        projectRoutes.put(projectId, filtered);
    }
}
