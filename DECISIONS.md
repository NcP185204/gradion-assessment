# DECISIONS.md

## Stack choice: Java Spring Boot + React

I chose Spring Boot for the backend because I have real project experience with it
(GoPlay booking platform). React with Vite for frontend because AI can generate
components quickly and I can review/wire them to the backend.

AI (Gemini assistant) suggested using Node.js/Express for a "lighter" stack.
I pushed back — Spring Boot gives me JPA, Security, and Validation out of the box,
and I'm faster with it. The overhead is worth the familiarity.

---

## Storage: H2 embedded database (not JSON files)

My call. The spec says JSON files are valid, but concurrent write safety with
JSON requires a per-project lock — that's the same complexity as a DB without
the query language. H2 gives me JPA, transactions, and no extra code.

AI agreed on this one without pushback.

Cost: H2 resets on in-memory mode — I'm using file mode (`jdbc:h2:file:`) so data
persists across restarts. This satisfies the resumable requirement.

### AI Override: JJWT Library Version Mismatch

**Decision:**
Manually refactored the JWT parsing logic in `JwtUtil.java` to support JJWT version `0.12.x`.

**Rationale:**
The AI assistant generated code using the deprecated `0.11.x` syntax (`Jwts.parserBuilder()`, `parseClaimsJws()`, `getBody()`). Since the project enforces the newer `0.12.x` standard, I overrode the AI's implementation, migrating it to the modern API (`Jwts.parser().verifyWith()`, `parseSignedClaims()`, `getPayload()`). This demonstrates active code review and ensures up-to-date dependency compatibility.

### Pure Unit Testing Strategy for AuthService
- **Decision:** Implemented pure unit tests using Mockito extensions instead of heavy Spring Boot integration tests.
- **Rationale:** Ensures fast execution and isolates the service layer logic completely. Added null-safety validation directly into `AuthService` to meet strict error-handling test criteria.

### Pipeline state model: two fields instead of one enum

Gemini initially proposed a single status enum:
PENDING/RUNNING/DONE/FAILED/STUCK.

I pushed back — STUCK is not a real status, it is a derived condition
(status=RUNNING AND startedAt > 5 minutes ago). Storing STUCK in DB
means we need a cleanup job to unstick them. Using isStepStuck() as a
computed boolean keeps the DB clean and the retry path simple.

Split into: status (PENDING/RUNNING/DONE/FAILED) + startedAt timestamp.
Cost: every stuck-check needs a time comparison. Acceptable at this scale.

---

### Pipeline execution: synchronous now, async later

Gemini generated runStep() as fully synchronous — set RUNNING then
set DONE in the same request. I kept this for now because GeminiService
is not yet implemented.

This will need to change when Gemini integration is added: the method
must set RUNNING, save to DB, return 202 immediately, then call Gemini
asynchronously (@Async). Without this split, the HTTP request blocks
for 10-30 seconds waiting for Gemini — unacceptable UX.

Cost accepted now: frontend sees RUNNING→DONE instantly during testing,
which is not realistic. Will fix in GeminiService integration step.

---

### AI Mistake & Correction: Unit Testing `PipelineService`

**Initial Mistake (by AI):**
The first version of `PipelineServiceTest.java` had several flaws:
1.  **`TooManyActualInvocations`**: The `retryStep` test incorrectly asserted that `save()` was called 2 times, when the actual flow (reset to PENDING, set to RUNNING, set to DONE) calls it 3 times.
2.  **`UnnecessaryStubbingException`**: A global `@BeforeEach` mock setup for `projectRepository.findById()` caused warnings in tests that didn't need that specific mock.
3.  **State Capture Flaw**: A subsequent fix used Mockito's `ArgumentCaptor` to verify the state changes of a `PipelineStep` object. This was a subtle but critical error. The captor only gets a *reference* to the object, so it saw the final state ("DONE") for all captured invocations, leading to assertion failures on intermediate states like "RUNNING".

**Correction:**
1.  **Corrected Invocation Count**: The test was fixed to assert `times(3)`.
2.  **Refined Mocking**: The global mock was removed. Mocks were moved into the specific tests that required them.
3.  **Correct State Verification**: The `ArgumentCaptor` was replaced with a more robust strategy. By using `thenAnswer` on the `save()` method, we captured the object's status into a `List<String>` *at the exact moment of each call*. This correctly recorded the sequence of states (`PENDING`, `RUNNING`, `DONE`) and allowed for accurate assertions.