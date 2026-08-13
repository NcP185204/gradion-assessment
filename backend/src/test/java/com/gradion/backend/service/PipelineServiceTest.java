package com.gradion.backend.service;

import com.gradion.backend.dto.PipelineStepDto;
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

    @Mock
    private StepRunner stepRunner;

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
    void runStep_step1_success_dispatchingAsync() {
        // Given
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(new Project()));
        PipelineStep step1 = makePipelineStep(1, "PENDING");
        PipelineStep running = makePipelineStep(1, "RUNNING");
        when(pipelineStepRepository.findByProjectIdAndStepNumber(PROJECT_ID, 1))
                .thenReturn(Optional.of(step1), Optional.of(running));
        when(pipelineStepRepository.claimStepForRunning(any(), any(LocalDateTime.class))).thenReturn(1);

        // When
        PipelineStepDto result = pipelineService.runStep(PROJECT_ID, 1, null);

        // Then — one async dispatch; no synchronous save of DONE.
        verify(stepRunner, times(1)).runStepAsync(PROJECT_ID, 1, null);
        assertEquals("RUNNING", result.getStatus());
        verify(pipelineStepRepository, never()).save(any(PipelineStep.class));
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
    void runStep_stepAlreadyRunning_claimReturnsZero_throws409_andNoAsyncDispatch() {
        // Given — step is RUNNING already; the atomic claim cannot flip it.
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(new Project()));
        PipelineStep step1 = makePipelineStep(1, "RUNNING");
        when(pipelineStepRepository.findByProjectIdAndStepNumber(PROJECT_ID, 1)).thenReturn(Optional.of(step1));
        when(pipelineStepRepository.claimStepForRunning(any(), any(LocalDateTime.class))).thenReturn(0);

        // When & Then
        assertThrows(StepAlreadyRunningException.class, () -> pipelineService.runStep(PROJECT_ID, 1, null));
        verify(stepRunner, never()).runStepAsync(any(), anyInt(), any());
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
        PipelineStep running = makePipelineStep(1, "RUNNING");
        when(pipelineStepRepository.findByProjectIdAndStepNumber(PROJECT_ID, 1))
                .thenReturn(Optional.of(step1), Optional.of(running));
        when(pipelineStepRepository.claimStepForRunning(any(), any(LocalDateTime.class))).thenReturn(1);

        // When
        pipelineService.retryStep(PROJECT_ID, 1);

        // Then — reset to PENDING (one save), then claimed + async-dispatched.
        verify(pipelineStepRepository, times(1)).save(any(PipelineStep.class));
        assertEquals("PENDING", step1.getStatus());
        verify(stepRunner, times(1)).runStepAsync(PROJECT_ID, 1, null);
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
