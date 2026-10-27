<template>
  <div class="todo-template-view">
    <!-- Header -->
    <div class="view-header">
      <div class="title-row">
        <div class="title-meta-left">
          <h2>定时待办</h2>
          <p class="subtitle-lbl">配置周期规则与生成时间点，系统自动生成待办；未完成时不生成下一条，完成后继续</p>
        </div>
        <button class="add-btn" @click="openCreateModal">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" style="width: 15px; height: 15px;">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
          </svg>
          <span>新增模板</span>
        </button>
      </div>
    </div>

    <!-- List -->
    <div v-if="loading" class="state-hint">加载中…</div>
    <div v-else-if="templates.length === 0" class="state-hint">
      暂无定时待办模板。点击右上角「新增模板」配置周期规则（如：每周五 交周报），到期系统会自动生成待办。
    </div>
    <div v-else class="template-list">
      <div v-for="t in templates" :key="t.id" class="template-card" :class="{ 'is-disabled': !t.enabled }">
        <div class="template-main">
          <div class="template-title-row">
            <span class="template-title">{{ t.title }}</span>
            <span class="enabled-tag" :class="t.enabled ? 'on' : 'off'">{{ t.enabled ? '启用中' : '已停用' }}</span>
          </div>
          <div class="template-meta">
            <span class="meta-rule">🔁 {{ ruleText(t) }}</span>
            <span v-if="t.categoryId">📂 {{ categoryName(t.categoryId) }}</span>
            <span>⚡ {{ priorityText(t.priority) }}</span>
            <span v-if="t.nextDate">下次生成: {{ t.nextDate }} {{ t.recurrenceTime || '09:00' }}</span>
          </div>
          <div v-if="t.lastTodoId" class="template-last">
            最近生成:
            <span v-if="t.lastTodoStatus === '0'" class="last-chip pending">进行中（完成后生成下一条）</span>
            <span v-else-if="t.lastTodoStatus === '1'" class="last-chip done">已完成</span>
            <span v-else class="last-chip">已删除</span>
          </div>
        </div>
        <div class="template-actions">
          <button class="action-btn" @click="openEditModal(t)">编辑</button>
          <button class="action-btn" @click="toggleEnabled(t)">{{ t.enabled ? '停用' : '启用' }}</button>
          <button class="action-btn danger" @click="confirmDelete(t)">{{ pendingDeleteId === t.id ? '确认删除？' : '删除' }}</button>
        </div>
      </div>
    </div>

    <!-- Create/Edit Modal -->
    <div v-if="modal.isOpen" class="modal-overlay" @click.self="modal.isOpen = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3>{{ modal.editId ? '编辑定时待办模板' : '新增定时待办模板' }}</h3>
          <button class="close-btn" @click="modal.isOpen = false">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="form-group">
          <label>待办标题<span class="required-star">*</span></label>
          <input v-model="modal.form.title" type="text" class="form-input" style="height: 38px; font-size: 13.5px;" placeholder="如 交周报 / 更新绩效进度" @keyup.enter="submitModal" />
        </div>
        <div class="form-group">
          <label>备注</label>
          <input v-model="modal.form.note" type="text" class="form-input" style="height: 36px; font-size: 13px;" placeholder="补充说明（可选）" />
        </div>

        <div class="form-group">
          <label>重复频率<span class="required-star">*</span></label>
          <div class="type-chips">
            <button
              v-for="opt in [['DAILY', '每天'], ['WEEKLY', '每周'], ['MONTHLY', '每月']]"
              :key="opt[0]"
              type="button"
              class="type-chip"
              :class="{ active: modal.form.recurrenceType === opt[0] }"
              @click="changeRecurrenceType(opt[0])"
            >{{ opt[1] }}</button>
          </div>
          <div v-if="modal.form.recurrenceType === 'WEEKLY'" class="value-chips">
            <button
              v-for="(name, idx) in ['一', '二', '三', '四', '五', '六', '日']"
              :key="idx"
              type="button"
              class="value-chip"
              :class="{ active: weeklyDays.includes(idx + 1) }"
              @click="toggleWeeklyDay(idx + 1)"
            >周{{ name }}</button>
          </div>
          <div v-if="modal.form.recurrenceType === 'MONTHLY'" class="value-chips months">
            <button
              v-for="d in 31"
              :key="d"
              type="button"
              class="value-chip"
              :class="{ active: monthlyDays.includes(d) }"
              :title="MONTHS_WITHOUT_DAY.includes(d) ? '部分月份没有这一天，会自动跳过' : ''"
              @click="toggleMonthlyDay(d)"
            >{{ d }}</button>
          </div>

        <div class="form-group">
          <label>生成时间点<span class="required-star">*</span></label>
          <div class="select-trigger" :class="{ active: timeDropdownOpen }" style="position: relative;" @click.stop="openTimePicker">
            <span style="display: inline-flex; align-items: center; gap: 6px; color: var(--text-main);">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" style="width: 14px; height: 14px; color: var(--text-muted);">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              {{ modal.form.recurrenceTime || '09:00' }}
            </span>
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.2" stroke="currentColor" style="width: 13px; height: 13px; color: var(--text-muted);">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <input
              ref="timeInputRef"
              v-model="modal.form.recurrenceTime"
              type="time"
              class="time-native-input"
              required
            />
          </div>
        </div>
        </div>

        <div style="display: flex; gap: 12px;">
          <div ref="catTriggerRef" class="form-group" style="flex: 1; position: relative;">
            <label>分类</label>
            <div class="select-trigger" :class="{ active: catDropdownOpen }" @click.stop="toggleCatDropdown">
              <span :style="{ color: modal.form.categoryId ? 'var(--text-main)' : 'var(--text-muted)' }">{{ categoryLabel(modal.form.categoryId) }}</span>
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.2" stroke="currentColor" style="width: 13px; height: 13px;">
                <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
              </svg>
            </div>
            <Teleport to="body">
              <div v-if="catDropdownOpen" class="dd-list dd-fixed" :style="ddPos.cat" @click.stop>
                <div class="dd-item" :class="{ selected: !modal.form.categoryId }" @click="modal.form.categoryId = null; catDropdownOpen = false">未分类</div>
                <template v-for="cat in todoStore.categories" :key="cat.id">
                  <div class="dd-item" :class="{ selected: modal.form.categoryId === cat.id }" @click="modal.form.categoryId = cat.id; catDropdownOpen = false">
                    <span class="color-dot" :style="{ backgroundColor: cat.color || '#94a3b8' }"></span>
                    {{ cat.name }}
                  </div>
                  <div
                    v-for="sub in (todoStore.subcategoriesMap[cat.id] || [])"
                    :key="sub.id"
                    class="dd-item sub"
                    :class="{ selected: modal.form.categoryId === sub.id }"
                    @click="modal.form.categoryId = sub.id; catDropdownOpen = false"
                  >{{ sub.name }}</div>
                </template>
              </div>
            </Teleport>
          </div>
          <div ref="priTriggerRef" class="form-group" style="flex: 1; position: relative;">
            <label>优先级</label>
            <div class="select-trigger" :class="{ active: priDropdownOpen }" @click.stop="togglePriDropdown">
              <span :style="{ color: ['0', '1', '2'].includes(modal.form.priority) ? 'var(--text-main)' : 'var(--text-muted)' }">{{ priorityText(modal.form.priority) }}</span>
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.2" stroke="currentColor" style="width: 13px; height: 13px;">
                <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
              </svg>
            </div>
            <Teleport to="body">
              <div v-if="priDropdownOpen" class="dd-list dd-fixed" :style="ddPos.pri" @click.stop>
                <div
                  v-for="opt in [['0', '低'], ['1', '中'], ['2', '高']]"
                  :key="opt[0]"
                  class="dd-item"
                  :class="{ selected: modal.form.priority === opt[0] }"
                  @click="modal.form.priority = opt[0]; priDropdownOpen = false"
                >{{ opt[1] }}</div>
              </div>
            </Teleport>
          </div>
        </div>

        <div class="modal-footer">
          <button type="button" class="action-btn-secondary" style="padding: 8px 18px; font-size: 13.5px;" :disabled="modal.submitting" @click="modal.isOpen = false">取消</button>
          <button
            type="button"
            class="action-btn-primary"
            style="padding: 8px 20px; font-size: 13.5px;"
            :disabled="modal.submitting || !modal.form.title.trim() || !isRecurrenceValid"
            @click="submitModal"
          >{{ modal.submitting ? '保存中…' : '保存' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useTodoStore } from '@/stores/todo'
import request from '@/api/request'

interface TemplateItem {
  id: number
  title: string
  note: string | null
  categoryId: number | null
  priority: string
  recurrenceType: string
  recurrenceValue: string
  recurrenceTime: string
  enabled: boolean
  nextDate: string
  lastTodoId: number | null
  lastTodoStatus: string | null
}

const authStore = useAuthStore()
const todoStore = useTodoStore()

const templates = ref<TemplateItem[]>([])
const loading = ref(false)
const pendingDeleteId = ref<number | null>(null)
let pendingDeleteTimer: number | null = null

const catDropdownOpen = ref(false)
const priDropdownOpen = ref(false)
const timeDropdownOpen = ref(false)
const timeInputRef = ref<HTMLInputElement | null>(null)
const catTriggerRef = ref<HTMLDivElement | null>(null)
const priTriggerRef = ref<HTMLDivElement | null>(null)

/** 下拉列表挂到 body 后按触发框坐标 fixed 定位（贴不上时向上弹） */
const ddPos = reactive({
  cat: { top: '0px', left: '0px', width: '200px' },
  pri: { top: '0px', left: '0px', width: '200px' }
})

const updateDdPos = (trigger: HTMLElement, pos: { top: string; left: string; width: string }) => {
  const rect = trigger.getBoundingClientRect()
  const listH = 204
  const openUp = rect.bottom + listH + 8 > window.innerHeight && rect.top > listH + 8
  pos.top = (openUp ? rect.top - listH - 8 : rect.bottom + 4) + 'px'
  pos.left = rect.left + 'px'
  pos.width = rect.width + 'px'
}

const toggleCatDropdown = () => {
  catDropdownOpen.value = !catDropdownOpen.value
  priDropdownOpen.value = false
  if (catDropdownOpen.value && catTriggerRef.value) updateDdPos(catTriggerRef.value, ddPos.cat)
}

const togglePriDropdown = () => {
  priDropdownOpen.value = !priDropdownOpen.value
  catDropdownOpen.value = false
  if (priDropdownOpen.value && priTriggerRef.value) updateDdPos(priTriggerRef.value, ddPos.pri)
}

/** 整框点击弹出原生时间选择器（浏览器要求用户手势内调用） */
const openTimePicker = () => {
  timeDropdownOpen.value = true
  const el = timeInputRef.value
  if (!el) return
  try {
    (el as HTMLInputElement & { showPicker?: () => void }).showPicker?.()
  } catch {
    el.focus()
  }
}

const emitToastMsg = (text: string, type?: string) => {
  window.dispatchEvent(new CustomEvent('app-toast', { detail: type ? { text, type } : { text } }))
}

const MONTHS_WITHOUT_DAY = [30, 31]

const ruleText = (t: { recurrenceType: string; recurrenceValue: string; recurrenceTime?: string | null }) => {
  let text = ''
  if (t.recurrenceType === 'DAILY') text = '每天'
  else if (t.recurrenceType === 'WEEKLY') {
    const names = ['一', '二', '三', '四', '五', '六', '日']
    text = '每周 ' + parseDays(t.recurrenceValue).map(d => names[d - 1]).join('、')
  } else {
    text = '每月 ' + parseDays(t.recurrenceValue).map(d => d + ' 号').join('、')
  }
  return t.recurrenceTime ? `${text} ${t.recurrenceTime}` : text
}

const parseDays = (value: string): number[] =>
  (value || '').split(',').map(v => Number(v.trim())).filter(v => v > 0)

const priorityText = (p: string) => ({ '0': '低', '1': '中', '2': '高' } as Record<string, string>)[p] || p

const categoryName = (id: number | null): string => {
  if (!id) return '未分类'
  for (const cat of todoStore.categories) {
    if (cat.id === id) return cat.name
    const sub = (todoStore.subcategoriesMap[cat.id] || []).find(s => s.id === id)
    if (sub) return sub.name
  }
  return '未分类'
}

const categoryLabel = (id: number | null) => (id ? categoryName(id) : '未分类')

// ---- 加载 ----
const loadTemplates = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/todo-templates')
    templates.value = (res.data as TemplateItem[]) || []
  } catch {
    // 拦截器统一 toast
  } finally {
    loading.value = false
  }
}

