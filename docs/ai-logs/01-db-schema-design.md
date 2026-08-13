# 01 - Database Schema Design

## 1. User Prompt
I'm building a book illustration web app using Spring Boot 3, Java 21, H2 database, JPA, Lombok.

Create the following JPA Entity classes in package com.gradion.backend.model:

1. User.java
    - id (Long, PK, auto)
    - name (String, not null)
    - email (String, not null, unique)
    - createdAt (LocalDateTime, not null)

2. Project.java
    - id (Long, PK, auto)
    - user (ManyToOne → User, not null)
    - title (String, not null)
    - bookText (Text, not null)
    - geminiFileUri (String, nullable) — URI after uploading to Gemini File API
    - style (Text, nullable) — result of pipeline step 1
    - overallStatus (String, not null, default "CREATED")
    - createdAt, updatedAt (LocalDateTime)

3. PipelineStep.java
    - id (Long, PK, auto)
    - project (ManyToOne → Project, not null)
    - stepNumber (int, not null) — 1 to 5
    - status (String, not null, default "PENDING") — PENDING/RUNNING/DONE/FAILED
    - resultJson (Text, nullable)
    - startedAt (LocalDateTime, nullable)
    - completedAt (LocalDateTime, nullable)

4. Character.java (table name: characters)
    - id, project (ManyToOne), name, imagePrompt (Text), portraitPath (nullable), displayOrder (int)

5. Chapter.java (table name: chapters)
    - id, project (ManyToOne), name, imagePrompt (Text), illustrationPath (nullable)

Use @Entity, @Table, @Id, @GeneratedValue, @Column, @ManyToOne, @JoinColumn, @CreationTimestamp, @UpdateTimestamp from Hibernate. Use Lombok @Data @NoArgsConstructor @AllArgsConstructor @Builder.

---

## 2. Gemini Response (Summary)
Gemini Code Assist successfully generated the source code for the 5 Entity classes (`User`, `Project`, `PipelineStep`, `Character`, `Chapter`).

---

## 3. Developer Notes / Actions Taken
- Reviewed the source code. The AI correctly applied Lombok and Spring Data JPA annotations.