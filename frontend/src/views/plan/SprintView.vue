<template>
  <div class="sprint-view">
    <!-- Header panel with Title and controls -->
    <div class="view-header premium-card">
      <div class="title-row">
        <div class="title-col">
          <h2>团队冲刺任务</h2>
          <p class="subtitle-desc">管理团队本月冲刺任务，把握团队主线与交付进度</p>
        </div>

        <div class="actions-col">
          <!-- Month Selector with Dropdown-like style -->
          <div class="month-selector-dropdown">
            <button class="nav-arrow-btn" @click="shiftMonth(-1)" title="上一个月">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 19.5L8.25 12l7.5-7.5" />
              </svg>
            </button>
            <span class="month-label">{{ formattedMonthLabel }}</span>
            <button class="nav-arrow-btn" @click="shiftMonth(1)" :disabled="selectedMonth >= currentMonthStr" title="下一个月">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
              </svg>
            </button>
          </div>

          <!-- New Sprint Button -->
          <button class="add-objective-btn" @click="openSprintModal(null)">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="btn-icon">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
            </svg>
            <span>新增冲刺任务</span>
          </button>
        </div>
      </div>
    </div>

    <!-- Main Monthly Progress Card -->
    <div class="monthly-total-progress-card premium-card">
      <div class="progress-header-row">
        <span class="progress-title-lbl">本月冲刺交付度</span>
        <span class="progress-percentage-val">{{ sprintCompletionRate }}%</span>
      </div>

      <!-- Main Progress Bar -->
      <div class="main-progress-container-bar">
        <div class="main-progress-fill-bar" :style="{ width: `${sprintCompletionRate}%` }"></div>
      </div>

      <div class="progress-footer-meta">
        <span class="objectives-kr-count">
          当前共 {{ totalSprints }} 个冲刺任务，其中 {{ involvedSprints }} 个需要介入
        </span>
        <span class="update-time-lbl">
          更新于 {{ new Date().toISOString().split('T')[0] }}
        </span>
      </div>
    </div>

    <!-- Sprint checklist filter row -->
    <div class="sprint-filter-bar">
      <div class="involved-toggle-wrapper">
        <label class="toggle-control">
          <input type="checkbox" v-model="filterOnlyInvolved" />
          <span class="toggle-slider"></span>
        </label>
        <span class="toggle-lbl">仅看我需要介入的任务</span>
      </div>
    </div>

    <!-- Objectives-like card layout for Sprint Checklist -->
    <div class="objectives-list-container">
      <div v-if="filteredSprints.length > 0" class="obj-list-wrapper">
        <div class="objective-card premium-card" style="padding: 24px;">
          <div class="key-results-sub-section" style="margin-top: 0;">
            <div class="kr-items-wrapper">
              <div v-for="(item, index) in filteredSprints" :key="item.id" class="kr-item-box-row" :class="{ completed: item.status === 'done' }">
                
                <!-- Left Status Checkbox/Indicator -->
                <div 
                  class="kr-status-icon-wrap" 
                  :class="{ disabled: !item.needInvolved || (item.keyResultIds && item.keyResultIds.length > 0) }"
                  @click="toggleSprintStatus(item)" 
                  :title="!item.needInvolved ? '该任务无需介入，不可修改状态' : (item.keyResultIds && item.keyResultIds.length > 0 ? '该任务已绑定关键成果，状态由关键成果联动，不可手动修改' : (item.status === 'done' ? '点击重置为进行中' : (item.status === 'in_progress' ? '点击标记为已完成' : '点击设置为进行中')))"
                >
                  <div class="custom-indicator-circle" :class="item.status">
                    <span v-if="item.status === 'done'" class="check-mark">✓</span>
                    <span v-else-if="item.status === 'in_progress'" class="spin-dot"></span>
                  </div>
                </div>

                <!-- Main Sprint Content -->
                <div class="kr-main-info">
                  <h5 class="kr-title-heading">冲刺{{ index + 1 }}: {{ item.title }}</h5>
                  <p class="kr-remark-content">
                    任务描述：{{ item.remark || '暂无详细描述，日常冲刺交付中。' }}
                  </p>
                  <div v-if="item.keyResultIds && item.keyResultIds.length > 0" class="kr-time-meta" style="margin-top: 2px; display: flex; gap: 16px; flex-wrap: wrap;">
                    <span class="meta-item-lbl">🔗 已关联 {{ item.keyResultIds.length }} 项关键成果</span>
                  </div>
                </div>

                <!-- Right Status Tag & Actions -->
                <div class="kr-right-meta-actions">
                  <span class="kr-status-tag" :class="item.needInvolved ? item.status : 'not_involved'">
                    {{ item.needInvolved ? (item.status === 'done' ? '已完成' : (item.status === 'in_progress' ? '进行中' : '未开始')) : '无需介入' }}
                  </span>
                  
                  <div class="kr-row-action-buttons">
                    <!-- Involved toggle button -->
                    <button class="involved-action-btn-item" :class="{ active: item.needInvolved }" @click="toggleInvolved(item)" :title="item.needInvolved ? '取消介入跟进' : '标记为需我介入跟进'">
                      {{ item.needInvolved ? '🙋‍♂️ 取消介入' : '🙋‍♂️ 标记介入' }}
                    </button>
                    <!-- Link Key Results -->
                    <button 
                      v-if="item.needInvolved"
                      class="kr-action-btn-item link-kr" 
                      @click="openLinkKRModal(item)" 
                      title="绑定本月绩效关键成果"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M13.19 8.688a4.5 4.5 0 011.242 7.244l-4.5 4.5a4.5 4.5 0 01-6.364-6.364l1.757-1.757m13.35-.622l1.757-1.757a4.5 4.5 0 00-6.364-6.364l-4.5 4.5a4.5 4.5 0 001.242 7.244" />
                      </svg>
                    </button>
                    <!-- Edit Sprint -->
                    <button class="kr-action-btn-item" @click="openSprintModal(item)" title="编辑冲刺任务">
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10" />
                      </svg>
                    </button>
                    <!-- Delete Sprint -->
                    <button class="kr-action-btn-item delete" @click="handleDeleteSprint(item)" title="删除此冲刺任务">
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" />
                      </svg>
                    </button>
                  </div>
                </div>

              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-else class="empty-state premium-card" style="padding: 60px 24px;">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.2" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" d="M18 18.72a9.094 9.094 0 003.741-.479 3 3 0 00-4.682-2.72m.94 3.198" />
        </svg>
        <h3>本月尚无冲刺任务</h3>
        <p>建立团队本月冲刺任务，把握团队主线，标记需介入项目以开展每日工作配合！</p>
      </div>
    </div>

    <!-- 1. Sprint Edit Modal -->
    <div v-if="sprintModal.isOpen" class="modal-overlay" @mousedown="mousedownTarget = $event.target" @click="mousedownTarget === $event.currentTarget && $event.target === $event.currentTarget && (sprintModal.isOpen = false)">
      <div class="modal-content" @click.stop>
        <h3>{{ sprintModal.isEdit ? '编辑冲刺任务' : '录入新团队冲刺任务' }}</h3>
        
        <form @submit.prevent="saveSprint">
          <div class="form-field">
            <label for="spr-title">任务标题</label>
            <input id="spr-title" v-model="sprintModal.title" type="text" required autocomplete="off" class="form-control" placeholder="如 低代码服务能力建设" />
          </div>


          <div class="form-field">
            <label for="spr-remark">任务描述</label>
            <textarea 
              id="spr-remark" 
              v-model="sprintModal.remark" 
              rows="3" 
              class="form-control" 
              placeholder="任务描述/备注详情..."
            ></textarea>
          </div>

          <div class="modal-actions">
            <button type="button" class="btn btn-secondary" @click="sprintModal.isOpen = false">取消</button>
            <button type="submit" class="btn btn-primary">保存</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Link Key Results Modal -->
    <div v-if="linkKRModal.isOpen" class="modal-overlay" @mousedown="mousedownTarget = $event.target" @click="mousedownTarget === $event.currentTarget && $event.target === $event.currentTarget && (linkKRModal.isOpen = false)">
      <div class="modal-content" @click.stop style="max-width: 580px; width: 95%;">
        <h3>🔗 绑定月度绩效关键成果</h3>

        <div class="kr-selection-body" style="max-height: 320px; overflow-y: auto; padding-right: 4px;">
          <div v-if="selectableKRsGrouped.length > 0" style="display: flex; flex-direction: column; gap: 16px;">
            <div v-for="group in selectableKRsGrouped" :key="group.id" style="background-color: var(--bg-card); border: 1px solid var(--border-medium); border-radius: var(--radius-md); padding: 14px;">
              <h6 style="margin: 0 0 10px 0; font-size: 13.5px; font-weight: 800; color: var(--text-main);">
                🎯 目标 O: {{ group.name }}
              </h6>
              <div style="display: flex; flex-direction: column; gap: 8px; padding-left: 6px;">
                <label v-for="kr in group.krs" :key="kr.id" style="display: flex; align-items: flex-start; gap: 8px; cursor: pointer; font-size: 12.5px; color: var(--text-main); user-select: none;">
                  <input type="checkbox" :value="kr.id" v-model="linkKRModal.selectedKRIds" style="margin-top: 3px;" />
                  <span>
                    <strong style="color: var(--text-muted);">[KR]</strong> {{ kr.title }}
                    <span class="kr-status-tag" :class="kr.status" style="font-size: 9.5px; padding: 1px 4px; margin-left: 4px;">
                      {{ kr.status === 'done' ? '已完成' : (kr.status === 'in_progress' ? '进行中' : '未开始') }}
                    </span>
                  </span>
                </label>
              </div>
            </div>
          </div>
          <div v-else style="text-align: center; color: var(--text-muted); padding: 30px 0; font-size: 13px;">
            本月绩效中尚无关键成果 (Key Results) 记录，请先去个人绩效中添加关键成果。
          </div>
        </div>

        <div class="modal-actions" style="margin-top: 10px;">
          <button type="button" class="btn btn-secondary" @click="linkKRModal.isOpen = false">取消</button>
          <button type="button" class="btn btn-primary" :disabled="selectableKRsGrouped.length === 0" @click="saveLinkKRs">保存</button>
        </div>
      </div>
    </div>

    <!-- Custom Premium Confirm Modal -->
    <div v-if="confirmModal.isOpen" class="modal-overlay" @mousedown="mousedownTarget = $event.target" @click="mousedownTarget === $event.currentTarget && $event.target === $event.currentTarget && (confirmModal.isOpen = false)">
      <div class="modal-content confirm-modal-content" @click.stop style="max-width: 420px; padding: 24px;">
        <div class="confirm-modal-header" style="display: flex; align-items: center; gap: 12px; margin-bottom: 16px;">
          <div class="warning-icon-circle" style="width: 36px; height: 36px; border-radius: 50%; background-color: #fffbeb; color: #d97706; display: flex; align-items: center; justify-content: center; flex-shrink: 0;">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.2" stroke="currentColor" style="width: 20px; height: 20px;">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
            </svg>
          </div>
          <h3 style="margin: 0; font-size: 17px; font-weight: 700; color: var(--text-main);">{{ confirmModal.title }}</h3>
        </div>
        <div class="confirm-modal-body" style="margin-bottom: 24px;">
          <p v-for="(p, idx) in confirmModal.messageLines" :key="idx" :style="idx > 0 ? { color: '#ef4444', fontSize: '12px', marginTop: '8px', lineHeight: '1.5', padding: '10px 12px', backgroundColor: '#fef2f2', borderRadius: '6px', border: '1px solid #fee2e2' } : { color: 'var(--text-main)', fontSize: '14px', lineHeight: '1.5', margin: 0 }">
            {{ p }}
          </p>
        </div>
        <div class="modal-actions" style="display: flex; justify-content: flex-end; gap: 10px;">
          <button class="btn btn-secondary" @click="confirmModal.isOpen = false" style="padding: 8px 16px; font-size: 13px;">取消</button>
          <button class="btn btn-danger" @click="executeConfirm" style="padding: 8px 16px; font-size: 13px; background-color: #ef4444; border-color: #ef4444; color: #fff; border-radius: var(--radius-md); font-weight: 600; cursor: pointer; transition: all var(--transition-fast);">确认删除</button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useTodoStore } from '@/stores/todo'
