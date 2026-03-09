import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'

import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

// 🌟 引入 axios
import axios from 'axios'
// 🌟 全局配置后端的接口基础地址，以后所有组件里发请求都会自动带上这个前缀！
axios.defaults.baseURL = 'http://localhost:8080'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

app.mount('#app')