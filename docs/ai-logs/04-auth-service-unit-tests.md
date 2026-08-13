# 04 - Unit Tests for AuthService

## 1. User Prompt
Write JUnit 5 unit tests for AuthService.java in src/test/java/com/gradion/backend/service/AuthServiceTest.java

Test cases needed:
1. loginOrRegister with NEW email → creates new user, returns token
2. loginOrRegister with EXISTING email → loads existing user, returns token
3. loginOrRegister with null email → throws IllegalArgumentException
4. Returned token is not null and not empty

Use @ExtendWith(MockitoExtension.class)
Mock: UserRepository, JwtUtil
Do NOT use Spring context (@SpringBootTest) — pure unit test only.

---

## 2. Gemini Response (Summary)
Generated pure unit tests using Mockito (`@ExtendWith(MockitoExtension.class)`) for `AuthService`, mocking `UserRepository` and `JwtUtil` without loading the Spring context, covering new user creation, existing user login, null email validation, and token assertions.

---

## 3. Developer Notes / Actions Taken
- Added input validation (`IllegalArgumentException`) for null/empty emails inside `AuthService.java` to satisfy the edge-case test requirement.
- Successfully ran and passed all 4 JUnit 5 test cases with 100% coverage on `AuthService`.