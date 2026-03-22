package com.tennis_court_booking.ai.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tennis_court_booking.ai.async.AiChatAsyncService;
import com.tennis_court_booking.ai.async.AiChatTaskCompensationService;
import com.tennis_court_booking.ai.async.AiChatTaskStatusVO;
import com.tennis_court_booking.ai.context.AiRequestContext;
import com.tennis_court_booking.ai.pojo.AiAsyncSubmitVO;
import com.tennis_court_booking.ai.pojo.AiChatReply;
import com.tennis_court_booking.ai.service.SimpleAiService;
import com.tennis_court_booking.ai.util.AiReplyParser;
import com.tennis_court_booking.pojo.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    @Autowired
    private SimpleAiService simpleAiService;

    @Autowired
    private AiChatAsyncService aiChatAsyncService;

    @Autowired
    private AiChatTaskCompensationService aiChatTaskCompensationService;

    @PostMapping("/chat/simple")
    public Result<AiChatReply> simpleChat(
            HttpServletRequest request,
            @RequestBody Map<String, String> body) {

        System.out.println("============ [Controller] 收到请求 ============");
        System.out.println("[Controller] URL: " + request.getRequestURI());
        System.out.println("[Controller] Authorization Header: " + request.getHeader("Authorization"));

        String message = body == null ? null : body.get("message");
        if (!StringUtils.hasText(message)) {
            System.out.println("[Controller] message 为空");
            return Result.error(400, "message 不能为空");
        }

        // 直接从 HttpServletRequest 获取 userId（由 JWT 过滤器设置）
        Integer userId = (Integer) request.getAttribute("userId");
        System.out.println("[Controller] 从 request 获取 userId: " + userId);
        System.out.println("[Controller] username: " + request.getAttribute("username"));
        System.out.println("[Controller] role: " + request.getAttribute("role"));

        if (userId == null) {
            System.out.println("[Controller] ❌ userId 为 null - JWT 过滤器可能没有正确设置 attribute");
            return Result.error(401, "未授权，请先登录");
        }

        System.out.println("[Controller] ✅ 认证成功，userId: " + userId);

        // 将当前登录用户绑定到 LLM 会话记忆（memoryId），从而支持多轮上下文
        String memoryId = "u:" + userId;
        try {
            AiRequestContext.setUserId(userId);
            String answer = simpleAiService.chat(memoryId, message);
            return Result.success(AiReplyParser.parse(answer));
        } finally {
            AiRequestContext.clear();
        }
    }

    /**
     * 异步对话：立即返回 taskId，由 RabbitMQ 消费后写入 Redis；客户端轮询 {@link #taskStatus}。
     * 需本地启动 RabbitMQ；任务状态见 Redis key ai:chat:task:{taskId}。
     */
    @PostMapping("/chat/async")
    public Result<AiAsyncSubmitVO> asyncChat(HttpServletRequest request, @RequestBody Map<String, String> body) {
        String message = body == null ? null : body.get("message");
        if (!StringUtils.hasText(message)) {
            return Result.error(400, "message 不能为空");
        }
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未授权，请先登录");
        }
        String memoryId = "u:" + userId;
        try {
            String taskId = aiChatAsyncService.submit(userId, memoryId, message.trim());
            return Result.success(new AiAsyncSubmitVO(taskId, "/api/ai/chat/task/" + taskId));
        } catch (JsonProcessingException e) {
            return Result.error(500, "任务创建失败");
        }
    }

    @GetMapping("/chat/task/{taskId}")
    public Result<AiChatTaskStatusVO> taskStatus(HttpServletRequest request, @PathVariable String taskId) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未授权，请先登录");
        }
        return aiChatAsyncService.getTaskForUser(taskId, userId);
    }

    /**
     * 用户对本人 DEAD/FAILED 任务发起补偿重投（重新入队，需轮询同一 taskId）。
     */
    @PostMapping("/chat/task/{taskId}/retry")
    public Result<Void> retryMyTask(HttpServletRequest request, @PathVariable String taskId) {
        Integer userId = (Integer) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未授权，请先登录");
        }
        return aiChatTaskCompensationService.retryForUser(taskId, userId);
    }
}
