<template>
  <div class="performance-view">
    <!-- Header Area (Title, Month Selector, Export, New K-Objective) -->
    <div class="view-header premium-card">
      <div class="title-row">
        <div class="title-meta-left">
          <h2>个人月度绩效</h2>
          <p class="subtitle-lbl">管理您的月度目标、关键成果与绩效进度</p>
        </div>

        <div class="action-buttons-header">
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

          <!-- Export Button -->
          <button class="export-data-btn" @click="generatePerformanceChecklist" title="生成本月绩效清单">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="btn-icon">
              <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
            </svg>
            <span>生成绩效清单</span>
          </button>

          <!-- New Objective Button -->
          <button class="add-objective-btn" @click="openCatModal(null)">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="btn-icon">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
            </svg>
            <span>新增目标O</span>
          </button>
        </div>
      </div>
    </div>

    <!-- Main Monthly Progress Card -->
    <div class="monthly-total-progress-card premium-card">
      <div class="progress-header-row">
        <span class="progress-title-lbl">本月绩效总进度</span>
        <span class="progress-percentage-val">{{ krCompletionRate }}%</span>
      </div>

      <!-- Main Progress Bar -->
      <div class="main-progress-container-bar">
        <div class="main-progress-fill-bar" :style="{ width: `${krCompletionRate}%` }"></div>
      </div>

      <div class="progress-footer-meta">
        <span class="objectives-kr-count">
          当前共 {{ totalObjectivesCount }} 个目标，{{ totalKRsCount }} 个关键成果（已关闭/取消 {{ cancelledKRsCount }} 项）
        </span>
        <span class="update-time-lbl">
          更新于 {{ new Date().toISOString().split('T')[0] }}
        </span>
      </div>
    </div>

    <!-- Objectives (K) and Key Results (R) Hierarchy Checklist -->
    <div class="objectives-list-container">
      <div v-if="objectivesList.length > 0" class="obj-list-wrapper">
        
        <!-- Objective Card Item -->
        <div v-for="obj in objectivesList" :key="obj.id" class="objective-card premium-card">
          
          <!-- Objective Card Top Section -->
          <div class="obj-card-top">
            <div class="obj-title-col">
              <div class="target-icon-wrap">
                <!-- Target radar-like icon -->
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.2" stroke="currentColor" class="radar-icon">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 21a9.004 9.004 0 008.716-6.747M12 21a9.004 9.004 0 01-8.716-6.747M12 21c2.485 0 4.5-4.03 4.5-9S14.485 3 12 3m0 18c-2.485 0-4.5-4.03-4.5-9S9.515 3 12 3m0 0a8.997 8.997 0 017.843 4.582M12 3a8.997 8.997 0 00-7.843 4.582m15.686 0A11.953 11.953 0 0112 10.5c-2.998 0-5.74-1.1-7.843-2.918m15.686 0A8.959 8.959 0 0121 12c0 .778-.099 1.533-.284 2.253m0 0A17.919 17.919 0 0112 16.5c-3.162 0-6.133-.815-8.716-2.247m0 0A9.015 9.015 0 013 12c0-.778.099-1.533.284-2.253m0 0L12 10.5" />
                </svg>
              </div>
              <h3 class="objective-heading">O{{ obj.keyIndex }}: {{ obj.name }}</h3>
            </div>

            <div class="obj-card-actions">
              <span class="status-badge" :class="obj.status">
                {{ obj.status === 'done' ? '已完成' : (obj.status === 'in_progress' ? '进行中' : '未开始') }}
              </span>
              <!-- Edit Goal Name -->
              <button class="action-icon-link" @click="openCatModal(obj)" title="编辑目标名称">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10" />
                </svg>
              </button>
              <!-- Delete Goal -->
              <button class="action-icon-link delete-link" @click="handleDeleteCategory(obj)" title="删除此大目标">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" />
                </svg>
              </button>
            </div>
          </div>

          <p class="obj-desc-lbl">目标描述：{{ obj.description || '暂无详细描述，日常指标细化推进中。' }}</p>

          <!-- Objective Progress Bar -->
          <div class="obj-progress-section">
            <span class="obj-progress-lbl">目标进度</span>
            <div class="obj-progress-bar-container">
              <div class="obj-progress-fill-bar" :style="{ width: `${obj.progress}%` }"></div>
            </div>
            <span class="obj-progress-val">{{ obj.progress }}%</span>
          </div>

          <!-- Key Results (R) Section inside Objective -->
          <div class="key-results-sub-section">
            <div class="kr-sec-header">
              <h4>关键成果 K</h4>
              <button class="add-kr-link-btn" @click="openKRModalWithCat(obj.id)">
                <span>+ 新增关键成果</span>
              </button>
            </div>

             <!-- KR Cards List -->
            <div class="kr-items-wrapper">
              <div v-for="item in obj.items" :key="item.id" class="kr-item-box-row" :class="{ completed: item.status === 'done', cancelled: item.status === 'cancelled' }">
                
                <!-- Left Status Checkbox/Indicator -->
                <div class="kr-status-icon-wrap" @click="toggleKRStatus(item)" :title="item.status === 'not_started' ? '点击开始执行' : (item.status === 'in_progress' ? '标记为已完成' : (item.status === 'cancelled' ? '点击恢复指标' : '取消完成，回退为进行中'))">
                  <div class="custom-indicator-circle" :class="item.status">
                    <span v-if="item.status === 'done'" class="check-mark">✓</span>
                    <span v-else-if="item.status === 'cancelled'" class="check-mark" style="font-size: 10px; font-weight: 800;">✕</span>
                    <span v-else-if="item.status === 'in_progress'" class="spin-dot"></span>
                    <svg v-else xmlns="http://www.w3.org/2000/svg" fill="currentColor" viewBox="0 0 24 24" class="start-play-icon">
                      <path d="M8 5v14l11-7z" />
                    </svg>
                  </div>
                </div>

                <!-- Main KR Content -->
                <div class="kr-main-info">
                  <h5 class="kr-title-heading">K{{ item.keyIndex }}: {{ item.title }}</h5>
                  <div class="kr-time-meta" style="margin-top: 4px; display: flex; gap: 8px;">
                    <span v-if="item.status === 'done'" class="time-lbl completed">完成时间：{{ item.doneAt || item.planCompleteDate }}</span>
                    <span v-else-if="item.status === 'cancelled'" class="time-lbl cancelled">已取消指标</span>
                    <span v-else class="time-lbl planned">计划完成时间：{{ item.planCompleteDate }}</span>
                  </div>
                  <p class="kr-remark-content">
                    完成内容 / 交付说明：{{ item.remark || '暂无详细成果描述，日常待办任务关联执行中。' }}
                  </p>
                  <div v-if="item.sprintIds && item.sprintIds.length > 0" class="kr-time-meta" style="margin-top: 6px; display: flex; gap: 8px;">
                    <span class="time-lbl" style="background-color: #eff6ff; color: #2563eb; border: 1.5px solid #dbeafe; font-weight: 600;">
                      🔗 已关联团队冲刺任务 (共 {{ item.sprintIds.length }} 个)
                    </span>
                  </div>
                </div>

                <!-- Right Status Tag & Actions -->
                <div class="kr-right-meta-actions">
                  <span class="kr-status-tag" :class="item.status">
                    {{ item.status === 'done' ? '已完成' : (item.status === 'in_progress' ? '进行中' : (item.status === 'cancelled' ? '已取消' : '未开始')) }}
                  </span>
                  
                  <div class="kr-row-action-buttons">
                    <!-- Cancel / Restore KR -->
                    <button class="kr-action-btn-item cancel-btn" v-if="item.status !== 'done'" @click="toggleKRCancelled(item)" :title="item.status === 'cancelled' ? '恢复关键成果' : '取消关键成果'">
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
                      </svg>
                    </button>
                    <!-- View R records -->
                    <button class="kr-action-btn-item view-r-btn" v-if="item.status === 'done'" @click="openViewRModal(item)" title="查看成果记录 R">
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" />
                        <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      </svg>
                    </button>
                    <!-- Edit KR -->
                    <button class="kr-action-btn-item" @click="openKRModal(item)" title="编辑关键成果">
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10" />
                      </svg>
                    </button>
                    <!-- Delete KR -->
                    <button class="kr-action-btn-item delete" @click="handleDeleteKR(item)" title="物理删除关键成果">
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" />
                      </svg>
                    </button>
                  </div>
                </div>

              </div>

              <!-- Empty KR State -->
              <div v-if="obj.items.length === 0" class="empty-kr-row">
                💡 目标下暂未添加任何关键成果。请点击上方“+ 新增关键成果”录入具体考核项！
              </div>
            </div>
          </div>

        </div>

      </div>

      <!-- Empty K-Objectives State -->
      <div v-else class="empty-state premium-card dashboard-empty">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.2" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" d="M15.59 14.37a6 6 0 01-5.84 7.38v-4.8m5.84-2.58a14.98 14.98 0 006.16-12.12A14.98 14.98 0 009.64 8.38" />
        </svg>
        <h3>本月尚无绩效考核目标(K)</h3>
        <p>个人绩效大目标和细化关键成果是推动工作的主线，点击右上方“+ 新增目标K”开启本月规划！</p>
      </div>
    </div>

    <!-- 0. Add / Edit Target Category (K-Objective) Form Modal -->
    <div v-if="catModal.isOpen" class="modal-overlay" @mousedown="mousedownTarget = $event.target" @click="mousedownTarget === $event.currentTarget && $event.target === $event.currentTarget && (catModal.isOpen = false)">
      <div class="modal-content" @click.stop>
        <div class="modal-header-with-close">
          <h3>{{ catModal.isEdit ? '编辑目标O' : '新增目标O' }}</h3>
          <button class="modal-close-icon-btn" @click="catModal.isOpen = false" title="关闭弹窗">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        <form @submit.prevent="saveCategory">
          <div class="form-field">
            <label for="cat-name-field">目标名称</label>
            <input id="cat-name-field" v-model="catModal.name" type="text" required class="form-control" placeholder="例如：O1：完成Q2核心功能模块开发" />
          </div>
          <div class="form-field">
            <label for="cat-desc-field">目标描述</label>
            <textarea id="cat-desc-field" v-model="catModal.description" class="form-control" placeholder="请详细描述该目标的具体内容与期望结果" rows="3"></textarea>
          </div>
          <div class="modal-actions">
            <button type="button" class="btn btn-secondary" @click="catModal.isOpen = false">取消</button>
            <button type="submit" class="btn btn-primary">保存</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 1. KR Edit Form Modal (Add / Edit) -->
    <div v-if="krModal.isOpen" class="modal-overlay" @mousedown="mousedownTarget = $event.target" @click="mousedownTarget === $event.currentTarget && $event.target === $event.currentTarget && (krModal.isOpen = false)">
      <div class="modal-content" @click.stop>
        <h3>{{ krModal.isEdit ? '编辑绩效关键成果 (K)' : '录入新绩效关键成果 (K)' }}</h3>
        <form @submit.prevent="saveKR">
          <div class="form-field">
            <label>所属大目标 (O)</label>
            <div class="static-value-badge" style="padding: 10px 14px; background-color: #f8fafc; border: 1px solid var(--border-medium); border-radius: var(--radius-md); color: var(--text-main); font-weight: 600; font-size: 13.5px; display: flex; align-items: center; gap: 8px;">
              <span style="display: inline-block; width: 6px; height: 6px; border-radius: 50%; background-color: #3b82f6;"></span>
              O{{ getObjectiveIndex(krModal.categoryId) }}: {{ getObjectiveName(krModal.categoryId) }}
            </div>
          </div>
          <div class="form-field">
            <label for="kr-title-field">关键成果标题 (如：完成用户中心模块开发与单元测试)</label>
            <input id="kr-title-field" v-model="krModal.title" type="text" required class="form-control" placeholder="如 完成用户中心模块开发与单元测试" autocomplete="off" />
          </div>
          <div class="form-field">
            <label for="kr-remark-field">详细完成内容 / 交付说明 (选填)</label>
            <textarea id="kr-remark-field" v-model="krModal.remark" rows="3" class="form-control" placeholder="请输入具体验收指标与细节说明"></textarea>
          </div>
          <div v-if="!(krModal.isEdit && krModal.status === 'done')" class="form-row" style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
            <div class="form-field">
              <label>成果状态</label>
              <div class="custom-select-container" style="position: relative; width: 100%;">
                <div 
                  class="form-control beautiful-select-trigger" 
                  @click.stop="isStatusDropdownOpen = !isStatusDropdownOpen"
                  :class="{ 'is-active': isStatusDropdownOpen }"
                  style="display: flex; align-items: center; justify-content: space-between; cursor: pointer; padding: 10px 14px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background: #fff; transition: all var(--transition-fast); height: 42px;"
                >
                  <div style="display: flex; align-items: center; gap: 8px;">
                    <span style="display: inline-block; width: 8px; height: 8px; border-radius: 50%;" :style="{ backgroundColor: currentStatusOption.dot }"></span>
                    <span style="font-size: 14px; font-weight: 500; color: var(--text-main);">{{ currentStatusOption.label }}</span>
                  </div>
                  <svg 
                    xmlns="http://www.w3.org/2000/svg" 
                    fill="none" 
                    viewBox="0 0 24 24" 
                    stroke-width="2.2" 
                    stroke="currentColor" 
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
                    v-for="opt in visibleStatusOptions" 
                    :key="opt.value"
                    @click="selectStatus(opt.value)"
                    class="custom-dropdown-item"
                    :class="{ 'is-selected': krModal.status === opt.value }"
                    style="display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
                  >
                    <div style="display: flex; align-items: center; gap: 8px;">
                      <span style="display: inline-block; width: 6px; height: 6px; border-radius: 50%;" :style="{ backgroundColor: opt.dot }"></span>
                      <span style="font-size: 13.5px; font-weight: 500; color: var(--text-main);">{{ opt.label }}</span>
                    </div>
                    <svg 
                      v-if="krModal.status === opt.value"
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
            <div class="form-field">
              <label>计划完成时间 (选填)</label>
              <input 
                id="kr-date-field" 
                v-model="krModal.planCompleteDate" 
                type="date" 
                class="form-control beautiful-date-input" 
                :class="{ 'has-value': !!krModal.planCompleteDate }"
                @mousedown.prevent="($event.target as HTMLInputElement).showPicker?.()" 
              />
            </div>
          </div>
          <div v-if="!(krModal.isEdit && krModal.status === 'done')" class="form-field" style="margin-top: -6px; margin-bottom: 12px;">
            <small class="form-help-text" style="color: var(--text-muted); font-size: 11px; display: block; line-height: 1.4;">
              💡 填单规则：如果是“已完成”状态且未传日期，默认为当天；“未开始/进行中”状态且不传日期，默认是本月底。
            </small>
          </div>
          <div class="modal-actions">
            <button type="button" class="btn btn-secondary" @click="krModal.isOpen = false">取消</button>
            <button type="submit" class="btn btn-primary">保存</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 5. Custom Premium Confirm Modal (PRD-like Design) -->
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

    <!-- 6. Custom Premium Alert Modal (Error / Info) -->
    <div v-if="alertModal.isOpen" class="modal-overlay" @mousedown="mousedownTarget = $event.target" @click="mousedownTarget === $event.currentTarget && $event.target === $event.currentTarget && (alertModal.isOpen = false)">
      <div class="modal-content alert-modal-content" @click.stop style="max-width: 400px; padding: 24px; text-align: center;">
        <div class="alert-modal-header" style="display: flex; flex-direction: column; align-items: center; gap: 12px; margin-bottom: 16px;">
          <div class="error-icon-circle" style="width: 48px; height: 48px; border-radius: 50%; background-color: #fef2f2; color: #ef4444; display: flex; align-items: center; justify-content: center; flex-shrink: 0;">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.2" stroke="currentColor" style="width: 24px; height: 24px;">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" />
            </svg>
          </div>
          <h3 style="margin: 0; font-size: 18px; font-weight: 700; color: var(--text-main);">{{ alertModal.title }}</h3>
        </div>
        <div class="alert-modal-body" style="margin-bottom: 20px;">
          <p style="color: var(--text-main); font-size: 14px; lineHeight: 1.5; margin: 0; white-space: pre-wrap;">
            {{ alertModal.message }}
          </p>
        </div>
        <div class="modal-actions" style="display: flex; justify-content: center;">
          <button class="btn btn-primary" @click="alertModal.isOpen = false" style="padding: 8px 32px; font-size: 13px; min-width: 100px;">我知道了</button>
        </div>
      </div>
    </div>
    <!-- Cancel KR Reason Modal -->
    <div v-if="cancelKRModal.isOpen" class="modal-overlay" @mousedown="mousedownTarget = $event.target" @click="mousedownTarget === $event.currentTarget && $event.target === $event.currentTarget && (cancelKRModal.isOpen = false)">
      <div class="modal-content" @click.stop style="max-width: 440px; padding: 24px;">
        <div class="modal-header-with-close">
          <h3>取消关键成果 K</h3>
          <button class="modal-close-icon-btn" @click="cancelKRModal.isOpen = false" title="关闭弹窗">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        
        <p style="font-size: 13.5px; color: var(--text-muted); margin: 12px 0 16px; line-height: 1.5;">
          确定要取消关键成果【<strong>{{ cancelKRModal.krTitle }}</strong>】吗？请输入取消原因（最长 500 字符）：
        </p>

        <form @submit.prevent="submitKRCancel">
          <div class="form-field">
            <label for="cancel-reason-input">取消原因</label>
            <textarea 
              id="cancel-reason-input" 
              v-model="cancelKRModal.reason" 
              required 
              class="form-control" 
              placeholder="例如：因业务线调整，此需求推迟或不再落实" 
              rows="3"
              maxlength="500"
            ></textarea>
          </div>
          <div class="modal-actions" style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px;">
            <button type="button" class="btn btn-secondary" @click="cancelKRModal.isOpen = false">取消</button>
            <button type="submit" class="btn btn-danger" style="background-color: #ef4444; border-color: #ef4444; color: #fff;">确认取消</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 2. R records fill-in Modal -->
    <div v-if="rModal.isOpen" class="modal-overlay" @mousedown="mousedownTarget = $event.target" @click="mousedownTarget === $event.currentTarget && $event.target === $event.currentTarget && (rModal.isOpen = false)">
      <div class="modal-content" @click.stop style="max-width: 480px; padding: 24px;">
        <div class="modal-header-with-close" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
          <h3 style="margin: 0; font-size: 17px; font-weight: 700; color: var(--text-main);">确认完成关键成果并提交成果记录 R</h3>
          <button class="modal-close-icon-btn" @click="rModal.isOpen = false" title="关闭弹窗" style="background: none; border: none; cursor: pointer; color: var(--text-muted); display: flex; align-items: center; justify-content: center; padding: 4px; border-radius: 50%;">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" style="width: 18px; height: 18px;">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        
        <div class="modal-body" style="display: flex; flex-direction: column; gap: 14px; margin-bottom: 20px;">
          <p style="font-size: 13.5px; color: var(--text-muted); line-height: 1.5; margin: 0;">
            关键成果：<strong style="color: var(--text-main);">{{ rModal.krTitle }}</strong>
          </p>
          <p style="font-size: 12.5px; color: #475569; margin: 0; background-color: #f8fafc; padding: 10px 12px; border-radius: 6px; border: 1px solid var(--border-light); line-height: 1.5;">
            💡 提示：成果记录 R 是指该关键成果实际产出的各项成果或指标。可输入多条（非强制，可选）。
          </p>
          
          <div style="display: flex; flex-direction: column; gap: 10px; max-height: 240px; overflow-y: auto; padding-right: 4px; margin-top: 4px;">
            <div 
              v-for="(record, idx) in rModal.records" 
              :key="idx"
              style="display: flex; align-items: center; gap: 10px; width: 100%;"
            >
              <span style="font-size: 14px; font-weight: 700; color: var(--text-muted); min-width: 28px; text-align: right;">R{{ idx + 1 }}</span>
              <input 
                v-model="record.value" 
                type="text" 
                class="form-control" 
                placeholder="请输入具体成果记录，最长2000字" 
                maxlength="2000"
                style="flex: 1;"
              />
              <button 
                type="button" 
                @click="removeRInput(idx)"
                style="background: none; border: none; cursor: pointer; color: #ef4444; padding: 4px; display: flex; align-items: center; justify-content: center; border-radius: 4px; transition: all 0.2s;"
                title="删除该条"
              >
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" style="width: 18px; height: 18px;">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" />
                </svg>
              </button>
            </div>
          </div>
          
          <button 
            type="button" 
            class="btn btn-secondary" 
            @click="addRInput"
            style="width: 100%; display: flex; align-items: center; justify-content: center; gap: 6px; border: 1.5px dashed var(--border-medium); background-color: #f8fafc; font-size: 13px; font-weight: 600; padding: 8px 12px; height: 38px;"
          >
            <span>+ 添加成果记录 R</span>
          </button>
        </div>
        
        <div class="modal-actions" style="display: flex; justify-content: flex-end; gap: 10px;">
          <button class="btn btn-secondary" @click="rModal.isOpen = false">取消</button>
          <button class="btn btn-primary" @click="saveRRecords">保存并完成</button>
        </div>
      </div>
    </div>

    <!-- 3. View R records Modal -->
    <div v-if="viewRModal.isOpen" class="modal-overlay" @mousedown="mousedownTarget = $event.target" @click="mousedownTarget === $event.currentTarget && $event.target === $event.currentTarget && (viewRModal.isOpen = false)">
      <div class="modal-content" @click.stop style="max-width: 460px; padding: 24px;">
        <div class="modal-header-with-close" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
          <h3 style="margin: 0; font-size: 17px; font-weight: 700; color: var(--text-main);">成果记录 R 列表</h3>
          <button class="modal-close-icon-btn" @click="viewRModal.isOpen = false" title="关闭弹窗" style="background: none; border: none; cursor: pointer; color: var(--text-muted); display: flex; align-items: center; justify-content: center; padding: 4px; border-radius: 50%;">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" style="width: 18px; height: 18px;">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        
        <div class="modal-body" style="display: flex; flex-direction: column; gap: 14px; margin-bottom: 20px;">
          <p style="font-size: 13.5px; color: var(--text-muted); line-height: 1.5; margin: 0;">
            关键成果：<strong style="color: var(--text-main);">{{ viewRModal.krTitle }}</strong>
          </p>
          
          <div style="display: flex; flex-direction: column; gap: 10px; max-height: 280px; overflow-y: auto; padding-right: 4px; margin-top: 4px;">
            <div 
              v-for="(record, idx) in viewRModal.records" 
              :key="idx"
              style="display: flex; align-items: flex-start; gap: 10px; background-color: #f8fafc; padding: 10px 14px; border: 1.5px solid var(--border-light); border-radius: 6px;"
            >
              <span style="font-size: 13px; font-weight: 800; color: var(--text-muted); min-width: 24px; padding-top: 1px;">R{{ idx + 1 }}</span>
              <span style="font-size: 13.5px; color: var(--text-main); line-height: 1.5; word-break: break-all; flex: 1;">{{ record }}</span>
            </div>
            <div v-if="viewRModal.records.length === 0" style="text-align: center; padding: 24px 0; color: var(--text-muted); font-size: 13px;">
              暂无已填写的成果记录
            </div>
          </div>
        </div>
        
        <div class="modal-actions" style="display: flex; justify-content: center;">
          <button class="btn btn-primary" @click="viewRModal.isOpen = false" style="padding: 8px 32px; font-size: 13px;">关闭</button>
        </div>
      </div>
    </div>

    <!-- 4. Generate Performance Checklist Modal -->
    <div v-if="isChecklistModalOpen" class="modal-overlay" @mousedown="mousedownTarget = $event.target" @click="mousedownTarget === $event.currentTarget && $event.target === $event.currentTarget && (isChecklistModalOpen = false)">
      <div class="modal-content" @click.stop style="max-width: 600px; padding: 24px; display: flex; flex-direction: column; max-height: 85vh; gap: 8px;">
        <div class="modal-header-with-close" style="display: flex; justify-content: space-between; align-items: center; flex-shrink: 0;">
          <h3 style="margin: 0; font-size: 17px; font-weight: 700; color: var(--text-main);">生成绩效清单</h3>
          <button class="modal-close-icon-btn" @click="isChecklistModalOpen = false" title="关闭弹窗" style="background: none; border: none; cursor: pointer; color: var(--text-muted); display: flex; align-items: center; justify-content: center; padding: 4px; border-radius: 50%;">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" style="width: 18px; height: 18px;">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        
        <p style="font-size: 13px; color: var(--text-muted); margin: 0; line-height: 1.5; flex-shrink: 0;">
          已为您自动整理并生成本月绩效清单内容，可直接复制使用：
        </p>
        
        <div class="modal-body" style="display: flex; flex-direction: column; gap: 16px; overflow-y: auto; padding-right: 6px; flex-grow: 1; margin-bottom: 8px;">
          <div v-for="(sec, idx) in checklistSections" :key="idx" style="display: flex; flex-direction: column; gap: 6px;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 2px;">
              <h4 style="margin: 0; font-size: 14px; font-weight: 700; color: var(--text-main); display: flex; align-items: center; gap: 6px;">
                <span style="display: inline-block; width: 6px; height: 6px; border-radius: 50%; background-color: var(--primary);"></span>
                {{ sec.title }}
              </h4>
              <button 
                class="btn btn-secondary" 
                @click="copyText(sec.text)"
                style="padding: 2px 10px; font-size: 11px; height: 26px; display: flex; align-items: center; gap: 4px; border-color: var(--border-medium); border-radius: var(--radius-sm);"
                title="复制该目标的清单内容"
              >
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" style="width: 12px; height: 12px;">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M15.666 3.888A2.25 2.25 0 0013.5 2.25h-3c-1.03 0-1.9.693-2.166 1.638m7.332 0c.055.194.084.4.084.612v0a.75.75 0 01-.75.75H9a.75.75 0 01-.75-.75v0c0-.212.03-.418.084-.612m7.332 0c.646.049 1.288.11 1.927.184 1.1.128 1.907 1.077 1.907 2.185V19.5a2.25 2.25 0 01-2.25 2.25H6.75A2.25 2.25 0 014.5 19.5V6.257c0-1.108.806-2.057 1.907-2.185a48.208 48.208 0 011.927-.184" />
                </svg>
                <span>复制</span>
              </button>
            </div>
            <textarea 
              readonly 
              v-model="sec.text" 
              :rows="sec.text ? Math.min(Math.max(sec.text.split('\n').length, 3), 10) : 2" 
              class="form-control" 
              style="width: 100%; font-family: monospace; font-size: 13px; line-height: 1.6; padding: 12px; background-color: #f8fafc; border: 1.5px solid var(--border-medium); border-radius: 8px; resize: none; color: var(--text-main);"
              @click="($event.target as HTMLTextAreaElement).select()"
              title="点击全选该部分内容"
            ></textarea>
          </div>
        </div>
      </div>
    </div>

    <!-- 3. Performance Category Management Drawer (PRD 6.8.1 / Issue 3) -->
    <transition name="drawer-slide">
      <div v-if="isPerfCatDrawerOpen" class="drawer-overlay" @mousedown="mousedownTarget = $event.target" @click="mousedownTarget === $event.currentTarget && $event.target === $event.currentTarget && (isPerfCatDrawerOpen = false)">
        <div class="detail-drawer" @click.stop>
          <div class="drawer-header">
            <h3>⚙️ 绩效分类维度管理</h3>
            <button class="close-btn" @click="isPerfCatDrawerOpen = false">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <div class="drawer-content-scroll">
            <p class="drawer-tip">定义您的绩效维度分类大项（例如：天宫建模开发、建模支持等），用于绩效 KR 的结构化组织分类。</p>
            
            <!-- Quick Add Performance Category -->
            <form @submit.prevent="handleQuickAddPerfCat" class="perf-cat-quick-add">
              <input 
                v-model="newPerfCatName" 
                type="text" 
                placeholder="新增分类大类名称..." 
                required 
                class="form-control" 
              />
              <button type="submit" class="btn btn-primary btn-sm">添加</button>
            </form>

            <!-- List of Performance Categories -->
            <div class="perf-cats-list">
              <div 
                v-for="cat in performanceCategories" 
                :key="cat.id" 
                class="perf-cat-item"
              >
                <!-- Edit mode vs View mode -->
                <div v-if="editingPerfCatId === cat.id" class="edit-row">
                  <input 
                    v-model="editingPerfCatName" 
                    type="text" 
                    class="form-control" 
                    required 
                  />
                  <button class="action-icon-btn save" @click="handleSavePerfCat(cat.id)">💾</button>
                  <button class="action-icon-btn cancel" @click="editingPerfCatId = null">❌</button>
                </div>
                <div v-else class="view-row">
                  <span class="cat-name">📂 {{ cat.name }}</span>
                  <div class="item-actions">
                    <button class="action-icon-btn" @click="startEditPerfCat(cat)" title="修改分类名">✏️</button>
                    <button class="action-icon-btn delete" @click="handleDeletePerfCat(cat)" title="删除此分类">🗑️</button>
                  </div>
                </div>
              </div>

              <div v-if="performanceCategories.length === 0" class="empty-perf-cats">
                目前还没有设定任何绩效分类。请在上方输入添加！
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
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useTodoStore } from '@/stores/todo'
import type { PerformanceKR, PerformanceCategory } from '@/stores/todo'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const todoStore = useTodoStore()

