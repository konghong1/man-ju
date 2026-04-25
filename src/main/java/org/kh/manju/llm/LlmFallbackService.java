package org.kh.manju.llm;

import com.fasterxml.jackson.databind.JsonNode;
import org.kh.manju.model.GenerationStep;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class LlmFallbackService {

    private static final Map<String, List<String>> FALLBACK_CHAIN = Map.of(
            "openai", List.of("openai", "anthropic", "gemini", "internal"),
            "anthropic", List.of("anthropic", "openai", "gemini", "internal"),
            "gemini", List.of("gemini", "openai", "anthropic", "internal"),
            "azure-openai", List.of("azure-openai", "openai", "anthropic", "internal"),
            "openai-compatible", List.of("openai-compatible", "openai", "anthropic", "internal"),
            "ollama", List.of("ollama", "openai", "anthropic", "internal"),
            "internal", List.of("internal")
    );

    private final LlmClientRegistry llmClientRegistry;
    private final ModelCatalogService modelCatalogService;
    private final ProviderStateService providerStateService;
    private final ProviderResilienceService providerResilienceService;
    private final LlmAuditService llmAuditService;

    public LlmFallbackService(
            LlmClientRegistry llmClientRegistry,
            ModelCatalogService modelCatalogService,
            ProviderStateService providerStateService,
            ProviderResilienceService providerResilienceService,
            LlmAuditService llmAuditService
    ) {
        this.llmClientRegistry = llmClientRegistry;
        this.modelCatalogService = modelCatalogService;
        this.providerStateService = providerStateService;
        this.providerResilienceService = providerResilienceService;
        this.llmAuditService = llmAuditService;
    }

    public LlmInvocationResult invokeWithFallback(String primaryProvider, GenerationStep step, JsonNode inputPayload) {
        List<String> chain = FALLBACK_CHAIN.getOrDefault(primaryProvider, List.of(primaryProvider, "internal"));
        List<String> attempted = new ArrayList<>();
        RuntimeException lastException = null;

        for (String provider : chain) {
            if (!providerStateService.isEnabled(provider)) {
                continue;
            }
            if (!providerResilienceService.canAttempt(provider)) {
                continue;
            }
            attempted.add(provider);
            String model = modelCatalogService.defaultModelForProvider(provider);
            try {
                ChatResult result = llmClientRegistry.resolve(provider).chat(new ChatRequest(
                        "step:" + step.name(),
                        inputPayload.toString(),
                        model,
                        0.2,
                        1600
                ));
                providerResilienceService.onSuccess(provider);
                llmAuditService.recordAttempt(step, provider, model, inputPayload.toString(), true, null);
                return new LlmInvocationResult(
                        provider,
                        model,
                        result,
                        Math.max(0, attempted.size() - 1),
                        List.copyOf(attempted)
                );
            } catch (RuntimeException ex) {
                providerResilienceService.onFailure(provider);
                llmAuditService.recordAttempt(step, provider, model, inputPayload.toString(), false, ex.getMessage());
                lastException = ex;
                continue;
            }
        }

        throw new IllegalStateException("No available provider in fallback chain: " + chain, lastException);
    }
}
