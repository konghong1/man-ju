package org.kh.manju.api;

import jakarta.validation.Valid;
import org.kh.manju.llm.LlmClientRegistry;
import org.kh.manju.llm.LlmMetricsResponse;
import org.kh.manju.llm.LlmMetricsService;
import org.kh.manju.llm.ModelCatalogEntry;
import org.kh.manju.llm.ModelCatalogService;
import org.kh.manju.llm.ProviderStateService;
import org.kh.manju.llm.RoutingPolicyService;
import org.kh.manju.model.GenerationStep;
import org.kh.manju.model.ProjectRouteUpdateRequest;
import org.kh.manju.model.ProviderToggleRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/llm")
public class LlmConfigController {

    private final ModelCatalogService modelCatalogService;
    private final LlmClientRegistry llmClientRegistry;
    private final RoutingPolicyService routingPolicyService;
    private final ProviderStateService providerStateService;
    private final LlmMetricsService llmMetricsService;

    public LlmConfigController(
            ModelCatalogService modelCatalogService,
            LlmClientRegistry llmClientRegistry,
            RoutingPolicyService routingPolicyService,
            ProviderStateService providerStateService,
            LlmMetricsService llmMetricsService
    ) {
        this.modelCatalogService = modelCatalogService;
        this.llmClientRegistry = llmClientRegistry;
        this.routingPolicyService = routingPolicyService;
        this.providerStateService = providerStateService;
        this.llmMetricsService = llmMetricsService;
    }

    @GetMapping("/models")
    public java.util.List<ModelCatalogEntry> models() {
        return modelCatalogService.list();
    }

    @GetMapping("/providers")
    public Map<String, Boolean> providers() {
        return providerStateService.states();
    }

    @PatchMapping("/providers/{provider}")
    public Map<String, Boolean> updateProvider(
            @PathVariable String provider,
            @Valid @RequestBody ProviderToggleRequest request
    ) {
        if (!llmClientRegistry.hasProvider(provider)) {
            return providerStateService.states();
        }
        return providerStateService.update(provider, request.enabled());
    }

    @GetMapping("/routes/project/{projectId}")
    public Map<GenerationStep, String> projectRoutes(@PathVariable String projectId) {
        return routingPolicyService.getEffectiveRoutes(projectId);
    }

    @PutMapping("/routes/project/{projectId}")
    public Map<GenerationStep, String> updateProjectRoutes(
            @PathVariable String projectId,
            @Valid @RequestBody ProjectRouteUpdateRequest request
    ) {
        routingPolicyService.setProjectRoutes(projectId, request.routes());
        return routingPolicyService.getEffectiveRoutes(projectId);
    }

    @GetMapping("/metrics")
    public LlmMetricsResponse metrics(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to
    ) {
        return llmMetricsService.query(from, to);
    }
}
