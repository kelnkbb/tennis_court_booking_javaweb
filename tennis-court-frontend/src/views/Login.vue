<!-- src/views/Login.vue -->
<template>
  <div class="login-container">
    <div class="bg-blobs" aria-hidden="true">
      <span class="blob blob-1" />
      <span class="blob blob-2" />
      <span class="blob blob-3" />
    </div>

    <div class="shell">
      <div class="brand-panel">
        <div class="brand">
          <div class="brand-badge">Court Booking</div>
          <h1 class="brand-title">运动场馆预约管理系统</h1>
          <p class="brand-subtitle">
            更高效的场地管理与预约体验：管理员一站式运营，您只需轻松选时下单。
          </p>
        </div>

        <div class="feature-list">
          <div class="feature">
            <div class="dot" />
            <div class="feature-text">
              <div class="feature-title">预约规则</div>
              <div class="feature-desc">每日7点开放场地，提前选好时段，静候佳音</div>
            </div>
          </div>
          <div class="feature">
            <div class="dot" />
            <div class="feature-text">
              <div class="feature-title">订单与支付</div>
              <div class="feature-desc">下单流程清晰可视，让您安心</div>
            </div>
          </div>
          <div class="feature">
            <div class="dot" />
            <div class="feature-text">
              <div class="feature-title">一键预订</div>
              <div class="feature-desc">拒绝繁琐流程，预定场地仅需一键</div>
            </div>
          </div>
        </div>

      </div>

      <div class="login-card">
        <div class="login-header">
          <h2>{{ isLogin ? '欢迎回来' : '创建账号' }}</h2>
          <p>{{ isLogin ? '登录后继续管理与预约' : '填写信息完成注册' }}</p>
        </div>

        <div class="mode-tabs">
          <button
            class="tab"
            :class="{ active: isLogin }"
            type="button"
            @click="isLogin || toggleMode()"
          >
            登录
          </button>
          <button
            class="tab"
            :class="{ active: !isLogin }"
            type="button"
            @click="!isLogin || toggleMode()"
          >
            注册
          </button>
        </div>

        <el-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          label-width="0"
          class="login-form"
        >
          <el-form-item prop="username">
            <el-input
              v-model="formData.username"
              placeholder="用户名"
              :prefix-icon="User"
              size="large"
              autocomplete="username"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="formData.password"
              type="password"
              placeholder="密码"
              :prefix-icon="Lock"
              size="large"
              show-password
              autocomplete="current-password"
            />
          </el-form-item>

          <el-form-item v-if="!isLogin" prop="confirmPassword">
            <el-input
              v-model="formData.confirmPassword"
              type="password"
              placeholder="确认密码"
              :prefix-icon="Lock"
              size="large"
              show-password
              autocomplete="new-password"
            />
          </el-form-item>

          <el-form-item v-if="!isLogin" prop="realName">
            <el-input
              v-model="formData.realName"
              placeholder="真实姓名"
              :prefix-icon="User"
              size="large"
              autocomplete="name"
            />
          </el-form-item>

          <el-form-item v-if="!isLogin" prop="phone">
            <el-input
              v-model="formData.phone"
              placeholder="手机号"
              :prefix-icon="Iphone"
              size="large"
              autocomplete="tel"
            />
          </el-form-item>

          <el-form-item v-if="!isLogin" prop="email">
            <el-input
              v-model="formData.email"
              placeholder="邮箱"
              :prefix-icon="Message"
              size="large"
              autocomplete="email"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              :loading="loading"
              @click="handleSubmit"
              size="large"
              class="submit-btn"
            >
              {{ isLogin ? '登录' : '注册' }}
            </el-button>
          </el-form-item>

          <div class="form-footer">
            <el-link type="primary" @click="toggleMode">
              {{ isLogin ? '没有账号？立即注册' : '已有账号？立即登录' }}
            </el-link>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Iphone, Message } from '@element-plus/icons-vue'
import { login, register } from '@/api/auth'

const router = useRouter()
const isLogin = ref(true)
const loading = ref(false)
const formRef = ref(null)

const formData = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  phone: '',
  email: ''
})

// 验证规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== formData.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

