<template>
  <div class="versions-page">
    <div class="page-header">
      <el-button :icon="ArrowLeft" @click="goBack">返回图纸管理</el-button>
      <h2>版本管理</h2>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="8" animated />
    </div>

    <!-- 物料信息卡片 -->
    <el-card v-else class="info-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>物料信息</span>
          <el-tag :type="drawings.length > 0 ? 'success' : 'info'" size="small">
            {{ drawings.length > 0 ? `共 ${drawings.length} 个历史版本` : '暂无图纸' }}
          </el-tag>
        </div>
      </template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="物料编码">{{ material.materialCode }}</el-descriptions-item>
        <el-descriptions-item label="物料名称">{{ material.materialName }}</el-descriptions-item>
        <el-descriptions-item label="物料规格">{{ material.specification || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 图纸文件列表 -->
    <el-card v-if="!loading && drawings.length > 0" class="files-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>图纸文件列表（按上传时间倒序）</span>
        </div>
      </template>
      <el-table :data="drawings" border stripe size="small">
        <el-table-column type="index" label="序号" width="60" align="center" :index="indexMethod" />
        <el-table-column prop="drawingName" label="文件名" min-width="250" show-overflow-tooltip />
        <el-table-column prop="fileFormat" label="格式" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="formatTagType(row.fileFormat)">{{ row.fileFormat?.toUpperCase() }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="大小" width="110" align="center">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column label="上传时间" width="170" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.fileFormat?.toLowerCase() === 'pdf'"
              type="primary" link size="small"
              @click="previewPdf(row)"
            >预览</el-button>
            <el-button type="success" link size="small" @click="downloadFile(row)">下载</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-empty v-else-if="!loading" description="该物料尚未上传任何图纸" />

    <!-- PDF 预览弹窗 -->
    <el-dialog
      v-model="previewVisible"
      width="90%"
      top="2vh"
      :fullscreen="previewFullscreen"
      :close-on-click-modal="false"
      destroy-on-close
      class="pdf-preview-dialog"
    >
      <template #header>
        <div class="pdf-preview-header">
          <span>PDF 预览 - {{ previewingDrawing?.drawingName || '' }}</span>
          <div class="pdf-preview-actions">
            <el-button v-if="!previewFullscreen" type="primary" link :icon="FullScreen" @click="previewFullscreen = true">放大</el-button>
            <el-button v-else type="primary" link :icon="Crop" @click="previewFullscreen = false">缩小</el-button>
            <el-button type="danger" link :icon="Close" @click="previewVisible = false">关闭</el-button>
          </div>
        </div>
      </template>
      <div class="pdf-preview-body" :style="{ height: previewFullscreen ? '100%' : 'calc(100vh - 200px)', minHeight: '500px', overflow: 'auto' }">
        <iframe
          v-if="previewUrl"
          :src="previewUrl"
          style="width: 100%; height: 100%; border: none;"
        />
        <el-empty v-else description="加载中..." />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, FullScreen, Crop, Close } from '@element-plus/icons-vue'
import { getDrawingsByMaterialId } from '@/api/drawing'

const route = useRoute()
const router = useRouter()
const materialId = Number(route.params.materialId)

const loading = ref(true)
const material = ref({})
const drawings = ref([])

// PDF 预览
const previewVisible = ref(false)
const previewUrl = ref('')
const previewFullscreen = ref(false)
const previewingDrawing = ref(null)

// 文件大小格式化
function formatFileSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1048576).toFixed(2) + ' MB'
}

// 日期格式化
function formatDateTime(value) {
  if (!value) return '-'
  const num = Number(value)
  const date = isNaN(num) ? new Date(value) : new Date(num)
  if (isNaN(date.getTime())) return '-'
  const pad = n => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

// 序号（从 1 开始）
function indexMethod(index) {
  return index + 1
}

// 格式 tag 颜色
function formatTagType(fmt) {
  const f = fmt?.toLowerCase()
  if (f === 'pdf') return 'danger'
  if (f === 'sldprt' || f === 'sldasm') return 'success'
  if (f === 'slddrw') return 'warning'
  return ''
}

// 返回图纸管理列表
function goBack() {
  router.push('/drawings')
}

// 构造下载 URL（带 token）
function buildDownloadUrl(id, inline = false) {
  const token = localStorage.getItem('token')
  let url = `/api/drawing/download/${id}`
  const params = []
  if (inline) params.push('inline=true')
  if (token) params.push(`token=${encodeURIComponent(token)}`)
  if (params.length) url += '?' + params.join('&')
  return url
}

function previewPdf(row) {
  previewingDrawing.value = row
  previewUrl.value = buildDownloadUrl(row.id, true)
  previewVisible.value = true
  previewFullscreen.value = false
}

function downloadFile(row) {
  window.open(buildDownloadUrl(row.id, false), '_blank')
}

// 加载数据
onMounted(async () => {
  // 优先使用 history state 传入的物料信息（避免重复请求）
  const stateMaterial = history.state?.material
  if (stateMaterial) {
    material.value = stateMaterial
  }

  try {
    const res = await getDrawingsByMaterialId(materialId)
    if (res.code !== 200) {
      ElMessage.error(res.msg || '加载图纸列表失败')
      return
    }
    // 后端已经按 created_at DESC 排序，无需再次排序
    drawings.value = res.data || []
  } catch (err) {
    console.error('加载图纸版本失败:', err)
    ElMessage.error(err.response?.data?.msg || err.message || '加载图纸版本失败')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.versions-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 16px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 0 16px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.loading-container {
  padding: 40px 0;
}

.info-card,
.files-card {
  border-radius: 8px;
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  font-size: 15px;
}

:deep(.el-card__header) {
  padding: 12px 20px;
}

:deep(.el-card__body) {
  padding: 16px 20px;
}

.pdf-preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  height: 100%;
}

.pdf-preview-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.pdf-preview-body {
  width: 100%;
  height: 100%;
  overflow: hidden;
}

:deep(.pdf-preview-dialog.is-fullscreen) {
  width: 100vw !important;
  height: 100vh !important;
  margin: 0 !important;
  top: 0 !important;
  left: 0 !important;
  border-radius: 0 !important;
  max-width: 100vw !important;
}

:deep(.pdf-preview-dialog.is-fullscreen .el-dialog__body) {
  width: 100vw !important;
  height: calc(100vh - 54px) !important;
  padding: 0 !important;
  overflow: hidden !important;
  box-sizing: border-box;
}

:deep(.pdf-preview-dialog .el-dialog__header) {
  margin-right: 0;
  display: flex;
  align-items: center;
  padding: 0 20px;
  height: 54px;
  box-sizing: border-box;
}

:deep(.pdf-preview-dialog .el-dialog__body) {
  padding: 0;
  overflow: hidden;
  box-sizing: border-box;
}

:deep(.pdf-preview-dialog .el-dialog__headerbtn) {
  display: none;
}
</style>