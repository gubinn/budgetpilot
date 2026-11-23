<template>
  <div class="mobile-categories m-page">
    <!-- 搜索 + 类型切换 -->
    <section class="m-card search-card">
      <n-input v-model:value="keyword" placeholder="搜索分类" clearable size="large" class="search-input">
        <template #prefix><n-icon :size="16"><SearchOutline /></n-icon></template>
      </n-input>
      <div class="m-chip-scroll type-tabs">
        <button class="m-chip" :class="{ active: filterType === null }" @click="filterType = null">全部</button>
        <button class="m-chip" :class="{ active: filterType === 1 }" @click="filterType = 1">支出</button>
        <button class="m-chip" :class="{ active: filterType === 2 }" @click="filterType = 2">收入</button>
      </div>
    </section>

    <div class="m-section-head list-head">
      <span class="m-muted">共 {{ groups.length }} 个父分类 · {{ categoryCount }} 个分类</span>
      <n-button text type="primary" size="small" @click="toggleAll">{{ allExpanded ? '全部收起' : '全部展开' }}</n-button>
    </div>

    <!-- 分类列表：父分类可折叠 -->
    <div class="cat-list">
      <section v-for="group in visibleGroups" :key="group.id" class="cat-group" :class="{ open: isExpanded(group) }">
        <div class="cat-row parent" @click="toggle(group.id)">
          <span class="color-dot" :style="{ background: group.color || '#cbd5e1' }"></span>
          <span class="cat-name m-ellipsis">{{ group.name }}</span>
          <n-tag
            v-if="group.type === 1 && group.budgetType && group.budgetType !== 'DAILY'"
            size="tiny"
            :type="budgetTagType(group.budgetType)"
            :bordered="false"
          >{{ budgetTypeLabels[group.budgetType] }}</n-tag>
          <n-tag v-if="group.isSystem" size="tiny" type="info" :bordered="false">系统</n-tag>
          <span class="child-count">{{ group.children.length }} 项</span>
          <n-dropdown trigger="click" placement="bottom-end" :options="catActions(group)" @select="key => handleAction(key, group)">
            <button class="m-more" @click.stop aria-label="分类操作">
              <n-icon :size="18"><EllipsisHorizontal /></n-icon>
            </button>
          </n-dropdown>
          <n-icon class="chevron" :class="{ rotated: isExpanded(group) }" :size="16">
            <ChevronDownOutline />
          </n-icon>
        </div>

        <div v-if="isExpanded(group)" class="cat-children">
          <div v-for="child in group.children" :key="child.id" class="cat-row child" @click="handleEdit(child)">
            <span class="color-dot small" :style="{ background: child.color || '#e2e8f0' }"></span>
            <span class="cat-name m-ellipsis">{{ child.name }}</span>
            <n-tag
              v-if="child.type === 1 && child.budgetType && child.budgetType !== 'DAILY'"
              size="tiny"
              :type="budgetTagType(child.budgetType)"
              :bordered="false"
            >{{ budgetTypeLabels[child.budgetType] }}</n-tag>
            <span v-if="child.transactionCount" class="tx-count">{{ child.transactionCount }} 笔</span>
            <n-dropdown trigger="click" placement="bottom-end" :options="catActions(child)" @select="key => handleAction(key, child)">
              <button class="m-more" @click.stop aria-label="分类操作">
                <n-icon :size="18"><EllipsisHorizontal /></n-icon>
              </button>
            </n-dropdown>
          </div>
          <div v-if="!group.children.length" class="empty-children">暂无子分类</div>
        </div>
      </section>

      <div v-if="!visibleGroups.length" class="m-card">
        <div class="m-empty-cta">
          <div class="cta-icon"><n-icon :size="26"><FolderOutline /></n-icon></div>
          <div class="cta-text">{{ keyword ? '没有匹配的分类' : '还没有分类' }}</div>
          <n-button v-if="!keyword" type="primary" round @click="openCreate">新增分类</n-button>
        </div>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <n-drawer v-model:show="showCreate" :height="'72vh'" placement="bottom">
      <n-drawer-content :title="editingId ? '编辑分类' : '新增分类'" closable>
        <n-form ref="formRef" :model="form" label-placement="top" label-width="auto" class="m-form">
          <n-form-item label="名称" required>
            <n-input v-model:value="form.name" placeholder="如：餐饮" size="large" />
          </n-form-item>
          <n-form-item label="类型" required>
            <n-select v-model:value="form.type" :options="typeOptions" size="large" />
          </n-form-item>
          <n-form-item label="父分类">
            <n-select v-model:value="form.parentId" :options="parentOptions" clearable placeholder="顶级分类" size="large" />
          </n-form-item>
          <n-form-item v-if="form.type === 1" label="预算属性">
            <n-select v-model:value="form.budgetType" :options="budgetTypeOptions" size="large" />
          </n-form-item>
          <n-form-item label="颜色">
            <n-color-picker v-model:value="form.color" :show-alpha="false" />
          </n-form-item>
          <n-form-item label="图标">
            <n-input v-model:value="form.icon" placeholder="图标标识" size="large" />
          </n-form-item>
        </n-form>
        <div class="m-sheet-actions">
          <n-button type="primary" :loading="submitting" @click="handleSubmit">保存</n-button>
          <n-button quaternary @click="showCreate = false">取消</n-button>
        </div>
      </n-drawer-content>
    </n-drawer>

    <div v-if="categories.length" class="fab">
      <n-button type="primary" circle size="large" aria-label="新增分类" @click="openCreate">
        <template #icon><n-icon :size="24"><AddOutline /></n-icon></template>
      </n-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useDialog, useMessage } from 'naive-ui'