const currentMonthStr = computed(() => {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  return `${y}-${m}`
})

// Selected Month (YYYY-MM)
const selectedMonth = ref(route.params.month as string || currentMonthStr.value)

const loadData = async (month: string) => {
  if (authStore.currentUser) {
    todoStore.setMonth(month)
    await todoStore.refreshObjectives(authStore.currentUser.userId)
  }
}

watch(selectedMonth, (newVal) => {
  router.push(`/plan/performance/${newVal}`)
  loadData(newVal)
})

watch(() => route.params.month, (newParam) => {
  if (newParam) {
    selectedMonth.value = newParam as string
  }
})

const formattedMonthLabel = computed(() => {
  const [year, month] = selectedMonth.value.split('-').map(Number)
  return `${year}年${month}月`
})

// Data Source
const performanceKRs = computed(() => todoStore.performanceKRs)
const dailyCategories = computed(() => todoStore.sortedCategories)
const performanceCategories = computed(() => todoStore.performanceCategories)

// Metrics
const totalKRs = computed(() => performanceKRs.value.filter(kr => kr.status !== 'cancelled').length)
const completedKRs = computed(() => performanceKRs.value.filter(kr => kr.status === 'done').length)
const krCompletionRate = computed(() => {
  if (totalKRs.value === 0) return 0
  return Math.round((completedKRs.value / totalKRs.value) * 100)
})

