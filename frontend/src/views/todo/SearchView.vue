<template>
  <div class="search-view">
    <!-- Header Area (PerformanceView style) -->
    <div class="view-header premium-card">
      <div class="title-row">
        <div class="title-meta-left">
          <h2>全局搜索</h2>
          <p class="subtitle-lbl">支持关键字多字段检索与分类/优先级/状态组合筛选</p>
        </div>
      </div>
    </div>

    <!-- Search Bar & Filter Header -->
    <div class="search-header premium-card">
      <div class="search-input-wrapper">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="search-icon">
          <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.604 10.604z" />
        </svg>
        <input 
          v-model="searchQuery" 
          type="text" 
          placeholder="搜索待办任务标题或描述内容..." 
          class="search-bar-input"
          autofocus
          autocomplete="off"
        />
        <button v-if="searchQuery" class="clear-search-btn" @click="searchQuery = ''">✕</button>
      </div>

      <div class="search-filters">
        <!-- Category Filter -->
        <div class="custom-select-container" style="position: relative; min-width: 180px;" @click.stop>
          <div 
            class="beautiful-select-trigger"
            :class="{ 'is-active': isCategoryDropdownOpen }"
            @click="isCategoryDropdownOpen = !isCategoryDropdownOpen; isPriorityDropdownOpen = false; isStatusDropdownOpen = false;"
            style="display: flex; align-items: center; justify-content: space-between; cursor: pointer; padding: 0 16px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background: #fff; transition: all var(--transition-fast); height: 48px;"
          >
            <div style="display: flex; align-items: center; gap: 8px;">
              <span style="display: inline-block; width: 8px; height: 8px; border-radius: 50%;" :style="{ backgroundColor: currentCategoryOption.color }"></span>
              <span style="font-size: 14px; font-weight: 500; color: var(--text-main);">{{ currentCategoryOption.label }}</span>
            </div>
            <svg 
              xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.2" stroke="currentColor" 
              style="width: 14px; height: 14px; transition: transform 0.2s;"
              :style="{ transform: isCategoryDropdownOpen ? 'rotate(180deg)' : 'rotate(0deg)' }"
            >
              <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
            </svg>
          </div>
          
          <div 
            v-if="isCategoryDropdownOpen" 
            class="custom-dropdown-list"
            style="position: absolute; top: calc(100% + 6px); left: 0; right: 0; background: rgba(255, 255, 255, 0.98); border: 1px solid rgba(0, 0, 0, 0.08); border-radius: var(--radius-md); box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.05); z-index: 999; padding: 6px; backdrop-filter: blur(12px); display: flex; flex-direction: column; gap: 4px; max-height: 250px; overflow-y: auto;"
          >
            <!-- All Categories option -->
            <div 
              @click="filterCategoryId = null; isCategoryDropdownOpen = false;"
              class="custom-dropdown-item"
              :class="{ 'is-selected': filterCategoryId === null }"
              style="display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
            >
              <div style="display: flex; align-items: center; gap: 8px;">
                <span style="display: inline-block; width: 6px; height: 6px; border-radius: 50%;" :style="{ backgroundColor: '#64748b' }"></span>
                <span style="font-size: 13.5px; font-weight: 500; color: var(--text-main);">全部大分类</span>
              </div>
              <svg 
                v-if="filterCategoryId === null"
                xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="#2563eb" style="width: 14px; height: 14px;"
              >
                <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
              </svg>
            </div>

            <!-- Loop Categories -->
            <template v-for="cat in categories" :key="cat.id">
              <!-- Parent Category Option -->
              <div 
                @click="filterCategoryId = cat.id; isCategoryDropdownOpen = false;"
                class="custom-dropdown-item parent-category-item"
                :class="{ 'is-selected': filterCategoryId === cat.id }"
                style="display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
              >
                <div style="display: flex; align-items: center; gap: 8px;">
                  <span style="display: inline-block; width: 6px; height: 6px; border-radius: 50%;" :style="{ backgroundColor: cat.color }"></span>
                  <span style="font-size: 13.5px; font-weight: 600; color: var(--text-main);">{{ cat.name }}</span>
                </div>
                <svg 
                  v-if="filterCategoryId === cat.id"
                  xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="#2563eb" style="width: 14px; height: 14px;"
                >
                  <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
                </svg>
              </div>

              <!-- Subcategories under this category -->
              <div 
                v-for="sub in todoStore.subcategoriesByCategoryId(cat.id)" 
                :key="sub.id"
                @click="filterCategoryId = sub.id; isCategoryDropdownOpen = false;"
                class="custom-dropdown-item subcategory-item"
                :class="{ 'is-selected': filterCategoryId === sub.id }"
                style="display: flex; align-items: center; justify-content: space-between; padding: 8px 12px 8px 28px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
              >
                <div style="display: flex; align-items: center; gap: 8px;">
                  <span style="display: inline-block; width: 4px; height: 4px; border-radius: 50%;" :style="{ backgroundColor: `${cat.color}aa` }"></span>
                  <span style="font-size: 13px; font-weight: 500; color: var(--text-muted);">{{ sub.name }}</span>
                </div>
                <svg 
                  v-if="filterCategoryId === sub.id"
                  xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="#2563eb" style="width: 14px; height: 14px;"
                >
                  <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
                </svg>
              </div>
            </template>
          </div>
        </div>

        <!-- Priority Filter -->
        <div class="custom-select-container" style="position: relative; min-width: 140px;" @click.stop>
          <div 
            class="beautiful-select-trigger"
            :class="{ 'is-active': isPriorityDropdownOpen }"
            @click="isPriorityDropdownOpen = !isPriorityDropdownOpen; isCategoryDropdownOpen = false; isStatusDropdownOpen = false;"
            style="display: flex; align-items: center; justify-content: space-between; cursor: pointer; padding: 0 16px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background: #fff; transition: all var(--transition-fast); height: 48px;"
          >
            <div style="display: flex; align-items: center; gap: 8px;">
              <span style="display: inline-block; width: 8px; height: 8px; border-radius: 50%;" :style="{ backgroundColor: currentPriorityOption.color }"></span>
              <span style="font-size: 14px; font-weight: 500; color: var(--text-main);">{{ currentPriorityOption.label }}</span>
            </div>
            <svg 
              xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.2" stroke="currentColor" 
              style="width: 14px; height: 14px; transition: transform 0.2s;"
              :style="{ transform: isPriorityDropdownOpen ? 'rotate(180deg)' : 'rotate(0deg)' }"
            >
              <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
            </svg>
          </div>
          
          <div 
            v-if="isPriorityDropdownOpen" 
            class="custom-dropdown-list"
            style="position: absolute; top: calc(100% + 6px); left: 0; right: 0; background: rgba(255, 255, 255, 0.98); border: 1px solid rgba(0, 0, 0, 0.08); border-radius: var(--radius-md); box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.05); z-index: 999; padding: 6px; backdrop-filter: blur(12px); display: flex; flex-direction: column; gap: 4px;"
          >
            <div 
              v-for="opt in priorityOptions" 
              :key="String(opt.value)"
              @click="filterPriority = opt.value; isPriorityDropdownOpen = false;"
              class="custom-dropdown-item"
              :class="{ 'is-selected': filterPriority === opt.value }"
              style="display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
            >
              <div style="display: flex; align-items: center; gap: 8px;">
                <span style="display: inline-block; width: 6px; height: 6px; border-radius: 50%;" :style="{ backgroundColor: opt.color }"></span>
                <span style="font-size: 13.5px; font-weight: 500; color: var(--text-main);">{{ opt.label }}</span>
              </div>
              <svg 
                v-if="filterPriority === opt.value"
                xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="#2563eb" style="width: 14px; height: 14px;"
              >
                <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
              </svg>
            </div>
          </div>
        </div>

        <!-- Status Filter -->
        <div class="custom-select-container" style="position: relative; min-width: 140px;" @click.stop>
          <div 
            class="beautiful-select-trigger"
            :class="{ 'is-active': isStatusDropdownOpen }"
            @click="isStatusDropdownOpen = !isStatusDropdownOpen; isCategoryDropdownOpen = false; isPriorityDropdownOpen = false;"
            style="display: flex; align-items: center; justify-content: space-between; cursor: pointer; padding: 0 16px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background: #fff; transition: all var(--transition-fast); height: 48px;"
          >
            <div style="display: flex; align-items: center; gap: 8px;">
              <span style="display: inline-block; width: 8px; height: 8px; border-radius: 50%;" :style="{ backgroundColor: currentStatusOption.color }"></span>
              <span style="font-size: 14px; font-weight: 500; color: var(--text-main);">{{ currentStatusOption.label }}</span>
            </div>
            <svg 
              xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.2" stroke="currentColor" 
              style="width: 14px; height: 14px; transition: transform 0.2s;"
              :style="{ transform: isStatusDropdownOpen ? 'rotate(180deg)' : 'rotate(0deg)' }"
            >
              <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
            </svg>
          </div>
          
          <div 
            v-if="isStatusDropdownOpen" 
            class="custom-dropdown-list"
            style="position: absolute; top: calc(100% + 6px); left: 0; right: 0; background: rgba(255, 255, 255, 0.98); border: 1px solid rgba(0, 0, 0, 0.08); border-radius: var(--radius-md); box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.05); z-index: 999; padding: 6px; backdrop-filter: blur(12px); display: flex; flex-direction: column; gap: 4px;"
          >
            <div 
              v-for="opt in statusOptions" 
              :key="String(opt.value)"
              @click="filterStatus = opt.value; isStatusDropdownOpen = false;"
              class="custom-dropdown-item"
              :class="{ 'is-selected': filterStatus === opt.value }"
              style="display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
            >
              <div style="display: flex; align-items: center; gap: 8px;">
                <span style="display: inline-block; width: 6px; height: 6px; border-radius: 50%;" :style="{ backgroundColor: opt.color }"></span>
                <span style="font-size: 13.5px; font-weight: 500; color: var(--text-main);">{{ opt.label }}</span>
              </div>
              <svg 
                v-if="filterStatus === opt.value"
                xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="#2563eb" style="width: 14px; height: 14px;"
              >
                <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
              </svg>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Search Results -->
    <div class="results-container">
      <div v-if="searchResults.length > 0" class="todo-list">
        <div class="results-meta">找到 {{ searchResults.length }} 条相关事项</div>
        
        <div 
          v-for="todo in searchResults" 
          :key="todo.id" 
          class="todo-card premium-card interactive-item"
          :class="{ 'todo-done': todo.status === 'done', 'todo-pinned': todo.isPinned }"
          @click="openDetail(todo)"
          style="display: flex; justify-content: space-between; align-items: center; padding: 10px 18px;"
        >
          <!-- Todo Details -->
          <div class="todo-main-info" style="flex: 1; margin-right: 24px;">
            <div class="todo-title-row" style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
              <span v-if="todo.isPinned" class="pin-icon">📌</span>
              <!-- Highlight title matching -->
              <h3 class="todo-title" v-html="highlightText(todo.title, searchQuery)" style="margin: 0;"></h3>
              
              <!-- Priority Badge (right of title) -->
              <span class="badge" :class="`badge-${todo.priority}`" style="margin: 0; display: inline-flex; align-items: center;">
                {{ priorityLabel(todo.priority) }}
              </span>

              <!-- Category Badge (right of priority) -->
              <span v-if="getCategoryName(todo)" class="todo-badge-item" :style="getCategoryStyle(todo)" style="margin: 0; display: inline-flex; align-items: center;">
                {{ getCategoryName(todo) }}
              </span>
            </div>
            
            <p 
              v-if="todo.content" 
              class="todo-desc" 
              v-html="highlightText(todo.content, searchQuery)"
              style="margin-top: 4px; margin-bottom: 4px;"
            ></p>
            
            <div v-if="todo.rolloverCount > 0" class="todo-badges" style="margin-top: 4px; display: flex; align-items: center; gap: 8px;">
              <!-- Rollover count -->
              <span class="badge rollover-badge" style="margin: 0;">
                ♻️ 延续 {{ todo.rolloverCount }} 次
              </span>
            </div>
          </div>

          <!-- Times (Right side) -->
          <div class="todo-times" style="display: flex; align-items: center; gap: 20px; flex-shrink: 0; text-align: right;">
            <div class="time-item" style="display: flex; flex-direction: column; gap: 2px;">
              <span style="font-size: 11px; color: var(--text-muted);">创建时间</span>
              <span style="font-size: 13px; font-weight: 500; color: var(--text-main);">{{ formatDateTime(todo.createdAt) }}</span>
            </div>
            <div v-if="todo.status === 'done' && todo.doneAt" class="time-item" style="display: flex; flex-direction: column; gap: 2px;">
              <span style="font-size: 11px; color: #10b981;">完成时间</span>
              <span style="font-size: 13px; font-weight: 500; color: #10b981;">{{ formatDateTime(todo.doneAt) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-else class="empty-state premium-card">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.2" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.604 10.604z" />
        </svg>
        <h3>无匹配结果</h3>
        <p>未找到符合检索条件的事项，请更换关键词或调整过滤器重试。</p>
      </div>
    </div>

    <!-- Edit Detail Modal (Read-Only Popup) -->
    <transition name="modal-fade">
      <div v-if="isDrawerOpen" class="modal-overlay" @mousedown="handleOverlayMousedown" @click="handleOverlayClick($event, closeDrawer)">
        <div class="modal-card" @click.stop>
          <div class="modal-header">
            <h2>待办事项详情</h2>
            <button class="close-btn" @click="closeDrawer">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <div class="modal-form">
            <div class="form-field">
              <label for="edit-title">待办标题 <span class="required-star" style="color: #ef4444; margin-left: 2px;">*</span></label>
              <input id="edit-title" v-model="editForm.title" type="text" disabled class="form-control" style="background-color: #ffffff; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); padding: 10px 14px; font-size: 14px; color: var(--text-main); cursor: not-allowed;" />
            </div>

            <div class="form-field">
              <label for="edit-content">备注 (详细描述)</label>
              <textarea id="edit-content" v-model="editForm.content" rows="4" disabled class="form-control" style="background-color: #ffffff; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); padding: 10px 14px; font-size: 14px; color: var(--text-main); cursor: not-allowed; resize: none;"></textarea>
            </div>

            <div class="form-row">
              <div class="form-field">
                <label>归属分类</label>
                <!-- Readonly custom selector style -->
                <div style="display: flex; align-items: center; justify-content: space-between; padding: 0 16px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background: #ffffff; height: 48px; cursor: not-allowed; opacity: 1;">
                  <div style="display: flex; align-items: center; gap: 8px;">
                    <span style="display: inline-block; width: 8px; height: 8px; border-radius: 50%;" :style="{ backgroundColor: getEditCategoryColor }"></span>
                    <span style="font-size: 14px; font-weight: 500; color: var(--text-main);">{{ getEditCategoryName || '无归属分类' }}</span>
                  </div>
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.2" stroke="currentColor" style="width: 14px; height: 14px; color: var(--text-muted);">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
                  </svg>
                </div>
              </div>
              <div class="form-field">
                <label>优先级</label>
                <!-- Readonly custom selector style -->
                <div style="display: flex; align-items: center; justify-content: space-between; padding: 0 16px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background: #ffffff; height: 48px; cursor: not-allowed; opacity: 1;">
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
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useTodoStore, mapTodoVOToTodo } from '@/stores/todo'
import type { Todo } from '@/stores/todo'
import request from '@/api/request'

const authStore = useAuthStore()
const todoStore = useTodoStore()

const searchQuery = ref('')
const filterCategoryId = ref<number | null>(null)
const filterPriority = ref<'high' | 'medium' | 'low' | null>(null)
const filterStatus = ref<'pending' | 'done' | null>(null)

// Custom dropdowns states
const isCategoryDropdownOpen = ref(false)
const isPriorityDropdownOpen = ref(false)
const isStatusDropdownOpen = ref(false)

const closeAllDropdowns = () => {
  isCategoryDropdownOpen.value = false
  isPriorityDropdownOpen.value = false
  isStatusDropdownOpen.value = false
}

// Categories
const categories = computed(() => todoStore.sortedCategories)

// Options formatting
const currentCategoryOption = computed(() => {
  if (filterCategoryId.value === null) {
    return { value: null, label: '全部大分类', color: '#64748b' }
  }
  // Look up parent category
  const parentCat = categories.value.find(c => c.id === filterCategoryId.value)
  if (parentCat) {
    return { value: parentCat.id, label: parentCat.name, color: parentCat.color }
  }
  // If not parent, look up subcategory
  for (const cat of categories.value) {
    const subList = todoStore.subcategoriesByCategoryId(cat.id)
    const sub = subList.find(s => s.id === filterCategoryId.value)
    if (sub) {
      return { value: sub.id, label: `${cat.name} > ${sub.name}`, color: cat.color }
    }
  }
  return { value: null, label: '全部大分类', color: '#64748b' }
})

const priorityOptions: { value: 'high' | 'medium' | 'low' | null, label: string, color: string }[] = [
  { value: null, label: '所有优先级', color: '#64748b' },
  { value: 'high', label: '高', color: '#ef4444' },
  { value: 'medium', label: '中', color: '#eab308' },
  { value: 'low', label: '低', color: '#22c55e' }
]

const currentPriorityOption = computed(() => {
  return priorityOptions.find(opt => opt.value === filterPriority.value) || priorityOptions[0]
})

const statusOptions: { value: 'pending' | 'done' | null, label: string, color: string }[] = [
  { value: null, label: '全部状态', color: '#64748b' },
  { value: 'pending', label: '待处理', color: '#3b82f6' },
  { value: 'done', label: '已完成', color: '#10b981' }
]

const currentStatusOption = computed(() => {
  return statusOptions.find(opt => opt.value === filterStatus.value) || statusOptions[0]
})

// Search & Pagination States
const searchResults = ref<Todo[]>([])
const pageNum = ref(1)
const totalCount = ref(0)
const hasMore = ref(true)
const loading = ref(false)

const fetchSearchResults = async (isAppend = false) => {
  if (loading.value) return
  loading.value = true
  
  try {
    const params: any = {
      pageNum: pageNum.value,
      pageSize: 20
    }
    
    const query = searchQuery.value.trim()
    if (query) {
      params.keyword = query
    }
    if (filterCategoryId.value) {
      params.categoryId = filterCategoryId.value
    }
    if (filterPriority.value) {
      params.priority = filterPriority.value.toUpperCase()
    }
    if (filterStatus.value) {
      params.status = filterStatus.value.toUpperCase()
    }
    
    const res = await request.get<any, any>('/api/todos', { params })
    
    if (res.code === 200 && res.data) {
      const records = res.data.records || []
      const total = res.data.total || 0
      
      const mapped = records.map((vo: any) => 
        mapTodoVOToTodo(vo, todoStore.categories, todoStore.subcategoriesMap)
      )
      
      if (isAppend) {
        searchResults.value = [...searchResults.value, ...mapped]
      } else {
        searchResults.value = mapped
      }
      
      totalCount.value = total
      hasMore.value = searchResults.value.length < total
    }
  } catch (e) {
    console.error('Failed to search page:', e)
  } finally {
    loading.value = false
  }
}

const onFilterChange = () => {
  pageNum.value = 1
  hasMore.value = true
  fetchSearchResults(false)
}

// Watch filters
watch([filterCategoryId, filterPriority, filterStatus], () => {
  onFilterChange()
})

// Debounced watch searchQuery
let searchTimer: any = null
watch(searchQuery, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    onFilterChange()
  }, 300)
})

