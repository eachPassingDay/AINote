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

    @GetMapping("/{id}/history")
    public java.util.List<com.ainote.dto.NoteHistoryDTO> getNoteHistory(@PathVariable String id) {
        return noteService.getNoteHistory(id);
    }

    @GetMapping("/{id}/history/{rev}")
    public com.ainote.entity.Note getNoteRevision(@PathVariable String id, @PathVariable Long rev) {
        return noteService.getNoteRevision(id, rev);
    }

    @PostMapping("/{id}/rollback/{rev}")
    public String rollbackNote(@PathVariable String id, @PathVariable Long rev) {
        noteService.rollbackNote(id, rev);
        return "Rolled back note " + id + " to revision " + rev;
    }

    @PostMapping("/chat")
    public String chat(@RequestBody com.ainote.dto.ChatRequestDTO request) {
        return noteService.chatWithNotes(request.getQuery(), request.getFilterDomain(), request.getFilterType());
    }

    @GetMapping("/tags")
    public java.util.Map<String, java.util.List<com.ainote.dto.TagStatDTO>> getKnowledgeTags() {
        return noteService.getKnowledgeTags();
    }
}
