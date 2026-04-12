<template>
  <div class="orders-container">
    <div class="orders-box">
      <h2 class="page-title">我的订单</h2>

      <el-table :data="orderList" border stripe style="width: 100%" v-loading="loading">

        <!-- 展开行：配送信息 -->
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

              <!-- 仲裁信息展示 -->
              <div v-if="props.row.isDispute === 1" style="margin-top: 12px; padding: 10px 14px; background: #fff3e0; border-left: 4px solid #e6a23c; border-radius: 4px;">
                <div style="font-weight: bold; color: #e6a23c; margin-bottom: 4px;">⚠️ 仲裁申请中</div>
                <div style="font-size: 13px; color: #666;">申请原因：{{ props.row.disputeReason }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="orderNo" label="订单号" width="190" />
        <el-table-column prop="amount" label="实付金额" width="100" align="center">
          <template #default="scope">
            <span style="color: #ff5000; font-weight: bold;">￥{{ scope.row.amount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="170" align="center" />

        <el-table-column label="状态" width="110" align="center">
          <template #default="scope">
            <div style="display: flex; flex-direction: column; align-items: center; gap: 4px;">
              <el-tag v-if="scope.row.status === 0" type="danger">待支付</el-tag>
              <el-tag v-else-if="scope.row.status === 1" type="warning">待发货</el-tag>
              <el-tag v-else-if="scope.row.status === 2" type="primary">已发货</el-tag>
              <el-tag v-else-if="scope.row.status === 3" type="success">已完成</el-tag>
              <el-tag v-else-if="scope.row.status === 4" type="info">已取消</el-tag>
              <!-- 仲裁中角标 -->
              <el-tag v-if="scope.row.isDispute === 1" type="warning" size="small" effect="dark">仲裁中</el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" align="center" fixed="right" min-width="200">
          <template #default="scope">
            <div style="display: flex; flex-wrap: wrap; gap: 6px; justify-content: center;">

              <!-- 待支付：继续支付 + 取消 -->
              <template v-if="scope.row.status === 0">
                <el-button size="small" type="primary" @click="continuePay(scope.row.orderNo)">支付</el-button>
                <el-button size="small" type="danger" plain @click="handleCancel(scope.row.id)">取消</el-button>
              </template>

              <!-- 改地址（待支付和待发货） -->
              <el-button
                  v-if="scope.row.status <= 1"
                  size="small" type="warning" plain
                  @click="openAddressDialog(scope.row)"
              >
                改地址
              </el-button>

              <!-- 确认收货（已发货） -->
              <el-button
                  v-if="scope.row.status === 2"
                  size="small" type="success"
                  @click="handleReceive(scope.row.id)"
              >
                确认收货
              </el-button>

              <!-- 申请仲裁（已支付或已发货，且未申请过） -->
              <el-button
                  v-if="(scope.row.status === 1 || scope.row.status === 2) && scope.row.isDispute !== 1"
                  size="small" type="danger" plain
                  @click="openDisputeDialog(scope.row)"
              >
                申请仲裁
              </el-button>

              <!-- 撤销仲裁（仲裁申请中） -->
              <el-button
                  v-if="scope.row.isDispute === 1"
                  size="small" type="info" plain
                  @click="handleCancelDispute(scope.row.id)"
              >
                撤销仲裁
              </el-button>

              <!-- 评价（已完成） -->
              <template v-if="scope.row.status === 3">
                <el-button
                    v-if="scope.row.isRated !== 1"
                    size="small" type="primary" plain
                    @click="openReviewDialog(scope.row, false)"
                >
                  评价
                </el-button>
                <el-button
                    v-else
                    size="small" type="info" plain
                    @click="openReviewDialog(scope.row, true)"
                >
                  追评
                </el-button>
              </template>

            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 改地址弹窗 -->
    <el-dialog v-model="addrDialogVisible" title="修改收货地址" width="500px">
      <div v-loading="addrLoading">
        <el-empty v-if="myAddressList.length === 0" description="暂无地址，请先去个人中心添加" />
        <el-radio-group v-else v-model="selectedAddrId" style="width: 100%; display: flex; flex-direction: column; gap: 10px;">
          <el-radio
              v-for="addr in myAddressList" :key="addr.id" :value="addr.id"
              style="border: 1px solid #ebeef5; padding: 15px; border-radius: 8px; width: 100%; height: auto;"
          >
            <div style="white-space: normal; line-height: 1.5;">
              <span style="font-weight: bold; margin-right: 10px; color: #333;">{{ addr.receiver }}</span>
              <span style="color: #666;">{{ addr.phone }}</span>
              <div style="color: #999; margin-top: 5px;">{{ addr.detailAddress }}</div>
            </div>
          </el-radio>
        </el-radio-group>
      </div>
      <template #footer>
        <el-button @click="addrDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitNewAddress">确认修改</el-button>
      </template>
    </el-dialog>

    <!-- 申请仲裁弹窗 -->
    <el-dialog v-model="disputeDialogVisible" title="⚖️ 申请平台仲裁" width="480px">
      <el-alert
          title="申请仲裁后，管理员将在24小时内介入处理。请如实描述问题，恶意仲裁将影响您的信誉分。"
          type="warning"
          show-icon
          :closable="false"
          style="margin-bottom: 20px;"
      />
      <div style="margin-bottom: 12px; font-size: 14px; color: #666;">
        订单号：<b>{{ currentOrder.orderNo }}</b>
      </div>
      <el-form label-position="top">
        <el-form-item label="仲裁原因（请详细描述遇到的问题）" required>
          <el-input
              v-model="disputeReason"
              type="textarea"
              :rows="5"
              placeholder="例如：卖家迟迟不发货 / 收到的商品与描述严重不符 / 卖家失联无法沟通..."
              maxlength="500"
              show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="disputeDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="submitDispute">提交仲裁申请</el-button>
      </template>
    </el-dialog>

    <!-- 评价弹窗 -->
    <el-dialog v-model="reviewVisible" :title="isAppendMode ? '追加评价' : '商品评价'" width="500px">
      <el-form label-position="top">
        <el-form-item label="综合星级打分" required v-if="!isAppendMode">
          <el-rate v-model="reviewForm.rating" :colors="['#99A9BF', '#F7BA2A', '#FF9900']" show-text />
        </el-form-item>
        <el-form-item :label="isAppendMode ? '追评内容' : '评价内容'" required>
          <el-input v-model="reviewForm.content" type="textarea" :rows="4" placeholder="说说这件闲置怎么样吧～" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" color="#ff5000" style="color: white; font-weight: bold;" @click="submitReview">
          提交评价
        </el-button>
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
const orderList = ref([])
const loading = ref(false)

// 改地址相关
const addrDialogVisible = ref(false)
const addrLoading = ref(false)
const myAddressList = ref([])
const selectedAddrId = ref(null)
const currentOrderId = ref(null)

// 仲裁相关
const disputeDialogVisible = ref(false)
const disputeReason = ref('')
const currentOrder = ref({})

// 评价相关
const reviewVisible = ref(false)
const isAppendMode = ref(false)
const reviewForm = reactive({ orderId: null, goodsId: null, sellerId: null, rating: 5, content: '' })

// 拉取订单列表
const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/orders/myList', { params: { userId: userStore.userInfo.id } })
    if (res.data.code === 200) orderList.value = res.data.data
  } catch (error) { ElMessage.error('获取订单失败') } finally { loading.value = false }
}

onMounted(() => fetchOrders())

// 继续支付
const continuePay = (orderNo) => {
  window.location.href = `http://localhost:8080/api/alipay/pay?orderNo=${orderNo}`
}

// 取消未支付订单
const handleCancel = (id) => {
  ElMessageBox.confirm('确定要取消这个未支付的订单吗？取消后商品将重新上架。', '取消订单确认', {
    confirmButtonText: '确定取消', cancelButtonText: '暂不取消', type: 'warning'
  }).then(async () => {
    try {
      const res = await axios.post(`/api/orders/cancel/${id}`)
      if (res.data.code === 200) { ElMessage.success(res.data.msg); fetchOrders() }
      else ElMessage.error(res.data.msg)
    } catch (error) { ElMessage.error('操作失败') }
  }).catch(() => {})
}

// 改地址
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
      fetchOrders()
    }
  } catch (error) { ElMessage.error('修改失败') }
}

