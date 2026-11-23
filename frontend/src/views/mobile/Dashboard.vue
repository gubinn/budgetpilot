<template>
  <div class="mobile-dashboard">
    <!-- 总资产卡片 -->
    <div class="asset-card">
      <img src="/logo-mark.svg" class="asset-mark" alt="" />
      <div class="asset-label">总资产</div>
      <div class="asset-value">¥ {{ formatNum(totalAssets) }}</div>
      <div class="asset-sub">所有 CNY 账户余额合计</div>
    </div>

    <!-- 本月概况 -->
    <div class="summary-grid">
      <div v-for="card in summaryCards" :key="card.label" class="summary-item">
        <div class="summary-label">{{ card.label }}</div>
        <div class="summary-value" :class="card.color">{{ card.value }}</div>
      </div>
    </div>

    <!-- 饼图：分类占比 -->
    <div v-if="hasCategoryData" class="chart-card">
      <div class="chart-header">
        <h3 class="chart-title">分类占比</h3>
        <n-radio-group v-model:value="pieMode" size="small">
          <n-radio-button value="excludingFixed">不含固定支出</n-radio-button>
          <n-radio-button value="all">全部支出</n-radio-button>
        </n-radio-group>
      </div>
      <div v-show="pieChartData.length" ref="pieChartRef" style="width: 100%; height: 200px"></div>
      <div v-if="pieChartData.length" class="pie-legend">
        <div v-for="item in pieLegend" :key="item.name" class="legend-item">
          <span class="legend-dot" :style="{ background: item.color }"></span>
          <span class="legend-name m-ellipsis">{{ item.name }}</span>
          <span class="legend-pct">{{ item.pct }}%</span>
        </div>
      </div>
      <n-empty v-if="!pieChartData.length" description="本月无支出数据" style="padding: 40px 0" />
    </div>

    <!-- 饼图：商户消费 -->
    <div v-if="hasMerchantData" class="chart-card">
      <div class="chart-header">
        <h3 class="chart-title">商户消费</h3>
        <n-radio-group v-model:value="merchantPieMode" size="small">
          <n-radio-button value="excludingFixed">不含固定支出</n-radio-button>
          <n-radio-button value="all">全部支出</n-radio-button>
        </n-radio-group>
      </div>
      <div v-show="merchantPieData.length" ref="merchantPieRef" style="width: 100%; height: 200px"></div>
      <div v-if="merchantPieData.length" class="pie-legend">
        <div v-for="item in merchantLegend" :key="item.name" class="legend-item">
          <span class="legend-dot" :style="{ background: item.color }"></span>
          <span class="legend-name m-ellipsis">{{ item.name }}</span>
          <span class="legend-pct">{{ item.pct }}%</span>
        </div>
      </div>
      <n-empty v-if="!merchantPieData.length" description="本月无商户消费数据" style="padding: 40px 0" />
    </div>

    <!-- 预算进度：月度 + 年度 -->
    <div v-if="monthlyBudget || annualBudget" class="section-card">
      <h3 class="section-title">预算进度</h3>

      <template v-if="monthlyBudget">
        <div class="budget-group-title">月度 · {{ monthlyBudget.yearMonth }}</div>
        <div class="budget-overall">
          <span>已花 ¥{{ formatNum(monthlyBudget.totalSpent) }} / ¥{{ formatNum(monthlyBudget.totalBudget) }}</span>
          <span class="budget-pct" :class="monthlyBudget.progressPct > 100 ? 'exceeded' : 'normal'">{{ monthlyBudget.progressPct }}%</span>
        </div>
        <n-progress
          type="line"
          :percentage="Math.min(monthlyBudget.progressPct, 100)"
          :status="getProgressStatus(monthlyBudget.progressPct > 100 ? 'EXCEEDED' : 'NORMAL')"
          :show-indicator="false"
          :height="6"
          style="margin-bottom: 12px"
        />
        <div v-for="item in monthlyBudget.items" :key="item.categoryId" class="budget-item">
          <div class="budget-header">
            <span>{{ item.categoryName }}</span>
            <span class="budget-pct" :class="item.status.toLowerCase()">{{ item.progressPct }}%</span>
          </div>
          <n-progress
            type="line"
            :percentage="Math.min(item.progressPct, 100)"
            :status="getProgressStatus(item.status)"
            :show-indicator="false"
            :height="6"
          />
          <div class="budget-amounts">
            <span>已花 ¥{{ formatNum(item.spent) }}</span>
            <span>预算 ¥{{ formatNum(item.budget) }}</span>
          </div>
        </div>
      </template>
      <template v-else>
        <div class="budget-group-title">月度 · {{ currentMonth }}</div>
        <div class="budget-empty">
          <span>本月未设置预算</span>
          <n-button text type="primary" size="small" @click="router.push('/budget')">去设置 →</n-button>
        </div>
      </template>

      <template v-if="annualBudget">
        <div class="budget-group-title" :class="{ spaced: monthlyBudget }">年度 · {{ annualBudget.year }}</div>
        <div class="budget-overall">
          <span>已花 ¥{{ formatNum(annualBudget.totalSpent) }} / ¥{{ formatNum(annualBudget.totalBudget) }}</span>
          <span class="budget-pct" :class="annualBudget.progressPct > 100 ? 'exceeded' : 'normal'">{{ annualBudget.progressPct }}%</span>
        </div>
        <n-progress
          type="line"
          :percentage="Math.min(annualBudget.progressPct, 100)"
          :status="getProgressStatus(annualBudget.progressPct > 100 ? 'EXCEEDED' : 'NORMAL')"
          :show-indicator="false"
          :height="6"
          style="margin-bottom: 12px"
        />
        <div v-for="item in annualBudget.items" :key="item.categoryId" class="budget-item">
          <div class="budget-header">
            <span>{{ item.categoryName }}</span>
            <span class="budget-pct" :class="item.status.toLowerCase()">{{ item.progressPct }}%</span>
          </div>
          <n-progress
            type="line"
            :percentage="Math.min(item.progressPct, 100)"
            :status="getProgressStatus(item.status)"
            :show-indicator="false"
            :height="6"
          />
          <div class="budget-amounts">
            <span>已花 ¥{{ formatNum(item.spent) }}</span>
            <span>预算 ¥{{ formatNum(item.budget) }}</span>
          </div>
        </div>
      </template>
    </div>

    <!-- 最近交易 -->
    <div class="section-card">
      <div class="section-header">
        <h3 class="section-title">最近交易</h3>
        <n-button text type="primary" size="small" @click="router.push('/transactions')">全部 →</n-button>
      </div>
      <div v-if="recentTransactions.length" class="tx-list">
        <div v-for="tx in recentTransactions" :key="tx.id" class="tx-item">
          <div class="tx-left">
            <div class="tx-category" :style="{ color: tx.categoryColor }">{{ tx.category }}</div>
            <div class="tx-meta">{{ tx.date }} · {{ tx.account }}</div>
          </div>
          <div class="tx-amount" :class="tx.type === 1 ? 'expense' : tx.type === 2 ? 'income' : ''">
            {{ tx.type === 1 ? '-' : '+' }}¥{{ tx.amount }}
          </div>
        </div>
      </div>
      <n-empty v-else description="暂无交易" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { reportApi, budgetApi, annualBudgetApi, transactionApi, accountApi } from '@/api'
