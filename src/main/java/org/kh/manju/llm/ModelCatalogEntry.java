package org.kh.manju.llm;

import java.util.List;

public record ModelCatalogEntry(
        String provider,
        String modelId,
        List<String> capabilities,
        double inputCostPerMillion,
        double outputCostPerMillion,
        boolean enabled
) {
}
