package com.ainote.service;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.xml.sax.ContentHandler;

import java.io.InputStream;

@Service
public class DocumentExtractionService {

    public DocumentExtractionService() {
    }

    /**
     * 从 Tika 支持的 InputStream（PDF、Word 等）中提取文本内容，并进行轻量 Markdown 格式化。
     *
     * @param stream 上传文件的输入流
     * @return 提取后的纯文本字符串（近似 Markdown 格式）
     * @throws Exception 解析失败时抛出异常
     */
    public String extractToMarkdown(InputStream stream) throws Exception {
        AutoDetectParser parser = new AutoDetectParser();
        // -1 表示不限制解析字符数
        ContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();

        // Tika 解析输入流并将文本推送到 handler
        parser.parse(stream, handler, metadata);

        String rawText = handler.toString();

        // 对原始文本进行基础格式化，使其更接近 Markdown
        return normalizeToMarkdown(rawText);
    }

    private String normalizeToMarkdown(String rawText) {
        if (rawText == null || rawText.isEmpty()) {
            return "";
        }

        // 1. 移除过多的连续空行（超过 2 行压缩为 2 行）
        String md = rawText.replaceAll("\\n{3,}", "\n\n");

        // TODO: 2. 尝试识别合成标题（短句且大写开头的行）
        // 当前使用粗略启发式规则，因为 BodyContentHandler 会丢弃 HTML 标签。
        // 后续可改用 ToXMLContentHandler 获取更精确的结构信息。

        // 3. 去除首尾空白字符
        return md.trim();
    }
}
