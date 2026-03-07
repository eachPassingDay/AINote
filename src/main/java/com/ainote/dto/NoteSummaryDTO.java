package com.ainote.dto;

import com.ainote.enums.NoteStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoteSummaryDTO {
    private String id;
    private String title;
    private String summary;
    private NoteStatus status;
    private LocalDateTime updatedAt;
    private NoteAnalysisResult aiMetadata;
}
