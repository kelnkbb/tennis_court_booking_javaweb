package com.tennis_court_booking.ai.rag;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

/**
 * RAG：注册 {@link ContentRetriever}，供 LangChain4j {@code @AiService(contentRetriever=...)} 使用。
 */
@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class RagConfiguration {

    @Bean(name = "contentRetriever")
    public ContentRetriever contentRetriever(
            RagProperties ragProperties,
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel) {

        if (!ragProperties.isEnabled()) {
            return (Query query) -> Collections.emptyList();
        }

        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(ragProperties.getMaxRetrieveResults())
                .minScore(ragProperties.getMinRetrieveScore())
                .displayName("tennis-platform-rag")
                .build();
    }
}