import dayjs from 'dayjs'
import { formatNum, getProgressStatus } from '@/utils/display'

// 图表库按需加载，避免首屏下载 1MB 的 echarts
let echarts = null

const router = useRouter()
const message = useMessage()
const totalAssets = ref(0)
const summaryCards = ref([])
const monthlyBudget = ref(null)
const annualBudget = ref(null)
const recentTransactions = ref([])
const pieChartRef = ref(null)
const merchantPieRef = ref(null)
const pieChart = ref(null)
const merchantPieChart = ref(null)

const currentMonth = dayjs().format('YYYY-MM')

const pieMode = ref('excludingFixed')
const categorySharesAll = ref([])
const categorySharesExcludingFixed = ref([])
const pieChartData = computed(() =>
  pieMode.value === 'excludingFixed' ? categorySharesExcludingFixed.value : categorySharesAll.value
)
const hasCategoryData = computed(() =>
  categorySharesAll.value.length > 0 || categorySharesExcludingFixed.value.length > 0
)

function buildLegend(items, nameKey) {
  const total = items.reduce((sum, item) => sum + Number(item.amount || 0), 0)
  if (!total) return []
  const top = items.slice(0, 6).map(item => ({
    name: item[nameKey] || '未命名',
    color: item.categoryColor || item.merchantColor || '#94a3b8',
    pct: Math.round(Number(item.amount || 0) / total * 1000) / 10
  }))
  if (items.length > 6) {
    const rest = items.slice(6).reduce((sum, item) => sum + Number(item.amount || 0), 0)
    top.push({
      name: `其他 ${items.length - 6} 项`,
      color: '#cbd5e1',
      pct: Math.round(rest / total * 1000) / 10
    })
  }
  return top
}