const ensureCategories = async () => {
  if (todoStore.categories.length === 0 && authStore.currentUser) {
    await todoStore.refreshCategories(authStore.currentUser.userId)
  }
}

onMounted(async () => {
  document.addEventListener('click', closeDropdowns)
  await Promise.all([loadTemplates(), ensureCategories()])
})
onUnmounted(() => document.removeEventListener('click', closeDropdowns))
const closeDropdowns = () => {
  catDropdownOpen.value = false
  priDropdownOpen.value = false
}

// ---- 新增/编辑 ----
const modal = reactive({
  isOpen: false,
  editId: null as number | null,
  submitting: false,
  form: {
    title: '',
    note: '',
    categoryId: null as number | null,
    priority: '1',
    recurrenceType: 'DAILY',
    recurrenceValue: '*',
    recurrenceTime: '09:00',
    enabled: true
  }
})

const weeklyDays = computed(() => (modal.form.recurrenceType === 'WEEKLY' ? parseDays(modal.form.recurrenceValue) : []))
const monthlyDays = computed(() => (modal.form.recurrenceType === 'MONTHLY' ? parseDays(modal.form.recurrenceValue) : []))
const isRecurrenceValid = computed(() =>
  modal.form.recurrenceType === 'DAILY' || parseDays(modal.form.recurrenceValue).length > 0)

