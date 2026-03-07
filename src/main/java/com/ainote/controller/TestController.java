package com.ainote.controller;

import com.ainote.dto.PropositionDTO;
import com.ainote.service.TestExtractionService;
import com.ainote.util.MarkdownAstSplitter;
import com.ainote.util.MarkdownAstSplitter.AstChunk;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private final TestExtractionService testExtractionService;

    public TestController(TestExtractionService testExtractionService) {
        this.testExtractionService = testExtractionService;
    }

    @PostMapping("/extract-propositions")
    public List<PropositionDTO> extractPropositions(@RequestBody Map<String, String> payload) {
        String text = payload.getOrDefault("content", "");
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text content cannot be empty.");
        }
        return testExtractionService.extractPropositions(text);
    }

    @PostMapping("/split-markdown")
    public List<AstChunk> splitMarkdown(@RequestBody Map<String, String> payload) {
        String text = payload.getOrDefault("content", "");
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text content cannot be empty.");
        }
        return MarkdownAstSplitter.splitMarkdown(text);
    }
}
