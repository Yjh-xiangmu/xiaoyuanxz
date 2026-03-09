<template>
  <div class="seller-container">
    <div class="seller-box" v-loading="loading">
      <div class="seller-card" v-if="sellerInfo.id">
        <div class="avatar">{{ sellerInfo.username.charAt(0).toUpperCase() }}</div>
        <div class="info-main">
          <h2 class="username">{{ sellerInfo.username }}</h2>
          <div class="tags">
            <el-tag :type="sellerInfo.creditScore >= 80 ? 'success' : 'danger'" effect="dark" round>
              信誉分：{{ sellerInfo.creditScore }}
            </el-tag>
            <el-tag type="warning" effect="dark" round style="margin-left: 10px;">
              好评率核心指标
            </el-tag>
          </div>
        </div>
        <div class="stats">
          <div class="stat-item">
            <div class="stat-value">{{ sellerInfo.avgRating }}<span class="small">星</span></div>
            <div class="stat-label">平均评价打分</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ sellerInfo.reviewCount }}<span class="small">人</span></div>
            <div class="stat-label">累计收到评价</div>
          </div>
        </div>
      </div>

      <el-tabs v-model="activeTab" class="custom-tabs" style="margin-top: 30px;">

        <el-tab-pane label="Ta 的在售闲置" name="goods">
          <el-empty v-if="activeGoodsList.length === 0" description="该卖家目前没有在售的闲置物品" />
          <div v-else class="card-grid">
            <el-card v-for="goods in activeGoodsList" :key="goods.id" class="goods-card" :body-style="{ padding: '0px' }" @click="goToDetail(goods.id)">
              <div class="image-wrapper">
                <el-image :src="goods.imageUrl ? goods.imageUrl.split(',')[0] : ''" class="cover-image" fit="cover" />
              </div>
              <div class="goods-info">
                <div class="goods-title">{{ goods.title }}</div>
                <div class="goods-bottom">
                  <span class="price"><span class="currency">￥</span>{{ goods.price }}</span>
                </div>
              </div>
            </el-card>
          </div>
        </el-tab-pane>

        <el-tab-pane label="历史买家评价" name="reviews">
          <el-empty v-if="reviewList.length === 0" description="暂无历史评价记录" />
          <div v-else class="review-list">
            <div v-for="(item, index) in reviewList" :key="index" class="review-item">
              <div class="review-header">
                <div class="buyer-info">
                  <div class="buyer-avatar">{{ item.buyerName.charAt(0) }}</div>
                  <span class="buyer-name">{{ item.buyerName }}</span>
                  <el-tag v-if="item.review.isAppend === 1" size="small" type="info" style="margin-left: 10px;">追加评价</el-tag>
                </div>
                <div class="review-time">{{ formatDate(item.review.createTime) }}</div>
              </div>

              <el-rate v-if="item.review.isAppend === 0" v-model="item.review.rating" disabled show-score text-color="#ff9900" style="margin-bottom: 10px;" />

              <div class="review-content">{{ item.review.content }}</div>
            </div>
          </div>
        </el-tab-pane>

      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const activeTab = ref('goods')

const sellerInfo = ref({})
const activeGoodsList = ref([])
const reviewList = ref([])

const fetchSellerData = async () => {
  const sellerId = route.params.id
  loading.value = true
  try {
    // 1. 获取卖家信息
    const res1 = await axios.get('/api/user/sellerInfo', { params: { sellerId } })
    if (res1.data.code === 200) sellerInfo.value = res1.data.data

    // 2. 获取该卖家的所有商品，并过滤出 status = 1 (在售) 的
    const res2 = await axios.get('/api/goods/myList', { params: { sellerId } })
    if (res2.data.code === 200) {
      activeGoodsList.value = res2.data.data.filter(g => g.status === 1)
    }

    // 3. 获取该卖家的评价列表
    const res3 = await axios.get('/api/review/sellerReviews', { params: { sellerId } })
    if (res3.data.code === 200) reviewList.value = res3.data.data

  } catch (error) {
    ElMessage.error('获取卖家主页数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => fetchSellerData())

const goToDetail = (id) => router.push(`/user/goods/${id}`)

const formatDate = (timeStr) => {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
</script>

<style scoped>
.seller-container { display: flex; justify-content: center; }
.seller-box { width: 1000px; background: #fff; padding: 30px 40px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); min-height: 600px; }

/* 顶部名片样式 */
.seller-card { display: flex; align-items: center; background: linear-gradient(135deg, #fffcf0 0%, #fff 100%); padding: 30px; border-radius: 12px; border: 1px solid #ffe60f; }
.avatar { width: 80px; height: 80px; background-color: #ffe60f; color: #333; font-size: 36px; font-weight: bold; border-radius: 50%; display: flex; justify-content: center; align-items: center; margin-right: 25px; box-shadow: 0 4px 10px rgba(255,230,15,0.4); }
.info-main { flex: 1; }
.username { margin: 0 0 10px 0; font-size: 24px; color: #333; }
.stats { display: flex; gap: 40px; }
.stat-item { text-align: center; }
.stat-value { font-size: 28px; font-weight: bold; color: #ff5000; }
.small { font-size: 14px; font-weight: normal; margin-left: 2px; color: #666; }
.stat-label { font-size: 13px; color: #999; margin-top: 5px; }

/* 闲置商品网格复用集市样式 */
.card-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 20px; }
.goods-card { border-radius: 12px; border: none; box-shadow: 0 2px 8px rgba(0,0,0,0.05); cursor: pointer; transition: all 0.3s; }
.goods-card:hover { transform: translateY(-5px); box-shadow: 0 8px 16px rgba(0,0,0,0.1); }
.image-wrapper { width: 100%; height: 200px; overflow: hidden; }
.cover-image { width: 100%; height: 100%; transition: transform 0.3s; }
.goods-card:hover .cover-image { transform: scale(1.05); }
.goods-info { padding: 12px; }
.goods-title { font-size: 14px; color: #333; font-weight: 500; height: 40px; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; margin-bottom: 8px;}
.price { color: #ff5000; font-size: 18px; font-weight: bold; }

/* 评价列表样式 */
.review-list { display: flex; flex-direction: column; gap: 20px; }
.review-item { padding: 20px; border: 1px solid #f0f0f0; border-radius: 8px; background: #fafafa; }
.review-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
.buyer-info { display: flex; align-items: center; }
.buyer-avatar { width: 32px; height: 32px; background: #ccc; color: #fff; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; margin-right: 10px; }
.buyer-name { font-weight: bold; color: #555; }
.review-time { font-size: 13px; color: #999; }
.review-content { font-size: 15px; color: #333; line-height: 1.6; }
</style>