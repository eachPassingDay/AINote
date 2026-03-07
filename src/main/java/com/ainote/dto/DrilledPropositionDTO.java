package com.ainote.dto;

import com.ainote.enums.ChunkType;

public record DrilledPropositionDTO(
        String noteId,
        String noteTitle,
        String chunkId,
        ChunkType chunkType,
        String conceptOrLanguage,
        String proposition,
        String originalContent,
        double relevanceScore) {
}
