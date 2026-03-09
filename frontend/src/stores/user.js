import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
    // 尝试从浏览器本地存储获取用户信息，防止刷新页面后登录状态丢失
    const userInfo = ref(JSON.parse(localStorage.getItem('userInfo')) || {})

    // 登录成功后保存信息
    const setUserInfo = (data) => {
        userInfo.value = data
        localStorage.setItem('userInfo', JSON.stringify(data))
    }

    // 退出登录时清除信息
    const clearUserInfo = () => {
        userInfo.value = {}
        localStorage.removeItem('userInfo')
    }

    return { userInfo, setUserInfo, clearUserInfo }
})