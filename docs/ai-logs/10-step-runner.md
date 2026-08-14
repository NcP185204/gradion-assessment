# 10 - StepRunner: Async Pipeline Execution

## 1. User Prompt
Implement StepRunner.java to execute each pipeline step against the real Gemini
API on a worker thread (@Async).

Requirements:
- Style, Characters (max 2, adults only), Portraits, Chapters (max 1), Illustrations.
- Enforce the 2-character / 1-chapter caps SERVER-SIDE.
- Never auto-retry — any failure marks the step FAILED.
- Persist interaction IDs on the Project so a restart resumes the conversation.
- Update progressJson as each portrait/illustration lands.

---

## 2. Gemini Response (Summary)
- `runStepAsync` decorated with `@Async("stepExecutor")`, wrapping
  `executeWithErrorHandling` in a try/catch that marks the step FAILED on error.
- Per-step methods (`runStyleStep`, `runCharactersStep`, `runPortraitsStep`,
  `runChaptersStep`, `runIllustrationsStep`) that chain the conversation through
  the project's stored interaction IDs.
- `MAX_CHARACTERS = 2` and `MAX_CHAPTERS = 1` constants enforced in the runner.
- A `SYSTEM_INSTRUCTIONS` block (no text on the image, family-friendly, no
  borders) copied from the notebook.
- Progress tracking via `progressJson` as each portrait/illustration is saved.
- Helpers to save characters/chapters/illustrations and to mark a step DONE or
  FAILED.

---

## 3. Developer Notes / Actions Taken
- I verified the caps are enforced server-side, not just in the UI, as the spec
  requires.
- The dispatch of `runStepAsync` originally happened inside the same transaction
  that claims the step. The worker reads the step from the DB, so it could see
  PENDING and skip the work. I moved the dispatch to run after the transaction
  commits (see `DECISIONS.md`), so the RUNNING state is durable first.
- I ran the pipeline end to end against the real API to confirm portraits and
  illustrations save through `ImageStorageService` and land one by one in the UI.

