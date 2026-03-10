<template>
  <div class="admin-layout">
    <div class="sidebar">
      <div class="logo">校园闲置-管理后台</div>
      <el-menu
          :default-active="route.path"
          router
          class="admin-menu"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#ffe60f"
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataLine /></el-icon>
          <span>数据大盘看板</span>
        </el-menu-item>

        <el-menu-item index="/admin/audit">
          <el-icon><Goods /></el-icon>
          <span>商品审核中心</span>
        </el-menu-item>

        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <span>用户与违规管理</span>
        </el-menu-item>

        <el-menu-item index="/admin/orders">
          <el-icon><List /></el-icon>
          <span>交易与仲裁大厅</span>
        </el-menu-item>

        <el-menu-item index="/admin/reviews">
          <el-icon><ChatDotSquare /></el-icon>
          <span>评价与合规管理</span>
        </el-menu-item>

      </el-menu>
    </div>

    <div class="main-content">
      <div class="header">
        <div class="header-right">
          <span class="welcome">管理员：{{ userStore.userInfo.username }}</span>
          <el-button type="danger" size="small" @click="logout" plain>退出系统</el-button>
        </div>
      </div>

      <div class="page-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
// 🌟 核心：记得把 User(用户) 和 List(列表) 图标引入进来
import {DataLine, Goods, User, List, ChatDotSquare} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const logout = () => {
  userStore.clearUserInfo()
  router.push('/login')
}
</script>

<style scoped>
.admin-layout { display: flex; height: 100vh; overflow: hidden; }

/* 左侧侧边栏样式 */
.sidebar { width: 230px; background-color: #304156; display: flex; flex-direction: column; }
.logo { height: 60px; line-height: 60px; text-align: center; color: #fff; font-size: 18px; font-weight: bold; background-color: #2b3643; letter-spacing: 1px; }
.admin-menu { flex: 1; border-right: none; }

/* 右侧内容区样式 */
.main-content { flex: 1; display: flex; flex-direction: column; background-color: #f0f2f5; }
.header { height: 60px; background-color: #fff; display: flex; align-items: center; justify-content: flex-end; padding: 0 20px; box-shadow: 0 1px 4px rgba(0,21,41,0.08); z-index: 10; }
.header-right { display: flex; align-items: center; gap: 20px; }
.welcome { font-size: 14px; color: #333; font-weight: 500; }
.page-content { flex: 1; padding: 20px; overflow-y: auto; }
</style>