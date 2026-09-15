<template>
  <div class="settings-view">
    <div class="view-header">
      <h1>系统设置</h1>
      <p class="subtitle">管理您的个人账户基本资料、安全密码及偏好设置</p>
    </div>

    <!-- Feedback alerts -->
    <div v-if="successMsg" class="alert alert-success">
      <span>{{ successMsg }}</span>
    </div>
    <div v-if="errorMsg" class="alert alert-error">
      <span>{{ errorMsg }}</span>
    </div>

    <div class="settings-grid">
      <!-- 1. Profile card -->
      <div class="settings-card premium-card">
        <h3>👤 个人资料修改</h3>
        <p class="card-sub">更新您的用户昵称及关联的邮箱地址</p>
        
        <form @submit.prevent="handleUpdateProfile" class="settings-form">
          <div class="form-field">
            <label for="nickname">个性昵称</label>
            <input 
              id="nickname" 
              v-model="profileForm.nickname" 
              type="text" 
              required 
              class="form-control"
              placeholder="请输入您的昵称"
            />
          </div>

          <div class="form-field">
            <label for="email">安全邮箱地址</label>
            <input 
              id="email" 
              v-model="profileForm.email" 
              type="email" 
              required 
              class="form-control"
              placeholder="如 user@example.com"
            />
          </div>

          <button type="submit" class="submit-btn" :disabled="profileLoading">
            {{ profileLoading ? '正在保存...' : '保存资料修改' }}
          </button>
        </form>
      </div>

      <!-- 2. Password card -->
      <div class="settings-card premium-card">
        <h3>🔒 账户安全密码修改</h3>
        <p class="card-sub">定期更换密码以保障账户数据隔离安全</p>
        
        <form @submit.prevent="handleUpdatePassword" class="settings-form">
          <div class="form-field">
            <label for="old-pwd">原密码</label>
            <input 
              id="old-pwd" 
              v-model="pwdForm.oldPassword" 
              type="password" 
              required 
              class="form-control"
              placeholder="请输入旧密码"
            />
          </div>

          <div class="form-field">
            <label for="new-pwd">新密码</label>
            <input 
              id="new-pwd" 
              v-model="pwdForm.newPassword" 
              type="password" 
              required 
              class="form-control"
              placeholder="请输入新密码（8位以上字母及数字）"
            />
          </div>

          <div class="form-field">
            <label for="confirm-pwd">确认新密码</label>
            <input 
              id="confirm-pwd" 
              v-model="pwdForm.confirmPassword" 
              type="password" 
              required 
              class="form-control"
              placeholder="请再次输入新密码"
            />
          </div>

          <button type="submit" class="submit-btn danger" :disabled="pwdLoading">
            {{ pwdLoading ? '正在修改...' : '执行密码修改' }}
          </button>
        </form>
      </div>

      <!-- 3. OA 对接配置 card -->
      <div class="settings-card premium-card full-width">
        <h3>🔗 OA 对接配置</h3>
        <p class="card-sub">用于把个人日报同步到公司 OA「工时日填报」（只暂存待办，最终提交请在 OA 端人工完成）</p>

        <form @submit.prevent="handleSaveOaConfig" class="settings-form">
          <div class="oa-field-row">
            <div class="form-field oa-grow">
              <label for="oa-url">OA 地址</label>
              <input
                id="oa-url"
                v-model="oaForm.oaBaseUrl"
                type="text"
                required
                class="form-control"
                placeholder="https://erp.zoesoft.com.cn/seeyon"
              />
            </div>
            <div class="form-field oa-grow">
              <label for="oa-username">OA 账号</label>
              <input
                id="oa-username"
                v-model="oaForm.username"
                type="text"
                required
                class="form-control"
                placeholder="OA 登录账号"
              />
            </div>
            <div class="form-field oa-grow">
              <label for="oa-password">OA 密码</label>
              <input
                id="oa-password"
                v-model="oaForm.password"
                type="password"
                class="form-control"
                :placeholder="oaHasPassword ? '已配置，留空表示不修改' : '首次配置必填'"
              />
            </div>
          </div>

          <div class="oa-field-row">
            <div class="form-field oa-grow">
              <label for="oa-dev-project">研发项目名</label>
              <input
                id="oa-dev-project"
                v-model="oaForm.devProjectName"
                type="text"
                required
                class="form-control"
                placeholder="如：中台底座（三期）"
              />
            </div>
            <div class="form-field oa-grow">
              <label for="oa-leave-project">请假项目名</label>
              <input
                id="oa-leave-project"
                v-model="oaForm.leaveProjectName"
                type="text"
                required
                class="form-control"
                placeholder="如：请假-产研中心中台事业群中台架构研发部"
              />
            </div>
          </div>

          <div class="pref-item">
            <div class="pref-text">
              <h4>保存日报后自动同步</h4>
              <p>开启后，日报/请假保存成功会自动把该周内容同步到 OA（手动「同步OA」按钮始终可用）</p>
            </div>
            <label class="toggle-control">
              <input v-model="oaForm.autoSync" type="checkbox" />
              <span class="toggle-slider"></span>
            </label>
          </div>

          <button type="submit" class="submit-btn" :disabled="oaLoading">
            {{ oaLoading ? '正在保存...' : '保存 OA 配置' }}
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useTodoStore } from '@/stores/todo'
import { getOaConfig, saveOaConfig, type OaConfigSaveReq } from '@/api/oa'

