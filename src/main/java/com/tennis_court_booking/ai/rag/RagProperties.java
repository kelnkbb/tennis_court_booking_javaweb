package com.tennis_court_booking.ai.rag;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RAG 知识库：向量化检索相关配置。
 */
@Data
@ConfigurationProperties(prefix = "ai.rag")
public class RagProperties {

    /**
     * 是否启用检索增强（关闭时仍注册空 ContentRetriever，避免 AiService 缺 Bean）。
     */
    private boolean enabled = true;

    /**
     * 向量存储：memory（默认，进程内）或 redis（需 Redis Stack / RediSearch 向量能力）。
     */
    private String store = "memory";

    /**
     * 与嵌入模型输出维度一致（通义 text-embedding-v2 为 1536）；仅 redis 存储时使用。
     */
    private int embeddingDimension = 1536;

    /**
     * Redis 向量索引名前缀（避免多环境冲突）。
     */
    private String redisIndexName = "tennis_rag_kb";

    private int maxRetrieveResults = 5;

    private double minRetrieveScore = 0.55;

    /** 文档切分：单段最大字符数（近似 token 控制） */
    private int maxSegmentChars = 480;

    private int segmentOverlapChars = 60;

    /** 启动时是否从 classpath:rag/ 加载文档并入库 */
    private boolean loadOnStartup = true;
}