import type { TeamSprint } from '@/stores/todo'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const todoStore = useTodoStore()

const currentMonthStr = computed(() => new Date().toISOString().substring(0, 7))

// Selected Month (YYYY-MM)
const selectedMonth = ref(route.params.month as string || currentMonthStr.value)
const filterOnlyInvolved = ref(false)

const loadData = async (month: string) => {
  if (authStore.currentUser) {
    todoStore.setMonth(month)
    await todoStore.refreshSprints(authStore.currentUser.userId)
  }
}

watch(selectedMonth, (newVal) => {
  router.push(`/plan/sprint/${newVal}`)
  loadData(newVal)
})

watch(() => route.params.month, (newParam) => {
  if (newParam) {
    selectedMonth.value = newParam as string
  }
})

// Formatted month title label
const formattedMonthLabel = computed(() => {
  const [year, month] = selectedMonth.value.split('-').map(Number)
  return `${year}年${month}月`
})

// Data Source
const teamSprints = computed(() => todoStore.teamSprints)
const dailyCategories = computed(() => todoStore.sortedCategories)

// Filtered Sprints
const filteredSprints = computed(() => {
  if (filterOnlyInvolved.value) {
    return teamSprints.value.filter(s => s.needInvolved)
  }
  return teamSprints.value
})

