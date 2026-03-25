import request from '@/utils/request'

/** 当前用户进行中的会话（无则 data 为 null） */
export function getMyCsConversation() {
  return request({ url: '/cs/me/conversation', method: 'get' })
}

/** 当前用户进行中会话的消息列表 */
export function getMyCsMessages(limit = 100) {
  return request({ url: '/cs/me/messages', method: 'get', params: { limit } })
}

/** 管理员：进行中的会话列表 */
export function listAdminCsConversations(limit = 50) {
  return request({ url: '/cs/admin/conversations', method: 'get', params: { limit } })
}

/** 管理员：某会话消息 */
export function getAdminCsMessages(conversationId, limit = 200) {
  return request({
    url: `/cs/admin/conversations/${conversationId}/messages`,
    method: 'get',
    params: { limit }
  })
}
