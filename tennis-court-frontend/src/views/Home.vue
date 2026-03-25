<!-- src/views/Home.vue -->
<template>
  <div class="home-container">
    <div class="welcome-card">
      <template v-if="isAdmin">
        <h1>欢迎使用网球场地管理系统</h1>
        <p>选择左侧菜单开始管理</p>
      </template>
      <template v-else>
        <h1>欢迎来到网球场地预约系统</h1>
        <p>您可以查看场地并管理自己的预约</p>
      </template>
    </div>

    <!-- 管理员首页：原有统计卡片 -->
    <div class="stats-grid" v-if="isAdmin">
      <div class="stat-card">
        <div class="stat-icon">🎾</div>
        <div class="stat-info">
          <h3>场地总数</h3>
          <p>{{ courtCount }}</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">👥</div>
        <div class="stat-info">
          <h3>用户总数</h3>
          <p>{{ userCount }}</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">📅</div>
        <div class="stat-info">
          <h3>今日预约</h3>
          <p>{{ todayBookings }}</p>
        </div>
      </div>
    </div>

    <!-- 普通用户首页：仅场地总数和我的预约总数 -->
    <div class="stats-grid" v-else>
      <div class="stat-card">
        <div class="stat-icon">🎾</div>
        <div class="stat-info">
          <h3>场地总数</h3>
          <p>{{ courtCount }}</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">📚</div>
        <div class="stat-info">
          <h3>我的预约总数</h3>
          <p>{{ myBookingCount }}</p>
        </div>
      </div>
    </div>

    <!-- 热门场地（访问热度 Top5，登录用户可见） -->
    <section v-if="hotCourts.length" class="hot-courts-section">
      <div class="hot-courts-header">
        <h2>热门场地</h2>
        <p class="hot-courts-sub">根据大家浏览次数实时更新</p>
      </div>
      <div class="hot-courts-grid">
        <div
          v-for="(c, idx) in hotCourts"
          :key="c.id"
          class="hot-court-card"
          @click="goBookWithCourt(c.id)"
        >
          <div class="hot-court-rank">{{ idx + 1 }}</div>
          <div class="hot-court-body">
            <h3 class="hot-court-name">{{ c.name || '场地' }}</h3>
            <p class="hot-court-addr">{{ c.address || '—' }}</p>
            <el-tag size="small" :type="c.status === 1 ? 'success' : 'info'">
              {{ c.status === 1 ? '营业中' : '暂停' }}
            </el-tag>
          </div>
          <div class="hot-court-action">去预约 →</div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getAllCourts, getHotCourts } from '@/api/court'
import { getUserList } from '@/api/user'
import { getTodayBookingCount, getMyBookingCount } from '@/api/booking'

const router = useRouter()

const courtCount = ref(0)
const userCount = ref(0)
const todayBookings = ref(0)
const myBookingCount = ref(0)
const hotCourts = ref([])

// 当前登录用户（从本地存储获取）
const currentUser = ref(null)

const isAdmin = computed(() => currentUser.value && currentUser.value.role === 2)

const goBookWithCourt = (courtId) => {
  if (!courtId) return
  if (isAdmin.value) {
    router.push({ path: '/bookings', query: { courtId } })
  } else {
    router.push({ path: '/my-bookings', query: { courtId } })
  }
}

// 加载所有统计数据
const loadStats = async () => {
  try {
    // 加载当前用户
    const userStr = localStorage.getItem('user')
    if (userStr) {
      try {
        currentUser.value = JSON.parse(userStr)
      } catch (e) {
        currentUser.value = null
      }
    }

    // 并行请求基础数据
    const basePromises = [getAllCourts(), getHotCourts(5)]

    if (isAdmin.value) {
      basePromises.push(getUserList())
      basePromises.push(getTodayBookingCount())
    } else {
      basePromises.push(getMyBookingCount())
    }

    const results = await Promise.allSettled(basePromises)

    // 处理场地数据
    const courtsResult = results[0]
    if (courtsResult.status === 'fulfilled' && courtsResult.value) {
      const result = courtsResult.value
      if (result.code === 200) {
        courtCount.value = result.data?.length || 0
      }
    }

    const hotResult = results[1]
    if (hotResult.status === 'fulfilled' && hotResult.value?.code === 200 && Array.isArray(hotResult.value.data)) {
      hotCourts.value = hotResult.value.data
    } else {
      hotCourts.value = []
    }

    if (isAdmin.value) {
      const usersResult = results[2]
      const bookingsResult = results[3]

      // 处理用户数据
      if (usersResult.status === 'fulfilled' && usersResult.value) {
        const result = usersResult.value
        if (result.code === 200) {
          userCount.value = result.data?.length || 0
        } else if (Array.isArray(result)) {
          userCount.value = result.length
        }
      }

      // 处理今日预约数据
      if (bookingsResult.status === 'fulfilled' && bookingsResult.value) {
        const result = bookingsResult.value
        if (result.code === 200) {
          todayBookings.value = result.data || 0
        }
      }
    } else {
      const myBookingsResult = results[2]
      if (myBookingsResult.status === 'fulfilled' && myBookingsResult.value) {
        const result = myBookingsResult.value
        if (result.code === 200) {
          myBookingCount.value = result.data || 0
        }
      }
    }

  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

onMounted(() => {
  loadStats()
})
</script>

<style scoped>
.home-container {
  max-width: 1200px;
  margin: 0 auto;
  padding-bottom: 32px;
}

.welcome-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 40px;
  border-radius: 12px;
  margin-bottom: 30px;
  text-align: center;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.welcome-card h1 {
  font-size: 32px;
  margin-bottom: 10px;
}

.welcome-card p {
  font-size: 18px;
  opacity: 0.9;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
}

.stat-card {
  background: white;
  padding: 24px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-icon {
  font-size: 48px;
}

.stat-info h3 {
  font-size: 16px;
  color: #666;
  margin-bottom: 8px;
}

.stat-info p {
  font-size: 28px;
  font-weight: 600;
  color: #333;
}

/* —— 热门场地：与统计卡片同系白底卡片 + 紫粉渐变点缀 —— */
.hot-courts-section {
  margin-top: 28px;
  padding: 24px;
  background: linear-gradient(180deg, #fafbff 0%, #ffffff 100%);
  border-radius: 16px;
  border: 1px solid rgba(102, 126, 234, 0.12);
  box-shadow: 0 2px 12px rgba(102, 126, 234, 0.08);
}

.hot-courts-header {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.hot-courts-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0 0 6px;
  letter-spacing: 0.02em;
}

.hot-courts-sub {
  margin: 0;
  font-size: 14px;
  color: #888;
  line-height: 1.5;
}

.hot-courts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.hot-court-card {
  position: relative;
  background: #fff;
  border-radius: 12px;
  padding: 18px 16px 16px 52px;
  min-height: 120px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  border: 1px solid rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
}

.hot-court-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.18);
  border-color: rgba(102, 126, 234, 0.25);
}

.hot-court-rank {
  position: absolute;
  left: 14px;
  top: 16px;
  width: 30px;
  height: 30px;
  border-radius: 10px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-weight: 700;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(118, 75, 162, 0.35);
}

.hot-court-body {
  flex: 1;
  min-width: 0;
}

.hot-court-name {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
  color: #222;
  line-height: 1.35;
}

.hot-court-addr {
  margin: 0 0 10px;
  font-size: 13px;
  color: #666;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.hot-court-action {
  font-size: 13px;
  color: #667eea;
  font-weight: 600;
  margin-top: auto;
  padding-top: 8px;
}
</style>