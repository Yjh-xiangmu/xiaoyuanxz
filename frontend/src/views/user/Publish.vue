<template>
  <div class="publish-container">
    <div class="publish-box">
      <h2 class="page-title">发布闲置物品</h2>
      <el-alert title="发布须知：所有商品提交后需经过管理员审核方可上架展示。" type="warning" show-icon style="margin-bottom: 20px;" />

      <el-form :model="goodsForm" label-width="100px" class="form-content">
        <el-form-item label="商品标题" required>
          <el-input v-model="goodsForm.title" placeholder="请输入商品标题（如：九成新 iPhone 13）" maxlength="50" show-word-limit />
        </el-form-item>

        <el-form-item label="商品分类" required>
          <el-select v-model="goodsForm.category" placeholder="请选择分类" style="width: 100%">
            <el-option label="数码电子" value="数码" />
            <el-option label="教材书籍" value="书籍" />
            <el-option label="生活用品" value="生活" />
            <el-option label="服饰鞋包" value="服饰" />
            <el-option label="其他闲置" value="其他" />
          </el-select>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="售卖价格(元)" required>
              <el-input-number v-model="goodsForm.price" :precision="2" :step="1" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="购入原价(元)">
              <el-input-number v-model="goodsForm.originalPrice" :precision="2" :step="1" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="商品描述" required>
          <el-input v-model="goodsForm.description" type="textarea" :rows="5" placeholder="请详细描述商品的新旧程度、购买时间、是否有瑕疵等..." maxlength="500" show-word-limit />
        </el-form-item>

        <el-form-item label="商品图/视频" required>
          <el-upload
              v-model:file-list="fileList"
              action="http://localhost:8080/api/file/upload"
              list-type="picture-card"
              multiple
              :limit="9"
              accept="image/*,video/*"
              :on-success="handleUploadSuccess"
              :on-remove="handleRemove"
              :on-exceed="handleExceed"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="el-upload__tip" style="color: #999; margin-top: 8px; line-height: 1.5;">
            点击加号上传。支持上传最多 9 张图片或视频。<br/>
            (上传成功后，文件将直接保存在你电脑的 uploads 文件夹中)
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" class="publish-btn" @click="submitPublish">确认发布 (提交审核)</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
// 🌟 引入 ElMessageBox 用于弹窗
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import axios from 'axios'

const userStore = useUserStore()
const router = useRouter()

const goodsForm = reactive({
  title: '',
  category: '',
  price: 0,
  originalPrice: 0,
  description: '',
  imageUrl: ''
})

const fileList = ref([])

const handleUploadSuccess = (response, uploadFile) => {
  if (response.code === 200) {
    uploadFile.url = response.data
    ElMessage.success('上传成功！')
    updateImageUrlForm()
  } else {
    ElMessage.error(response.msg || '上传失败')
  }
}

const handleRemove = () => {
  updateImageUrlForm()
}

const handleExceed = () => {
  ElMessage.warning('最多只能上传 9 个文件哦！')
}

const updateImageUrlForm = () => {
  const urls = fileList.value.map(item => item.url || (item.response && item.response.data)).filter(url => url)
  goodsForm.imageUrl = urls.join(',')
}

// 🌟 新增：清空表单的方法
const resetForm = () => {
  goodsForm.title = ''
  goodsForm.category = ''
  goodsForm.price = 0
  goodsForm.originalPrice = 0
  goodsForm.description = ''
  goodsForm.imageUrl = ''
  fileList.value = [] // 清空已上传的图片卡片
}

const submitPublish = async () => {
  if (!goodsForm.title || !goodsForm.category || !goodsForm.price || !goodsForm.description) {
    ElMessage.warning('请将带红星的必填项填写完整！')
    return
  }
  if (!goodsForm.imageUrl) {
    ElMessage.warning('请至少上传一张商品图片或视频！')
    return
  }

  try {
    const res = await axios.post('/api/goods/publish', {
      ...goodsForm,
      sellerId: userStore.userInfo.id
    })

    if (res.data.code === 200) {
      // 🌟 核心改动：不再直接跳转，而是弹出友好的选择框
      ElMessageBox.confirm(
          '商品发布成功！已提交给管理员审核。接下来您想？',
          '🎉 发布成功',
          {
            confirmButtonText: '继续发闲置',
            cancelButtonText: '返回大厅',
            type: 'success',
            center: true, // 文字居中更好看
          }
      ).then(() => {
        // 用户点击了“继续发闲置”
        resetForm()
      }).catch(() => {
        // 用户点击了“返回大厅”
        router.push('/user/home')
      })

    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (error) {
    ElMessage.error('网络请求失败')
  }
}
</script>

<style scoped>
.publish-container { display: flex; justify-content: center; }
.publish-box { width: 800px; background: #fff; padding: 30px 40px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
.page-title { margin-top: 0; margin-bottom: 20px; color: #333; border-bottom: 2px solid #ffe60f; display: inline-block; padding-bottom: 8px; }
.publish-btn { background-color: #ffe60f; border-color: #ffe60f; color: #333; font-weight: bold; width: 200px; height: 40px; border-radius: 20px; transition: all 0.3s;}
.publish-btn:hover { background-color: #ffd000; border-color: #ffd000; transform: translateY(-1px);}
:deep(.el-upload--picture-card) { background-color: #fafafa; border: 1px dashed #d9d9d9; }
:deep(.el-upload--picture-card:hover) { border-color: #ffe60f; color: #ffe60f; }
</style>