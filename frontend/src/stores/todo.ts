import { defineStore } from 'pinia'
import request from '@/api/request'
import type { ApiResult } from '@/api/types'

export interface Todo {
  id: number
  userId: number
  title: string
  content: string
  planDate: string
  priority: 'high' | 'medium' | 'low'
  status: 'pending' | 'done' | 'deleted'
  isPinned: boolean
  reminderTime: string | null
  categoryId: number | null
  subcategoryId: number | null
  doneAt: string | null
  createdAt: string
  derivedFromType: 'performance' | 'sprint' | null
  derivedFromId: number | null
  rolloverCount: number
}

export interface Category {
  id: number
  userId: number
  name: string
  color: string
  order: number
}

export interface Subcategory {
  id: number
  categoryId: number
  name: string
  order: number
}

export interface PerformanceCategory {
  id: number
  userId: number
  name: string
  description?: string
  status?: 'not_started' | 'in_progress' | 'done'
}

export interface PerformanceKR {
  id: number
  userId: number
  month: string
  categoryId: number
  title: string
  status: 'not_started' | 'in_progress' | 'done' | 'cancelled'
  doneAt: string | null
  planCompleteDate?: string | null
  remark: string
  sourceSprintId: number | null
  derivedTodoIds: number[]
  sprintIds: number[]
}

export interface TeamSprint {
  id: number
  userId: number
  month: string
  title: string
  status: 'not_started' | 'in_progress' | 'done'
  needInvolved: boolean
  owner: string
  module: string
  remark: string
  keyResultIds: number[]
}

