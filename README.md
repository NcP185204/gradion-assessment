# Gradion Assessment — Book Illustrator

A web app that turns a book's text into character portraits and a chapter
illustration using the Gemini API. Five user-driven steps: **Style →
Characters → Portraits → Chapters → Illustrations**, each resumable and
failure-retryable.

## Stack

- **Backend:** Spring Boot 3 (Java 21), H2 (file mode), Spring Security + JWT
- **Frontend:** React 19 + Vite, React Router, Axios
- **Gemini:** plain REST — File API (book upload, once) + Interactions API
  (conversation chaining) + Nano Banana image generation. No Google SDK.

## Prerequisites

- Java 21 (the build is pinned to 21; see note below)
- Node.js 20+
- A Gemini API key

> Java: the project targets Java 21. If your default `java -version` is newer
> (e.g. 24), Lombok 1.18.42 is pinned so it still compiles — but `start.sh` and
> `test.sh` auto-detect a Java 21 runtime if present and prefer it.

## Environment

Copy `.env.example` to `.env` (a `.env` next to the backend reads via
dotenv) and fill in:

```
GEMINI_API_KEY=...            # required for real Gemini calls
GEMINI_TEXT_MODEL=...         # optional, default gemini-3.6-flash
GEMINI_IMAGE_MODEL=...        # optional, default gemini-2.5-flash-image
JWT_SECRET=...                # optional (dev fallback provided)
UPLOAD_DIR=./uploads          # optional
```

## Run

One command starts the whole stack:

```bash
./start.sh
```

- Backend → http://localhost:8080
- Frontend → http://localhost:3000

## Test

One command runs both suites:

```bash
./test.sh
```

- Backend: `./mvnw test` (pipeline ordering, retry, stuck recovery, duplicate-call guard)
- Frontend: `npm test` (Vitest + Testing Library — component loading/error/empty states)

## Architecture (short)

```
frontend (React)                 backend (Spring Boot)
─────────────────               ─────────────────────────
LoginPage ─────────┐             AuthController
ProjectListPage    │             ProjectController (list/detail/create)
NewProjectPage     ├─ Axios ──▶  StepController (run/retry/reset-stuck)
ProjectDetailPage  │  /api       FileController (serve local images)
  └ polls while RUNNING          PipelineService  ──▶ StepRunner (@Async) ──▶ GeminiClient (REST)
```

- **Storage** — H2 in `jdbc:h2:file:./data/gradiondb`. Users, projects, 5
  pipeline steps, characters, chapters. Images + book text live on the local
  filesystem (`UPLOAD_DIR`), served through `/api/files/{projectId}/...`.
- **Pipeline** — `PipelineService.runStep` validates ordering, then performs an
  atomic conditional UPDATE (`claimStepForRunning`) that flips
  `PENDING|FAILED → RUNNING`. Exactly one concurrent request wins; the loser
  gets 409. `StepRunner` executes the real Gemini call on a worker thread and
  updates the step to `DONE`/`FAILED`. The frontend polls while any step is
  RUNNING.
- **Resumable** — the Gemini conversation is chained via `previous_interaction_id`
  and those IDs are persisted on the `Project`, so a restart resumes the real
  conversation instead of re-sending the book.
- **Caps** — 2 characters / 1 chapter enforced server-side in `StepRunner`.

See `DECISIONS.md` for the reasoning and trade-offs, and `TESTING.md` for the
test strategy and a real run report.