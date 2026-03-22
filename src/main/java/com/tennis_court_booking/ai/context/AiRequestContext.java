package com.tennis_court_booking.ai.context;

/**
 * AI 对话请求内显式传递当前用户 ID，避免仅依赖 RequestContextHolder
 * （工具执行时可能拿不到 Servlet 请求属性，导致误查成空列表进而被模型编造数据）。
 */
public final class AiRequestContext {

    private static final ThreadLocal<Integer> USER_ID = new ThreadLocal<>();

    private AiRequestContext() {
    }

    public static void setUserId(Integer userId) {
        if (userId == null) {
            USER_ID.remove();
        } else {
            USER_ID.set(userId);
        }
    }

    public static Integer getUserId() {
        return USER_ID.get();
    }

    public static void clear() {
        USER_ID.remove();
    }
}
