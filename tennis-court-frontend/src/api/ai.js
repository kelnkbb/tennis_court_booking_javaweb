// src/api/ai.js - 添加异步接口
import request from '@/utils/request'

// 原有的同步接口（保持不变）
export function aiSimpleChat(message) {
    return request({
        url: '/ai/chat/simple',
        method: 'post',
        data: { message },
        timeout: 120000
    })
}

// 🆕 新增：异步提交接口
export function aiAsyncChat(message) {
    return request({
        url: '/ai/chat/async',
        method: 'post',
        data: { message },
        timeout: 5000  // 只需短暂等待，立即返回 taskId
    })
}

// 🆕 新增：查询任务状态（轮询用，关闭全局错误弹窗避免刷屏）
export function getAiTaskStatus(taskId) {
    return request({
        url: `/ai/chat/task/${taskId}`,
        method: 'get',
        timeout: 5000,
        skipErrorToast: true
    })
}