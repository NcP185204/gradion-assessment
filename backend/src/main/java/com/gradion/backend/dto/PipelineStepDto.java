package com.gradion.backend.dto;

import com.gradion.backend.model.PipelineStep;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PipelineStepDto {
    private Long id;
    private int stepNumber;
    private String status;
    private String resultJson;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public static PipelineStepDto fromEntity(PipelineStep entity) {
        return PipelineStepDto.builder()
                .id(entity.getId())
                .stepNumber(entity.getStepNumber())
                .status(entity.getStatus())
                .resultJson(entity.getResultJson())
                .startedAt(entity.getStartedAt())
                .completedAt(entity.getCompletedAt())
                .build();
    }
}
