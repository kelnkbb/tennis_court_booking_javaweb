package com.tennis_court_booking.pojo.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单统计：汇总 + 按日趋势
 */
@Data
public class OrderStatsVO {

    private OrderStatsSummaryVO summary;
    private List<OrderDailyStatsVO> daily = new ArrayList<>();
}
