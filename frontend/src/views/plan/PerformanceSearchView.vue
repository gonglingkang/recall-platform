<template>
  <div class="performance-search-view">
    <!-- Header + Search Box -->
    <div class="view-header">
      <div class="title-row">
        <h2>绩效搜索</h2>
        <p class="subtitle-lbl">跨月份搜索目标、关键成果与成果记录</p>
      </div>

      <div class="search-box">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="search-icon">
          <path stroke-linecap="round" stroke-linejoin="round" d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z" />
        </svg>
        <input
          ref="searchInputRef"
          v-model="keyword"
          type="text"
          class="search-input"
          placeholder="搜索 KR 名称、描述、成果记录…（至少 2 个字符）"
          autocomplete="off"
        />
        <button v-if="keyword" class="clear-btn" title="清空" @click="keyword = ''">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>
    </div>

    <!-- States -->
    <div v-if="loading" class="state-hint">搜索中…</div>
    <div v-else-if="trimmedKeyword.length < 2" class="state-hint">输入至少 2 个字符开始搜索，支持目标名称、KR 名称/描述与成果记录内容</div>
    <div v-else-if="groups.length === 0" class="state-hint">未找到与「{{ trimmedKeyword }}」相关的绩效内容</div>

    <!-- Results grouped by month -->
    <div v-else class="result-groups">
      <div v-for="group in groups" :key="group.month" class="month-group">
        <div class="month-title">📅 {{ group.month }}<span class="month-count">{{ group.hits.length }} 项</span></div>

        <div v-for="hit in group.hits" :key="hit.krId" class="hit-card">
          <div class="hit-objective">🎯 {{ hit.objectiveName }}</div>
          <div class="hit-kr">
            <span class="kr-name" v-html="highlight(hit.krName)"></span>
            <span class="kr-status" :class="statusName(hit.krStatus)">{{ statusCn(hit.krStatus) }}</span>
          </div>
          <p v-if="hit.krDescription" class="kr-desc" v-html="highlight(hit.krDescription)"></p>
          <p v-if="hit.cancelReason" class="kr-cancel">取消原因：{{ hit.cancelReason }}</p>
          <div v-if="hit.records.length > 0" class="kr-records">
            <div class="records-title">成果记录</div>
            <div v-for="(rec, i) in hit.records" :key="i" class="record-line">
              <span class="record-idx">({{ i + 1 }})</span>
              <span v-html="highlight(rec)"></span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import request from '@/api/request'

interface SearchHit {
  month: string
  objectiveId: number
  objectiveName: string
  krId: number
  krName: string
  krDescription: string
  krStatus: string
  cancelReason: string
  records: string[]
}

const keyword = ref('')
const results = ref<SearchHit[]>([])
const loading = ref(false)
const searchInputRef = ref<HTMLInputElement | null>(null)
let debounceTimer: number | null = null
// 防竞态：仅采纳最后一次请求的结果
let searchSeq = 0

const trimmedKeyword = computed(() => keyword.value.trim())

const runSearch = async () => {
  const kw = trimmedKeyword.value
  if (kw.length < 2) {
    results.value = []
    return
  }
  const seq = ++searchSeq
  loading.value = true
  try {
    const res = await request.get(`/api/objectives/search?keyword=${encodeURIComponent(kw)}`)
    if (seq !== searchSeq) return
    results.value = (res.data as SearchHit[]) || []
  } catch {
    if (seq === searchSeq) results.value = []
  } finally {
    if (seq === searchSeq) loading.value = false
  }
}

watch(trimmedKeyword, () => {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = window.setTimeout(runSearch, 300)
})

const groups = computed(() => {
  const map = new Map<string, SearchHit[]>()
  results.value.forEach(hit => {
    if (!map.has(hit.month)) map.set(hit.month, [])
    map.get(hit.month)!.push(hit)
  })
  return Array.from(map.entries()).map(([month, hits]) => ({ month, hits }))
})

const STATUS_CN: Record<string, string> = {
  done: '已完成',
  in_progress: '进行中',
  not_started: '未开始',
  cancelled: '已取消'
}
// API 的 KR 状态是数字码（0未开始/1进行中/2已完成/3已取消），先映射成枚举名
const STATUS_FROM_CODE: Record<string, string> = {
  '0': 'not_started',
  '1': 'in_progress',
  '2': 'done',
  '3': 'cancelled'
}
const statusName = (code: string) => STATUS_FROM_CODE[code] || code
const statusCn = (code: string) => STATUS_CN[statusName(code)] || statusName(code)

