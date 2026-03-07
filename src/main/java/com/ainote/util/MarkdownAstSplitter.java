package com.ainote.util;

import com.ainote.enums.ChunkType;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;

import java.util.ArrayList;
import java.util.List;

public class MarkdownAstSplitter {

    public record AstChunk(ChunkType type, String content) {
    }

    public static List<AstChunk> splitMarkdown(String markdown) {
        List<AstChunk> chunks = new ArrayList<>();
        if (markdown == null || markdown.isBlank()) {
            return chunks;
        }

        Parser parser = Parser.builder().build();
        Document document = parser.parse(markdown);

        StringBuilder currentTextContext = new StringBuilder();

        for (Node node : document.getChildren()) {
            if (node instanceof Heading || node instanceof Paragraph) {
                // 累积文本节点作为前置上下文
                if (currentTextContext.length() > 0) {
                    currentTextContext.append("\n\n");
                }
                currentTextContext.append(node.getChars().toString().trim());
            } else if (node instanceof FencedCodeBlock) {
                FencedCodeBlock codeNode = (FencedCodeBlock) node;

                // 如果已累积文本，将其与代码块合并为一个 CODE 类型的 Chunk
                // （前置说明文本 + 代码块合并为一体，便于语义检索）

                // 将已累积的文本上下文与代码块合并
                String codeContent = codeNode.getChars().toString().trim();
                if (currentTextContext.length() > 0) {
                    codeContent = currentTextContext + "\n\n" + codeContent;
                    currentTextContext.setLength(0); // 清空上下文
                }
                chunks.add(new AstChunk(ChunkType.CODE, codeContent));
            } else {
                // 其他通用节点（列表、引用块等），视为文本上下文
                if (currentTextContext.length() > 0) {
                    currentTextContext.append("\n\n");
                }
                currentTextContext.append(node.getChars().toString().trim());
            }
        }

        // 刷出剩余的文本内容
        if (currentTextContext.length() > 0) {
            chunks.add(new AstChunk(ChunkType.TEXT, currentTextContext.toString()));
        }

        return chunks;
    }
}
