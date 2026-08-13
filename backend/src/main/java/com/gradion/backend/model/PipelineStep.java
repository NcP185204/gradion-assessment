package com.gradion.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "pipeline_steps")
public class PipelineStep {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private int stepNumber;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Lob
    private String resultJson;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;
}