export const useTodoStore = defineStore('todo', {
  state: () => ({
    todos: [] as Todo[],
    categories: [] as Category[],
    subcategoriesMap: {} as Record<number, Subcategory[]>,
    performanceCategories: [] as PerformanceCategory[],
    performanceKRs: [] as PerformanceKR[],
    teamSprints: [] as TeamSprint[],
    currentMonth: new Date().toISOString().substring(0, 7), // YYYY-MM
    pendingCounts: {} as Record<string, number>
  }),
  getters: {
    // Filtered todos (non-deleted)
    activeTodos: (state) => state.todos.filter(t => t.status !== 'deleted'),
    trashTodos: (state) => state.todos.filter(t => t.status === 'deleted'),
    
    // Todos for a specific date (PRD 6.5.1)
    todosByDate: (state) => (dateStr: string) => {
      return state.todos
        .filter(t => t.status !== 'deleted' && t.planDate === dateStr)
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
    },

    // Categories sorted
    sortedCategories: (state) => [...state.categories].sort((a, b) => a.order - b.order),
    
    // Subcategories for a category
    subcategoriesByCategoryId: (state) => (catId: number) => {
      return state.subcategoriesMap[catId] || []
    },
    getCategoryPendingCount: (state) => (catId: number) => {
      return state.pendingCounts[String(catId)] || 0
    },
    todayPendingCount: (state) => {
      return state.pendingCounts['today'] || 0
    }
  },
  actions: {
    async refreshCategories(userId: number) {
      try {
        const catsRes = await request.get<any[], ApiResult<any[]>>('/api/categories')
        const categoriesList: Category[] = []
        const subMap: Record<number, Subcategory[]> = {}
        if (catsRes && catsRes.data) {
          catsRes.data.forEach((cat: any) => {
            categoriesList.push({
              id: Number(cat.id),
              userId,
              name: cat.name,
              color: cat.color,
              order: cat.sortOrder || 0
            })
            subMap[Number(cat.id)] = (cat.subcategories || []).map((sub: any) => ({
              id: Number(sub.id),
              categoryId: Number(cat.id),
              name: sub.name,
              order: sub.sortOrder || 0
            }))
          })
        }
        this.categories = categoriesList
        this.subcategoriesMap = subMap
      } catch (e) {
        console.error('Failed to refresh categories:', e)
      }
    },

    async refreshPendingCounts(userId: number) {
      try {
        if (!this.categories || this.categories.length === 0) {
          await this.refreshCategories(userId)
        }
        
        const promises: Promise<{ key: string; count: number }>[] = []
        
        // 1. 今日待办未完成数
        promises.push(
          request.get<number, ApiResult<number>>('/api/todos/pending-count')
            .then(res => ({ key: 'today', count: res.data || 0 }))
        )
        
        // 2. 各分类未完成数
        this.categories.forEach(cat => {
          promises.push(
            request.get<number, ApiResult<number>>(`/api/todos/pending-count?categoryId=${cat.id}`)
              .then(res => ({ key: String(cat.id), count: res.data || 0 }))
          )
        })
        
        const results = await Promise.all(promises)
        const countsMap: Record<string, number> = {}
        results.forEach(r => {
          countsMap[r.key] = r.count
        })
        this.pendingCounts = countsMap
      } catch (e) {
        console.error('Failed to refresh pending counts:', e)
      }
    },

    async refreshTodayTodos(userId: number) {
      try {
        if (!this.categories || this.categories.length === 0) {
          await this.refreshCategories(userId)
        }
        const todayRes = await request.get<any, ApiResult<any[]>>('/api/todos/today')
        this.todos = todayRes && todayRes.data 
          ? todayRes.data.map((vo: any) => mapTodoVOToTodo(vo, this.categories, this.subcategoriesMap)) 
          : []
      } catch (e) {
        console.error('Failed to refresh today todos:', e)
      }
    },

    async refreshObjectives(userId: number) {
      try {
        const objectivesRes = await request.get<any[], ApiResult<any[]>>(`/api/objectives?month=${this.currentMonth}`)
        const perfCategoriesList: PerformanceCategory[] = []
        const krsList: PerformanceKR[] = []
        
        if (objectivesRes && objectivesRes.data) {
          objectivesRes.data.forEach((obj: any) => {
            let oStatus: 'not_started' | 'in_progress' | 'done' = 'not_started'
            if (obj.status === '2') oStatus = 'done'
            else if (obj.status === '1') oStatus = 'in_progress'

            perfCategoriesList.push({
              id: Number(obj.id),
              userId,
              name: obj.name,
              description: obj.description || '',
              status: oStatus
            })
            
            if (obj.keyResults) {
              obj.keyResults.forEach((kr: any) => {
                krsList.push(mapPerfItemVOToKR(kr, obj.id, obj.month))
              })
            }
          })
        }
        this.performanceCategories = perfCategoriesList
        this.performanceKRs = krsList
      } catch (e) {
        console.error('Failed to refresh objectives:', e)
      }
    },

    async refreshSprints(userId: number) {
      try {
        const sprintsRes = await request.get<any[], ApiResult<any[]>>(`/api/sprint/items?month=${this.currentMonth}`)
        this.teamSprints = sprintsRes && sprintsRes.data ? sprintsRes.data.map(mapSprintItemVOToSprint) : []
      } catch (e) {
        console.error('Failed to refresh sprints:', e)
      }
    },



    setMonth(month: string) {
      this.currentMonth = month
    },

    async fetchTodosByCategory(
      userId: number,
      categoryId: number | null, 
      subcategoryId: number | null,
      startDate?: string | null,
      endDate?: string | null,
      status?: string | null,
      pageNum: number = 1
    ) {
      try {
        if (!this.categories || this.categories.length === 0) {
          await this.refreshCategories(userId)
        }
        const params: any = { pageNum, pageSize: 20 }
        if (subcategoryId) {
          params.categoryId = subcategoryId
        } else if (categoryId) {
          params.categoryId = categoryId
        }
        
        if (startDate) {
          params.startDate = startDate
        }
        if (endDate) {
          params.endDate = endDate
        }
        if (status && status !== 'all') {
          params.status = status.toUpperCase()
        }
        
        console.log('fetchTodosByCategory request params:', JSON.stringify(params))
        
        const res = await request.get<any, ApiResult<any>>('/api/todos', { params })
        const mapped = res.data && res.data.records 
          ? res.data.records.map((vo: any) => mapTodoVOToTodo(vo, this.categories, this.subcategoriesMap)) 
          : []
        
        if (pageNum === 1) {
          this.todos = this.todos.filter(t => t.categoryId !== categoryId && t.categoryId !== subcategoryId).concat(mapped)
        } else {
          const existingIds = new Set(mapped.map((t: any) => t.id))
          this.todos = this.todos.filter(t => !existingIds.has(t.id)).concat(mapped)
        }
        return res.data
      } catch (e) {
        console.error(`Failed to fetch todos for category ${categoryId}:`, e)
        return null
      }
    },

    async searchTodos(keyword: string) {
      try {
        const res = await request.get<any, ApiResult<any>>(`/api/todos/search?keyword=${encodeURIComponent(keyword)}&pageNum=1&pageSize=20`)
        const mapped = res.data && res.data.records 
          ? res.data.records.map((vo: any) => mapTodoVOToTodo(vo, this.categories, this.subcategoriesMap)) 
          : []
        this.todos = mapped
      } catch (e) {
        console.error('Failed to search todos:', e)
      }
    },

    async fetchTrashTodos() {
      // Backend deprecated soft delete, all deletes are physical
      this.todos = this.todos.filter(t => t.status !== 'deleted')
    },

    // --- Todo Actions ---
    async addTodo(userId: number, todo: Omit<Todo, 'id' | 'userId' | 'createdAt' | 'rolloverCount'>) {
      const categoryIdToSend = todo.subcategoryId ? todo.subcategoryId : todo.categoryId
      const payload = {
        title: todo.title,
        note: todo.content,
        categoryId: categoryIdToSend,
        priority: todo.priority === 'high' ? '2' : (todo.priority === 'low' ? '0' : '1')
      }
      
      const res = await request.post<any, ApiResult<any>>('/api/todos', payload)
      const newTodo = mapTodoVOToTodo(res.data, this.categories, this.subcategoriesMap)
      this.todos.unshift(newTodo)
      await this.refreshPendingCounts(userId)
      return newTodo
    },

    async updateTodo(userId: number, id: number, updates: Partial<Todo>) {
      const existing = this.todos.find(t => t.id === id)
      if (!existing) return

      let updatedTodoVO = null

      // Handle status update
      if (updates.status !== undefined) {
        const statusVal = updates.status === 'done' ? '1' : '0'
        const res = await request.patch<any, ApiResult<any>>(`/api/todos/${id}/status`, { status: statusVal })
        updatedTodoVO = res.data
      }

      // Handle other fields (title, note, category, priority)
      const isTitleChanging = updates.title !== undefined && updates.title !== existing.title
      const isNoteChanging = updates.content !== undefined && updates.content !== existing.content
      const isCategoryChanging = updates.categoryId !== undefined && updates.categoryId !== existing.categoryId
      const isSubcategoryChanging = updates.subcategoryId !== undefined && updates.subcategoryId !== existing.subcategoryId
      const isPriorityChanging = updates.priority !== undefined && updates.priority !== existing.priority

      if (isTitleChanging || isNoteChanging || isCategoryChanging || isSubcategoryChanging || isPriorityChanging) {
        const title = updates.title !== undefined ? updates.title : existing.title
        const note = updates.content !== undefined ? updates.content : existing.content
        
        // Resolve categoryId to send
        const catId = updates.categoryId !== undefined ? updates.categoryId : existing.categoryId
        const subId = updates.subcategoryId !== undefined ? updates.subcategoryId : existing.subcategoryId
        const categoryIdToSend = subId ? subId : catId

        const prio = updates.priority !== undefined ? updates.priority : existing.priority
        const priorityToSend = prio === 'high' ? '2' : (prio === 'low' ? '0' : '1')

        const payload = {
          title: title,
          note: note,
          categoryId: categoryIdToSend,
          priority: priorityToSend
        }

        const res = await request.put<any, ApiResult<any>>(`/api/todos/${id}`, payload)
        updatedTodoVO = res.data
      }

      // Update local state in-place
      const index = this.todos.findIndex(t => t.id === id)
      if (index !== -1) {
        if (updatedTodoVO) {
          const mapped = mapTodoVOToTodo(updatedTodoVO, this.categories, this.subcategoriesMap)
          this.todos[index] = mapped
        } else {
          this.todos[index] = { ...this.todos[index], ...updates }
        }
      }

      await this.refreshPendingCounts(userId)
    },

    async deleteTodo(userId: number, id: number) {
      await request.delete(`/api/todos/${id}`)
      this.todos = this.todos.filter(t => t.id !== id)
      await this.refreshPendingCounts(userId)
      return true
    },

    async hardDeleteTodo(userId: number, id: number) {
      return true
    },

    async clearTrash(userId: number) {
      return true
    },

    // --- Category Actions ---
    async addCategory(userId: number, name: string, color: string) {
      const res = await request.post<any, ApiResult<any>>('/api/categories', { name, color })
      await this.refreshCategories(userId)
      return res.data
    },

    async updateCategory(userId: number, id: number, name: string, color: string) {
      const res = await request.put<any, ApiResult<any>>(`/api/categories/${id}`, { name, color })
      await this.refreshCategories(userId)
      return res.data
    },

    async deleteCategory(userId: number, id: number, strategy?: 'delete_all' | 'migrate', targetCategoryId?: number) {
      await request.delete(`/api/categories/${id}`)
      await this.refreshCategories(userId)
      return true
    },

    // --- Subcategory Actions ---
    async addSubcategory(userId: number, categoryId: number, name: string) {
      const res = await request.post<any, ApiResult<any>>('/api/categories', { parentId: categoryId, name })
      await this.refreshCategories(userId)
      return res.data
    },

    async updateSubcategory(userId: number, id: number, name: string) {
      const res = await request.put<any, ApiResult<any>>(`/api/categories/${id}`, { name })
      await this.refreshCategories(userId)
      return res.data
    },

    async deleteSubcategory(userId: number, id: number, strategy?: 'delete_all' | 'migrate' | 'clear', targetSubcategoryId?: number) {
      await request.delete(`/api/categories/${id}`)
      await this.refreshCategories(userId)
      return true
    },

    // --- Performance Category Actions ---
    async addPerformanceCategory(userId: number, name: string, description = '') {
      const res = await request.post<any, ApiResult<any>>('/api/objectives', { 
        month: this.currentMonth,
        name,
        description
      })
      await this.refreshObjectives(userId)
      return res.data
    },

    async updatePerformanceCategory(userId: number, id: number, name: string, description = '') {
      const res = await request.put<any, ApiResult<any>>(`/api/objectives/${id}`, { name, description })
      await this.refreshObjectives(userId)
      return res.data
    },

    async deletePerformanceCategory(userId: number, id: number) {
      await request.delete(`/api/objectives/${id}`)
      await Promise.all([
        this.refreshObjectives(userId),
        this.refreshSprints(userId)
      ])
      return true
    },

    // --- Performance KR Actions ---
    async addPerformanceKR(userId: number, categoryId: number, title: string, remark = '', status = 'not_started', planCompleteDate = '') {
      let backendStatus = '0'
      if (status === 'done') {
        backendStatus = '2'
      } else if (status === 'in_progress') {
        backendStatus = '1'
      } else if (status === 'cancelled') {
        backendStatus = '3'
      }

      const res = await request.post<any, ApiResult<any>>('/api/key-results', {
        objectiveId: categoryId,
        name: title,
        description: remark,
        status: backendStatus,
        planCompleteDate: planCompleteDate || null
      })
      await Promise.all([
        this.refreshObjectives(userId),
        this.refreshSprints(userId)
      ])
      return mapPerfItemVOToKR(res.data, categoryId, this.currentMonth)
    },

    async updatePerformanceKR(userId: number, id: number, updates: Partial<PerformanceKR> & { cancelReason?: string }) {
      const payload: any = {}
      if (updates.title !== undefined) payload.name = updates.title
      if (updates.remark !== undefined) payload.description = updates.remark
      if (updates.planCompleteDate !== undefined) payload.planCompleteDate = updates.planCompleteDate

      if (Object.keys(payload).length > 0) {
        await request.put(`/api/key-results/${id}`, payload)
      }

      if (updates.status !== undefined) {
        let backendStatus = '0'
        if (updates.status === 'done') {
          backendStatus = '2'
        } else if (updates.status === 'in_progress') {
          backendStatus = '1'
        } else if (updates.status === 'cancelled') {
          backendStatus = '3'
        } else if (updates.status === 'not_started') {
          backendStatus = '0'
        }

        const patchPayload: any = {
          status: backendStatus
        }
        if (updates.cancelReason !== undefined) {
          patchPayload.cancelReason = updates.cancelReason
        }

        await request.patch(`/api/key-results/${id}/status`, patchPayload)
      }

      await Promise.all([
        this.refreshObjectives(userId),
        this.refreshSprints(userId)
      ])
    },

    async deletePerformanceKR(userId: number, id: number) {
      await request.delete(`/api/key-results/${id}`)
      await Promise.all([
        this.refreshObjectives(userId),
        this.refreshSprints(userId)
      ])
      return true
    },

    async carryForwardKR(userId: number, id: number) {
      // 2.0 API does not support carryover, we provide dummy implementation to prevent errors.
      await Promise.all([
        this.refreshObjectives(userId),
        this.refreshSprints(userId)
      ])
      return null
    },

    // --- Team Sprint Actions ---
    async addTeamSprint(userId: number, title: string, remark = '') {
      const res = await request.post<any, ApiResult<any>>('/api/sprint/items', {
        month: this.currentMonth,
        title,
        note: remark
      })
      await this.refreshSprints(userId)
      return mapSprintItemVOToSprint(res.data)
    },

    async updateTeamSprint(userId: number, id: number, updates: Partial<TeamSprint>) {
      const payload: any = {}
      if (updates.title !== undefined) payload.title = updates.title
      if (updates.remark !== undefined) payload.note = updates.remark

      if (updates.status !== undefined) {
        let backendStatus = '0'
        if (updates.status === 'done') backendStatus = '2'
        else if (updates.status === 'in_progress') backendStatus = '1'
        await request.patch(`/api/sprint/items/${id}/status`, { status: backendStatus })
      }

      if (updates.needInvolved !== undefined) {
        await request.patch(`/api/sprint/items/${id}/involved`, { needInvolved: updates.needInvolved })
      }

      if (Object.keys(payload).length > 0) {
        await request.put(`/api/sprint/items/${id}`, payload)
      }

      await this.refreshSprints(userId)
    },

    async deleteTeamSprint(userId: number, id: number) {
      await request.delete(`/api/sprint/items/${id}`)
      await this.refreshSprints(userId)
      return true
    },

    async carryForwardSprint(userId: number, id: number) {
      const res = await request.post<any, ApiResult<any>>(`/api/sprint/items/${id}/carryover`)
      await this.refreshSprints(userId)
      return mapSprintItemVOToSprint(res.data)
    },

    async linkSprintKeyResults(userId: number, sprintId: number, keyResultIds: number[]) {
      const res = await request.put<any, ApiResult<any>>(`/api/sprint/items/${sprintId}/key-results`, {
        keyResultIds
      })
      await Promise.all([
        this.refreshObjectives(userId),
        this.refreshSprints(userId)
      ])
      return mapSprintItemVOToSprint(res.data)
    },

    // --- Derive Action (PRD 6.8.3 / Issue 4) ---
    async deriveTodo(
      userId: number,
      sourceType: 'performance' | 'sprint',
      sourceId: number,
      sourceTitle: string,
      sourceCategoryName: string,
      title: string,
      planDate: string,
      categoryId: number | null,
      subcategoryId: number | null,
      priority: 'high' | 'medium' | 'low',
      userRemark: string
    ) {
      const payload = {
        title,
        planDate,
        categoryId,
        subcategoryId,
        priority,
        note: userRemark
      }
      
      const url = sourceType === 'performance'
        ? `/api/perf/items/${sourceId}/derive-todo`
        : `/api/sprint/items/${sourceId}/derive-todo`

      const res = await request.post<any, ApiResult<any>>(url, payload)
      const newTodo = mapTodoVOToTodo(res.data, this.categories, this.subcategoriesMap)
      this.todos.unshift(newTodo)
      await this.refreshPendingCounts(userId)
      return newTodo
    }
  }
})

