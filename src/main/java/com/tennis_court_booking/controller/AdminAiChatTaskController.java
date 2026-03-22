package com.tennis_court_booking.controller;

import com.tennis_court_booking.ai.async.AiChatTaskAdminSummaryVO;
import com.tennis_court_booking.ai.async.AiChatTaskCompensationService;
import com.tennis_court_booking.pojo.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理员：异步 AI 任务补偿（死信/失败重投、任务列表排查）。
 */
@RestController
@RequestMapping("/api/admin/ai/chat")
public class AdminAiChatTaskController {

    @Autowired
    private AiChatTaskCompensationService aiChatTaskCompensationService;

    /**
     * 扫描 Redis 中的任务（按状态过滤）。status 为空或 ALL 时默认只看 DEAD、FAILED。
     */
    @GetMapping("/tasks")
    public Result<List<AiChatTaskAdminSummaryVO>> listTasks(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "100") int limit) {
        return Result.success(aiChatTaskCompensationService.listForAdmin(status, limit));
    }

    /**
     * 将指定任务补偿重投到工作队列（需为 DEAD 或 FAILED，且记录中含 memoryId/userMessage）。
     */
    @PostMapping("/task/{taskId}/retry")
    public Result<Void> retryTask(@PathVariable String taskId) {
        return aiChatTaskCompensationService.retryForAdmin(taskId);
    }
}