const changeRecurrenceType = (type: string) => {
  modal.form.recurrenceType = type
  modal.form.recurrenceValue = type === 'DAILY' ? '*' : ''
}

const toggleWeeklyDay = (day: number) => {
  const days = weeklyDays.value
  modal.form.recurrenceValue = (days.includes(day) ? days.filter(d => d !== day) : [...days, day].sort((a, b) => a - b))
    .join(',') || ''
}
const toggleMonthlyDay = (day: number) => {
  const days = monthlyDays.value
  modal.form.recurrenceValue = (days.includes(day) ? days.filter(d => d !== day) : [...days, day].sort((a, b) => a - b))
    .join(',') || ''
}

const openCreateModal = () => {
  modal.editId = null
  modal.form = {
    title: '',
    note: '',
    categoryId: null,
    priority: '1',
    recurrenceType: 'DAILY',
    recurrenceValue: '*',
    recurrenceTime: '09:00',
    enabled: true
  }
  modal.isOpen = true
}

const openEditModal = (t: TemplateItem) => {
  modal.editId = t.id
  modal.form = {
    title: t.title,
    note: t.note || '',
    categoryId: t.categoryId,
    priority: t.priority,
    recurrenceType: t.recurrenceType,
    recurrenceValue: t.recurrenceValue,
    recurrenceTime: t.recurrenceTime || '09:00',
    enabled: t.enabled
  }
  modal.isOpen = true
}