import { categoryApi } from '@/api'
import {
  AddOutline, SearchOutline, ChevronDownOutline,
  EllipsisHorizontal, FolderOutline
} from '@vicons/ionicons5'

const dialog = useDialog()
const message = useMessage()
const filterType = ref(null)
const keyword = ref('')
const categories = ref([])
const showCreate = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const expandedIds = ref(new Set())

const form = ref({ name: '', type: 1, parentId: 0, budgetType: 'DAILY', color: '#3498db', icon: '' })

const typeOptions = [
  { label: '支出', value: 1 },
  { label: '收入', value: 2 }
]
const budgetTypeOptions = [
  { label: '日常（月度预算）', value: 'DAILY' },
  { label: '固定（年度预算）', value: 'FIXED' },
  { label: '专项（年度预算）', value: 'SPECIAL' },
  { label: '非预算流水', value: 'NONE' }
]
const budgetTypeLabels = {
  FIXED: '固定',
  SPECIAL: '专项',
  NONE: '非预算'
}

function budgetTagType(type) {
  return type === 'FIXED' ? 'default' : type === 'SPECIAL' ? 'warning' : 'error'
}

const parentOptions = computed(() => {
  // 混合类型父分类（如「往来款」）在收入/支出两种表单里都要可选
  const roots = categories.value.filter(c => c.parentId === 0 && (
    c.type === form.value.type ||
    categories.value.some(child => child.parentId === c.id && child.type === form.value.type)
  ))
  return [{ label: '顶级分类', value: 0 }, ...roots.map(c => ({ label: c.name, value: c.id }))]
})

// 按类型筛选时保留父级容器，避免子分类因为父分类类型不同而消失
function includeAncestors(list, type) {
  const byId = new Map(list.map(c => [c.id, c]))
  const keep = new Set()
  for (const c of list) {
    if (c.type !== type) continue
    keep.add(c.id)
    let parent = byId.get(c.parentId)
    while (parent) {
      keep.add(parent.id)
      parent = byId.get(parent.parentId)
    }
  }
  return list.filter(c => keep.has(c.id))
}

const baseList = computed(() =>
  filterType.value ? includeAncestors(categories.value, filterType.value) : categories.value
)

const groups = computed(() => {
  const roots = baseList.value.filter(c => c.parentId === 0)
  return roots.map(root => ({
    ...root,
    isParent: true,
    children: baseList.value
      .filter(c => c.parentId === root.id)
      .map(c => ({ ...c, isParent: false }))
  }))
})

const categoryCount = computed(() => categories.value.length)

const visibleGroups = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return groups.value
  return groups.value
    .map(g => {
      if (g.name.toLowerCase().includes(kw)) return g
      const kids = g.children.filter(c => c.name.toLowerCase().includes(kw))
      return kids.length ? { ...g, children: kids } : null
    })
    .filter(Boolean)
})

const allExpanded = computed(() =>
  groups.value.length > 0 && groups.value.every(g => expandedIds.value.has(g.id))
)

