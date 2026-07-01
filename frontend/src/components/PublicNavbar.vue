<template>
  <header class="landing-nav">
    <router-link to="/" class="nav-logo">
      <div class="logo-box">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12c0 1.268-.63 2.39-1.593 3.068a3.745 3.745 0 01-1.043 3.296 3.745 3.745 0 01-3.296 1.043A3.745 3.745 0 0110 21c-1.268 0-2.39-.63-3.068-1.593a3.746 3.746 0 01-3.296-1.043 3.745 3.745 0 01-1.043-3.296A3.745 3.745 0 013 12c0-1.268.63-2.39 1.593-3.068a3.745 3.745 0 011.043-3.296 3.746 3.746 0 013.296-1.043A3.746 3.746 0 0114 3c1.268 0 2.39.63 3.068 1.593a3.746 3.746 0 013.296 1.043 3.746 3.746 0 011.043 3.296A3.745 3.745 0 0121 12z" />
        </svg>
      </div>
      <span>Recall 待办系统</span>
    </router-link>
    <div class="nav-actions">
      <router-link to="/" class="nav-btn-link text-btn" v-if="$route.path !== '/'">首页</router-link>
      <router-link to="/login" class="nav-btn-link primary-btn" v-if="$route.path === '/' || $route.path === '/register'">登录</router-link>
    </div>
  </header>
</template>

<script setup lang="ts">
// No extra logic needed, purely navigation
</script>

<style scoped>
.landing-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto 40px;
  padding: 20px 0;
  box-sizing: border-box;
}

.nav-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 700;
  font-size: 20px;
  color: #fff;
  text-decoration: none;
  transition: opacity var(--transition-fast, 0.2s);
}

.nav-logo:hover {
  opacity: 0.9;
}

.logo-box {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, var(--primary, #4f46e5), #818cf8);
  border-radius: var(--radius-md, 8px);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.4);
}

.logo-box svg {
  width: 20px;
  height: 20px;
  color: #fff;
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.nav-btn-link {
  font-size: 14px;
  font-weight: 500;
  border-radius: var(--radius-md, 8px);
  transition: all var(--transition-fast, 0.2s);
  text-decoration: none;
}

.text-btn {
  color: #9ca3af;
  padding: 8px 16px;
}

.text-btn:hover {
  color: #fff;
}

.primary-btn {
  background-color: var(--primary, #4f46e5);
  color: #fff;
  padding: 10px 20px;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

.primary-btn:hover {
  background-color: var(--primary-hover, #4338ca);
  transform: translateY(-1px);
}

@media (max-width: 768px) {
  .landing-nav {
    flex-direction: column;
    gap: 16px;
    margin-bottom: 30px;
  }
}
</style>
