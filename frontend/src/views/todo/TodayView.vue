<template>
  <div class="today-view">
    <!-- Header Summary Section (PRD 6.3.1) -->
    <div class="today-header premium-card">
      <div class="title-section">
        <h1>今日待办</h1>
        <p class="date-subtitle">{{ todayDateFormatted }}</p>
      </div>

      <!-- Stats progress bar and action buttons -->
      <div class="stats-and-actions">
        <div class="stats-bar premium-card">
          <div class="stats-info">
            <div class="stat-item">
              <span class="stat-val">{{ totalTodos }}</span>
              <span class="stat-lbl">今日总数</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-val text-success">{{ completedTodos }}</span>
              <span class="stat-lbl">已完成</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-val text-indigo">{{ completionRate }}%</span>
              <span class="stat-lbl">完成率</span>
            </div>
          </div>
        </div>

        <button class="add-todo-btn" @click="openAddModal">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
          </svg>
          <span>新增待办</span>
        </button>
      </div>
    </div>

    <!-- Todo List Grouped by Category -->
    <div class="todos-container">
      <div v-if="groupedTodos.length > 0" class="todo-groups-list">
        <!-- Parent Category Group Section -->
        <div 
          v-for="group in groupedTodos" 
          :key="group.id" 
          class="category-group-section"
          :style="{ borderLeftColor: group.color }"
        >
          <!-- Group Section Header -->
          <div class="group-header">
            <div class="group-title-row">
              <span class="group-color-dot" :style="{ backgroundColor: group.color }"></span>
              <h2 class="group-name">{{ group.name }}</h2>
              <span class="group-count-badge" :style="{ backgroundColor: `${group.color}12`, color: group.color }">
                {{ group.totalCount }}
              </span>
            </div>
          </div>

          <!-- Group Content Container -->
          <div class="group-content">
            <!-- 1. Direct parent todos (if any) -->
            <div v-if="group.todos.length > 0" class="subgroup-direct-todos">
              <div class="todo-list">
                <div 
                  v-for="todo in group.todos" 
                  :key="todo.id" 
                  class="todo-card premium-card"
                  :class="{ 
                    'todo-done': todo.status === 'done',
                    'todo-high-priority': todo.priority === 'high' && todo.status !== 'done'
                  }"
                  :style="todo.priority === 'high' && todo.status !== 'done' ? { borderLeft: `4px solid ${group.color}` } : {}"
                >
                  <!-- Custom Checkbox (PerformanceView circular style) -->
                  <div class="kr-status-icon-wrap" @click.stop="toggleStatus(todo)" style="align-self: flex-start; margin-top: 2px;">
                    <div class="custom-indicator-circle" :class="todo.status === 'done' ? 'done' : 'not_started'">
                      <span v-if="todo.status === 'done'" class="check-mark">✓</span>
                    </div>
                  </div>

                  <!-- Todo Details -->
                  <div class="todo-main-info" style="flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 4px;">
                    <div class="todo-title-row" style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
                      <h3 class="todo-title" style="margin: 0;">{{ todo.title }}</h3>
                      <span class="badge" :class="`badge-${todo.priority}`" style="margin-left: 4px; flex-shrink: 0; font-size: 10px; padding: 2px 6px;">
                        {{ priorityLabel(todo.priority) }}
                      </span>
                      <span v-if="isHistorical(todo)" class="badge historical-badge" style="margin-left: 4px; flex-shrink: 0; font-size: 10px; padding: 2px 6px; background-color: #fff2e8; border: 1px solid #ffbb96; color: #fa541c; font-weight: 600;" :title="`这是历史待办（创建于${getHistoricalDateLabel(todo)}），请优先处理！`">
                        📅 {{ getHistoricalDateLabel(todo) }}
                      </span>
                    </div>
                    <p v-if="todo.content" class="todo-desc" style="margin: 0; padding-left: 0;">{{ truncate(todo.content, 80) }}</p>
                    
                    <div class="todo-badges" style="margin-top: 2px; padding-left: 0;">
                      <!-- Rollover Badge -->
                      <span v-if="todo.rolloverCount > 0" class="badge rollover-badge" title="未按期完成，自动延续到今天">
                        ♻️ 延续 {{ todo.rolloverCount }} 次
                      </span>

                      <!-- Linkage badge if derived -->
                      <span v-if="todo.derivedFromType" class="badge derived-badge" :title="`派生自：${todo.derivedFromType === 'performance' ? '月度绩效' : '团队冲刺'}`">
                        🔗 规划派生
                      </span>
                    </div>
                  </div>

                  <!-- Action buttons -->
                  <div class="todo-actions" @click.stop>
                    <!-- Edit Button -->
                    <button class="action-btn edit-action-btn" @click="openDetail(todo)" title="编辑待办事项">
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.2" stroke="currentColor" style="width: 16px; height: 16px;">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10" />
                      </svg>
                    </button>
                    <!-- Delete Button -->
                    <button class="action-btn delete-btn" @click="deleteTodoItem(todo)" title="删除待办事项">
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" />
                      </svg>
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 2. Subcategory subgroups -->
            <div 
              v-for="subgroup in group.subgroups" 
              :key="subgroup.id" 
              class="subcategory-subgroup-panel"
            >
              <div class="subcategory-header">
                <div class="subcategory-title-row">
                  <span class="subcategory-bullet" :style="{ backgroundColor: group.color }"></span>
                  <span class="subcategory-name">{{ subgroup.name }}</span>
                  <span class="subcategory-count-badge">{{ subgroup.todos.length }}</span>
                </div>
              </div>

              <div class="todo-list">
                <div 
                  v-for="todo in subgroup.todos" 
                  :key="todo.id" 
                  class="todo-card premium-card"
                  :class="{ 
                    'todo-done': todo.status === 'done',
                    'todo-high-priority': todo.priority === 'high' && todo.status !== 'done'
                  }"
                  :style="todo.priority === 'high' && todo.status !== 'done' ? { borderLeft: `4px solid ${group.color}` } : {}"
                >
                  <!-- Custom Checkbox (PerformanceView circular style) -->
                  <div class="kr-status-icon-wrap" @click.stop="toggleStatus(todo)" style="align-self: flex-start; margin-top: 2px;">
                    <div class="custom-indicator-circle" :class="todo.status === 'done' ? 'done' : 'not_started'">
                      <span v-if="todo.status === 'done'" class="check-mark">✓</span>
                    </div>
                  </div>

                  <!-- Todo Details -->
                  <div class="todo-main-info" style="flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 4px;">
                    <div class="todo-title-row" style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
                      <h3 class="todo-title" style="margin: 0;">{{ todo.title }}</h3>
                      <span class="badge" :class="`badge-${todo.priority}`" style="margin-left: 4px; flex-shrink: 0; font-size: 10px; padding: 2px 6px;">
                        {{ priorityLabel(todo.priority) }}
                      </span>
                      <span v-if="isHistorical(todo)" class="badge historical-badge" style="margin-left: 4px; flex-shrink: 0; font-size: 10px; padding: 2px 6px; background-color: #fff2e8; border: 1px solid #ffbb96; color: #fa541c; font-weight: 600;" :title="`这是历史待办（创建于${getHistoricalDateLabel(todo)}），请优先处理！`">
                        📅 {{ getHistoricalDateLabel(todo) }}
                      </span>
                    </div>
                    <p v-if="todo.content" class="todo-desc" style="margin: 0; padding-left: 0;">{{ truncate(todo.content, 80) }}</p>
                    
                    <div class="todo-badges" style="margin-top: 2px; padding-left: 0;">
                      <!-- Rollover Badge -->
                      <span v-if="todo.rolloverCount > 0" class="badge rollover-badge" title="未按期完成，自动延续到今天">
                        ♻️ 延续 {{ todo.rolloverCount }} 次
                      </span>

                      <!-- Linkage badge if derived -->
                      <span v-if="todo.derivedFromType" class="badge derived-badge" :title="`派生自：${todo.derivedFromType === 'performance' ? '月度绩效' : '团队冲刺'}`">
                        🔗 规划派生
                      </span>
                    </div>
                  </div>

                  <!-- Action buttons -->
                  <div class="todo-actions" @click.stop>
                    <!-- Edit Button -->
                    <button class="action-btn edit-action-btn" @click="openDetail(todo)" title="编辑待办事项">
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.2" stroke="currentColor" style="width: 16px; height: 16px;">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10" />
                      </svg>
                    </button>
                    <!-- Delete Button -->
                    <button class="action-btn delete-btn" @click="deleteTodoItem(todo)" title="删除待办事项">
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
      <div v-else class="empty-state premium-card">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.2" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12c0 1.268-.63 2.39-1.593 3.068a3.745 3.745 0 01-1.043 3.296 3.745 3.745 0 01-3.296 1.043A3.745 3.745 0 0110 21c-1.268 0-2.39-.63-3.068-1.593a3.746 3.746 0 01-3.296-1.043 3.745 3.745 0 01-1.043-3.296A3.745 3.745 0 013 12c0-1.268.63-2.39 1.593-3.068a3.745 3.745 0 011.043-3.296 3.746 3.746 0 013.296-1.043A3.746 3.746 0 0114 3c1.268 0 2.39.63 3.068 1.593a3.746 3.746 0 013.296 1.043 3.746 3.746 0 011.043 3.296A3.745 3.745 0 0121 12z" />
        </svg>
        <h3>今日无待办事项</h3>
        <p>今天已经全部清空了，去享受生活吧 🎉 或者在上方新建一条事项！</p>
      </div>
    </div>

    <!-- Edit Detail Modal -->
    <transition name="modal-fade">
      <div v-if="isDrawerOpen" class="modal-overlay" @mousedown="handleOverlayMousedown" @click="handleOverlayClick($event, closeDrawer)">
        <div class="modal-card" @click.stop="closeEditDropdowns">
          <div class="modal-header">
            <h2>编辑待办事项</h2>
            <button class="close-btn" @click="closeDrawer">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <form class="modal-form" @submit.prevent="saveDrawerEdit">
            <div class="form-field">
              <label for="edit-title">待办标题 <span class="required-star">*</span></label>
              <input id="edit-title" v-model="editForm.title" type="text" required class="form-control" placeholder="想要做什么？" autocomplete="off" />
            </div>

            <div class="form-field">
              <label for="edit-content">备注 (详细描述)</label>
              <textarea id="edit-content" v-model="editForm.content" rows="4" class="form-control" placeholder="添加备忘或详细描述..."></textarea>
            </div>

            <div class="form-row">
              <!-- Category Custom Select -->
              <div class="form-field">
                <label>归属分类</label>
                <div class="custom-select-container" style="position: relative; width: 100%;">
                  <div 
                    class="form-control beautiful-select-trigger" 
                    @click.stop="toggleEditCategoryDropdown"
                    :class="{ 'is-active': isEditCategoryDropdownOpen }"
                    style="display: flex; align-items: center; justify-content: space-between; cursor: pointer; padding: 10px 14px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background: #fff; transition: all var(--transition-fast); height: 42px;"
                  >
                    <div style="display: flex; align-items: center; gap: 8px;">
                      <span style="display: inline-block; width: 8px; height: 8px; border-radius: 50%;" :style="{ backgroundColor: currentEditCategoryOption.color }"></span>
                      <span style="font-size: 14px; font-weight: 500; color: var(--text-main);">{{ currentEditCategoryOption.name }}</span>
                    </div>
                    <svg 
                      xmlns="http://www.w3.org/2000/svg" 
                      fill="none" 
                      viewBox="0 0 24 24" 
                      stroke-width="2.2" 
                      stroke="currentColor" 
                      style="width: 14px; height: 14px; transition: transform 0.2s; color: var(--text-muted);"
                      :style="{ transform: isEditCategoryDropdownOpen ? 'rotate(180deg)' : 'rotate(0deg)' }"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
                    </svg>
                  </div>
                  
                  <div 
                    v-if="isEditCategoryDropdownOpen" 
                    class="custom-dropdown-list"
                    style="position: absolute; top: calc(100% + 6px); left: 0; right: 0; background: rgba(255, 255, 255, 0.98); border: 1px solid rgba(0, 0, 0, 0.08); border-radius: var(--radius-md); box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.05); z-index: 999; padding: 6px; backdrop-filter: blur(12px); display: flex; flex-direction: column; gap: 4px; max-height: 200px; overflow-y: auto;"
                  >
                    <template v-for="cat in categories" :key="cat.id">
                      <!-- Parent Category Option -->
                      <div 
                        @click="selectEditCategory(cat.id, null)"
                        class="custom-dropdown-item parent-category-item"
                        :class="{ 'is-selected': editForm.categoryId === cat.id && editForm.subcategoryId === null }"
                        style="display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
                      >
                        <div style="display: flex; align-items: center; gap: 8px;">
                          <span style="display: inline-block; width: 6px; height: 6px; border-radius: 50%;" :style="{ backgroundColor: cat.color }"></span>
                          <span style="font-size: 13.5px; font-weight: 600; color: var(--text-main);">{{ cat.name }}</span>
                        </div>
                        <svg 
                          v-if="editForm.categoryId === cat.id && editForm.subcategoryId === null"
                          xmlns="http://www.w3.org/2000/svg" 
                          fill="none" 
                          viewBox="0 0 24 24" 
                          stroke-width="2.5" 
                          stroke="var(--primary)" 
                          style="width: 14px; height: 14px;"
                        >
                          <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
                        </svg>
                      </div>

                      <!-- Subcategories -->
                      <div 
                        v-for="sub in todoStore.subcategoriesByCategoryId(cat.id)" 
                        :key="sub.id"
                        @click="selectEditCategory(cat.id, sub.id)"
                        class="custom-dropdown-item subcategory-item"
                        :class="{ 'is-selected': editForm.categoryId === cat.id && editForm.subcategoryId === sub.id }"
                        style="display: flex; align-items: center; justify-content: space-between; padding: 8px 12px 8px 28px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
                      >
                        <div style="display: flex; align-items: center; gap: 8px;">
                          <span style="display: inline-block; width: 4px; height: 4px; border-radius: 50%;" :style="{ backgroundColor: `${cat.color}aa` }"></span>
                          <span style="font-size: 13px; font-weight: 500; color: var(--text-muted);">{{ sub.name }}</span>
                        </div>
                        <svg 
                          v-if="editForm.categoryId === cat.id && editForm.subcategoryId === sub.id"
                          xmlns="http://www.w3.org/2000/svg" 
                          fill="none" 
                          viewBox="0 0 24 24" 
                          stroke-width="2.5" 
                          stroke="var(--primary)" 
                          style="width: 14px; height: 14px;"
                        >
                          <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
                        </svg>
                      </div>
                    </template>
                  </div>
                </div>
              </div>

              <!-- Priority Select -->
              <div class="form-field">
                <label>优先级</label>
                <div class="custom-select-container" style="position: relative; width: 100%;">
                  <div 
                    class="form-control beautiful-select-trigger" 
                    @click.stop="toggleEditPriorityDropdown"
                    :class="{ 'is-active': isEditPriorityDropdownOpen }"
                    style="display: flex; align-items: center; justify-content: space-between; cursor: pointer; padding: 10px 14px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background: #fff; transition: all var(--transition-fast); height: 42px;"
                  >
                    <div style="display: flex; align-items: center; gap: 8px;">
                      <span style="display: inline-block; width: 8px; height: 8px; border-radius: 50%;" :style="{ backgroundColor: currentEditPriorityOption.dot }"></span>
                      <span style="font-size: 14px; font-weight: 500; color: var(--text-main);">{{ currentEditPriorityOption.label }}</span>
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
                        stroke="var(--primary)" 
                        style="width: 14px; height: 14px;"
                      >
                        <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
                      </svg>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Metadata info -->
            <div class="metadata-section" style="background-color: var(--bg-app); padding: 12px 16px; border-radius: var(--radius-md); font-size: 13px; color: var(--text-muted); display: flex; flex-direction: column; gap: 8px; width: 100%;">
              <div style="display: flex; justify-content: space-between;">
                <span>创建时间：</span>
                <span>{{ formatDateTime(editForm.createdAt) }}</span>
              </div>
              <div v-if="editForm.status === 'done' && editForm.doneAt" style="display: flex; justify-content: space-between;">
                <span>完成时间：</span>
                <span class="text-success" style="color: var(--success); font-weight: 600;">{{ formatDateTime(editForm.doneAt) }}</span>
              </div>
            </div>

            <div class="modal-actions">
              <button type="button" class="btn btn-secondary" @click="closeDrawer">取消</button>
              <button type="submit" class="btn btn-primary">保存修改</button>
            </div>
          </form>
        </div>
      </div>
    </transition>

    <!-- Custom Delete Confirmation Modal -->
    <transition name="modal-fade">
      <div v-if="isDeleteConfirmOpen" class="modal-overlay" @mousedown="handleOverlayMousedown" @click="handleOverlayClick($event, cancelDelete)">
        <div class="modal-card" style="max-width: 480px; padding: 32px; border-radius: 12px; background-color: var(--bg-card); box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04); display: flex; flex-direction: column;" @click.stop>
          
          <!-- Header (horizontal: icon + title) -->
          <div style="display: flex; align-items: center; gap: 16px; width: 100%; margin-bottom: 24px;">
            <div style="width: 40px; height: 40px; border-radius: 50%; background-color: #fffbeb; display: flex; align-items: center; justify-content: center; color: #d97706; flex-shrink: 0;">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" style="width: 20px; height: 20px;">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
            </div>
            <h3 style="font-size: 20px; font-weight: 700; color: var(--text-main); margin: 0; line-height: 1;">删除待办事项</h3>
          </div>

          <!-- Message (left aligned) -->
          <p style="font-size: 15px; color: var(--text-main); line-height: 1.6; margin: 0 0 20px 0; text-align: left;">
            确定要删除待办事项 <strong style="color: var(--primary);">【{{ todoToDelete?.title }}】</strong> 吗？
          </p>

          <!-- Completed Todo Warning Alert Container -->
          <div v-if="todoToDelete?.status === 'done'" style="background-color: #fef2f2; border: 1px solid #fee2e2; border-radius: 8px; padding: 12px 16px; text-align: left; margin-bottom: 24px;">
            <p style="font-size: 13px; color: #ef4444; line-height: 1.5; margin: 0; display: flex; align-items: flex-start; gap: 8px;">
              <span style="flex-shrink: 0; font-size: 14px; margin-top: -1px;">⚠️</span>
              <span style="font-weight: 600;">该待办事项已完成，确定要继续删除吗？</span>
            </p>
          </div>

          <!-- Modal Actions (right aligned cancel and confirm) -->
          <div class="modal-actions" style="margin-top: 8px; display: flex; justify-content: flex-end; gap: 12px; border-top: none; padding-top: 0; width: 100%;">
            <button type="button" class="btn btn-secondary" style="min-width: 80px; min-height: 38px; cursor: pointer; background-color: #f1f5f9; border: none; color: #475569; font-weight: 600; border-radius: 8px; transition: background-color 0.15s;" @click="cancelDelete">取消</button>
            <button type="button" class="btn btn-danger" style="min-width: 100px; background-color: #ef4444; color: white; border: none; border-radius: 8px; font-weight: 600; cursor: pointer; min-height: 38px; transition: background-color 0.15s;" @click="confirmDelete">确认删除</button>
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
                rows="4" 
                placeholder="添加备忘或详细描述..." 
                class="form-control"
              ></textarea>
            </div>

            <div class="form-row">
              <!-- Category Custom Select -->
              <div class="form-field">
                <label>归属分类</label>
                <div class="custom-select-container" style="position: relative; width: 100%;">
                  <div 
                    class="form-control beautiful-select-trigger" 
                    @click.stop="toggleCategoryDropdown"
                    :class="{ 'is-active': isCategoryDropdownOpen }"
                    style="display: flex; align-items: center; justify-content: space-between; cursor: pointer; padding: 10px 14px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background: #fff; transition: all var(--transition-fast); height: 42px;"
                  >
                    <div style="display: flex; align-items: center; gap: 8px;">
                      <span style="display: inline-block; width: 8px; height: 8px; border-radius: 50%;" :style="{ backgroundColor: currentCategoryOption.color }"></span>
                      <span style="font-size: 14px; font-weight: 500; color: var(--text-main);">{{ currentCategoryOption.name }}</span>
                    </div>
                    <svg 
                      xmlns="http://www.w3.org/2000/svg" 
                      fill="none" 
                      viewBox="0 0 24 24" 
                      stroke-width="2.2" 
                      stroke="currentColor" 
                      style="width: 14px; height: 14px; transition: transform 0.2s; color: var(--text-muted);"
                      :style="{ transform: isCategoryDropdownOpen ? 'rotate(180deg)' : 'rotate(0deg)' }"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
                    </svg>
                  </div>
                  
                  <div 
                    v-if="isCategoryDropdownOpen" 
                    class="custom-dropdown-list"
                    style="position: absolute; top: calc(100% + 6px); left: 0; right: 0; background: rgba(255, 255, 255, 0.98); border: 1px solid rgba(0, 0, 0, 0.08); border-radius: var(--radius-md); box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.05); z-index: 999; padding: 6px; backdrop-filter: blur(12px); display: flex; flex-direction: column; gap: 4px; max-height: 200px; overflow-y: auto;"
                  >
                    <template v-for="cat in categories" :key="cat.id">
                      <!-- Parent Category Option -->
                      <div 
                        @click="selectCategory(cat.id, null)"
                        class="custom-dropdown-item parent-category-item"
                        :class="{ 'is-selected': addForm.categoryId === cat.id && addForm.subcategoryId === null }"
                        style="display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
                      >
                        <div style="display: flex; align-items: center; gap: 8px;">
                          <span style="display: inline-block; width: 6px; height: 6px; border-radius: 50%;" :style="{ backgroundColor: cat.color }"></span>
                          <span style="font-size: 13.5px; font-weight: 600; color: var(--text-main);">{{ cat.name }}</span>
                        </div>
                        <svg 
                          v-if="addForm.categoryId === cat.id && addForm.subcategoryId === null"
                          xmlns="http://www.w3.org/2000/svg" 
                          fill="none" 
                          viewBox="0 0 24 24" 
                          stroke-width="2.5" 
                          stroke="var(--primary)" 
                          style="width: 14px; height: 14px;"
                        >
                          <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
                        </svg>
                      </div>

                      <!-- Subcategories -->
                      <div 
                        v-for="sub in todoStore.subcategoriesByCategoryId(cat.id)" 
                        :key="sub.id"
                        @click="selectCategory(cat.id, sub.id)"
                        class="custom-dropdown-item subcategory-item"
                        :class="{ 'is-selected': addForm.categoryId === cat.id && addForm.subcategoryId === sub.id }"
                        style="display: flex; align-items: center; justify-content: space-between; padding: 8px 12px 8px 28px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
                      >
                        <div style="display: flex; align-items: center; gap: 8px;">
                          <span style="display: inline-block; width: 4px; height: 4px; border-radius: 50%;" :style="{ backgroundColor: `${cat.color}aa` }"></span>
                          <span style="font-size: 13px; font-weight: 500; color: var(--text-muted);">{{ sub.name }}</span>
                        </div>
                        <svg 
                          v-if="addForm.categoryId === cat.id && addForm.subcategoryId === sub.id"
                          xmlns="http://www.w3.org/2000/svg" 
                          fill="none" 
                          viewBox="0 0 24 24" 
                          stroke-width="2.5" 
                          stroke="var(--primary)" 
                          style="width: 14px; height: 14px;"
                        >
                          <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
                        </svg>
                      </div>
                    </template>
                  </div>
                </div>
              </div>
              
              <!-- Priority Custom Select -->
              <div class="form-field">
                <label>优先级</label>
                <div class="custom-select-container" style="position: relative; width: 100%;">
                  <div 
                    class="form-control beautiful-select-trigger" 
                    @click.stop="togglePriorityDropdown"
                    :class="{ 'is-active': isPriorityDropdownOpen }"
                    style="display: flex; align-items: center; justify-content: space-between; cursor: pointer; padding: 10px 14px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background: #fff; transition: all var(--transition-fast); height: 42px;"
                  >
                    <div style="display: flex; align-items: center; gap: 8px;">
                      <span style="display: inline-block; width: 8px; height: 8px; border-radius: 50%;" :style="{ backgroundColor: currentPriorityOption.dot }"></span>
                      <span style="font-size: 14px; font-weight: 500; color: var(--text-main);">{{ currentPriorityOption.label }}</span>
                    </div>
                    <svg 
                      xmlns="http://www.w3.org/2000/svg" 
                      fill="none" 
                      viewBox="0 0 24 24" 
                      stroke-width="2.2" 
                      stroke="currentColor" 
                      style="width: 14px; height: 14px; transition: transform 0.2s; color: var(--text-muted);"
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
                      :key="opt.value"
                      @click="selectPriority(opt.value)"
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
                        stroke="var(--primary)" 
                        style="width: 14px; height: 14px;"
                      >
                        <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
                      </svg>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="modal-actions">
              <button type="button" class="btn btn-secondary" @click="closeAddModal">取消</button>
              <button type="submit" class="btn btn-primary">确定创建</button>
            </div>
          </form>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useTodoStore } from '@/stores/todo'
