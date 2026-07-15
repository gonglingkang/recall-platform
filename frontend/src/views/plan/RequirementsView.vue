<template>
  <div class="requirements-view">
    <!-- Header Area (PRD 6.3.1) -->
    <div class="requirements-header premium-card">
      <div class="title-section">
        <h1>需求管理</h1>
        <p class="subtitle">跟踪业务需求的生命周期，并与绩效关键成果（K）深度绑定</p>
      </div>

      <div style="display: flex; gap: 12px; align-items: center;">
        <button class="action-btn-primary add-requirement-btn" @click="openCreateModal">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
          </svg>
          <span>新建需求</span>
        </button>

        <button class="action-btn-secondary reset-filter-btn" @click="resetSearch">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.2" stroke="currentColor" style="width: 16px; height: 16px;">
            <path stroke-linecap="round" stroke-linejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99" />
          </svg>
          <span>重置搜索</span>
        </button>
      </div>
    </div>

    <!-- Filter & Search Bar -->
    <div class="filter-bar premium-card" style="display: flex; gap: 16px; align-items: center; padding: 16px 24px; justify-content: flex-start; position: relative; z-index: 100;">
      <!-- Search Input Wrapper (Matches SearchView.vue style) -->
      <div class="search-input-wrapper" style="position: relative; display: flex; align-items: center; width: 360px; max-width: 100%;">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="search-icon" style="width: 20px; height: 20px; color: var(--text-muted); position: absolute; left: 16px; pointer-events: none;">
          <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.604 10.604z" />
        </svg>
        <input 
          v-model="searchQuery" 
          type="text" 
          placeholder="搜索需求标题或描述内容..." 
          class="search-bar-input"
          autocomplete="off"
          style="width: 100%; height: 48px; padding: 0 48px; border-radius: var(--radius-lg); border: 1.5px solid var(--border-medium); outline: none; font-size: 15px; font-weight: 500; background-color: var(--bg-app); transition: all var(--transition-fast);"
        />
        <button v-if="searchQuery" class="clear-search-btn" @click="searchQuery = ''" style="position: absolute; right: 16px; background: none; border: none; color: var(--text-muted); font-size: 14px; cursor: pointer; padding: 4px;">✕</button>
      </div>

      <div class="filter-actions" style="display: flex; align-items: center; gap: 12px;">
        <!-- Main Category Filter -->
        <div class="custom-select-container" style="position: relative; min-width: 120px;" @click.stop>
          <div 
            class="beautiful-select-trigger"
            :class="{ 'is-active': isFilterCategoryDropdownOpen }"
            @click="isFilterCategoryDropdownOpen = !isFilterCategoryDropdownOpen; isFilterSubCategoryDropdownOpen = false; isStatusDropdownOpen = false; isDateDropdownOpen = false;"
            style="display: flex; align-items: center; justify-content: space-between; cursor: pointer; padding: 0 16px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background: #fff; transition: all var(--transition-fast); height: 48px;"
          >
            <div style="display: flex; align-items: center; gap: 8px;">
              <span style="font-size: 13.5px; font-weight: 500; color: var(--text-main);">{{ filterCategoryLabel }}</span>
            </div>
            <svg 
              xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.2" stroke="currentColor" 
              style="width: 14px; height: 14px; transition: transform 0.2s;"
              :style="{ transform: isFilterCategoryDropdownOpen ? 'rotate(180deg)' : 'rotate(0deg)' }"
            >
              <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
            </svg>
          </div>
          
          <div 
            v-if="isFilterCategoryDropdownOpen" 
            class="custom-dropdown-list"
            style="position: absolute; top: calc(100% + 6px); left: 0; background: rgba(255, 255, 255, 0.98); border: 1px solid rgba(0, 0, 0, 0.08); border-radius: var(--radius-md); box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.05); z-index: 999; padding: 6px; backdrop-filter: blur(12px); display: flex; flex-direction: column; gap: 4px; max-height: 200px; overflow-y: auto; min-width: 180px;"
          >
            <div 
              @click="filterCategoryId = null; filterSubCategoryId = null; isFilterCategoryDropdownOpen = false;"
              class="custom-dropdown-item"
              :class="{ 'is-selected': filterCategoryId === null }"
              style="display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
            >
              <span style="font-size: 13.5px; font-weight: 500; color: var(--text-main);">全部主分类</span>
            </div>
            <div 
              v-for="cat in reqCategoriesList" 
              :key="cat.id"
              @click="filterCategoryId = cat.id; filterSubCategoryId = null; isFilterCategoryDropdownOpen = false;"
              class="custom-dropdown-item"
              :class="{ 'is-selected': filterCategoryId === cat.id }"
              style="display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
            >
              <span style="font-size: 13.5px; font-weight: 500; color: var(--text-main);">{{ cat.name }}</span>
            </div>
          </div>
        </div>

        <!-- Subcategory Filter -->
        <div class="custom-select-container" style="position: relative; min-width: 120px;" @click.stop>
          <div 
            class="beautiful-select-trigger"
            :class="{ 
              'is-active': isFilterSubCategoryDropdownOpen,
              'is-disabled': !filterCategoryId 
            }"
            @click="filterCategoryId && (isFilterSubCategoryDropdownOpen = !isFilterSubCategoryDropdownOpen); isFilterCategoryDropdownOpen = false; isStatusDropdownOpen = false; isDateDropdownOpen = false;"
            style="display: flex; align-items: center; justify-content: space-between; cursor: pointer; padding: 0 16px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background: #fff; transition: all var(--transition-fast); height: 48px;"
            :style="{ 
              backgroundColor: filterCategoryId ? '#fff' : 'var(--bg-app)', 
              cursor: filterCategoryId ? 'pointer' : 'not-allowed',
              opacity: filterCategoryId ? 1 : 0.65 
            }"
          >
            <div style="display: flex; align-items: center; gap: 8px;">
              <span style="font-size: 13.5px; font-weight: 500; color: var(--text-main);">{{ filterSubCategoryLabel }}</span>
            </div>
            <svg 
              xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.2" stroke="currentColor" 
              style="width: 14px; height: 14px; transition: transform 0.2s;"
              :style="{ transform: isFilterSubCategoryDropdownOpen ? 'rotate(180deg)' : 'rotate(0deg)' }"
            >
              <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
            </svg>
          </div>
          
          <div 
            v-if="isFilterSubCategoryDropdownOpen" 
            class="custom-dropdown-list"
            style="position: absolute; top: calc(100% + 6px); left: 0; background: rgba(255, 255, 255, 0.98); border: 1px solid rgba(0, 0, 0, 0.08); border-radius: var(--radius-md); box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.05); z-index: 999; padding: 6px; backdrop-filter: blur(12px); display: flex; flex-direction: column; gap: 4px; max-height: 200px; overflow-y: auto; min-width: 180px;"
          >
            <div 
              @click="filterSubCategoryId = null; isFilterSubCategoryDropdownOpen = false;"
              class="custom-dropdown-item"
              :class="{ 'is-selected': filterSubCategoryId === null }"
              style="display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
            >
              <span style="font-size: 13.5px; font-weight: 500; color: var(--text-main);">全部子分类</span>
            </div>
            <div 
              v-for="sub in getSubcategoriesForCategory(filterCategoryId)" 
              :key="sub.id"
              @click="filterSubCategoryId = sub.id; isFilterSubCategoryDropdownOpen = false;"
              class="custom-dropdown-item"
              :class="{ 'is-selected': filterSubCategoryId === sub.id }"
              style="display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
            >
              <span style="font-size: 13.5px; font-weight: 500; color: var(--text-main);">{{ sub.name }}</span>
            </div>
          </div>
        </div>

        <!-- Status Filter (Multiple selection) -->
        <div class="custom-select-container" style="position: relative; min-width: 120px;" @click.stop>
          <div 
            class="beautiful-select-trigger"
            :class="{ 'is-active': isStatusDropdownOpen }"
            @click="isStatusDropdownOpen = !isStatusDropdownOpen; isDateDropdownOpen = false; isFilterCategoryDropdownOpen = false; isFilterSubCategoryDropdownOpen = false;"
            style="display: flex; align-items: center; justify-content: space-between; cursor: pointer; padding: 0 16px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background: #fff; transition: all var(--transition-fast); height: 48px;"
          >
            <div style="display: flex; align-items: center; gap: 8px;">
              <span style="font-size: 13.5px; font-weight: 500; color: var(--text-main);">{{ currentStatusLabel }}</span>
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
            style="position: absolute; top: calc(100% + 6px); left: 0; background: rgba(255, 255, 255, 0.98); border: 1px solid rgba(0, 0, 0, 0.08); border-radius: var(--radius-md); box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.05); z-index: 999; padding: 6px; backdrop-filter: blur(12px); display: flex; flex-direction: column; gap: 4px; min-width: 160px;"
          >
            <div 
              v-for="opt in statusFilterOptions" 
              :key="opt.value"
              @click="toggleStatusFilter(opt.value)"
              class="custom-dropdown-item"
              :class="{ 'is-selected': opt.value === 'all' ? statusFilters.length === 0 : statusFilters.includes(opt.value) }"
              style="display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
            >
              <div style="display: flex; align-items: center; gap: 8px;">
                <span style="display: inline-block; width: 6px; height: 6px; border-radius: 50%;" :style="{ backgroundColor: opt.color }"></span>
                <span style="font-size: 13.5px; font-weight: 500; color: var(--text-main);">{{ opt.label }}</span>
              </div>
              <svg 
                v-if="opt.value === 'all' ? statusFilters.length === 0 : statusFilters.includes(opt.value)"
                xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="#2563eb" style="width: 14px; height: 14px;"
              >
                <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
              </svg>
            </div>
          </div>
        </div>

        <!-- Date Range Filter (Matches CategoryView.vue style but height 48px) -->
        <div class="custom-select-container" style="position: relative; min-width: 280px;" @click.stop>
          <div 
            class="beautiful-select-trigger"
            :class="{ 'is-active': isDateDropdownOpen }"
            @click="toggleDateDropdown"
            style="display: flex; align-items: center; justify-content: space-between; cursor: pointer; padding: 0 16px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background: #fff; transition: all var(--transition-fast); height: 48px;"
          >
            <div style="display: flex; align-items: center; gap: 8px;">
              <span style="font-size: 14px; line-height: 1;">📅</span>
              <span style="font-size: 13.5px; font-weight: 500; color: var(--text-main); max-width: 220px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
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
                style="width: 100%; padding: 8px 12px; border: 1.5px solid var(--border-medium); border-radius: 6px; outline: none; font-size: 13.5px;"
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
                style="width: 100%; padding: 8px 12px; border: 1.5px solid var(--border-medium); border-radius: 6px; outline: none; font-size: 13.5px;"
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
      </div>
    </div>

    <!-- Requirements Card List -->
    <div class="requirements-container" style="position: relative; min-height: 200px;">
      <!-- Loading Skeleton -->
      <div v-if="isLoading" class="loading-state" style="display: flex; flex-direction: column; gap: 16px;">
        <div v-for="i in 3" :key="i" class="skeleton-card premium-card" style="padding: 24px; border-radius: var(--radius-lg); background: #fff; border: 1.5px solid var(--border-medium); display: flex; flex-direction: column; gap: 12px; position: relative; overflow: hidden;">
          <div class="shimmer-effect" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: linear-gradient(90deg, rgba(255,255,255,0) 0%, rgba(255,255,255,0.6) 50%, rgba(255,255,255,0) 100%); animation: shimmer 1.5s infinite;"></div>
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <div style="width: 40%; height: 20px; background: #e2e8f0; border-radius: 4px;"></div>
            <div style="width: 80px; height: 24px; background: #e2e8f0; border-radius: 12px;"></div>
          </div>
          <div style="width: 80%; height: 16px; background: #f1f5f9; border-radius: 4px; margin-top: 4px;"></div>
          <div style="display: flex; gap: 16px; margin-top: 8px;">
            <div style="width: 150px; height: 14px; background: #f1f5f9; border-radius: 4px;"></div>
            <div style="width: 120px; height: 14px; background: #f1f5f9; border-radius: 4px;"></div>
          </div>
        </div>
      </div>

      <!-- Actual Data View -->
      <template v-else>
        <div v-if="filteredRequirements.length === 0" class="empty-state premium-card">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="empty-icon">
            <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 13.5h3.86a2.25 2.25 0 012.008 1.24l.885 1.77a2.25 2.25 0 002.007 1.24h1.98a2.25 2.25 0 002.007-1.24l.885-1.77a2.25 2.25 0 012.007-1.24h3.86m-18 0h18a2.25 2.25 0 002.25-2.25V5.25A2.25 2.25 0 0019.5 3h-15A2.25 2.25 0 002.25 5.25v6a2.25 2.25 0 002.25 2.25z" />
          </svg>
          <h3>暂无匹配的需求</h3>
          <p>可以尝试调整筛选条件或点击右上角“新建需求”。</p>
        </div>

        <div 
          v-for="req in filteredRequirements" 
          :key="req.id" 
          class="requirement-card premium-card"
          :class="{ 
            'req-not-involved': req.status === '1',
            'req-released': req.status === '5'
          }"
          :style="{ borderLeftColor: getStatusColor(req.status) }"
        >
        <!-- Card Body -->
        <div class="card-layout">
          <div class="card-main">
            <!-- Title & Status Badge -->
            <div class="title-row">
              <h2 class="requirement-title">{{ req.categoryName ? req.categoryName + (req.subCategoryName ? '-' + req.subCategoryName : '') + '-' : '' }}{{ req.title }}</h2>
              <span class="status-badge" :style="getStatusStyle(req.status)">
                {{ getStatusLabel(req.status) }}
              </span>
            </div>

            <!-- Description -->
            <p v-if="req.description" class="requirement-desc">{{ req.description }}</p>
            
            <!-- Info / Dates row -->
            <div class="info-row">
              <span v-if="req.firstDemandDate" class="info-item">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5A2.25 2.25 0 0121 11.25v7.5" />
                </svg>
                首次需求时间: {{ req.firstDemandDate }}
              </span>

              <!-- Dynamic Dates based on status (non-exclusive display) -->
              <span v-if="req.status === '1' && req.cancelReason" class="info-item cancel-reason">
                <strong>不涉及原因:</strong> {{ req.cancelReason }}
              </span>
              <span v-if="req.devCompleteDate" class="info-item">
                🎉 开发完成: {{ req.devCompleteDate }}
              </span>
              <span v-if="req.acceptanceDate" class="info-item">
                📋 验收完成: {{ req.acceptanceDate }} <span v-if="req.acceptancePerson" style="margin-left: 4px;">(验收人: {{ req.acceptancePerson }})</span>
              </span>
              <span v-if="req.releaseDate" class="info-item">
                🚀 发布完成: {{ req.releaseDate }}
              </span>
              <span v-if="['0', '2'].includes(req.status) && req.keyResult && req.keyResult.planCompleteDate" class="info-item">
                📅 预计完成: {{ req.keyResult.planCompleteDate }} (由 K 驱动)
              </span>
            </div>

            <!-- KR Binding Section -->
            <div class="kr-binding-section">
              <div v-if="req.keyResult" class="kr-pill-bound">
                <span class="kr-icon">🎯</span>
                <span class="kr-text" :title="`关键成果: ${req.keyResult.name}`">
                  已关联 K: <strong>{{ req.keyResult.name }}</strong>
                </span>
                <span class="kr-status" :class="'kr-status-' + req.keyResult.status">
                  ({{ getKRStatusLabel(req.keyResult.status) }})
                </span>
              </div>
            </div>
          </div>

          <!-- Card Actions (Top Right) -->
          <div class="card-actions">
            <!-- Add Link Action -->
            <button 
              class="card-action-btn btn-add-link" 
              @click="openAddLinkModal(req)"
              v-if="req.status !== '5'"
            >
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" d="M13.19 8.688a4.5 4.5 0 011.242 7.244l-4.5 4.5a4.5 4.5 0 01-6.364-6.364l1.757-1.757m13.35-.622l1.757-1.757a4.5 4.5 0 00-6.364-6.364l-4.5 4.5a4.5 4.5 0 001.242 7.244" />
              </svg>
              <span>添加文档链接</span>
            </button>

            <!-- Status Transition Action -->
            <div class="status-transition-container" v-if="req.status !== '5'">
              <button 
                class="card-action-btn btn-status-change"
                :class="{ 'btn-disabled': isStatusLockedByKR(req) }"
                @click.stop="toggleStatusMenu(req)"
                :title="isStatusLockedByKR(req) ? '状态当前由关联的 K 驱动锁定，要手动更改必须先解绑 K 或在绩效处更改 K' : '手动变更需求状态'"
              >
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99" />
                </svg>
                <span>变更状态</span>
              </button>

              <!-- Dropdown Panel for transitions -->
              <transition name="popover-fade">
                <div v-if="activeStatusMenuId === req.id" class="status-dropdown premium-card" v-click-outside="closeStatusMenu">
                  <div class="dropdown-header">状态转换</div>
                  <div class="dropdown-list">
                    <button 
                      v-for="state in getLegalTransitions(req)" 
                      :key="state.value"
                      class="dropdown-item"
                      @click="triggerStatusTransition(req, state.value)"
                    >
                      <span class="dot" :style="{ backgroundColor: getStatusColor(state.value) }"></span>
                      <span class="lbl">{{ state.label }}</span>
                      <span v-if="req.keyResult && state.value === '1'" class="warn-badge">解绑 K</span>
                    </button>
                    <div v-if="getLegalTransitions(req).length === 0" class="dropdown-empty">
                      当前不可手动流转
                    </div>
                  </div>
                </div>
              </transition>
            </div>

            <!-- Edit Button -->
            <button 
              class="card-action-btn btn-edit" 
              @click="openEditModal(req)"
              v-if="req.status !== '5'"
            >
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10" />
              </svg>
              <span>编辑</span>
            </button>

            <!-- Delete Button -->
            <button class="card-action-btn btn-delete" @click="confirmDelete(req)">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" />
              </svg>
              <span>删除</span>
            </button>
          </div>
        </div>

        <!-- Document Links Section (关联链接) -->
        <div v-if="req.documents && req.documents.length > 0" class="links-section">
          <!-- Group 1: 产品与设计文档 (prototype / requirement) -->
          <div v-if="req.documents.some(d => d.type === '1' || d.type === '2')" class="links-group" style="margin-bottom: 16px;">
            <div class="links-title" style="display: flex; align-items: center; gap: 6px; font-size: 12.5px; color: var(--text-muted); font-weight: 600; margin-bottom: 8px;">
              <span>📋 产品设计与规格说明</span>
              <span style="font-size: 11px; padding: 2px 8px; border-radius: 12px; background: rgba(37, 99, 235, 0.08); color: var(--primary); font-weight: bold;">
                {{ req.documents.filter(d => d.type === '1' || d.type === '2').length }}
              </span>
            </div>
            <div class="links-grid">
              <div 
                v-for="doc in [...req.documents].filter(d => d.type === '1' || d.type === '2').sort((a, b) => a.type === b.type ? 0 : (a.type === '1' ? -1 : 1))" 
                :key="doc.id" 
                class="link-chip"
                @click="openLink(doc.url)"
              >
                <!-- Icon mapped dynamically -->
                <span class="link-icon-wrapper" :style="getLinkIconBg(doc)">
                  <component :is="getLinkIcon(doc)" class="link-svg-icon" />
                </span>
                <div class="link-content">
                  <div class="link-title-text">{{ doc.title }}</div>
                  <div class="link-url-text">{{ doc.url }}</div>
                  <div class="link-date-text" style="font-size: 11px; color: var(--text-muted); margin-top: 4px; display: flex; align-items: center; gap: 4px;">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" style="width: 12px; height: 12px;">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5A2.25 2.25 0 0121 11.25v7.5" />
                    </svg>
                    <span>文档时间: {{ doc.documentDate || formatDate(doc.createdAt || '') }}</span>
                  </div>
                </div>
                <button 
                  class="remove-link-btn" 
                  @click.stop="confirmDeleteDocument(req, doc)"
                  title="删除此链接"
                  v-if="req.status !== '5'"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
            </div>
          </div>

          <!-- Group 2: 会议纪要 (meeting) -->
          <div v-if="req.documents.some(d => d.type === '3')" class="links-group">
            <div class="links-title" style="display: flex; align-items: center; gap: 6px; font-size: 12.5px; color: var(--text-muted); font-weight: 600; margin-bottom: 8px;">
              <span>📅 会议沟通纪要</span>
              <span style="font-size: 11px; padding: 2px 8px; border-radius: 12px; background: rgba(220, 38, 38, 0.08); color: #dc2626; font-weight: bold;">
                {{ req.documents.filter(d => d.type === '3').length }}
              </span>
            </div>
            <div class="links-grid">
              <div 
                v-for="doc in [...req.documents].filter(d => d.type === '3').sort((a, b) => new Date(b.documentDate || b.createdAt || 0).getTime() - new Date(a.documentDate || a.createdAt || 0).getTime())" 
                :key="doc.id" 
                class="link-chip"
                @click="openLink(doc.url)"
              >
                <!-- Icon mapped dynamically -->
                <span class="link-icon-wrapper" :style="getLinkIconBg(doc)">
                  <component :is="getLinkIcon(doc)" class="link-svg-icon" />
                </span>
                <div class="link-content">
                  <div class="link-title-text">{{ doc.title }}</div>
                  <div class="link-url-text">{{ doc.url }}</div>
                  <div class="link-date-text" style="font-size: 11px; color: var(--text-muted); margin-top: 4px; display: flex; align-items: center; gap: 4px;">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" style="width: 12px; height: 12px;">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5A2.25 2.25 0 0121 11.25v7.5" />
                    </svg>
                    <span>文档时间: {{ doc.documentDate || formatDate(doc.createdAt || '') }}</span>
                  </div>
                </div>
                <button 
                  class="remove-link-btn" 
                  @click.stop="confirmDeleteDocument(req, doc)"
                  title="删除此链接"
                  v-if="req.status !== '5'"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
    </div>

    <!-- Pagination Footer -->
    <div v-if="totalPages > 1" class="pagination-footer premium-card" style="display: flex; justify-content: center; align-items: center; gap: 16px; padding: 12px 24px; margin-top: 20px; background: #fff; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md);">
      <button 
        class="action-btn-secondary" 
        :disabled="currentPage === 1" 
        @click="currentPage--"
        style="padding: 6px 12px; font-size: 13px;"
        :style="{ cursor: currentPage === 1 ? 'not-allowed' : 'pointer', opacity: currentPage === 1 ? 0.5 : 1 }"
      >
        上一页
      </button>
      <span style="font-size: 13.5px; font-weight: 600; color: var(--text-main);">
        第 {{ currentPage }} / {{ totalPages }} 页 (共 {{ totalRecords }} 条)
      </span>
      <button 
        class="action-btn-secondary" 
        :disabled="currentPage === totalPages" 
        @click="currentPage++"
        style="padding: 6px 12px; font-size: 13px;"
        :style="{ cursor: currentPage === totalPages ? 'not-allowed' : 'pointer', opacity: currentPage === totalPages ? 0.5 : 1 }"
      >
        下一页
      </button>
    </div>

    <!-- MODAL 1: Create Requirement -->
    <transition name="modal-fade">
      <div 
        v-if="isCreateModalOpen" 
        class="modal-overlay" 
        @mousedown.self="onMousedownOverlay"
        @mouseup.self="onMouseupOverlay('create')"
      >
        <div class="modal-card">
          <div class="modal-header">
            <h2>新建需求</h2>
            <button class="close-modal-btn" @click="closeCreateModal">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <form @submit.prevent="submitCreate" class="modal-form">
            <div class="form-group">
              <label for="create-title">需求标题<span class="required-star">*</span></label>
              <input 
                id="create-title" 
                v-model="createForm.title" 
                type="text" 
                placeholder="请输入需求标题 (最长200字符)" 
                required
                class="form-input"
                autocomplete="off"
              />
            </div>
            <div class="form-group">
              <label for="create-desc">需求描述</label>
              <textarea 
                id="create-desc" 
                v-model="createForm.description" 
                placeholder="请输入需求详细背景或描述..." 
                rows="2"
                class="form-textarea"
              ></textarea>
            </div>
            <div class="form-row" style="display: flex; gap: 16px;">
              <div class="form-group" style="flex: 1;">
                <label>需求主分类<span class="required-star">*</span></label>
                <div class="custom-select-container" style="position: relative;" @click.stop>
                  <div 
                    class="beautiful-select-trigger"
                    :class="{ 'is-active': isCreateCategoryDropdownOpen }"
                    @click="isCreateCategoryDropdownOpen = !isCreateCategoryDropdownOpen; isCreateSubCategoryDropdownOpen = false;"
                    style="display: flex; align-items: center; justify-content: space-between; cursor: pointer; padding: 0 16px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background: #fff; transition: all var(--transition-fast); height: 42px;"
                  >
                    <span style="font-size: 13.5px; font-weight: 500; color: createForm.categoryId ? 'var(--text-main)' : 'var(--text-muted)';">
                      {{ getCategoryNameById(createForm.categoryId) }}
                    </span>
                    <svg 
                      xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.2" stroke="currentColor" 
                      style="width: 14px; height: 14px; transition: transform 0.2s;"
                      :style="{ transform: isCreateCategoryDropdownOpen ? 'rotate(180deg)' : 'rotate(0deg)' }"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
                    </svg>
                  </div>
                  
                  <div 
                    v-if="isCreateCategoryDropdownOpen" 
                    class="custom-dropdown-list"
                    style="position: absolute; top: calc(100% + 6px); left: 0; right: 0; background: rgba(255, 255, 255, 0.98); border: 1px solid rgba(0, 0, 0, 0.08); border-radius: var(--radius-md); box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.05); z-index: 999; padding: 6px; backdrop-filter: blur(12px); display: flex; flex-direction: column; gap: 4px; max-height: 200px; overflow-y: auto;"
                  >
                    <div 
                      v-for="cat in reqCategoriesList" 
                      :key="cat.id"
                      @click="createForm.categoryId = cat.id; createForm.subCategoryId = null; isCreateCategoryDropdownOpen = false;"
                      class="custom-dropdown-item"
                      :class="{ 'is-selected': createForm.categoryId === cat.id }"
                      style="display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
                    >
                      <span style="font-size: 13.5px; font-weight: 500; color: var(--text-main);">{{ cat.name }}</span>
                    </div>
                  </div>
                </div>
              </div>
              <div class="form-group" style="flex: 1;">
                <label>需求子分类</label>
                <div class="custom-select-container" style="position: relative;" @click.stop>
                  <div 
                    class="beautiful-select-trigger"
                    :class="{ 
                      'is-active': isCreateSubCategoryDropdownOpen,
                      'is-disabled': !createForm.categoryId
                    }"
                    @click="createForm.categoryId && (isCreateSubCategoryDropdownOpen = !isCreateSubCategoryDropdownOpen); isCreateCategoryDropdownOpen = false;"
                    style="display: flex; align-items: center; justify-content: space-between; cursor: pointer; padding: 0 16px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background: #fff; transition: all var(--transition-fast); height: 42px;"
                    :style="{ 
                      backgroundColor: createForm.categoryId ? '#fff' : 'var(--bg-app)', 
                      cursor: createForm.categoryId ? 'pointer' : 'not-allowed',
                      opacity: createForm.categoryId ? 1 : 0.65 
                    }"
                  >
                    <span style="font-size: 13.5px; font-weight: 500; color: createForm.subCategoryId ? 'var(--text-main)' : 'var(--text-muted)';">
                      {{ getSubCategoryNameById(createForm.categoryId, createForm.subCategoryId) }}
                    </span>
                    <svg 
                      xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.2" stroke="currentColor" 
                      style="width: 14px; height: 14px; transition: transform 0.2s;"
                      :style="{ transform: isCreateSubCategoryDropdownOpen ? 'rotate(180deg)' : 'rotate(0deg)' }"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
                    </svg>
                  </div>
                  
                  <div 
                    v-if="isCreateSubCategoryDropdownOpen" 
                    class="custom-dropdown-list"
                    style="position: absolute; top: calc(100% + 6px); left: 0; right: 0; background: rgba(255, 255, 255, 0.98); border: 1px solid rgba(0, 0, 0, 0.08); border-radius: var(--radius-md); box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.05); z-index: 999; padding: 6px; backdrop-filter: blur(12px); display: flex; flex-direction: column; gap: 4px; max-height: 200px; overflow-y: auto;"
                  >
                    <div 
                      @click="createForm.subCategoryId = null; isCreateSubCategoryDropdownOpen = false;"
                      class="custom-dropdown-item"
                      :class="{ 'is-selected': createForm.subCategoryId === null }"
                      style="display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
                    >
                      <span style="font-size: 13.5px; font-weight: 500; color: var(--text-main);">不选择子分类</span>
                    </div>
                    <div 
                      v-for="sub in getSubcategoriesForCategory(createForm.categoryId)" 
                      :key="sub.id"
                      @click="createForm.subCategoryId = sub.id; isCreateSubCategoryDropdownOpen = false;"
                      class="custom-dropdown-item"
                      :class="{ 'is-selected': createForm.subCategoryId === sub.id }"
                      style="display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
                    >
                      <span style="font-size: 13.5px; font-weight: 500; color: var(--text-main);">{{ sub.name }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="form-group">
              <label for="create-first-demand-date">首次需求时间<span class="required-star">*</span></label>
              <input 
                id="create-first-demand-date" 
                v-model="createForm.firstDemandDate" 
                type="date" 
                required
                :max="todayDateStr"
                class="form-input"
                @mousedown.prevent="($event.target as HTMLInputElement).showPicker?.()"
              />
            </div>
            <div class="form-group" style="margin-top: 4px;">
              <label>关联关键成果 (K) <span style="font-size: 12px; font-weight: normal; color: var(--text-muted);">(可选)</span></label>
              <div class="kr-selection-display" style="display: flex; align-items: center; justify-content: space-between; padding: 12px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background-color: var(--bg-app); margin-top: 6px;">
                <div style="display: flex; align-items: center; gap: 8px;">
                  <span style="font-size: 16px;">🎯</span>
                  <span v-if="createForm.keyResultId" style="font-size: 13.5px; font-weight: 600; color: var(--text-main);">
                    {{ getKRNameById(createForm.keyResultId) }}
                  </span>
                  <span v-else style="font-size: 13.5px; color: var(--text-muted);">
                    当前未绑定关键成果
                  </span>
                </div>
                <div style="display: flex; gap: 8px; align-items: center;">
                  <button type="button" @click="openSelectKRSubModal('create')" class="action-btn-secondary" style="display: inline-flex; align-items: center; justify-content: center; padding: 0 16px; font-size: 12px; min-width: 68px; height: 32px; cursor: pointer; text-align: center;">
                    {{ createForm.keyResultId ? '修改' : '绑定' }}
                  </button>
                  <button v-if="createForm.keyResultId" type="button" @click="createForm.keyResultId = null" class="action-btn-secondary" style="display: inline-flex; align-items: center; justify-content: center; padding: 0 16px; font-size: 12px; min-width: 68px; height: 32px; color: #ef4444; border-color: #fecaca; background-color: #fef2f2; cursor: pointer; text-align: center;">
                    解绑
                  </button>
                </div>
              </div>
            </div>
            <div class="modal-actions">
              <button type="button" class="action-btn-secondary" @click="closeCreateModal">取消</button>
              <button type="submit" class="action-btn-primary">确定创建</button>
            </div>
          </form>
        </div>
      </div>
    </transition>

    <!-- MODAL 2: Edit Requirement -->
    <transition name="modal-fade">
      <div 
        v-if="isEditModalOpen" 
        class="modal-overlay" 
        @mousedown.self="onMousedownOverlay"
        @mouseup.self="onMouseupOverlay('edit')"
      >
        <div class="modal-card">
          <div class="modal-header">
            <h2>编辑需求</h2>
            <button class="close-modal-btn" @click="closeEditModal">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <form @submit.prevent="submitEdit" class="modal-form">
            <div class="form-group">
              <label for="edit-title">需求标题<span class="required-star">*</span></label>
              <input 
                id="edit-title" 
                v-model="editForm.title" 
                type="text" 
                required
                class="form-input"
                autocomplete="off"
              />
            </div>
            <div class="form-group">
              <label for="edit-desc">需求描述</label>
              <textarea 
                id="edit-desc" 
                v-model="editForm.description" 
                rows="2"
                class="form-textarea"
              ></textarea>
            </div>
            <div class="form-row" style="display: flex; gap: 16px;">
              <div class="form-group" style="flex: 1;">
                <label>需求主分类<span class="required-star">*</span></label>
                <div class="custom-select-container" style="position: relative;" @click.stop>
                  <div 
                    class="beautiful-select-trigger"
                    :class="{ 'is-active': isEditCategoryDropdownOpen }"
                    @click="isEditCategoryDropdownOpen = !isEditCategoryDropdownOpen; isEditSubCategoryDropdownOpen = false;"
                    style="display: flex; align-items: center; justify-content: space-between; cursor: pointer; padding: 0 16px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background: #fff; transition: all var(--transition-fast); height: 42px;"
                  >
                    <span style="font-size: 13.5px; font-weight: 500; color: editForm.categoryId ? 'var(--text-main)' : 'var(--text-muted)';">
                      {{ getCategoryNameById(editForm.categoryId) }}
                    </span>
                    <svg 
                      xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.2" stroke="currentColor" 
                      style="width: 14px; height: 14px; transition: transform 0.2s;"
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
                    <div 
                      v-for="cat in reqCategoriesList" 
                      :key="cat.id"
                      @click="editForm.categoryId = cat.id; editForm.subCategoryId = null; isEditCategoryDropdownOpen = false;"
                      class="custom-dropdown-item"
                      :class="{ 'is-selected': editForm.categoryId === cat.id }"
                      style="display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
                    >
                      <span style="font-size: 13.5px; font-weight: 500; color: var(--text-main);">{{ cat.name }}</span>
                    </div>
                  </div>
                </div>
              </div>
              <div class="form-group" style="flex: 1;">
                <label>需求子分类</label>
                <div class="custom-select-container" style="position: relative;" @click.stop>
                  <div 
                    class="beautiful-select-trigger"
                    :class="{ 
                      'is-active': isEditSubCategoryDropdownOpen,
                      'is-disabled': !editForm.categoryId
                    }"
                    @click="editForm.categoryId && (isEditSubCategoryDropdownOpen = !isEditSubCategoryDropdownOpen); isEditCategoryDropdownOpen = false;"
                    style="display: flex; align-items: center; justify-content: space-between; cursor: pointer; padding: 0 16px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background: #fff; transition: all var(--transition-fast); height: 42px;"
                    :style="{ 
                      backgroundColor: editForm.categoryId ? '#fff' : 'var(--bg-app)', 
                      cursor: editForm.categoryId ? 'pointer' : 'not-allowed',
                      opacity: editForm.categoryId ? 1 : 0.65 
                    }"
                  >
                    <span style="font-size: 13.5px; font-weight: 500; color: editForm.subCategoryId ? 'var(--text-main)' : 'var(--text-muted)';">
                      {{ getSubCategoryNameById(editForm.categoryId, editForm.subCategoryId) }}
                    </span>
                    <svg 
                      xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.2" stroke="currentColor" 
                      style="width: 14px; height: 14px; transition: transform 0.2s;"
                      :style="{ transform: isEditSubCategoryDropdownOpen ? 'rotate(180deg)' : 'rotate(0deg)' }"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
                    </svg>
                  </div>
                  
                  <div 
                    v-if="isEditSubCategoryDropdownOpen" 
                    class="custom-dropdown-list"
                    style="position: absolute; top: calc(100% + 6px); left: 0; right: 0; background: rgba(255, 255, 255, 0.98); border: 1px solid rgba(0, 0, 0, 0.08); border-radius: var(--radius-md); box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.05); z-index: 999; padding: 6px; backdrop-filter: blur(12px); display: flex; flex-direction: column; gap: 4px; max-height: 200px; overflow-y: auto;"
                  >
                    <div 
                      @click="editForm.subCategoryId = null; isEditSubCategoryDropdownOpen = false;"
                      class="custom-dropdown-item"
                      :class="{ 'is-selected': editForm.subCategoryId === null }"
                      style="display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
                    >
                      <span style="font-size: 13.5px; font-weight: 500; color: var(--text-main);">不选择子分类</span>
                    </div>
                    <div 
                      v-for="sub in getSubcategoriesForCategory(editForm.categoryId)" 
                      :key="sub.id"
                      @click="editForm.subCategoryId = sub.id; isEditSubCategoryDropdownOpen = false;"
                      class="custom-dropdown-item"
                      :class="{ 'is-selected': editForm.subCategoryId === sub.id }"
                      style="display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
                    >
                      <span style="font-size: 13.5px; font-weight: 500; color: var(--text-main);">{{ sub.name }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="form-group">
              <label for="edit-first-demand-date">首次需求时间<span class="required-star">*</span></label>
              <input 
                id="edit-first-demand-date" 
                v-model="editForm.firstDemandDate" 
                type="date" 
                required
                :max="todayDateStr"
                class="form-input"
                @mousedown.prevent="($event.target as HTMLInputElement).showPicker?.()"
              />
            </div>
            <div class="form-group" style="margin-top: 4px;">
              <label>关联关键成果 (K) <span style="font-size: 12px; font-weight: normal; color: var(--text-muted);">(可选)</span></label>
              <div class="kr-selection-display" style="display: flex; align-items: center; justify-content: space-between; padding: 12px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background-color: var(--bg-app); margin-top: 6px;">
                <div style="display: flex; align-items: center; gap: 8px;">
                  <span style="font-size: 16px;">🎯</span>
                  <span v-if="editForm.keyResultId" style="font-size: 13.5px; font-weight: 600; color: var(--text-main);">
                    {{ getKRNameById(editForm.keyResultId) }}
                  </span>
                  <span v-else style="font-size: 13.5px; color: var(--text-muted);">
                    当前未绑定关键成果
                  </span>
                </div>
                <div style="display: flex; gap: 8px; align-items: center;">
                  <template v-if="!isKRReadOnlyInEdit">
                    <button type="button" @click="openSelectKRSubModal('edit')" class="action-btn-secondary" style="display: inline-flex; align-items: center; justify-content: center; padding: 0 16px; font-size: 12px; min-width: 68px; height: 32px; cursor: pointer; text-align: center;">
                      {{ editForm.keyResultId ? '修改' : '绑定' }}
                    </button>
                    <button v-if="editForm.keyResultId" type="button" @click="editForm.keyResultId = null" class="action-btn-secondary" style="display: inline-flex; align-items: center; justify-content: center; padding: 0 16px; font-size: 12px; min-width: 68px; height: 32px; color: #ef4444; border-color: #fecaca; background-color: #fef2f2; cursor: pointer; text-align: center;">
                      解绑
                    </button>
                  </template>
                  <span v-else style="font-size: 12.5px; color: var(--text-muted); font-weight: 500; background-color: var(--bg-app); padding: 4px 8px; border-radius: var(--radius-sm); border: 1px solid var(--border-medium);">
                    状态已锁定，不可修改 K
                  </span>
                </div>
              </div>
            </div>
            <div class="modal-actions">
              <button type="button" class="action-btn-secondary" @click="closeEditModal">取消</button>
              <button type="submit" class="action-btn-primary">保存修改</button>
            </div>
          </form>
        </div>
      </div>
    </transition>

    <!-- MODAL 3: Add Link -->
    <transition name="modal-fade">
      <div 
        v-if="isLinkModalOpen" 
        class="modal-overlay" 
        @mousedown.self="onMousedownOverlay"
        @mouseup.self="onMouseupOverlay('link')"
      >
        <div class="modal-card">
          <div class="modal-header">
            <h2>添加文档链接</h2>
            <button class="close-modal-btn" @click="closeLinkModal">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <form @submit.prevent="submitAddLink" class="modal-form" autocomplete="off">
            <div class="form-group">
              <label for="link-title">文档标题<span class="required-star">*</span></label>
              <input 
                id="link-title" 
                v-model="linkForm.title" 
                type="text" 
                placeholder="例如: 原型设计稿, 需求文档" 
                required
                class="form-input"
                autocomplete="off"
              />
            </div>
            <div class="form-group">
              <label for="link-url">文档链接<span class="required-star">*</span></label>
              <input 
                id="link-url" 
                v-model="linkForm.url" 
                type="text" 
                placeholder="https://..." 
                required
                class="form-input"
                autocomplete="off"
              />
            </div>
            <div class="form-group">
              <label for="link-date">文档时间<span class="required-star">*</span></label>
              <input 
                id="link-date" 
                v-model="linkForm.date" 
                type="date" 
                required
                :max="todayDateStr"
                class="form-input"
                @mousedown.prevent="($event.target as HTMLInputElement).showPicker?.()"
              />
            </div>
            <div class="form-group">
              <label>文档类型</label>
              <div class="custom-select-container" style="position: relative;" @click.stop>
                <div 
                  class="beautiful-select-trigger"
                  :class="{ 'is-active': isLinkTypeDropdownOpen }"
                  @click="isLinkTypeDropdownOpen = !isLinkTypeDropdownOpen"
                  style="display: flex; align-items: center; justify-content: space-between; cursor: pointer; padding: 0 16px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background: #fff; transition: all var(--transition-fast); height: 42px;"
                >
                  <div style="display: flex; align-items: center; gap: 8px;">
                    <span style="font-size: 14px; line-height: 1;">
                      {{ linkForm.type === '1' ? '🎨' : linkForm.type === '3' ? '📅' : '📄' }}
                    </span>
                    <span style="font-size: 13.5px; font-weight: 500; color: var(--text-main);">
                      {{ linkForm.type === '1' ? '原型设计' : linkForm.type === '3' ? '会议纪要' : '需求文档' }}
                    </span>
                  </div>
                  <svg 
                    xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.2" stroke="currentColor" 
                    style="width: 14px; height: 14px; transition: transform 0.2s;"
                    :style="{ transform: isLinkTypeDropdownOpen ? 'rotate(180deg)' : 'rotate(0deg)' }"
                  >
                    <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
                  </svg>
                </div>
                
                <div 
                  v-if="isLinkTypeDropdownOpen" 
                  class="custom-dropdown-list"
                  style="position: absolute; top: calc(100% + 6px); left: 0; right: 0; background: rgba(255, 255, 255, 0.98); border: 1px solid rgba(0, 0, 0, 0.08); border-radius: var(--radius-md); box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.05); z-index: 999; padding: 6px; backdrop-filter: blur(12px); display: flex; flex-direction: column; gap: 4px;"
                >
                  <div 
                    @click="linkForm.type = '1'; isLinkTypeDropdownOpen = false;"
                    class="custom-dropdown-item"
                    :class="{ 'is-selected': linkForm.type === '1' }"
                    style="display: flex; align-items: center; justify-content: space-between; padding: 8px 12px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
                  >
                    <div style="display: flex; align-items: center; gap: 8px;">
                      <span style="font-size: 14px;">🎨</span>
                      <span style="font-size: 13.5px; font-weight: 500; color: var(--text-main);">原型设计</span>
                    </div>
                    <svg 
                      v-if="linkForm.type === '1'"
                      xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="#2563eb" style="width: 14px; height: 14px;"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
                    </svg>
                  </div>

                  <div 
                    @click="linkForm.type = '2'; isLinkTypeDropdownOpen = false;"
                    class="custom-dropdown-item"
                    :class="{ 'is-selected': linkForm.type === '2' }"
                    style="display: flex; align-items: center; justify-content: space-between; padding: 8px 12px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
                  >
                    <div style="display: flex; align-items: center; gap: 8px;">
                      <span style="font-size: 14px;">📄</span>
                      <span style="font-size: 13.5px; font-weight: 500; color: var(--text-main);">需求文档</span>
                    </div>
                    <svg 
                      v-if="linkForm.type === '2'"
                      xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="#2563eb" style="width: 14px; height: 14px;"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
                    </svg>
                  </div>

                  <div 
                    @click="linkForm.type = '3'; isLinkTypeDropdownOpen = false;"
                    class="custom-dropdown-item"
                    :class="{ 'is-selected': linkForm.type === '3' }"
                    style="display: flex; align-items: center; justify-content: space-between; padding: 8px 12px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
                  >
                    <div style="display: flex; align-items: center; gap: 8px;">
                      <span style="font-size: 14px;">📅</span>
                      <span style="font-size: 13.5px; font-weight: 500; color: var(--text-main);">会议纪要</span>
                    </div>
                    <svg 
                      v-if="linkForm.type === '3'"
                      xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="#2563eb" style="width: 14px; height: 14px;"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
                    </svg>
                  </div>
                </div>
              </div>
            </div>
            <div class="modal-actions">
              <button type="button" class="action-btn-secondary" @click="closeLinkModal">取消</button>
              <button type="submit" class="action-btn-primary">添加</button>
            </div>
          </form>
        </div>
      </div>
    </transition>



    <!-- MODAL 5: Cancel Reason (for Transitioning to Not Involved) -->
    <transition name="modal-fade">
      <div 
        v-if="isCancelModalOpen" 
        class="modal-overlay" 
        @mousedown.self="onMousedownOverlay"
        @mouseup.self="onMouseupOverlay('cancel')"
      >
        <div class="modal-card">
          <div class="modal-header">
            <h2>流转至「不涉及」状态</h2>
            <button class="close-modal-btn" @click="closeCancelModal">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <form @submit.prevent="submitCancelReason" class="modal-form">
            <div class="form-group">
              <label for="cancel-reason-input">不涉及原因<span class="required-star">*</span></label>
              <textarea 
                id="cancel-reason-input" 
                v-model="cancelReasonInput" 
                placeholder="请填写此需求不涉及开发的原因 (最长500字符)" 
                rows="4"
                required
                class="form-textarea"
              ></textarea>
            </div>
            <div class="modal-actions">
              <button type="button" class="action-btn-secondary" @click="closeCancelModal">取消</button>
              <button type="submit" class="action-btn-primary">确认流转</button>
            </div>
          </form>
        </div>
      </div>
    </transition>

    <!-- MODAL 6: Select Key Result Sub-Modal -->
    <transition name="modal-fade">
      <div 
        v-if="isSelectKRSubModalOpen" 
        class="modal-overlay" 
        @mousedown.self="onMousedownOverlay"
        @mouseup.self="onMouseupOverlay('select')"
        style="z-index: 2500;"
      >
        <div class="modal-card" style="width: 480px; max-width: 90vw;">
          <div class="modal-header">
            <h2>选择要绑定的关键成果 (K)</h2>
            <button type="button" class="close-modal-btn" @click="closeSelectKRSubModal">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          
          <div class="modal-body-content" style="padding: 0 4px;">
            <!-- Month search filter (Matches status dropdown style) -->
            <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 16px;">
              <label style="font-size: 14px; font-weight: 600; color: var(--text-main); white-space: nowrap;">月份过滤：</label>
              
              <div class="custom-select-container" style="position: relative; flex: 1;" @click.stop>
                <div 
                  class="beautiful-select-trigger"
                  :class="{ 'is-active': isMonthDropdownOpen }"
                  @click="isMonthDropdownOpen = !isMonthDropdownOpen"
                  style="display: flex; align-items: center; justify-content: space-between; cursor: pointer; padding: 0 16px; border: 1.5px solid var(--border-medium); border-radius: var(--radius-md); background: #fff; transition: all var(--transition-fast); height: 42px;"
                >
                  <div style="display: flex; align-items: center; gap: 8px;">
                    <span style="font-size: 14px; line-height: 1;">📅</span>
                    <span style="font-size: 13.5px; font-weight: 500; color: var(--text-main);">
                      {{ krMonthQuery }}
                    </span>
                  </div>
                  <svg 
                    xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.2" stroke="currentColor" 
                    style="width: 14px; height: 14px; transition: transform 0.2s;"
                    :style="{ transform: isMonthDropdownOpen ? 'rotate(180deg)' : 'rotate(0deg)' }"
                  >
                    <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
                  </svg>
                </div>
                
                <div 
                  v-if="isMonthDropdownOpen" 
                  class="custom-dropdown-list"
                  style="position: absolute; top: calc(100% + 6px); left: 0; right: 0; background: rgba(255, 255, 255, 0.98); border: 1px solid rgba(0, 0, 0, 0.08); border-radius: var(--radius-md); box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.05); z-index: 999; padding: 6px; backdrop-filter: blur(12px); display: flex; flex-direction: column; gap: 4px; max-height: 200px; overflow-y: auto;"
                >

                  <div 
                    v-for="m in krMonthsList" 
                    :key="m"
                    @click="krMonthQuery = m; isMonthDropdownOpen = false;"
                    class="custom-dropdown-item"
                    :class="{ 'is-selected': krMonthQuery === m }"
                    style="display: flex; align-items: center; justify-content: space-between; padding: 8px 12px; border-radius: 6px; cursor: pointer; transition: all 0.15s ease;"
                  >
                    <div style="display: flex; align-items: center; gap: 8px;">
                      <span style="font-size: 13.5px; font-weight: 500; color: var(--text-main);">{{ m }}</span>
                    </div>
                    <svg 
                      v-if="krMonthQuery === m"
                      xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="#2563eb" style="width: 14px; height: 14px;"
                    >
                      <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
                    </svg>
                  </div>
                </div>
              </div>
            </div>
            
            <div v-if="filteredKRsForSelection.length === 0" class="bind-empty">
              <span class="icon">⚠️</span>
              <p>该月份暂无可选的关键成果 (K)。</p>
            </div>
            
            <div v-else class="bind-krs-list" style="max-height: 280px; overflow-y: auto; display: flex; flex-direction: column; gap: 8px;">
              <div 
                v-for="kr in filteredKRsForSelection" 
                :key="kr.id" 
                class="bind-kr-item"
                :class="{ selected: tempSelectedKRId === kr.id }"
                @click="selectKR(kr.id)"
                style="display: flex; align-items: flex-start; gap: 12px; border: 1.5px solid var(--border-medium); border-radius: 8px; padding: 12px; cursor: pointer; transition: all 0.15s ease;"
              >
                <div class="radio-indicator" style="margin-top: 3px;">
                  <span v-if="tempSelectedKRId === kr.id" class="dot"></span>
                </div>
                <div class="kr-info">
                  <div class="kr-title" style="font-size: 13.5px; font-weight: 600; color: var(--text-main); text-align: left;">{{ kr.name }}</div>
                  <div class="kr-meta" style="margin-top: 4px; display: flex; gap: 8px; font-size: 11px; color: var(--text-muted);">
                    <span class="kr-badge-status" :class="'status-' + kr.status">
                      K 状态: {{ getKRStatusLabel(kr.status) }}
                    </span>
                    <span v-if="kr.status === '2'" class="kr-due" style="color: #10b981; font-weight: 600;">实际完成: {{ kr.completeDate || kr.planCompleteDate }}</span>
                    <span v-else-if="kr.planCompleteDate" class="kr-due" style="color: #f59e0b; font-weight: 600;">预计完成: {{ kr.planCompleteDate }}</span>
                  </div>
                </div>
              </div>
            </div>
            
            <div class="modal-actions" style="display: flex; justify-content: flex-end; gap: 12px; margin-top: 20px; border-top: 1px solid var(--border-light); padding-top: 16px;">
              <button type="button" class="action-btn-primary" @click="confirmSelectKR">确定选择</button>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- MODAL 7: Custom Delete Confirmation Modal -->
    <transition name="modal-fade">
      <div 
        v-if="isDeleteModalOpen" 
        class="modal-overlay" 
        @mousedown.self="onMousedownOverlay"
        @mouseup.self="onMouseupOverlay('delete')"
        style="z-index: 2600;"
      >
        <div class="modal-card" style="max-width: 480px; padding: 32px; border-radius: 12px; background-color: var(--bg-card); box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04); display: flex; flex-direction: column;" @click.stop>
          
          <!-- Header (horizontal: icon + title) -->
          <div style="display: flex; align-items: center; gap: 16px; width: 100%; margin-bottom: 24px;">
            <div style="width: 40px; height: 40px; border-radius: 50%; background-color: #fffbeb; display: flex; align-items: center; justify-content: center; color: #d97706; flex-shrink: 0;">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" style="width: 20px; height: 20px;">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
            </div>
            <h3 style="font-size: 20px; font-weight: 700; color: var(--text-main); margin: 0; line-height: 1;">删除需求</h3>
          </div>

          <!-- Message (left aligned) -->
          <p style="font-size: 15px; color: var(--text-main); line-height: 1.6; margin: 0 0 12px 0; text-align: left;">
            确定要删除需求 <strong style="color: var(--primary);">【{{ deletingRequirement?.title }}】</strong> 吗？
          </p>
          <p style="font-size: 13.5px; color: var(--text-muted); line-height: 1.5; margin: 0 0 24px 0; text-align: left;">
            此操作不可逆，相关的所有文档链接也将一同删除。
          </p>

          <!-- Modal Actions (right aligned cancel and confirm) -->
          <div class="modal-actions" style="margin-top: 8px; display: flex; justify-content: flex-end; gap: 12px; border-top: none; padding-top: 0; width: 100%;">
            <button type="button" class="btn btn-secondary" style="display: inline-flex; align-items: center; justify-content: center; min-width: 80px; min-height: 38px; cursor: pointer; background-color: #f1f5f9; border: none; color: #475569; font-weight: 600; border-radius: 8px; transition: background-color 0.15s;" @click="closeDeleteModal">取消</button>
            <button type="button" class="btn btn-danger" style="display: inline-flex; align-items: center; justify-content: center; min-width: 100px; background-color: #ef4444; color: white; border: none; border-radius: 8px; font-weight: 600; cursor: pointer; min-height: 38px; transition: background-color 0.15s;" @click="executeDelete">确认删除</button>
          </div>
        </div>
      </div>
    </transition>

    <!-- MODAL 8: Custom Document Link Delete Confirmation Modal -->
    <transition name="modal-fade">
      <div 
        v-if="isDeleteDocModalOpen" 
        class="modal-overlay" 
        @mousedown.self="onMousedownOverlay"
        @mouseup.self="onMouseupOverlay('deleteDoc')"
        style="z-index: 2600;"
      >
        <div class="modal-card" style="max-width: 480px; padding: 32px; border-radius: 12px; background-color: var(--bg-card); box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04); display: flex; flex-direction: column;" @click.stop>
          
          <!-- Header (horizontal: icon + title) -->
          <div style="display: flex; align-items: center; gap: 16px; width: 100%; margin-bottom: 24px;">
            <div style="width: 40px; height: 40px; border-radius: 50%; background-color: #fffbeb; display: flex; align-items: center; justify-content: center; color: #d97706; flex-shrink: 0;">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" style="width: 20px; height: 20px;">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
            </div>
            <h3 style="font-size: 20px; font-weight: 700; color: var(--text-main); margin: 0; line-height: 1;">删除文档链接</h3>
          </div>

          <!-- Message (left aligned) -->
          <p style="font-size: 15px; color: var(--text-main); line-height: 1.6; margin: 0 0 12px 0; text-align: left;">
            确定要删除文档链接 <strong style="color: var(--primary);">【{{ deletingDocTitle }}】</strong> 吗？
          </p>
          <p style="font-size: 13.5px; color: var(--text-muted); line-height: 1.5; margin: 0 0 24px 0; text-align: left;">
            此操作不可逆，将从需求关联中完全移除该文档。
          </p>

          <!-- Modal Actions (right aligned cancel and confirm) -->
          <div class="modal-actions" style="margin-top: 8px; display: flex; justify-content: flex-end; gap: 12px; border-top: none; padding-top: 0; width: 100%;">
            <button type="button" class="btn btn-secondary" style="display: inline-flex; align-items: center; justify-content: center; min-width: 80px; min-height: 38px; cursor: pointer; background-color: #f1f5f9; border: none; color: #475569; font-weight: 600; border-radius: 8px; transition: background-color 0.15s;" @click="closeDeleteDocModal">取消</button>
            <button type="button" class="btn btn-danger" style="display: inline-flex; align-items: center; justify-content: center; min-width: 100px; background-color: #ef4444; color: white; border: none; border-radius: 8px; font-weight: 600; cursor: pointer; min-height: 38px; transition: background-color 0.15s;" @click="executeDeleteDocument">确认删除</button>
          </div>
        </div>
      </div>
    </transition>

    <!-- MODAL 9: Acceptance Done Modal -->
    <transition name="modal-fade">
      <div 
        v-if="isAcceptanceModalOpen" 
        class="modal-overlay" 
        @mousedown.self="onMousedownOverlay"
        @mouseup.self="onMouseupOverlay('acceptance')"
        style="z-index: 2500;"
      >
        <div class="modal-card" style="max-width: 480px;" @click.stop>
          <div class="modal-header">
            <h2>填写验收信息</h2>
            <button class="close-modal-btn" @click="closeAcceptanceModal">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <form @submit.prevent="submitAcceptance" class="modal-form" autocomplete="off">
            <div class="form-group">
              <label for="acceptance-date">验收时间<span class="required-star">*</span></label>
              <input 
                id="acceptance-date" 
                v-model="acceptanceDateInput" 
                type="date" 
                required
                :max="todayDateStr"
                class="form-input"
                @mousedown.prevent="($event.target as HTMLInputElement).showPicker?.()"
              />
            </div>
            <div class="form-group">
              <label for="acceptance-person">验收人<span class="required-star">*</span></label>
              <input 
                id="acceptance-person" 
                v-model="acceptancePersonInput" 
                type="text" 
                placeholder="请输入验收人姓名" 
                required
                class="form-input"
                autocomplete="off"
              />
            </div>
            <div class="modal-actions">
              <button type="button" class="action-btn-secondary" @click="closeAcceptanceModal">取消</button>
              <button type="submit" class="action-btn-primary">确定</button>
            </div>
          </form>
        </div>
      </div>
    </transition>

    <!-- MODAL 10: Released Modal -->
    <transition name="modal-fade">
      <div 
        v-if="isReleaseModalOpen" 
        class="modal-overlay" 
        @mousedown.self="onMousedownOverlay"
        @mouseup.self="onMouseupOverlay('release')"
        style="z-index: 2500;"
      >
        <div class="modal-card" style="max-width: 480px;" @click.stop>
          <div class="modal-header">
            <h2>填写发布信息</h2>
            <button class="close-modal-btn" @click="closeReleaseModal">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <form @submit.prevent="submitRelease" class="modal-form" autocomplete="off">
            <div class="form-group">
              <label for="release-date">发布时间<span class="required-star">*</span></label>
              <input 
                id="release-date" 
                v-model="releaseDateInput" 
                type="date" 
                required
                :max="todayDateStr"
                class="form-input"
                @mousedown.prevent="($event.target as HTMLInputElement).showPicker?.()"
              />
            </div>
            <div class="modal-actions">
              <button type="button" class="action-btn-secondary" @click="closeReleaseModal">取消</button>
              <button type="submit" class="action-btn-primary">确定</button>
            </div>
          </form>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted, h } from 'vue'
