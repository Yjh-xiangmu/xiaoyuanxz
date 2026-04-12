<template>
  <div class="message-container">
    <div class="message-box">
      <h2 class="page-title">消息中心</h2>

      <el-tabs v-model="activeTab" class="custom-tabs" @tab-click="handleTabChange">

        <!-- ===================== 系统通知 ===================== -->
        <el-tab-pane name="system">
          <template #label>
            <el-badge :value="sysUnreadCount" :max="99" :hidden="sysUnreadCount === 0">
              <span style="padding-right: 6px;">系统通知</span>
            </el-badge>
          </template>

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

        <!-- ===================== 我的私信 ===================== -->
        <el-tab-pane name="chat">
          <template #label>
            <el-badge :value="chatTotalUnread" :max="99" :hidden="chatTotalUnread === 0">
              <span style="padding-right: 6px;">我的私信</span>
            </el-badge>
          </template>

          <div v-loading="chatLoading" style="min-height: 300px;">
            <el-empty v-if="chatSessions.length === 0" description="暂无私信记录，去集市找人聊聊吧！" />

            <div v-else class="session-list">
              <div
                  v-for="session in chatSessions" :key="session.partnerId"
                  class="session-item"
                  :class="{ 'session-unread': session.unreadCount > 0 }"
                  @click="openChatWindow(session)"
              >
                <!-- 头像 + 未读角标 -->
                <div class="avatar-wrap">
                  <div class="avatar-box">
                    <img v-if="session.partnerAvatar" :src="session.partnerAvatar" style="width:100%;height:100%;object-fit:cover;border-radius:50%;" />
                    <span v-else>{{ session.partnerName.charAt(0).toUpperCase() }}</span>
                  </div>
                  <!-- 微信风格角标：超过99显示99+ -->
                  <div v-if="session.unreadCount > 0" class="unread-badge">
                    {{ session.unreadCount > 99 ? '99+' : session.unreadCount }}
                  </div>
                </div>

                <!-- 会话信息 -->
                <div class="session-info">
                  <div class="session-top">
                    <span class="partner-name">{{ session.partnerName }}</span>
                    <span class="time">{{ formatDate(session.updateTime) }}</span>
                  </div>
                  <div class="session-bottom">
                    <!-- 最新一条消息预览，未读加粗 -->
                    <span class="latest-msg" :class="{ 'msg-bold': session.unreadCount > 0 }">
                      {{ session.latestMessage }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

      </el-tabs>
    </div>

    <!-- ===================== 聊天抽屉 ===================== -->
    <el-drawer
        v-model="chatVisible"
        :title="'与 ' + currentChatPartnerName + ' 的聊天'"
        size="420px"
        @close="closeChat"
    >
      <div class="chat-container">
        <!-- 聊天记录区 -->
        <div class="chat-history" id="chatHistoryBox2">
          <div
              v-for="(msg, index) in chatMessages"
              :key="index"
              class="msg-row"
              :class="msg.senderId === userStore.userInfo.id ? 'msg-right' : 'msg-left'"
          >
            <!-- 对方消息：左侧显示头像 -->
            <div v-if="msg.senderId !== userStore.userInfo.id" class="msg-avatar msg-avatar-left">
              {{ currentChatPartnerName.charAt(0).toUpperCase() }}
            </div>

            <div class="bubble-wrap">
              <!-- 时间戳（每5条或间隔超过5分钟才显示一次） -->
              <div v-if="shouldShowTime(index)" class="msg-timestamp">
                {{ formatDate(msg.createTime) }}
              </div>

              <div class="bubble-row" :class="msg.senderId === userStore.userInfo.id ? 'bubble-row-right' : 'bubble-row-left'">
                <!-- 已读/未读状态（仅自己发的消息右侧显示） -->
                <div v-if="msg.senderId === userStore.userInfo.id" class="read-status">
                  <span v-if="msg.isRead === 1" class="status-read">已读</span>
                  <span v-else class="status-unread">未读</span>
                </div>

                <div class="msg-bubble">{{ msg.content }}</div>
              </div>
            </div>

            <!-- 自己消息：右侧显示头像 -->
            <div v-if="msg.senderId === userStore.userInfo.id" class="msg-avatar msg-avatar-right">
              {{ (userStore.userInfo.realName || userStore.userInfo.username).charAt(0).toUpperCase() }}
            </div>
          </div>
        </div>

        <!-- 输入框 -->
        <div class="chat-input-box">
          <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="3"
              placeholder="发送消息..."
              resize="none"
              @keydown.enter.prevent="handleEnterSend"
          />
          <div class="input-actions">
            <span class="enter-tip">按 Enter 发送，Shift+Enter 换行</span>
            <el-button
                type="primary"
                color="#ffe60f"
                style="color: #333; font-weight: bold;"
                @click="sendMessage"
            >
              发 送
            </el-button>
          </div>
        </div>
      </div>
    </el-drawer>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useUserStore } from '@/stores/user'
