package com.gradion.backend.service;

import com.gradion.backend.exception.StepAlreadyRunningException;
import com.gradion.backend.exception.StepNotReadyException;
import com.gradion.backend.exception.StepNotStuckException;
import com.gradion.backend.model.PipelineStep;
import com.gradion.backend.model.Project;
import com.gradion.backend.repository.PipelineStepRepository;
import com.gradion.backend.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineServiceTest {

    @Mock
    private PipelineStepRepository pipelineStepRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private PipelineService pipelineService;

    private static final Long PROJECT_ID = 1L;

    private PipelineStep makePipelineStep(int stepNumber, String status) {
        return PipelineStep.builder()
                .id((long) stepNumber)
                .project(Project.builder().id(PROJECT_ID).build())
                .stepNumber(stepNumber)
                .status(status)
                .build();
    }

    @Test
    void runStep_step1_success() {
        // Given
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(new Project()));
        PipelineStep step1 = makePipelineStep(1, "PENDING");
        when(pipelineStepRepository.findByProjectIdAndStepNumber(PROJECT_ID, 1)).thenReturn(Optional.of(step1));

        List<String> statusHistory = new ArrayList<>();
        when(pipelineStepRepository.save(any(PipelineStep.class))).thenAnswer(invocation -> {
            PipelineStep savedStep = invocation.getArgument(0);
            statusHistory.add(savedStep.getStatus());
            return savedStep;
        });

        // When
        pipelineService.runStep(PROJECT_ID, 1, null);

        // Then
        verify(pipelineStepRepository, times(2)).save(any(PipelineStep.class));
        assertEquals(List.of("RUNNING", "DONE"), statusHistory);
    }

    @Test
    void runStep_prerequisiteNotDone_throwsStepNotReadyException() {
        // Given
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(new Project()));
        PipelineStep step1 = makePipelineStep(1, "PENDING");
        when(pipelineStepRepository.findByProjectIdAndStepNumber(PROJECT_ID, 1)).thenReturn(Optional.of(step1));

        // When & Then
        assertThrows(StepNotReadyException.class, () -> pipelineService.runStep(PROJECT_ID, 2, null));
        verify(pipelineStepRepository, never()).save(any());
    }

    @Test
    void runStep_stepAlreadyRunning_throwsStepAlreadyRunningException() {
        // Given
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(new Project()));
        PipelineStep step1 = makePipelineStep(1, "RUNNING");
        when(pipelineStepRepository.findByProjectIdAndStepNumber(PROJECT_ID, 1)).thenReturn(Optional.of(step1));

        // When & Then
        assertThrows(StepAlreadyRunningException.class, () -> pipelineService.runStep(PROJECT_ID, 1, null));
        verify(pipelineStepRepository, never()).save(any());
    }

    @Test
    void runStep_stepIsFailed_throwsStepNotReadyException() {
        // Given
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(new Project()));
        PipelineStep step1 = makePipelineStep(1, "DONE");
        PipelineStep step2 = makePipelineStep(2, "FAILED");
        when(pipelineStepRepository.findByProjectIdAndStepNumber(PROJECT_ID, 1)).thenReturn(Optional.of(step1));
        when(pipelineStepRepository.findByProjectIdAndStepNumber(PROJECT_ID, 2)).thenReturn(Optional.of(step2));

        // When & Then
        assertThrows(StepNotReadyException.class, () -> pipelineService.runStep(PROJECT_ID, 2, null), "A FAILED step must use the retry endpoint.");
        verify(pipelineStepRepository, never()).save(any());
    }

    @Test
    void retryStep_failedStep_resetsAndRuns() {
        // Given
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(new Project()));
        PipelineStep step1 = makePipelineStep(1, "FAILED");
        when(pipelineStepRepository.findByProjectIdAndStepNumber(PROJECT_ID, 1)).thenReturn(Optional.of(step1));

        List<String> statusHistory = new ArrayList<>();
        when(pipelineStepRepository.save(any(PipelineStep.class))).thenAnswer(invocation -> {
            PipelineStep savedStep = invocation.getArgument(0);
            statusHistory.add(savedStep.getStatus());
            return savedStep;
        });

        // When
        pipelineService.retryStep(PROJECT_ID, 1);

        // Then
        verify(pipelineStepRepository, times(3)).save(any(PipelineStep.class));
        assertEquals(List.of("PENDING", "RUNNING", "DONE"), statusHistory);
    }

    @Test
    void retryStep_notFailedStep_throwsException() {
        // Given
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(new Project())); // FIX: Added this line
        PipelineStep step1 = makePipelineStep(1, "DONE");
        when(pipelineStepRepository.findByProjectIdAndStepNumber(PROJECT_ID, 1)).thenReturn(Optional.of(step1));

        // When & Then
        assertThrows(StepNotReadyException.class, () -> pipelineService.retryStep(PROJECT_ID, 1));
        verify(pipelineStepRepository, never()).save(any());
    }

    @Test
    void isStepStuck_runningOver5Minutes_returnsTrue() {
        // Given
        PipelineStep step = makePipelineStep(1, "RUNNING");
        step.setStartedAt(LocalDateTime.now().minusMinutes(6));

        // When
        boolean isStuck = pipelineService.isStepStuck(step);

        // Then
        assertTrue(isStuck);
    }

    @Test
    void isStepStuck_runningUnder5Minutes_returnsFalse() {
        // Given
        PipelineStep step = makePipelineStep(1, "RUNNING");
        step.setStartedAt(LocalDateTime.now().minusMinutes(2));

        // When
        boolean isStuck = pipelineService.isStepStuck(step);

        // Then
        assertFalse(isStuck);
    }

    @Test
    void resetStuckStep_notStuck_throwsStepNotStuckException() {
        // Given
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(new Project())); // FIX: Added this line
        PipelineStep step = makePipelineStep(1, "RUNNING");
        step.setStartedAt(LocalDateTime.now().minusMinutes(2)); // Not stuck
        when(pipelineStepRepository.findByProjectIdAndStepNumber(PROJECT_ID, 1)).thenReturn(Optional.of(step));

        // When & Then
        assertThrows(StepNotStuckException.class, () -> pipelineService.resetStuckStep(PROJECT_ID, 1));
        verify(pipelineStepRepository, never()).save(any());
    }
}
