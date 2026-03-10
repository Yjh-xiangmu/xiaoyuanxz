<template>
  <div class="profile-container">
    <div class="profile-box">
      <h2 class="page-title">个人中心</h2>

      <div class="user-card">
        <div class="avatar-circle">
          <img v-if="userStore.userInfo.avatar" :src="userStore.userInfo.avatar" style="width: 100%; height: 100%; border-radius: 50%; object-fit: cover;" />
          <span v-else>{{ (userStore.userInfo.realName || userStore.userInfo.username).charAt(0).toUpperCase() }}</span>
        </div>
        <div class="user-brief">
          <h3>{{ userStore.userInfo.realName || userStore.userInfo.username }}</h3>
          <p>学号/登录账号：{{ userStore.userInfo.username }}</p>
        </div>
        <div class="credit-score">
          <div class="score-label">当前信誉分</div>
          <div class="score-value" :class="{ 'text-danger': userStore.userInfo.creditScore < 80 }">
            {{ userStore.userInfo.creditScore }}
          </div>
        </div>
      </div>

      <el-form :model="profileForm" label-width="110px" class="form-content">

        <el-form-item label="上传新头像">
          <el-upload
              class="avatar-uploader"
              action="http://localhost:8080/api/file/upload"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
              accept="image/*"
          >
            <img v-if="profileForm.avatar" :src="profileForm.avatar" class="avatar-preview" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div class="tip-text" style="display: block; margin-top: 5px;">点击框内上传新头像 (建议比例 1:1)</div>
        </el-form-item>

        <el-form-item label="学号">
          <el-input v-model="profileForm.username" disabled />
          <div class="tip-text" style="line-height: 1.4; margin-top: 5px;">
            此项为您登录系统的唯一凭证，不可修改。
          </div>
        </el-form-item>

        <el-form-item label="真实姓名">
          <el-input v-model="profileForm.realName" placeholder="未实名" disabled />
          <span class="tip-text" style="margin-left: 10px;">实名信息不可自行修改。</span>
        </el-form-item>

        <el-form-item label="登录密码" required>
          <el-input v-model="profileForm.password" type="password" show-password placeholder="如需修改请在此输入，不修改请保持原密码" />
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
import { Plus } from '@element-plus/icons-vue'
import axios from 'axios'

const userStore = useUserStore()
const router = useRouter()

const profileForm = reactive({
  id: null,
  username: '',
  password: '',
  realName: '',
  avatar: ''
})

onMounted(() => {
  profileForm.id = userStore.userInfo.id
  profileForm.username = userStore.userInfo.username
  profileForm.password = userStore.userInfo.password
  profileForm.realName = userStore.userInfo.realName
  profileForm.avatar = userStore.userInfo.avatar || ''
})

const handleAvatarSuccess = (res) => {
  if (res.code === 200) {
    profileForm.avatar = res.data
    ElMessage.success('头像图片上传成功，请点击【保存修改】生效！')
  } else {
    ElMessage.error('图片上传失败')
  }
}

const submitUpdate = async () => {
  // 🌟 核心修改：去掉了 username 的非空校验，因为它现在是直接读取且不可更改的
  if (!profileForm.password) {
    ElMessage.warning('密码不能为空！')
    return
  }
  try {
    const res = await axios.post('/api/user/update', {
      id: profileForm.id,
      username: profileForm.username,
      password: profileForm.password,
      avatar: profileForm.avatar
    })

    if (res.data.code === 200) {
      ElMessageBox.alert('个人信息(含头像)修改成功，为了安全和数据同步，请重新登录。', '提示', {
        confirmButtonText: '去登录',
        type: 'success',
        callback: () => {
          userStore.clearUserInfo()
          router.push('/login')
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

.user-card { display: flex; align-items: center; background: #fafafa; padding: 20px 30px; border-radius: 12px; margin-bottom: 30px; border: 1px solid #ebeef5; }
.avatar-circle { width: 60px; height: 60px; background-color: #ffe60f; color: #333; font-size: 28px; font-weight: bold; border-radius: 50%; display: flex; justify-content: center; align-items: center; margin-right: 20px; overflow: hidden;}
.user-brief { flex: 1; }
.user-brief h3 { margin: 0 0 5px 0; color: #333; font-size: 20px; }
.user-brief p { margin: 0; color: #999; font-size: 14px; }
.credit-score { text-align: right; }
.score-label { font-size: 13px; color: #666; margin-bottom: 5px; }
.score-value { font-size: 32px; font-weight: bold; color: #67c23a; }
.text-danger { color: #f56c6c; }

.form-content { width: 500px; margin-top: 20px; }
.tip-text { font-size: 12px; color: #999; }

/* 头像上传组件样式 */
.avatar-uploader { display: inline-block; cursor: pointer; }
:deep(.avatar-uploader .el-upload) { border: 1px dashed #d9d9d9; border-radius: 50%; cursor: pointer; position: relative; overflow: hidden; width: 80px; height: 80px; transition: border-color 0.3s; background: #fafafa; }
:deep(.avatar-uploader .el-upload:hover) { border-color: #ffe60f; }
.avatar-preview { width: 80px; height: 80px; display: block; object-fit: cover; }
.avatar-uploader-icon { font-size: 24px; color: #8c939d; width: 80px; height: 80px; text-align: center; line-height: 80px; }
</style>