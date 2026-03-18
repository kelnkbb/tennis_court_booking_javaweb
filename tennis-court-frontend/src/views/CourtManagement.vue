<!-- src/views/CourtManagement.vue -->
<template>
  <div class="court-container">
    <!-- 页面标题和操作栏 -->
    <div class="page-header">
      <h1 class="page-title">
        🎾 {{ isAdmin ? '场地管理' : '场地列表' }}
      </h1>
      <div class="actions">
        <button
          v-if="isAdmin"
          class="btn btn-primary"
          @click="handleAddCourt"
        >
          ➕ 添加场地
        </button>
        <button class="btn btn-secondary" @click="loadCourts">
          刷新
        </button>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <input
          type="text"
          v-model="searchKeyword"
          placeholder="🔍 搜索场地名称或地址..."
          class="search-input"
          @keyup.enter="handleSearch"
      />
      <button class="btn btn-search" @click="handleSearch">
        搜索
      </button>
    </div>

    <!-- 数据表格 -->
    <div class="table-container">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading">
        <span class="loading-icon">⏳</span>
        <p>加载中...</p>
      </div>

      <!-- 错误提示 -->
      <div v-else-if="error" class="error">
        <span class="error-icon">❌</span>
        <p>{{ error }}</p>
        <button @click="loadCourts" class="retry-btn">重试</button>
      </div>

      <!-- 空数据 -->
      <div v-else-if="courts.length === 0" class="empty">
        <span class="empty-icon">📭</span>
        <p>暂无场地数据</p>
      </div>

      <!-- 场地表格 -->
      <table v-else class="court-table">
        <thead>
        <tr>
          <th>场地名称</th>
          <th>地址</th>
          <th>类型</th>
          <th>价格（元/小时）</th>
          <th>营业时间</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="court in courts" :key="court.id">
          <td class="court-name">{{ court.name }}</td>
          <td>{{ court.address }}</td>
          <td>
              <span :class="['type-badge', getTypeClass(court.type)]">
                {{ getTypeText(court.type) }}
              </span>
          </td>
          <td class="price">¥{{ court.price }}</td>
          <td>{{ court.openTime }}</td>
          <td>
              <span :class="['status-badge', court.status === 1 ? 'status-active' : 'status-disabled']">
                {{ court.status === 1 ? '营业中' : '已关闭' }}
              </span>
          </td>
          <td class="actions">
            <template v-if="isAdmin">
              <button class="btn-action btn-edit" @click="handleEdit(court)">
                编辑
              </button>
              <button class="btn-action btn-delete" @click="handleDelete(court)">
                删除
              </button>
            </template>
            <template v-else>
              <button class="btn-action btn-edit" @click="handleBook(court)">
                预订
              </button>
            </template>
          </td>
        </tr>
        </tbody>
      </table>
    </div>

    <!-- 新增/编辑场地对话框 -->
    <el-dialog
        v-model="dialogVisible"
        :title="dialogTitle"
        width="600px"
        destroy-on-close
        @close="handleDialogClose"
    >
      <el-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          label-width="120px"
          class="court-form"
      >
        <!-- 场地名称 -->
        <el-form-item label="场地名称" prop="name">
          <el-input
              v-model="formData.name"
              placeholder="请输入场地名称"
              clearable
          />
        </el-form-item>

        <!-- 场地类型 -->
        <el-form-item label="场地类型" prop="type">
          <el-select v-model="formData.type" placeholder="请选择场地类型" clearable>
            <el-option :value="1" label="室内" />
            <el-option :value="2" label="室外" />
          </el-select>
        </el-form-item>

        <!-- 地址 -->
        <el-form-item label="地址" prop="address">
          <el-input
              v-model="formData.address"
              placeholder="请输入详细地址"
              type="textarea"
              :rows="2"
          />
        </el-form-item>

        <!-- 价格 -->
        <el-form-item label="价格（元/小时）" prop="price">
          <el-input-number
              v-model="formData.price"
              :min="0"
              :precision="2"
              :step="10"
              placeholder="请输入价格"
          />
        </el-form-item>

        <!-- 营业时间 -->
        <el-form-item label="营业时间" prop="openTime">
          <el-time-picker
              v-model="openTimeRange"
              is-range
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              format="HH:mm"
              value-format="HH:mm"
              @change="handleTimeChange"
          />
        </el-form-item>

        <!-- 状态（编辑时显示） -->
        <el-form-item v-if="currentCourt" label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">营业中</el-radio>
            <el-radio :value="0">已关闭</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 图片URL（可选） -->
        <el-form-item label="图片URL" prop="images">
          <el-input
              v-model="formData.images"
              placeholder="请输入图片URL，多个用逗号分隔"
              type="textarea"
              :rows="2"
          />
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
import { ref, onMounted, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAllCourts, deleteCourtById, addCourt, updateCourt } from '@/api/court'

