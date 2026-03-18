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
        </template>
      </nav>
    </aside>

    <!-- 主内容区 -->
    <main class="main">
      <!-- 头部 -->
      <header class="header">
        <div class="header-left">
          <button class="menu-btn">☰</button>
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, User, Lock, SwitchButton } from '@element-plus/icons-vue'
import { logout, changePassword, getCurrentUser } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const currentUser = ref(null)

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
    'CourtStats': '场地统计'
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

      ElMessage.success('已退出登录')
      router.push('/login')
    } catch (error) {
      console.error('退出失败:', error)
      // 即使接口调用失败，也清除本地存储并跳转
      localStorage.removeItem('token')
      localStorage.removeItem('user')
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

onMounted(() => {
  loadCurrentUser()
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
</style>