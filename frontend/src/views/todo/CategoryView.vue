<template>
  <div class="category-view">
    <!-- Header with Category Details -->
    <div class="category-header premium-card" v-if="category">
      <div class="header-main-row">
        <!-- Left: Title & Subtitle -->
        <div class="header-left">
          <span class="color-indicator" :style="{ backgroundColor: category?.color }"></span>
          <div class="title-details">
            <h1>{{ category?.name }}</h1>
            <p class="stats-subtitle">
              共计 {{ filteredTodos.length }} 项事项 · {{ pendingTodosCount }} 项待完成
            </p>
          </div>
        </div>

        <!-- Right: Filters & Create button -->
        <div class="header-right">
          <!-- Subcategory Filter -->
          <div class="custom-select-container" style="position: relative; min-width: 150px;" @click.stop>
            <div 
              class="beautiful-select-trigger"
              :class="{ 'is-active': isSubcatDropdownOpen }"
              @click="toggleSubcatDropdown"
              style="display: flex; align-items: center; justify-content: space-between; cursor: pointer; padding: 0 12px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background: #fff; transition: all var(--transition-fast); height: 38px;"
            >
              <div style="display: flex; align-items: center; gap: 8px;">
                <span style="display: inline-block; width: 8px; height: 8px; border-radius: 50%;" :style="{ backgroundColor: currentSubcatOption.color }"></span>
                <span style="font-size: 13.5px; font-weight: 500; color: var(--text-main);">{{ currentSubcatOption.label }}</span>
              </div>
              <svg 
                xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.2" stroke="currentColor" 
                style="width: 14px; height: 14px; transition: transform 0.2s;"
                :style="{ transform: isSubcatDropdownOpen ? 'rotate(180deg)' : 'rotate(0deg)' }"
              >
                <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
              </svg>
            </div>
            
            <div 
              v-if="isSubcatDropdownOpen" 
              class="custom-dropdown-list"
              style="position: absolute; top: calc(100% + 6px); left: 0; right: 0; background: rgba(255, 255, 255, 0.98); border: 1px solid rgba(0, 0, 0, 0.08); border-radius: var(--radius-md); box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.05); z-index: 999; padding: 6px; backdrop-filter: blur(12px); display: flex; flex-direction: column; gap: 4px; max-height: 250px; overflow-y: auto;"
            >
              <!-- All option -->
              <div 
                @click="selectedSubcategoryId = null; isSubcatDropdownOpen = false;"
                class="custom-dropdown-item"
                :class="{ 'is-selected': selectedSubcategoryId === null }"
                style="display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
              >
                <div style="display: flex; align-items: center; gap: 8px;">
                  <span style="display: inline-block; width: 6px; height: 6px; border-radius: 50%;" :style="{ backgroundColor: '#64748b' }"></span>
                  <span style="font-size: 13px; font-weight: 500; color: var(--text-main);">全部子分类</span>
                </div>
                <svg 
                  v-if="selectedSubcategoryId === null"
                  xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="#2563eb" style="width: 14px; height: 14px;"
                >
                  <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
                </svg>
              </div>

              <!-- Loop Subcategories -->
              <div 
                v-for="sub in subcategories" 
                :key="sub.id"
                @click="selectedSubcategoryId = sub.id; isSubcatDropdownOpen = false;"
                class="custom-dropdown-item"
                :class="{ 'is-selected': selectedSubcategoryId === sub.id }"
                style="display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
              >
                <div style="display: flex; align-items: center; gap: 8px;">
                  <span style="display: inline-block; width: 6px; height: 6px; border-radius: 50%;" :style="{ backgroundColor: category?.color || '#4f46e5' }"></span>
                  <span style="font-size: 13px; font-weight: 500; color: var(--text-main);">{{ sub.name }}</span>
                </div>
                <svg 
                  v-if="selectedSubcategoryId === sub.id"
                  xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="#2563eb" style="width: 14px; height: 14px;"
                >
                  <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
                </svg>
              </div>
            </div>
          </div>

          <!-- Date Range Filter -->
          <div class="custom-select-container" style="position: relative; min-width: 255px;" @click.stop>
            <div 
              class="beautiful-select-trigger"
              :class="{ 'is-active': isDateDropdownOpen }"
              @click="toggleDateDropdown"
              style="display: flex; align-items: center; justify-content: space-between; cursor: pointer; padding: 0 12px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background: #fff; transition: all var(--transition-fast); height: 38px;"
            >
              <div style="display: flex; align-items: center; gap: 8px;">
                <span style="font-size: 14px; line-height: 1;">📅</span>
                <span style="font-size: 13.5px; font-weight: 500; color: var(--text-main); max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                  {{ formattedDateRangeLabel }}
                </span>
              </div>
              <svg 
                xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.2" stroke="currentColor" 
                style="width: 14px; height: 14px; transition: transform 0.2s;"
                :style="{ transform: isDateDropdownOpen ? 'rotate(180deg)' : 'rotate(0deg)' }"
              >
                <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
              </svg>
            </div>
            
            <div 
              v-if="isDateDropdownOpen" 
              class="custom-dropdown-list"
              style="position: absolute; top: calc(100% + 6px); right: 0; background: rgba(255, 255, 255, 0.98); border: 1px solid rgba(0, 0, 0, 0.08); border-radius: var(--radius-md); box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.05); z-index: 999; padding: 16px; backdrop-filter: blur(12px); display: flex; flex-direction: column; gap: 12px; width: 260px;"
            >
              <!-- Start Date -->
              <div style="display: flex; flex-direction: column; gap: 6px;">
                <label style="font-size: 12px; font-weight: 600; color: var(--text-muted); text-align: left;">开始日期：</label>
                <input 
                  type="date" 
                  v-model="tempStartDate" 
                  class="form-control beautiful-date-input"
                  :class="{ 'has-value': !!tempStartDate }"
                  @mousedown.prevent="($event.target as HTMLInputElement).showPicker?.()"
                  style="width: 100%; padding: 8px 12px;"
                />
              </div>

              <!-- End Date -->
              <div style="display: flex; flex-direction: column; gap: 6px;">
                <label style="font-size: 12px; font-weight: 600; color: var(--text-muted); text-align: left;">结束日期：</label>
                <input 
                  type="date" 
                  v-model="tempEndDate" 
                  class="form-control beautiful-date-input"
                  :class="{ 'has-value': !!tempEndDate }"
                  @mousedown.prevent="($event.target as HTMLInputElement).showPicker?.()"
                  style="width: 100%; padding: 8px 12px;"
                />
              </div>

              <!-- Actions -->
              <div style="display: flex; justify-content: space-between; gap: 8px; margin-top: 4px;">
                <button 
                  type="button" 
                  @click="clearDateRange"
                  style="flex: 1; border: none; background: #f1f5f9; color: #475569; font-size: 12px; font-weight: 600; padding: 8px 0; border-radius: 6px; cursor: pointer; transition: background 0.15s;"
                >
                  清空
                </button>
                <button 
                  type="button" 
                  @click="applyDateRange"
                  style="flex: 1; border: none; background: #2563eb; color: #fff; font-size: 12px; font-weight: 600; padding: 8px 0; border-radius: 6px; cursor: pointer; transition: background 0.15s;"
                >
                  确定
                </button>
              </div>
            </div>
          </div>

          <!-- Status Filter -->
          <div class="custom-select-container" style="position: relative; min-width: 130px;" @click.stop>
            <div 
              class="beautiful-select-trigger"
              :class="{ 'is-active': isStatusDropdownOpen }"
              @click="toggleStatusDropdown"
              style="display: flex; align-items: center; justify-content: space-between; cursor: pointer; padding: 0 12px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background: #fff; transition: all var(--transition-fast); height: 38px;"
            >
              <div style="display: flex; align-items: center; gap: 8px;">
                <span style="display: inline-block; width: 8px; height: 8px; border-radius: 50%;" :style="{ backgroundColor: currentStatusOption.color }"></span>
                <span style="font-size: 13.5px; font-weight: 500; color: var(--text-main);">{{ currentStatusOption.label }}</span>
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
                :key="opt.value"
                @click="activeStatusTab = opt.value; isStatusDropdownOpen = false;"
                class="custom-dropdown-item"
                :class="{ 'is-selected': activeStatusTab === opt.value }"
                style="display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
              >
                <div style="display: flex; align-items: center; gap: 8px;">
                  <span style="display: inline-block; width: 6px; height: 6px; border-radius: 50%;" :style="{ backgroundColor: opt.color }"></span>
                  <span style="font-size: 13px; font-weight: 500; color: var(--text-main);">{{ opt.label }}</span>
                </div>
                <svg 
                  v-if="activeStatusTab === opt.value"
                  xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="#2563eb" style="width: 14px; height: 14px;"
                >
                  <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
                </svg>
              </div>
            </div>
          </div>

          <!-- Add Button -->
          <button class="add-todo-btn" @click="openAddModal" :style="{ backgroundColor: category?.color }">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" style="width: 16px; height: 16px;">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
            </svg>
            <span>新增待办</span>
          </button>
        </div>
      </div>
    </div>



    <!-- Todo list -->
    <div class="todos-container">
      <div v-if="sortedTodos.length > 0" class="todo-list">
        <div 
          v-for="todo in sortedTodos" 
          :key="todo.id" 
          class="todo-card premium-card interactive-item"
          :class="{ 'todo-done': todo.status === 'done', 'todo-pinned': todo.isPinned }"
          @click="openDetail(todo)"
        >
          <!-- Custom Checkbox (PerformanceView circular style) -->
          <div class="kr-status-icon-wrap" @click.stop="toggleStatus(todo)" style="align-self: flex-start; margin-top: 2px;">
            <div class="custom-indicator-circle" :class="todo.status === 'done' ? 'done' : 'not_started'">
              <span v-if="todo.status === 'done'" class="check-mark">✓</span>
            </div>
          </div>

          <!-- Todo Details -->
          <div class="todo-main-info">
            <div class="todo-title-row">
              <span v-if="todo.isPinned" class="pin-icon">📌</span>
              <h3 class="todo-title">{{ todo.title }}</h3>
              
              <!-- Priority Badge -->
              <span class="badge" :class="`badge-${todo.priority}`" style="margin-left: 4px; flex-shrink: 0; font-size: 10px; padding: 2px 6px;">
                {{ priorityLabel(todo.priority) }}
              </span>

              <!-- Subcategory Badge -->
              <span v-if="getSubName(todo)" class="badge subcat-badge" :style="{ color: category?.color, borderColor: `${category?.color}40`, backgroundColor: `${category?.color}10` }" style="margin-left: 4px; flex-shrink: 0; font-size: 10px; padding: 2px 6px;">
                {{ getSubName(todo) }}
              </span>
            </div>
            <p v-if="todo.content" class="todo-desc">{{ truncate(todo.content, 80) }}</p>
            
            <div class="todo-badges">
              <!-- Rollover count -->
              <span v-if="todo.rolloverCount > 0" class="badge rollover-badge">
                ♻️ 延续 {{ todo.rolloverCount }} 次
              </span>
            </div>
          </div>

          <!-- Times -->
          <div class="todo-times" style="display: flex; align-items: center; gap: 20px; flex-shrink: 0; text-align: right; margin-left: auto;">
            <div class="time-item" style="display: flex; flex-direction: column; gap: 2px;">
              <span style="font-size: 11px; color: var(--text-muted);">创建时间</span>
              <span style="font-size: 13px; font-weight: 500; color: var(--text-main);">{{ formatDateTime(todo.createdAt) }}</span>
            </div>
            <div v-if="todo.status === 'done' && todo.doneAt" class="time-item" style="display: flex; flex-direction: column; gap: 2px;">
              <span style="font-size: 11px; color: #10b981;">完成时间</span>
              <span style="font-size: 13px; font-weight: 500; color: #10b981;">{{ formatDateTime(todo.doneAt) }}</span>
            </div>
          </div>

          <!-- Action buttons -->
          <div class="todo-actions" @click.stop>
            <!-- Edit Button -->
            <button class="action-btn edit-action-btn" @click="openDetail(todo)" title="编辑待办">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.2" stroke="currentColor" style="width: 16px; height: 16px;">
                <path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10" />
              </svg>
            </button>
            <!-- Delete Button -->
            <button class="action-btn delete-btn" @click="deleteTodoItem(todo)" title="删除待办">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" />
              </svg>
            </button>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-else class="empty-state premium-card">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.2" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6A2.25 2.25 0 016 3.75h2.25A2.25 2.25 0 0110.5 6v2.25a2.25 2.25 0 01-2.25 2.25H6a2.25 2.25 0 01-2.25-2.25V6z" />
        </svg>
        <h3>无相关待办事项</h3>
        <p>在此分类下暂时没有符合当前筛选的待办任务。</p>
      </div>
    </div>

    <!-- Edit Todo Modal -->
    <transition name="modal-fade">
      <div v-if="isEditModalOpen" class="modal-overlay" @mousedown="handleOverlayMousedown" @click="handleOverlayClick($event, closeEditModal)">
        <div class="modal-card" @click.stop="closeDropdowns">
          <div class="modal-header">
            <h2>编辑待办事项</h2>
            <button class="close-btn" @click="closeEditModal">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <form class="modal-form" @submit.prevent="saveEditModal">
            <div class="form-field">
              <label for="edit-title">待办标题 <span class="required-star">*</span></label>
              <input id="edit-title" v-model="editForm.title" type="text" required class="form-control" placeholder="想要做什么？" autocomplete="off" />
            </div>

            <div class="form-field">
              <label for="edit-content">备注 (详细描述)</label>
              <textarea id="edit-content" v-model="editForm.content" placeholder="添加备注信息..." rows="3" class="form-control"></textarea>
            </div>

            <div class="form-row">
              <!-- Subcategory Custom Select -->
              <div class="form-field">
                <label>子分类</label>
                <div class="custom-select-container" style="position: relative; width: 100%;" @click.stop>
                  <div 
                    class="form-control beautiful-select-trigger" 
                    @click="toggleEditSubcatDropdown"
                    :class="{ 'is-active': isEditSubcatDropdownOpen }"
                    style="display: flex; align-items: center; justify-content: space-between; cursor: pointer; padding: 0 14px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background: #fff; transition: all var(--transition-fast); height: 42px;"
                  >
                    <div style="display: flex; align-items: center; gap: 8px;">
                      <span style="display: inline-block; width: 8px; height: 8px; border-radius: 50%;" :style="{ backgroundColor: editSubcatOption.color }"></span>
                      <span style="font-size: 14px; font-weight: 500; color: var(--text-main);">{{ editSubcatOption.label }}</span>
                    </div>
                    <svg 
                      xmlns="http://www.w3.org/2000/svg" 
                      fill="none" 
                      viewBox="0 0 24 24" 
                      stroke-width="2.2" 
                      stroke="currentColor" 
                      style="width: 14px; height: 14px; transition: transform 0.2s; color: var(--text-muted);"
                      :style="{ transform: isEditSubcatDropdownOpen ? 'rotate(180deg)' : 'rotate(0deg)' }"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
                    </svg>
                  </div>
                  
                  <div 
                    v-if="isEditSubcatDropdownOpen" 
                    class="custom-dropdown-list"
                    style="position: absolute; top: calc(100% + 6px); left: 0; right: 0; background: rgba(255, 255, 255, 0.98); border: 1px solid rgba(0, 0, 0, 0.08); border-radius: var(--radius-md); box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.05); z-index: 999; padding: 6px; backdrop-filter: blur(12px); display: flex; flex-direction: column; gap: 4px; max-height: 200px; overflow-y: auto;"
                  >
                    <!-- Actual Subcategories -->
                    <div 
                      v-for="sub in subcategories" 
                      :key="sub.id"
                      @click="editForm.subcategoryId = sub.id; isEditSubcatDropdownOpen = false;"
                      class="custom-dropdown-item"
                      :class="{ 'is-selected': editForm.subcategoryId === sub.id }"
                      style="display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
                    >
                      <div style="display: flex; align-items: center; gap: 8px;">
                        <span style="display: inline-block; width: 6px; height: 6px; border-radius: 50%;" :style="{ backgroundColor: category?.color || '#4f46e5' }"></span>
                        <span style="font-size: 13px; font-weight: 500; color: var(--text-main);">{{ sub.name }}</span>
                      </div>
                      <svg 
                        v-if="editForm.subcategoryId === sub.id"
                        xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="#2563eb" style="width: 14px; height: 14px;"
                      >
                        <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
                      </svg>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Priority Custom Select -->
              <div class="form-field">
                <label>优先级</label>
                <div class="custom-select-container" style="position: relative; width: 100%;" @click.stop>
                  <div 
                    class="form-control beautiful-select-trigger" 
                    @click="toggleEditPriorityDropdown"
                    :class="{ 'is-active': isEditPriorityDropdownOpen }"
                    style="display: flex; align-items: center; justify-content: space-between; cursor: pointer; padding: 0 14px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background: #fff; transition: all var(--transition-fast); height: 42px;"
                  >
                    <div style="display: flex; align-items: center; gap: 8px;">
                      <span style="display: inline-block; width: 8px; height: 8px; border-radius: 50%;" :style="{ backgroundColor: editPriorityOption.dot }"></span>
                      <span style="font-size: 14px; font-weight: 500; color: var(--text-main);">{{ editPriorityOption.label }}</span>
                    </div>
                    <svg 
                      xmlns="http://www.w3.org/2000/svg" 
                      fill="none" 
                      viewBox="0 0 24 24" 
                      stroke-width="2.2" 
                      stroke="currentColor" 
                      style="width: 14px; height: 14px; transition: transform 0.2s; color: var(--text-muted);"
                      :style="{ transform: isEditPriorityDropdownOpen ? 'rotate(180deg)' : 'rotate(0deg)' }"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
                    </svg>
                  </div>
                  
                  <div 
                    v-if="isEditPriorityDropdownOpen" 
                    class="custom-dropdown-list"
                    style="position: absolute; top: calc(100% + 6px); left: 0; right: 0; background: rgba(255, 255, 255, 0.98); border: 1px solid rgba(0, 0, 0, 0.08); border-radius: var(--radius-md); box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.05); z-index: 999; padding: 6px; backdrop-filter: blur(12px); display: flex; flex-direction: column; gap: 4px;"
                  >
                    <div 
                      v-for="opt in priorityOptions" 
                      :key="opt.value"
                      @click="selectEditPriority(opt.value)"
                      class="custom-dropdown-item"
                      :class="{ 'is-selected': editForm.priority === opt.value }"
                      style="display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
                    >
                      <div style="display: flex; align-items: center; gap: 8px;">
                        <span style="display: inline-block; width: 6px; height: 6px; border-radius: 50%;" :style="{ backgroundColor: opt.dot }"></span>
                        <span style="font-size: 13.5px; font-weight: 500; color: var(--text-main);">{{ opt.label }}</span>
                      </div>
                      <svg 
                        v-if="editForm.priority === opt.value"
                        xmlns="http://www.w3.org/2000/svg" 
                        fill="none" 
                        viewBox="0 0 24 24" 
                        stroke-width="2.5" 
                        stroke="#2563eb" 
                        style="width: 14px; height: 14px;"
                      >
                        <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
                      </svg>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="modal-actions" style="margin-top: 8px; display: flex; justify-content: flex-end; gap: 12px; border-top: none; padding-top: 0; width: 100%;">
              <button type="button" class="btn btn-secondary" style="min-width: 80px; min-height: 38px; cursor: pointer; background-color: #f1f5f9; border: none; color: #475569; font-weight: 600; border-radius: 8px; transition: background-color 0.15s;" @click="closeEditModal">取消</button>
              <button type="submit" class="btn btn-primary" :style="{ backgroundColor: category?.color || '#2563eb' }" style="min-width: 100px; color: white; border: none; border-radius: 8px; font-weight: 600; cursor: pointer; min-height: 38px; transition: opacity 0.15s;">保存修改</button>
            </div>
          </form>
        </div>
      </div>
    </transition>

    <!-- Delete Confirmation Modal -->
    <transition name="modal-fade">
      <div v-if="isDeleteModalOpen" class="modal-overlay" @mousedown="handleOverlayMousedown" @click="handleOverlayClick($event, cancelDelete)">
        <div class="modal-card delete-confirm-modal" @click.stop style="max-width: 460px; padding: 24px;">
          <div style="display: flex; align-items: flex-start; gap: 16px; margin-bottom: 20px;">
            <div style="width: 40px; height: 40px; border-radius: 50%; background-color: #fffbeb; display: flex; align-items: center; justify-content: center; flex-shrink: 0;">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="#d97706" style="width: 22px; height: 22px;">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
              </svg>
            </div>
            <div style="flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 8px;">
              <h3 style="margin: 0; font-size: 18px; font-weight: 700; color: var(--text-main); display: flex; align-items: center; height: 40px;">
                删除待办事项
              </h3>
              <p style="margin: 0; font-size: 14px; color: var(--text-main); line-height: 1.6; margin-top: 8px;">
                确定要删除待办事项 <span style="color: #2563eb; font-weight: 600;">【{{ todoToDelete?.title }}】</span> 吗？
              </p>
            </div>
          </div>

          <!-- If done, show warning alert -->
          <div 
            v-if="todoToDelete?.status === 'done'" 
            style="display: flex; align-items: center; gap: 8px; background-color: #fff1f0; border: 1.5px solid #ffccc7; border-radius: 8px; padding: 10px 16px; margin-bottom: 24px;"
          >
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="#ff4d4f" style="width: 14px; height: 14px; flex-shrink: 0;">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
            </svg>
            <span style="font-size: 13px; font-weight: 600; color: #ff4d4f;">该待办事项已完成，确定要继续删除吗？</span>
          </div>

          <!-- Buttons -->
          <div style="display: flex; justify-content: flex-end; gap: 12px; margin-top: 16px;">
            <button 
              type="button" 
              class="btn btn-secondary" 
              style="min-width: 80px; min-height: 38px; cursor: pointer; background-color: #f1f5f9; border: none; color: #475569; font-weight: 600; border-radius: 8px; transition: background-color 0.15s; font-size: 13.5px;"
              @click="cancelDelete"
            >
              取消
            </button>
            <button 
              type="button" 
              class="btn btn-danger" 
              style="min-width: 100px; min-height: 38px; cursor: pointer; background-color: #ef4444; border: none; color: white; font-weight: 600; border-radius: 8px; transition: opacity 0.15s; font-size: 13.5px;"
              @click="confirmDelete"
            >
              确认删除
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- Add Todo Modal -->
    <transition name="modal-fade">
      <div v-if="isAddModalOpen" class="modal-overlay" @mousedown="handleOverlayMousedown" @click="handleOverlayClick($event, closeAddModal)">
        <div class="modal-card" @click.stop="closeDropdowns">
          <div class="modal-header">
            <h2>新建待办事项</h2>
            <button class="close-btn" @click="closeAddModal">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <form class="modal-form" @submit.prevent="handleAddTodoSubmit">
            <div class="form-field">
              <label for="add-title">待办标题 <span class="required-star">*</span></label>
              <input 
                id="add-title" 
                v-model="addForm.title" 
                type="text" 
                placeholder="想要做什么？" 
                required 
                class="form-control" 
                ref="addTitleInput"
                autocomplete="off"
              />
            </div>

            <div class="form-field">
              <label for="add-content">备注 (详细描述)</label>
              <textarea 
                id="add-content" 
                v-model="addForm.content" 
                placeholder="添加备注信息..." 
                rows="3" 
                class="form-control"
              ></textarea>
            </div>

            <div class="form-row">
              <!-- Subcategory Custom Select -->
              <div class="form-field">
                <label>子分类</label>
                <div class="custom-select-container" style="position: relative; width: 100%;" @click.stop>
                  <div 
                    class="form-control beautiful-select-trigger" 
                    @click="toggleModalSubcatDropdown"
                    :class="{ 'is-active': isModalSubcatDropdownOpen }"
                    style="display: flex; align-items: center; justify-content: space-between; cursor: pointer; padding: 0 14px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background: #fff; transition: all var(--transition-fast); height: 42px;"
                  >
                    <div style="display: flex; align-items: center; gap: 8px;">
                      <span style="display: inline-block; width: 8px; height: 8px; border-radius: 50%;" :style="{ backgroundColor: modalSubcatOption.color }"></span>
                      <span style="font-size: 14px; font-weight: 500; color: var(--text-main);">{{ modalSubcatOption.label }}</span>
                    </div>
                    <svg 
                      xmlns="http://www.w3.org/2000/svg" 
                      fill="none" 
                      viewBox="0 0 24 24" 
                      stroke-width="2.2" 
                      stroke="currentColor" 
                      style="width: 14px; height: 14px; transition: transform 0.2s; color: var(--text-muted);"
                      :style="{ transform: isModalSubcatDropdownOpen ? 'rotate(180deg)' : 'rotate(0deg)' }"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
                    </svg>
                  </div>
                  
                  <div 
                    v-if="isModalSubcatDropdownOpen" 
                    class="custom-dropdown-list"
                    style="position: absolute; top: calc(100% + 6px); left: 0; right: 0; background: rgba(255, 255, 255, 0.98); border: 1px solid rgba(0, 0, 0, 0.08); border-radius: var(--radius-md); box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.05); z-index: 999; padding: 6px; backdrop-filter: blur(12px); display: flex; flex-direction: column; gap: 4px; max-height: 200px; overflow-y: auto;"
                  >


                    <!-- Actual Subcategories -->
                    <div 
                      v-for="sub in subcategories" 
                      :key="sub.id"
                      @click="addForm.subcategoryId = sub.id; isModalSubcatDropdownOpen = false;"
                      class="custom-dropdown-item"
                      :class="{ 'is-selected': addForm.subcategoryId === sub.id }"
                      style="display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
                    >
                      <div style="display: flex; align-items: center; gap: 8px;">
                        <span style="display: inline-block; width: 6px; height: 6px; border-radius: 50%;" :style="{ backgroundColor: category?.color || '#4f46e5' }"></span>
                        <span style="font-size: 13px; font-weight: 500; color: var(--text-main);">{{ sub.name }}</span>
                      </div>
                      <svg 
                        v-if="addForm.subcategoryId === sub.id"
                        xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="#2563eb" style="width: 14px; height: 14px;"
                      >
                        <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
                      </svg>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Priority Custom Select -->
              <div class="form-field">
                <label>优先级</label>
                <div class="custom-select-container" style="position: relative; width: 100%;" @click.stop>
                  <div 
                    class="form-control beautiful-select-trigger" 
                    @click="toggleModalPriorityDropdown"
                    :class="{ 'is-active': isModalPriorityDropdownOpen }"
                    style="display: flex; align-items: center; justify-content: space-between; cursor: pointer; padding: 0 14px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background: #fff; transition: all var(--transition-fast); height: 42px;"
                  >
                    <div style="display: flex; align-items: center; gap: 8px;">
                      <span style="display: inline-block; width: 8px; height: 8px; border-radius: 50%;" :style="{ backgroundColor: modalPriorityOption.dot }"></span>
                      <span style="font-size: 14px; font-weight: 500; color: var(--text-main);">{{ modalPriorityOption.label }}</span>
                    </div>
                    <svg 
                      xmlns="http://www.w3.org/2000/svg" 
                      fill="none" 
                      viewBox="0 0 24 24" 
                      stroke-width="2.2" 
                      stroke="currentColor" 
                      style="width: 14px; height: 14px; transition: transform 0.2s; color: var(--text-muted);"
                      :style="{ transform: isModalPriorityDropdownOpen ? 'rotate(180deg)' : 'rotate(0deg)' }"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
                    </svg>
                  </div>
                  
                  <div 
                    v-if="isModalPriorityDropdownOpen" 
                    class="custom-dropdown-list"
                    style="position: absolute; top: calc(100% + 6px); left: 0; right: 0; background: rgba(255, 255, 255, 0.98); border: 1px solid rgba(0, 0, 0, 0.08); border-radius: var(--radius-md); box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.05); z-index: 999; padding: 6px; backdrop-filter: blur(12px); display: flex; flex-direction: column; gap: 4px;"
                  >
                    <div 
                      v-for="opt in priorityOptions" 
                      :key="opt.value"
                      @click="selectModalPriority(opt.value)"
                      class="custom-dropdown-item"
                      :class="{ 'is-selected': addForm.priority === opt.value }"
                      style="display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
                    >
                      <div style="display: flex; align-items: center; gap: 8px;">
                        <span style="display: inline-block; width: 6px; height: 6px; border-radius: 50%;" :style="{ backgroundColor: opt.dot }"></span>
                        <span style="font-size: 13.5px; font-weight: 500; color: var(--text-main);">{{ opt.label }}</span>
                      </div>
                      <svg 
                        v-if="addForm.priority === opt.value"
                        xmlns="http://www.w3.org/2000/svg" 
                        fill="none" 
                        viewBox="0 0 24 24" 
                        stroke-width="2.5" 
                        stroke="#2563eb" 
                        style="width: 14px; height: 14px;"
                      >
                        <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
                      </svg>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="modal-actions" style="margin-top: 8px; display: flex; justify-content: flex-end; gap: 12px; border-top: none; padding-top: 0; width: 100%;">
              <button type="button" class="btn btn-secondary" style="min-width: 80px; min-height: 38px; cursor: pointer; background-color: #f1f5f9; border: none; color: #475569; font-weight: 600; border-radius: 8px; transition: background-color 0.15s;" @click="closeAddModal">取消</button>
              <button type="submit" class="btn btn-primary" :style="{ backgroundColor: category?.color || '#2563eb' }" style="min-width: 100px; color: white; border: none; border-radius: 8px; font-weight: 600; cursor: pointer; min-height: 38px; transition: opacity 0.15s;">确认创建</button>
            </div>
          </form>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useTodoStore } from '@/stores/todo'
