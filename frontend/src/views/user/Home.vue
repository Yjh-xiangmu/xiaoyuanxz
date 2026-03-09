<template>
  <div class="home-container">
    <div class="home-box">
      <div class="welcome-header">
        <div class="greeting">
          <h2>👋 欢迎回来，<span class="highlight">{{ userStore.userInfo.username }}</span>！</h2>
          <p class="subtitle">让校园里的每一件闲置，都找到它的新主人。</p>
        </div>
      </div>

      <el-row :gutter="20" class="data-cards">
        <el-col :span="6">
          <el-card shadow="hover" class="data-card bg-blue">
            <div class="card-icon">👥</div>
            <div class="card-info">
              <div class="card-title">平台入驻师生</div>
              <div class="card-value">{{ dashData.totalUsers }} <span class="unit">人</span></div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="data-card bg-green">
            <div class="card-icon">📦</div>
            <div class="card-info">
              <div class="card-title">当前在售闲置</div>
              <div class="card-value">{{ dashData.activeGoods }} <span class="unit">件</span></div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="data-card bg-orange">
            <div class="card-icon">🤝</div>
            <div class="card-info">
              <div class="card-title">累计撮合成交</div>
              <div class="card-value">{{ dashData.completedOrders }} <span class="unit">笔</span></div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="data-card bg-red">
            <div class="card-icon">💰</div>
            <div class="card-info">
              <div class="card-title">累计交易总额</div>
              <div class="card-value"><span class="unit">￥</span>{{ dashData.totalAmount }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" class="bottom-section">
        <el-col :span="10">
          <el-card shadow="never" class="quick-actions-card">
            <template #header>
              <div class="card-header">⚡ 快捷操作台</div>
            </template>
            <div class="action-grid">
              <div class="action-item" @click="router.push('/user/market')">
                <div class="action-icon" style="background: #e8f4ff; color: #1890ff;">🛒</div>
                <div class="action-name">去逛集市</div>
              </div>
              <div class="action-item" @click="router.push('/user/publish')">
                <div class="action-icon" style="background: #fff3e0; color: #ff9800;">📸</div>
                <div class="action-name">发布闲置</div>
              </div>
              <div class="action-item" @click="router.push('/user/orders')">
                <div class="action-icon" style="background: #e8f5e9; color: #4caf50;">🚚</div>
                <div class="action-name">我买到的</div>
              </div>
              <div class="action-item" @click="router.push('/user/sellorders')">
                <div class="action-icon" style="background: #fce4ec; color: #e91e63;">💸</div>
                <div class="action-name">我卖出的</div>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="14">
          <el-card shadow="never" class="chart-card">
            <template #header>
              <div class="card-header">📊 平台商品分类占比</div>
            </template>
            <div id="categoryPieChart" style="width: 100%; height: 300px;"></div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import * as echarts from 'echarts' // 🌟 引入 ECharts

const userStore = useUserStore()
const router = useRouter()

// 大盘数据对象
const dashData = reactive({
  totalUsers: 0,
  activeGoods: 0,
  completedOrders: 0,
  totalAmount: 0,
  categoryPieData: []
})

// 拉取后端大盘数据
const fetchDashboardData = async () => {
  try {
    const res = await axios.get('/api/data/dashboard')
    if (res.data.code === 200) {
      Object.assign(dashData, res.data.data)
      // 数据拿到后，等 DOM 渲染完毕，开始画图
      nextTick(() => {
        initPieChart()
      })
    }
  } catch (error) {
    ElMessage.error('获取大盘数据失败')
  }
}

// 初始化 ECharts 饼图
const initPieChart = () => {
  const chartDom = document.getElementById('categoryPieChart')
  if (!chartDom) return
  const myChart = echarts.init(chartDom)

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b} : {c}件 ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    color: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de'],
    series: [
      {
        name: '商品分类',
        type: 'pie',
        radius: ['40%', '70%'], // 环形图设计，更美观
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: { show: false, position: 'center' },
        emphasis: {
          label: { show: true, fontSize: '20', fontWeight: 'bold' }
        },
        labelLine: { show: false },
        data: dashData.categoryPieData // 绑定后端返回的数据！
      }
    ]
  }
  myChart.setOption(option)

  // 监听窗口缩放，图表自适应大小
  window.addEventListener('resize', () => {
    myChart.resize()
  })
}

onMounted(() => {
  fetchDashboardData()
})
</script>

<style scoped>
.home-container { display: flex; justify-content: center; }
.home-box { width: 1000px; padding: 20px 0; min-height: 600px; }

/* 欢迎头部 */
.welcome-header { margin-bottom: 30px; }
.greeting h2 { margin: 0; color: #333; font-size: 26px; font-weight: 600;}
.highlight { color: #ff5000; }
.subtitle { margin: 10px 0 0 0; color: #666; font-size: 15px; }

/* 数据卡片 */
.data-cards { margin-bottom: 30px; }
.data-card { border: none; border-radius: 12px; transition: transform 0.3s; cursor: pointer; color: #fff;}
.data-card:hover { transform: translateY(-5px); box-shadow: 0 8px 16px rgba(0,0,0,0.1); }
:deep(.data-card .el-card__body) { display: flex; align-items: center; padding: 20px; }
.card-icon { font-size: 36px; margin-right: 15px; opacity: 0.9; }
.card-info { flex: 1; }
.card-title { font-size: 14px; opacity: 0.9; margin-bottom: 5px; }
.card-value { font-size: 26px; font-weight: bold; }
.unit { font-size: 14px; font-weight: normal; }

/* 卡片渐变背景色 */
.bg-blue { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }
.bg-green { background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); }
.bg-orange { background: linear-gradient(135deg, #fa709a 0%, #fee140 100%); }
.bg-red { background: linear-gradient(135deg, #ff0844 0%, #ffb199 100%); }

/* 底部图表与操作区 */
.bottom-section .el-card { border-radius: 12px; border: 1px solid #ebeef5; }
.card-header { font-size: 16px; font-weight: bold; color: #333; }

.action-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; padding: 10px; }
.action-item { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 20px; background: #fafafa; border-radius: 12px; cursor: pointer; transition: all 0.3s; }
.action-item:hover { background: #f0f0f0; transform: scale(1.02); }
.action-icon { width: 50px; height: 50px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 24px; margin-bottom: 10px; }
.action-name { font-size: 14px; color: #333; font-weight: 500; }
</style>