import type { Todo } from '@/stores/todo'

const authStore = useAuthStore()
const todoStore = useTodoStore()

const todayStr = computed(() => new Date().toISOString().split('T')[0])

// Format current header date
const todayDateFormatted = computed(() => {
  const options: Intl.DateTimeFormatOptions = { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }
  return new Date().toLocaleDateString('zh-CN', options)
})

// Categories
const categories = computed(() => todoStore.sortedCategories)

// Todos for today (filtered & sorted)
const todayTodos = computed(() => {
  return todoStore.todos
    .filter(t => t.status !== 'deleted')
    .sort((a, b) => {
      // Sort order (PRD 4.6): Pin > Priority (high > medium > low) > status (pending > done) > createdAt asc
      if (a.isPinned !== b.isPinned) return a.isPinned ? -1 : 1
      
      const priorityWeight = { high: 3, medium: 2, low: 1 }
      const weightA = priorityWeight[a.priority] || 2
      const weightB = priorityWeight[b.priority] || 2
      if (weightA !== weightB) return weightB - weightA

      if (a.status !== b.status) return a.status === 'pending' ? -1 : 1
      
      return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
    })
})
const sortedTodos = computed(() => todayTodos.value)

// Stats (PRD 6.3.1)
const totalTodos = computed(() => todayTodos.value.length)
const completedTodos = computed(() => todayTodos.value.filter(t => t.status === 'done').length)
const completionRate = computed(() => {
  if (totalTodos.value === 0) return 0
  return Math.round((completedTodos.value / totalTodos.value) * 100)
})

