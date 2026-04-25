package org.kh.manju.storage;

import org.kh.manju.model.GenerationJob;

import java.util.List;
import java.util.Optional;

public interface JobRepository {
    GenerationJob save(GenerationJob job);

    Optional<GenerationJob> findById(String jobId);

    List<GenerationJob> findByProjectId(String projectId);

    List<GenerationJob> findAll();
}
