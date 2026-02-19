package com.ainote.dto;

import java.util.List;

public record NoteAnalysisResult(
        String contentType,
        String primaryDomain,
        List<String> entities) {
}
