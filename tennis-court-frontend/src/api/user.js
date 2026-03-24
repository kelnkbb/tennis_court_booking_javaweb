import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { getApiBaseUrl } from '@/config/api.js'

const request = axios.create({
  baseURL: getApiBaseUrl(),
  timeout: 15000
})

// 请求拦截器 - 添加 token
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
    }
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

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