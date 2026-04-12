<template>
  <div class="dashboard-container">
    <h2 class="page-title">平台数据总览与控制台</h2>

    <!-- 顶部四大核心指标 -->
    <el-row :gutter="20" class="data-cards">
      <el-col :span="6">
        <el-card shadow="hover" class="data-card bg-blue">
          <div class="card-icon">👥</div>
          <div class="card-info">
            <div class="card-title">注册用户总数</div>
            <div class="card-value">{{ dashData.totalUsers }} <span class="unit">人</span></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="data-card bg-green">
          <div class="card-icon">📦</div>
          <div class="card-info">
            <div class="card-title">在售商品总数</div>
            <div class="card-value">{{ dashData.activeGoods }} <span class="unit">件</span></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="data-card bg-orange">
          <div class="card-icon">🤝</div>
          <div class="card-info">
            <div class="card-title">累计成交订单</div>
            <div class="card-value">{{ dashData.completedOrders }} <span class="unit">笔</span></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="data-card bg-red">
          <div class="card-icon">💰</div>
          <div class="card-info">
            <div class="card-title">平台总交易额</div>
            <div class="card-value"><span class="unit">￥</span>{{ dashData.totalAmount }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 待审核 + 仲裁 两张告警卡 -->
    <el-row :gutter="20" style="margin-top: 20px;">

      <!-- 待审核商品 -->
      <el-col :span="12">
        <el-card
            shadow="hover"
            class="todo-alert-card"
            :class="dashData.pendingAuditCount > 0 ? 'card-warn' : 'card-ok'"
        >
          <template #header>
            <div class="alert-header">
              <div class="alert-header-left">
                <span class="alert-icon">🔍</span>
                <span class="alert-title">待审核商品</span>
                <el-badge
                    v-if="dashData.pendingAuditCount > 0"
                    :value="dashData.pendingAuditCount"
                    :max="99"
                    style="margin-left: 8px;"
                />
              </div>
              <el-button type="warning" size="small" plain @click="router.push('/admin/audit')">
                去处理 →
              </el-button>
            </div>
          </template>

          <el-empty
              v-if="!dashData.recentPendingGoods || dashData.recentPendingGoods.length === 0"
              :image-size="60"
              description="✅ 当前没有待审核商品，处理完毕！"
          />
          <div v-else class="todo-list">
            <div
                v-for="goods in dashData.recentPendingGoods"
                :key="goods.id"
                class="todo-item"
                @click="router.push('/admin/audit')"
            >
              <el-image
                  v-if="goods.imageUrl"
                  :src="goods.imageUrl.split(',')[0]"
                  fit="cover"
                  class="todo-img"
              />
              <div v-else class="todo-img-placeholder">📦</div>
              <div class="todo-info">
                <div class="todo-name">{{ goods.title }}</div>
                <div class="todo-meta">
                  <el-tag size="small" type="info">{{ goods.category }}</el-tag>
                  <span class="todo-price">￥{{ goods.price }}</span>
                </div>
              </div>
              <div class="todo-time">{{ formatDate(goods.createTime) }}</div>
              <el-tag size="small" type="warning" effect="dark">待审核</el-tag>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 仲裁订单（真正买家申请了仲裁的） -->
      <el-col :span="12">
        <el-card
            shadow="hover"
            class="todo-alert-card"
            :class="dashData.disputeOrderCount > 0 ? 'card-dispute' : 'card-ok'"
        >
          <template #header>
            <div class="alert-header">
              <div class="alert-header-left">
                <span class="alert-icon">⚖️</span>
                <span class="alert-title">买家申请仲裁</span>
                <el-badge
                    v-if="dashData.disputeOrderCount > 0"
                    :value="dashData.disputeOrderCount"
                    :max="99"
                    style="margin-left: 8px;"
                />
              </div>
              <el-button type="danger" size="small" plain @click="router.push('/admin/orders')">
                去仲裁 →
              </el-button>
            </div>
          </template>

          <el-empty
              v-if="!dashData.recentDisputeOrders || dashData.recentDisputeOrders.length === 0"
              :image-size="60"
              description="✅ 当前无仲裁申请，交易正常！"
          />
          <div v-else class="todo-list">
            <div
                v-for="order in dashData.recentDisputeOrders"
                :key="order.id"
                class="todo-item"
                @click="router.push('/admin/orders')"
            >
              <div class="todo-order-icon">⚖️</div>
              <div class="todo-info">
                <div class="todo-name">{{ order.orderNo }}</div>
                <div class="todo-meta">
                  <span class="todo-price">￥{{ order.amount }}</span>
                  <span class="todo-reason">{{ order.disputeReason }}</span>
                </div>
              </div>
              <div class="todo-time">{{ formatDate(order.createTime) }}</div>
              <el-tag size="small" type="danger" effect="dark">仲裁中</el-tag>
            </div>
          </div>
        </el-card>
      </el-col>

    </el-row>

    <!-- 饼图 + 快捷通道 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="14">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">📊 平台在售商品分类占比</div>
          </template>
          <div id="adminPieChart" style="width: 100%; height: 350px;"></div>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card shadow="hover" class="todo-card">
          <template #header>
            <div class="card-header">⚡ 核心业务快捷通道</div>
          </template>
          <div class="shortcut-list">

            <div class="shortcut-item" @click="router.push('/admin/audit')">
              <div class="shortcut-left">
                <div class="shortcut-icon" style="background: #e8f4ff; color: #1890ff;">🔍</div>
                <div class="shortcut-text">
                  <div class="shortcut-title">商品审核中心</div>
                  <div class="shortcut-desc">审核新发布的闲置，下架违规物品</div>
                </div>
              </div>
              <el-badge v-if="dashData.pendingAuditCount > 0" :value="dashData.pendingAuditCount" :max="99" />
              <el-icon v-else style="color: #ccc;"><ArrowRight /></el-icon>
            </div>

            <div class="shortcut-item" @click="router.push('/admin/users')">
              <div class="shortcut-left">
                <div class="shortcut-icon" style="background: #fff3e0; color: #ff9800;">🚫</div>
                <div class="shortcut-text">
                  <div class="shortcut-title">用户与违规管理</div>
                  <div class="shortcut-desc">处理违规账号，手动调整信誉分</div>
                </div>
              </div>
              <el-icon style="color: #ccc;"><ArrowRight /></el-icon>
            </div>

            <div class="shortcut-item" @click="router.push('/admin/orders')">
              <div class="shortcut-left">
                <div class="shortcut-icon" style="background: #fce4ec; color: #e91e63;">⚖️</div>
                <div class="shortcut-text">
                  <div class="shortcut-title">交易与仲裁大厅</div>
                  <div class="shortcut-desc">全局订单监控，执行强制退款操作</div>
                </div>
              </div>
              <el-badge v-if="dashData.disputeOrderCount > 0" :value="dashData.disputeOrderCount" :max="99" />
              <el-icon v-else style="color: #ccc;"><ArrowRight /></el-icon>
            </div>

            <div class="shortcut-item" @click="router.push('/admin/reviews')">
              <div class="shortcut-left">
                <div class="shortcut-icon" style="background: #e8f5e9; color: #4caf50;">💬</div>
                <div class="shortcut-text">
                  <div class="shortcut-title">评价与合规管理</div>
                  <div class="shortcut-desc">屏蔽恶意差评与违规发言</div>
                </div>
              </div>
              <el-icon style="color: #ccc;"><ArrowRight /></el-icon>
            </div>

          </div>
        </el-card>
      </el-col>
    </el-row>

  </div>
</template>

<script setup>
import { reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import axios from 'axios'
import * as echarts from 'echarts'

const router = useRouter()

const dashData = reactive({
  totalUsers: 0, activeGoods: 0, completedOrders: 0, totalAmount: 0,
  categoryPieData: [],
  pendingAuditCount: 0, recentPendingGoods: [],
  disputeOrderCount: 0, recentDisputeOrders: []
})

const fetchDashboardData = async () => {
  try {
    const res = await axios.get('/api/data/dashboard')
    if (res.data.code === 200) {
      Object.assign(dashData, res.data.data)
      nextTick(() => initPieChart())
    }
  } catch (error) { ElMessage.error('获取大盘数据失败') }
}

const initPieChart = () => {
  const chartDom = document.getElementById('adminPieChart')
  if (!chartDom) return
  const myChart = echarts.getInstanceByDom(chartDom) || echarts.init(chartDom)
  myChart.setOption({
    tooltip: { trigger: 'item', formatter: '{a} <br/>{b} : {c}件 ({d}%)' },
    legend: { top: 'bottom' },
    color: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de'],
    series: [{
      name: '商品分类', type: 'pie',
      radius: [30, 120], center: ['50%', '45%'],
      roseType: 'area', itemStyle: { borderRadius: 8 },
      data: dashData.categoryPieData
    }]
  })
  window.addEventListener('resize', () => myChart.resize())
}

const formatDate = (timeStr) => {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  return `${d.getMonth() + 1}-${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

onMounted(() => fetchDashboardData())
</script>

<style scoped>
.dashboard-container { padding: 10px; }
.page-title { margin-top: 0; margin-bottom: 25px; color: #333; }

/* 四大核心指标 */
.data-cards { margin-bottom: 0; }
.data-card { border: none; border-radius: 12px; transition: transform 0.3s; color: #fff; cursor: pointer; }
.data-card:hover { transform: translateY(-5px); box-shadow: 0 8px 16px rgba(0,0,0,0.1); }
:deep(.data-card .el-card__body) { display: flex; align-items: center; padding: 25px 20px; }
.card-icon { font-size: 40px; margin-right: 15px; opacity: 0.9; }
.card-info { flex: 1; }
.card-title { font-size: 15px; opacity: 0.9; margin-bottom: 8px; }
.card-value { font-size: 28px; font-weight: bold; }
.unit { font-size: 14px; font-weight: normal; }
.bg-blue { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }
.bg-green { background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); }
.bg-orange { background: linear-gradient(135deg, #fa709a 0%, #fee140 100%); }
.bg-red { background: linear-gradient(135deg, #ff0844 0%, #ffb199 100%); }

/* 告警卡片 */
.todo-alert-card { border-radius: 12px; }
.card-warn  { border: 2px solid #e6a23c !important; }
.card-dispute { border: 2px solid #f56c6c !important; }
.card-ok { border: 2px solid #67c23a !important; }

.alert-header { display: flex; justify-content: space-between; align-items: center; }
.alert-header-left { display: flex; align-items: center; gap: 6px; }
.alert-icon { font-size: 18px; }
.alert-title { font-size: 16px; font-weight: bold; color: #333; }

/* 待办列表 */
.todo-list { display: flex; flex-direction: column; gap: 10px; min-height: 160px; }
.todo-item {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 12px; background: #fafafa; border-radius: 8px;
  cursor: pointer; transition: background 0.2s; border: 1px solid transparent;
}
.todo-item:hover { background: #fff; border-color: #ddd; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.todo-img { width: 48px; height: 48px; border-radius: 6px; flex-shrink: 0; }
.todo-img-placeholder, .todo-order-icon {
  width: 48px; height: 48px; border-radius: 6px;
  display: flex; align-items: center; justify-content: center;
  font-size: 24px; flex-shrink: 0; background: #f0f0f0;
}
.todo-order-icon { background: #fff3e0; }
.todo-info { flex: 1; overflow: hidden; }
.todo-name { font-size: 14px; font-weight: 500; color: #333; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-bottom: 4px; }
.todo-meta { display: flex; align-items: center; gap: 8px; }
.todo-price { color: #f56c6c; font-size: 13px; font-weight: bold; }
.todo-reason { font-size: 12px; color: #999; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 160px; }
.todo-time { font-size: 12px; color: #bbb; white-space: nowrap; flex-shrink: 0; }

/* 饼图与快捷通道 */
.chart-card, .todo-card { border-radius: 12px; border: 1px solid #ebeef5; }
.card-header { font-size: 16px; font-weight: bold; color: #333; }
.shortcut-list { display: flex; flex-direction: column; gap: 12px; }
.shortcut-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px; background: #fafafa; border-radius: 8px;
  cursor: pointer; transition: background 0.3s; border: 1px solid transparent;
}
.shortcut-item:hover { background: #fff; border-color: #ffe60f; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
.shortcut-left { display: flex; align-items: center; }
.shortcut-icon { width: 44px; height: 44px; border-radius: 8px; display: flex; justify-content: center; align-items: center; font-size: 20px; margin-right: 14px; }
.shortcut-title { font-size: 14px; font-weight: bold; color: #333; margin-bottom: 3px; }
.shortcut-desc { font-size: 12px; color: #999; }
</style>