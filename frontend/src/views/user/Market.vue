<template>
  <div class="market-container">
    <div class="filter-header">
      <div class="category-tabs">
        <span class="tab-item" :class="{ active: currentCategory === '' }" @click="selectCategory('')">全部闲置</span>
        <span class="tab-item" :class="{ active: currentCategory === '数码' }" @click="selectCategory('数码')">数码电子</span>
        <span class="tab-item" :class="{ active: currentCategory === '书籍' }" @click="selectCategory('书籍')">教材书籍</span>
        <span class="tab-item" :class="{ active: currentCategory === '生活' }" @click="selectCategory('生活')">生活用品</span>
        <span class="tab-item" :class="{ active: currentCategory === '服饰' }" @click="selectCategory('服饰')">服饰鞋包</span>
        <span class="tab-item" :class="{ active: currentCategory === '其他' }" @click="selectCategory('其他')">其他</span>
      </div>
      <div class="search-box">
        <el-input
            v-model="searchKeyword"
            placeholder="搜索你想要的闲置..."
            class="input-with-select"
            clearable
            @keyup.enter="fetchGoods"
            @clear="fetchGoods"
        >
          <template #append>
            <el-button style="background-color: #ffe60f; color: #333; font-weight: bold; border-radius: 0 4px 4px 0;" @click="fetchGoods">
              搜索
            </el-button>
          </template>
        </el-input>
      </div>
    </div>

    <div class="goods-list" v-loading="loading">
      <el-empty v-if="goodsList.length === 0" description="没有找到相关的闲置物品哦~" />

      <div v-else class="card-grid">
        <el-card
            v-for="goods in goodsList"
            :key="goods.id"
            class="goods-card"
            :body-style="{ padding: '0px' }"
            @click="goToDetail(goods.id)"
        >
          <div class="image-wrapper">
            <el-image
                :src="goods.imageUrl ? goods.imageUrl.split(',')[0] : ''"
                class="cover-image"
                fit="cover"
            />
          </div>
          <div class="goods-info">
            <div class="goods-title">{{ goods.title }}</div>
            <div class="goods-bottom">
              <span class="price">
                <span class="currency">￥</span>{{ goods.price }}
              </span>
              <span class="original-price" v-if="goods.originalPrice && goods.originalPrice > goods.price">
                原价￥{{ goods.originalPrice }}
              </span>
            </div>
            <div class="seller-tag">
              <el-tag size="small" type="info" effect="plain">{{ goods.category }}</el-tag>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const goodsList = ref([])
const loading = ref(false)

// 筛选条件
const currentCategory = ref('')
const searchKeyword = ref('')

// 获取审核通过的商品列表
const fetchGoods = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/goods/list', {
      params: {
        status: 1, // 核心：买家大厅只能看到审核通过(1)的商品
        category: currentCategory.value,
        keyword: searchKeyword.value
      }
    })
    if (res.data.code === 200) {
      goodsList.value = res.data.data
    }
  } catch (error) {
    ElMessage.error('获取商品列表失败')
  } finally {
    loading.value = false
  }
}

// 页面加载时自动获取一次
onMounted(() => {
  fetchGoods()
})

// 点击分类标签
const selectCategory = (cat) => {
  currentCategory.value = cat
  fetchGoods()
}

// 点击卡片进入详情（详情页下一步写）
const goToDetail = (id) => {
  router.push(`/user/goods/${id}`) // 占位，等写了详情页再放开
  ElMessage.success('点击了商品 ID: ' + id + '，进入详情页！')
}
</script>

<style scoped>
.market-container { padding: 0; }

/* 顶部筛选和搜索区 */
.filter-header {
  background: #fff;
  padding: 20px 30px;
  border-radius: 12px;
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
}
.category-tabs { display: flex; gap: 20px; }
.tab-item {
  font-size: 16px;
  color: #666;
  cursor: pointer;
  padding: 5px 10px;
  border-radius: 20px;
  transition: all 0.3s;
}
.tab-item:hover { color: #333; background: #f5f5f5; }
.tab-item.active { background: #ffe60f; color: #333; font-weight: bold; }

.search-box { width: 350px; }
:deep(.el-input-group__append) { background-color: #ffe60f; border-color: #ffe60f; }

/* 商品网格布局 */
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
}

/* 卡片样式仿闲鱼 */
.goods-card {
  border-radius: 12px;
  border: none;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  cursor: pointer;
  transition: all 0.3s;
}
.goods-card:hover { transform: translateY(-5px); box-shadow: 0 8px 24px rgba(0,0,0,0.12); }

.image-wrapper { width: 100%; height: 220px; overflow: hidden; }
.cover-image { width: 100%; height: 100%; transition: transform 0.3s; }
.goods-card:hover .cover-image { transform: scale(1.05); }

.goods-info { padding: 12px; }
.goods-title {
  font-size: 15px;
  color: #333;
  font-weight: 500;
  line-height: 1.4;
  height: 42px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  margin-bottom: 8px;
}
.goods-bottom { display: flex; align-items: baseline; gap: 8px; }
.price { color: #ff5000; font-size: 20px; font-weight: bold; }
.currency { font-size: 14px; margin-right: 2px; }
.original-price { color: #999; font-size: 12px; text-decoration: line-through; }
.seller-tag { margin-top: 10px; }
</style>