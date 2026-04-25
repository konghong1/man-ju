package org.kh.manju.model;

import java.time.Instant;
import java.util.List;

public record ProjectVersion(
        String versionId,
        String jobId,
        Instant createdAt,
        String synopsis,
        List<Episode> episodes
) {
}
