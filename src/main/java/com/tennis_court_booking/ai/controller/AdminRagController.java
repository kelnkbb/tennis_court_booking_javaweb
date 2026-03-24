package com.tennis_court_booking.ai.controller;

import com.tennis_court_booking.ai.rag.RagKnowledgeIngestionService;
import com.tennis_court_booking.pojo.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理员：重建 RAG 知识库（重新切分并写入向量存储）。
 */
@RestController
@RequestMapping("/api/admin/ai/rag")
public class AdminRagController {

    @Autowired
    private RagKnowledgeIngestionService ragKnowledgeIngestionService;

    @PostMapping("/reindex")
    public Result<Map<String, Object>> reindex() {
        int n = ragKnowledgeIngestionService.ingestClasspathDocuments(true);
        Map<String, Object> data = new HashMap<>(2);
        data.put("ingestedChunks", n);
        data.put("message", "已根据 classpath:rag/ 下文档全量重建索引");
        return Result.success(data);
    }
}
