<template>
  <div class="page-container">
    <!-- 顶部按钮栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="keyword"
          placeholder="请输入物料编码/名称/规格"
          clearable
          style="width: 260px"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>
      <div class="toolbar-right">
        <template v-if="canEditDrawings">
          <el-button
            type="success"
            :icon="Refresh"
            :loading="erpSyncing"
            @click="handleErpSync"
          >ERP物料同步</el-button>
          <el-button :icon="Connection" :loading="erpTesting" @click="handleErpTest">测试ERP连接</el-button>
          <el-button :icon="Link" :loading="erpFlagSyncing" @click="handleErpSyncFlag">同步ERP图纸标记</el-button>
          <el-button :icon="Upload" @click="handleUpload">上传图纸</el-button>
        </template>
        <el-button :icon="Download" @click="handleDownloadPdf">下载PDF图纸</el-button>
        <el-button :icon="Download" @click="handleDownload3d">下载三维图纸</el-button>
        <el-button :icon="Download" @click="handleDownloadEngineering">下载工程图纸</el-button>
        <el-button v-if="canEditDrawings" type="danger" :icon="Delete" @click="handleDelete">删除</el-button>
        <el-button :icon="View" @click="handlePreview">PDF预览</el-button>
      </div>
    </div>

    <el-alert
      v-if="error"
      :title="error"
      type="error"
      show-icon
      :closable="false"
      style="margin-bottom: 12px"
    />

    <!-- 数据表格 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      border
      stripe
      highlight-current-row
      style="width: 100%"
      @current-change="handleCurrentChange"
    >
      <el-table-column type="index" label="序号" width="60" align="center" :index="indexMethod" />
      <el-table-column prop="materialCode" label="物料编码" min-width="120" show-overflow-tooltip />
      <el-table-column prop="materialName" label="物料名称" min-width="150" show-overflow-tooltip />
      <el-table-column prop="specification" label="物料规格/图号" min-width="180" show-overflow-tooltip />
      <el-table-column label="来源" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.source === 'ERP' ? 'success' : 'info'" size="small" effect="plain">
            {{ row.source === 'ERP' ? 'ERP' : '手工' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="ERP同步时间" width="160" align="center">
        <template #default="{ row }">
          {{ formatDateTime(row.erpSyncTime) }}
        </template>
      </el-table-column>
      <el-table-column label="ERP图纸标记" width="110" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.erpHaveDrawing === 1" type="success" size="small" effect="dark">是</el-tag>
          <el-tag v-else-if="row.erpHaveDrawing === 0" type="info" size="small" effect="plain">否</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="drawingAddDate" label="图纸新增日期" width="150" align="center" />
      <el-table-column prop="drawingUpdateDate" label="图纸修改时间" width="150" align="center" />
      <el-table-column label="是否存在图纸" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="row.hasDrawing ? 'success' : 'info'" size="small" effect="dark">
            {{ row.hasDrawing ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="是否存在三维" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="row.has3d ? 'success' : 'info'" size="small" effect="dark">
            {{ row.has3d ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="是否存在工程" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="row.hasEngineering ? 'success' : 'info'" size="small" effect="dark">
            {{ row.hasEngineering ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click.stop="handleRowDownloadPdf(row)">下载PDF图纸</el-button>
          <el-button type="warning" link @click.stop="handleVersions(row)">版本管理</el-button>
          <el-button v-if="canEditDrawings" type="success" link @click.stop="handleShare(row)">分享</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无数据" />
      </template>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="pageSizes"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 上传弹窗 -->
    <el-dialog v-model="uploadDialogVisible" title="上传图纸文件"
               width="600px" :close-on-click-modal="false">
      <el-upload
        ref="uploadRef"
        :auto-upload="false"
        multiple
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
        :before-upload="() => false"
        accept=".pdf,.sldprt,.sldasm,.slddrw"
        drag
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽文件到此处，或 <em>点击选择</em></div>
        <template #tip>
          <div class="el-upload__tip">
            支持多文件上传（.pdf / .sldprt / .sldasm / .slddrw）<br/>
            文件命名规范："图号 名称"，如"ED2.1-1325-10001-25 框架床身"<br/>
            系统将根据第一个空格前的图号自动匹配对应物料
          </div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUploadSubmit">确定上传</el-button>
      </template>
    </el-dialog>

    <!-- 上传结果弹窗 -->
    <el-dialog v-model="uploadResultVisible" title="上传结果" width="750px">
      <el-alert
        :title="uploadResultSummary"
        :type="uploadResultData.unmatched > 0 || uploadResultData.skipped > 0 ? 'warning' : 'success'"
        show-icon
        :closable="false"
        style="margin-bottom: 12px"
      />
      <el-table :data="uploadResultData.details || []" max-height="400" border stripe>
        <el-table-column prop="fileName" label="文件名" min-width="220" show-overflow-tooltip />
        <el-table-column prop="drawingNo" label="提取图号" width="180" show-overflow-tooltip />
        <el-table-column label="匹配状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.matched ? 'success' : 'danger'" size="small" effect="dark">
              {{ row.matched ? '已匹配' : '未匹配' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="materialName" label="匹配物料" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.matched ? row.materialName : (row.error || '-') }}
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button type="primary" @click="uploadResultVisible = false">确定</el-button>
      </template>
    </el-dialog>

    <!-- PDF 预览 iframe 弹窗 -->
    <el-dialog
      v-model="previewVisible"
      width="90%"
      top="2vh"
      :fullscreen="previewFullscreen"
      :close-on-click-modal="false"
      destroy-on-close
      class="pdf-preview-dialog"
      @closed="revokePreviewUrl"
    >
      <template #header>
        <div class="pdf-preview-header">
          <span>PDF 预览</span>
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

    <!-- 3D 预览 -->
    <ThreeViewer
      v-if="previewVisible && previewType === '3d'"
      v-model:visible="previewVisible"
      :model-path="previewUrl"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, Upload, Download, Delete, View, Refresh, UploadFilled, FullScreen, Crop, Close, Connection, Link
} from '@element-plus/icons-vue'
import {
  getMaterialList, searchMaterial, deleteMaterial,
  erpSyncMaterial, erpTestConnection, erpSyncDrawingFlag
} from '@/api/material'
import { uploadDrawing, batchUploadDrawings, getDrawingsByMaterialId } from '@/api/drawing'
import { createShare } from '@/api/share'
import ThreeViewer from '@/components/ThreeViewer.vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
// 上传/删除等写操作仅管理员和研发工程师可用
const canEditDrawings = computed(() => userStore.canEditDrawings)
const loading = ref(false)
const error = ref('')
const keyword = ref('')
const tableData = ref([])
const selectedRow = ref(null)

// 分页
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const pageSizes = [20, 50, 100]

// 上传
const uploadDialogVisible = ref(false)
const uploadRef = ref(null)
const pendingFiles = ref([])
const uploading = ref(false)

// 上传结果
const uploadResultVisible = ref(false)
const uploadResultData = ref({})
const uploadResultSummary = ref('')

// 预览
const previewVisible = ref(false)
const previewType = ref('')
const previewUrl = ref('')
const previewFullscreen = ref(false)

// ERP 同步
const erpSyncing = ref(false)
const erpTesting = ref(false)
const erpFlagSyncing = ref(false)

// 日期格式化
function formatDateTime(value) {
  if (!value || value === '-') return '-'
  const num = Number(value)
  const date = isNaN(num) ? new Date(value) : new Date(num)
  if (isNaN(date.getTime())) return '-'
  const pad = n => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

// 获取列表
async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    const res = keyword.value.trim()
      ? await searchMaterial(keyword.value.trim(), currentPage.value, pageSize.value)
      : await getMaterialList(currentPage.value, pageSize.value)
    // 后端返回 PageResult: { list, total, page, size }
    const pageData = res.data || {}
    const list = pageData.list || []
    total.value = pageData.total || 0
    tableData.value = list.map(item => ({
      ...item,
      hasDrawing: item.hasDrawing || false,
      has3d: item.has3d || false,
      hasEngineering: item.hasEngineering || false,
      drawingAddDate: formatDateTime(item.drawingAddDate),
      drawingUpdateDate: formatDateTime(item.drawingUpdateDate)
    }))
  } catch (err) {
    error.value = '加载失败'
    console.error(err)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchData()
}

function handleReset() {
  keyword.value = ''
  selectedRow.value = null
  currentPage.value = 1
  fetchData()
}

// 测试 ERP 连接
async function handleErpTest() {
  erpTesting.value = true
  try {
    const res = await erpTestConnection()
    const data = res.data || {}
    ElMessage.success(`ERP连接成功：token有效期 ${data.timeout || '-'} 秒`)
  } catch (err) {
    console.error(err)
  } finally {
    erpTesting.value = false
  }
}

// 手动同步 ERP 物料基础数据
async function handleErpSync() {
  try {
    await ElMessageBox.confirm(
      '将从正航T9 ERP同步全部物料基础数据，按物料编码新增或更新本地物料，是否继续？',
      'ERP物料同步',
      { type: 'warning', confirmButtonText: '开始同步', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  erpSyncing.value = true
  try {
    const res = await erpSyncMaterial()
    ElMessage.success(res.msg || 'ERP物料同步完成')
    fetchData()
  } catch (err) {
    console.error(err)
  } finally {
    erpSyncing.value = false
  }
}

// 手动同步选中物料的 ERP 图纸标记（CU_HaveDrawing：1=是 0=否）
async function handleErpSyncFlag() {
  if (!selectedRow.value) {
    ElMessage.warning('请先选中一行物料')
    return
  }
  erpFlagSyncing.value = true
  try {
    const res = await erpSyncDrawingFlag(selectedRow.value.materialCode)
    ElMessage.success(res.msg || 'ERP图纸标记同步完成')
    fetchData()
  } catch (err) {
    console.error(err)
  } finally {
    erpFlagSyncing.value = false
  }
}

function handleCurrentChange(row) {
  selectedRow.value = row
}

// 序号：跨页连续编号
function indexMethod(index) {
  return (currentPage.value - 1) * pageSize.value + index + 1
}

// 分页
function handlePageChange() {
  selectedRow.value = null
  fetchData()
}

function handleSizeChange() {
  currentPage.value = 1
  selectedRow.value = null
  fetchData()
}

// 上传图纸（批量多文件，自动按图号匹配物料）
function handleUpload() {
  pendingFiles.value = []
  uploadDialogVisible.value = true
}

// 复制文本到剪贴板（兼容 HTTP 非安全上下文）
async function copyToClipboard(text) {
  if (navigator.clipboard && navigator.clipboard.writeText) {
    try {
      await navigator.clipboard.writeText(text)
      return true
    } catch (err) {
      console.warn('Clipboard API 复制失败，使用 fallback:', err)
    }
  }
  // fallback：创建隐藏 textarea 并 execCommand
  return new Promise((resolve) => {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.left = '-9999px'
    textarea.style.top = '-9999px'
    document.body.appendChild(textarea)
    textarea.focus()
    textarea.select()
    try {
      const success = document.execCommand('copy')
      document.body.removeChild(textarea)
      resolve(success)
    } catch (err) {
      document.body.removeChild(textarea)
      resolve(false)
    }
  })
}

// 分享
async function handleShare(row) {
  try {
    const res = await createShare({ materialId: row.id })
    const shareUrl = window.location.origin + res.data.shareUrl

    ElMessageBox.alert(
      `<div>
        <p style="margin-bottom:12px;font-size:14px;">分享链接已生成（<b>有效期7天</b>）：</p>
        <div style="background:#f5f7fa;padding:12px;border-radius:4px;
                    word-break:break-all;font-family:monospace;font-size:13px;">
          ${shareUrl}
        </div>
        <p style="margin-top:12px;color:#909399;font-size:12px;">
          采购部无需登录即可查看物料信息和图纸
        </p>
      </div>`,
      '图纸分享',
      {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '复制链接',
        cancelButtonText: '关闭',
        showCancelButton: true
      }
    ).then(async () => {
      const success = await copyToClipboard(shareUrl)
      if (success) {
        ElMessage.success('链接已复制到剪贴板')
      } else {
        ElMessage.warning('复制失败，请手动复制链接')
      }
    }).catch(() => {})
  } catch (err) {
    console.error('分享失败:', err)
    ElMessage.error('分享失败')
  }
}

// 版本管理：跳转到版本管理页面，展示该物料所有历史图纸
function handleVersions(row) {
  router.push({
    name: 'DrawingVersions',
    params: { materialId: row.id },
    state: { material: row }
  })
}

function handleFileChange(file, fileList) {
  pendingFiles.value = fileList.map(f => f.raw)
}

function handleFileRemove(file, fileList) {
  pendingFiles.value = fileList.map(f => f.raw)
}

async function handleUploadSubmit() {
  if (pendingFiles.value.length === 0) {
    ElMessage.warning('请选择文件')
    return
  }
  // 大批量上传时分批提交（单批 ≤1000 个文件且 ≤200MB，避免超 500MB 请求上限）
  const MAX_BATCH_FILES = 1000
  const MAX_BATCH_BYTES = 200 * 1024 * 1024
  const batches = []
  let current = []
  let currentBytes = 0
  for (const file of pendingFiles.value) {
    if (current.length >= MAX_BATCH_FILES || (currentBytes > 0 && currentBytes + (file.size || 0) > MAX_BATCH_BYTES)) {
      batches.push(current)
      current = []
      currentBytes = 0
    }
    current.push(file)
    currentBytes += file.size || 0
  }
  if (current.length > 0) {
    batches.push(current)
  }

  uploading.value = true
  try {
    // 汇总统计
    const agg = { total: 0, matched: 0, unmatched: 0, skipped: 0, details: [], drawings: [] }
    for (let i = 0; i < batches.length; i++) {
      const fd = new FormData()
      for (const file of batches[i]) {
        fd.append('files', file)
      }
      ElMessage.info(`正在上传第 ${i + 1}/${batches.length} 批（${batches[i].length} 个文件）...`)
      const res = await batchUploadDrawings(fd)
      const d = res.data || {}
      agg.total += d.total || 0
      agg.matched += d.matched || 0
      agg.unmatched += d.unmatched || 0
      agg.skipped += d.skipped || 0
      agg.details = agg.details.concat(d.details || [])
      agg.drawings = agg.drawings.concat(d.drawings || [])
    }

    // 显示上传结果
    uploadResultData.value = agg
    uploadResultSummary.value = `上传完成：共 ${agg.total} 个文件，成功匹配 ${agg.matched} 个，未匹配 ${agg.unmatched} 个，跳过 ${agg.skipped} 个（分 ${batches.length} 批）`
    uploadResultVisible.value = true

    // 关闭上传弹窗
    uploadDialogVisible.value = false
    pendingFiles.value = []
    uploadRef.value?.clearFiles()
    fetchData()
  } catch (err) {
    console.error(err)
    ElMessage.error(err.response?.data?.msg || '上传失败')
  } finally {
    uploading.value = false
  }
}

// 构造下载 URL
function buildDownloadUrl(id, inline = false) {
  // 在 URL 里带上 token，这样 Chrome PDF 查看器二次请求时能通过认证
  const token = localStorage.getItem('token')
  let url = `/api/drawing/download/${id}`
  const params = []
  if (inline) params.push('inline=true')
  if (token) params.push(`token=${encodeURIComponent(token)}`)
  if (params.length) url += '?' + params.join('&')
  return url
}

// 下载PDF图纸（工具栏）
async function handleDownloadPdf() {
  if (!selectedRow.value) {
    ElMessage.warning('请先选中一行物料')
    return
  }
  await downloadByFormats(selectedRow.value, ['pdf'], '该物料暂无PDF图纸')
}

// 下载三维图纸（工具栏）
async function handleDownload3d() {
  if (!selectedRow.value) {
    ElMessage.warning('请先选中一行物料')
    return
  }
  await downloadByFormats(selectedRow.value, ['sldprt', 'sldasm'], '该物料暂无三维图纸')
}

// 下载工程图纸（工具栏）
async function handleDownloadEngineering() {
  if (!selectedRow.value) {
    ElMessage.warning('请先选中一行物料')
    return
  }
  await downloadByFormats(selectedRow.value, ['slddrw'], '该物料暂无工程图纸')
}

// 行内下载PDF图纸
async function handleRowDownloadPdf(row) {
  await downloadByFormats(row, ['pdf'], '该物料暂无PDF图纸')
}

/**
 * 按格式下载图纸
 * @param row 物料行
 * @param formats 支持的扩展名数组
 * @param emptyMsg 无文件时的提示语
 */
async function downloadByFormats(row, formats, emptyMsg) {
  try {
    const res = await getDrawingsByMaterialId(row.id)
    const drawings = (res.data || []).filter(d => formats.includes(d.fileFormat?.toLowerCase()))
    if (drawings.length === 0) {
      ElMessage.warning(emptyMsg)
      return
    }
    // 按创建时间倒序，取最新的
    drawings.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    const drawing = drawings[0]
    window.open(buildDownloadUrl(drawing.id), '_blank')
  } catch (err) {
    console.error('下载失败:', err)
    ElMessage.error('下载失败')
  }
}

// 删除
async function handleDelete() {
  if (!selectedRow.value) {
    ElMessage.warning('请先选中一行物料')
    return
  }
  try {
    await ElMessageBox.confirm('确定删除该物料信息吗？', '确认删除', { type: 'warning' })
    await deleteMaterial(selectedRow.value.id)
    ElMessage.success('删除成功')
    selectedRow.value = null
    // 如果当前页只剩一条且不是第一页，回退一页
    if (tableData.value.length <= 1 && currentPage.value > 1) {
      currentPage.value--
    }
    fetchData()
  } catch (err) {
    if (err !== 'cancel') console.error(err)
  }
}

// 预览
async function handlePreview() {
  if (!selectedRow.value) {
    ElMessage.warning('请先选中一行物料')
    return
  }
  try {
    const res = await getDrawingsByMaterialId(selectedRow.value.id)
    const drawings = (res.data || []).filter(d => d.fileFormat?.toLowerCase() === 'pdf')
    if (drawings.length === 0) {
      ElMessage.warning('该物料未上传PDF文件，无法预览')
      return
    }
    drawings.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))

    // 在新标签页打开 PDF（inline 模式让 Chrome 用 PDF 查看器在新标签渲染）
    // 新标签页直接打开 PDF URL，Chrome 不会二次请求丢失 token
    const url = buildDownloadUrl(drawings[0].id, true)
    window.open(url, '_blank')
  } catch (err) {
    console.error('预览失败:', err)
    ElMessage.error('预览失败，请确认PDF文件可正常下载')
  }
}

function revokePreviewUrl() {
  if (previewUrl.value && previewUrl.value.startsWith('blob:')) {
    URL.revokeObjectURL(previewUrl.value)
  }
  previewUrl.value = ''
  previewFullscreen.value = false
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
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

:deep(.pdf-preview-dialog.is-fullscreen) {
  width: 100vw !important;
  height: 100vh !important;
  margin: 0 !important;
  top: 0 !important;
  left: 0 !important;
  border-radius: 0 !important;
  max-width: 100vw !important;
}

:deep(.pdf-preview-dialog.is-fullscreen .el-dialog__header) {
  height: 54px;
  padding: 0 20px;
  margin-right: 0;
  display: flex;
  align-items: center;
  box-sizing: border-box;
}

:deep(.pdf-preview-dialog.is-fullscreen .el-dialog__body) {
  width: 100vw !important;
  height: calc(100vh - 54px) !important;
  padding: 0 !important;
  overflow: hidden !important;
  box-sizing: border-box;
}

.pdf-preview-body {
  width: 100%;
  height: 100%;
  overflow: hidden;
}

:deep(.pdf-preview-dialog .el-dialog__header) {
  margin-right: 0;
  display: flex;
  align-items: center;
  padding: 0 20px 0 20px;
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

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}

.toolbar-left {
  display: flex;
  gap: 8px;
  align-items: center;
}

.toolbar-right {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
