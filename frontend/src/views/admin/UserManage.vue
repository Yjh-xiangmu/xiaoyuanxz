<template>
  <div class="admin-user-container">
    <h2 style="margin-top: 0; margin-bottom: 20px;">用户信息与违规管理</h2>

    <div class="filter-box">
      <el-input
          v-model="searchKeyword"
          placeholder="输入用户名或真实姓名搜索..."
          clearable
          style="width: 350px;"
          @keyup.enter="fetchUsers"
          @clear="fetchUsers"
      >
        <template #append>
          <el-button @click="fetchUsers">搜 索</el-button>
        </template>
      </el-input>
    </div>

    <el-table :data="userList" border stripe style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="用户ID" width="80" align="center" />
      <el-table-column prop="username" label="用户名/学号" />
      <el-table-column prop="realName" label="真实姓名" width="120" align="center">
        <template #default="scope">{{ scope.row.realName || '未实名' }}</template>
      </el-table-column>
      <el-table-column prop="phone" label="联系电话" width="130" align="center">
        <template #default="scope">{{ scope.row.phone || '-' }}</template>
      </el-table-column>

      <el-table-column prop="creditScore" label="信誉分" width="100" align="center">
        <template #default="scope">
          <span :style="{ color: scope.row.creditScore < 80 ? '#f56c6c' : '#67c23a', fontWeight: 'bold' }">
            {{ scope.row.creditScore }}
          </span>
        </template>
      </el-table-column>

      <el-table-column label="账号状态" width="100" align="center">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 1" type="success">正常</el-tag>
          <el-tag v-else type="danger">已封禁</el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="createTime" label="注册时间" width="170" align="center" />

      <el-table-column label="平台操作" width="220" align="center" fixed="right">
        <template #default="scope">
          <el-button size="small" type="primary" plain @click="openScoreDialog(scope.row)">调分</el-button>

          <el-button v-if="scope.row.status === 1" size="small" type="danger" @click="handleStatus(scope.row.id, 0)">
            封禁账号
          </el-button>
          <el-button v-else size="small" type="success" @click="handleStatus(scope.row.id, 1)">
            解除封禁
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="scoreDialogVisible" :title="'调整信誉分 - ' + currentUser.username" width="400px">
      <el-form label-position="top">
        <el-form-item label="直接设定新的信誉分 (最高100)" required>
          <el-input-number v-model="scoreForm.score" :min="0" :max="100" :precision="1" :step="1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="操作原因 (将通过系统通知发给用户)" required>
          <el-input v-model="scoreForm.reason" type="textarea" :rows="3" placeholder="例如：违规发布商品扣除5分 / 协助处理纠纷奖励5分" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scoreDialogVisible = false">取 消</el-button>
        <el-button type="primary" color="#ff5000" style="color: white; font-weight: bold;" @click="submitScoreAdjust">
          确 认 执 行
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const userList = ref([])
const loading = ref(false)
const searchKeyword = ref('')

// 获取全平台用户列表
const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/user/list', { params: { keyword: searchKeyword.value } })
    if (res.data.code === 200) userList.value = res.data.data
  } catch (error) {
    ElMessage.error('获取列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => fetchUsers())

// 🌟 封禁与解封逻辑 (完全适配你的 1=正常, 0=封禁 逻辑)
const handleStatus = (id, status) => {
  const actionText = status === 0 ? '强制封禁该账号' : '解除该账号的封禁'
  ElMessageBox.confirm(`确定要${actionText}吗？系统将自动下发通知。`, '危险操作确认', {
    confirmButtonText: '确定执行',
    cancelButtonText: '取消',
    type: status === 0 ? 'error' : 'warning'
  }).then(async () => {
    try {
      const res = await axios.post('/api/user/changeStatus', { id, status })
      if (res.data.code === 200) {
        ElMessage.success(res.data.msg)
        fetchUsers() // 刷新列表，状态标签会随之改变
      } else {
        ElMessage.error(res.data.msg)
      }
    } catch (error) {
      ElMessage.error('操作失败')
    }
  }).catch(() => {})
}

// 🌟 手动调分逻辑
const scoreDialogVisible = ref(false)
const currentUser = ref({})
const scoreForm = reactive({ score: 100, reason: '' })

const openScoreDialog = (row) => {
  currentUser.value = row
  scoreForm.score = row.creditScore
  scoreForm.reason = ''
  scoreDialogVisible.value = true
}

const submitScoreAdjust = async () => {
  if (!scoreForm.reason.trim()) {
    ElMessage.warning('必须填写操作原因！')
    return
  }
  try {
    const res = await axios.post('/api/user/adjustScore', {
      id: currentUser.value.id,
      score: scoreForm.score,
      reason: scoreForm.reason
    })
    if (res.data.code === 200) {
      ElMessage.success('信誉分已更新并通知用户！')
      scoreDialogVisible.value = false
      fetchUsers() // 刷新列表看最新分数
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (error) {
    ElMessage.error('调整失败')
  }
}
</script>

<style scoped>
.admin-user-container { padding: 10px; }
.filter-box { margin-bottom: 20px; background-color: #fff; padding: 15px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); display: flex; align-items: center; }
</style>