import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

/**
 * 路由表（PRD 第 10 章）。
 * 需登录页面 meta.requiresAuth = true；未登录访问跳 /login 并带 redirect 回跳（PRD 6.1 / 6.2.2）。
 * 已登录访问 / / /login / /register 自动跳 /today。
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'home',
    component: () => import('@/views/HomeView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/today',
    name: 'today',
    component: () => import('@/views/todo/TodayView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/date/:date',
    name: 'date',
    component: () => import('@/views/todo/DateView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/category/:id',
    name: 'category',
    component: () => import('@/views/todo/CategoryView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/search',
    name: 'search',
    component: () => import('@/views/todo/SearchView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/categories',
    name: 'categories',
    component: () => import('@/views/category/CategoriesView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/plan/:month?',
    name: 'plan-overview',
    component: () => import('@/views/plan/PlanOverviewView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/plan/performance/:month?',
    name: 'plan-performance',
    component: () => import('@/views/plan/PerformanceView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/plan/sprint/:month?',
    name: 'plan-sprint',
    component: () => import('@/views/plan/SprintView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/daily-report/:month?',
    name: 'daily-report',
    component: () => import('@/views/plan/DailyReportView.vue'),
    meta: { requiresAuth: true },
  },

  {
    path: '/settings',
    name: 'settings',
    component: () => import('@/views/SettingsView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/todo/:id',
    name: 'todo-detail',
    component: () => import('@/views/todo/TodoDetailView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 全局前置守卫
router.beforeEach((to) => {
  const auth = useAuthStore()
  // 已登录访问公开页（首页/登录/注册）→ 跳今日待办（PRD 6.1）
  if (auth.isLoggedIn && ['home', 'login', 'register'].includes(to.name as string)) {
    return { name: 'today' }
  }
  // 未登录访问需鉴权页 → 跳登录页并带 redirect（PRD 6.1）
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
