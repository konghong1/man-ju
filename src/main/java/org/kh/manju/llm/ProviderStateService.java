package org.kh.manju.llm;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProviderStateService {

    private final Map<String, Boolean> enabledByProvider = new ConcurrentHashMap<>();

    public ProviderStateService(LlmClientRegistry llmClientRegistry) {
        llmClientRegistry.providers().keySet()
                .forEach(provider -> enabledByProvider.put(provider, true));
    }

    public Map<String, Boolean> states() {
        return Map.copyOf(new LinkedHashMap<>(enabledByProvider));
    }

    public boolean isEnabled(String provider) {
        return enabledByProvider.getOrDefault(provider, false);
    }

    public Map<String, Boolean> update(String provider, boolean enabled) {
        if (enabledByProvider.containsKey(provider)) {
            enabledByProvider.put(provider, enabled);
        }
        return states();
    }
}