// Group KRs by Category (PRD 6.8.1 / Issue 3)
const krsGrouped = computed(() => {
  const map: Record<number, PerformanceKR[]> = {}
  performanceKRs.value.forEach(kr => {
    if (!map[kr.categoryId]) {
      map[kr.categoryId] = []
    }
    map[kr.categoryId].push(kr)
  })
  
  return performanceCategories.value.map(cat => {
    const items = map[cat.id] || []
    const totalCount = items.filter(i => i.status !== 'cancelled').length
    const completedCount = items.filter(i => i.status === 'done').length
    return {
      categoryId: cat.id,
      categoryName: cat.name,
      items,
      totalCount,
      completedCount
    }
  }).filter(g => g.totalCount > 0 || performanceCategories.value.length > 0)
})

// Performance Category Management Drawer State
const isPerfCatDrawerOpen = ref(false)
const newPerfCatName = ref('')
const editingPerfCatId = ref<number | null>(null)
const editingPerfCatName = ref('')

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

const mousedownTarget = ref<EventTarget | null>(null)

// Modals States
const krModal = reactive({
  isOpen: false,
  isEdit: false,
  id: 0,
  categoryId: null as number | null,
  title: '',
  remark: '',
  status: 'not_started' as 'not_started' | 'in_progress' | 'done' | 'cancelled',
  planCompleteDate: ''
})

