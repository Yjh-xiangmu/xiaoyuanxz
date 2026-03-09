<template>
  <div class="orders-container">
    <div class="orders-box">
      <h2 class="page-title">我卖出的订单</h2>

      <el-table :data="sellList" border stripe style="width: 100%" v-loading="loading">
        <el-table-column type="expand">
          <template #default="props">
            <div style="padding: 15px 30px; background-color: #fff9e6; border: 1px dashed #ffe60f;">
              <p style="margin: 0 0 10px 0; font-weight: bold; color: #ff5000;">买家收货信息 (请按此地址发货)：</p>
              <div v-if="props.row.receiver">
                <span style="margin-right: 20px;">收件人：<b>{{ props.row.receiver }}</b></span>
                <span style="margin-right: 20px;">电话：<b>{{ props.row.phone }}</b></span>
                <span>详细地址：<b>{{ props.row.address }}</b></span>
              </div>
              <div v-else style="color: #f56c6c;">买家尚未填写地址！</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="orderNo" label="订单号" width="200" />
        <el-table-column prop="amount" label="实收金额" width="100" align="center">
          <template #default="scope"><span style="color: #ff5000; font-weight: bold;">￥{{ scope.row.amount }}</span></template>
        </el-table-column>
        <el-table-column prop="payTime" label="买家付款时间" width="170" align="center" />

        <el-table-column label="当前状态" width="120" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 0" type="danger">待买家支付</el-tag>
            <el-tag v-else-if="scope.row.status === 1" type="warning">待我发货</el-tag>
            <el-tag v-else-if="scope.row.status === 2" type="primary">我已发货</el-tag>
            <el-tag v-else-if="scope.row.status === 3" type="success">交易完成</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" align="center" fixed="right" width="150">
          <template #default="scope">
            <el-button v-if="scope.row.status === 1" size="small" type="primary" color="#ff5000" style="color: white; font-weight: bold;" @click="handleShip(scope.row.id)">
              去发货
            </el-button>
            <span v-else-if="scope.row.status > 1" style="color: #999; font-size: 13px;">等待买家收货</span>
            <span v-else style="color: #999; font-size: 13px;">等待付款</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const userStore = useUserStore()
const sellList = ref([])
const loading = ref(false)

const fetchSellList = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/orders/sellList', { params: { sellerId: userStore.userInfo.id } })
    if (res.data.code === 200) sellList.value = res.data.data
  } catch (error) { ElMessage.error('获取列表失败') } finally { loading.value = false }
}

onMounted(() => fetchSellList())

// 卖家点击发货
const handleShip = (id) => {
  ElMessageBox.confirm('确认已经将商品寄出并且填写了正确的买家地址吗？', '发货确认', {
    confirmButtonText: '确认已发货',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await axios.post(`/api/orders/ship/${id}`)
      if (res.data.code === 200) {
        ElMessage.success(res.data.msg)
        fetchSellList() // 刷新列表，状态变成已发货
      }
    } catch (error) { ElMessage.error('操作失败') }
  }).catch(() => {})
}
</script>

<style scoped>
.orders-container { display: flex; justify-content: center; }
.orders-box { width: 1000px; background: #fff; padding: 30px 40px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); min-height: 500px; }
.page-title { margin-top: 0; margin-bottom: 20px; color: #333; border-bottom: 2px solid #ffe60f; padding-bottom: 8px; display: inline-block; }
</style>