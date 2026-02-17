package com.ainote.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Note {
    private String id;
    private String title;
    private String content;
    private String summary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
