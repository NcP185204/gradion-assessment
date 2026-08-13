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

@Service
@RequiredArgsConstructor
public class PipelineService {

    private final PipelineStepRepository pipelineStepRepository;
    private final ProjectRepository projectRepository;
    // private final GeminiService geminiService; // Will be needed later

    private static final long STUCK_TIMEOUT_MINUTES = 5;

    @Transactional
    public PipelineStepDto runStep(Long projectId, int stepNumber, String customStyle) {
        projectRepository.findById(projectId).orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        // Rule 1: Check if prerequisite step is DONE
        if (stepNumber > 1) {
            PipelineStep previousStep = pipelineStepRepository.findByProjectIdAndStepNumber(projectId, stepNumber - 1)
                    .orElseThrow(() -> new StepNotReadyException("Prerequisite step " + (stepNumber - 1) + " does not exist."));
            if (!"DONE".equals(previousStep.getStatus())) {
                throw new StepNotReadyException("Prerequisite step " + (stepNumber - 1) + " is not DONE. Current status: " + previousStep.getStatus());
            }
        }

        PipelineStep step = pipelineStepRepository.findByProjectIdAndStepNumber(projectId, stepNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Step " + stepNumber + " not found for project " + projectId));

        // Rule 2: Check if step is already RUNNING
        if ("RUNNING".equals(step.getStatus())) {
            throw new StepAlreadyRunningException("Step " + stepNumber + " is already running.");
        }

        // New Rule: A FAILED step must be retried, not run directly.
        if ("FAILED".equals(step.getStatus())) {
            throw new StepNotReadyException("Step " + stepNumber + " has FAILED. Please use the retry endpoint.");
        }

        // Mark step as RUNNING
        step.setStatus("RUNNING");
        step.setStartedAt(LocalDateTime.now());
        step.setCompletedAt(null);
        pipelineStepRepository.save(step);

        // TODO: Call Gemini Service asynchronously here
        // For now, we'll just simulate a long-running process and mark as DONE
        // In a real scenario, this would be handled by an @Async method
        // geminiService.executeStep(step, customStyle);

        // Simulating completion for now
        step.setStatus("DONE");
        step.setCompletedAt(LocalDateTime.now());
        step.setResultJson("{\"message\": \"Step " + stepNumber + " completed successfully.\"}"); // Placeholder result
        PipelineStep savedStep = pipelineStepRepository.save(step);

        return PipelineStepDto.fromEntity(savedStep);
    }

    @Transactional
    public PipelineStepDto retryStep(Long projectId, int stepNumber) {
        projectRepository.findById(projectId).orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
        PipelineStep step = pipelineStepRepository.findByProjectIdAndStepNumber(projectId, stepNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Step " + stepNumber + " not found for project " + projectId));

        if (!"FAILED".equals(step.getStatus())) {
            throw new StepNotReadyException("Step " + stepNumber + " cannot be retried as it is not in FAILED state. Current status: " + step.getStatus());
        }

        // Reset to PENDING before running again
        step.setStatus("PENDING");
        step.setStartedAt(null);
        step.setCompletedAt(null);
        step.setResultJson(null);
        pipelineStepRepository.save(step);

        // Now, call the main run logic
        return runStep(projectId, stepNumber, null); // Assuming retry doesn't need a new custom style
    }

    @Transactional
    public PipelineStepDto resetStuckStep(Long projectId, int stepNumber) {
        projectRepository.findById(projectId).orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
        PipelineStep step = pipelineStepRepository.findByProjectIdAndStepNumber(projectId, stepNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Step " + stepNumber + " not found for project " + projectId));

        // Rule 3: Check if the step is actually stuck
        if (!isStepStuck(step)) {
            throw new StepNotStuckException("Step " + stepNumber + " is not stuck. It must be in RUNNING state for more than " + STUCK_TIMEOUT_MINUTES + " minutes.");
        }

        step.setStatus("FAILED");
        step.setCompletedAt(LocalDateTime.now());
        step.setResultJson("{\"error\": \"Reset due to timeout.\"}");
        PipelineStep savedStep = pipelineStepRepository.save(step);

        return PipelineStepDto.fromEntity(savedStep);
    }

    public List<PipelineStepDto> getSteps(Long projectId) {
        projectRepository.findById(projectId).orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
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
