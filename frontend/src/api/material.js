import request from './request'

export function searchMaterial(keyword, page = 1, size = 20, filters = {}) {
  const params = { keyword, page, size }
  if (filters.hasDrawing !== undefined && filters.hasDrawing !== null && filters.hasDrawing !== '') params.hasDrawing = filters.hasDrawing
  if (filters.has3d !== undefined && filters.has3d !== null && filters.has3d !== '') params.has3d = filters.has3d
  if (filters.hasEngineering !== undefined && filters.hasEngineering !== null && filters.hasEngineering !== '') params.hasEngineering = filters.hasEngineering
  return request.get('/material/search', { params })
}

export function importMaterial(formData) {
  return request.post('/material/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function addMaterial(data) {
  return request.post('/material', data)
}

export function updateMaterial(data) {
  return request.put('/material', data)
}

export function deleteMaterial(id) {
  return request.delete(`/material/${id}`)
}

export function getMaterialList(page = 1, size = 20, filters = {}) {
  const params = { page, size }
  if (filters.hasDrawing !== undefined && filters.hasDrawing !== null && filters.hasDrawing !== '') params.hasDrawing = filters.hasDrawing
  if (filters.has3d !== undefined && filters.has3d !== null && filters.has3d !== '') params.has3d = filters.has3d
  if (filters.hasEngineering !== undefined && filters.hasEngineering !== null && filters.hasEngineering !== '') params.hasEngineering = filters.hasEngineering
  return request.get('/material/list', { params })
}

// ===== 正航 T9 ERP 物料同步 =====

export function erpTestConnection() {
  return request.post('/material/erp/test', null, { timeout: 120000 })
}

export function erpSyncMaterial(condition = '') {
  return request.post('/material/erp/sync', { condition }, { timeout: 180000 })
}

export function getErpSyncStatus() {
  return request.get('/material/erp/status')
}

/** 手动同步某个物料的“是否存在图纸”标记到 ERP（CU_HaveDrawing） */
export function erpSyncDrawingFlag(materialCode) {
  return request.post('/material/erp/sync-drawing-flag', { materialCode }, { timeout: 120000 })
}
