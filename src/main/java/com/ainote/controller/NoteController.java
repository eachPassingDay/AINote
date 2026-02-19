package com.ainote.controller;

import com.ainote.dto.NoteRequestDTO;
import com.ainote.dto.NoteResponseDTO;
import com.ainote.service.NoteService;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @PostMapping("/summarize")
    public NoteResponseDTO summarizeNote(@RequestBody NoteRequestDTO noteRequest) {
        return noteService.generateSummary(noteRequest);
    }

    @PostMapping("/add")
    public String addNote(@RequestBody NoteRequestDTO noteRequest) {
        noteService.ingestNote(noteRequest);
        return "Note processing started. Segments will be merged or added as new notes.";
    }

    @PostMapping("/merge")
    public String mergeNotes(@RequestParam String sourceId, @RequestParam String targetId) {
        return noteService.mergeNotes(sourceId, targetId);
    }

    @GetMapping("/search")
    public String search(@RequestParam String query, @RequestParam(defaultValue = "0.6") double threshold) {
        return noteService.semanticSearch(query, threshold);
    }
}