const pieLegend = computed(() => buildLegend(pieChartData.value, 'categoryName'))
const merchantLegend = computed(() => buildLegend(merchantPieData.value, 'merchantName'))

watch(pieMode, async () => {
  await nextTick()
  renderPieChart()
})

const merchantPieMode = ref('excludingFixed')
const merchantSharesAll = ref([])
const merchantSharesExcludingFixed = ref([])
const merchantPieData = computed(() =>
  merchantPieMode.value === 'excludingFixed' ? merchantSharesExcludingFixed.value : merchantSharesAll.value
)
const hasMerchantData = computed(() =>
  merchantSharesAll.value.length > 0 || merchantSharesExcludingFixed.value.length > 0
)

watch(merchantPieMode, async () => {
  await nextTick()
  renderMerchantPieChart()
})

onMounted(async () => {
  echarts = await import('echarts')
  const now = dayjs()
  const month = now.format('YYYY-MM')

  try {
    const [summaryRes, budgetRes, txRes, assetsRes, merchantRes, annualRes] = await Promise.allSettled([
      reportApi.monthlySummary(month),
      budgetApi.progress(month, true),
      transactionApi.list({ page: 1, size: 10, sort: 'transaction_date,desc' }),
      accountApi.totalAssets(),
      reportApi.merchantDistribution(month),
      annualBudgetApi.progress(String(now.year()))
    ])

    // 月度概况
    if (summaryRes.status === 'fulfilled' && summaryRes.value.data?.monthlySummary) {
      const s = summaryRes.value.data.monthlySummary
      summaryCards.value = [
        { label: '本月收入', value: `¥${formatNum(s.totalIncome)}`, color: 'green' },
        { label: '本月支出', value: `¥${formatNum(s.totalExpense)}`, color: 'red' },
        { label: '本月结余', value: `¥${formatNum(s.balance)}`, color: s.balance >= 0 ? 'green' : 'red' },
        { label: '日均支出', value: `¥${formatNum(s.avgDailyExpense)}`, color: 'blue' }
      ]

      // 分类占比饼图
      categorySharesAll.value = s.categoryShares || []
      categorySharesExcludingFixed.value = s.categorySharesExcludingFixed || []
      await nextTick()
      renderPieChart()
    }

    // 预算进度：月度 + 年度
    if (budgetRes.status === 'fulfilled' && budgetRes.value.data) {
      monthlyBudget.value = budgetRes.value.data
      if (!budgetRes.value.data.items?.length) {
        monthlyBudget.value = null
      }
    }
    if (annualRes.status === 'fulfilled' && annualRes.value.data) {
      annualBudget.value = annualRes.value.data
    }

    // 最近交易
    if (txRes.status === 'fulfilled' && txRes.value.data?.items) {
      recentTransactions.value = txRes.value.data.items.map(t => ({
        id: t.id,
        date: t.transactionDate,
        account: t.accountName || '-',
        category: t.categoryName || '-',
        categoryColor: t.categoryColor,
        type: t.type,
        amount: formatNum(t.amount)
      }))
    }

    // 总资产
    if (assetsRes.status === 'fulfilled' && assetsRes.value.data != null) {
      totalAssets.value = assetsRes.value.data
    }

    // 商户占比
    if (merchantRes.status === 'fulfilled' && merchantRes.value.data) {
      merchantSharesAll.value = merchantRes.value.data.merchantShares || []
      merchantSharesExcludingFixed.value = merchantRes.value.data.merchantSharesExcludingFixed || []
      await nextTick()
      renderMerchantPieChart()
    }
  } catch (e) {
    message.error('加载首页数据失败')
  }
})

onBeforeUnmount(() => {
  pieChart.value?.dispose()
  merchantPieChart.value?.dispose()
})

function renderPieChart() {
  if (!pieChartRef.value || !pieChartData.value.length) return
  if (pieChart.value && pieChart.value.getDom() !== pieChartRef.value) {
    pieChart.value.dispose()
    pieChart.value = null
  }
  if (!pieChart.value) {
    pieChart.value = echarts.init(pieChartRef.value)
  }
  pieChart.value.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    series: [{
      type: 'pie',
      radius: ['46%', '72%'],
      center: ['50%', '50%'],
      label: { show: false },
      labelLine: { show: false },
      data: pieChartData.value.map(c => ({
        name: c.categoryName,
        value: Number(c.amount),
        itemStyle: { color: c.categoryColor || '#3498db' }
      }))
    }]
  }, true)
  pieChart.value.resize()
}

