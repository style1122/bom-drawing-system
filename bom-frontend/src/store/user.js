import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, getCurrentUser } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)

  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => userInfo.value?.role || '')

  // 仪表盘模块：所有角色可访问
  const canAccessDashboard = computed(() => !!role.value)
  // 图纸管理模块：管理员、研发工程师和生产可访问
  const canAccessDrawings = computed(() => ['ADMIN', 'ENGINEER', 'PRODUCTION'].includes(role.value))
  // 采购订单管理模块：管理员、研发工程师和采购可访问
  const canAccessRequisitions = computed(() => ['ADMIN', 'ENGINEER', 'PURCHASER'].includes(role.value))
  // 默认首页路径（采购员跳转采购订单，其他跳转仪表盘）
  const homePath = computed(() => role.value === 'PURCHASER' ? '/requisitions' : '/dashboard')

  async function login(credentials) {
    const res = await loginApi(credentials)
    // 后端业务错误时 HTTP 状态码仍为 200，需检查 code
    if (res.code !== 200) {
      throw new Error(res.msg || '登录失败')
    }
    token.value = res.data.token
    localStorage.setItem('token', res.data.token)
    await fetchCurrentUser()
    return res
  }

  async function fetchCurrentUser() {
    try {
      const res = await getCurrentUser()
      userInfo.value = res.data
    } catch (error) {
      console.error('获取用户信息失败:', error)
    }
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    role,
    canAccessDashboard,
    canAccessDrawings,
    canAccessRequisitions,
    homePath,
    login,
    logout,
    fetchCurrentUser
  }
})
