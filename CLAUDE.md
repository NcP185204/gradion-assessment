# CLAUDE.md — Project context for AI assistant

## Project
Gradion take-home assessment — Book illustration web app using Gemini API.

## Stack
- Backend: Spring Boot 3, Java 17, H2 database, Maven
- Frontend: React 18, Vite, Axios
- API: Gemini REST API (text + image generation)

## Key rules (from spec)
- Max 2 characters, max 1 chapter — enforce SERVER-SIDE
- Never auto-retry Gemini calls — retries are user-triggered only
- Book text sent to Gemini ONCE via File API, reused across all steps
- Step N cannot run if step N-1 is not DONE
- If a step is RUNNING and server restarts → must be recoverable (timeout to FAILED)

## Pipeline steps
1. Style — text generation, optional user input
2. Characters — structured JSON, max 2 adults
3. Portraits — image generation per character
4. Chapters — structured JSON, max 1
5. Illustrations — image generation per chapter

## Pipeline step states
- status: PENDING | RUNNING | DONE | FAILED
- Each step has: stepNumber, status, resultJson, startedAt, completedAt

## File structure
- Backend: com.gradion.controller / service / repository / model / dto / config
- Frontend: src/pages / components / api / hooks

## What to focus on
- Duplicate call prevention: if step status=RUNNING, return 409, do NOT call Gemini again
- Stuck recovery: if startedAt > 5 minutes ago and status=RUNNING → allow reset to FAIL