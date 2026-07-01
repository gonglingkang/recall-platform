<template>
  <div class="todo-detail-view">
    <div v-if="todo" class="detail-card premium-card">
      <div class="card-header">
        <router-link to="/today" class="back-link">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="back-icon">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 19.5L8.25 12l7.5-7.5" />
          </svg>
          <span>返回今日待办</span>
        </router-link>
        
        <div class="header-badges">
          <span class="badge" :class="`badge-${todo.priority}`">
            {{ priorityLabel(todo.priority) }} 优先级
          </span>
          <span class="status-badge" :class="todo.status">
            {{ todo.status === 'done' ? '已完成' : '待执行' }}
          </span>
        </div>
      </div>

      <form @submit.prevent="handleSave" class="detail-form">
        <!-- Title Input -->
        <div class="form-field title-field">
          <label for="todo-title">待办事项标题</label>
          <input 
            id="todo-title" 
            v-model="form.title" 
            type="text" 
            required 
            class="form-control title-input" 
            placeholder="事项标题" 
          />
        </div>

        <!-- Content/Remark -->
        <div class="form-field">
          <label for="todo-content">详细备注描述</label>
          <textarea 
            id="todo-content" 
            v-model="form.content" 
            rows="6" 
            class="form-control content-input" 
            placeholder="在此添加详细的任务描述或补充备注信息..."
          ></textarea>
        </div>

        <!-- Plan Date and Priority -->
        <div class="form-row">
          <div class="form-field">
            <label for="todo-plan-date">计划执行日期</label>
            <input 
              id="todo-plan-date" 
              v-model="form.planDate" 
              type="date" 
              required 
              class="form-control" 
            />
          </div>
          
          <div class="form-field">
            <label for="todo-priority">任务优先级</label>
            <select id="todo-priority" v-model="form.priority" class="form-control">
              <option value="high">🔴 高</option>
              <option value="medium">🟡 中</option>
              <option value="low">🟢 低</option>
            </select>
          </div>
        </div>

        <!-- Categories -->
        <div class="form-row">
          <div class="form-field">
            <label for="todo-category">所属大分类</label>
            <select id="todo-category" v-model="form.categoryId" class="form-control" @change="onCategoryChange">
              <option :value="null">无</option>
              <option v-for="cat in categories" :key="cat.id" :value="cat.id">
                {{ cat.name }}
              </option>
            </select>
          </div>

          <div class="form-field">
            <label for="todo-subcategory">二级子分类</label>
            <select id="todo-subcategory" v-model="form.subcategoryId" class="form-control" :disabled="!form.categoryId">
              <option :value="null">无</option>
              <option v-for="sub in subcategories" :key="sub.id" :value="sub.id">
                {{ sub.name }}
              </option>
            </select>
          </div>
        </div>

        <!-- Reminder -->
        <div class="form-field">
          <label for="todo-reminder">到期提醒时间</label>
          <input 
            id="todo-reminder" 
            v-model="form.reminderTime" 
            type="datetime-local" 
            class="form-control" 
          />
        </div>

        <!-- Metadata logs -->
        <div class="meta-logs-panel">
          <div class="meta-row">
            <span>📅 创建时间：</span>
            <span>{{ formatDateTime(todo.createdAt) }}</span>
          </div>
          <div v-if="todo.doneAt" class="meta-row">
            <span>✅ 完成时间：</span>
            <span class="text-success">{{ formatDateTime(todo.doneAt) }}</span>
          </div>
          <div v-if="todo.rolloverCount > 0" class="meta-row">
            <span>♻️ 自动延续次数：</span>
            <span>由于逾期被系统自动延续了 {{ todo.rolloverCount }} 次</span>
          </div>
          <div v-if="todo.derivedFromType" class="meta-row">
            <span>🔗 派生关系：</span>
            <span class="text-indigo">
              派生自月度计划 ({{ todo.derivedFromType === 'performance' ? '个人绩效' : '团队冲刺' }})
            </span>
          </div>
        </div>

        <!-- Form Actions -->
        <div class="form-actions">
          <button type="button" class="btn btn-delete" @click="handleDelete">
            🗑️ 删除待办
          </button>
          
          <button type="button" class="btn btn-toggle-status" @click="toggleStatus">
            {{ todo.status === 'done' ? '撤销完成' : '标记已完成' }}
          </button>

          <button type="submit" class="btn btn-save">
            💾 保存修改
          </button>
        </div>
      </form>
    </div>

    <!-- Error state -->
    <div v-else class="empty-state premium-card">
      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.2" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" />
      </svg>
      <h3>未找到待办详情</h3>
      <p>抱歉，该待办事项不存在或已被彻底删除。</p>
      <router-link to="/today" class="btn btn-save" style="margin-top: 12px; display: inline-block;">
        返回今日待办
      </router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useTodoStore } from '@/stores/todo'
import type { Todo } from '@/stores/todo'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const todoStore = useTodoStore()

const todoId = computed(() => Number(route.params.id))
const todo = computed(() => todoStore.todos.find(t => t.id === todoId.value))

const categories = computed(() => todoStore.sortedCategories)
const subcategories = computed(() => {
  if (!form.categoryId) return []
  return todoStore.subcategoriesByCategoryId(form.categoryId)
})

