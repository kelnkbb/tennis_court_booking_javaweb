import request from '@/utils/request'

// 获取用户列表
export function getUserList() {
  return request({
    url: '/users',
    method: 'get'
  })
}

// 新增用户
export function addUser(data) {
  console.log('📤 新增用户数据:', data)
  return request({
    url: '/users',
    method: 'post',
    data: data
  })
}