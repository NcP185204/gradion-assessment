# 02 - JPA Repositories Design

## 1. User Prompt
Create Spring Data JPA Repository interfaces in package com.gradion.backend.repository:

1. UserRepository extends JpaRepository<User, Long>
    - findByEmail(String email) → Optional<User>

2. ProjectRepository extends JpaRepository<Project, Long>
    - findByUserIdOrderByCreatedAtDesc(Long userId) → List<Project>

3. PipelineStepRepository extends JpaRepository<PipelineStep, Long>
    - findByProjectIdOrderByStepNumber(Long projectId) → List<PipelineStep>
    - findByProjectIdAndStepNumber(Long projectId, int stepNumber) → Optional<PipelineStep>

4. CharacterRepository extends JpaRepository<Character, Long>
    - findByProjectIdOrderByDisplayOrder(Long projectId) → List<Character>
    - countByProjectId(Long projectId) → long

5. ChapterRepository extends JpaRepository<Chapter, Long>
    - findByProjectId(Long projectId) → List<Chapter>
    - countByProjectId(Long projectId) → long

---

## 2. Gemini Response (Summary)
Gemini Code Assist successfully generated the 5 Spring Data JPA Repository interfaces (`UserRepository`, `ProjectRepository`, `PipelineStepRepository`, `CharacterRepository`, `ChapterRepository`) with all the requested custom query methods. 

---

## 3. Developer Notes / Actions Taken
- Reviewed the generated repository interfaces. The AI correctly followed Spring Data JPA naming conventions for the custom query methods (e.g., `findByProjectIdOrderByStepNumber`).
- Added the interfaces to the `com.gradion.backend.repository` package.
- The data access layer is now fully connected and ready for the Service logic.