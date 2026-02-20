package com.ainote.service;

import com.ainote.dto.NoteRequestDTO;
import com.ainote.dto.NoteResponseDTO;

public interface NoteService {

    NoteResponseDTO generateSummary(NoteRequestDTO noteRequest);

    void ingestNote(NoteRequestDTO noteRequest);

    void processNoteAsync(String noteId, NoteRequestDTO noteRequest);

    String semanticSearch(String query, double threshold);

    String chatWithNotes(String query);

    String chatWithNotes(String query, String filterDomain, String filterType);

    java.util.Map<String, java.util.List<com.ainote.dto.TagStatDTO>> getKnowledgeTags();

    String mergeNotes(String sourceId, String targetId);

    java.util.List<com.ainote.dto.NoteHistoryDTO> getNoteHistory(String noteId);

    com.ainote.entity.Note getNoteRevision(String noteId, Number revision);

    void rollbackNote(String noteId, Number revision);
}
