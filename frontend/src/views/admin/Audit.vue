<template>
  <div class="audit-container">
    <h2 style="margin-top: 0; margin-bottom: 20px;">商品审核中心</h2>

    <div class="filter-box">
      <el-radio-group v-model="filterStatus" @change="fetchGoodsList">
        <el-radio-button :value="null">全部商品</el-radio-button>
        <el-radio-button :value="0">待审核 (新发布)</el-radio-button>
        <el-radio-button :value="1">已通过 (展示中)</el-radio-button>
        <el-radio-button :value="2">已驳回 (待修改)</el-radio-button>
        <el-radio-button :value="3">违规下架</el-radio-button>
      </el-radio-group>
    </div>

    <el-table :data="goodsList" border stripe style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="商品ID" width="80" align="center" />
      <el-table-column prop="title" label="商品标题" show-overflow-tooltip />
      <el-table-column prop="category" label="分类" width="100" align="center" />
      <el-table-column prop="price" label="售价" width="100" align="center">
        <template #default="scope">
          <span style="color: #f56c6c; font-weight: bold;">￥{{ scope.row.price }}</span>
        </template>
      </el-table-column>
      <el-table-column label="当前状态" width="120" align="center">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 0" type="warning">待审核</el-tag>
          <el-tag v-else-if="scope.row.status === 1" type="success">已通过</el-tag>
          <el-tag v-else-if="scope.row.status === 2" type="info">已驳回</el-tag>
          <el-tag v-else-if="scope.row.status === 3" type="danger">违规下架</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="center" fixed="right">
        <template #default="scope">
          <el-button size="small" type="primary" plain @click="openDetails(scope.row)">查看详情审核</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-drawer v-model="drawerVisible" title="商品详细信息与审核" size="50%">
      <div v-if="currentGoods" class="goods-details">
        <div class="detail-item"><span class="label">商品标题：</span>{{ currentGoods.title }}</div>
        <div class="detail-item"><span class="label">商品分类：</span>{{ currentGoods.category }}</div>
        <div class="detail-item"><span class="label">售卖价格：</span><span class="price">￥{{ currentGoods.price }}</span> (原价: ￥{{ currentGoods.originalPrice }})</div>
        <div class="detail-item"><span class="label">商品描述：</span><div class="desc-box">{{ currentGoods.description }}</div></div>

        <div class="detail-item">
          <span class="label">商品图集：</span>
          <div class="img-list" v-if="currentGoods.imageUrl">
            <el-image
                v-for="(img, index) in currentGoods.imageUrl.split(',')"
                :key="index"
                :src="img"
                :preview-src-list="currentGoods.imageUrl.split(',')"
                :initial-index="index"
                fit="cover"
                class="preview-img"
            />
          </div>
        </div>

        <div class="audit-actions" v-if="currentGoods.status === 0 || currentGoods.status === 2">
          <h3>审核决策区</h3>
          <el-button type="success" size="large" @click="handlePass">✅ 审核通过 (允许上架)</el-button>
          <el-button type="warning" size="large" @click="openActionDialog(2)">⚠️ 驳回 (要求补全)</el-button>
          <el-button type="danger" size="large" @click="openActionDialog(3)">🚨 违规下架 (扣除信誉分)</el-button>
        </div>
        <div v-else style="margin-top: 30px; text-align: center; color: #999;">
          该商品已处理完毕，当前状态不允许再次审核。
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="actionDialogVisible" :title="actionType === 2 ? '驳回并要求补全' : '判定违规并下架'" width="450px">
      <el-form label-position="top">
        <el-form-item :label="actionType === 2 ? '补全/修改建议 (必填)' : '违规提醒/下架原因 (必填)'" required>
          <el-input
              v-model="auditForm.rejectReason"
              type="textarea"
              :rows="3"
              :placeholder="actionType === 2 ? '请输入需要用户补充说明或修改的内容...' : '请输入违规的具体原因...'"
          />
        </el-form-item>

        <el-form-item v-if="actionType === 3" label="扣除信誉分值" required>
          <el-input-number v-model="auditForm.creditDeduct" :min="0" :max="100" :step="1" />
          <span style="margin-left: 10px; color: #999; font-size: 12px;">注：扣分将直接影响用户购买和发布权限</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="actionDialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="submitAuditAction">确认提交并发送系统通知</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const goodsList = ref([])
const loading = ref(false)
const filterStatus = ref(0) // 默认筛选待审核

// 抽屉详情
const drawerVisible = ref(false)
const currentGoods = ref(null)

// 操作弹窗
const actionDialogVisible = ref(false)
const actionType = ref(2) // 2: 驳回, 3: 违规
const auditForm = reactive({
  rejectReason: '',
  creditDeduct: 0
})

const fetchGoodsList = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/goods/list', {
      params: { status: filterStatus.value === null ? undefined : filterStatus.value }
    })
    if (res.data.code === 200) {
      goodsList.value = res.data.data
    }
  } catch (error) {
    ElMessage.error('获取列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => fetchGoodsList())

const openDetails = (row) => {
  currentGoods.value = row
  drawerVisible.value = true
}

const openActionDialog = (type) => {
  actionType.value = type
  auditForm.rejectReason = ''
  auditForm.creditDeduct = type === 3 ? 5 : 0 // 如果是违规，默认扣除5分
  actionDialogVisible.value = true
}

// 直接通过
const handlePass = () => {
  ElMessageBox.confirm('确认让该商品审核通过并上架展示吗？同时会向用户发送通过通知。', '通过确认', {
    confirmButtonText: '确定上架',
    cancelButtonText: '取消',
    type: 'success'
  }).then(async () => {
    try {
      const res = await axios.post('/api/goods/audit', {
        id: currentGoods.value.id,
        status: 1
      })
      if (res.data.code === 200) {
        ElMessage.success('审核通过，商品已正式上架！')
        drawerVisible.value = false
        fetchGoodsList()
      }
    } catch (error) {
      ElMessage.error('操作失败')
    }
  }).catch(() => {})
}

// 提交驳回/违规
const submitAuditAction = async () => {
  if (!auditForm.rejectReason.trim()) {
    ElMessage.warning('必须填写原因或建议！')
    return
  }

  try {
    const res = await axios.post('/api/goods/audit', {
      id: currentGoods.value.id,
      status: actionType.value,
      rejectReason: auditForm.rejectReason,
      creditDeduct: actionType.value === 3 ? auditForm.creditDeduct : 0
    })

    if (res.data.code === 200) {
      ElMessage.success('操作成功！已向用户发送系统通知。')
      actionDialogVisible.value = false
      drawerVisible.value = false
      fetchGoodsList()
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}
</script>

<style scoped>
.audit-container { padding: 10px; }
.filter-box { margin-bottom: 20px; background-color: #fff; padding: 15px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }

/* 详情抽屉样式 */
.goods-details { padding: 0 20px; }
.detail-item { margin-bottom: 15px; font-size: 15px; line-height: 1.6; }
.label { font-weight: bold; color: #666; display: inline-block; width: 90px; }
.price { color: #f56c6c; font-size: 20px; font-weight: bold; }
.desc-box { background: #f9f9f9; padding: 10px; border-radius: 6px; margin-top: 5px; color: #333; }
.img-list { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 10px; }
.preview-img { width: 100px; height: 100px; border-radius: 6px; border: 1px solid #ebeef5; cursor: pointer; }

/* 审核决策区 */
.audit-actions { margin-top: 40px; padding-top: 20px; border-top: 1px dashed #ccc; display: flex; flex-direction: column; gap: 15px; }
.audit-actions h3 { margin-top: 0; color: #333; }
</style>