// Mappers to bridge backend VOs and frontend models (prevents breaking Vue templates)
export function mapTodoVOToTodo(vo: any, categories?: Category[], subcategoriesMap?: Record<number, Subcategory[]>): Todo {
  let status: 'pending' | 'done' = 'pending'
  if (vo.status === '1' || vo.status === 'done') {
    status = 'done'
  }
  
  let priority: 'high' | 'medium' | 'low' = 'medium'
  if (vo.priority === '2' || vo.priority === 'high') {
    priority = 'high'
  } else if (vo.priority === '0' || vo.priority === 'low') {
    priority = 'low'
  }

  const planDate = vo.createdAt ? vo.createdAt.split('T')[0] : new Date().toISOString().split('T')[0]

  // Resolve category and subcategory from the flat categoryId returned by backend
  let categoryId: number | null = null
  let subcategoryId: number | null = null
  
  if (vo.categoryId) {
    const voCatId = Number(vo.categoryId)
    let cats = categories
    let subMap = subcategoriesMap
    
    if (!cats || !subMap) {
      try {
        const store = useTodoStore()
        cats = store.categories
        subMap = store.subcategoriesMap
      } catch (e) {
        // Fallback if pinia not active yet
      }
    }
    
    if (cats && subMap) {
      let foundSub = false
      for (const parentId in subMap) {
        const subs = subMap[parentId] || []
        const sub = subs.find(s => s.id === voCatId)
        if (sub) {
          categoryId = Number(parentId)
          subcategoryId = voCatId
          foundSub = true
          break
        }
      }
      if (!foundSub) {
        categoryId = voCatId
        subcategoryId = null
      }
    } else {
      categoryId = voCatId
    }
  }

  return {
    id: Number(vo.id),
    userId: 0,
    title: vo.title,
    content: vo.note || '',
    planDate: planDate,
    priority: priority,
    status: status,
    isPinned: false,
    reminderTime: null,
    categoryId,
    subcategoryId,
    doneAt: vo.doneAt,
    createdAt: vo.createdAt,
    derivedFromType: null,
    derivedFromId: null,
    rolloverCount: 0
  }
}

