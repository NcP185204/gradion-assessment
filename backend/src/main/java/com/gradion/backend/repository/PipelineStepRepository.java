package com.gradion.backend.repository;

import com.gradion.backend.model.PipelineStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PipelineStepRepository extends JpaRepository<PipelineStep, Long> {
    List<PipelineStep> findByProjectIdOrderByStepNumber(Long projectId);
    Optional<PipelineStep> findByProjectIdAndStepNumber(Long projectId, int stepNumber);

    /**
     * Atomically claims a step for execution. The conditional UPDATE is the
     * duplicate-call guard: two concurrent requests (double-click, second tab,
     * refresh) both try to flip PENDING/FAILED -> RUNNING, but the database
     * only lets one win. The loser gets 0 rows and never reaches Gemini.
     *
     * @return number of rows updated (1 = claimed, 0 = already running/done)
     */
    @Modifying
    @Query("""
            UPDATE PipelineStep s
               SET s.status = 'RUNNING',
                   s.startedAt = :now,
                   s.completedAt = null,
                   s.resultJson = null,
                   s.progressJson = null
             WHERE s.id = :stepId
               AND s.status IN ('PENDING', 'FAILED')
            """)
    int claimStepForRunning(@Param("stepId") Long stepId, @Param("now") LocalDateTime now);
}
