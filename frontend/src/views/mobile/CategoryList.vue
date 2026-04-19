<template>
  <div class="mobile-categories">
    <!-- 类型切换 -->
    <div class="type-tabs">
      <n-button :type="filterType === null ? 'primary' : 'default'" size="small" @click="filterType = null">全部</n-button>
      <n-button :type="filterType === 1 ? 'primary' : 'default'" size="small" @click="filterType = 1">支出</n-button>
      <n-button :type="filterType === 2 ? 'primary' : 'default'" size="small" @click="filterType = 2">收入</n-button>
    </div>

    <!-- 分类列表 -->
    <div class="cat-list">
      <template v-for="cat in filteredCats" :key="cat.id">
        <div class="cat-item" :class="{ 'is-parent': cat.isParent, 'is-child': !cat.isParent }">
          <div class="cat-left">
            <span class="color-dot" :style="{ background: cat.color || '#ccc' }"></span>
            <span class="cat-name">{{ cat.name }}</span>
            <n-tag v-if="cat.isSystem" size="tiny" type="info" :bordered="false">系统</n-tag>
          </div>
          <div class="cat-actions">
            <n-button size="tiny" type="primary" secondary @click="handleEdit(cat)">编辑</n-button>
            <n-button v-if="!cat.isSystem" size="tiny" type="error" secondary @click="handleDelete(cat.id)">删除</n-button>
          </div>
        </div>
      </template>
    </div>

    <!-- 新增/编辑弹窗 -->
    <n-drawer v-model:show="showCreate" :height="'55vh'" placement="bottom">
      <n-drawer-content :title="editingId ? '编辑分类' : '新增分类'" closable>
        <n-form ref="formRef" :model="form" label-placement="top" label-width="auto">
          <n-form-item label="名称" required>
            <n-input v-model:value="form.name" placeholder="如：餐饮" />
          </n-form-item>
          <n-form-item label="类型" required>
            <n-select v-model:value="form.type" :options="typeOptions" />
          </n-form-item>
          <n-form-item label="父分类">
            <n-select v-model:value="form.parentId" :options="parentOptions" clearable placeholder="顶级分类" />
          </n-form-item>
          <n-form-item label="颜色">
            <n-color-picker v-model:value="form.color" :show-alpha="false" />
          </n-form-item>
          <n-form-item label="图标">
            <n-input v-model:value="form.icon" placeholder="图标标识" />
          </n-form-item>
        </n-form>
        <template #footer>
          <n-space>
            <n-button type="primary" :loading="submitting" @click="handleSubmit">保存</n-button>
            <n-button @click="showCreate = false">取消</n-button>
          </n-space>
        </template>
      </n-drawer-content>
    </n-drawer>

    <div class="fab">
      <n-button type="primary" round size="large" @click="showCreate = true">
        <template #icon><n-icon><AddOutline /></n-icon></template>
        新增分类
      </n-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useDialog, useMessage } from 'naive-ui'
import { categoryApi } from '@/api'
import { AddOutline } from '@vicons/ionicons5'

const dialog = useDialog()
const message = useMessage()
const filterType = ref(null)
const categories = ref([])
const showCreate = ref(false)
const editingId = ref(null)
const submitting = ref(false)

const form = ref({ name: '', type: 1, parentId: 0, color: '#3498db', icon: '' })

const typeOptions = [
  { label: '支出', value: 1 },
  { label: '收入', value: 2 }
]

const parentOptions = computed(() => {
  const roots = categories.value.filter(c => c.parentId === 0 && c.type === form.value.type)
  return [{ label: '顶级分类', value: 0 }, ...roots.map(c => ({ label: c.name, value: c.id }))]
})

const filteredCats = computed(() => {
  const filtered = filterType.value
    ? categories.value.filter(c => c.type === filterType.value)
    : categories.value
  // 排序：父级在前，子级紧跟在父级后面
  const sorted = []
  const roots = filtered.filter(c => c.parentId === 0)
  const byParent = {}
  filtered.filter(c => c.parentId !== 0).forEach(c => {
    if (!byParent[c.parentId]) byParent[c.parentId] = []
    byParent[c.parentId].push(c)
  })
  roots.forEach(r => {
    sorted.push({ ...r, isParent: true })
    if (byParent[r.id]) {
      byParent[r.id].forEach(child => sorted.push({ ...child, isParent: false }))
    }
  })
  return sorted
})

function handleEdit(row) {
  editingId.value = row.id
  form.value = { name: row.name, type: row.type, parentId: row.parentId, color: row.color, icon: row.icon || '' }
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
.mobile-categories { padding-bottom: calc(70px + env(safe-area-inset-bottom, 0px)); }

.type-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.cat-list { display: flex; flex-direction: column; gap: 2px; }

.cat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: #fff;
  border-radius: 8px;
  gap: 8px;
}
.cat-item.is-child { padding-left: 28px; }
.cat-item.is-parent { border-bottom: 1px solid #f5f5f5; border-radius: 8px 8px 0 0; }
.cat-item.is-parent + .cat-item.is-child { border-radius: 0 0 8px 8px; }

.cat-left { display: flex; align-items: center; gap: 8px; flex: 1; min-width: 0; }
.color-dot { width: 12px; height: 12px; border-radius: 50%; flex-shrink: 0; }
.cat-name { font-size: 14px; font-weight: 500; }

.cat-actions { display: flex; gap: 4px; flex-shrink: 0; }

.fab {
  position: fixed;
  bottom: calc(70px + env(safe-area-inset-bottom));
  right: 16px;
  z-index: 50;
}
</style>
