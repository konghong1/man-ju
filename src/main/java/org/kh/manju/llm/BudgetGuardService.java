package org.kh.manju.llm;

import org.kh.manju.config.ManJuProperties;
import org.kh.manju.model.GenerationStepResult;
import org.kh.manju.storage.JobRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BudgetGuardService {

    private final JobRepository jobRepository;
    private final double projectBudgetUsd;

    public BudgetGuardService(JobRepository jobRepository, ManJuProperties properties) {
        this.jobRepository = jobRepository;
        this.projectBudgetUsd = Math.max(0, properties.getLlmProjectBudgetUsd());
    }

    public String selectProvider(String projectId, String preferredProvider, List<GenerationStepResult> currentRunTrace) {
        if ("internal".equals(preferredProvider)) {
            return preferredProvider;
        }
        if (projectBudgetUsd <= 0) {
            return "internal";
        }

        double historicalCost = jobRepository.findByProjectId(projectId).stream()
                .flatMap(job -> job.trace().stream())
                .mapToDouble(GenerationStepResult::costUsd)
                .sum();
        double currentCost = currentRunTrace.stream().mapToDouble(GenerationStepResult::costUsd).sum();
        if (historicalCost + currentCost >= projectBudgetUsd) {
            return "internal";
        }
        return preferredProvider;
    }
}
