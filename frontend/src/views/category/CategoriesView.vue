<template>
  <div class="categories-view">
    <div class="view-header">
      <h1>分类管理</h1>
      <p class="subtitle">管理您的两层分类体系（大分类 > 子分类），组织任务井井有条</p>
    </div>

    <div class="category-grid">
      <!-- Left Panel: Big Categories Master List -->
      <div class="panel-card premium-card">
        <div class="panel-header">
          <h2>大分类列表</h2>
          <button class="add-btn" @click="openCategoryModal(null)" title="新建大分类">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
            </svg>
            <span>新建</span>
          </button>
        </div>

        <div class="master-list">
          <div 
            v-for="cat in categories" 
            :key="cat.id" 
            class="category-item" 
            :class="{ active: selectedCategoryId === cat.id }"
            @click="selectedCategoryId = cat.id"
          >
            <span class="color-indicator" :style="{ backgroundColor: cat.color }"></span>
            <span class="category-name">{{ cat.name }}</span>
            
            <div class="actions" @click.stop>
              <!-- Edit -->
              <button class="icon-btn" @click="openCategoryModal(cat)" title="编辑名称与颜色">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10" />
                </svg>
              </button>
              <!-- Delete -->
              <button class="icon-btn danger-hover" @click="promptDeleteCategory(cat)" title="删除分类">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" />
                </svg>
              </button>
            </div>
          </div>
          <div v-if="categories.length === 0" class="empty-list-info">暂无分类，请点击上方“新建”添加</div>
        </div>
      </div>

      <!-- Right Panel: Nested Subcategories Detail List -->
      <div class="panel-card premium-card">
        <template v-if="selectedCategory">
          <div class="panel-header">
            <h2>【{{ selectedCategory.name }}】的子分类</h2>
            <button class="add-btn" @click="openSubcatModal(null)" :style="{ backgroundColor: selectedCategory.color }">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
              </svg>
              <span>添加子分类</span>
            </button>
          </div>

          <div class="master-list">
            <div 
              v-for="sub in subcategories" 
              :key="sub.id" 
              class="subcategory-item"
            >
              <span class="sub-bullet" :style="{ borderColor: selectedCategory.color }"></span>
              <span class="subcategory-name">{{ sub.name }}</span>

              <div class="actions">
                <!-- Edit -->
                <button class="icon-btn" @click="openSubcatModal(sub)">
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10" />
                  </svg>
                </button>
                <!-- Delete -->
                <button class="icon-btn danger-hover" @click="promptDeleteSubcategory(sub)">
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" />
                  </svg>
                </button>
              </div>
            </div>
            
            <div v-if="subcategories.length === 0" class="empty-list-info">
              本大分类下暂无子分类。点击上方按钮可快速添加！
            </div>
          </div>
        </template>
        
        <div v-else class="empty-state">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" d="M8.25 6.75h12M8.25 12h12m-12 5.25h12M3.75 6.75h.007v.008H3.75V6.75zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zM3.75 12h.007v.008H3.75V12zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm-.375 5.25h.007v.008H3.75v-.008zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0z" />
          </svg>
          <h3>选择大分类</h3>
          <p>请点击左侧大分类以管理其关联的二级子分类列表。</p>
        </div>
      </div>
    </div>

    <!-- 1. Category Form Modal (Add / Edit) -->
    <div v-if="catModal.isOpen" class="modal-overlay" @mousedown="mousedownTarget = $event.target" @click="mousedownTarget === $event.currentTarget && $event.target === $event.currentTarget && (catModal.isOpen = false)">
      <div class="modal-content" @click.stop>
        <h3>{{ catModal.isEdit ? '编辑大分类' : '新建大分类' }}</h3>
        
        <form @submit.prevent="saveCategory">
          <div class="form-field">
            <label for="cat-name">大分类名称</label>
            <input id="cat-name" v-model="catModal.name" type="text" maxlength="20" required class="form-control" placeholder="如 公司事务、生活事务等" autocomplete="off" />
          </div>

          <div class="form-field">
            <label>视觉颜色标记</label>
            <div class="color-picker-grid">
              <span 
                v-for="color in presetColors" 
                :key="color" 
                class="color-preset-dot"
                :style="{ backgroundColor: color }"
                :class="{ selected: catModal.color === color }"
                @click="catModal.color = color"
              ></span>
            </div>
          </div>

          <div class="modal-actions">
            <button type="button" class="btn btn-secondary" @click="catModal.isOpen = false">取消</button>
            <button type="submit" class="btn btn-primary">保存</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 2. Subcategory Form Modal (Add / Edit) -->
    <div v-if="subcatModal.isOpen" class="modal-overlay" @mousedown="mousedownTarget = $event.target" @click="mousedownTarget === $event.currentTarget && $event.target === $event.currentTarget && (subcatModal.isOpen = false)">
      <div class="modal-content" @click.stop>
        <h3>{{ subcatModal.isEdit ? '编辑子分类' : '新建子分类' }}</h3>
        
        <form @submit.prevent="saveSubcategory">
          <div class="form-field">
            <label for="sub-name">子分类名称</label>
            <input id="sub-name" v-model="subcatModal.name" type="text" maxlength="20" required class="form-control" placeholder="请输入子分类名称" autocomplete="off" />
          </div>

          <div class="modal-actions">
            <button type="button" class="btn btn-secondary" @click="subcatModal.isOpen = false">取消</button>
            <button type="submit" class="btn btn-primary" :style="{ backgroundColor: selectedCategory?.color }">保存</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 3. Delete Category Confirmation Modal (PRD 6.4.1) -->
    <div v-if="delCatModal.isOpen" class="modal-overlay" @mousedown="mousedownTarget = $event.target" @click="mousedownTarget === $event.currentTarget && $event.target === $event.currentTarget && (delCatModal.isOpen = false)">
      <div class="modal-content delete-strategy-modal" @click.stop>
        <div class="danger-header">
          <span class="warning-icon">⚠️</span>
          <h3>删除分类确认</h3>
        </div>

        <p class="modal-alert-desc">
          您将要删除大分类【<strong>{{ delCatModal.name }}</strong>】。
        </p>

        <div style="background-color: #fef2f2; border: 1.5px solid #fee2e2; padding: 12px; border-radius: var(--radius-md); font-size: 13px; color: #991b1b; line-height: 1.5; margin-bottom: 20px;">
          ℹ️ <strong>注意：</strong>若该大分类下包含子分类或直接关联的待办任务，系统将拒绝删除。请在删除前确保已清空其所有的子分类和待办任务。
        </div>

        <div class="modal-actions">
          <button type="button" class="btn btn-secondary" @click="delCatModal.isOpen = false">取消</button>
          <button 
            type="button" 
            class="btn btn-danger" 
            @click="executeCategoryDelete"
          >
            确认执行删除
          </button>
        </div>
      </div>
    </div>

    <!-- 4. Delete Subcategory Modal -->
    <div v-if="delSubModal.isOpen" class="modal-overlay" @mousedown="mousedownTarget = $event.target" @click="mousedownTarget === $event.currentTarget && $event.target === $event.currentTarget && (delSubModal.isOpen = false)">
      <div class="modal-content delete-strategy-modal" @click.stop>
        <div class="danger-header">
          <span class="warning-icon">⚠️</span>
          <h3>删除子分类确认</h3>
        </div>

        <p class="modal-alert-desc">
          您将要删除子分类【<strong>{{ delSubModal.name }}</strong>】。
        </p>

        <div style="background-color: #eff6ff; border: 1.5px solid #dbeafe; padding: 12px; border-radius: var(--radius-md); font-size: 13px; color: #1e40af; line-height: 1.5; margin-bottom: 20px;">
          ℹ️ <strong>注意：</strong>删除后，该子分类下关联的待办任务将自动迁移到父分类【<strong>{{ selectedCategory?.name }}</strong>】下。
        </div>

        <div class="modal-actions">
          <button type="button" class="btn btn-secondary" @click="delSubModal.isOpen = false">取消</button>
          <button 
            type="button" 
            class="btn btn-danger" 
            @click="executeSubcategoryDelete"
          >
            确认执行删除
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useTodoStore } from '@/stores/todo'
import type { Category, Subcategory } from '@/stores/todo'

