package org.kh.manju.llm;

import org.kh.manju.config.ManJuProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProviderResilienceService {

    private final Map<String, ProviderState> providerStates = new ConcurrentHashMap<>();
    private final int failureThreshold;
    private final long circuitOpenMillis;
    private final int rateLimitMaxRequests;
    private final long rateLimitWindowMillis;

    public ProviderResilienceService(ManJuProperties properties) {
        this.failureThreshold = Math.max(1, properties.getLlmCircuitFailureThreshold());
        this.circuitOpenMillis = Math.max(1, properties.getLlmCircuitOpenMillis());
        this.rateLimitMaxRequests = Math.max(1, properties.getLlmRateLimitMaxRequests());
        this.rateLimitWindowMillis = Math.max(1, properties.getLlmRateLimitWindowMillis());
    }

    public boolean canAttempt(String provider) {
        if (isInternal(provider)) {
            return true;
        }

        ProviderState state = providerStates.computeIfAbsent(provider, ignored -> new ProviderState());
        long now = System.currentTimeMillis();
        synchronized (state) {
            if (state.openUntilEpochMs > now) {
                return false;
            }

            if (state.windowStartEpochMs == 0 || now - state.windowStartEpochMs >= rateLimitWindowMillis) {
                state.windowStartEpochMs = now;
                state.windowCount = 0;
            }
            if (state.windowCount >= rateLimitMaxRequests) {
                return false;
            }

            state.windowCount += 1;
            return true;
        }
    }

    public void onSuccess(String provider) {
        if (isInternal(provider)) {
            return;
        }

        ProviderState state = providerStates.computeIfAbsent(provider, ignored -> new ProviderState());
        synchronized (state) {
            state.consecutiveFailures = 0;
            state.openUntilEpochMs = 0;
        }
    }

    public void onFailure(String provider) {
        if (isInternal(provider)) {
            return;
        }

        ProviderState state = providerStates.computeIfAbsent(provider, ignored -> new ProviderState());
        long now = System.currentTimeMillis();
        synchronized (state) {
            state.consecutiveFailures += 1;
            if (state.consecutiveFailures >= failureThreshold) {
                state.openUntilEpochMs = now + circuitOpenMillis;
                state.consecutiveFailures = 0;
            }
        }
    }

    private boolean isInternal(String provider) {
        return "internal".equals(provider);
    }

    private static final class ProviderState {
        private int consecutiveFailures;
        private long openUntilEpochMs;
        private long windowStartEpochMs;
        private int windowCount;
    }
}
