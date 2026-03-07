package com.ainote.listener;

import com.ainote.event.NoteIngestEvent;
import com.ainote.service.NoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoteIngestListener {

    private final NoteService noteService;

    @Async
    @EventListener
    public void onNoteIngest(NoteIngestEvent event) {
        log.info("Processing note async: {}", event.getNoteId());
        noteService.processNoteAsync(event.getNoteId(), event.getNoteRequest());
    }
}