const authStore = useAuthStore()
const todoStore = useTodoStore()

const selectedCategoryId = ref<number | null>(null)
const mousedownTarget = ref<EventTarget | null>(null)

// Preset design colors for categories (PRD 6.4.1)
const presetColors = [
  '#6366f1', // Indigo
  '#ec4899', // Pink
  '#14b8a6', // Teal
  '#f59e0b', // Amber
  '#10b981', // Emerald
  '#06b6d4', // Cyan
  '#a855f7', // Purple
  '#ef4444'  // Red
]

// All Big Categories
const categories = computed(() => todoStore.sortedCategories)

// Selected Big Category detail
const selectedCategory = computed(() => {
  if (selectedCategoryId.value === null) return null
  return categories.value.find(c => c.id === selectedCategoryId.value) || null
})

// Subcategories under selected category
const subcategories = computed(() => {
  if (selectedCategoryId.value === null) return []
  return todoStore.subcategoriesByCategoryId(selectedCategoryId.value)
})

onMounted(() => {
  if (authStore.currentUser) {
    todoStore.refreshCategories(authStore.currentUser.userId)
    // Auto select first category if available
    if (categories.value.length > 0) {
      selectedCategoryId.value = categories.value[0].id
    }
  }
})

// Modals reactive states
const catModal = reactive({
  isOpen: false,
  isEdit: false,
  id: 0,
  name: '',
  color: presetColors[0]
})

