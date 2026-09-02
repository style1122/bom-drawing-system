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

        <el-card>
          <template #header>
            <span>最近操作日志</span>
          </template>
          <el-table
            v-if="logs.length > 0"
            :data="logs"
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
          <el-empty v-else description="暂无操作日志" />
        </el-card>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  Files, User
} from '@element-plus/icons-vue'
import { getStats } from '@/api/dashboard'

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
  }
])

const logs = computed(() => stats.value?.recentLogs || [])

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
</style>
