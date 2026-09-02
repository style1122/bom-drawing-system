<template>
  <div class="share-page">
    <!-- 错误状态 -->
    <div v-if="error" class="error-container">
      <el-result icon="error" :title="error" sub-title="分享链接可能已过期或不存在">
        <template #extra>
          <el-button type="primary" @click="$router.push('/login')">登录系统</el-button>
        </template>
      </el-result>
    </div>

    <!-- 加载状态 -->
    <div v-else-if="loading" class="loading-container">
      <el-skeleton :rows="10" animated />
    </div>

    <!-- 正常内容 -->
    <div v-else class="share-content">
      <div class="page-header">
        <h2>图纸分享</h2>
        <el-tag type="warning" effect="dark" size="small">无需登录访问</el-tag>
      </div>

      <!-- 物料信息卡片 -->
      <el-card class="info-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>物料信息</span>
            <el-tag :type="pdfDrawings.length > 0 ? 'success' : 'info'" size="small">
              {{ pdfDrawings.length > 0 ? '有图纸' : '无图纸' }}
            </el-tag>
          </div>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="物料编码">{{ material.materialCode }}</el-descriptions-item>
          <el-descriptions-item label="物料名称">{{ material.materialName }}</el-descriptions-item>
          <el-descriptions-item label="物料规格" :span="2">
            {{ material.specification || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- PDF 图纸预览 -->
      <el-card v-if="pdfDrawings.length > 0" class="preview-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>PDF 图纸预览</span>
            <span class="drawing-count">共 {{ pdfDrawings.length }} 个 PDF 文件</span>
          </div>
        </template>
        <div class="preview-container">
          <iframe
            v-if="previewUrl"
            :src="previewUrl"
            class="pdf-iframe"
            frameborder="0"
          />
          <el-empty v-else description="选择下方 PDF 文件进行预览" />
        </div>
      </el-card>

      <!-- 图纸文件列表 -->
      <el-card class="files-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>图纸文件列表</span>
          </div>
        </template>
        <el-table :data="drawings" border stripe size="small">
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="drawingName" label="文件名" min-width="200" show-overflow-tooltip />
          <el-table-column prop="fileFormat" label="格式" width="80" align="center">
            <template #default="{ row }">
              <el-tag size="small">{{ row.fileFormat?.toUpperCase() }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="大小" width="100" align="center">
            <template #default="{ row }">
              {{ formatFileSize(row.fileSize) }}
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
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getShareData, getShareDownloadUrl } from '@/api/share'

const route = useRoute()
const token = route.params.token

const loading = ref(true)
const error = ref('')
const material = ref({})
const drawings = ref([])
const previewUrl = ref('')

const pdfDrawings = computed(() =>
  drawings.value.filter(d => d.fileFormat?.toLowerCase() === 'pdf')
)

function formatFileSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1048576).toFixed(2) + ' MB'
}

function previewPdf(drawing) {
  previewUrl.value = getShareDownloadUrl(token, drawing.id, true)
}

function downloadFile(drawing) {
  window.open(getShareDownloadUrl(token, drawing.id, false), '_blank')
}

onMounted(async () => {
  try {
    const res = await getShareData(token)
    if (res.code !== 200) {
      error.value = res.msg || '分享链接无效'
      return
    }
    material.value = res.data.material
    drawings.value = res.data.drawings || []

    // 默认预览第一个 PDF
    if (pdfDrawings.value.length > 0) {
      previewUrl.value = getShareDownloadUrl(token, pdfDrawings.value[0].id, true)
    }
  } catch (err) {
    error.value = err.message || '加载失败，分享链接可能已过期'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.share-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  min-height: 100vh;
  background: #f0f2f5;
}

.error-container,
.loading-container {
  max-width: 600px;
  margin: 100px auto;
}

.share-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 0 8px;
}

.page-header h2 {
  margin: 0;
  font-size: 22px;
  color: #303133;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  font-size: 15px;
}

.drawing-count {
  font-size: 13px;
  color: #909399;
  font-weight: 400;
}

.preview-container {
  height: 650px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #525659;
}

.pdf-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.info-card,
.preview-card,
.files-card {
  border-radius: 8px;
}

:deep(.el-card__header) {
  padding: 12px 20px;
}

:deep(.el-card__body) {
  padding: 16px 20px;
}
</style>
