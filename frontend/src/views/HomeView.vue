<template>
  <div class="landing-page">

    <!-- Hero Section -->
    <section class="hero-section">
      <div class="hero-content">
        <span class="hero-badge">⚡️ 个人专属、轻量高效、可回顾、可分类</span>
        <h1 class="hero-title">
          专注今日，<br />
          <span class="gradient-text">事事有归处</span>
        </h1>
        <p class="hero-subtitle">
          Recall 待办系统将您的「月度规划」与「每日执行」无缝连接。未完成的今日待办次日自动延续，支持大分类与子分类的双层体系，帮您清空大脑，高效专注。
        </p>
        <div class="hero-actions">
          <router-link to="/register" class="hero-btn primary-btn-lg">免费注册使用</router-link>
          <router-link to="/login" class="hero-btn secondary-btn-lg">已有账号登录</router-link>
        </div>
      </div>

      <!-- High-fidelity Mock UI Mockup -->
      <div class="hero-mockup">
        <div class="mockup-header">
          <div class="mockup-dots">
            <span class="dot-red"></span>
            <span class="dot-yellow"></span>
            <span class="dot-green"></span>
          </div>
          <span class="mockup-title">Recall 今日待办 - 工作台</span>
        </div>
        <div class="mockup-body">
          <div class="mockup-sidebar">
            <div class="mockup-menu-item active">今日待办 <span class="mockup-badge">3</span></div>
            <div class="mockup-menu-item">月度规划</div>
            <div class="mockup-menu-item">分类管理</div>
            <div class="mockup-menu-item">回顾统计</div>
          </div>
          <div class="mockup-content">
            <div class="mockup-todo-item">
              <span class="mockup-checkbox checked"></span>
              <span class="mockup-todo-text line-through">参加下午 2 点的敏捷站会</span>
              <span class="mockup-tag">日常会议</span>
            </div>
            <div class="mockup-todo-item">
              <span class="mockup-checkbox"></span>
              <span class="mockup-todo-text font-bold">完成低代码服务能力建设的设计文档</span>
              <span class="mockup-tag high">高优先级</span>
            </div>
            <div class="mockup-todo-item">
              <span class="mockup-checkbox"></span>
              <span class="mockup-todo-text">阅读 Vue3 性能优化指南并做笔记</span>
              <span class="mockup-tag teal">读书笔记</span>
            </div>
            <div class="mockup-todo-item rollover-highlight">
              <span class="mockup-checkbox"></span>
              <span class="mockup-todo-text">回复产品经理关于月度绩效的反馈</span>
              <span class="mockup-tag rollover">♻️ 延续 1 次</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Key Features Grid -->
    <section class="features-section">
      <div class="section-header">
        <h2 class="section-title">为什么选择 Recall ？</h2>
        <p class="section-subtitle">专注提升个人执行效率，告别拖延与遗忘</p>
      </div>

      <div class="features-grid">
        <!-- Feature 1 -->
        <div class="feature-card">
          <div class="feature-icon bg-indigo">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <h3>Today First · 今日聚焦</h3>
          <p>打开即看到今天该做什么，专注解决核心矛盾。今日完成率条实时体现执行成果，促使高效闭环。</p>
        </div>

        <!-- Feature 2 -->
        <div class="feature-card">
          <div class="feature-icon bg-green">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99" />
            </svg>
          </div>
          <h3>Never Lost · 自动延续</h3>
          <p>今日未完成的 pending 事项，次日凌晨或再次打开时自动调整计划日期为「今天」，记录延续次数，绝不遗漏。</p>
        </div>

        <!-- Feature 3 -->
        <div class="feature-card">
          <div class="feature-icon bg-cyan">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6A2.25 2.25 0 016 3.75h2.25A2.25 2.25 0 0110.5 6v2.25a2.25 2.25 0 01-2.25 2.25H6a2.25 2.25 0 01-2.25-2.25V6zM3.75 15.75A2.25 2.25 0 016 13.5h2.25a2.25 2.25 0 012.25 2.25V18a2.25 2.25 0 01-2.25 2.25H6A2.25 2.25 0 013.75 18v-2.25zM13.5 6a2.25 2.25 0 012.25-2.25H18A2.25 2.25 0 0120.25 6v2.25A2.25 2.25 0 0118 10.5h-2.25a2.25 2.25 0 01-2.25-2.25V6zM13.5 15.75a2.25 2.25 0 012.25-2.25H18a2.25 2.25 0 012.25 2.25V18A2.25 2.25 0 0118 20.25h-2.25A2.25 2.25 0 0113.5 18v-2.25z" />
            </svg>
          </div>
          <h3>Two-Level Categories · 两层分类</h3>
          <p>大分类配合二级子分类，公司事务、个人生活、学习成长分门别类，主从布局管理，拖拽排序更随心。</p>
        </div>

        <!-- Feature 4 -->
        <div class="feature-card">
          <div class="feature-icon bg-pink">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" d="M15.59 14.37a6 6 0 01-5.84 7.38v-4.8m5.84-2.58a14.98 14.98 0 006.16-12.12A14.98 14.98 0 009.64 8.38m9.79 3.41A14.97 14.97 0 0116.5 16.5M9.64 8.38a14.96 14.96 0 01-6.16 12.12A14.96 14.96 0 019.64 8.38zm0 0c1.33 0 2.5-.35 3.5-.96" />
            </svg>
          </div>
          <h3>Plan & Execute · 规划执行闭环</h3>
          <p>v1.1 新增月度规划模块：个人绩效 KR 考核与团队冲刺，一键派生为日常待办，建立宏观规划与微观执行闭环。</p>
        </div>
      </div>
    </section>

    <!-- Usage Steps -->
    <section class="steps-section">
      <div class="section-header">
        <h2 class="section-title">三步开启高效工作流</h2>
        <p class="section-subtitle">极速上手，仅需 30 秒</p>
      </div>

      <div class="steps-container">
        <div class="step-card">
          <div class="step-num">01</div>
          <h4>极速注册登录</h4>
          <p>创建专属的私有账号，您的所有待办、分类与月度规划数据严格隔离保护。</p>
        </div>
        <div class="step-card">
          <div class="step-num">02</div>
          <h4>梳理本月规划</h4>
          <p>在月度规划板中录入您的月度关键成果（KR）和本月团队冲刺任务，心中有大局。</p>
        </div>
        <div class="step-card">
          <div class="step-num">03</div>
          <h4>派生执行今日</h4>
          <p>一键派生大颗粒任务到今日执行，日常快速新建待办，次日未完成智能自动延续。</p>
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer class="landing-footer">
      <p>&copy; 2026 Recall Platform. All rights reserved.</p>
      <p class="footer-version">当前系统版本: v1.1 (月度规划闭环版)</p>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

