import request from '@/utils/request'

/** 管理员：秒杀活动列表（含草稿） */
export function listAdminCouponActivities() {
  return request({
    url: '/admin/coupon-activities',
    method: 'get'
  })
}

/** 管理员：创建活动（草稿） */
export function createCouponActivity(data) {
  return request({
    url: '/admin/coupon-activities',
    method: 'post',
    data
  })
}

/** 管理员：发布并初始化 Redis 库存 */
export function publishCouponActivity(id) {
  return request({
    url: `/admin/coupon-activities/${id}/publish`,
    method: 'put'
  })
}
