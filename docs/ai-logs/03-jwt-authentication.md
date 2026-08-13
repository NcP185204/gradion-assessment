# 03 - JWT Authentication Setup

## 1. User Prompt
Create JWT-based authentication for Spring Boot 3 (no password, email+name only).

Package: com.gradion.backend

1. JwtUtil.java (in config/) — generate and validate JWT tokens
    - generateToken(User user) → String
    - extractEmail(String token) → String
    - isTokenValid(String token) → boolean
    - Use io.jsonwebtoken (jjwt) library
    - Secret from @Value("${app.jwt.secret}")
    - Expiration from @Value("${app.jwt.expiration}")

2. JwtAuthFilter.java (in config/) — OncePerRequestFilter
    - Extract Bearer token from Authorization header
    - Validate and set SecurityContextHolder

3. SecurityConfig.java (in config/)
    - Permit: POST /api/auth/login and /h2-console/**
    - Authenticate: all other /api/** endpoints
    - Add JwtAuthFilter before UsernamePasswordAuthenticationFilter
    - Disable CSRF, enable CORS
    - For H2 Console to work, add: http.headers().frameOptions().disable()

4. AuthController.java (in controller/)
   POST /api/auth/login
   Request: { "name": "...", "email": "..." }
   Logic: find user by email → if exists load, else create new → return JWT + user info
   Response: { "token": "...", "user": { "id", "name", "email" } }

5. AuthService.java (in service/)
    - loginOrRegister(String name, String email) → LoginResponse

Make sure pom.xml includes jjwt dependency (io.jsonwebtoken, version 0.12.x).

---

## 2. Gemini Response (Summary)
Gemini Code Assist generated the JWT utility classes (`JwtUtil`, `JwtAuthFilter`), the authentication service (`AuthService`), and the controller (`AuthController`). It also successfully generated the `SecurityConfig` to integrate the JWT filter while properly keeping the `/h2-console/**` path accessible. 

---

## 3. Developer Notes / Actions Taken
- Added the `jjwt` dependencies to `pom.xml` and reloaded Maven.
- Verified that `SecurityConfig` allows access to H2 Console and properly authenticates all other `/api/**` endpoints.
- Tested the login flow; the system correctly creates a new user or returns an existing one without requiring a password, completely matching the assessment requirements.