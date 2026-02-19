package com.ainote.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "notes")
@Data
public class Note {

    @Id
    @Column(name = "id")
    private String id;

    @Column(columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private com.ainote.enums.NoteStatus status;

    @Version
    private long version;

    @Column(name = "deleted")
    private boolean deleted = false;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "ai_metadata", columnDefinition = "jsonb")
    private com.ainote.dto.NoteAnalysisResult aiMetadata;
}
