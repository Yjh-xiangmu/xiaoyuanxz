<template>
  <div class="my-goods-container">
    <div class="my-goods-box">
      <h2 class="page-title">我发布的闲置</h2>
      <el-alert title="风控提示：修改商品标题、价格或描述后，为保障交易安全，商品将重新进入【待审核】状态。" type="warning" show-icon style="margin-bottom: 20px;" />

      <el-table :data="goodsList" border stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="title" label="商品标题" show-overflow-tooltip />
        <el-table-column prop="price" label="当前售价" width="100" align="center">
          <template #default="scope">
            <span style="color: #ff5000; font-weight: bold;">￥{{ scope.row.price }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 0" type="warning">待审核</el-tag>
            <el-tag v-else-if="scope.row.status === 1" type="success">展示中</el-tag>
            <el-tag v-else-if="scope.row.status === 2" type="info">已驳回</el-tag>
            <el-tag v-else-if="scope.row.status === 3" type="danger">已下架</el-tag>
            <el-tag v-else-if="scope.row.status === 4" type="info">已售出</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="260" align="center" fixed="right">
          <template #default="scope">
            <el-button v-if="scope.row.status !== 4" size="small" type="primary" @click="openEditDialog(scope.row)">
              编辑
            </el-button>

            <el-button v-if="scope.row.status === 1" size="small" type="warning" plain @click="handleStatusChange(scope.row.id, 3)">
              下架
            </el-button>

            <el-button v-if="scope.row.status === 3" size="small" type="success" plain @click="handleStatusChange(scope.row.id, 1)">
              重新上架
            </el-button>

            <el-button v-if="scope.row.status !== 4 && scope.row.status !== 1" size="small" type="danger" @click="handleDelete(scope.row.id)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="editVisible" title="编辑商品信息" width="500px">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="商品标题" required>
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item label="售卖价格" required>
          <el-input-number v-model="editForm.price" :precision="2" :step="1" :min="0" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="商品描述" required>
          <el-input v-model="editForm.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <div style="color: #f56c6c; font-size: 13px; text-align: center; margin-top: 10px;">
        ⚠️ 确认修改后，商品将自动下架并重新提交管理员审核。
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editVisible = false">取 消</el-button>
          <el-button type="primary" @click="submitEdit">确 认 修 改</el-button>
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
const goodsList = ref([])
const loading = ref(false)

const editVisible = ref(false)
const editForm = reactive({ id: null, title: '', price: 0, description: '' })

// 拉取列表
const fetchMyGoods = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/goods/myList', { params: { sellerId: userStore.userInfo.id } })
    if (res.data.code === 200) goodsList.value = res.data.data
  } catch (error) {
    ElMessage.error('获取列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => fetchMyGoods())

// 🌟 打开编辑弹窗
const openEditDialog = (row) => {
  editForm.id = row.id
  editForm.title = row.title
  editForm.price = row.price
  editForm.description = row.description
  editVisible.value = true
}

// 🌟 提交编辑 (打回待审核)
const submitEdit = async () => {
  if (!editForm.title || !editForm.price || !editForm.description) {
    ElMessage.warning('必填项不能为空！')
    return
  }
  try {
    const res = await axios.post('/api/goods/update', editForm)
    if (res.data.code === 200) {
      ElMessage.success(res.data.msg)
      editVisible.value = false
      fetchMyGoods() // 刷新列表，状态会变成黄色的“待审核”
    }
  } catch (error) { ElMessage.error('修改失败') }
}

// 🌟 上下架快捷操作
const handleStatusChange = async (id, status) => {
  const actionText = status === 1 ? '重新上架' : '下架'
  ElMessageBox.confirm(`确定要${actionText}该商品吗？`, '提示', { type: 'warning' }).then(async () => {
    try {
      const res = await axios.post('/api/goods/changeStatus', { id, status })
      if (res.data.code === 200) {
        ElMessage.success(`${actionText}成功！`)
        fetchMyGoods()
      }
    } catch (error) { ElMessage.error('操作失败') }
  }).catch(() => {})
}

// 🌟 删除商品
const handleDelete = (id) => {
  ElMessageBox.confirm('确定要永久删除这条商品记录吗？此操作不可恢复。', '危险警告', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'error'
  }).then(async () => {
    try {
      const res = await axios.delete(`/api/goods/delete/${id}`)
      if (res.data.code === 200) {
        ElMessage.success('删除成功')
        fetchMyGoods()
      } else {
        ElMessage.error(res.data.msg)
      }
    } catch (error) { ElMessage.error('删除失败') }
  }).catch(() => {})
}
</script>

<style scoped>
.my-goods-container { display: flex; justify-content: center; }
.my-goods-box { width: 1000px; background: #fff; padding: 30px 40px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); min-height: 500px; }
.page-title { margin-top: 0; margin-bottom: 20px; color: #333; border-bottom: 2px solid #ffe60f; padding-bottom: 8px; display: inline-block;}
</style>