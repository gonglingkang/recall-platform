<template>
  <div class="meeting-minutes-view">
    <!-- Header -->
    <div class="view-header">
      <div class="title-row">
        <div class="title-meta-left">
          <h2>文档中转</h2>
          <p class="subtitle-lbl">推送进来的会议纪要在这里等待中转，绑定到具体需求后归档；已绑定的在需求详情文档列表中查看</p>
        </div>
      </div>
    </div>

    <!-- Pool List -->
    <div v-if="loading" class="state-hint">加载中…</div>
    <div v-else-if="minutes.length === 0" class="state-hint">
      没有等待中转的纪要，推送的会议纪要会出现在这里，绑定需求后归档到对应需求文档列表。
    </div>
    <div v-else class="minutes-list">
      <div v-for="m in minutes" :key="m.id" class="minute-card">
        <div class="minute-main">
          <a :href="m.url" target="_blank" rel="noopener" class="minute-title" :title="m.title">{{ m.title }}</a>
          <div class="minute-meta">
            <span>📅 {{ m.documentDate }}</span>
            <span v-if="m.createdAt">推送于 {{ formatDateTime(m.createdAt) }}</span>
          </div>
        </div>
        <div class="minute-actions">
          <button class="action-btn bind" @click="openBindModal(m)" title="绑定到需求，归档到需求文档列表">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" style="width: 12px; height: 12px;">
              <path stroke-linecap="round" stroke-linejoin="round" d="M13.19 8.688a4.5 4.5 0 011.242 7.244l-4.5 4.5a4.5 4.5 0 01-6.364-6.364l1.757-1.757m13.35-.622l1.757-1.757a4.5 4.5 0 00-6.364-6.364l-4.5 4.5a4.5 4.5 0 001.242 7.244" />
            </svg>
            绑定需求
          </button>
          <button class="action-btn" @click="openEditModal(m)" title="修改标题">编辑</button>
          <button class="action-btn danger" @click="confirmDelete(m)">删除</button>
        </div>
      </div>
    </div>

    <!-- Bind Modal（默认仅候选列表；点击入口后展开右侧新建需求面板） -->
    <div v-if="bindModal.isOpen" class="modal-overlay" @click.self="bindModal.isOpen = false">
      <div class="modal-content" :style="{ maxWidth: createMode ? '920px' : '560px', width: '95%', maxHeight: '86vh', display: 'flex', flexDirection: 'column' }">
        <h3>绑定会议纪要到需求</h3>
        <p class="modal-doc-title">📄 {{ bindModal.minute?.title }}</p>

        <div style="display: flex; gap: 20px; flex: 1; min-height: 0;">
          <!-- 左栏：候选列表 -->
          <div style="flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 10px;">
            <!-- AI 推荐横幅 -->
            <div
              v-if="bindModal.loadingOptions"
              style="font-size: 12.5px; color: var(--text-muted); background-color: #f8fafc; border: 1px solid var(--border-light); border-radius: var(--radius-md); padding: 8px 12px; display: flex; align-items: center; gap: 8px; flex-shrink: 0;"
            >
              <span class="mini-spinner"></span>
              正在加载需求列表<template v-if="bindModal.recommendLoading">，AI 分析最相关需求中…</template>
            </div>
            <div
              v-else-if="bindModal.recommend"
              style="font-size: 12.5px; color: #1d4ed8; background-color: #eff6ff; border: 1px solid #bfdbfe; border-radius: var(--radius-md); padding: 8px 12px; line-height: 1.5; flex-shrink: 0;"
            >
              🎯 AI 推荐需求：<strong>{{ bindModal.recommend.categoryName ? bindModal.recommend.categoryName + ' / ' : '' }}{{ bindModal.recommend.requirementTitle }}</strong>
              （置信度 {{ Math.round(bindModal.recommend.confidence * 100) }}%，已自动勾选，可调整）
            </div>
            <div
              v-else-if="bindModal.recommendChecked"
              style="font-size: 12.5px; color: #92400e; background-color: #fffbeb; border: 1px solid #fcd34d; border-radius: var(--radius-md); padding: 8px 12px; line-height: 1.5; flex-shrink: 0;"
            >
              AI 未找到足够相关的需求，请手动选择；若本次纪要对应尚不存在的新需求，可在下方新建并绑定。
            </div>

            <!-- 新建需求入口（用户需要时才展开右侧面板） -->
            <button
              v-if="!createMode"
              @click="openCreateMode"
              style="width: 100%; flex-shrink: 0; border: 1.5px dashed #fca5a5; background-color: #fef2f2; color: #b91c1c; border-radius: var(--radius-md); padding: 8px 12px; font-size: 12.5px; font-weight: 600; cursor: pointer;"
              onmouseover="this.style.borderColor='#ef4444'; this.style.backgroundColor='#fee2e2';"
              onmouseout="this.style.borderColor='#fca5a5'; this.style.backgroundColor='#fef2f2';"
            >＋ 没有合适的需求？新建需求并自动绑定</button>

            <input
              v-model="bindModal.keyword"
              type="text"
              class="form-control"
              placeholder="搜索需求标题…"
              style="flex-shrink: 0;"
            />
            <div class="req-select-list" style="flex: 1; min-height: 140px; max-height: none;">
              <div v-if="bindModal.loadingOptions" class="state-hint" style="padding: 24px 0; border: none;">加载中…</div>
              <div v-else-if="filteredRequirements.length === 0" class="state-hint" style="padding: 24px 0; border: none;">无匹配需求</div>
              <label
                v-for="req in filteredRequirements"
                :key="req.id"
                class="req-option"
                :class="{ selected: bindModal.requirementId === req.id, recommended: bindModal.recommend?.requirementId === req.id }"
              >
                <input type="radio" name="bind-req" :value="req.id" v-model="bindModal.requirementId" />
                <span class="req-status-tag" :class="'rs-' + req.status">{{ statusCn(req.status) }}</span>
                <span v-if="req.categoryName" class="req-category">{{ req.categoryName }}</span>
                <span class="req-title">{{ req.title }}</span>
                <span v-if="bindModal.recommend?.requirementId === req.id" class="req-ai-tag">AI 推荐</span>
              </label>
            </div>
          </div>

          <!-- 右栏：新建需求并绑定（点击入口后展开） -->
          <div v-if="createMode" class="quick-create-form" style="width: 300px; flex-shrink: 0; align-self: flex-start;">
            <div class="quick-create-title">新建需求并绑定</div>
            <div class="form-field">
              <label>需求标题<span class="required-star">*</span></label>
              <div style="display: flex; gap: 6px;">
                <input
                  v-model="createForm.title"
                  type="text"
                  class="form-control"
                  style="flex: 1; min-width: 0;"
                  placeholder="新需求标题"
                  @input="titleTouched = true"
                />
                <button
                  class="btn btn-secondary"
                  style="flex-shrink: 0; height: 36px; font-size: 12px; padding: 0 10px; display: flex; align-items: center; gap: 4px;"
                  :disabled="titleGenerating"
                  title="由 LLM 从纪要标题生成规范的需求标题"
                  @click="generateTitle(true)"
                ><span v-if="titleGenerating" class="mini-spinner"></span>{{ titleGenerating ? '生成中' : '✨ AI 生成' }}</button>
              </div>
            </div>
            <div class="form-field" style="position: relative;">
              <label>需求主分类<span class="required-star">*</span></label>
              <div
                class="select-trigger"
                :class="{ active: ddState.main }"
                @click="ddState.main = !ddState.main; ddState.sub = false"
              >
                <span :style="{ color: createForm.categoryId ? 'var(--text-main)' : 'var(--text-muted)' }">{{ mainCategoryName }}</span>
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.2" stroke="currentColor" style="width: 13px; height: 13px;">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
                </svg>
              </div>
              <div v-if="ddState.main" class="dd-list">
                <div v-if="reqCategories.length === 0" class="dd-empty">暂无需求分类，请先到需求分类管理中创建</div>
                <div
                  v-for="cat in reqCategories"
                  :key="cat.id"
                  class="dd-item"
                  :class="{ selected: createForm.categoryId === cat.id }"
                  @click="createForm.categoryId = cat.id; createForm.subCategoryId = null; ddState.main = false"
                >{{ cat.name }}</div>
              </div>
            </div>
            <div class="form-field" style="position: relative;">
              <label>需求子分类</label>
              <div
                class="select-trigger"
                :class="{ active: ddState.sub, disabled: !createForm.categoryId }"
                @click="createForm.categoryId && (ddState.sub = !ddState.sub); ddState.main = false"
              >
                <span :style="{ color: createForm.subCategoryId ? 'var(--text-main)' : 'var(--text-muted)' }">{{ subCategoryName }}</span>
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.2" stroke="currentColor" style="width: 13px; height: 13px;">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
                </svg>
              </div>
              <div v-if="ddState.sub" class="dd-list">
                <div v-if="subCategoryOptions.length === 0" class="dd-empty">该主分类下暂无子分类</div>
                <div
                  v-for="sub in subCategoryOptions"
                  :key="sub.id"
                  class="dd-item"
                  :class="{ selected: createForm.subCategoryId === sub.id }"
                  @click="createForm.subCategoryId = sub.id; ddState.sub = false"
                >{{ sub.name }}</div>
              </div>
            </div>
            <div style="font-size: 11.5px; color: var(--text-muted); line-height: 1.5;">
              首次需求时间取该纪要的会议日期；创建后立即绑定并归档。
            </div>
            <button
              class="btn btn-primary"
              style="width: 100%;"
              :disabled="creating || !createForm.title.trim() || !createForm.categoryId"
              @click="submitCreateAndBind"
            >{{ creating ? '创建中…' : '创建并绑定' }}</button>
            <button class="btn btn-secondary" style="width: 100%;" :disabled="creating" @click="createMode = false">收起</button>
          </div>
        </div>

        <div class="modal-actions" style="flex-shrink: 0;">
          <button class="btn btn-secondary" @click="bindModal.isOpen = false">取消</button>
          <button class="btn btn-primary" :disabled="!bindModal.requirementId || bindModal.submitting" @click="submitBind">
            {{ bindModal.submitting ? '绑定中…' : '确认绑定' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Edit Title Modal -->
    <div v-if="editModal.isOpen" class="modal-overlay" @click.self="editModal.isOpen = false">
      <div class="modal-content" style="max-width: 460px; width: 95%;">
        <h3>修改纪要标题</h3>
        <input v-model="editModal.title" type="text" class="form-control" placeholder="纪要标题" @keyup.enter="submitEdit" />
        <div class="modal-actions">
          <button class="btn btn-secondary" @click="editModal.isOpen = false">取消</button>
          <button class="btn btn-primary" :disabled="!editModal.title.trim() || editModal.submitting" @click="submitEdit">保存</button>
        </div>
      </div>
    </div>

    <!-- Delete Confirm Modal -->
    <div v-if="deleteModal.isOpen" class="modal-overlay" @click.self="deleteModal.isOpen = false">
      <div class="modal-content" style="max-width: 420px; width: 95%;">
        <h3 style="color: #ef4444;">删除确认</h3>
        <p style="font-size: 13.5px; color: var(--text-muted); line-height: 1.6; margin: 0;">
          确定删除纪要 <strong>「{{ deleteModal.minute?.title }}」</strong> 吗？删除后不可恢复。
        </p>
        <div class="modal-actions">
          <button class="btn btn-secondary" @click="deleteModal.isOpen = false">取消</button>
          <button class="btn btn-primary" style="background-color: #ef4444; border-color: #ef4444;" @click="submitDelete">确认删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import request from '@/api/request'

interface MinuteItem {
  id: number
  title: string
  url: string
  documentDate: string
  createdAt: string
}

interface RequirementOption {
  id: number
  title: string
  status: string
  categoryName: string
}

interface RecommendResult {
  requirementId: number
  requirementTitle: string
  categoryName: string
  status: string
  confidence: number
}

const minutes = ref<MinuteItem[]>([])
const requirements = ref<RequirementOption[]>([])
const loading = ref(false)

const emitToastMsg = (text: string, type?: string) => {
  window.dispatchEvent(new CustomEvent('app-toast', { detail: type ? { text, type } : { text } }))
}

const formatDateTime = (s: string) => {
  const d = new Date(s)
  if (isNaN(d.getTime())) return s
  const p = (n: number) => String(n).padStart(2, '0')
  return `${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

const loadMinutes = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/meeting-minutes')
    minutes.value = (res.data as MinuteItem[]) || []
  } catch {
    // 拦截器统一 toast
  } finally {
    loading.value = false
  }
}

const loadRequirements = async () => {
  try {
    const res = await request.get('/api/meeting-minutes/bind-options')
    const list = (res.data as any[]) || []
    requirements.value = list.map(r => ({
      id: r.id,
      title: r.title,
      status: r.status,
      categoryName: [r.categoryName, r.subCategoryName].filter(Boolean).join('/')
    }))
  } catch {
    // 绑定选择器数据加载失败时列表为空，不影响其他功能
  }
}

/** 需求状态码 → 文案（绑定列表仅会出现 0/2/3） */
const REQ_STATUS_CN: Record<string, string> = {
  '0': '讨论中',
  '2': '进行中',
  '3': '开发完成'
}
const statusCn = (code: string) => REQ_STATUS_CN[code] || '其他'

// ---- 绑定需求 ----
const bindModal = reactive<{
  isOpen: boolean
  minute: MinuteItem | null
  keyword: string
  requirementId: number | null
  submitting: boolean
  loadingOptions: boolean
  recommendLoading: boolean
  recommendChecked: boolean
  options: RequirementOption[]
  recommend: RecommendResult | null
}>({
  isOpen: false,
  minute: null,
  keyword: '',
  requirementId: null,
  submitting: false,
  loadingOptions: false,
  recommendLoading: false,
  recommendChecked: false,
  options: [],
  recommend: null
})

const filteredRequirements = computed(() => {
  const kw = bindModal.keyword.trim().toLowerCase()
  if (!kw) return bindModal.options
  return bindModal.options.filter(r => r.title.toLowerCase().includes(kw) || r.categoryName.toLowerCase().includes(kw))
})

const openBindModal = async (m: MinuteItem) => {
  bindModal.minute = m
  bindModal.keyword = ''
  bindModal.requirementId = null
  bindModal.options = []
  bindModal.recommend = null
  bindModal.recommendChecked = false
  // 新建需求表单：默认收起，标题预填纪要标题，分类重置
  createMode.value = false
  createForm.title = m.title
  titleTouched.value = false
  titleGenerating.value = false
  createForm.categoryId = null
  createForm.subCategoryId = null
  ddState.main = false
  ddState.sub = false
  bindModal.isOpen = true
  // 打开即加载需求列表 + 分类 + AI 推荐；加载期间显示状态，避免误显示“无匹配需求”
  bindModal.loadingOptions = true
  bindModal.recommendLoading = true
  try {
    await Promise.all([loadRequirements(), loadReqCategories()])
    bindModal.options = [...requirements.value]
  } finally {
    bindModal.loadingOptions = false
  }
  try {
    const res = await request.get(
      `/api/meeting-minutes/recommend-requirement?title=${encodeURIComponent(m.title)}`,
      { timeout: 60000 }
    )
    // data 为 null = 模型判定无合适需求（或暂无可选需求），不显示推荐
    bindModal.recommend = (res.data as RecommendResult | null) || null
    bindModal.recommendChecked = true
    if (bindModal.recommend && !bindModal.requirementId) {
      bindModal.requirementId = bindModal.recommend.requirementId
    }
  } catch {
    // 推荐失败（jev 未启用/调用异常）不打断绑定，用户手动选择；错误 toast 由拦截器统一弹出
  } finally {
    bindModal.recommendLoading = false
  }
}

const submitBind = async () => {
  if (!bindModal.minute || !bindModal.requirementId) return
  bindModal.submitting = true
  try {
    await request.put(`/api/meeting-minutes/${bindModal.minute.id}/bind`, {
      requirementId: bindModal.requirementId
    })
    const req = bindModal.options.find(r => r.id === bindModal.requirementId)
    emitToastMsg(`已绑定到需求「${req?.title || ''}」`)
    bindModal.isOpen = false
    await loadMinutes()
  } catch {
    // 拦截器统一 toast
  } finally {
    bindModal.submitting = false
  }
}

// ---- 新建需求并绑定（右栏常驻，标题预填纪要标题） ----
interface ReqCategoryOpt { id: number; name: string; subcategories?: { id: number; name: string }[] }

const reqCategories = ref<ReqCategoryOpt[]>([])
const createMode = ref(false)
const createForm = reactive({ title: '', categoryId: null as number | null, subCategoryId: null as number | null })
/** 用户是否手动改过标题：手动改过后 AI 生成结果不自动覆盖 */
const titleTouched = ref(false)
const titleGenerating = ref(false)

/** 由 LLM 从纪要标题生成需求标题；force=true（点击按钮）时无视手动修改强制覆盖 */
const generateTitle = async (force = false) => {
  const source = bindModal.minute?.title
  if (!source || titleGenerating.value) return
  if (!force && titleTouched.value) return
  titleGenerating.value = true
  try {
    const res = await request.get(
      `/api/meeting-minutes/generate-requirement-title?title=${encodeURIComponent(source)}`,
      { timeout: 60000 }
    )
    const generated = ((res.data as string) || '').trim()
    if (generated && (force || !titleTouched.value)) {
      createForm.title = generated
      titleTouched.value = false
    }
  } catch {
    // 生成失败保留原标题（纪要标题），不打断创建流程
  } finally {
    titleGenerating.value = false
  }
}
const ddState = reactive({ main: false, sub: false })
const creating = ref(false)

const subCategoryOptions = computed(() => {
  if (!createForm.categoryId) return []
  return reqCategories.value.find(c => c.id === createForm.categoryId)?.subcategories || []
})
const mainCategoryName = computed(() =>
  reqCategories.value.find(c => c.id === createForm.categoryId)?.name || '请选择主分类')
const subCategoryName = computed(() =>
  subCategoryOptions.value.find(s => s.id === createForm.subCategoryId)?.name || '不选择子分类')

const loadReqCategories = async () => {
  if (reqCategories.value.length > 0) return
  try {
    const res = await request.get('/api/requirement-categories')
    reqCategories.value = (res.data as ReqCategoryOpt[]) || []
  } catch {
    // 加载失败时下拉为空并给出提示，不阻断其他操作
  }
}

const openCreateMode = () => {
  createMode.value = true
  // 打开面板即自动生成需求标题（用户已手动改过则不覆盖）
  generateTitle(false)
}

const submitCreateAndBind = async () => {
  if (creating.value || !bindModal.minute || !createForm.categoryId) return
  creating.value = true
  try {
    const res = await request.post(`/api/meeting-minutes/${bindModal.minute.id}/create-requirement`, {
      title: createForm.title.trim(),
      categoryId: createForm.categoryId,
      subCategoryId: createForm.subCategoryId || undefined
    }, { timeout: 30000 })
    emitToastMsg(`已新建需求「${(res.data as any)?.title || ''}」并完成绑定`)
    bindModal.isOpen = false
    await loadMinutes()
  } catch {
    // 拦截器统一 toast（如标题重复 4701）
  } finally {
    creating.value = false
  }
}

// ---- 编辑标题 ----
const editModal = reactive({ isOpen: false, minute: null as MinuteItem | null, title: '', submitting: false })

const openEditModal = (m: MinuteItem) => {
  editModal.minute = m
  editModal.title = m.title
  editModal.isOpen = true
}

const submitEdit = async () => {
  if (!editModal.minute) return
  editModal.submitting = true
  try {
    await request.put(`/api/meeting-minutes/${editModal.minute.id}`, editModal.title.trim(), {
      headers: { 'Content-Type': 'text/plain' }
    })
    emitToastMsg('标题已更新')
    editModal.isOpen = false
    await loadMinutes()
  } catch {
    // 拦截器统一 toast
  } finally {
    editModal.submitting = false
  }
}

// ---- 删除 ----
const deleteModal = reactive<{ isOpen: boolean; minute: MinuteItem | null; submitting: boolean }>({
  isOpen: false,
  minute: null,
  submitting: false
})

const confirmDelete = (m: MinuteItem) => {
  deleteModal.minute = m
  deleteModal.isOpen = true
}

const submitDelete = async () => {
  if (!deleteModal.minute) return
  deleteModal.submitting = true
  try {
    await request.delete(`/api/meeting-minutes/${deleteModal.minute.id}`)
    emitToastMsg('纪要已删除')
    deleteModal.isOpen = false
    await loadMinutes()
  } catch {
    // 拦截器统一 toast
  } finally {
    deleteModal.submitting = false
  }
}

onMounted(loadMinutes)
</script>

<style scoped>
.meeting-minutes-view {
  max-width: 1100px;
  margin: 0 auto;
  padding-bottom: 60px;
  color: #334155;
}

.view-header {
  background-color: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 24px 32px;
  margin-bottom: 20px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
}
.title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}
.title-row h2 {
  margin: 0 0 4px 0;
  font-size: 20px;
  font-weight: 800;
  color: var(--text-main);
}
.subtitle-lbl {
  margin: 0;
  font-size: 12.5px;
  color: var(--text-muted);
}

.state-hint {
  text-align: center;
  color: var(--text-muted);
  font-size: 13.5px;
  padding: 48px 32px;
  background-color: var(--bg-card);
  border: 1px dashed var(--border-medium);
  border-radius: var(--radius-lg);
  line-height: 1.7;
}

.minutes-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.minute-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background-color: var(--bg-card);
  border: 1px solid var(--border-light);
  border-left: 3px solid #2563eb;
  border-radius: var(--radius-md);
  padding: 14px 18px;
}
.minute-main {
  min-width: 0;
  flex: 1;
}
.minute-title {
  display: block;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-main);
  text-decoration: none;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.minute-title:hover {
  color: #2563eb;
  text-decoration: underline;
}
.minute-meta {
  display: flex;
  gap: 14px;
  margin-top: 5px;
  font-size: 12px;
  color: var(--text-muted);
}
.minute-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}
.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  background: none;
  border: 1px solid var(--border-medium);
  border-radius: 6px;
  padding: 5px 10px;
  cursor: pointer;
  white-space: nowrap;
  transition: all var(--transition-fast);
}
.action-btn:hover {
  color: #2563eb;
  border-color: #2563eb;
}
.action-btn.bind {
  color: #2563eb;
  border-color: rgba(37, 99, 235, 0.35);
}
.action-btn.danger:hover {
  color: #ef4444;
  border-color: #ef4444;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2100;
}
.modal-content {
  background: #fff;
  border-radius: 14px;
  padding: 24px;
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.25);
  border: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.modal-content h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 800;
  color: var(--text-main);
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-light);
}
.modal-doc-title {
  margin: 0;
  font-size: 12.5px;
  color: var(--text-muted);
  background-color: #f8fafc;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  padding: 8px 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 2px;
}
.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.form-field label {
  font-size: 12.5px;
  font-weight: 700;
  color: #475569;
}

/* 表单控件（本页 scoped，项目内该类为各页面私有样式） */
.form-control {
  width: 100%;
  border: 1.5px solid var(--border-medium);
  border-radius: 8px;
  padding: 9px 12px;
  font-size: 13.5px;
  color: var(--text-main);
  background-color: #fff;
  outline: none;
  font-family: inherit;
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);
}
.form-control:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}
.form-control::placeholder {
  color: #94a3b8;
}

/* 按钮 */
.btn {
  border-radius: 8px;
  padding: 0 16px;
  height: 36px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-family: inherit;
  transition: all var(--transition-fast);
}
.btn-primary {
  background-color: #2563eb;
  color: #fff;
  border: 1px solid #2563eb;
}
.btn-primary:hover:not(:disabled) {
  background-color: #1d4ed8;
}
.btn-secondary {
  background-color: #fff;
  color: var(--text-main);
  border: 1px solid var(--border-medium);
}
.btn-secondary:hover:not(:disabled) {
  border-color: #94a3b8;
  background-color: #f8fafc;
}
.btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.req-select-list {
  max-height: 320px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 6px;
}
.req-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-main);
}
.req-option:hover {
  background-color: #f1f5f9;
}
.req-option.selected {
  background-color: #eff6ff;
}
.req-option.recommended {
  outline: 1px solid #bfdbfe;
}
.req-status-tag {
  flex-shrink: 0;
  font-size: 10.5px;
  font-weight: 700;
  padding: 1px 7px;
  border-radius: 10px;
}
.req-status-tag.rs-0 {
  color: #d97706;
  background-color: #fef3c7;
}
.req-status-tag.rs-2 {
  color: #2563eb;
  background-color: #eff6ff;
}
.req-status-tag.rs-3 {
  color: #16a34a;
  background-color: #f0fdf4;
}
.req-category {
  flex-shrink: 0;
  font-size: 10.5px;
  color: var(--text-muted);
  background-color: #f1f5f9;
  padding: 1px 7px;
  border-radius: 10px;
  max-width: 130px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.req-title {
  flex: 1;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.req-ai-tag {
  flex-shrink: 0;
  font-size: 9.5px;
  font-weight: 700;
  color: #1d4ed8;
  background-color: #dbeafe;
  padding: 1px 6px;
  border-radius: 8px;
}
.mini-spinner {
  width: 11px;
  height: 11px;
  border: 2px solid rgba(37, 99, 235, 0.25);
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: mm-spin 0.8s linear infinite;
  flex-shrink: 0;
}
@keyframes mm-spin {
  to {
    transform: rotate(360deg);
  }
}

/* 新建需求并绑定（右栏） */
.quick-create-form {
  border: 1.5px solid var(--border-medium);
  border-radius: var(--radius-md);
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  background-color: #fafbfc;
}
.quick-create-title {
  font-size: 13.5px;
  font-weight: 800;
  color: var(--text-main);
  padding-bottom: 6px;
  border-bottom: 1px dashed var(--border-light);
}
.required-star {
  color: #ef4444;
  margin-left: 2px;
}
.select-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  height: 36px;
  padding: 0 10px;
  border: 1.5px solid var(--border-medium);
  border-radius: 6px;
  background-color: #fff;
  cursor: pointer;
  font-size: 13px;
  transition: all var(--transition-fast);
}
.select-trigger.active {
  border-color: var(--primary);
}
.select-trigger.disabled {
  background-color: var(--bg-app);
  cursor: not-allowed;
  opacity: 0.65;
}
.dd-list {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  background: #fff;
  border: 1px solid var(--border-medium);
  border-radius: var(--radius-md);
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.12);
  z-index: 999;
  padding: 5px;
  display: flex;
  flex-direction: column;
  gap: 3px;
  max-height: 180px;
  overflow-y: auto;
}
.dd-item {
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-main);
}
.dd-item:hover {
  background-color: #f1f5f9;
}
.dd-item.selected {
  background-color: #eff6ff;
  color: #1d4ed8;
  font-weight: 600;
}
.dd-empty {
  padding: 10px;
  font-size: 12px;
  color: var(--text-muted);
  text-align: center;
}
</style>
