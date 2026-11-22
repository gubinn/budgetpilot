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

    <n-grid :cols="3" :x-gap="16" :y-gap="16" style="margin-top: 16px">
      <n-gi>
        <n-card title="分类占比" hoverable>
          <template #header-extra>
            <n-radio-group v-model:value="categoryLevel" size="small">
              <n-radio-button :value="1">一级</n-radio-button>
              <n-radio-button :value="2">二级</n-radio-button>
            </n-radio-group>
          </template>
          <div ref="pieChartRef" style="width: 100%; height: 300px"></div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card title="商户消费" hoverable>
          <div ref="merchantChartRef" style="width: 100%; height: 300px"></div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card title="币种分布" hoverable>
          <div ref="currencyChartRef" style="width: 100%; height: 300px"></div>
        </n-card>
      </n-gi>
    </n-grid>

    <n-card title="标签统计" hoverable style="margin-top: 16px">
      <div class="tag-chart-wrap">
        <div ref="tagChartRef" style="width: 100%; height: 300px"></div>
        <div v-if="!hasTagData" class="chart-empty">暂无标签数据</div>
      </div>
    </n-card>

    <n-card :title="extKey === 'channel' ? '支付渠道' : '扩展字段统计'" hoverable style="margin-top: 16px">
      <template #header-extra>
        <n-select
          v-model:value="extKey"
          :options="extKeyOptions"
          size="small"
          style="width: 180px"
          placeholder="选择扩展字段"
          :disabled="extKeys.length === 0"
        />
      </template>
      <div class="tag-chart-wrap">
        <div ref="extChartRef" style="width: 100%; height: 300px"></div>
        <div v-if="!hasExtData" class="chart-empty">暂无扩展字段数据</div>
      </div>
    </n-card>

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
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useMessage } from 'naive-ui'
import { reportApi, transactionApi } from '@/api'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import { formatNum } from '@/utils/display'

const message = useMessage()
const selectedMonth = ref(dayjs().format('YYYY-MM'))
const trendMonths = ref(12)
const categoryLevel = ref(1)
const summaryCards = ref([])
const compareData = ref(null)
const hasTagData = ref(false)
const extKeys = ref([])
const extKey = ref('channel')
const hasExtData = ref(false)
const extKeyOptions = computed(() => extKeys.value.map(k => ({
  label: k === 'channel' ? '支付渠道' : k,
  value: k
})))

watch([selectedMonth, trendMonths, categoryLevel, extKey], loadAll)

const trendChartRef = ref(null)
const pieChartRef = ref(null)
const merchantChartRef = ref(null)
const currencyChartRef = ref(null)
const tagChartRef = ref(null)
const extChartRef = ref(null)

let trendChart, pieChart, merchantChart, currencyChart, tagChart, extChart

const monthOptions = ref([])
for (let i = -12; i <= 0; i++) {
  const m = dayjs().add(i, 'month')
  monthOptions.value.push({ label: m.format('YYYY年MM月'), value: m.format('YYYY-MM') })
}

function resetCharts() {
  trendChart?.dispose(); trendChart = null
  pieChart?.dispose(); pieChart = null
  merchantChart?.dispose(); merchantChart = null
  currencyChart?.dispose(); currencyChart = null
  tagChart?.dispose(); tagChart = null
  hasTagData.value = false
  extChart?.dispose(); extChart = null
  hasExtData.value = false
}