// Metrics
const totalSprints = computed(() => teamSprints.value.length)
const involvedSprints = computed(() => teamSprints.value.filter(s => s.needInvolved).length)
const completedInvolvedSprints = computed(() => teamSprints.value.filter(s => s.needInvolved && s.status === 'done').length)

const sprintCompletionRate = computed(() => {
  if (involvedSprints.value === 0) return 0
  return Math.round((completedInvolvedSprints.value / involvedSprints.value) * 100)
})

onMounted(() => {
  loadData(selectedMonth.value)
})

const shiftMonth = (offset: number) => {
  const [year, month] = selectedMonth.value.split('-').map(Number)
  const d = new Date(year, month - 1 + offset, 1)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const targetMonth = `${y}-${m}`
  
  if (offset > 0 && targetMonth > currentMonthStr.value) {
    return
  }
  selectedMonth.value = targetMonth
}

const statusLabel = (status: string) => {
  if (status === 'done') return '已完成'
  if (status === 'in_progress') return '进行中'
  return '未开始'
}

const mousedownTarget = ref<EventTarget | null>(null)

// Modals States
const sprintModal = reactive({
  isOpen: false,
  isEdit: false,
  id: 0,
  title: '',
  status: 'not_started' as 'not_started' | 'in_progress' | 'done',
  remark: '',
  keyResultIds: [] as number[],
  needInvolved: false
})

