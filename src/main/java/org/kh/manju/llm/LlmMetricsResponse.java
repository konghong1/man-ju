package org.kh.manju.llm;

import java.time.Instant;
import java.util.List;

public record LlmMetricsResponse(
        Instant from,
        Instant to,
        List<LlmRuntimeMetric> metrics,
        List<LlmAlert> alerts
) {
}