const subcatModal = reactive({
  isOpen: false,
  isEdit: false,
  id: 0,
  name: ''
})

const delCatModal = reactive({
  isOpen: false,
  id: 0,
  name: ''
})

const delSubModal = reactive({
  isOpen: false,
  sourceSubcatId: 0,
  name: ''
})

// --- Category CRUD Actions ---
const openCategoryModal = (cat: Category | null) => {
  if (cat) {
    catModal.isEdit = true
    catModal.id = cat.id
    catModal.name = cat.name
    catModal.color = cat.color
  } else {
    catModal.isEdit = false
    catModal.id = 0
    catModal.name = ''
    catModal.color = presetColors[0]
  }
  catModal.isOpen = true
}

const saveCategory = async () => {
  if (!authStore.currentUser) return
  try {
    if (catModal.isEdit) {
      await todoStore.updateCategory(authStore.currentUser.userId, catModal.id, catModal.name, catModal.color)
      const event = new CustomEvent('app-toast', { detail: { text: '大分类保存成功！' } })
      window.dispatchEvent(event)
    } else {
      const newCat = await todoStore.addCategory(authStore.currentUser.userId, catModal.name, catModal.color)
      selectedCategoryId.value = newCat.id
      const event = new CustomEvent('app-toast', { detail: { text: '成功创建大分类！' } })
      window.dispatchEvent(event)
    }
    catModal.isOpen = false
  } catch (err: any) {
    // Global toast handler will display the error
  }
}

const promptDeleteCategory = (cat: Category) => {
  delCatModal.id = cat.id
  delCatModal.name = cat.name
  delCatModal.isOpen = true
}

const executeCategoryDelete = async () => {
  if (!authStore.currentUser) return
  try {
    await todoStore.deleteCategory(
      authStore.currentUser.userId, 
      delCatModal.id
    )
    
    delCatModal.isOpen = false
    selectedCategoryId.value = categories.value.length > 0 ? categories.value[0].id : null
    
    const event = new CustomEvent('app-toast', { detail: { text: '分类删除成功' } })
    window.dispatchEvent(event)
  } catch (err: any) {
    // Global toast handler will display the error
  }
}

// --- Subcategory CRUD Actions ---
const openSubcatModal = (sub: Subcategory | null) => {
  if (sub) {
    subcatModal.isEdit = true
    subcatModal.id = sub.id
    subcatModal.name = sub.name
  } else {
    subcatModal.isEdit = false
    subcatModal.id = 0
    subcatModal.name = ''
  }
  subcatModal.isOpen = true
}

const saveSubcategory = async () => {
  if (!authStore.currentUser || selectedCategoryId.value === null) return
  try {
    if (subcatModal.isEdit) {
      await todoStore.updateSubcategory(authStore.currentUser.userId, subcatModal.id, subcatModal.name)
      const event = new CustomEvent('app-toast', { detail: { text: '子分类保存成功！' } })
      window.dispatchEvent(event)
    } else {
      await todoStore.addSubcategory(authStore.currentUser.userId, selectedCategoryId.value, subcatModal.name)
      const event = new CustomEvent('app-toast', { detail: { text: '成功创建子分类！' } })
      window.dispatchEvent(event)
    }
    subcatModal.isOpen = false
  } catch (err: any) {
    // Global toast handler will display the error
  }
}

const promptDeleteSubcategory = (sub: Subcategory) => {
  delSubModal.sourceSubcatId = sub.id
  delSubModal.name = sub.name
  delSubModal.isOpen = true
}

const executeSubcategoryDelete = async () => {
  if (!authStore.currentUser) return
  try {
    await todoStore.deleteSubcategory(
      authStore.currentUser.userId,
      delSubModal.sourceSubcatId
    )

    delSubModal.isOpen = false
    const event = new CustomEvent('app-toast', { detail: { text: '子分类删除成功' } })
    window.dispatchEvent(event)
  } catch (err: any) {
    // Global toast handler will display the error
  }
}
</script>