// 切换登录/注册模式
const toggleMode = () => {
  isLogin.value = !isLogin.value
  // 清空表单
  formData.username = ''
  formData.password = ''
  formData.confirmPassword = ''
  formData.realName = ''
  formData.phone = ''
  formData.email = ''
  formRef.value?.clearValidate()
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        if (isLogin.value) {
          // 登录
          const result = await login({
            username: formData.username,
            password: formData.password
          })

          if (result.code === 200) {
            // 保存token和用户信息
            localStorage.setItem('token', result.data.token)
            localStorage.setItem('user', JSON.stringify(result.data.user))

            ElMessage.success('登录成功')

            const user = result.data.user
            // 根据角色跳转到对应首页
            if (user && user.role === 2) {
              // 管理员：进入完整管理界面（当前首页）
              router.push('/home')
            } else {
              // 普通用户：进入简化用户首页（同一路由，不同展示）
              router.push('/home')
            }
          } else {
            ElMessage.error(result.message || '登录失败')
          }
        } else {
          // 注册
          const result = await register({
            username: formData.username,
            password: formData.password,
            confirmPassword: formData.confirmPassword,
            realName: formData.realName,
            phone: formData.phone,
            email: formData.email
          })

          if (result.code === 200) {
            ElMessage.success('注册成功，请登录')
            toggleMode()
          } else {
            ElMessage.error(result.message || '注册失败')
          }
        }
      } catch (error) {
        console.error('操作失败:', error)
        ElMessage.error(error.message || '操作失败')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
  background: white;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h2 {
  color: #333;
  font-size: 24px;
  margin-bottom: 10px;
}

.login-header p {
  color: #666;
  font-size: 16px;
}

.login-form {
  margin-top: 20px;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

.submit-btn:hover {
  opacity: 0.9;
}

.form-footer {
  text-align: center;
  margin-top: 20px;
}

:deep(.el-input__wrapper) {
  padding: 4px 12px;
}

:deep(.el-input__prefix) {
  margin-right: 8px;
}
/* ====== 新版视觉增强（追加覆盖旧样式） ====== */
.login-container {
  background:
    radial-gradient(1200px 800px at 10% 10%, rgba(102, 126, 234, 0.45), transparent 55%),
    radial-gradient(900px 600px at 90% 20%, rgba(118, 75, 162, 0.40), transparent 55%),
    radial-gradient(900px 700px at 50% 90%, rgba(16, 185, 129, 0.18), transparent 55%),
    linear-gradient(135deg, #0b1220 0%, #141a2e 55%, #0b1220 100%);
  padding: 24px;
  position: relative;
  overflow: hidden;
}

.bg-blobs {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.9;
  filter: blur(32px);
}

.blob {
  position: absolute;
  width: 520px;
  height: 520px;
  border-radius: 999px;
  background: radial-gradient(circle at 30% 30%, rgba(102, 126, 234, 0.95), rgba(102, 126, 234, 0) 65%);
  animation: float 10s ease-in-out infinite;
}

.blob-1 {
  left: -180px;
  top: -180px;
}

.blob-2 {
  right: -220px;
  top: 40px;
  width: 620px;
  height: 620px;
  background: radial-gradient(circle at 30% 30%, rgba(118, 75, 162, 0.90), rgba(118, 75, 162, 0) 65%);
  animation-duration: 12s;
}

.blob-3 {
  left: 15%;
  bottom: -260px;
  width: 760px;
  height: 760px;
  background: radial-gradient(circle at 30% 30%, rgba(16, 185, 129, 0.35), rgba(16, 185, 129, 0) 65%);
  animation-duration: 14s;
}

.shell {
  position: relative;
  width: min(1000px, 100%);
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 18px;
  z-index: 1;
}

.brand-panel {
  border-radius: 16px;
  padding: 28px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.10);
  box-shadow: 0 18px 60px rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(14px);
  color: rgba(255, 255, 255, 0.92);
  display: flex;
  flex-direction: column;
}

.brand-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  width: fit-content;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(255, 255, 255, 0.08);
  font-size: 12px;
  letter-spacing: 0.3px;
  color: rgba(255, 255, 255, 0.85);
}

.brand-title {
  margin: 14px 0 8px;
  font-size: 26px;
  line-height: 1.2;
  letter-spacing: 0.2px;
}

.brand-subtitle {
  margin: 0;
  color: rgba(255, 255, 255, 0.72);
  line-height: 1.6;
  font-size: 14px;
}

.feature-list {
  margin-top: 22px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.feature {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.10);
}

.dot {
  width: 10px;
  height: 10px;
  margin-top: 6px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 1), rgba(118, 75, 162, 1));
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.15);
}

.feature-title {
  font-weight: 700;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.92);
}

.feature-desc {
  margin-top: 2px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.70);
}

.brand-footer {
  margin-top: auto;
  padding-top: 14px;
}

.muted {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.60);
}

.login-card {
  width: auto;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 16px;
  padding: 26px;
  box-shadow: 0 18px 60px rgba(0, 0, 0, 0.35);
  border: 1px solid rgba(255, 255, 255, 0.35);
  backdrop-filter: blur(10px);
}

.login-header {
  margin-bottom: 14px;
}

.login-header h2 {
  margin-bottom: 6px;
}

.login-header p {
  margin: 0;
}

.login-form {
  margin-top: 14px;
}

.mode-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  background: rgba(17, 24, 39, 0.06);
  border-radius: 12px;
  padding: 6px;
}

.tab {
  border: none;
  background: transparent;
  height: 38px;
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 700;
  color: rgba(17, 24, 39, 0.70);
  transition: all 160ms ease;
}

.tab.active {
  background: white;
  color: rgba(17, 24, 39, 0.95);
  box-shadow: 0 8px 22px rgba(17, 24, 39, 0.12);
}

.submit-btn {
  border-radius: 10px;
  box-shadow: 0 10px 22px rgba(102, 126, 234, 0.25);
  background: linear-gradient(135deg, #667eea 0%, #764ba2 60%, #5b7cfa 100%);
}

:deep(.el-input__wrapper) {
  padding: 6px 12px;
  border-radius: 10px;
}

@keyframes float {
  0% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(0, 16px, 0) scale(1.04);
  }
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
}

@media (max-width: 960px) {
  .shell {
    grid-template-columns: 1fr;
  }
  .brand-panel {
    display: none;
  }
}
</style>