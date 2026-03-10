<template>
  <div class="user-layout">
    <div class="top-nav">
      <div class="nav-content">
        <div class="logo-area">校园闲置交易平台</div>
        <div class="menu-area">
          <router-link to="/user/home" class="nav-item">首页大盘</router-link>
          <router-link to="/user/market" class="nav-item">逛集市</router-link>
          <router-link to="/user/publish" class="nav-item">我要发布</router-link>
          <router-link to="/user/orders" class="nav-item">我的订单</router-link>
          <router-link to="/user/mygoods" class="nav-item">我的发布</router-link>
          <router-link to="/user/sellorders" class="nav-item">我卖出的</router-link>
          <router-link to="/user/address" class="nav-item">地址管理</router-link>
          <router-link to="/user/profile" class="nav-item">个人中心</router-link>
          <router-link to="/user/message" class="nav-item">
            <el-badge :value="unreadCount" :max="99" :hidden="unreadCount === 0">
              消息中心
            </el-badge>
          </router-link>
        </div>
        <div class="user-area">
          <span class="credit">信誉分: {{ userStore.userInfo.creditScore }}</span>
          <el-button type="info" size="small" plain @click="logout">退出登录</el-button>
        </div>
      </div>
    </div>

    <div class="main-content">
      <router-view />
    </div>

    <el-dialog
        v-model="showAuthDialog"
        title="🔒 强制实名认证"
        width="400px"
        :close-on-click-modal="false"
        :close-on-press-escape="false"
        :show-close="false"
        align-center
    >
      <div style="margin-bottom: 20px; color: #666; font-size: 14px;">
        为了保障平台交易安全，根据规定，发布和购买商品前必须完成实名认证。
      </div>
      <el-form :model="authForm" label-width="80px">
        <el-form-item label="真实姓名" required>
          <el-input v-model="authForm.realName" placeholder="请输入您的真实姓名" />
        </el-form-item>
        <el-form-item label="身份证号" required>
          <el-input v-model="authForm.idCard" placeholder="请输入18位身份证号" maxlength="18" />
        </el-form-item>
        <el-form-item label="手机号码" required>
          <el-input v-model="authForm.phone" placeholder="请输入11位手机号" maxlength="11" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="logout">退出登录</el-button>
          <el-button type="primary" color="#ffe60f" style="color: #333; font-weight: bold;" @click="submitAuth">
            立即认证
          </el-button>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const userStore = useUserStore()

// 控制实名认证弹窗的显示
const showAuthDialog = ref(false)

// 认证表单数据
const authForm = reactive({
  realName: '',
  idCard: '',
  phone: ''
})

// 页面加载时检查是否已经实名
onMounted(() => {
  // 如果没有真实姓名，就强制弹窗
  if (!userStore.userInfo.realName) {
    showAuthDialog.value = true
  }
})

// 提交实名认证
const submitAuth = async () => {
  if (!authForm.realName || !authForm.idCard || !authForm.phone) {
    ElMessage.warning('姓名、身份证号和手机号必须填写！')
    return
  }

  // 1. 真实姓名校验：2-10个汉字
  const nameReg = /^[\u4e00-\u9fa5]{2,10}$/
  if (!nameReg.test(authForm.realName)) {
    ElMessage.warning('真实姓名格式错误，请输入2-10个汉字！')
    return
  }

  // 2. 身份证号严格校验
  const idCardReg = /^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[1-2]\d|3[0-1])\d{3}[\dX]$/i
  if (!idCardReg.test(authForm.idCard)) {
    ElMessage.warning('身份证号格式不合法，请输入正确的18位身份证号！')
    return
  }

  // 3. 手机号严格校验：必须是1开头，第二位是3-9，共11位数字
  const phoneReg = /^1[3-9]\d{9}$/
  if (!phoneReg.test(authForm.phone)) {
    ElMessage.warning('手机号码格式不合法！')
    return
  }

  try {
    const res = await axios.post('/api/user/auth', {
      id: userStore.userInfo.id,
      realName: authForm.realName,
      // 统一将身份证最后一位的 x 转为大写 X 存入数据库，方便后续比对
      idCard: authForm.idCard.toUpperCase(),
      phone: authForm.phone
    })

    if (res.data.code === 200) {
      ElMessage.success('实名认证成功！可以开始使用平台啦。')
      // 更新 Pinia 中的用户信息
      userStore.setUserInfo(res.data.data)
      // 关闭弹窗
      showAuthDialog.value = false
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (error) {
    ElMessage.error('网络请求失败')
  }
}
// 🌟 新增：获取未读消息数量逻辑
const unreadCount = ref(0)
const fetchUnreadCount = async () => {
  if (!userStore.userInfo.id) return
  try {
    const res = await axios.get('/api/notification/unreadCount', {
      params: { userId: userStore.userInfo.id }
    })
    if (res.data.code === 200) {
      unreadCount.value = res.data.data
    }
  } catch (error) {
    console.error('获取未读消息数量失败')
  }
}

// 页面加载时拉取一次未读数量
onMounted(() => {
  // 原有的实名认证弹窗逻辑...
  if (!userStore.userInfo.realName) {
    showAuthDialog.value = true
  }
  // 🌟 新增拉取数量
  fetchUnreadCount()
})
// 退出登录
const logout = () => {
  userStore.clearUserInfo()
  router.push('/login')
}
</script>

<style scoped>
.user-layout { min-height: 100vh; background-color: #f5f5f5; }
.top-nav { background-color: #409EFF; height: 60px; display: flex; justify-content: center; box-shadow: 0 2px 8px rgba(0,0,0,0.1); position: sticky; top: 0; z-index: 999; }
.nav-content { width: 1200px; display: flex; justify-content: space-between; align-items: center; }
.logo-area { font-size: 22px; font-weight: bold; color: #333; }
.menu-area { display: flex; gap: 30px; }
.nav-item { text-decoration: none; color: #333; font-size: 16px; font-weight: 500; transition: all 0.3s; }
.nav-item:hover { color: #f5f5f5; }
.nav-item.router-link-active { font-weight: bold; border-bottom: 2px solid #333; padding-bottom: 4px; }
.user-area { display: flex; align-items: center; gap: 15px; }
.credit { font-weight: bold; color: #333; font-size: 14px; background: rgba(255,255,255,0.5); padding: 4px 10px; border-radius: 12px; }
.main-content { width: 1200px; margin: 20px auto; }
</style>