function mapPerfItemVOToKR(vo: any, categoryId = 0, month = ''): PerformanceKR {
  let status: 'not_started' | 'in_progress' | 'done' | 'cancelled' = 'not_started'
  if (vo.status === '2' || vo.status === 'done') {
    status = 'done'
  } else if (vo.status === '1' || vo.status === 'in_progress') {
    status = 'in_progress'
  } else if (vo.status === '3' || vo.status === 'cancelled') {
    status = 'cancelled'
  } else if (vo.status === '0' || vo.status === 'not_started') {
    status = 'not_started'
  }

  return {
    id: Number(vo.id),
    userId: 0,
    month: month || vo.month || '',
    categoryId: Number(categoryId || vo.objectiveId || 0),
    title: vo.name || vo.title || '',
    status,
    doneAt: vo.completeDate ? vo.completeDate.toString() : null,
    planCompleteDate: vo.planCompleteDate || null,
    remark: vo.description || vo.note || '',
    sourceSprintId: null,
    derivedTodoIds: vo.derivedTodoIds ? vo.derivedTodoIds.map(Number) : [],
    sprintIds: vo.sprintIds ? vo.sprintIds.map(Number) : []
  }
}

function mapSprintItemVOToSprint(vo: any): TeamSprint {
  let status: 'not_started' | 'in_progress' | 'done' = 'not_started'
  if (vo.status === '2' || vo.status === 'done') {
    status = 'done'
  } else if (vo.status === '1' || vo.status === 'in_progress') {
    status = 'in_progress'
  } else if (vo.status === '0' || vo.status === 'not_started') {
    status = 'not_started'
  }

  return {
    id: Number(vo.id),
    userId: 0,
    month: vo.month,
    title: vo.title,
    status,
    needInvolved: !!vo.needInvolved,
    owner: vo.owner || '',
    module: vo.module || '',
    remark: vo.note || '',
    keyResultIds: vo.keyResultIds ? vo.keyResultIds.map(Number) : []
  }
}
