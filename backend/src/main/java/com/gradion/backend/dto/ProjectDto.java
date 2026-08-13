package com.gradion.backend.dto;

import com.gradion.backend.model.Project;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectDto {
    private Long id;
    private String title;
    private String overallStatus;
    private LocalDateTime createdAt;
    private List<PipelineStepDto> steps;

    public static ProjectDto fromEntity(Project project, List<PipelineStepDto> steps) {
        return ProjectDto.builder()
                .id(project.getId())
                .title(project.getTitle())
                .overallStatus(project.getOverallStatus())
                .createdAt(project.getCreatedAt())
                .steps(steps)
                .build();
    }
}
