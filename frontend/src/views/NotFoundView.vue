<template>
  <div class="not-found-wrapper">
    <div class="not-found-card premium-card">
      <div class="icon">🔍</div>
      <h1>404</h1>
      <h2>页面不存在</h2>
      <p>抱歉，您访问的页面不存在或已被移除。</p>
      <router-link to="/today" class="back-btn">返回今日待办</router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.not-found-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 120px);
}
.not-found-card {
  max-width: 400px;
  width: 100%;
  padding: 40px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}
.icon {
  font-size: 48px;
}
h1 {
  font-size: 64px;
  font-weight: 800;
  line-height: 1;
  background: linear-gradient(135deg, var(--primary), #818cf8);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
h2 {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-main);
}
p {
  font-size: 14px;
  color: var(--text-muted);
  line-height: 1.5;
  margin-bottom: 8px;
}
.back-btn {
  background-color: var(--primary);
  color: #fff;
  border-radius: var(--radius-md);
  padding: 10px 24px;
  font-size: 14px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.2);
  transition: all var(--transition-fast);
}
.back-btn:hover {
  background-color: var(--primary-hover);
  transform: translateY(-1px);
}
</style>
