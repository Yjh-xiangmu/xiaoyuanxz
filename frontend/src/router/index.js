import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/login'
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/login/Login.vue')
    },
    // 管理员路由体系
    {
      path: '/admin',
      component: () => import('../layout/admin/AdminLayout.vue'),
      redirect: '/admin/dashboard',
      children: [
        { path: 'dashboard', component: () => import('../views/admin/Dashboard.vue') },
        { path: 'audit', component: () => import('../views/admin/Audit.vue') }
        // TODO: 之后把审核中心等加在这里
      ]
    },
    // 普通用户路由体系
    {
      path: '/user',
      component: () => import('../layout/user/UserLayout.vue'),
      redirect: '/user/home',
      children: [
        { path: 'home', component: () => import('../views/user/Home.vue') },
        { path: 'publish', component: () => import('../views/user/Publish.vue') },
        { path: 'message', component: () => import('../views/user/Message.vue') },
        { path: 'market', component: () => import('../views/user/Market.vue') },
        { path: 'goods/:id', component: () => import('../views/user/GoodsDetail.vue') },
        { path: 'orders', component: () => import('../views/user/Orders.vue') },
        { path: 'address', component: () => import('../views/user/Address.vue') },
        { path: 'mygoods', component: () => import('../views/user/MyGoods.vue') },
        { path: 'sellorders', component: () => import('../views/user/SellOrders.vue') }
        // TODO: 之后把集市、发布闲置等加在这里
      ]
    }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (to.path !== '/login' && !userStore.userInfo.id) {
    next('/login')
  } else {
    next()
  }
})

export default router