package com.ainote.controller;

import com.ainote.common.BusinessException;
import com.ainote.common.ErrorCodeEnum;
import com.ainote.common.PageData;
import com.ainote.dto.ChatRequestDTO;
import com.ainote.dto.ChatResponseDTO;
import com.ainote.dto.DrilledPropositionDTO;
import com.ainote.dto.GraphDataDTO;
import com.ainote.dto.NoteAnalysisResult;
import com.ainote.dto.NoteHistoryDTO;
import com.ainote.dto.NoteRequestDTO;
import com.ainote.dto.NoteResponseDTO;
import com.ainote.dto.NoteSummaryDTO;
import com.ainote.dto.PolishRequestDTO;
import com.ainote.dto.SearchResultDTO;
import com.ainote.dto.TagStatDTO;
import com.ainote.entity.Note;
import com.ainote.service.DocumentExtractionService;
import com.ainote.service.NoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;
    private final DocumentExtractionService documentExtractionService;

    @PostMapping("/upload-doc")
    public Map<String, Object> uploadDoc(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "File is empty");
        }

        try {
            // 1. 通过 Apache Tika 从文档（PDF/Docx 等）中提取原始文本
            String extractedMarkdown = documentExtractionService.extractToMarkdown(file.getInputStream());

            // 2. 构建笔记请求
            NoteRequestDTO request = new NoteRequestDTO();
            // 使用用户提供的标题，若为空则回退到原始文件名
            request.setTitle(title != null && !title.isBlank() ? title : file.getOriginalFilename());
            request.setContent(extractedMarkdown);

            // 3. 交给现有的 AI 摄入管线处理（ingestNote 会触发异步事件）
            String noteId = noteService.ingestNote(request);

            return Map.of("noteId", noteId, "message",
                    "Document successfully extracted and queued for AI analysis.");
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException(ErrorCodeEnum.FILE_UPLOAD_ERROR,
                    "Failed to process document: " + e.getMessage());
        }
    }

    @PostMapping("/summarize")
    public NoteResponseDTO summarizeNote(@RequestBody NoteRequestDTO noteRequest) {
        return noteService.generateSummary(noteRequest);
    }

    @PostMapping("/add")
    public Map<String, String> addNote(@RequestBody NoteRequestDTO noteRequest) {
        log.info("Received request to ingest new note. Title: {}", noteRequest.getTitle());
        String noteId = noteService.ingestNote(noteRequest);
        log.info("Note ingestion queued successfully. NoteId: {}", noteId);
        return Map.of(
                "id", noteId,
                "message", "Note processing started. Segments will be merged or added as new notes.");
    }

    @PostMapping("/merge")
    public Map<String, Object> mergeNotes(@RequestParam String sourceId, @RequestParam String targetId) {
        String result = noteService.mergeNotes(sourceId, targetId);
        if ("Note not found.".equals(result) || "One or both notes are deleted.".equals(result)) {
            throw new BusinessException(ErrorCodeEnum.NOTE_NOT_FOUND, result);
        }
        return Map.of("message", result);
    }

    @GetMapping("/search")
    public List<SearchResultDTO> search(@RequestParam String query,
            @RequestParam(defaultValue = "0.6") double threshold) {
        log.debug("Received semantic search request. Query: [{}], Threshold: {}", query, threshold);
        List<SearchResultDTO> results = noteService.semanticSearch(query, threshold);
        log.debug("Semantic search returning {} results.", results.size());
        return results;
    }

    @GetMapping("/{id}")
    public Note getNote(@PathVariable String id) {
        return noteService.getNote(id);
    }

    @GetMapping("/{id}/similar")
    public List<SearchResultDTO> getSimilarNotes(@PathVariable String id) {
        return noteService.getSimilarNotes(id);
    }

    @GetMapping(value = "/{id}/stream-status", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNoteStatus(@PathVariable String id) {
        return noteService.subscribeToStatus(id);
    }

    @GetMapping("/{id}/status")
    public Map<String, String> getNoteStatus(@PathVariable String id) {
        Note note = noteService.getNote(id);
        return Map.of(
                "status", note.getStatus() != null ? note.getStatus().name() : "UNKNOWN",
                "message", note.getStatusMessage() != null ? note.getStatusMessage() : "");
    }

    @PutMapping("/{id}")
    public Map<String, String> updateNote(@PathVariable String id, @RequestBody NoteRequestDTO noteRequest) {
        noteService.updateNote(id, noteRequest);
        return Map.of("success", "true", "message", "Note updated.");
    }

    @PostMapping("/{id}/analyze")
    public Map<String, String> analyzeNote(@PathVariable String id) {
        noteService.analyzeNote(id);
        return Map.of("success", "true", "message", "Note background processing started for AI analysis.");
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deleteNote(@PathVariable String id) {
        noteService.deleteNote(id);
        return Map.of("message", "Note deleted successfully.");
    }

    @PostMapping("/upload")
    public Map<String, Object> uploadImage(
            @RequestParam("file") MultipartFile file) {
        return noteService.uploadImage(file);
    }

    @GetMapping(value = "/export", produces = "application/zip")
    public ResponseEntity<StreamingResponseBody> exportToZip() {
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"ainote_backup.zip\"")
                .body(noteService.exportToZip());
    }

    @GetMapping("/entity/{entityName}/wiki")
    public Map<String, String> getEntityWiki(
            @PathVariable String entityName,
            @RequestParam(defaultValue = "false") boolean force) {
        String wikiContent = noteService.generateEntityWiki(entityName, force);
        return Map.of("wiki", wikiContent);
    }

    @PatchMapping("/{id}/metadata")
    public Map<String, Object> updateNoteMetadata(@PathVariable String id,
            @RequestBody NoteAnalysisResult metadataRequest) {
        noteService.updateNoteMetadata(id, metadataRequest);
        return Map.of("message", "Note metadata updated successfully.");
    }

    @GetMapping("/graph")
    public GraphDataDTO getKnowledgeGraph(@RequestParam(required = false) String domain) {
        return noteService.getKnowledgeGraph(domain);
    }

    @PostMapping(value = "/ai/polish/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamPolishText(@RequestBody PolishRequestDTO request) {
        log.info("Received Inline AI Polish stream request for instruction: [{}]", request.getInstruction());
        return noteService.streamPolishText(request);
    }

    @GetMapping("/graph/init")
    public GraphDataDTO getInitialGraph() {
        return noteService.getInitialGraph();
    }

    @GetMapping("/graph/expand")
    public GraphDataDTO expandGraphNode(
            @RequestParam String nodeId,
            @RequestParam String nodeType) {
        return noteService.expandGraphNode(nodeId, nodeType);
    }

    @GetMapping("/{id}/history")
    public List<NoteHistoryDTO> getNoteHistory(@PathVariable String id) {
        return noteService.getNoteHistory(id);
    }

    @GetMapping("/{id}/history/{rev}")
    public Note getNoteRevision(@PathVariable String id, @PathVariable Long rev) {
        return noteService.getNoteRevision(id, rev);
    }

    @PostMapping("/{id}/rollback/{rev}")
    public Map<String, Object> rollbackNote(@PathVariable String id, @PathVariable Long rev) {
        try {
            noteService.rollbackNote(id, rev);
            return Map.of("message", "Rolled back note " + id + " to revision " + rev);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, e.getMessage());
        }
    }

    @PostMapping("/chat")
    public ChatResponseDTO chat(@RequestBody ChatRequestDTO request) {
        log.info("Received Chat Request. Query: [{}], SessionId: [{}]", request.getQuery(), request.getSessionId());
        return noteService.chatWithNotes(request.getQuery(), request.getFilterDomain(),
                request.getFilterType(), request.getSessionId());
    }

    @GetMapping("/tags")
    public Map<String, List<TagStatDTO>> getKnowledgeTags() {
        return noteService.getKnowledgeTags();
    }

    @GetMapping("/tags/suggest")
    public Map<String, List<String>> suggestTags(@RequestParam String keyword) {
        return noteService.suggestTags(keyword);
    }

    @GetMapping
    public PageData<NoteSummaryDTO> getNotes(
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return noteService.getNotes(domain, type, PageRequest.of(page, size));
    }

    @GetMapping("/trash")
    public PageData<NoteSummaryDTO> getTrashNotes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return noteService.getTrashNotes(PageRequest.of(page, size));
    }

    @PostMapping("/{id}/restore")
    public Map<String, String> restoreNote(@PathVariable String id) {
        noteService.restoreNote(id);
        return Map.of("message", "Note restored from Trash and background processing started.");
    }

    @GetMapping("/drill-down")
    public List<DrilledPropositionDTO> drillDownConcept(@RequestParam String concept) {
        log.info("Received request to drill down concept: {}", concept);
        return noteService.drillDownConcept(concept);
    }
}
