// src/api/court.js
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 5000
})

// 请求拦截器 - 添加 token
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    console.log('📤 发送请求:', config.url, config.data || config.params)
    return config
  },
  error => {
    console.error('📤 请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    console.log('📥 收到响应:', response.config.url, response.data)
    return response.data
  },
  error => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
    }
    console.error('📥 响应错误:', error)
    return Promise.reject(error)
  }
)

// ... existing code ...


// 获取所有场馆
export function getAllCourts() {
    return request({
        url: '/courts',
        method: 'get'
    })
}

/** 热门场地 TopN（后端按访问热度 ZSet/Set） */
export function getHotCourts(topN = 5) {
    return request({
        url: '/courts/hot',
        method: 'get',
        params: { topN }
    })
}

// 根据 ID 获取场馆
export function getCourtById(id) {
    return request({
        url: `/courts/${id}`,
        method: 'get'
    })
}

// 根据 ID 删除场馆
export function deleteCourtById(id) {
    console.log('📤 deleteCourtById 被调用，接收到的 id =', id)
    return request({
        url: `/courts/${id}`,
        method: 'delete'
    })
}

// 添加场馆
export function addCourt(data) {
    return request({
        url: '/courts',
        method: 'post',
        data: data
    })
}

// 更新场馆
export function updateCourt(id, data) {
    console.log('📤 updateCourt 被调用，id =', id, 'data =', data)
    return request({
        url: `/courts/${id}`,
        method: 'put',
        data: data
    })
}