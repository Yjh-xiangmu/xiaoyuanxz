<template>
  <div class="admin-orders-container">
    <h2 style="margin-top: 0; margin-bottom: 20px;">全平台交易大厅与仲裁</h2>

    <div class="filter-box">
      <el-input v-model="searchKeyword" placeholder="输入订单号精准定位..." clearable style="width: 350px;" @keyup.enter="fetchAdminOrders" @clear="fetchAdminOrders">
        <template #append>
          <el-button @click="fetchAdminOrders">搜 索</el-button>
        </template>
      </el-input>
    </div>

    <el-table :data="orderList" border stripe style="width: 100%" v-loading="loading">
      <el-table-column type="expand">
        <template #default="props">
          <div style="padding: 15px 30px; background-color: #fafafa; border: 1px dashed #ccc;">
            <p style="margin: 0 0 10px 0; font-weight: bold; color: #333;">交易双方与物流档案：</p>
            <div style="margin-bottom: 5px;">
              <el-tag size="small" type="info">买家ID: {{ props.row.buyerId }}</el-tag>
              <el-tag size="small" type="warning" style="margin-left: 10px;">卖家ID: {{ props.row.sellerId }}</el-tag>
              <el-tag size="small" type="success" style="margin-left: 10px;">关联商品ID: {{ props.row.goodsId }}</el-tag>
            </div>
            <div style="margin-top: 10px; color: #666;" v-if="props.row.receiver">
              <b>收件信息：</b>{{ props.row.receiver }} / {{ props.row.phone }} / {{ props.row.address }}
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="orderNo" label="系统订单号" width="200" />
      <el-table-column prop="amount" label="交易金额" width="100" align="center">
        <template #default="scope"><span style="color: #ff5000; font-weight: bold;">￥{{ scope.row.amount }}</span></template>
      </el-table-column>
      <el-table-column prop="createTime" label="订单生成时间" width="170" align="center" />

      <el-table-column label="当前状态" width="120" align="center">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 0" type="info">待买家支付</el-tag>
          <el-tag v-else-if="scope.row.status === 1" type="warning">已付(待发货)</el-tag>
          <el-tag v-else-if="scope.row.status === 2" type="primary">已发货</el-tag>
          <el-tag v-else-if="scope.row.status === 3" type="success">交易完成</el-tag>
          <el-tag v-else-if="scope.row.status === 4" type="danger">已强制取消</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="平台介入操作" align="center" fixed="right" min-width="150">
        <template #default="scope">
          <el-button
              v-if="scope.row.status === 1 || scope.row.status === 2"
              size="small" type="danger" plain
              @click="handleForceRefund(scope.row.id)"
          >
            强制取消并退款
          </el-button>
          <span v-else style="color: #999; font-size: 13px;">无可用操作</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const orderList = ref([])
const loading = ref(false)
const searchKeyword = ref('')

// 获取全平台订单
const fetchAdminOrders = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/orders/adminList', { params: { keyword: searchKeyword.value } })
    if (res.data.code === 200) orderList.value = res.data.data
  } catch (error) { ElMessage.error('获取列表失败') } finally { loading.value = false }
}

onMounted(() => fetchAdminOrders())

// 🌟 管理员仲裁：强制退款
const handleForceRefund = (id) => {
  ElMessageBox.confirm(
      '您正在执行最高权限的仲裁操作：将该订单强制取消并原路退回买家款项。同时，涉事商品将被强制下架。是否继续？',
      '仲裁退款确认',
      {
        confirmButtonText: '确定强制退款',
        cancelButtonText: '取消',
        type: 'error'
      }
  ).then(async () => {
    try {
      const res = await axios.post(`/api/orders/forceRefund/${id}`)
      if (res.data.code === 200) {
        ElMessage.success(res.data.msg)
        fetchAdminOrders() // 刷新列表，状态变成已取消(4)
      } else {
        ElMessage.error(res.data.msg)
      }
    } catch (error) { ElMessage.error('操作失败') }
  }).catch(() => {})
}
</script>

<style scoped>
.admin-orders-container { padding: 10px; }
.filter-box { margin-bottom: 20px; background-color: #fff; padding: 15px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); display: flex; align-items: center; }
</style>