import request from '@/api/request'
import { ApiResult } from '@/api/types'

// Icons used in link chips
const FigmaIcon = {
  render() {
    return h('svg', { xmlns: 'http://www.w3.org/2000/svg', viewBox: '0 0 384 512', fill: 'currentColor' }, [
      h('path', { d: 'M181.3 324.5V230.1h45.4c25.1 0 45.4 20.3 45.4 45.4 0 25.1-20.3 45.4-45.4 45.4h-45.4zM90.4 466.6V376c0-25.1 20.3-45.4 45.4-45.4h45.4v45.4c0 25.1-20.3 45.4-45.4 45.4H90.4zm0-182c0-25.1 20.3-45.4 45.4-45.4h45.4v90.9h-45.4c-25.1 0-45.4-20.3-45.4-45.5zm90.9-182v90.9H136c-25.1 0-45.4-20.3-45.4-45.4 0-25.1 20.3-45.5 45.4-45.5h45.4zm90.8 90.9V47.1h45.4c25.1 0 45.4 20.3 45.4 45.4 0 25.1-20.3 45.4-45.4 45.4h-45.4z' })
    ])
  }
}

const MeetingIcon = {
  render() {
    return h('svg', { xmlns: 'http://www.w3.org/2000/svg', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z' })
    ])
  }
}