// --- Sprint Operations ---
const openSprintModal = (sprint: TeamSprint | null) => {
  if (sprint) {
    sprintModal.isEdit = true
    sprintModal.id = sprint.id
    sprintModal.title = sprint.title
    sprintModal.status = sprint.status
    sprintModal.remark = sprint.remark || ''
    sprintModal.keyResultIds = sprint.keyResultIds || []
    sprintModal.needInvolved = sprint.needInvolved
  } else {
    sprintModal.isEdit = false
    sprintModal.id = 0
    sprintModal.title = ''
    sprintModal.status = 'not_started'
    sprintModal.remark = ''
    sprintModal.keyResultIds = []
    sprintModal.needInvolved = false
  }
  sprintModal.isOpen = true
}

const saveSprint = async () => {
  if (!authStore.currentUser) return
  
  try {
    if (sprintModal.isEdit) {
      // 1. Update title/remark
      await todoStore.updateTeamSprint(authStore.currentUser.userId, sprintModal.id, {
        title: sprintModal.title.trim(),
        remark: sprintModal.remark.trim()
      })
      const event = new CustomEvent('app-toast', { detail: { text: '冲刺任务修改成功！' } })
      window.dispatchEvent(event)
    } else {
      await todoStore.addTeamSprint(
        authStore.currentUser.userId,
        sprintModal.title.trim(),
        sprintModal.remark.trim()
      )
      const event = new CustomEvent('app-toast', { detail: { text: '成功创建冲刺任务！' } })
      window.dispatchEvent(event)
    }
    sprintModal.isOpen = false
  } catch (err: any) {
    // Error is handled globally by axios interceptor toast
  }
}

const toggleInvolved = (sprint: TeamSprint) => {
  if (!authStore.currentUser) return
  todoStore.updateTeamSprint(authStore.currentUser.userId, sprint.id, { needInvolved: !sprint.needInvolved })
  
  const text = !sprint.needInvolved ? '已标记为需我介入跟进！' : '取消介入跟进'
  const event = new CustomEvent('app-toast', { detail: { text } })
  window.dispatchEvent(event)
}

