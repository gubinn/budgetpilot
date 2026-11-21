<template>
  <div class="dashboard">
    <!-- 总资产卡片 -->
    <n-card class="hero-card" :bordered="false" style="margin-bottom: 18px">
      <div class="hero-inner">
        <div>
          <div class="hero-eyebrow">TOTAL BALANCE</div>
          <div class="assets-value">¥ {{ formatNum(totalAssets) }}</div>
          <div class="assets-sub">所有 CNY 账户余额合计</div>
        </div>
        <div class="hero-mark">
          <img src="/logo-mark.svg" alt="" />
        </div>
      </div>
    </n-card>

    <n-grid :cols="4" :x-gap="16" :y-gap="16">
      <n-gi v-for="card in summaryCards" :key="card.label">
        <n-card class="stat-card-box" :title="card.label" hoverable>
          <div class="stat-card">
            <div class="stat-value" :class="card.color">{{ card.value }}</div>
            <div class="stat-sub">{{ card.sub }}</div>
          </div>
        </n-card>
      </n-gi>
    </n-grid>

    <n-grid :cols="3" :x-gap="16" :y-gap="16" style="margin-top: 16px">
      <n-gi>
        <n-card title="预算进度" hoverable>
          <div v-if="budgetProgress" class="budget-list">
            <div v-for="item in budgetProgress" :key="item.categoryId" class="budget-item">
              <div class="budget-header">
                <span>{{ item.categoryName }}</span>
                <span :class="['badge', item.status.toLowerCase()]">{{ item.progressPct }}%</span>
              </div>
              <n-progress
                type="line"
                :percentage="Math.min(item.progressPct, 100)"
                :status="getProgressStatus(item.status)"
                :show-indicator="false"
                :height="8"
                style="margin-top: 8px"
              />
              <div class="budget-amounts">
                <span class="spent">已花 ¥{{ formatNum(item.spent) }}</span>
                <span class="budget">预算 ¥{{ formatNum(item.budget) }}</span>
              </div>
            </div>
          </div>
          <n-empty v-else description="本月未设置预算" />
        </n-card>
      </n-gi>
      <n-gi>
        <n-card title="分类占比" hoverable>
          <div ref="pieChartRef" style="width: 100%; height: 300px"></div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card title="商户消费" hoverable>
          <div ref="merchantPieRef" style="width: 100%; height: 300px"></div>
        </n-card>
      </n-gi>
    </n-grid>

    <n-card title="最近交易" hoverable style="margin-top: 16px">
      <n-data-table
        :columns="columns"
        :data="recentTransactions"
        :pagination="false"
        :loading="loading"
        size="small"
      />
      <div style="margin-top: 12px; text-align: right">
        <n-button text type="primary" @click="router.push('/transactions')">查看全部 →</n-button>
      </div>
    </n-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, h } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { reportApi, budgetApi, transactionApi, accountApi } from '@/api'
import * as echarts from 'echarts'
import dayjs from 'dayjs'

const router = useRouter()
const message = useMessage()
const loading = ref(true)
const totalAssets = ref(0)
const summaryCards = ref([])
const budgetProgress = ref([])
const recentTransactions = ref([])
const pieChartRef = ref(null)
const merchantPieRef = ref(null)
const pieChart = ref(null)
const merchantPieChart = ref(null)

const columns = [
  { title: '日期', key: 'date', width: 110 },
  { title: '账户', key: 'account', width: 100 },
  {
    title: '分类', key: 'category', width: 120,
    render: (row) => h('span', {
      style: { color: row.categoryColor, fontSize: '13px' }
    }, row.category)
  },
  { title: '备注', key: 'note', ellipsis: { tooltip: true } },
  {
    title: '金额', key: 'amount', width: 120, align: 'right',
    render: (row) => h('span', {
      style: { color: row.type === 1 ? '#e74c3c' : row.type === 2 ? '#27ae60' : '#333' }
    }, row.type === 1 ? `-¥${row.amount}` : `+¥${row.amount}`)
  }
]