onMounted(() => {
  // PRD 6.1: If already logged in, redirect to today view
  if (authStore.isLoggedIn) {
    router.replace('/today')
  }
})
</script>

<style scoped>
.landing-page {
  overflow-x: hidden;
}

/* Navbar */
.landing-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 1200px;
  margin: 0 auto 60px;
  padding: 10px 0;
}
.nav-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 700;
  font-size: 18px;
  color: #fff;
}
.logo-box {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, var(--primary), #818cf8);
  border-radius: var(--radius-md);
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
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}
.text-btn {
  color: #9ca3af;
  padding: 8px 16px;
}
.text-btn:hover {
  color: #fff;
}
.primary-btn {
  background-color: var(--primary);
  color: #fff;
  padding: 10px 20px;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}
.primary-btn:hover {
  background-color: var(--primary-hover);
  transform: translateY(-1px);
}

/* Hero Section */
.hero-section {
  max-width: 1200px;
  margin: 0 auto 120px;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  align-items: center;
  gap: 60px;
}

@media (max-width: 1024px) {
  .hero-section {
    grid-template-columns: 1fr;
    gap: 48px;
    text-align: center;
  }
}

.hero-content {
  display: flex;
  flex-direction: column;
}
.hero-badge {
  align-self: flex-start;
  background-color: rgba(99, 102, 241, 0.1);
  color: #a5b4fc;
  padding: 6px 12px;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 24px;
  border: 1px solid rgba(99, 102, 241, 0.2);
}
@media (max-width: 1024px) {
  .hero-badge {
    align-self: center;
  }
}
.hero-title {
  font-size: 56px;
  font-weight: 800;
  line-height: 1.15;
  letter-spacing: -0.02em;
  margin-bottom: 24px;
  color: #fff;
}
@media (max-width: 768px) {
  .hero-title {
    font-size: 40px;
  }
}
.gradient-text {
  background: linear-gradient(135deg, #a5b4fc, #6366f1 50%, #ec4899);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.hero-subtitle {
  font-size: 18px;
  color: #9ca3af;
  line-height: 1.6;
  margin-bottom: 40px;
}
.hero-actions {
  display: flex;
  gap: 16px;
}
@media (max-width: 1024px) {
  .hero-actions {
    justify-content: center;
  }
}
.primary-btn-lg {
  background-color: var(--primary);
  color: #fff;
  padding: 14px 28px;
  border-radius: var(--radius-md);
  font-weight: 600;
  font-size: 16px;
  box-shadow: 0 4px 14px rgba(79, 70, 229, 0.4);
  transition: all var(--transition-fast);
}
.primary-btn-lg:hover {
  background-color: var(--primary-hover);
  transform: translateY(-2px);
}
.secondary-btn-lg {
  background-color: rgba(255, 255, 255, 0.05);
  color: #f3f4f6;
  padding: 14px 28px;
  border-radius: var(--radius-md);
  font-weight: 600;
  font-size: 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all var(--transition-fast);
}
.secondary-btn-lg:hover {
  background-color: rgba(255, 255, 255, 0.1);
  transform: translateY(-2px);
}

/* Hero Mockup Panel */
.hero-mockup {
  background-color: #1e293b;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  height: 320px;
  text-align: left;
}
.mockup-header {
  background-color: #0f172a;
  height: 40px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 16px;
}
.mockup-dots {
  display: flex;
  gap: 6px;
}
.mockup-dots span {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}
.dot-red { background-color: #ef4444; }
.dot-yellow { background-color: #f59e0b; }
.dot-green { background-color: #10b981; }
.mockup-title {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}
.mockup-body {
  display: flex;
  flex: 1;
}
.mockup-sidebar {
  width: 120px;
  background-color: #0f172a;
  padding: 16px 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  border-right: 1px solid rgba(255, 255, 255, 0.04);
}
.mockup-menu-item {
  font-size: 11px;
  color: #64748b;
  padding: 6px 10px;
  border-radius: var(--radius-sm);
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.mockup-menu-item.active {
  background-color: rgba(99, 102, 241, 0.1);
  color: #a5b4fc;
  font-weight: 600;
}
.mockup-badge {
  background-color: var(--primary);
  color: #fff;
  padding: 1px 4px;
  border-radius: 4px;
  font-size: 8px;
}
.mockup-content {
  flex: 1;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background-color: #1e293b;
}
.mockup-todo-item {
  background-color: #0f172a;
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: var(--radius-md);
  padding: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
}
.mockup-checkbox {
  width: 16px;
  height: 16px;
  border: 1.5px solid #475569;
  border-radius: 4px;
  flex-shrink: 0;
  display: inline-block;
}
.mockup-checkbox.checked {
  background-color: var(--success);
  border-color: var(--success);
  position: relative;
}
.mockup-checkbox.checked::after {
  content: "";
  position: absolute;
  left: 5px;
  top: 2px;
  width: 3px;
  height: 6px;
  border: solid white;
  border-width: 0 1.5px 1.5px 0;
  transform: rotate(45deg);
}
.mockup-todo-text {
  flex: 1;
  color: #e2e8f0;
}
.line-through {
  text-decoration: line-through;
  color: #475569;
}
.font-bold {
  font-weight: 600;
}
.mockup-tag {
  font-size: 9px;
  padding: 2px 6px;
  border-radius: var(--radius-full);
  background-color: #334155;
  color: #94a3b8;
}
.mockup-tag.high {
  background-color: rgba(239, 68, 68, 0.15);
  color: #f87171;
}
.mockup-tag.teal {
  background-color: rgba(20, 184, 166, 0.15);
  color: #2dd4bf;
}
.rollover-highlight {
  border-color: rgba(245, 158, 11, 0.2) !important;
  background-color: rgba(245, 158, 11, 0.05) !important;
}
.mockup-tag.rollover {
  background-color: rgba(245, 158, 11, 0.15);
  color: #fbbf24;
}

/* Features Grid */
.features-section {
  max-width: 1200px;
  margin: 0 auto 120px;
}
.section-header {
  text-align: center;
  margin-bottom: 60px;
}
.section-title {
  font-size: 32px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 12px;
}
.section-subtitle {
  font-size: 16px;
  color: #9ca3af;
}
.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 24px;
}
.feature-card {
  background-color: #111827;
  border: 1px solid #1f2937;
  border-radius: var(--radius-lg);
  padding: 32px;
  transition: transform var(--transition-normal), border-color var(--transition-normal);
}
.feature-card:hover {
  transform: translateY(-4px);
  border-color: rgba(99, 102, 241, 0.4);
}
.feature-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
}
.feature-icon svg {
  width: 24px;
  height: 24px;
  color: #fff;
}
.bg-indigo { background-color: var(--primary); }
.bg-green { background-color: var(--success); }
.bg-cyan { background-color: #0891b2; }
.bg-pink { background-color: #db2777; }

.feature-card h3 {
  font-size: 18px;
  color: #fff;
  margin-bottom: 12px;
  font-weight: 600;
}
.feature-card p {
  color: #9ca3af;
  font-size: 14px;
  line-height: 1.6;
}

/* Steps Section */
.steps-section {
  max-width: 1200px;
  margin: 0 auto 100px;
}
.steps-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 32px;
  margin-top: 48px;
}
.step-card {
  position: relative;
  background-color: #111827;
  border: 1px solid #1f2937;
  border-radius: var(--radius-lg);
  padding: 40px 32px 32px;
}
.step-num {
  position: absolute;
  top: -24px;
  left: 32px;
  font-size: 36px;
  font-weight: 800;
  color: #4f46e5;
  background-color: #0b0f19;
  padding: 0 10px;
}
.step-card h4 {
  font-size: 18px;
  color: #fff;
  margin-bottom: 12px;
}
.step-card p {
  color: #9ca3af;
  font-size: 14px;
  line-height: 1.6;
}

/* Footer */
.landing-footer {
  text-align: center;
  padding: 48px 0 24px;
  border-top: 1px solid #1f2937;
  color: #6b7280;
  font-size: 14px;
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.footer-version {
  font-size: 12px;
  color: #4b5563;
}
</style>
