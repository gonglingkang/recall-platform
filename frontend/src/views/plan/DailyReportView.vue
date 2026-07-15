<template>
  <div class="daily-report-view">
    <!-- View Header (Premium Style) -->
    <div class="view-header premium-card" style="margin-bottom: 24px;">
      <div class="title-row" style="display: flex; justify-content: space-between; align-items: center; width: 100%; flex-wrap: wrap; gap: 16px;">
        <div class="title-meta-left">
          <h2>个人日报</h2>
          <p class="subtitle-lbl">查看并总结本月日常工作内容，按周归档排版</p>
        </div>
        
        <!-- Right side actions -->
        <div class="header-actions" style="display: flex; align-items: center; gap: 20px; flex-wrap: wrap;">
          <!-- Holiday Filters Toggle -->
          <label class="filter-toggle-label" style="display: flex; align-items: center; gap: 8px; font-size: 13.5px; color: var(--text-main); cursor: pointer; user-select: none; font-weight: 500;">
            <input type="checkbox" v-model="filterSettings.hideOffDays" style="width: 16px; height: 16px; cursor: pointer;" />
            <span>隐藏周末及节假日</span>
          </label>

          <!-- Month Selector / Nav -->
          <div class="month-nav" style="display: flex; align-items: center; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background: #fff; padding: 2px; overflow: hidden; height: 42px;">
            <button class="nav-arrow-btn" @click.stop="shiftMonth(-1)" title="上一个月" style="border: none; background: none; width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; cursor: pointer; color: var(--text-muted); border-radius: 4px; transition: all 0.2s;">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" style="width: 16px; height: 16px;">
                <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 19.5L8.25 12l7.5-7.5" />
              </svg>
            </button>
            <span style="font-size: 14.5px; font-weight: 700; color: var(--text-main); padding: 0 16px; min-width: 100px; text-align: center; user-select: none;">
              {{ selectedYear }}年{{ selectedMonthNum }}月
            </span>
            <button 
              class="nav-arrow-btn" 
              @click.stop="shiftMonth(1)" 
              title="下一个月" 
              :disabled="selectedMonth >= currentMonthStr"
              :style="{ 
                border: 'none', 
                background: 'none', 
                width: '36px', 
                height: '36px', 
                display: 'flex', 
                alignItems: 'center', 
                justifyContent: 'center', 
                color: selectedMonth >= currentMonthStr ? 'var(--text-light)' : 'var(--text-muted)', 
                borderRadius: '4px', 
                transition: 'all 0.2s',
                opacity: selectedMonth >= currentMonthStr ? 0.35 : 1,
                cursor: selectedMonth >= currentMonthStr ? 'not-allowed' : 'pointer'
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

    <!-- Empty State -->
    <div v-if="weeklyGroups.length === 0" class="empty-report-state">
      <div class="empty-icon">📂</div>
      <h3>该月份没有可显示的日报日期</h3>
      <p>当前过滤设置隐藏了所有日期，或者本月尚未规划任何工作任务。</p>
    </div>

    <!-- Weekly Groups List -->
    <div v-else class="weekly-groups-container">
      <div v-for="group in weeklyGroups" :key="group.weekNum" class="week-card-box" :class="{ 'has-today': hasToday(group) }">
        <!-- Week Heading -->
        <div class="week-header-row">
          <div class="week-num-badge">第 {{ toChineseNumeral(group.weekNum) }} 周</div>
          <span class="week-range-label">{{ group.weekRangeLabel }}</span>
        </div>

        <!-- Days in Week List -->
        <div class="week-days-list">
          <div 
            v-for="day in group.days" 
            :key="day.dateStr" 
            class="day-report-item" 
            :class="[day.type, { 'has-content': day.savedItems.length > 0, 'is-today': day.dateStr === todayDateStr }]"
          >
            <!-- Left Info Panel -->
            <div class="day-meta-info">
              <div class="day-date-label" style="display: flex; align-items: center; gap: 6px;">
                <span class="day-number" :style="{ color: day.dateStr === todayDateStr ? '#ef4444' : '' }">{{ day.dayLabel }}</span>
                <span class="day-weekday" :style="{ color: day.dateStr === todayDateStr ? '#ef4444' : '' }">{{ day.weekDayLabel }}</span>
                <span v-if="day.dateStr === todayDateStr" style="font-size: 10px; font-weight: 800; color: #ef4444; background-color: #fee2e2; padding: 1px 5px; border-radius: 4px; display: inline-block;">今天</span>
              </div>
              
              <!-- Badges -->
              <span class="status-badge" :class="day.type">
                {{ day.typeLabel }}
              </span>
            </div>

            <!-- Center Items List Panel -->
            <div class="day-items-center-panel" style="flex: 1; display: flex; flex-direction: column; gap: 10px; justify-content: center;">
              <div v-if="day.savedItems.length === 0" style="color: var(--text-muted); font-size: 13px; font-style: italic; display: flex; align-items: center; height: 100%;">
                暂无日报记录，请点击右侧“编写日报”开始录入。
              </div>
              <div v-else style="display: flex; flex-direction: column; gap: 8px;">
                <div 
                  v-for="(item, idx) in day.savedItems" 
                  :key="item.id" 
                  style="display: flex; align-items: center; gap: 10px; font-size: 13.5px; line-height: 1.6; color: var(--text-main); flex-wrap: wrap;"
                >
                  <span style="font-weight: 700; color: var(--text-muted);">{{ idx + 1 }}、</span>
                  <span style="color: var(--text-main);">{{ item.content }}</span>
                  <span 
                    style="font-size: 12.5px; margin-left: 6px; font-weight: 600; padding: 2px 6px; border-radius: 4px; transition: all 0.2s;"
                    :style="{
                      backgroundColor: item.progress === 100 ? '#dcfce7' : '#dbeafe',
                      color: item.progress === 100 ? '#15803d' : '#1d4ed8'
                    }"
                  >
                    完成进度：{{ item.progress }}%
                  </span>
                  
                  <template v-if="item.associatedTodoId">
                    <span style="display: inline-flex; align-items: center; gap: 6px; padding: 2px 8px; background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 12px; margin-left: 6px; vertical-align: middle;">
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" style="width: 12px; height: 12px; color: #64748b; flex-shrink: 0;">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M13.19 8.688a4.5 4.5 0 011.242 7.244l-4.5 4.5a4.5 4.5 0 01-6.364-6.364l1.757-1.757m13.35-.622l1.757-1.757a4.5 4.5 0 00-6.364-6.364l-4.5 4.5a4.5 4.5 0 001.242 7.244" />
                      </svg>
                      <span style="color: #64748b; font-weight: 600; font-size: 11px;">关联待办</span>
                      <span 
                        class="todo-status-tag"
                        :class="(item.associatedTodoStatus || getTodoStatus(day.todos, item.associatedTodoId)) === 'done' ? 'done' : 'pending'"
                        style="font-weight: 700; font-size: 10px; padding: 1px 4px; border-radius: 3px;"
                      >
                        {{ (item.associatedTodoStatus || getTodoStatus(day.todos, item.associatedTodoId)) === 'done' ? '已完成' : '进行中' }}
                      </span>
                      <span 
                        style="color: #475569; font-weight: 500;"
                        :style="{ textDecoration: (item.associatedTodoStatus || getTodoStatus(day.todos, item.associatedTodoId)) === 'done' ? 'line-through' : 'none', opacity: (item.associatedTodoStatus || getTodoStatus(day.todos, item.associatedTodoId)) === 'done' ? 0.7 : 1 }"
                      >
                        {{ item.associatedTodoTitle || getTodoTitle(day.todos, item.associatedTodoId) }}
                        <span v-if="item.associatedTodoDateLabel" style="font-size: 10px; color: #94a3b8; font-weight: 600; text-decoration: none; display: inline-block; margin-left: 4px;">({{ item.associatedTodoDateLabel }})</span>
                      </span>
                    </span>
                  </template>
                </div>
              </div>
            </div>

            <!-- Right Actions Panel -->
            <div class="day-actions-panel" style="display: flex; align-items: center; gap: 8px; justify-content: flex-end; flex-shrink: 0; margin-left: auto;">
              <!-- Edit Button -->
              <button 
                class="action-btn edit-action-btn" 
                @click="openEditorModal(day)"
                title="编辑日报"
              >
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L6.83 20.04a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10" />
                </svg>
              </button>
              
              <!-- Copy Button -->
              <button 
                v-if="day.savedItems.length > 0"
                class="action-btn copy-action-btn" 
                @click="copyStructuredDayReport(day)"
                title="复制日报"
              >
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M15.666 3.888A2.25 2.25 0 0013.5 2.25h-3c-1.03 0-1.9.693-2.166 1.638m7.332 0c.055.194.084.4.084.612v0a.75.75 0 01-.75.75H9a.75.75 0 01-.75-.75v0c0-.212.03-.418.084-.612m7.332 0c.646.049 1.288.11 1.927.184 1.1.128 1.907 1.077 1.907 2.185V19.5a2.25 2.25 0 01-2.25 2.25H6.75A2.25 2.25 0 014.5 19.5V6.257c0-1.108.806-2.057 1.907-2.185a48.208 48.208 0 011.927-.184" />
                </svg>
              </button>

              <!-- Clear Button -->
              <button 
                v-if="day.savedItems.length > 0"
                class="action-btn delete-action-btn" 
                @click="confirmClearDayReport(day)"
                title="清除日报"
              >
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" />
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Edit Daily Report Modal -->
    <div v-if="editorModal.isOpen" class="modal-overlay" @mousedown="mousedownTarget = $event.target" @click="mousedownTarget === $event.currentTarget && $event.target === $event.currentTarget && (editorModal.isOpen = false)">
      <div class="modal-content" style="max-width: 850px; width: 100%; padding: 24px; display: flex; flex-direction: column; overflow: visible;">
        <div class="modal-header-with-close" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-shrink: 0; border-bottom: 1px solid var(--border-light); padding-bottom: 12px;">
          <h3 style="margin: 0; font-size: 16px; font-weight: 700; color: var(--text-main);">
            编写日报 - {{ editorModal.dayLabel }} ({{ editorModal.weekDayLabel }})
          </h3>
          <button class="modal-close-icon-btn" @click="editorModal.isOpen = false" title="关闭弹窗" style="background: none; border: none; cursor: pointer; color: var(--text-muted); display: flex; align-items: center; justify-content: center; padding: 4px; border-radius: 50%;">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" style="width: 18px; height: 18px;">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="modal-body" style="display: flex; flex-direction: column; gap: 14px; overflow: visible; padding-right: 6px; flex-grow: 1; margin-bottom: 20px;">
          <p style="font-size: 13px; color: var(--text-muted); margin: 0; line-height: 1.5;">
            请录入日报工作项，您可以为每项关联当天的日常待办事项：
          </p>

          <div style="display: flex; flex-direction: column; gap: 12px;">
            <div v-for="(item, idx) in editorModal.items" :key="item.id" style="display: flex; align-items: center; gap: 10px; background-color: #f8fafc; padding: 12px; border: 1px solid var(--border-medium); border-radius: 8px;">
              <span style="font-size: 13px; font-weight: 700; color: var(--text-muted); min-width: 28px;">项{{ idx + 1 }}</span>
              
              <!-- Item Text Content -->
              <input 
                type="text" 
                v-model="item.content" 
                class="form-control" 
                placeholder="请输入工作内容描述..." 
                style="flex: 1; font-size: 13px; height: 36px; border: 1.5px solid var(--border-medium); border-radius: 6px; padding: 0 10px;"
              />

              <!-- Progress Percent Input -->
              <div style="display: flex; align-items: center; gap: 4px; flex-shrink: 0;">
                <input 
                  type="number" 
                  v-model.number="item.progress" 
                  min="0" 
                  max="100" 
                  class="form-control" 
                  placeholder="进度" 
                  style="width: 58px; font-size: 13px; height: 36px; border: 1.5px solid var(--border-medium); border-radius: 6px; padding: 0 8px; text-align: center;"
                />
                <span style="font-size: 13px; color: var(--text-muted); font-weight: 600;">%</span>
              </div>

              <!-- Associated Todo Selector & Cancellation Button (Custom Select) -->
              <div style="display: flex; align-items: center; gap: 6px; position: relative;">
                <div class="custom-select-container" style="position: relative; width: 220px;">
                  <div 
                    class="form-control beautiful-select-trigger" 
                    @click.stop="toggleTodoDropdown(item.id)"
                    :class="{ 'is-active': openDropdownItemId === item.id }"
                    style="display: flex; align-items: center; justify-content: space-between; cursor: pointer; padding: 0 10px; border: 1.5px solid var(--border-medium); border-radius: 6px; background: #fff; transition: all var(--transition-fast); height: 36px; user-select: none;"
                  >
                    <div style="display: flex; align-items: center; gap: 6px; min-width: 0; flex: 1; padding-right: 4px;">
                      <span 
                        v-if="item.associatedTodoId"
                        class="todo-status-tag"
                        :class="getTodoStatus(editorModal.availableTodos, item.associatedTodoId)"
                        style="flex-shrink: 0;"
                      >
                        {{ getTodoStatusLabel(editorModal.availableTodos, item.associatedTodoId) }}
                      </span>
                      <span 
                        style="font-size: 13px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;"
                        :style="{ color: item.associatedTodoId ? 'var(--text-main)' : 'var(--text-muted)' }"
                      >
                        {{ item.associatedTodoId ? getTodoTitle(editorModal.availableTodos, item.associatedTodoId) : '请选择关联待办' }}
                      </span>
                    </div>
                    
                    <div style="display: flex; align-items: center; gap: 4px; flex-shrink: 0;">
                      <!-- Unlink Button inside the trigger -->
                      <button 
                        v-if="item.associatedTodoId !== null"
                        type="button"
                        @click.stop="item.associatedTodoId = null" 
                        title="取消关联"
                        style="color: var(--text-muted); background: none; border: none; cursor: pointer; padding: 2px; display: flex; align-items: center; justify-content: center; border-radius: 50%; transition: all 0.15s;"
                        onmouseover="this.style.color='#ef4444'; this.style.backgroundColor='#fee2e2';"
                        onmouseout="this.style.color='var(--text-muted)'; this.style.backgroundColor='transparent';"
                      >
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" style="width: 13px; height: 13px;">
                          <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
                        </svg>
                      </button>

                      <svg 
                        xmlns="http://www.w3.org/2000/svg" 
                        fill="none" 
                        viewBox="0 0 24 24" 
                        stroke-width="2.2" 
                        stroke="currentColor" 
                        style="width: 14px; height: 14px; transition: transform 0.2s; color: var(--text-muted);"
                        :style="{ transform: openDropdownItemId === item.id ? 'rotate(180deg)' : 'rotate(0deg)' }"
                      >
                        <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
                      </svg>
                    </div>
                  </div>
                  
                  <!-- Dropdown overlay list -->
                  <div 
                    v-if="openDropdownItemId === item.id" 
                    class="custom-dropdown-list"
                    style="position: absolute; top: calc(100% + 6px); left: 0; right: 0; background: rgba(255, 255, 255, 0.98); border: 1px solid rgba(0, 0, 0, 0.08); border-radius: var(--radius-md); box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.05); z-index: 999; padding: 6px; backdrop-filter: blur(12px); display: flex; flex-direction: column; gap: 4px; max-height: 220px; overflow-y: auto;"
                  >
                    <!-- placeholder option -->
                    <div 
                      @click="selectTodoForItem(item, null)"
                      class="custom-dropdown-item"
                      style="display: flex; align-items: center; justify-content: space-between; padding: 8px 10px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
                      :class="{ 'is-selected': item.associatedTodoId === null }"
                    >
                      <span style="font-size: 13px; color: var(--text-muted);">请选择关联待办</span>
                      <svg 
                        v-if="item.associatedTodoId === null"
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

                    <!-- Empty state if no todos -->
                    <div v-if="groupedAvailableTodos.length === 0" style="padding: 12px; text-align: center; font-size: 12.5px; color: var(--text-muted);">
                      当天没有待办记录
                    </div>

                    <!-- Grouped categories with available options -->
                    <template v-else v-for="group in groupedAvailableTodos" :key="group.categoryId">
                      <!-- Category Header -->
                      <div style="padding: 6px 10px 4px 10px; font-size: 11px; font-weight: 700; color: var(--text-muted); display: flex; align-items: center; gap: 6px; border-bottom: 1px solid var(--border-light); margin-top: 4px; padding-bottom: 4px;">
                        <span style="display: inline-block; width: 6px; height: 6px; border-radius: 50%;" :style="{ backgroundColor: group.categoryColor }"></span>
                        <span>{{ group.categoryName }}</span>
                      </div>

                      <!-- Todos in this category -->
                      <div 
                        v-for="todo in group.todos" 
                        :key="todo.id"
                        @click="selectTodoForItem(item, todo.id)"
                        class="custom-dropdown-item"
                        style="display: flex; align-items: center; justify-content: space-between; padding: 8px 10px 8px 18px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
                        :class="{ 'is-selected': item.associatedTodoId === todo.id }"
                      >
                        <div style="display: flex; align-items: center; gap: 8px; min-width: 0; flex: 1;">
                          <span class="todo-status-tag" :class="todo.status" style="flex-shrink: 0;">
                            {{ todo.status === 'done' ? '已完成' : '进行中' }}
                          </span>
                          <span style="font-size: 13px; color: var(--text-main); white-space: nowrap; overflow: hidden; text-overflow: ellipsis;" :title="todo.title">
                            {{ todo.title }}
                          </span>
                        </div>
                        <svg 
                          v-if="item.associatedTodoId === todo.id"
                          xmlns="http://www.w3.org/2000/svg" 
                          fill="none" 
                          viewBox="0 0 24 24" 
                          stroke-width="2.5" 
                          stroke="var(--primary)" 
                          style="width: 14px; height: 14px; flex-shrink: 0;"
                        >
                          <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
                        </svg>
                      </div>
                    </template>
                  </div>
                </div>
              </div>

              <!-- Delete Button -->
              <button 
                class="btn-text-action" 
                @click="removeEditorItem(idx)" 
                title="删除此项"
                style="color: #ef4444; background: none; border: none; cursor: pointer; padding: 4px; display: flex; align-items: center; justify-content: center; border-radius: 4px;"
                :disabled="editorModal.items.length === 1"
              >
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" style="width: 16px; height: 16px;">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" />
                </svg>
              </button>
            </div>
            
            <button 
              class="btn btn-secondary" 
              @click="addEditorItem" 
              style="display: flex; align-items: center; justify-content: center; gap: 6px; border-style: dashed; border-width: 1.5px; border-color: var(--primary); color: var(--primary); height: 38px; width: 100%; font-weight: 700; border-radius: 8px; cursor: pointer; background: none;"
            >
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" style="width: 16px; height: 16px;">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
              </svg>
              <span>添加日报项</span>
            </button>
          </div>
        </div>

        <div class="modal-actions" style="display: flex; justify-content: flex-end; gap: 10px; flex-shrink: 0; padding-top: 12px; border-top: 1px solid var(--border-light);">
          <button class="btn btn-secondary" @click="editorModal.isOpen = false">取消</button>
          <button class="btn btn-primary" @click="saveDailyReportItems">保存</button>
        </div>
      </div>
    </div>

    <!-- Confirm Clear Daily Report Modal -->
    <div v-if="confirmModal.isOpen" class="modal-overlay" style="position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(15, 23, 42, 0.4); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 2100;">
      <div class="modal-content" style="max-width: 420px; padding: 24px; display: flex; flex-direction: column; gap: 16px; border-radius: var(--radius-lg); background: #fff; box-shadow: var(--shadow-xl); border: 1px solid var(--border-light);">
        <div style="display: flex; align-items: center; gap: 12px; color: #ef4444;">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" style="width: 24px; height: 24px;">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
          </svg>
          <h3 style="margin: 0; font-size: 16px; font-weight: 800; color: #ef4444;">清除确认</h3>
        </div>
        
        <p style="font-size: 13.5px; color: var(--text-muted); margin: 0; line-height: 1.5;">
          您确定要清除 <strong>{{ confirmModal.dayLabel }}</strong> 的工作日报吗？清除后内容将无法恢复。
        </p>
        
        <div style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 8px;">
          <button class="btn btn-secondary" @click="confirmModal.isOpen = false">取消</button>
          <button class="btn btn-primary" style="background-color: #ef4444; border-color: #ef4444;" @click="clearDailyReport">确认清除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useTodoStore } from '@/stores/todo'
