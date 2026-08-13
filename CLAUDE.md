# CLAUDE.md — Project context for AI assistant

## Project
Gradion take-home assessment — Book illustration web app using the Gemini API.

## Stack
- Backend: Spring Boot 3.2, Java 21, H2 (file mode `jdbc:h2:file:./data/gradiondb`), Maven
- Frontend: React 19, Vite, React Router, Axios
- Tests: JUnit 5 + Mockito (backend), Vitest + Testing Library (frontend)
- API: Gemini REST (no SDK) — File API + Interactions API + Nano Banana images

## Hard rules (from spec)
- Max 2 characters, max 1 chapter — enforced SERVER-SIDE in `StepRunner` (constants `MAX_CHARACTERS`/`MAX_CHAPTERS`)
- Never auto-retry Gemini — retries are user-triggered only (`retryStep`)
- Book text sent to Gemini ONCE via File API, then chained via `previous_interaction_id` (IDs persisted on `Project`)
- Step N cannot run if step N-1 is not DONE
- RUNNING step stranded >5 min → `reset-stuck` → FAILED → retry

## Pipeline (5 steps)
1. Style — text; optional user style; chained to `bookInteraction`
2. Characters — structured JSON (adults only, max 2); chained to `styleInteraction`
3. Portraits — image model, one per character, chained; save to `UPLOAD_DIR`
4. Chapters — structured JSON (max 1); chained to `charactersInteraction`
5. Illustrations — image model; chained to last portrait interaction (consistency)

## Duplicate-call guard (important)
- `PipelineService.runStep` uses `PipelineStepRepository.claimStepForRunning` — one atomic conditional UPDATE (PENDING|FAILED → RUNNING) returning rowcount. 0 → 409, 1 → wins.
- Execution is ASYNC: `StepRunner.runStepAsync` runs on `stepExecutor`; HTTP returns RUNNING immediately; frontend polls.

## Key files
- Backend: `service/PipelineService.java` (orchestration), `service/StepRunner.java` (Gemini execution), `service/gemini/GeminiClient.java` (REST)
- Frontend: `pages/ProjectDetailPage.jsx` + `hooks/useProjectPolling.js` (poll while RUNNING)
- `api/client.js` attaches JWT; `context/AuthContext.jsx`

## Build / env gotchas
- **JAVA_HOME**: Lombok 1.18.42 is pinned (Java 24-safe), but a Java 21 runtime is preferred. `start.sh`/`test.sh` auto-detect `ms-21.0.10`.
- Gemini key is optional at boot (defaults to empty) — steps fail only when actually calling Gemini.
- `backend/.env` (dotenv) holds GEMINI_API_KEY etc. Never commit it.