const submitModal = async () => {
  if (modal.submitting || !modal.form.title.trim() || !isRecurrenceValid.value) return
  modal.submitting = true
  try {
    const payload = {
      title: modal.form.title.trim(),
      note: modal.form.note.trim() || undefined,
      categoryId: modal.form.categoryId || undefined,
      priority: modal.form.priority,
      recurrenceType: modal.form.recurrenceType,
      recurrenceValue: modal.form.recurrenceValue || '*',
      recurrenceTime: modal.form.recurrenceTime || '09:00'
    }
    if (modal.editId) {
      await request.put(`/api/todo-templates/${modal.editId}`, payload)
      emitToastMsg('模板已更新')
    } else {
      await request.post('/api/todo-templates', payload)
      emitToastMsg('模板已创建，到期将自动生成待办')
    }
    modal.isOpen = false
    await loadTemplates()
  } catch {
    // 拦截器统一 toast
  } finally {
    modal.submitting = false
  }
}

// ---- 启停 / 删除 ----
const toggleEnabled = async (t: TemplateItem) => {
  try {
    await request.put(`/api/todo-templates/${t.id}`, {
      title: t.title,
      note: t.note || undefined,
      categoryId: t.categoryId || undefined,
      priority: t.priority,
      recurrenceType: t.recurrenceType,
      recurrenceValue: t.recurrenceValue,
      enabled: !t.enabled
    })
    t.enabled = !t.enabled
    emitToastMsg(t.enabled ? '模板已启用' : '模板已停用')
  } catch {
    // 拦截器统一 toast
  }
}

