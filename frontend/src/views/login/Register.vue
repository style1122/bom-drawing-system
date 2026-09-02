<template>
  <div class="login-page">
    <el-card class="register-card" shadow="always">
      <template #header>
        <div class="register-header">
          <h2>用户注册</h2>
          <p>BOM图纸管理系统</p>
        </div>
      </template>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="0"
        size="large"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请确认密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item prop="displayName">
          <el-input
            v-model="form.displayName"
            placeholder="请输入显示姓名"
            :prefix-icon="UserFilled"
          />
        </el-form-item>
        <el-form-item prop="role">
          <el-select
            v-model="form.role"
            placeholder="请选择申请角色"
            class="full-width"
          >
            <el-option label="研发" value="ENGINEER" />
            <el-option label="采购" value="PURCHASER" />
            <el-option label="生产" value="PRODUCTION" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="register-btn"
            @click="handleRegister"
          >
            注 册
          </el-button>
        </el-form-item>
      </el-form>
      <div class="register-footer">
        <span>已有账号？</span>
        <router-link to="/login">立即登录</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, UserFilled } from '@element-plus/icons-vue'
import { register } from '@/api/user'

const router = useRouter()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  displayName: '',
  role: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  displayName: [
    { required: true, message: '请输入显示姓名', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择申请角色', trigger: 'change' }
  ]
}

async function handleRegister() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    const res = await register({
      username: form.username,
      password: form.password,
      displayName: form.displayName,
      role: form.role
    })
    // 后端业务错误时 HTTP 状态码仍为 200，需检查 code
    if (res.code !== 200) {
      ElMessage.error(res.msg || '注册失败，请稍后重试')
      return
    }
    ElMessage.success('注册申请已提交，请等待管理员审核')
    router.push('/login')
  } catch (error) {
    console.error('注册失败:', error)
    ElMessage.error(error.message || '注册失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-card {
  width: 450px;
  border-radius: 8px;
}

.register-header {
  text-align: center;
}

.register-header h2 {
  font-size: 22px;
  color: #303133;
}

.register-header p {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.register-btn {
  width: 100%;
}

.full-width {
  width: 100%;
}

.register-footer {
  text-align: center;
  font-size: 13px;
  color: #909399;
}

.register-footer a {
  margin-left: 4px;
}
</style>
