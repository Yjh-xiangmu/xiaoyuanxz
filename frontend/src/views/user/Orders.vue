<template>
  <div class="orders-container">
    <div class="orders-box">
      <h2 class="page-title">我的订单</h2>

      <el-table :data="orderList" border stripe style="width: 100%" v-loading="loading">
        <el-table-column type="expand">
          <template #default="props">
            <div style="padding: 15px 30px; background-color: #fcfcfc;">
              <p style="margin: 0 0 10px 0; font-weight: bold; color: #333;">配送信息：</p>
              <div v-if="props.row.receiver">
                <span style="margin-right: 20px;">收件人：{{ props.row.receiver }}</span>
                <span style="margin-right: 20px;">电话：{{ props.row.phone }}</span>
                <span>地址：{{ props.row.address }}</span>
              </div>
              <div v-else style="color: #f56c6c;">尚未填写收货地址！</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="orderNo" label="订单号" width="200" />
        <el-table-column prop="amount" label="实付金额" width="100" align="center">
          <template #default="scope"><span style="color: #ff5000; font-weight: bold;">￥{{ scope.row.amount }}</span></template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="170" align="center" />

        <el-table-column label="状态" width="120" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 0" type="danger">待支付</el-tag>
            <el-tag v-else-if="scope.row.status === 1" type="success">待发货</el-tag>
            <el-tag v-else-if="scope.row.status === 2" type="primary">已发货</el-tag>
            <el-tag v-else-if="scope.row.status === 3" type="info">已完成</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" align="center" fixed="right" width="220">
          <template #default="scope">
            <el-button v-if="scope.row.status === 0" size="small" type="primary" @click="continuePay(scope.row.orderNo)">
              支付
            </el-button>
            <el-button v-if="scope.row.status <= 1" size="small" type="warning" plain @click="openAddressDialog(scope.row)">
              修改地址
            </el-button>
            <el-button v-if="scope.row.status === 2" size="small" type="success" @click="handleReceive(scope.row.id)">
              确认收货
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="addrDialogVisible" title="修改收货地址" width="500px">
      <div v-loading="addrLoading">
        <el-radio-group v-model="selectedAddrId" style="width: 100%; display: flex; flex-direction: column; gap: 10px;">
          <el-radio v-for="addr in myAddressList" :key="addr.id" :value="addr.id" style="border: 1px solid #ebeef5; padding: 15px; border-radius: 8px; width: 100%;">
            <span style="font-weight: bold; margin-right: 10px;">{{ addr.receiver }}</span>
            <span style="color: #666;">{{ addr.phone }}</span>
            <div style="color: #999; margin-top: 5px; white-space: normal;">{{ addr.detailAddress }}</div>
          </el-radio>
        </el-radio-group>
      </div>
      <template #footer>
        <el-button @click="addrDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitNewAddress">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const userStore = useUserStore()
const orderList = ref([])
const loading = ref(false)

// 地址弹窗相关
const addrDialogVisible = ref(false)
const addrLoading = ref(false)
const myAddressList = ref([])
const selectedAddrId = ref(null)
const currentOrderId = ref(null)

const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/orders/myList', { params: { userId: userStore.userInfo.id } })
    if (res.data.code === 200) orderList.value = res.data.data
  } catch (error) { ElMessage.error('获取订单失败') } finally { loading.value = false }
}

onMounted(() => fetchOrders())

const continuePay = (orderNo) => { window.location.href = `http://localhost:8080/api/alipay/pay?orderNo=${orderNo}` }

// 打开修改地址弹窗
const openAddressDialog = async (row) => {
  currentOrderId.value = row.id
  addrDialogVisible.value = true
  addrLoading.value = true
  try {
    const res = await axios.get('/api/address/list', { params: { userId: userStore.userInfo.id } })
    if (res.data.code === 200) {
      myAddressList.value = res.data.data
      if (myAddressList.value.length > 0) selectedAddrId.value = myAddressList.value[0].id
    }
  } catch (error) { ElMessage.error('加载地址薄失败') } finally { addrLoading.value = false }
}

// 提交新地址到订单
const submitNewAddress = async () => {
  const addr = myAddressList.value.find(a => a.id === selectedAddrId.value)
  if (!addr) return

  try {
    const res = await axios.post('/api/orders/updateAddress', {
      id: currentOrderId.value,
      receiver: addr.receiver, phone: addr.phone, address: addr.detailAddress
    })
    if (res.data.code === 200) {
      ElMessage.success('订单地址已更新！')
      addrDialogVisible.value = false
      fetchOrders() // 刷新列表看最新地址
    }
  } catch (error) { ElMessage.error('修改失败') }
  // 🌟 买家确认收货
  const handleReceive = (id) => {
    ElMessageBox.confirm('请确认您已收到商品并且无质量问题。确认收货后，交易将完成。', '确认收货', {
      confirmButtonText: '确认收货',
      cancelButtonText: '取消',
      type: 'success'
    }).then(async () => {
      try {
        const res = await axios.post(`/api/orders/receive/${id}`)
        if (res.data.code === 200) {
          ElMessage.success(res.data.msg)
          fetchOrders() // 刷新列表，状态变成已完成
        }
      } catch (error) { ElMessage.error('操作失败') }
    }).catch(() => {})
  }
}
</script>

<style scoped>
.orders-container { display: flex; justify-content: center; }
.orders-box { width: 1000px; background: #fff; padding: 30px 40px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); min-height: 500px; }
.page-title { margin-top: 0; margin-bottom: 20px; color: #333; border-bottom: 2px solid #ffe60f; padding-bottom: 8px; display: inline-block; }
</style>