function renderMerchantPieChart() {
  if (!merchantPieRef.value || !merchantPieData.value.length) return
  if (merchantPieChart.value && merchantPieChart.value.getDom() !== merchantPieRef.value) {
    merchantPieChart.value.dispose()
    merchantPieChart.value = null
  }
  if (!merchantPieChart.value) {
    merchantPieChart.value = echarts.init(merchantPieRef.value)
  }
  merchantPieChart.value.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    series: [{
      type: 'pie',
      radius: ['46%', '72%'],
      center: ['50%', '50%'],
      label: { show: false },
      labelLine: { show: false },
      data: merchantPieData.value.map(m => ({
        name: m.merchantName,
        value: Number(m.amount),
        itemStyle: { color: m.merchantColor || '#3498db' }
      }))
    }]
  }, true)
  merchantPieChart.value.resize()
}

</script>

<style scoped>
.mobile-dashboard {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 总资产卡片 */
.asset-card {
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #0b1220 0%, #1d4ed8 62%, #0ea5a4 100%);
  color: #fff;
  border-radius: 18px;
  padding: 22px 18px;
  box-shadow: 0 20px 40px -22px rgba(29, 78, 216, 0.7);
}
.asset-card::after {
  content: '';
  position: absolute;
  width: 180px;
  height: 180px;
  right: -70px;
  bottom: -100px;
  border-radius: 50%;
  background: rgba(167, 243, 208, 0.16);
}
.asset-mark {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 46px;
  height: 46px;
  border-radius: 13px;
  opacity: 0.9;
  box-shadow: 0 12px 26px -14px rgba(0, 0, 0, 0.8);
}
.asset-label { font-size: 12px; opacity: 0.66; letter-spacing: 0.08em; }
.asset-value { font-size: 34px; font-weight: 750; margin: 8px 0; letter-spacing: -0.02em; }
.asset-sub { font-size: 12px; opacity: 0.6; }

/* 概况 2x2 网格 */
.summary-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.summary-item {
  background: #fff;
  border-radius: 14px;
  padding: 14px;
  box-shadow: var(--bp-shadow);
}

.summary-label { font-size: 12px; color: var(--bp-muted); margin-bottom: 4px; }
.summary-value { font-size: 19px; font-weight: 750; letter-spacing: -0.01em; }
.summary-value.green { color: #10b981; }
.summary-value.red { color: #ef4444; }
.summary-value.blue { color: #2563eb; }

/* 图表卡片 */
.chart-card, .section-card {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  box-shadow: var(--bp-shadow);
}

.chart-title, .section-title {
  font-size: 15px;
  font-weight: 650;
  margin: 0 0 10px;
  color: var(--bp-ink);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.chart-header .chart-title { margin-bottom: 0; }

/* 饼图下方自绘图例：两列，避免画布标签互相重叠 */
.pie-legend {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 14px;
  margin-top: 4px;
  padding-top: 10px;
  border-top: 1px solid var(--bp-line);
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
}
.legend-dot {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  border-radius: 3px;
}
.legend-name {
  flex: 1;
  min-width: 0;
  font-size: 12px;
  color: #475569;
}
.legend-pct {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  font-variant-numeric: tabular-nums;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

/* 预算进度 */
.budget-group-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 650;
  color: var(--bp-ink);
  margin-bottom: 6px;
}
.budget-group-title.spaced { margin-top: 20px; padding-top: 16px; border-top: 1px solid var(--bp-line); }
.budget-overall {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--bp-muted);
  margin-bottom: 4px;
}
.budget-item { margin-bottom: 14px; }
.budget-header { display: flex; justify-content: space-between; align-items: center; font-size: 13px; }
.budget-pct { flex-shrink: 0; font-size: 11px; padding: 2px 7px; border-radius: 999px; color: #fff; font-variant-numeric: tabular-nums; }
.budget-empty {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f8fafc;
  font-size: 13px;
  color: var(--bp-muted);
}
.budget-pct.exceeded { background: #ef4444; }
.budget-pct.warning { background: #f59e0b; }
.budget-pct.caution { background: #f97316; }
.budget-pct.normal { background: #10b981; }
.budget-amounts { display: flex; justify-content: space-between; font-size: 11px; color: #999; margin-top: 4px; }

/* 交易列表 */
.tx-list { }
.tx-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}
.tx-item:last-child { border-bottom: none; }

.tx-category { font-size: 14px; font-weight: 500; }
.tx-meta { font-size: 11px; color: #999; margin-top: 2px; }
.tx-amount { font-size: 15px; font-weight: 600; }
.tx-amount.expense { color: #e74c3c; }
.tx-amount.income { color: #27ae60; }
</style>
