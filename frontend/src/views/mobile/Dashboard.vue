<template>
  <div class="mobile-dashboard">
    <!-- 总资产卡片 -->
    <div class="asset-card">
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
      <h3 class="chart-title">分类占比</h3>
      <div ref="pieChartRef" style="width: 100%; height: 260px"></div>
    </div>

    <!-- 饼图：商户消费 -->
    <div v-if="hasMerchantData" class="chart-card">
      <h3 class="chart-title">商户消费</h3>
      <div ref="merchantPieRef" style="width: 100%; height: 260px"></div>
    </div>

    <!-- 预算进度 -->
    <div v-if="budgetProgress?.length" class="section-card">
      <h3 class="section-title">预算进度</h3>
      <div v-for="item in budgetProgress" :key="item.categoryId" class="budget-item">
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
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { reportApi, budgetApi, transactionApi, accountApi } from '@/api'
import * as echarts from 'echarts'
import dayjs from 'dayjs'

const router = useRouter()
const message = useMessage()
const totalAssets = ref(0)
const summaryCards = ref([])
const budgetProgress = ref([])
const recentTransactions = ref([])
const hasCategoryData = ref(false)
const hasMerchantData = ref(false)
const pieChartRef = ref(null)
const merchantPieRef = ref(null)
const pieChart = ref(null)
const merchantPieChart = ref(null)

onMounted(async () => {
  const month = dayjs().format('YYYY-MM')

  try {
    const [summaryRes, budgetRes, txRes, assetsRes, merchantRes] = await Promise.allSettled([
      reportApi.monthlySummary(month),
      budgetApi.progress(month),
      transactionApi.list({ page: 1, size: 10, sort: 'transaction_date,desc' }),
      accountApi.totalAssets(),
      reportApi.merchantDistribution(month)
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
      if (pieChartRef.value && s.categoryShares?.length) {
        hasCategoryData.value = true
        await new Promise(r => setTimeout(r, 100))
        pieChart.value = echarts.init(pieChartRef.value)
        pieChart.value.setOption({
          tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
          series: [{
            type: 'pie',
            radius: ['40%', '70%'],
            data: s.categoryShares.map(c => ({
              name: c.categoryName,
              value: Number(c.amount),
              itemStyle: { color: c.categoryColor || '#3498db' }
            }))
          }]
        })
      }
    }

    // 预算进度
    if (budgetRes.status === 'fulfilled' && budgetRes.value.data) {
      budgetProgress.value = Array.isArray(budgetRes.value.data.items) ? budgetRes.value.data.items : []
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
    if (merchantRes.status === 'fulfilled' && merchantRes.value.data?.merchantShares?.length) {
      hasMerchantData.value = true
      await new Promise(r => setTimeout(r, 100))
      if (merchantPieRef.value) {
        merchantPieChart.value = echarts.init(merchantPieRef.value)
        merchantPieChart.value.setOption({
          tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
          series: [{
            type: 'pie',
            radius: ['40%', '70%'],
            data: merchantRes.value.data.merchantShares.map(m => ({
              name: m.merchantName,
              value: Number(m.amount),
              itemStyle: { color: m.merchantColor || '#3498db' }
            }))
          }]
        })
      }
    }
  } catch (e) {
    message.error('加载首页数据失败')
  }
})

onBeforeUnmount(() => {
  pieChart.value?.dispose()
  merchantPieChart.value?.dispose()
})

function getProgressStatus(status) {
  switch (status) {
    case 'EXCEEDED': return 'error'
    case 'WARNING': return 'warning'
    case 'CAUTION': return 'warning'
    default: return 'success'
  }
}

function formatNum(n) {
  if (n == null) return '0.00'
  return Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
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
  background: linear-gradient(135deg, #2c3e50, #34495e);
  color: #fff;
  border-radius: 12px;
  padding: 20px 16px;
  text-align: center;
}

.asset-label { font-size: 13px; opacity: 0.8; }
.asset-value { font-size: 32px; font-weight: 700; margin: 8px 0; }
.asset-sub { font-size: 12px; opacity: 0.6; }

/* 概况 2x2 网格 */
.summary-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.summary-item {
  background: #fff;
  border-radius: 10px;
  padding: 12px;
}

.summary-label { font-size: 12px; color: #888; margin-bottom: 4px; }
.summary-value { font-size: 18px; font-weight: 700; }
.summary-value.green { color: #27ae60; }
.summary-value.red { color: #e74c3c; }
.summary-value.blue { color: #3498db; }

/* 图表卡片 */
.chart-card, .section-card {
  background: #fff;
  border-radius: 10px;
  padding: 14px;
}

.chart-title, .section-title {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 10px;
  color: #333;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

/* 预算进度 */
.budget-item { margin-bottom: 14px; }
.budget-header { display: flex; justify-content: space-between; align-items: center; font-size: 13px; }
.budget-pct { font-size: 11px; padding: 2px 6px; border-radius: 8px; color: #fff; }
.budget-pct.exceeded { background: #e74c3c; }
.budget-pct.warning { background: #f39c12; }
.budget-pct.caution { background: #e67e22; }
.budget-pct.normal { background: #27ae60; }
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
