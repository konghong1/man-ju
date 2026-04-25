package org.kh.manju.model;

import java.util.List;

public record HarnessRunResult(
        String jobId,
        String synopsis,
        List<Episode> episodes,
        List<GenerationStepResult> trace
) {
}