const escapeHtml = (s: string) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

/** 关键词命中标红（先转义防 XSS，再对转义后的文本做关键词替换） */
const highlight = (text: string | null): string => {
  const safe = escapeHtml(text || '')
  const kw = trimmedKeyword.value
  if (!kw || !text) return safe
  const safeKw = escapeHtml(kw).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return safe.replace(new RegExp(safeKw, 'gi'), m => `<mark>${m}</mark>`)
}

onMounted(() => {
  searchInputRef.value?.focus()
})
</script>

<style scoped>
.performance-search-view {
  max-width: 1100px;
  margin: 0 auto;
  padding-bottom: 60px;
  color: #334155;
}

.view-header {
  background-color: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 24px 32px;
  margin-bottom: 24px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
}
.title-row h2 {
  margin: 0 0 4px 0;
  font-size: 20px;
  font-weight: 800;
  color: var(--text-main);
}
.subtitle-lbl {
  margin: 0 0 16px 0;
  font-size: 12.5px;
  color: var(--text-muted);
}

.search-box {
  display: flex;
  align-items: center;
  gap: 10px;
  border: 2px solid var(--border-medium);
  border-radius: var(--radius-md);
  padding: 0 14px;
  height: 46px;
  background-color: #fff;
  transition: border-color var(--transition-fast);
}
.search-box:focus-within {
  border-color: var(--primary);
}
.search-icon {
  width: 18px;
  height: 18px;
  color: var(--text-muted);
  flex-shrink: 0;
}
.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 14px;
  color: var(--text-main);
  background: transparent;
  height: 100%;
}
.clear-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  padding: 4px;
  border-radius: 50%;
}
.clear-btn:hover {
  color: var(--text-main);
  background-color: var(--bg-hover, #f1f5f9);
}
.clear-btn svg {
  width: 14px;
  height: 14px;
}

.state-hint {
  text-align: center;
  color: var(--text-muted);
  font-size: 13.5px;
  padding: 60px 0;
  background-color: var(--bg-card);
  border: 1px dashed var(--border-medium);
  border-radius: var(--radius-lg);
}

.month-group {
  margin-bottom: 24px;
}
.month-title {
  font-size: 14px;
  font-weight: 800;
  color: var(--text-main);
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.month-count {
  font-size: 11.5px;
  font-weight: 600;
  color: var(--text-muted);
  background-color: var(--bg-card);
  border: 1px solid var(--border-light);
  padding: 1px 8px;
  border-radius: 10px;
}

.hit-card {
  background-color: var(--bg-card);
  border: 1px solid var(--border-light);
  border-left: 3px solid var(--primary);
  border-radius: var(--radius-md);
  padding: 12px 16px;
  margin-bottom: 10px;
}
.hit-objective {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-muted);
  margin-bottom: 6px;
}
.hit-kr {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.kr-name {
  font-size: 13.5px;
  font-weight: 700;
  color: var(--text-main);
}
.kr-status {
  font-size: 10.5px;
  font-weight: 700;
  padding: 1px 8px;
  border-radius: 10px;
  flex-shrink: 0;
}
.kr-status.done {
  color: #16a34a;
  background-color: #f0fdf4;
  border: 1px solid #bbf7d0;
}
.kr-status.in_progress {
  color: #2563eb;
  background-color: #eff6ff;
  border: 1px solid #bfdbfe;
}
.kr-status.not_started {
  color: #64748b;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
}
.kr-status.cancelled {
  color: #dc2626;
  background-color: #fef2f2;
  border: 1px solid #fecaca;
}
.kr-desc {
  margin: 6px 0 0 0;
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.6;
}
.kr-cancel {
  margin: 4px 0 0 0;
  font-size: 12px;
  color: #dc2626;
  line-height: 1.6;
}

.kr-records {
  margin-top: 8px;
  border-top: 1px dashed var(--border-light);
  padding-top: 8px;
}
.records-title {
  font-size: 11px;
  font-weight: 700;
  color: var(--text-muted);
  margin-bottom: 4px;
}
.record-line {
  font-size: 12.5px;
  color: var(--text-main);
  line-height: 1.7;
  display: flex;
  gap: 4px;
}
.record-idx {
  color: var(--text-muted);
  flex-shrink: 0;
}

.hit-card :deep(mark) {
  background-color: #fef08a;
  color: inherit;
  padding: 0 1px;
  border-radius: 2px;
}
</style>
