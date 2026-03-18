// src/utils/request.js
import axios from 'axios'
import router from '@/router'
import { ElMessage } from 'element-plus'

const request = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
    timeout: 5000
  })

// 请求拦截器 - 添加token
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

// 响应拦截器 - 统一处理错误
request.interceptors.response.use(
    response => {
        console.log('📥 收到响应:', response.config.url, response.data)

        // 如果后端返回了code，可以根据code做统一处理
        if (response.data && response.data.code === 401) {
            // token过期或无效
            localStorage.removeItem('token')
            localStorage.removeItem('user')
            router.push('/login')
            ElMessage.error('登录已过期，请重新登录')
            return Promise.reject(new Error('登录已过期'))
        }

        return response.data
    },
    error => {
        console.error('📥 响应错误:', error.response || error)

        if (error.response) {
            switch (error.response.status) {
                case 401:
                    localStorage.removeItem('token')
                    localStorage.removeItem('user')
                    router.push('/login')
                    ElMessage.error('未授权，请重新登录')
                    break
                case 403:
                    ElMessage.error('没有权限访问')
                    break
                case 404:
                    ElMessage.error('请求的资源不存在')
                    break
                case 500:
                    ElMessage.error('服务器错误')
                    break
                default:
                    ElMessage.error(error.response.data?.message || '请求失败')
            }
        } else {
            ElMessage.error('网络错误，请检查连接')
        }

        return Promise.reject(error)
    }
)

export default request