function isExpanded(group) {
  return !!keyword.value.trim() || expandedIds.value.has(group.id)
}

function toggle(id) {
  const next = new Set(expandedIds.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  expandedIds.value = next
}

function toggleAll() {
  if (allExpanded.value) expandedIds.value = new Set()
  else expandedIds.value = new Set(groups.value.map(g => g.id))
}

function catActions(cat) {
  const actions = [{ label: '编辑分类', key: 'edit' }]
  if (!cat.isSystem) actions.push({ label: '删除分类', key: 'delete' })
  return actions
}

function handleAction(key, cat) {
  if (key === 'edit') handleEdit(cat)
  else if (key === 'delete') handleDelete(cat.id)
}

function openCreate() {
  editingId.value = null
  form.value = { name: '', type: filterType.value || 1, parentId: 0, budgetType: 'DAILY', color: '#3498db', icon: '' }
  showCreate.value = true
}

function handleEdit(row) {
  editingId.value = row.id
  form.value = { name: row.name, type: row.type, parentId: row.parentId, budgetType: row.budgetType || 'DAILY', color: row.color, icon: row.icon || '' }
  showCreate.value = true
}

async function handleSubmit() {
  if (!form.value.name) { message.error('请输入名称'); return }
  submitting.value = true
  try {
    if (editingId.value) {
      await categoryApi.update(editingId.value, form.value)
      message.success('更新成功')
    } else {
      await categoryApi.create(form.value)
      message.success('创建成功')
    }
    showCreate.value = false
    editingId.value = null
    loadCategories()
  } catch (e) { message.error('操作失败') } finally { submitting.value = false }
}

function handleDelete(id) {
  dialog.warning({
    title: '确认删除', content: '删除后不可恢复，是否继续？',
    positiveText: '删除', negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await categoryApi.remove(id)
        message.success('已删除')
        loadCategories()
      } catch (e) { message.error('删除失败') }
    }
  })
}

async function loadCategories() {
  try {
    const res = await categoryApi.tree(null)
    const flat = []
    function flatten(list) {
      list.forEach(c => { flat.push(c); if (c.children?.length) flatten(c.children) })
    }
    flatten(res.data)
    categories.value = flat
  } catch (e) { message.error('加载失败') }
}

onMounted(loadCategories)
</script>

<style scoped>
.mobile-categories {
  padding-bottom: calc(88px + env(safe-area-inset-bottom, 0px));
}

.search-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.search-input :deep(.n-input__input-el) {
  height: 42px;
}

.list-head {
  margin-bottom: -2px;
  padding: 0 4px;
}

.cat-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.cat-group {
  background: #fff;
  border-radius: var(--m-radius);
  box-shadow: var(--bp-shadow);
  overflow: hidden;
}

.cat-row {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 0 10px 0 16px;
  min-height: 52px;
  -webkit-tap-highlight-color: transparent;
}
.cat-row:active {
  background: #f8fafc;
}
.cat-row.parent {
  cursor: pointer;
}
.cat-group.open .cat-row.parent {
  border-bottom: 1px solid #f1f5f9;
}

.color-dot {
  flex-shrink: 0;
  width: 10px;
  height: 10px;
  border-radius: 50%;
}
.color-dot.small {
  width: 7px;
  height: 7px;
  margin-left: 1px;
}

.cat-name {
  font-size: 15px;
  font-weight: 550;
  color: var(--bp-ink);
}
.cat-row.child .cat-name {
  font-weight: 400;
  font-size: 14px;
}

.child-count,
.tx-count {
  margin-left: auto;
  flex-shrink: 0;
  font-size: 12px;
  color: #b0b8c4;
  font-variant-numeric: tabular-nums;
}

.chevron {
  flex-shrink: 0;
  color: #b0b8c4;
  transition: transform 0.18s ease;
}
.chevron.rotated {
  transform: rotate(180deg);
}

.cat-children {
  background: #fcfdff;
  padding: 2px 0;
}
.cat-row.child {
  min-height: 48px;
  padding-left: 35px;
}
.empty-children {
  padding: 12px 16px 14px 35px;
  font-size: 12px;
  color: #b0b8c4;
}

.fab {
  position: fixed;
  bottom: calc(76px + env(safe-area-inset-bottom));
  right: 16px;
  z-index: 80;
}
</style>