<template>
  <div class="mobile-transactions m-page">
    <!-- 筛选区：常用条件直接露出，其余收进底部面板 -->
    <section class="m-card filter-card">
      <div class="filter-top">
        <div class="m-chip-scroll date-chips">
          <button
            v-for="p in datePresets"
            :key="p.value"
            class="m-chip"
            :class="{ active: datePreset === p.value }"
            @click="chooseDatePreset(p.value)"
          >{{ p.label }}</button>
          <button v-if="datePreset === 'custom'" class="m-chip active" @click="showFilter = true">自定义</button>
        </div>
        <button class="m-chip filter-trigger" :class="{ active: activeFilterCount > 0 }" @click="showFilter = true">
          <n-icon :size="15"><OptionsOutline /></n-icon>
          筛选
          <span v-if="activeFilterCount" class="filter-count">{{ activeFilterCount }}</span>
        </button>
      </div>

      <div class="m-chip-scroll type-chips">
        <button
          v-for="opt in typeOptions"
          :key="String(opt.value)"
          class="m-chip"
          :class="{ active: filters.type === opt.value }"
          @click="setType(opt.value)"
        >{{ opt.label }}</button>
      </div>

      <div v-if="filterChips.length" class="m-chip-scroll applied-chips">
        <button v-for="chip in filterChips" :key="chip.key" class="m-chip ghost" @click="clearChip(chip.key)">
          {{ chip.label }}
          <span class="chip-x">×</span>
        </button>
      </div>
    </section>

    <!-- 汇总：查询条件对应的全量结果 -->
    <section v-if="summary" class="m-card summary-card">
      <div class="sum-top">
        <span class="sum-count">共 <b>{{ summary.count }}</b> 笔</span>
        <span class="sum-net">净支出 <b>¥{{ formatNum(summary.netExpense) }}</b></span>
      </div>
      <div class="sum-detail">
        <span class="expense">支出 ¥{{ formatNum(summary.expense) }}</span>
        <span v-if="Number(summary.refund) > 0" class="refund">退款 ¥{{ formatNum(summary.refund) }}</span>
        <span v-if="Number(summary.income) > 0" class="income">收入 ¥{{ formatNum(summary.income) }}</span>
        <span v-if="Number(summary.transfer) > 0">转账 ¥{{ formatNum(summary.transfer) }}</span>
      </div>
    </section>

    <!-- 按日期分组的交易列表 -->
    <n-spin :show="loading && !transactions.length">
      <div v-if="groups.length" class="tx-groups">
        <section v-for="group in groups" :key="group.date" class="tx-group">
          <div class="group-head">
            <span class="group-date">{{ group.label }}</span>
            <span class="group-sum">
              <template v-if="group.expense">支出 ¥{{ formatNum(group.expense) }}</template>
              <template v-if="group.expense && group.income"> · </template>
              <template v-if="group.income">收入 ¥{{ formatNum(group.income) }}</template>
            </span>
          </div>
          <div class="tx-card">
            <div v-for="tx in group.items" :key="tx.id" class="tx-row" @click="router.push(`/transactions/edit/${tx.id}`)">
              <div class="tx-main">
                <div class="tx-title m-ellipsis">
                  <span class="tx-cat" :style="{ color: tx.parentCategoryColor || tx.categoryColor }">
                    {{ tx.parentCategoryName || tx.categoryName || '未分类' }}
                  </span>
                  <span v-if="tx.parentCategoryId" class="tx-sub">/ {{ tx.categoryName }}</span>
                </div>
                <div class="tx-meta m-ellipsis">
                  <span v-if="tx.transactionTime">{{ String(tx.transactionTime).slice(0, 5) }} · </span>
                  <span>{{ tx.accountName || '-' }}</span>
                  <template v-if="tx.merchantName"> · {{ tx.merchantName }}</template>
                  <template v-if="tx.type === 3 && tx.targetAccountName">
                    → {{ tx.targetAccountId === tx.accountId ? '自己' : tx.targetAccountName }}
                  </template>
                </div>
                <div v-if="hasBadges(tx)" class="tx-badges">
                  <n-tag v-if="!tx.isConfirmed" size="tiny" type="warning" :bordered="false">待入账</n-tag>
                  <n-tag v-if="tx.reviewStatus === 'PENDING'" size="tiny" type="warning" :bordered="false">待复核</n-tag>
                  <n-tag v-if="tx.reconciled" size="tiny" type="info" :bordered="false">已对账</n-tag>
                  <n-tag v-for="tag in (tx.tags || []).slice(0, 2)" :key="tag" size="tiny" :bordered="false">{{ tag }}</n-tag>
                  <button v-if="tx.reviewStatus === 'PENDING'" class="review-link" @click.stop="openReview(tx)">去复核</button>
                </div>
              </div>
              <div class="tx-side">
                <div class="tx-amount m-amount" :class="amountClass(tx.type)">
                  {{ amountSign(tx.type) }}¥{{ formatNum(tx.amount) }}
                </div>
                <div v-if="tx.currency !== 'CNY'" class="tx-currency">{{ tx.currency }}</div>
              </div>
            </div>
          </div>
        </section>

        <div v-if="hasMore" class="m-load-more">
          <n-button secondary :loading="loading" @click="loadMore">
            加载更多（还有 {{ pagination.itemCount - transactions.length }} 笔）
          </n-button>
        </div>
        <div v-else-if="transactions.length > 4" class="list-end">已显示全部 {{ pagination.itemCount }} 笔</div>
      </div>

      <div v-else-if="!loading" class="m-card">
        <div class="m-empty-cta">
          <div class="cta-icon"><n-icon :size="26"><ReceiptOutline /></n-icon></div>
          <div class="cta-text">当前条件下没有交易<br />换个日期或清空筛选试试</div>
          <n-button secondary round @click="resetFilters">清空筛选</n-button>
        </div>
      </div>
    </n-spin>

    <!-- 筛选面板 -->
    <n-drawer v-model:show="showFilter" placement="bottom" :height="'86vh'">
      <n-drawer-content title="筛选交易" closable>
        <div class="m-form sheet-form">
          <n-form-item label="日期范围">
            <n-date-picker
              v-model:value="sheetRange"
              type="daterange"
              value-format="yyyy-MM-dd"
              size="large"
              clearable
              style="width: 100%"
            />
          </n-form-item>
          <n-form-item label="账户">
            <n-select v-model:value="filters.accountId" :options="accountOptions" placeholder="全部账户" size="large" clearable />
          </n-form-item>
          <n-form-item label="分类">
            <n-tree-select
              v-model:value="filters.categoryId"
              :options="categoryOptions"
              placeholder="全部分类"
              size="large"
              clearable
              key-field="value"
              label-field="label"
              children-field="children"
            />
          </n-form-item>
          <n-form-item label="标签">
            <n-select v-model:value="filters.tags" :options="tagOptions" multiple filterable clearable placeholder="不限" size="large" />
          </n-form-item>
          <n-form-item label="入账状态">
            <n-select v-model:value="filters.isConfirmed" :options="confirmOptions" placeholder="不限" size="large" clearable />
          </n-form-item>
          <n-form-item label="复核状态">
            <n-select v-model:value="filters.reviewStatus" :options="reviewOptions" placeholder="不限" size="large" clearable />
          </n-form-item>
          <n-form-item label="对账状态">
            <n-select v-model:value="filters.reconciled" :options="reconcileOptions" placeholder="不限" size="large" clearable />
          </n-form-item>
        </div>
        <template #footer>
          <div class="m-sheet-actions">
            <n-button type="primary" @click="applySheet">查看结果</n-button>
            <n-button quaternary @click="resetFilters">清空全部筛选</n-button>
          </div>
        </template>
      </n-drawer-content>
    </n-drawer>

    <!-- 复核弹窗 -->
    <n-drawer v-model:show="showReview" placement="bottom" :height="'78vh'" class="review-drawer">
      <n-drawer-content title="复核交易" closable>
        <div v-if="reviewRow" class="review-summary">
          <div class="review-amount">{{ reviewRow.transactionDate }} · {{ reviewRow.type === 1 ? '-' : '+' }}¥{{ formatNum(reviewRow.amount) }}</div>
          <div class="review-note">{{ reviewRow.note || '无备注' }}</div>
        </div>
        <n-radio-group v-model:value="reviewAction" class="review-options">
          <n-space vertical size="large">
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
          size="large"
          style="margin-top: 16px"
        />
        <template #footer>
          <div class="m-sheet-actions">
            <n-button type="primary" :loading="reviewSubmitting" @click="submitReview">保存复核</n-button>
            <n-button quaternary @click="showReview = false">取消</n-button>
          </div>
        </template>
      </n-drawer-content>
    </n-drawer>

    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { transactionApi, accountApi, categoryApi } from '@/api'
