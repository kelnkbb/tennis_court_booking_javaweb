// src/api/court.js
// src/api/court.js
import request from '@/utils/request'

// ... existing code ...


// 获取所有场馆
export function getAllCourts() {
    return request({
        url: '/courts',
        method: 'get'
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