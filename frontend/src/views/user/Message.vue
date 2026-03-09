<template>
  <div class="message-container">
    <div class="message-box">
      <h2 class="page-title">消息中心</h2>

      <el-tabs v-model="activeTab" class="custom-tabs" @tab-click="handleTabChange">

        <el-tab-pane label="系统通知" name="system">
          <div v-loading="sysLoading" style="min-height: 300px;">
            <el-empty v-if="sysMessageList.length === 0" description="暂无任何系统消息" />
            <div v-else class="msg-list">
              <el-card
                  v-for="msg in sysMessageList" :key="msg.id"
                  class="msg-card" :class="{ 'is-unread': msg.isRead === 0 }"
                  @click="handleSysRead(msg)"
              >
                <div class="msg-header">
                  <div class="msg-title">
                    <span v-if="msg.isRead === 0" class="unread-dot"></span>
                    {{ msg.title }}
                  </div>
                  <div class="msg-time">{{ formatDate(msg.createTime) }}</div>
                </div>
                <div class="msg-content">{{ msg.content }}</div>
              </el-card>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="我的私信" name="chat">
          <div v-loading="chatLoading" style="min-height: 300px;">
            <el-empty v-if="chatSessions.length === 0" description="暂无私信记录，去集市找人聊聊吧！" />

            <div v-else class="session-list">
              <div
                  v-for="session in chatSessions" :key="session.partnerId"
                  class="session-item"
                  @click="openChatWindow(session.partnerId, session.partnerName)"
              >
                <div class="avatar-box">
                  {{ session.partnerName.charAt(0).toUpperCase() }}
                  <el-badge v-if="session.unreadCount > 0" :value="session.unreadCount" class="badge-item" />
                </div>

                <div class="session-info">
                  <div class="session-top">
                    <span class="partner-name">{{ session.partnerName }}</span>
                    <span class="time">{{ formatDate(session.updateTime) }}</span>
                  </div>
                  <div class="latest-msg">{{ session.latestMessage }}</div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

      </el-tabs>
    </div>

    <el-drawer v-model="chatVisible" :title="'与 ' + currentChatPartnerName + ' 的聊天'" size="400px" @close="closeChat">
      <div class="chat-container">
        <div class="chat-history" id="chatHistoryBox2">
          <div
              v-for="(msg, index) in chatMessages" :key="index"
              class="msg-row" :class="msg.senderId === userStore.userInfo.id ? 'msg-right' : 'msg-left'"
          >
            <div class="msg-bubble">{{ msg.content }}</div>
          </div>
        </div>
        <div class="chat-input-box">
          <el-input
              v-model="inputMessage" type="textarea" :rows="3"
              placeholder="发送消息..." resize="none"
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
import { useUserStore } from '@/stores/user'
import axios from 'axios'

const userStore = useUserStore()
const activeTab = ref('system')

// --- 系统消息相关 ---
const sysMessageList = ref([])
const sysLoading = ref(false)

const fetchSysMessages = async () => {
  sysLoading.value = true
  try {
    const res = await axios.get('/api/notification/list', { params: { userId: userStore.userInfo.id } })
    if (res.data.code === 200) sysMessageList.value = res.data.data
  } catch (error) { console.error(error) } finally { sysLoading.value = false }
}

const handleSysRead = async (msg) => {
  if (msg.isRead === 1) return
  try {
    const res = await axios.post(`/api/notification/read/${msg.id}`)
    if (res.data.code === 200) msg.isRead = 1
  } catch (error) { console.error(error) }
}

// --- 🌟 私信会话相关 ---
const chatSessions = ref([])
const chatLoading = ref(false)

const fetchChatSessions = async () => {
  chatLoading.value = true
  try {
    const res = await axios.get('/api/chat/sessions', { params: { userId: userStore.userInfo.id } })
    if (res.data.code === 200) chatSessions.value = res.data.data
  } catch (error) { console.error(error) } finally { chatLoading.value = false }
}

// 切换标签页时刷新对应的数据
const handleTabChange = (tab) => {
  if (tab.paneName === 'system') fetchSysMessages()
  else if (tab.paneName === 'chat') fetchChatSessions()
}

onMounted(() => {
  fetchSysMessages()
  // 提前建立全局 WebSocket 连接，这样在消息列表也能收到实时新消息
  initGlobalWebSocket()
})

// --- 🌟 聊天窗口逻辑 ---
const chatVisible = ref(false)
const chatMessages = ref([])
const inputMessage = ref('')
const currentChatPartnerId = ref(null)
const currentChatPartnerName = ref('')
let ws = null