import axios from 'axios'

const userStore = useUserStore()
const activeTab = ref('system')

// ==================== 系统通知 ====================
const sysMessageList = ref([])
const sysLoading = ref(false)

const sysUnreadCount = computed(() =>
    sysMessageList.value.filter(m => m.isRead === 0).length
)

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

// ==================== 私信会话列表 ====================
const chatSessions = ref([])
const chatLoading = ref(false)

// 所有私信会话的总未读数
const chatTotalUnread = computed(() =>
    chatSessions.value.reduce((sum, s) => sum + (s.unreadCount || 0), 0)
)

const fetchChatSessions = async () => {
  chatLoading.value = true
  try {
    const res = await axios.get('/api/chat/sessions', { params: { userId: userStore.userInfo.id } })
    if (res.data.code === 200) chatSessions.value = res.data.data
  } catch (error) { console.error(error) } finally { chatLoading.value = false }
}

// ==================== 切换 Tab ====================
const handleTabChange = (tab) => {
  if (tab.paneName === 'system') fetchSysMessages()
  else if (tab.paneName === 'chat') fetchChatSessions()
}

// ==================== 聊天窗口 ====================
const chatVisible = ref(false)
const chatMessages = ref([])
const inputMessage = ref('')
const currentChatPartnerId = ref(null)
const currentChatPartnerName = ref('')
let ws = null
let readStatusTimer = null // 定时刷新已读状态

// 判断是否显示时间戳（首条消息 or 距上一条超过5分钟）
const shouldShowTime = (index) => {
  if (index === 0) return true
  const cur = chatMessages.value[index]
  const prev = chatMessages.value[index - 1]
  if (!cur.createTime || !prev.createTime) return false
  const diff = new Date(cur.createTime) - new Date(prev.createTime)
  return diff > 5 * 60 * 1000 // 5分钟
}

// 全局 WebSocket 连接
const initGlobalWebSocket = () => {
  if (ws && ws.readyState === WebSocket.OPEN) return
  ws = new WebSocket(`ws://localhost:8080/ws/${userStore.userInfo.id}`)

  ws.onmessage = (event) => {
    const msg = JSON.parse(event.data)

    // 如果当前聊天窗口正对着这个发送者
    if (chatVisible.value && msg.senderId === currentChatPartnerId.value) {
      // 直接标为已读（因为窗口是打开的）
      msg.isRead = 1
      chatMessages.value.push(msg)
      scrollToBottom()
      // 通知后端标记已读
      axios.post('/api/chat/readAll', null, {
        params: { senderId: msg.senderId, receiverId: userStore.userInfo.id }
      })
      // 刷新会话列表，清零该会话角标
      fetchChatSessions()
    } else {
      // 窗口没开着：刷新会话列表，角标 +1
      fetchChatSessions()
    }
  }

  ws.onclose = () => {
    // 断线后3秒重连
    setTimeout(initGlobalWebSocket, 3000)
  }
}

