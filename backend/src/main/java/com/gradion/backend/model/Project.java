package com.gradion.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(nullable = false)
    private String bookText;

    private String geminiFileUri;

    /**
     * Durable Gemini conversation state. The notebook chains every step through
     * {@code previous_interaction_id}; persisting these ids is what makes the
     * pipeline resumable across server restarts without re-sending the book.
     */
    private String bookInteractionId;

    private String styleInteractionId;

    private String charactersInteractionId;

    private String charactersImageInteractionId;

    private String chaptersInteractionId;

    @Lob
    private String style;

    @Column(nullable = false)
    @Builder.Default
    private String overallStatus = "CREATED";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
