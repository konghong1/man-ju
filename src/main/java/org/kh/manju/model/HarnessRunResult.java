package org.kh.manju.model;

import java.util.List;

public record HarnessRunResult(
        String jobId,
        String versionId,
        String synopsis,
        List<Episode> episodes,
        List<GenerationStepResult> trace,
        boolean succeeded,
        GenerationStep failedStep,
        String error
) {
}
