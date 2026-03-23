<template>
  <div class="coupon-admin-page">
    <div class="page-header">
      <h1 class="page-title">🎫 秒杀优惠券管理</h1>
      <el-button type="primary" @click="openCreateDialog">新建活动</el-button>
    </div>
    <p class="page-desc">
      设置<strong>开始/结束时间</strong>、<strong>发放数量（库存）</strong>与<strong>面额</strong>；保存为草稿后点击「发布」，用户即可在「优惠券秒杀」页面抢购，在线用户会收到 WebSocket 通知。
    </p>

    <el-table v-loading="loading" :data="activities" border stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="活动标题" min-width="160" show-overflow-tooltip />
      <el-table-column prop="totalStock" label="库存" width="90" align="center" />
      <el-table-column label="面额(元)" width="100" align="right">
        <template #default="{ row }">{{ formatMoney(row.discountAmount) }}</template>
      </el-table-column>
      <el-table-column label="开始时间" width="170">
        <template #default="{ row }">{{ formatDt(row.startTime) }}</template>
      </el-table-column>
      <el-table-column label="结束时间" width="170">
        <template #default="{ row }">{{ formatDt(row.endTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 0"
            type="success"
            size="small"
            :loading="publishingId === row.id"
            @click="handlePublish(row)"
          >
            发布
          </el-button>
          <span v-else class="op-muted">—</span>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新建秒杀活动" width="520px" destroy-on-close @closed="resetCreateForm">
      <el-form ref="formRef" :model="createForm" :rules="rules" label-width="100px">
        <el-form-item label="活动标题" prop="title">
          <el-input v-model="createForm.title" placeholder="例如：周末场地立减" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="发放数量" prop="totalStock">
          <el-input-number v-model="createForm.totalStock" :min="1" :max="999999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="优惠面额" prop="discountAmount">
          <el-input-number v-model="createForm.discountAmount" :min="0.01" :precision="2" :step="1" style="width: 100%" />
          <span class="form-tip">元，预约下单时可抵扣（不超过订单原价）</span>
        </el-form-item>
        <el-form-item label="活动时间" prop="timeRange">
          <el-date-picker
            v-model="createForm.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">保存为草稿</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAdminCouponActivities, createCouponActivity, publishCouponActivity } from '@/api/adminCoupon'

const loading = ref(false)
const activities = ref([])
const dialogVisible = ref(false)
const submitting = ref(false)
const publishingId = ref(null)
const formRef = ref(null)

const createForm = reactive({
  title: '',
  totalStock: 100,
  discountAmount: 10,
  timeRange: null
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  totalStock: [{ required: true, message: '请设置数量', trigger: 'change' }],
  discountAmount: [{ required: true, message: '请设置面额', trigger: 'change' }],
  timeRange: [{ required: true, message: '请选择活动时间', trigger: 'change' }]
}

const formatMoney = (v) => {
  if (v == null || v === '') return '-'
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(2) : String(v)
}

const formatDt = (s) => {
  if (!s) return '-'
  return String(s).replace('T', ' ').substring(0, 19)
}

const statusText = (s) => {
  if (s === 0) return '草稿'
  if (s === 1) return '已发布'
  if (s === 2) return '已结束'
  return String(s ?? '-')
}

const statusTagType = (s) => {
  if (s === 0) return 'info'
  if (s === 1) return 'success'
  return 'warning'
}

const load = async () => {
  loading.value = true
  try {
    const res = await listAdminCouponActivities()
    if (res.code === 200 && Array.isArray(res.data)) {
      activities.value = res.data
    } else {
      activities.value = []
      ElMessage.error(res.message || '加载失败')
    }
  } catch (e) {
    activities.value = []
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  resetCreateForm()
  dialogVisible.value = true
}

const resetCreateForm = () => {
  createForm.title = ''
  createForm.totalStock = 100
  createForm.discountAmount = 10
  createForm.timeRange = null
  formRef.value?.clearValidate?.()
}

const submitCreate = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  const tr = createForm.timeRange
  if (!tr || tr.length !== 2) {
    ElMessage.warning('请选择活动时间')
    return
  }
  submitting.value = true
  try {
    const res = await createCouponActivity({
      title: createForm.title.trim(),
      totalStock: createForm.totalStock,
      discountAmount: createForm.discountAmount,
      startTime: tr[0],
      endTime: tr[1]
    })
    if (res.code === 200) {
      ElMessage.success('已保存草稿，请点击「发布」后用户方可抢购')
      dialogVisible.value = false
      await load()
    } else {
      ElMessage.error(res.message || '创建失败')
    }
  } catch (e) {
    ElMessage.error(e.message || '创建失败')
  } finally {
    submitting.value = false
  }
}

const handlePublish = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定发布「${row.title}」？发布后将初始化 Redis 库存，并向在线用户推送通知。`,
      '发布活动',
      { type: 'warning' }
    )
  } catch {
    return
  }
  publishingId.value = row.id
  try {
    const res = await publishCouponActivity(row.id)
    if (res.code === 200) {
      ElMessage.success('发布成功')
      await load()
    } else {
      ElMessage.error(res.message || '发布失败')
    }
  } catch (e) {
    ElMessage.error(e.message || '发布失败')
  } finally {
    publishingId.value = null
  }
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.coupon-admin-page {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  color: #303133;
}

.page-desc {
  margin: 0 0 20px;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

.form-tip {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.op-muted {
  color: #c0c4cc;
}
</style>