// Scroll handler
const handleScroll = () => {
  const scrollTop = window.scrollY || document.documentElement.scrollTop
  const windowHeight = window.innerHeight
  const scrollHeight = document.documentElement.scrollHeight
  
  if (scrollHeight - scrollTop - windowHeight < 100) {
    if (!loading.value && hasMore.value) {
      pageNum.value++
      fetchSearchResults(true)
    }
  }
}

// Drawer
const isDrawerOpen = ref(false)
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

const editSubcategories = computed(() => {
  if (!editForm.categoryId) return []
  return todoStore.subcategoriesByCategoryId(editForm.categoryId)
})

onMounted(async () => {
  document.addEventListener('click', closeAllDropdowns)
  window.addEventListener('scroll', handleScroll)
  
  if (authStore.currentUser) {
    todoStore.refreshCategories(authStore.currentUser.userId)
  }
  
  // Reset conditions on entering
  searchQuery.value = ''
  filterCategoryId.value = null
  filterPriority.value = null
  filterStatus.value = null
  
  // Trigger initial fetch
  onFilterChange()
})

onUnmounted(() => {
  document.removeEventListener('click', closeAllDropdowns)
  window.removeEventListener('scroll', handleScroll)
})

// Highlight matching text (PRD 6.5.3)
const highlightText = (text: string, query: string) => {
  if (!text) return ''
  if (!query.trim()) return text
  
  const escapedQuery = query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const regex = new RegExp(`(${escapedQuery})`, 'gi')
  return text.replace(regex, '<mark class="highlight-mark">$1</mark>')
}

