<template>
  <div class="page-container">
    <!-- 顶部按钮栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="keyword"
          placeholder="请输入单据编号/采购人员/部门"
          clearable
          style="width: 280px"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
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
      @row-click="handleRowClick"
    >
      <el-table-column type="index" label="序号" width="60" align="center" :index="indexMethod" />
      <el-table-column prop="requisitionNo" label="单据编号" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          <el-button type="primary" link @click.stop="goDetail(row)">{{ row.requisitionNo }}</el-button>
        </template>
      </el-table-column>
      <el-table-column prop="requisitionDate" label="单据日期" width="120" align="center" />
      <el-table-column prop="requester" label="采购人员" width="100" align="center" />
      <el-table-column prop="department" label="部门" width="120" align="center" show-overflow-tooltip />
      <el-table-column prop="itemCount" label="物料数量" width="100" align="center">
        <template #default="{ row }">
          <el-tag type="info" size="small">{{ row.itemCount || 0 }} 项</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right" align="center">
        <template #default="{ row }">
          <el-button type="primary" link :icon="View" @click.stop="goDetail(row)">查看详情</el-button>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Refresh, View } from '@element-plus/icons-vue'
import { getRequisitionList, searchRequisition } from '@/api/requisition'

const router = useRouter()

const loading = ref(false)
const error = ref('')
const keyword = ref('')
const tableData = ref([])

// 分页
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const pageSizes = [20, 50, 100]

// 日期格式化（仅日期）
function formatDate(value) {
  if (!value || value === '-') return '-'
  const num = Number(value)
  const date = isNaN(num) ? new Date(value) : new Date(num)
  if (isNaN(date.getTime())) return '-'
  const pad = n => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

// 获取列表
async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    const res = keyword.value.trim()
      ? await searchRequisition(keyword.value.trim(), currentPage.value, pageSize.value)
      : await getRequisitionList(currentPage.value, pageSize.value)
    const pageData = res.data || {}
    const list = pageData.list || []
    total.value = pageData.total || 0
    tableData.value = list.map(item => ({
      ...item,
      requisitionDate: formatDate(item.requisitionDate)
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
  currentPage.value = 1
  fetchData()
}

// 序号：跨页连续编号
function indexMethod(index) {
  return (currentPage.value - 1) * pageSize.value + index + 1
}

function handlePageChange() {
  fetchData()
}

function handleSizeChange() {
  currentPage.value = 1
  fetchData()
}

function handleRowClick(row) {
  // 点击行也可以进入详情
}

function goDetail(row) {
  router.push(`/requisitions/${row.id}`)
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
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

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
