<template>
  <div class="page-container">
    <!-- 顶部操作栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-button :icon="Back" @click="goBack">返回列表</el-button>
      </div>
      <div class="toolbar-right">
        <el-button type="success" :icon="Document" :loading="exportingExcel" @click="handleExportExcel">导出Excel</el-button>
        <el-button type="warning" :icon="Download" :loading="exportingDrawings" @click="handleExportDrawings">导出全部图纸</el-button>
      </div>
    </div>

    <!-- 采购订单信息卡片 -->
    <el-card class="info-card" shadow="hover" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span class="card-title">采购订单信息</span>
          <el-tag type="success" size="small">{{ requisition.requisitionNo }}</el-tag>
        </div>
      </template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="单据编号">{{ requisition.requisitionNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="单据日期">{{ formatDate(requisition.requisitionDate) }}</el-descriptions-item>
        <el-descriptions-item label="采购人员">{{ requisition.requester || '-' }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ requisition.department || '-' }}</el-descriptions-item>
        <el-descriptions-item label="物料数量">{{ requisition.itemCount || 0 }} 项</el-descriptions-item>
        <el-descriptions-item label="备注">{{ requisition.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 物料明细表格 -->
    <el-card class="detail-card" shadow="hover" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span class="card-title">物料明细</span>
        </div>
      </template>

      <el-table
        :data="requisition.items || []"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="materialCode" label="物料编码" min-width="120" show-overflow-tooltip />
        <el-table-column prop="materialName" label="物料名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="specification" label="规格型号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="quantity" label="采购数量" width="110" align="center">
          <template #default="{ row }">
            {{ row.quantity }}{{ row.unit ? row.unit : '' }}
          </template>
        </el-table-column>
        <el-table-column label="物料图纸" width="140" align="center">
          <template #default="{ row }">
            <template v-if="row.hasDrawing && row.drawingId">
              <el-button type="primary" link :icon="Document" @click="handlePreviewDrawing(row)">
                {{ row.drawingName || '查看图纸' }}
              </el-button>
            </template>
            <template v-else>
              <el-tag type="info" size="small">暂无图纸</el-tag>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.hasDrawing && row.drawingId"
              type="primary"
              link
              :icon="View"
              @click="handlePreviewDrawing(row)"
            >PDF预览</el-button>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无物料明细" />
        </template>
      </el-table>
    </el-card>

    <!-- 统计信息 -->
    <div class="stats-bar" v-if="requisition.items && requisition.items.length > 0">
      <el-tag type="info">共 {{ requisition.items.length }} 项物料</el-tag>
      <el-tag type="success">有图纸 {{ drawingCount }} 项</el-tag>
      <el-tag type="warning">暂无图纸 {{ requisition.items.length - drawingCount }} 项</el-tag>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, Document, Download, View } from '@element-plus/icons-vue'
import {
  getRequisitionDetail,
  buildDrawingPreviewUrl
} from '@/api/requisition'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const requisition = ref({})
const exportingExcel = ref(false)
const exportingDrawings = ref(false)

const drawingCount = computed(() => {
  const items = requisition.value.items || []
  return items.filter(item => item.hasDrawing && item.drawingId).length
})

function formatDate(value) {
  if (!value || value === '-') return '-'
  const num = Number(value)
  const date = isNaN(num) ? new Date(value) : new Date(num)
  if (isNaN(date.getTime())) return '-'
  const pad = n => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

async function fetchDetail() {
  loading.value = true
  try {
    const id = route.params.id
    const res = await getRequisitionDetail(id)
    requisition.value = res.data || {}
  } catch (err) {
    console.error('获取采购订单详情失败:', err)
    ElMessage.error('获取采购订单详情失败')
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push('/requisitions')
}

function handlePreviewDrawing(row) {
  if (!row.drawingId) {
    ElMessage.warning('该物料暂无图纸')
    return
  }
  const url = buildDrawingPreviewUrl(row.drawingId)
  window.open(url, '_blank')
}

async function handleExportExcel() {
  exportingExcel.value = true
  try {
    const id = route.params.id
    const token = localStorage.getItem('token')
    const url = `/api/requisition/export/excel/${id}?token=${encodeURIComponent(token || '')}`

    const res = await fetch(url)
    const contentType = res.headers.get('Content-Type') || ''

    if (contentType.includes('application/json')) {
      const result = await res.json()
      ElMessage.error(result.msg || '导出失败')
      return
    }

    const blob = await res.blob()
    const disposition = res.headers.get('Content-Disposition') || ''
    let filename = `采购订单_${id}.xlsx`
    const match = disposition.match(/filename\*=UTF-8''(.+)/)
    if (match) {
      filename = decodeURIComponent(match[1])
    }
    const downloadUrl = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = downloadUrl
    a.download = filename
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(downloadUrl)
    ElMessage.success('Excel导出成功')
  } catch (err) {
    console.error('导出Excel失败:', err)
    ElMessage.error('导出Excel失败，请稍后重试')
  } finally {
    exportingExcel.value = false
  }
}

async function handleExportDrawings() {
  exportingDrawings.value = true
  try {
    const id = route.params.id
    const token = localStorage.getItem('token')
    const url = `/api/requisition/export/drawings/${id}?token=${encodeURIComponent(token || '')}`

    const res = await fetch(url)
    const contentType = res.headers.get('Content-Type') || ''

    if (contentType.includes('application/json')) {
      const result = await res.json()
      ElMessage.error(result.msg || '导出失败')
      return
    }

    const blob = await res.blob()
    const disposition = res.headers.get('Content-Disposition') || ''
    let filename = `图纸包_${id}.zip`
    const match = disposition.match(/filename\*=UTF-8''(.+)/)
    if (match) {
      filename = decodeURIComponent(match[1])
    }
    const downloadUrl = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = downloadUrl
    a.download = filename
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(downloadUrl)
    ElMessage.success('图纸导出成功')
  } catch (err) {
    console.error('导出图纸失败:', err)
    ElMessage.error('导出图纸失败，请稍后重试')
  } finally {
    exportingDrawings.value = false
  }
}

onMounted(() => {
  fetchDetail()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  gap: 8px;
  align-items: center;
}

.info-card {
  margin-bottom: 16px;
}

.detail-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-title {
  font-size: 16px;
  font-weight: bold;
}

.text-muted {
  color: #c0c4cc;
}

.stats-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 8px 0;
}
</style>