const priorityLabel = (prio: string) => {
  if (prio === 'high') return '高'
  if (prio === 'medium') return '中'
  return '低'
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

const openDetail = (todo: Todo) => {
  editForm.id = todo.id
  editForm.title = todo.title
  editForm.content = todo.content || ''
  editForm.planDate = todo.planDate
  editForm.priority = todo.priority
  editForm.categoryId = todo.categoryId
  editForm.subcategoryId = todo.subcategoryId
  editForm.createdAt = todo.createdAt
  editForm.status = todo.status
  editForm.doneAt = todo.doneAt
  if (todo.reminderTime) {
    const [d, t] = todo.reminderTime.split(' ')
    editForm.reminderTime = `${d}T${t}`
  } else {
    editForm.reminderTime = ''
  }
  isDrawerOpen.value = true
}

const closeDrawer = () => {
  isDrawerOpen.value = false
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
.search-view {
  max-width: 1100px;
  margin: 0 auto;
}

/* Search bar & filter */
/* Header Styles */
.view-header {
  background-color: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 24px 32px;
  margin-bottom: 24px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
  position: relative;
  z-index: 10;
}
.title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
}
.title-meta-left h2 {
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

.search-header {
  padding: 16px 20px;
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  position: relative;
  z-index: 10;
}

.search-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  flex: 1;
}
.search-icon {
  width: 20px;
  height: 20px;
  color: var(--text-muted);
  position: absolute;
  left: 16px;
  pointer-events: none;
}
.search-bar-input {
  width: 100%;
  height: 48px;
  padding: 0 48px;
  border-radius: var(--radius-lg);
  border: 1.5px solid var(--border-medium);
  outline: none;
  font-size: 15px;
  font-weight: 500;
  background-color: var(--bg-app);
  transition: all var(--transition-fast);
}
.search-bar-input:focus {
  border-color: var(--primary);
  background-color: #fff;
  box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.08);
}
.clear-search-btn {
  position: absolute;
  right: 16px;
  background: none;
  border: none;
  color: var(--text-muted);
  font-size: 14px;
  cursor: pointer;
  padding: 4px;
}

