<template>
  <div class="mobile-reports">
    <!-- 顶部控制栏 -->
    <div class="control-bar">
      <n-select v-model:value="selectedMonth" :options="monthOptions" size="small" style="flex: 1" />
      <n-select v-model:value="trendMonths" :options="[{label:'近6个月',value:6},{label:'近12个月',value:12}]" size="small" style="flex: 1; margin-left: 8px" />
      <n-button type="primary" size="small" @click="loadAll" style="margin-left: 8px">查询</n-button>
    </div>

    <!-- 月度总览 -->
    <div class="summary-grid">
      <div v-for="card in summaryCards" :key="card.label" class="summary-item">
        <div class="summary-label">{{ card.label }}</div>
        <div class="summary-value" :class="card.type">
          {{ card.value }}
          <n-tag :type="card.type" size="tiny" style="margin-left: 6px">{{ card.tag }}</n-tag>
        </div>
      </div>
    </div>

    <!-- 趋势图 -->
    <div class="chart-card">
      <h3 class="chart-title">收支趋势</h3>
      <div ref="trendChartRef" style="width: 100%; height: 250px"></div>
    </div>

    <!-- 分类占比 -->
    <div class="chart-card">
      <h3 class="chart-title">分类占比</h3>
      <div v-if="hasPieData" ref="pieChartRef" style="width: 100%; height: 250px"></div>
      <n-empty v-else description="暂无数据" />
    </div>

    <!-- 商户消费 -->
    <div class="chart-card">
      <h3 class="chart-title">商户消费</h3>
      <div v-if="hasMerchantData" ref="merchantChartRef" style="width: 100%; height: 250px"></div>
      <n-empty v-else description="暂无数据" />
    </div>

    <!-- 币种分布 -->
    <div class="chart-card">
      <h3 class="chart-title">币种分布</h3>
      <div v-if="hasCurrencyData" ref="currencyChartRef" style="width: 100%; height: 250px"></div>
      <n-empty v-else description="暂无数据" />
    </div>

    <!-- 月度对比 -->
    <div v-if="compareData" class="chart-card">
      <h3 class="chart-title">月度对比</h3>
      <div class="compare-row">
        <div class="compare-month">{{ compareData.currentMonth }}</div>
        <div class="compare-item">
          <span>收入</span>
          <span class="green">¥{{ formatNum(compareData.currentIncome) }}</span>
        </div>
        <div class="compare-item">
          <span>支出</span>
          <span class="red">¥{{ formatNum(compareData.currentExpense) }}</span>
        </div>
      </div>
      <div class="compare-divider"></div>
      <div class="compare-row">
        <div class="compare-month">{{ compareData.compareMonth }}</div>
        <div class="compare-item">
          <span>收入</span>
          <span class="green">¥{{ formatNum(compareData.compareIncome) }}</span>
        </div>
        <div class="compare-item">
          <span>支出</span>
          <span class="red">¥{{ formatNum(compareData.compareExpense) }}</span>
        </div>
      </div>
      <div v-if="compareData.expenseChangePct != null" class="compare-change">
        支出变化: {{ compareData.expenseChangePct > 0 ? '+' : '' }}{{ compareData.expenseChangePct }}%
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { useMessage } from 'naive-ui'
import { reportApi } from '@/api'
import * as echarts from 'echarts'
import dayjs from 'dayjs'

const message = useMessage()
const selectedMonth = ref(dayjs().format('YYYY-MM'))
const trendMonths = ref(12)
const summaryCards = ref([])
const compareData = ref(null)
const hasPieData = ref(false)
const hasMerchantData = ref(false)
const hasCurrencyData = ref(false)

watch([selectedMonth, trendMonths], loadAll)

const trendChartRef = ref(null)
const pieChartRef = ref(null)
const merchantChartRef = ref(null)
const currencyChartRef = ref(null)

let trendChart, pieChart, merchantChart, currencyChart

const monthOptions = ref([])
for (let i = -12; i <= 0; i++) {
  const m = dayjs().add(i, 'month')
  monthOptions.value.push({ label: m.format('YYYY-MM'), value: m.format('YYYY-MM') })
}

