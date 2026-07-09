<template>
  <div class="app-container">
    <!-- Mobile Header -->
    <header class="mobile-header">
      <button class="menu-toggle" @click="isSidebarOpen = !isSidebarOpen">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
        </svg>
      </button>
      <span class="mobile-title">Recall Todo</span>
      <div style="width: 24px;"></div>
    </header>

    <!-- Sidebar Overlay for Mobile -->
    <div v-if="isSidebarOpen" class="sidebar-overlay" @click="isSidebarOpen = false"></div>

    <!-- Sidebar -->
    <aside class="app-sidebar" :class="{ 'sidebar-open': isSidebarOpen, 'sidebar-collapsed': isCollapsed }">
      <!-- Sidebar Brand -->
      <div class="sidebar-brand">
        <div class="brand-logo">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="logo-svg">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12c0 1.268-.63 2.39-1.593 3.068a3.745 3.745 0 01-1.043 3.296 3.745 3.745 0 01-3.296 1.043A3.745 3.745 0 0110 21c-1.268 0-2.39-.63-3.068-1.593a3.746 3.746 0 01-3.296-1.043 3.745 3.745 0 01-1.043-3.296A3.745 3.745 0 013 12c0-1.268.63-2.39 1.593-3.068a3.745 3.745 0 011.043-3.296 3.746 3.746 0 013.296-1.043A3.746 3.746 0 0114 3c1.268 0 2.39.63 3.068 1.593a3.746 3.746 0 013.296 1.043 3.746 3.746 0 011.043 3.296A3.745 3.745 0 0121 12z" />
          </svg>
        </div>
        <span class="brand-name">Recall · 待办系统</span>
      </div>

      <!-- Navigation Content -->
      <nav class="sidebar-nav">
        <!-- Execution Group -->
        <div class="nav-group">
          <div class="nav-group-title">日常执行</div>
          
          <router-link to="/today" class="nav-item" active-class="nav-item-active">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="nav-icon">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5A2.25 2.25 0 0121 11.25v7.5m-9-6h.008v.008H12v-.008zM12 15h.008v.008H12V15zm0 2.25h.008v.008H12v-.008zM9.75 15h.008v.008H9.75V15zm0 2.25h.008v.008H9.75v-.008zM7.5 15h.008v.008H7.5V15zm0 2.25h.008v.008H7.5v-.008zM14.25 15h.008v.008H14.25V15zm0 2.25h.008v.008H14.25v-.008zM16.5 15h.008v.008H16.5V15zm0 2.25h.008v.008H16.5v-.008z" />
            </svg>
            <span class="nav-text">今日待办</span>
            <span v-if="todayPendingCount > 0" class="nav-badge">{{ todayPendingCount }}</span>
          </router-link>

          <router-link to="/search" class="nav-item" active-class="nav-item-active">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="nav-icon">
              <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.604 10.604z" />
            </svg>
            <span class="nav-text">全局搜索</span>
          </router-link>

          <router-link :to="`/date/${todayDateStr}`" class="nav-item" active-class="nav-item-active" :class="{ 'nav-item-active': $route.name === 'date' }">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="nav-icon">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5A2.25 2.25 0 0121 11.25v7.5m-9-6h.008v.008H12v-.008zM12 15h.008v.008H12V15zm0 2.25h.008v.008H12v-.008zM9.75 15h.008v.008H9.75V15zm0 2.25h.008v.008H9.75v-.008zM7.5 15h.008v.008H7.5V15zm0 2.25h.008v.008H7.5v-.008zm6.75-4.5h.008v.008h-.008v-.008zm0 2.25h.008v.008h-.008V15zm0 2.25h.008v.008h-.008v-.008zm2.25-4.5h.008v.008H16.5v-.008zm0 2.25h.008v.008H16.5V15z" />
            </svg>
            <span class="nav-text">日历视图</span>
          </router-link>
        </div>

        <!-- Productivity Group -->
        <div class="nav-group">
          <div class="nav-group-title">工作效能</div>
          
          <router-link to="/daily-report" class="nav-item" active-class="nav-item-active" :class="{ 'nav-item-active': $route.name === 'daily-report' }">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="nav-icon">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 7.5h1.5m-1.5 3h1.5m-7.5 3h7.5m-7.5 3h7.5m3-9h3.375c.621 0 1.125.504 1.125 1.125V18a2.25 2.25 0 01-2.25 2.25H5.25A2.25 2.25 0 013 17.75V5.625C3 5.004 3.504 4.5 4.125 4.5h9.75M8.25 21h8.25" />
            </svg>
            <span class="nav-text">个人日报</span>
          </router-link>
        </div>

        <!-- Planning Group -->
        <div class="nav-group">
          <div class="nav-group-title">月度规划</div>

          <router-link to="/plan" class="nav-item" active-class="nav-item-active" :class="{ 'nav-item-active': $route.name === 'plan-overview' }">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="nav-icon">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 17.25v1.007a3 3 0 01-.879 2.122L7.5 21h9l-.621-.621A3 3 0 0115 18.257V17.25m6-12V15a2.25 2.25 0 01-2.25 2.25H5.25A2.25 2.25 0 013 15V5.25m18 0A2.25 2.25 0 0018.75 3H5.25A2.25 2.25 0 003 5.25m18 0V12a2.25 2.25 0 01-2.25 2.25H5.25A2.25 2.25 0 013 12V5.25" />
            </svg>
            <span class="nav-text">月度总览</span>
          </router-link>

          <router-link to="/plan/performance" class="nav-item" active-class="nav-item-active" :class="{ 'nav-item-active': $route.name === 'plan-performance' }">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="nav-icon">
              <path stroke-linecap="round" stroke-linejoin="round" d="M15.59 14.37a6 6 0 01-5.84 7.38v-4.8m5.84-2.58a14.98 14.98 0 006.16-12.12A14.98 14.98 0 009.64 8.38m9.79 3.41A14.97 14.97 0 0116.5 16.5M9.64 8.38a14.96 14.96 0 01-6.16 12.12A14.96 14.96 0 019.64 8.38zm0 0c1.33 0 2.5-.35 3.5-.96" />
            </svg>
            <span class="nav-text">个人绩效</span>
          </router-link>

          <router-link to="/plan/sprint" class="nav-item" active-class="nav-item-active" :class="{ 'nav-item-active': $route.name === 'plan-sprint' }">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="nav-icon">
              <path stroke-linecap="round" stroke-linejoin="round" d="M18 18.72a9.094 9.094 0 003.741-.479 3 3 0 00-4.682-2.72m.94 3.198l.001.031c0 .225-.012.447-.037.666A11.944 11.944 0 0112 21c-2.17 0-4.207-.576-5.963-1.584A6.062 6.062 0 016 18.719m12 0a5.97 5.97 0 00-.75-2.985m-.938-3.197A5.903 5.903 0 0012 12a5.903 5.903 0 00-4.312 2.733m-.938 3.197A5.97 5.97 0 006 18.72m0 0a5.002 5.002 0 01-5.007-5.007c0-1.39.565-2.65 1.48-3.56a5.002 5.002 0 017.054 0c.915.91.48 2.17.48 3.56A5.002 5.002 0 016 18.72zm0-9a3 3 0 100-6 3 3 0 000 6zm12 0a3 3 0 100-6 3 3 0 000 6zM9.75 9a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0z" />
            </svg>
            <span class="nav-text">团队冲刺</span>
          </router-link>
        </div>

        <!-- Categories Group -->
        <div class="nav-group">
          <div class="nav-group-title header-actionable">
            <span>我的分类</span>
            <router-link to="/categories" class="header-action-btn" title="管理分类">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" style="width: 14px; height: 14px;">
                <path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10" />
              </svg>
            </router-link>
          </div>
          <div class="categories-list">
            <router-link 
              v-for="cat in categories" 
              :key="cat.id" 
              :to="`/category/${cat.id}`" 
              class="nav-item category-item"
              active-class="nav-item-active"
            >
              <span class="color-dot" :style="{ backgroundColor: cat.color }"></span>
              <span class="nav-text">{{ cat.name }}</span>
              <span v-if="getCategoryPendingCount(cat.id) > 0" class="nav-badge">{{ getCategoryPendingCount(cat.id) }}</span>
            </router-link>
            <div v-if="categories.length === 0" class="nav-empty">暂无分类</div>
          </div>
        </div>

        <!-- Analysis & Utilities -->
        <div class="nav-group">
          <div class="nav-group-title">系统设置</div>

          <router-link to="/settings" class="nav-item" active-class="nav-item-active">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="nav-icon">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9.594 3.94c.09-.542.56-.94 1.11-.94h2.593c.55 0 1.02.398 1.11.94l.213 1.281c.063.374.313.686.645.87.074.04.147.083.22.127.324.196.72.257 1.075.124l1.217-.456a1.125 1.125 0 011.37.49l1.296 2.247a1.125 1.125 0 01-.26 1.43l-1.003.828c-.293.241-.438.613-.43.992a7.723 7.723 0 010 .255c-.008.378.137.75.43.991l1.004.827c.424.35.534.954.26 1.43l-1.298 2.247a1.125 1.125 0 01-1.369.491l-1.217-.456c-.355-.133-.75-.072-1.076.124a6.57 6.57 0 01-.22.128c-.331.183-.581.495-.644.869l-.213 1.28c-.09.543-.56.94-1.11.94h-2.594c-.55 0-1.02-.398-1.11-.94l-.213-1.281c-.062-.374-.312-.686-.644-.87a6.52 6.52 0 01-.22-.127c-.325-.196-.72-.257-1.076-.124l-1.217.456a1.125 1.125 0 01-1.369-.49l-1.297-2.247a1.125 1.125 0 01.26-1.43l1.004-.827c.292-.24.437-.613.43-.992a6.932 6.932 0 010-.255c.007-.378-.138-.75-.43-.991l-1.004-.827a1.125 1.125 0 01-.26-1.43l1.297-2.247a1.125 1.125 0 011.37-.491l1.216.456c.356.133.751.072 1.076-.124.072-.044.146-.087.22-.128.332-.183.582-.495.645-.869l.214-1.28z" />
              <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            <span class="nav-text">设置</span>
          </router-link>
        </div>
      </nav>

      <!-- Sidebar Footer User Info -->
      <div class="sidebar-footer">
        <div class="user-card" @click.stop="isDropdownOpen = !isDropdownOpen">
          <div class="user-avatar">
            {{ userInitial }}
          </div>
          <div class="user-info">
            <div class="user-name">{{ nickname }}</div>
            <div class="user-email">{{ email }}</div>
          </div>
          <div class="chevron">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" style="width: 16px; height: 16px;">
              <path fill-rule="evenodd" d="M10 3a.75.75 0 01.75.75v10.63l3.72-3.72a.75.75 0 111.06 1.06l-5 5a.75.75 0 01-1.06 0l-5-5a.75.75 0 111.06-1.06l3.72 3.72V3.75A.75.75 0 0110 3z" clip-rule="evenodd" />
            </svg>
          </div>
          
          <!-- Dropdown Popover -->
          <div v-if="isDropdownOpen" class="user-dropdown">
            <router-link to="/settings" class="dropdown-item">
              <span>设置与密码</span>
            </router-link>
            <div class="dropdown-divider"></div>
            <div class="dropdown-item danger" @click="handleLogout">
              <span>安全退出</span>
            </div>
          </div>
        </div>

        <!-- Sidebar Collapse toggle button on Desktop -->
        <button class="collapse-btn" @click="isCollapsed = !isCollapsed">
          <svg v-if="isCollapsed" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" d="M11.25 4.5l7.5 7.5-7.5 7.5m-6-15l7.5 7.5-7.5 7.5" />
          </svg>
          <svg v-else xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" d="M18.75 19.5l-7.5-7.5 7.5-7.5m-6 15L5.25 12l7.5-7.5" />
          </svg>
        </button>
      </div>
    </aside>

    <!-- Main Content Area -->
    <main class="app-main-content">
      <slot />
    </main>

    <!-- Toast Notifications Renderer -->
    <div class="toast-container">
      <div v-for="toast in activeToasts" :key="toast.id" class="toast-msg" :class="`toast-${toast.type || 'success'}`">
        <svg v-if="toast.type === 'error'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" style="width: 18px; height: 18px; color: var(--danger);">
          <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z" clip-rule="evenodd" />
        </svg>
        <svg v-else-if="toast.type === 'warning'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" style="width: 18px; height: 18px; color: var(--warning, #f59e0b);">
          <path fill-rule="evenodd" d="M8.485 2.495c.673-1.167 2.357-1.167 3.03 0l6.28 10.875c.673 1.167-.17 2.625-1.516 2.625H3.72c-1.347 0-2.189-1.458-1.515-2.625L8.485 2.495zM10 5a.75.75 0 01.75.75v3.5a.75.75 0 01-1.5 0v-3.5A.75.75 0 0110 5zm0 9a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd" />
        </svg>
        <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" style="width: 18px; height: 18px; color: var(--success);">
          <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.857-9.809a.75.75 0 00-1.214-.882l-3.483 4.79-1.88-1.88a.75.75 0 10-1.06 1.061l2.5 2.5a.75.75 0 001.137-.089l4-5.5z" clip-rule="evenodd" />
        </svg>
        <span>{{ toast.text }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useTodoStore } from '@/stores/todo'

