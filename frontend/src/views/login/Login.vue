<template>
  <div class="login-wrapper">
    <ul class="bg-bubbles">
      <li v-for="n in 10" :key="n"></li>
    </ul>

    <div class="login-container">
      <div class="left-panel">
        <div class="brand-box">
          <div class="logo-icon">📦</div>
          <h2>校园闲置集市</h2>
          <p>让每一件闲置物品，都能在校园里找到它的新主人。</p>
        </div>
      </div>

      <div class="right-panel">
        <transition name="slide-fade" mode="out-in">

          <div class="form-box" v-if="isLogin" key="login">
            <h3 class="form-title">欢迎回来</h3>
            <p class="form-subtitle">请输入您的学号与密码登录系统</p>

            <el-form :model="loginForm" @keyup.enter="handleLogin">
              <el-form-item>
                <el-input v-model="loginForm.username" placeholder="请输入学号" size="large" :prefix-icon="User" />
              </el-form-item>
              <el-form-item>
                <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" size="large" show-password :prefix-icon="Lock" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" class="submit-btn" :loading="loading" @click="handleLogin">
                  登 录
                </el-button>
              </el-form-item>
            </el-form>

            <div class="switch-action">
              还没有账号？ <span class="link" @click="isLogin = false">立即注册 ❯</span>
            </div>
          </div>

          <div class="form-box" v-else key="register">
            <h3 class="form-title">加入集市</h3>
            <p class="form-subtitle">只需几秒钟，开启校园绿色交易</p>

            <el-form :model="registerForm">
              <el-form-item>
                <el-input v-model="registerForm.username" placeholder="请输入12位学号 (作为登录账号)" size="large" :prefix-icon="User" maxlength="12" />
              </el-form-item>
              <el-form-item>
                <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" size="large" show-password :prefix-icon="Lock" />
              </el-form-item>
              <el-form-item>
                <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请再次确认密码" size="large" show-password :prefix-icon="Lock" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" class="submit-btn" :loading="loading" @click="handleRegister">
                  注 册
                </el-button>
              </el-form-item>
            </el-form>

            <div class="switch-action">
              已有账号？ <span class="link" @click="isLogin = true">❮ 返回登录</span>
            </div>
          </div>

        </transition>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import axios from 'axios'

const router = useRouter()
const userStore = useUserStore()

const isLogin = ref(true)
const loading = ref(false)

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', confirmPassword: '' })

