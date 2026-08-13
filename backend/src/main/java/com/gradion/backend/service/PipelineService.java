package com.gradion.backend.service;

import com.gradion.backend.dto.PipelineStepDto;
import com.gradion.backend.exception.ResourceNotFoundException;
import com.gradion.backend.exception.StepAlreadyRunningException;
import com.gradion.backend.exception.StepNotReadyException;
import com.gradion.backend.exception.StepNotStuckException;
import com.gradion.backend.model.PipelineStep;
import com.gradion.backend.repository.PipelineStepRepository;
import com.gradion.backend.repository.ProjectRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Orchestrates the 5-step pipeline.
 *
 * <p>Duplicate-call prevention is the heart of this class. Instead of a
 * read-then-write check (which has a race window between two concurrent
 * requests), {@code runStep} uses a single conditional UPDATE
 * ({@link PipelineStepRepository#claimStepForRunning}) that atomically flips
 * PENDING/FAILED -> RUNNING. Exactly one concurrent request wins; the loser
 * sees 0 rows updated and gets a 409 without ever touching Gemini.
 *
 * <p>Once claimed, the step is handed to {@link StepRunner} which executes it
 * asynchronously against the real Gemini API. The HTTP request returns
 * immediately with the RUNNING state so the UI can poll.
 */
@Service
@RequiredArgsConstructor
public class PipelineService {

    private final PipelineStepRepository pipelineStepRepository;
    private final ProjectRepository projectRepository;
    private final StepRunner stepRunner;

    private static final long STUCK_TIMEOUT_MINUTES = 5;

    /**
     * Validates ordering, atomically claims the step, and kicks off the async
     * Gemini execution. Returns the claimed (RUNNING) step.
     */
    @Transactional
    public PipelineStepDto runStep(Long projectId, int stepNumber, String customStyle) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        // Rule 1: prerequisite must be DONE.
        if (stepNumber > 1) {
            PipelineStep previousStep = pipelineStepRepository
                    .findByProjectIdAndStepNumber(projectId, stepNumber - 1)
                    .orElseThrow(() -> new StepNotReadyException(
                            "Prerequisite step " + (stepNumber - 1) + " does not exist."));
            if (!"DONE".equals(previousStep.getStatus())) {
                throw new StepNotReadyException("Prerequisite step " + (stepNumber - 1)
                        + " is not DONE. Current status: " + previousStep.getStatus());
            }
        }

        PipelineStep step = pipelineStepRepository
                .findByProjectIdAndStepNumber(projectId, stepNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Step " + stepNumber + " not found for project " + projectId));

        // Rule 2: a FAILED step must go through the retry endpoint.
        if ("FAILED".equals(step.getStatus())) {
            throw new StepNotReadyException("Step " + stepNumber
                    + " has FAILED. Please use the retry endpoint.");
        }

        // Rule 3: atomic claim — the duplicate-call guard.
        int claimed = pipelineStepRepository.claimStepForRunning(step.getId(), LocalDateTime.now());
        if (claimed == 0) {
            throw new StepAlreadyRunningException("Step " + stepNumber + " is already running.");
        }

        // Fire-and-forget the real Gemini work; the UI polls for progress.
        stepRunner.runStepAsync(projectId, stepNumber, customStyle);

        PipelineStep running = pipelineStepRepository
                .findByProjectIdAndStepNumber(projectId, stepNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Step " + stepNumber + " not found for project " + projectId));
        return PipelineStepDto.fromEntity(running);
    }

    /**
     * Retries a FAILED step only. Resets to PENDING then re-runs through the
     * same atomic claim path — completed steps are never touched.
     */
    @Transactional
    public PipelineStepDto retryStep(Long projectId, int stepNumber) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
        PipelineStep step = pipelineStepRepository
                .findByProjectIdAndStepNumber(projectId, stepNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Step " + stepNumber + " not found for project " + projectId));

        if (!"FAILED".equals(step.getStatus())) {
            throw new StepNotReadyException("Step " + stepNumber
                    + " cannot be retried as it is not in FAILED state. Current status: " + step.getStatus());
        }

        // Reset to PENDING so the atomic claim in runStep can pick it up.
        step.setStatus("PENDING");
        step.setStartedAt(null);
        step.setCompletedAt(null);
        step.setResultJson(null);
        step.setProgressJson(null);
        pipelineStepRepository.save(step);

        return runStep(projectId, stepNumber, null);
    }

    /**
     * Recovers a step stranded in RUNNING (server died mid-call). Only allowed
     * when the step has been RUNNING for longer than the stuck timeout.
     */
    @Transactional
    public PipelineStepDto resetStuckStep(Long projectId, int stepNumber) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
        PipelineStep step = pipelineStepRepository
                .findByProjectIdAndStepNumber(projectId, stepNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Step " + stepNumber + " not found for project " + projectId));

        if (!isStepStuck(step)) {
            throw new StepNotStuckException("Step " + stepNumber
                    + " is not stuck. It must be in RUNNING state for more than "
                    + STUCK_TIMEOUT_MINUTES + " minutes.");
        }

        step.setStatus("FAILED");
        step.setCompletedAt(LocalDateTime.now());
        step.setResultJson("{\"error\": \"Reset due to timeout.\"}");
        PipelineStep savedStep = pipelineStepRepository.save(step);

        return PipelineStepDto.fromEntity(savedStep);
    }

    public List<PipelineStepDto> getSteps(Long projectId) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
        return pipelineStepRepository.findByProjectIdOrderByStepNumber(projectId)
                .stream()
                .map(PipelineStepDto::fromEntity)
                .collect(Collectors.toList());
    }

    public boolean isStepStuck(PipelineStep step) {
        if (!"RUNNING".equals(step.getStatus()) || step.getStartedAt() == null) {
            return false;
        }
        long minutesSinceStart = ChronoUnit.MINUTES.between(step.getStartedAt(), LocalDateTime.now());
        return minutesSinceStart > STUCK_TIMEOUT_MINUTES;
    }
}