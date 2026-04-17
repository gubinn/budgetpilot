<template>
  <n-card>
    <n-form inline :model="filters" style="margin-bottom: 16px">
      <n-form-item label="类型">
        <n-select v-model:value="filters.type" :options="typeOptions" clearable style="width: 100px" />
      </n-form-item>
      <n-form-item label="账户">
        <n-select v-model:value="filters.accountId" :options="accountOptions" clearable style="width: 120px" />
      </n-form-item>
      <n-form-item label="分类">
        <n-select v-model:value="filters.categoryId" :options="categoryOptions" clearable style="width: 120px" />
      </n-form-item>
      <n-form-item label="状态">
        <n-select v-model:value="filters.isConfirmed" :options="confirmOptions" clearable style="width: 100px" />
      </n-form-item>
      <n-form-item label="日期">
        <n-date-picker v-model:value="filters.dateRange" type="daterange" clearable style="width: 240px" />
      </n-form-item>
      <n-form-item>
        <n-button type="primary" @click="loadData">查询</n-button>
        <n-button @click="resetFilters" style="margin-left: 8px">重置</n-button>
      </n-form-item>
    </n-form>

    <n-data-table
      :columns="columns"
      :data="transactions"
      :loading="loading"
      :pagination="pagination"
      @update:page="onPageChange"
      @update:page-size="onSizeChange"
      remote
    />

    <!-- 操作按钮 -->
    <n-space style="margin-top: 16px; justify-content: flex-end">
      <n-button type="primary" @click="router.push('/transactions/add')">新增交易</n-button>
    </n-space>
  </n-card>
</template>

<script setup>
import { ref, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { useDialog, useMessage, NButton, NSpace, NTag } from 'naive-ui'
import { transactionApi, accountApi, categoryApi } from '@/api'

const router = useRouter()
const dialog = useDialog()
const message = useMessage()

const loading = ref(false)
const transactions = ref([])
const accounts = ref([])
const categories = ref([])

const filters = ref({
  type: null,
  accountId: null,
  categoryId: null,
  isConfirmed: null,
  dateRange: null
})

const typeOptions = [
  { label: '支出', value: 1 },
  { label: '收入', value: 2 },
  { label: '转账', value: 3 }
]
const confirmOptions = [
  { label: '已确认', value: true },
  { label: '待确认', value: false }
]
const accountOptions = ref([])
const categoryOptions = ref([])

const pagination = ref({
  page: 1,
  pageSize: 20,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50, 100]
})

const columns = [
  {
    title: '日期', key: 'transactionDate', width: 110,
    render: (row) => row.transactionDate
  },
  {
    title: '账户', key: 'accountName', width: 120,
    render: (row) => row.accountName || '-'
  },
  {
    title: '分类', key: 'categoryName', width: 120,
    render: (row) => h('span', { style: { color: row.categoryColor, fontWeight: 500 } }, row.categoryName || '-')
  },
  {
    title: '金额', key: 'amount', width: 140, align: 'right',
    render: (row) => {
      const color = row.type === 1 ? '#e74c3c' : row.type === 2 ? '#27ae60' : '#333'
      const prefix = row.type === 1 ? '-' : row.type === 2 ? '+' : ''
      const currency = row.currency !== 'CNY' ? ` ${row.currency}` : ''
      return h('div', { style: { color, fontWeight: 600 } }, [
        `${prefix}¥${formatNum(row.amount)}${currency}`,
        row.currency !== 'CNY' ? h('div', { style: { fontSize: '11px', color: '#888', fontWeight: 'normal' } }, `≈ ¥${formatNum(row.amountBase)}`) : null
      ].filter(Boolean))
    }
  },
  { title: '备注', key: 'note', ellipsis: { tooltip: true } },
  {
    title: '状态', key: 'isConfirmed', width: 80,
    render: (row) => h(NTag, {
      size: 'small',
      type: row.isConfirmed ? 'success' : 'warning',
      bordered: false
    }, { default: () => row.isConfirmed ? '已确认' : '待确认' })
  },
  {
    title: '标签', key: 'tags', width: 100,
    render: (row) => row.tags?.length ? h(NSpace, { size: 4 }, row.tags.map(t =>
      h(NTag, { size: 'tiny', type: 'info', bordered: false }, { default: () => t })
    )) : '-'
  },
  {
    title: '操作', key: 'actions', width: 160, fixed: 'right',
    render: (row) => h(NSpace, { size: 8 }, [
      row.isConfirmed ? null : h(NButton, { size: 'small', type: 'success', onClick: () => handleConfirm(row.id) }, { default: () => '确认' }),
      h(NButton, { size: 'small', type: 'primary', onClick: () => router.push(`/transactions/edit/${row.id}`) }, { default: () => '编辑' }),
      h(NButton, { size: 'small', type: 'error', onClick: () => handleDelete(row.id) }, { default: () => '删除' })
    ].filter(Boolean))
  }
]

async function loadData() {
  loading.value = true
  try {
    const params = {
      page: pagination.value.page,
      size: pagination.value.pageSize,
      type: filters.value.type,
      accountId: filters.value.accountId,
      categoryId: filters.value.categoryId,
      confirmed: filters.value.isConfirmed
    }
    if (filters.value.dateRange?.length === 2) {
      params.startDate = filters.value.dateRange[0].split('T')[0]
      params.endDate = filters.value.dateRange[1].split('T')[0]
    }
    const res = await transactionApi.list(params)
    transactions.value = res.data.items
    pagination.value.itemCount = res.data.total
  } catch (e) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

function onPageChange(page) {
  pagination.value.page = page
  loadData()
}

function onSizeChange(size) {
  pagination.value.pageSize = size
  pagination.value.page = 1
  loadData()
}

function resetFilters() {
  filters.value = { type: null, accountId: null, categoryId: null, isConfirmed: null, dateRange: null }
  loadData()
}

async function handleConfirm(id) {
  try {
    await transactionApi.confirm(id)
    message.success('已确认')
    loadData()
  } catch (e) {
    message.error('确认失败: ' + e.message)
  }
}

function handleDelete(id) {
  dialog.warning({
    title: '确认删除',
    content: '删除交易将回滚账户余额，是否继续？',
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await transactionApi.remove(id)
        message.success('删除成功')
        loadData()
      } catch (e) {
        message.error('删除失败')
      }
    }
  })
}

function formatNum(n) {
  if (n == null) return '0.00'
  return Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

onMounted(async () => {
  const [accRes, catRes] = await Promise.all([
    accountApi.list(true),
    categoryApi.tree(null)
  ])
  accounts.value = accRes.data
  categories.value = catRes.data
  accountOptions.value = accounts.value.map(a => ({ label: a.name, value: a.id }))

  const allCats = []
  function flatten(list) {
    list.forEach(c => {
      allCats.push({ label: c.name, value: c.id })
      if (c.children?.length) flatten(c.children)
    })
  }
  flatten(categories.value)
  categoryOptions.value = allCats

  loadData()
})
</script>