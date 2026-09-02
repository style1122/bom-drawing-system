import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/login/Register.vue'),
    meta: { requiresAuth: false }
  },
  // 图纸分享页面（公开访问，无需登录）
  {
    path: '/share/:token',
    name: 'ShareView',
    component: () => import('@/views/share/ShareView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    meta: { requiresAuth: true },
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'ENGINEER', 'PRODUCTION', 'PURCHASER'] }
      },
      // 图纸管理 — 研发工程师、生产和管理员
      {
        path: 'drawings',
        name: 'DrawingManage',
        component: () => import('@/views/drawing/DrawingManage.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'ENGINEER', 'PRODUCTION'] }
      },
      // 图纸版本管理 — 研发工程师、生产和管理员
      {
        path: 'drawings/:materialId/versions',
        name: 'DrawingVersions',
        component: () => import('@/views/drawing/DrawingVersions.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'ENGINEER', 'PRODUCTION'] }
      },
      // 采购订单管理 — 研发工程师、采购和管理员
      {
        path: 'requisitions',
        name: 'RequisitionManage',
        component: () => import('@/views/requisition/RequisitionManage.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'ENGINEER', 'PURCHASER'] }
      },
      // 采购订单详情
      {
        path: 'requisitions/:id',
        name: 'RequisitionDetail',
        component: () => import('@/views/requisition/RequisitionDetail.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN', 'ENGINEER', 'PURCHASER'] }
      },
      // 管理员
      {
        path: 'admin/users',
        name: 'UserManage',
        component: () => import('@/views/admin/UserManage.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN'] }
      },
      {
        path: 'admin/review',
        name: 'UserReview',
        component: () => import('@/views/admin/UserReview.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN'] }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const token = localStorage.getItem('token')
  const userStore = useUserStore()

  // 未登录拦截
  if (to.meta.requiresAuth !== false && !token) {
    next({ name: 'Login' })
    return
  }

  // 已登录访问登录页 → 跳转首页
  if (to.name === 'Login' && token) {
    next(userStore.homePath)
    return
  }

  // 已登录但用户信息未加载 → 先加载
  if (token && !userStore.userInfo) {
    // 用户信息接口慢时不让页面空白等待：5 秒超时后先进入页面（fetchCurrentUser 内部已捕获错误）
    await Promise.race([
      userStore.fetchCurrentUser(),
      new Promise(resolve => setTimeout(resolve, 5000))
    ])
  }

  // 角色权限检查
  if (to.meta.roles && userStore.userInfo) {
    const userRole = userStore.userInfo.role
    if (!to.meta.roles.includes(userRole)) {
      // 无权限 → 跳转到有权限的首页
      next(userStore.homePath)
      return
    }
  }

  next()
})

export default router
