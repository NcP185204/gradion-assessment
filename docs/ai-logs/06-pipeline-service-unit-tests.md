# 07 - PipelineService Unit Tests

## 1. User Prompt
Write JUnit 5 unit tests for PipelineService.java
File: src/test/java/com/gradion/backend/service/PipelineServiceTest.java

Use @ExtendWith(MockitoExtension.class) — pure unit tests, no Spring context.
Mock: PipelineStepRepository, ProjectRepository

Test cases (all 8 required):
1. runStep_step1_success
2. runStep_prerequisiteNotDone_throwsStepNotReadyException
3. runStep_stepAlreadyRunning_throwsStepAlreadyRunningException
4. runStep_stepIsFailed_throwsStepNotReadyException
5. retryStep_failedStep_resetsAndRuns
6. retryStep_notFailedStep_throwsException
7. isStepStuck_runningOver5Minutes_returnsTrue
8. isStepStuck_runningUnder5Minutes_returnsFalse
9. resetStuckStep_notStuck_throwsStepNotStuckException

Helper: create a helper method makePipelineStep(int stepNumber, String status) to avoid repetition across tests.

---

## 2. Gemini Response (Summary)
Generated pure unit tests using Mockito (`@ExtendWith(MockitoExtension.class)`) for `PipelineService`, mocking `PipelineStepRepository` and `ProjectRepository` without loading the Spring context, covering all required success, exception, retry, and stuck-step verification cases along with the helper method `makePipelineStep`.

---

## 3. Developer Notes / Actions Taken
- Successfully created `PipelineServiceTest.java` covering all business constraints, sequence validation, and timeout checks.