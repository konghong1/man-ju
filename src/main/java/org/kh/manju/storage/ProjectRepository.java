package org.kh.manju.storage;

import org.kh.manju.model.ComicProject;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository {
    ComicProject save(ComicProject project);

    Optional<ComicProject> findById(String projectId);

    List<ComicProject> findLatest(int limit);
}
