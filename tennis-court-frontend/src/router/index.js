// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layout/MainLayout.vue'
import Home from '../views/Home.vue'
import CourtManagement from '../views/CourtManagement.vue'
import Bookings from '../views/Bookings.vue'
import UserManagement from '../views/UserManagement.vue'
import UserStats from '../views/UserStats.vue'
import CourtStats from '../views/CourtStats.vue'
import Login from '../views/Login.vue'
import AdminCouponActivities from '../views/AdminCouponActivities.vue'
import CouponSeckill from '../views/CouponSeckill.vue'



const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: MainLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/home'
      },
      {
        path: '/home',
        name: 'Home',
        component: Home
      },
      {
        path: '/courts',
        name: 'CourtManagement',
        component: CourtManagement
      },
      {
        path: '/bookings',
        name: 'Bookings',
        component: Bookings
      },
      {
        path: '/my-bookings',
        name: 'MyBookings',
        component: Bookings
      },
      {
        path: '/users',
        name: 'UserManagement',
        component: UserManagement
      },
      {
        path: '/stats/user',
        name: 'UserStats',
        component: UserStats
      },
      {
        path: '/stats/court',
        name: 'CourtStats',
        component: CourtStats
      },
      {
        path: '/admin/coupons',
        name: 'AdminCouponActivities',
        component: AdminCouponActivities,
        meta: { requiresAdmin: true }
      },
      {
        path: '/coupons',
        name: 'CouponSeckill',
        component: CouponSeckill,
        meta: { requiresAuth: false }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫（子路由 meta.requiresAuth === false 可覆盖父级，用于 /coupons 等半公开页）
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  if (to.path === '/login') {
    if (token) next('/home')
    else next()
    return
  }

  const leaf = to.matched[to.matched.length - 1]
  if (leaf?.meta?.requiresAuth === false) {
    next()
    return
  }

  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!token) {
      next('/login')
      return
    }
    if (to.matched.some(record => record.meta.requiresAdmin)) {
      const userStr = localStorage.getItem('user')
      let role = null
      try {
        role = userStr ? JSON.parse(userStr).role : null
      } catch {
        role = null
      }
      if (Number(role) !== 2) {
        next('/home')
        return
      }
    }
  }
  next()
})

export default router