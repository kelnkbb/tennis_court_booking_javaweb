<!-- src/views/UserManagement.vue -->
<template>
  <div class="user-list-container">
    <div class="header">
      <h1>👥 用户管理</h1>
    </div>

    <!-- 搜索栏板块 -->
    <div class="search-section">
      <div class="search-form">
        <div class="search-row">
          <div class="search-item">
            <label>用户名：</label>
            <el-input
                v-model="searchForm.username"
                placeholder="请输入用户名"
                clearable
                @keyup.enter="handleSearch"
            />
          </div>
          <div class="search-item">
            <label>真实姓名：</label>
            <el-input
                v-model="searchForm.realName"
                placeholder="请输入真实姓名"
                clearable
                @keyup.enter="handleSearch"
            />
          </div>
          <div class="search-item">
            <label>手机号：</label>
            <el-input
                v-model="searchForm.phone"
                placeholder="请输入手机号"
                clearable
                @keyup.enter="handleSearch"
            />
          </div>
        </div>
        <div class="search-row">
          <div class="search-item">
            <label>邮箱：</label>
            <el-input
                v-model="searchForm.email"
                placeholder="请输入邮箱"
                clearable
                @keyup.enter="handleSearch"
            />
          </div>
          <div class="search-item">
            <label>角色：</label>
            <el-select v-model="searchForm.role" placeholder="请选择角色" clearable>
              <el-option label="管理员" :value="2" />
              <el-option label="普通用户" :value="1" />
            </el-select>
          </div>
          <div class="search-item">
            <label>状态：</label>
            <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
              <el-option label="正常" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
          </div>
        </div>
        <div class="search-buttons">
          <el-button type="primary" @click="handleSearch">
            <span>🔍</span> 查询
          </el-button>
          <el-button @click="resetSearch">
            <span>🗑️</span> 清空
          </el-button>
        </div>
      </div>
    </div>

    <!-- 操作按钮栏 -->
    <div class="action-bar">
      <div class="action-buttons">
        <el-button type="success" @click="handleAddUser">
          <span>➕</span> 新增用户
        </el-button>
        <el-button type="danger" @click="handleBatchDelete" :disabled="selectedUsers.length === 0">
          <span>🗑️</span> 批量删除 ({{ selectedUsers.length }})
        </el-button>
      </div>
    </div>

    <div class="content">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading">
        <span class="loading-icon">⏳</span>
        <p>加载中...</p>
      </div>

      <!-- 错误提示 -->
      <div v-else-if="error" class="error">
        <span class="error-icon">❌</span>
        <p>{{ error }}</p>
        <button @click="loadUsers" class="retry-btn">重试</button>
      </div>

      <!-- 空数据 -->
      <div v-else-if="filteredUsers.length === 0" class="empty">
        <span class="empty-icon">📭</span>
        <p>暂无用户数据</p>
      </div>

      <!-- 用户表格 -->
      <div v-else class="table-wrapper">
        <div class="user-count">
          当前用户总数：<strong>{{ filteredUsers.length }}</strong>
        </div>
        <table class="user-table">
          <thead>
          <tr>
            <th width="50">
              <el-checkbox
                  v-model="selectAll"
                  @change="handleSelectAll"
                  :indeterminate="isIndeterminate"
              />
            </th>
            <th>ID</th>
            <th>用户名</th>
            <th>真实姓名</th>
            <th>手机号</th>
            <th>邮箱</th>
            <th>角色</th>
            <th>状态</th>
            <th>注册时间</th>
            <th width="180">操作</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="user in filteredUsers" :key="user.id">
            <td>
              <el-checkbox
                  v-model="selectedUsers"
                  :label="user.id"
                  @change="handleSelectChange"
              />
            </td>
            <td>{{ user.id }}</td>
            <td class="username">{{ user.username }}</td>
            <td>{{ user.realName || '-' }}</td>
            <td>{{ user.phone || '-' }}</td>
            <td>{{ user.email || '-' }}</td>
            <td>
                <span :class="['role-badge', user.role === 2 ? 'role-admin' : 'role-user']">
                  {{ user.role === 2 ? '管理员' : '普通用户' }}
                </span>
            </td>
            <td>
                <span :class="['status-badge', user.status === 1 ? 'status-active' : 'status-disabled']">
                  {{ user.status === 1 ? '正常' : '禁用' }}
                </span>
            </td>
            <td>{{ formatDate(user.createTime) }}</td>
            <td class="actions">
              <el-button
                  type="primary"
                  size="small"
                  @click="handleEditUser(user)"
              >
                <span>✏️</span> 编辑
              </el-button>
              <el-button
                  type="danger"
                  size="small"
                  @click="handleDeleteUser(user)"
              >
                <span>🗑️</span> 删除
              </el-button>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 新增/编辑用户对话框 -->
    <el-dialog
        v-model="dialogVisible"
        :title="dialogTitle"
        width="500px"
        destroy-on-close
        @close="handleDialogClose"
    >
      <el-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          label-width="100px"
          class="user-form"
      >
        <!-- 用户名 -->
        <el-form-item label="用户名" prop="username">
          <el-input
              v-model="formData.username"
              placeholder="请输入用户名"
              :disabled="!!currentUser"
          />
        </el-form-item>

        <!-- 密码（新增时必填，编辑时可选） -->
        <el-form-item
            :label="currentUser ? '新密码' : '密码'"
            prop="password"
        >
          <el-input
              v-model="formData.password"
              type="password"
              :placeholder="currentUser ? '不修改请留空' : '请输入密码'"
              show-password
          />
        </el-form-item>

        <!-- 确认密码 -->
        <el-form-item
            v-if="!currentUser || formData.password"
            label="确认密码"
            prop="confirmPassword"
        >
          <el-input
              v-model="formData.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              show-password
          />
        </el-form-item>

        <!-- 真实姓名 -->
        <el-form-item label="真实姓名" prop="realName">
          <el-input
              v-model="formData.realName"
              placeholder="请输入真实姓名"
          />
        </el-form-item>

        <!-- 手机号 -->
        <el-form-item label="手机号" prop="phone">
          <el-input
              v-model="formData.phone"
              placeholder="请输入手机号"
          />
        </el-form-item>

        <!-- 邮箱 -->
        <el-form-item label="邮箱" prop="email">
          <el-input
              v-model="formData.email"
              placeholder="请输入邮箱"
          />
        </el-form-item>

        <!-- 角色 -->
        <el-form-item label="角色" prop="role">
          <el-radio-group v-model="formData.role">
            <el-radio :value="1">普通用户</el-radio>
            <el-radio :value="2">管理员</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 状态（编辑时显示） -->
        <el-form-item v-if="currentUser" label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleDialogConfirm" :loading="submitting">
            {{ submitting ? '提交中...' : '确认' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, addUser } from '@/api/user'

const users = ref([])
const loading = ref(true)
const error = ref(null)
const submitting = ref(false)

// 搜索表单
const searchForm = ref({
  username: '',
  realName: '',
  phone: '',
  email: '',
  role: null,
  status: null
})

// 选中的用户ID列表
const selectedUsers = ref([])
const selectAll = ref(false)
const isIndeterminate = ref(false)

// 对话框控制
const dialogVisible = ref(false)
const dialogTitle = ref('')
const currentUser = ref(null)
const formRef = ref(null)

// 表单数据
const formData = ref({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  phone: '',
  email: '',
  role: 1,
  status: 1
})

// 表单验证规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    {
      validator: (rule, value, callback) => {
        if (!currentUser.value && !value) {
          callback(new Error('请输入密码'))
        } else if (value && value.length < 6) {
          callback(new Error('密码长度不能小于6位'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    {
      validator: (rule, value, callback) => {
        if ((!currentUser.value || formData.value.password) && !value) {
          callback(new Error('请再次输入密码'))
        } else if (value !== formData.value.password) {
          callback(new Error('两次输入密码不一致'))
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
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
}

// 加载用户列表
const loadUsers = async () => {
  console.log('🚀 开始加载用户数据...')
  loading.value = true
  error.value = null

  try {
    const response = await getUserList()
    console.log('✅ 用户数据返回:', response)

    if (response && response.code === 200) {
      users.value = response.data || []
    } else if (Array.isArray(response)) {
      users.value = response
    } else {
      users.value = []
    }

    console.log('✅ 用户数量:', users.value.length)
  } catch (err) {
    console.error('❌ 加载用户列表失败:', err)
    error.value = err.message || '加载失败，请稍后重试'
    users.value = []
  } finally {
    loading.value = false
  }
}

// 过滤后的用户列表（根据搜索条件）
const filteredUsers = computed(() => {
  return users.value.filter(user => {
    let match = true

    if (searchForm.value.username) {
      match = match && user.username?.toLowerCase().includes(searchForm.value.username.toLowerCase())
    }
    if (searchForm.value.realName) {
      match = match && user.realName?.toLowerCase().includes(searchForm.value.realName.toLowerCase())
    }
    if (searchForm.value.phone) {
      match = match && user.phone?.includes(searchForm.value.phone)
    }
    if (searchForm.value.email) {
      match = match && user.email?.toLowerCase().includes(searchForm.value.email.toLowerCase())
    }
    if (searchForm.value.role) {
      match = match && user.role === searchForm.value.role
    }
    if (searchForm.value.status !== null && searchForm.value.status !== '') {
      match = match && user.status === searchForm.value.status
    }

    return match
  })
})

// 监听选中项变化，更新全选状态
watch(selectedUsers, (newVal) => {
  if (newVal.length === 0) {
    selectAll.value = false
    isIndeterminate.value = false
  } else if (newVal.length === filteredUsers.value.length) {
    selectAll.value = true
    isIndeterminate.value = false
  } else {
    selectAll.value = false
    isIndeterminate.value = true
  }
})

// 处理全选
const handleSelectAll = (value) => {
  if (value) {
    selectedUsers.value = filteredUsers.value.map(u => u.id)
  } else {
    selectedUsers.value = []
  }
  isIndeterminate.value = false
}

// 处理单个选择变化
const handleSelectChange = () => {
  // 已经在watch中处理
}

// 搜索
const handleSearch = () => {
  console.log('执行搜索:', searchForm.value)
  ElMessage.success('搜索完成')
}

// 重置搜索
const resetSearch = () => {
  searchForm.value = {
    username: '',
    realName: '',
    phone: '',
    email: '',
    role: null,
    status: null
  }
  ElMessage.success('已清空搜索条件')
}

// 新增用户
const handleAddUser = () => {
  console.log('新增用户')
  currentUser.value = null
  dialogTitle.value = '新增用户'
  resetForm()
  dialogVisible.value = true
}

// 批量删除
const handleBatchDelete = () => {
  if (selectedUsers.value.length === 0) return

  ElMessageBox.confirm(
      `确定要删除选中的 ${selectedUsers.value.length} 个用户吗？此操作不可恢复！`,
      '批量删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
  ).then(() => {
    console.log('批量删除用户:', selectedUsers.value)
    ElMessage.success('批量删除成功（模拟）')
    selectedUsers.value = []
    // 实际项目中应该调用API删除后重新加载
    // loadUsers()
  }).catch(() => {})
}

// 编辑用户
const handleEditUser = (user) => {
  console.log('编辑用户:', user)
  currentUser.value = { ...user }
  dialogTitle.value = '编辑用户'
  // 填充表单数据
  formData.value = {
    username: user.username,
    password: '',
    confirmPassword: '',
    realName: user.realName || '',
    phone: user.phone || '',
    email: user.email || '',
    role: user.role,
    status: user.status
  }
  dialogVisible.value = true
}

// 删除用户
const handleDeleteUser = (user) => {
  ElMessageBox.confirm(
      `确定要删除用户 "${user.username}" 吗？此操作不可恢复！`,
      '删除用户',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
  ).then(() => {
    console.log('删除用户:', user.id)
    ElMessage.success('删除成功（模拟）')
    // 实际项目中应该调用API删除后重新加载
    // loadUsers()
  }).catch(() => {})
}

// 对话框确认
const handleDialogConfirm = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        submitting.value = true

        // 准备提交数据
        const submitData = {
          username: formData.value.username,
          password: formData.value.password,
          realName: formData.value.realName,
          phone: formData.value.phone,
          email: formData.value.email,
          role: formData.value.role
        }

        // 如果是编辑且没有输入新密码，不传密码字段
        if (currentUser.value && !formData.value.password) {
          delete submitData.password
        }

        // 如果是编辑，添加状态
        if (currentUser.value) {
          submitData.status = formData.value.status
        }

        console.log('提交数据:', submitData)

        // 调用新增用户API
        const result = await addUser(submitData)
        console.log('新增结果:', result)

        if (result && result.code === 200) {
          ElMessage.success(currentUser.value ? '编辑成功' : '新增成功')
          dialogVisible.value = false
          loadUsers() // 重新加载列表
        } else {
          ElMessage.error(result?.message || (currentUser.value ? '编辑失败' : '新增失败'))
        }
      } catch (err) {
        console.error('操作失败:', err)
        ElMessage.error(err.message || (currentUser.value ? '编辑失败' : '新增失败'))
      } finally {
        submitting.value = false
      }
    }
  })
}

// 对话框关闭
const handleDialogClose = () => {
  resetForm()
  currentUser.value = null
}

// 重置表单
const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
  formData.value = {
    username: '',
    password: '',
    confirmPassword: '',
    realName: '',
    phone: '',
    email: '',
    role: 1,
    status: 1
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
  loadUsers()
})
</script>

<style scoped>
.user-list-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.header {
  text-align: center;
  margin-bottom: 30px;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.header h1 {
  font-size: 28px;
  margin: 0;
}

/* 搜索栏样式 */
.search-section {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.search-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-row {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.search-item {
  flex: 1;
  min-width: 200px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-item label {
  width: 70px;
  color: #606266;
  font-size: 14px;
  white-space: nowrap;
}

.search-item :deep(.el-input),
.search-item :deep(.el-select) {
  flex: 1;
}

.search-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}

/* 操作按钮栏 */
.action-bar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 20px;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.content {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.loading, .error, .empty {
  text-align: center;
  padding: 60px 20px;
}

.loading-icon, .error-icon, .empty-icon {
  font-size: 48px;
  display: block;
  margin-bottom: 16px;
}

.loading p {
  color: #6c757d;
  font-size: 16px;
}

.error p {
  color: #dc3545;
  font-size: 16px;
  margin-bottom: 16px;
}

.retry-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 10px 24px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.retry-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.empty p {
  color: #6c757d;
  font-size: 16px;
}

.table-wrapper {
  padding: 20px;
}

.user-count {
  margin-bottom: 20px;
  font-size: 16px;
  color: #6c757d;
}

.user-count strong {
  color: #667eea;
  font-size: 20px;
}

.user-table {
  width: 100%;
  border-collapse: collapse;
}

.user-table thead {
  background: #f8f9fa;
}

.user-table th {
  padding: 16px;
  text-align: left;
  font-weight: 600;
  color: #495057;
  border-bottom: 2px solid #dee2e6;
}

.user-table td {
  padding: 16px;
  border-bottom: 1px solid #dee2e6;
}

.user-table tbody tr:hover {
  background: #f8f9fa;
}

.username {
  font-weight: 600;
  color: #333;
}

.role-badge,
.status-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.role-admin {
  background: #fee2e2;
  color: #dc2626;
}

.role-user {
  background: #dbeafe;
  color: #2563eb;
}

.status-active {
  background: #d1fae5;
  color: #059669;
}

.status-disabled {
  background: #fee2e2;
  color: #dc2626;
}

.actions {
  display: flex;
  gap: 8px;
}

.actions :deep(.el-button) {
  padding: 5px 10px;
  font-size: 12px;
}

.actions :deep(.el-button span) {
  margin-right: 4px;
}

/* 对话框样式 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.user-form {
  padding: 20px 20px 0;
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

:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

/* 响应式调整 */
@media (max-width: 1200px) {
  .search-item {
    min-width: 150px;
  }
}

@media (max-width: 768px) {
  .user-list-container {
    padding: 10px;
  }

  .header h1 {
    font-size: 22px;
  }

  .search-row {
    flex-direction: column;
    gap: 12px;
  }

  .search-item {
    width: 100%;
  }

  .action-buttons {
    flex-direction: column;
    width: 100%;
  }

  .action-buttons :deep(.el-button) {
    width: 100%;
  }

  .user-table {
    font-size: 14px;
  }

  .user-table th,
  .user-table td {
    padding: 12px 8px;
  }

  .actions {
    flex-direction: column;
  }

  :deep(.el-dialog) {
    width: 90% !important;
    margin: 10% auto !important;
  }

  .user-form {
    padding: 10px;
  }
}
</style>