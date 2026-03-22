package com.tennis_court_booking.ai.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tennis_court_booking.ai.pojo.AiChatReply;

import java.util.ArrayList;
import java.util.List;

/**
 * 从模型原文中拆分展示正文与快捷选项 JSON 数组（取最后一次出现的分隔符，避免正文误匹配）。
 * 若模型未按约定输出或 JSON 不合法，会尝试从尾部提取数组，仍失败则按正文关键词推断快捷项。
 */
public final class AiReplyParser {

    private static final String MARKER = "---QUICK---";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AiReplyParser() {
    }

    public static AiChatReply parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new AiChatReply("", new ArrayList<>());
        }
        int idx = raw.lastIndexOf(MARKER);
        if (idx < 0) {
            String body = raw.trim();
            return finish(body, new ArrayList<>());
        }
        String body = raw.substring(0, idx).trim();
        String tail = raw.substring(idx + MARKER.length()).trim().replaceFirst("^\\R+", "");
        tail = stripMarkdownFence(tail);
        List<String> options = parseQuickJson(tail);
        if (body.isEmpty()) {
            body = options.isEmpty() ? raw.replace(MARKER, "").trim() : "请查看下方快捷操作。";
        }
        return finish(body, options);
    }

    private static AiChatReply finish(String body, List<String> options) {
        List<String> out = options == null ? new ArrayList<>() : new ArrayList<>(options);
        if (out.isEmpty()) {
            out.addAll(AiQuickOptionsFallback.infer(body));
        }
        return new AiChatReply(body, out);
    }

    private static String stripMarkdownFence(String s) {
        String t = s.trim();
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            if (firstNl > 0) {
                t = t.substring(firstNl + 1);
            }
            int end = t.lastIndexOf("```");
            if (end >= 0) {
                t = t.substring(0, end);
            }
        }
        return t.trim();
    }

    private static List<String> parseQuickJson(String tail) {
        List<String> first = tryReadStringList(tail.trim());
        if (!first.isEmpty()) {
            return first;
        }
        String extracted = extractLastJsonArray(tail);
        if (extracted != null) {
            return tryReadStringList(extracted);
        }
        return new ArrayList<>();
    }

    private static List<String> tryReadStringList(String json) {
        try {
            List<String> list = MAPPER.readValue(json, new TypeReference<>() {
            });
            if (list == null) {
                return new ArrayList<>();
            }
            List<String> out = new ArrayList<>();
            for (String s : list) {
                if (s == null) {
                    continue;
                }
                String x = s.trim();
                if (!x.isEmpty() && out.size() < 5) {
                    out.add(x);
                }
            }
            return out;
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    /**
     * 从文本中取最后一个与括号匹配的 JSON 数组子串（应对模型在数组前后多打了说明文字）。
     */
    private static String extractLastJsonArray(String s) {
        int end = s.lastIndexOf(']');
        if (end < 0) {
            return null;
        }
        int depth = 0;
        for (int i = end; i >= 0; i--) {
            char c = s.charAt(i);
            if (c == ']') {
                depth++;
            } else if (c == '[') {
                depth--;
                if (depth == 0) {
                    return s.substring(i, end + 1);
                }
            }
        }
        return null;
    }
}