import { OptionsOutline, ReceiptOutline } from '@vicons/ionicons5'
import dayjs from 'dayjs'
import { formatNum } from '@/utils/display'

const router = useRouter()
const message = useMessage()
const loading = ref(false)
const transactions = ref([])
const summary = ref(null)

const typeLabels = { 1: '支出', 2: '收入', 3: '转账', 4: '退款' }
const WEEKDAYS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

const defaultRange = () => [dayjs().subtract(29, 'day').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')]

const datePresets = [
  { label: '30天', value: '30d' },
  { label: '本月', value: 'month' },
  { label: '上月', value: 'lastMonth' },
  { label: '全部', value: 'all' }
]
const datePreset = ref('30d')
const customRange = ref(defaultRange())

const filters = ref({
  type: null,
  accountId: null,
  categoryId: null,
  tags: [],
  isConfirmed: null,
  reviewStatus: null,
  reconciled: null
})

const typeOptions = [
  { label: '全部', value: null },
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

const showFilter = ref(false)
const sheetRange = computed({
  get: () => customRange.value,
  set: (val) => {
    customRange.value = val && val.length === 2 ? val : null
    if (customRange.value) datePreset.value = 'custom'
  }
})

const showReview = ref(false)
const reviewRow = ref(null)
const reviewAction = ref(null)
const reviewCategoryId = ref(null)
const reviewSubmitting = ref(false)

const effectiveRange = computed(() => {
  const today = dayjs()
  switch (datePreset.value) {
    case 'month':
      return [today.startOf('month').format('YYYY-MM-DD'), today.format('YYYY-MM-DD')]
    case 'lastMonth': {
      const prev = today.subtract(1, 'month')
      return [prev.startOf('month').format('YYYY-MM-DD'), prev.endOf('month').format('YYYY-MM-DD')]
    }
    case '30d':
      return defaultRange()
    case 'all':
      return null
    case 'custom':
      return customRange.value
    default:
      return null
  }
})

const categoryNameMap = computed(() => {
  const map = new Map()
  const walk = (list) => {
    for (const c of list || []) {
      map.set(c.id, c.name)
      if (c.children?.length) walk(c.children)
    }
  }
  walk(categories.value)
  return map
})

const filterChips = computed(() => {
  const chips = []
  if (filters.value.accountId) {
    const acc = accountOptions.value.find(a => a.value === filters.value.accountId)
    chips.push({ key: 'accountId', label: acc?.label || '账户' })
  }
  if (filters.value.categoryId) {
    chips.push({ key: 'categoryId', label: categoryNameMap.value.get(filters.value.categoryId) || '分类' })
  }
  if (filters.value.tags?.length) {
    chips.push({ key: 'tags', label: `标签：${filters.value.tags.join('、')}` })
  }
  if (filters.value.isConfirmed !== null) {
    chips.push({ key: 'isConfirmed', label: filters.value.isConfirmed ? '已入账' : '待入账' })
  }
  if (filters.value.reviewStatus) {
    chips.push({ key: 'reviewStatus', label: filters.value.reviewStatus === 'REVIEWED' ? '已复核' : '待复核' })
  }
  if (filters.value.reconciled !== null) {
    chips.push({ key: 'reconciled', label: filters.value.reconciled ? '已对账' : '未对账' })
  }
  if (datePreset.value === 'custom' && customRange.value) {
    chips.push({ key: 'date', label: `${customRange.value[0]} 至 ${customRange.value[1]}` })
  }
  return chips
})
const activeFilterCount = computed(() => filterChips.value.filter(c => c.key !== 'date').length)

const groups = computed(() => {
  const map = new Map()
  for (const tx of transactions.value) {
    const key = tx.transactionDate || '未知日期'
    if (!map.has(key)) {
      map.set(key, { date: key, label: dateLabel(key), items: [], expense: 0, income: 0 })
    }
    const group = map.get(key)
    group.items.push(tx)
    const amount = Number(tx.amount) || 0
    if (tx.type === 1) group.expense += amount
    else if (tx.type === 4) group.expense -= amount
    else if (tx.type === 2) group.income += amount
  }
  for (const group of map.values()) {
    group.expense = Math.max(0, Math.round(group.expense * 100) / 100)
    group.income = Math.round(group.income * 100) / 100
  }
  return [...map.values()]
})

function dateLabel(date) {
  const day = dayjs(date)
  if (!day.isValid()) return date
  const today = dayjs()
  if (day.isSame(today, 'day')) return '今天'
  if (day.isSame(today.subtract(1, 'day'), 'day')) return '昨天'
  return day.format('M月D日') + ' ' + WEEKDAYS[day.day()]
}

function amountClass(type) {
  if (type === 1) return 'expense'
  if (type === 2 || type === 4) return 'income'
  return ''
}
function amountSign(type) {
  if (type === 1) return '-'
  if (type === 2 || type === 4) return '+'
  return ''
}
function hasBadges(tx) {
  return !tx.isConfirmed || tx.reviewStatus === 'PENDING' || tx.reconciled || (tx.tags || []).length
}

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
    reload()
  } catch (e) {
    message.error('复核失败: ' + (e.response?.data?.message || e.message))
  } finally {
    reviewSubmitting.value = false
  }
}

