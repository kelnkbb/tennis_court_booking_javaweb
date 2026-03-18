package com.tennis_court_booking.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Court {

    /**
     * 主键 ID（自增）
     */
    private Integer id;

    /**
     * 场馆名称
     */
    private String name;

    /**
     * 场馆地址
     */
    private String address;

    /**
     * 价格（每小时）
     */
    private BigDecimal price;

    /**
     * 图片 URL（多个图片用逗号分隔）
     */
    private String images;

    /**
     * 营业时间（如："09:00-22:00"）
     */
    private String openTime;

    /**
     * 状态：1-营业，0-关闭
     */
    private Integer status;

    /**
     * 场馆类型：1-室内，2-室外
     */
    private Integer type;

    /**
     * 联系电话
     */
    private String phone;
}