// 打开聊天窗口
const openChatWindow = async (session) => {
  currentChatPartnerId.value = session.partnerId
  currentChatPartnerName.value = session.partnerName
  chatVisible.value = true

  // 拉历史消息
  try {
    const res = await axios.get('/api/chat/history', {
      params: { user1: userStore.userInfo.id, user2: session.partnerId }
    })
    if (res.data.code === 200) {
      chatMessages.value = res.data.data
      scrollToBottom()
    }
    // 标记该会话全部已读
    await axios.post('/api/chat/readAll', null, {
      params: { senderId: session.partnerId, receiverId: userStore.userInfo.id }
    })
    // 刷新会话列表角标清零
    fetchChatSessions()
  } catch (error) { console.error(error) }

  // 启动定时器：每3秒刷新一次消息的已读状态（对方读了我发的消息）
  startReadStatusPolling()
}

// 定时刷新已读状态：让"未读→已读"实时变化
const startReadStatusPolling = () => {
  stopReadStatusPolling()
  readStatusTimer = setInterval(async () => {
    if (!chatVisible.value || !currentChatPartnerId.value) return
    try {
      const res = await axios.get('/api/chat/history', {
        params: { user1: userStore.userInfo.id, user2: currentChatPartnerId.value }
      })
      if (res.data.code === 200) {
        // 只更新 isRead 字段，不重新渲染整个列表（避免闪烁）
        const newMsgs = res.data.data
        chatMessages.value.forEach((msg, i) => {
          if (newMsgs[i] && msg.isRead !== newMsgs[i].isRead) {
            msg.isRead = newMsgs[i].isRead
          }
        })
      }
    } catch (e) {}
  }, 3000)
}

const stopReadStatusPolling = () => {
  if (readStatusTimer) {
    clearInterval(readStatusTimer)
    readStatusTimer = null
  }
}

// 关闭聊天窗口
const closeChat = () => {
  currentChatPartnerId.value = null
  stopReadStatusPolling()
  fetchChatSessions()
}

// 发送消息
const sendMessage = () => {
  if (!inputMessage.value.trim()) return
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    initGlobalWebSocket()
    return
  }

  const content = inputMessage.value.trim()
  const msgObj = { receiverId: currentChatPartnerId.value, content }
  ws.send(JSON.stringify(msgObj))

  // 本地先渲染，isRead=0（未读，等对方打开后变成已读）
  chatMessages.value.push({
    senderId: userStore.userInfo.id,
    receiverId: currentChatPartnerId.value,
    content,
    isRead: 0,
    createTime: new Date().toISOString()
  })
  inputMessage.value = ''
  scrollToBottom()
  fetchChatSessions()
}

