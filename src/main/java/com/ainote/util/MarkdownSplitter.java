package com.ainote.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

public class MarkdownSplitter {

    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```[\\s\\S]*?```");
    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[.*?\\]\\(.*?\\)");

    public static class ProtectedContent {
        public String textWithPlaceholders;
        public Map<String, String> replacements = new HashMap<>();
    }

    /**
     * 第一步：用占位符替换代码块和图片，保护其不被 LLM 修改。
     */
    public static ProtectedContent extractAndProtect(String content) {
        ProtectedContent result = new ProtectedContent();
        String tempContent = content;

        // 1. 提取代码块
        Matcher codeMatcher = CODE_BLOCK_PATTERN.matcher(tempContent);
        int codeCounter = 1;
        StringBuffer sbCode = new StringBuffer();
        while (codeMatcher.find()) {
            String placeholder = "{{CODE_BLOCK_" + codeCounter + "}}";
            result.replacements.put(placeholder, codeMatcher.group());
            codeMatcher.appendReplacement(sbCode, placeholder);
            codeCounter++;
        }
        codeMatcher.appendTail(sbCode);
        tempContent = sbCode.toString();

        // 2. 提取图片
        Matcher imageMatcher = IMAGE_PATTERN.matcher(tempContent);
        int imageCounter = 1;
        StringBuffer sbImage = new StringBuffer();
        while (imageMatcher.find()) {
            String placeholder = "{{IMAGE_" + imageCounter + "}}";
            result.replacements.put(placeholder, imageMatcher.group());
            imageMatcher.appendReplacement(sbImage, placeholder);
            imageCounter++;
        }
        imageMatcher.appendTail(sbImage);
        tempContent = sbImage.toString();

        result.textWithPlaceholders = tempContent;
        return result;
    }

    /**
     * 第三步：将 LLM 处理后的内容精确切分为 Chunk。
     * 代码块保持为单个完整 Chunk；图片与前置文本合并；普通文本使用 TokenTextSplitter 切分。
     */
    public static List<Document> splitAndRestore(String processedContentWithPlaceholders,
            Map<String, String> replacements, Map<String, Object> baseMetadata) {
        List<Document> finalDocuments = new ArrayList<>();
        TokenTextSplitter textSplitter = new TokenTextSplitter(500, 100, 5, 10000, true);

        // 用于匹配占位符的正则表达式
        Pattern placeholderPattern = Pattern.compile("\\{\\{(CODE_BLOCK_|IMAGE_)\\d+\\}\\}");
        Matcher matcher = placeholderPattern.matcher(processedContentWithPlaceholders);

        int lastEnd = 0;
        String pendingTextForImage = "";

        while (matcher.find()) {
            // 占位符之前的文本
            String textBefore = processedContentWithPlaceholders.substring(lastEnd, matcher.start()).trim();
            String placeholder = matcher.group();
            String originalElement = replacements.get(placeholder);

            if (placeholder.startsWith("{{CODE_BLOCK_")) {
                // 如果存在前置文本，先切分并添加
                if (!pendingTextForImage.isEmpty() || !textBefore.isEmpty()) {
                    String textToSplit = (pendingTextForImage + " " + textBefore).trim();
                    if (!textToSplit.isEmpty()) {
                        finalDocuments.addAll(splitNormalText(textToSplit, textSplitter, baseMetadata));
                    }
                    pendingTextForImage = ""; // 重置待合并文本
                }

                // 将代码块作为单个完整 Chunk 添加
                Map<String, Object> codeMeta = new HashMap<>(baseMetadata);
                codeMeta.put("type", "code_block");
                codeMeta.put("original_length", originalElement.length());
                finalDocuments.add(new Document(originalElement, codeMeta));

            } else if (placeholder.startsWith("{{IMAGE_")) {
                // 图片应与前置文本合并
                pendingTextForImage += " " + textBefore + "\n" + originalElement;
            }

            lastEnd = matcher.end();
        }

        // 处理剩余文本
        String remainingText = processedContentWithPlaceholders.substring(lastEnd).trim();
        if (!pendingTextForImage.isEmpty() || !remainingText.isEmpty()) {
            String textToSplit = (pendingTextForImage + " " + remainingText).trim();
            if (!textToSplit.isEmpty()) {
                finalDocuments.addAll(splitNormalText(textToSplit, textSplitter, baseMetadata));
            }
        }

        return finalDocuments;
    }

    private static List<Document> splitNormalText(String text, TokenTextSplitter splitter,
            Map<String, Object> baseMetadata) {
        List<Document> docs = new ArrayList<>();
        if (text.length() < 100) {
            Map<String, Object> meta = new HashMap<>(baseMetadata);
            meta.put("original_length", text.length());
            meta.put("type", "text");
            docs.add(new Document(text, meta));
        } else {
            Document doc = new Document(text, baseMetadata);
            List<Document> splitDocs = splitter.apply(List.of(doc));
            for (Document d : splitDocs) {
                d.getMetadata().put("type", "text");
                d.getMetadata().put("original_length", d.getContent().length());
                docs.add(d);
            }
        }
        return docs;
    }
}
