package org.kh.manju.storage;

import org.kh.manju.model.ComicProject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
@ConditionalOnProperty(prefix = "manju", name = "storage-mode", havingValue = "memory")
public class InMemoryProjectRepository implements ProjectRepository {

    private final ConcurrentMap<String, ComicProject> store = new ConcurrentHashMap<>();

    @Override
    public ComicProject save(ComicProject project) {
        store.put(project.projectId(), project);
        return project;
    }

    @Override
    public Optional<ComicProject> findById(String projectId) {
        return Optional.ofNullable(store.get(projectId));
    }

    @Override
    public List<ComicProject> findLatest(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return store.values().stream()
                .sorted(Comparator.comparing(ComicProject::createdAt).reversed())
                .limit(limit)
                .toList();
    }
}
