package org.kh.manju.llm;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ModelCatalogService {

    public List<ModelCatalogEntry> list() {
        return List.of(
                new ModelCatalogEntry("openai", "gpt-5.2", List.of("chat", "structured"), 1.2, 2.4, true),
                new ModelCatalogEntry("anthropic", "claude-opus-4.1", List.of("chat", "structured"), 1.8, 3.2, true),
                new ModelCatalogEntry("gemini", "gemini-2.5-pro", List.of("chat", "structured"), 1.0, 2.1, true),
                new ModelCatalogEntry("azure-openai", "gpt-4.1", List.of("chat", "structured"), 1.3, 2.6, true),
                new ModelCatalogEntry("openai-compatible", "deepseek-chat", List.of("chat"), 0.9, 1.7, true),
                new ModelCatalogEntry("ollama", "qwen2.5:14b", List.of("chat"), 0, 0, true),
                new ModelCatalogEntry("internal", "rule-engine-v1", List.of("deterministic"), 0, 0, true)
        );
    }

    public String defaultModelForProvider(String provider) {
        return list().stream()
                .filter(entry -> entry.provider().equals(provider))
                .findFirst()
                .map(ModelCatalogEntry::modelId)
                .orElse("rule-engine-v1");
    }
}
