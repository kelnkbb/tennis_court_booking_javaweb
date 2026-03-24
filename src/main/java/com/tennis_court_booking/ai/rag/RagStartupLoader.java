package com.tennis_court_booking.ai.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 启动时加载平台文档（若开启 {@link RagProperties#loadOnStartup}）。
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class RagStartupLoader implements ApplicationRunner {

    private final RagProperties ragProperties;
    private final RagKnowledgeIngestionService ragKnowledgeIngestionService;

    @Override
    public void run(ApplicationArguments args) {
        if (!ragProperties.isLoadOnStartup()) {
            return;
        }
        int n = ragKnowledgeIngestionService.ingestClasspathDocuments(true);
        log.info("RAG 启动加载完成，共写入 {} 条向量片段", n);
    }
}
