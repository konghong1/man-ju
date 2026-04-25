package org.kh.manju.llm;

import java.util.List;

public record LlmInvocationResult(
        String providerUsed,
        String modelUsed,
        ChatResult chatResult,
        int retries,
        List<String> attemptedProviders
) {
}
