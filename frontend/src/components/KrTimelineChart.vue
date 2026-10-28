<template>
  <div class="ktl">
    <!-- 图例 -->
    <div class="ktl-legend">
      <span class="legend-item"><span class="dot dot-done"></span>已完成</span>
      <span class="legend-item"><span class="dot dot-overdue"></span>已逾期</span>
      <span class="legend-item"><span class="dot dot-planned"></span>未到期</span>
      <span class="legend-item ktl-today-lbl">┆ 今天</span>
    </div>

    <div v-if="rows.length === 0" class="ktl-empty">
      暂无可展示的 K（需要设置计划完成时间，或已完成状态）
    </div>

    <div v-else class="ktl-body">
      <!-- 左侧 K 名称列 -->
      <div class="ktl-labels">
        <div
          v-for="row in rows"
          :key="row.key"
          class="ktl-label"
          :title="row.tooltip"
        >{{ row.label }}</div>
        <div class="ktl-axis-spacer"></div>
      </div>

      <!-- 右侧轨道区 -->
      <div class="ktl-tracks">
        <!-- 日网格线 + 今天竖线 -->
        <div class="ktl-grid">
          <div
            v-for="d in daysInMonth"
            :key="'g' + d"
            class="ktl-gridline"
            :class="{ weekend: isWeekend(d) }"
            :style="{ left: pct(d) }"
          ></div>
          <div v-if="todayDay" class="ktl-todayline" :style="{ left: pct(todayDay) }">
            <span class="ktl-today-tag">今天</span>
          </div>
        </div>

        <!-- K 行 -->
        <div
          v-for="row in rows"
          :key="row.key"
          class="ktl-row"
          :title="row.tooltip"
        >
          <div class="ktl-track">
            <div class="ktl-track-bg"></div>
            <div v-if="row.day" class="ktl-dot" :class="'dot-' + row.cls" :style="{ left: pct(row.day) }"></div>
            <span v-if="row.day" class="ktl-date" :class="'text-' + row.cls" :style="{ left: pct(row.day) }">{{ row.dateText }}</span>
            <span v-else class="ktl-nodate">无日期</span>
          </div>
        </div>

        <!-- 日期轴 -->
        <div class="ktl-axis">
          <span
            v-for="d in axisDays"
            :key="'a' + d"
            :style="{ left: pct(d) }"
          >{{ d }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

export interface KrTimelineItem {
  key: string | number
  name: string
  objectiveName?: string
  status: string
  planCompleteDate?: string | null
  /** 完成日期 YYYY-MM-DD（可由 doneAt 截取） */
  doneDate?: string | null
}

const props = defineProps<{
  month: string
  items: KrTimelineItem[]
}>()

const daysInMonth = computed(() => {
  const [y, m] = props.month.split('-').map(Number)
  return new Date(y, m, 0).getDate()
})

const now = new Date()
const todayStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
/** 今天的日期号（仅查看当月时存在）——必须随 month 变化重算，否则切到其他月份会错误显示今天线 */
const todayDay = computed(() =>
  props.month === todayStr.substring(0, 7) ? now.getDate() : null)

const dayOf = (date: string | null | undefined): number | null => {
  if (!date) return null
  const d = new Date(date)
  if (isNaN(d.getTime())) return null
  return d.getDate()
}

const rows = computed(() => {
  const [y, m] = props.month.split('-').map(Number)
  const monthPrefix = `${y}-${String(m).padStart(2, '0')}`
  return props.items.map(item => {
    const done = item.status === 'done'
    const planDay = dayOf(item.planCompleteDate)
    let day: number | null = null
    let cls = 'planned'
    let dateText = ''
    if (done) {
      const actual = item.doneDate && item.doneDate.startsWith(monthPrefix)
        ? dayOf(item.doneDate)
        : null
      day = actual ?? planDay
      cls = 'done'
      dateText = item.doneDate ? `完成 ${item.doneDate.slice(5)}` : '已完成'
    } else if (planDay != null) {
      day = planDay
      const overdue = item.planCompleteDate! < todayStr
      cls = overdue ? 'overdue' : 'planned'
      dateText = (overdue ? '逾期 ' : '计划 ') + item.planCompleteDate!.slice(5)
    }
    return {
      key: item.key,
      label: item.name,
      tooltip: `${item.objectiveName ? item.objectiveName + ' / ' : ''}${item.name}\n` +
        (item.planCompleteDate ? `计划完成: ${item.planCompleteDate}\n` : '') +
        (done ? `已完成: ${item.doneDate || '—'}` : (planDay != null ? (item.planCompleteDate! < todayStr ? '已逾期未完成' : '进行中') : '未设置计划完成时间')),
      day: day == null ? null : Math.min(Math.max(day, 1), daysInMonth.value),
      cls,
      dateText
    }
  })
})

const axisDays = computed(() => Array.from({ length: daysInMonth.value }, (_, i) => i + 1))

/** 日期→横向位置：坐标轴虚拟扩展 3 个槽位（天数+3），月末日期 ~91% 处，
    右侧留出一个标签宽的空白，保证「计划 10-31」这类标签不溢出容器、无横向滚动 */
const pct = (day: number): string =>
  daysInMonth.value > 0 ? ((day - 1) / (daysInMonth.value + 3)) * 100 + '%' : '0%'

const isWeekend = (day: number) => {
  const [y, m] = props.month.split('-').map(Number)
  const dow = new Date(y, m - 1, day).getDay()
  return dow === 0 || dow === 6
}
</script>

<style scoped>
.ktl {
  --row-h: 36px;
  --label-w: 180px;
  font-size: 12.5px;
  color: var(--text-main);
}

.ktl-legend {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 16px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}
.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11.5px;
  color: var(--text-muted);
}
.ktl-today-lbl {
  color: #d97706;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid #fff;
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.08);
  flex-shrink: 0;
}
.dot-done { background-color: #16a34a; }
.dot-overdue { background-color: #dc2626; }
.dot-planned { background-color: #2563eb; }

.ktl-empty {
  text-align: center;
  color: var(--text-muted);
  font-size: 13px;
  padding: 36px 0;
}

.ktl-body {
  display: flex;
  gap: 0;
  min-height: 0;
}

/* 左侧名称列 */
.ktl-labels {
  width: var(--label-w);
  flex-shrink: 0;
}
.ktl-label {
  height: var(--row-h);
  display: flex;
  align-items: center;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  padding-right: 10px;
  border-bottom: 1px dashed var(--border-light);
}
.ktl-axis-spacer {
  height: 22px;
}

/* 右侧轨道区 */
.ktl-tracks {
  flex: 1;
  min-width: 0;
  position: relative;
}
.ktl-grid {
  position: absolute;
  top: 0;
  bottom: 22px;
  left: 0;
  right: 0;
  pointer-events: none;
}
.ktl-gridline {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 1px;
  background-color: var(--border-light);
}
.ktl-gridline.weekend {
  background-color: #fef3c7;
  width: 2px;
  margin-left: -1px;
}
.ktl-todayline {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 0;
  border-left: 2px dashed #d97706;
  z-index: 2;
}
.ktl-today-tag {
  position: absolute;
  top: -2px;
  left: 3px;
  font-size: 10.5px;
  font-weight: 700;
  color: #d97706;
  white-space: nowrap;
}

.ktl-row {
  height: var(--row-h);
  position: relative;
  border-bottom: 1px dashed var(--border-light);
}
.ktl-track {
  position: absolute;
  inset: 0;
}
.ktl-track-bg {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 2px;
  transform: translateY(-50%);
  background-color: var(--border-light);
  border-radius: 1px;
}
.ktl-dot {
  position: absolute;
  top: 50%;
  width: 11px;
  height: 11px;
  border-radius: 50%;
  border: 2px solid #fff;
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.12);
  transform: translate(-50%, -50%);
  z-index: 3;
}
.ktl-dot.dot-done { background-color: #16a34a; }
.ktl-dot.dot-overdue { background-color: #dc2626; }
.ktl-dot.dot-planned { background-color: #2563eb; }

.ktl-date {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  margin-left: 9px;
  font-size: 10.5px;
  font-weight: 600;
  white-space: nowrap;
  z-index: 3;
}
.ktl-date.text-done { color: #16a34a; }
.ktl-date.text-overdue { color: #dc2626; }
.ktl-date.text-planned { color: #2563eb; }
.ktl-nodate {
  position: absolute;
  top: 50%;
  left: 8px;
  transform: translateY(-50%);
  font-size: 11px;
  color: var(--text-muted);
  opacity: 0.7;
}

/* 日期轴 */
.ktl-axis {
  position: relative;
  height: 22px;
  border-top: 1px solid var(--border-light);
}
.ktl-axis span {
  position: absolute;
  transform: translateX(-50%);
  font-size: 11px;
  color: var(--text-muted);
  top: 4px;
}
</style>
