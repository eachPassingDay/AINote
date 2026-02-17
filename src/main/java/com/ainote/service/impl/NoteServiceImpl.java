package com.ainote.service.impl;

import com.ainote.dto.NoteRequestDTO;
import com.ainote.dto.NoteResponseDTO;
import com.ainote.service.NoteService;

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

    @Override
    public void ingestNote(NoteRequestDTO noteRequest) {
        String systemInstruction = """
                You are a strict data processing engine. NOT a chat assistant.
                Rules:
                1. Insert the delimiter '%s' between distinct, unrelated topics.
                2. Do NOT change the original wording unless there are formatting errors.
                3. OUTPUT ONLY THE PROCESSED TEXT. NO PREAMBLE. NO POSTSCRIPT. NO "Here is the text".
                4. If the text is empty, output NOTHING.
                """.formatted(SEMANTIC_DELIMITER);

        String userContent = noteRequest.getContent();

        String processedContent = chatModel.call(
                new Prompt(List.of(
                        new SystemMessage(systemInstruction),
                        new UserMessage(userContent))))
                .getResult().getOutput().getContent();

        System.out.println("====== [DEBUG] AI Raw Output Start ======");
        System.out.println(processedContent);
        System.out.println("====== [DEBUG] AI Raw Output End ======");

        String[] semanticSegments = processedContent.split(Pattern.quote(SEMANTIC_DELIMITER));

        List<Document> documentsToStore = new ArrayList<>();
        TokenTextSplitter textSplitter = new TokenTextSplitter(500, 100, 5, 10000, true);

        for (String segment : semanticSegments) {
            String cleanSegment = segment.strip();

            if (cleanSegment.isBlank()) {
                continue;
            }

            Document segmentDoc = new Document(cleanSegment,
                    Map.of(
                            "title", noteRequest.getTitle() != null ? noteRequest.getTitle() : "",
                            "original_length", cleanSegment.length()));

            List<Document> chunkedDocs = textSplitter.apply(List.of(segmentDoc));
            System.out.println("   -> Segment splitted into " + chunkedDocs.size() + " chunks.");
            documentsToStore.addAll(chunkedDocs);
        }

        if (!documentsToStore.isEmpty()) {
            vectorStore.add(documentsToStore);
            if (vectorStore instanceof SimpleVectorStore) {
                ((SimpleVectorStore) vectorStore).save(new File(vectorStorePath));
            }
        }
        System.out.println("Final: Stored " + documentsToStore.size() + " vectors.");
    }

    private void ingestNewNote(String content) {
        NoteRequestDTO dto = new NoteRequestDTO();
        dto.setContent(content);
        dto.setTitle("Auto-Ingested Note");
        ingestNote(dto);
    }

    @Override
    public String semanticSearch(String query, double threshold) {
        List<Document> initialResults = vectorStore.similaritySearch(
                org.springframework.ai.vectorstore.SearchRequest.query(query).withTopK(10));

        if (initialResults.isEmpty()) {
            ingestNewNote(query);
            return "No relevant content found. Saved as new note.";
        }

        return executeRerankLogic(query, initialResults, threshold);
    }

    private String executeRerankLogic(String query, List<Document> initialResults, double threshold) {
        try {
            // 1. Prepare Request Data
            List<String> documents = initialResults.stream()
                    .map(Document::getContent)
                    .collect(Collectors.toList());

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

            // Expected Response: { "output": { "results": [ { "index": 0,
            // "relevance_score": 0.8 }, ... ] } }
            JsonNode resultsNode = root.path("output").path("results");

            if (resultsNode.isMissingNode() || resultsNode.isEmpty()) {
                ingestNewNote(query);
                return "No relevant content after rerank (Empty response). Saved as new note.";
            }

            // Get top result
            JsonNode topResult = resultsNode.get(0);
            int index = topResult.path("index").asInt();
            double score = topResult.path("relevance_score").asDouble();

            System.out.println("Top Rerank Score: " + score);

            if (score >= threshold) {
                // Return the original document content based on index
                Document bestDoc = initialResults.get(index);
                return "Found relevant note (Score: " + score + "):\n" + bestDoc.getContent();
            } else {
                ingestNewNote(query);
                return "Relevance score (" + score + ") below threshold (" + threshold + "). Saved as new note.";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Error during rerank: " + e.getMessage();
        }
    }
}
