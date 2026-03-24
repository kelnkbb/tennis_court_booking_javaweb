package com.tennis_court_booking.ai.rag;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 将 classpath:rag/ 下文档切分、向量化并写入 {@link EmbeddingStore}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagKnowledgeIngestionService {

    private final RagProperties ragProperties;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    /**
     * @param clearFirst 是否先清空向量库（全量重建）
     * @return 写入的文本片段数量
     */
    public int ingestClasspathDocuments(boolean clearFirst) {
        if (!ragProperties.isEnabled()) {
            log.info("RAG 已关闭，跳过文档入库");
            return 0;
        }
        if (clearFirst) {
            embeddingStore.removeAll();
        }

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources;
        try {
            resources = resolver.getResources("classpath:rag/**/*.md");
        } catch (IOException e) {
            log.warn("扫描 classpath:rag/**/*.md 失败: {}", e.getMessage());
            return 0;
        }

        int total = 0;
        for (Resource resource : resources) {
            if (!resource.isReadable()) {
                continue;
            }
            String filename = resource.getFilename() != null ? resource.getFilename() : "unknown";
            try {
                String text = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                if (text.isBlank()) {
                    continue;
                }
                Metadata meta = Metadata.from("source", filename);
                List<TextSegment> segments = splitIntoSegments(text, meta);
                for (TextSegment seg : segments) {
                    Embedding emb = embeddingModel.embed(seg).content();
                    embeddingStore.add(emb, seg);
                    total++;
                }
                log.info("RAG 入库: {} -> {} 片段", filename, segments.size());
            } catch (IOException e) {
                log.warn("读取 RAG 文档失败 {}: {}", filename, e.getMessage());
            }
        }
        return total;
    }

    private List<TextSegment> splitIntoSegments(String text, Metadata baseMeta) {
        int max = Math.max(200, ragProperties.getMaxSegmentChars());
        int overlap = Math.max(0, Math.min(ragProperties.getSegmentOverlapChars(), max / 2));
        List<TextSegment> out = new ArrayList<>();
        int step = max - overlap;
        if (step <= 0) {
            step = max;
        }
        for (int start = 0; start < text.length(); start += step) {
            int end = Math.min(text.length(), start + max);
            if (start >= end) {
                break;
            }
            String chunk = text.substring(start, end).trim();
            if (chunk.isEmpty()) {
                continue;
            }
            out.add(TextSegment.from(chunk, baseMeta));
        }
        return out;
    }
}
