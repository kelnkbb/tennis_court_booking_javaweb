// src/api/auth.js
import request from '@/utils/request'

// 登录
export function login(data) {
    return request({
        url: '/login',
        method: 'post',
        data
    })
}

// 注册
export function register(data) {
    return request({
        url: '/register',
        method: 'post',
        data
    })
}

// 退出登录
export function logout() {
    return request({
        url: '/logout',
        method: 'post'
    })
}

// 获取当前用户信息
export function getCurrentUser() {
    return request({
        url: '/user/current',
        method: 'get'
    })
}

// 修改密码
export function changePassword(data) {
    return request({
        url: '/user/password',
        method: 'put',
        data
    })
}