const router = useRouter()
const authStore = useAuthStore()
const todoStore = useTodoStore()

const isSidebarOpen = ref(false)
const isCollapsed = ref(false)
const isDropdownOpen = ref(false)

const todayDateStr = computed(() => {
  return new Date().toISOString().split('T')[0]
})

// Current user details
const nickname = computed(() => authStore.currentUser?.nickname || authStore.currentUser?.username || '未登录')
const email = computed(() => authStore.currentUser?.email || '')
const userInitial = computed(() => {
  const name = nickname.value
  return name ? name.charAt(0).toUpperCase() : 'U'
})

// Load categories dynamically
const categories = computed(() => todoStore.sortedCategories)

const todayPendingCount = computed(() => todoStore.todayPendingCount)
const getCategoryPendingCount = (catId: number) => todoStore.getCategoryPendingCount(catId)

// Close dropdown on click outside
const closeDropdown = () => {
  isDropdownOpen.value = false
}

onMounted(() => {
  window.addEventListener('click', closeDropdown)
  if (authStore.currentUser) {
    todoStore.refreshCategories(authStore.currentUser.userId)
    todoStore.refreshTodayTodos(authStore.currentUser.userId)
    todoStore.refreshPendingCounts(authStore.currentUser.userId)
  }
})

onUnmounted(() => {
  window.removeEventListener('click', closeDropdown)
})

