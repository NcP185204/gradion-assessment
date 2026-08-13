# TESTING.md

## Strategy

Testing is split by what actually carries risk, not by coverage target.

### Backend (JUnit 5 + Mockito)

The graded logic is **step ordering, progress/resume state, retry, and the
duplicate-call guarantee**, so that's exactly what we pin in
`PipelineServiceTest`:

- step 1 runs and dispatches exactly one async job (no synchronous Gemini call);
- `claimStepForRunning` returning 0 → `StepAlreadyRunningException` (409) and
  **no** async dispatch — the whole "no duplicate calls" guarantee;
- a FAILED step is rejected by `runStep` and must go through `retryStep`;
- prerequisite-not-DONE is rejected;
- `retryStep` resets FAILED → PENDING and re-dispatches;
- `retryStep` on a non-FAILED step is rejected;
- `isStepStuck` true/false for the >5-min boundary and non-RUNNING;
- `resetStuckStep` marks a stale RUNNING step FAILED, and rejects a non-stuck one.

`AuthServiceTest` covers login-or-register: new email creates, existing email
loads, a token is always returned, and null/empty email throws.

### Backend — deliberately NOT tested

- **The Gemini HTTP calls themselves** (`GeminiClient`, `StepRunner`'s network
  path). These are exercised manually against the real API, not mocked, to keep
  the suite fast and deterministic without burning quota. The seam where they
  plug in (`runStepAsync`) *is* unit-tested via the `StepRunner` mock.
- **Image storage bytes** — covered implicitly by manual runs; hashing/writing
  is thin over `java.nio.file`.

### Frontend (Vitest + Testing Library)

"Pick a couple that matter" — we cover components and the states the spec calls
out:

- `Stepper` — renders all five labels, marks RUNNING, marks FAILED.
- `ProjectListPage` — **empty** state (no projects), **list** state (title +
  "In progress" status pill), and **error** state (API failure banner).

### Frontend — deliberately NOT tested

- `ProjectDetailPage`'s polling loop (timer-based, would be a flaky test for
  little signal). The polling *hook* is trivial composition.
- Visual/layout snapshots — "match or beat the demo" is a human UAT pass, not an
  assertion.

### Not attempted

- E2E (the spec says it's not expected). A mock-Gemini integration test through
  all 5 steps is the nice-to-have we skipped to keep the harness simple.

---

## Test report (real run)

### Backend — `./mvnw test`

```
Tests run: 1, Failures: 0, Errors: 0  -- BackendApplicationTests
Tests run: 9, Failures: 0, Errors: 0  -- PipelineServiceTest
Tests run: 4, Failures: 0, Errors: 0  -- AuthServiceTest
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Frontend — `npm test`

```
✓ src/test/stepper.test.jsx (3 tests)
✓ src/test/projectList.test.jsx (3 tests)

Test Files  2 passed (2)
     Tests  6 passed (6)
```

Combined: **20 tests, 20 passing** across both sides.