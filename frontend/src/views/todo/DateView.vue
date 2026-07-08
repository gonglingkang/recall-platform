<template>
  <div class="calendar-view">
    <!-- View Header (Premium Style) -->
    <div class="view-header premium-card">
      <div class="title-row" style="display: flex; justify-content: space-between; align-items: center; width: 100%; flex-wrap: wrap; gap: 16px;">
        <div class="title-meta-left">
          <h2>待办日历视图</h2>
          <p class="subtitle-lbl">按月排程直观预览每日待办事项</p>
        </div>
        <!-- Right side actions -->
        <div class="header-actions" style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap;">
          <!-- Month nav controls -->
          <div class="month-nav" style="display: flex; align-items: center; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background: #fff; padding: 2px; overflow: hidden; height: 42px;">
            <button class="nav-arrow-btn" @click.stop="shiftMonth(-1)" title="上一个月" style="border: none; background: none; width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; cursor: pointer; color: var(--text-muted); border-radius: 4px; transition: all 0.2s;">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" style="width: 16px; height: 16px;">
                <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 19.5L8.25 12l7.5-7.5" />
              </svg>
            </button>
            <span style="font-size: 14.5px; font-weight: 700; color: var(--text-main); padding: 0 16px; min-width: 100px; text-align: center; user-select: none;">
              {{ currentYear }}年{{ currentMonth }}月
            </span>
            <button 
              class="nav-arrow-btn" 
              @click.stop="shiftMonth(1)" 
              title="下一个月" 
              :disabled="isNextMonthDisabled"
              :style="{ 
                border: 'none', 
                background: 'none', 
                width: '36px', 
                height: '36px', 
                display: 'flex', 
                alignItems: 'center', 
                justifyContent: 'center', 
                color: isNextMonthDisabled ? 'var(--text-light)' : 'var(--text-muted)', 
                borderRadius: '4px', 
                transition: 'all 0.2s',
                opacity: isNextMonthDisabled ? 0.35 : 1,
                cursor: isNextMonthDisabled ? 'not-allowed' : 'pointer'
              }"
            >
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" style="width: 16px; height: 16px;">
                <path stroke-linecap="round" stroke-linejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Calendar Grid Container -->
    <div class="calendar-container premium-card" style="padding: 0; overflow: hidden; border: 1px solid var(--border-light); border-radius: var(--radius-lg); background: #fff; box-shadow: var(--shadow-sm); margin-bottom: 30px;">
      <!-- Weekday Headers -->
      <div class="weekday-header-grid" style="display: grid; grid-template-columns: repeat(7, 1fr); background: #f8fafc; border-bottom: 1px solid var(--border-light); text-align: center;">
        <div v-for="dayName in ['周日', '周一', '周二', '周三', '周四', '周五', '周六']" :key="dayName" style="padding: 14px 0; font-size: 14px; font-weight: 700; color: var(--text-muted);">
          {{ dayName }}
        </div>
      </div>

      <!-- Days Grid -->
      <div class="days-grid" style="display: grid; grid-template-columns: repeat(7, 1fr); background: var(--border-light); gap: 1px;">
        <div 
          v-for="cell in calendarDays" 
          :key="cell.dateStr"
          class="calendar-cell"
          :class="{ 'other-month': !cell.isCurrentMonth, 'is-today': cell.isToday, 'has-todos': (monthlyTodosMap[cell.dateStr] || []).length > 0 }"
          @click="(monthlyTodosMap[cell.dateStr] || []).length > 0 ? openDayListModal(cell.dateStr) : null"
          :style="{ cursor: (monthlyTodosMap[cell.dateStr] || []).length > 0 ? 'pointer' : 'default' }"
          style="min-height: 120px; background: #fff; padding: 10px; display: flex; flex-direction: column; gap: 6px; transition: background-color 0.15s ease;"
        >
          <!-- Day Number -->
          <div class="cell-day-num" style="font-size: 13.5px; font-weight: 700; color: var(--text-main);" :style="{ opacity: cell.isCurrentMonth ? 1 : 0.45 }">
            {{ cell.day }}
          </div>

          <!-- Todo Items List -->
          <div class="cell-todos-list" style="display: flex; flex-direction: column; gap: 4px; flex: 1; overflow: hidden;">
            <template v-if="(monthlyTodosMap[cell.dateStr] || []).length <= 3">
              <div 
                v-for="todo in (monthlyTodosMap[cell.dateStr] || [])"
                :key="todo.id"
                class="todo-capsule"
                :class="{ 'todo-capsule-done': todo.status === 'done' }"
                :style="getTodoCapsuleStyle(todo)"
                style="padding: 4px 8px; border-radius: 4px; font-size: 11.5px; font-weight: 600; text-overflow: ellipsis; overflow: hidden; white-space: nowrap; transition: all 0.15s ease;"
                :title="todo.title"
              >
                {{ todo.title }}
              </div>
            </template>
            <template v-else>
              <div 
                v-for="todo in (monthlyTodosMap[cell.dateStr] || []).slice(0, 2)"
                :key="todo.id"
                class="todo-capsule"
                :class="{ 'todo-capsule-done': todo.status === 'done' }"
                :style="getTodoCapsuleStyle(todo)"
                style="padding: 4px 8px; border-radius: 4px; font-size: 11.5px; font-weight: 600; text-overflow: ellipsis; overflow: hidden; white-space: nowrap; transition: all 0.15s ease;"
                :title="todo.title"
              >
                {{ todo.title }}
              </div>
              <div 
                style="padding: 4px 8px; border-radius: 4px; font-size: 11px; font-weight: 700; color: #64748b; background: #f1f5f9; border: 1.5px dashed var(--border-medium); text-align: center; user-select: none;"
              >
                + {{ (monthlyTodosMap[cell.dateStr] || []).length - 2 }} 更多
              </div>
            </template>
          </div>
        </div>
      </div>
    </div>

    <!-- Day List Modal (Dialog Popup) -->
    <transition name="modal-fade">
      <div v-if="isDayListModalOpen" class="modal-overlay" @mousedown="handleOverlayMousedown" @click="handleOverlayClick($event, closeDayListModal)" style="position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(15, 23, 42, 0.4); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 1999;">
        <div class="modal-card" @click.stop style="background: #fff; border-radius: var(--radius-lg); width: 100%; max-width: 520px; box-shadow: var(--shadow-xl); border: 1px solid var(--border-light); padding: 28px; display: flex; flex-direction: column; gap: 20px; position: relative;">
          <!-- Modal Header -->
          <div class="modal-header" style="display: flex; justify-content: space-between; align-items: center;">
            <h2 style="font-size: 18px; font-weight: 800; color: var(--text-main); margin: 0;">
              {{ formatDateChinese(selectedCellDate) }} 待办列表
            </h2>
            <button class="close-btn" @click="closeDayListModal" style="background: none; border: none; cursor: pointer; color: var(--text-muted); display: flex; align-items: center; justify-content: center; padding: 4px; border-radius: 50%; transition: background-color 0.2s;">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" style="width: 18px; height: 18px;">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <!-- Modal Body (List of todos for this day) -->
          <div class="modal-body" style="max-height: 400px; overflow-y: auto; display: flex; flex-direction: column; gap: 16px; padding-right: 4px;">
            <div v-if="groupedDayTodos.length > 0" style="display: flex; flex-direction: column; gap: 16px;">
              <!-- Group Section -->
              <div 
                v-for="group in groupedDayTodos" 
                :key="group.id" 
                class="category-group-section"
                style="border-left: 3px solid; padding-left: 12px; display: flex; flex-direction: column; gap: 8px;"
                :style="{ borderLeftColor: group.color }"
              >
                <!-- Group Header -->
                <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 2px;">
                  <span style="font-size: 14px; font-weight: 800; color: var(--text-main);">{{ group.name }}</span>
                  <span style="font-size: 10px; font-weight: 700; padding: 1px 6px; border-radius: 10px; background-color: #f1f5f9; color: #475569;">
                    {{ group.totalCount }}
                  </span>
                </div>

                <!-- Todos directly in parent category -->
                <div v-if="group.todos.length > 0" style="display: flex; flex-direction: column; gap: 6px;">
                  <div 
                    v-for="todo in group.todos" 
                    :key="todo.id" 
                    @click="openEditModal(todo)"
                    class="todo-list-row"
                    style="display: flex; align-items: center; gap: 12px; padding: 10px 14px; border: 1px solid var(--border-light); border-radius: var(--radius-md); background: #f8fafc; cursor: pointer; transition: all 0.2s;"
                  >
                    <div style="flex: 1; min-width: 0; display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
                      <span 
                        style="font-size: 13.5px; font-weight: 600; color: var(--text-main); text-overflow: ellipsis; overflow: hidden; white-space: nowrap; max-width: 60%;"
                        :style="{ textDecoration: todo.status === 'done' ? 'line-through' : 'none', opacity: todo.status === 'done' ? 0.5 : 1 }"
                        :title="todo.title"
                      >
                        {{ todo.title }}
                      </span>
                      <span class="badge" :class="`badge-${todo.priority}`" style="font-size: 10px; margin: 0; padding: 1px 5px; flex-shrink: 0;">
                        {{ priorityLabel(todo.priority) }}
                      </span>
                      <span v-if="isHistorical(todo)" class="badge historical-badge" style="flex-shrink: 0; font-size: 10px; padding: 2px 6px; background-color: #fff2e8; border: 1px solid #ffbb96; color: #fa541c; font-weight: 600;" :title="`这是历史待办（创建于${getHistoricalDateLabel(todo)}），请优先处理！`">
                        📅 {{ getHistoricalDateLabel(todo) }}
                      </span>
                    </div>
                  </div>
                </div>

                <!-- Subcategory groups -->
                <div v-if="group.subgroups.length > 0" style="display: flex; flex-direction: column; gap: 8px; margin-top: 2px;">
                  <div 
                    v-for="subgroup in group.subgroups" 
                    :key="subgroup.id"
                    style="display: flex; flex-direction: column; gap: 6px; padding-left: 8px;"
                  >
                    <div style="display: flex; align-items: center; gap: 6px; font-size: 12px; font-weight: 700; color: var(--text-muted);">
                      <span style="display: inline-block; width: 4px; height: 4px; border-radius: 50%;" :style="{ backgroundColor: group.color }"></span>
                      <span>{{ subgroup.name }}</span>
                    </div>
                    <div style="display: flex; flex-direction: column; gap: 6px;">
                      <div 
                        v-for="todo in subgroup.todos" 
                        :key="todo.id" 
                        @click="openEditModal(todo)"
                        class="todo-list-row"
                        style="display: flex; align-items: center; gap: 12px; padding: 10px 14px; border: 1px solid var(--border-light); border-radius: var(--radius-md); background: #f8fafc; cursor: pointer; transition: all 0.2s;"
                      >
                        <div style="flex: 1; min-width: 0; display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
                          <span 
                            style="font-size: 13.5px; font-weight: 600; color: var(--text-main); text-overflow: ellipsis; overflow: hidden; white-space: nowrap; max-width: 60%;"
                            :style="{ textDecoration: todo.status === 'done' ? 'line-through' : 'none', opacity: todo.status === 'done' ? 0.5 : 1 }"
                            :title="todo.title"
                          >
                            {{ todo.title }}
                          </span>
                          <span class="badge" :class="`badge-${todo.priority}`" style="font-size: 10px; margin: 0; padding: 1px 5px; flex-shrink: 0;">
                            {{ priorityLabel(todo.priority) }}
                          </span>
                          <span v-if="isHistorical(todo)" class="badge historical-badge" style="flex-shrink: 0; font-size: 10px; padding: 2px 6px; background-color: #fff2e8; border: 1px solid #ffbb96; color: #fa541c; font-weight: 600;" :title="`这是历史待办（创建于${getHistoricalDateLabel(todo)}），请优先处理！`">
                            📅 {{ getHistoricalDateLabel(todo) }}
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <!-- Empty State -->
            <div v-else style="text-align: center; padding: 30px 0; color: var(--text-muted);">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width: 40px; height: 40px; margin: 0 auto 10px; opacity: 0.5;">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5A2.25 2.25 0 0121 11.25v7.5" />
              </svg>
              <h3 style="font-size: 14.5px; font-weight: 700; color: var(--text-main); margin: 0 0 4px;">该日期无待办事项</h3>
              <p style="font-size: 12px; margin: 0;">点击下方按钮安排任务</p>
            </div>
          </div>

          <!-- Space padding bottom -->
          <div style="height: 10px;"></div>
        </div>
      </div>
    </transition>

    <!-- Edit Detail Modal (Read-Only Popup) -->
    <transition name="modal-fade">
      <div v-if="isModalOpen" class="modal-overlay" @mousedown="handleOverlayMousedown" @click="handleOverlayClick($event, closeModal)" style="position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(15, 23, 42, 0.4); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 2000;">
        <div class="modal-card" @click.stop style="background: #fff; border-radius: var(--radius-lg); width: 100%; max-width: 520px; box-shadow: var(--shadow-xl); border: 1px solid var(--border-light); padding: 28px; display: flex; flex-direction: column; gap: 24px; position: relative;">
          <!-- Modal Header -->
          <div class="modal-header" style="display: flex; justify-content: space-between; align-items: center;">
            <h2 style="font-size: 18px; font-weight: 800; color: var(--text-main); margin: 0;">待办事项详情</h2>
            <button class="close-btn" @click="closeModal" style="background: none; border: none; cursor: pointer; color: var(--text-muted); display: flex; align-items: center; justify-content: center; padding: 4px; border-radius: 50%; transition: background-color 0.2s;">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" style="width: 18px; height: 18px;">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <div class="modal-form" style="display: flex; flex-direction: column; gap: 16px;">
            <!-- Title & Priority side by side -->
            <div style="display: grid; grid-template-columns: 2.2fr 1fr; gap: 16px; width: 100%;">
              <div class="form-field" style="display: flex; flex-direction: column; gap: 6px;">
                <label style="font-size: 13.5px; font-weight: 700; color: var(--text-main);">
                  待办标题 <span style="color: #ef4444; margin-left: 2px;">*</span>
                </label>
                <input 
                  v-model="editForm.title" 
                  type="text" 
                  disabled 
                  class="form-control" 
                  style="width: 100%; height: 42px; padding: 0 14px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); font-size: 14px; color: var(--text-main); cursor: not-allowed; background-color: #ffffff;" 
                />
              </div>
              <div class="form-field" style="display: flex; flex-direction: column; gap: 6px;">
                <label style="font-size: 13.5px; font-weight: 700; color: var(--text-main);">优先级</label>
                <!-- Readonly custom selector style -->
                <div style="display: flex; align-items: center; justify-content: space-between; padding: 0 16px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background: #ffffff; height: 42px; cursor: not-allowed; opacity: 1;">
                  <div style="display: flex; align-items: center; gap: 8px;">
                    <span style="display: inline-block; width: 8px; height: 8px; border-radius: 50%;" :style="{ backgroundColor: getEditPriorityColor }"></span>
                    <span style="font-size: 14px; font-weight: 500; color: var(--text-main);">{{ getEditPriorityLabel }}</span>
                  </div>
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.2" stroke="currentColor" style="width: 14px; height: 14px; color: var(--text-muted);">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
                  </svg>
                </div>
              </div>
            </div>

            <div class="form-field" style="display: flex; flex-direction: column; gap: 6px;">
              <label style="font-size: 13.5px; font-weight: 700; color: var(--text-main);">备注 (详细描述)</label>
              <textarea 
                v-model="editForm.content" 
                rows="3" 
                disabled 
                class="form-control" 
                style="width: 100%; padding: 12px 14px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); font-size: 14px; color: var(--text-main); cursor: not-allowed; background-color: #ffffff; resize: none;"
              ></textarea>
            </div>

            <div class="form-field" style="display: flex; flex-direction: column; gap: 6px;">
              <label style="font-size: 13.5px; font-weight: 700; color: var(--text-main);">归属分类</label>
              <!-- Readonly custom selector style -->
              <div style="display: flex; align-items: center; justify-content: space-between; padding: 0 16px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background: #ffffff; height: 42px; cursor: not-allowed; opacity: 1;">
                <div style="display: flex; align-items: center; gap: 8px;">
                  <span style="display: inline-block; width: 8px; height: 8px; border-radius: 50%;" :style="{ backgroundColor: getEditCategoryColor }"></span>
                  <span style="font-size: 14px; font-weight: 500; color: var(--text-main);">{{ getEditCategoryName || '无归属分类' }}</span>
                </div>
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.2" stroke="currentColor" style="width: 14px; height: 14px; color: var(--text-muted);">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
                </svg>
              </div>
            </div>

            <!-- Creation / Completion Time Container (Gray Card) -->
            <div style="background-color: #f8fafc; border-radius: 8px; padding: 16px 20px; display: flex; flex-direction: column; gap: 10px; margin-top: 8px;">
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span style="color: #64748b; font-size: 14px; font-weight: 500;">创建时间：</span>
                <span style="color: #334155; font-size: 14px; font-weight: 500;">{{ formatDateTime(editForm.createdAt) }}</span>
              </div>
              <div v-if="editForm.status === 'done' && editForm.doneAt" style="display: flex; justify-content: space-between; align-items: center;">
                <span style="color: #64748b; font-size: 14px; font-weight: 500;">完成时间：</span>
                <span style="color: #10b981; font-size: 14px; font-weight: 500;">{{ formatDateTime(editForm.doneAt) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useTodoStore, mapTodoVOToTodo } from '@/stores/todo'
import type { Todo } from '@/stores/todo'
import request from '@/api/request'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const todoStore = useTodoStore()

// State
const currentYear = ref(new Date().getFullYear())
const currentMonth = ref(new Date().getMonth() + 1) // 1-based

const todayStr = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})