<style scoped>
.categories-view {
  max-width: 1100px;
  margin: 0 auto;
  padding-bottom: 60px;
}

.view-header {
  margin-bottom: 32px;
}
.view-header h1 {
  font-size: 28px;
  font-weight: 800;
  color: var(--text-main);
  margin-bottom: 4px;
}
.subtitle {
  font-size: 14px;
  color: var(--text-muted);
  font-weight: 500;
}

.category-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  align-items: start;
}
@media (max-width: 768px) {
  .category-grid {
    grid-template-columns: 1fr;
  }
}

.panel-card {
  padding: 24px;
  min-height: 400px;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  border-bottom: 1.5px solid var(--border-light);
  padding-bottom: 12px;
}
.panel-header h2 {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-main);
}
.add-btn {
  border: none;
  background-color: var(--primary);
  color: #fff;
  border-radius: var(--radius-md);
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: all var(--transition-fast);
}
.add-btn:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}
.add-btn svg {
  width: 14px;
  height: 14px;
}

.master-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
  max-height: 450px;
}

.category-item,
.subcategory-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: var(--radius-md);
  background-color: var(--bg-app);
  border: 1px solid var(--border-medium);
  cursor: pointer;
  transition: all var(--transition-fast);
}
.category-item:hover,
.subcategory-item:hover {
  border-color: var(--primary);
  background-color: var(--primary-light);
}
.category-item.active {
  background-color: var(--primary-light);
  border-color: var(--primary);
  box-shadow: var(--shadow-sm);
}

.color-indicator {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
}
.category-name,
.subcategory-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
  flex: 1;
}

.sub-bullet {
  width: 6px;
  height: 6px;
  border: 2px solid;
  border-radius: 50%;
  flex-shrink: 0;
}

.actions {
  display: flex;
  gap: 4px;
}
.icon-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--text-muted);
  padding: 4px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}
.icon-btn:hover {
  background-color: var(--border-medium);
  color: var(--text-main);
}
.icon-btn.danger-hover:hover {
  background-color: var(--danger-bg);
  color: var(--danger);
}
.icon-btn svg {
  width: 15px;
  height: 15px;
}

.empty-list-info {
  font-size: 13px;
  color: var(--text-muted);
  text-align: center;
  padding: 40px 0;
  font-style: italic;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  color: var(--text-muted);
  text-align: center;
}
.empty-state svg {
  width: 64px;
  height: 64px;
  color: var(--border-medium);
  margin-bottom: 12px;
}

/* Modals */
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
  padding: 32px;
  width: 100%;
  max-width: 440px;
  box-shadow: var(--shadow-lg);
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.modal-content h3 {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-main);
  border-bottom: 1px solid var(--border-light);
  padding-bottom: 8px;
}
.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
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
}

.color-picker-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 10px;
}
.color-preset-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  cursor: pointer;
  transition: all var(--transition-fast);
  border: 2px solid transparent;
}
.color-preset-dot:hover {
  transform: scale(1.1);
}
.color-preset-dot.selected {
  border-color: #0f172a;
  transform: scale(1.15);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 10px;
}
.btn {
  padding: 10px 20px;
  border-radius: var(--radius-md);
  font-size: 13px;
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
}
.btn-danger {
  background-color: var(--danger);
  color: #fff;
}
.btn-danger:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Delete strategy details */
.delete-strategy-modal {
  max-width: 500px;
}
.danger-header {
  display: flex;
  align-items: center;
  gap: 10px;
  border-bottom: 1.5px solid var(--danger-bg);
  padding-bottom: 12px;
}
.warning-icon {
  font-size: 24px;
}
.danger-header h3 {
  border: none;
  padding: 0;
  color: var(--danger);
}
.modal-alert-desc {
  font-size: 13.5px;
  line-height: 1.5;
  color: var(--text-main);
}
.strategy-options {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 8px 0;
}
.strategy-option-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px;
  border: 1.5px solid var(--border-medium);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-fast);
}
.strategy-option-card:hover {
  border-color: var(--primary);
  background-color: var(--primary-light);
}
.strategy-option-card.selected {
  border-color: var(--primary);
  background-color: var(--primary-light);
}
.strategy-option-card input {
  margin-top: 4px;
  cursor: pointer;
}
.option-desc h4 {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-main);
  margin-bottom: 4px;
}
.option-desc p {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.4;
}
.migration-select-field {
  background-color: var(--bg-app);
  border-radius: var(--radius-md);
  padding: 12px 16px;
}
</style>