const DocIcon = {
  render() {
    return h('svg', { xmlns: 'http://www.w3.org/2000/svg', viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m2.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z' })
    ])
  }
}

// Click outside directive helper
const vClickOutside = {
  mounted(el: any, binding: any) {
    el.clickOutsideEvent = (event: Event) => {
      if (!(el === event.target || el.contains(event.target))) {
        binding.value(event)
      }
    }
    document.addEventListener('click', el.clickOutsideEvent)
  },
  unmounted(el: any) {
    document.removeEventListener('click', el.clickOutsideEvent)
  }
}

// Interfaces
interface KeyResult {
  id: number
  name: string
  status: '0' | '1' | '2' | '3' // 0未开始/1进行中/2已完成/3已取消
  planCompleteDate?: string
  completeDate?: string
}

interface RequirementDocument {
  id: number
  requirementId: number
  type: '1' | '2' | '3' // 1原型设计 / 2需求文档 / 3会议纪要
  title: string
  url: string
  documentDate?: string
  createdAt?: string
}

interface Requirement {
  id: number
  title: string
  description?: string
  status: '0' | '1' | '2' | '3' | '4' | '5' // 0讨论中/1不涉及/2进行中/3开发完成/4验收完成/5发布完成
  keyResultId: number | null
  keyResult: KeyResult | null
  categoryId: number
  categoryName?: string
  subCategoryId?: number | null
  subCategoryName?: string
  firstDemandDate: string
  devCompleteDate: string | null
  acceptanceDate: string | null
  releaseDate: string | null
  cancelReason: string | null
  documents: RequirementDocument[]
  createdAt: string
  acceptancePerson?: string
}

