package com.gradion.backend.service;

import com.gradion.backend.dto.CharacterDto;
import com.gradion.backend.dto.ChapterDto;
import com.gradion.backend.dto.ProjectDetailDto;
import com.gradion.backend.dto.ProjectDto;
import com.gradion.backend.dto.PipelineStepDto;
import com.gradion.backend.exception.ResourceNotFoundException;
import com.gradion.backend.model.PipelineStep;
import com.gradion.backend.model.Project;
import com.gradion.backend.model.User;
import com.gradion.backend.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final PipelineStepRepository pipelineStepRepository;
    private final CharacterRepository characterRepository;
    private final ChapterRepository chapterRepository;

    @Transactional
    public Project createProject(Long userId, String title, String bookText) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Project project = Project.builder()
                .user(user)
                .title(title)
                .bookText(bookText)
                .overallStatus("CREATED")
                .build();

        Project savedProject = projectRepository.save(project);

        // Auto-create 5 pipeline steps
        List<PipelineStep> steps = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> PipelineStep.builder()
                        .project(savedProject)
                        .stepNumber(i)
                        .status("PENDING")
                        .build())
                .collect(Collectors.toList());
        pipelineStepRepository.saveAll(steps);

        return savedProject;
    }

    public List<ProjectDto> getProjects(Long userId) {
        return projectRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(project -> {
                    List<PipelineStepDto> stepDtos = pipelineStepRepository.findByProjectIdOrderByStepNumber(project.getId())
                            .stream()
                            .map(PipelineStepDto::fromEntity)
                            .collect(Collectors.toList());
                    return ProjectDto.fromEntity(project, stepDtos);
                })
                .collect(Collectors.toList());
    }

    public ProjectDetailDto getProjectDetail(Long userId, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        // Verify project belongs to the current user
        if (!project.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to view this project.");
        }

        List<PipelineStepDto> steps = pipelineStepRepository.findByProjectIdOrderByStepNumber(projectId)
                .stream().map(PipelineStepDto::fromEntity).collect(Collectors.toList());
        List<CharacterDto> characters = characterRepository.findByProjectIdOrderByDisplayOrder(projectId)
                .stream().map(CharacterDto::fromEntity).collect(Collectors.toList());
        List<ChapterDto> chapters = chapterRepository.findByProjectId(projectId)
                .stream().map(ChapterDto::fromEntity).collect(Collectors.toList());

        return ProjectDetailDto.fromEntity(project, steps, characters, chapters);
    }
}