const toggleSprintStatus = async (sprint: TeamSprint) => {
  if (!authStore.currentUser) return
  
  if (!sprint.needInvolved) {
    const event = new CustomEvent('app-toast', { detail: { text: '无需我介入的冲刺任务不可更改状态', type: 'error' } })
    window.dispatchEvent(event)
    return
  }
  
  if (sprint.keyResultIds && sprint.keyResultIds.length > 0) {
    const event = new CustomEvent('app-toast', { detail: { text: '已关联关键成果的冲刺任务状态由关键成果联动，不可手动变更', type: 'error' } })
    window.dispatchEvent(event)
    return
  }

  let newStatus: 'not_started' | 'in_progress' | 'done' = 'in_progress'
  let toastText = ''
  
  if (sprint.status === 'not_started') {
    newStatus = 'in_progress'
    toastText = '已开始执行冲刺任务！'
  } else if (sprint.status === 'in_progress') {
    newStatus = 'done'
    toastText = '已完成此冲刺任务！'
  } else if (sprint.status === 'done') {
    newStatus = 'in_progress'
    toastText = '已重置此冲刺任务为进行中。'
  }

  try {
    await todoStore.updateTeamSprint(authStore.currentUser.userId, sprint.id, {
      status: newStatus
    })
    const event = new CustomEvent('app-toast', { detail: { text: toastText } })
    window.dispatchEvent(event)
  } catch (err: any) {
    // Handled globally by axios response interceptor
  }
}

const confirmModal = reactive({
  isOpen: false,
  title: '',
  messageLines: [] as string[],
  onConfirm: null as (() => void | Promise<void>) | null
})

const executeConfirm = async () => {
  if (confirmModal.onConfirm) {
    try {
      await confirmModal.onConfirm()
    } catch (e) {
      console.error(e)
    }
  }
  confirmModal.isOpen = false
}

const showConfirm = (title: string, message: string, onConfirm: () => void | Promise<void>) => {
  confirmModal.title = title
  confirmModal.messageLines = message.split('\n')
  confirmModal.onConfirm = onConfirm
  confirmModal.isOpen = true
}

const handleDeleteSprint = (sprint: TeamSprint) => {
  if (!authStore.currentUser) return
  const userId = authStore.currentUser.userId
  
  let confirmMsg = `确定要删除团队冲刺任务【${sprint.title}】吗？`
  const warnings: string[] = []
  
  if (sprint.keyResultIds && sprint.keyResultIds.length > 0) {
    warnings.push(`⚠️ 该任务已关联 ${sprint.keyResultIds.length} 项关键成果，删除后关联关系将一并解除！`)
  }
  if (sprint.status === 'done') {
    warnings.push(`⚠️ 该任务目前处于【已完成】状态。`)
  }
  
  if (warnings.length > 0) {
    confirmMsg += '\n' + warnings.join('\n')
  }
  
  showConfirm('删除冲刺任务', confirmMsg, async () => {
    await todoStore.deleteTeamSprint(userId, sprint.id)
    const event = new CustomEvent('app-toast', { detail: { text: '冲刺任务已删除' } })
    window.dispatchEvent(event)
  })
}

// --- Link Key Results Operations ---
const linkKRModal = reactive({
  isOpen: false,
  sprintId: 0,
  sprintTitle: '',
  selectedKRIds: [] as number[]
})

const selectableKRsGrouped = computed(() => {
  const map: Record<number, any[]> = {}
  todoStore.performanceKRs.forEach(kr => {
    if (kr.status !== 'cancelled') {
      if (!map[kr.categoryId]) {
        map[kr.categoryId] = []
      }
      map[kr.categoryId].push(kr)
    }
  })
  
  return todoStore.performanceCategories.map(cat => {
    return {
      id: cat.id,
      name: cat.name,
      krs: map[cat.id] || []
    }
  }).filter(group => group.krs.length > 0)
})

const openLinkKRModal = async (sprint: TeamSprint) => {
  if (!sprint.needInvolved) return
  if (!authStore.currentUser) return
  
  try {
    // Only call refreshObjectives when user clicks to bind key results
    await todoStore.refreshObjectives(authStore.currentUser.userId)
    
    linkKRModal.sprintId = sprint.id
    linkKRModal.sprintTitle = sprint.title
    linkKRModal.selectedKRIds = [...(sprint.keyResultIds || [])]
    linkKRModal.isOpen = true
  } catch (e) {
    console.error('Failed to load performance key results for sprint task:', e)
  }
}