const krMonthQuery = ref(new Date().toISOString().substring(0, 7))
const requirements = ref<Requirement[]>([])
const keyResultsList = ref<any[]>([])

async function fetchKeyResults() {
  try {
    const month = krMonthQuery.value
    const res = await request.get<any[], ApiResult<any[]>>(`/api/objectives?month=${month}`)
    const objectives = res.data || []
    const krs: any[] = []
    objectives.forEach(obj => {
      if (obj.keyResults) {
        obj.keyResults.forEach((kr: any) => {
          krs.push({
            id: kr.id,
            name: kr.name,
            status: kr.status,
            planCompleteDate: kr.planCompleteDate,
            completeDate: kr.completeDate
          })
        })
      }
    })
    keyResultsList.value = krs
  } catch (err) {
    console.error('Failed to fetch key results:', err)
  }
}

async function fetchRequirements() {
  isLoading.value = true
  try {
    let url = `/api/requirements/page?pageNum=${currentPage.value}&pageSize=${pageSize.value}`
    if (searchQuery.value.trim()) {
      url += `&keyword=${encodeURIComponent(searchQuery.value.trim())}`
    }
    if (statusFilters.value.length > 0) {
      statusFilters.value.forEach(s => {
        url += `&statuses=${s}`
      })
    }
    if (startDate.value) {
      url += `&startDate=${startDate.value}`
    }
    if (endDate.value) {
      url += `&endDate=${endDate.value}`
    }
    if (filterCategoryId.value !== null) {
      url += `&categoryId=${filterCategoryId.value}`
    }
    if (filterSubCategoryId.value !== null) {
      url += `&subCategoryId=${filterSubCategoryId.value}`
    }
    const res = await request.get<any, ApiResult<any>>(url)
    requirements.value = res.data.records || []
    totalPages.value = res.data.pages || 1
    totalRecords.value = res.data.total || 0
  } catch (err) {
    console.error('Failed to fetch requirements page:', err)
  } finally {
    isLoading.value = false
  }
}

