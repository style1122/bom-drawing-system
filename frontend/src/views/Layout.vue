<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapse ? '64px' : '210px'" class="layout-aside">
      <div class="logo" @click="router.push(userStore.homePath)">
        <span v-if="!isCollapse" class="logo-text">BOM图纸管理</span>
        <span v-else class="logo-text-mini">BOM</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        router
      >
        <el-menu-item v-if="canAccessDashboard" index="/dashboard">
          <el-icon><Monitor /></el-icon>
          <template #title>仪表盘</template>
        </el-menu-item>
        <el-menu-item v-if="canAccessDrawings" index="/drawings">
          <el-icon><Files /></el-icon>
          <template #title>图纸管理</template>
        </el-menu-item>
        <el-menu-item v-if="canAccessRequisitions" index="/requisitions">
          <el-icon><Document /></el-icon>
          <template #title>采购订单管理</template>
        </el-menu-item>
        <template v-if="isAdmin">
          <el-menu-item index="/admin/users">
            <el-icon><User /></el-icon>
            <template #title>用户管理</template>
          </el-menu-item>
          <el-menu-item index="/admin/review">
            <el-icon><CircleCheck /></el-icon>
            <template #title>
              用户审核
              <el-badge
                v-if="pendingCount > 0"
                :value="pendingCount"
                class="menu-badge"
              />
            </template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon
            class="collapse-btn"
            @click="isCollapse = !isCollapse"
          >
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <span class="system-title">BOM图纸管理</span>
        </div>
        <div class="header-right">
          <span class="user-name">{{ userStore.userInfo?.displayName || '用户' }}</span>
          <el-button type="danger" text @click="handleLogout">退出</el-button>
        </div>
      </el-header>

      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  Monitor, User, CircleCheck, Fold, Expand, Files, Document
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { getPendingUsers } from '@/api/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isCollapse = ref(false)
const pendingCount = ref(0)

const activeMenu = computed(() => {
  // 详情页 /requisitions/:id 高亮列表菜单
  if (route.path.startsWith('/requisitions/')) return '/requisitions'
  return route.path
})
const isAdmin = computed(() => userStore.userInfo?.role === 'ADMIN')
const canAccessDashboard = computed(() => userStore.canAccessDashboard)
const canAccessDrawings = computed(() => userStore.canAccessDrawings)
const canAccessRequisitions = computed(() => userStore.canAccessRequisitions)

async function fetchPendingCount() {
  if (!isAdmin.value) return
  try {
    const res = await getPendingUsers()
    pendingCount.value = Array.isArray(res.data) ? res.data.length : 0
  } catch (error) {
    console.error('获取待审核数量失败:', error)
  }
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
}

onMounted(() => {
  if (userStore.isLoggedIn && !userStore.userInfo) {
    userStore.fetchCurrentUser()
  }
})

watch(isAdmin, (val) => {
  if (val) {
    fetchPendingCount()
  }
})

onMounted(() => {
  fetchPendingCount()
})
</script>

<style scoped>
.layout-container {
  height: 100%;
}

.layout-aside {
  background-color: #304156;
  overflow: hidden;
  transition: width 0.3s;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo-text {
  color: #fff;
  font-size: 16px;
  font-weight: bold;
  white-space: nowrap;
}

.logo-text-mini {
  color: #fff;
  font-size: 16px;
  font-weight: bold;
}

.el-menu {
  border-right: none;
}

.menu-badge {
  margin-left: 8px;
}

.layout-header {
  background-color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: #606266;
}

.collapse-btn:hover {
  color: #409eff;
}

.system-title {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  color: #606266;
  font-size: 14px;
}

.layout-main {
  background-color: #f5f7fa;
  padding: 0;
  overflow: auto;
}
</style>