const pagination = ref({ page: 1, pageSize: 20, itemCount: 0 })
const hasMore = computed(() => transactions.value.length < pagination.value.itemCount)

function chooseDatePreset(value) {
  if (value === 'custom') {
    showFilter.value = true
    return
  }
  datePreset.value = value
  reload()
}

function setType(value) {
  filters.value.type = value
  reload()
}

function clearChip(key) {
  if (key === 'date') {
    datePreset.value = '30d'
    customRange.value = defaultRange()
  } else if (key === 'tags') {
    filters.value.tags = []
  } else {
    filters.value[key] = null
  }
  reload()
}

function applySheet() {
  showFilter.value = false
  reload()
}

function resetFilters() {
  filters.value = { type: null, accountId: null, categoryId: null, tags: [], isConfirmed: null, reviewStatus: null, reconciled: null }
  datePreset.value = '30d'
  customRange.value = defaultRange()
  showFilter.value = false
  reload()
}

function buildParams() {
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
  if (filters.value.tags?.length) params.tags = filters.value.tags.join(',')
  const range = effectiveRange.value
  if (range?.length === 2) {
    params.startDate = range[0]
    params.endDate = range[1]
  }
  return params
}

function reload() {
  pagination.value.page = 1
  loadData(true)
}

function loadMore() {
  pagination.value.page += 1
  loadData(false)
}

