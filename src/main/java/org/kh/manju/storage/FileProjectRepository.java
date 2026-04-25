package org.kh.manju.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.kh.manju.config.ManJuProperties;
import org.kh.manju.model.ComicProject;
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
public class FileProjectRepository implements ProjectRepository {

    private final ObjectMapper objectMapper;
    private final Path storageDir;

    public FileProjectRepository(ObjectMapper objectMapper, ManJuProperties properties) {
        this.objectMapper = objectMapper;
        this.storageDir = Path.of(properties.getStorageDir());
        ensureDir();
    }

    @Override
    public ComicProject save(ComicProject project) {
        ensureDir();
        Path target = pathFor(project.projectId());
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(project);
            Files.writeString(
                    target,
                    json,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            return project;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save project " + project.projectId(), e);
        }
    }

    @Override
    public Optional<ComicProject> findById(String projectId) {
        Path target = pathFor(projectId);
        if (!Files.exists(target)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(target.toFile(), ComicProject.class));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read project " + projectId, e);
        }
    }

    @Override
    public List<ComicProject> findLatest(int limit) {
        if (limit <= 0) {
            return List.of();
        }

        try (Stream<Path> paths = Files.list(storageDir)) {
            return paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(this::lastModifiedSafe).reversed())
                    .limit(limit)
                    .map(this::readProjectSafe)
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to list projects", e);
        }
    }

    private Path pathFor(String projectId) {
        return storageDir.resolve(projectId + ".json");
    }

    private void ensureDir() {
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create storage directory: " + storageDir, e);
        }
    }

    private long lastModifiedSafe(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0;
        }
    }

    private ComicProject readProjectSafe(Path path) {
        try {
            return objectMapper.readValue(path.toFile(), ComicProject.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse project file: " + path, e);
        }
    }
}
