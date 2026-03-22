package com.tennis_court_booking.ai.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 当模型未输出 ---QUICK--- 或 JSON 解析失败时，根据正文关键词推断快捷选项，保证前端每轮都有可点项。
 */
public final class AiQuickOptionsFallback {

    private AiQuickOptionsFallback() {
    }

    public static List<String> infer(String content) {
        if (content == null || content.isBlank()) {
            return defaults();
        }
        String c = content;
        Set<String> set = new LinkedHashSet<>();

        if (contains(c, "待付款", "尚未支付", "未完成支付")) {
            set.add("微信");
            set.add("支付宝");
            set.add("闲鱼");
            set.add("取消该订单");
        }
        if (contains(c, "支付", "付款") && !set.contains("微信")) {
            set.add("微信");
            set.add("支付宝");
        }
        if (contains(c, "取消", "取消该订单", "确认取消")) {
            set.add("确认取消订单");
            set.add("暂不取消");
        }
        if (contains(c, "改约", "重订", "换时间")) {
            set.add("帮我改约其他时段");
        }
        if (contains(c, "我的预约", "订单号", "预约记录", "共") && contains(c, "笔", "订单")) {
            set.add("查看最新待付款");
            set.add("查看全部场地");
        }
        if (contains(c, "场地", "网球场") && contains(c, "可约", "时段", "预约")) {
            set.add("查明天可约时段");
            set.add("查看全部场地");
        }

        if (set.size() < 2) {
            set.addAll(defaults());
        }
        return limit(new ArrayList<>(set), 5);
    }

    private static boolean contains(String text, String... needles) {
        for (String n : needles) {
            if (text.contains(n)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> defaults() {
        return List.of("我的预约", "查看全部场地", "怎么预约");
    }

    private static List<String> limit(List<String> list, int max) {
        if (list.size() <= max) {
            return list;
        }
        return new ArrayList<>(list.subList(0, max));
    }
}
