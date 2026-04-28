package org.kh.manju.api;

import jakarta.validation.Valid;
import org.kh.manju.model.ComicProject;
import org.kh.manju.model.CreateProjectRequest;
import org.kh.manju.model.GenerationJob;
import org.kh.manju.model.ProjectVersion;
import org.kh.manju.model.RetryJobRequest;
import org.kh.manju.model.RollbackVersionRequest;
import org.kh.manju.service.ComicProjectService;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api")
public class ComicProjectController {

    private final ComicProjectService comicProjectService;

    public ComicProjectController(ComicProjectService comicProjectService) {
        this.comicProjectService = comicProjectService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @PostMapping("/projects")
    public ResponseEntity<ComicProject> create(@Valid @RequestBody CreateProjectRequest request) {
        ComicProject created = comicProjectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/projects/{projectId}")
    public ComicProject getById(@PathVariable String projectId) {
        return comicProjectService.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found: " + projectId));
    }

    @PostMapping("/projects/{projectId}/jobs")
    public ComicProject rerun(@PathVariable String projectId) {
        return comicProjectService.rerunProject(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found: " + projectId));
    }

    @GetMapping("/jobs/{jobId}")
    public GenerationJob getJob(@PathVariable String jobId) {
        return comicProjectService.findJobById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found: " + jobId));
    }

    @PostMapping("/jobs/{jobId}/retry")
    public GenerationJob retry(
            @PathVariable String jobId,
            @RequestBody(required = false) RetryJobRequest request
    ) {
        return comicProjectService.retryJob(jobId, request)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found: " + jobId));
    }

    @GetMapping(path = "/jobs/{jobId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@PathVariable String jobId) {
        GenerationJob job = comicProjectService.findJobById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found: " + jobId));

        SseEmitter emitter = new SseEmitter(0L);
        try {
            emitter.send(SseEmitter.event().name("status").data(Map.of(
                    "jobId", job.jobId(),
                    "status", "QUEUED",
                    "at", job.createdAt()
            )));

            if (job.startedAt() != null) {
                emitter.send(SseEmitter.event().name("status").data(Map.of(
                        "jobId", job.jobId(),
                        "status", "RUNNING",
                        "at", job.startedAt()
                )));
            }

            for (var step : job.trace()) {
                emitter.send(SseEmitter.event().name("step").data(step));
            }

            emitter.send(SseEmitter.event().name("status").data(Map.of(
                    "jobId", job.jobId(),
                    "status", job.status().name(),
                    "at", job.updatedAt(),
                    "error", job.error() == null ? "" : job.error()
            )));
            emitter.complete();
        } catch (IOException ex) {
            emitter.completeWithError(ex);
        }

        return emitter;
    }

    @GetMapping("/projects/{projectId}/versions")
    public List<ProjectVersion> versions(@PathVariable String projectId) {
        return comicProjectService.listVersions(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found: " + projectId));
    }

    @PostMapping("/projects/{projectId}/rollback")
    public ComicProject rollback(
            @PathVariable String projectId,
            @Valid @RequestBody RollbackVersionRequest request
    ) {
        try {
            return comicProjectService.rollbackToVersion(projectId, request.versionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found: " + projectId));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @GetMapping("/projects/{projectId}/export")
    public ResponseEntity<String> export(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "json") String format
    ) {
        String content = comicProjectService.exportProject(projectId, format)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found: " + projectId));

        MediaType contentType = "json".equalsIgnoreCase(format)
                ? MediaType.APPLICATION_JSON
                : MediaType.TEXT_PLAIN;
        return ResponseEntity.ok().contentType(contentType).body(content);
    }

    @GetMapping("/projects")
    public List<ComicProject> latest(@RequestParam(defaultValue = "10") int limit) {
        if (limit < 1 || limit > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and 100");
        }
        return comicProjectService.latestProjects(limit);
    }
}
