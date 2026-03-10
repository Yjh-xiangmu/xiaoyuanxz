<template>
  <div class="dashboard-container">
    <h2 class="page-title">平台数据总览与控制台</h2>

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
          <div class="todo-list">
            <div class="todo-item" @click="router.push('/admin/audit')">
              <div class="todo-left">
                <div class="todo-icon" style="background: #e8f4ff; color: #1890ff;">🔍</div>
                <div class="todo-text">
                  <div class="todo-title">商品审核中心</div>
                  <div class="todo-desc">审核新发布的闲置，下架违规物品</div>
                </div>
              </div>
              <el-icon class="arrow"><ArrowRight /></el-icon>
            </div>

            <div class="todo-item" @click="router.push('/admin/users')">
              <div class="todo-left">
                <div class="todo-icon" style="background: #fff3e0; color: #ff9800;">🚫</div>
                <div class="todo-text">
                  <div class="todo-title">用户与违规管理</div>
                  <div class="todo-desc">处理违规账号，手动调整信誉分</div>
                </div>
              </div>
              <el-icon class="arrow"><ArrowRight /></el-icon>
            </div>

            <div class="todo-item" @click="router.push('/admin/orders')">
              <div class="todo-left">
                <div class="todo-icon" style="background: #fce4ec; color: #e91e63;">⚖️</div>
                <div class="todo-text">
                  <div class="todo-title">交易与仲裁大厅</div>
                  <div class="todo-desc">全局订单监控，执行强制退款操作</div>
                </div>
              </div>
              <el-icon class="arrow"><ArrowRight /></el-icon>
            </div>

            <div class="todo-item" @click="router.push('/admin/reviews')">
              <div class="todo-left">
                <div class="todo-icon" style="background: #e8f5e9; color: #4caf50;">💬</div>
                <div class="todo-text">
                  <div class="todo-title">评价与合规管理</div>
                  <div class="todo-desc">屏蔽恶意差评与违规发言</div>
                </div>
              </div>
              <el-icon class="arrow"><ArrowRight /></el-icon>
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

// 大盘数据
const dashData = reactive({
  totalUsers: 0,
  activeGoods: 0,
  completedOrders: 0,
  totalAmount: 0,
  categoryPieData: []
})

// 获取后端数据
const fetchDashboardData = async () => {
  try {
    const res = await axios.get('/api/data/dashboard')
    if (res.data.code === 200) {
      Object.assign(dashData, res.data.data)
      // 数据加载完毕后渲染图表
      nextTick(() => {
        initPieChart()
      })
    }
  } catch (error) {
    ElMessage.error('获取大盘数据失败')
  }
}

// 渲染饼图
const initPieChart = () => {
  const chartDom = document.getElementById('adminPieChart')
  if (!chartDom) return
  const myChart = echarts.init(chartDom)

  const option = {
    tooltip: { trigger: 'item', formatter: '{a} <br/>{b} : {c}件 ({d}%)' },
    legend: { top: 'bottom' },
    color: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de'],
    series: [
      {
        name: '商品分类',
        type: 'pie',
        radius: [30, 120],
        center: ['50%', '45%'],
        roseType: 'area', // 南丁格尔玫瑰图模式，管理员专属高级感！
        itemStyle: { borderRadius: 8 },
        data: dashData.categoryPieData
      }
    ]
  }
  myChart.setOption(option)

  // 随窗口自适应
  window.addEventListener('resize', () => { myChart.resize() })
}

onMounted(() => fetchDashboardData())
</script>

<style scoped>
.dashboard-container { padding: 10px; }
.page-title { margin-top: 0; margin-bottom: 25px; color: #333; }

/* 核心数据卡片 */
.data-cards { margin-bottom: 20px; }
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

/* 图表与待办卡片 */
.chart-card, .todo-card { border-radius: 12px; border: 1px solid #ebeef5; }
.card-header { font-size: 16px; font-weight: bold; color: #333; }

/* 快捷待办列表 */
.todo-list { display: flex; flex-direction: column; gap: 15px; height: 350px; overflow-y: auto; }
.todo-item { display: flex; justify-content: space-between; align-items: center; padding: 15px; background: #fafafa; border-radius: 8px; cursor: pointer; transition: background 0.3s; border: 1px solid transparent; }
.todo-item:hover { background: #fff; border-color: #ffe60f; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
.todo-left { display: flex; align-items: center; }
.todo-icon { width: 45px; height: 45px; border-radius: 8px; display: flex; justify-content: center; align-items: center; font-size: 22px; margin-right: 15px; }
.todo-title { font-size: 15px; font-weight: bold; color: #333; margin-bottom: 4px; }
.todo-desc { font-size: 13px; color: #999; }
.arrow { color: #ccc; font-size: 20px; }
</style>