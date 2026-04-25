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

    private final HarnessOrchestrator harnessOrchestrator;
    private final ProjectRepository projectRepository;

    public ComicProjectService(HarnessOrchestrator harnessOrchestrator, ProjectRepository projectRepository) {
        this.harnessOrchestrator = harnessOrchestrator;
        this.projectRepository = projectRepository;
    }

    public ComicProject createProject(CreateProjectRequest request) {
        String projectId = "proj-" + UUID.randomUUID();
        var harnessResult = harnessOrchestrator.run(projectId, request);
        ComicProject project = new ComicProject(
                projectId,
                Instant.now(),
                request,
                harnessResult.synopsis(),
                harnessResult.episodes(),
                harnessResult.jobId(),
                harnessResult.trace()
        );
        return projectRepository.save(project);
    }

    public Optional<ComicProject> findById(String projectId) {
        return projectRepository.findById(projectId);
    }

    public Optional<ComicProject> rerunProject(String projectId) {
        return projectRepository.findById(projectId)
                .map(existing -> {
                    var harnessResult = harnessOrchestrator.run(existing.projectId(), existing.input());
                    ComicProject updated = new ComicProject(
                            existing.projectId(),
                            existing.createdAt(),
                            existing.input(),
                            harnessResult.synopsis(),
                            harnessResult.episodes(),
                            harnessResult.jobId(),
                            harnessResult.trace()
                    );
                    return projectRepository.save(updated);
                });
    }

    public List<ComicProject> latestProjects(int limit) {
        return projectRepository.findLatest(limit);
    }
}