import type { Todo } from '@/stores/todo'

const route = useRoute()
const authStore = useAuthStore()
const todoStore = useTodoStore()

const todayStr = computed(() => new Date().toISOString().split('T')[0])

// Parsed Category Id from route
const categoryId = computed(() => Number(route.params.id))

// Category Details
const category = computed(() => todoStore.categories.find(c => c.id === categoryId.value))

// Subcategories under this category
const subcategories = computed(() => todoStore.subcategoriesByCategoryId(categoryId.value))

// Dropdown subcategory filter state
const selectedSubcategoryId = ref<number | null>(null)

// Status Tab filter state (now mapped to dropdown status)
const activeStatusTab = ref('all') // all | pending | done

const getLocalDateString = (d: Date) => {
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const getPastMonthDateStr = () => {
  const d = new Date()
  d.setMonth(d.getMonth() - 1)
  return getLocalDateString(d)
}

// Date Range Filter state (defaults to past 1 month)
const startDate = ref(getPastMonthDateStr())
const endDate = ref(getLocalDateString(new Date()))

const tempStartDate = ref('')
const tempEndDate = ref('')

// Dropdowns active states
const isSubcatDropdownOpen = ref(false)
const isDateDropdownOpen = ref(false)
const isStatusDropdownOpen = ref(false)

const toggleSubcatDropdown = () => {
  isSubcatDropdownOpen.value = !isSubcatDropdownOpen.value
  isDateDropdownOpen.value = false
  isStatusDropdownOpen.value = false
}

const toggleDateDropdown = () => {
  if (!isDateDropdownOpen.value) {
    tempStartDate.value = startDate.value
    tempEndDate.value = endDate.value
  }
  isDateDropdownOpen.value = !isDateDropdownOpen.value
  isSubcatDropdownOpen.value = false
  isStatusDropdownOpen.value = false
}

const applyDateRange = () => {
  startDate.value = tempStartDate.value
  endDate.value = tempEndDate.value
  isDateDropdownOpen.value = false
}

const clearDateRange = () => {
  startDate.value = ''
  endDate.value = ''
  tempStartDate.value = ''
  tempEndDate.value = ''
  isDateDropdownOpen.value = false
}

const toggleStatusDropdown = () => {
  isStatusDropdownOpen.value = !isStatusDropdownOpen.value
  isSubcatDropdownOpen.value = false
  isDateDropdownOpen.value = false
}

const closeDropdowns = () => {
  isSubcatDropdownOpen.value = false
  isDateDropdownOpen.value = false
  isStatusDropdownOpen.value = false
  isModalSubcatDropdownOpen.value = false
  isModalPriorityDropdownOpen.value = false
}

// Current subcategory option details
const currentSubcatOption = computed(() => {
  if (selectedSubcategoryId.value === null) {
    return { label: '全部子分类', color: '#64748b' }
  }
  const sub = subcategories.value.find(s => s.id === selectedSubcategoryId.value)
  return sub ? { label: sub.name, color: category.value?.color || '#4f46e5' } : { label: '全部子分类', color: '#64748b' }
})

// Status Options
const statusOptions = [
  { label: '全部状态', value: 'all', color: '#64748b' },
  { label: '未完成', value: 'pending', color: '#3b82f6' },
  { label: '已完成', value: 'done', color: '#10b981' }
]

const currentStatusOption = computed(() => {
  return statusOptions.find(o => o.value === activeStatusTab.value) || statusOptions[0]
})

// Date Range formatted label
const formattedDateRangeLabel = computed(() => {
  if (!startDate.value && !endDate.value) return '全部时间'
  if (startDate.value && !endDate.value) return `${startDate.value} 起`
  if (!startDate.value && endDate.value) return `${endDate.value} 止`
  return `${startDate.value} 至 ${endDate.value}`
})

// Drawer mode state
const isEditMode = ref(true)

// Watch categoryId change to reset filter
watch(categoryId, () => {
  selectedSubcategoryId.value = null
  activeStatusTab.value = 'all'
  startDate.value = getPastMonthDateStr()
  endDate.value = getLocalDateString(new Date())
  closeDropdowns()
})

// Pagination States
const pageNum = ref(1)
const totalCount = ref(0)
const hasMore = ref(true)
const loading = ref(false)

const fetchTodos = async (isAppend = false) => {
  if (loading.value) return
  loading.value = true
  
  try {
    if (authStore.currentUser) {
      const data = await todoStore.fetchTodosByCategory(
        authStore.currentUser.userId,
        categoryId.value,
        selectedSubcategoryId.value,
        startDate.value,
        endDate.value,
        activeStatusTab.value,
        pageNum.value
      )
      if (data) {
        totalCount.value = data.total || 0
        hasMore.value = filteredTodos.value.length < totalCount.value
      } else {
        hasMore.value = false
      }
    }
  } catch (e) {
    console.error('Failed to fetch category page:', e)
  } finally {
    loading.value = false
  }
}

watch([categoryId, selectedSubcategoryId, activeStatusTab, startDate, endDate], () => {
  pageNum.value = 1
  hasMore.value = true
  fetchTodos(false)
}, { immediate: true })

const handleScroll = () => {
  const scrollTop = window.scrollY || document.documentElement.scrollTop
  const windowHeight = window.innerHeight
  const scrollHeight = document.documentElement.scrollHeight
  
  if (scrollHeight - scrollTop - windowHeight < 100) {
    if (!loading.value && hasMore.value) {
      pageNum.value++
      fetchTodos(true)
    }
  }
}

// Categories
const categories = computed(() => todoStore.sortedCategories)

// Filtered todos (PRD 6.5.2)
const filteredTodos = computed(() => {
  return todoStore.todos.filter(t => {
    if (t.status === 'deleted') return false
    if (t.categoryId !== categoryId.value) return false
    
    // Subcategory filter
    if (selectedSubcategoryId.value !== null && t.subcategoryId !== selectedSubcategoryId.value) return false
    
    // Status filter
    if (activeStatusTab.value === 'pending' && t.status !== 'pending') return false
    if (activeStatusTab.value === 'done' && t.status !== 'done') return false
    
    // Date Range Filter
    if (startDate.value) {
      if (t.planDate < startDate.value) return false
    }
    if (endDate.value) {
      if (t.planDate > endDate.value) return false
    }
    
    return true
  })
})

const pendingTodosCount = computed(() => {
  return filteredTodos.value.filter(t => t.status === 'pending').length
})

// Render directly using backend's sorted results
const sortedTodos = computed(() => {
  return filteredTodos.value
})

onMounted(() => {
  if (authStore.currentUser) {
    todoStore.refreshCategories(authStore.currentUser.userId)
  }
  document.addEventListener('click', closeDropdowns)
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  document.removeEventListener('click', closeDropdowns)
  window.removeEventListener('scroll', handleScroll)
})

const isEditModalOpen = ref(false)
const editForm = reactive({
  id: 0,
  title: '',
  content: '',
  planDate: '',
  priority: 'medium' as 'high' | 'medium' | 'low',
  categoryId: null as number | null,
  subcategoryId: null as number | null,
  reminderTime: ''
})

const editSubcategories = computed(() => {
  if (!editForm.categoryId) return []
  return todoStore.subcategoriesByCategoryId(editForm.categoryId)
})

const truncate = (text: string, len: number) => {
  if (text.length <= len) return text
  return text.substring(0, len) + '...'
}

const statusLabel = (status: string) => {
  if (status === 'all') return '全部'
  if (status === 'pending') return '未完成'
  return '已完成'
}

const priorityLabel = (prio: string) => {
  if (prio === 'high') return '高'
  if (prio === 'medium') return '中'
  return '低'
}

const isOverdue = (todo: Todo) => {
  return todo.status === 'pending' && todo.planDate < todayStr.value
}

const getSubName = (todo: Todo) => {
  if (!todo.subcategoryId) return ''
  const sub = subcategories.value.find(s => s.id === todo.subcategoryId)
  return sub ? sub.name : ''
}

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

const toggleStatus = (todo: Todo) => {
  if (!authStore.currentUser) return
  const newStatus = todo.status === 'done' ? 'pending' : 'done'
  todoStore.updateTodo(authStore.currentUser.userId, todo.id, { status: newStatus })
  
  const text = newStatus === 'done' ? '任务标记完成' : '任务撤销完成'
  const event = new CustomEvent('app-toast', { detail: { text } })
  window.dispatchEvent(event)
}

const togglePin = (todo: Todo) => {
  if (!authStore.currentUser) return
  todoStore.updateTodo(authStore.currentUser.userId, todo.id, { isPinned: !todo.isPinned })
}

const isDeleteModalOpen = ref(false)
const todoToDelete = ref<Todo | null>(null)

const deleteTodoItem = (todo: Todo) => {
  todoToDelete.value = todo
  isDeleteModalOpen.value = true
}

const cancelDelete = () => {
  todoToDelete.value = null
  isDeleteModalOpen.value = false
}

const confirmDelete = () => {
  if (todoToDelete.value && authStore.currentUser) {
    todoStore.deleteTodo(authStore.currentUser.userId, todoToDelete.value.id)
    const event = new CustomEvent('app-toast', { detail: { text: '删除成功' } })
    window.dispatchEvent(event)
  }
  isDeleteModalOpen.value = false
  todoToDelete.value = null
}

// Add Todo Modal state
const isAddModalOpen = ref(false)
const addTitleInput = ref<HTMLInputElement | null>(null)
const addForm = reactive({
  title: '',
  content: '',
  planDate: '',
  priority: 'medium' as 'high' | 'medium' | 'low',
  categoryId: null as number | null,
  subcategoryId: null as number | null
})

const mousedownTarget = ref<EventTarget | null>(null)

const handleOverlayMousedown = (e: MouseEvent) => {
  mousedownTarget.value = e.target
}

const handleOverlayClick = (e: MouseEvent, closeFn: () => void) => {
  if (e.target === e.currentTarget && mousedownTarget.value === e.currentTarget) {
    closeFn()
  }
}

const isModalSubcatDropdownOpen = ref(false)
const isModalPriorityDropdownOpen = ref(false)

const toggleModalSubcatDropdown = () => {
  isModalSubcatDropdownOpen.value = !isModalSubcatDropdownOpen.value
  isModalPriorityDropdownOpen.value = false
}

const toggleModalPriorityDropdown = () => {
  isModalPriorityDropdownOpen.value = !isModalPriorityDropdownOpen.value
  isModalSubcatDropdownOpen.value = false
}

const modalSubcatOption = computed(() => {
  if (addForm.subcategoryId === null) {
    return { label: '无子分类', color: '#94a3b8' }
  }
  const sub = subcategories.value.find(s => s.id === addForm.subcategoryId)
  return sub ? { label: sub.name, color: category.value?.color || '#4f46e5' } : { label: '无子分类', color: '#94a3b8' }
})

const priorityOptions = [
  { value: 'high', label: '高优先级', dot: '#ef4444' },
  { value: 'medium', label: '中优先级', dot: '#f59e0b' },
  { value: 'low', label: '低优先级', dot: '#10b981' }
] as const

const modalPriorityOption = computed(() => {
  const opt = priorityOptions.find(o => o.value === addForm.priority)
  return opt || { value: 'medium', label: '中优先级', dot: '#f59e0b' }
})

const selectModalPriority = (prio: 'high' | 'medium' | 'low') => {
  addForm.priority = prio
  isModalPriorityDropdownOpen.value = false
}

const openAddModal = () => {
  addForm.title = ''
  addForm.content = ''
  addForm.planDate = todayStr.value
  addForm.priority = 'medium'
  addForm.categoryId = categoryId.value
  
  if (selectedSubcategoryId.value !== null) {
    addForm.subcategoryId = selectedSubcategoryId.value
  } else if (subcategories.value.length > 0) {
    addForm.subcategoryId = subcategories.value[0].id
  } else {
    addForm.subcategoryId = null
  }
  
  isModalSubcatDropdownOpen.value = false
  isModalPriorityDropdownOpen.value = false
  isAddModalOpen.value = true
  
  setTimeout(() => {
    if (addTitleInput.value) {
      addTitleInput.value.focus()
    }
  }, 100)
}

const closeAddModal = () => {
  isAddModalOpen.value = false
}

const handleAddTodoSubmit = () => {
  if (!authStore.currentUser || !addForm.title.trim()) return
  
  todoStore.addTodo(authStore.currentUser.userId, {
    title: addForm.title.trim(),
    content: addForm.content.trim(),
    planDate: addForm.planDate,
    priority: addForm.priority,
    status: 'pending',
    isPinned: false,
    reminderTime: null,
    categoryId: categoryId.value,
    subcategoryId: addForm.subcategoryId,
    doneAt: null,
    derivedFromType: null,
    derivedFromId: null
  })

  closeAddModal()
  
  const event = new CustomEvent('app-toast', { detail: { text: '待办事项创建成功！' } })
  window.dispatchEvent(event)
}

const isEditSubcatDropdownOpen = ref(false)
const isEditPriorityDropdownOpen = ref(false)

const toggleEditSubcatDropdown = () => {
  isEditSubcatDropdownOpen.value = !isEditSubcatDropdownOpen.value
  isEditPriorityDropdownOpen.value = false
}

const toggleEditPriorityDropdown = () => {
  isEditPriorityDropdownOpen.value = !isEditPriorityDropdownOpen.value
  isEditSubcatDropdownOpen.value = false
}

const editSubcatOption = computed(() => {
  if (editForm.subcategoryId === null) {
    return { label: '无子分类', color: '#94a3b8' }
  }
  const sub = subcategories.value.find(s => s.id === editForm.subcategoryId)
  return sub ? { label: sub.name, color: category.value?.color || '#4f46e5' } : { label: '无子分类', color: '#94a3b8' }
})

const editPriorityOption = computed(() => {
  const opt = priorityOptions.find(o => o.value === editForm.priority)
  return opt || { value: 'medium', label: '中优先级', dot: '#f59e0b' }
})

const selectEditPriority = (prio: 'high' | 'medium' | 'low') => {
  editForm.priority = prio
  isEditPriorityDropdownOpen.value = false
}

const openDetail = (todo: Todo) => {
  editForm.id = todo.id
  editForm.title = todo.title
  editForm.content = todo.content || ''
  editForm.planDate = todo.planDate
  editForm.priority = todo.priority
  editForm.categoryId = todo.categoryId
  
  if (todo.subcategoryId !== null) {
    editForm.subcategoryId = todo.subcategoryId
  } else if (subcategories.value.length > 0) {
    editForm.subcategoryId = subcategories.value[0].id
  } else {
    editForm.subcategoryId = null
  }

  if (todo.reminderTime) {
    const [d, t] = todo.reminderTime.split(' ')
    editForm.reminderTime = `${d}T${t}`
  } else {
    editForm.reminderTime = ''
  }

  isEditSubcatDropdownOpen.value = false
  isEditPriorityDropdownOpen.value = false
  isEditModalOpen.value = true
}

const closeEditModal = () => {
  isEditModalOpen.value = false
}

const onEditCategoryChange = () => {
  editForm.subcategoryId = null
}

const saveEditModal = () => {
  if (!authStore.currentUser) return
  
  let formattedReminder: string | null = null
  if (editForm.reminderTime) {
    formattedReminder = editForm.reminderTime.replace('T', ' ')
  }

  todoStore.updateTodo(authStore.currentUser.userId, editForm.id, {
    title: editForm.title.trim(),
    content: editForm.content.trim(),
    planDate: editForm.planDate,
    priority: editForm.priority,
    categoryId: editForm.categoryId,
    subcategoryId: editForm.subcategoryId,
    reminderTime: formattedReminder
  })
  
  const event = new CustomEvent('app-toast', { detail: { text: '保存待办修改成功！' } })
  window.dispatchEvent(event)
  closeEditModal()
}
</script>

<style scoped>
.category-view {
  max-width: 1100px;
  margin: 0 auto;
}

/* Header styling */
.category-header {
  position: relative;
  z-index: 100;
  padding: 24px 32px;
  margin-bottom: 24px;
}
.header-main-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.header-right {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}
.color-indicator {
  width: 16px;
  height: 36px;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
}
.title-details h1 {
  font-size: 22px;
  font-weight: 800;
  color: var(--text-main);
  margin-bottom: 2px;
}
.stats-subtitle {
  font-size: 13px;
  color: var(--text-muted);
  font-weight: 500;
}
.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
}
.filter-group label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-muted);
}
.filter-select {
  border: 1.5px solid var(--border-medium);
  border-radius: var(--radius-md);
  padding: 6px 12px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-main);
  background-color: var(--bg-app);
  outline: none;
  cursor: pointer;
}
.add-todo-btn {
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all var(--transition-fast);
  height: 36px;
}
.add-todo-btn:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}
@media (max-width: 1024px) {
  .header-main-row {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
  .header-right {
    justify-content: flex-start;
  }
}
@media (max-width: 480px) {
  .header-right {
    flex-direction: column;
    align-items: stretch;
  }
  .add-todo-btn {
    width: 100%;
    justify-content: center;
  }
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

.status-tabs {
  display: flex;
  background-color: var(--bg-app);
  padding: 3px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-medium);
}
.tab-btn {
  border: none;
  background: none;
  padding: 6px 16px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}
.tab-btn:hover {
  color: var(--text-main);
}
.tab-btn.active {
  background-color: #fff;
  color: var(--primary);
  box-shadow: var(--shadow-sm);
}

/* Quick create */
.quick-create-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 24px;
  margin-bottom: 24px;
}
.input-container {
  flex: 1;
}
.quick-input {
  width: 100%;
  border: none;
  background: transparent;
  outline: none;
  font-size: 15px;
  font-weight: 500;
  color: var(--text-main);
}
.quick-input::placeholder {
  color: var(--text-muted);
}
.quick-add-btn {
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all var(--transition-fast);
}
.quick-add-btn:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

