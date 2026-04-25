package org.kh.manju.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.kh.manju.model.ComicProject;
import org.kh.manju.model.CreateProjectRequest;
import org.kh.manju.model.Episode;
import org.kh.manju.model.GenerationJob;
import org.kh.manju.model.GenerationStep;
import org.kh.manju.model.GenerationStepResult;
import org.kh.manju.model.HarnessRunResult;
import org.kh.manju.model.JobStatus;
import org.kh.manju.model.ProjectVersion;
import org.kh.manju.model.RetryJobRequest;
import org.kh.manju.model.StepStatus;
import org.kh.manju.storage.JobRepository;
import org.kh.manju.storage.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ComicProjectService {

    private final ObjectMapper objectMapper;
    private final HarnessOrchestrator harnessOrchestrator;
    private final ProjectRepository projectRepository;
    private final JobRepository jobRepository;

    public ComicProjectService(
            ObjectMapper objectMapper,
            HarnessOrchestrator harnessOrchestrator,
            ProjectRepository projectRepository,
            JobRepository jobRepository
    ) {
        this.objectMapper = objectMapper;
        this.harnessOrchestrator = harnessOrchestrator;
        this.projectRepository = projectRepository;
        this.jobRepository = jobRepository;
    }

    public ComicProject createProject(CreateProjectRequest request) {
        String projectId = "proj-" + UUID.randomUUID();
        ComicProject seed = new ComicProject(
                projectId,
                Instant.now(),
                request,
                "",
                List.of(),
                null,
                List.of(),
                List.of()
        );
        return runAndPersist(seed, null, null, List.of());
    }

    public Optional<ComicProject> findById(String projectId) {
        return projectRepository.findById(projectId).map(this::normalizeProject);
    }

    public Optional<ComicProject> rerunProject(String projectId) {
        return findById(projectId)
                .map(project -> runAndPersist(project, null, null, List.of()));
    }

    public Optional<GenerationJob> findJobById(String jobId) {
        return jobRepository.findById(jobId).map(this::normalizeJob);
    }

    public Optional<GenerationJob> retryJob(String sourceJobId, RetryJobRequest request) {
        return jobRepository.findById(sourceJobId)
                .map(this::normalizeJob)
                .flatMap(sourceJob -> findById(sourceJob.projectId())
                        .map(project -> {
                            GenerationStep resumeFrom = resolveResumeFrom(sourceJob, request);
                            List<GenerationStepResult> resumeTrace = resumeFrom == null ? List.of() : safeTrace(sourceJob.trace());
                            ComicProject updated = runAndPersist(project, resumeFrom, sourceJob.jobId(), resumeTrace);
                            return jobRepository.findById(updated.latestJobId())
                                    .map(this::normalizeJob)
                                    .orElseThrow(() -> new IllegalStateException("Failed to load new job " + updated.latestJobId()));
                        }));
    }

    public Optional<List<ProjectVersion>> listVersions(String projectId) {
        return findById(projectId).map(project -> List.copyOf(safeVersions(project.versions())));
    }

    public Optional<ComicProject> rollbackToVersion(String projectId, String versionId) {
        return findById(projectId).map(project -> {
            ProjectVersion version = safeVersions(project.versions()).stream()
                    .filter(item -> item.versionId().equals(versionId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionId));

            List<GenerationStepResult> trace = jobRepository.findById(version.jobId())
                    .map(GenerationJob::trace)
                    .map(this::safeTrace)
                    .orElseGet(() -> safeTrace(project.generationTrace()));

            ComicProject rolledBack = new ComicProject(
                    project.projectId(),
                    project.createdAt(),
                    project.input(),
                    version.synopsis(),
                    safeEpisodes(version.episodes()),
                    version.jobId(),
                    trace,
                    safeVersions(project.versions())
            );
            return projectRepository.save(rolledBack);
        });
    }

    public List<ComicProject> latestProjects(int limit) {
        return projectRepository.findLatest(limit).stream().map(this::normalizeProject).toList();
    }

    public Optional<String> exportProject(String projectId, String format) {
        return findById(projectId).map(project -> {
            String mode = format == null || format.isBlank() ? "json" : format.trim().toLowerCase();
            return switch (mode) {
                case "json" -> toJson(project);
                case "markdown", "md" -> toMarkdown(project);
                case "prompt", "prompts" -> toPromptPack(project);
                default -> throw new IllegalArgumentException("Unsupported export format: " + format);
            };
        });
    }

    private ComicProject runAndPersist(
            ComicProject baseProject,
            GenerationStep resumeFromStep,
            String retriedFromJobId,
            List<GenerationStepResult> previousTrace
    ) {
        String jobId = "job-" + UUID.randomUUID();
        Instant createdAt = Instant.now();
        jobRepository.save(new GenerationJob(
                jobId,
                baseProject.projectId(),
                JobStatus.QUEUED,
                resumeFromStep,
                retriedFromJobId,
                null,
                null,
                createdAt,
                null,
                null,
                createdAt,
                List.of()
        ));

        Instant startedAt = Instant.now();
        jobRepository.save(new GenerationJob(
                jobId,
                baseProject.projectId(),
                JobStatus.RUNNING,
                resumeFromStep,
                retriedFromJobId,
                null,
                null,
                createdAt,
                startedAt,
                null,
                startedAt,
                List.of()
        ));

        HarnessRunResult harnessResult = harnessOrchestrator.run(
                baseProject.projectId(),
                baseProject.input(),
                jobId,
                resumeFromStep,
                previousTrace == null ? List.of() : previousTrace
        );

        Instant endedAt = Instant.now();
        JobStatus finalStatus = harnessResult.succeeded() ? JobStatus.SUCCEEDED : JobStatus.FAILED;
        GenerationJob completedJob = new GenerationJob(
                jobId,
                baseProject.projectId(),
                finalStatus,
                resumeFromStep,
                retriedFromJobId,
                harnessResult.versionId(),
                harnessResult.error(),
                createdAt,
                startedAt,
                endedAt,
                endedAt,
                safeTrace(harnessResult.trace())
        );
        jobRepository.save(completedJob);

        String synopsis = harnessResult.succeeded() ? harnessResult.synopsis() : baseProject.synopsis();
        List<Episode> episodes = harnessResult.succeeded() ? safeEpisodes(harnessResult.episodes()) : safeEpisodes(baseProject.episodes());
        List<ProjectVersion> versions = appendVersionIfNeeded(baseProject, completedJob, synopsis, episodes);

        ComicProject updated = new ComicProject(
                baseProject.projectId(),
                baseProject.createdAt(),
                baseProject.input(),
                synopsis,
                episodes,
                completedJob.jobId(),
                safeTrace(harnessResult.trace()),
                versions
        );
        return projectRepository.save(updated);
    }

    private List<ProjectVersion> appendVersionIfNeeded(
            ComicProject baseProject,
            GenerationJob completedJob,
            String synopsis,
            List<Episode> episodes
    ) {
        List<ProjectVersion> current = safeVersions(baseProject.versions());
        if (completedJob.status() != JobStatus.SUCCEEDED || completedJob.versionId() == null || completedJob.versionId().isBlank()) {
            return current;
        }

        List<ProjectVersion> next = new ArrayList<>(current);
        next.add(new ProjectVersion(
                completedJob.versionId(),
                completedJob.jobId(),
                completedJob.endedAt(),
                synopsis,
                episodes
        ));
        return List.copyOf(next);
    }

    private GenerationStep resolveResumeFrom(GenerationJob sourceJob, RetryJobRequest request) {
        if (request != null && request.fromStep() != null) {
            return request.fromStep();
        }

        return safeTrace(sourceJob.trace()).stream()
                .filter(step -> step.status() == StepStatus.FAILED)
                .map(GenerationStepResult::step)
                .findFirst()
                .orElse(null);
    }

    private ComicProject normalizeProject(ComicProject project) {
        return new ComicProject(
                project.projectId(),
                project.createdAt(),
                project.input(),
                project.synopsis() == null ? "" : project.synopsis(),
                safeEpisodes(project.episodes()),
                project.latestJobId(),
                safeTrace(project.generationTrace()),
                safeVersions(project.versions())
        );
    }

    private List<Episode> safeEpisodes(List<Episode> episodes) {
        return episodes == null ? List.of() : List.copyOf(episodes);
    }

    private List<GenerationStepResult> safeTrace(List<GenerationStepResult> trace) {
        return trace == null ? List.of() : List.copyOf(trace);
    }

    private List<ProjectVersion> safeVersions(List<ProjectVersion> versions) {
        return versions == null ? List.of() : List.copyOf(versions);
    }

    private GenerationJob normalizeJob(GenerationJob job) {
        return new GenerationJob(
                job.jobId(),
                job.projectId(),
                job.status(),
                job.resumeFromStep(),
                job.retriedFromJobId(),
                job.versionId(),
                job.error(),
                job.createdAt(),
                job.startedAt(),
                job.endedAt(),
                job.updatedAt(),
                safeTrace(job.trace())
        );
    }

    private String toJson(ComicProject project) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(project);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize project export", e);
        }
    }

    private String toMarkdown(ComicProject project) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(project.input().title()).append('\n').append('\n');
        builder.append("## Synopsis").append('\n');
        builder.append(project.synopsis()).append('\n').append('\n');

        for (Episode episode : project.episodes()) {
            builder.append("## Episode ").append(episode.index()).append(": ").append(episode.title()).append('\n');
            builder.append(episode.summary()).append('\n').append('\n');
            episode.scenes().forEach(scene -> {
                builder.append("### Scene ").append(scene.index()).append(" - ").append(scene.location()).append('\n');
                builder.append("- Goal: ").append(scene.goal()).append('\n');
                builder.append("- Conflict: ").append(scene.conflictBeat()).append('\n');
                scene.panels().forEach(panel -> {
                    builder.append("- Panel ").append(panel.index())
                            .append(": ").append(panel.camera())
                            .append(" | ").append(panel.dialogue())
                            .append('\n');
                });
                builder.append('\n');
            });
        }
        return builder.toString();
    }

    private String toPromptPack(ComicProject project) {
        StringBuilder builder = new StringBuilder();
        builder.append("title: ").append(project.input().title()).append('\n');
        project.episodes().forEach(episode ->
                episode.scenes().forEach(scene ->
                        scene.panels().forEach(panel ->
                                builder.append("E").append(episode.index())
                                        .append("-S").append(scene.index())
                                        .append("-P").append(panel.index())
                                        .append(": ").append(panel.imagePrompt())
                                        .append('\n')
                        )
                )
        );
        return builder.toString();
    }
}
