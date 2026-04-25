package org.kh.manju.model;

import java.time.Instant;
import java.util.List;

public record GenerationJob(
        String jobId,
        String projectId,
        JobStatus status,
        GenerationStep resumeFromStep,
        String retriedFromJobId,
        String versionId,
        String error,
        Instant createdAt,
        Instant startedAt,
        Instant endedAt,
        Instant updatedAt,
        List<GenerationStepResult> trace
) {
}
