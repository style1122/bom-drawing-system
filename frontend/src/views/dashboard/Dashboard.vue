<template>
  <div class="page-container">
    <div v-loading="loading">
      <el-alert
        v-if="error"
        :title="error"
        type="error"
        show-icon
        :closable="false"
        style="margin-bottom: 20px"
      />

      <el-skeleton v-if="loading && !stats" :rows="6" animated />

      <template v-else-if="stats">
        <el-row :gutter="20" style="margin-bottom: 20px">
          <el-col :span="6" v-for="card in statCards" :key="card.label">
            <el-card shadow="hover">
              <div class="stat-card">
                <div class="stat-icon" :style="{ backgroundColor: card.color }">
                  <el-icon :size="28" color="#fff">
                    <component :is="card.icon" />
                  </el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-label">{{ card.label }}</div>
                  <div class="stat-value">{{ card.value }}</div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="20" style="margin-bottom: 20px" class="chart-row">
          <el-col :span="14" class="chart-col">
            <el-card>
              <template #header>
                <span>每日上传图纸数量（近 30 天）</span>
              </template>
              <LineChart :points="dailyPoints" color="#409eff" :height="260" />
            </el-card>
          </el-col>
          <el-col :span="10" class="chart-col">
            <el-card>
              <template #header>
                <span>总存储占用增长趋势（近 30 天）</span>
                <span class="storage-summary">
                  当前总占用：<b>{{ formatBytes(totalStorage) }}</b>
                </span>
              </template>
              <LineChart
                :points="storagePoints"
                color="#67c23a"
                :area="true"
                y-min-mode="auto"
                :value-formatter="formatBytes"
                :height="260"
              />
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="20" style="margin-bottom: 20px">
          <el-col :span="24">
            <el-card>
              <template #header>
                <span>图纸类型数量对比</span>
              </template>
              <LineChart :points="typePoints" type="bar" color="#409eff" :height="260" />
            </el-card>
          </el-col>
        </el-row>

        <el-card>
          <template #header>
            <span>最近操作日志</span>
          </template>
          <el-table
            v-if="logs.length > 0"
            :data="paginatedLogs"
            style="width: 100%"
            size="small"
          >
            <el-table-column prop="userName" label="操作用户" width="100" />
            <el-table-column prop="operation" label="操作类型" width="80" />
            <el-table-column label="操作内容" show-overflow-tooltip>
              <template #default="{ row }">
                {{ formatLogDetail(row) }}
              </template>
            </el-table-column>
            <el-table-column label="操作时间" width="180">
              <template #default="{ row }">
                {{ formatLogTime(row.createdAt) }}
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-if="logs.length > pageSize"
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="logs.length"
            layout="total, prev, pager, next"
            :pager-count="5"
            size="small"
            style="margin-top: 16px; justify-content: flex-end"
          />
          <el-empty v-else description="暂无操作日志" />
        </el-card>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  Files, User, Upload, Coin
} from '@element-plus/icons-vue'
import { getStats } from '@/api/dashboard'
import LineChart from '@/components/LineChart.vue'

const loading = ref(false)
const error = ref('')
const stats = ref(null)

const statCards = computed(() => [
  {
    label: '图纸文件数',
    value: stats.value?.drawingCount || 0,
    icon: Files,
    color: '#e6a23c'
  },
  {
    label: '活跃用户数',
    value: stats.value?.activeUserCount || 0,
    icon: User,
    color: '#f56c6c'
  },
  {
    label: '今日上传',
    value: (stats.value?.todayUploadCount || 0) + ' 张',
    icon: Upload,
    color: '#409eff'
  },
  {
    label: '总存储占用',
    value: formatBytes(stats.value?.totalStorageBytes),
    icon: Coin,
    color: '#67c23a'
  }
])

// 每日上传折线图数据（近 30 天，按日期补齐）
const dailyPoints = computed(() =>
  (stats.value?.dailyUploadList || []).map(d => ({ label: d.date, value: d.count }))
)
// 存储增长趋势数据（累计字节）
const storagePoints = computed(() =>
  (stats.value?.storageTrendList || []).map(d => ({ label: d.date, value: d.cumulativeBytes }))
)
const totalStorage = computed(() => stats.value?.totalStorageBytes || 0)

// 图纸类型数量对比：PDF 图纸 / 三维图纸 / 工程图纸（每类单独配色）
const typePoints = computed(() => [
  { label: 'PDF 图纸', value: stats.value?.pdfCount || 0, color: '#409eff' },
  { label: '三维图纸', value: stats.value?.model3dCount || 0, color: '#67c23a' },
  { label: '工程图纸', value: stats.value?.engineeringCount || 0, color: '#e6a23c' }
])

function formatBytes(bytes) {
  if (bytes == null) return '-'
  if (bytes < 1024) return bytes + ' B'
  const units = ['KB', 'MB', 'GB', 'TB']
  let i = 0
  let n = bytes / 1024
  while (n >= 1024 && i < units.length - 1) {
    n /= 1024
    i++
  }
  return n.toFixed(2) + ' ' + units[i]
}

const logs = computed(() => stats.value?.recentLogs || [])
const currentPage = ref(1)
const pageSize = ref(10)
const paginatedLogs = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return logs.value.slice(start, start + pageSize.value)
})

function formatLogTime(createdAt) {
  // 后端返回 yyyy-MM-dd HH:mm:ss 格式
  if (!createdAt) return '-'
  return createdAt.replace(/-/g, '年').replace(' ', '日 ').replace(/^(\d+)年/, '$1年').replace(/(\d+):(\d+):\d+$/, '$1:$2')
}

function formatLogDetail(row) {
  // 格式: 上传 后台板排刀1000-535.pdf 图纸
  const detail = row.detail || ''
  const op = row.operation || ''
  const targetType = row.targetType || ''
  if (targetType === 'DRAWING') {
    return `${op} ${detail} 图纸`
  }
  if (targetType === 'MATERIAL') {
    return `${op} 物料 ${detail}`
  }
  return `${op} ${detail}`
}

async function fetchStats() {
  loading.value = true
  error.value = ''
  try {
    const res = await getStats()
    stats.value = res.data
    currentPage.value = 1
  } catch (err) {
    error.value = '加载仪表盘数据失败'
    console.error('获取统计数据失败:', err)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchStats()
})
</script>

<style scoped>
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.chart-row {
  display: flex;
}

.chart-col {
  display: flex;
}

.chart-col > .el-card {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chart-col > .el-card :deep(.el-card__body) {
  flex: 1;
}

.storage-summary {
  float: right;
  font-size: 13px;
  color: #909399;
  margin: 0;
}

.storage-summary b {
  color: #303133;
  font-size: 14px;
}
</style>
