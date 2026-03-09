<template>
  <div class="login-container">
    <div class="login-box">
      <div class="title-container">
        <h2>校园闲置交易平台</h2>
        <p>让闲置物品重新发光</p>
      </div>

      <el-form v-if="isLogin" :model="loginForm" class="form-wrapper">
        <el-form-item>
          <el-input v-model="loginForm.username" placeholder="请输入用户名/学号" size="large" clearable />
        </el-form-item>
        <el-form-item>
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" size="large" show-password />
        </el-form-item>
        <el-button type="primary" class="xy-btn" size="large" @click="handleLogin">登 录</el-button>
        <div class="switch-text">
          还没有账号？ <span class="highlight" @click="isLogin = false">立即注册</span>
        </div>
      </el-form>

      <el-form v-else :model="registerForm" class="form-wrapper">
        <el-form-item>
          <el-input v-model="registerForm.username" placeholder="设置用户名/学号" size="large" clearable />
        </el-form-item>
        <el-form-item>
          <el-input v-model="registerForm.password" type="password" placeholder="设置密码" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" size="large" show-password />
        </el-form-item>
        <el-button type="primary" class="xy-btn" size="large" @click="handleRegister">注 册</el-button>
        <div class="switch-text">
          已有账号？ <span class="highlight" @click="isLogin = true">返回登录</span>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

axios.defaults.baseURL = 'http://localhost:8080'
const router = useRouter()
const userStore = useUserStore()
const isLogin = ref(true)

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', confirmPassword: '' })

const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  try {
    const res = await axios.post('/api/user/login', loginForm)
    if (res.data.code === 200) {
      ElMessage.success('登录成功！')
      userStore.setUserInfo(res.data.data) // 存入 Pinia

      // 核心逻辑：根据角色分别跳转到不同的骨架路由
      if (res.data.data.role === 'ADMIN') {
        router.push('/admin/dashboard')
      } else {
        router.push('/user/home')
      }
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (error) {
    ElMessage.error('网络请求失败，请检查后端是否启动')
  }
}

const handleRegister = async () => {
  if (!registerForm.username || !registerForm.password) {
    ElMessage.warning('请填写完整的注册信息')
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.error('两次输入的密码不一致！')
    return
  }
  try {
    const res = await axios.post('/api/user/register', {
      username: registerForm.username,
      password: registerForm.password
    })
    if (res.data.code === 200) {
      ElMessage.success('注册成功！请登录')
      isLogin.value = true
      loginForm.username = registerForm.username
      loginForm.password = ''
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (error) {
    ElMessage.error('网络请求失败，请检查后端是否启动')
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
.login-box {
  width: 400px;
  background: #ffffff;
  padding: 40px;
  border-radius: 20px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.05);
}
.title-container {
  text-align: center;
  margin-bottom: 30px;
}
.title-container h2 { margin: 0; font-size: 24px; color: #333; }
.title-container p { margin: 8px 0 0; font-size: 14px; color: #999; }
.form-wrapper .el-form-item { margin-bottom: 24px; }
.xy-btn {
  width: 100%; background-color: #ffe60f !important; border-color: #ffe60f !important;
  color: #333 !important; font-weight: bold; border-radius: 24px;
  margin-top: 10px; height: 44px; font-size: 16px; transition: all 0.3s;
}
.xy-btn:hover { background-color: #ffd000 !important; border-color: #ffd000 !important; transform: translateY(-1px); }
.switch-text { text-align: center; margin-top: 20px; font-size: 14px; color: #666; }
.switch-text .highlight { color: #333; font-weight: bold; cursor: pointer; text-decoration: underline; text-decoration-color: #ffe60f; text-decoration-thickness: 3px; }
:deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px #333 inset !important; }
</style>