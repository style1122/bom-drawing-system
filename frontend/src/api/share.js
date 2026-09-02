import request from './request'

/**
 * 创建分享链接（需登录）
 */
export function createShare(data) {
  return request.post('/share/create', data)
}

/**
 * 作废分享链接（需登录）
 */
export function invalidateShare(token) {
  return request.put(`/share/invalidate/${token}`)
}

/**
 * 获取分享页数据（公开接口，无需认证）
 * 使用原生 fetch 避免 axios 拦截器处理 401 跳转
 */
export function getShareData(token) {
  return fetch(`/api/share/public/${token}/data`).then(res => {
    if (!res.ok) throw new Error('分享链接无效或已过期')
    return res.json()
  })
}

/**
 * 获取分享图纸下载/预览 URL
 */
export function getShareDownloadUrl(token, drawingId, inline = false) {
  const params = inline ? '?inline=true' : ''
  return `/api/share/public/${token}/download/${drawingId}${params}`
}