// 确认收货
const handleReceive = (id) => {
  ElMessageBox.confirm('请确认您已收到商品并无质量问题。确认后交易将完成。', '确认收货', {
    confirmButtonText: '确认收货', cancelButtonText: '取消', type: 'success'
  }).then(async () => {
    try {
      const res = await axios.post(`/api/orders/receive/${id}`)
      if (res.data.code === 200) { ElMessage.success(res.data.msg); fetchOrders() }
    } catch (error) { ElMessage.error('操作失败') }
  }).catch(() => {})
}

// 打开申请仲裁弹窗
const openDisputeDialog = (row) => {
  currentOrder.value = row
  disputeReason.value = ''
  disputeDialogVisible.value = true
}

// 提交仲裁申请
const submitDispute = async () => {
  if (!disputeReason.value.trim()) {
    ElMessage.warning('请填写仲裁原因！')
    return
  }
  try {
    const res = await axios.post(`/api/orders/applyDispute/${currentOrder.value.id}`, {
      reason: disputeReason.value
    })
    if (res.data.code === 200) {
      ElMessage.success(res.data.msg)
      disputeDialogVisible.value = false
      fetchOrders()
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (error) { ElMessage.error('提交失败') }
}

// 撤销仲裁
const handleCancelDispute = (id) => {
  ElMessageBox.confirm('确定撤销仲裁申请吗？撤销后如需再次申请可重新提交。', '撤销仲裁', {
    confirmButtonText: '确定撤销', cancelButtonText: '取消', type: 'warning'
  }).then(async () => {
    try {
      const res = await axios.post(`/api/orders/cancelDispute/${id}`)
      if (res.data.code === 200) { ElMessage.success(res.data.msg); fetchOrders() }
      else ElMessage.error(res.data.msg)
    } catch (error) { ElMessage.error('操作失败') }
  }).catch(() => {})
}

// 评价
const openReviewDialog = (row, append) => {
  isAppendMode.value = append
  reviewForm.orderId = row.id
  reviewForm.goodsId = row.goodsId
  reviewForm.sellerId = row.sellerId
  reviewForm.rating = 5
  reviewForm.content = ''
  reviewVisible.value = true
}

const submitReview = async () => {
  if (!isAppendMode.value && !reviewForm.rating) { ElMessage.warning('请为该订单打星！'); return }
  if (!reviewForm.content.trim()) { ElMessage.warning('评价内容不能为空！'); return }
  try {
    const res = await axios.post('/api/review/add', {
      orderId: reviewForm.orderId, goodsId: reviewForm.goodsId,
      buyerId: userStore.userInfo.id, sellerId: reviewForm.sellerId,
      rating: isAppendMode.value ? 0 : reviewForm.rating,
      content: reviewForm.content, isAppend: isAppendMode.value ? 1 : 0
    })
    if (res.data.code === 200) {
      ElMessage.success(res.data.msg)
      reviewVisible.value = false
      fetchOrders()
    } else { ElMessage.error(res.data.msg) }
  } catch (error) { ElMessage.error('评价提交失败') }
}
</script>

<style scoped>
.orders-container { display: flex; justify-content: center; }
.orders-box { width: 1050px; background: #fff; padding: 30px 40px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); min-height: 500px; }
.page-title { margin-top: 0; margin-bottom: 20px; color: #333; border-bottom: 2px solid #409EFF; padding-bottom: 8px; display: inline-block; }
</style>