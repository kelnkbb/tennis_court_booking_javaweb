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
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  if (to.matched.some(record => record.meta.requiresAuth)) {
    // 需要登录的页面
    if (!token) {
      next('/login')
    } else {
      next()
    }
  } else {
    // 不需要登录的页面（如登录页）
    if (to.path === '/login' && token) {
      next('/home')
    } else {
      next()
    }
  }
})

export default router