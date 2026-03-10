<template>
  <div class="admin-reviews-container">
    <h2 style="margin-top: 0; margin-bottom: 20px;">评价内容与合规管理</h2>

    <div class="filter-box">
      <el-input
          v-model="searchKeyword"
          placeholder="输入评价内容关键字搜索敏感词..."
          clearable
          style="width: 350px;"
          @keyup.enter="fetchReviews"
          @clear="fetchReviews"
      >
        <template #append>
          <el-button @click="fetchReviews">内容检索</el-button>
        </template>
      </el-input>
    </div>

    <el-table :data="reviewList" border stripe style="width: 100%" v-loading="loading">

      <el-table-column label="评价详情 (含敏感词检索)" min-width="300">
        <template #default="scope">
          <div style="font-size: 14px; line-height: 1.6; color: #333;">
            <el-tag v-if="scope.row.review.isAppend === 1" size="small" type="info" style="margin-right: 8px;">追评</el-tag>
            <span :style="{ color: scope.row.review.content.includes('屏蔽') ? '#f56c6c' : '#333' }">
              {{ scope.row.review.content }}
            </span>
          </div>
          <div style="margin-top: 8px;">
            <el-rate
                v-if="scope.row.review.isAppend === 0"
                v-model="scope.row.review.rating"
                disabled
                show-score
                text-color="#ff9900"
            />
          </div>
        </template>
      </el-table-column>

      <el-table-column label="交易双方" width="200">
        <template #default="scope">
          <div style="font-size: 13px; color: #666;">
            <div>🗣️ 买家: <b>{{ scope.row.buyerName }}</b></div>
            <div style="margin-top: 5px;">🏪 卖家: <b>{{ scope.row.sellerName }}</b></div>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="关联订单 / 商品 ID" width="160" align="center">
        <template #default="scope">
          <div style="font-size: 13px;">
            <div>单号: {{ scope.row.review.orderId }}</div>
            <div>商品: {{ scope.row.review.goodsId }}</div>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="review.createTime" label="评价时间" width="170" align="center" />

      <el-table-column label="合规操作" width="120" align="center" fixed="right">
        <template #default="scope">
          <el-button
              v-if="!scope.row.review.content.includes('已被管理员屏蔽')"
              size="small" type="danger"
              @click="handleShield(scope.row.review.id)"
          >
            屏蔽内容
          </el-button>
          <span v-else style="color: #999; font-size: 12px;">已屏蔽</span>
        </template>
      </el-table-column>

    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const reviewList = ref([])
const loading = ref(false)
const searchKeyword = ref('')

// 获取所有评价列表
const fetchReviews = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/review/adminList', { params: { keyword: searchKeyword.value } })
    if (res.data.code === 200) {
      reviewList.value = res.data.data
    }
  } catch (error) {
    ElMessage.error('获取评价列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => fetchReviews())

// 🌟 管理员屏蔽恶意评价
const handleShield = (id) => {
  ElMessageBox.confirm(
      '确认屏蔽该条评价吗？屏蔽后评价内容将被官方声明替换，并自动下发系统红牌警告给该买家！',
      '违规屏蔽确认',
      {
        confirmButtonText: '强制屏蔽',
        cancelButtonText: '取消',
        type: 'error'
      }
  ).then(async () => {
    try {
      const res = await axios.post(`/api/review/shield/${id}`)
      if (res.data.code === 200) {
        ElMessage.success(res.data.msg)
        fetchReviews() // 刷新列表，文字会变成被屏蔽的红字
      }
    } catch (error) {
      ElMessage.error('操作失败')
    }
  }).catch(() => {})
}
</script>

<style scoped>
.admin-reviews-container { padding: 10px; }
.filter-box { margin-bottom: 20px; background-color: #fff; padding: 15px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); display: flex; align-items: center; }
</style>