package org.kh.manju.llm;

import org.kh.manju.model.GenerationStepResult;
import org.kh.manju.model.StepStatus;
import org.kh.manju.storage.JobRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class LlmMetricsService {

    private final JobRepository jobRepository;

    public LlmMetricsService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public LlmMetricsResponse query(Instant from, Instant to) {
        Instant fromValue = from == null ? Instant.EPOCH : from;
        Instant toValue = to == null ? Instant.now() : to;
        if (toValue.isBefore(fromValue)) {
            throw new IllegalArgumentException("Invalid range: to must be greater than or equal to from.");
        }

        List<GenerationStepResult> samples = jobRepository.findAll().stream()
                .flatMap(job -> job.trace().stream())
                .filter(step -> step.startedAt() != null)
                .filter(step -> !step.startedAt().isBefore(fromValue) && !step.startedAt().isAfter(toValue))
                .toList();

        Map<MetricKey, List<GenerationStepResult>> grouped = samples.stream()
                .collect(Collectors.groupingBy(step -> new MetricKey(step.provider(), step.model(), step.step())));

        List<LlmRuntimeMetric> metrics = grouped.entrySet().stream()
                .map(entry -> toMetric(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(LlmRuntimeMetric::provider)
                        .thenComparing(LlmRuntimeMetric::model)
                        .thenComparing(item -> item.step().name()))
                .toList();

        List<LlmAlert> alerts = buildAlerts(metrics);
        return new LlmMetricsResponse(fromValue, toValue, metrics, alerts);
    }

    private LlmRuntimeMetric toMetric(MetricKey key, List<GenerationStepResult> steps) {
        int requests = steps.size();
        long successCount = steps.stream().filter(step -> step.status() == StepStatus.SUCCESS || step.status() == StepStatus.SKIPPED).count();
        double successRate = requests == 0 ? 0 : round3((double) successCount / requests);
        List<Long> latencies = steps.stream().map(GenerationStepResult::latencyMs).sorted().toList();
        long p95 = percentile95(latencies);
        double avgCost = round6(steps.stream().mapToDouble(GenerationStepResult::costUsd).average().orElse(0));
        int totalTokens = steps.stream().mapToInt(step -> step.inputTokens() + step.outputTokens()).sum();

        return new LlmRuntimeMetric(
                key.provider(),
                key.model(),
                key.step(),
                requests,
                successRate,
                p95,
                avgCost,
                totalTokens
        );
    }

    private List<LlmAlert> buildAlerts(List<LlmRuntimeMetric> metrics) {
        List<LlmAlert> alerts = new ArrayList<>();
        for (LlmRuntimeMetric metric : metrics) {
            if (metric.requests() >= 3 && metric.successRate() < 0.8) {
                alerts.add(new LlmAlert(
                        "warning",
                        metric.provider(),
                        metric.model(),
                        metric.step(),
                        "Success rate below 80% in the selected window."
                ));
            }
            if (metric.avgCostUsd() > 0.01) {
                alerts.add(new LlmAlert(
                        "info",
                        metric.provider(),
                        metric.model(),
                        metric.step(),
                        "Average cost exceeded 0.01 USD."
                ));
            }
        }
        return List.copyOf(alerts);
    }

    private long percentile95(List<Long> values) {
        if (values.isEmpty()) {
            return 0;
        }
        int index = (int) Math.ceil(values.size() * 0.95) - 1;
        return values.get(Math.max(0, index));
    }

    private double round3(double value) {
        return Math.round(value * 1_000d) / 1_000d;
    }

    private double round6(double value) {
        return Math.round(value * 1_000_000d) / 1_000_000d;
    }

    private record MetricKey(String provider, String model, org.kh.manju.model.GenerationStep step) {
    }
}
