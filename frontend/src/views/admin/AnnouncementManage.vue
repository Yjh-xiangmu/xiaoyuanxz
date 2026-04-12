<template>
  <div class="admin-announcement-container">
    <h2 style="margin-top: 0; margin-bottom: 20px;">系统公告管理</h2>

    <div class="filter-box">
      <el-button type="primary" color="#ff5000" style="color: white; font-weight: bold;" @click="handleAdd">
        发布新公告
      </el-button>
    </div>

    <el-table :data="tableData" border stripe style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="公告ID" width="80" align="center" />
      <el-table-column prop="title" label="公告标题" width="300" />
      <el-table-column prop="content" label="公告内容" show-overflow-tooltip />
      <el-table-column prop="createTime" label="发布时间" width="200" align="center" />

      <el-table-column label="平台操作" width="200" align="center" fixed="right">
        <template #default="scope">
          <el-button size="small" type="primary" plain @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="600px">
      <el-form :model="form" label-width="80px" label-position="top">
        <el-form-item label="公告标题" required>
          <el-input v-model="form.title" placeholder="请输入公告标题"></el-input>
        </el-form-item>
        <el-form-item label="公告详细内容" required>
          <el-input type="textarea" :rows="6" v-model="form.content" placeholder="请输入公告内容"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" color="#ff5000" style="color: white; font-weight: bold;" @click="save">
          确 认 发 布
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('发布公告')
const form = ref({ title: '', content: '' })

// 获取公告列表
const fetchAnnouncements = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/announcement/list')
    if (res.data.code === 200) {
      tableData.value = res.data.data
    }
  } catch (error) {
    ElMessage.error('获取列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => fetchAnnouncements())

const handleAdd = () => {
  dialogTitle.value = '发布公告'
  form.value = { title: '', content: '' }
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑公告'
  form.value = JSON.parse(JSON.stringify(row))
  dialogVisible.value = true
}

const save = async () => {
  if (!form.value.title.trim() || !form.value.content.trim()) {
    ElMessage.warning('公告标题和内容不能为空！')
    return
  }
  try {
    let res
    if (form.value.id) {
      res = await axios.put('/api/announcement/update', form.value)
    } else {
      res = await axios.post('/api/announcement/add', form.value)
    }

    if (res.data.code === 200) {
      ElMessage.success(form.value.id ? '更新成功' : '发布成功')
      dialogVisible.value = false
      fetchAnnouncements()
    } else {
      ElMessage.error(res.data.msg || '操作失败')
    }
  } catch (error) {
    ElMessage.error('网络异常，操作失败')
  }
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定要永久删除这条公告吗？', '危险操作确认', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'error'
  }).then(async () => {
    try {
      const res = await axios.delete('/api/announcement/delete/' + id)
      if (res.data.code === 200) {
        ElMessage.success('删除成功')
        fetchAnnouncements()
      } else {
        ElMessage.error(res.data.msg || '删除失败')
      }
    } catch (error) {
      ElMessage.error('网络异常，删除失败')
    }
  }).catch(() => {})
}
</script>

<style scoped>
.admin-announcement-container {
  padding: 10px;
}
.filter-box {
  margin-bottom: 20px;
  background-color: #fff;
  padding: 15px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  display: flex;
  align-items: center;
}
</style>