const saveLinkKRs = async () => {
  if (!authStore.currentUser) return
  
  try {
    await todoStore.linkSprintKeyResults(
      authStore.currentUser.userId,
      linkKRModal.sprintId,
      linkKRModal.selectedKRIds
    )
    const event = new CustomEvent('app-toast', { detail: { text: '成功绑定关键成果！' } })
    window.dispatchEvent(event)
    linkKRModal.isOpen = false
  } catch (err: any) {
    // Handled globally
  }
}

</script>

<style scoped>
.sprint-view {
  max-width: 1100px;
  margin: 0 auto;
  padding-bottom: 60px;
  color: #334155;
  font-family: var(--font-sans);
}

/* 1. Header Styles */
.view-header {
  background-color: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 24px 32px;
  margin-bottom: 24px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
}
.title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
}
.title-col h2 {
  font-size: 22px;
  font-weight: 800;
  color: var(--text-main);
  margin-bottom: 4px;
}
.subtitle-desc {
  font-size: 13.5px;
  color: var(--text-muted);
}
.actions-col {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.month-selector-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  background-color: #fff;
  border: 1.5px solid var(--border-medium);
  border-radius: var(--radius-md);
  padding: 4px 8px;
  height: 42px;
}
.month-label {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-main);
  min-width: 90px;
  text-align: center;
}
.nav-arrow-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  border-radius: 50%;
  transition: background var(--transition-fast);
}
.nav-arrow-btn:hover {
  background-color: var(--border-light);
  color: var(--text-main);
}
.nav-arrow-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.nav-arrow-btn:disabled:hover {
  background: none;
  color: var(--text-muted);
}
.nav-arrow-btn svg {
  width: 16px;
  height: 16px;
}
.add-objective-btn {
  background-color: #2563eb;
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  padding: 0 20px;
  height: 42px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: background var(--transition-fast);
}
.add-objective-btn:hover {
  background-color: #1d4ed8;
}
.add-objective-btn .btn-icon {
  width: 16px;
  height: 16px;
}

/* 2. Monthly Total Progress Card */
.monthly-total-progress-card {
  background-color: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 16px 24px;
  margin-bottom: 20px;
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.progress-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}
.progress-title-lbl {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-main);
}
.progress-percentage-val {
  font-size: 26px;
  font-weight: 800;
  color: #2563eb;
  line-height: 1;
}
.main-progress-container-bar {
  height: 8px;
  background-color: #f1f5f9;
  border-radius: var(--radius-full);
  overflow: hidden;
  position: relative;
}
.main-progress-fill-bar {
  height: 100%;
  background-color: #2563eb;
  border-radius: var(--radius-full);
  transition: width 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}
.progress-footer-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: var(--text-muted);
  flex-wrap: wrap;
  gap: 10px;
}
.update-time-lbl {
  font-weight: 500;
}

