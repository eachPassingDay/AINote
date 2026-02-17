package com.ainote.service.impl;

import com.ainote.dto.NoteRequestDTO;
import com.ainote.dto.NoteResponseDTO;
import com.ainote.service.NoteService;
import com.ainote.repository.NoteRepository;
import com.ainote.entity.Note;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Optional;
import java.util.regex.Pattern;
import java.io.File;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    private final ChatModel chatModel;
    private final VectorStore vectorStore;
    private final RestClient.Builder restClientBuilder;
    private final NoteRepository noteRepository;

    @Value("${vector.store.path}")
    private String vectorStorePath;

    @Value("${spring.ai.dashscope.api-key}")
    private String dashscopeApiKey;

    @Override
    public NoteResponseDTO generateSummary(NoteRequestDTO noteRequest) {
        String prompt = "Please provide a concise summary of the following note content:\n\n"
                + noteRequest.getContent();

        String summary = chatModel.call(prompt);

        NoteResponseDTO response = new NoteResponseDTO();
        response.setOriginalContent(noteRequest.getContent());
        response.setSummary(summary);

        return response;
    }

    private static final String SEMANTIC_DELIMITER = "||||";
    private static final double MERGE_THRESHOLD = 0.6; // Threshold for automatic merging

    @Override
    public void ingestNote(NoteRequestDTO noteRequest) {
        String systemInstruction = """
                You are a strict data processing engine. NOT a chat assistant.
                Rules:
                1. Insert the delimiter '%s' between distinct, unrelated topics.
                2. Do NOT change the original wording unless there are formatting errors.
                3. OUTPUT ONLY THE PROCESSED TEXT. NO PREAMBLE. NO POSTSCRIPT. NO "Here is the text".
                4. If the text is empty, output NOTHING.
                5. If the text is short or contains only one topic, output it AS IS without delimiters.
                """.formatted(SEMANTIC_DELIMITER);

        String userContent = noteRequest.getContent();

        String processedContent = chatModel.call(
                new Prompt(List.of(
                        new SystemMessage(systemInstruction),
                        new UserMessage(userContent))))
                .getResult().getOutput().getContent();

        System.out.println("====== [DEBUG] AI Processed Content ======");
        System.out.println(processedContent);

        String[] semanticSegments = processedContent.split(Pattern.quote(SEMANTIC_DELIMITER));

        List<String> newSegments = new ArrayList<>();

        for (String segment : semanticSegments) {
            String cleanSegment = segment.strip();
            if (cleanSegment.isBlank())
                continue;

            // Step: Compare each piece
            boolean merged = checkAndMerge(cleanSegment);

            if (!merged) {
                newSegments.add(cleanSegment);
            }
        }

        // Check if we ended up with nothing because AI returned garbage (empty)
        if (newSegments.isEmpty() && semanticSegments.length == 0 && !userContent.isBlank()) {
            System.out.println("Warning: AI returned empty segments. Fallback to original content.");
            // Try to merge the original content directly
            if (!checkAndMerge(userContent)) {
                newSegments.add(userContent);
            }
        }

        // Save remaining segments as new note(s)
        if (!newSegments.isEmpty()) {
            // Combine remaining segments into one Note (or multiple? Let's do one for now
            // to keep context)
            String combinedContent = String.join("\n\n" + SEMANTIC_DELIMITER + "\n\n", newSegments);

            Note note = new Note();
            note.setId(UUID.randomUUID().toString());
            note.setTitle(noteRequest.getTitle() != null ? noteRequest.getTitle() : "Untitled Note");
            note.setContent(combinedContent);
            note.setSummary(generateSummary(noteRequest).getSummary());

            noteRepository.save(note);
            System.out.println("Saved new note to DB: " + note.getId());

            // Vectorize
            vectorizeContent(combinedContent, note.getId(), note.getTitle());
        }
    }

    /**
     * Checks if the segment matches an existing note. If so, merges it.
     * 
     * @return true if merged, false if it should be treated as new.
     */
    private boolean checkAndMerge(String segment) {
        // 1. Initial Retrieval
        List<Document> initialResults = vectorStore.similaritySearch(
                org.springframework.ai.vectorstore.SearchRequest.query(segment).withTopK(5));

        if (initialResults.isEmpty()) {
            return false;
        }

        // 2. Rerank
        RerankResult topResult = performRerank(segment, initialResults);

        if (topResult != null && topResult.score >= MERGE_THRESHOLD) {
            // 3. Merge
            Document bestDoc = initialResults.get(topResult.index);
            String noteId = (String) bestDoc.getMetadata().get("note_id");

            if (noteId != null) {
                Optional<Note> noteOpt = noteRepository.findById(noteId);
                if (noteOpt.isPresent()) {
                    System.out.println("Segment matches existing note " + noteId + " (Score: " + topResult.score
                            + "). Merging...");
                    mergeAndSave(noteOpt.get(), segment);
                    return true;
                }
            }
        }

        return false;
    }

    private record RerankResult(int index, double score) {
    }

    private RerankResult performRerank(String query, List<Document> documents) {
        try {
            List<String> docContents = documents.stream()
                    .map(Document::getContent)
                    .collect(Collectors.toList());

            // Structure for standard API:
            Map<String, Object> input = Map.of(
                    "query", query,
                    "documents", docContents);

            Map<String, Object> requestBody = Map.of(
                    "model", "gte-rerank",
                    "input", input);

            RestClient restClient = restClientBuilder.build();
            // Use the standard URL
            String responseBody = restClient.post()
                    .uri("https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank")
                    .header("Authorization", "Bearer " + dashscopeApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseBody);

            // Standard API returns: { "output": { "results": [ { "index": 0,
            // "relevance_score": 0.9 }, ... ] } }
            JsonNode resultsNode = root.path("output").path("results");

            if (resultsNode.isMissingNode() || resultsNode.isEmpty()) {
                return null;
            }

            JsonNode topResult = resultsNode.get(0);
            return new RerankResult(
                    topResult.path("index").asInt(),
                    topResult.path("relevance_score").asDouble());

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Helper to vectorize content specifically for existing notes (Merge flow)
    private void vectorizeContent(String content, String noteId, String title) {
        // Reuse the logic from ingestNote but skip DB creation and use existing ID
        // 1. Process
        String systemInstruction = """
                You are a strict data processing engine.
                Rules:
                1. Insert the delimiter '%s' between distinct, unrelated topics.
                2. Do NOT change the original wording unless there are formatting errors.
                3. OUTPUT ONLY THE PROCESSED TEXT.
                """.formatted(SEMANTIC_DELIMITER);

        String processedContent = chatModel.call(
                new Prompt(List.of(
                        new SystemMessage(systemInstruction),
                        new UserMessage(content))))
                .getResult().getOutput().getContent();

        String[] semanticSegments = processedContent.split(Pattern.quote(SEMANTIC_DELIMITER));
        List<Document> documentsToStore = new ArrayList<>();
        TokenTextSplitter textSplitter = new TokenTextSplitter(500, 100, 5, 10000, true);

        for (String segment : semanticSegments) {
            String cleanSegment = segment.strip();
            if (cleanSegment.isBlank())
                continue;

            Document segmentDoc = new Document(cleanSegment,
                    Map.of(
                            "title", title != null ? title : "",
                            "note_id", noteId,
                            "original_length", cleanSegment.length()));

            documentsToStore.addAll(textSplitter.apply(List.of(segmentDoc)));
        }

        if (!documentsToStore.isEmpty()) {
            vectorStore.add(documentsToStore);
            if (vectorStore instanceof SimpleVectorStore) {
                ((SimpleVectorStore) vectorStore).save(new File(vectorStorePath));
            }
        }
    }

    @Override
    public String semanticSearch(String query, double threshold) {
        List<Document> initialResults = vectorStore.similaritySearch(
                org.springframework.ai.vectorstore.SearchRequest.query(query).withTopK(10));

        if (initialResults.isEmpty()) {
            return "No relevant content found.";
        }

        return executeRerankLogic(query, initialResults, threshold);
    }

    private String executeRerankLogic(String query, List<Document> initialResults, double threshold) {
        try {
            // 1. Prepare Request Data
            List<String> documents = initialResults.stream()
                    .map(Document::getContent)
                    .collect(Collectors.toList());

            // Structure for standard API:
            Map<String, Object> input = Map.of(
                    "query", query,
                    "documents", documents);

            Map<String, Object> requestBody = Map.of(
                    "model", "gte-rerank",
                    "input", input);

            // 2. Call DashScope API manually
            RestClient restClient = restClientBuilder.build();
            String responseBody = restClient.post()
                    .uri("https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank")
                    .header("Authorization", "Bearer " + dashscopeApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            // 3. Parse Response
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseBody);

            // Standard API returns: { "output": { "results": [ { "index": 0,
            // "relevance_score": 0.9 }, ... ] } }
            JsonNode resultsNode = root.path("output").path("results");

            if (resultsNode.isMissingNode() || resultsNode.isEmpty()) {
                return "No relevant content after rerank (Empty response).";
            }

            // Get top result
            JsonNode topResult = resultsNode.get(0);
            int index = topResult.path("index").asInt();
            double score = topResult.path("relevance_score").asDouble();

            if (score >= threshold) {
                // Return the original document content based on index
                Document bestDoc = initialResults.get(index);
                return "Found relevant note (Score: " + score + "):\n" + bestDoc.getContent();
            } else {
                return "Relevance score (" + score + ") below threshold (" + threshold
                        + "). No sufficient match found.";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Error during rerank: " + e.getMessage();
        }
    }

    private String mergeAndSave(Note existingNote, String newContent) {
        // 1. LLM Merge
        String mergePrompt = """
                You are an expert editor. Please merge the following two notes into one coherent note.
                Remove duplicates and ensure smooth transitions.

                Note A (Existing):
                %s

                Note B (New Info):
                %s

                Parsed Merged Content:
                """;

        // Escape % in the content to prevent formatting processing issues if
        // formatted() is still desired,
        // OR simply use standard concat or MessageFormat.
        // Better: Use String.format / .formatted but ensure no user content is inside
        // the format string itself.
        // Wait, the block text above HAS %s placeholders.
        // If content has %, .formatted throws.
        // So we must escape content.

        System.out.println("DEBUG: Merging Note ID: " + existingNote.getId());

        String inputA = existingNote.getContent().replace("%", "%%");
        String inputB = newContent.replace("%", "%%");

        String finalPrompt = mergePrompt.formatted(inputA, inputB);

        String mergedContent = chatModel.call(finalPrompt);

        // 2. Update DB
        existingNote.setContent(mergedContent);
        // Regenerate summary? Optional.
        // existingNote.setSummary(...);
        noteRepository.save(existingNote);

        // 3. Update Vector Store
        // For SimpleVectorStore, we cannot easily delete old segments by ID without
        // iterating.
        // We will just add the new segments. In a production PGVector/Milvus, we would
        // delete by note_id.
        vectorizeContent(mergedContent, existingNote.getId(), existingNote.getTitle());

        return "Merged successfully. New content length: " + mergedContent.length();
    }
}