onMounted(async () => {
  const now = dayjs()
  const month = now.format('YYYY-MM')

  try {
    const [summaryRes, budgetRes, txRes, assetsRes, merchantRes] = await Promise.allSettled([
      reportApi.monthlySummary(month),
      budgetApi.progress(month),
      transactionApi.list({ page: 1, size: 10, sort: 'transaction_date,desc' }),
      accountApi.totalAssets(),
      reportApi.merchantDistribution(month)
    ])

    if (summaryRes.status === 'fulfilled' && summaryRes.value.data?.monthlySummary) {
      const s = summaryRes.value.data.monthlySummary
      summaryCards.value = [
        { label: '本月收入', value: `¥${formatNum(s.totalIncome)}`, sub: '收入合计', color: 'green' },
        { label: '本月支出', value: `¥${formatNum(s.totalExpense)}`, sub: '支出合计', color: 'red' },
        { label: '本月结余', value: `¥${formatNum(s.balance)}`, sub: s.balance >= 0 ? '收入 > 支出' : '支出 > 收入', color: s.balance >= 0 ? 'green' : 'red' },
        { label: '日均支出', value: `¥${formatNum(s.avgDailyExpense)}`, sub: '日均消费', color: 'blue' }
      ]

      // 饼图
      if (pieChartRef.value && s.categoryShares?.length) {
        pieChart.value = echarts.init(pieChartRef.value)
        pieChart.value.setOption({
          tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
          series: [{
            type: 'pie',
            radius: ['40%', '70%'],
            avoidLabelOverlap: false,
            itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
            data: s.categoryShares.map(c => ({
              name: c.categoryName,
              value: Number(c.amount),
              itemStyle: { color: c.categoryColor || '#3498db' }
            }))
          }]
        })
      }
    }

    if (budgetRes.status === 'fulfilled' && budgetRes.value.data) {
      budgetProgress.value = Array.isArray(budgetRes.value.data.items) ? budgetRes.value.data.items : []
    }

    if (txRes.status === 'fulfilled' && txRes.value.data?.items) {
      recentTransactions.value = txRes.value.data.items.map(t => ({
        date: t.transactionDate,
        account: t.accountName,
        category: t.categoryName,
        categoryColor: t.categoryColor,
        note: t.note || '-',
        type: t.type,
        amount: formatNum(t.amount)
      }))
    }

    if (assetsRes.status === 'fulfilled' && assetsRes.value.data != null) {
      totalAssets.value = assetsRes.value.data
    }

    // 商户饼图
    if (merchantRes.status === 'fulfilled' && merchantRes.value.data?.merchantShares?.length) {
      if (merchantPieRef.value) {
        merchantPieChart.value = echarts.init(merchantPieRef.value)
        merchantPieChart.value.setOption({
          tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
          legend: { bottom: 0, type: 'scroll' },
          series: [{
            type: 'pie',
            radius: ['40%', '70%'],
            avoidLabelOverlap: false,
            itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
            label: { show: true, formatter: '{b}\n{d}%' },
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
  } finally {
    loading.value = false
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
.hero-card {
  position: relative;
  overflow: hidden;
  color: #fff;
  background: linear-gradient(135deg, #0b1220 0%, #1d4ed8 62%, #0ea5a4 100%) !important;
  border: none !important;
  box-shadow: 0 22px 50px -22px rgba(29, 78, 216, 0.65);
}
.hero-card::before,
.hero-card::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}
.hero-card::before {
  width: 260px;
  height: 260px;
  right: -60px;
  top: -120px;
  background: rgba(255, 255, 255, 0.12);
}
.hero-card::after {
  width: 180px;
  height: 180px;
  right: 90px;
  bottom: -110px;
  background: rgba(167, 243, 208, 0.16);
}
.hero-inner {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}
.hero-eyebrow {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.22em;
  color: rgba(255, 255, 255, 0.55);
}
.assets-value {
  margin-top: 10px;
  font-size: 38px;
  font-weight: 750;
  letter-spacing: -0.02em;
  line-height: 1.1;
}
.assets-sub {
  margin-top: 8px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.66);
}
.hero-mark img {
  width: 72px;
  height: 72px;
  border-radius: 18px;
  opacity: 0.92;
  box-shadow: 0 18px 40px -18px rgba(0, 0, 0, 0.65);
}
.stat-card-box {
  height: 100%;
}
.stat-value {
  font-size: 27px;
  font-weight: 750;
  letter-spacing: -0.02em;
}
.stat-sub {
  margin-top: 5px;
  font-size: 12px;
  color: var(--bp-muted);
}
.green { color: #10b981; }
.red { color: #ef4444; }
.blue { color: #2563eb; }

.budget-list { max-height: 300px; overflow-y: auto; }
.budget-item { margin-bottom: 18px; }
.budget-header { display: flex; justify-content: space-between; align-items: center; font-size: 13px; }
.badge { font-size: 11px; padding: 2px 8px; border-radius: 999px; color: #fff; font-weight: 600; }
.badge.exceeded { background: #ef4444; }
.badge.warning { background: #f59e0b; }
.badge.caution { background: #f97316; }
.badge.normal { background: #10b981; }
.budget-amounts { display: flex; justify-content: space-between; font-size: 11px; color: var(--bp-muted); margin-top: 6px; }

@media (max-width: 640px) {
  .assets-value { font-size: 30px; }
  .hero-mark img { width: 54px; height: 54px; border-radius: 14px; }
}
</style>