const router = useRouter()

const courts = ref([])
const loading = ref(true)
const error = ref(null)
const searchKeyword = ref('')
const submitting = ref(false)

// 当前用户及角色
const currentUser = ref(null)
const isAdmin = computed(() => currentUser.value && currentUser.value.role === 2)

// 对话框控制
const dialogVisible = ref(false)
const dialogTitle = ref('')
const currentCourt = ref(null)
const formRef = ref(null)

// 营业时间范围（用于时间选择器）
const openTimeRange = ref([])

// 表单数据
const formData = reactive({
  name: '',
  type: null,
  address: '',
  price: null,
  openTime: '',
  status: 1,
  images: ''
})

// 表单验证规则
const rules = {
  name: [
    { required: true, message: '请输入场地名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择场地类型', trigger: 'change' }
  ],
  address: [
    { required: true, message: '请输入地址', trigger: 'blur' }
  ],
  price: [
    { required: true, message: '请输入价格', trigger: 'blur' }
  ],
  openTime: [
    { required: true, message: '请选择营业时间', trigger: 'change' }
  ]
}

// 从本地加载当前用户
const loadCurrentUser = () => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    try {
      currentUser.value = JSON.parse(userStr)
    } catch (e) {
      currentUser.value = null
    }
  }
}

// 加载场地列表
const loadCourts = async () => {
  console.log('🚀 开始加载场地数据...')
  loading.value = true
  error.value = null

  try {
    const result = await getAllCourts()
    console.log('✅ 场地数据返回:', result)

    if (result && result.code === 200) {
      courts.value = result.data || []
    } else {
      courts.value = []
      error.value = result?.message || '加载失败'
    }

    console.log('✅ 场地数量:', courts.value.length)
  } catch (err) {
    console.error('❌ 加载失败:', err)
    error.value = err.message || '加载失败，请稍后重试'
    courts.value = []
  } finally {
    loading.value = false
  }
}

// 处理时间变化
const handleTimeChange = (val) => {
  if (val && val.length === 2) {
    formData.openTime = `${val[0]}-${val[1]}`
  } else {
    formData.openTime = ''
  }
}

// 解析营业时间
const parseOpenTime = (openTime) => {
  if (openTime && openTime.includes('-')) {
    return openTime.split('-')
  }
  return []
}

// 新增场地
const handleAddCourt = () => {
  console.log('新增场地')
  currentCourt.value = null
  dialogTitle.value = '新增场地'
  resetForm()
  dialogVisible.value = true
}

// 普通用户：从场地列表发起预订
const handleBook = (court) => {
  if (!court || !court.id) return
  router.push({
    path: '/my-bookings',
    query: { courtId: court.id }
  })
}

// 编辑场地
const handleEdit = (court) => {
  console.log('编辑场地:', court)
  currentCourt.value = { ...court }
  dialogTitle.value = '编辑场地'

  // 填充表单数据
  Object.assign(formData, {
    name: court.name,
    type: court.type,
    address: court.address,
    price: court.price,
    openTime: court.openTime || '',
    status: court.status,
    images: court.images || ''
  })

  // 解析营业时间
  const timeRange = parseOpenTime(court.openTime)
  if (timeRange.length === 2) {
    openTimeRange.value = timeRange
  } else {
    openTimeRange.value = []
  }

  dialogVisible.value = true
}

