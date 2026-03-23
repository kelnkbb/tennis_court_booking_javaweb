<template>
  <div class="coupon-seckill-page">
    <div class="page-header">
      <h1 class="page-title">🎫 优惠券秒杀</h1>
      <el-button @click="loadAll" :loading="loading">刷新</el-button>
    </div>
    <p class="page-desc">
      以下为当前<strong>进行中</strong>的秒杀活动；抢购请求由消息队列异步处理（削峰），页面将自动轮询结果。领取成功后可在「我的预订」下单时使用券码抵扣。每人每活动限领一张。
    </p>

    <div v-if="!loading && activities.length === 0" class="empty-block">
      <el-empty description="暂无进行中的活动，请稍后再来" />
    </div>

    <el-row v-else :gutter="16">
      <el-col v-for="act in activities" :key="act.id" :xs="24" :sm="12" :md="8" class="card-col">
        <el-card shadow="hover" class="act-card">
          <div class="act-title">{{ act.title }}</div>
          <div class="act-face">
            <span class="yen">¥</span>{{ formatMoney(act.discountAmount) }}
            <span class="face-label">抵扣额</span>
          </div>
          <div class="act-time">
            <div>{{ formatDt(act.startTime) }}</div>
            <div>至 {{ formatDt(act.endTime) }}</div>
          </div>
          <el-button
            type="primary"
            class="grab-btn"
            :loading="grabbingId === act.id"
            :disabled="!canGrab"
            @click="handleGrab(act)"
          >
            {{ token ? '立即抢购' : '登录后抢购' }}
          </el-button>
        </el-card>
      </el-col>
    </el-row>

    <el-divider content-position="left">我的未使用券</el-divider>
    <div v-if="!token" class="hint-login">登录后可查看已领取的券，并在预约时使用。</div>
    <el-table v-else v-loading="unusedLoading" :data="unusedList" border size="small" empty-text="暂无未使用券">
      <el-table-column prop="activityTitle" label="活动" min-width="140" />
      <el-table-column prop="couponCode" label="券码" width="220" />
      <el-table-column label="面额(元)" width="100" align="right">
        <template #default="{ row }">{{ formatMoney(row.discountAmount) }}</template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listCouponActivities, grabCoupon, getGrabResult, getMyUnusedCoupons } from '@/api/coupon'

const router = useRouter()
const loading = ref(false)
const unusedLoading = ref(false)
const activities = ref([])
const unusedList = ref([])
const grabbingId = ref(null)

const token = computed(() => localStorage.getItem('token'))

const canGrab = computed(() => !!token.value)

const formatMoney = (v) => {
  if (v == null || v === '') return '-'
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(2) : String(v)
}

const formatDt = (s) => {
  if (!s) return '-'
  return String(s).replace('T', ' ').substring(0, 19)
}

const loadActivities = async () => {
  loading.value = true
  try {
    const res = await listCouponActivities()
    if (res.code === 200 && Array.isArray(res.data)) {
      activities.value = res.data
    } else {
      activities.value = []
    }
  } catch {
    activities.value = []
  } finally {
    loading.value = false
  }
}

const loadUnused = async () => {
  if (!token.value) {
    unusedList.value = []
    return
  }
  unusedLoading.value = true
  try {
    const res = await getMyUnusedCoupons()
    if (res.code === 200 && Array.isArray(res.data)) {
      unusedList.value = res.data
    } else {
      unusedList.value = []
    }
  } catch {
    unusedList.value = []
  } finally {
    unusedLoading.value = false
  }
}

const loadAll = async () => {
  await loadActivities()
  await loadUnused()
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

/** 轮询异步抢购结果（PENDING → SUCCESS / FAILED） */
const pollGrabResult = async (grabId) => {
  const maxAttempts = 60
  const intervalMs = 400
  for (let i = 0; i < maxAttempts; i++) {
    let r
    try {
      r = await getGrabResult(grabId)
    } catch {
      await sleep(intervalMs)
      continue
    }
    if (r.code === 200 && r.data) {
      const st = r.data.status
      if (st === 'SUCCESS') return r.data
      if (st === 'FAILED') return r.data
    }
    await sleep(intervalMs)
  }
  return { status: 'FAILED', message: '处理超时，请刷新「我的未使用券」或稍后重试' }
}

const handleGrab = async (act) => {
  if (!token.value) {
    ElMessage.info('请先登录')
    router.push({ path: '/login', query: { redirect: '/coupons' } })
    return
  }
  grabbingId.value = act.id
  try {
    const res = await grabCoupon(act.id)
    if (res.code !== 200) {
      ElMessage.error(res.message || '抢购失败')
      return
    }
    const grabId = res.data?.grabId
    if (!grabId) {
      ElMessage.error('未返回 grabId')
      return
    }
    ElMessage.info('已排队处理，请稍候…')
    const result = await pollGrabResult(grabId)
    if (result.status === 'SUCCESS') {
      const code = result.couponCode
      await ElMessageBox.alert(
        code ? `您的券码：${code}\n请妥善保存，预约下单时可选择使用。` : '领取成功，请在下方「我的未使用券」查看券码。',
        '抢购成功',
        { confirmButtonText: '好的' }
      )
      await loadUnused()
    } else {
      ElMessage.error(result.message || '抢购失败')
    }
  } catch (e) {
    ElMessage.error(e.message || '抢购失败')
  } finally {
    grabbingId.value = null
  }
}

onMounted(() => {
  loadAll()
})
</script>

<style scoped>
.coupon-seckill-page {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  max-width: 1200px;
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

.empty-block {
  padding: 24px 0;
}

.card-col {
  margin-bottom: 16px;
}

.act-card {
  border-radius: 12px;
}

.act-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.act-face {
  font-size: 32px;
  font-weight: 700;
  color: #e6a23c;
  margin-bottom: 12px;
}

.act-face .yen {
  font-size: 18px;
  margin-right: 2px;
}

.face-label {
  display: inline-block;
  margin-left: 8px;
  font-size: 13px;
  font-weight: 400;
  color: #909399;
}

.act-time {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  margin-bottom: 16px;
}

.grab-btn {
  width: 100%;
}

.hint-login {
  font-size: 14px;
  color: #909399;
  margin-bottom: 12px;
}
</style>
