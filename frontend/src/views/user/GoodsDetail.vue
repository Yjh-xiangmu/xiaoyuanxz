<template>
  <div class="detail-container">
    <div class="detail-box" v-loading="loading">
      <el-breadcrumb separator="/" style="margin-bottom: 20px;">
        <el-breadcrumb-item :to="{ path: '/user/market' }">闲置集市</el-breadcrumb-item>
        <el-breadcrumb-item>{{ goods.category }}</el-breadcrumb-item>
        <el-breadcrumb-item>商品详情</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="content-wrapper" v-if="goods.id">
        <div class="left-images">
          <el-carousel trigger="click" height="400px" arrow="always">
            <el-carousel-item v-for="(img, index) in imageList" :key="index">
              <el-image :src="img" fit="contain" style="width: 100%; height: 100%; background: #f5f5f5;" />
            </el-carousel-item>
          </el-carousel>
        </div>

        <div class="right-info">
          <h1 class="goods-title">{{ goods.title }}</h1>
          <div style="display: flex; align-items: center; margin: 15px 0; padding: 10px; background: #fafafa; border-radius: 8px; cursor: pointer; transition: background 0.3s;" @click="router.push(`/user/seller/${goods.sellerId}`)">
            <div style="width: 40px; height: 40px; background: #ffe60f; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 18px; margin-right: 15px;">卖</div>
            <div style="flex: 1;">
              <div style="font-size: 15px; font-weight: bold; color: #333;">点击查看卖家信用主页</div>
              <div style="font-size: 12px; color: #999;">深入了解卖家的历史评价与在售商品</div>
            </div>
            <div style="color: #ccc;">❯</div>
          </div>
          <div class="price-box">
            <div class="current-price"><span class="symbol">￥</span>{{ goods.price }}</div>
            <div class="original-price" v-if="goods.originalPrice">原价: ￥{{ goods.originalPrice }}</div>
          </div>

          <div class="info-item"><span class="label">成色分类：</span><el-tag type="info" effect="plain">{{ goods.category }}</el-tag></div>
          <div class="info-item"><span class="label">发布时间：</span><span>{{ goods.createTime }}</span></div>

          <div class="desc-box">
            <div class="label" style="margin-bottom: 10px;">商品详细描述：</div>
            <p class="desc-text">{{ goods.description }}</p>
          </div>

          <div class="action-box">
            <el-button
                type="primary" class="buy-btn" size="large"
                :disabled="goods.status !== 1"
                @click="openCheckoutDialog"
            >
              {{ goods.status === 1 ? '立即购买' : '该商品已售出或下架' }}
            </el-button>
            <el-button
                type="warning" class="chat-btn" size="large" color="#ffe60f" style="color: #333; font-weight: bold;"
                @click="openChatWindow"
            >
              联系卖家
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="checkoutVisible" title="确认订单信息" width="600px">
      <div v-loading="addressLoading">
        <h3 style="margin-top: 0;">请选择收货地址：</h3>
        <el-empty v-if="addressList.length === 0" description="您还没有收货地址哦">
          <el-button type="primary" @click="goToAddressManage">去添加地址</el-button>
        </el-empty>
        <el-radio-group v-model="selectedAddressId" style="width: 100%; display: flex; flex-direction: column; gap: 10px;" v-else>
          <el-radio v-for="addr in addressList" :key="addr.id" :value="addr.id" style="border: 1px solid #ebeef5; padding: 15px; border-radius: 8px; margin-right: 0; width: 100%; height: auto;">
            <div style="display: inline-block; white-space: normal; line-height: 1.5;">
              <span style="font-weight: bold; color: #333; margin-right: 10px;">{{ addr.receiver }}</span>
              <span style="color: #666; margin-right: 10px;">{{ addr.phone }}</span>
              <el-tag v-if="addr.isDefault === 1" size="small" type="danger" effect="plain">默认</el-tag>
              <div style="color: #999; margin-top: 5px;">{{ addr.detailAddress }}</div>
            </div>
          </el-radio>
        </el-radio-group>
        <div style="margin-top: 30px; border-top: 1px dashed #ccc; padding-top: 20px; text-align: right; font-size: 16px;">
          实付金额：<span style="color: #ff5000; font-size: 24px; font-weight: bold;">￥{{ goods.price }}</span>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="checkoutVisible = false">取 消</el-button>
          <el-button type="primary" color="#ff5000" style="font-weight: bold; color: white;" :disabled="!selectedAddressId" @click="confirmAndPay">确认下单并前往支付宝</el-button>
        </span>
      </template>
    </el-dialog>

    <el-drawer v-model="chatVisible" title="联系卖家" size="400px" @close="closeChat">
      <div class="chat-container">
        <div class="chat-history" id="chatHistoryBox">
          <div
              v-for="(msg, index) in chatMessages"
              :key="index"
              class="msg-row"
              :class="msg.senderId === userStore.userInfo.id ? 'msg-right' : 'msg-left'"
          >
            <div class="msg-bubble">
              {{ msg.content }}
            </div>
          </div>
        </div>
        <div class="chat-input-box">
          <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="3"
              placeholder="问问卖家商品的细节吧..."
              resize="none"
              @keyup.enter.native.prevent="sendMessage"
          />
          <el-button type="primary" color="#ffe60f" style="color: #333; font-weight: bold; width: 100%; margin-top: 10px;" @click="sendMessage">
            发送 (Enter)
          </el-button>
        </div>
      </div>
    </el-drawer>

  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const goods = ref({})
