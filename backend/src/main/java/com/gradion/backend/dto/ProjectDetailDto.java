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
public class ProjectDetailDto {
    private Long id;
    private String title;
    private String bookText;
    private String style;
    private String overallStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PipelineStepDto> steps;
    private List<CharacterDto> characters;
    private List<ChapterDto> chapters;

    public static ProjectDetailDto fromEntity(Project project, List<PipelineStepDto> steps, List<CharacterDto> characters, List<ChapterDto> chapters) {
        return ProjectDetailDto.builder()
                .id(project.getId())
                .title(project.getTitle())
                .bookText(project.getBookText())
                .style(project.getStyle())
                .overallStatus(project.getOverallStatus())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .steps(steps)
                .characters(characters)
                .chapters(chapters)
                .build();
    }
}