.search-filters {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 12px;
  border-top: none;
  padding-top: 0;
}

.beautiful-select-trigger:hover {
  border-color: #2563eb !important;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.05);
}
.beautiful-select-trigger.is-active {
  border-color: #2563eb !important;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12) !important;
}
.custom-dropdown-item:hover {
  background-color: #f1f5f9 !important;
}
.custom-dropdown-item.is-selected {
  background-color: #eff6ff !important;
}
.custom-dropdown-item.is-selected span {
  color: #2563eb !important;
}

@media (max-width: 992px) {
  .search-header {
    flex-direction: column;
    align-items: stretch;
  }
  .search-filters {
    width: 100%;
    display: grid;
    grid-template-columns: 1fr 1fr 1fr;
    gap: 10px;
  }
  .custom-select-container {
    width: 100% !important;
    min-width: 0 !important;
  }
}
@media (max-width: 600px) {
  .search-filters {
    grid-template-columns: 1fr;
  }
}

.results-meta {
  font-size: 13px;
  color: var(--text-muted);
  font-weight: 600;
  margin-bottom: 12px;
  padding-left: 4px;
}

/* Todos list */
.results-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: relative;
  z-index: 1;
}
.todo-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.todo-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 18px;
}
.todo-done {
  border-color: var(--border-light);
}
.todo-done .todo-title {
  text-decoration: line-through;
  color: var(--text-muted);
}
.todo-done .todo-desc {
  color: var(--border-medium);
}
.todo-done .badge {
  opacity: 0.5;
}

