package com.ainote.service;

import com.ainote.dto.NoteRequestDTO;
import com.ainote.dto.NoteResponseDTO;

public interface NoteService {

    NoteResponseDTO generateSummary(NoteRequestDTO noteRequest);

    void ingestNote(NoteRequestDTO noteRequest);

    void processNoteAsync(String noteId, NoteRequestDTO noteRequest);

    String semanticSearch(String query, double threshold);

    String mergeNotes(String sourceId, String targetId);
}
