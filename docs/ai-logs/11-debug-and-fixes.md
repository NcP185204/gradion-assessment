# 11 - Debug & Fixes (end-to-end run)

## 1. User Prompt
Run the full stack and debug the real Gemini pipeline end to end. Fix whatever
breaks.

---

## 2. Issues Found & Fixed
- **403 "unregistered callers"** — the Gemini calls went out without an API key.
  Root cause: `Dotenv.configure().load()` reads `.env` from the process working
  directory, and IntelliJ ran with cwd = repo root while `.env` lives in
  `backend/`. Fixed by making `loadDotenv()` search both `./.env` and
  `backend/.env`.
- **"Illegal base64 character: '-'"** — with the key unloaded, `JWT_SECRET` fell
  back to a non-base64 default, so `Decoders.BASE64.decode()` threw on login.
  Same fix as above.
- **Spinner "Running Style" spun forever** — polling only ran once on mount and
  never restarted after a run. Fixed the hook so `refresh()` re-arms the loop
  when a step is RUNNING.
- **"Reset stuck step" button never appeared** — it sat in an unreachable branch
  (`!runningStep` but gated on `RUNNING`). Moved it out and gated it on RUNNING
  for more than 5 minutes.
- **429 on the text model** — `gemini-3.6-flash` hit the free-tier limit and
  `gemini-3.7-flash` rejected an API key on the Interactions endpoint. Switched
  `GEMINI_TEXT_MODEL` to `gemini-3.5-flash`.

---

## 3. Developer Notes / Actions Taken
- I tested the Gemini endpoints directly with curl to prove the key was valid,
  which isolated the problem to the key not being loaded rather than the client.
- I used `jcmd VM.system_properties` to confirm the running JVM had the right
  `user.dir` and env values before and after the fix.
- I deleted the H2 database and re-ran the pipeline from scratch a few times to
  confirm the resume, retry, and stuck-recovery paths all behaved correctly.

