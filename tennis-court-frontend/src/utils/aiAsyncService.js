// src/utils/aiAsyncService.js
import { aiAsyncChat, getAiTaskStatus } from '@/api/ai'

export class AiAsyncService {
    constructor(options = {}) {
        this.pollingInterval = options.pollingInterval || 2000  // 默认2秒轮询
        this.maxPollingAttempts = options.maxPollingAttempts || 30  // 最多轮询30次
        this.pendingTasks = new Map()  // 存储正在轮询的任务
    }

    /**
     * 发送异步消息
     * @param {string} message - 用户消息
     * @returns {Promise} 返回 AI 回复结果
     */
    async sendMessage(message) {
        // 1. 提交异步任务
        const submitResult = await aiAsyncChat(message)
        
        if (submitResult.code !== 200) {
            throw new Error(submitResult.message || '提交失败')
        }
        
        const { taskId, statusPath } = submitResult.data
        
        // 2. 开始轮询
        return this.pollTask(taskId)
    }

    /**
     * 轮询任务状态
     * @param {string} taskId - 任务ID
     * @returns {Promise} 返回 AI 回复结果
     */
    pollTask(taskId) {
        return new Promise((resolve, reject) => {
            let attempts = 0
            let timeoutId = null

            const clear = () => {
                if (timeoutId != null) {
                    clearTimeout(timeoutId)
                    timeoutId = null
                }
                this.pendingTasks.delete(taskId)
            }

            const scheduleNext = () => {
                timeoutId = setTimeout(run, this.pollingInterval)
                this.pendingTasks.set(taskId, timeoutId)
            }

            const run = async () => {
                attempts++
                try {
                    const result = await getAiTaskStatus(taskId)

                    if (result.code === 200) {
                        const { status, reply, errorMessage } = result.data

                        if (status === 'SUCCEEDED') {
                            clear()
                            resolve(reply)
                            return
                        }
                        if (status === 'FAILED' || status === 'DEAD') {
                            clear()
                            reject(new Error(errorMessage || '处理失败'))
                            return
                        }
                    } else if (result.code === 404) {
                        clear()
                        reject(new Error(result.message || '任务不存在或已过期'))
                        return
                    } else if (result.code === 401) {
                        clear()
                        reject(new Error(result.message || '请先登录'))
                        return
                    }

                    if (attempts >= this.maxPollingAttempts) {
                        clear()
                        reject(new Error('请求超时，请稍后重试'))
                        return
                    }
                    scheduleNext()
                } catch (error) {
                    console.error('轮询失败:', error)
                    if (attempts >= this.maxPollingAttempts) {
                        clear()
                        reject(error instanceof Error ? error : new Error('请求超时，请稍后重试'))
                        return
                    }
                    scheduleNext()
                }
            }

            run()
        })
    }

    /**
     * 取消轮询（可选）
     */
    cancelPolling(taskId) {
        const timer = this.pendingTasks.get(taskId)
        if (timer) {
            clearInterval(timer)
            this.pendingTasks.delete(taskId)
        }
    }
}

// 导出单例
export const aiAsyncService = new AiAsyncService()