async function loadData(reset) {
  loading.value = true
  try {
    const params = buildParams()
    const res = await transactionApi.list(params)
    const items = res.data.items || []
    transactions.value = reset ? items : transactions.value.concat(items)
    pagination.value.itemCount = res.data.total
    if (reset) {
      const sumRes = await transactionApi.summary(params)
      summary.value = sumRes.data
    } else if (!transactions.value.length) {
      summary.value = null
    }
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
  reload()
})
</script>

<style scoped>
.mobile-transactions {
  padding-bottom: calc(88px + env(safe-area-inset-bottom, 0px));
}

/* 筛选区 */
.filter-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.filter-top {
  display: flex;
  align-items: center;
  gap: 8px;
}
.date-chips {
  flex: 1;
  min-width: 0;
}
.filter-trigger {
  gap: 5px;
  padding: 0 12px;
}
.filter-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: rgba(255, 255, 255, 0.24);
  font-size: 11px;
  line-height: 1;
}
.filter-trigger:not(.active) .filter-count {
  background: var(--bp-primary);
  color: #fff;
}
.type-chips {
  padding-top: 2px;
}
.applied-chips .m-chip {
  height: 30px;
  font-size: 12px;
  padding: 0 10px;
  color: #475569;
}
.chip-x {
  font-size: 14px;
  line-height: 1;
  opacity: 0.6;
  margin-left: 2px;
}