const cancelKRModal = reactive({
  isOpen: false,
  krId: 0,
  krTitle: '',
  reason: ''
})

const rModal = reactive({
  isOpen: false,
  krId: 0,
  krTitle: '',
  records: [] as { value: string }[]
})

const viewRModal = reactive({
  isOpen: false,
  krTitle: '',
  records: [] as string[]
})

const isChecklistModalOpen = ref(false)
const checklistText = ref('')
const checklistSections = ref([] as { title: string; text: string }[])

// Custom Dropdown logic for KR Status
const isStatusDropdownOpen = ref(false)
const statusOptions = [
  { value: 'not_started', label: '未开始', color: '#64748b', dot: '#94a3b8' },
  { value: 'in_progress', label: '进行中', color: '#2563eb', dot: '#3b82f6' },
  { value: 'done', label: '已完成', color: '#16a34a', dot: '#22c55e' },
  { value: 'cancelled', label: '已取消', color: '#94a3b8', dot: '#cbd5e1' }
] as const

const currentStatusOption = computed(() => {
  return statusOptions.find(opt => opt.value === krModal.status) || statusOptions[0]
})

const visibleStatusOptions = computed(() => {
  if (krModal.isEdit) {
    if (krModal.status === 'cancelled') {
      return statusOptions
    }
    return statusOptions.filter(opt => opt.value !== 'cancelled')
  }
  return statusOptions.filter(opt => opt.value !== 'cancelled')
})

