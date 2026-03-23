package com.tennis_court_booking.cache.compensation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 缓存删除补偿消息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheEvictCompensationMessage implements Serializable {

    /** 业务类型，预留扩展（当前为 COURT）。 */
    private String bizType;
    /** 业务主键（统一字符串，具体含义由 handler 解析）。 */
    private String bizId;
    /** 第几次补偿尝试（从 1 开始）。 */
    private int attempt;
}