// Simple Toast Manager inside the layout
interface ToastItem {
  id: number
  text: string
  type?: 'success' | 'error' | 'warning' | 'info'
}
const activeToasts = ref<ToastItem[]>([])
let toastId = 0

// Listen for a custom global toast event
const handleToastEvent = (e: Event) => {
  const customEvent = e as CustomEvent<{ text: string; type?: 'success' | 'error' | 'warning' | 'info' }>
  const { text, type = 'success' } = customEvent.detail
  const id = toastId++
  activeToasts.value.push({ id, text, type })
  setTimeout(() => {
    activeToasts.value = activeToasts.value.filter(t => t.id !== id)
  }, 3000)
}

onMounted(() => {
  window.addEventListener('app-toast', handleToastEvent)
})
onUnmounted(() => {
  window.removeEventListener('app-toast', handleToastEvent)
})

const handleLogout = async () => {
  await authStore.logout()
  router.push('/')
}
</script>

<style scoped>
/* Mobile Header */
.mobile-header {
  display: none;
  height: 60px;
  background-color: var(--bg-sidebar);
  color: #fff;
  padding: 0 16px;
  align-items: center;
  justify-content: space-between;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  box-shadow: var(--shadow-sm);
}
.menu-toggle {
  background: none;
  border: none;
  color: #fff;
  cursor: pointer;
}
.menu-toggle svg {
  width: 24px;
  height: 24px;
}
.mobile-title {
  font-weight: 700;
  font-size: 18px;
  letter-spacing: 0.05em;
  background: linear-gradient(135deg, #a5b4fc, #818cf8);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

/* Sidebar Overlay */
.sidebar-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  z-index: 1010;
}

/* Sidebar Container */
.app-sidebar {
  display: flex;
  flex-direction: column;
  width: 260px;
  background-color: var(--bg-sidebar);
  color: var(--sidebar-text);
  border-right: 1px solid rgba(255, 255, 255, 0.05);
  height: 100vh;
  position: sticky;
  top: 0;
  transition: width var(--transition-normal);
  z-index: 1020;
}

/* Sidebar Brand */
.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 72px;
  padding: 0 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  overflow: hidden;
}
.brand-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, var(--primary), #818cf8);
  border-radius: var(--radius-md);
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}
.logo-svg {
  width: 18px;
  height: 18px;
}
.brand-name {
  font-weight: 700;
  font-size: 16px;
  color: #fff;
  letter-spacing: 0.05em;
  white-space: nowrap;
}