/* 汇总 */
.summary-card {
  padding: 12px 16px;
}
.sum-top {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  font-size: 13px;
  color: var(--bp-muted);
}
.sum-top b {
  font-size: 16px;
  color: var(--bp-ink);
  font-variant-numeric: tabular-nums;
}
.sum-net b {
  color: var(--bp-ink);
}
.sum-detail {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 14px;
  margin-top: 6px;
  font-size: 12px;
  color: #94a3b8;
  font-variant-numeric: tabular-nums;
}
.sum-detail .expense { color: #ef4444; }
.sum-detail .refund { color: #2563eb; }
.sum-detail .income { color: #10b981; }

/* 日期分组 */
.tx-groups {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.tx-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.group-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 0 4px;
}
.group-date {
  font-size: 13px;
  font-weight: 650;
  color: #475569;
}
.group-sum {
  font-size: 11px;
  color: #94a3b8;
  font-variant-numeric: tabular-nums;
}
.tx-card {
  background: #fff;
  border-radius: var(--m-radius);
  box-shadow: var(--bp-shadow);
  padding: 2px 16px;
}
.tx-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px 0;
  border-bottom: 1px solid #f1f5f9;
  -webkit-tap-highlight-color: transparent;
}
.tx-row:last-child {
  border-bottom: none;
}
.tx-row:active {
  background: #f8fafc;
}
.tx-main {
  flex: 1;
  min-width: 0;
}
.tx-title {
  display: flex;
  align-items: baseline;
  gap: 4px;
  font-size: 15px;
  font-weight: 550;
}
.tx-cat {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tx-sub {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 400;
  color: #94a3b8;
}
.tx-meta {
  margin-top: 3px;
  font-size: 12px;
  color: #94a3b8;
}
.tx-badges {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
}
.review-link {
  border: none;
  background: none;
  padding: 0 2px;
  font-size: 12px;
  color: var(--bp-primary);
  font-weight: 500;
}
.tx-side {
  flex-shrink: 0;
  text-align: right;
  align-self: flex-start;
  padding-top: 2px;
}
.tx-amount {
  font-size: 16px;
}
.tx-amount.expense { color: #ef4444; }
.tx-amount.income { color: #10b981; }
.tx-currency {
  margin-top: 2px;
  font-size: 11px;
  color: #94a3b8;
}

.list-end {
  padding: 4px 0 8px;
  text-align: center;
  font-size: 12px;
  color: #b0b8c4;
}

/* 筛选面板 */
.sheet-form {
  padding-bottom: 4px;
}

/* 复核 */
.review-summary {
  background: #f8fafc;
  border-radius: 12px;
  padding: 12px 14px;
}
.review-amount {
  font-size: 16px;
  font-weight: 650;
  font-variant-numeric: tabular-nums;
}
.review-note {
  margin-top: 4px;
  font-size: 12px;
  color: var(--bp-muted);
}
.review-options {
  margin-top: 18px;
}

</style>