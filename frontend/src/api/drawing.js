import request from './request'

/** 上传图纸（单个文件，关联指定物料） */
export function uploadDrawing(formData) {
  return request.post('/drawing/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 批量上传图纸（多文件，根据文件名图号自动匹配物料） */
export function batchUploadDrawings(formData) {
  return request.post('/drawing/batch-upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 300000
  })
}

/** 以 ArrayBuffer 形式下载图纸，用于 vue-pdf-embed 预览 */
export function downloadDrawingArrayBuffer(id) {
  return request.get(`/drawing/download/${id}?inline=true`, {
    responseType: 'arraybuffer'
  })
}

/** 以 Blob 形式下载图纸，用于 PDF 预览（避免 token 在 URL 中暴露） */
export function downloadDrawingBlob(id) {
  return request.get(`/drawing/download/${id}?inline=true`, {
    responseType: 'blob'
  })
}

/** 获取物料关联的图纸列表 */
export function getDrawingsByMaterialId(materialId) {
  return request.get(`/drawing/material/${materialId}`)
}

/** 搜索图纸 */
export function searchDrawing(keyword) {
  return request.get('/drawing/search', { params: { keyword } })
}
