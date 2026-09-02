import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  // 开发服务器（Vite 3000 端口）响应偏慢，放宽默认超时避免页面空白；
  // 生产环境静态部署后可改回 30000
  timeout: 90000
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  (response) => {
    // blob 类型不能解包 response.data（会破坏二进制数据）
    if (response.config.responseType === 'blob') {
      return response.data
    }
    return response.data
  },
  async (error) => {
    const status = error.response?.status
    let message = error.message || '请求失败'

    // 如果响应是 Blob（如下载预览出错），先解析成文本再取 message
    const data = error.response?.data
    if (data instanceof Blob) {
      try {
        const text = await data.text()
        const json = JSON.parse(text)
        message = json.msg || json.message || text
      } catch (e) {
        message = await data.text().catch(() => '请求失败')
      }
    } else {
      message = data?.msg || data?.message || message
    }

    if (status === 401) {
      localStorage.removeItem('token')
      ElMessage.error('登录已过期，请重新登录')
      router.push('/login')
    } else if (status === 403) {
      ElMessage.error(message || '权限不足')
      // 权限不足时跳转到用户首页
      const { useUserStore } = await import('@/store/user')
      const userStore = useUserStore()
      if (userStore.homePath) {
        router.push(userStore.homePath)
      }
    } else {
      ElMessage.error(message)
    }

    return Promise.reject(error)
  }
)

export default request
