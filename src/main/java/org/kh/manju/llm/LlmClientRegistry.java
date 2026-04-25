package org.kh.manju.llm;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class LlmClientRegistry {

    private final Map<String, LlmClient> byProvider;

    public LlmClientRegistry(List<LlmClient> clients) {
        Map<String, LlmClient> map = new LinkedHashMap<>();
        for (LlmClient client : clients) {
            map.put(client.provider(), client);
        }
        this.byProvider = Map.copyOf(map);
    }

    public LlmClient resolve(String provider) {
        LlmClient client = byProvider.get(provider);
        if (client != null) {
            return client;
        }
        LlmClient fallback = byProvider.get("internal");
        if (fallback != null) {
            return fallback;
        }
        throw new IllegalStateException("No LLM clients available");
    }

    public boolean hasProvider(String provider) {
        return byProvider.containsKey(provider);
    }

    public Map<String, LlmClient> providers() {
        return byProvider;
    }
}
