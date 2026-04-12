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
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const userStore = useUserStore()

// ==================== 实名认证弹窗 ====================
const showAuthDialog = ref(false)
const authForm = reactive({ realName: '', idCard: '', phone: '' })

const submitAuth = async () => {
  if (!authForm.realName || !authForm.idCard || !authForm.phone) {
    ElMessage.warning('姓名、身份证号和手机号必须填写！')
    return
  }
  const nameReg = /^[\u4e00-\u9fa5]{2,10}$/
  if (!nameReg.test(authForm.realName)) {
    ElMessage.warning('真实姓名格式错误，请输入2-10个汉字！')
    return
  }
  const idCardReg = /^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[1-2]\d|3[0-1])\d{3}[\dX]$/i
  if (!idCardReg.test(authForm.idCard)) {
    ElMessage.warning('身份证号格式不合法，请输入正确的18位身份证号！')
    return
  }
  const phoneReg = /^1[3-9]\d{9}$/
  if (!phoneReg.test(authForm.phone)) {
    ElMessage.warning('手机号码格式不合法！')
    return
  }
  try {
    const res = await axios.post('/api/user/update', {
      id: userStore.userInfo.id,
      realName: authForm.realName,
      idCard: authForm.idCard.toUpperCase(),
      phone: authForm.phone
    })
    if (res.data.code === 200) {
      ElMessage.success('实名认证成功！可以开始使用平台啦。')
      userStore.userInfo.realName = authForm.realName
      userStore.userInfo.idCard = authForm.idCard.toUpperCase()
      userStore.userInfo.phone = authForm.phone
      showAuthDialog.value = false
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (error) {
    ElMessage.error('网络请求失败')
  }
}

// ==================== 未读消息角标（系统通知 + 私信合并） ====================
const unreadCount = ref(0)
let pollingTimer = null

const fetchUnreadCount = async () => {
  if (!userStore.userInfo.id) return
  try {
    // 并发请求：系统通知未读数 + 私信会话列表
    const [res1, res2] = await Promise.all([
      axios.get('/api/notification/unreadCount', {
        params: { userId: userStore.userInfo.id }
      }),
      axios.get('/api/chat/sessions', {
        params: { userId: userStore.userInfo.id }
      })
    ])

    // 系统通知未读数
    const sysUnread = (res1.data.code === 200) ? Number(res1.data.data) : 0

    // 所有私信会话的未读数相加
    let chatUnread = 0
    if (res2.data.code === 200 && Array.isArray(res2.data.data)) {
      chatUnread = res2.data.data.reduce((sum, session) => sum + (session.unreadCount || 0), 0)
    }

    unreadCount.value = sysUnread + chatUnread
  } catch (error) {
    // 静默失败，不影响主流程
  }
}

// ==================== 退出登录 ====================
const logout = () => {
  userStore.clearUserInfo()
  router.push('/login')
}

// ==================== 生命周期 ====================
onMounted(() => {
  // 检查是否需要实名认证
  if (!userStore.userInfo.realName) {
    showAuthDialog.value = true
  }

  // 立即拉取一次未读数
  fetchUnreadCount()

  // 每 30 秒自动轮询刷新，保持角标实时
  pollingTimer = setInterval(fetchUnreadCount, 30000)
})

onUnmounted(() => {
  // 组件销毁时清除定时器，防止内存泄漏
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
})
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