const selectStatus = (val: 'not_started' | 'in_progress' | 'done' | 'cancelled') => {
  krModal.status = val
  handleKRStatusChange()
  isStatusDropdownOpen.value = false
}

const closeStatusDropdown = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  if (!target.closest('.custom-select-container')) {
    isStatusDropdownOpen.value = false
  }
}

onMounted(() => {
  window.addEventListener('click', closeStatusDropdown, true)
})

onUnmounted(() => {
  window.removeEventListener('click', closeStatusDropdown, true)
})

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

const alertModal = reactive({
  isOpen: false,
  title: '系统提示',
  message: ''
})

const showAlert = (message: string, title = '系统提示') => {
  alertModal.title = title
  alertModal.message = message
  alertModal.isOpen = true
}

const getObjectiveName = (catId: number | null) => {
  if (!catId) return '未知目标'
  const cat = performanceCategories.value.find(c => c.id === catId)
  return cat ? cat.name : '未知目标'
}

const getObjectiveIndex = (catId: number | null) => {
  if (!catId) return ''
  const idx = performanceCategories.value.findIndex(c => c.id === catId)
  return idx !== -1 ? `${idx + 1}` : ''
}



// --- Performance Category CRUD Methods ---
const handleQuickAddPerfCat = async () => {
  if (!authStore.currentUser) return
  const name = newPerfCatName.value.trim()
  if (!name) return
  if (name.length > 20) {
    showAlert('分类大类名称最长为 20 个字符')
    return
  }
  try {
    await todoStore.addPerformanceCategory(authStore.currentUser.userId, name)
    newPerfCatName.value = ''
    const event = new CustomEvent('app-toast', { detail: { text: '成功添加绩效分类' } })
    window.dispatchEvent(event)
  } catch (err: any) {
    // Global toast handler will display the error
  }
}

const startEditPerfCat = (cat: PerformanceCategory) => {
  editingPerfCatId.value = cat.id
  editingPerfCatName.value = cat.name
}

const handleSavePerfCat = async (id: number) => {
  if (!authStore.currentUser) return
  const name = editingPerfCatName.value.trim()
  if (!name) return
  if (name.length > 20) {
    showAlert('分类大类名称最长为 20 个字符')
    return
  }
  try {
    await todoStore.updatePerformanceCategory(authStore.currentUser.userId, id, name)
    editingPerfCatId.value = null
    const event = new CustomEvent('app-toast', { detail: { text: '绩效分类修改成功' } })
    window.dispatchEvent(event)
  } catch (err: any) {
    // Global toast handler will display the error
  }
}

const handleDeletePerfCat = (cat: PerformanceCategory) => {
  if (!authStore.currentUser) return
  const userId = authStore.currentUser.userId
  
  // Cascade verification warning
  const count = performanceKRs.value.filter(kr => kr.categoryId === cat.id).length
  let confirmMsg = `确定要删除绩效分类【${cat.name}】吗？`
  if (count > 0) {
    confirmMsg += `\n⚠️ 警告：该分类下已录入 ${count} 项关键成果指标(KR)，删除此分类将连同这些指标数据一起物理删除，不可恢复！`
  }
  
  showConfirm('删除绩效分类', confirmMsg, async () => {
    try {
      await todoStore.deletePerformanceCategory(userId, cat.id)
      const event = new CustomEvent('app-toast', { detail: { text: `绩效分类【${cat.name}】已成功删除` } })
      window.dispatchEvent(event)
    } catch (err: any) {
      // Global toast handler will display the error
    }
  })
}

const openQuickCategoryModal = async () => {
  if (!authStore.currentUser) return
  const name = prompt('请输入新绩效分类名称（最长 20 字符）:')
  if (name && name.trim()) {
    if (name.trim().length > 20) {
      showAlert('分类大类名称最长为 20 个字符')
      return
    }
    try {
      const newCat = await todoStore.addPerformanceCategory(authStore.currentUser.userId, name.trim())
      krModal.categoryId = newCat.id // Auto-select newly created
      const event = new CustomEvent('app-toast', { detail: { text: `成功创建分类【${name.trim()}】并已自动选择` } })
      window.dispatchEvent(event)
    } catch (err: any) {
      // Global toast handler will display the error
    }
  }
}

// --- KR Operations ---
const openKRModal = (kr: PerformanceKR | null) => {
  if (kr) {
    krModal.isEdit = true
    krModal.id = kr.id
    krModal.categoryId = kr.categoryId
    krModal.title = kr.title
    krModal.remark = kr.remark || ''
    krModal.status = kr.status
    krModal.planCompleteDate = kr.planCompleteDate || ''
  } else {
    krModal.isEdit = false
    krModal.id = 0
    // Auto select first performance category if exists
    krModal.categoryId = performanceCategories.value.length > 0 ? performanceCategories.value[0].id : null
    krModal.title = ''
    krModal.remark = ''
    krModal.status = 'not_started'
    krModal.planCompleteDate = ''
  }
  krModal.isOpen = true
}

const getMonthLastDay = (monthStr: string) => {
  if (!monthStr) return ''
  const [year, month] = monthStr.split('-').map(Number)
  const lastDay = new Date(year, month, 0).getDate()
  return `${monthStr}-${String(lastDay).padStart(2, '0')}`
}

const handleKRStatusChange = () => {
  if (krModal.status === 'done') {
    krModal.planCompleteDate = new Date().toISOString().split('T')[0]
  }
}

