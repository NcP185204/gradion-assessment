package com.gradion.backend.repository;

import com.gradion.backend.model.PipelineStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PipelineStepRepository extends JpaRepository<PipelineStep, Long> {
    List<PipelineStep> findByProjectIdOrderByStepNumber(Long projectId);
    Optional<PipelineStep> findByProjectIdAndStepNumber(Long projectId, int stepNumber);
}
