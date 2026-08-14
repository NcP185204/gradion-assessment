# 08 - React Frontend: Auth, Project List, Pipeline Detail & Polling

## 1. User Prompt
Build the React frontend for the book illustration app using React 19 + Vite,
React Router, and Axios.

Screens required:
- Login (name + email, no password)
- Project list (title, created date, status pill, 5-step progress indicator, empty state)
- New project (title + paste text OR .txt upload)
- Project detail (stepper, style, character cards, chapter cards, action button, sign out)

Behavior:
- Attach the JWT to every request; on 401 clear the session and redirect to login.
- Poll the project detail while any step is RUNNING, so portraits land one by one.

---

## 2. Gemini Response (Summary)
- **Axios client** (`api/client.js`) with a base URL of `/api` (proxied to the
  backend on :8080 by Vite). A request interceptor adds the `Authorization:
  Bearer` header from localStorage, and a response interceptor clears the
  session and redirects to `/login` on any 401.
- **Auth context** (`context/AuthContext.jsx`) holding the current user and
  exposing `login` / `signOut`, with session saved to localStorage.
- **Pages**: `LoginPage` (name + email with basic validation), `ProjectListPage`
  (list with status pill and progress indicator, empty state, sign out),
  `NewProjectPage` (paste text or `.txt` file upload), and `ProjectDetailPage`
  (stepper, style text, character and chapter cards, the current-step action
  button).
- **Components**: `Stepper`, `CharacterCard`, `ChapterCard`, `ErrorBanner`.
- **Polling hook** (`useProjectPolling`) that fetches the project on mount and
  keeps polling while a step is RUNNING.

---

## 3. Developer Notes / Actions Taken
- I reviewed each page against `app-demo.html` to make sure every screen and
  state the demo shows (empty, loading, error, in-progress) is covered.
- I found and fixed two real bugs during the end-to-end run:
  - **Polling never restarted.** The hook only polled once on mount. After a
    user kicked off a step, the UI showed "Running Style" forever even though
    the backend had already marked the step DONE. I added a `pollToken` state
    that `refresh()` bumps whenever a step is RUNNING, so the polling loop
    re-arms.
  - **The "Reset stuck step" button never rendered.** It sat inside the
    `!runningStep` branch under a `status === 'RUNNING'` check, which could
    never be true. I moved it out and gated it on the step being RUNNING for
    more than 5 minutes (matching the backend's reset-stuck threshold).
- I deliberately did not port the demo's fake timings or its localStorage-only
  store; real Gemini calls take 10–30s+, and the duplicate-call guard lives on
  the backend, not in one browser tab.

