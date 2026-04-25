package org.kh.manju.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.kh.manju.config.ManJuProperties;
import org.kh.manju.model.GenerationStep;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class LlmFallbackServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldOpenCircuitAfterConsecutiveFailuresAndSkipProvider() {
        LlmFallbackService service = newFallbackService(
                1,
                60_000L,
                100,
                1_000L,
                new FailClient("openai"),
                new SuccessClient("anthropic"),
                new SuccessClient("gemini"),
                new SuccessClient("internal")
        );

        LlmInvocationResult first = service.invokeWithFallback(
                "openai",
                GenerationStep.S2_STORY_PLAN,
                objectMapper.valueToTree(java.util.Map.of("k", "v"))
        );
        assertThat(first.providerUsed()).isEqualTo("anthropic");
        assertThat(first.attemptedProviders()).containsExactly("openai", "anthropic");

        LlmInvocationResult second = service.invokeWithFallback(
                "openai",
                GenerationStep.S2_STORY_PLAN,
                objectMapper.valueToTree(java.util.Map.of("k", "v2"))
        );
        assertThat(second.providerUsed()).isEqualTo("anthropic");
        assertThat(second.attemptedProviders()).containsExactly("anthropic");
    }

    @Test
    void shouldRateLimitProviderAndFallbackToSecondary() {
        LlmFallbackService service = newFallbackService(
                2,
                60_000L,
                1,
                60_000L,
                new SuccessClient("openai"),
                new SuccessClient("anthropic"),
                new SuccessClient("gemini"),
                new SuccessClient("internal")
        );

        LlmInvocationResult first = service.invokeWithFallback(
                "openai",
                GenerationStep.S2_STORY_PLAN,
                objectMapper.valueToTree(java.util.Map.of("k", "v"))
        );
        assertThat(first.providerUsed()).isEqualTo("openai");
        assertThat(first.attemptedProviders()).containsExactly("openai");

        LlmInvocationResult second = service.invokeWithFallback(
                "openai",
                GenerationStep.S2_STORY_PLAN,
                objectMapper.valueToTree(java.util.Map.of("k", "v2"))
        );
        assertThat(second.providerUsed()).isEqualTo("anthropic");
        assertThat(second.attemptedProviders()).containsExactly("anthropic");
    }

    private LlmFallbackService newFallbackService(
            int circuitFailureThreshold,
            long circuitOpenMillis,
            int rateLimitMaxRequests,
            long rateLimitWindowMillis,
            LlmClient... clients
    ) {
        LlmClientRegistry registry = new LlmClientRegistry(List.of(clients));
        ModelCatalogService modelCatalogService = new ModelCatalogService();
        ProviderStateService providerStateService = new ProviderStateService(registry);
        ManJuProperties properties = new ManJuProperties();
        properties.setLlmCircuitFailureThreshold(circuitFailureThreshold);
        properties.setLlmCircuitOpenMillis(circuitOpenMillis);
        properties.setLlmRateLimitMaxRequests(rateLimitMaxRequests);
        properties.setLlmRateLimitWindowMillis(rateLimitWindowMillis);
        ProviderResilienceService providerResilienceService = new ProviderResilienceService(properties);
        return new LlmFallbackService(
                registry,
                modelCatalogService,
                providerStateService,
                providerResilienceService,
                new LlmAuditService()
        );
    }

    private record SuccessClient(String provider) implements LlmClient {
        @Override
        public ChatResult chat(ChatRequest request) {
            return new ChatResult("ok", 10, 10, 10);
        }

        @Override
        public StructuredResult structured(StructuredRequest request) {
            return new StructuredResult(null, 0, 0, 0);
        }

        @Override
        public Stream<ChatChunk> stream(ChatRequest request) {
            return Stream.empty();
        }
    }

    private record FailClient(String provider) implements LlmClient {
        @Override
        public ChatResult chat(ChatRequest request) {
            throw new IllegalStateException("provider error");
        }

        @Override
        public StructuredResult structured(StructuredRequest request) {
            throw new IllegalStateException("provider error");
        }

        @Override
        public Stream<ChatChunk> stream(ChatRequest request) {
            return Stream.empty();
        }
    }
}
