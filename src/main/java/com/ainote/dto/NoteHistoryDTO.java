package com.ainote.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteHistoryDTO {
    private Number revisionId;
    private Date revisionDate;
    private String revisionType; // ADD, MOD, DEL
    private String title;
    private String summary;
    private String status;
}