/* Todos list */
.todos-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
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
.date-overdue {
  background-color: var(--danger-bg);
  color: var(--danger);
  font-weight: 700;
}
.subcat-badge {
  border: 1px solid transparent;
}
.rollover-badge {
  background-color: var(--warning-bg);
  color: var(--warning);
  font-weight: 700;
}

.todo-actions {
  display: flex;
  gap: 8px;
  opacity: 1;
}
@media (max-width: 768px) {
  .todo-actions {
    opacity: 1;
  }
}
.action-btn svg {
  width: 16px;
  height: 16px;
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
.edit-action-btn:hover {
  background-color: var(--primary-light);
  color: var(--primary) !important;
}

.kr-status-icon-wrap {
  cursor: pointer;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  margin: 0;
  background: none;
  border: none;
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

.custom-indicator-circle.not_started {
  border-color: var(--border-medium);
  background-color: #fff;
  color: #94a3b8;
}

.custom-indicator-circle.not_started:hover {
  border-color: #2563eb;
  color: #2563eb;
}

.custom-indicator-circle.done {
  border-color: #10b981;
  background-color: #10b981;
  color: #fff;
}

.check-mark {
  font-size: 12px;
  font-weight: 800;
  line-height: 1;
}

/* Edit Drawer Overlay */
.drawer-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(4px);
  z-index: 2000;
  display: flex;
  justify-content: flex-end;
}
.drawer-panel {
  width: 100%;
  max-width: 480px;
  background-color: var(--bg-card);
  height: 100vh;
  box-shadow: -10px 0 30px rgba(0, 0, 0, 0.1);
  padding: 32px;
  display: flex;
  flex-direction: column;
  animation: slideIn 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}
@keyframes slideIn {
  from { transform: translateX(100%); }
  to { transform: translateX(0); }
}

.drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
}
.drawer-header h2 {
  font-size: 18px;
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

.drawer-form {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
  overflow-y: auto;
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
  color: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
.btn-primary:hover {
  opacity: 0.9;
}

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

.modal-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
  padding-top: 20px;
  border-top: 1px solid var(--border-light);
}

.required-star {
  color: var(--danger);
  margin-left: 2px;
}

/* Modal Transition */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.25s ease;
}
.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-active .modal-card {
  animation: modalScaleIn 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.modal-fade-leave-active .modal-card {
  animation: modalScaleOut 0.2s cubic-bezier(0.36, 0.07, 0.19, 0.97);
}

@keyframes modalScaleIn {
  from {
    transform: scale(0.9) translateY(10px);
  }
  to {
    transform: scale(1) translateY(0);
  }
}

@keyframes modalScaleOut {
  from {
    transform: scale(1) translateY(0);
  }
  to {
    transform: scale(0.95) translateY(5px);
  }
}

.form-control.beautiful-date-input {
  cursor: pointer;
  position: relative;
}
.form-control.beautiful-date-input:not(.has-value):not(:focus)::-webkit-datetime-edit {
  color: transparent !important;
  opacity: 0;
}
.form-control.beautiful-date-input::before {
  content: "年/月/日";
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
  font-size: 13.5px;
  pointer-events: none;
  transition: opacity var(--transition-fast);
}
.form-control.beautiful-date-input:focus::before,
.form-control.beautiful-date-input.has-value::before {
  opacity: 0;
  display: none;
}
.form-control.beautiful-date-input::-webkit-calendar-picker-indicator {
  cursor: pointer;
  opacity: 0.65;
  transition: opacity var(--transition-fast);
}
.form-control.beautiful-date-input::-webkit-calendar-picker-indicator:hover {
  opacity: 1;
}
</style>