.checkbox-area {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.todo-main-info {
  flex: 1;
  min-width: 0;
}
.todo-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 2px;
}
.todo-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.todo-desc {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.todo-badges {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.date-badge {
  background-color: var(--border-light);
  color: var(--text-muted);
}
.todo-badge-item {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: var(--radius-sm);
}
.rollover-badge {
  background-color: var(--warning-bg);
  color: var(--warning);
  font-weight: 700;
}

/* Highlight mark overrides */
:deep(.highlight-mark) {
  background-color: #fef08a; /* Soft yellow highlight */
  color: #1e293b;
  border-radius: 2px;
  padding: 0 2px;
  font-weight: 600;
}

.todo-actions {
  display: flex;
  gap: 6px;
  opacity: 0;
  transition: opacity var(--transition-fast);
}
.todo-card:hover .todo-actions {
  opacity: 1;
}
@media (max-width: 768px) {
  .todo-actions {
    opacity: 1;
  }
}
.action-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--text-muted);
  padding: 6px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}
.action-btn:hover {
  background-color: var(--border-light);
  color: var(--text-main);
}
.pinned-active {
  color: var(--primary) !important;
}
.delete-btn:hover {
  background-color: var(--danger-bg);
  color: var(--danger) !important;
}

/* Modal Popup Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(4px);
  z-index: 2000;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
}
.modal-card {
  width: 100%;
  max-width: 520px;
  background-color: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  padding: 32px;
  display: flex;
  flex-direction: column;
  position: relative;
  border: 1px solid var(--border-light);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.modal-header h2 {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-main);
}
.close-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--text-muted);
  padding: 4px;
  border-radius: 50%;
  display: flex;
}
.close-btn:hover {
  background-color: var(--border-light);
  color: var(--text-main);
}
.close-btn svg {
  width: 20px;
  height: 20px;
}

.modal-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* Modal Transition Effects */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.2s ease;
}
.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-active .modal-card {
  animation: modalPop 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.modal-fade-leave-active .modal-card {
  animation: modalPopOut 0.2s ease-in;
}

@keyframes modalPop {
  from { transform: scale(0.9) translateY(10px); opacity: 0; }
  to { transform: scale(1) translateY(0); opacity: 1; }
}
@keyframes modalPopOut {
  from { transform: scale(1) translateY(0); opacity: 1; }
  to { transform: scale(0.9) translateY(10px); opacity: 0; }
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
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}
textarea.form-control {
  resize: vertical;
}

.drawer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: auto;
  padding-top: 20px;
  border-top: 1px solid var(--border-light);
}
.btn {
  padding: 10px 20px;
  border-radius: var(--radius-md);
  font-size: 14px;
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
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.2);
}
.btn-primary:hover {
  background-color: var(--primary-hover);
}
</style>