const imageList = ref([])
const loading = ref(false)

// 结算弹窗相关
const checkoutVisible = ref(false)
const addressList = ref([])
const addressLoading = ref(false)
const selectedAddressId = ref(null)

// 🌟 聊天相关变量
const chatVisible = ref(false)
const chatMessages = ref([])
const inputMessage = ref('')
let ws = null // WebSocket 实例

// 1. 获取商品详情
const fetchGoodsDetail = async () => {
  loading.value = true
  try {
    const res = await axios.get(`/api/goods/detail/${route.params.id}`)
    if (res.data.code === 200) {
      goods.value = res.data.data
      if (goods.value.imageUrl) imageList.value = goods.value.imageUrl.split(',')
    }
  } catch (error) {
    ElMessage.error('获取详情失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => fetchGoodsDetail())

// ---------- 下单购买相关逻辑 ----------
const openCheckoutDialog = async () => {
  checkoutVisible.value = true
  addressLoading.value = true
  try {
    const res = await axios.get('/api/address/list', { params: { userId: userStore.userInfo.id } })
    if (res.data.code === 200) {
      addressList.value = res.data.data
      if (addressList.value.length > 0) selectedAddressId.value = addressList.value[0].id
    }
  } catch (error) {
    ElMessage.error('获取地址失败')
  } finally {
    addressLoading.value = false
  }
}

const goToAddressManage = () => { checkoutVisible.value = false; router.push('/user/address') }

const confirmAndPay = async () => {
  const selectedAddr = addressList.value.find(a => a.id === selectedAddressId.value)
  if (!selectedAddr) return
  try {
    const res = await axios.post('/api/orders/create', {
      goodsId: goods.value.id, buyerId: userStore.userInfo.id,
      receiver: selectedAddr.receiver, phone: selectedAddr.phone, address: selectedAddr.detailAddress
    })
    if (res.data.code === 200) {
      ElMessage.success('订单生成成功，正在拉起支付宝...')
      window.location.href = `http://localhost:8080/api/alipay/pay?orderNo=${res.data.data.orderNo}`
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (error) { ElMessage.error('下单失败') }
}

// ---------- 🌟 实时聊天 WebSocket 核心逻辑 ----------

const openChatWindow = async () => {
  if (goods.value.sellerId === userStore.userInfo.id) {
    ElMessage.warning('这是您自己发布的商品哦，不能自己和自己聊天！')
    return
  }
  chatVisible.value = true
  await fetchChatHistory()
  initWebSocket()
}

const fetchChatHistory = async () => {
  try {
    const res = await axios.get('/api/chat/history', {
      params: { user1: userStore.userInfo.id, user2: goods.value.sellerId }
    })
    if (res.data.code === 200) {
      chatMessages.value = res.data.data
      scrollToBottom()
    }
  } catch (error) {
    console.error('获取历史记录失败')
  }
}

const initWebSocket = () => {
  if (ws) return // 防止重复连接
  // 核心：连上我们在后端写的 ws 接口
  ws = new WebSocket(`ws://localhost:8080/ws/${userStore.userInfo.id}`)

  ws.onopen = () => console.log('WebSocket 连接成功！')

  ws.onmessage = (event) => {
    const msg = JSON.parse(event.data)
    // 如果收到的消息是当前这个卖家的，才推入对话框
    if (msg.senderId === goods.value.sellerId) {
      chatMessages.value.push(msg)
      scrollToBottom()
    } else {
      // 别人发来的消息，可以在右上角弹个提示
      ElMessage.info('您收到了一条新的闲置私信！')
    }
  }

  ws.onerror = () => ElMessage.error('聊天连接断开，请刷新页面')
}

const sendMessage = () => {
  if (!inputMessage.value.trim() || !ws) return

  const msgObj = {
    receiverId: goods.value.sellerId,
    goodsId: goods.value.id,
    content: inputMessage.value
  }

  // 1. 通过 WebSocket 发给后端路由给卖家
  ws.send(JSON.stringify(msgObj))

  // 2. 将自己的消息立刻展示在右侧
  chatMessages.value.push({
    senderId: userStore.userInfo.id,
    content: inputMessage.value
  })

  inputMessage.value = ''
  scrollToBottom()
}

const closeChat = () => {
  // 离开抽屉不必须关闭 WS（为了能继续收通知），但这里为了简单可以不断开
}

const scrollToBottom = () => {
  nextTick(() => {
    const box = document.getElementById('chatHistoryBox')
    if (box) box.scrollTop = box.scrollHeight
  })
}
</script>

<style scoped>
/* 原有样式保留 */
.detail-container { display: flex; justify-content: center; }
.detail-box { width: 1000px; background: #fff; padding: 30px 40px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
.content-wrapper { display: flex; gap: 40px; margin-top: 20px; }
.left-images { width: 400px; border-radius: 8px; overflow: hidden; border: 1px solid #ebeef5; }
.right-info { flex: 1; display: flex; flex-direction: column; }
.goods-title { margin-top: 0; font-size: 22px; color: #333; line-height: 1.4; }
.price-box { background: #fff8e6; padding: 20px; border-radius: 8px; margin: 15px 0; display: flex; align-items: baseline; gap: 15px; }
.current-price { color: #ff5000; font-size: 28px; font-weight: bold; }
.symbol { font-size: 16px; margin-right: 2px; }
.original-price { color: #999; text-decoration: line-through; }
.info-item { margin-bottom: 15px; font-size: 14px; color: #666; }
.label { color: #999; display: inline-block; width: 80px; }
.desc-box { margin-top: 20px; border-top: 1px dashed #eee; padding-top: 20px; flex: 1; }
.desc-text { color: #333; line-height: 1.8; white-space: pre-wrap; font-size: 15px; }
.action-box { margin-top: 30px; display: flex; gap: 15px; }
.buy-btn { background-color: #ff5000; border-color: #ff5000; color: #fff; font-weight: bold; width: 160px; border-radius: 20px; }
.chat-btn { border-radius: 20px; width: 140px; }

/* 🌟 聊天框样式 (模仿微信/闲鱼) */
.chat-container { display: flex; flex-direction: column; height: calc(100vh - 80px); }
.chat-history { flex: 1; overflow-y: auto; padding: 20px 10px; background-color: #f5f5f5; border-radius: 8px; }
.msg-row { display: flex; margin-bottom: 20px; width: 100%; }
.msg-left { justify-content: flex-start; }
.msg-right { justify-content: flex-end; }
.msg-bubble {
  max-width: 75%;
  padding: 10px 15px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-all;
}
.msg-left .msg-bubble { background-color: #fff; color: #333; border: 1px solid #ebeef5; border-top-left-radius: 0; }
.msg-right .msg-bubble { background-color: #ffe60f; color: #333; border-top-right-radius: 0; }

.chat-input-box { margin-top: 15px; border-top: 1px solid #eee; padding-top: 15px; }
/* 深度修改 Element Plus textarea 隐藏右下角的拖拽块 */
:deep(.el-textarea__inner) { border-radius: 8px; }
</style>