// Filter states
const searchQuery = ref("")
const statusFilters = ref<string[]>([])
const filterCategoryId = ref<number | null>(null)
const filterSubCategoryId = ref<number | null>(null)

// Pagination states
const currentPage = ref(1)
const pageSize = ref(20)
const totalPages = ref(1)
const totalRecords = ref(0)
const isLoading = ref(false)

// Custom select dropdown & date range filter states
const isStatusDropdownOpen = ref(false)
const isDateDropdownOpen = ref(false)
const isFilterCategoryDropdownOpen = ref(false)
const isFilterSubCategoryDropdownOpen = ref(false)
const startDate = ref("")
const endDate = ref("")
const tempStartDate = ref("")
const tempEndDate = ref("")

const todayDateStr = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})

const statusFilterOptions = [
  { label: '全部状态', value: 'all', color: '#64748b' },
  { label: '讨论中', value: '0', color: '#3b82f6' },
  { label: '不涉及', value: '1', color: '#94a3b8' },
  { label: '进行中', value: '2', color: '#f59e0b' },
  { label: '开发完成', value: '3', color: '#6366f1' },
  { label: '验收完成', value: '4', color: '#10b981' },
  { label: '发布完成', value: '5', color: '#059669' }
]

const currentStatusLabel = computed(() => {
  if (statusFilters.value.length === 0) return '全部状态'
  if (statusFilters.value.length === 1) {
    const opt = statusFilterOptions.find(o => o.value === statusFilters.value[0])
    return opt ? opt.label : '全部状态'
  }
  return `已选 ${statusFilters.value.length} 个状态`
})

