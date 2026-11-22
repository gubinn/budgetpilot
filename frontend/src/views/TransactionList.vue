<template>
  <n-card>
    <div class="filter-bar">
      <n-form :model="filters" label-placement="left" :show-feedback="false" class="filter-form">
        <div class="filter-grid">
          <n-form-item label="类型" :label-width="72" class="filter-field">
            <n-select v-model:value="filters.type" :options="typeOptions" clearable :consistent-menu-width="false" />
          </n-form-item>
          <n-form-item label="账户" :label-width="72" class="filter-field">
            <n-select v-model:value="filters.accountId" :options="accountOptions" clearable :consistent-menu-width="false" />
          </n-form-item>
          <n-form-item label="分类" :label-width="72" class="filter-field">
            <n-tree-select v-model:value="filters.categoryId" :options="categoryOptions" placeholder="选择分类" clearable :consistent-menu-width="false" key-field="value" label-field="label" children-field="children" />
          </n-form-item>
          <n-form-item label="标签" :label-width="72" class="filter-field">
            <n-select v-model:value="filters.tags" :options="tagOptions" multiple filterable clearable placeholder="选择标签" :consistent-menu-width="false" />
          </n-form-item>
          <n-form-item label="入账状态" :label-width="72" class="filter-field">
            <n-select v-model:value="filters.isConfirmed" :options="confirmOptions" clearable :consistent-menu-width="false" />
          </n-form-item>
          <n-form-item label="复核状态" :label-width="72" class="filter-field">
            <n-select v-model:value="filters.reviewStatus" :options="reviewOptions" clearable :consistent-menu-width="false" />
          </n-form-item>
          <n-form-item label="对账状态" :label-width="72" class="filter-field">
            <n-select v-model:value="filters.reconciled" :options="reconcileOptions" clearable :consistent-menu-width="false" />
          </n-form-item>
          <n-form-item label="日期" :label-width="72" class="filter-field filter-date">
            <n-date-picker v-model:value="filters.dateRange" type="daterange" value-format="yyyy-MM-dd" clearable style="width: 100%" />
          </n-form-item>
        </div>
        <div class="filter-actions">
          <n-button @click="resetFilters">重置</n-button>
          <n-button type="primary" @click="applyFilters">查询</n-button>
        </div>
      </n-form>
    </div>

    <n-data-table
      :columns="columns"
      :data="transactions"
      :loading="loading"
      :pagination="pagination"
      :row-key="row => row.id"
      :scroll-x="1100"
      @update:page="onPageChange"
      @update:page-size="onSizeChange"
      remote
    />

    <!-- 汇总栏：按当前查询条件统计全部结果，不受分页影响 -->
    <div v-if="summary" class="summary-bar">
      <div class="summary-group">
        <span class="summary-item">共 <b>{{ summary.count }}</b> 笔</span>
        <span class="summary-item expense">支出 <b>¥{{ formatNum(summary.expense) }}</b><em>{{ summary.expenseCount }} 笔</em></span>
        <span v-if="Number(summary.refund) > 0" class="summary-item refund">退款 <b>¥{{ formatNum(summary.refund) }}</b><em>{{ summary.refundCount }} 笔</em></span>
        <span class="summary-item net">净支出 <b>¥{{ formatNum(summary.netExpense) }}</b></span>
      </div>
      <div class="summary-group">
        <span v-if="Number(summary.income) > 0" class="summary-item income">收入 <b>¥{{ formatNum(summary.income) }}</b><em>{{ summary.incomeCount }} 笔</em></span>
        <span v-if="Number(summary.transfer) > 0" class="summary-item transfer">转账 <b>¥{{ formatNum(summary.transfer) }}</b><em>{{ summary.transferCount }} 笔</em></span>
        <span v-if="Number(summary.income) > 0" class="summary-item net-total">结余 <b>¥{{ formatNum(summary.net) }}</b></span>
      </div>
    </div>

    <!-- 复核弹窗 -->
    <n-modal v-model:show="showReview" preset="card" title="复核交易" style="width: 560px">
      <n-descriptions v-if="reviewRow" :column="1" size="small" bordered>
        <n-descriptions-item label="日期">{{ reviewRow.transactionDate }}</n-descriptions-item>
        <n-descriptions-item label="金额">
          {{ reviewRow.type === 1 ? '-' : '+' }}¥{{ reviewRow.amount }}
        </n-descriptions-item>
        <n-descriptions-item label="账户">{{ reviewRow.accountName || '-' }}</n-descriptions-item>
        <n-descriptions-item label="摘要">{{ reviewRow.note || '-' }}</n-descriptions-item>
      </n-descriptions>

      <n-form label-placement="left" label-width="90" style="margin-top: 16px">
        <n-form-item label="处理方式">
          <n-radio-group v-model:value="reviewAction">
            <n-space vertical>
              <n-radio v-for="opt in reviewActions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </n-radio>
            </n-space>
          </n-radio-group>
        </n-form-item>
        <n-form-item v-if="reviewAction === 'EXPENSE' || reviewAction === 'INCOME'" label="分类">
          <n-tree-select
            v-model:value="reviewCategoryId"
            :options="reviewCategoryOptions"
            placeholder="选择分类"
            filterable
            clearable
          />
        </n-form-item>
      </n-form>

      <template #footer>
        <n-space justify="end">
          <n-button @click="showReview = false">取消</n-button>
          <n-button type="primary" :loading="reviewSubmitting" @click="submitReview">保存复核</n-button>
        </n-space>
      </template>
    </n-modal>
  </n-card>
