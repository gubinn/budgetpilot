<template>
  <div class="mobile-transactions">
    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-row">
        <n-select v-model:value="filters.type" :options="typeOptions" placeholder="类型" size="small" style="flex: 1" clearable />
        <n-select v-model:value="filters.accountId" :options="accountOptions" placeholder="账户" size="small" style="flex: 1" clearable />
        <n-tree-select v-model:value="filters.categoryId" :options="categoryOptions" placeholder="分类" size="small" style="flex: 1" clearable key-field="value" label-field="label" children-field="children" />
      </div>
      <div class="filter-row">
        <n-date-picker v-model:value="filters.dateRange" type="daterange" value-format="yyyy-MM-dd" size="small" style="flex: 1" clearable />
        <n-select v-model:value="filters.isConfirmed" :options="confirmOptions" placeholder="入账状态" size="small" style="flex: 1" clearable />
        <n-select v-model:value="filters.reviewStatus" :options="reviewOptions" placeholder="复核状态" size="small" style="flex: 1" clearable />
      </div>
      <div class="filter-row">
        <n-select v-model:value="filters.tags" :options="tagOptions" multiple filterable clearable placeholder="标签" size="small" style="flex: 1" />
        <n-select v-model:value="filters.reconciled" :options="reconcileOptions" placeholder="对账状态" size="small" style="flex: 1" clearable />
      </div>
      <div class="filter-actions">
        <n-button type="primary" size="small" @click="applyFilters">查询</n-button>
        <n-button size="small" @click="resetFilters" style="margin-left: 8px">重置</n-button>
      </div>
    </div>

    <!-- 交易列表 -->
    <div v-if="transactions.length" class="tx-list">
      <div v-for="tx in transactions" :key="tx.id" class="tx-item" @click="router.push(`/transactions/edit/${tx.id}`)">
        <div class="tx-left">
          <div class="tx-category" :style="{ color: tx.parentCategoryColor || tx.categoryColor }">
            {{ tx.parentCategoryName || tx.categoryName || '-' }}
            <span v-if="tx.parentCategoryId" class="tx-subcategory">/ {{ tx.categoryName }}</span>
          </div>
          <div class="tx-meta">
            <n-tag size="tiny" :type="typeTagType(tx.type)" :bordered="false">{{ typeLabel(tx.type) }}</n-tag>
            <span>{{ tx.transactionDate }}</span>
            <span v-if="tx.accountName">{{ tx.accountName }}</span>
            <span v-if="tx.type === 3 && tx.targetAccountName">对手方：{{ tx.targetAccountId === tx.accountId ? '自己' : tx.targetAccountName }}</span>
            <span v-if="tx.merchantName">{{ tx.merchantName }}</span>
          </div>
          <div class="tx-tags">
            <n-tag v-if="!tx.isConfirmed" size="tiny" type="warning" :bordered="false">待入账</n-tag>
            <n-tag v-if="tx.reviewStatus === 'PENDING'" size="tiny" type="warning" :bordered="false">待复核</n-tag>
            <n-tag v-if="tx.reconciled" size="tiny" type="info" :bordered="false">已对账</n-tag>
            <n-button v-if="tx.reviewStatus === 'PENDING'" size="tiny" type="warning" secondary style="margin-left: 4px" @click.stop="openReview(tx)">复核</n-button>
            <n-tag v-for="tag in (tx.tags || [])" :key="tag" size="tiny" type="info" :bordered="false" style="margin-left: 4px">{{ tag }}</n-tag>
          </div>
        </div>
        <div class="tx-right">
          <div class="tx-amount" :class="tx.type === 1 ? 'expense' : (tx.type === 2 || tx.type === 4) ? 'income' : ''">
            {{ tx.type === 1 ? '-' : (tx.type === 2 || tx.type === 4) ? '+' : '' }}¥{{ formatNum(tx.amount) }}
          </div>
          <div v-if="tx.currency !== 'CNY'" class="tx-currency">{{ tx.currency }}</div>
        </div>
      </div>
    </div>
    <n-empty v-else description="暂无交易" />

    <n-modal v-model:show="showReview" preset="card" title="复核交易" style="width: 92vw; max-width: 520px">
      <div v-if="reviewRow" class="review-summary">
        <div>{{ reviewRow.transactionDate }} · {{ reviewRow.type === 1 ? '-' : '+' }}¥{{ formatNum(reviewRow.amount) }}</div>
        <div class="review-note">{{ reviewRow.note || '-' }}</div>
      </div>
      <n-radio-group v-model:value="reviewAction" style="margin-top: 12px">
        <n-space vertical>
          <n-radio v-for="opt in reviewActions" :key="opt.value" :value="opt.value">{{ opt.label }}</n-radio>
        </n-space>
      </n-radio-group>
      <n-tree-select
        v-if="reviewAction === 'EXPENSE' || reviewAction === 'INCOME'"
        v-model:value="reviewCategoryId"
        :options="reviewCategoryOptions"
        placeholder="选择分类"
        filterable
        clearable
        style="margin-top: 12px"
      />
      <template #footer>
        <n-space justify="end">
          <n-button @click="showReview = false">取消</n-button>
          <n-button type="primary" :loading="reviewSubmitting" @click="submitReview">保存复核</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 分页 -->
    <div v-if="pagination.itemCount > pagination.pageSize" class="pagination">
      <n-button size="small" :disabled="pagination.page <= 1" @click="pageChange(pagination.page - 1)">上一页</n-button>
      <span class="page-info">{{ pagination.page }} / {{ totalPages }}</span>
      <n-button size="small" :disabled="pagination.page >= totalPages" @click="pageChange(pagination.page + 1)">下一页</n-button>
    </div>

    <!-- 底部新增按钮 -->
    <div class="fab">
      <n-button type="primary" round size="large" @click="router.push('/transactions/add')">
        <template #icon><n-icon><AddOutline /></n-icon></template>
        记一笔
      </n-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { transactionApi, accountApi, categoryApi } from '@/api'