const form = reactive({
  title: '',
  content: '',
  planDate: '',
  priority: 'medium' as 'high' | 'medium' | 'low',
  categoryId: null as number | null,
  subcategoryId: null as number | null,
  reminderTime: ''
})

onMounted(() => {
  if (authStore.currentUser) {
    todoStore.refreshCategories(authStore.currentUser.userId)
    todoStore.refreshTodayTodos(authStore.currentUser.userId)
    if (todo.value) {
      initForm(todo.value)
    }
  }
})

const initForm = (t: Todo) => {
  form.title = t.title
  form.content = t.content || ''
  form.planDate = t.planDate
  form.priority = t.priority
  form.categoryId = t.categoryId
  form.subcategoryId = t.subcategoryId
  if (t.reminderTime) {
    const [d, time] = t.reminderTime.split(' ')
    form.reminderTime = `${d}T${time}`
  } else {
    form.reminderTime = ''
  }
}

const onCategoryChange = () => {
  form.subcategoryId = null
}

const priorityLabel = (prio: string) => {
  if (prio === 'high') return '高'
  if (prio === 'medium') return '中'
  return '低'
}

const formatDateTime = (isoStr: string) => {
  if (!isoStr) return '-'
  return new Date(isoStr).toLocaleString('zh-CN')
}

const handleSave = () => {
  if (!authStore.currentUser || !todo.value) return
  
  let formattedReminder: string | null = null
  if (form.reminderTime) {
    formattedReminder = form.reminderTime.replace('T', ' ')
  }

  todoStore.updateTodo(authStore.currentUser.userId, todo.value.id, {
    title: form.title.trim(),
    content: form.content.trim(),
    planDate: form.planDate,
    priority: form.priority,
    categoryId: form.categoryId,
    subcategoryId: form.subcategoryId,
    reminderTime: formattedReminder
  })

  const event = new CustomEvent('app-toast', { detail: { text: '保存成功！' } })
  window.dispatchEvent(event)
  router.push('/today')
}

const toggleStatus = () => {
  if (!authStore.currentUser || !todo.value) return
  const newStatus = todo.value.status === 'done' ? 'pending' : 'done'
  todoStore.updateTodo(authStore.currentUser.userId, todo.value.id, { status: newStatus })
  
  const text = newStatus === 'done' ? '任务已标记完成' : '撤销完成任务'
  const event = new CustomEvent('app-toast', { detail: { text } })
  window.dispatchEvent(event)
  router.push('/today')
}

const handleDelete = () => {
  if (!authStore.currentUser || !todo.value) return
  if (confirm(`确定要将待办【${todo.value.title}】删除吗？`)) {
    todoStore.deleteTodo(authStore.currentUser.userId, todo.value.id)
    const event = new CustomEvent('app-toast', { detail: { text: '删除成功' } })
    window.dispatchEvent(event)
    router.push('/today')
  }
}
</script>

<style scoped>
.todo-detail-view {
  max-width: 720px;
  margin: 40px auto;
}

.detail-card {
  padding: 32px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1.5px solid var(--border-light);
  padding-bottom: 20px;
  margin-bottom: 24px;
}
.back-link {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--text-muted);
  font-weight: 600;
  transition: color var(--transition-fast);
}
.back-link:hover {
  color: var(--primary);
}
.back-icon {
  width: 18px;
  height: 18px;
}

.header-badges {
  display: flex;
  align-items: center;
  gap: 10px;
}
.status-badge {
  font-size: 12px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: var(--radius-sm);
}
.status-badge.pending {
  background-color: var(--warning-bg);
  color: var(--warning);
}
.status-badge.done {
  background-color: var(--success-bg);
  color: var(--success);
}

.detail-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}
.form-field label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-main);
}
.form-control {
  border: 1.5px solid var(--border-medium);
  border-radius: var(--radius-md);
  padding: 12px 16px;
  outline: none;
  font-size: 14px;
  background-color: #fff;
  transition: all var(--transition-fast);
}
.form-control:focus {
  border-color: var(--primary);
}
.title-input {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-main);
}
textarea.form-control {
  resize: vertical;
}

.meta-logs-panel {
  background-color: var(--bg-app);
  border-radius: var(--radius-md);
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.meta-row {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 500;
}

.form-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  border-top: 1.5px solid var(--border-light);
  padding-top: 24px;
  margin-top: 12px;
}
.btn {
  height: 44px;
  padding: 0 20px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  transition: all var(--transition-fast);
}
.btn-delete {
  background-color: var(--danger-bg);
  color: var(--danger);
  border: 1.5px solid rgba(239, 68, 68, 0.1);
  margin-right: auto;
}
.btn-delete:hover {
  background-color: var(--danger);
  color: #fff;
}
.btn-toggle-status {
  background-color: var(--border-medium);
  color: var(--text-main);
}
.btn-toggle-status:hover {
  background-color: #cbd5e1;
}
.btn-save {
  background-color: var(--primary);
  color: #fff;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.2);
}
.btn-save:hover {
  background-color: var(--primary-hover);
  transform: translateY(-1px);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 24px;
  text-align: center;
}
.empty-state svg {
  width: 72px;
  height: 72px;
  color: var(--border-medium);
  margin-bottom: 16px;
}
</style>
