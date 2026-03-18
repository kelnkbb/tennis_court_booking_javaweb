import request from '@/utils/request'

/** 管理员：用户统计 */
export function getUserStats(params) {
  return request({
    url: '/admin/stats/users',
    method: 'get',
    params
  })
}

/** 管理员：场地统计 */
export function getCourtStats(params) {
  return request({
    url: '/admin/stats/courts',
    method: 'get',
    params
  })
}