/* 3. Filter Bar */
.sprint-filter-bar {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  margin-bottom: 16px;
  padding: 0 4px;
}
.involved-toggle-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
}
.toggle-lbl {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--text-main);
}
.toggle-control {
  position: relative;
  display: inline-block;
  width: 44px;
  height: 24px;
  cursor: pointer;
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

/* 4. Sprint Checklist Container */
.objectives-list-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.obj-list-wrapper {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.objective-card {
  background-color: var(--bg-card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
}

/* 5. Item Row styles (aligned with KR design) */
.key-results-sub-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.kr-items-wrapper {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.kr-item-box-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  background-color: var(--bg-card);
  border: 1px solid var(--border-medium);
  border-radius: var(--radius-md);
  padding: 18px 24px;
  transition: all var(--transition-fast);
}
.kr-item-box-row:hover {
  box-shadow: var(--shadow-sm);
  border-color: #cbd5e1;
}
.kr-item-box-row.completed {
  background-color: #fafbfc;
}
.kr-item-box-row.blocked {
  border-left: 4px solid var(--danger);
  background-color: rgba(239, 68, 68, 0.01);
}

/* Custom indicator checkbox circle */
.kr-status-icon-wrap {
  padding-top: 2px;
  cursor: pointer;
  flex-shrink: 0;
}
.kr-status-icon-wrap.disabled {
  cursor: not-allowed;
}
.custom-indicator-circle {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 2px solid var(--border-medium);
  background-color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}
.custom-indicator-circle.disabled {
  background-color: #f1f5f9;
  border-color: #e2e8f0;
  cursor: not-allowed;
  opacity: 0.6;
}
.custom-indicator-circle.in_progress {
  border-color: #2563eb;
  position: relative;
  background-color: #f8fafc;
}
.custom-indicator-circle.in_progress .spin-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #2563eb;
  animation: pulse-dot 1.6s infinite ease-in-out;
}
.custom-indicator-circle.done {
  border-color: #10b981;
  background-color: #10b981;
  color: #fff;
}

@keyframes pulse-dot {
  0%, 100% { transform: scale(0.8); opacity: 0.5; }
  50% { transform: scale(1.25); opacity: 1; }
}
.check-mark {
  font-size: 12px;
  font-weight: 800;
  line-height: 1;
}

/* Item details */
.kr-main-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.kr-title-heading {
  font-size: 14.5px;
  font-weight: 700;
  color: var(--text-main);
  line-height: 1.4;
}
.completed .kr-title-heading {
  text-decoration: line-through;
  color: var(--text-muted);
}
.kr-time-meta {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 600;
}
.meta-item-lbl {
  color: #64748b;
  background-color: #f1f5f9;
  padding: 2px 8px;
  border-radius: 4px;
}
.kr-remark-content {
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.4;
}

/* Actions panel aligned right */
.kr-right-meta-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
  flex-shrink: 0;
}
.kr-status-tag {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: var(--radius-sm);
}
.kr-status-tag.in_progress {
  background-color: #eff6ff;
  color: #2563eb;
}
.kr-status-tag.done {
  background-color: #e6fcf5;
  color: #0ca678;
}
.kr-status-tag.not_started {
  background-color: #f1f5f9;
  color: #64748b;
}
.kr-status-tag.not_involved {
  background-color: #f3f4f6;
  color: #9ca3af;
}

.kr-row-action-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
}
.kr-action-btn-item {
  background: none;
  border: 1px solid var(--border-medium);
  background-color: #fff;
  cursor: pointer;
  color: var(--text-muted);
  padding: 6px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}
.kr-action-btn-item:hover {
  background-color: var(--border-light);
  color: var(--text-main);
  border-color: var(--border-medium);
}
.kr-action-btn-item.delete:hover {
  background-color: var(--danger-bg);
  color: var(--danger);
  border-color: rgba(239, 68, 68, 0.2);
}
.kr-action-btn-item svg {
  width: 15px;
  height: 15px;
}

/* Involved & Carry forward button styles */
.involved-action-btn-item {
  background-color: #fff;
  border: 1.5px solid var(--border-medium);
  border-radius: var(--radius-sm);
  padding: 5px 10px;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--text-muted);
  cursor: pointer;
  transition: all var(--transition-fast);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
}
.involved-action-btn-item:hover {
  border-color: var(--primary);
  color: var(--primary);
  background-color: var(--primary-light);
}
.involved-action-btn-item.active {
  background-color: var(--primary);
  border-color: var(--primary);
  color: #fff;
}
.involved-action-btn-item.carry-forward-btn:hover {
  border-color: var(--warning);
  color: var(--warning);
  background-color: var(--warning-bg);
}

/* Modals */
.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background-color: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(4px);
  z-index: 2500;
  display: flex;
  align-items: center;
  justify-content: center;
}
.modal-content {
  background-color: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 32px;
  width: 100%;
  max-width: 440px;
  box-shadow: var(--shadow-lg);
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.modal-content h3 {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-main);
  border-bottom: 1px solid var(--border-light);
  padding-bottom: 8px;
}
.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
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
textarea.form-control {
  resize: vertical;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 10px;
}
.btn {
  padding: 10px 20px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
  border: none;
}
.btn-secondary {
  background-color: var(--border-medium);
  color: var(--text-muted);
}
.btn-secondary:hover {
  background-color: #cbd5e1;
  color: var(--text-main);
}
.btn-primary {
  background-color: var(--primary);
  color: #fff;
}

.derive-modal-content {
  max-width: 520px;
}
.derive-header {
  display: flex;
  align-items: center;
  gap: 8px;
  border-bottom: 1.5px solid var(--primary-light);
  padding-bottom: 12px;
}
.derive-icon {
  font-size: 22px;
}
.derive-header h3 {
  border: none;
  padding: 0;
  color: var(--primary);
}
.derive-tip-desc {
  font-size: 13px;
  line-height: 1.5;
  color: var(--text-muted);
}
</style>
