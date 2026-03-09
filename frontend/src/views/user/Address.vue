<template>
  <div class="address-container">
    <div class="address-box">
      <div class="header-action">
        <h2 class="page-title">收货地址管理</h2>
        <el-button type="primary" color="#ffe60f" style="color: #333; font-weight: bold;" @click="openAddDialog">
          + 新增收货地址
        </el-button>
      </div>

      <div class="address-list" v-loading="loading">
        <el-empty v-if="addressList.length === 0" description="您还没有添加过收货地址哦~" />

        <el-card v-else v-for="item in addressList" :key="item.id" class="address-card" shadow="hover">
          <div class="card-content">
            <div class="info-top">
              <span class="receiver">{{ item.receiver }}</span>
              <span class="phone">{{ item.phone }}</span>
              <el-tag v-if="item.isDefault === 1" size="small" type="danger" effect="dark" style="margin-left: 10px;">默认</el-tag>
            </div>
            <div class="detail-addr">{{ item.detailAddress }}</div>
          </div>
          <div class="card-actions">
            <el-button link type="primary" @click="openEditDialog(item)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(item.id)">删除</el-button>
          </div>
        </el-card>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="addressForm.id ? '编辑收货地址' : '新增收货地址'" width="500px">
      <el-form :model="addressForm" label-width="90px">
        <el-form-item label="收件人" required>
          <el-input v-model="addressForm.receiver" placeholder="请输入收件人真实姓名" />
        </el-form-item>
        <el-form-item label="手机号码" required>
          <el-input v-model="addressForm.phone" placeholder="请输入11位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="详细地址" required>
          <el-input v-model="addressForm.detailAddress" type="textarea" :rows="3" placeholder="如：XX校区X区X栋XXX室" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="addressForm.isDefault" :active-value="1" :inactive-value="0" active-color="#ffe60f" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" color="#ffe60f" style="color: #333; font-weight: bold;" @click="submitAddress">
            保 存
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const userStore = useUserStore()
const addressList = ref([])
const loading = ref(false)
const dialogVisible = ref(false)

const addressForm = reactive({
  id: null,
  receiver: '',
  phone: '',
  detailAddress: '',
  isDefault: 0
})

// 获取地址列表
const fetchAddressList = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/address/list', {
      params: { userId: userStore.userInfo.id }
    })
    if (res.data.code === 200) {
      addressList.value = res.data.data
    }
  } catch (error) {
    ElMessage.error('获取地址失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => fetchAddressList())

// 打开新增弹窗
const openAddDialog = () => {
  addressForm.id = null
  addressForm.receiver = ''
  addressForm.phone = ''
  addressForm.detailAddress = ''
  addressForm.isDefault = 0
  dialogVisible.value = true
}

// 打开编辑弹窗
const openEditDialog = (item) => {
  Object.assign(addressForm, item) // 把当前行的数据拷贝到表单里
  dialogVisible.value = true
}

// 提交保存
const submitAddress = async () => {
  if (!addressForm.receiver || !addressForm.phone || !addressForm.detailAddress) {
    ElMessage.warning('请填写完整的地址信息！')
    return
  }
  const phoneReg = /^1[3-9]\d{9}$/
  if (!phoneReg.test(addressForm.phone)) {
    ElMessage.warning('手机号码格式不正确！')
    return
  }

  try {
    const res = await axios.post('/api/address/save', {
      ...addressForm,
      userId: userStore.userInfo.id
    })
    if (res.data.code === 200) {
      ElMessage.success('保存成功！')
      dialogVisible.value = false
      fetchAddressList() // 刷新列表
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

// 删除地址
const handleDelete = (id) => {
  ElMessageBox.confirm('确定要删除这条收货地址吗？', '提示', { type: 'warning' }).then(async () => {
    try {
      const res = await axios.delete(`/api/address/delete/${id}`)
      if (res.data.code === 200) {
        ElMessage.success('删除成功')
        fetchAddressList()
      }
    } catch (error) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}
</script>

<style scoped>
.address-container { display: flex; justify-content: center; }
.address-box { width: 800px; background: #fff; padding: 30px 40px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); min-height: 500px; }
.header-action { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; border-bottom: 2px solid #ffe60f; padding-bottom: 8px; }
.page-title { margin: 0; color: #333; }
.address-list { display: flex; flex-direction: column; gap: 15px; }
.address-card { border-radius: 8px; border: 1px solid #ebeef5; }
:deep(.el-card__body) { display: flex; justify-content: space-between; align-items: center; padding: 15px 20px; }
.info-top { margin-bottom: 8px; }
.receiver { font-size: 16px; font-weight: bold; color: #333; margin-right: 15px; }
.phone { font-size: 15px; color: #666; font-weight: bold; }
.detail-addr { font-size: 14px; color: #666; }
.card-actions { display: flex; gap: 10px; }
</style>