import { AddOutline } from '@vicons/ionicons5'
import dayjs from 'dayjs'
import { formatNum } from '@/utils/display'

const router = useRouter()
const message = useMessage()
const loading = ref(false)
const transactions = ref([])

const typeLabels = { 1: '支出', 2: '收入', 3: '转账', 4: '退款' }
const typeTagTypes = { 1: 'error', 2: 'success', 3: 'info', 4: 'warning' }
const typeLabel = (type) => typeLabels[type] || '-'
const typeTagType = (type) => typeTagTypes[type] || 'default'

const filters = ref({
  type: null,
  accountId: null,
  categoryId: null,
  tags: [],
  isConfirmed: null,
  reviewStatus: null,
  reconciled: null,
  dateRange: null
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
const categories = ref([])
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
      .map(c => ({ label: c.name, value: c.id, children: build(c.children) }))
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
      message.error('分类不存在：' + REVIEW_CATEGORY_PATHS[reviewAction.value])
      return
    }
    categoryId = category.id
  }
  reviewSubmitting.value = true
  try {
    await transactionApi.update(reviewRow.value.id, { categoryId, reviewStatus: 'REVIEWED' })
    message.success('复核完成')
    showReview.value = false
    loadData()
  } catch (e) {
    message.error('复核失败: ' + (e.response?.data?.message || e.message))
  } finally {
    reviewSubmitting.value = false
  }
}

const pagination = ref({
  page: 1,
  pageSize: 20,
  itemCount: 0
})

const totalPages = computed(() => Math.ceil(pagination.value.itemCount / pagination.value.pageSize))

function applyFilters() {
  pagination.value.page = 1
  loadData()
}

function resetFilters() {
  filters.value = { type: null, accountId: null, categoryId: null, tags: [], isConfirmed: null, reviewStatus: null, reconciled: null, dateRange: null }
  pagination.value.page = 1
  loadData()
}

function pageChange(page) {
  pagination.value.page = page
  loadData()
}

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
    const res = await transactionApi.list(params)
    transactions.value = res.data.items
    pagination.value.itemCount = res.data.total
  } catch (e) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    const [accRes, catRes, tagRes] = await Promise.all([
      accountApi.list(true),
      categoryApi.tree(null),
      transactionApi.tags()
    ])
    tagOptions.value = (tagRes.data || []).map(t => ({ label: t, value: t }))
    accountOptions.value = accRes.data.map(a => ({ label: a.name, value: a.id }))
    categories.value = catRes.data || []

    function toTreeOption(c) {
      const opt = { label: c.name, value: c.id }
      if (c.children?.length) opt.children = c.children.map(toTreeOption)
      return opt
    }
    categoryOptions.value = catRes.data.flatMap(c => toTreeOption(c))
  } catch (e) {
    message.error('初始化失败: ' + e.message)
  }
  loadData()
})
</script>

<style scoped>
.mobile-transactions {
  padding-bottom: calc(70px + env(safe-area-inset-bottom, 0px));
}

/* 筛选栏 */
.filter-bar {
  background: #fff;
  border-radius: 10px;
  padding: 10px 12px;
  margin-bottom: 12px;
}
.filter-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}
.filter-row:last-of-type { margin-bottom: 6px; }
.filter-actions { display: flex; justify-content: flex-end; }

/* 交易列表 */
.tx-list { }
.tx-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
  -webkit-tap-highlight-color: transparent;
}
.tx-item:active { background: #f5f5f5; }

.tx-left { flex: 1; min-width: 0; }
.tx-category { font-size: 14px; font-weight: 500; }
.tx-subcategory { font-size: 12px; font-weight: 400; opacity: 0.7; margin-left: 2px; }
.tx-meta { font-size: 12px; color: #999; margin-top: 3px; }
.tx-meta span { margin-right: 6px; }
.tx-tags { margin-top: 4px; display: flex; flex-wrap: wrap; gap: 4px; }

.tx-right { text-align: right; flex-shrink: 0; margin-left: 12px; }
.tx-amount { font-size: 16px; font-weight: 600; }
.tx-amount.expense { color: #e74c3c; }
.tx-amount.income { color: #27ae60; }
.tx-currency { font-size: 11px; color: #999; margin-top: 2px; }

/* 分页 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  padding: 16px 0;
}
.page-info { font-size: 13px; color: #666; }

/* 底部悬浮按钮 */
.fab {
  position: fixed;
  bottom: calc(70px + env(safe-area-inset-bottom));
  right: 16px;
  z-index: 50;
}

/* 复核弹窗 */
.review-summary { font-size: 14px; color: #333; }
.review-note { margin-top: 4px; font-size: 12px; color: #888; }
</style>
