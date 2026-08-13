# Test Execution Report - AuthService

## 1. Test Summary
* **Total Tests:** 4
* **Passed:** 4
* **Failed:** 0
* **Execution Time:** ~996 ms
* **Coverage:** 100% Classes, 94% Methods, 62% Branch (AuthService)

## 2. Console Output Log
```text
4 tests passed (4 tests total, 996 ms)
  ✓ loginOrRegister_withNewEmail_createsNewUserAndReturnsToken() (979 ms)
  ✓ loginOrRegister_withExistingEmail_loadsExistingUserAndReturnsToken() (5 ms)
  ✓ loginOrRegister_returnedTokenIsNotNullAndNotEmpty() (7 ms)
  ✓ loginOrRegister_withNullEmail_throwsIllegalArgumentException() (5 ms)

Process finished with exit code 0
```

---

# Test Execution Report - PipelineService

## 1. Test Summary
* **Total Tests:** 9
* **Passed:** 9
* **Failed:** 0
* **Execution Time:** ~1.319 s
* **Coverage:** 100% Classes, 100% Methods, 95% Branch (PipelineService)
* **Scope:** Core pipeline execution engine (sequential ordering, concurrency control, stuck-step timeout evaluation, manual-only retries)

## 2. Console Output Log
```text
9 tests passed (9 tests total, 1 sec 319 ms)
  ✓ runStep_stepAlreadyRunning_throwsStepAlreadyRunningException()
  ✓ runStep_stepIsFailed_throwsStepNotReadyException()
  ✓ isStepStuck_runningOver5Minutes_returnsTrue()
  ✓ retryStep_failedStep_resetsAndRuns()
  ✓ runStep_step1_success()
  ✓ resetStuckStep_notStuck_throwsStepNotStuckException()
  ✓ retryStep_notFailedStep_throwsException()
  ✓ isStepStuck_runningUnder5Minutes_returnsFalse()
  ✓ runStep_prerequisiteNotDone_throwsStepNotReadyException()

Process finished with exit code 0
```