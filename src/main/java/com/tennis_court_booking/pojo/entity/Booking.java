// com/tennis_court_booking/pojo/entity/Booking.java
package com.tennis_court_booking.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Booking {

    /**
     * 主键 ID（自增）
     */
    private Integer id;

    /**
     * 预约单号（唯一）
     */
    private String bookingNo;

    /**
     * 场地ID
     */
    private Integer courtId;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 预约日期
     */
    private LocalDate bookingDate;

    /**
     * 开始时间
     */
    private LocalTime startTime;

    /**
     * 结束时间
     */
    private LocalTime endTime;

    /**
     * 预约时长（小时）
     */
    private BigDecimal duration;

    /**
     * 单价（元/小时）
     */
    private BigDecimal unitPrice;

    /**
     * 总金额
     */
    private BigDecimal totalAmount;

    /**
     * 联系人姓名
     */
    private String contactName;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 备注
     */
    private String remark;

    /**
     * 状态：0-已取消 1-待付款 2-已付款 3-已完成 4-已过期
     */
    private Integer status;

    /**
     * 取消申请：0-无 1-待管理员审核 2-已驳回
     */
    private Integer cancelRequestStatus;

    /** 付款渠道：wechat / alipay / xianyu */
    private String paymentChannel;

    private LocalDateTime payTime;

    /**
     * 线下付款审核：0-无 1-待管理员确认已付款 2-已驳回
     */
    private Integer paymentVerifyStatus;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}