/* Sidebar Navigation */
.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  padding: 24px 16px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.nav-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-group-title {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: rgba(255, 255, 255, 0.3);
  padding: 0 12px 8px;
}

.header-actionable {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-action-btn {
  color: rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  transition: color var(--transition-fast);
}
.header-action-btn:hover {
  color: #fff;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 40px;
  padding: 0 12px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  transition: all var(--transition-fast);
  white-space: nowrap;
  position: relative;
}
.nav-item:hover {
  background-color: rgba(255, 255, 255, 0.05);
  color: #fff;
}
.nav-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}
.nav-text {
  flex: 1;
}
.nav-badge {
  background: var(--primary);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: var(--radius-full);
}
.nav-item-active {
  background: var(--primary) !important;
  color: #fff !important;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.2);
}

/* Category item */
.category-item {
  padding-left: 16px;
}
.color-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.nav-empty {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.2);
  padding: 8px 16px;
  font-style: italic;
}

/* Sidebar Footer User Card */
.sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.user-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border-radius: var(--radius-md);
  cursor: pointer;
  flex: 1;
  min-width: 0;
  transition: background-color var(--transition-fast);
}
.user-card:hover {
  background-color: rgba(255, 255, 255, 0.05);
}
.user-avatar {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #10b981, #059669);
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.2);
}
.user-info {
  flex: 1;
  min-width: 0;
}
.user-name {
  color: #fff;
  font-weight: 600;
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.user-email {
  color: rgba(255, 255, 255, 0.4);
  font-size: 11px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.chevron {
  color: rgba(255, 255, 255, 0.3);
  flex-shrink: 0;
}

/* Popover Dropdown */
.user-dropdown {
  position: absolute;
  bottom: 70px;
  left: 16px;
  right: 16px;
  background-color: #1e293b;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  padding: 6px;
  z-index: 1030;
}
.dropdown-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  font-size: 13px;
  color: #e2e8f0;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}
.dropdown-item:hover {
  background-color: rgba(255, 255, 255, 0.05);
  color: #fff;
}
.dropdown-item.danger {
  color: #f87171;
}
.dropdown-item.danger:hover {
  background-color: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}
.dropdown-divider {
  height: 1px;
  background-color: rgba(255, 255, 255, 0.08);
  margin: 6px 0;
}

/* Collapse Toggle Button */
.collapse-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.2);
  cursor: pointer;
  padding: 8px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}
