/**
 * 人工客服 WebSocket（与后端 /api/ws/cs、JwtHandshakeInterceptor 一致）
 * 用户：仅 query token；管理员：另加 conversationId。
 *
 * connectionGen：每次发起新连接自增，用于忽略「旧连接」异步 onclose，避免误清 UI 状态。
 */
let ws = null
let connectionGen = 0

function buildCsWsUrl(token, conversationId) {
  let q = `token=${encodeURIComponent(token)}`
  if (conversationId != null && conversationId !== '') {
    q += `&conversationId=${encodeURIComponent(String(conversationId))}`
  }
  if (import.meta.env.DEV) {
    const port = import.meta.env.VITE_BACKEND_PORT || '8080'
    return `ws://127.0.0.1:${port}/api/ws/cs?${q}`
  }
  const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${proto}//${window.location.host}/api/ws/cs?${q}`
}

export function isCsChatConnected() {
  return ws != null && ws.readyState === WebSocket.OPEN
}

/**
 * @param {object} opts
 * @param {string|number|null} [opts.conversationId] 管理员接入指定会话时必填
 * @param {function(object): void} [opts.onJson] 解析后的 JSON 消息
 * @param {function(): void} [opts.onOpen] 传输层已连接（供 Vue 更新响应式状态，勿依赖 ws.readyState 做 computed）
 * @param {function(): void} [opts.onClose] 仅当前连接被关闭且非被新连接顶替时调用
 */
export function connectCsChat(opts = {}) {
  disconnectCsChat()
  const token = localStorage.getItem('token')
  if (!token) {
    opts.onClose?.()
    return
  }
  const myGen = ++connectionGen
  const url = buildCsWsUrl(token, opts.conversationId)
  if (import.meta.env.DEV) {
    console.debug('[cs WS] connect', url.replace(/token=[^&]+/, 'token=***'))
  }
  ws = new WebSocket(url)

  ws.onopen = () => {
    if (myGen !== connectionGen) return
    opts.onOpen?.()
  }

  ws.onmessage = (ev) => {
    if (myGen !== connectionGen) return
    if (ev.data === 'pong') return
    try {
      const data = JSON.parse(ev.data)
      opts.onJson?.(data)
    } catch (e) {
      console.warn('[cs WS] parse error', ev.data)
    }
  }

  ws.onclose = () => {
    if (myGen !== connectionGen) return
    ws = null
    opts.onClose?.()
  }

  ws.onerror = (ev) => {
    if (import.meta.env.DEV) {
      console.warn('[cs WS] error', ev)
    }
  }
}

export function sendCsChatJson(obj) {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(obj))
  }
}

/** 保活：与后端 handleTextMessage 中 ping/pong 一致 */
export function sendCsPing() {
  if (isCsChatConnected()) {
    ws.send('ping')
  }
}

export function disconnectCsChat() {
  if (ws) {
    try {
      ws.close()
    } catch (_) {
      /* ignore */
    }
    ws = null
  }
}
