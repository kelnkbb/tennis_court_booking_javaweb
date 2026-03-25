<!-- src/layout/MainLayout.vue -->
<template>
  <div class="layout">
    <!-- 侧边栏菜单 -->
    <aside class="sidebar">
      <div class="logo">
        <h2>网球场地预约管理系统</h2>
      </div>
      <nav class="nav">
        <!-- 管理员菜单：保持原界面 -->
        <template v-if="isAdmin">
          <router-link to="/home" class="nav-item" active-class="active">
            <span class="icon">🏠</span>
            首页
          </router-link>
          <router-link to="/courts" class="nav-item" active-class="active">
            <span class="icon">🎾</span>
            场地管理
          </router-link>
          <router-link to="/bookings" class="nav-item" active-class="active">
            <span class="icon">📅</span>
            预约管理
          </router-link>
          <router-link to="/users" class="nav-item" active-class="active">
            <span class="icon">👥</span>
            用户管理
          </router-link>
          <div class="nav-divider"></div>
          <div class="nav-title">统计报表</div>
          <router-link to="/stats/user" class="nav-item" active-class="active">
            <span class="icon">📊</span>
            用户统计
          </router-link>
          <router-link to="/stats/court" class="nav-item" active-class="active">
            <span class="icon">📈</span>
            场地统计
          </router-link>
          <router-link to="/admin/coupons" class="nav-item" active-class="active">
            <span class="icon">🎫</span>
            秒杀券管理
          </router-link>
          <router-link to="/coupons" class="nav-item" active-class="active">
            <span class="icon">🎯</span>
            优惠券秒杀
          </router-link>
        </template>

        <!-- 普通用户菜单：首页、我的预订、场地列表 -->
        <template v-else>
          <router-link to="/home" class="nav-item" active-class="active">
            <span class="icon">🏠</span>
            首页
          </router-link>
          <router-link to="/my-bookings" class="nav-item" active-class="active">
            <span class="icon">📖</span>
            我的预订
          </router-link>
          <router-link to="/courts" class="nav-item" active-class="active">
            <span class="icon">🎾</span>
            场地列表
          </router-link>
          <router-link to="/coupons" class="nav-item" active-class="active">
            <span class="icon">🎫</span>
            优惠券秒杀
          </router-link>
        </template>
      </nav>
    </aside>

    <!-- 主内容区 -->
    <main class="main">
      <!-- 头部 -->
      <header class="header">
        <div class="header-left">
          <button class="menu-btn" @click="toggleSidebar">☰</button>
          <span class="page-title">{{ currentRoute }}</span>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              {{ currentUser?.realName || currentUser?.username || '管理员' }}
              <el-icon class="el-icon--right"><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><user /></el-icon>个人信息
                </el-dropdown-item>
                <el-dropdown-item command="changePassword">
                  <el-icon><lock /></el-icon>修改密码
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><switch-button /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 内容区 -->
      <div class="content">
        <router-view />
      </div>
    </main>

    <!-- 修改密码对话框 -->
    <el-dialog
        v-model="passwordDialogVisible"
        title="修改密码"
        width="400px"
        destroy-on-close
        @close="handlePasswordDialogClose"
    >
      <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-width="100px"
          class="password-form"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
              v-model="passwordForm.oldPassword"
              type="password"
              placeholder="请输入原密码"
              show-password
          />
        </el-form-item>

        <el-form-item label="新密码" prop="newPassword">
          <el-input
              v-model="passwordForm.newPassword"
              type="password"
              placeholder="请输入新密码"
              show-password
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              show-password
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="passwordDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleChangePassword" :loading="passwordSubmitting">
            {{ passwordSubmitting ? '提交中...' : '确认' }}
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 个人信息对话框 -->
    <el-dialog
        v-model="profileDialogVisible"
        title="个人信息"
        width="500px"
        destroy-on-close
    >
      <el-descriptions :column="1" border>
        <el-descriptions-item label="用户名">{{ currentUser?.username }}</el-descriptions-item>
        <el-descriptions-item label="真实姓名">{{ currentUser?.realName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentUser?.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ currentUser?.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag :type="currentUser?.role === 2 ? 'danger' : 'primary'">
            {{ currentUser?.role === 2 ? '管理员' : '普通用户' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentUser?.status === 1 ? 'success' : 'info'">
            {{ currentUser?.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ formatDate(currentUser?.createTime) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="profileDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 在线客服：AI + 人工 同一悬浮窗，分段切换 -->
    <div class="ai-chat-widget" aria-live="polite">
      <transition name="ai-panel">
        <div v-show="aiPanelOpen" class="ai-chat-panel">
          <div class="ai-chat-header">
            <div class="ai-chat-header-row">
              <span class="ai-chat-title">在线客服</span>
              <div class="ai-chat-header-actions">
                <el-button text circle @click="aiPanelOpen = false" title="关闭">
                  <el-icon><close /></el-icon>
                </el-button>
              </div>
            </div>
            <el-radio-group v-model="supportTab" size="small" class="support-tabs">
              <el-radio-button label="ai">AI 助手</el-radio-button>
              <el-radio-button label="human">人工客服</el-radio-button>
            </el-radio-group>
          </div>
          <!-- AI -->
          <div v-show="supportTab === 'ai'" ref="aiMessagesRef" class="ai-chat-messages">
            <div
              v-for="(m, i) in aiMessages"
              :key="i"
              class="ai-msg"
              :class="m.role === 'user' ? 'ai-msg-user' : 'ai-msg-assistant'"
            >
              <span class="ai-msg-label">{{ m.role === 'user' ? '我' : '客服' }}</span>
              <div
                v-if="m.role === 'user'"
                class="ai-msg-bubble ai-msg-plain"
              >{{ m.content }}</div>
              <div
                v-else
                class="ai-msg-bubble ai-msg-md"
                v-html="renderAssistantMarkdown(m.content)"
              />
              <div
                v-if="m.role === 'assistant' && m.quickOptions?.length"
                class="ai-quick-options"
              >
                <div class="ai-quick-chips">
                  <el-button
                    v-for="(q, qi) in m.quickOptions"
                    :key="qi"
                    size="small"
                    round
                    plain
                    type="primary"
                    :disabled="aiAwaitingReply"
                    @click="sendAiQuick(q)"
                  >
                    {{ q }}
                  </el-button>
                </div>
              </div>
            </div>
            <div v-if="aiAwaitingReply" class="ai-msg ai-msg-assistant ai-msg-pending">
              <span class="ai-msg-label">客服</span>
              <div class="ai-msg-bubble">正在思考…</div>
            </div>
          </div>
          <div v-show="supportTab === 'ai'" class="ai-chat-footer">
            <el-input
              v-model="aiInput"
              type="textarea"
              :rows="2"
              placeholder="输入问题，Enter 发送（Shift+Enter 换行）"
              resize="none"
              :disabled="aiInputLocked"
              @keydown.enter.exact.prevent="submitAiFromInput"
            />
            <el-button
              type="primary"
              class="ai-send-btn"
              :loading="aiAwaitingReply"
              :disabled="!aiInput.trim() || aiAwaitingReply"
              @click="submitAiFromInput"
            >
              发送
            </el-button>
            <div class="ai-mode-switch" v-if="showModeSwitch">
              <el-tag 
                size="small" 
                :type="useAsyncMode ? 'success' : 'info'"
                style="cursor: pointer"
                @click="toggleAsyncMode"
              >
                {{ useAsyncMode ? '⚡异步模式' : '🔄同步模式' }}
              </el-tag>
            </div>
          </div>
          <!-- 人工客服 -->
          <div v-show="supportTab === 'human'" class="human-panel-body">
            <div v-if="isAdmin" class="human-admin-bar">
              <el-select
                v-model="adminCsConversationId"
                placeholder="选择进行中的会话"
                filterable
                clearable
                style="width: 100%"
                @change="onAdminCsConversationChange"
              >
                <el-option
                  v-for="c in adminCsConversations"
                  :key="c.id"
                  :label="`#${c.id} · 用户 ${c.userId}`"
                  :value="c.id"
                />
              </el-select>
              <el-button size="small" @click="loadAdminCsList">刷新</el-button>
            </div>
            <div ref="humanMessagesRef" class="ai-chat-messages human-chat-messages">
              <div v-if="isAdmin && !adminCsConversations.length" class="human-empty-hint">
                暂无进行中的会话，用户发起咨询后会出现在此处。
              </div>
              <div
                v-for="m in humanMessages"
                :key="m.id"
                class="ai-msg"
                :class="isSelfHuman(m) ? 'ai-msg-user' : 'ai-msg-assistant'"
              >
                <span class="ai-msg-label">{{ humanMsgLabel(m) }}</span>
                <div class="ai-msg-bubble ai-msg-plain">{{ m.content }}</div>
              </div>
            </div>
            <div class="ai-chat-footer">
              <div v-if="humanWsHint" class="human-ws-hint">
                {{ humanWsHint }}
              </div>
              <el-input
                v-model="humanInput"
                type="textarea"
                :rows="2"
                placeholder="输入消息，Enter 发送（Shift+Enter 换行）"
                resize="none"
                :disabled="!canSendHuman"
                @keydown.enter.exact.prevent="submitHumanFromInput"
              />
              <el-button
                type="primary"
                class="ai-send-btn"
                :disabled="!canSendHuman || !humanInput.trim()"
                @click="submitHumanFromInput"
              >
                发送
              </el-button>
            </div>
          </div>
        </div>
      </transition>
      <button
        type="button"
        class="ai-chat-fab"
        :class="{ 'ai-chat-fab--open': aiPanelOpen }"
        :aria-label="aiPanelOpen ? '收起在线客服' : '打开在线客服'"
        @click="toggleAiPanel"
      >
        <el-icon :size="aiPanelOpen ? 22 : 26"><chat-dot-round /></el-icon>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { h } from 'vue'
import { ArrowDown, User, Lock, SwitchButton, ChatDotRound, Close } from '@element-plus/icons-vue'
import { ElNotification, ElButton } from 'element-plus'
import { logout, changePassword, getCurrentUser } from '@/api/auth'
import { aiSimpleChat, aiAsyncChat, getAiTaskStatus } from '@/api/ai'
import { renderAssistantMarkdown } from '@/utils/renderMarkdown'
import { connectBookingNotifications, disconnectBookingNotifications } from '@/utils/bookingNotificationWs'
import { connectCsChat, disconnectCsChat, sendCsChatJson } from '@/utils/csChatWs'
import {
  getMyCsMessages,
  listAdminCsConversations,
  getAdminCsMessages
} from '@/api/cs'

const route = useRoute()
const router = useRouter()
const currentUser = ref(null)

// AI 异步服务配置
const useAsyncMode = ref(true)  // true: 异步模式, false: 同步模式
const showModeSwitch = ref(false)  // 是否显示模式切换（开发调试用，生产可设为 false）
const pollingInterval = 2000  // 轮询间隔（毫秒）
const maxPollingAttempts = 30  // 最大轮询次数

const isAdmin = computed(() => {
  const r = currentUser.value?.role
  return r != null && Number(r) === 2
})

// 密码对话框
const passwordDialogVisible = ref(false)
const passwordSubmitting = ref(false)
const passwordFormRef = ref(null)
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 个人信息对话框
const profileDialogVisible = ref(false)

// 在线客服：AI / 人工 分段
const supportTab = ref('ai')

// AI 客服悬浮窗
const aiPanelOpen = ref(false)
const aiInput = ref('')
/** 等待本轮客服回复（含异步轮询）；用于「正在思考」与发送按钮 loading */
const aiAwaitingReply = ref(false)
/** 仅同步模式在等待时锁定输入框；异步模式可继续输入草稿，仅禁止重复发送 */
const aiInputLocked = computed(() => !useAsyncMode.value && aiAwaitingReply.value)
const aiMessagesRef = ref(null)
const aiMessages = ref([
  {
    role: 'assistant',
    content: '您好，我是网球场地预约助手。需要时可直接点下方快捷操作，或输入您的问题。',
    quickOptions: ['有哪些网球场', '查看我的预约', '明天怎么预约']
  }
])

// —— 人工客服（WebSocket + REST 历史）——
const humanMessagesRef = ref(null)
const humanMessages = ref([])
const humanInput = ref('')
/** 服务端 joined 之前为 false；之后为 true */
const humanWsConnected = ref(false)
/** WebSocket 已 onopen（响应式，勿用 ws.readyState 做 computed） */
const humanWsTransportOpen = ref(false)
const adminCsConversations = ref([])
const adminCsConversationId = ref(null)

const mapApiRow = (m) => ({
  id: m.id,
  senderType: m.senderType,
  senderUserId: m.senderUserId,
  content: m.content
})

const scrollHumanToBottom = () => {
  nextTick(() => {
    const el = humanMessagesRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

const humanMsgLabel = (m) => {
  if (isAdmin.value) {
    return m.senderType === 2 ? '我' : '用户'
  }
  return m.senderType === 1 ? '我' : '客服'
}

const isSelfHuman = (m) => {
  if (isAdmin.value) return m.senderType === 2
  return m.senderType === 1
}

const canSendHuman = computed(() => {
  if (!localStorage.getItem('token')) return false
  if (isAdmin.value && !adminCsConversationId.value) return false
  return humanWsTransportOpen.value && humanWsConnected.value
})

const humanWsHint = computed(() => {
  if (!localStorage.getItem('token')) return '请先登录后使用人工客服。'
  if (isAdmin.value && !adminCsConversationId.value && adminCsConversations.value.length > 0) {
    return '请选择要接入的会话。'
  }
  if (isAdmin.value && !adminCsConversations.value.length) return ''
  if (!humanWsTransportOpen.value) return '正在连接服务器…'
  if (!humanWsConnected.value) return '正在加入会话…'
  return ''
})

const handleHumanWsJson = (data) => {
  if (!data) return
  if (data.type === 'joined') {
    humanWsConnected.value = true
    scrollHumanToBottom()
    return
  }
  if (data.type === 'message') {
    humanMessages.value.push({
      id: data.id,
      senderType: data.senderType,
      senderUserId: data.senderUserId,
      content: data.content
    })
    scrollHumanToBottom()
    return
  }
  if (data.type === 'error') {
    ElMessage.error(data.message || '错误')
  }
}

const loadAdminCsList = async () => {
  try {
    const r = await listAdminCsConversations(50)
    if (r.code === 200 && Array.isArray(r.data)) {
      adminCsConversations.value = r.data
    }
  } catch (e) {
    console.error(e)
  }
}

const connectAdminHuman = async (id) => {
  if (!id) {
    disconnectCsChat()
    humanMessages.value = []
    humanWsConnected.value = false
    return
  }
  try {
    const r = await getAdminCsMessages(id, 200)
    if (r.code === 200 && Array.isArray(r.data)) {
      humanMessages.value = r.data.map(mapApiRow)
    } else {
      humanMessages.value = []
    }
  } catch (e) {
    console.error(e)
    humanMessages.value = []
  }
  humanWsConnected.value = false
  humanWsTransportOpen.value = false
  disconnectCsChat()
  connectCsChat({
    conversationId: id,
    onJson: handleHumanWsJson,
    onOpen: () => {
      humanWsTransportOpen.value = true
    },
    onClose: () => {
      humanWsTransportOpen.value = false
      humanWsConnected.value = false
    }
  })
}

const onAdminCsConversationChange = async (id) => {
  if (!aiPanelOpen.value || supportTab.value !== 'human') return
  await connectAdminHuman(id)
}

const enterHumanTab = async () => {
  if (!localStorage.getItem('token')) {
    ElMessage.warning('请先登录后再使用人工客服')
    return
  }
  humanWsConnected.value = false
  humanWsTransportOpen.value = false
  disconnectCsChat()

  if (isAdmin.value) {
    await loadAdminCsList()
    if (!adminCsConversations.value.length) {
      humanMessages.value = []
      adminCsConversationId.value = null
      return
    }
    if (adminCsConversationId.value == null) {
      adminCsConversationId.value = adminCsConversations.value[0].id
    }
    await connectAdminHuman(adminCsConversationId.value)
    return
  }

  try {
    const r = await getMyCsMessages(100)
    if (r.code === 200 && Array.isArray(r.data)) {
      humanMessages.value = r.data.map(mapApiRow)
    } else {
      humanMessages.value = []
    }
  } catch (e) {
    console.error(e)
    humanMessages.value = []
  }
  connectCsChat({
    conversationId: null,
    onJson: handleHumanWsJson,
    onOpen: () => {
      humanWsTransportOpen.value = true
    },
    onClose: () => {
      humanWsTransportOpen.value = false
      humanWsConnected.value = false
    }
  })
}

const submitHumanFromInput = () => {
  const t = humanInput.value.trim()
  if (!t || !canSendHuman.value) return
  humanInput.value = ''
  sendCsChatJson({ type: 'chat', content: t })
}

watch(
  () => [supportTab.value, aiPanelOpen.value],
  async ([tab, open]) => {
    if (!open || tab !== 'human') {
      disconnectCsChat()
      humanWsConnected.value = false
      humanWsTransportOpen.value = false
      return
    }
    await enterHumanTab()
  }
)

watch(humanMessages, () => scrollHumanToBottom(), { deep: true })

// 侧边栏折叠状态（移动端）
const sidebarOpen = ref(false)

const scrollAiToBottom = () => {
  nextTick(() => {
    const el = aiMessagesRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

const toggleAiPanel = () => {
  aiPanelOpen.value = !aiPanelOpen.value
  if (aiPanelOpen.value) {
    nextTick(() => {
      if (supportTab.value === 'ai') scrollAiToBottom()
      else scrollHumanToBottom()
    })
  }
}

const toggleSidebar = () => {
  sidebarOpen.value = !sidebarOpen.value
  const sidebar = document.querySelector('.sidebar')
  if (sidebar) {
    if (sidebarOpen.value) {
      sidebar.classList.add('open')
    } else {
      sidebar.classList.remove('open')
    }
  }
}

watch(aiMessages, () => scrollAiToBottom(), { deep: true })
watch(aiAwaitingReply, (v) => {
  if (v) scrollAiToBottom()
})

/** 仅使用接口返回的 quickOptions，不再用固定文案兜底，以便随每次客服回复变化 */
const normalizeAiPayload = (data) => {
  if (data == null) {
    return { content: '（无回复内容）', quickOptions: [] }
  }
  if (typeof data === 'string') {
    return { content: data, quickOptions: [] }
  }
  const content = data.content != null ? String(data.content) : '（无回复内容）'
  const raw = data.quickOptions
  const quickOptions = Array.isArray(raw)
    ? raw.map((x) => String(x).trim()).filter(Boolean).slice(0, 5)
    : []
  return { content, quickOptions }
}

/**
 * 异步模式：轮询任务状态（串行：上一请求结束后再间隔 pollingInterval，避免 setInterval 与未完成请求重叠）
 */
const pollTaskStatus = (taskId) => {
  return new Promise((resolve, reject) => {
    let attempts = 0
    let timeoutId = null

    const clear = () => {
      if (timeoutId != null) {
        clearTimeout(timeoutId)
        timeoutId = null
      }
    }

    const scheduleNext = () => {
      timeoutId = setTimeout(run, pollingInterval)
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

        if (attempts >= maxPollingAttempts) {
          clear()
          reject(new Error('请求超时，请稍后重试'))
          return
        }
        scheduleNext()
      } catch (error) {
        console.error('轮询失败:', error)
        if (attempts >= maxPollingAttempts) {
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
 * 异步模式：发送消息
 */
const sendAsyncMessage = async (text) => {
  // 1. 提交异步任务
  const submitResult = await aiAsyncChat(text)
  
  if (submitResult.code !== 200) {
    throw new Error(submitResult.message || '提交失败')
  }
  
  const { taskId } = submitResult.data
  
  // 2. 轮询获取结果
  const reply = await pollTaskStatus(taskId)
  return { code: 200, data: reply }
}

/**
 * 同步模式：发送消息（原有逻辑）
 */
const sendSyncMessage = async (text) => {
  return await aiSimpleChat(text)
}

const sendAiQuick = (label) => {
  sendAiText(String(label || '').trim())
}

const submitAiFromInput = () => {
  const text = aiInput.value.trim()
  if (!text || aiAwaitingReply.value) return
  aiInput.value = ''
  sendAiText(text)
}

const sendAiText = async (text) => {
  if (!text || aiAwaitingReply.value) return

  aiMessages.value.push({ role: 'user', content: text })
  aiAwaitingReply.value = true
  scrollAiToBottom()

  try {
    let result

    if (useAsyncMode.value) {
      console.log('🚀 使用异步模式发送消息')
      result = await sendAsyncMessage(text)
    } else {
      console.log('🔄 使用同步模式发送消息')
      result = await sendSyncMessage(text)
    }

    if (result.code === 200) {
      const { content, quickOptions } = normalizeAiPayload(result.data)
      aiMessages.value.push({
        role: 'assistant',
        content,
        quickOptions
      })
    } else {
      ElMessage.error(result.message || '请求失败')
      aiMessages.value.push({
        role: 'assistant',
        content: `抱歉，${result.message || '暂时无法回答'}。`,
        quickOptions: []
      })
    }
  } catch (e) {
    console.error('AI 客服:', e)
    const errorMsg = e.message || '网络或服务异常，请稍后再试。'
    ElMessage.error(errorMsg)
    aiMessages.value.push({
      role: 'assistant',
      content: errorMsg,
      quickOptions: []
    })
  } finally {
    aiAwaitingReply.value = false
    scrollAiToBottom()
  }
}

// 切换同步/异步模式（开发调试用）
const toggleAsyncMode = () => {
  useAsyncMode.value = !useAsyncMode.value
  ElMessage.info(`已切换到${useAsyncMode.value ? '异步模式' : '同步模式'}${useAsyncMode.value ? '（推荐，页面不卡顿）' : '（传统模式，可能卡顿）'}`)
}

// 密码验证规则
const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.value.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 当前路由名称
const currentRoute = computed(() => {
  const routeNames = {
    'Home': '首页',
    'CourtManagement': '场地管理',
    'Bookings': '预约管理',
    'MyBookings': '我的预订',
    'UserManagement': '用户管理',
    'UserStats': '用户统计',
    'CourtStats': '场地统计',
    'AdminCouponActivities': '秒杀券管理',
    'CouponSeckill': '优惠券秒杀'
  }
  return routeNames[route.name] || '网球场地管理系统'
})

// 获取当前用户信息
const loadCurrentUser = async () => {
  try {
    // 先从 localStorage 获取
    const userStr = localStorage.getItem('user')
    if (userStr) {
      currentUser.value = JSON.parse(userStr)
    }

    // 再从后端获取最新信息
    const result = await getCurrentUser()
    if (result.code === 200) {
      currentUser.value = result.data
      localStorage.setItem('user', JSON.stringify(result.data))
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
}

// 处理下拉菜单命令
const handleCommand = (command) => {
  switch (command) {
    case 'profile':
      showProfile()
      break
    case 'changePassword':
      showChangePassword()
      break
    case 'logout':
      handleLogout()
      break
  }
}

// 显示个人信息
const showProfile = () => {
  profileDialogVisible.value = true
}

// 显示修改密码对话框
const showChangePassword = () => {
  passwordForm.value = {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  }
  passwordDialogVisible.value = true
}

// 修改密码
const handleChangePassword = async () => {
  if (!passwordFormRef.value) return

  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        passwordSubmitting.value = true

        const result = await changePassword({
          oldPassword: passwordForm.value.oldPassword,
          newPassword: passwordForm.value.newPassword
        })

        if (result.code === 200) {
          ElMessage.success('密码修改成功，请重新登录')
          passwordDialogVisible.value = false
          // 退出登录
          await handleLogout(true) // true 表示不弹出确认框
        } else {
          ElMessage.error(result.message || '修改失败')
        }
      } catch (error) {
        console.error('修改密码失败:', error)
        ElMessage.error(error.message || '修改失败')
      } finally {
        passwordSubmitting.value = false
      }
    }
  })
}

// 关闭密码对话框
const handlePasswordDialogClose = () => {
  passwordForm.value = {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  }
  passwordFormRef.value?.clearValidate()
}

// 退出登录
const handleLogout = async (silent = false) => {
  const doLogout = async () => {
    try {
      // 调用退出登录接口
      await logout()

      // 清除本地存储
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      disconnectBookingNotifications()
      disconnectCsChat()

      ElMessage.success('已退出登录')
      router.push('/login')
    } catch (error) {
      console.error('退出失败:', error)
      // 即使接口调用失败，也清除本地存储并跳转
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      disconnectBookingNotifications()
      disconnectCsChat()
      router.push('/login')
    }
  }

  if (silent) {
    await doLogout()
  } else {
    ElMessageBox.confirm(
        '确定要退出登录吗？',
        '退出登录',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'info'
        }
    ).then(() => {
      doLogout()
    }).catch(() => {})
  }
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-'
  try {
    const date = new Date(dateString)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    }).replace(/\//g, '-')
  } catch {
    return dateString
  }
}

// 监听路由变化，关闭移动端侧边栏
watch(() => route.path, () => {
  if (window.innerWidth <= 768) {
    sidebarOpen.value = false
    const sidebar = document.querySelector('.sidebar')
    if (sidebar) {
      sidebar.classList.remove('open')
    }
  }
})

const handleBookingWsMessage = (data) => {
  const t = data?.type
  if (t === 'COUPON_ACTIVITY_PUBLISHED') {
    ElNotification({
      title: data?.title || '新秒杀优惠券',
      message: h('div', { class: 'ws-coupon-notify' }, [
        h('p', { style: 'margin:0 0 10px;font-size:14px;line-height:1.5;color:#606266' }, data?.message || ''),
        h(ElButton, {
          type: 'primary',
          size: 'small',
          onClick: () => {
            router.push('/coupons')
          }
        }, () => '去抢购')
      ]),
      duration: 12000,
      type: 'success'
    })
    return
  }
  const title = data?.title || '预约通知'
  const body = data?.message || ''
  ElMessage.success({ message: `${title}${body ? '：' + body : ''}`, duration: 6500, showClose: true })
}

onMounted(() => {
  loadCurrentUser()

  if (localStorage.getItem('token')) {
    connectBookingNotifications(handleBookingWsMessage)
  }

  // 可选：在开发环境显示模式切换（生产环境可注释）
  if (import.meta.env.DEV) {
    showModeSwitch.value = true
  }
})

onUnmounted(() => {
  disconnectBookingNotifications()
  disconnectCsChat()
})
</script>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

.sidebar {
  width: 260px;
  background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 10px rgba(0, 0, 0, 0.1);
  transition: left 0.3s ease;
  z-index: 1000;
}

.logo {
  padding: 24px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: white;
}

.nav {
  flex: 1;
  padding: 20px 0;
  overflow-y: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  color: rgba(255, 255, 255, 0.8);
  text-decoration: none;
  transition: all 0.3s;
  margin: 4px 8px;
  border-radius: 8px;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

.nav-item.active {
  background: rgba(255, 255, 255, 0.2);
  color: white;
  font-weight: 500;
}

.icon {
  margin-right: 12px;
  font-size: 18px;
}

.nav-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.1);
  margin: 16px 20px;
}

.nav-title {
  padding: 8px 20px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  text-transform: uppercase;
  letter-spacing: 1px;
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #f5f7fa;
}

.header {
  height: 60px;
  background: white;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.menu-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #666;
  display: none;
}

.page-title {
  font-size: 18px;
  font-weight: 500;
  color: #333;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
}

.user-info {
  color: #666;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 20px;
  transition: all 0.3s;
}

.user-info:hover {
  background: #f5f5f5;
}

.content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

/* 密码表单样式 */
.password-form {
  padding: 20px 20px 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

:deep(.el-dialog) {
  border-radius: 12px;
}

:deep(.el-dialog__header) {
  padding: 20px 20px 10px;
  margin: 0;
  border-bottom: 1px solid #f0f2f5;
}

:deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

:deep(.el-dialog__body) {
  padding: 20px;
}

:deep(.el-dialog__footer) {
  padding: 10px 20px 20px;
  border-top: 1px solid #f0f2f5;
}

/* 响应式 */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: -260px;
    top: 0;
    bottom: 0;
    z-index: 1000;
    transition: left 0.3s;
  }

  .sidebar.open {
    left: 0;
  }

  .menu-btn {
    display: block;
  }
}

/* —— AI 客服悬浮组件 —— */
.ai-chat-widget {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 2000;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
  pointer-events: none;
}

.ai-chat-widget > * {
  pointer-events: auto;
}

.ai-chat-fab {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  color: #fff;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s, box-shadow 0.2s;
}

.ai-chat-fab:hover {
  transform: scale(1.06);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.55);
}

.ai-chat-fab--open {
  width: 48px;
  height: 48px;
  opacity: 0.95;
}

.ai-panel-enter-active,
.ai-panel-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.ai-panel-enter-from,
.ai-panel-leave-to {
  opacity: 0;
  transform: translateY(12px) scale(0.98);
}

.ai-chat-panel {
  width: min(100vw - 48px, 760px);
  height: min(100vh - 80px, 1040px);
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #e8e8ef;
}

.ai-chat-header {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
  padding: 12px 12px 10px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.ai-chat-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ai-chat-title {
  font-weight: 600;
  font-size: 16px;
}

.support-tabs {
  width: 100%;
  display: flex;
  justify-content: center;
}

.support-tabs :deep(.el-radio-button__inner) {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.35);
  color: #fff;
}

.support-tabs :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: rgba(255, 255, 255, 0.95);
  color: #5e4bcf;
  border-color: rgba(255, 255, 255, 0.35);
  box-shadow: none;
}

.human-panel-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.human-admin-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #eef0f6;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.human-chat-messages {
  flex: 1;
  min-height: 0;
}

.human-empty-hint,
.human-ws-hint {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.human-ws-hint {
  margin-bottom: 4px;
}

.ai-chat-header-actions .el-button {
  color: #fff;
}

.ai-chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f7f8fc;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ai-msg {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 100%;
}

.ai-msg-user {
  align-items: flex-end;
}

.ai-msg-assistant {
  align-items: flex-start;
}

.ai-msg-label {
  font-size: 11px;
  color: #909399;
  padding: 0 4px;
}

.ai-msg-bubble {
  padding: 10px 12px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
}

.ai-msg-plain {
  white-space: pre-wrap;
}

.ai-msg-md {
  white-space: normal;
}

.ai-msg-md :deep(p) {
  margin: 0.35em 0;
}

.ai-msg-md :deep(p:first-child) {
  margin-top: 0;
}

.ai-msg-md :deep(p:last-child) {
  margin-bottom: 0;
}

.ai-msg-md :deep(ul),
.ai-msg-md :deep(ol) {
  margin: 0.35em 0;
  padding-left: 1.35em;
}

.ai-msg-md :deep(li) {
  margin: 0.15em 0;
}

.ai-msg-md :deep(strong) {
  font-weight: 600;
}

.ai-msg-md :deep(em) {
  font-style: italic;
}

.ai-msg-md :deep(a) {
  color: #667eea;
  word-break: break-all;
}

.ai-msg-md :deep(code) {
  font-size: 0.9em;
  padding: 0.12em 0.35em;
  background: rgba(0, 0, 0, 0.06);
  border-radius: 4px;
}

.ai-msg-md :deep(pre) {
  overflow-x: auto;
  margin: 0.5em 0;
  padding: 8px 10px;
  background: #f5f6f8;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.45;
}

.ai-msg-md :deep(pre code) {
  padding: 0;
  background: none;
}

.ai-msg-md :deep(h1),
.ai-msg-md :deep(h2),
.ai-msg-md :deep(h3) {
  font-size: 1em;
  font-weight: 600;
  margin: 0.5em 0 0.25em;
}

.ai-msg-md :deep(h1:first-child),
.ai-msg-md :deep(h2:first-child),
.ai-msg-md :deep(h3:first-child) {
  margin-top: 0;
}

.ai-msg-md :deep(blockquote) {
  margin: 0.35em 0;
  padding-left: 0.75em;
  border-left: 3px solid #dcdfe6;
  color: #606266;
}

.ai-msg-md :deep(hr) {
  margin: 0.6em 0;
  border: none;
  border-top: 1px solid #ebeef5;
}

.ai-msg-user .ai-msg-bubble {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.ai-msg-assistant .ai-msg-bubble {
  background: #fff;
  color: #303133;
  border: 1px solid #ebeef5;
  border-bottom-left-radius: 4px;
}

.ai-msg-pending .ai-msg-bubble {
  color: #909399;
  font-style: italic;
}

.ai-quick-options {
  margin-top: 6px;
  max-width: 100%;
  padding-left: 4px;
}

.ai-quick-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.ai-quick-chips .el-button {
  margin: 0;
  font-size: 12px;
}

.ai-chat-footer {
  padding: 12px;
  background: #fff;
  border-top: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ai-send-btn {
  align-self: flex-end;
}

.ai-mode-switch {
  align-self: flex-end;
  margin-top: 4px;
}
</style>