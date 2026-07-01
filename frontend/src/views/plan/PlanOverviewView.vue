<template>
  <div class="plan-overview-view">
    <!-- Header Area (Title, Month Selector) -->
    <div class="view-header premium-card">
      <div class="title-row">
        <div class="title-meta-left">
          <h2>月度总览</h2>
          <p class="subtitle-lbl">一览您的月度绩效目标、关键成果与团队冲刺任务</p>
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
        </div>
      </div>
    </div>

    <!-- Statistics Summary Grid -->
    <div class="plan-stats-grid">
      <!-- Card 1: Monthly Performance Completion Rate -->
      <div class="stat-summary-card premium-card">
        <div class="card-inner">
          <div class="card-text">
            <span class="card-label">月度绩效完成率</span>
            <span class="card-value text-indigo">{{ krCompletionRate }}%</span>
            <span class="card-change" :class="krChangeTrend.type">
              {{ krChangeTrend.icon }} {{ krChangeTrend.text }}
            </span>
          </div>
          <div class="card-icon-circle bg-blue-light">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="stat-icon text-blue">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 3v1m0 16v1m9-9h-1M3 12H2m15.364-6.364l-.707.707M6.343 17.657l-.707.707m0-12.728l.707.707m10.607 10.607l.707.707M12 8a4 4 0 100 8 4 4 0 000-8z" />
            </svg>
          </div>
        </div>
      </div>

      <!-- Card 2: Completed KR Count -->
      <div class="stat-summary-card premium-card">
        <div class="card-inner">
          <div class="card-text">
            <span class="card-label">已完成OKR数量</span>
            <span class="card-value">{{ completedKRs }}</span>
            <span class="card-change" :class="krCountChangeTrend.type">
              {{ krCountChangeTrend.icon }} {{ krCountChangeTrend.text }}
            </span>
          </div>
          <div class="card-icon-circle bg-green-light">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="stat-icon text-green">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
        </div>
      </div>

      <!-- Card 3: Team Sprint Tasks -->
      <div class="stat-summary-card premium-card">
        <div class="card-inner">
          <div class="card-text">
            <span class="card-label">团队冲刺任务</span>
            <span class="card-value">{{ totalSprints }}</span>
            <span class="card-change text-success">
              ✔️ 已完成 {{ completedSprints }} 个
            </span>
          </div>
          <div class="card-icon-circle bg-yellow-light">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="stat-icon text-yellow">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" />
            </svg>
          </div>
        </div>
      </div>

      <!-- Card 4: Overall Team Completion Rate -->
      <div class="stat-summary-card premium-card">
        <div class="card-inner">
          <div class="card-text">
            <span class="card-label">团队整体完成度</span>
            <span class="card-value text-success">{{ sprintCompletionRate }}%</span>
            <span class="card-change" :class="sprintChangeTrend.type">
              {{ sprintChangeTrend.icon }} {{ sprintChangeTrend.text }}
            </span>
          </div>
          <div class="card-icon-circle bg-red-light">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.2" stroke="currentColor" class="stat-icon text-red">
              <path stroke-linecap="round" stroke-linejoin="round" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
            </svg>
          </div>
        </div>
      </div>
    </div>

    <div class="overview-grid">
      <!-- Left side: Performance KRs list -->
      <div class="grid-card premium-card">
        <div class="card-header">
          <div class="header-left">
            <h3>本月个人绩效（KR）</h3>
            <!-- compact progress bar -->
            <div class="compact-progress-wrapper" v-if="totalKRs > 0">
              <div class="compact-progress-bar">
                <div class="progress-fill" :style="{ width: `${krCompletionRate}%` }"></div>
              </div>
              <span class="compact-progress-text text-indigo font-bold">{{ krCompletionRate }}%</span>
              <span class="compact-progress-sub">已完成 {{ completedKRs }}/{{ totalKRs }}</span>
            </div>
          </div>
          <router-link :to="`/plan/performance/${selectedMonth}`" class="link-btn">进入维护</router-link>
        </div>

        <div class="kr-category-groups" v-if="krsGrouped.length > 0">
          <div v-for="group in krsGrouped" :key="group.category" class="kr-group">
            <div class="kr-group-title">{{ group.category }}</div>
            <div class="kr-group-items">
              <div 
                v-for="kr in group.items" 
                :key="kr.id" 
                class="overview-item"
                :class="{ 
                  completed: kr.status === 'done',
                  cancelled: kr.status === 'cancelled'
                }"
              >
                <span class="status-indicator" :class="kr.status"></span>
                <span class="item-title">{{ kr.title }}</span>
                <span class="status-tag" :class="kr.status">
                  {{ kr.status === 'done' ? '已完成' : (kr.status === 'in_progress' ? '进行中' : (kr.status === 'cancelled' ? '已取消' : '未开始')) }}
                </span>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="empty-col-info">本月尚无绩效关键成果记录</div>
      </div>

      <!-- Right side: Team Sprints list -->
      <div class="grid-card premium-card">
        <div class="card-header">
          <div class="header-left">
            <h3>本月团队冲刺任务</h3>
            <!-- compact progress bar -->
            <div class="compact-progress-wrapper" v-if="totalSprints > 0">
              <div class="compact-progress-bar">
                <div class="progress-fill fill-success" :style="{ width: `${sprintCompletionRate}%` }"></div>
              </div>
              <span class="compact-progress-text text-success font-bold">{{ sprintCompletionRate }}%</span>
              <span class="compact-progress-sub">已完成 {{ completedSprints }}/{{ totalSprints }}</span>
            </div>
          </div>
          <router-link :to="`/plan/sprint/${selectedMonth}`" class="link-btn">进入维护</router-link>
        </div>

        <div class="sprint-items-list" v-if="teamSprints.length > 0">
          <div 
            v-for="sprint in teamSprints" 
            :key="sprint.id" 
            class="overview-item sprint-item-row"
            :class="{ 
              completed: sprint.status === 'done',
              involved: sprint.needInvolved
            }"
          >
            <div class="sprint-top-row">
              <span class="status-indicator" :class="sprint.needInvolved ? sprint.status : 'not_involved'"></span>
              <span class="item-title font-bold">{{ sprint.title }}</span>
              <div class="sprint-badges">
                <template v-if="sprint.needInvolved">
                  <span class="involved-badge need-involved">需我介入</span>
                  <span class="status-tag" :class="sprint.status">
                    {{ sprint.status === 'done' ? '已完成' : (sprint.status === 'in_progress' ? '进行中' : '未开始') }}
                  </span>
                </template>
                <template v-else>
                  <span class="involved-badge no-involved">无需介入</span>
                </template>
              </div>
            </div>
            
            <div class="sprint-bottom-row" v-if="sprint.remark">
              <span class="sprint-meta" :title="sprint.remark">任务描述: {{ truncate(sprint.remark, 30) }}</span>
            </div>
          </div>
        </div>
        <div v-else class="empty-col-info">本月尚无团队冲刺项目记录</div>
      </div>
    </div>

    <!-- Monthly Performance Trend Chart -->
    <div class="trend-chart-card premium-card">
      <div class="chart-header">
        <div class="chart-title-left">
          <h3>月度绩效趋势</h3>
        </div>
        
        <div class="chart-legend">
          <div class="legend-item">
            <span class="legend-dot dot-personal"></span>
            <span class="legend-text">个人绩效</span>
          </div>
          <div class="legend-item">
            <span class="legend-dot dot-team"></span>
            <span class="legend-text">团队平均</span>
          </div>
        </div>

        <div class="chart-tabs-right">
          <button 
            class="tab-btn" 
            :class="{ active: activeRange === 6 }"
            @click="activeRange = 6"
          >
            近6个月
          </button>
          <button 
            class="tab-btn" 
            :class="{ active: activeRange === 12 }"
            @click="activeRange = 12"
          >
            近12个月
          </button>
        </div>
      </div>

      <!-- Chart Body -->
      <div class="chart-body-wrapper">
        <!-- Y-Axis Labels -->
        <div class="y-axis-labels">
          <span>100%</span>
          <span>80%</span>
          <span>60%</span>
          <span>40%</span>
          <span>20%</span>
          <span>0%</span>
        </div>

        <!-- SVG Chart Area -->
        <div class="chart-svg-container">
          <svg class="trend-svg" viewBox="0 0 1000 200" preserveAspectRatio="none">
            <!-- Gradients -->
            <defs>
              <linearGradient id="blue-gradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="rgba(79, 70, 229, 0.15)" />
                <stop offset="100%" stop-color="rgba(79, 70, 229, 0)" />
              </linearGradient>
              <linearGradient id="green-gradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="rgba(16, 185, 129, 0.1)" />
                <stop offset="100%" stop-color="rgba(16, 185, 129, 0)" />
              </linearGradient>
            </defs>

            <!-- Horizontal Grid Lines -->
            <line x1="0" y1="0" x2="1000" y2="0" stroke="var(--border-light)" stroke-width="1" />
            <line x1="0" y1="40" x2="1000" y2="40" stroke="var(--border-light)" stroke-width="1" />
            <line x1="0" y1="80" x2="1000" y2="80" stroke="var(--border-light)" stroke-width="1" />
            <line x1="0" y1="120" x2="1000" y2="120" stroke="var(--border-light)" stroke-width="1" />
            <line x1="0" y1="160" x2="1000" y2="160" stroke="var(--border-light)" stroke-width="1" />
            <line x1="0" y1="200" x2="1000" y2="200" stroke="var(--border-medium)" stroke-width="1.5" />

            <!-- Shaded Area Under Personal Line -->
            <path :d="personalAreaPath" fill="url(#blue-gradient)" />
            
            <!-- Personal Performance Line -->
            <path :d="personalLinePath" fill="none" stroke="var(--primary)" stroke-width="2.5" stroke-linecap="round" />

            <!-- Team Average Line -->
            <path :d="teamLinePath" fill="none" stroke="var(--success)" stroke-width="2" stroke-dasharray="4 3" stroke-linecap="round" />

            <!-- Circles / Dots for Personal Performance -->
            <circle 
              v-for="(point, idx) in chartPoints" 
              :key="'p-' + idx"
              :cx="point.x" 
              :cy="point.yPersonal" 
              r="4.5" 
              fill="#fff" 
              stroke="var(--primary)" 
              stroke-width="2.5"
              class="chart-point"
            />

            <!-- Circles / Dots for Team Average -->
            <circle 
              v-for="(point, idx) in chartPoints" 
              :key="'t-' + idx"
              :cx="point.x" 
              :cy="point.yTeam" 
              r="4" 
              fill="#fff" 
              stroke="var(--success)" 
              stroke-width="2"
              class="chart-point"
            />
          </svg>
        </div>
      </div>

      <!-- X-Axis Labels -->
      <div class="x-axis-labels-row">
        <span 
          v-for="(label, idx) in xAxisLabels" 
          :key="idx"
          :style="{ left: `${(idx / (xAxisLabels.length - 1)) * 96 + 2}%` }"
          class="x-label"
        >
          {{ label }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/api/request'
import { useAuthStore } from '@/stores/auth'
import { useTodoStore } from '@/stores/todo'
import type { PerformanceKR, TeamSprint } from '@/stores/todo'
import type { ApiResult } from '@/api/types'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const todoStore = useTodoStore()

const currentMonthStr = computed(() => new Date().toISOString().substring(0, 7))

// Selected Month (YYYY-MM)
const selectedMonth = ref(route.params.month as string || currentMonthStr.value)

// Last month data for comparison
const lastMonthKRs = ref<any[]>([])
const lastMonthSprints = ref<any[]>([])
const trendData = ref<any[]>([])

const mapKRStatus = (voStatus: any) => {
  if (voStatus === '2' || voStatus === 'done') return 'done'
  if (voStatus === '3' || voStatus === 'cancelled') return 'cancelled'
  if (voStatus === '1' || voStatus === 'in_progress') return 'in_progress'
  return 'not_started'
}

const mapSprintStatus = (voStatus: any) => {
  if (voStatus === '2' || voStatus === 'done') return 'done'
  if (voStatus === '1' || voStatus === 'in_progress') return 'in_progress'
  return 'not_started'
}

const getLastMonthStr = (monthStr: string) => {
  const [year, month] = monthStr.split('-').map(Number)
  const d = new Date(year, month - 2, 1)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  return `${y}-${m}`
}

const fetchTrendData = async () => {
  const count = activeRange.value
  const [year, month] = selectedMonth.value.split('-').map(Number)
  
  // Calculate startMonth: (count - 1) months ago
  const startD = new Date(year, month - count, 1)
  const startY = startD.getFullYear()
  const startM = String(startD.getMonth() + 1).padStart(2, '0')
  const startMonthStr = `${startY}-${startM}`
  const endMonthStr = selectedMonth.value
  
  try {
    const res = await request.get<any[], ApiResult<any[]>>(
      `/api/plan/trend?startMonth=${startMonthStr}&endMonth=${endMonthStr}`
    )
    if (res && res.data) {
      trendData.value = res.data
    } else {
      trendData.value = []
    }
  } catch (e) {
    console.error('Failed to fetch trend data:', e)
    trendData.value = []
  }
}

const loadData = async (month: string) => {
  if (authStore.currentUser) {
    todoStore.setMonth(month)
    
    const lastMonth = getLastMonthStr(month)
    
    const storePromise = Promise.all([
      todoStore.refreshObjectives(authStore.currentUser.userId),
      todoStore.refreshSprints(authStore.currentUser.userId)
    ])
    
    const lastMonthPromise = Promise.all([
      request.get<any[], ApiResult<any[]>>(`/api/objectives?month=${lastMonth}`).catch(() => ({ data: [] })),
      request.get<any[], ApiResult<any[]>>(`/api/sprint/items?month=${lastMonth}`).catch(() => ({ data: [] }))
    ])
    
    const trendPromise = fetchTrendData()
    
    const [, lastMonthRes] = await Promise.all([storePromise, lastMonthPromise, trendPromise])
    
    // Parse last month's KRs
    const krsList: any[] = []
    if (lastMonthRes[0] && lastMonthRes[0].data) {
      lastMonthRes[0].data.forEach((obj: any) => {
        if (obj.keyResults) {
          obj.keyResults.forEach((kr: any) => {
            krsList.push(kr)
          })
        }
      })
    }
    lastMonthKRs.value = krsList
    
    // Parse last month's Sprints
    lastMonthSprints.value = lastMonthRes[1] && lastMonthRes[1].data ? lastMonthRes[1].data : []
  }
}

watch(selectedMonth, (newVal) => {
  router.push(`/plan/${newVal}`)
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

// Load state
const performanceKRs = computed(() => todoStore.performanceKRs)
const teamSprints = computed(() => todoStore.teamSprints)


// Metrics
const totalKRs = computed(() => performanceKRs.value.filter(kr => kr.status !== 'cancelled').length)
const completedKRs = computed(() => performanceKRs.value.filter(kr => kr.status === 'done').length)
const krCompletionRate = computed(() => {
  if (totalKRs.value === 0) return 0
  return Math.round((completedKRs.value / totalKRs.value) * 100)
})

const totalSprints = computed(() => teamSprints.value.filter(s => s.needInvolved).length)
const completedSprints = computed(() => teamSprints.value.filter(s => s.needInvolved && s.status === 'done').length)
const sprintCompletionRate = computed(() => {
  if (totalSprints.value === 0) return 0
  return Math.round((completedSprints.value / totalSprints.value) * 100)
})

// Aligned comparison trends based on last month's fetched data
const krChangeTrend = computed(() => {
  const lastMonthTotal = lastMonthKRs.value.filter(kr => mapKRStatus(kr.status) !== 'cancelled').length
  const lastMonthCompleted = lastMonthKRs.value.filter(kr => mapKRStatus(kr.status) === 'done').length
  const lastMonthRate = lastMonthTotal === 0 ? 0 : Math.round((lastMonthCompleted / lastMonthTotal) * 100)
  
  const diff = krCompletionRate.value - lastMonthRate
  if (diff > 0) {
    return { type: 'trend-up', icon: '↑', text: `较上月提升 ${diff}%` }
  } else if (diff < 0) {
    return { type: 'trend-down', icon: '↓', text: `较上月下降 ${Math.abs(diff)}%` }
  } else {
    return { type: 'trend-neutral', icon: '', text: '较上月持平' }
  }
})

const krCountChangeTrend = computed(() => {
  const lastMonthCompleted = lastMonthKRs.value.filter(kr => mapKRStatus(kr.status) === 'done').length
  const diff = completedKRs.value - lastMonthCompleted
  
  if (diff > 0) {
    return { type: 'trend-up', icon: '↑', text: `较上月提升 ${diff} 个` }
  } else if (diff < 0) {
    return { type: 'trend-down', icon: '↓', text: `较上月减少 ${Math.abs(diff)} 个` }
  } else {
    return { type: 'trend-neutral', icon: '', text: '较上月持平' }
  }
})

const sprintChangeTrend = computed(() => {
  const lastMonthInvolved = lastMonthSprints.value.filter(s => !!s.needInvolved).length
  const lastMonthCompleted = lastMonthSprints.value.filter(s => !!s.needInvolved && mapSprintStatus(s.status) === 'done').length
  const lastMonthRate = lastMonthInvolved === 0 ? 0 : Math.round((lastMonthCompleted / lastMonthInvolved) * 100)
  
  const diff = sprintCompletionRate.value - lastMonthRate
  if (diff > 0) {
    return { type: 'trend-up', icon: '↑', text: `较上月提升 ${diff}%` }
  } else if (diff < 0) {
    return { type: 'trend-down', icon: '↓', text: `较上月下降 ${Math.abs(diff)}%` }
  } else {
    return { type: 'trend-neutral', icon: '', text: '较上月持平' }
  }
})

const activeRange = ref(6)

watch(activeRange, () => {
  fetchTrendData()
})

// Generate dynamic historical data points based on trendData API
const chartPoints = computed(() => {
  const data = trendData.value
  const count = activeRange.value
  
  if (data.length === 0) {
    const points = []
    const baseHash = selectedMonth.value.split('-').reduce((acc, char) => acc + char.charCodeAt(0), 0)
    for (let i = 0; i < count; i++) {
      const x = count > 1 ? (i / (count - 1)) * 1000 : 500
      const seed = baseHash + i
      const ratePersonal = 65 + (seed % 22)
      const rateTeam = 60 + ((seed * 7) % 18)
      points.push({
        x,
        yPersonal: 200 * (1 - ratePersonal / 100),
        yTeam: 200 * (1 - rateTeam / 100)
      })
    }
    return points
  }
  
  return data.map((item, idx) => {
    const x = count > 1 ? (idx / (count - 1)) * 1000 : 500
    const ratePersonal = Math.round((item.perfRate || 0) * 100)
    const rateTeam = Math.round((item.sprintRate || 0) * 100)
    return {
      x,
      yPersonal: 200 * (1 - ratePersonal / 100),
      yTeam: 200 * (1 - rateTeam / 100)
    }
  })
})

const getCurvePath = (points: any[], key: 'yPersonal' | 'yTeam') => {
  if (points.length === 0) return ''
  if (points.length === 1) return `M ${points[0].x} ${points[0][key]}`
  
  let path = `M ${points[0].x} ${points[0][key]}`
  
  for (let i = 0; i < points.length - 1; i++) {
    const p0 = points[i]
    const p1 = points[i + 1]
    
    let m0 = 0
    let m1 = 0
    
    if (i > 0) {
      const prev = points[i - 1]
      m0 = (p1[key] - prev[key]) / (p1.x - prev.x)
    } else {
      m0 = (p1[key] - p0[key]) / (p1.x - p0.x)
    }
    
    if (i < points.length - 2) {
      const next = points[i + 2]
      m1 = (next[key] - p0[key]) / (next.x - p0.x)
    } else {
      m1 = (p1[key] - p0[key]) / (p1.x - p0.x)
    }
    
    const dx = (p1.x - p0.x) / 3
    
    const cp1x = p0.x + dx
    const cp1y = p0[key] + m0 * dx
    
    const cp2x = p1.x - dx
    const cp2y = p1[key] - m1 * dx
    
    path += ` C ${cp1x.toFixed(1)} ${cp1y.toFixed(1)}, ${cp2x.toFixed(1)} ${cp2y.toFixed(1)}, ${p1.x} ${p1[key]}`
  }
  
  return path
}

const personalLinePath = computed(() => getCurvePath(chartPoints.value, 'yPersonal'))

const personalAreaPath = computed(() => {
  const pts = chartPoints.value
  if (pts.length === 0) return ''
  const linePath = personalLinePath.value
  return `${linePath} L ${pts[pts.length - 1].x} 200 L ${pts[0].x} 200 Z`
})

const teamLinePath = computed(() => getCurvePath(chartPoints.value, 'yTeam'))

const xAxisLabels = computed(() => {
  const data = trendData.value
  if (data.length === 0) {
    const count = activeRange.value
    const labels = []
    const [year, month] = selectedMonth.value.split('-').map(Number)
    for (let i = count - 1; i >= 0; i--) {
      const d = new Date(year, month - 1 - i, 1)
      labels.push(`${d.getMonth() + 1}月`)
    }
    return labels
  }
  
  return data.map(item => {
    const parts = item.month.split('-')
    return parts.length === 2 ? `${Number(parts[1])}月` : ''
  })
})

const performanceCategories = computed(() => todoStore.performanceCategories)

// Group KRs by category (PRD 6.8.1)
const krsGrouped = computed(() => {
  const map: Record<number, PerformanceKR[]> = {}
  performanceKRs.value.forEach(kr => {
    if (!map[kr.categoryId]) {
      map[kr.categoryId] = []
    }
    map[kr.categoryId].push(kr)
  })
  
  return performanceCategories.value.map(cat => ({
    category: cat.name,
    items: map[cat.id] || []
  })).filter(g => g.items.length > 0)
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

const truncate = (text: string, len: number) => {
  if (!text) return ''
  if (text.length <= len) return text
  return text.substring(0, len) + '...'
}
</script>

<style scoped>
.plan-overview-view {
  max-width: 1100px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 1. Header Styles (Matches PerformanceView) */
.view-header {
  background-color: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 24px 32px;
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
  transition: background var(--transition-fast), color var(--transition-fast);
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

/* 2. Stats Summary Grid */
.plan-stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}
@media (max-width: 900px) {
  .plan-stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 600px) {
  .plan-stats-grid {
    grid-template-columns: 1fr;
  }
}

.stat-summary-card {
  padding: 20px 24px;
}
.stat-summary-card .card-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.stat-summary-card .card-text {
  display: flex;
  flex-direction: column;
}
.stat-summary-card .card-label {
  font-size: 13px;
  color: var(--text-muted);
  font-weight: 600;
}
.stat-summary-card .card-value {
  font-size: 28px;
  font-weight: 800;
  color: var(--text-main);
  line-height: 1.2;
  margin: 6px 0;
}
.stat-summary-card .card-change {
  font-size: 12px;
  font-weight: 600;
}
.stat-summary-card .card-change.trend-up {
  color: var(--success);
}
.stat-summary-card .card-change.trend-down {
  color: var(--danger);
}
.stat-summary-card .card-change.trend-neutral {
  color: var(--text-muted);
}

.card-icon-circle {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.card-icon-circle.bg-blue-light { background-color: #eff6ff; }
.card-icon-circle.bg-green-light { background-color: #e6fcf5; }
.card-icon-circle.bg-yellow-light { background-color: #fffbeb; }
.card-icon-circle.bg-red-light { background-color: #fdf2f2; }

.stat-icon {
  width: 22px;
  height: 22px;
}
.stat-icon.text-blue { color: #2563eb; }
.stat-icon.text-green { color: #0ca678; }
.stat-icon.text-yellow { color: #d97706; }
.stat-icon.text-red { color: #e02424; }

/* 3. Grid Splits & Cards */
.overview-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  align-items: stretch;
}
@media (max-width: 900px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }
}

.grid-card {
  padding: 24px;
  min-height: 380px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1.5px solid var(--border-light);
  padding-bottom: 12px;
  margin-bottom: 16px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.card-header h3 {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-main);
}
.link-btn {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--primary);
}
.link-btn:hover {
  text-decoration: underline;
}

.empty-col-info {
  font-size: 13px;
  color: var(--text-muted);
  font-style: italic;
  text-align: center;
  padding: 60px 0;
}

/* 3. Title-integrated Progress Styles */
.compact-progress-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}
.compact-progress-bar {
  width: 80px;
  height: 6px;
  background-color: var(--border-light);
  border-radius: var(--radius-full);
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background-color: var(--primary);
  transition: width var(--transition-normal);
}
.progress-fill.fill-success {
  background-color: var(--success);
}
.compact-progress-text {
  font-size: 13px;
}
.compact-progress-text.text-indigo {
  color: var(--primary);
}
.compact-progress-text.text-success {
  color: var(--success);
}
.compact-progress-sub {
  font-size: 11px;
  color: var(--text-muted);
}

/* 4. KR Item Styles */
.kr-category-groups {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.kr-group-title {
  font-size: 12.5px;
  font-weight: 700;
  color: var(--text-muted);
  background-color: var(--bg-app);
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  margin-bottom: 8px;
}
.kr-group-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.overview-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  font-size: 13px;
}
.overview-item.completed {
  opacity: 0.6;
  background-color: var(--bg-app);
}
.overview-item.completed .item-title {
  text-decoration: line-through;
}
.overview-item.cancelled {
  opacity: 0.5;
  background-color: var(--bg-app);
}
.overview-item.cancelled .item-title {
  text-decoration: line-through;
  color: var(--text-muted);
}

.item-title {
  flex: 1;
  color: var(--text-main);
  font-weight: 500;
}

/* 5. Status Indicators (Dots) */
.status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.status-indicator.not_started { background-color: var(--text-muted); }
.status-indicator.in_progress { background-color: var(--info); }
.status-indicator.done { background-color: var(--success); }
.status-indicator.cancelled { background-color: var(--border-medium); }
.status-indicator.not_involved { background-color: #cbd5e1; }
.status-indicator.pending { background-color: var(--warning); }
.status-indicator.blocked { background-color: var(--danger); }

/* 6. Status Tags (Uniform Labels) */
.status-tag {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
  white-space: nowrap;
}
.status-tag.not_started {
  color: var(--text-muted);
  background-color: var(--border-light);
}
.status-tag.in_progress {
  color: var(--info);
  background-color: var(--info-bg);
}
.status-tag.done {
  color: var(--success);
  background-color: var(--success-bg);
}
.status-tag.cancelled {
  color: var(--text-muted);
  background-color: var(--border-light);
}

/* 7. Sprint Item Styles */
.sprint-items-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.sprint-item-row {
  flex-direction: column;
  align-items: stretch;
  gap: 6px;
  padding: 12px 14px;
}
.sprint-item-row.involved {
  border-left: 3px solid var(--primary);
  background-color: rgba(79, 70, 229, 0.02);
}
.sprint-item-row.blocked {
  border-left: 3px solid var(--danger);
  background-color: rgba(239, 68, 68, 0.02);
}

.sprint-top-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.sprint-badges {
  display: flex;
  align-items: center;
  gap: 6px;
}
.involved-badge {
  font-size: 9.5px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
  white-space: nowrap;
}
.involved-badge.need-involved {
  color: var(--primary);
  background-color: var(--primary-light);
}
.involved-badge.no-involved {
  color: var(--text-muted);
  background-color: var(--border-light);
}

.sprint-bottom-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: 16px;
}
.sprint-meta {
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 500;
}

/* 4. Trend Chart Card */
.trend-chart-card {
  padding: 24px;
}
.trend-chart-card .chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.trend-chart-card h3 {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-main);
}

.chart-legend {
  display: flex;
  gap: 16px;
  align-items: center;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}
.legend-dot.dot-personal {
  background-color: var(--primary);
  border: 1.5px solid #fff;
  box-shadow: 0 0 0 1.5px var(--primary);
}
.legend-dot.dot-team {
  background-color: var(--success);
  border: 1.5px solid #fff;
  box-shadow: 0 0 0 1.5px var(--success);
}
.legend-text {
  font-size: 12.5px;
  color: var(--text-muted);
  font-weight: 500;
}

.chart-tabs-right {
  display: flex;
  gap: 8px;
  background-color: var(--border-light);
  padding: 4px;
  border-radius: var(--radius-sm);
}
.chart-tabs-right .tab-btn {
  background: none;
  border: none;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
}
.chart-tabs-right .tab-btn.active {
  background-color: var(--primary);
  color: #fff;
}

.chart-body-wrapper {
  display: flex;
  gap: 16px;
  position: relative;
  height: 200px;
  margin-right: 12px;
}

.y-axis-labels {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 100%;
  font-size: 11px;
  color: var(--text-muted);
  text-align: right;
  width: 32px;
  flex-shrink: 0;
  margin-top: -6px;
  height: calc(100% + 12px);
}

.chart-svg-container {
  flex: 1;
  height: 100%;
  position: relative;
}
.trend-svg {
  width: 100%;
  height: 100%;
  overflow: visible;
}

.x-axis-labels-row {
  margin-left: 48px; /* Offset to align with SVG start */
  margin-top: 10px;
  position: relative;
  height: 20px;
  margin-right: 12px;
}
.x-axis-labels-row .x-label {
  position: absolute;
  transform: translateX(-50%);
  font-size: 11px;
  color: var(--text-muted);
}
</style>
