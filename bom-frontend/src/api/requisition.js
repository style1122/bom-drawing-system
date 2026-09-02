import request from './request'

/** 分页查询采购订单列表 */
export function getRequisitionList(page = 1, size = 20) {
  return request.get('/requisition/list', { params: { page, size } })
}

/** 搜索采购订单 */
export function searchRequisition(keyword, page = 1, size = 20) {
  return request.get('/requisition/search', { params: { keyword, page, size } })
}

/** 获取采购订单详情（含明细 + 图纸关联） */
export function getRequisitionDetail(id) {
  return request.get(`/requisition/${id}`)
}

/** 构造导出 Excel 的 URL（带 token） */
export function buildExportExcelUrl(id) {
  const token = localStorage.getItem('token')
  let url = `/api/requisition/export/excel/${id}`
  if (token) url += `?token=${encodeURIComponent(token)}`
  return url
}

/** 构造导出图纸 ZIP 的 URL（带 token） */
export function buildExportDrawingsUrl(id) {
  const token = localStorage.getItem('token')
  let url = `/api/requisition/export/drawings/${id}`
  if (token) url += `?token=${encodeURIComponent(token)}`
  return url
}

/** 构造图纸预览 URL（inline 模式，带 token） */
export function buildDrawingPreviewUrl(drawingId) {
  const token = localStorage.getItem('token')
  let url = `/api/drawing/download/${drawingId}?inline=true`
  if (token) url += `&token=${encodeURIComponent(token)}`
  return url
}
