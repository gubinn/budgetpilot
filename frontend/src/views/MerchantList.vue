<template>
  <n-card title="商户管理">
    <template #header-extra>
      <n-space>
        <n-input v-model:value="searchKeyword" placeholder="搜索商户" clearable style="width: 200px" />
        <n-button type="primary" @click="openCreateModal">
          <template #icon><n-icon><AddOutline /></n-icon></template>
          新增商户
        </n-button>
      </n-space>
    </template>

    <n-data-table
      :columns="columns"
      :data="filteredMerchants"
      :loading="loading"
      :pagination="pagination"
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
import { ref, h, computed, onMounted } from 'vue'
import { NButton, NTag, NSpace, NIcon, useDialog, useMessage } from 'naive-ui'
import { AddOutline, BusinessOutline } from '@vicons/ionicons5'
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

const pagination = ref({
  page: 1,
  pageSize: 20,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

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

const filteredMerchants = computed(() => {
  if (!searchKeyword.value) return merchants.value
  const kw = searchKeyword.value.toLowerCase()
  return merchants.value.filter(m =>
    m.name.toLowerCase().includes(kw) ||
    (m.alias && m.alias.toLowerCase().includes(kw))
  )
})

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
          display: 'inline-block'
        }
      }),
      h('span', {}, row.name),
      row.isSystem ? h(NTag, { size: 'tiny', type: 'info' }, { default: () => '系统' }) : null
    ])
  },
  { title: '别名', key: 'alias', ellipsis: { tooltip: true } },
  { title: '关联分类', key: 'categoryName' },
  { title: '使用次数', key: 'usageCount', width: 100 },
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

async function loadData() {
  loading.value = true
  try {
    const [mRes, cRes] = await Promise.all([
      merchantApi.list({ size: 1000 }),
      categoryApi.tree(1)  // 仅支出分类
    ])
    merchants.value = mRes.data || []

    // 展开分类树为扁平列表
    const flat = []
    function flatten(list) {
      list.forEach(c => {
        flat.push(c)
        if (c.children?.length) flatten(c.children)
      })
    }
    flatten(cRes.data || [])
    categories.value = flat
  } catch (e) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

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

onMounted(loadData)
</script>