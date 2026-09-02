<template>
  <div class="page-container">
    <el-alert
      v-if="error"
      :title="error"
      type="error"
      show-icon
      :closable="false"
      style="margin-bottom: 16px"
    />

    <el-table
      v-loading="loading"
      :data="userList"
      border
      stripe
      style="width: 100%"
    >
      <el-table-column prop="username" label="用户名" width="140" />
      <el-table-column prop="displayName" label="姓名" width="120" />
      <el-table-column label="角色" width="120">
        <template #default="{ row }">
          <el-tag size="small">
            {{ roleMap[row.role] || row.role }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag
            :type="statusType[row.status]"
            size="small"
          >
            {{ statusMap[row.status] || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="注册时间" width="180" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button
            :type="row.status === 'DISABLED' ? 'success' : 'warning'"
            link
            @click="handleToggleStatus(row)"
          >
            {{ row.status === 'DISABLED' ? '启用' : '停用' }}
          </el-button>
          <el-button type="primary" link @click="handleResetPassword(row)">
            重置密码
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无用户数据" />
      </template>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, disableUser, resetPassword } from '@/api/user'

const loading = ref(false)
const error = ref('')
const userList = ref([])

const roleMap = {
  ADMIN: '管理员',
  ENGINEER: '研发',
  PURCHASER: '采购',
  PRODUCTION: '生产'
}

const statusMap = {
  ACTIVE: '正常',
  DISABLED: '已停用',
  REJECTED: '已驳回'
}

const statusType = {
  ACTIVE: 'success',
  PENDING: 'warning',
  REJECTED: 'danger',
  DISABLED: 'info'
}

async function fetchUsers() {
  loading.value = true
  error.value = ''
  try {
    const res = await getUserList()
    // 过滤掉 PENDING 状态的用户
    userList.value = (res.data || []).filter(u => u.status !== 'PENDING')
  } catch (err) {
    error.value = '加载用户列表失败'
    console.error('加载用户列表失败:', err)
  } finally {
    loading.value = false
  }
}

async function handleToggleStatus(row) {
  const isDisabling = row.status !== 'DISABLED'
  const action = isDisabling ? '停用' : '启用'

  try {
    await ElMessageBox.confirm(
      `确定要${action}用户 "${row.username}" 吗？`,
      `确认${action}`,
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await disableUser(row.id)
    ElMessage.success(`${action}成功`)
    fetchUsers()
  } catch (err) {
    if (err !== 'cancel') {
      console.error(`${action}失败:`, err)
    }
  }
}

async function handleResetPassword(row) {
  try {
    await ElMessageBox.confirm(
      `确定要重置用户 "${row.username}" 的密码吗？`,
      '确认重置密码',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await resetPassword(row.id)
    ElMessage.success('密码重置成功')
  } catch (err) {
    if (err !== 'cancel') {
      console.error('重置密码失败:', err)
    }
  }
}

onMounted(() => {
  fetchUsers()
})
</script>