function toggleStatusFilter(value: string) {
  if (value === 'all') {
    statusFilters.value = []
  } else {
    const index = statusFilters.value.indexOf(value)
    if (index > -1) {
      statusFilters.value.splice(index, 1)
    } else {
      statusFilters.value.push(value)
    }
  }
}

const filterCategoryLabel = computed(() => {
  if (filterCategoryId.value === null) return '全部主分类'
  const cat = reqCategoriesList.value.find(c => c.id === filterCategoryId.value)
  return cat ? cat.name : '全部主分类'
})

const filterSubCategoryLabel = computed(() => {
  if (filterSubCategoryId.value === null) return '全部子分类'
  const cat = reqCategoriesList.value.find(c => c.id === filterCategoryId.value)
  if (!cat || !cat.subcategories) return '全部子分类'
  const sub = cat.subcategories.find(s => s.id === filterSubCategoryId.value)
  return sub ? sub.name : '全部子分类'
})

const formattedDateRangeLabel = computed(() => {
  if (!startDate.value && !endDate.value) return '首次需求时间范围'
  if (startDate.value && !endDate.value) return `${startDate.value} 起`
  if (!startDate.value && endDate.value) return `${endDate.value} 止`
  return `${startDate.value} 至 ${endDate.value}`
})

const toggleDateDropdown = () => {
  if (!isDateDropdownOpen.value) {
    tempStartDate.value = startDate.value
    tempEndDate.value = endDate.value
    isStatusDropdownOpen.value = false
  }
  isDateDropdownOpen.value = !isDateDropdownOpen.value
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

const closeAllDropdowns = () => {
  isStatusDropdownOpen.value = false
  isDateDropdownOpen.value = false
  isMonthDropdownOpen.value = false
  isLinkTypeDropdownOpen.value = false
  isCreateCategoryDropdownOpen.value = false
  isCreateSubCategoryDropdownOpen.value = false
  isEditCategoryDropdownOpen.value = false
  isEditSubCategoryDropdownOpen.value = false
  isFilterCategoryDropdownOpen.value = false
  isFilterSubCategoryDropdownOpen.value = false
}

onMounted(() => {
  document.addEventListener('click', closeAllDropdowns)
  fetchRequirements()
  fetchKeyResults()
  fetchKrMonths()
  fetchReqCategories()
})

onUnmounted(() => {
  document.removeEventListener('click', closeAllDropdowns)
})

let searchTimer: any = null
watch(searchQuery, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 1
    fetchRequirements()
  }, 300)
})

watch([statusFilters, startDate, endDate, filterCategoryId, filterSubCategoryId], () => {
  currentPage.value = 1
  fetchRequirements()
}, { deep: true })

watch(currentPage, () => {
  fetchRequirements()
})

watch(krMonthQuery, () => {
  fetchKeyResults()
})

// Active state transition menu ID
const activeStatusMenuId = ref<number | null>(null)

// Modal states & Forms
const isCreateModalOpen = ref(false)
const createForm = reactive({
  title: "",
  description: "",
  firstDemandDate: "",
  keyResultId: null as number | null,
  categoryId: null as number | null,
  subCategoryId: null as number | null
})

const isEditModalOpen = ref(false)
const isKRReadOnlyInEdit = ref(false)
const editingReqId = ref<number | null>(null)
const editForm = reactive({
  title: "",
  description: "",
  firstDemandDate: "",
  keyResultId: null as number | null,
  categoryId: null as number | null,
  subCategoryId: null as number | null
})

// Select KR Sub-Modal States
const isSelectKRSubModalOpen = ref(false)
const selectKRSubModalTarget = ref<'create' | 'edit' | null>(null)
const tempSelectedKRId = ref<number | null>(null)
const isMonthDropdownOpen = ref(false)

interface ReqCategory {
  id: number
  parentId: number | null
  name: string
  subcategories?: ReqCategory[]
}
const reqCategoriesList = ref<ReqCategory[]>([])

const isCreateCategoryDropdownOpen = ref(false)
const isCreateSubCategoryDropdownOpen = ref(false)
const isEditCategoryDropdownOpen = ref(false)
const isEditSubCategoryDropdownOpen = ref(false)

function getCategoryNameById(catId: number | null): string {
  if (!catId) return '请选择主分类'
  const cat = reqCategoriesList.value.find(c => c.id === catId)
  return cat ? cat.name : '请选择主分类'
}

function getSubCategoryNameById(catId: number | null, subId: number | null): string {
  if (!subId || !catId) return '不选择子分类'
  const cat = reqCategoriesList.value.find(c => c.id === catId)
  if (!cat || !cat.subcategories) return '不选择子分类'
  const sub = cat.subcategories.find(s => s.id === subId)
  return sub ? sub.name : '不选择子分类'
}

async function fetchReqCategories() {
  try {
    const res = await request.get<ReqCategory[], ApiResult<ReqCategory[]>>('/api/requirement-categories')
    reqCategoriesList.value = res.data || []
  } catch (err) {
    console.error('Failed to fetch requirement categories:', err)
  }
}

function getSubcategoriesForCategory(catId: number | null) {
  if (!catId) return []
  const cat = reqCategoriesList.value.find(c => c.id === catId)
  return cat ? cat.subcategories || [] : []
}

// Helper function to resolve KR Month or details
const krMonthsList = ref<string[]>([])

async function fetchKrMonths() {
  try {
    const res = await request.get<any, ApiResult<string[]>>('/api/objectives/months')
    let list = res.data || []
    if (list.length === 0) {
      const now = new Date()
      list = [`${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`]
    }
    krMonthsList.value = list.sort((a, b) => b.localeCompare(a))
  } catch (err) {
    console.error('Failed to fetch objectives months:', err)
    // Fallback on error
    const now = new Date()
    krMonthsList.value = [`${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`]
  }
}

const filteredKRsForSelection = computed(() => {
  return keyResultsList.value.filter(kr => kr.status !== '3')
})

function getKRNameById(id: number | null) {
  if (id === null) return ''
  const kr = keyResultsList.value.find(k => k.id === id)
  if (kr) {
    const month = kr.planCompleteDate ? ` (${kr.planCompleteDate.substring(0, 7)})` : ''
    return `${kr.name}${month}`
  }
  const req = requirements.value.find(r => r.keyResultId === id)
  if (req && req.keyResult) {
    return req.keyResult.name
  }
  return ''
}

function getDefaultMonthForTarget(target: 'create' | 'edit') {
  const krId = target === 'create' ? createForm.keyResultId : editForm.keyResultId
  if (krId !== null) {
    const kr = keyResultsList.value.find(k => k.id === krId)
    if (kr && kr.planCompleteDate) {
      return kr.planCompleteDate.substring(0, 7)
    }
  }
  return new Date().toISOString().substring(0, 7)
}

function openSelectKRSubModal(target: 'create' | 'edit') {
  selectKRSubModalTarget.value = target
  krMonthQuery.value = getDefaultMonthForTarget(target)
  if (target === 'create') {
    tempSelectedKRId.value = createForm.keyResultId
  } else {
    tempSelectedKRId.value = editForm.keyResultId
  }
  fetchKrMonths()
  isSelectKRSubModalOpen.value = true
}

function closeSelectKRSubModal() {
  isSelectKRSubModalOpen.value = false
  isMonthDropdownOpen.value = false
  selectKRSubModalTarget.value = null
}

function selectKR(krId: number) {
  tempSelectedKRId.value = krId
}

function confirmSelectKR() {
  if (selectKRSubModalTarget.value === 'create') {
    createForm.keyResultId = tempSelectedKRId.value
  } else if (selectKRSubModalTarget.value === 'edit') {
    editForm.keyResultId = tempSelectedKRId.value
  }
  closeSelectKRSubModal()
}

function clearKRSelection() {
  if (selectKRSubModalTarget.value === 'create') {
    createForm.keyResultId = null
  } else if (selectKRSubModalTarget.value === 'edit') {
    editForm.keyResultId = null
  }
  closeSelectKRSubModal()
}

const isLinkModalOpen = ref(false)
const linkFormReqId = ref<number | null>(null)
const linkForm = reactive({
  title: "",
  url: "",
  type: "2" as "1" | "2" | "3",
  date: ""
})
const isLinkTypeDropdownOpen = ref(false)



const isCancelModalOpen = ref(false)
const cancelReqId = ref<number | null>(null)
const cancelReasonInput = ref("")

const isDeleteModalOpen = ref(false)
const deletingRequirement = ref<Requirement | null>(null)

const isDeleteDocModalOpen = ref(false)
const deletingDocId = ref<number | null>(null)
const deletingDocTitle = ref("")
const deletingDocReq = ref<Requirement | null>(null)

const isAcceptanceModalOpen = ref(false)
const acceptanceReqId = ref<number | null>(null)
const acceptanceDateInput = ref("")
const acceptancePersonInput = ref("")

const isReleaseModalOpen = ref(false)
const releaseReqId = ref<number | null>(null)
const releaseDateInput = ref("")



// Filtered Requirements logic
const filteredRequirements = computed(() => {
  return requirements.value
})



// Helpers
function getStatusLabel(status: string) {
  const labels: Record<string, string> = {
    "0": "讨论中",
    "1": "不涉及",
    "2": "进行中",
    "3": "开发完成",
    "4": "验收完成",
    "5": "发布完成"
  }
  return labels[status] || "未知"
}

function getKRStatusLabel(status: string) {
  const labels: Record<string, string> = {
    "0": "未开始",
    "1": "进行中",
    "2": "已完成",
    "3": "已取消"
  }
  return labels[status] || "未知"
}

function getStatusColor(status: string) {
  const colors: Record<string, string> = {
    "0": "#3b82f6", // blue (discussing)
    "1": "#94a3b8", // slate (not involved)
    "2": "#f59e0b", // amber (in progress)
    "3": "#6366f1", // indigo (dev done)
    "4": "#10b981", // emerald (acceptance done)
    "5": "#059669"  // dark emerald (released)
  }
  return colors[status] || "#cbd5e1"
}

function getStatusStyle(status: string) {
  const color = getStatusColor(status)
  return {
    backgroundColor: `${color}12`,
    color: color,
    borderColor: `${color}30`
  }
}

function getLinkIcon(doc: RequirementDocument) {
  if (doc.type === '1') return FigmaIcon
  if (doc.type === '3') return MeetingIcon
  return DocIcon
}

function getLinkIconBg(doc: RequirementDocument) {
  if (doc.type === '1') {
    return { backgroundColor: '#f3e8ff', color: '#a855f7' } // Purple
  }
  if (doc.type === '3') {
    return { backgroundColor: '#fee2e2', color: '#ef4444' } // Red
  }
  if (doc.type === '2') {
    return { backgroundColor: '#dbeafe', color: '#3b82f6' } // Blue
  }
  
  // Fallbacks for legacy/mock data
  const url = doc.url.toLowerCase()
  const title = doc.title.toLowerCase()
  if (url.includes('figma.com') || title.includes('原型') || title.includes('设计') || url.includes('sketch') || url.includes('axure')) {
    return { backgroundColor: '#f3e8ff', color: '#a855f7' } // Purple
  } else if (doc.type === '3' || url.includes('meeting') || title.includes('会议')) {
    return { backgroundColor: '#fee2e2', color: '#ef4444' } // Red
  }
  return { backgroundColor: '#dbeafe', color: '#3b82f6' } // Blue
}

