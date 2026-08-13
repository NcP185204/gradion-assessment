# DECISIONS.md

Decisions only — who proposed it, who pushed back, where we landed, what it cost.

---

## Stack: Spring Boot 3 + React (Vite)

My call. The spec permits any stack; I chose Spring Boot because I have real
project experience with it (GoPlay booking platform) and it gives me JPA,
Security, and bean validation out of the box. React + Vite on the frontend
because it's what I can review and wire quickly.

Gemini (the copilot) suggested Node/Express for a "lighter" stack and pushed
back on Spring as "heavier than this scope needs." I stayed with Spring — the
"overhead" is tooling I already know, and JPA transactions are exactly what the
no-duplicate-call and resumability rules need for free. Cost: two processes to
start (scripts handle it), and a heavier JVM than a Node service. Worth it.

---

## Storage: H2 in file mode (not JSON files, not a "real" DB)

My call. The spec explicitly blesses JSON files if done safely, but safe JSON
means a per-project write lock — which is the same concurrency complexity as a
DB without the query language. H2 in `jdbc:h2:file:` mode gives me JPA,
transactions, and a conditional-UPDATE primitive (see the duplicate-call
decision below) for zero extra code.

Gemini agreed without pushback. Costs I accepted: data is one `.mv.db` file on
disk (fine at this scope), and it's not a Postgres-grade DB — if this grew
multi-user with concurrent writers on the same project, I'd revisit.

---

## Modeling progress: `status` + `startedAt`, no STUCK enum

Gemini's first draft used a single enum
`PENDING/RUNNING/DONE/FAILED/STUCK`. I pushed back: STUCK is not a real state,
it's a *derived condition* (RUNNING for longer than N minutes). Storing STUCK
means a cleanup job to un-stick rows, and a row can be both RUNNING and STUCK
which is unrepresentable in one field.

Landed on: `status` (PENDING/RUNNING/DONE/FAILED) plus a `startedAt` timestamp.
`isStepStuck()` is a computed predicate. The stuck-recovery endpoint turns a
stale RUNNING step into FAILED so the user can retry it — no DB surgery.
Cost: every stuck check is a time comparison, and the "N minutes" threshold is a
magic constant (5). Acceptable.

---

## No duplicate calls: atomic conditional UPDATE, not a read-then-throw

Gemini proposed the obvious guard: *read the step; if it's already RUNNING,
throw 409.* I pushed back — that's a TOCTOU race. Two concurrent requests
(second tab, double-click, refresh) can both read PENDING before either writes,
and both proceed to call Gemini twice.

Instead the repository exposes `claimStepForRunning(stepId, now)`, one `UPDATE`
that flips `PENDING|FAILED → RUNNING` **only if** it's still in that state, and
returns the row count. Exactly one concurrent request gets 1 row; the loser
gets 0 and surfaces a 409 with no Gemini call. The DB is the arbiter, not the
app.

Cost: a native-ish `@Modifying` JPQL query to maintain, and one extra round-trip
per run. This is the single most important correctness decision in the project —
it's what makes "no duplicate calls" true across processes and server restarts,
not just within one browser tab.

### AI override — the pipeline was fully synchronous

The first `PipelineService` Gemini wrote did *set RUNNING → call Gemini → set
DONE* all inside the HTTP request. That blocks for 10–30s+ (longer for images)
and makes a mock of the "specific in-progress state" requirement. I overrode it:
the request now (1) validates ordering, (2) claims atomically, (3) kicks off
`StepRunner` on a worker thread (`@Async`), and (4) returns RUNNING immediately.
The frontend polls. Cost: added `AsyncConfig`, `StepRunner`, and a polling hook
— real complexity, but the only way to show *which step is running* while a real
image model is working.

---

## Resumability: persist Gemini interaction IDs, never re-send the book

Gemini's early drafts re-sent the whole book text on every step. That burns
tokens and is exactly what the notebook avoids. I overrode it to mirror the
notebook: upload the book to the File API **once**, open a conversation
(`book_interaction`), then chain every later step through
`previous_interaction_id`.

The interaction IDs are stored on the `Project` row
(`bookInteractionId`, `styleInteractionId`, `charactersInteractionId`,
`charactersImageInteractionId`, `chaptersInteractionId`). Because they're
durable, a server restart mid-pipeline resumes from where the conversation
actually was — no re-upload, no lost context. It also means the 2-character /
1-chapter caps are the only thing bounding cost per step.

Cost: five extra columns and the discipline of threading the right "previous"
ID into each call. This is what makes "resumable" real, not cosmetic.

---

## Small AI corrections I made (the "you overrode the AI" list)

These aren't architectural, but each was AI output that was wrong or unsafe and
I caught it by running the thing:

1. **JWT library version drift.** Gemini produced `Jwts.parserBuilder()` /
   `parseClaimsJws()` — the deprecated 0.11.x API — while the POM pins
   `jjwt-0.12.5`. I rewrote `JwtUtil` to `Jwts.parser().verifyWith(...)` /
   `parseSignedClaims()`. Build would have failed silently until runtime.
2. **Principal type mismatch.** Gemini's `JwtAuthFilter` put Spring's
   `UserDetails` into the security context, but controllers read
   `@AuthenticationPrincipal User` (the JPA entity). That's a guaranteed
   `ClassCastException` on the first authenticated request. I changed the filter
   to resolve the real `User` entity — a bug that only shows up under load, not
   in the happy-path mock.
3. **JSON bodies on a multipart endpoint.** Gemini declared one
   `createProject` method with `@RequestPart` *and* `APPLICATION_JSON`. Pasted
   text (a JSON body) would never bind. I split it into two handlers: one
   `@RequestBody` for paste-text, one `@RequestPart` multipart for `.txt` upload.
4. **Boot fails without a key.** Gemini wired `${GEMINI_API_KEY}` with no
   default, so the Spring context (and every test) wouldn't start without an
   env var. I added a default so the app boots keyless and only fails — with a
   clear error — when a step actually calls Gemini.
5. **Lombok vs Java 24.** The repo's default JDK is 24, and Lombok 1.18.30
   (from Boot 3.2.5) silently drops annotation processing on 24, yielding
   "cannot find symbol: builder()/getId()". I pinned `lombok.version=1.18.42`.

---

## Model choice: current IDs, env-overridable

The notebook selects `gemini-3.6-flash` (text) and `gemini-2.5-flash-image`
(Nano Banana family, free tier). I kept those exact IDs, read from
`GEMINI_TEXT_MODEL` / `GEMINI_IMAGE_MODEL` so they're pinned but patchable.
Recorded here because the spec asks for the IDs and for `DECISIONS.md` to note
them.

---

## If I had one more day, what would I build next?

An **attempt/retry history per step.** Right now a retry overwrites the previous
failure's `resultJson`, so if a step fails, succeeds, then fails again, you can
only see the latest error. Cheap to store (append a row per attempt with the
error + timestamp), it directly supports the "failures are retryable" story, and
it's the kind of thing a user actually needs to trust a multi-minute pipeline —
more valuable than any of the polish items or the bonus Veo/Lyria sections.