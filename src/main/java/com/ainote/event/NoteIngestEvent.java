package com.ainote.event;

import com.ainote.dto.NoteRequestDTO;
import org.springframework.context.ApplicationEvent;

public class NoteIngestEvent extends ApplicationEvent {
    private final String noteId;
    private final NoteRequestDTO noteRequest;

    public NoteIngestEvent(Object source, String noteId, NoteRequestDTO noteRequest) {
        super(source);
        this.noteId = noteId;
        this.noteRequest = noteRequest;
    }

    public String getNoteId() {
        return noteId;
    }

    public NoteRequestDTO getNoteRequest() {
        return noteRequest;
    }
}
