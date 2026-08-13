package com.gradion.backend.controller;

import com.gradion.backend.dto.PipelineStepDto;
import com.gradion.backend.dto.RunStepRequest;
import com.gradion.backend.service.PipelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/steps")
@RequiredArgsConstructor
public class StepController {

    private final PipelineService pipelineService;

    @PostMapping("/{stepNumber}/run")
    public ResponseEntity<PipelineStepDto> runStep(
            @PathVariable Long projectId,
            @PathVariable int stepNumber,
            @RequestBody(required = false) RunStepRequest request) {
        String customStyle = (request != null) ? request.getCustomStyle() : null;
        PipelineStepDto result = pipelineService.runStep(projectId, stepNumber, customStyle);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{stepNumber}/retry")
    public ResponseEntity<PipelineStepDto> retryStep(
            @PathVariable Long projectId,
            @PathVariable int stepNumber) {
        PipelineStepDto result = pipelineService.retryStep(projectId, stepNumber);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{stepNumber}/reset-stuck")
    public ResponseEntity<PipelineStepDto> resetStuckStep(
            @PathVariable Long projectId,
            @PathVariable int stepNumber) {
        PipelineStepDto result = pipelineService.resetStuckStep(projectId, stepNumber);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<List<PipelineStepDto>> getSteps(@PathVariable Long projectId) {
        List<PipelineStepDto> steps = pipelineService.getSteps(projectId);
        return ResponseEntity.ok(steps);
    }
}
