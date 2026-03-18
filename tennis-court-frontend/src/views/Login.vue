<!-- src/views/Login.vue -->
<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h2>网球场地预约管理系统</h2>
        <p>{{ isLogin ? '登录' : '注册' }}</p>
      </div>

      <el-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          label-width="0"
          class="login-form"
      >
        <!-- 用户名 -->
        <el-form-item prop="username">
          <el-input
              v-model="formData.username"
              placeholder="用户名"
              :prefix-icon="User"
              size="large"
          />
        </el-form-item>

        <!-- 密码 -->
        <el-form-item prop="password">
          <el-input
              v-model="formData.password"
              type="password"
              placeholder="密码"
              :prefix-icon="Lock"
              size="large"
              show-password
          />
        </el-form-item>

        <!-- 确认密码（注册时显示） -->
        <el-form-item v-if="!isLogin" prop="confirmPassword">
          <el-input
              v-model="formData.confirmPassword"
              type="password"
              placeholder="确认密码"
              :prefix-icon="Lock"
              size="large"
              show-password
          />
        </el-form-item>

        <!-- 真实姓名（注册时显示） -->
        <el-form-item v-if="!isLogin" prop="realName">
          <el-input
              v-model="formData.realName"
              placeholder="真实姓名"
              :prefix-icon="User"
              size="large"
          />
        </el-form-item>

        <!-- 手机号（注册时显示） -->
        <el-form-item v-if="!isLogin" prop="phone">
          <el-input
              v-model="formData.phone"
              placeholder="手机号"
              :prefix-icon="Iphone"
              size="large"
          />
        </el-form-item>

        <!-- 邮箱（注册时显示） -->
        <el-form-item v-if="!isLogin" prop="email">
          <el-input
              v-model="formData.email"
              placeholder="邮箱"
              :prefix-icon="Message"
              size="large"
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
</style>