// Current month formatted string, e.g. "2024-06"
const currentMonthStr = computed(() => {
  return `${currentYear.value}-${String(currentMonth.value).padStart(2, '0')}`
})

// Parse parameters from route
function parseRouteParam() {
  const dateParam = route.params.date as string
  if (dateParam && dateParam.match(/^\d{4}-\d{2}-\d{2}$/)) {
    const parts = dateParam.split('-')
    currentYear.value = parseInt(parts[0])
    currentMonth.value = parseInt(parts[1])
  } else {
    const today = new Date()
    currentYear.value = today.getFullYear()
    currentMonth.value = today.getMonth() + 1
  }
}

// Watch route params to parse dates and fetch monthly calendar data
watch(() => route.params.date, () => {
  parseRouteParam()
  if (authStore.currentUser) {
    fetchMonthlyTodos()
  }
}, { immediate: true })

onMounted(() => {
  if (authStore.currentUser) {
    todoStore.refreshCategories(authStore.currentUser.userId)
  }
})

// Categories
const categories = computed(() => todoStore.sortedCategories)

// Monthly todos map fetched from the calendar endpoint
const monthlyTodosMap = ref<Record<string, Todo[]>>({})

async function fetchMonthlyTodos() {
  if (!authStore.currentUser) return
  try {
    if (!todoStore.categories || todoStore.categories.length === 0) {
      await todoStore.refreshCategories(authStore.currentUser.userId)
    }
    const res = await request.get<any, any>(`/api/todos/month?month=${currentMonthStr.value}`)
    const map: Record<string, Todo[]> = {}
    if (res.data && res.data.days) {
      res.data.days.forEach((group: any) => {
        const dateStr = group.date
        const dayTodos = (group.todos || []).map((vo: any) => 
          mapTodoVOToTodo(vo, categories.value, todoStore.subcategoriesMap)
        )
        map[dateStr] = dayTodos
      })
    }
    monthlyTodosMap.value = map
  } catch (e) {
    console.error('Failed to fetch monthly calendar:', e)
  }
}

