# 05 - Pipeline Step Execution Engine, Controller & Exception Handling

## 1. User Prompt
Create the pipeline step execution engine in com.gradion.backend.service.PipelineService.java

Rules to enforce (these are CRITICAL requirements):
1. Steps must run in order 1→5. Step N cannot start if step N-1 is not DONE.
2. If a step status is RUNNING → return error immediately, do NOT proceed. No duplicate Gemini calls.
3. If a step status is RUNNING and startedAt is more than 5 minutes ago → it is stuck. Allow reset to FAILED.
4. Failed steps can be retried (reset to PENDING then run again).
5. Never auto-retry — retries are user-triggered only.

Methods needed:
- runStep(Long projectId, int stepNumber, String customStyle) throws exception if rules violated
- retryStep(Long projectId, int stepNumber)
- resetStuckStep(Long projectId, int stepNumber)
- getSteps(Long projectId) → List<PipelineStepDto>
- isStepStuck(PipelineStep step) → boolean (startedAt > 5 min ago and status = RUNNING)

Exceptions to throw:
- StepAlreadyRunningException (HTTP 409) — step is currently RUNNING
- StepNotReadyException (HTTP 400) — prerequisite step not DONE
- StepNotStuckException (HTTP 400) — step is not stuck, cannot reset

Create GlobalExceptionHandler.java to return consistent JSON errors:
{ "error": "STEP_ALREADY_RUNNING", "message": "..." }

Also create StepController.java:
- POST /api/projects/{id}/steps/{n}/run (body: optional customStyle)
- POST /api/projects/{id}/steps/{n}/retry
- POST /api/projects/{id}/steps/{n}/reset-stuck
- GET /api/projects/{id}/steps

---

## 2. Gemini Response (Summary)
- **Custom Exceptions:** Created `StepAlreadyRunningException`, `StepNotReadyException`, `StepNotStuckException`, and `ResourceNotFoundException`.
- **Global Exception Handler:** Set up `GlobalExceptionHandler` to catch custom exceptions and return structured JSON error structures with matching HTTP status codes.
- **PipelineService:** Implemented core sequential logic (1→5), concurrency guards, stuck-step timeout evaluation (> 5 minutes), and manual-only retries.
- **StepController:** Exposed the 4 standard endpoints (`/run`, `/retry`, `/reset-stuck`, and GET `/`) for pipeline management.
- **DTOs:** Added `PipelineStepDto`, `RunStepRequest`, and `ErrorResponse`.

---

## 3. Developer Notes / Actions Taken
- Verified successful integration of exception mapping and service logic.
- Confirmed strict adherence to rule constraints regarding execution order and user-triggered retries.