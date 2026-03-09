<template>
  <div class="profile-container">
    <div class="profile-box">
      <h2 class="page-title">个人中心</h2>

      <div class="user-card">
        <div class="avatar-circle">
          {{ userStore.userInfo.username.charAt(0).toUpperCase() }}
        </div>
        <div class="user-brief">
          <h3>{{ userStore.userInfo.username }}</h3>
          <p>学号/工号：{{ userStore.userInfo.studentId }}</p>
        </div>
        <div class="credit-score">
          <div class="score-label">当前信誉分</div>
          <div class="score-value" :class="{ 'text-danger': userStore.userInfo.creditScore < 80 }">
            {{ userStore.userInfo.creditScore }}
          </div>
        </div>
      </div>

      <el-form :model="profileForm" label-width="100px" class="form-content">
        <el-form-item label="用户昵称" required>
          <el-input v-model="profileForm.username" placeholder="请输入新的昵称" />
        </el-form-item>

        <el-form-item label="真实姓名">
          <el-input v-model="profileForm.realName" placeholder="已实名认证" disabled />
          <span class="tip-text">实名信息不可自行修改，如需变更请联系管理员。</span>
        </el-form-item>

        <el-form-item label="登录密码" required>
          <el-input v-model="profileForm.password" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" color="#ffe60f" style="color: #333; font-weight: bold; width: 120px;" @click="submitUpdate">
            保存修改
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const userStore = useUserStore()
const router = useRouter()

// 初始化表单数据，把当前缓存里的用户信息填进去
const profileForm = reactive({
  id: null,
  username: '',
  password: '',
  realName: ''
})

onMounted(() => {
  profileForm.id = userStore.userInfo.id
  profileForm.username = userStore.userInfo.username
  profileForm.password = userStore.userInfo.password
  profileForm.realName = userStore.userInfo.realName
})

const submitUpdate = async () => {
  if (!profileForm.username || !profileForm.password) {
    ElMessage.warning('昵称和密码不能为空！')
    return
  }

  try {
    const res = await axios.post('/api/user/update', {
      id: profileForm.id,
      username: profileForm.username,
      password: profileForm.password
    })

    if (res.data.code === 200) {
      ElMessageBox.alert('个人信息修改成功，为了安全起见，请重新登录。', '提示', {
        confirmButtonText: '去登录',
        type: 'success',
        callback: () => {
          userStore.clearUserInfo() // 清除本地登录状态
          router.push('/login')     // 踢回登录页
        }
      })
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (error) {
    ElMessage.error('修改失败，网络异常')
  }
}
</script>

<style scoped>
.profile-container { display: flex; justify-content: center; }
.profile-box { width: 800px; background: #fff; padding: 30px 40px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); min-height: 500px; }
.page-title { margin-top: 0; margin-bottom: 20px; color: #333; border-bottom: 2px solid #ffe60f; padding-bottom: 8px; display: inline-block; }

/* 顶部用户卡片 */
.user-card { display: flex; align-items: center; background: #fafafa; padding: 20px 30px; border-radius: 12px; margin-bottom: 30px; border: 1px solid #ebeef5; }
.avatar-circle { width: 60px; height: 60px; background-color: #ffe60f; color: #333; font-size: 28px; font-weight: bold; border-radius: 50%; display: flex; justify-content: center; align-items: center; margin-right: 20px; }
.user-brief { flex: 1; }
.user-brief h3 { margin: 0 0 5px 0; color: #333; font-size: 20px; }
.user-brief p { margin: 0; color: #999; font-size: 14px; }

/* 信誉分展示 */
.credit-score { text-align: right; }
.score-label { font-size: 13px; color: #666; margin-bottom: 5px; }
.score-value { font-size: 32px; font-weight: bold; color: #67c23a; }
.text-danger { color: #f56c6c; } /* 低于80分变红 */

.form-content { width: 500px; margin-top: 20px; }
.tip-text { font-size: 12px; color: #999; margin-left: 10px; }
</style>