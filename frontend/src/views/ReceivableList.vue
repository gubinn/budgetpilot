<template>
  <div class="receivable-page">
    <n-card title="往来款" :bordered="false">
      <template #header-extra>
        <n-space :size="8" align="center" :wrap="true">
          <n-radio-group v-model:value="directionFilter" size="small">
            <n-radio-button value="ALL">全部</n-radio-button>
            <n-radio-button value="RECEIVABLE">应收</n-radio-button>
            <n-radio-button value="PAYABLE">应付</n-radio-button>
          </n-radio-group>
          <n-input
            v-model:value="keyword"
            placeholder="搜索对方"
            clearable
            size="small"
            style="width: 150px"
          />
          <n-checkbox v-model:checked="showClosed" size="small">显示已结清</n-checkbox>
          <n-switch v-model:value="includeUnconfirmed" size="small">
            <template #checked>含待确认</template>
            <template #unchecked>仅已确认</template>
          </n-switch>
          <n-button size="small" :loading="loading" @click="loadData">刷新</n-button>
        </n-space>
      </template>

      <div class="summary-grid" :class="{ compact: isCompact }">
        <div class="summary-card receivable">
          <div class="summary-label">应收合计</div>
          <div class="summary-value">¥ {{ formatNum(summary.receivableTotal) }}</div>
          <div class="summary-sub">{{ summary.counterpartyCount }} 人未结清</div>
        </div>
        <div class="summary-card payable">
          <div class="summary-label">应付合计</div>
          <div class="summary-value">¥ {{ formatNum(summary.payableTotal) }}</div>
          <div class="summary-sub">我借入未归还</div>
        </div>
        <div class="summary-card net">
          <div class="summary-label">净往来</div>
          <div class="summary-value" :class="netClass">
            ¥ {{ formatNum(summary.net) }}
          </div>
          <div class="summary-sub">应收 − 应付</div>
        </div>
        <div class="summary-card age">
          <div class="summary-label">最长账龄</div>
          <div class="summary-value">{{ summary.oldestDays }} 天</div>
          <div class="summary-sub" :class="{ danger: summary.overdueCount > 0 }">
            {{ summary.overdueCount > 0 ? `${summary.overdueCount} 项已逾期` : '暂无逾期' }}
          </div>
        </div>
      </div>

      <n-alert
        v-if="summary.unlabeledCount > 0"
        type="warning"
        :show-icon="true"
        style="margin-bottom: 12px"
      >
        有 {{ summary.unlabeledCount }} 笔往来款没有识别出对方。建议在交易记录里把对方姓名补到「商户」字段。
      </n-alert>

      <n-data-table
        v-if="!isCompact"
        :columns="columns"
        :data="filteredGroups"
        :loading="loading"
        :row-key="rowKey"
        :scroll-x="1080"
        :bordered="false"
      />
      <div v-else class="group-cards">
        <div
          v-for="group in filteredGroups"
          :key="rowKey(group)"
          class="group-card"
          @click="openDetail(group)"
        >
          <div class="group-card-head">
            <div>
              <span class="group-name">{{ group.counterparty }}</span>
              <n-tag :type="group.direction === 'RECEIVABLE' ? 'warning' : 'info'" size="tiny" :bordered="false">
                {{ directionLabel(group.direction) }}
              </n-tag>
            </div>
            <n-tag :type="statusTag(group).type" size="small" :bordered="false">
              {{ statusTag(group).text }}
            </n-tag>
          </div>
          <div class="group-card-amount" :class="{ overdue: group.overdueDays > 0 }">
            ¥ {{ formatNum(group.outstanding) }}
          </div>
          <div class="group-card-meta">
            <span>累计 {{ principalLabel(group) }} ¥{{ formatNum(group.principalTotal) }}</span>
            <span>{{ settledLabel(group) }} ¥{{ formatNum(group.settledTotal) }}</span>
          </div>
          <div class="group-card-foot">
            <span>账龄 {{ group.ageDays }} 天</span>
            <span>最近 {{ group.lastActivityDate || '-' }}</span>
          </div>
        </div>
        <n-empty v-if="!filteredGroups.length" description="没有符合条件的往来款" style="padding: 34px 0" />
      </div>

      <n-empty
        v-if="!isCompact && !filteredGroups.length"
        description="没有符合条件的往来款"
        style="padding: 48px 0"
      />
    </n-card>

    <n-drawer v-model:show="showDetail" :width="isCompact ? '100%' : 560" placement="right">
      <n-drawer-content :title="detailTitle" closable>
        <template v-if="detail">
          <div class="detail-hero" :class="detail.direction === 'RECEIVABLE' ? 'receivable' : 'payable'">
            <div class="detail-hero-label">未结清</div>
            <div class="detail-hero-value">¥ {{ formatNum(detail.outstanding) }}</div>
            <div class="detail-hero-sub">
              {{ directionLabel(detail.direction) }} · 账龄 {{ detail.ageDays }} 天
              <template v-if="detail.overdueDays > 0"> · 逾期 {{ detail.overdueDays }} 天</template>
            </div>
          </div>

          <n-alert
            v-if="detail.overdueDays > 0"
            type="error"
            :show-icon="true"
            style="margin-bottom: 14px"
          >
            已超过约定还款日 {{ detail.overdueDays }} 天，约定日期为 {{ detail.expectedRepayDate }}。
          </n-alert>
          <n-alert
            v-else-if="detail.expectedRepayDate"
            type="info"
            :show-icon="true"
            style="margin-bottom: 14px"
          >
            约定还款日：{{ detail.expectedRepayDate }}。
          </n-alert>

          <div class="detail-stats">
            <div>
              <div class="detail-stat-label">累计{{ principalLabel(detail) }}</div>
              <div class="detail-stat-value">¥ {{ formatNum(detail.principalTotal) }}</div>
            </div>
            <div>
              <div class="detail-stat-label">累计{{ settledLabel(detail) }}</div>
              <div class="detail-stat-value">¥ {{ formatNum(detail.settledTotal) }}</div>
            </div>
            <div>
              <div class="detail-stat-label">首笔日期</div>
              <div class="detail-stat-value small">{{ detail.firstLoanDate || '-' }}</div>
            </div>
            <div>
              <div class="detail-stat-label">最近往来</div>
              <div class="detail-stat-value small">{{ detail.lastActivityDate || '-' }}</div>
            </div>
          </div>

          <n-alert
            v-if="Number(detail.overpaid) > 0"
            type="warning"
            :show-icon="true"
            style="margin-bottom: 14px"
          >
            多{{ detail.direction === 'RECEIVABLE' ? '收' : '还' }} ¥{{ formatNum(detail.overpaid) }}，
            会抵扣下一笔同方向的往来款。
          </n-alert>

          <div class="detail-section-title">流水明细（{{ detail.transactionCount }} 笔）</div>
          <n-timeline>
            <n-timeline-item
              v-for="tx in detail.transactions"
              :key="tx.id"
              :type="effectTagType(tx.effect)"
              :title="`${effectLabel(tx.effect)} ¥${formatNum(tx.amount)}`"
            >
              <div class="tx-line">{{ tx.date }} · {{ tx.accountName || '账户未知' }}</div>
              <div class="tx-line muted">
                {{ tx.categoryName || '-' }}
                <template v-if="tx.note"> · {{ tx.note }}</template>
              </div>
            </n-timeline-item>
          </n-timeline>
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup>
import { computed, h, onMounted, ref, watch } from 'vue'
import { NButton, NTag, useMessage } from 'naive-ui'
import { receivableApi } from '@/api'
import { formatNum } from '@/utils/display'
import { useCompactViewport } from '@/composables/useCompactViewport'