const saveKR = async () => {
  if (!authStore.currentUser) return
  if (!krModal.categoryId) {
    showAlert('请选择所属的绩效分类大项！')
    return
  }

  let finalDate = krModal.planCompleteDate ? krModal.planCompleteDate.trim() : ''
  if (krModal.status === 'done') {
    if (!finalDate) {
      finalDate = new Date().toISOString().split('T')[0]
    }
  } else {
    if (!finalDate) {
      finalDate = getMonthLastDay(selectedMonth.value)
    }
  }
  
  try {
    if (krModal.isEdit) {
      const existingKR = performanceKRs.value.find(k => k.id === krModal.id)
      const isStatusTransitioningToDone = krModal.status === 'done' && (!existingKR || existingKR.status !== 'done')

      if (isStatusTransitioningToDone) {
        await todoStore.updatePerformanceKR(authStore.currentUser.userId, krModal.id, {
          categoryId: krModal.categoryId,
          title: krModal.title.trim(),
          remark: krModal.remark.trim(),
          planCompleteDate: finalDate
        })
        krModal.isOpen = false
        openRModal({
          id: krModal.id,
          title: krModal.title.trim(),
          status: existingKR ? existingKR.status : 'in_progress',
          records: existingKR ? existingKR.records : []
        } as any)
      } else {
        const updatePayload: any = {
          categoryId: krModal.categoryId,
          title: krModal.title.trim(),
          remark: krModal.remark.trim(),
          planCompleteDate: finalDate
        }
        if (existingKR && existingKR.status !== krModal.status) {
          updatePayload.status = krModal.status
        }
        await todoStore.updatePerformanceKR(authStore.currentUser.userId, krModal.id, updatePayload)
        const event = new CustomEvent('app-toast', { detail: { text: '修改关键成果 (K) 成功！' } })
        window.dispatchEvent(event)
        krModal.isOpen = false
      }
    } else {
      const newKR = await todoStore.addPerformanceKR(
        authStore.currentUser.userId,
        krModal.categoryId,
        krModal.title.trim(),
        krModal.remark.trim(),
        finalDate
      )
      krModal.isOpen = false
      if (krModal.status === 'done') {
        openRModal(newKR)
      } else {
        if (krModal.status === 'in_progress') {
          await todoStore.updatePerformanceKR(authStore.currentUser.userId, newKR.id, { status: 'in_progress' })
        }
        const event = new CustomEvent('app-toast', { detail: { text: '成功录入绩效关键成果 (K)！' } })
        window.dispatchEvent(event)
      }
    }
  } catch (err: any) {
    // Global toast handler will display the error
  }
}

// --- Cancel / Restore Key Result Operations ---
const toggleKRCancelled = (kr: PerformanceKR) => {
  if (!authStore.currentUser) return
  if (kr.status === 'cancelled') {
    executeKRRestore(kr)
  } else {
    cancelKRModal.krId = kr.id
    cancelKRModal.krTitle = kr.title
    cancelKRModal.reason = ''
    cancelKRModal.isOpen = true
  }
}

const executeKRRestore = async (kr: PerformanceKR) => {
  if (!authStore.currentUser) return
  try {
    await todoStore.updatePerformanceKR(authStore.currentUser.userId, kr.id, { 
      status: 'not_started' 
    })
    const event = new CustomEvent('app-toast', { detail: { text: '已将已取消的关键成果恢复为未开始。' } })
    window.dispatchEvent(event)
  } catch (err: any) {
    // Error is handled globally
  }
}

const submitKRCancel = async () => {
  if (!authStore.currentUser) return
  try {
    await todoStore.updatePerformanceKR(authStore.currentUser.userId, cancelKRModal.krId, { 
      status: 'cancelled',
      cancelReason: cancelKRModal.reason.trim()
    })
    cancelKRModal.isOpen = false
    const event = new CustomEvent('app-toast', { detail: { text: '已取消该关键成果。' } })
    window.dispatchEvent(event)
  } catch (err: any) {
    // Error is handled globally
  }
}

const toggleKRStatus = async (kr: PerformanceKR) => {
  if (!authStore.currentUser) return
  let newStatus: 'not_started' | 'in_progress' | 'done' | 'cancelled' = 'in_progress'
  let toastText = ''
  
  if (kr.status === 'not_started') {
    newStatus = 'in_progress'
    toastText = '关键成果已点击开始，设为进行中！'
  } else if (kr.status === 'in_progress') {
    openRModal(kr)
    return
  } else if (kr.status === 'done') {
    newStatus = 'in_progress'
    toastText = '已取消完成，关键成果恢复为进行中。'
  } else if (kr.status === 'cancelled') {
    newStatus = 'not_started'
    toastText = '已将已取消的关键成果恢复为未开始。'
  }
  
  try {
    await todoStore.updatePerformanceKR(authStore.currentUser.userId, kr.id, { status: newStatus })
    const event = new CustomEvent('app-toast', { detail: { text: toastText } })
    window.dispatchEvent(event)
  } catch (err: any) {
    // Global toast handler will display the error
  }
}

const openRModal = (kr: PerformanceKR) => {
  rModal.krId = kr.id
  rModal.krTitle = kr.title
  rModal.records = kr.records && kr.records.length > 0
    ? kr.records.map(r => ({ value: r }))
    : [{ value: '' }]
  rModal.isOpen = true
}

const addRInput = () => {
  if (rModal.records.length >= 50) {
    showAlert('成果记录最多只能添加 50 条！')
    return
  }
  rModal.records.push({ value: '' })
}

const removeRInput = (index: number) => {
  rModal.records.splice(index, 1)
  if (rModal.records.length === 0) {
    rModal.records.push({ value: '' })
  }
}

const saveRRecords = async () => {
  if (!authStore.currentUser) return
  const cleanRecords = rModal.records
    .map(r => r.value.trim())
    .filter(r => r !== '')

  try {
    await todoStore.updatePerformanceKR(authStore.currentUser.userId, rModal.krId, {
      status: 'done',
      records: cleanRecords
    })
    rModal.isOpen = false
    const event = new CustomEvent('app-toast', { detail: { text: '关键成果已标记完成并保存成果记录！' } })
    window.dispatchEvent(event)
  } catch (err: any) {
    // Error is handled globally
  }
}

const openViewRModal = (kr: PerformanceKR) => {
  viewRModal.krTitle = kr.title
  viewRModal.records = kr.records || []
  viewRModal.isOpen = true
}

const handleDeleteKR = (kr: PerformanceKR) => {
  if (!authStore.currentUser) return
  const userId = authStore.currentUser.userId
  
  let confirmMsg = `确定要删除关键成果【${kr.title}】吗？`
  const warnings: string[] = []
  
  if (kr.sprintIds && kr.sprintIds.length > 0) {
    warnings.push(`⚠️ 该关键成果已被团队冲刺任务关联 (共 ${kr.sprintIds.length} 个任务)，删除后关联关系将一并解除！`)
  }
  if (kr.status === 'done') {
    warnings.push(`⚠️ 该关键成果目前处于【已完成】状态。`)
  }
  
  if (warnings.length > 0) {
    confirmMsg += '\n' + warnings.join('\n')
  } else {
    confirmMsg += '\n⚠️ 警告：删除后数据将不可恢复！'
  }
  
  showConfirm(
    '删除关键成果 K',
    confirmMsg,
    async () => {
      try {
        await todoStore.deletePerformanceKR(userId, kr.id)
        const event = new CustomEvent('app-toast', { detail: { text: `关键成果【${kr.title}】已删除` } })
        window.dispatchEvent(event)
      } catch (err: any) {
        // Global toast handler will display the error
      }
    }
  )
}

const handleCarryForward = (kr: PerformanceKR) => {
  if (!authStore.currentUser) return
  todoStore.carryForwardKR(authStore.currentUser.userId, kr.id)
  const event = new CustomEvent('app-toast', { detail: { text: '已将此指标结转/复制到下月绩效版' } })
  window.dispatchEvent(event)
}