// 初始化全局 WebSocket
const initGlobalWebSocket = () => {
  if (ws) return
  ws = new WebSocket(`ws://localhost:8080/ws/${userStore.userInfo.id}`)
  ws.onmessage = (event) => {
    const msg = JSON.parse(event.data)

    // 1. 如果当前正打开着和这个人的聊天窗口，直接把消息塞进屏幕
    if (chatVisible.value && msg.senderId === currentChatPartnerId.value) {
      chatMessages.value.push(msg)
      scrollToBottom()
      // 告诉后端我已经读了
      axios.post('/api/chat/readAll', null, { params: { senderId: msg.senderId, receiverId: userStore.userInfo.id } })
    }
    // 2. 如果没打开聊天窗口，或者在和别人聊，那就刷新外面的会话列表！
    else {
      if (activeTab.value === 'chat') fetchChatSessions()
    }
  }
}

// 点击会话列表，打开聊天窗口
const openChatWindow = async (partnerId, partnerName) => {
  currentChatPartnerId.value = partnerId
  currentChatPartnerName.value = partnerName
  chatVisible.value = true

  // 拉取历史记录
  try {
    const res = await axios.get('/api/chat/history', {
      params: { user1: userStore.userInfo.id, user2: partnerId }
    })
    if (res.data.code === 200) {
      chatMessages.value = res.data.data
      scrollToBottom()
    }
    // 告诉后端，这人的消息我全点开看了，清零未读数
    await axios.post('/api/chat/readAll', null, { params: { senderId: partnerId, receiverId: userStore.userInfo.id } })
    fetchChatSessions() // 刷新一下外面的小红点
  } catch (error) { console.error(error) }
}

const sendMessage = () => {
  if (!inputMessage.value.trim() || !ws) return
  const msgObj = { receiverId: currentChatPartnerId.value, content: inputMessage.value }
  ws.send(JSON.stringify(msgObj))

  chatMessages.value.push({ senderId: userStore.userInfo.id, content: inputMessage.value })
  inputMessage.value = ''
  scrollToBottom()
  fetchChatSessions() // 自己发了消息，更新一下外面列表的最新消息
}

const closeChat = () => {
  currentChatPartnerId.value = null
  fetchChatSessions() // 关掉抽屉时更新一下外面的列表
}

const scrollToBottom = () => {
  nextTick(() => {
    const box = document.getElementById('chatHistoryBox2')
    if (box) box.scrollTop = box.scrollHeight
  })
}

const formatDate = (timeStr) => {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  return `${d.getMonth()+1}-${d.getDate()} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}
</script>

<style scoped>
/* 系统消息样式保留... */
.message-container { display: flex; justify-content: center; }
.message-box { width: 800px; background: #fff; padding: 30px 40px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); min-height: 600px; }
.page-title { margin-top: 0; margin-bottom: 20px; color: #333; }
.msg-list { display: flex; flex-direction: column; gap: 15px; }
.msg-card { cursor: pointer; border: 1px solid #ebeef5; }
.msg-card.is-unread { background-color: #fdfdfd; border-color: #ffe60f; }
.msg-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.msg-title { font-size: 16px; font-weight: bold; color: #333; display: flex; align-items: center; gap: 8px; }
.unread-dot { width: 8px; height: 8px; background-color: #f56c6c; border-radius: 50%; display: inline-block; }
.msg-time { font-size: 13px; color: #999; }
.msg-content { font-size: 14px; color: #666; line-height: 1.6; background: #f9f9f9; padding: 10px; border-radius: 6px; }

/* 🌟 私信会话列表样式 */
.session-list { display: flex; flex-direction: column; }
.session-item { display: flex; align-items: center; padding: 15px 10px; border-bottom: 1px solid #f0f0f0; cursor: pointer; transition: background 0.3s; }
.session-item:hover { background-color: #f9f9f9; }
.avatar-box { width: 50px; height: 50px; background-color: #ffe60f; color: #333; font-size: 24px; font-weight: bold; border-radius: 50%; display: flex; justify-content: center; align-items: center; margin-right: 15px; position: relative; }
.badge-item { position: absolute; top: -5px; right: -5px; }
.session-info { flex: 1; overflow: hidden; }
.session-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.partner-name { font-size: 16px; font-weight: bold; color: #333; }
.time { font-size: 12px; color: #999; }
.latest-msg { font-size: 14px; color: #666; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* 🌟 聊天框样式复用 */
.chat-container { display: flex; flex-direction: column; height: calc(100vh - 80px); }
.chat-history { flex: 1; overflow-y: auto; padding: 20px 10px; background-color: #f5f5f5; border-radius: 8px; }
.msg-row { display: flex; margin-bottom: 20px; width: 100%; }
.msg-left { justify-content: flex-start; }
.msg-right { justify-content: flex-end; }
.msg-bubble { max-width: 75%; padding: 10px 15px; border-radius: 8px; font-size: 14px; line-height: 1.5; word-break: break-all; }
.msg-left .msg-bubble { background-color: #fff; color: #333; border: 1px solid #ebeef5; border-top-left-radius: 0; }
.msg-right .msg-bubble { background-color: #ffe60f; color: #333; border-top-right-radius: 0; }
.chat-input-box { margin-top: 15px; border-top: 1px solid #eee; padding-top: 15px; }
</style>