const message = useMessage()
const isCompact = useCompactViewport()

const loading = ref(false)
const includeUnconfirmed = ref(false)
const showClosed = ref(false)
const directionFilter = ref('ALL')
const keyword = ref('')
const summary = ref({
  receivableTotal: 0,
  payableTotal: 0,
  net: 0,
  counterpartyCount: 0,
  oldestDays: 0,
  overdueCount: 0,
  overdueTotal: 0,
  unlabeledCount: 0
})
const groups = ref([])
const showDetail = ref(false)
const detail = ref(null)

const netClass = computed(() => (Number(summary.value.net) >= 0 ? 'positive' : 'negative'))

const filteredGroups = computed(() => {
  const term = keyword.value.trim()
  return groups.value.filter(group => {
    if (directionFilter.value !== 'ALL' && group.direction !== directionFilter.value) {
      return false
    }
    if (!showClosed.value && Number(group.outstanding) <= 0) {
      return false
    }
    if (term && !String(group.counterparty || '').includes(term)) {
      return false
    }
    return true
  })
})

const columns = [
  {
    title: '对方',
    key: 'counterparty',
    minWidth: 150,
    render: row => h('div', { class: 'counterparty-cell' }, [
      h('span', { class: 'counterparty-name' }, row.counterparty),
      row.counterparty === '未标注对方'
        ? h(NTag, { size: 'tiny', type: 'warning', bordered: false }, { default: () => '待补对方' })
        : null
    ])
  },
  {
    title: '方向',
    key: 'direction',
    width: 80,
    render: row => h(NTag, {
      size: 'small',
      type: row.direction === 'RECEIVABLE' ? 'warning' : 'info',
      bordered: false
    }, { default: () => directionLabel(row.direction) })
  },
  {
    title: '未结清',
    key: 'outstanding',
    width: 130,
    align: 'right',
    render: row => h('span', {
      class: row.overdueDays > 0 ? 'amount overdue' : 'amount'
    }, `¥${formatNum(row.outstanding)}`)
  },
  {
    title: '累计借出/借入',
    key: 'principalTotal',
    width: 140,
    align: 'right',
    render: row => `¥${formatNum(row.principalTotal)}`
  },
  {
    title: '累计收回/归还',
    key: 'settledTotal',
    width: 140,
    align: 'right',
    render: row => `¥${formatNum(row.settledTotal)}`
  },
  {
    title: '最近往来',
    key: 'lastActivityDate',
    width: 115,
    render: row => row.lastActivityDate || '-'
  },
  {
    title: '账龄',
    key: 'ageDays',
    width: 92,
    render: row => `${row.ageDays} 天`
  },
  {
    title: '约定还款',
    key: 'expectedRepayDate',
    width: 115,
    render: row => row.expectedRepayDate || '-'
  },
  {
    title: '状态',
    key: 'status',
    width: 95,
    render: row => {
      const tag = statusTag(row)
      return h(NTag, { size: 'small', type: tag.type, bordered: false }, { default: () => tag.text })
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 90,
    render: row => h(NButton, {
      size: 'small',
      quaternary: true,
      type: 'primary',
      onClick: () => openDetail(row)
    }, { default: () => '明细' })
  }
]

function rowKey(group) {
  return `${group.direction}:${group.counterparty}`
}

function directionLabel(value) {
  return value === 'RECEIVABLE' ? '应收' : '应付'
}

function principalLabel(group) {
  return group.direction === 'RECEIVABLE' ? '借出' : '借入'
}

function settledLabel(group) {
  return group.direction === 'RECEIVABLE' ? '收回' : '归还'
}

function effectLabel(effect) {
  return {
    LEND: '借出',
    COLLECT: '收回',
    BORROW: '借入',
    REPAY: '归还'
  }[effect] || effect
}

function effectTagType(effect) {
  return effect === 'LEND' || effect === 'BORROW' ? 'warning' : 'success'
}

function statusTag(group) {
  if (Number(group.outstanding) <= 0) {
    return { text: '已结清', type: 'success' }
  }
  if (group.overdueDays > 0) {
    return { text: '已逾期', type: 'error' }
  }
  if (Number(group.settledTotal) > 0) {
    return { text: `部分${group.direction === 'RECEIVABLE' ? '收回' : '归还'}`, type: 'warning' }
  }
  return { text: '未结清', type: 'warning' }
}

function openDetail(group) {
  detail.value = group
  showDetail.value = true
}

async function loadData() {
  loading.value = true
  try {
    const res = await receivableApi.overview(includeUnconfirmed.value)
    summary.value = res.data?.summary || summary.value
    groups.value = res.data?.groups || []
  } catch (e) {
    message.error(e.message || '加载往来款失败')
  } finally {
    loading.value = false
  }
}

watch(includeUnconfirmed, loadData)
onMounted(loadData)
</script>

<style scoped>
.receivable-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.summary-grid.compact {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.summary-card {
  border-radius: 14px;
  padding: 14px 16px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
}
.summary-card.receivable { background: #fff7ed; border-color: #ffedd5; }
.summary-card.payable { background: #eff6ff; border-color: #dbeafe; }
.summary-card.net { background: #f0fdf4; border-color: #dcfce7; }
.summary-card.age { background: #f8fafc; }

.summary-label {
  font-size: 12px;
  color: #64748b;
}
.summary-value {
  margin-top: 4px;
  font-size: 22px;
  font-weight: 750;
  letter-spacing: -0.02em;
  color: #0f172a;
  font-variant-numeric: tabular-nums;
}
.summary-value.positive { color: #059669; }
.summary-value.negative { color: #dc2626; }
.summary-sub {
  margin-top: 4px;
  font-size: 11px;
  color: #94a3b8;
}
.summary-sub.danger { color: #dc2626; }

:deep(.counterparty-cell) {
  display: flex;
  align-items: center;
  gap: 6px;
}
:deep(.counterparty-name) {
  font-weight: 600;
  color: #0f172a;
}
:deep(.amount) {
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
:deep(.amount.overdue) {
  color: #dc2626;
}

.group-cards {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.group-card {
  background: #fff;
  border: 1px solid #eef2f7;
  border-radius: 14px;
  padding: 14px;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.group-card:active {
  border-color: #bfdbfe;
  box-shadow: 0 6px 18px -12px rgba(37, 99, 235, 0.5);
}
.group-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.group-card-head > div {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}
.group-name {
  font-weight: 650;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.group-card-amount {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 750;
  color: #0f172a;
  font-variant-numeric: tabular-nums;
}
.group-card-amount.overdue { color: #dc2626; }
.group-card-meta,
.group-card-foot {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-top: 6px;
  font-size: 11px;
  color: #64748b;
}
.group-card-foot {
  padding-top: 8px;
  margin-top: 8px;
  border-top: 1px solid #f1f5f9;
  color: #94a3b8;
}

.detail-hero {
  border-radius: 16px;
  padding: 18px;
  margin-bottom: 14px;
}
.detail-hero.receivable {
  background: linear-gradient(135deg, #fff7ed, #ffedd5);
}
.detail-hero.payable {
  background: linear-gradient(135deg, #eff6ff, #dbeafe);
}
.detail-hero-label { font-size: 12px; color: #64748b; }
.detail-hero-value {
  margin-top: 4px;
  font-size: 30px;
  font-weight: 750;
  color: #0f172a;
  font-variant-numeric: tabular-nums;
}
.detail-hero-sub { margin-top: 6px; font-size: 12px; color: #64748b; }

.detail-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  padding: 14px;
  border-radius: 12px;
  background: #f8fafc;
  margin-bottom: 16px;
}
.detail-stat-label { font-size: 11px; color: #94a3b8; }
.detail-stat-value {
  margin-top: 3px;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  font-variant-numeric: tabular-nums;
}
.detail-stat-value.small { font-size: 13px; }

.detail-section-title {
  margin: 18px 0 12px;
  font-size: 13px;
  font-weight: 650;
  color: #334155;
}
.tx-line { font-size: 12px; color: #475569; margin-top: 2px; }
.tx-line.muted { color: #94a3b8; }

@media (max-width: 640px) {
  .summary-value { font-size: 19px; }
  .summary-grid { gap: 8px; }
}
</style>