// Calendar Grid Generation
const calendarDays = computed(() => {
  const year = currentYear.value
  const month = currentMonth.value
  
  // First day of month
  const firstDay = new Date(year, month - 1, 1)
  const startDayOfWeek = firstDay.getDay() // 0 = Sunday, 1 = Monday, ...
  
  // Days in month
  const totalDays = new Date(year, month, 0).getDate()
  
  // Days in previous month
  const prevMonthTotalDays = new Date(year, month - 1, 0).getDate()
  
  const days = []
  
  // Previous month padding
  for (let i = startDayOfWeek - 1; i >= 0; i--) {
    const d = prevMonthTotalDays - i
    const prevMonth = month === 1 ? 12 : month - 1
    const prevYear = month === 1 ? year - 1 : year
    const dateStr = `${prevYear}-${String(prevMonth).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    days.push({
      day: d,
      dateStr,
      isCurrentMonth: false,
      isToday: dateStr === todayStr.value
    })
  }
  
  // Current month days
  for (let d = 1; d <= totalDays; d++) {
    const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    days.push({
      day: d,
      dateStr,
      isCurrentMonth: true,
      isToday: dateStr === todayStr.value
    })
  }
  
  // Next month padding
  const remaining = days.length <= 35 ? 35 - days.length : 42 - days.length
  for (let d = 1; d <= remaining; d++) {
    const nextMonth = month === 12 ? 1 : month + 1
    const nextYear = month === 12 ? year + 1 : year
    const dateStr = `${nextYear}-${String(nextMonth).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    days.push({
      day: d,
      dateStr,
      isCurrentMonth: false,
      isToday: dateStr === todayStr.value
    })
  }
  
  return days
})

// Month navigation limit
const isNextMonthDisabled = computed(() => {
  const today = new Date()
  return currentYear.value > today.getFullYear() || 
    (currentYear.value === today.getFullYear() && currentMonth.value >= today.getMonth() + 1)
})

// Month selection helpers
const shiftMonth = (offset: number) => {
  let m = currentMonth.value + offset
  let y = currentYear.value
  if (m < 1) {
    m = 12
    y -= 1
  } else if (m > 12) {
    m = 1
    y += 1
  }
  
  // Enforce month upper bound (cannot go beyond today's month)
  const today = new Date()
  const todayYear = today.getFullYear()
  const todayMonth = today.getMonth() + 1
  if (y > todayYear || (y === todayYear && m > todayMonth)) {
    return
  }
  
  // Sync router path; watch on route.params.date will handle updating local state and fetching data
  const targetDate = `${y}-${String(m).padStart(2, '0')}-01`
  router.push(`/date/${targetDate}`)
}

const goToToday = () => {
  router.push(`/date/${todayStr.value}`)
}

// Day List Modal State
const isDayListModalOpen = ref(false)
const selectedCellDate = ref('')
const dayTodos = computed(() => monthlyTodosMap.value[selectedCellDate.value] || [])

// Grouped day todos by Category (Parent & Subcategory)
interface DayTodoGroup {
  id: number | 'uncategorized'
  name: string
  color: string
  todos: Todo[]
  subgroups: {
    id: number
    name: string
    todos: Todo[]
  }[]
  totalCount: number
}

const groupedDayTodos = computed(() => {
  const groupsMap: Record<string | number, DayTodoGroup> = {}
  
  // Uncategorized group
  const uncategorizedGroup: DayTodoGroup = {
    id: 'uncategorized',
    name: '未分类',
    color: '#64748b',
    todos: [],
    subgroups: [],
    totalCount: 0
  }

  dayTodos.value.forEach(todo => {
    if (todo.categoryId === null) {
      uncategorizedGroup.todos.push(todo)
      uncategorizedGroup.totalCount++
      return
    }

    const parentId = todo.categoryId
    const parentCat = categories.value.find(c => c.id === parentId)
    const catName = parentCat ? parentCat.name : '未知分类'
    const catColor = parentCat ? parentCat.color : '#64748b'

    if (!groupsMap[parentId]) {
      groupsMap[parentId] = {
        id: parentId,
        name: catName,
        color: catColor,
        todos: [],
        subgroups: [],
        totalCount: 0
      }
    }

    const group = groupsMap[parentId]
    group.totalCount++

    if (todo.subcategoryId === null) {
      group.todos.push(todo)
    } else {
      const subId = todo.subcategoryId
      const subList = todoStore.subcategoriesByCategoryId(parentId)
      const subCat = subList.find(s => s.id === subId)
      const subName = subCat ? subCat.name : '未知子分类'

      let subgroup = group.subgroups.find(sg => sg.id === subId)
      if (!subgroup) {
        subgroup = {
          id: subId,
          name: subName,
          todos: []
        }
        group.subgroups.push(subgroup)
      }
      subgroup.todos.push(todo)
    }
  })

  // Order groups: categories list order, then uncategorized
  const result: DayTodoGroup[] = []
  
  categories.value.forEach(cat => {
    if (groupsMap[cat.id]) {
      // Sort subgroups by subcategories list order
      const subList = todoStore.subcategoriesByCategoryId(cat.id)
      groupsMap[cat.id].subgroups.sort((a, b) => {
        const idxA = subList.findIndex(s => s.id === a.id)
        const idxB = subList.findIndex(s => s.id === b.id)
        return idxA - idxB
      })
      result.push(groupsMap[cat.id])
    }
  })

  if (uncategorizedGroup.todos.length > 0) {
    result.push(uncategorizedGroup)
  }

  return result
})

const openDayListModal = (dateStr: string) => {
  selectedCellDate.value = dateStr
  isDayListModalOpen.value = true
}

const closeDayListModal = () => {
  isDayListModalOpen.value = false
}

const formatDateChinese = (dateStr: string) => {
  if (!dateStr) return ''
  const parts = dateStr.split('-')
  if (parts.length < 3) return dateStr
  const m = parseInt(parts[1])
  const d = parseInt(parts[2])
  return `${m}月${d}日`
}

// Details Modal State
const isModalOpen = ref(false)

const editForm = reactive({
  id: 0,
  title: '',
  content: '',
  planDate: '',
  priority: 'medium' as 'high' | 'medium' | 'low',
  categoryId: null as number | null,
  subcategoryId: null as number | null,
  reminderTime: '',
  createdAt: '',
  status: 'pending' as 'pending' | 'done' | 'deleted',
  doneAt: null as string | null
})

const getEditCategoryColor = computed(() => {
  if (!editForm.categoryId) return '#cbd5e1'
  const cat = todoStore.categories.find(c => c.id === editForm.categoryId)
  return cat ? cat.color : '#cbd5e1'
})

const getEditCategoryName = computed(() => {
  if (editForm.categoryId === null) return ''
  const cat = todoStore.categories.find(c => c.id === editForm.categoryId)
  if (!cat) return ''
  
  if (editForm.subcategoryId !== null) {
    const subList = todoStore.subcategoriesByCategoryId(editForm.categoryId)
    const sub = subList.find(s => s.id === editForm.subcategoryId)
    if (sub) {
      return `${cat.name} > ${sub.name}`
    }
  }
  return cat.name
})

const getEditPriorityColor = computed(() => {
  if (editForm.priority === 'high') return '#ef4444'
  if (editForm.priority === 'medium') return '#eab308'
  return '#22c55e'
})

const getEditPriorityLabel = computed(() => {
  if (editForm.priority === 'high') return '高优先级'
  if (editForm.priority === 'medium') return '中优先级'
  return '低优先级'
})

const formatDateTime = (dateTimeStr: string | null) => {
  if (!dateTimeStr) return '-'
  try {
    const date = new Date(dateTimeStr)
    if (isNaN(date.getTime())) return dateTimeStr
    const m = date.getMonth() + 1
    const d = date.getDate()
    const hh = String(date.getHours()).padStart(2, '0')
    const mm = String(date.getMinutes()).padStart(2, '0')
    return `${m}月${d}日 ${hh}:${mm}`
  } catch (e) {
    return dateTimeStr
  }
}

const openEditModal = (todo: Todo) => {
  editForm.id = todo.id
  editForm.title = todo.title
  editForm.content = todo.content || ''
  editForm.planDate = todo.planDate
  editForm.priority = todo.priority
  editForm.categoryId = todo.categoryId
  editForm.subcategoryId = todo.subcategoryId
  if (todo.reminderTime) {
    const [d, t] = todo.reminderTime.split(' ')
    editForm.reminderTime = `${d}T${t}`
  } else {
    editForm.reminderTime = ''
  }
  editForm.createdAt = todo.createdAt || ''
  editForm.status = todo.status
  editForm.doneAt = todo.doneAt || null
  
  isModalOpen.value = true
}

const closeModal = () => {
  isModalOpen.value = false
}

const truncate = (text: string, len: number) => {
  if (text.length <= len) return text
  return text.substring(0, len) + '...'
}

const priorityLabel = (prio: string) => {
  if (prio === 'high') return '高'
  if (prio === 'medium') return '中'
  return '低'
}

const getHistoricalDateLabel = (todo: Todo) => {
  if (!todo.createdAt) return ''
  const dateObj = new Date(todo.createdAt)
  const month = dateObj.getMonth() + 1
  const date = dateObj.getDate()
  return `${month}月${date}日`
}

const isHistorical = (todo: Todo) => {
  if (!todo.createdAt || !selectedCellDate.value) return false
  const createdDateStr = todo.createdAt.split('T')[0]
  return createdDateStr < selectedCellDate.value
}

const getCategoryName = (todo: Todo) => {
  if (!todo.categoryId) return ''
  const cat = categories.value.find(c => c.id === todo.categoryId)
  if (!cat) return ''
  
  const subList = todoStore.subcategoriesByCategoryId(todo.categoryId)
  const sub = subList.find(s => s.id === todo.subcategoryId)
  return sub ? `${cat.name} > ${sub.name}` : cat.name
}

const getCategoryStyle = (todo: Todo) => {
  if (!todo.categoryId) return {}
  const cat = categories.value.find(c => c.id === todo.categoryId)
  if (!cat) return {}
  return {
    backgroundColor: `${cat.color}15`,
    color: cat.color,
    border: `1px solid ${cat.color}30`
  }
}

// Helpers for grid cells
const getTodoCapsuleStyle = (todo: Todo) => {
  if (todo.categoryId) {
    const cat = categories.value.find(c => c.id === todo.categoryId)
    if (cat) {
      return {
        backgroundColor: `${cat.color}12`,
        color: cat.color,
        borderLeft: `3px solid ${cat.color}`,
        border: `1px solid ${cat.color}25`,
        borderLeftWidth: '3px'
      }
    }
  }
  return {
    backgroundColor: '#f8fafc',
    color: '#475569',
    borderLeft: `3px solid #94a3b8`,
    border: `1px solid #e2e8f0`,
    borderLeftWidth: '3px'
  }
}

let mousedownTarget: any = null
const handleOverlayMousedown = (e: MouseEvent) => {
  mousedownTarget = e.target
}
const handleOverlayClick = (e: MouseEvent, closeFn: () => void) => {
  if (e.target === e.currentTarget && mousedownTarget === e.currentTarget) {
    closeFn()
  }
}
</script>

<style scoped>
.calendar-view {
  max-width: 1100px;
  margin: 0 auto;
}

/* Header Styles */
.view-header {
  background-color: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 24px 32px;
  margin-bottom: 24px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
}

.title-row h2 {
  font-size: 22px;
  font-weight: 800;
  color: var(--text-main);
  margin: 0 0 4px 0;
}

.subtitle-lbl {
  font-size: 13.5px;
  color: var(--text-muted);
  margin: 0;
}

/* Calendar Grid */
.calendar-cell.has-todos:hover {
  background-color: #fafbfc !important;
}

.calendar-cell.other-month {
  background-color: #f8fafc;
}

.calendar-cell.is-today {
  box-shadow: inset 0 0 0 2px var(--primary);
  background-color: #f0f7ff !important;
}

.todo-capsule:hover {
  filter: brightness(0.97);
}

.todo-capsule-done {
  text-decoration: line-through;
  opacity: 0.6;
}

/* Day List Modal Styles */
.todo-list-row:hover {
  background-color: #f1f5f9 !important;
  border-color: var(--border-medium) !important;
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

/* Selectors & Modal styles */
.beautiful-select-trigger:hover {
  border-color: var(--primary) !important;
  background: var(--bg-hover) !important;
}

.beautiful-select-trigger.is-active {
  border-color: var(--primary) !important;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.custom-dropdown-item {
  color: var(--text-muted);
}

.custom-dropdown-item:hover {
  background-color: var(--bg-hover);
  color: var(--text-main);
}

.custom-dropdown-item.is-selected {
  background-color: #eff6ff !important;
}

.custom-dropdown-item.is-selected span {
  color: #2563eb !important;
}

/* Transitions */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.25s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-active .modal-card,
.modal-fade-leave-active .modal-card {
  transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.modal-fade-enter-from .modal-card {
  transform: scale(0.95) translateY(10px);
}

.modal-fade-leave-to .modal-card {
  transform: scale(0.98) translateY(-5px);
}

/* Nav arrows hover */
.nav-arrow-btn:hover:not(:disabled) {
  background-color: #f1f5f9 !important;
  color: var(--primary) !important;
}

.close-btn:hover {
  background-color: #f1f5f9;
  color: var(--text-main) !important;
}
</style>
