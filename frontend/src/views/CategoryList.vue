<template>
  <n-card title="分类管理">
    <template #header-extra>
      <n-radio-group v-model:value="filterType" size="small" style="margin-right: 12px">
        <n-radio-button :value="null">全部</n-radio-button>
        <n-radio-button :value="1">支出</n-radio-button>
        <n-radio-button :value="2">收入</n-radio-button>
      </n-radio-group>
      <n-button secondary @click="openMerge">合并分类</n-button>
      <n-button type="primary" @click="openCreate">新增分类</n-button>
    </template>

    <n-tree
      :data="treeData"
      block-line
      show-line
      :render-label="renderLabel"
      style="max-height: 70vh; overflow-y: auto"
    />

    <!-- 合并分类弹窗 -->
    <n-modal v-model:show="showMerge" preset="card" title="合并分类" style="width: 460px">
      <n-form label-placement="left" label-width="90">
        <n-form-item label="源分类">
          <n-select v-model:value="mergeSourceId" :options="mergeOptions" filterable placeholder="要停用的分类" />
        </n-form-item>
        <n-form-item label="目标分类">
          <n-select v-model:value="mergeTargetId" :options="mergeOptions" filterable placeholder="保留的分类" />
        </n-form-item>
        <n-alert type="warning" :bordered="false">
          源分类的交易、预算、商户默认分类都会转到目标分类，源分类随后停用。只支持叶子分类。
        </n-alert>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showMerge = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="handleMerge">合并</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 新增/编辑弹窗 -->
    <n-modal v-model:show="showCreate" preset="card" :title="editingId ? '编辑分类' : '新增分类'" style="width: 450px">
      <n-form ref="formRef" :model="form" label-placement="left" label-width="80">
        <n-form-item label="名称" required>
          <n-input v-model:value="form.name" placeholder="如：餐饮" />
        </n-form-item>
        <n-form-item label="类型" required>
          <n-select v-model:value="form.type" :options="typeOptions" />
        </n-form-item>
        <n-form-item label="父分类">
          <n-select v-model:value="form.parentId" :options="parentOptions" clearable placeholder="顶级分类" />
        </n-form-item>
        <n-form-item v-if="form.type === 1" label="预算属性">
          <n-select v-model:value="form.budgetType" :options="budgetTypeOptions" />
        </n-form-item>
        <n-form-item label="颜色">
          <n-color-picker v-model:value="form.color" :show-alpha="false" />
        </n-form-item>
        <n-form-item label="图标">
          <n-input v-model:value="form.icon" placeholder="图标标识" />
        </n-form-item>
        <n-form-item>
          <n-space>
            <n-button type="primary" :loading="submitting" @click="handleSubmit">保存</n-button>
            <n-button @click="showCreate = false">取消</n-button>
          </n-space>
        </n-form-item>
      </n-form>
    </n-modal>
  </n-card>
</template>

<script setup>
import { ref, h, computed, onMounted } from 'vue'
import { useDialog, useMessage, NButton, NTag } from 'naive-ui'
import { categoryApi } from '@/api'

const dialog = useDialog()
const message = useMessage()
const filterType = ref(null)
const categories = ref([])
const showCreate = ref(false)
const showMerge = ref(false)
const mergeSourceId = ref(null)
const mergeTargetId = ref(null)
const editingId = ref(null)
const submitting = ref(false)

const mergeOptions = computed(() => categories.value
  .filter(c => !c.isSystem)
  .map(c => ({ label: `${c.parentId ? '　' : ''}${c.name}${c.budgetType && c.type === 1 ? `（${budgetTypeLabels[c.budgetType] || c.budgetType}）` : ''}`, value: c.id })))

const form = ref({
  name: '',
  type: 1,
  parentId: 0,
  budgetType: 'DAILY',
  color: '#3498db',
  icon: ''
})

const typeOptions = [
  { label: '支出', value: 1 },
  { label: '收入', value: 2 }
]
const budgetTypeOptions = [
  { label: '日常（月度预算）', value: 'DAILY' },
  { label: '固定（不预算）', value: 'FIXED' },
  { label: '专项（年度预算）', value: 'SPECIAL' },
  { label: '非预算流水', value: 'NONE' }
]
const budgetTypeLabels = Object.fromEntries(budgetTypeOptions.map(o => [o.value, o.label]))

const parentOptions = computed(() => {
  const roots = categories.value.filter(c => c.parentId === 0 && c.type === form.value.type)
  return [{ label: '顶级分类', value: 0 }, ...roots.map(c => ({ label: c.name, value: c.id }))]
})

const treeData = computed(() => {
  const filtered = filterType.value
    ? categories.value.filter(c => c.type === filterType.value)
    : categories.value
  return buildTree(filtered)
})

function buildTree(list) {
  const roots = list.filter(c => c.parentId === 0)
  const map = {}
  list.forEach(c => {
    map[c.id] = {
      key: c.id,
      label: c.name,
      ...c,
      children: []
    }
  })
  list.forEach(c => {
    if (c.parentId !== 0 && map[c.parentId]) {
      map[c.parentId].children.push(map[c.id])
    }
  })
  return roots.map(r => map[r.id])
}

function renderLabel({ option }) {
  return h('div', { style: { display: 'flex', alignItems: 'center', gap: '8px' } }, [
    h('span', {
      style: {
        width: '12px', height: '12px', borderRadius: '50%',
        background: option.color || '#ccc', display: 'inline-block', flexShrink: 0
      }
    }),
    h('span', {}, option.name),
    option.type === 1 && option.budgetType && option.budgetType !== 'DAILY'
      ? h(NTag, {
          size: 'tiny',
          type: option.budgetType === 'FIXED' ? 'default' : option.budgetType === 'SPECIAL' ? 'warning' : 'error',
          bordered: false,
          style: { marginLeft: '4px' }
        }, { default: () => budgetTypeLabels[option.budgetType] || option.budgetType })
      : null,
    option.transactionCount ? h('span', { style: { color: '#999', fontSize: '12px', marginLeft: '4px' } }, `${option.transactionCount} 笔`) : null,
    option.isSystem ? h(NTag, { size: 'tiny', type: 'info', bordered: false, style: { marginLeft: '4px' } }, { default: () => '系统' }) : null,
    h('div', { style: { marginLeft: 'auto', display: 'flex', gap: '8px' } }, [
      h(NButton, { size: 'small', type: 'primary', onClick: (e) => { e.stopPropagation(); handleEdit(option) } }, { default: () => '编辑' }),
      !option.isSystem ? h(NButton, { size: 'small', type: 'error', onClick: (e) => { e.stopPropagation(); handleDelete(option.id) } }, { default: () => '删除' }) : null
    ].filter(Boolean))
  ])
}

function openCreate() {
  editingId.value = null
  form.value = { name: '', type: filterType.value || 1, parentId: 0, budgetType: 'DAILY', color: '#3498db', icon: '' }
  showCreate.value = true
}

function openMerge() {
  mergeSourceId.value = null
  mergeTargetId.value = null
  showMerge.value = true
}

async function handleMerge() {
  if (!mergeSourceId.value || !mergeTargetId.value) {
    message.error('请选择源分类和目标分类')
    return
  }
  submitting.value = true
  try {
    await categoryApi.merge(mergeSourceId.value, mergeTargetId.value)
    message.success('合并完成')
    showMerge.value = false
    loadCategories()
  } catch (e) {
    message.error(e.response?.data?.message || '合并失败')
  } finally {
    submitting.value = false
  }
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
