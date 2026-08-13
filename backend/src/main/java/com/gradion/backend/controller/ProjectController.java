package com.gradion.backend.controller;

import com.gradion.backend.dto.CreateProjectRequest;
import com.gradion.backend.dto.ProjectDetailDto;
import com.gradion.backend.dto.ProjectDto;
import com.gradion.backend.model.Project;
import com.gradion.backend.model.User;
import com.gradion.backend.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /**
     * Creates a project from pasted book text (JSON body).
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProjectDto> createProjectFromText(
            @AuthenticationPrincipal User user,
            @RequestBody CreateProjectRequest request) {

        if (request.getTitle() == null || request.getTitle().isBlank()
                || request.getBookText() == null || request.getBookText().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Project newProject = projectService.createProject(user.getId(), request.getTitle(), request.getBookText());
        return new ResponseEntity<>(toDto(newProject), HttpStatus.CREATED);
    }

    /**
     * Creates a project from an uploaded .txt file (multipart).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProjectDto> createProjectFromFile(
            @AuthenticationPrincipal User user,
            @RequestPart("title") String title,
            @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String bookText = new String(file.getBytes(), StandardCharsets.UTF_8);
        if (bookText.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Project newProject = projectService.createProject(user.getId(), title, bookText);
        return new ResponseEntity<>(toDto(newProject), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ProjectDto>> getProjects(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(projectService.getProjects(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDetailDto> getProjectDetail(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(projectService.getProjectDetail(user.getId(), id));
    }

    private ProjectDto toDto(Project p) {
        return ProjectDto.builder()
                .id(p.getId())
                .title(p.getTitle())
                .overallStatus(p.getOverallStatus())
                .createdAt(p.getCreatedAt())
                .build();
    }
}