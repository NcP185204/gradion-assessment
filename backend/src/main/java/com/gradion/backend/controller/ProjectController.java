package com.gradion.backend.controller;

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

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<ProjectDto> createProject(
            @AuthenticationPrincipal User user,
            @RequestPart(value = "title") String title,
            @RequestPart(value = "bookText", required = false) String bookText,
            @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {

        String content = bookText;
        if (file != null && !file.isEmpty()) {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        }

        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Project newProject = projectService.createProject(user.getId(), title, content);
        // We can call getProjectDetail to get a fully populated DTO, but that's less efficient.
        // For now, returning a simplified DTO is fine.
        ProjectDto projectDto = ProjectDto.builder()
                .id(newProject.getId())
                .title(newProject.getTitle())
                .overallStatus(newProject.getOverallStatus())
                .createdAt(newProject.getCreatedAt())
                .build();

        return new ResponseEntity<>(projectDto, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ProjectDto>> getProjects(@AuthenticationPrincipal User user) {
        List<ProjectDto> projects = projectService.getProjects(user.getId());
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDetailDto> getProjectDetail(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        ProjectDetailDto projectDetail = projectService.getProjectDetail(user.getId(), id);
        return ResponseEntity.ok(projectDetail);
    }
}
