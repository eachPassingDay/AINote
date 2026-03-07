package com.ainote.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDTO {
    private String id;
    private String title;
    private String highlightContext;
    private Double similarityScore;
}