.collapse-btn:hover {
  background-color: rgba(255, 255, 255, 0.05);
  color: #fff;
}
.collapse-btn svg {
  width: 16px;
  height: 16px;
}

/* Sidebar Collapsed State overrides */
.sidebar-collapsed {
  width: 72px;
}
.sidebar-collapsed .brand-name,
.sidebar-collapsed .nav-text,
.sidebar-collapsed .nav-badge,
.sidebar-collapsed .nav-group-title,
.sidebar-collapsed .user-info,
.sidebar-collapsed .chevron,
.sidebar-collapsed .categories-list {
  display: none !important;
}
.sidebar-collapsed .sidebar-brand {
  padding: 0;
  justify-content: center;
}
.sidebar-collapsed .nav-item {
  justify-content: center;
  padding: 0;
}
.sidebar-collapsed .sidebar-footer {
  flex-direction: column;
  padding: 16px 8px;
  gap: 12px;
}
.sidebar-collapsed .user-card {
  padding: 0;
  justify-content: center;
}
.sidebar-collapsed .user-avatar {
  margin: 0;
}
.sidebar-collapsed .user-dropdown {
  left: 80px;
  bottom: 16px;
  width: 160px;
}

/* Main Area */
.app-main-content {
  flex: 1;
  height: 100vh;
  overflow-y: auto;
  padding: 32px 40px;
  position: relative;
  min-width: 0;
}

/* Responsive adjustments */
@media (max-width: 1024px) {
  .app-main-content {
    padding: 24px;
  }
}

@media (max-width: 768px) {
  .mobile-header {
    display: flex;
  }
  .app-sidebar {
    position: fixed;
    top: 60px;
    bottom: 0;
    left: -260px;
    height: calc(100vh - 60px);
    transition: left var(--transition-normal);
  }
  .sidebar-open {
    left: 0;
  }
  .app-main-content {
    padding: 24px 16px;
    margin-top: 60px;
    height: calc(100vh - 60px);
  }
  .sidebar-collapsed {
    width: 260px;
  }
  .sidebar-collapsed .brand-name,
  .sidebar-collapsed .nav-text,
  .sidebar-collapsed .nav-badge,
  .sidebar-collapsed .nav-group-title,
  .sidebar-collapsed .user-info,
  .sidebar-collapsed .chevron,
  .sidebar-collapsed .categories-list {
    display: flex !important;
  }
  .sidebar-collapsed .sidebar-brand {
    padding: 0 24px;
    justify-content: flex-start;
  }
  .sidebar-collapsed .nav-item {
    justify-content: flex-start;
    padding: 0 12px;
  }
  .sidebar-collapsed .sidebar-footer {
    flex-direction: row;
    padding: 16px;
  }
  .sidebar-collapsed .user-card {
    padding: 8px;
    justify-content: flex-start;
  }
  .sidebar-collapsed .user-avatar {
    margin-right: 0;
  }
  .sidebar-collapsed .collapse-btn {
    display: none;
  }
}
</style>
