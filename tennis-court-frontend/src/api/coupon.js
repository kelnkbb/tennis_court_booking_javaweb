import request from '@/utils/request'

/** 进行中的秒杀活动（可匿名） */
export function listCouponActivities() {
  return request({
    url: '/coupons/activities',
    method: 'get'
  })
}

/** 抢购入队（需登录），返回 grabId，需轮询 getGrabResult */
export function grabCoupon(activityId) {
  return request({
    url: `/coupons/seckill/${activityId}/grab`,
    method: 'post'
  })
}

/** 查询异步抢购结果（需登录） */
export function getGrabResult(grabId) {
  return request({
    url: `/coupons/seckill/result/${grabId}`,
    method: 'get',
    skipErrorToast: true
  })
}

/** 当前用户未使用的秒杀券（下单抵扣可选） */
export function getMyUnusedCoupons() {
  return request({
    url: '/coupons/my-unused',
    method: 'get'
  })
}
