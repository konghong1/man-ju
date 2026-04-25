package org.kh.manju.api;

import jakarta.validation.Valid;
import org.kh.manju.llm.LlmClientRegistry;
import org.kh.manju.llm.ModelCatalogEntry;
import org.kh.manju.llm.ModelCatalogService;
import org.kh.manju.llm.RoutingPolicyService;
import org.kh.manju.model.GenerationStep;
import org.kh.manju.model.ProjectRouteUpdateRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@Validated
@RestController
@RequestMapping("/api/llm")
public class LlmConfigController {

    private final ModelCatalogService modelCatalogService;
    private final LlmClientRegistry llmClientRegistry;
    private final RoutingPolicyService routingPolicyService;

    public LlmConfigController(
            ModelCatalogService modelCatalogService,
            LlmClientRegistry llmClientRegistry,
            RoutingPolicyService routingPolicyService
    ) {
        this.modelCatalogService = modelCatalogService;
        this.llmClientRegistry = llmClientRegistry;
        this.routingPolicyService = routingPolicyService;
    }

    @GetMapping("/models")
    public java.util.List<ModelCatalogEntry> models() {
        return modelCatalogService.list();
    }

    @GetMapping("/providers")
    public Set<String> providers() {
        return llmClientRegistry.providers().keySet();
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
}
