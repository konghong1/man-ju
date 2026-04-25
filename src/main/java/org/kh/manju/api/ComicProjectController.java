package org.kh.manju.api;

import jakarta.validation.Valid;
import org.kh.manju.model.ComicProject;
import org.kh.manju.model.CreateProjectRequest;
import org.kh.manju.service.ComicProjectService;
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
import org.springframework.web.server.ResponseStatusException;

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

    @GetMapping("/projects")
    public List<ComicProject> latest(@RequestParam(defaultValue = "10") int limit) {
        return comicProjectService.latestProjects(limit);
    }
}
