package com.ainote.dto;

import lombok.Data;

@Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class NoteRequestDTO {
    private String title;
    private String content;
}
