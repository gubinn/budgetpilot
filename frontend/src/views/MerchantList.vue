<template>
  <n-card title="商户管理">
    <template #header-extra>
      <n-space>
        <n-input v-model:value="searchKeyword" placeholder="搜索商户 / 原始名称" clearable style="width: 240px" />
        <n-button secondary @click="handleConsolidate">合并重复</n-button>
        <n-button type="primary" @click="openCreateModal">
          <template #icon><n-icon><AddOutline /></n-icon></template>
          新增商户
        </n-button>
      </n-space>
    </template>

    <n-data-table
      :columns="columns"
      :data="merchants"
      :loading="loading"
      :pagination="pagination"
      remote
      :row-key="row => row.id"
    />

    <!-- 新增/编辑弹窗 -->
    <n-modal v-model:show="showModal" preset="card" :title="editingId ? '编辑商户' : '新增商户'" style="width: 500px">
      <n-form ref="formRef" :model="form" label-placement="left" label-width="80">
        <n-form-item label="名称" path="name" :rule="{ required: true, message: '请输入商户名称' }">
          <n-input v-model:value="form.name" placeholder="如：麦当劳" />
        </n-form-item>
        <n-form-item label="别名">
          <n-input v-model:value="form.alias" placeholder="用于模糊匹配，如：M记" />
        </n-form-item>
        <n-form-item label="关联分类">
          <n-cascader
            v-model:value="form.categoryId"
            :options="categoryTree"
            placeholder="可选"
            check-strategy="child"
            clearable
            filterable
          />
        </n-form-item>
        <n-form-item label="颜色">
          <n-color-picker v-model:value="form.color" :show-alpha="false" />
        </n-form-item>
        <n-form-item label="图标">
          <n-input v-model:value="form.icon" placeholder="图标标识" />
        </n-form-item>
        <n-form-item label="描述">
          <n-input v-model:value="form.description" placeholder="可选描述" />
        </n-form-item>
        <n-form-item label="标签">
          <n-dynamic-tags v-model:value="form.tags" />
        </n-form-item>
        <n-form-item v-if="editingId" label="状态">
          <n-switch v-model:value="form.isActive" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showModal = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="handleSubmit">保存</n-button>
        </n-space>
      </template>
    </n-modal>
  </n-card>
</template>

<script setup>
import { ref, h, computed, onMounted, watch } from 'vue'
import { NButton, NTag, NSpace, NIcon, useDialog, useMessage } from 'naive-ui'
import { AddOutline } from '@vicons/ionicons5'
import { merchantApi, categoryApi } from '@/api'

const dialog = useDialog()
const message = useMessage()
const loading = ref(false)
const merchants = ref([])
const categories = ref([])
const searchKeyword = ref('')
const showModal = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const formRef = ref(null)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
let searchTimer = null

const pagination = computed(() => ({
  page: page.value,
  pageSize: pageSize.value,
  itemCount: total.value,
  showSizePicker: true,
  pageSizes: [10, 20, 50, 100],
  prefix: ({ itemCount }) => `共 ${itemCount} 个商户`,
  onUpdatePage: (p) => {
    page.value = p
    loadData()
  },
  onUpdatePageSize: (size) => {
    pageSize.value = size
    page.value = 1
    loadData()
  }
}))

const form = ref({
  name: '',
  alias: '',
  categoryId: null,
  color: '#3498db',
  icon: '',
  description: '',
  tags: [],
  isActive: true
})

// 构建分类树（仅支出分类）
const categoryTree = computed(() => {
  const expenseCategories = categories.value.filter(c => c.type === 1 && c.parentId === 0)
  return expenseCategories.map(buildCascaderOption)
})

function buildCascaderOption(c) {
  const opt = { label: c.name, value: c.id }
  const children = categories.value.filter(child => child.parentId === c.id)
  if (children.length) {
    opt.children = children.map(buildCascaderOption)
  }
  return opt
}

