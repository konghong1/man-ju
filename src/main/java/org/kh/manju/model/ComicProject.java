package org.kh.manju.model;

import java.time.Instant;
import java.util.List;

public record ComicProject(
        String projectId,
        Instant createdAt,
        CreateProjectRequest input,
        String synopsis,
        List<Episode> episodes,
        String latestJobId,
        List<GenerationStepResult> generationTrace
) {
}