// Grouped todos by Category (Parent & Subcategory)
interface TodoGroup {
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

const groupedTodos = computed(() => {
  const groupsMap: Record<string | number, TodoGroup> = {}
  
  // Uncategorized group
  const uncategorizedGroup: TodoGroup = {
    id: 'uncategorized',
    name: '未分类',
    color: '#64748b',
    todos: [],
    subgroups: [],
    totalCount: 0
  }

  sortedTodos.value.forEach(todo => {
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
  const result: TodoGroup[] = []
  
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

// Add Todo Modal State
const isAddModalOpen = ref(false)
const addTitleInput = ref<HTMLInputElement | null>(null)
const addForm = reactive({
  title: '',
  content: '',
  categoryId: null as number | null,
  subcategoryId: null as number | null,
  priority: 'medium' as 'high' | 'medium' | 'low'
})

// Custom dropdown selectors for Add Todo Modal
const isCategoryDropdownOpen = ref(false)
const isPriorityDropdownOpen = ref(false)

const toggleCategoryDropdown = () => {
  isCategoryDropdownOpen.value = !isCategoryDropdownOpen.value
  isPriorityDropdownOpen.value = false
}

const togglePriorityDropdown = () => {
  isPriorityDropdownOpen.value = !isPriorityDropdownOpen.value
  isCategoryDropdownOpen.value = false
}

const selectCategory = (catId: number | null, subId: number | null = null) => {
  addForm.categoryId = catId
  addForm.subcategoryId = subId
  isCategoryDropdownOpen.value = false
}

const selectPriority = (prio: 'high' | 'medium' | 'low') => {
  addForm.priority = prio
  isPriorityDropdownOpen.value = false
}

const closeDropdowns = () => {
  isCategoryDropdownOpen.value = false
  isPriorityDropdownOpen.value = false
}

const currentCategoryOption = computed(() => {
  if (addForm.categoryId === null) {
    return { name: '选择分类', color: '#94a3b8' }
  }
  const cat = categories.value.find(c => c.id === addForm.categoryId)
  if (!cat) return { name: '选择分类', color: '#94a3b8' }
  
  if (addForm.subcategoryId !== null) {
    const subList = todoStore.subcategoriesByCategoryId(addForm.categoryId)
    const sub = subList.find(s => s.id === addForm.subcategoryId)
    if (sub) {
      return { name: `${cat.name} > ${sub.name}`, color: cat.color }
    }
  }
  return { name: cat.name, color: cat.color }
})

const priorityOptions = [
  { value: 'high', label: '高优先级', dot: 'var(--priority-high)' },
  { value: 'medium', label: '中优先级', dot: 'var(--priority-medium)' },
  { value: 'low', label: '低优先级', dot: 'var(--priority-low)' }
] as const

const currentPriorityOption = computed(() => {
  const opt = priorityOptions.find(o => o.value === addForm.priority)
  return opt || { value: 'medium', label: '中优先级', dot: 'var(--priority-medium)' }
})

// Drawer edit state
const isDrawerOpen = ref(false)
const editForm = reactive({
  id: 0,
  title: '',
  content: '',
  planDate: '',
  priority: 'medium' as 'high' | 'medium' | 'low',
  categoryId: null as number | null,
  subcategoryId: null as number | null,
  createdAt: '',
  status: 'pending' as 'pending' | 'done' | 'deleted',
  doneAt: null as string | null,
  rolloverCount: 0,
  reminderTime: ''
})

const isEditCategoryDropdownOpen = ref(false)
const isEditPriorityDropdownOpen = ref(false)

const toggleEditCategoryDropdown = () => {
  isEditCategoryDropdownOpen.value = !isEditCategoryDropdownOpen.value
  isEditPriorityDropdownOpen.value = false
}

const selectEditCategory = (catId: number | null, subId: number | null = null) => {
  editForm.categoryId = catId
  editForm.subcategoryId = subId
  isEditCategoryDropdownOpen.value = false
}

const toggleEditPriorityDropdown = () => {
  isEditPriorityDropdownOpen.value = !isEditPriorityDropdownOpen.value
  isEditCategoryDropdownOpen.value = false
}

const selectEditPriority = (val: 'high' | 'medium' | 'low') => {
  editForm.priority = val
  isEditPriorityDropdownOpen.value = false
}

const currentEditPriorityOption = computed(() => {
  return priorityOptions.find(opt => opt.value === editForm.priority) || priorityOptions[1]
})

const closeEditDropdowns = () => {
  isEditCategoryDropdownOpen.value = false
  isEditPriorityDropdownOpen.value = false
}

const currentEditCategoryOption = computed(() => {
  if (editForm.categoryId === null) {
    return { name: '选择分类', color: '#94a3b8' }
  }
  const cat = categories.value.find(c => c.id === editForm.categoryId)
  if (!cat) return { name: '选择分类', color: '#94a3b8' }
  
  if (editForm.subcategoryId !== null) {
    const subList = todoStore.subcategoriesByCategoryId(editForm.categoryId)
    const sub = subList.find(s => s.id === editForm.subcategoryId)
    if (sub) {
      return { name: `${cat.name} > ${sub.name}`, color: cat.color }
    }
  }
  return { name: cat.name, color: cat.color }
})

onMounted(() => {
  if (authStore.currentUser) {
    todoStore.refreshCategories(authStore.currentUser.userId)
    todoStore.refreshTodayTodos(authStore.currentUser.userId)
    
    // Default to first category if exists
    addForm.priority = 'medium'
    addForm.categoryId = categories.value.length > 0 ? categories.value[0].id : null
    addForm.subcategoryId = null
  }
})

// Watch categories to select first one if no choice made yet
watch(categories, (newCats) => {
  if (authStore.currentUser) {
    if (addForm.categoryId === null && newCats.length > 0) {
      addForm.categoryId = newCats[0].id
      addForm.subcategoryId = null
    }
  }
}, { immediate: true })

// Helper methods
const truncate = (text: string, len: number) => {
  if (text.length <= len) return text
  return text.substring(0, len) + '...'
}

const formatDateTime = (isoStr: string) => {
  if (!isoStr) return '-'
  return new Date(isoStr).toLocaleString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const isHistorical = (todo: Todo) => {
  if (!todo.createdAt) return false
  const createdDate = new Date(todo.createdAt)
  const createdDay = new Date(createdDate.getFullYear(), createdDate.getMonth(), createdDate.getDate())
  const today = new Date()
  const todayDay = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  return createdDay.getTime() < todayDay.getTime()
}

const getHistoricalDateLabel = (todo: Todo) => {
  if (!todo.createdAt) return ''
  const dateObj = new Date(todo.createdAt)
  const month = dateObj.getMonth() + 1
  const date = dateObj.getDate()
  return `${month}月${date}日`
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

// Modal drag-to-dismiss prevention helpers
const mousedownTarget = ref<EventTarget | null>(null)

const handleOverlayMousedown = (e: MouseEvent) => {
  mousedownTarget.value = e.target
}

const handleOverlayClick = (e: MouseEvent, closeCallback: () => void) => {
  if (e.target === e.currentTarget && mousedownTarget.value === e.currentTarget) {
    closeCallback()
  }
}

// Modal Actions
const openAddModal = () => {
  addForm.title = ''
  addForm.content = ''
  addForm.subcategoryId = null
  isAddModalOpen.value = true
  
  // Auto focus input
  setTimeout(() => {
    if (addTitleInput.value) {
      addTitleInput.value.focus()
    }
  }, 100)
}

const closeAddModal = () => {
  isAddModalOpen.value = false
  closeDropdowns()
}

const handleAddTodoSubmit = () => {
  if (!authStore.currentUser || !addForm.title.trim()) return
  
  todoStore.addTodo(authStore.currentUser.userId, {
    title: addForm.title.trim(),
    content: addForm.content.trim(),
    planDate: todayStr.value,
    priority: addForm.priority,
    status: 'pending',
    isPinned: false,
    reminderTime: null,
    categoryId: addForm.categoryId,
    subcategoryId: addForm.subcategoryId,
    doneAt: null,
    derivedFromType: null,
    derivedFromId: null
  })

  closeAddModal()
  
  // Show toast
  const event = new CustomEvent('app-toast', { detail: { text: '待办事项创建成功！' } })
  window.dispatchEvent(event)
}

const toggleStatus = (todo: Todo) => {
  if (!authStore.currentUser) return
  const newStatus = todo.status === 'done' ? 'pending' : 'done'
  todoStore.updateTodo(authStore.currentUser.userId, todo.id, { status: newStatus })
  
  // Dispatch toast
  const text = newStatus === 'done' ? '待办事项已完成！' : '撤销完成待办'
  const event = new CustomEvent('app-toast', { detail: { text } })
  window.dispatchEvent(event)
}

// Custom Delete Confirmation Modal State
const isDeleteConfirmOpen = ref(false)
const todoToDelete = ref<Todo | null>(null)

const deleteTodoItem = (todo: Todo) => {
  todoToDelete.value = todo
  isDeleteConfirmOpen.value = true
}

const cancelDelete = () => {
  isDeleteConfirmOpen.value = false
  todoToDelete.value = null
}

const confirmDelete = () => {
  if (!authStore.currentUser || !todoToDelete.value) return
  todoStore.deleteTodo(authStore.currentUser.userId, todoToDelete.value.id)
  
  // Dispatch toast
  const event = new CustomEvent('app-toast', { detail: { text: '待办事项已删除' } })
  window.dispatchEvent(event)
  
  // Reset state
  isDeleteConfirmOpen.value = false
  todoToDelete.value = null
}

// Drawer management
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
  editForm.rolloverCount = todo.rolloverCount
  // convert reminderTime YYYY-MM-DD HH:mm to datetime-local value
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
  closeEditDropdowns()
}

const saveDrawerEdit = () => {
  if (!authStore.currentUser) return
  
  todoStore.updateTodo(authStore.currentUser.userId, editForm.id, {
    title: editForm.title.trim(),
    content: editForm.content.trim(),
    priority: editForm.priority,
    categoryId: editForm.categoryId,
    subcategoryId: editForm.subcategoryId
  })

  closeDrawer()
  const event = new CustomEvent('app-toast', { detail: { text: '待办详情更新成功！' } })
  window.dispatchEvent(event)
}
</script>

<style scoped>
.today-view {
  max-width: 1100px;
  margin: 0 auto;
}

/* Header Summary */
.today-header {
  background-color: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 24px 32px;
  margin-bottom: 24px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  transition: none;
}
.today-header:hover {
  transform: none;
  box-shadow: var(--shadow-sm);
}
@media (max-width: 768px) {
  .today-header {
    flex-direction: column;
    align-items: stretch;
    padding: 20px 24px;
  }
}
.title-section h1 {
  font-size: 22px;
  font-weight: 800;
  color: var(--text-main);
  margin-bottom: 4px;
}
.date-subtitle {
  font-size: 13.5px;
  color: var(--text-muted);
  font-weight: 500;
}

.stats-bar {
  flex: 1;
  max-width: 460px;
  padding: 10px 20px;
}
.stats-info {
  display: flex;
  justify-content: space-around;
  align-items: center;
}
.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.stat-val {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-main);
}
.stat-val.text-success { color: var(--success); }
.stat-val.text-indigo { color: var(--primary); }
.stat-lbl {
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 600;
  margin-top: 2px;
}
.stat-divider {
  width: 1px;
  height: 28px;
  background-color: var(--border-medium);
}

/* Stats & Actions container */
.stats-and-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  max-width: 620px;
  justify-content: flex-end;
}
@media (max-width: 768px) {
  .stats-and-actions {
    max-width: none;
    width: 100%;
    margin-top: 12px;
  }
}
@media (max-width: 480px) {
  .stats-and-actions {
    flex-direction: column;
    align-items: stretch;
  }
}

/* Add Todo Button */
.add-todo-btn {
  background-color: var(--primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.2);
  transition: all var(--transition-fast);
  white-space: nowrap;
  height: 42px;
}

.add-todo-btn:hover {
  background-color: var(--primary-hover);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(79, 70, 229, 0.3);
}

.add-todo-btn:active {
  transform: translateY(0);
}

.add-todo-btn svg {
  width: 20px;
  height: 20px;
}

/* Add Todo Modal Styles */
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
  gap: 10px;
  padding: 10px 18px;
  transition: transform var(--transition-normal), box-shadow var(--transition-normal), border-color var(--transition-normal);
  cursor: default;
}
.todo-card:hover {
  transform: translateY(-1px);
}
.todo-high-priority {
  border-left: 4px solid var(--priority-high);
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
.todo-done .todo-badge-item, 
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
  margin-bottom: 4px;
}
.pin-icon {
  font-size: 14px;
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
  margin-bottom: 8px;
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
.derived-badge {
  background-color: var(--primary-light);
  color: var(--primary);
}

.todo-actions {
  display: flex;
  gap: 8px;
  opacity: 1;
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
.action-btn svg {
  width: 16px;
  height: 16px;
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

/* Custom indicator circle status button (identical to monthly summary/PerformanceView) */
.kr-status-icon-wrap {
  cursor: pointer;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  margin: 0;
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

.metadata-section {
  background-color: var(--bg-app);
  border-radius: var(--radius-md);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}
.metadata-row {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 500;
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

/* Category Group Section Styling */
.todo-groups-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.category-group-section {
  background-color: var(--bg-card);
  border-left: 4px solid var(--border-medium);
  border-radius: var(--radius-lg);
  box-shadow: 0 4px 16px -2px rgba(0, 0, 0, 0.03), 0 2px 4px -2px rgba(0, 0, 0, 0.02);
  padding: 22px;
  border: 1px solid var(--border-light);
  border-left-width: 4px;
  transition: all var(--transition-normal);
}

.category-group-section:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px -4px rgba(0, 0, 0, 0.06), 0 4px 12px -4px rgba(0, 0, 0, 0.04);
}

.group-header {
  border-bottom: 1px solid var(--border-light);
  padding-bottom: 14px;
  margin-bottom: 18px;
}

.group-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.group-color-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.group-name {
  font-size: 16.5px;
  font-weight: 700;
  color: var(--text-main);
  margin: 0;
}

.group-count-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 12px;
  margin-left: auto;
}

.group-content {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.subcategory-subgroup-panel {
  background-color: rgba(15, 23, 42, 0.015);
  border: 1.5px dashed var(--border-medium);
  border-radius: var(--radius-md);
  padding: 16px;
  transition: all var(--transition-fast);
}

.subcategory-subgroup-panel:hover {
  background-color: rgba(15, 23, 42, 0.025);
  border-color: var(--border-dark);
}

.subcategory-header {
  margin-bottom: 12px;
}

.subcategory-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.subcategory-bullet {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  opacity: 0.7;
}

.subcategory-name {
  font-size: 13.5px;
  font-weight: 700;
  color: var(--text-muted);
}

.subcategory-count-badge {
  font-size: 10.5px;
  font-weight: 700;
  color: var(--text-muted);
  background-color: var(--border-medium);
  padding: 1px 6px;
  border-radius: 8px;
  margin-left: 6px;
}

/* Custom Select Dropdown Styles (Matches PerformanceView layout with premium vars) */
.beautiful-select-trigger:hover {
  border-color: var(--primary) !important;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.05);
}
.beautiful-select-trigger.is-active {
  border-color: var(--primary) !important;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.12) !important;
}
.custom-dropdown-item:hover {
  background-color: #f1f5f9 !important;
}
.custom-dropdown-item.is-selected {
  background-color: var(--primary-light) !important;
}
.custom-dropdown-item.is-selected span {
  color: var(--primary) !important;
}
</style>
