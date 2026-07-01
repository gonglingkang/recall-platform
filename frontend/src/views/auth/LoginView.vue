<template>
  <div class="auth-wrapper">
    <div class="auth-card">
      <div class="auth-header">
        <div class="auth-logo">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12c0 1.268-.63 2.39-1.593 3.068a3.745 3.745 0 01-1.043 3.296 3.745 3.745 0 01-3.296 1.043A3.745 3.745 0 0110 21c-1.268 0-2.39-.63-3.068-1.593a3.746 3.746 0 01-3.296-1.043 3.745 3.745 0 01-1.043-3.296A3.745 3.745 0 013 12c0-1.268.63-2.39 1.593-3.068a3.745 3.745 0 011.043-3.296 3.746 3.746 0 013.296-1.043A3.746 3.746 0 0114 3c1.268 0 2.39.63 3.068 1.593a3.746 3.746 0 013.296 1.043 3.746 3.746 0 011.043 3.296A3.745 3.745 0 0121 12z" />
          </svg>
        </div>
        <h2>登录到 Recall</h2>
        <p>专注今日，事事有归处</p>
      </div>

      <!-- Error Alert -->
      <div v-if="errorMsg" class="auth-error-alert">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" class="alert-icon">
          <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-8-5a.75.75 0 01.75.75v4.5a.75.75 0 01-1.5 0v-4.5A.75.75 0 0110 5zm0 10a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd" />
        </svg>
        <span>{{ errorMsg }}</span>
      </div>

      <!-- Login Form -->
      <form class="auth-form" @submit.prevent="handleSubmit">
        <div class="form-field">
          <label for="username">账号或邮箱</label>
          <div class="input-wrapper">
            <input 
              id="username" 
              v-model="form.username" 
              type="text" 
              placeholder="请输入用户名或邮箱" 
              required
              :disabled="loading"
            />
          </div>
        </div>

        <div class="form-field">
          <div class="label-row">
            <label for="password">密码</label>
          </div>
          <div class="input-wrapper">
            <input 
              id="password" 
              v-model="form.password" 
              type="password" 
              placeholder="请输入您的密码" 
              required
              :disabled="loading"
            />
          </div>
        </div>

        <!-- Submit Button -->
        <button type="submit" class="submit-btn" :disabled="loading">
          <span v-if="loading" class="spinner"></span>
          <span v-else>安全登录</span>
        </button>

      </form>

      <div class="auth-footer">
        <span>还没有账号？</span>
        <router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const loading = ref(false)
const errorMsg = ref('')

const form = reactive({
  username: '',
  password: ''
})

const handleSubmit = async () => {
  if (!form.username || !form.password) return
  
  loading.value = true
  errorMsg.value = ''
  
  try {
    await authStore.login(form.username, form.password)
    
    // Success - trigger a global toast
    const event = new CustomEvent('app-toast', { detail: { text: '登录成功！欢迎回来。' } })
    window.dispatchEvent(event)

    // Redirect to requested page or fallback to today
    const redirect = route.query.redirect as string
    if (redirect) {
      router.push(redirect)
    } else {
      router.push('/today')
    }
  } catch (err: any) {
    // PRD 6.2.2: security-friendly generic error
    errorMsg.value = err.message || '登录失败，请检查账号和密码'
  } finally {
    loading.value = false
  }
}


</script>

<style scoped>
.auth-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 160px);
}

.auth-card {
  width: 100%;
  max-width: 420px;
  background: rgba(30, 41, 59, 0.45);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: var(--radius-lg);
  backdrop-filter: blur(16px);
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.5), 0 10px 10px -5px rgba(0, 0, 0, 0.4);
  padding: 40px;
}

.auth-header {
  text-align: center;
  margin-bottom: 32px;
}

.auth-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, var(--primary), #818cf8);
  border-radius: var(--radius-md);
  color: #fff;
  margin-bottom: 16px;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}
.auth-logo svg {
  width: 24px;
  height: 24px;
}

.auth-header h2 {
  font-size: 22px;
  font-weight: 700;
  color: #ffffff;
  margin-bottom: 6px;
}

.auth-header p {
  font-size: 14px;
  color: #94a3b8;
}

.auth-error-alert {
  background-color: var(--danger-bg);
  border: 1px solid rgba(239, 68, 68, 0.2);
  color: var(--danger);
  padding: 12px 16px;
  border-radius: var(--radius-md);
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 500;
}
.alert-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-field label {
  font-size: 13px;
  font-weight: 600;
  color: #cbd5e1;
}

.label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.input-wrapper input {
  width: 100%;
  height: 42px;
  padding: 0 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: var(--radius-md);
  outline: none;
  font-size: 14px;
  transition: all var(--transition-fast);
  background-color: rgba(15, 23, 42, 0.6);
  color: #ffffff;
}
.input-wrapper input:focus {
  border-color: var(--primary);
  background-color: rgba(15, 23, 42, 0.8);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.15);
}

.input-wrapper input:-webkit-autofill,
.input-wrapper input:-webkit-autofill:hover, 
.input-wrapper input:-webkit-autofill:focus, 
.input-wrapper input:-webkit-autofill:active {
  -webkit-box-shadow: 0 0 0 1000px #0f172a inset !important;
  -webkit-text-fill-color: #ffffff !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  transition: background-color 5000s ease-in-out 0s;
}

.submit-btn {
  height: 44px;
  background-color: var(--primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  font-weight: 600;
  font-size: 15px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.2);
}
.submit-btn:hover {
  background-color: var(--primary-hover);
  transform: translateY(-1px);
}
.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}



.auth-footer {
  text-align: center;
  margin-top: 32px;
  font-size: 13px;
  color: var(--text-muted);
}
.auth-footer a {
  color: var(--primary);
  font-weight: 600;
}
.auth-footer a:hover {
  text-decoration: underline;
}

/* Spinner */
.spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: #fff;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
