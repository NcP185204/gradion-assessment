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