// Enter发送，Shift+Enter换行
const handleEnterSend = (e) => {
  if (e.shiftKey) return // Shift+Enter 换行，不拦截
  sendMessage()
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
  const now = new Date()
  const isToday = d.toDateString() === now.toDateString()
  if (isToday) {
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  }
  return `${d.getMonth() + 1}-${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

// ==================== 生命周期 ====================
onMounted(() => {
  fetchSysMessages()
  initGlobalWebSocket()
})

onUnmounted(() => {
  stopReadStatusPolling()
  if (ws) {
    ws.onclose = null // 防止触发重连
    ws.close()
    ws = null
  }
})
</script>

<style scoped>
.message-container { display: flex; justify-content: center; }
.message-box { width: 800px; background: #fff; padding: 30px 40px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); min-height: 600px; }
.page-title { margin-top: 0; margin-bottom: 20px; color: #333; }

/* 系统通知 */
.msg-list { display: flex; flex-direction: column; gap: 15px; }
.msg-card { cursor: pointer; border: 1px solid #ebeef5; transition: border-color 0.2s; }
.msg-card.is-unread { background-color: #fdfdfd; border-color: #ffe60f; }
.msg-card:hover { border-color: #409EFF; }
.msg-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.msg-title { font-size: 16px; font-weight: bold; color: #333; display: flex; align-items: center; gap: 8px; }
.unread-dot { width: 8px; height: 8px; background-color: #f56c6c; border-radius: 50%; display: inline-block; flex-shrink: 0; }
.msg-time { font-size: 13px; color: #999; white-space: nowrap; }
.msg-content { font-size: 14px; color: #666; line-height: 1.6; background: #f9f9f9; padding: 10px; border-radius: 6px; }

/* 私信会话列表 */
.session-list { display: flex; flex-direction: column; }
.session-item {
  display: flex;
  align-items: center;
  padding: 14px 10px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
  border-radius: 8px;
}
.session-item:hover { background-color: #f5f7fa; }
.session-unread { background-color: #fffbf0; }
.session-unread:hover { background-color: #fff5d6; }

/* 头像 + 角标 */
.avatar-wrap { position: relative; margin-right: 15px; flex-shrink: 0; }
.avatar-box {
  width: 50px; height: 50px;
  background-color: #ffe60f;
  color: #333;
  font-size: 22px; font-weight: bold;
  border-radius: 50%;
  display: flex; justify-content: center; align-items: center;
  overflow: hidden;
}
.unread-badge {
  position: absolute;
  top: -5px; right: -5px;
  min-width: 18px; height: 18px;
  background-color: #f56c6c;
  color: #fff;
  font-size: 11px; font-weight: bold;
  border-radius: 9px;
  display: flex; justify-content: center; align-items: center;
  padding: 0 4px;
  border: 2px solid #fff;
  line-height: 1;
}

.session-info { flex: 1; overflow: hidden; }
.session-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.partner-name { font-size: 15px; font-weight: bold; color: #333; }
.time { font-size: 12px; color: #bbb; white-space: nowrap; }
.latest-msg { font-size: 13px; color: #999; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; display: block; }
.msg-bold { color: #333; font-weight: 600; }

/* 聊天窗口 */
.chat-container { display: flex; flex-direction: column; height: calc(100vh - 80px); }
.chat-history { flex: 1; overflow-y: auto; padding: 16px 12px; background-color: #f0f2f5; border-radius: 8px; }

/* 消息行 */
.msg-row { display: flex; align-items: flex-end; margin-bottom: 18px; gap: 8px; }
.msg-left { justify-content: flex-start; }
.msg-right { justify-content: flex-end; }

/* 头像 */
.msg-avatar {
  width: 36px; height: 36px;
  border-radius: 50%;
  background: #ffe60f;
  color: #333;
  font-size: 16px; font-weight: bold;
  display: flex; justify-content: center; align-items: center;
  flex-shrink: 0;
}
.msg-avatar-right { background: #409EFF; color: #fff; }

/* 气泡区域 */
.bubble-wrap { display: flex; flex-direction: column; max-width: 68%; }
.bubble-row { display: flex; align-items: flex-end; gap: 6px; }
.bubble-row-left { flex-direction: row; }
.bubble-row-right { flex-direction: row-reverse; }

.msg-bubble {
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px; line-height: 1.6;
  word-break: break-word;
}
.msg-left .msg-bubble {
  background-color: #fff;
  color: #333;
  border-top-left-radius: 2px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.msg-right .msg-bubble {
  background-color: #ffe60f;
  color: #333;
  border-top-right-radius: 2px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
}

/* 时间戳 */
.msg-timestamp {
  text-align: center;
  font-size: 11px;
  color: #bbb;
  margin-bottom: 10px;
  margin-top: 4px;
}

/* 已读/未读状态 */
.read-status { font-size: 11px; align-self: flex-end; white-space: nowrap; }
.status-read { color: #67c23a; }
.status-unread { color: #bbb; }

/* 输入框 */
.chat-input-box { margin-top: 12px; border-top: 1px solid #eee; padding-top: 12px; }
.input-actions { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; }
.enter-tip { font-size: 12px; color: #bbb; }
:deep(.el-textarea__inner) { border-radius: 8px; }
</style>