// 删除场地
const handleDelete = async (court) => {
  console.log('🔍 handleDelete 被调用')

  if (!court.id) {
    console.error('❌ court.id 不存在或为 null')
    ElMessage.error('错误：场地ID不存在')
    return
  }

  ElMessageBox.confirm(
      `确定要删除场地 "${court.name}" 吗？此操作不可恢复！`,
      '删除场地',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
  ).then(async () => {
    try {
      loading.value = true
      console.log('🚀 调用 deleteCourtById，参数 id =', court.id)

      const result = await deleteCourtById(court.id)
      console.log('✅ deleteCourtById 返回结果:', result)

      if (result && result.code === 200) {
        ElMessage.success('删除成功！')
        await loadCourts()
      } else {
        ElMessage.error(result?.message || '删除失败')
      }
    } catch (err) {
      console.error('❌ 删除失败:', err)
      ElMessage.error('删除失败：' + (err.message || '网络错误'))
    } finally {
      loading.value = false
    }
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
          name: formData.name,
          type: formData.type,
          address: formData.address,
          price: formData.price,
          openTime: formData.openTime,
          images: formData.images || ''
        }

        // 如果是编辑，添加状态
        if (currentCourt.value) {
          submitData.status = formData.status
        }

        console.log('提交数据:', submitData)

        let result
        if (currentCourt.value) {
          // 编辑场地
          result = await updateCourt(currentCourt.value.id, submitData)
        } else {
          // 新增场地
          result = await addCourt(submitData)
        }

        console.log('操作结果:', result)

        if (result && result.code === 200) {
          ElMessage.success(currentCourt.value ? '编辑成功' : '新增成功')
          dialogVisible.value = false
          await loadCourts() // 重新加载列表
        } else {
          ElMessage.error(result?.message || (currentCourt.value ? '编辑失败' : '新增失败'))
        }
      } catch (err) {
        console.error('操作失败:', err)
        ElMessage.error(err.message || (currentCourt.value ? '编辑失败' : '新增失败'))
      } finally {
        submitting.value = false
      }
    }
  })
}

// 对话框关闭
const handleDialogClose = () => {
  resetForm()
  currentCourt.value = null
  openTimeRange.value = []
}

// 重置表单
const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
  Object.assign(formData, {
    name: '',
    type: null,
    address: '',
    price: null,
    openTime: '',
    status: 1,
    images: ''
  })
  openTimeRange.value = []
}

// 搜索
const handleSearch = () => {
  if (!searchKeyword.value) {
    loadCourts()
    return
  }
  courts.value = courts.value.filter(court =>
      court.name?.includes(searchKeyword.value) ||
      court.address?.includes(searchKeyword.value)
  )
}

// 类型相关函数
const getTypeClass = (type) => {
  if (!type) return ''
  const typeNum = Number(type)
  if (typeNum === 1) return 'type-indoor'
  if (typeNum === 2) return 'type-outdoor'
  return ''
}

const getTypeText = (type) => {
  if (!type) return '未知'
  const typeNum = Number(type)
  if (typeNum === 1) return '室内'
  if (typeNum === 2) return '室外'
  return '其他'
}

onMounted(() => {
  console.log('🎯 组件已挂载')
  loadCurrentUser()
  loadCourts()
})
</script>

<style scoped>
.court-container {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f0f2f5;
}

.page-title {
  font-size: 24px;
  color: #333;
  margin: 0;
}

.actions {
  display: flex;
  gap: 12px;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-secondary {
  background: #f5f5f5;
  color: #333;
}

.btn-secondary:hover {
  background: #e8e8e8;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.search-input {
  flex: 1;
  padding: 10px 16px;
  border: 2px solid #e8e8e8;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  transition: all 0.3s;
}

.search-input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
}

.btn-search {
  background: #667eea;
  color: white;
  padding: 10px 24px;
}

.btn-search:hover {
  background: #5568d3;
}

.table-container {
  overflow-x: auto;
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
}

.empty p {
  color: #6c757d;
  font-size: 16px;
}

.court-table {
  width: 100%;
  border-collapse: collapse;
}

.court-table thead {
  background: #f8f9fa;
}

.court-table th {
  padding: 16px;
  text-align: left;
  font-weight: 600;
  color: #495057;
  border-bottom: 2px solid #dee2e6;
  white-space: nowrap;
}

.court-table td {
  padding: 16px;
  border-bottom: 1px solid #dee2e6;
}

.court-table tbody tr:hover {
  background: #f8f9fa;
}

.court-name {
  font-weight: 600;
  color: #333;
}

.price {
  color: #f59e0b;
  font-weight: 600;
  font-size: 16px;
}

.type-badge, .status-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.type-indoor {
  background: #dbeafe;
  color: #2563eb;
}

.type-outdoor {
  background: #d1fae5;
  color: #059669;
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

.btn-action {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.3s;
}

.btn-edit {
  background: #f59e0b;
  color: white;
}

.btn-edit:hover {
  background: #d97706;
}

.btn-delete {
  background: #ef4444;
  color: white;
}

.btn-delete:hover {
  background: #dc2626;
}

/* 对话框样式 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.court-form {
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
  margin-bottom: 22px;
}

:deep(.el-input-number) {
  width: 100%;
}

@media (max-width: 768px) {
  .court-container {
    padding: 16px;
  }

  .actions {
    flex-direction: column;
  }

  .btn {
    width: 100%;
  }

  :deep(.el-dialog) {
    width: 90% !important;
    margin: 10% auto !important;
  }
}
</style>