const confirmDelete = async (t: TemplateItem) => {
  if (pendingDeleteId.value !== t.id) {
    pendingDeleteId.value = t.id
    if (pendingDeleteTimer) clearTimeout(pendingDeleteTimer)
    pendingDeleteTimer = window.setTimeout(() => {
      pendingDeleteId.value = null
    }, 3000)
    return
  }
  pendingDeleteId.value = null
  if (pendingDeleteTimer) clearTimeout(pendingDeleteTimer)
  try {
    await request.delete(`/api/todo-templates/${t.id}`)
    emitToastMsg('模板已删除（已生成的待办不受影响）')
    await loadTemplates()
  } catch {
    // 拦截器统一 toast
  }
}
</script>

<style scoped>
.todo-template-view {
  max-width: 1100px;
  margin: 0 auto;
  padding-bottom: 60px;
  color: #334155;
}

.view-header {
  background-color: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 24px 32px;
  margin-bottom: 20px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
}
.title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}
.title-row h2 {
  margin: 0 0 4px 0;
  font-size: 20px;
  font-weight: 800;
  color: var(--text-main);
}
.subtitle-lbl {
  margin: 0;
  font-size: 12.5px;
  color: var(--text-muted);
}
.add-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background-color: #2563eb;
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  padding: 0 18px;
  height: 38px;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  flex-shrink: 0;
  transition: background var(--transition-fast);
}
.add-btn:hover {
  background-color: #1d4ed8;
}

.state-hint {
  text-align: center;
  color: var(--text-muted);
  font-size: 13.5px;
  padding: 48px 32px;
  background-color: var(--bg-card);
  border: 1px dashed var(--border-medium);
  border-radius: var(--radius-lg);
  line-height: 1.7;
}

.template-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.template-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background-color: var(--bg-card);
  border: 1px solid var(--border-light);
  border-left: 3px solid #2563eb;
  border-radius: var(--radius-md);
  padding: 14px 18px;
}
.template-card.is-disabled {
  opacity: 0.55;
  border-left-color: var(--border-medium);
}
.template-main {
  min-width: 0;
  flex: 1;
}
.template-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.template-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-main);
}
.enabled-tag {
  font-size: 10.5px;
  font-weight: 700;
  padding: 1px 8px;
  border-radius: 10px;
  flex-shrink: 0;
}
.enabled-tag.on {
  color: #15803d;
  background-color: #f0fdf4;
  border: 1px solid #bbf7d0;
}
.enabled-tag.off {
  color: #64748b;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
}
.template-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 5px;
  font-size: 12px;
  color: var(--text-muted);
}
.meta-rule {
  color: #1d4ed8;
  font-weight: 600;
}
.template-last {
  margin-top: 5px;
  font-size: 12px;
  color: var(--text-muted);
}
.last-chip {
  font-size: 11px;
  font-weight: 600;
  padding: 1px 8px;
  border-radius: 10px;
  margin-left: 4px;
}
.last-chip.pending {
  color: #d97706;
  background-color: #fef3c7;
}
.last-chip.done {
  color: #15803d;
  background-color: #f0fdf4;
}
.template-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}
.action-btn {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  background: none;
  border: 1px solid var(--border-medium);
  border-radius: 6px;
  padding: 5px 10px;
  cursor: pointer;
  white-space: nowrap;
  transition: all var(--transition-fast);
}
.action-btn:hover {
  color: #2563eb;
  border-color: #2563eb;
}
.action-btn.danger:hover {
  color: #ef4444;
  border-color: #ef4444;
}

