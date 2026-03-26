<template>
  <div class="page-container">
    <div class="page-header">
      <h1 class="title">📋 订单统计</h1>
      <div class="filters">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          clearable
        />
        <el-button type="primary" :loading="loading" @click="loadData">查询</el-button>
        <el-button :disabled="loading" @click="reset">重置</el-button>
      </div>
    </div>

    <div class="cards">
      <div class="card">
        <div class="card-label">订单总数</div>
        <div class="card-value">{{ ni(summary.totalOrders) }}</div>
      </div>
      <div class="card">
        <div class="card-label">订单总金额(元)</div>
        <div class="card-value">{{ fmtMoney(summary.totalAmount) }}</div>
      </div>
      <div class="card">
        <div class="card-label">已收款(元)</div>
        <div class="card-value">{{ fmtMoney(summary.paidAmount) }}</div>
      </div>
      <div class="card">
        <div class="card-label">预约总时长(h)</div>
        <div class="card-value">{{ fmtHours(summary.totalHours) }}</div>
      </div>
    </div>

    <div class="cards cards--status">
      <div class="card">
        <div class="card-label">待付款</div>
        <div class="card-value">{{ ni(summary.pendingPaymentCount) }}</div>
      </div>
      <div class="card">
        <div class="card-label">已付款</div>
        <div class="card-value">{{ ni(summary.paidCount) }}</div>
      </div>
      <div class="card">
        <div class="card-label">已完成</div>
        <div class="card-value">{{ ni(summary.completedCount) }}</div>
      </div>
      <div class="card">
        <div class="card-label">已取消</div>
        <div class="card-value">{{ ni(summary.canceledCount) }}</div>
      </div>
      <div class="card">
        <div class="card-label">已过期</div>
        <div class="card-value">{{ ni(summary.expiredCount) }}</div>
      </div>
    </div>

    <el-table :data="daily" border stripe v-loading="loading" class="table">
      <el-table-column prop="statDate" label="日期" min-width="120" />
      <el-table-column prop="orderCount" label="订单数" width="100" align="right" sortable />
      <el-table-column prop="totalAmount" label="总金额(元)" width="130" align="right" sortable>
        <template #default="{ row }">{{ fmtMoney(row.totalAmount) }}</template>
      </el-table-column>
      <el-table-column prop="paidAmount" label="已收款(元)" width="130" align="right" sortable>
        <template #default="{ row }">{{ fmtMoney(row.paidAmount) }}</template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getOrderStats } from '@/api/stats'

const loading = ref(false)
const dateRange = ref([])
const daily = ref([])

const summary = reactive({
  totalOrders: 0,
  pendingPaymentCount: 0,
  paidCount: 0,
  completedCount: 0,
  canceledCount: 0,
  expiredCount: 0,
  totalAmount: null,
  paidAmount: null,
  totalHours: null
})

const ni = (v) => (v == null || v === '' ? 0 : Number(v) || 0)

const fmtMoney = (v) => {
  if (v == null || v === '') return '0.00'
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

const fmtHours = (v) => {
  if (v == null || v === '') return '0.00'
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

const applySummary = (s) => {
  if (!s || typeof s !== 'object') {
    summary.totalOrders = 0
    summary.pendingPaymentCount = 0
    summary.paidCount = 0
    summary.completedCount = 0
    summary.canceledCount = 0
    summary.expiredCount = 0
    summary.totalAmount = null
    summary.paidAmount = null
    summary.totalHours = null
    return
  }
  summary.totalOrders = ni(s.totalOrders)
  summary.pendingPaymentCount = ni(s.pendingPaymentCount)
  summary.paidCount = ni(s.paidCount)
  summary.completedCount = ni(s.completedCount)
  summary.canceledCount = ni(s.canceledCount)
  summary.expiredCount = ni(s.expiredCount)
  summary.totalAmount = s.totalAmount
  summary.paidAmount = s.paidAmount
  summary.totalHours = s.totalHours
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {}
    if (dateRange.value?.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await getOrderStats(params)
    if (res?.code === 200 && res.data) {
      applySummary(res.data.summary)
      daily.value = Array.isArray(res.data.daily) ? res.data.daily : []
    } else {
      applySummary(null)
      daily.value = []
      ElMessage.error(res?.message || '加载失败')
    }
  } catch (e) {
    applySummary(null)
    daily.value = []
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const reset = async () => {
  dateRange.value = []
  await loadData()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.page-container {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.title {
  margin: 0;
  font-size: 20px;
  color: #111827;
}

.filters {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.cards--status {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.card {
  border: 1px solid #eef2f7;
  border-radius: 10px;
  padding: 12px;
  background: #fafafa;
}

.card-label {
  font-size: 12px;
  color: #6b7280;
}

.card-value {
  margin-top: 8px;
  font-size: 20px;
  font-weight: 700;
  color: #111827;
}

.table {
  width: 100%;
}

@media (max-width: 1000px) {
  .cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .cards--status {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
