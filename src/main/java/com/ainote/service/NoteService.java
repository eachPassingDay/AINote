package com.ainote.service;

import com.ainote.common.PageData;
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
import com.ainote.enums.NoteStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

public interface NoteService {

        NoteResponseDTO generateSummary(NoteRequestDTO noteRequest);

        String ingestNote(NoteRequestDTO noteRequest);

        void processNoteAsync(String noteId, NoteRequestDTO noteRequest);

        void updateNoteProgress(String noteId, NoteStatus status, String message);

        SseEmitter subscribeToStatus(String noteId);

        List<SearchResultDTO> semanticSearch(String query, double threshold);

        String chatWithNotes(String query);

        ChatResponseDTO chatWithNotes(String query, String filterDomain, String filterType,
                        String sessionId);

        String generateEntityWiki(String entityName, boolean force);

        Map<String, List<TagStatDTO>> getKnowledgeTags();

        PageData<NoteSummaryDTO> getNotes(String domain, String type,
                        Pageable pageable);

        Note getNote(String id);

        void updateNote(String id, NoteRequestDTO noteRequest);

        void analyzeNote(String id);

        void updateNoteMetadata(String id, NoteAnalysisResult metadataRequest);

        void deleteNote(String id);

        Map<String, Object> uploadImage(MultipartFile file);

        StreamingResponseBody exportToZip();

        GraphDataDTO getKnowledgeGraph(String domain);

        GraphDataDTO getInitialGraph();

        GraphDataDTO expandGraphNode(String nodeId, String nodeType);

        Flux<String> streamPolishText(PolishRequestDTO request);

        String mergeNotes(String sourceId, String targetId);

        List<NoteHistoryDTO> getNoteHistory(String noteId);

        Note getNoteRevision(String noteId, Number revision);

        void rollbackNote(String noteId, Number revision);

        Map<String, List<String>> suggestTags(String keyword);

        PageData<NoteSummaryDTO> getTrashNotes(
                        Pageable pageable);

        void restoreNote(String id);

        List<SearchResultDTO> getSimilarNotes(String id);

        List<DrilledPropositionDTO> drillDownConcept(String concept);
}