</template>

<script setup>
import { ref, computed, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { useDialog, useMessage, NButton, NSpace, NTag } from 'naive-ui'
import { transactionApi, accountApi, categoryApi } from '@/api'
import dayjs from 'dayjs'
import { formatNum } from '@/utils/display'

const router = useRouter()
const dialog = useDialog()
const message = useMessage()

const loading = ref(false)
const transactions = ref([])
const summary = ref(null)
const accounts = ref([])
const categories = ref([])

// 默认只查最近一个月，避免一进页面就拉全量
function defaultDateRange() {
  return [dayjs().subtract(1, 'month').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')]
}

const filters = ref({
  type: null,
  accountId: null,
  categoryId: null,
  tags: [],
  isConfirmed: null,
  reviewStatus: null,
  reconciled: null,
  dateRange: defaultDateRange()
})

const typeOptions = [
  { label: '支出', value: 1 },
  { label: '收入', value: 2 },
  { label: '转账', value: 3 },
  { label: '退款', value: 4 }
]
const confirmOptions = [
  { label: '已入账', value: true },
  { label: '待入账', value: false }
]
const reviewOptions = [
  { label: '已复核', value: 'REVIEWED' },
  { label: '待复核', value: 'PENDING' }
]
const reconcileOptions = [
  { label: '已对账', value: true },
  { label: '未对账', value: false }
]
const accountOptions = ref([])
const categoryOptions = ref([])
const tagOptions = ref([])

const showReview = ref(false)
const reviewRow = ref(null)
const reviewAction = ref(null)
const reviewCategoryId = ref(null)
const reviewSubmitting = ref(false)

const reviewActions = computed(() => {
  if (reviewRow.value?.type === 2) {
    return [
      { value: 'COLLECT_LOAN', label: '收回借出款（别人还我，不计收入）' },
      { value: 'LOAN_IN', label: '借入款（我向别人借，不计收入）' },
      { value: 'INCOME', label: '实际收入（选择收入分类）' },
      { value: 'KEEP', label: '保持待复核' }
    ]
  }
  return [
    { value: 'LOAN_OUT', label: '借出款（我借给别人，不计收支）' },
    { value: 'REPAY_LOAN', label: '归还借入款（我还别人，不计收支）' },
    { value: 'EXPENSE', label: '实际支出（选择支出分类）' },
    { value: 'KEEP', label: '保持待复核' }
  ]
})

const reviewCategoryOptions = computed(() => {
  const type = reviewAction.value === 'INCOME' ? 2 : 1
  function build(nodes) {
    return (nodes || [])
      .filter(c => c.type === type && !c.excludeFromReports
        && (type === 2 || c.budgetType !== 'NONE'))
      .map(c => ({
        label: c.name,
        value: c.id,
        children: build(c.children)
      }))
  }
  return build(categories.value)
})

const REVIEW_CATEGORY_PATHS = {
  LOAN_OUT: '往来款/借出款',
  REPAY_LOAN: '往来款/归还借入款',
  COLLECT_LOAN: '往来款/收回借出款',
  LOAN_IN: '往来款/借入款'
}

function findCategoryByPath(path) {
  const parts = path.split('/')
  let list = categories.value
  let found = null
  for (const part of parts) {
    found = (list || []).find(c => c.name === part)
    if (!found) return null
    list = found.children
  }
  return found
}

const pagination = ref({
  page: 1,
  pageSize: 20,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50, 100]
})

const typeLabels = { 1: '支出', 2: '收入', 3: '转账', 4: '退款' }
const typeTagTypes = { 1: 'error', 2: 'success', 3: 'info', 4: 'warning' }
const channelLabels = {
  ALIPAY: '支付宝', WECHAT: '微信',
  CMB: '招行储蓄卡', CMB_DEBIT: '招行储蓄卡', CMB_CREDIT: '招行信用卡'
}
function channelLabel(channel) {
  return channelLabels[channel] || channel
}

function hasExtFields(row) {
  const ext = row.extFields
  return !!ext && typeof ext === 'object' && Object.keys(ext).length > 0
}

function renderExtDetail(row) {
  const entries = Object.entries(row.extFields || {})
  if (!entries.length) return null
  return h('div', { class: 'ext-detail' }, [
    h('div', { class: 'ext-detail-title' }, `账单扩展信息 · ${entries.length} 项`),
    h('div', { class: 'ext-detail-grid' }, entries.map(([k, v]) => h('div', { class: 'ext-detail-row' }, [
      h('span', { class: 'ext-detail-key' }, k),
      h('span', { class: 'ext-detail-value', title: String(v ?? '') }, String(v ?? '-'))
    ])))
  ])
}

const columns = [
  {
    type: 'expand',
    key: 'expand',
    width: 36,
    expandable: hasExtFields,
    renderExpand: renderExtDetail
  },
  {
    title: '日期', key: 'transactionDate', width: 130,
    render: (row) => {
      const time = row.transactionTime ? String(row.transactionTime).slice(0, 8) : ''
      if (!time) return row.transactionDate
      return h('div', { class: 'date-cell' }, [
        h('div', null, row.transactionDate),
        h('div', { class: 'date-time' }, time)
      ])
    }
  },
  {
    title: '类型', key: 'type', width: 64,
    render: (row) => h(NTag, {
      size: 'small', type: typeTagTypes[row.type] || 'default', bordered: false
    }, { default: () => typeLabels[row.type] || '-' })
  },
  {
    title: '账户', key: 'accountName', width: 130,
    ellipsis: { tooltip: true },
    render: (row) => {
      if (row.type === 3 && row.targetAccountName) {
        const target = row.targetAccountId != null && row.targetAccountId === row.accountId
          ? '自己'
          : row.targetAccountName
        return h('span', [
          row.accountName || '-',
          h('span', { style: { color: '#b0b7c3', margin: '0 4px' } }, '→'),
          target
        ])
      }
      return row.accountName || '-'
    }
  },
  {
    title: '分类', key: 'categoryName', width: 130,
    ellipsis: { tooltip: true },
    render: (row) => {
      const parentName = row.parentCategoryName || row.categoryName || '-'
      const parentColor = row.parentCategoryColor || row.categoryColor
      if (!row.parentCategoryId) {
        return h('span', { style: { color: parentColor, fontWeight: 500 } }, parentName)
      }
      return h('span', [
        h('span', { style: { color: parentColor, fontWeight: 500 } }, parentName),
        h('span', { style: { color: '#b0b7c3', margin: '0 4px' } }, '/'),
        h('span', { style: { color: row.categoryColor || '#666' } }, row.categoryName || '-')
      ])
    }
  },
  {
    title: '商户', key: 'merchantName', width: 80,
    ellipsis: { tooltip: true },
    render: (row) => row.merchantName ? h('span', { style: { color: row.merchantColor || '#666' } }, row.merchantName) : '-'
  },
  {
    title: '金额', key: 'amount', width: 110, align: 'right',
    render: (row) => {
      const color = row.type === 1 ? '#e74c3c' : row.type === 2 ? '#27ae60' : row.type === 4 ? '#2d98da' : '#333'
      const prefix = row.type === 1 ? '-' : (row.type === 2 || row.type === 4) ? '+' : ''
      const currency = row.currency !== 'CNY' ? ` ${row.currency}` : ''
      return h('div', { style: { color, fontWeight: 600 } }, [
        `${prefix}¥${formatNum(row.amount)}${currency}`,
        row.currency !== 'CNY' ? h('div', { style: { fontSize: '11px', color: '#888', fontWeight: 'normal' } }, `≈ ¥${formatNum(row.amountBase)}`) : null
      ].filter(Boolean))
    }
  },
  {
    title: '状态', key: 'isConfirmed', width: 130,
    render: (row) => {
      const pending = []
      if (!row.isConfirmed) pending.push('待入账')
      if (row.reviewStatus === 'PENDING') pending.push('待复核')
      return h('div', { class: 'status-cell' }, [
        h(NTag, {
          size: 'tiny',
          type: pending.length ? 'warning' : 'success',
          bordered: false
        }, { default: () => pending.length ? pending.join('·') : '已入账' }),
        row.reconciled
          ? h(NTag, { size: 'tiny', type: 'info', bordered: false }, {
            default: () => `已对账${row.reconcileChannel ? ':' + channelLabel(row.reconcileChannel) : ''}`
          })
          : null
      ].filter(Boolean))
    }
  },
  {
    title: '备注', key: 'note', minWidth: 120,
    render: (row) => {
      const note = row.note || ''
      const tags = row.tags || []
      if (!note && !tags.length) return '-'
      return h('div', { class: 'note-cell' }, [
        note ? h('span', { class: 'note-text', title: note }, note) : null,
        tags.length
          ? h(NSpace, { size: 4, class: 'note-tags' }, tags.map(t =>
            h(NTag, { size: 'tiny', type: 'info', bordered: false }, { default: () => t })))
          : null
      ].filter(Boolean))
    }
  },
  {
    title: '操作', key: 'actions', width: 150, fixed: 'right',
    render: (row) => h(NSpace, { size: 8 }, [
      row.isConfirmed ? null : h(NButton, { size: 'small', type: 'success', onClick: () => handleConfirm(row.id) }, { default: () => '确认' }),
      row.reviewStatus === 'PENDING'
        ? h(NButton, { size: 'small', type: 'warning', onClick: () => openReview(row) }, { default: () => '复核' })
        : null,
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
      confirmed: filters.value.isConfirmed,
      reviewStatus: filters.value.reviewStatus,
      reconciled: filters.value.reconciled
    }
    if (filters.value.tags?.length) {
      params.tags = filters.value.tags.join(',')
    }
    if (filters.value.dateRange?.length === 2) {
      params.startDate = dayjs(filters.value.dateRange[0]).format('YYYY-MM-DD')
      params.endDate = dayjs(filters.value.dateRange[1]).format('YYYY-MM-DD')
    }
    const [res, sumRes] = await Promise.all([
      transactionApi.list(params),
      transactionApi.summary(params)
    ])
    transactions.value = res.data.items
    pagination.value.itemCount = res.data.total
    summary.value = sumRes.data
  } catch (e) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  pagination.value.page = 1
  loadData()
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
  filters.value = { type: null, accountId: null, categoryId: null, tags: [], isConfirmed: null, reviewStatus: null, reconciled: null, dateRange: defaultDateRange() }
  pagination.value.page = 1
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

function openReview(row) {
  reviewRow.value = row
  reviewAction.value = null
  reviewCategoryId.value = null
  showReview.value = true
}

async function submitReview() {
  if (!reviewAction.value) {
    message.warning('请选择处理方式')
    return
  }
  if (reviewAction.value === 'KEEP') {
    showReview.value = false
    return
  }

  let categoryId = null
  if (reviewAction.value === 'EXPENSE' || reviewAction.value === 'INCOME') {
    if (!reviewCategoryId.value) {
      message.warning('请选择分类')
      return
    }
    categoryId = reviewCategoryId.value
  } else {
    const category = findCategoryByPath(REVIEW_CATEGORY_PATHS[reviewAction.value])
    if (!category) {
      message.error(`分类不存在：${REVIEW_CATEGORY_PATHS[reviewAction.value]}`)
      return
    }
    categoryId = category.id
  }

  reviewSubmitting.value = true
  try {
    await transactionApi.update(reviewRow.value.id, {
      categoryId,
      reviewStatus: 'REVIEWED'
    })
    message.success('复核完成')
    showReview.value = false
    loadData()
  } catch (e) {
    message.error('复核失败: ' + (e.response?.data?.message || e.message))
  } finally {
    reviewSubmitting.value = false
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

onMounted(async () => {
  const [accRes, catRes, tagRes] = await Promise.all([
    accountApi.list(true),
    categoryApi.tree(null),
    transactionApi.tags()
  ])
  accounts.value = accRes.data
  categories.value = catRes.data
  tagOptions.value = (tagRes.data || []).map(t => ({ label: t, value: t }))
  accountOptions.value = accounts.value.map(a => ({ label: a.name, value: a.id }))

  // 将分类树转为 tree-select 可用的格式
  function toTreeOption(c) {
    const opt = { label: c.name, value: c.id }
    if (c.children?.length) {
      opt.children = c.children.map(toTreeOption)
    }
    return opt
  }
  categoryOptions.value = categories.value.flatMap(c => toTreeOption(c))

  loadData()
})
</script>

<style scoped>
.filter-bar {
  margin-bottom: 16px;
  padding: 14px 16px 12px;
  background: #f8fafc;
  border: 1px solid var(--bp-line);
  border-radius: 12px;
}
/* 查询条件统一为等宽网格，标签定宽，控件占满单元格 */
.filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px 20px;
}
.filter-field {
  width: 100%;
}
.filter-field :deep(.n-form-item-blank) {
  min-width: 0;
}
/* 日期区间跨度更大，避免两个日期挤在一起被截断 */
.filter-date {
  grid-column: span 2;
}
.filter-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}
/* 日期列：日期为主，时间用弱化的第二行显示 */
:deep(.date-time) {
  font-size: 11px;
  color: #8a94a6;
  line-height: 14px;
}
/* 状态列：小标签自动换行，最多两行 */
:deep(.status-cell) {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
/* 备注与标签同格：备注超长省略，标签不参与挤压 */
:deep(.note-cell) {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}
:deep(.note-text) {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
:deep(.note-tags) {
  flex-shrink: 0;
}
/* 展开行：账单扩展字段按两列键值对平铺，避免挤压主表格 */
:deep(.ext-detail) {
  padding: 4px 16px 12px 20px;
  background: #fafbfc;
  border-radius: 8px;
}
:deep(.ext-detail-title) {
  font-size: 12px;
  color: #8a94a6;
  margin-bottom: 6px;
}
:deep(.ext-detail-grid) {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 4px 24px;
}
:deep(.ext-detail-row) {
  display: flex;
  gap: 8px;
  min-width: 0;
  font-size: 12px;
  line-height: 18px;
}
:deep(.ext-detail-key) {
  flex: 0 0 112px;
  color: #8a94a6;
  text-align: right;
}
:deep(.ext-detail-value) {
  flex: 1;
  min-width: 0;
  color: #333;
  word-break: break-all;
}
.summary-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px 20px;
  margin-top: 12px;
  padding: 10px 16px;
  background: #f8fafc;
  border: 1px solid var(--bp-line);
  border-radius: 10px;
  font-size: 13px;
  color: #5b6472;
}
.summary-group {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 6px 18px;
}
.summary-item {
  white-space: nowrap;
}
.summary-item b {
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
.summary-item em {
  margin-left: 4px;
  font-style: normal;
  font-size: 11px;
  color: #98a2b3;
}
.summary-item.expense b { color: #e74c3c; }
.summary-item.refund b { color: #2d98da; }
.summary-item.income b { color: #27ae60; }
.summary-item.transfer b { color: #8a94a6; }
.summary-item.net b { color: #1f2937; }
.summary-item.net-total {
  padding-left: 14px;
  border-left: 1px solid var(--bp-line);
}
.summary-item.net-total b { color: #1f2937; }

@media (max-width: 1279px) {
  .filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 719px) {
  .filter-grid {
    grid-template-columns: minmax(0, 1fr);
  }
  .filter-date {
    grid-column: span 1;
  }
  .filter-bar {
    padding: 12px;
  }
}
</style>