const authStore = useAuthStore()
const todoStore = useTodoStore()

const categories = computed(() => todoStore.sortedCategories)

const successMsg = ref('')
const errorMsg = ref('')

const profileLoading = ref(false)
const pwdLoading = ref(false)
const oaLoading = ref(false)
const oaHasPassword = ref(false)

const profileForm = reactive({
  nickname: '',
  email: ''
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const oaForm = reactive<OaConfigSaveReq>({
  oaBaseUrl: 'https://erp.zoesoft.com.cn/seeyon',
  username: '',
  password: '',
  devProjectName: '中台底座（三期）',
  leaveProjectName: '请假-产研中心中台事业群中台架构研发部',
  autoSync: false
})

onMounted(() => {
  if (authStore.currentUser) {
    profileForm.nickname = authStore.currentUser.nickname || authStore.currentUser.username
    profileForm.email = authStore.currentUser.email || ''
    // Load categories for selector
    todoStore.refreshCategories(authStore.currentUser.userId)
  }
  loadOaConfig()
})

const loadOaConfig = async () => {
  try {
    const res = await getOaConfig()
    if (res.data) {
      oaForm.oaBaseUrl = res.data.oaBaseUrl
      oaForm.username = res.data.username
      oaForm.devProjectName = res.data.devProjectName
      oaForm.leaveProjectName = res.data.leaveProjectName
      oaForm.autoSync = res.data.autoSync
      oaHasPassword.value = res.data.hasPassword
    }
  } catch {
    // 未配置时不提示，表单保持默认值
  }
}

const handleSaveOaConfig = async () => {
  oaLoading.value = true
  try {
    await saveOaConfig({
      ...oaForm,
      // 留空表示不修改密码
      password: oaForm.password || undefined
    })
    triggerAlert('success', '🔗 OA 对接配置保存成功！')
    oaForm.password = ''
    await loadOaConfig()
  } catch (err: any) {
    triggerAlert('error', err.message || 'OA 配置保存失败')
  } finally {
    oaLoading.value = false
  }
}

const triggerAlert = (type: 'success' | 'error', msg: string) => {
  if (type === 'success') {
    successMsg.value = msg
    errorMsg.value = ''
  } else {
    errorMsg.value = msg
    successMsg.value = ''
  }
  
  // Clear after 4 seconds
  setTimeout(() => {
    successMsg.value = ''
    errorMsg.value = ''
  }, 4000)
}

const handleUpdateProfile = async () => {
  profileLoading.value = true
  try {
    await authStore.updateProfile(profileForm.nickname.trim(), profileForm.email.trim())
    triggerAlert('success', '👤 个人资料修改保存成功！')
  } catch (err: any) {
    triggerAlert('error', err.message || '资料更新失败')
  } finally {
    profileLoading.value = false
  }
}

const handleUpdatePassword = async () => {
  if (pwdForm.newPassword.length < 8) {
    triggerAlert('error', '密码长度必须至少为 8 位')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    triggerAlert('error', '两次输入的新密码不匹配，请重新确认')
    return
  }

  pwdLoading.value = true
  try {
    await authStore.updatePassword(pwdForm.oldPassword, pwdForm.newPassword)
    triggerAlert('success', '🔑 安全密码修改完成！请牢记您的新密码。')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } catch (err: any) {
    triggerAlert('error', err.message || '原密码校验错误')
  } finally {
    pwdLoading.value = false
  }
}


</script>

<style scoped>
.settings-view {
  max-width: 1000px;
  margin: 0 auto;
}

.view-header {
  margin-bottom: 32px;
}
.view-header h1 {
  font-size: 28px;
  font-weight: 800;
  color: var(--text-main);
  margin-bottom: 4px;
}
.subtitle {
  font-size: 14px;
  color: var(--text-muted);
  font-weight: 500;
}

/* Feedback alerts */
.alert {
  padding: 12px 20px;
  border-radius: var(--radius-md);
  margin-bottom: 24px;
  font-size: 13.5px;
  font-weight: 600;
  display: flex;
  align-items: center;
}
.alert-success {
  background-color: var(--success-bg);
  color: var(--success);
  border: 1px solid rgba(16, 185, 129, 0.2);
}
.alert-error {
  background-color: var(--danger-bg);
  color: var(--danger);
  border: 1px solid rgba(239, 68, 68, 0.2);
}

.settings-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}
@media (max-width: 768px) {
  .settings-grid {
    grid-template-columns: 1fr;
  }
}

.settings-card {
  padding: 24px;
  display: flex;
  flex-direction: column;
}
.settings-card.full-width {
  grid-column: 1 / -1;
}

.settings-card h3 {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-main);
  margin-bottom: 4px;
}
.card-sub {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 20px;
}

