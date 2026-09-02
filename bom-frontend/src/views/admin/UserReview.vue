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

    <template v-if="pendingList.length > 0">
      <el-table
        v-loading="loading"
        :data="pendingList"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="displayName" label="姓名" width="120" />
        <el-table-column label="申请角色" width="120">
          <template #default="{ row }">
            <el-tag size="small">
              {{ roleMap[row.role] || row.role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="申请时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              type="success"
              size="small"
              @click="handleApprove(row)"
            >
              批准
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click="handleReject(row)"
            >
              驳回
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <el-empty v-else-if="!loading" description="暂无待审核用户" />

    <!-- 驳回弹窗 -->
    <el-dialog
      v-model="rejectDialogVisible"
      title="驳回申请"
      width="450px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="rejectFormRef"
        :model="rejectForm"
        :rules="rejectFormRules"
        label-width="80px"
      >
        <el-form-item label="驳回原因" prop="reason">
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入驳回原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejecting" @click="handleRejectSubmit">
          确定驳回
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPendingUsers, approveUser, rejectUser } from '@/api/user'

const loading = ref(false)
const error = ref('')
const pendingList = ref([])

const rejectDialogVisible = ref(false)
const rejectFormRef = ref(null)
const rejecting = ref(false)
const currentRejectUser = ref(null)

const rejectForm = ref({ reason: '' })

const rejectFormRules = {
  reason: [
    { required: true, message: '请输入驳回原因', trigger: 'blur' }
  ]
}

const roleMap = {
  ADMIN: '管理员',
  ENGINEER: '研发',
  PURCHASER: '采购',
  PRODUCTION: '生产'
}

async function fetchPendingUsers() {
  loading.value = true
  error.value = ''
  try {
    const res = await getPendingUsers()
    pendingList.value = res.data || []
  } catch (err) {
    error.value = '加载待审核用户列表失败'
    console.error('加载待审核用户列表失败:', err)
  } finally {
    loading.value = false
  }
}

async function handleApprove(row) {
  try {
    await ElMessageBox.confirm(
      `确定要批准用户 "${row.username}" 的注册申请吗？`,
      '确认批准',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }
    )
    await approveUser(row.id)
    ElMessage.success('批准成功')
    fetchPendingUsers()
  } catch (err) {
    if (err !== 'cancel') {
      console.error('批准失败:', err)
    }
  }
}

function handleReject(row) {
  currentRejectUser.value = row
  rejectForm.value.reason = ''
  rejectDialogVisible.value = true
}

async function handleRejectSubmit() {
  if (!rejectFormRef.value) return
  try {
    await rejectFormRef.value.validate()
  } catch {
    return
  }

  rejecting.value = true
  try {
    await rejectUser(currentRejectUser.value.id, rejectForm.value.reason)
    ElMessage.success('驳回成功')
    rejectDialogVisible.value = false
    fetchPendingUsers()
  } catch (err) {
    console.error('驳回失败:', err)
  } finally {
    rejecting.value = false
  }
}

onMounted(() => {
  fetchPendingUsers()
})
</script>
