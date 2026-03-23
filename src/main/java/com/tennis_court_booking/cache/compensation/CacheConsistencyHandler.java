package com.tennis_court_booking.cache.compensation;

/**
 * 统一缓存一致性补偿处理器。
 */
public interface CacheConsistencyHandler {

    /**
     * 处理器负责的业务类型（如 COURT）。
     */
    String bizType();

    /**
     * 执行 Redis 侧缓存删除。
     *
     * @param bizId 业务主键（字符串形式）
     * @return true 删除成功；false 删除失败（可继续补偿）
     */
    boolean evictRedis(String bizId);
}