async function loadAll() {
  // 先清空旧数据和图表，避免切到无数据月份时继续显示上个月
  resetCharts()
  summaryCards.value = []
  compareData.value = null
  try {
    const extPromise = extKey.value
      ? reportApi.extFieldDistribution(selectedMonth.value, extKey.value, 1)
      : Promise.resolve({ data: [] })
    const [summary, trend, currencyDist, merchantDist, extDist] = await Promise.allSettled([
      reportApi.monthlySummary(selectedMonth.value, categoryLevel.value),
      reportApi.trend(trendMonths.value),
      reportApi.currencyDistribution(selectedMonth.value),
      reportApi.merchantDistribution(selectedMonth.value),
      extPromise
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
              name: categoryLabel(c), value: Number(c.amount),
              itemStyle: { color: c.categoryColor || '#3498db' }
            }))
          }]
        })
      }

      // 标签统计
      const tagShares = s.tagShares || []
      hasTagData.value = tagShares.length > 0
      if (tagChart) { tagChart.dispose(); tagChart = null }
      if (tagShares.length && tagChartRef.value) {
        const top = tagShares.slice(0, 12).slice().reverse()
        tagChart = echarts.init(tagChartRef.value)
        tagChart.setOption({
          grid: { left: 100, right: 32, top: 8, bottom: 24 },
          tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'shadow' },
            formatter: p => `${p[0].name}: ¥${Number(p[0].value).toFixed(2)}`
          },
          xAxis: {
            type: 'value',
            axisLabel: { color: '#94a3b8' },
            splitLine: { lineStyle: { color: '#eef2f7' } }
          },
          yAxis: {
            type: 'category',
            data: top.map(t => t.tag),
            axisLabel: { color: '#64748b' },
            axisLine: { show: false },
            axisTick: { show: false }
          },
          series: [{
            type: 'bar',
            data: top.map(t => Number(t.amount)),
            barWidth: 14,
            itemStyle: {
              borderRadius: [0, 6, 6, 0],
              color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                { offset: 0, color: '#2563eb' },
                { offset: 1, color: '#0ea5a4' }
              ])
            }
          }]
        })
      }
    }

    // 扩展字段统计（默认支付渠道）
    const extItems = extDist.status === 'fulfilled' ? (extDist.value.data || []) : []
    hasExtData.value = extItems.length > 0
    if (extChart) { extChart.dispose(); extChart = null }
    if (extItems.length && extChartRef.value) {
      const top = extItems.slice(0, 10).slice().reverse()
      extChart = echarts.init(extChartRef.value)
      extChart.setOption({
        grid: { left: 96, right: 32, top: 8, bottom: 24 },
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' },
          formatter: p => `${p[0].name}: ¥${Number(p[0].value).toFixed(2)}`
        },
        xAxis: {
          type: 'value',
          axisLabel: { color: '#94a3b8' },
          splitLine: { lineStyle: { color: '#eef2f7' } }
        },
        yAxis: {
          type: 'category',
          data: top.map(i => i.value),
          axisLabel: { color: '#64748b' },
          axisLine: { show: false },
          axisTick: { show: false }
        },
        series: [{
          type: 'bar',
          data: top.map(i => Number(i.amount)),
          barWidth: 14,
          itemStyle: {
            borderRadius: [0, 6, 6, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#0ea5a4' },
              { offset: 1, color: '#2563eb' }
            ])
          }
        }]
      })
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

    // 商户消费分布
    if (merchantDist.status === 'fulfilled' && merchantDist.value.data?.merchantShares?.length) {
      if (merchantChart) merchantChart.dispose()
      merchantChart = echarts.init(merchantChartRef.value)
      merchantChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
        legend: { bottom: 0, type: 'scroll' },
        series: [{
          type: 'pie', radius: ['35%', '65%'], center: ['50%', '45%'],
          itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
          label: { show: true, formatter: '{b}\n{d}%' },
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

function categoryLabel(c) {
  return c.parentName ? `${c.parentName}/${c.categoryName}` : c.categoryName
}

function handleResize() {
  trendChart?.resize()
  pieChart?.resize()
  merchantChart?.resize()
  tagChart?.resize()
  extChart?.resize()
  currencyChart?.resize()
}

async function loadExtKeys() {
  try {
    const res = await transactionApi.extKeys()
    extKeys.value = res.data || []
    if (!extKeys.value.includes(extKey.value)) {
      extKey.value = extKeys.value.includes('channel') ? 'channel' : (extKeys.value[0] || '')
    }
  } catch (e) {
    extKeys.value = []
  }
}

onMounted(async () => {
  await loadExtKeys()
  loadAll()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  pieChart?.dispose()
  merchantChart?.dispose()
  currencyChart?.dispose()
  tagChart?.dispose()
  extChart?.dispose()
})
</script>

<style scoped>
.green { color: #27ae60; font-weight: 600; }
.red { color: #e74c3c; font-weight: 600; }
.compare-card { background: #f8f9fa; padding: 20px; border-radius: 8px; }
.compare-card h3 { margin-bottom: 12px; font-size: 18px; }
.compare-card p { margin: 6px 0; font-size: 14px; color: #555; }
.tag-chart-wrap { position: relative; min-height: 300px; }
.chart-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 13px;
}
</style>