import request from '@/api/request'
import type { ApiResult } from '@/api/types'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const todoStore = useTodoStore()

const currentMonthStr = ref(new Date().toISOString().substring(0, 7))
const selectedMonth = ref(route.params.month as string || currentMonthStr.value)

const getTodayDateStr = () => {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const r = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${r}`
}
const todayDateStr = ref(getTodayDateStr())

const hasToday = (group: WeekGroup) => {
  return group.days.some(d => d.dateStr === todayDateStr.value)
}

const scrollToTodayWeek = () => {
  nextTick(() => {
    const todayElement = document.querySelector('.day-report-item.is-today')
    if (todayElement) {
      todayElement.scrollIntoView({ behavior: 'smooth', block: 'center' })
    } else {
      const todayWeekCard = document.querySelector('.week-card-box.has-today')
      if (todayWeekCard) {
        todayWeekCard.scrollIntoView({ behavior: 'smooth', block: 'center' })
      }
    }
  })
}

// Filter settings
const filterSettings = reactive({
  hideOffDays: true // Defaults to hiding weekends and holidays
})

// Chinese holiday map (2025 and 2026)
const HOLIDAYS_DB = new Set([
  // 2026
  '2026-01-01',
  '2026-01-28', '2026-01-29', '2026-01-30', '2026-01-31', '2026-02-01', '2026-02-02', '2026-02-03', '2026-02-04',
  '2026-04-04', '2026-04-05', '2026-04-06',
  '2026-05-01', '2026-05-02', '2026-05-03', '2026-05-04', '2026-05-05',
  '2026-06-19', '2026-06-20', '2026-06-21',
  '2026-10-01', '2026-10-02', '2026-10-03', '2026-10-04', '2026-10-05', '2026-10-06', '2026-10-07', '2026-10-08',
  // 2025
  '2025-01-01',
  '2025-01-28', '2025-01-29', '2025-01-30', '2025-01-31', '2025-02-01', '2025-02-02', '2025-02-03', '2025-02-04',
  '2025-04-04', '2025-04-05', '2025-04-06',
  '2025-05-01', '2025-05-02', '2025-05-03', '2025-05-04', '2025-05-05',
  '2025-05-31', '2025-06-01', '2025-06-02',
  '2025-10-01', '2025-10-02', '2025-10-03', '2025-10-04', '2025-10-05', '2025-10-06', '2025-10-07', '2025-10-08'
])

// Chinese make-up workdays (2025 and 2026)
const MAKEUPS_DB = new Set([
  // 2026
  '2026-01-25', '2026-02-08', '2026-04-26', '2026-05-09', '2026-09-27', '2026-10-10',
  // 2025
  '2025-01-26', '2025-02-08', '2025-04-27', '2025-05-10', '2025-09-28', '2025-10-11'
])

interface DailyReportItem {
  id: string
  content: string
  progress: number
  associatedTodoId: number | null
  associatedTodoTitle?: string
  associatedTodoStatus?: string
  associatedTodoDateLabel?: string
}

interface DayItem {
  dateStr: string
  dayLabel: string
  weekDayLabel: string
  type: 'workday' | 'weekend' | 'holiday' | 'makeup'
  typeLabel: string
  todos: any[]
  savedItems: DailyReportItem[]
}

interface WeekGroup {
  weekNum: number
  weekRangeLabel: string
  days: DayItem[]
}

const formattedMonthLabel = computed(() => {
  const [y, m] = selectedMonth.value.split('-')
  return `${y} 年 ${parseInt(m)} 月`
})

const selectedYear = computed(() => selectedMonth.value.split('-')[0])
const selectedMonthNum = computed(() => parseInt(selectedMonth.value.split('-')[1]))

const toChineseNumeral = (num: number): string => {
  const chineseNumerals = ['零', '一', '二', '三', '四', '五', '六', '七', '八', '九', '十']
  if (num <= 10) return chineseNumerals[num]
  if (num < 20) return '十' + (num % 10 === 0 ? '' : chineseNumerals[num % 10])
  return String(num)
}

const getDateType = (dateStr: string): { type: 'workday' | 'weekend' | 'holiday' | 'makeup', label: string } => {
  if (MAKEUPS_DB.has(dateStr)) {
    return { type: 'makeup', label: '补班工作日' }
  }
  if (HOLIDAYS_DB.has(dateStr)) {
    return { type: 'holiday', label: '法定节假日' }
  }
  const day = new Date(dateStr).getDay()
  if (day === 0 || day === 6) {
    return { type: 'weekend', label: '周末双休' }
  }
  return { type: 'workday', label: '日常工作日' }
}

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
  router.push(`/daily-report/${targetMonth}`)
}

// Keep track of route changes
watch(() => route.params.month, (newMonth) => {
  if (newMonth) {
    selectedMonth.value = newMonth as string
  }
})

const refreshTrigger = ref(0)

const editorModal = reactive({
  isOpen: false,
  dateStr: '',
  dayLabel: '',
  weekDayLabel: '',
  availableTodos: [] as any[],
  items: [] as DailyReportItem[]
})

const mousedownTarget = ref<any>(null)

const openDropdownItemId = ref<string | null>(null)

const toggleTodoDropdown = (itemId: string) => {
  if (openDropdownItemId.value === itemId) {
    openDropdownItemId.value = null
  } else {
    openDropdownItemId.value = itemId
  }
}

const selectTodoForItem = (item: DailyReportItem, todoId: number | null) => {
  item.associatedTodoId = todoId
  openDropdownItemId.value = null
  
  // If a todo is selected, and the item content is empty, autofill the todo's title
  if (todoId !== null && item.content.trim() === '') {
    const todoTitle = getTodoTitle(editorModal.availableTodos, todoId)
    if (todoTitle) {
      item.content = todoTitle
    }
  }
}

const closeAllDropdowns = () => {
  openDropdownItemId.value = null
}

const reportsMap = ref<Record<string, any>>({})
const isLoadingReports = ref(false)

const confirmModal = reactive({
  isOpen: false,
  dateStr: '',
  dayLabel: ''
})

const confirmClearDayReport = (day: DayItem) => {
  confirmModal.dateStr = day.dateStr
  confirmModal.dayLabel = day.dayLabel
  confirmModal.isOpen = true
}

const clearDailyReport = async () => {
  try {
    await request.delete(`/api/daily-reports/${confirmModal.dateStr}`)
    delete reportsMap.value[confirmModal.dateStr]
    confirmModal.isOpen = false
    refreshTrigger.value++
    
    const event = new CustomEvent('app-toast', { detail: { text: `${confirmModal.dayLabel}日报内容已清除！` } })
    window.dispatchEvent(event)
  } catch (err) {
    console.error('Failed to delete daily report:', err)
    const event = new CustomEvent('app-toast', { detail: { text: '清除失败，请稍后重试。', type: 'error' } })
    window.dispatchEvent(event)
  }
}

const fetchMonthlyReports = async (monthStr: string) => {
  isLoadingReports.value = true
  try {
    const res = await request.get<any, ApiResult<any>>(`/api/daily-reports?month=${monthStr}`)
    if (res && res.code === 200 && res.data && res.data.reports) {
      const map: Record<string, any> = {}
      res.data.reports.forEach((rep: any) => {
        map[rep.reportDate] = rep
      })
      reportsMap.value = map
      scrollToTodayWeek()
    } else {
      reportsMap.value = {}
    }
  } catch (err) {
    console.error('Failed to fetch monthly daily reports:', err)
    reportsMap.value = {}
  } finally {
    isLoadingReports.value = false
  }
}

watch(selectedMonth, (newMonth) => {
  if (newMonth) {
    fetchMonthlyReports(newMonth)
  }
}, { immediate: true })

const groupedAvailableTodos = computed(() => {
  const groupsMap = new Map<number | null, any[]>()
  
  editorModal.availableTodos.forEach(todo => {
    const catId = todo.categoryId
    if (!groupsMap.has(catId)) {
      groupsMap.set(catId, [])
    }
    groupsMap.get(catId)!.push(todo)
  })
  
  const list: { categoryId: number | null; categoryName: string; categoryColor: string; todos: any[] }[] = []
  
  groupsMap.forEach((todos, catId) => {
    let categoryName = '无分类'
    let categoryColor = '#94a3b8' // Slate gray
    
    if (catId !== null) {
      const cat = todoStore.categories.find(c => c.id === catId)
      if (cat) {
        categoryName = cat.name
        categoryColor = cat.color
      }
    }
    
    list.push({
      categoryId: catId,
      categoryName,
      categoryColor,
      todos
    })
  })
  
  // Sort groups so that '无分类' is at the end, other categories sorted by their index in todoStore.sortedCategories
  return list.sort((a, b) => {
    if (a.categoryId === null) return 1
    if (b.categoryId === null) return -1
    const idxA = todoStore.sortedCategories.findIndex(c => c.id === a.categoryId)
    const idxB = todoStore.sortedCategories.findIndex(c => c.id === b.categoryId)
    const orderA = idxA !== -1 ? idxA : 999
    const orderB = idxB !== -1 ? idxB : 999
    return orderA - orderB
  })
})

// Load todos and load local storage records
const weeklyGroups = computed(() => {
  // Reactivity trigger dependency
  refreshTrigger.value
  
  const [year, month] = selectedMonth.value.split('-').map(Number)
  const daysInMonth = new Date(year, month, 0).getDate()
  
  const allDays: DayItem[] = []
  
  for (let d = 1; d <= daysInMonth; d++) {
    const dStr = String(d).padStart(2, '0')
    const mStr = String(month).padStart(2, '0')
    const dateStr = `${year}-${mStr}-${dStr}`
    
    // Check holiday status
    const { type, label } = getDateType(dateStr)
    
    // Filter out weekends and holidays if hideOffDays is checked
    if (filterSettings.hideOffDays && (type === 'weekend' || type === 'holiday')) {
      continue
    }
    
    // Get todos of the day from todoStore
    const dayTodos = todoStore.todos.filter(t => t.status !== 'deleted' && t.planDate === dateStr)
    
    // Load report items from backend reportsMap
    const savedItems: DailyReportItem[] = []
    const reportData = reportsMap.value[dateStr]
    if (reportData && reportData.items) {
      reportData.items.forEach((item: any) => {
        const relatedTodo = item.todos && item.todos.length > 0 ? item.todos[0] : null
        let todoStatus: string | undefined = undefined
        if (relatedTodo) {
          todoStatus = (relatedTodo.status === '1' || relatedTodo.status === 'done') ? 'done' : 'pending'
        }
        const todoDateStr = relatedTodo && relatedTodo.createdAt ? relatedTodo.createdAt.split('T')[0] : undefined
        let associatedTodoDateLabel: string | undefined = undefined
        if (todoDateStr && todoDateStr !== dateStr) {
          const parts = todoDateStr.split('-')
          if (parts.length === 3) {
            associatedTodoDateLabel = `${parseInt(parts[1])}月${parseInt(parts[2])}日`
          } else {
            associatedTodoDateLabel = todoDateStr
          }
        }

        savedItems.push({
          id: item.id?.toString() || (Date.now() + Math.random()).toString(),
          content: item.content,
          progress: item.progress,
          associatedTodoId: relatedTodo ? relatedTodo.id : null,
          associatedTodoTitle: relatedTodo ? relatedTodo.title : undefined,
          associatedTodoStatus: todoStatus,
          associatedTodoDateLabel
        })
      })
    }
    
    // Format week day index
    const dateObj = new Date(dateStr)
    const weekDays = ['周日', '周防', '周二', '周三', '周四', '周五', '周六'] // Will correct to standard index
    const weekDayLabel = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][dateObj.getDay()]
    
    allDays.push({
      dateStr,
      dayLabel: `${parseInt(mStr)}月${d}日`,
      weekDayLabel,
      type,
      typeLabel: label,
      todos: dayTodos,
      savedItems
    })
  }
  
  // Group into weeks
  const groups: WeekGroup[] = []
  let currentWeekNum = 1
  let currentWeekDays: DayItem[] = []
  
  allDays.forEach((day) => {
    const dateObj = new Date(day.dateStr)
    const dayOfWeek = dateObj.getDay() // 0 is Sunday, 1 is Monday, etc.
    
    // Grouping rule: Start a new week on Monday (dayOfWeek === 1) or when it is the first element
    if (currentWeekDays.length > 0 && dayOfWeek === 1) {
      // Push completed week
      const startLabel = formatRangeLabel(currentWeekDays[0].dateStr)
      const endLabel = formatRangeLabel(currentWeekDays[currentWeekDays.length - 1].dateStr)
      groups.push({
        weekNum: currentWeekNum++,
        weekRangeLabel: `${startLabel} ~ ${endLabel}`,
        days: currentWeekDays
      })
      currentWeekDays = []
    }
    currentWeekDays.push(day)
  })
  
  // Push the final week
  if (currentWeekDays.length > 0) {
    const startLabel = formatRangeLabel(currentWeekDays[0].dateStr)
    const endLabel = formatRangeLabel(currentWeekDays[currentWeekDays.length - 1].dateStr)
    groups.push({
      weekNum: currentWeekNum,
      weekRangeLabel: `${startLabel} ~ ${endLabel}`,
      days: currentWeekDays
    })
  }
  
  return groups
})

const formatRangeLabel = (dateStr: string) => {
  const [, m, d] = dateStr.split('-')
  return `${m}.${d}`
}

const getTodoTitle = (todos: any[], todoId: number) => {
  const todo = todos.find(t => t.id === todoId)
  return todo ? todo.title : ''
}

const getTodoStatus = (todos: any[], todoId: number) => {
  const todo = todos.find(t => t.id === todoId)
  return todo ? todo.status : 'pending'
}

const getTodoStatusLabel = (todos: any[], todoId: number) => {
  const todo = todos.find(t => t.id === todoId)
  return todo ? (todo.status === 'done' ? '已完成' : '进行中') : ''
}

const openEditorModal = async (day: DayItem) => {
  editorModal.dateStr = day.dateStr
  editorModal.dayLabel = day.dayLabel
  editorModal.weekDayLabel = day.weekDayLabel
  editorModal.availableTodos = [] // Clear first to show fresh list
  
  if (day.savedItems.length > 0) {
    editorModal.items = JSON.parse(JSON.stringify(day.savedItems))
  } else {
    editorModal.items = [{ id: Date.now().toString(), content: '', progress: 100, associatedTodoId: null }]
  }
  
  editorModal.isOpen = true

  // Fetch todos dynamically via API
  const freshTodos = await todoStore.fetchTodosByDate(day.dateStr)
  editorModal.availableTodos = freshTodos
}

const addEditorItem = () => {
  editorModal.items.push({
    id: (Date.now() + Math.random()).toString(),
    content: '',
    progress: 100,
    associatedTodoId: null
  })
}

const removeEditorItem = (idx: number) => {
  editorModal.items.splice(idx, 1)
}

const saveDailyReportItems = async () => {
  const dateStr = editorModal.dateStr
  
  // Filter out completely empty items
  const cleaned = editorModal.items.filter(item => item.content.trim() !== '')
  
  try {
    if (cleaned.length === 0) {
      // Call delete endpoint if report is cleared
      await request.delete(`/api/daily-reports/${dateStr}`)
      delete reportsMap.value[dateStr]
    } else {
      // Call save endpoint
      const payload = {
        items: cleaned.map(item => ({
          content: item.content,
          progress: item.progress || 100,
          todoIds: item.associatedTodoId ? [item.associatedTodoId] : []
        }))
      }
      const res = await request.put<any, ApiResult<any>>(`/api/daily-reports/${dateStr}`, payload)
      if (res && res.code === 200 && res.data) {
        reportsMap.value[dateStr] = res.data
      }
    }
    editorModal.isOpen = false
    refreshTrigger.value++
  } catch (err) {
    console.error('Failed to save daily report:', err)
    const event = new CustomEvent('app-toast', { detail: { text: '保存失败，请稍后重试。', type: 'error' } })
    window.dispatchEvent(event)
  }
}

const copyStructuredDayReport = async (day: DayItem) => {
  let content = ''
  day.savedItems.forEach((item, idx) => {
    content += `${idx + 1}、${item.content}，进度${item.progress}%\n`
  })
  
  try {
    await navigator.clipboard.writeText(content.trim())
    const event = new CustomEvent('app-toast', { detail: { text: `${day.dayLabel}日报内容已复制！` } })
    window.dispatchEvent(event)
  } catch (err) {
    const event = new CustomEvent('app-toast', { detail: { text: '复制失败，请手动选择复制。', type: 'error' } })
    window.dispatchEvent(event)
  }
}

onMounted(async () => {
  document.addEventListener('click', closeAllDropdowns)
  if (authStore.currentUser) {
    todoStore.refreshTodayTodos(authStore.currentUser.userId)
    await todoStore.refreshCategories(authStore.currentUser.userId)
  }
  scrollToTodayWeek()
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeAllDropdowns)
})
</script>

<style scoped>
.daily-report-view {
  max-width: 1100px;
  margin: 0 auto;
  padding-bottom: 50px;
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

.nav-arrow-btn:hover:not(:disabled) {
  background-color: #f1f5f9 !important;
  color: var(--primary) !important;
}

/* Empty State */
.empty-report-state {
  background-color: var(--bg-card);
  border: 1.5px dashed var(--border-medium);
  border-radius: var(--radius-lg);
  padding: 60px 24px;
  text-align: center;
  margin-top: 20px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-report-state h3 {
  font-size: 18px;
  color: var(--text-main);
  margin-bottom: 8px;
  font-weight: 700;
}

.empty-report-state p {
  font-size: 13.5px;
  color: var(--text-muted);
}

/* Weekly Groups container */
.weekly-groups-container {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.week-card-box {
  background-color: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  overflow: hidden;
  transition: transform var(--transition-normal), box-shadow var(--transition-normal);
}

.week-card-box:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}

.week-header-row {
  background-color: var(--bg-body);
  padding: 12px 20px;
  border-bottom: 1.5px solid var(--border-medium);
  display: flex;
  align-items: center;
  gap: 12px;
}

.week-num-badge {
  background-color: var(--primary-light);
  color: var(--primary);
  font-size: 12px;
  font-weight: 800;
  padding: 4px 10px;
  border-radius: 4px;
  border: 1px solid rgba(37, 99, 235, 0.1);
}

.week-range-label {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--text-muted);
}

.week-days-list {
  display: flex;
  flex-direction: column;
}

.day-report-item {
  display: grid;
  grid-template-columns: 160px 1fr auto;
  border-bottom: 1px solid var(--border-light);
  padding: 18px 20px;
  gap: 24px;
  align-items: center;
}

.day-report-item.is-today {
  border-left: 4px solid #ef4444;
  background-color: #fef2f2;
  padding-left: 16px;
}

.day-report-item:last-child {
  border-bottom: none;
}

/* Day meta styling */
.day-meta-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.day-date-label {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.day-number {
  font-size: 17px;
  font-weight: 800;
  color: var(--text-main);
}

.day-weekday {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-muted);
}

.status-badge {
  display: inline-block;
  padding: 2.5px 8px;
  font-size: 11px;
  font-weight: 700;
  border-radius: 4px;
  width: fit-content;
}

.status-badge.workday {
  background-color: #f1f5f9;
  color: #475569;
  border: 1px solid #cbd5e1;
}

.status-badge.makeup {
  background-color: #fef3c7;
  color: #d97706;
  border: 1px solid #fcd34d;
}

.status-badge.weekend {
  background-color: #f1f5f9;
  color: #94a3b8;
}

.status-badge.holiday {
  background-color: #fee2e2;
  color: #dc2626;
  border: 1px solid #fca5a5;
}

/* Middle Day Tasks */
.day-tasks-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
  background-color: rgba(248, 250, 252, 0.5);
  border: 1px dashed var(--border-medium);
  border-radius: 8px;
  padding: 12px;
  max-height: 180px;
  overflow-y: auto;
}

.section-title-wrap {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-title-wrap h5 {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-main);
}

.todo-count-lbl {
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 600;
}

.no-todos-tip {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}

.day-todos-ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.day-todos-ul li {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  color: var(--text-main);
  line-height: 1.4;
}

.day-todos-ul li.done .todo-title-txt {
  text-decoration: line-through;
  color: var(--text-muted);
}

.todo-status-tag {
  font-size: 9.5px;
  font-weight: 800;
  padding: 1px 4px;
  border-radius: 3px;
}

.todo-status-tag.done {
  background-color: #dcfce7;
  color: #15803d;
}

.todo-status-tag.pending {
  background-color: #dbeafe;
  color: #1d4ed8;
}

.todo-title-txt {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.todo-prio-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.todo-prio-dot.high { background-color: #ef4444; }
.todo-prio-dot.medium { background-color: #eab308; }
.todo-prio-dot.low { background-color: #22c55e; }

/* Right Editor styling */
.day-editor-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.editor-header h5 {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-main);
}

.editor-actions {
  display: flex;
  gap: 12px;
}

.btn-text-action {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--primary);
  font-size: 12px;
  font-weight: 700;
  padding: 2px 4px;
  border-radius: 4px;
  transition: all var(--transition-fast);
}

.btn-text-action:hover {
  background-color: var(--primary-light);
}

.btn-text-action.copy-btn {
  color: #16a34a;
}

.btn-text-action.copy-btn:hover {
  background-color: rgba(22, 163, 74, 0.08);
}

.textarea-wrapper {
  position: relative;
  display: flex;
  flex-direction: column;
}

.report-textarea {
  width: 100%;
  border: 1.5px solid var(--border-medium);
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 12.5px;
  line-height: 1.5;
  resize: vertical;
  color: var(--text-main);
  background-color: var(--bg-card);
  transition: all var(--transition-fast);
}

.report-textarea:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
}

.autosave-indicator {
  position: absolute;
  bottom: 8px;
  right: 12px;
  font-size: 11px;
  font-weight: 700;
  color: #16a34a;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.3s ease;
}

.autosave-indicator.visible {
  opacity: 1;
}

/* Responsiveness */
@media (max-width: 950px) {
  .day-report-item {
    grid-template-columns: 140px 1fr;
    gap: 16px;
  }
  .day-editor-section {
    grid-column: span 2;
  }
}

@media (max-width: 650px) {
  .day-report-item {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  .day-editor-section, .day-tasks-section {
    grid-column: span 1;
  }
}
/* Modal overlay styling */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
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
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--border-light);
  width: 100%;
}

/* Action button styles matching TodayView */
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

.edit-action-btn:hover {
  background-color: var(--primary-light);
  color: var(--primary) !important;
}

.copy-action-btn:hover {
  background-color: rgba(22, 163, 74, 0.08);
  color: #16a34a !important;
}

.delete-action-btn:hover {
  background-color: rgba(239, 68, 68, 0.08);
  color: #ef4444 !important;
}

/* Styled Buttons */
.btn {
  padding: 10px 20px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
  border: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
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

.btn-primary:hover {
  background-color: #1d4ed8;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
