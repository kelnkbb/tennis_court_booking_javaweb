<!-- src/views/Bookings.vue -->
<template>
  <div class="booking-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">
        📅 {{ isMyPage ? '我的预订' : '预约管理' }}
      </h1>
    </div>

    <!-- 搜索栏 -->
    <div class="search-section">
      <el-form :model="searchForm" label-width="100px" class="search-form">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="关键词">
              <el-input
                  v-model="searchForm.keyword"
                  placeholder="单号/用户/电话"
                  clearable
                  @keyup.enter="handleSearch"
              />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="场地">
              <el-select v-model="searchForm.courtId" placeholder="全部场地" clearable filterable>
                <el-option
                    v-for="court in courtOptions"
                    :key="court.id"
                    :label="court.name"
                    :value="court.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="状态">
              <el-select v-model="searchForm.status" placeholder="全部状态" clearable>
                <el-option label="已取消" :value="0" />
                <el-option label="待付款" :value="1" />
                <el-option label="已付款" :value="2" />
                <el-option label="已完成" :value="3" />
                <el-option label="已过期" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="预约日期">
              <el-date-picker
                  v-model="dateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  value-format="YYYY-MM-DD"
                  clearable
                  @change="handleDateChange"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24" style="text-align: right">
            <el-button type="primary" @click="handleSearch">
              <span>🔍</span> 查询
            </el-button>
            <el-button @click="resetSearch">
              <span>🗑️</span> 重置
            </el-button>
            <el-button type="success" @click="handleAddBooking()">
              <span>➕</span> 新增预约
            </el-button>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <!-- 管理员：待审核「我已支付」 -->
    <div v-if="isAdmin && !isMyPage && pendingPaymentVerifies.length" class="pending-payment-panel">
      <h3 class="pending-payment-title">待审核付款确认（{{ pendingPaymentVerifies.length }}）</h3>
      <el-table :data="pendingPaymentVerifies" size="small" border stripe class="pending-payment-table">
        <el-table-column prop="bookingNo" label="预约单号" width="170" />
        <el-table-column label="付款渠道" width="100">
          <template #default="{ row }">{{ payChannelLabel(row.paymentChannel) }}</template>
        </el-table-column>
        <el-table-column prop="courtName" label="场地" width="110" />
        <el-table-column prop="userName" label="用户" width="100" />
        <el-table-column label="预约时间" min-width="170">
          <template #default="{ row }">
            {{ row.bookingDate }} {{ row.startTime }} - {{ row.endTime }}
          </template>
        </el-table-column>
        <el-table-column label="应付(元)" width="100" align="right">
          <template #default="{ row }">
            <span class="price">¥{{ displayPayAmount(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="success" size="small" @click="handleApprovePaymentVerify(row)">确认已收款</el-button>
            <el-button type="warning" size="small" @click="handleRejectPaymentVerify(row)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 管理员：待审核取消申请 -->
    <div v-if="isAdmin && !isMyPage && pendingCancels.length" class="pending-cancel-panel">
      <h3 class="pending-cancel-title">待审核取消申请（{{ pendingCancels.length }}）</h3>
      <el-table :data="pendingCancels" size="small" border stripe class="pending-cancel-table">
        <el-table-column prop="bookingNo" label="预约单号" width="170" />
        <el-table-column prop="courtName" label="场地" width="110" />
        <el-table-column prop="userName" label="用户" width="100" />
        <el-table-column label="预约时间" min-width="170">
          <template #default="{ row }">
            {{ row.bookingDate }} {{ row.startTime }} - {{ row.endTime }}
          </template>
        </el-table-column>
        <el-table-column label="应付(元)" width="100" align="right">
          <template #default="{ row }">
            <span class="price">¥{{ displayPayAmount(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="success" size="small" @click="handleApproveCancel(row)">通过取消</el-button>
            <el-button type="warning" size="small" @click="handleRejectCancel(row)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar" v-if="showBatchActions">
      <div class="action-buttons">
        <el-button
            type="danger"
            @click="handleBatchDelete"
            :disabled="selectedBookings.length === 0"
        >
          <span>🗑️</span> 批量删除 ({{ selectedBookings.length }})
        </el-button>
      </div>
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
        <el-button @click="loadBookings" class="retry-btn">重试</el-button>
      </div>

      <!-- 空数据 -->
      <div v-else-if="filteredBookings.length === 0" class="empty">
        <span class="empty-icon">📭</span>
        <p>暂无预约数据</p>
      </div>

      <!-- 预约表格 -->
      <el-table
          v-else
          :data="filteredBookings"
          style="width: 100%"
          @selection-change="handleSelectionChange"
          border
          stripe
      >
        <el-table-column
            v-if="showBatchActions"
            type="selection"
            width="55"
        />
        <el-table-column prop="bookingNo" label="预约单号" width="180" />
        <el-table-column prop="courtName" label="场地" width="120" />
        <el-table-column prop="userName" label="用户名" width="120" v-if="!isMyPage" />
        <el-table-column label="预约时间" width="200">
          <template #default="{ row }">
            {{ row.bookingDate }} {{ row.startTime }} - {{ row.endTime }}
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="时长(小时)" width="100" align="center" />
        <el-table-column label="金额" min-width="150" align="right">
          <template #default="{ row }">
            <div class="booking-amount-cell">
              <span>原价 ¥{{ row.totalAmount }}</span>
              <template v-if="hasCouponDiscount(row)">
                <br /><span class="amount-discount">抵扣 ¥{{ row.couponDiscount }}</span>
              </template>
              <br /><span class="price">应付 ¥{{ displayPayAmount(row) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="contactName" label="联系人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="120" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ row.statusText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" :width="isMyPage ? 300 : 240">
          <template #default="{ row }">
            <el-button-group v-if="isAdmin && !isMyPage">
              <el-button
                  type="primary"
                  size="small"
                  @click="handleViewBooking(row)"
              >
                详情
              </el-button>
              <el-button
                  type="warning"
                  size="small"
                  @click="handleEditBooking(row)"
                  :disabled="row.status === 0 || row.status === 3 || row.status === 4"
              >
                编辑
              </el-button>
              <el-dropdown @command="(command) => handleStatusCommand(command, row)">
                <el-button size="small" type="info">
                  状态 <el-icon><arrow-down /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="cancel" :disabled="row.status === 0">取消预约</el-dropdown-item>
                    <el-dropdown-item command="pay" :disabled="row.status !== 1">确认付款</el-dropdown-item>
                    <el-dropdown-item command="complete" :disabled="row.status !== 2">完成预约</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </el-button-group>
            <el-button-group v-else-if="isMyPage">
              <el-button type="primary" size="small" @click="handleViewBooking(row)">详情</el-button>
              <template v-if="row.status === 1">
                <template v-if="Number(row.cancelRequestStatus) === 1">
                  <el-tag type="info" size="small" style="margin-left: 8px">取消审核中</el-tag>
                </template>
                <template v-else-if="Number(row.paymentVerifyStatus) === 1">
                  <el-tag type="warning" size="small" style="margin-left: 8px">付款审核中</el-tag>
                </template>
                <template v-else>
                  <el-button type="success" size="small" @click="openPayDialog(row)">付款</el-button>
                  <el-button type="danger" size="small" plain @click="handleUserRequestCancel(row)">申请取消</el-button>
                </template>
              </template>
            </el-button-group>
            <el-button-group v-else>
              <el-button type="primary" size="small" @click="handleViewBooking(row)">详情</el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 预约详情对话框 -->
    <el-dialog
        v-model="detailVisible"
        title="预约详情"
        width="600px"
        destroy-on-close
    >
      <div v-if="currentBooking" class="detail-container">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="预约单号">{{ currentBooking.bookingNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentBooking.status)">
              {{ currentBooking.statusText }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="场地">{{ currentBooking.courtName }}</el-descriptions-item>
          <el-descriptions-item label="用户">{{ currentBooking.userName }}</el-descriptions-item>
          <el-descriptions-item label="预约日期">{{ currentBooking.bookingDate }}</el-descriptions-item>
          <el-descriptions-item label="时间段">{{ currentBooking.startTime }} - {{ currentBooking.endTime }}</el-descriptions-item>
          <el-descriptions-item label="时长">{{ currentBooking.duration }} 小时</el-descriptions-item>
          <el-descriptions-item label="单价">¥{{ currentBooking.unitPrice }}</el-descriptions-item>
          <el-descriptions-item label="原价" :span="2">
            <span class="price">¥{{ currentBooking.totalAmount }}</span>
          </el-descriptions-item>
          <el-descriptions-item v-if="hasCouponDiscount(currentBooking)" label="券抵扣" :span="2">
            ¥{{ currentBooking.couponDiscount }}（券码 {{ currentBooking.couponCode || '-' }}）
          </el-descriptions-item>
          <el-descriptions-item label="应付金额" :span="2">
            <span class="price">¥{{ displayPayAmount(currentBooking) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="联系人">{{ currentBooking.contactName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentBooking.contactPhone }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ currentBooking.remark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="支付方式" v-if="currentBooking.paymentChannel">
            {{ payChannelLabel(currentBooking.paymentChannel) }}
          </el-descriptions-item>
          <el-descriptions-item label="付款时间" v-if="currentBooking.payTime">
            {{ formatDateTime(currentBooking.payTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(currentBooking.createTime) }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 用户付款方式 -->
    <el-dialog
        v-model="payDialogVisible"
        title="选择付款方式"
        width="480px"
        destroy-on-close
        @close="onPayMethodDialogClose"
    >
      <div v-if="payTarget" class="pay-dialog-body">
        <p class="pay-order-info">订单 {{ payTarget.bookingNo }}，应付 <span class="price">¥{{ displayPayAmount(payTarget) }}</span></p>
        <el-radio-group v-model="payChannel" class="pay-channel-group">
          <el-radio label="wechat" border>微信支付</el-radio>
          <el-radio label="alipay" border>支付宝</el-radio>
          <el-radio label="xianyu" border>闲鱼</el-radio>
          <el-radio label="stripe" border>Stripe（在线卡支付）</el-radio>
        </el-radio-group>
        <p v-if="payChannel === 'stripe'" class="pay-tip">
          将跳转至 Stripe 安全支付页完成付款（需后端配置 Stripe 密钥；测试卡见 Stripe 文档）。
        </p>
        <p v-else-if="payChannel === 'xianyu'" class="pay-tip">
          请选择后将弹出闲鱼收款码，按页面提示扫码付款。
        </p>
        <p v-else class="pay-tip">
          请选择后将弹出对应收款码，扫码完成付款。
        </p>
      </div>
      <template #footer>
        <el-button @click="payDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="stripeCheckoutLoading" @click="confirmPay">去付款</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="payQrDialogVisible"
      :title="`${payChannelLabel(payChannel)}收款码`"
      width="560px"
      destroy-on-close
      @close="payTarget = null"
    >
      <div class="pay-qr-body">
        <p v-if="payTarget" class="pay-order-info">
          订单 {{ payTarget.bookingNo }}，应付 <span class="price">¥{{ displayPayAmount(payTarget) }}</span>
        </p>
        <img :src="payQrImageMap[payChannel]" :alt="`${payChannelLabel(payChannel)}收款码`" class="pay-qr-image" />
        <p class="pay-tip">请使用{{ payChannelLabel(payChannel) }}扫码付款。</p>
      </div>
      <template #footer>
        <el-button @click="payQrDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="claimPaidSubmitting" @click="handleClaimPaid">我已支付</el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑预约对话框 -->
    <el-dialog
        v-model="formVisible"
        :title="formTitle"
        width="600px"
        destroy-on-close
        @close="handleFormClose"
    >
      <el-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          label-width="100px"
          class="booking-form"
      >
        <!-- 场地选择 -->
        <el-form-item label="场地" prop="courtId">
          <el-select
              v-model="formData.courtId"
              placeholder="请选择场地"
              filterable
              @change="handleCourtChange"
          >
            <el-option
                v-for="court in courtOptions"
                :key="court.id"
                :label="court.name"
                :value="court.id"
            />
          </el-select>
        </el-form-item>

        <!-- 用户选择（仅管理员使用） -->
        <el-form-item v-if="isAdmin && !isMyPage" label="用户" prop="userId">
          <el-select
              v-model="formData.userId"
              placeholder="请选择用户"
              filterable
          >
            <el-option
                v-for="user in userOptions"
                :key="user.id"
                :label="user.username + (user.realName ? ' (' + user.realName + ')' : '')"
                :value="user.id"
            />
          </el-select>
        </el-form-item>

        <!-- 预约日期 -->
        <el-form-item label="预约日期" prop="bookingDate">
          <el-date-picker
              v-model="formData.bookingDate"
              type="date"
              placeholder="选择日期"
              :disabled-date="disabledDate"
              value-format="YYYY-MM-DD"
              @change="onBookingDateChange"
          />
        </el-form-item>

        <!-- 预约时段：以按钮形式选择，每个按钮代表 1 小时，可连续选择最多 2 个 -->
        <el-form-item label="预约时段">
          <div class="slot-grid" v-if="formData.courtId && formData.bookingDate">
            <button
                v-for="opt in slotOptions"
                :key="opt.value"
                type="button"
                class="slot-btn"
                :class="{ 'slot-btn--selected': selectedSlots.includes(opt.value), 'slot-btn--disabled': slotDisabled }"
                :disabled="slotDisabled"
                @click="toggleSlot(opt.value)"
            >
              {{ opt.label }}
            </button>
          </div>
          <div v-else class="slot-cap-hint">请先选择场地和日期，再选择预约时段。</div>
          <div v-if="slotCapHint" class="slot-cap-hint">{{ slotCapHint }}</div>
        </el-form-item>

        <!-- 联系人 -->
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="formData.contactName" placeholder="请输入联系人姓名" />
        </el-form-item>

        <!-- 联系电话 -->
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="formData.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>

        <!-- 备注 -->
        <el-form-item label="备注" prop="remark">
          <el-input
              v-model="formData.remark"
              type="textarea"
              :rows="3"
              placeholder="请输入备注信息"
          />
        </el-form-item>

        <!-- 优惠券（仅新增） -->
        <el-form-item v-if="!currentEditBooking" label="优惠券">
          <el-select
              v-model="formData.couponCode"
              filterable
              clearable
              allow-create
              default-first-option
              placeholder="可选：从列表选择或输入券码"
              style="width: 100%"
          >
            <el-option
                v-for="c in unusedCoupons"
                :key="c.couponCode"
                :label="`${c.activityTitle || '优惠券'} ¥${formatCouponFace(c.discountAmount)}`"
                :value="c.couponCode"
            />
          </el-select>
          <p v-if="unusedCouponsHint" class="slot-cap-hint">{{ unusedCouponsHint }}</p>
        </el-form-item>

        <!-- 状态（编辑时显示） -->
        <el-form-item v-if="currentEditBooking" label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :value="0">已取消</el-radio>
            <el-radio :value="1">待付款</el-radio>
            <el-radio :value="2">已付款</el-radio>
            <el-radio :value="3">已完成</el-radio>
            <el-radio :value="4">已过期</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 显示金额信息 -->
        <el-form-item label="金额信息" v-if="formData.courtId && selectedSlots.length">
          <div class="amount-info">
            <p>单价：¥{{ unitPrice }}</p>
            <p>时长：{{ duration }} 小时</p>
            <p class="total-amount">原价：¥{{ totalAmount }}</p>
            <template v-if="!currentEditBooking && formData.couponCode">
              <p v-if="couponEstimateDiscount != null">抵扣：¥{{ couponEstimateDiscount.toFixed(2) }}</p>
              <p v-else class="slot-cap-hint">抵扣金额以下单结果为准（手动输入券码时无法预估）</p>
              <p class="total-amount">预估应付：¥{{ estimatedPayAmount }}</p>
            </template>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="formVisible = false">取消</el-button>
          <el-button type="primary" @click="handleFormConfirm" :loading="submitting">
            {{ submitting ? '提交中...' : '确认' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { getAllBookings, getMyBookings, searchBookings, addBooking, updateBooking,
  deleteBookingById, batchDeleteBookings, cancelBooking,
  completeBooking, updateBookingStatus, getCourtSlotOptions,
  requestCancelBooking, getPendingCancelBookings,
  adminApproveCancelBooking, adminRejectCancelBooking,
  claimPaidBooking, getPendingPaymentVerifyBookings,
  adminApprovePaymentVerifyBooking, adminRejectPaymentVerifyBooking,
  createStripeCheckoutSession } from '@/api/booking'
import { getAllCourts } from '@/api/court'
import { getUserList } from '@/api/user'
import { getMyUnusedCoupons } from '@/api/coupon'

const route = useRoute()

// 数据
const bookings = ref([])
const filteredBookings = ref([])
const loading = ref(true)
const error = ref(null)
const submitting = ref(false)

// 当前用户与角色
const currentUser = ref(null)
const isAdmin = computed(() => {
  const r = currentUser.value?.role
  return r != null && Number(r) === 2
})
const isMyPage = computed(() => route.name === 'MyBookings')
const showBatchActions = computed(() => isAdmin.value && !isMyPage.value)

// 搜索表单
const searchForm = reactive({
  keyword: '',
  courtId: null,
  userId: null,
  status: null,
  startDate: null,
  endDate: null
})
const dateRange = ref([])

// 选项数据
const courtOptions = ref([])
const userOptions = ref([])

// 选中项
const selectedBookings = ref([])

// 详情对话框
const detailVisible = ref(false)
const currentBooking = ref(null)

// 表单对话框
const formVisible = ref(false)
const formTitle = ref('')
const currentEditBooking = ref(null)
const formRef = ref(null)

// 表单数据
const formData = reactive({
  courtId: null,
  userId: null,
  bookingDate: '',
  contactName: '',
  contactPhone: '',
  remark: '',
  status: 1,
  couponCode: ''
})

const unusedCoupons = ref([])
const unusedCouponsHint = ref('')

const formatCouponFace = (v) => {
  if (v == null || v === '') return '-'
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(2) : String(v)
}

/** 列表/详情/付款弹窗：应付金额（无券或未迁移时退回原价） */
const displayPayAmount = (row) => {
  if (!row) return ''
  const p = row.payAmount
  if (p != null && p !== '') return p
  return row.totalAmount
}

const hasCouponDiscount = (row) => {
  if (!row) return false
  const d = Number(row.couponDiscount)
  return Number.isFinite(d) && d > 0
}

const slotOptions = ref([])
const slotLoading = ref(false)
const pendingCancels = ref([])
const pendingPaymentVerifies = ref([])

const payDialogVisible = ref(false)
const payTarget = ref(null)
const payChannel = ref('wechat')
const payQrDialogVisible = ref(false)
const claimPaidSubmitting = ref(false)
const stripeCheckoutLoading = ref(false)
const payQrImageMap = {
  wechat: '/pay/wechat.png',
  alipay: '/pay/alipay.png',
  xianyu: '/pay/xianyu.png'
}

const slotMeta = ref({
  usedHours: 0,
  maxDailyHours: 2,
  remainingHours: 2,
  dayFull: false,
  courtClosed: false,
  openTimeDisplay: ''
})

// 已选时段（value 形如 "HH:mm|HH:mm"），最多 2 个
const selectedSlots = ref([])

/** 点选时段：再次点击可取消；第二段必须与已选相邻 */
const toggleSlot = (value) => {
  if (slotDisabled.value || !value) return
  const i = selectedSlots.value.indexOf(value)
  if (i >= 0) {
    selectedSlots.value.splice(i, 1)
    return
  }
  if (selectedSlots.value.length >= 2) {
    ElMessage.warning('最多选择两个连续时段')
    return
  }
  if (selectedSlots.value.length === 0) {
    selectedSlots.value.push(value)
    return
  }
  const only = selectedSlots.value[0]
  const [a0, b0] = only.split('|')
  const [a1, b1] = value.split('|')
  const adjacent = b0 === a1 || b1 === a0
  if (adjacent) {
    selectedSlots.value.push(value)
  } else {
    ElMessage.warning('请选择与已选时段紧挨着的时段（例如已选 08:30-09:30，再选 09:30-10:30）')
  }
}

// 场地单价缓存
const courtPriceMap = ref(new Map())

// 表单验证规则
const rules = {
  courtId: [
    { required: true, message: '请选择场地', trigger: 'change' }
  ],
  userId: [
    { required: true, message: '请选择用户', trigger: 'change' }
  ],
  bookingDate: [
    { required: true, message: '请选择预约日期', trigger: 'change' }
  ],
  contactName: [
    { required: true, message: '请输入联系人姓名', trigger: 'blur' }
  ],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

// 只看自己的预订
const applyMyFilter = () => {
  if (!isMyPage.value && isAdmin.value) {
    filteredBookings.value = [...bookings.value]
    return
  }
  const uid = currentUser.value?.id
  if (!uid) {
    filteredBookings.value = []
    return
  }
  filteredBookings.value = bookings.value.filter(b => b.userId === uid)
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

const toHM = (t) => {
  if (t == null || t === '') return ''
  const s = String(t)
  return s.length >= 5 ? s.substring(0, 5) : s
}

const hoursBetweenHM = (a, b) => {
  const [ah, am] = a.split(':').map(Number)
  const [bh, bm] = b.split(':').map(Number)
  return ((bh * 60 + bm) - (ah * 60 + am)) / 60
}

// 计算时长（小时，与后端计费一致）
const duration = computed(() => {
  if (!selectedSlots.value.length) return '0'
  const slots = selectedSlots.value
    .map(v => v && v.includes('|') ? v.split('|') : null)
    .filter(Boolean)
    .map(([s, e]) => ({ start: s, end: e }))
  if (!slots.length) return '0'
  // 总时长为所有选择时段之和
  let total = 0
  slots.forEach(s => {
    total += hoursBetweenHM(s.start, s.end)
  })
  return total > 0 ? total.toFixed(2) : '0'
})

// 计算单价
const unitPrice = computed(() => {
  if (formData.courtId) {
    return courtPriceMap.value.get(formData.courtId) || 0
  }
  return 0
})

// 计算总金额
const totalAmount = computed(() => {
  const d = parseFloat(duration.value) || 0
  return (unitPrice.value * d).toFixed(2)
})

/** 从「我的未使用券」选中时可预估抵扣；手动输入券码则为 null */
const couponEstimateDiscount = computed(() => {
  if (!formData.couponCode) return null
  const total = parseFloat(totalAmount.value) || 0
  const c = unusedCoupons.value.find(x => x.couponCode === formData.couponCode)
  if (!c) return null
  const face = Number(c.discountAmount)
  if (!Number.isFinite(face)) return null
  return Math.min(face, total)
})

const estimatedPayAmount = computed(() => {
  const total = parseFloat(totalAmount.value) || 0
  const d = couponEstimateDiscount.value
  if (d == null) return total.toFixed(2)
  return Math.max(0, total - d).toFixed(2)
})

const slotCapHint = computed(() => {
  if (!formData.courtId || !formData.bookingDate) return ''
  const m = slotMeta.value
  if (m.courtClosed) {
    return '该场地当前未营业，无法预约。'
  }
  if (m.dayFull && !currentEditBooking.value) {
    return '该日该场地可预约总时长已满（所有用户合计最多 2 小时），请选择其他日期。'
  }
  if (m.openTimeDisplay) {
    return `营业时间：${m.openTimeDisplay}；当日已用 ${Number(m.usedHours || 0).toFixed(2)} / ${Number(m.maxDailyHours || 2).toFixed(0)} 小时，剩余约 ${Number(m.remainingHours || 0).toFixed(2)} 小时。`
  }
  return ''
})

const slotDisabled = computed(() => {
  const m = slotMeta.value
  return m.courtClosed || (m.dayFull && !currentEditBooking.value)
})

const resetSlotMeta = () => {
  slotMeta.value = {
    usedHours: 0,
    maxDailyHours: 2,
    remainingHours: 2,
    dayFull: false,
    courtClosed: false,
    openTimeDisplay: ''
  }
}

const loadSlotOptions = async () => {
  if (!formData.courtId || !formData.bookingDate) {
    slotOptions.value = []
    return
  }
  resetSlotMeta()
  slotLoading.value = true
  try {
    const res = await getCourtSlotOptions({
      courtId: formData.courtId,
      date: formData.bookingDate,
      excludeId: currentEditBooking.value?.id || undefined
    })
    if (res.code === 200 && res.data) {
      const d = res.data
      slotMeta.value = {
        usedHours: d.usedHours,
        maxDailyHours: d.maxDailyHours,
        remainingHours: d.remainingHours,
        dayFull: d.dayFull,
        courtClosed: !!d.courtClosed,
        openTimeDisplay: d.openTimeDisplay || ''
      }
      slotOptions.value = [...(d.options || [])]
      if (d.dayFull && !currentEditBooking.value) {
        selectedSlots.value = []
      }
    } else {
      slotOptions.value = []
    }
  } catch (e) {
    console.error(e)
    slotOptions.value = []
  } finally {
    slotLoading.value = false
  }
}

watch(
  () => [formVisible.value, formData.courtId, formData.bookingDate],
  () => {
    if (formVisible.value && formData.courtId && formData.bookingDate) {
      loadSlotOptions()
    }
  }
)

const loadPendingCancels = async () => {
  if (!isAdmin.value || isMyPage.value) return
  try {
    const res = await getPendingCancelBookings()
    if (res.code === 200 && res.data) {
      pendingCancels.value = res.data
    } else {
      pendingCancels.value = []
    }
  } catch (e) {
    pendingCancels.value = []
  }
}

const loadPendingPaymentVerifies = async () => {
  if (!isAdmin.value || isMyPage.value) return
  try {
    const res = await getPendingPaymentVerifyBookings()
    if (res.code === 200 && res.data) {
      pendingPaymentVerifies.value = res.data
    } else {
      pendingPaymentVerifies.value = []
    }
  } catch (e) {
    pendingPaymentVerifies.value = []
  }
}

const handleUserRequestCancel = (row) => {
  ElMessageBox.confirm(
    '提交后需管理员审核通过，预约才会取消。确定申请取消？',
    '申请取消',
    { type: 'warning' }
  ).then(async () => {
    try {
      const res = await requestCancelBooking(row.id)
      if (res.code === 200) {
        ElMessage.success(res.message || '已提交申请')
        await loadBookings()
      } else {
        ElMessage.error(res.message || '提交失败')
      }
    } catch (e) {
      ElMessage.error(e.message || '提交失败')
    }
  }).catch(() => {})
}

const openPayDialog = (row) => {
  payTarget.value = row
  payChannel.value = 'wechat'
  payDialogVisible.value = true
}

const confirmPay = async () => {
  if (!payTarget.value) return
  if (payChannel.value === 'stripe') {
    stripeCheckoutLoading.value = true
    try {
      const res = await createStripeCheckoutSession(payTarget.value.id)
      if (res.code === 200 && res.data?.url) {
        payDialogVisible.value = false
        window.location.href = res.data.url
      } else {
        ElMessage.error(res?.message || '无法创建 Stripe 支付')
      }
    } catch (e) {
      ElMessage.error(e.message || 'Stripe 支付失败')
    } finally {
      stripeCheckoutLoading.value = false
    }
    return
  }
  payDialogVisible.value = false
  payQrDialogVisible.value = true
}

/** 仅关闭选渠道弹窗（未进入收款码）时清空订单；若已点「去付款」会先打开收款码，此处不应清空 */
const onPayMethodDialogClose = () => {
  if (!payQrDialogVisible.value) {
    payTarget.value = null
  }
}

const handleClaimPaid = async () => {
  if (!payTarget.value) {
    ElMessage.warning('订单信息丢失，请关闭后重新点「付款」')
    return
  }
  claimPaidSubmitting.value = true
  try {
    const res = await claimPaidBooking(payTarget.value.id, { channel: payChannel.value })
    if (res.code === 200) {
      ElMessage.success(res.message || '已提交，请等待管理员确认')
      payQrDialogVisible.value = false
      payTarget.value = null
      await loadBookings()
    } else {
      ElMessage.error(res.message || '提交失败')
    }
  } catch (e) {
    ElMessage.error(e.message || '提交失败')
  } finally {
    claimPaidSubmitting.value = false
  }
}

const handleApprovePaymentVerify = (row) => {
  ElMessageBox.confirm('确认已收到该笔款项？订单将变为已付款。', '确认收款', { type: 'warning' })
    .then(async () => {
      const res = await adminApprovePaymentVerifyBooking(row.id)
      if (res.code === 200) {
        ElMessage.success(res.message || '已处理')
        await loadBookings()
        await loadPendingPaymentVerifies()
      } else {
        ElMessage.error(res.message || '操作失败')
      }
    }).catch(() => {})
}

const handleRejectPaymentVerify = (row) => {
  ElMessageBox.confirm('驳回后用户可再次扫码提交「我已支付」，确定？', '驳回', { type: 'info' })
    .then(async () => {
      const res = await adminRejectPaymentVerifyBooking(row.id)
      if (res.code === 200) {
        ElMessage.success(res.message || '已驳回')
        await loadBookings()
        await loadPendingPaymentVerifies()
      } else {
        ElMessage.error(res.message || '操作失败')
      }
    }).catch(() => {})
}

const handleApproveCancel = (row) => {
  ElMessageBox.confirm('通过后该预约将取消，确定？', '通过取消申请', { type: 'warning' })
    .then(async () => {
      const res = await adminApproveCancelBooking(row.id)
      if (res.code === 200) {
        ElMessage.success(res.message || '已处理')
        await loadBookings()
        await loadPendingCancels()
      } else {
        ElMessage.error(res.message || '操作失败')
      }
    }).catch(() => {})
}

const handleRejectCancel = (row) => {
  ElMessageBox.confirm('驳回后用户可继续付款或再次申请取消，确定？', '驳回', { type: 'info' })
    .then(async () => {
      const res = await adminRejectCancelBooking(row.id)
      if (res.code === 200) {
        ElMessage.success(res.message || '已驳回')
        await loadBookings()
        await loadPendingCancels()
      } else {
        ElMessage.error(res.message || '操作失败')
      }
    }).catch(() => {})
}

const payChannelLabel = (c) => {
  const m = { wechat: '微信', alipay: '支付宝', xianyu: '闲鱼', stripe: 'Stripe' }
  return m[c] || c
}

// 加载预约列表
const loadBookings = async () => {
  console.log('🚀 开始加载预约数据...')
  loading.value = true
  error.value = null

  try {
    // 管理员：用 /bookings/search（无筛选=全量）；普通用户/我的预订：/bookings/mine（与首页 my-count 同源鉴权）
    const result =
      isAdmin.value && !isMyPage.value
        ? await getAllBookings()
        : await getMyBookings()
    console.log('✅ 预约数据返回:', result)

    if (result && result.code === 200) {
      bookings.value = result.data || []
      applyMyFilter()
      await loadPendingCancels()
      await loadPendingPaymentVerifies()
    } else {
      filteredBookings.value = []
      error.value = result?.message || '加载失败'
    }
  } catch (err) {
    console.error('❌ 加载预约列表失败:', err)
    error.value = err.message || '加载失败，请稍后重试'
    filteredBookings.value = []
  } finally {
    loading.value = false
  }
}

// 加载场地选项
const loadCourtOptions = async () => {
  try {
    const result = await getAllCourts()
    if (result && result.code === 200) {
      courtOptions.value = result.data || []
      // 建立价格映射
      courtPriceMap.value.clear()
      courtOptions.value.forEach(court => {
        courtPriceMap.value.set(court.id, court.price)
      })
    }
  } catch (err) {
    console.error('加载场地列表失败:', err)
  }
}

// 加载用户选项
const loadUserOptions = async () => {
  try {
    if (!isAdmin.value) {
      userOptions.value = []
      return
    }
    const result = await getUserList()
    if (result && result.code === 200) {
      userOptions.value = result.data || []
    } else if (Array.isArray(result)) {
      userOptions.value = result
    }
  } catch (err) {
    console.error('加载用户列表失败:', err)
  }
}

// 处理搜索
const handleSearch = async () => {
  console.log('执行搜索:', searchForm)

  // 如果有搜索条件，调用搜索API
  if (searchForm.keyword || searchForm.courtId || searchForm.userId ||
      searchForm.status || searchForm.startDate || searchForm.endDate) {
    try {
      loading.value = true
      // 普通用户或“我的预订”页面，强制只查当前用户
      if (!isAdmin.value || isMyPage.value) {
        const uid = currentUser.value?.id
        if (uid) {
          searchForm.userId = uid
        }
      }
      const result = await searchBookings(searchForm)
      if (result && result.code === 200) {
        bookings.value = result.data || []
        applyMyFilter()
        await loadPendingCancels()
        await loadPendingPaymentVerifies()
      }
    } catch (err) {
      console.error('搜索失败:', err)
      ElMessage.error('搜索失败')
    } finally {
      loading.value = false
    }
  } else {
    // 无搜索条件，显示全部
    filteredBookings.value = [...bookings.value]
  }
}

// 重置搜索
const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.courtId = null
  searchForm.userId = null
  searchForm.status = null
  searchForm.startDate = null
  searchForm.endDate = null
  dateRange.value = []
  applyMyFilter()
}

// 处理日期范围变化
const handleDateChange = (val) => {
  if (val && val.length === 2) {
    searchForm.startDate = val[0]
    searchForm.endDate = val[1]
  } else {
    searchForm.startDate = null
    searchForm.endDate = null
  }
}

// 处理选中变化
const handleSelectionChange = (selection) => {
  selectedBookings.value = selection.map(item => item.id)
}

// 查看详情
const handleViewBooking = (row) => {
  currentBooking.value = row
  detailVisible.value = true
}

const loadMyUnusedCoupons = async () => {
  unusedCouponsHint.value = ''
  unusedCoupons.value = []
  if (!currentUser.value?.id) return
  try {
    const res = await getMyUnusedCoupons()
    if (res.code === 200 && Array.isArray(res.data)) {
      unusedCoupons.value = res.data
    }
  } catch (e) {
    unusedCouponsHint.value = '未使用券列表加载失败，仍可手动输入券码下单'
  }
}

// 新增预约（可选带入场地ID）
const handleAddBooking = async (courtId = null) => {
  currentEditBooking.value = null
  formTitle.value = '新增预约'
  resetForm()
  // 普通用户：自动绑定为自己
  if (!isAdmin.value || isMyPage.value) {
    formData.userId = currentUser.value?.id || null
    // 默认联系人信息使用当前用户
    formData.contactName = currentUser.value?.realName || currentUser.value?.username || ''
    formData.contactPhone = currentUser.value?.phone || ''
  }
  if (courtId) {
    formData.courtId = courtId
  }
  selectedSlots.value = []
  formVisible.value = true
  await loadMyUnusedCoupons()
}

/**
 * 从首页「热门场地」等链接携带 ?courtId= 进入时：打开新建预约弹窗并去掉 query，避免重复弹出、刷新再次打开。
 */
const openBookingFromCourtQuery = async () => {
  const raw = route.query.courtId
  if (raw == null || raw === '') return
  const courtId = Number(raw)
  if (!Number.isFinite(courtId) || courtId <= 0) return
  if (route.name !== 'Bookings' && route.name !== 'MyBookings') return
  await handleAddBooking(courtId)
  const q = { ...route.query }
  delete q.courtId
  await router.replace({ path: route.path, query: q })
}

// 编辑预约
const handleEditBooking = (row) => {
  currentEditBooking.value = { ...row }
  formTitle.value = '编辑预约'

  // 填充表单数据
  formData.courtId = row.courtId
  formData.userId = row.userId
  formData.bookingDate = row.bookingDate
  formData.contactName = row.contactName
  formData.contactPhone = row.contactPhone
  formData.remark = row.remark || ''
  formData.status = row.status
  selectedSlots.value = [`${toHM(row.startTime)}|${toHM(row.endTime)}`]

  formVisible.value = true
}

// 处理状态下拉命令
const handleStatusCommand = async (command, row) => {
  try {
    if (command === 'cancel') {
      await ElMessageBox.confirm(`确定要取消预约 "${row.bookingNo}" 吗？`, '取消预约', {
        type: 'warning'
      })
      await cancelBooking(row.id)
      ElMessage.success('已取消预约')
    } else if (command === 'pay') {
      await updateBookingStatus(row.id, 2)
      ElMessage.success('已确认付款')
    } else if (command === 'complete') {
      await updateBookingStatus(row.id, 3)
      ElMessage.success('已完成预约')
    }
    await loadBookings()
  } catch (err) {
    if (err !== 'cancel') {
      console.error('操作失败:', err)
      ElMessage.error('操作失败')
    }
  }
}

// 删除预约
const handleDeleteBooking = (row) => {
  ElMessageBox.confirm(
      `确定要删除预约 "${row.bookingNo}" 吗？此操作不可恢复！`,
      '删除预约',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
  ).then(async () => {
    try {
      await deleteBookingById(row.id)
      ElMessage.success('删除成功')
      await loadBookings()
    } catch (err) {
      console.error('删除失败:', err)
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

// 批量删除
const handleBatchDelete = () => {
  if (selectedBookings.value.length === 0) return

  ElMessageBox.confirm(
      `确定要删除选中的 ${selectedBookings.value.length} 个预约吗？此操作不可恢复！`,
      '批量删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
  ).then(async () => {
    try {
      await batchDeleteBookings(selectedBookings.value)
      ElMessage.success('批量删除成功')
      selectedBookings.value = []
      await loadBookings()
    } catch (err) {
      console.error('批量删除失败:', err)
      ElMessage.error('批量删除失败')
    }
  }).catch(() => {})
}

// 场地变更处理
const handleCourtChange = () => {
  selectedSlots.value = []
  resetSlotMeta()
}

const onBookingDateChange = () => {
  selectedSlots.value = []
  resetSlotMeta()
}

// 禁用日期（不能选择过去的日期）
const disabledDate = (date) => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date < today
}

// 表单确认
const handleFormConfirm = async () => {
  if (!formRef.value) return

  // 普通用户 / 我的预订 页面，强制将 userId 绑定为自己
  if (!isAdmin.value || isMyPage.value) {
    formData.userId = currentUser.value?.id || null
  }

  await formRef.value.validate(async (valid) => {
    if (valid) {
      if (!selectedSlots.value.length) {
        ElMessage.warning('请选择至少一个预约时段')
        return
      }

      // 最多两个时段
      if (selectedSlots.value.length > 2) {
        ElMessage.warning('一次最多选择两个连续时段')
        return
      }

      const slots = selectedSlots.value
        .map(v => v && v.includes('|') ? v.split('|') : null)
        .filter(Boolean)
        .map(([s, e]) => ({ start: s, end: e }))
        .sort((a, b) => a.start.localeCompare(b.start))

      if (!slots.length) {
        ElMessage.warning('请选择有效的预约时段')
        return
      }

      // 若选择两个，必须连续：前一段的结束时间 == 下一段的开始时间
      if (slots.length === 2 && slots[0].end !== slots[1].start) {
        ElMessage.warning('两个时段必须是连续的，例如 08:30-09:30 和 09:30-10:30')
        return
      }

      // 总时长不能超过 2 小时
      const totalHours = parseFloat(duration.value) || 0
      if (totalHours > 2) {
        ElMessage.warning('同一场地同一天单次预约最多 2 小时')
        return
      }

      const st = slots[0].start
      const et = slots[slots.length - 1].end

      try {
        submitting.value = true

        // 准备提交数据
        const submitData = {
          courtId: formData.courtId,
          userId: formData.userId,
          bookingDate: formData.bookingDate,
          startTime: st,
          endTime: et,
          contactName: formData.contactName,
          contactPhone: formData.contactPhone,
          remark: formData.remark || '',
          status: formData.status
        }
        if (!currentEditBooking.value) {
          const cc = formData.couponCode && String(formData.couponCode).trim()
          if (cc) submitData.couponCode = cc
        }

        console.log('提交数据:', submitData)

        let result
        if (currentEditBooking.value) {
          result = await updateBooking(currentEditBooking.value.id, submitData)
        } else {
          result = await addBooking(submitData)
        }

        console.log('操作结果:', result)

        if (result && result.code === 200) {
          ElMessage.success(currentEditBooking.value ? '编辑成功' : '新增成功')
          formVisible.value = false
          await loadBookings()
        } else {
          ElMessage.error(result?.message || (currentEditBooking.value ? '编辑失败' : '新增失败'))
        }
      } catch (err) {
        console.error('操作失败:', err)
        ElMessage.error(err.message || (currentEditBooking.value ? '编辑失败' : '新增失败'))
      } finally {
        submitting.value = false
      }
    }
  })
}

// 表单关闭
const handleFormClose = () => {
  resetForm()
  currentEditBooking.value = null
}

// 重置表单
const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
  formData.courtId = null
  formData.userId = null
  formData.bookingDate = ''
  selectedSlots.value = []
  formData.contactName = ''
  formData.contactPhone = ''
  formData.remark = ''
  formData.status = 1
  formData.couponCode = ''
  unusedCouponsHint.value = ''
  slotOptions.value = []
}

// 获取状态标签类型
const getStatusType = (status) => {
  const types = {
    0: 'danger',
    1: 'warning',
    2: 'success',
    3: 'info',
    4: 'danger'
  }
  return types[status] || 'info'
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  return dateTime.replace('T', ' ').substring(0, 16)
}

// 初始化
onMounted(async () => {
  loadCurrentUser()
  await Promise.all([
    loadBookings(),
    loadCourtOptions(),
    loadUserOptions()
  ])
  await loadPendingCancels()
  await loadPendingPaymentVerifies()

  await nextTick()
  await openBookingFromCourtQuery()

  if (route.query.stripe === 'success') {
    ElMessage.success('支付成功，订单状态将更新为已付款')
    await loadBookings()
  } else if (route.query.stripe === 'cancel') {
    ElMessage.info('已取消支付')
  }
})

// 已在预约页时，从首页再次点击热门场地（仅 query 变化、组件复用）也要打开弹窗
watch(
  () => route.query.courtId,
  (cid) => {
    if (cid == null || cid === '') return
    openBookingFromCourtQuery()
  }
)
</script>

<style scoped>
.booking-container {
  padding: 24px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.page-header {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f0f2f5;
}

.page-title {
  font-size: 24px;
  color: #333;
  margin: 0;
}

.search-section {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.search-form {
  max-width: 100%;
}

.pending-payment-panel {
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 20px;
}

.pending-payment-title {
  margin: 0 0 12px;
  font-size: 16px;
  color: #1e40af;
}

.pending-cancel-panel {
  background: #fff7ed;
  border: 1px solid #fed7aa;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 20px;
}

.pending-cancel-title {
  margin: 0 0 12px;
  font-size: 16px;
  color: #9a3412;
}

.pay-dialog-body {
  padding: 8px 0;
}

.pay-order-info {
  margin-bottom: 16px;
  font-size: 15px;
}

.pay-channel-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}

.pay-channel-group :deep(.el-radio) {
  margin-right: 0;
  width: 100%;
}

.pay-tip {
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}

.pay-qr-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 6px 0 2px;
}

.pay-qr-image {
  width: 320px;
  max-width: 100%;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
}

.action-bar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 20px;
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

.price {
  color: #f59e0b;
  font-weight: 600;
}

.booking-amount-cell {
  font-size: 12px;
  line-height: 1.5;
  text-align: right;
}

.amount-discount {
  color: var(--el-color-success);
}

.detail-container {
  padding: 20px;
}

.amount-info {
  background: #f8f9fa;
  padding: 12px;
  border-radius: 6px;
  font-size: 14px;
}

.amount-info p {
  margin: 4px 0;
}

.total-amount {
  font-size: 16px;
  font-weight: 600;
  color: #f59e0b;
}

.slot-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.slot-btn {
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  font-size: 14px;
  cursor: pointer;
  text-align: center;
  transition: all 0.2s;
}

.slot-btn:hover {
  border-color: #667eea;
  box-shadow: 0 0 0 1px rgba(102, 126, 234, 0.15);
}

.slot-btn--selected {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  border-color: transparent;
  box-shadow: 0 2px 6px rgba(102, 126, 234, 0.4);
}

.slot-btn--disabled {
  cursor: not-allowed;
  opacity: 0.6;
  box-shadow: none;
}

.slot-cap-hint {
  color: #606266;
  font-size: 12px;
  margin-top: 8px;
  line-height: 1.5;
}

.booking-form {
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

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 响应式 */
@media (max-width: 768px) {
  .booking-container {
    padding: 16px;
  }

  .search-section {
    padding: 12px;
  }

  :deep(.el-dialog) {
    width: 90% !important;
    margin: 10% auto !important;
  }
}
</style>