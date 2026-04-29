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
        <n-select v-model:value="filters.isConfirmed" :options="confirmOptions" placeholder="状态" size="small" style="flex: 1" clearable />
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
          <div class="tx-category" :style="{ color: tx.categoryColor }">{{ tx.categoryName || '-' }}</div>
          <div class="tx-meta">
            <span>{{ tx.transactionDate }}</span>
            <span v-if="tx.accountName">{{ tx.accountName }}</span>
            <span v-if="tx.merchantName">{{ tx.merchantName }}</span>
          </div>
          <div class="tx-tags">
            <n-tag v-if="!tx.isConfirmed" size="tiny" type="warning" :bordered="false">待确认</n-tag>
            <n-tag v-for="tag in (tx.tags || [])" :key="tag" size="tiny" type="info" :bordered="false" style="margin-left: 4px">{{ tag }}</n-tag>
          </div>
        </div>
        <div class="tx-right">
          <div class="tx-amount" :class="tx.type === 1 ? 'expense' : tx.type === 2 ? 'income' : ''">
            {{ tx.type === 1 ? '-' : tx.type === 2 ? '+' : '' }}¥{{ formatNum(tx.amount) }}
          </div>
          <div v-if="tx.currency !== 'CNY'" class="tx-currency">{{ tx.currency }}</div>
        </div>
      </div>
    </div>
    <n-empty v-else description="暂无交易" />

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

const router = useRouter()
const message = useMessage()
const loading = ref(false)
const transactions = ref([])

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
  itemCount: 0
})

const totalPages = computed(() => Math.ceil(pagination.value.itemCount / pagination.value.pageSize))

function applyFilters() {
  pagination.value.page = 1
  loadData()
}

function resetFilters() {
  filters.value = { type: null, accountId: null, categoryId: null, isConfirmed: null, dateRange: null }
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
      confirmed: filters.value.isConfirmed
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

function formatNum(n) {
  if (n == null) return '0.00'
  return Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

onMounted(async () => {
  try {
    const [accRes, catRes] = await Promise.all([
      accountApi.list(true),
      categoryApi.tree(null)
    ])
    accountOptions.value = accRes.data.map(a => ({ label: a.name, value: a.id }))

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
</style>
