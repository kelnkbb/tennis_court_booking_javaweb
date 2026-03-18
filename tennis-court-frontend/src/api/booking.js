// src/api/booking.js
import request from '@/utils/request'  // 使用 @ 别名导入

// 获取所有预约（管理员列表，实际走条件查询接口，避免部分环境下 GET /bookings 返回 401）
export function getAllBookings() {
    return request({
        url: '/bookings/search',
        method: 'get',
        params: {}
    })
}

/** 当前登录用户自己的预约列表（后端按 JWT 过滤） */
export function getMyBookings() {
    return request({
        url: '/bookings/mine',
        method: 'get'
    })
}

// 条件搜索预约
export function searchBookings(params) {
    return request({
        url: '/bookings/search',
        method: 'get',
        params
    })
}

// 根据ID获取预约
export function getBookingById(id) {
    return request({
        url: `/bookings/${id}`,
        method: 'get'
    })
}

// 新增预约
export function addBooking(data) {
    return request({
        url: '/bookings',
        method: 'post',
        data
    })
}

// 更新预约
export function updateBooking(id, data) {
    return request({
        url: `/bookings/${id}`,
        method: 'put',
        data
    })
}

// 取消预约
export function cancelBooking(id) {
    return request({
        url: `/bookings/${id}/cancel`,
        method: 'put'
    })
}

// 完成预约
export function completeBooking(id) {
    return request({
        url: `/bookings/${id}/complete`,
        method: 'put'
    })
}

// 更新预约状态
export function updateBookingStatus(id, status) {
    return request({
        url: `/bookings/${id}/status`,
        method: 'put',
        params: { status }
    })
}

// 删除预约
export function deleteBookingById(id) {
    return request({
        url: `/bookings/${id}`,
        method: 'delete'
    })
}

// 批量删除预约
export function batchDeleteBookings(ids) {
    return request({
        url: '/bookings/batch',
        method: 'delete',
        data: ids
    })
}

// 检查时间段是否可用
export function checkTimeAvailable(params) {
    return request({
        url: '/bookings/check-time',
        method: 'get',
        params
    })
}

// 获取场地预约统计
export function getCourtBookingStats() {
    return request({
        url: '/bookings/stats/court',
        method: 'get'
    })
}

// 获取今日预约数量
export function getTodayBookingCount() {
    return request({
        url: '/bookings/today-count',
        method: 'get'
    })
}

// 获取当前登录用户的预约总数
export function getMyBookingCount() {
    return request({
        url: '/bookings/my-count',
        method: 'get'
    })
}

/** 某场地某日可选时段（整点 1h / 连续 2h），每日全用户合计最多 2 小时 */
export function getCourtSlotOptions(params) {
    return request({
        url: '/bookings/court-slot-options',
        method: 'get',
        params
    })
}

/** 用户：申请取消（待管理员审核） */
export function requestCancelBooking(id) {
    return request({
        url: `/bookings/${id}/request-cancel`,
        method: 'put'
    })
}

/** 用户：确认付款 channel: wechat | alipay | xianyu */
export function userPayBooking(id, data) {
    return request({
        url: `/bookings/${id}/pay`,
        method: 'post',
        data
    })
}

/** 管理员：待审核取消列表 */
export function getPendingCancelBookings() {
    return request({
        url: '/admin/bookings/pending-cancels',
        method: 'get'
    })
}

export function adminApproveCancelBooking(id) {
    return request({
        url: `/admin/bookings/${id}/cancel-request/approve`,
        method: 'put'
    })
}

export function adminRejectCancelBooking(id) {
    return request({
        url: `/admin/bookings/${id}/cancel-request/reject`,
        method: 'put'
    })
}