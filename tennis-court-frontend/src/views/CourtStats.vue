<template>
  <div class="page-container">
    <div class="page-header">
      <h1 class="title">🎾 场地统计</h1>
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
        <div class="card-label">统计场地数</div>
        <div class="card-value">{{ totals.courtCount }}</div>
      </div>
      <div class="card">
        <div class="card-label">预约总数</div>
        <div class="card-value">{{ totals.bookingCount }}</div>
      </div>
      <div class="card">
        <div class="card-label">总时长(h)</div>
        <div class="card-value">{{ totals.totalHours }}</div>
      </div>
      <div class="card">
        <div class="card-label">已付款金额(元)</div>
        <div class="card-value">{{ totals.paidAmount }}</div>
      </div>
    </div>

    <el-table :data="rows" border stripe v-loading="loading" class="table">
      <el-table-column prop="courtName" label="场地" min-width="160" />
      <el-table-column prop="bookingCount" label="预约数" width="90" align="right" sortable />
      <el-table-column prop="totalHours" label="总时长(h)" width="120" align="right" sortable />
      <el-table-column prop="totalAmount" label="总金额(元)" width="130" align="right" sortable />
      <el-table-column prop="paidCount" label="已付款单" width="110" align="right" sortable />
      <el-table-column prop="paidAmount" label="已付款金额(元)" width="150" align="right" sortable />
    </el-table>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getCourtStats } from '@/api/stats'

const loading = ref(false)
const dateRange = ref([])
const rows = ref([])

const totals = computed(() => {
  const courtCount = rows.value.length
  const sum = (key) =>
    rows.value.reduce((acc, r) => acc + (Number(r?.[key]) || 0), 0)

  const totalHours = sum('totalHours')
  const paidAmount = sum('paidAmount')
  return {
    courtCount,
    bookingCount: sum('bookingCount'),
    totalHours: totalHours.toFixed(2),
    paidAmount: paidAmount.toFixed(2)
  }
})

const loadData = async () => {
  loading.value = true
  try {
    const params = {}
    if (dateRange.value?.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await getCourtStats(params)
    if (res?.code === 200) {
      rows.value = Array.isArray(res.data) ? res.data : []
    } else {
      rows.value = []
      ElMessage.error(res?.message || '加载失败')
    }
  } catch (e) {
    rows.value = []
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
}
</style>