const columns = [
  {
    title: '商户',
    key: 'name',
    render: (row) => h('div', { style: { display: 'flex', alignItems: 'center', gap: '8px' } }, [
      h('span', {
        style: {
          width: '24px',
          height: '24px',
          borderRadius: '4px',
          background: row.color || '#ccc',
          display: 'inline-block',
          flexShrink: 0
        }
      }),
      h('span', {}, row.name),
      row.isSystem ? h(NTag, { size: 'tiny', type: 'info' }, { default: () => '系统' }) : null
    ])
  },
  { title: '关联分类', key: 'categoryName', width: 140 },
  {
    title: '交易笔数',
    key: 'transactionCount',
    width: 100,
    render: (row) => row.transactionCount ?? row.usageCount ?? 0
  },
  {
    title: '原始名称',
    key: 'aliasCount',
    width: 100,
    render: (row) => {
      if (!row.aliasCount) return '-'
      return h('span', { title: (row.aliases || []).join('\n') }, `${row.aliasCount} 个`)
    }
  },
  {
    title: '最近使用',
    key: 'lastUsedAt',
    width: 120,
    render: (row) => row.lastUsedAt || '-'
  },
  {
    title: '操作',
    key: 'actions',
    width: 160,
    render: (row) => h(NSpace, { size: 8 }, [
      h(NButton, {
        size: 'small',
        type: 'primary',
        onClick: () => handleEdit(row)
      }, { default: () => '编辑' }),
      !row.isSystem ? h(NButton, {
        size: 'small',
        type: 'error',
        onClick: () => handleDelete(row.id)
      }, { default: () => '删除' }) : null
    ])
  }
]

async function loadCategories() {
  try {
    const res = await categoryApi.tree(1)
    const flat = []
    function flatten(list) {
      list.forEach(c => {
        flat.push(c)
        if (c.children?.length) flatten(c.children)
      })
    }
    flatten(res.data || [])
    categories.value = flat
  } catch (e) {
    message.error('分类加载失败')
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await merchantApi.page({
      page: page.value,
      size: pageSize.value,
      keyword: searchKeyword.value?.trim() || undefined
    })
    const data = res.data || {}
    merchants.value = data.items || []
    total.value = data.total || 0
  } catch (e) {
    message.error('加载失败')
    merchants.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

watch(searchKeyword, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    page.value = 1
    loadData()
  }, 300)
})

function openCreateModal() {
  editingId.value = null
  form.value = {
    name: '',
    alias: '',
    categoryId: null,
    color: '#3498db',
    icon: '',
    description: '',
    tags: [],
    isActive: true
  }
  showModal.value = true
}

function handleEdit(row) {
  editingId.value = row.id
  form.value = {
    name: row.name,
    alias: row.alias || '',
    categoryId: row.categoryId,
    color: row.color || '#3498db',
    icon: row.icon || '',
    description: row.description || '',
    tags: row.tags || [],
    isActive: row.isActive
  }
  showModal.value = true
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  if (!form.value.name) {
    message.error('请输入商户名称')
    return
  }

  submitting.value = true
  try {
    const data = { ...form.value }
    // 处理 cascader 返回的数组
    if (Array.isArray(data.categoryId)) {
      data.categoryId = data.categoryId[data.categoryId.length - 1] || null
    }

    if (editingId.value) {
      await merchantApi.update(editingId.value, data)
      message.success('更新成功')
    } else {
      await merchantApi.create(data)
      message.success('创建成功')
    }
    showModal.value = false
    editingId.value = null
    loadData()
  } catch (e) {
    message.error(e.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

function handleDelete(id) {
  dialog.warning({
    title: '确认删除',
    content: '删除后不可恢复，是否继续？',
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await merchantApi.remove(id)
        message.success('已删除')
        loadData()
      } catch (e) {
        message.error(e.response?.data?.message || '删除失败')
      }
    }
  })
}

function handleConsolidate() {
  dialog.warning({
    title: '合并重复商户',
    content: '将按支付渠道、公司后缀和品牌名聚合重复商户，原始名称会保留为别名，交易不会丢失。是否继续？',
    positiveText: '开始合并',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await merchantApi.consolidate()
        const d = res.data || {}
        message.success(`合并完成：处理 ${d.groups ?? 0} 组，合并 ${d.mergedGroups ?? 0} 组，删除 ${d.deleted ?? 0} 条重复商户`)
        page.value = 1
        loadData()
      } catch (e) {
        message.error(e.response?.data?.message || '合并失败')
      }
    }
  })
}

onMounted(async () => {
  await loadCategories()
  await loadData()
})
</script>