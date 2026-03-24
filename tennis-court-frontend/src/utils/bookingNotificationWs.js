/**
 * 预约审核 / 付款确认 WebSocket（JWT 通过 query token，与后端 JwtHandshakeInterceptor 一致）
 *
 * 开发环境直连后端 8080：Vite 对 /api 的 HTTP 代理正常，但 WebSocket 升级经 5173 代理常失败，故不用 window.location.host。
 * 生产环境（Vercel）：使用与 HTTP 相同的 VITE_API_BASE_URL 推导 wss:// 后端地址。
 */
import { getApiBaseUrl } from '@/config/api.js'

let ws = null
let reconnectTimer = null

function buildNotificationWsUrl(token) {
  const q = `token=${encodeURIComponent(token)}`
  if (import.meta.env.DEV) {
    const port = import.meta.env.VITE_BACKEND_PORT || '8080'
    return `ws://127.0.0.1:${port}/api/ws/notifications?${q}`
  }
  const base = getApiBaseUrl()
  if (base !== '/api') {
    try {
      const withApi = base.endsWith('/api') ? base : `${base}/api`
      const u = new URL(withApi)
      const wsProto = u.protocol === 'https:' ? 'wss:' : 'ws:'
      return `${wsProto}//${u.host}/api/ws/notifications?${q}`
    } catch (e) {
      console.warn('[booking WS] invalid API base URL', e)
    }
  }
  const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${proto}//${window.location.host}/api/ws/notifications?${q}`
}

export function connectBookingNotifications(onJsonMessage) {
  disconnectBookingNotifications()
  const token = localStorage.getItem('token')
  if (!token) return

  const url = buildNotificationWsUrl(token)
  if (import.meta.env.DEV) {
    console.debug('[booking WS] connect', url.replace(/token=[^&]+/, 'token=***'))
  }
  ws = new WebSocket(url)

  ws.onmessage = (ev) => {
    if (ev.data === 'pong') return
    try {
      const data = JSON.parse(ev.data)
      onJsonMessage?.(data)
    } catch (e) {
      console.warn('[booking WS] parse error', ev.data)
    }
  }

  ws.onclose = () => {
    ws = null
    const t = localStorage.getItem('token')
    if (t) {
      reconnectTimer = window.setTimeout(() => connectBookingNotifications(onJsonMessage), 5000)
    }
  }

  ws.onerror = () => {
    /* 关闭后会触发 onclose 重连 */
  }
}

export function disconnectBookingNotifications() {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  if (ws) {
    try {
      ws.close()
    } catch (_) {
      /* ignore */
    }
    ws = null
  }
}
