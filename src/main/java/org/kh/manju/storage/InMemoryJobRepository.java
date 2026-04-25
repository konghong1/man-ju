package org.kh.manju.storage;

import org.kh.manju.model.GenerationJob;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
@ConditionalOnProperty(prefix = "manju", name = "storage-mode", havingValue = "memory")
public class InMemoryJobRepository implements JobRepository {

    private final ConcurrentMap<String, GenerationJob> store = new ConcurrentHashMap<>();

    @Override
    public GenerationJob save(GenerationJob job) {
        store.put(job.jobId(), job);
        return job;
    }

    @Override
    public Optional<GenerationJob> findById(String jobId) {
        return Optional.ofNullable(store.get(jobId));
    }

    @Override
    public List<GenerationJob> findByProjectId(String projectId) {
        return store.values().stream()
                .filter(job -> projectId.equals(job.projectId()))
                .sorted(Comparator.comparing(GenerationJob::createdAt).reversed())
                .toList();
    }

    @Override
    public List<GenerationJob> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(GenerationJob::createdAt).reversed())
                .toList();
    }
}