function formatDate(isoStr: string) {
  const d = new Date(isoStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}



function isStatusLockedByKR(req: Requirement) {
  return req.keyResultId != null && ['0', '2'].includes(req.status)
}

// Status Transition Logic
function toggleStatusMenu(req: Requirement) {
  if (isStatusLockedByKR(req)) return
  activeStatusMenuId.value = activeStatusMenuId.value === req.id ? null : req.id
}

// Click outside helper handler
function closeStatusMenu() {
  activeStatusMenuId.value = null
}

function getLegalTransitions(req: Requirement) {
  const current = req.status
  const transitions: Record<string, { value: string, label: string }[]> = {
    "0": [
      { value: "1", label: "不涉及" },
      { value: "2", label: "进行中" }
    ],
    "1": [
      { value: "0", label: "重新讨论" },
      { value: "2", label: "进行中" }
    ],
    "2": [
      { value: "0", label: "重新讨论" },
      { value: "1", label: "不涉及" },
      { value: "3", label: "开发完成" }
    ],
    "3": [
      { value: "2", label: "退回进行中" },
      { value: "1", label: "不涉及" },
      { value: "4", label: "验收完成" },
      { value: "5", label: "发布完成" }
    ],
    "4": [
      { value: "5", label: "发布完成" }
    ],
    "5": [] // 终态，不可流转
  }

  // If bound to K, manual targets are limited to: 不涉及(1), 验收完成(4), 发布完成(5)
  // Let's filter transitions accordingly
  let list = transitions[current] || []
  if (req.keyResultId != null) {
    if (current === '3') {
      return [
        { value: "4", label: "验收完成" },
        { value: "5", label: "发布完成" }
      ]
    }
    list = list.filter(t => ['1', '4', '5'].includes(t.value))
  }
  return list
}

function triggerStatusTransition(req: Requirement, targetStatus: string) {
  closeStatusMenu()
  
  if (targetStatus === '1') {
    // Requires a cancellation reason modal
    cancelReqId.value = req.id
    cancelReasonInput.value = ""
    isCancelModalOpen.value = true
    return
  }

  if (targetStatus === '4') {
    acceptanceReqId.value = req.id
    acceptanceDateInput.value = todayDateStr.value
    acceptancePersonInput.value = ""
    isAcceptanceModalOpen.value = true
    return
  }

  if (targetStatus === '5') {
    releaseReqId.value = req.id
    releaseDateInput.value = todayDateStr.value
    isReleaseModalOpen.value = true
    return
  }

  executeStatusTransition(req, targetStatus)
}

function closeAcceptanceModal() {
  isAcceptanceModalOpen.value = false
  acceptanceReqId.value = null
  acceptanceDateInput.value = ""
  acceptancePersonInput.value = ""
}

async function submitAcceptance() {
  if (!acceptanceReqId.value || !acceptanceDateInput.value || !acceptancePersonInput.value.trim()) {
    dispatchToast("请填写完整的验收信息", "error")
    return
  }
  
  const reqId = acceptanceReqId.value
  const req = requirements.value.find(r => r.id === reqId)
  if (!req) return
  
  try {
    await executeStatusTransition(
      req, 
      '4', 
      null, 
      acceptanceDateInput.value, 
      acceptancePersonInput.value.trim()
    )
    closeAcceptanceModal()
  } catch (e) {
    // Handled by request interceptor
  }
}

function closeReleaseModal() {
  isReleaseModalOpen.value = false
  releaseReqId.value = null
  releaseDateInput.value = ""
}

async function submitRelease() {
  if (!releaseReqId.value || !releaseDateInput.value) {
    dispatchToast("请选择发布时间", "error")
    return
  }
  
  const reqId = releaseReqId.value
  const req = requirements.value.find(r => r.id === reqId)
  if (!req) return
  
  try {
    await executeStatusTransition(
      req, 
      '5', 
      null, 
      null, 
      null, 
      releaseDateInput.value
    )
    closeReleaseModal()
  } catch (e) {
    // Handled by request interceptor
  }
}

async function executeStatusTransition(
  req: Requirement, 
  targetStatus: string, 
  cancelReason: string | null = null,
  acceptanceDate: string | null = null,
  acceptancePerson: string | null = null,
  releaseDate: string | null = null
) {
  try {
    const payload: any = {
      status: targetStatus,
      cancelReason: cancelReason
    }
    if (targetStatus === '4') {
      payload.acceptanceDate = acceptanceDate
      payload.acceptancePerson = acceptancePerson
    }
    if (targetStatus === '5') {
      payload.releaseDate = releaseDate
    }
    await request.patch(`/api/requirements/${req.id}/status`, payload)
    dispatchToast("需求状态流转成功!")
    await fetchRequirements()
  } catch (e) {
    // Handled by request interceptor
  }
}

function resetSearch() {
  if (searchTimer) clearTimeout(searchTimer)
  searchQuery.value = ""
  statusFilters.value = []
  filterCategoryId.value = null
  filterSubCategoryId.value = null
  startDate.value = ""
  endDate.value = ""
  tempStartDate.value = ""
  tempEndDate.value = ""
  currentPage.value = 1
}

// Link actions
function openLink(url: string) {
  window.open(url, '_blank')
}

// Modals logic: Create Requirement
function openCreateModal() {
  createForm.title = ""
  createForm.description = ""
  createForm.firstDemandDate = todayDateStr.value
  createForm.keyResultId = null
  createForm.categoryId = null
  createForm.subCategoryId = null
  isCreateModalOpen.value = true
}

function closeCreateModal() {
  isCreateModalOpen.value = false
}

async function submitCreate() {
  if (!createForm.title.trim()) return
  if (!createForm.categoryId) {
    dispatchToast("请选择需求主分类", "error")
    return
  }
  
  if (createForm.firstDemandDate && createForm.firstDemandDate > todayDateStr.value) {
    dispatchToast("首次需求时间不能是未来的时间", "error")
    return
  }
  
  try {
    const payload = {
      title: createForm.title.trim(),
      description: createForm.description.trim() || null,
      firstDemandDate: createForm.firstDemandDate || todayDateStr.value,
      keyResultId: createForm.keyResultId,
      categoryId: createForm.categoryId,
      subCategoryId: createForm.subCategoryId
    }
    await request.post('/api/requirements', payload)
    closeCreateModal()
    dispatchToast("新建需求成功!")
    await fetchRequirements()
  } catch (e) {
    // Handled by request interceptor
  }
}

// Modals logic: Edit Requirement
function openEditModal(req: Requirement) {
  editingReqId.value = req.id
  editForm.title = req.title
  editForm.description = req.description || ""
  editForm.firstDemandDate = req.firstDemandDate || todayDateStr.value
  editForm.keyResultId = req.keyResultId
  editForm.categoryId = req.categoryId || null
  editForm.subCategoryId = req.subCategoryId || null
  isKRReadOnlyInEdit.value = req.status === '4' || req.status === '5'
  isEditModalOpen.value = true
}

function closeEditModal() {
  isEditModalOpen.value = false
  editingReqId.value = null
}

async function submitEdit() {
  if (!editingReqId.value || !editForm.title.trim()) return
  if (!editForm.categoryId) {
    dispatchToast("请选择需求主分类", "error")
    return
  }
  
  if (editForm.firstDemandDate && editForm.firstDemandDate > todayDateStr.value) {
    dispatchToast("首次需求时间不能是未来的时间", "error")
    return
  }
  
  try {
    const payload = {
      title: editForm.title.trim(),
      description: editForm.description.trim() || null,
      firstDemandDate: editForm.firstDemandDate || todayDateStr.value,
      keyResultId: editForm.keyResultId,
      categoryId: editForm.categoryId,
      subCategoryId: editForm.subCategoryId
    }
    await request.put(`/api/requirements/${editingReqId.value}`, payload)
    closeEditModal()
    dispatchToast("需求更新成功!")
    await fetchRequirements()
  } catch (e) {
    // Handled by request interceptor
  }
}

// Delete logic
function confirmDelete(req: Requirement) {
  deletingRequirement.value = req
  isDeleteModalOpen.value = true
}

function closeDeleteModal() {
  isDeleteModalOpen.value = false
  deletingRequirement.value = null
}

async function executeDelete() {
  const req = deletingRequirement.value
  if (req) {
    try {
      await request.delete(`/api/requirements/${req.id}`)
      dispatchToast("需求已成功删除")
      await fetchRequirements()
    } catch (e) {
      // Handled by request interceptor
    }
  }
  closeDeleteModal()
}

// Modals logic: Add Link
function openAddLinkModal(req: Requirement) {
  linkFormReqId.value = req.id
  linkForm.title = ""
  linkForm.url = ""
  linkForm.type = "2"
  linkForm.date = todayDateStr.value
  isLinkTypeDropdownOpen.value = false
  isLinkModalOpen.value = true
}

function closeLinkModal() {
  isLinkModalOpen.value = false
  isLinkTypeDropdownOpen.value = false
  linkFormReqId.value = null
}

async function submitAddLink() {
  if (!linkFormReqId.value || !linkForm.title.trim() || !linkForm.url.trim()) return

  const trimmedUrl = linkForm.url.trim()
  if (!/^https?:\/\/\S+/i.test(trimmedUrl)) {
    dispatchToast("文档链接格式不正确，必须以 http:// 或 https:// 开头", "error")
    return
  }

  try {
    const payload = {
      type: linkForm.type,
      title: linkForm.title.trim(),
      url: trimmedUrl,
      documentDate: linkForm.date
    }
    await request.post(`/api/requirements/${linkFormReqId.value}/documents`, payload)
    closeLinkModal()
    dispatchToast("关联文档链接添加成功!")
    await fetchRequirements()
  } catch (e) {
    // Handled by request interceptor
  }
}

function confirmDeleteDocument(req: Requirement, doc: RequirementDocument) {
  deletingDocReq.value = req
  deletingDocId.value = doc.id
  deletingDocTitle.value = doc.title
  isDeleteDocModalOpen.value = true
}

function closeDeleteDocModal() {
  isDeleteDocModalOpen.value = false
  deletingDocReq.value = null
  deletingDocId.value = null
  deletingDocTitle.value = ""
}

async function executeDeleteDocument() {
  if (!deletingDocId.value) return
  try {
    await request.delete(`/api/requirement-documents/${deletingDocId.value}`)
    closeDeleteDocModal()
    dispatchToast("链接删除成功")
    await fetchRequirements()
  } catch (e) {
    // Handled by request interceptor
  }
}



async function unbindKeyResult(req: Requirement) {
  if (confirm("解绑关键成果后，需求将回归「讨论中」状态。确认解绑吗？")) {
    try {
      const payload = {
        title: req.title,
        description: req.description || null,
        firstDemandDate: req.firstDemandDate,
        keyResultId: null
      }
      await request.put(`/api/requirements/${req.id}`, payload)
      dispatchToast("已解除绑定，需求回归讨论中。")
      await fetchRequirements()
    } catch (e) {
      // Handled by request interceptor
    }
  }
}

// Cancel reason submit
function closeCancelModal() {
  isCancelModalOpen.value = false
  cancelReqId.value = null
  cancelReasonInput.value = ""
}

function submitCancelReason() {
  if (!cancelReqId.value || !cancelReasonInput.value.trim()) return

  const req = requirements.value.find(r => r.id === cancelReqId.value)
  if (req) {
    executeStatusTransition(req, '1', cancelReasonInput.value.trim())
  }

  closeCancelModal()
}



// Toast helper
function dispatchToast(msg: string, type: 'success' | 'error' = 'success') {
  const event = new CustomEvent('app-toast', { detail: { text: msg, type } })
  window.dispatchEvent(event)
}

// Modal drag-to-close prevention helper
let isMousedownOverlaySelf = false

function onMousedownOverlay(e: MouseEvent) {
  isMousedownOverlaySelf = e.target === e.currentTarget
}

function onMouseupOverlay(target: 'create' | 'edit' | 'link' | 'bind' | 'cancel' | 'select' | 'delete' | 'deleteDoc' | 'acceptance' | 'release') {
  if (isMousedownOverlaySelf) {
    if (target === 'create') closeCreateModal()
    else if (target === 'edit') closeEditModal()
    else if (target === 'link') closeLinkModal()
    else if (target === 'cancel') closeCancelModal()
    else if (target === 'select') closeSelectKRSubModal()
    else if (target === 'delete') closeDeleteModal()
    else if (target === 'deleteDoc') closeDeleteDocModal()
    else if (target === 'acceptance') closeAcceptanceModal()
    else if (target === 'release') closeReleaseModal()
  }
  isMousedownOverlaySelf = false
}
</script>

<style scoped>
.requirements-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 1100px;
  margin: 0 auto;
  padding-bottom: 40px;
}

