<template>
  <div class="reports">
    <!-- 顶部控制栏 -->
    <n-card style="margin-bottom: 16px">
      <n-space align="center" wrap>
        <n-select v-model:value="selectedMonth" :options="monthOptions" style="width: 160px" />
        <n-button type="primary" @click="loadAll">查询</n-button>
        <n-select v-model:value="trendMonths" :options="[{label:'近6个月',value:6},{label:'近12个月',value:12}]" style="width: 130px" />
      </n-space>
    </n-card>

    <!-- 月度总览 -->
    <n-grid :cols="4" :x-gap="16" :y-gap="16">
      <n-gi v-for="card in summaryCards" :key="card.label">
        <n-card hoverable>
          <n-statistic :label="card.label" :value="card.value">
            <template #suffix>
              <n-tag :type="card.type" size="small">{{ card.tag }}</n-tag>
            </template>
          </n-statistic>
        </n-card>
      </n-gi>
    </n-grid>

    <!-- 趋势图 -->
    <n-card title="收支趋势" hoverable style="margin-top: 16px">
      <div ref="trendChartRef" style="width: 100%; height: 320px"></div>
    </n-card>

    <n-grid :cols="2" :x-gap="16" :y-gap="16" style="margin-top: 16px">
      <n-gi>
        <n-card title="分类占比" hoverable>
          <div ref="pieChartRef" style="width: 100%; height: 300px"></div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card title="币种分布" hoverable>
          <div ref="currencyChartRef" style="width: 100%; height: 300px"></div>
        </n-card>
      </n-gi>
    </n-grid>

    <!-- 月度对比 -->
    <n-card title="月度对比" hoverable style="margin-top: 16px">
      <div v-if="compareData" class="compare-grid">
        <n-grid :cols="2" :x-gap="24">
          <n-gi>
            <div class="compare-card">
              <h3>{{ compareData.currentMonth }}</h3>
              <p>收入: <span class="green">¥{{ formatNum(compareData.currentIncome) }}</span></p>
              <p>支出: <span class="red">¥{{ formatNum(compareData.currentExpense) }}</span></p>
            </div>
          </n-gi>
          <n-gi>
            <div class="compare-card">
              <h3>{{ compareData.compareMonth }}</h3>
              <p>收入: <span class="green">¥{{ formatNum(compareData.compareIncome) }}</span></p>
              <p>支出: <span class="red">¥{{ formatNum(compareData.compareExpense) }}</span></p>
              <p v-if="compareData.incomeChangePct != null">收入变化: {{ compareData.incomeChangePct > 0 ? '+' : '' }}{{ compareData.incomeChangePct }}%</p>
              <p v-if="compareData.expenseChangePct != null">支出变化: {{ compareData.expenseChangePct > 0 ? '+' : '' }}{{ compareData.expenseChangePct }}%</p>
            </div>
          </n-gi>
        </n-grid>
      </div>
    </n-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useMessage } from 'naive-ui'
import { reportApi } from '@/api'
import * as echarts from 'echarts'
import dayjs from 'dayjs'

const message = useMessage()
const selectedMonth = ref(dayjs().format('YYYY-MM'))
const trendMonths = ref(12)
const summaryCards = ref([])
const compareData = ref(null)

const trendChartRef = ref(null)
const pieChartRef = ref(null)
const currencyChartRef = ref(null)

let trendChart, pieChart, currencyChart

const monthOptions = ref([])
for (let i = -12; i <= 0; i++) {
  const m = dayjs().add(i, 'month')
  monthOptions.value.push({ label: m.format('YYYY年MM月'), value: m.format('YYYY-MM') })
}

async function loadAll() {
  try {
    const [summary, trend, currencyDist] = await Promise.allSettled([
      reportApi.monthlySummary(selectedMonth.value),
      reportApi.trend(trendMonths.value),
      reportApi.currencyDistribution(selectedMonth.value)
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
        if (pieChart) pieChart.dispose()
        pieChart = echarts.init(pieChartRef.value)
        pieChart.setOption({
          tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
          legend: { bottom: 0, type: 'scroll' },
          series: [{
            type: 'pie', radius: ['35%', '65%'], center: ['50%', '45%'],
            itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
            label: { show: true, formatter: '{b}\n{d}%' },
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
      if (!trendChartRef.value) return
      if (trendChart) trendChart.dispose()
      trendChart = echarts.init(trendChartRef.value)
      const t = trend.value.data.trend
      trendChart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['收入', '支出', '结余'] },
        xAxis: { type: 'category', data: t.map(i => i.month) },
        yAxis: { type: 'value' },
        series: [
          { name: '收入', type: 'line', data: t.map(i => Number(i.income)), smooth: true, itemStyle: { color: '#27ae60' } },
          { name: '支出', type: 'line', data: t.map(i => Number(i.expense)), smooth: true, itemStyle: { color: '#e74c3c' } },
          { name: '结余', type: 'bar', data: t.map(i => Number(i.balance)), itemStyle: { color: '#3498db' } }
        ]
      })
    }

    // 币种分布
    if (currencyDist.status === 'fulfilled' && currencyDist.value.data?.currencyDistribution?.length) {
      if (currencyChart) currencyChart.dispose()
      currencyChart = echarts.init(currencyChartRef.value)
      currencyChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
        series: [{
          type: 'pie', radius: ['35%', '65%'],
          label: { formatter: '{b}\n{d}%' },
          data: currencyDist.value.data.currencyDistribution.map(c => ({
            name: c.currency, value: Number(c.totalBase),
            itemStyle: { color: c.currency === 'CNY' ? '#e74c3c' : c.currency === 'USD' ? '#3498db' : '#2ecc71' }
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

onMounted(() => {
  loadAll()
  window.addEventListener('resize', () => {
    trendChart?.resize()
    pieChart?.resize()
    currencyChart?.resize()
  })
})
</script>

<style scoped>
.green { color: #27ae60; font-weight: 600; }
.red { color: #e74c3c; font-weight: 600; }
.compare-card { background: #f8f9fa; padding: 20px; border-radius: 8px; }
.compare-card h3 { margin-bottom: 12px; font-size: 18px; }
.compare-card p { margin: 6px 0; font-size: 14px; color: #555; }
</style>