async function loadAll() {
  try {
    const [summary, trend, currencyDist, merchantDist] = await Promise.allSettled([
      reportApi.monthlySummary(selectedMonth.value),
      reportApi.trend(trendMonths.value),
      reportApi.currencyDistribution(selectedMonth.value),
      reportApi.merchantDistribution(selectedMonth.value)
    ])

    // 月度总览
    if (summary.status === 'fulfilled' && summary.value.data?.monthlySummary) {
      const s = summary.value.data.monthlySummary
      summaryCards.value = [
        { label: '总收入', value: `¥${formatNum(s.totalIncome)}`, tag: '收入', type: 'success' },
        { label: '总支出', value: `¥${formatNum(s.totalExpense)}`, tag: '支出', type: 'error' },
        { label: '结余', value: `¥${formatNum(s.balance)}`, tag: s.balance >= 0 ? '盈余' : '亏损', type: s.balance >= 0 ? 'success' : 'error' },
        { label: '日均支出', value: `¥${formatNum(s.avgDailyExpense)}`, tag: '日均', type: 'info' }
      ]

      // 饼图
      if (pieChartRef.value && s.categoryShares?.length) {
        hasPieData.value = true
        if (pieChart) pieChart.dispose()
        await new Promise(r => setTimeout(r, 50))
        pieChart = echarts.init(pieChartRef.value)
        pieChart.setOption({
          tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
          series: [{
            type: 'pie', radius: ['35%', '65%'],
            data: s.categoryShares.map(c => ({
              name: c.categoryName, value: Number(c.amount),
              itemStyle: { color: c.categoryColor || '#3498db' }
            }))
          }]
        })
      }
    }

    // 趋势图
    if (trend.status === 'fulfilled' && trend.value.data?.trend?.length) {
      if (trendChart) trendChart.dispose()
      await new Promise(r => setTimeout(r, 50))
      trendChart = echarts.init(trendChartRef.value)
      const t = trend.value.data.trend
      trendChart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['收入', '支出'], top: 0 },
        grid: { top: 30, bottom: 20, left: 10, right: 10 },
        xAxis: { type: 'category', data: t.map(i => i.month), axisLabel: { fontSize: 10 } },
        yAxis: { type: 'value', axisLabel: { fontSize: 10 } },
        series: [
          { name: '收入', type: 'line', data: t.map(i => Number(i.income)), smooth: true, itemStyle: { color: '#27ae60' } },
          { name: '支出', type: 'line', data: t.map(i => Number(i.expense)), smooth: true, itemStyle: { color: '#e74c3c' } }
        ]
      })
    }

    // 币种分布
    if (currencyDist.status === 'fulfilled' && currencyDist.value.data?.currencyDistribution?.length) {
      hasCurrencyData.value = true
      if (currencyChart) currencyChart.dispose()
      await new Promise(r => setTimeout(r, 50))
      currencyChart = echarts.init(currencyChartRef.value)
      currencyChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
        series: [{
          type: 'pie', radius: ['35%', '65%'],
          data: currencyDist.value.data.currencyDistribution.map(c => ({
            name: c.currency, value: Number(c.totalBase),
            itemStyle: { color: c.currency === 'CNY' ? '#e74c3c' : c.currency === 'USD' ? '#3498db' : '#2ecc71' }
          }))
        }]
      })
    }

    // 商户分布
    if (merchantDist.status === 'fulfilled' && merchantDist.value.data?.merchantShares?.length) {
      hasMerchantData.value = true
      if (merchantChart) merchantChart.dispose()
      await new Promise(r => setTimeout(r, 50))
      merchantChart = echarts.init(merchantChartRef.value)
      merchantChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
        series: [{
          type: 'pie', radius: ['35%', '65%'],
          data: merchantDist.value.data.merchantShares.map(m => ({
            name: m.merchantName, value: Number(m.amount),
            itemStyle: { color: m.merchantColor || '#3498db' }
          }))
        }]
      })
    }

    // 月度对比
    const prevMonth = dayjs(selectedMonth.value).subtract(1, 'month').format('YYYY-MM')
    try {
      const cmp = await reportApi.compare(selectedMonth.value, prevMonth)
      compareData.value = cmp.data.compare
    } catch (e) {}
  } catch (e) {
    message.error('加载报表失败')
  }
}

function formatNum(n) {
  return n == null ? '0.00' : Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

function handleResize() {
  trendChart?.resize()
  pieChart?.resize()
  merchantChart?.resize()
  currencyChart?.resize()
}

onMounted(() => {
  loadAll()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  pieChart?.dispose()
  merchantChart?.dispose()
  currencyChart?.dispose()
})
</script>

<style scoped>
.mobile-reports {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.control-bar {
  display: flex;
  gap: 8px;
  background: #fff;
  border-radius: 10px;
  padding: 10px 12px;
}

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
.summary-value { font-size: 16px; font-weight: 700; display: flex; align-items: center; }
.summary-value.success { color: #27ae60; }
.summary-value.error { color: #e74c3c; }
.summary-value.info { color: #3498db; }

.chart-card {
  background: #fff;
  border-radius: 10px;
  padding: 14px;
}

.chart-title {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 10px;
  color: #333;
}

.compare-row { padding: 4px 0; }
.compare-month { font-size: 14px; font-weight: 600; margin-bottom: 6px; color: #2c3e50; }
.compare-item { display: flex; justify-content: space-between; font-size: 13px; color: #555; padding: 2px 0; }
.compare-divider { height: 1px; background: #eee; margin: 8px 0; }
.compare-change { text-align: center; font-size: 13px; font-weight: 600; margin-top: 8px; }
.green { color: #27ae60; }
.red { color: #e74c3c; }
</style>