/* Header summary card */
.requirements-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}

.title-section h1 {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-main);
  margin-bottom: 4px;
}

.title-section .subtitle {
  font-size: 14px;
  color: var(--text-muted);
}

.action-btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background-color: var(--primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  padding: 10px 18px;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  box-shadow: 0 4px 10px -2px rgba(79, 70, 229, 0.2);
  transition: all var(--transition-fast);
}

.action-btn-primary:hover {
  background-color: var(--primary-hover);
  transform: translateY(-1px);
  box-shadow: 0 6px 14px -2px rgba(79, 70, 229, 0.3);
}

.action-btn-primary:active {
  transform: translateY(0);
}

.action-btn-primary svg {
  width: 18px;
  height: 18px;
}

.action-btn-secondary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background-color: var(--bg-app);
  color: var(--text-main);
  border: 1px solid var(--border-medium);
  border-radius: var(--radius-md);
  padding: 8px 14px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.action-btn-secondary:hover {
  background-color: var(--border-light);
  border-color: #cbd5e1;
}

/* Filter Bar */
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  padding: 16px 24px;
  position: relative;
  z-index: 100;
}

.search-input-wrapper {
  position: relative;
  flex: 1;
  max-width: 480px;
}

.search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  width: 18px;
  height: 18px;
  color: var(--text-muted);
}

.search-input {
  width: 100%;
  padding: 9px 12px 9px 38px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-medium);
  background-color: #fff;
  font-size: 14px;
  transition: all var(--transition-fast);
  outline: none;
}

.search-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.select-wrapper {
  position: relative;
}

.status-select, .form-select {
  padding: 9px 36px 9px 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-medium);
  background-color: #fff;
  font-size: 14px;
  color: var(--text-main);
  appearance: none;
  cursor: pointer;
  outline: none;
  background-image: url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='%2364748b' stroke-width='2'%3e%3cpath stroke-linecap='round' stroke-linejoin='round' d='M19.5 8.25l-7.5 7.5-7.5-7.5'/%3e%3c/svg%3e");
  background-repeat: no-repeat;
  background-position: right 12px center;
  background-size: 14px;
  min-width: 150px;
  transition: all var(--transition-fast);
}

.status-select:focus, .form-select:focus {
  border-color: var(--primary);
}

.filter-btn svg {
  width: 16px;
  height: 16px;
  color: var(--text-muted);
}

/* More Filters Area */
.more-filters-panel {
  padding: 16px 24px;
  background-color: #f8fafc;
  border-color: var(--border-medium);
  margin-top: -8px;
}

.filters-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.filter-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-field label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
}

.filter-field select {
  width: 100%;
}

/* Collapsible transition */
.expand-enter-active, .expand-leave-active {
  transition: all 0.25s ease-out;
  overflow: hidden;
  max-height: 120px;
}
.expand-enter-from, .expand-leave-to {
  max-height: 0;
  opacity: 0;
  padding-top: 0 !important;
  padding-bottom: 0 !important;
  margin-top: 0 !important;
  border-top-width: 0 !important;
  border-bottom-width: 0 !important;
}

/* Card Lists */
.requirements-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px;
  text-align: center;
  color: var(--text-muted);
}

.empty-icon {
  width: 48px;
  height: 48px;
  color: #94a3b8;
  margin-bottom: 12px;
}

.empty-state h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-main);
  margin-bottom: 6px;
}

.requirement-card {
  border-left: 5px solid transparent;
  padding: 24px 28px;
}

.card-layout {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
}

.card-main {
  flex: 1;
  min-width: 0;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.requirement-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-main);
  margin: 0;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 600;
  border: 1px solid transparent;
}

.requirement-desc {
  font-size: 14px;
  color: var(--text-muted);
  line-height: 1.6;
  margin-bottom: 16px;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.info-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-muted);
}

.info-item svg {
  width: 14px;
  height: 14px;
  color: #94a3b8;
}

.info-item.cancel-reason {
  background-color: #fee2e2;
  color: #b91c1c;
  padding: 2px 8px;
  border-radius: 4px;
}

/* Card Actions styling */
.card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.card-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background-color: transparent;
  border: 1px solid var(--border-medium);
  border-radius: 6px;
  padding: 6px 12px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-main);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.card-action-btn svg {
  width: 14px;
  height: 14px;
}

.btn-add-link {
  border-color: #cbd5e1;
  color: var(--primary);
}

.btn-add-link:hover {
  background-color: var(--primary-light);
  border-color: var(--primary);
}

.btn-status-change {
  border-color: #cbd5e1;
  color: #0d9488; /* teal */
}

.btn-status-change:hover:not(.btn-disabled) {
  background-color: #f0fdfa;
  border-color: #0d9488;
}

.btn-disabled {
  opacity: 0.55;
  cursor: not-allowed;
  background-color: #f1f5f9;
  border-color: var(--border-medium);
  color: var(--text-muted);
}

.btn-edit {
  color: var(--text-muted);
}

.btn-edit:hover {
  background-color: #f1f5f9;
  color: var(--text-main);
}

.btn-delete {
  color: var(--danger);
  border-color: #fca5a5;
}

.btn-delete:hover {
  background-color: var(--danger-bg);
  border-color: var(--danger);
}

/* Card un-involved style override */
.req-not-involved .requirement-title {
  color: #94a3b8;
  text-decoration: line-through;
}

.req-not-involved .requirement-desc {
  color: #94a3b8;
}

.req-released {
  opacity: 0.95;
}

.req-released .requirement-card {
  background-color: #fafafa;
}

/* Links Section */
.links-section {
  border-top: 1px dashed var(--border-medium);
  margin-top: 16px;
  padding-top: 16px;
}

.links-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-muted);
  margin-bottom: 12px;
}

.links-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.link-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  background-color: #f8fafc;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 10px 14px;
  cursor: pointer;
  position: relative;
  transition: all var(--transition-fast);
}

.link-chip:hover {
  background-color: #f1f5f9;
  border-color: var(--border-medium);
  transform: translateY(-1px);
}

.link-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  flex-shrink: 0;
}

.link-svg-icon {
  width: 14px;
  height: 14px;
}

.link-content {
  flex: 1;
  min-width: 0;
}

.link-title-text {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-main);
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.link-url-text {
  font-size: 11px;
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.remove-link-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  background-color: transparent;
  border: none;
  border-radius: var(--radius-full);
  color: var(--text-muted);
  cursor: pointer;
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%) scale(0.9);
  opacity: 0;
  transition: all var(--transition-fast);
}

.link-chip:hover .remove-link-btn {
  opacity: 1;
  transform: translateY(-50%) scale(1);
}

.remove-link-btn:hover {
  background-color: #e2e8f0;
  color: var(--danger);
}

.remove-link-btn svg {
  width: 12px;
  height: 12px;
}

/* KR Bound pill styling */
.kr-binding-section {
  margin-top: 4px;
}

.kr-pill-bound {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background-color: #eff6ff;
  border: 1px solid #bfdbfe;
  color: #1e40af;
  font-size: 13px;
  padding: 6px 12px;
  border-radius: var(--radius-md);
}

.kr-pill-bound strong {
  font-weight: 600;
}

.kr-pill-bound .kr-status {
  font-size: 11px;
  background-color: #dbeafe;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 500;
}

.kr-status-0 { color: #3b82f6; }
.kr-status-1 { color: #f59e0b; }
.kr-status-2 { color: #10b981; }
.kr-status-3 { color: #ef4444; }

.unbind-btn {
  background: transparent;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #1e40af;
  opacity: 0.6;
  padding: 2px;
  border-radius: 4px;
  transition: all var(--transition-fast);
}

.unbind-btn:hover {
  background-color: #dbeafe;
  color: var(--danger);
  opacity: 1;
}

.unbind-btn svg {
  width: 14px;
  height: 14px;
}

.kr-pill-unbound {
  display: inline-block;
}

.bind-kr-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background-color: transparent;
  border: 1px dashed var(--border-medium);
  color: var(--text-muted);
  font-size: 12px;
  padding: 5px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.bind-kr-action-btn:hover {
  background-color: #f8fafc;
  color: var(--primary);
  border-color: var(--primary);
  border-style: solid;
}

.bind-kr-action-btn svg {
  width: 12px;
  height: 12px;
}

/* Status transition popover dropdown */
.status-transition-container {
  position: relative;
}

.status-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 180px;
  z-index: 1000;
  padding: 10px;
  background-color: #fff;
  border-color: var(--border-medium);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -4px rgba(0, 0, 0, 0.05);
}

.dropdown-header {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  color: var(--text-muted);
  letter-spacing: 0.05em;
  padding: 4px 8px;
  border-bottom: 1px solid var(--border-light);
  margin-bottom: 6px;
}

.dropdown-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  background-color: transparent;
  border: none;
  padding: 8px 10px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  color: var(--text-main);
  text-align: left;
  cursor: pointer;
  transition: background-color var(--transition-fast);
}

.dropdown-item:hover {
  background-color: #f8fafc;
}

.dropdown-item .dot {
  width: 8px;
  height: 8px;
  border-radius: var(--radius-full);
}

.dropdown-item .lbl {
  flex: 1;
}

.dropdown-item .warn-badge {
  font-size: 9px;
  background-color: #fee2e2;
  color: #b91c1c;
  padding: 1px 4px;
  border-radius: 4px;
  font-weight: 600;
}

.dropdown-empty {
  font-size: 12px;
  color: var(--text-muted);
  padding: 8px;
  text-align: center;
}

/* Modal form adjustments */
.modal-body-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.bind-notice {
  font-size: 13px;
  color: var(--text-muted);
}

.bind-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px;
  text-align: center;
  background-color: #f8fafc;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  color: var(--text-muted);
  gap: 8px;
}

.bind-empty .icon {
  font-size: 24px;
}

.bind-krs-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 260px;
  overflow-y: auto;
  padding-right: 4px;
}

.bind-kr-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  border: 1px solid var(--border-medium);
  border-radius: var(--radius-md);
  padding: 12px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.bind-kr-item:hover {
  border-color: var(--primary);
  background-color: #f8fafc;
}

.bind-kr-item.selected {
  border-color: var(--primary);
  background-color: var(--primary-light);
}

.radio-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border: 2px solid var(--border-medium);
  border-radius: var(--radius-full);
  margin-top: 2px;
  flex-shrink: 0;
  background-color: #fff;
}

.bind-kr-item.selected .radio-indicator {
  border-color: var(--primary);
}

.radio-indicator .dot {
  width: 8px;
  height: 8px;
  background-color: var(--primary);
  border-radius: var(--radius-full);
}

.kr-info {
  flex: 1;
  min-width: 0;
}

.kr-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.kr-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 11px;
}

.kr-badge-status {
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 600;
}

.kr-badge-status.status-0 { background-color: #e0f2fe; color: #0369a1; }
.kr-badge-status.status-1 { background-color: #fef3c7; color: #b45309; }
.kr-badge-status.status-2 { background-color: #d1fae5; color: #047857; }

.kr-due {
  color: var(--text-muted);
}

/* Popover Fade Animation */
.popover-fade-enter-active, .popover-fade-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}
.popover-fade-enter-from, .popover-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* Modals global configuration */
.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
}

.form-input, .form-textarea {
  width: 100%;
  padding: 10px 14px;
  border-radius: var(--radius-md);
  border: 1.5px solid var(--border-medium);
  background-color: #fff;
  font-size: 14px;
  outline: none;
  transition: all var(--transition-fast);
}

select.form-input {
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  background-image: url("data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='rgba(15, 23, 42, 0.6)' stroke-width='2.5'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' d='M19.5 8.25l-7.5 7.5-7.5-7.5'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 14px center;
  background-size: 14px;
  padding-right: 40px;
  cursor: pointer;
}

select.form-input:disabled {
  background-color: var(--bg-app);
  cursor: not-allowed;
  opacity: 0.65;
}

.form-input:focus, .form-textarea:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.form-textarea {
  resize: vertical;
}

.close-modal-btn {
  background: transparent;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  padding: 4px;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}

.close-modal-btn:hover {
  background-color: var(--border-light);
  color: var(--text-main);
}

.close-modal-btn svg {
  width: 20px;
  height: 20px;
}

.modal-actions button {
  min-width: 80px;
}

/* Beautiful Select Trigger Styles matching SearchView */
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

/* Beautiful Date Input Styles matching CategoryView */
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

/* Modal Styles copied from CategoryView/SprintView */
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
  margin: 0;
}

.modal-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
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

.fetching-dot-animation {
  display: inline-block;
  animation: pulse 1s infinite alternate;
}
@keyframes pulse {
  from { opacity: 0.3; transform: scale(0.8); }
  to { opacity: 1; transform: scale(1.2); }
}
@keyframes shimmer {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}
</style>

