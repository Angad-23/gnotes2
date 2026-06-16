package com.tutornotes.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "session_notes")
@Data
@NoArgsConstructor
public class SessionNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "note_type", nullable = false)
    private NoteType noteType = NoteType.STUDENT;

    @Column(name = "student_names", nullable = false, length = 300)
    private String studentNames;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "standard_id")
    private Standard standard;

    // Denormalized for easy display without join
    @Column(length = 10)
    private String state;

    @Column(length = 20)
    private String grade;

    @Column(length = 100)
    private String topic;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String engagement;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String skills;

    @Column(length = 500)
    private String activities;

    @Column(name = "generated_note", columnDefinition = "TEXT")
    private String generatedNote;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum NoteType {
        STUDENT, GROUP
    }
}
