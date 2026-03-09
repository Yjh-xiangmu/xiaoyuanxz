<template>
  <el-container class="admin-layout">
    <el-aside width="200px" class="sidebar">
      <div class="logo">平台管理后台</div>
      <el-menu :default-active="$route.path" router background-color="#2c3e50" text-color="#fff" active-text-color="#409eff">
        <el-menu-item index="/admin/dashboard">数据大盘</el-menu-item>
        <el-menu-item index="/admin/audit">审核中心</el-menu-item>
        <el-menu-item index="/admin/users">用户与信誉治理</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="title">管理员工作台</span>
        <div>
          <span style="margin-right: 20px">Admin</span>
          <el-button size="small" type="danger" @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main>
        <router-view /> </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
const router = useRouter()
const userStore = useUserStore()
const logout = () => {
  userStore.clearUserInfo()
  router.push('/login')
}
</script>

<style scoped>
.admin-layout { height: 100vh; }
.sidebar { background-color: #2c3e50; }
.logo { height: 60px; line-height: 60px; text-align: center; color: white; font-weight: bold; font-size: 18px; border-bottom: 1px solid #1a252f; }
.header { background-color: white; display: flex; justify-content: space-between; align-items: center; box-shadow: 0 2px 4px rgba(0,0,0,0.1); z-index: 1;}
.title { font-weight: bold; font-size: 18px; }
</style>