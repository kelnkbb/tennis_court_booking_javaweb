package com.tennis_court_booking.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 某场地某日可选时段及每日容量说明
 */
@Data
public class CourtSlotOptionsVO {
    /** 当日已有预约总时长（小时），含所有用户 */
    private BigDecimal usedHours;
    /** 每日全用户合计上限（小时） */
    private BigDecimal maxDailyHours;
    /** 当日剩余可约时长 */
    private BigDecimal remainingHours;
    /** 场地营业时间展示 */
    private String openTimeDisplay;
    /** 该日是否已约满（≥2 小时则不可再选） */
    private boolean dayFull;
    /** 场地未营业 */
    private boolean courtClosed;
    private List<SlotOptionVO> options = new ArrayList<>();
}
