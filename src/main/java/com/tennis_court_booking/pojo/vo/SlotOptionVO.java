package com.tennis_court_booking.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 可选预约时段（整点起 1 小时或连续 2 小时）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlotOptionVO {
    /** 下拉 value，如 09:00|10:00 */
    private String value;
    /** 展示文案 */
    private String label;
    private String startTime;
    private String endTime;
    private int hours;
}