// --- OKR-Oriented Monthly Performance Logic (K objectives & R Key Results) ---
const catModal = reactive({
  isOpen: false,
  isEdit: false,
  id: 0,
  name: '',
  description: ''
})

// Fetch all derived daily tasks for a given KR
const getDerivedTodosForKR = (krId: number) => {
  return todoStore.todos.filter(t => t.derivedFromType === 'performance' && t.derivedFromId === krId)
}

// Calculate completion status of derived daily tasks
const getDerivedStats = (kr: PerformanceKR) => {
  const list = getDerivedTodosForKR(kr.id)
  const total = list.length
  const completed = list.filter(t => t.status === 'done').length
  return { total, completed }
}

// Map categories to "K Objectives" and KRs to "R Key Results"
const objectivesList = computed(() => {
  return performanceCategories.value.map((cat, idx) => {
    const items = performanceKRs.value.filter(kr => kr.categoryId === cat.id)
    const totalCount = items.filter(kr => kr.status !== 'cancelled').length
    const completedCount = items.filter(kr => kr.status === 'done').length
    const progress = totalCount === 0 ? 0 : Math.round((completedCount / totalCount) * 100)
    
    // Use the parsed objective status directly computed and returned by backend
    const status = cat.status || 'not_started'
    
    // Retrieve description natively from store entity
    const description = cat.description || ''
    
    return {
      id: cat.id,
      keyIndex: idx + 1, // K1, K2...
      name: cat.name,
      description,
      progress,
      status,
      items: items.map((kr, krIdx) => {
        const stats = getDerivedStats(kr)
        return {
          ...kr,
          keyIndex: `${krIdx + 1}`, // K1, K2...
          derivedStats: stats
        }
      }),
      totalCount,
      completedCount
    }
  })
})

// Total stats
const totalKRsCount = computed(() => performanceKRs.value.length)
const cancelledKRsCount = computed(() => performanceKRs.value.filter(kr => kr.status === 'cancelled').length)
const totalObjectivesCount = computed(() => performanceCategories.value.length)

// Action triggers
const openCatModal = (cat: any | null) => {
  if (cat) {
    catModal.isEdit = true
    catModal.id = cat.id
    catModal.name = cat.name
    catModal.description = cat.description || ''
  } else {
    catModal.isEdit = false
    catModal.id = 0
    catModal.name = ''
    catModal.description = ''
  }
  catModal.isOpen = true
}

const saveCategory = async () => {
  if (!authStore.currentUser) return
  const name = catModal.name.trim()
  const description = catModal.description.trim()
  if (!name) return
  if (name.length > 20) {
    showAlert('目标名称最长为 20 个字符')
    return
  }
  
  try {
    if (catModal.isEdit) {
      await todoStore.updatePerformanceCategory(authStore.currentUser.userId, catModal.id, name, description)
      const event = new CustomEvent('app-toast', { detail: { text: '目标更新成功！' } })
      window.dispatchEvent(event)
    } else {
      await todoStore.addPerformanceCategory(authStore.currentUser.userId, name, description)
      const event = new CustomEvent('app-toast', { detail: { text: '成功录入新月度大目标！' } })
      window.dispatchEvent(event)
    }
    catModal.isOpen = false
  } catch (err: any) {
    // Global toast handler will display the error
  }
}

const handleDeleteCategory = (cat: { id: number; name: string }) => {
  if (!authStore.currentUser) return
  const userId = authStore.currentUser.userId
  
  const count = performanceKRs.value.filter(kr => kr.categoryId === cat.id).length
  let confirmMsg = `确定要删除大目标【${cat.name}】吗？`
  if (count > 0) {
    confirmMsg += `\n⚠️ 警告：该目标下已录入 ${count} 项关键成果(K)，删除目标将连同这些关键成果一并删除，数据不可恢复！`
  }
  
  showConfirm('删除大目标 O', confirmMsg, async () => {
    try {
      await todoStore.deletePerformanceCategory(userId, cat.id)
      const event = new CustomEvent('app-toast', { detail: { text: `目标【${cat.name}】已成功删除` } })
      window.dispatchEvent(event)
    } catch (err: any) {
      // Global toast handler will display the error
    }
  })
}

const openKRModalWithCat = (catId: number) => {
  krModal.isEdit = false
  krModal.id = 0
  krModal.categoryId = catId
  krModal.title = ''
  krModal.remark = ''
  krModal.status = 'not_started'
  krModal.planCompleteDate = ''
  krModal.isOpen = true
}

const toChineseNumeral = (num: number): string => {
  const chineseNumerals = ['零', '一', '二', '三', '四', '五', '六', '七', '八', '九', '十'];
  if (num <= 10) return chineseNumerals[num];
  if (num < 20) return '十' + (num % 10 === 0 ? '' : chineseNumerals[num % 10]);
  if (num < 100) {
    const ten = Math.floor(num / 10);
    const one = num % 10;
    return chineseNumerals[ten] + '十' + (one === 0 ? '' : chineseNumerals[one]);
  }
  return String(num);
}

// Generate structured performance checklist
const generatePerformanceChecklist = () => {
  if (performanceKRs.value.length === 0 && performanceCategories.value.length === 0) {
    showAlert('暂无绩效数据可生成清单！')
    return
  }
  
  const sections: { title: string; text: string }[] = []
  
  objectivesList.value.forEach(obj => {
    // Filter KRs that are not cancelled
    const activeKRs = obj.items.filter(kr => kr.status !== 'cancelled')
    
    let objText = ''
    activeKRs.forEach((kr, krIdx) => {
      // 工作一, 工作二, etc.
      const workNum = toChineseNumeral(krIdx + 1)
      objText += `[工作${workNum}]${kr.title}\n`
      
      // Determine progress using getDerivedStats completed/total or status
      let progress = 0
      if (kr.status === 'done') {
        progress = 100
      } else if (kr.status === 'in_progress') {
        const stats = getDerivedStats(kr)
        progress = stats.total > 0 ? Math.round((stats.completed / stats.total) * 100) : 50
      }
      
      // Format R content or use default if not started
      if (kr.status === 'not_started') {
        objText += `完成进度：由于该功能从产品层面那边暂时没有完整的功能内容，无法提前知道具体方向，无法完成该功能的设计和实现，进度0%\n`
      } else {
        let rContent = ''
        if (kr.records && kr.records.length > 0) {
          rContent = kr.records.map((r, i) => `(${i + 1})${r}`).join('')
        } else {
          rContent = '进行中，相关工作正在推进'
        }
        objText += `完成进度：${rContent}，进度${progress}%\n`
      }
    })
    
    // Trim final newline
    if (objText.endsWith('\n')) {
      objText = objText.substring(0, objText.length - 1)
    }
    
    sections.push({
      title: obj.name,
      text: objText
    })
  })
  
  checklistSections.value = sections
  
  // Combine for full copying
  let fullText = ''
  sections.forEach((sec, idx) => {
    fullText += `${sec.title}\n${sec.text}`
    if (idx < sections.length - 1) {
      fullText += '\n\n'
    }
  })
  checklistText.value = fullText
  isChecklistModalOpen.value = true
}

const copyText = async (val: string) => {
  if (!val) {
    const event = new CustomEvent('app-toast', { detail: { text: '内容为空，无需复制。', type: 'error' } })
    window.dispatchEvent(event)
    return
  }
  try {
    await navigator.clipboard.writeText(val)
    const event = new CustomEvent('app-toast', { detail: { text: '已复制当前目标清单！' } })
    window.dispatchEvent(event)
  } catch (err) {
    const event = new CustomEvent('app-toast', { detail: { text: '复制失败，请手动选择复制。', type: 'error' } })
    window.dispatchEvent(event)
  }
}
</script>