// 登录逻辑
const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入学号和密码')
    return
  }
  loading.value = true
  try {
    const res = await axios.post('/api/user/login', loginForm)
    if (res.data.code === 200) {
      ElMessage.success('登录成功！')
      userStore.setUserInfo(res.data.data)

      if (res.data.data.role === 'ADMIN') {
        router.push('/admin')
      } else {
        router.push('/user')
      }
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (error) {
    ElMessage.error('网络请求失败，请检查后端服务')
  } finally {
    loading.value = false
  }
}

// 注册逻辑
const handleRegister = async () => {
  if (!registerForm.username || !registerForm.password) {
    ElMessage.warning('学号和密码不能为空')
    return
  }

  // 🌟 核心新增：严格校验必须是 12 位纯数字
  const studentIdRegex = /^\d{12}$/
  if (!studentIdRegex.test(registerForm.username)) {
    ElMessage.warning('注册失败：学号必须是严格的12位纯数字！')
    return
  }

  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }

  loading.value = true
  try {
    const res = await axios.post('/api/user/register', {
      username: registerForm.username,
      password: registerForm.password
    })
    if (res.data.code === 200) {
      ElMessage.success('注册成功，请登录！')
      isLogin.value = true
      loginForm.username = registerForm.username
      loginForm.password = ''
      registerForm.username = ''
      registerForm.password = ''
      registerForm.confirmPassword = ''
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (error) {
    ElMessage.error('注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* =========== 整体页面与背景 =========== */
.login-wrapper {
  position: relative;
  width: 100vw;
  height: 100vh;
  /* 🌟 底色改为淡蓝色系 */
  background-color: #f0f5fa;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
}

/* 🌟 动态气泡动画 (颜色全部替换为半透明的蓝色 rgb(64,158,255)) */
.bg-bubbles {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%; z-index: 0; margin: 0; padding: 0;
}
.bg-bubbles li {
  position: absolute; list-style: none; display: block;
  width: 40px; height: 40px;
  background-color: rgba(64, 158, 255, 0.2);
  bottom: -160px;
  animation: square 25s infinite; transition-timing-function: linear; border-radius: 20%;
}
.bg-bubbles li:nth-child(1) { left: 10%; }
.bg-bubbles li:nth-child(2) { left: 20%; width: 80px; height: 80px; animation-delay: 2s; animation-duration: 17s; }
.bg-bubbles li:nth-child(3) { left: 25%; animation-delay: 4s; }
.bg-bubbles li:nth-child(4) { left: 40%; width: 60px; height: 60px; animation-duration: 22s; background-color: rgba(64, 158, 255, 0.3); }
.bg-bubbles li:nth-child(5) { left: 70%; }
.bg-bubbles li:nth-child(6) { left: 80%; width: 120px; height: 120px; animation-delay: 3s; background-color: rgba(64, 158, 255, 0.1); }
.bg-bubbles li:nth-child(7) { left: 32%; width: 160px; height: 160px; animation-delay: 7s; }
.bg-bubbles li:nth-child(8) { left: 55%; width: 20px; height: 20px; animation-delay: 15s; animation-duration: 40s; }
.bg-bubbles li:nth-child(9) { left: 25%; width: 10px; height: 10px; animation-delay: 2s; animation-duration: 40s; background-color: rgba(64, 158, 255, 0.4); }
.bg-bubbles li:nth-child(10) { left: 90%; width: 160px; height: 160px; animation-delay: 11s; }

@keyframes square {
  0% { transform: translateY(0) rotate(0deg); opacity: 1; }
  100% { transform: translateY(-1000px) rotate(600deg); opacity: 0; }
}

/* =========== 登录卡片主体 =========== */
.login-container {
  position: relative;
  z-index: 1;
  width: 850px;
  height: 500px;
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.08);
  display: flex;
  overflow: hidden;
}

/* 左侧品牌区 (🌟 渐变色改为经典蓝到浅蓝) */
.left-panel {
  width: 45%;
  background: linear-gradient(135deg, #409EFF 0%, #79bbff 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px;
  /* 🌟 文字改为白色 */
  color: #ffffff;
  text-align: center;
}
.logo-icon { font-size: 60px; margin-bottom: 20px; }
.brand-box h2 { font-size: 28px; font-weight: bold; margin-bottom: 15px; letter-spacing: 2px;}
.brand-box p { font-size: 15px; line-height: 1.6; opacity: 0.9; }

/* 右侧表单区 */
.right-panel {
  width: 55%;
  padding: 50px 60px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: #fff;
}

/* Vue 切换动画定义 (保持原样不动) */
.slide-fade-enter-active, .slide-fade-leave-active { transition: all 0.4s ease; }
.slide-fade-enter-from { opacity: 0; transform: translateX(30px); }
.slide-fade-leave-to { opacity: 0; transform: translateX(-30px); }

/* 表单内容细节 */
.form-title { font-size: 26px; color: #333; margin: 0 0 10px 0; }
.form-subtitle { font-size: 14px; color: #999; margin-bottom: 30px; }

:deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #ebeef5 inset;
  transition: all 0.3s;
}
:deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px #409EFF inset !important; }

/* 🌟 按钮颜色改为科技蓝 */
.submit-btn {
  width: 100%;
  height: 45px;
  background-color: #409EFF;
  border-color: #409EFF;
  /* 🌟 按钮文字改为白色 */
  color: #fff;
  font-size: 16px;
  font-weight: bold;
  border-radius: 8px;
  margin-top: 10px;
  transition: all 0.3s;
}
.submit-btn:hover {
  background-color: #66b1ff;
  border-color: #66b1ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
}

.switch-action { text-align: center; margin-top: 25px; font-size: 14px; color: #666; }
/* 🌟 底部链接文字改为蓝色 */
.link { color: #409EFF; font-weight: bold; cursor: pointer; transition: color 0.3s; }
.link:hover { color: #66b1ff; text-decoration: underline; }
</style>