.settings-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.form-field label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-main);
}
.form-control {
  border: 1.5px solid var(--border-medium);
  border-radius: var(--radius-md);
  padding: 10px 14px;
  outline: none;
  font-size: 14px;
  background-color: #fff;
  transition: all var(--transition-fast);
}
.form-control:focus {
  border-color: var(--primary);
}

.submit-btn {
  height: 40px;
  background-color: var(--primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
  margin-top: 8px;
  box-shadow: 0 4px 10px rgba(79, 70, 229, 0.15);
}
.submit-btn:hover {
  background-color: var(--primary-hover);
  transform: translateY(-1px);
}
.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
.submit-btn.danger {
  background-color: var(--danger);
  box-shadow: 0 4px 10px rgba(239, 68, 68, 0.15);
}
.submit-btn.danger:hover {
  background-color: #dc2626;
}

/* Preference items list rules */
.oa-field-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}
.oa-field-row .oa-grow {
  flex: 1;
  min-width: 220px;
}
.preference-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.pref-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  border-bottom: 1px solid var(--border-light);
  padding-bottom: 16px;
}
.pref-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}
.pref-text h4 {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-main);
  margin-bottom: 4px;
}
.pref-text p {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.4;
}

/* Toggle Switch Slider */
.toggle-control {
  position: relative;
  display: inline-block;
  width: 44px;
  height: 24px;
  cursor: pointer;
  flex-shrink: 0;
}
.toggle-control input {
  opacity: 0;
  width: 0;
  height: 0;
}
.toggle-slider {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background-color: var(--border-medium);
  border-radius: var(--radius-full);
  transition: background-color var(--transition-fast);
}
.toggle-slider::after {
  content: "";
  position: absolute;
  height: 18px;
  width: 18px;
  left: 3px;
  bottom: 3px;
  background-color: #fff;
  border-radius: 50%;
  transition: transform var(--transition-fast);
  box-shadow: var(--shadow-sm);
}
.toggle-control input:checked + .toggle-slider {
  background-color: var(--primary);
}
.toggle-control input:checked + .toggle-slider::after {
  transform: translateX(20px);
}

.pref-selector-wrap {
  flex-shrink: 0;
  width: 180px;
}
.pref-select-field {
  width: 100%;
  height: 38px;
  padding: 0 10px;
}

.pref-save-bar {
  margin-top: 24px;
  border-top: 1.5px solid var(--border-light);
  padding-top: 20px;
  display: flex;
  justify-content: flex-end;
}
.pref-save-btn {
  width: 200px;
}
</style>