<style scoped>
.performance-view {
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
.title-meta-left h2 {
  font-size: 22px;
  font-weight: 800;
  color: var(--text-main);
  margin-bottom: 4px;
}
.subtitle-lbl {
  font-size: 13.5px;
  color: var(--text-muted);
}

.action-buttons-header {
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

.export-data-btn {
  background-color: #fff;
  border: 1.5px solid #2563eb;
  color: #2563eb;
  border-radius: var(--radius-md);
  padding: 0 18px;
  height: 42px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all var(--transition-fast);
}
.export-data-btn:hover {
  background-color: rgba(37, 99, 235, 0.05);
}
.export-data-btn .btn-icon {
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

/* 3. Objectives & Key Results Hierarchy Container */
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

/* 4. Objective (K) Card */
.objective-card {
  background-color: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 28px 32px;
  border: 1px solid var(--border-medium);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.obj-card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}
.obj-title-col {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;
  min-width: 0;
}
.target-icon-wrap {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: rgba(37, 99, 235, 0.08);
  color: #2563eb;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.radar-icon {
  width: 20px;
  height: 20px;
}
.objective-heading {
  font-size: 17px;
  font-weight: 700;
  color: var(--text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.obj-card-actions {
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
.status-badge.pending,
.status-badge.in_progress {
  background-color: #eff6ff;
  color: #2563eb;
}
.status-badge.not_started {
  background-color: #f1f5f9;
  color: #64748b;
}
.status-badge.done {
  background-color: #f0fdf4;
  color: #10b981;
}

.action-icon-link {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--text-muted);
  padding: 6px;
  border-radius: var(--radius-sm);
  display: flex;
  transition: all var(--transition-fast);
}
.action-icon-link:hover {
  background-color: var(--border-light);
  color: var(--text-main);
}
.action-icon-link.delete-link:hover {
  background-color: var(--danger-bg);
  color: var(--danger);
}
.action-icon-link svg {
  width: 17px;
  height: 17px;
}

.obj-desc-lbl {
  font-size: 13.5px;
  color: var(--text-muted);
  line-height: 1.5;
  margin-top: -4px;
}

/* 5. Objective Progress Slider Bar */
.obj-progress-section {
  display: flex;
  align-items: center;
  gap: 16px;
  background-color: #f8fafc;
  padding: 12px 18px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
}
.obj-progress-lbl {
  font-size: 12.5px;
  font-weight: 700;
  color: var(--text-muted);
}
.obj-progress-bar-container {
  flex: 1;
  height: 10px;
  background-color: #e2e8f0;
  border-radius: var(--radius-full);
  overflow: hidden;
}
.obj-progress-fill-bar {
  height: 100%;
  background-color: #10b981;
  border-radius: var(--radius-full);
  transition: width 0.6s ease;
}
.obj-progress-val {
  font-size: 14px;
  font-weight: 800;
  color: var(--text-main);
}

/* 6. Key Results Sub-Section (OKR Hierarchy Layout) */
.key-results-sub-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 8px;
}
.kr-sec-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1.5px solid var(--border-light);
  padding-bottom: 10px;
}
.kr-sec-header h4 {
  font-size: 14.5px;
  font-weight: 700;
  color: var(--text-main);
}
.add-kr-link-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: #2563eb;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: opacity var(--transition-fast);
}
.add-kr-link-btn:hover {
  opacity: 0.8;
  text-decoration: underline;
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
.kr-item-box-row.cancelled {
  opacity: 0.65;
  background-color: #f8fafc;
  border-style: dashed;
}

/* Status checkbox custom look */
.kr-status-icon-wrap {
  padding-top: 2px;
  cursor: pointer;
  flex-shrink: 0;
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
.start-play-icon {
  width: 10px;
  height: 10px;
  margin-left: 1px;
  transition: transform var(--transition-fast);
}
.custom-indicator-circle.not_started:hover .start-play-icon {
  transform: scale(1.15);
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
.custom-indicator-circle.cancelled {
  border-color: #94a3b8;
  background-color: #cbd5e1;
  color: #64748b;
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

/* KR Text columns */
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
.time-lbl.planned {
  color: #2563eb;
  background-color: #eff6ff;
  padding: 2px 6px;
  border-radius: 4px;
}
.time-lbl.completed {
  color: #16a34a;
  background-color: #f0fdf4;
  padding: 2px 6px;
  border-radius: 4px;
}
.kr-remark-content {
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.4;
}

/* Right alignment */
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
.kr-status-tag.pending,
.kr-status-tag.in_progress {
  background-color: #eff6ff;
  color: #2563eb;
}
.kr-status-tag.not_started {
  background-color: #f1f5f9;
  color: #64748b;
}
.kr-status-tag.done {
  background-color: #e6fcf5;
  color: #0ca678;
}
.kr-status-tag.cancelled {
  background-color: #f1f5f9;
  color: #94a3b8;
}
.time-lbl.cancelled {
  color: #64748b;
  background-color: #f1f5f9;
  padding: 2px 6px;
  border-radius: 4px;
}
.cancelled .kr-title-heading {
  text-decoration: line-through;
  color: var(--text-muted);
}

.kr-row-action-buttons {
  display: flex;
  align-items: center;
  gap: 6px;
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
.kr-action-btn-item.cancel-btn:hover {
  background-color: #fffbeb;
  color: #d97706;
  border-color: rgba(217, 119, 6, 0.2);
}
.kr-action-btn-item.delete:hover {
  background-color: var(--danger-bg);
  color: var(--danger);
  border-color: rgba(239, 68, 68, 0.2);
}
.kr-action-btn-item.view-r-btn:hover {
  background-color: #f0fdf4;
  color: #16a34a;
  border-color: rgba(22, 163, 74, 0.2);
}
.kr-action-btn-item svg {
  width: 15px;
  height: 15px;
}

.empty-kr-row {
  text-align: center;
  padding: 24px;
  border: 1.5px dashed var(--border-medium);
  border-radius: var(--radius-md);
  font-size: 13px;
  color: var(--text-muted);
  font-style: italic;
}

.empty-kr-row💡 {
  font-style: normal;
}

/* ==========================================================================
   Modals & Forms Styles
   ========================================================================== */
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
  max-width: 480px;
  box-shadow: var(--shadow-lg);
  display: flex;
  flex-direction: column;
  gap: 20px;
  border: 1px solid var(--border-light);
}
.modal-header-with-close {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--border-light);
  padding-bottom: 12px;
  margin-bottom: 4px;
}
.modal-header-with-close h3 {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-main);
  border-bottom: none;
  padding: 0;
  margin: 0;
}
.modal-close-icon-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--text-muted);
  padding: 4px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}
.modal-close-icon-btn:hover {
  background-color: var(--border-light);
  color: var(--text-main);
}
.modal-close-icon-btn svg {
  width: 18px;
  height: 18px;
}
.form-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}
.form-field label {
  font-size: 13.5px;
  font-weight: 700;
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
  color: var(--text-main);
}
.form-control:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}
textarea.form-control {
  resize: vertical;
  min-height: 80px;
}
/* Custom Select & Date styling extensions */
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

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 12px;
}
.btn {
  padding: 10px 20px;
  border-radius: var(--radius-md);
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
  border: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.btn-secondary {
  background-color: #f1f5f9;
  color: #475569;
}
.btn-secondary:hover {
  background-color: #e2e8f0;
  color: #1e293b;
}
.btn-primary {
  background-color: #2563eb;
  color: #fff;
}
.btn-primary:hover {
  background-color: #1d4ed8;
}

/* Derive Modal Unique Style */
.derive-modal-content {
  max-width: 540px;
}
.derive-header {
  display: flex;
  align-items: center;
  gap: 8px;
  border-bottom: 1.5px solid rgba(37, 99, 235, 0.1);
  padding-bottom: 12px;
}
.derive-icon {
  font-size: 22px;
}
.derive-header h3 {
  border: none;
  padding: 0;
  margin: 0;
  color: #2563eb;
}
.derive-tip-desc {
  font-size: 13px;
  line-height: 1.5;
  color: var(--text-muted);
  background-color: #f8fafc;
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-light);
}
</style>
