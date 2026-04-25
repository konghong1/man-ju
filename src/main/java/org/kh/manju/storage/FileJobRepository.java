package org.kh.manju.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.kh.manju.config.ManJuProperties;
import org.kh.manju.model.GenerationJob;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Repository
@ConditionalOnProperty(prefix = "manju", name = "storage-mode", havingValue = "file", matchIfMissing = true)
public class FileJobRepository implements JobRepository {

    private final ObjectMapper objectMapper;
    private final Path storageDir;

    public FileJobRepository(ObjectMapper objectMapper, ManJuProperties properties) {
        this.objectMapper = objectMapper;
        this.storageDir = Path.of(properties.getJobStorageDir());
        ensureDir();
    }

    @Override
    public GenerationJob save(GenerationJob job) {
        ensureDir();
        Path target = pathFor(job.jobId());
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(job);
            Files.writeString(
                    target,
                    json,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            return job;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save job " + job.jobId(), e);
        }
    }

    @Override
    public Optional<GenerationJob> findById(String jobId) {
        Path target = pathFor(jobId);
        if (!Files.exists(target)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(target.toFile(), GenerationJob.class));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read job " + jobId, e);
        }
    }

    @Override
    public List<GenerationJob> findByProjectId(String projectId) {
        return findAll().stream()
                .filter(job -> projectId.equals(job.projectId()))
                .toList();
    }

    @Override
    public List<GenerationJob> findAll() {
        try (Stream<Path> paths = Files.list(storageDir)) {
            return paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(this::lastModifiedSafe).reversed())
                    .map(this::readJobSafe)
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to list jobs", e);
        }
    }

    private Path pathFor(String jobId) {
        return storageDir.resolve(jobId + ".json");
    }

    private void ensureDir() {
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create job storage directory: " + storageDir, e);
        }
    }

    private long lastModifiedSafe(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0;
        }
    }

    private GenerationJob readJobSafe(Path path) {
        try {
            return objectMapper.readValue(path.toFile(), GenerationJob.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse job file: " + path, e);
        }
    }
}
