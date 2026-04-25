package org.kh.manju.service;

import org.kh.manju.model.ComicProject;
import org.kh.manju.model.CreateProjectRequest;
import org.kh.manju.storage.ProjectRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ComicProjectService {

    private final ComicDraftGenerator draftGenerator;
    private final ProjectRepository projectRepository;

    public ComicProjectService(ComicDraftGenerator draftGenerator, ProjectRepository projectRepository) {
        this.draftGenerator = draftGenerator;
        this.projectRepository = projectRepository;
    }

    public ComicProject createProject(CreateProjectRequest request) {
        ComicProject project = new ComicProject(
                "proj-" + UUID.randomUUID(),
                Instant.now(),
                request,
                draftGenerator.buildSynopsis(request),
                draftGenerator.buildEpisodes(request)
        );
        return projectRepository.save(project);
    }

    public Optional<ComicProject> findById(String projectId) {
        return projectRepository.findById(projectId);
    }

    public List<ComicProject> latestProjects(int limit) {
        return projectRepository.findLatest(limit);
    }
}