/* Modal */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2100;
}
.modal-content {
  background: #fff;
  border-radius: var(--radius-lg);
  padding: 24px 28px;
  box-shadow: var(--shadow-xl);
  border: 1px solid var(--border-light);
  max-width: 520px;
  width: 95%;
  max-height: 88vh;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.modal-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 800;
  color: var(--text-main);
}
.close-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--text-muted);
  display: flex;
  padding: 4px;
  border-radius: 50%;
}
.close-btn:hover {
  color: var(--text-main);
  background-color: #f1f5f9;
}
.close-btn svg {
  width: 16px;
  height: 16px;
}
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 4px;
}

.action-btn-primary,
.action-btn-secondary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-radius: var(--radius-md);
  padding: 8px 18px;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
}
.action-btn-primary {
  background-color: var(--primary);
  color: #fff;
  border: none;
  box-shadow: 0 4px 10px -2px rgba(79, 70, 229, 0.2);
}
.action-btn-primary:hover:not(:disabled) {
  background-color: var(--primary-hover);
}
.action-btn-secondary {
  background-color: var(--bg-app);
  color: var(--text-main);
  border: 1px solid var(--border-medium);
}
.action-btn-secondary:hover:not(:disabled) {
  background-color: #f1f5f9;
  color: var(--text-main);
}
.action-btn-primary:disabled,
.action-btn-secondary:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.form-group label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-main);
}
.required-star {
  color: #ef4444;
  margin-left: 2px;
}
.form-input {
  width: 100%;
  padding: 0 12px;
  border-radius: var(--radius-md);
  border: 1.5px solid var(--border-medium);
  background-color: #fff;
  outline: none;
  transition: all var(--transition-fast);
}
.form-input:focus {
  border-color: var(--primary);
}

.type-chips,
.value-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.value-chips.months {
  max-width: 320px;
}
.type-chip,
.value-chip {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-muted);
  background-color: #fff;
  border: 1.5px solid var(--border-medium);
  border-radius: 8px;
  padding: 5px 12px;
  cursor: pointer;
  transition: all var(--transition-fast);
}
.value-chip {
  padding: 4px 10px;
}
.type-chip.active,
.value-chip.active {
  color: #fff;
  background-color: #2563eb;
  border-color: #2563eb;
}
.value-chip:hover:not(.active) {
  border-color: #93c5fd;
  color: #1d4ed8;
}

.select-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  height: 38px;
  padding: 0 12px;
  border: 1.5px solid var(--border-medium);
  border-radius: var(--radius-md);
  background-color: #fff;
  cursor: pointer;
  font-size: 13.5px;
  transition: all var(--transition-fast);
}
.select-trigger.active {
  border-color: var(--primary);
}
.time-native-input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
  border: none;
  padding: 0;
}
.dd-list {
  background: #fff;
  border: 1px solid var(--border-medium);
  border-radius: var(--radius-md);
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.12);
  padding: 5px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  max-height: 200px;
  overflow-y: auto;
}
.dd-fixed {
  position: fixed;
  z-index: 2200;
}
.dd-item {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-main);
}
.dd-item:hover {
  background-color: #f1f5f9;
}
.dd-item.selected {
  background-color: #eff6ff;
  color: #1d4ed8;
  font-weight: 600;
}
.dd-item.sub {
  padding-left: 24px;
  font-size: 12.5px;
}
.color-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}
</style>
