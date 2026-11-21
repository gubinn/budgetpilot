<template>
  <n-card title="预算管理">
    <template #header-extra>
      <n-space :size="8" :wrap="true">
        <n-radio-group v-model:value="budgetMode" size="small">
          <n-radio-button value="monthly">月度</n-radio-button>
          <n-radio-button value="annual">年度专项</n-radio-button>
        </n-radio-group>
        <n-select v-if="budgetMode === 'monthly'" v-model:value="selectedMonth" :options="monthOptions" style="min-width: 120px; max-width: 140px" />
        <n-select v-else v-model:value="selectedYear" :options="yearOptions" style="min-width: 100px; max-width: 120px" />
        <n-button type="primary" size="small" @click="openCreate">
          {{ budget ? '编辑' : '创建' }}
        </n-button>
        <n-button v-if="budgetMode === 'monthly'" size="small" @click="handleCopy" :disabled="!budget">复制</n-button>
      </n-space>
    </template>

    <!-- 总览卡片 -->
    <n-grid v-if="budget" :cols="2" :x-gap="12" :y-gap="12" style="margin-bottom: 16px" item-responsive responsive="screen">
      <n-gi span="2 m:1">
        <n-statistic label="总预算" :value="`¥${formatNum(budget.totalBudget)}`" />
      </n-gi>
      <n-gi span="2 m:1">
        <n-statistic label="已消费" :value="`¥${formatNum(budget.totalSpent)}`">
          <template #suffix>
            <n-tag :type="budget.progressPct > 100 ? 'error' : budget.progressPct > 80 ? 'warning' : 'success'" size="small">
              {{ budget.progressPct }}%
            </n-tag>
          </template>
        </n-statistic>
      </n-gi>
      <n-gi span="2 m:1">
        <n-statistic label="剩余" :value="`¥${formatNum(budget.remaining)}`" />
      </n-gi>
      <n-gi v-if="budgetMode === 'monthly'" span="2 m:1">
        <n-statistic label="日均剩余" :value="`¥${formatNum(budget.dailyAvgRemaining)}`" />
      </n-gi>
    </n-grid>

    <!-- 分类进度 -->
    <div v-if="budget?.items" class="budget-items">
      <div v-for="item in budget.items" :key="item.categoryId" class="budget-item-card">
        <div class="item-header">
          <div class="item-title">
            <span :style="{ background: item.categoryColor, width: '14px', height: '14px', borderRadius: '50%', display: 'inline-block' }"></span>
            <span style="margin-left: 8px; font-weight: 600">{{ item.categoryName }}</span>
          </div>
          <n-tag :type="getStatusType(item.status)" size="small" bordered>{{ item.status }}</n-tag>
        </div>
        <n-progress
          type="line"
          :percentage="Math.min(item.progressPct, 100)"
          :status="getProgressStatus(item.status)"
          :show-indicator="true"
          indicator-placement="inside"
          :height="12"
          style="margin: 12px 0"
        />
        <div class="item-footer">
          <span>¥{{ formatNum(item.spent) }}</span>
          <span>/ ¥{{ formatNum(item.budget) }}</span>
          <span :class="item.remaining < 0 ? 'over' : ''">余 ¥{{ formatNum(item.remaining) }}</span>
        </div>
      </div>
    </div>

    <n-empty v-if="!budget" :description="budgetMode === 'annual' ? '该年度未设置专项预算，点击「创建」开始' : '该月未设置预算，点击「创建」开始'" />

    <!-- 创建/编辑弹窗 -->
    <n-modal v-model:show="showCreate" preset="card" :title="budgetMode === 'annual' ? '设定年度专项预算' : '设定月度预算'" style="max-width: 90vw; width: 550px">
      <n-form ref="formRef" :model="createForm" label-placement="left" label-width="auto">
        <n-form-item label="总预算" required>
          <n-input-number v-model:value="createForm.totalAmount" :min="0" :precision="2" disabled style="width: 100%">
            <template #prefix>¥</template>
          </n-input-number>
        </n-form-item>
        <n-form-item label="分类明细">
          <div style="width: 100%">
            <n-data-table
              :columns="itemColumns"
              :data="createForm.items"
              :pagination="false"
              size="small"
            />
            <div class="items-total">
              明细合计：<strong>¥{{ formatNum(itemsTotal) }}</strong>
              <span class="items-tip">父分类和子分类只能选一层，否则会重复计算</span>
            </div>
          </div>
        </n-form-item>
        <n-form-item label="备注">
          <n-input v-model:value="createForm.note" type="textarea" :autosize="{ minRows: 2 }" />
        </n-form-item>
        <n-form-item>
          <n-space>
            <n-button type="primary" :loading="submitting" @click="handleSave">保存</n-button>
            <n-button @click="showCreate = false">取消</n-button>
          </n-space>
        </n-form-item>
      </n-form>
    </n-modal>
  </n-card>
</template>

<script setup>
import { ref, h, computed, onMounted, watch } from 'vue'
import { useMessage, NInputNumber } from 'naive-ui'
import { budgetApi, annualBudgetApi, categoryApi } from '@/api'
import dayjs from 'dayjs'

const message = useMessage()
const budgetMode = ref('monthly')
const selectedMonth = ref(dayjs().format('YYYY-MM'))
const selectedYear = ref(String(dayjs().year()))
const budget = ref(null)
const showCreate = ref(false)
const submitting = ref(false)
const expenseCategories = ref([])
const annualCategories = ref([])

const monthOptions = ref([])
for (let i = -6; i <= 6; i++) {
  const m = dayjs().add(i, 'month')
  monthOptions.value.push({ label: m.format('YYYY年MM月'), value: m.format('YYYY-MM') })
}

const yearOptions = ref([])
for (let i = -1; i <= 1; i++) {
  const y = dayjs().add(i, 'year').format('YYYY')
  yearOptions.value.push({ label: `${y}年`, value: y })
}

const categoriesForMode = computed(() =>
  budgetMode.value === 'annual' ? annualCategories.value : expenseCategories.value
)

const createForm = ref({
  totalAmount: null,
  note: '',
  items: []
})

const itemsTotal = computed(() =>
  createForm.value.items.reduce((sum, item) => sum + Number(item.amount || 0), 0)
)

watch(itemsTotal, (v) => {
  if (showCreate.value) createForm.value.totalAmount = v
})

const itemColumns = [
  { title: '分类', key: 'categoryName' },
  {
    title: '预算金额', key: 'amount', width: 150,
    render: (row) => h(NInputNumber, {
      value: row.amount,
      onUpdateValue: (v) => { row.amount = v },
      min: 0,
      precision: 2
    })
  }
]

async function loadBudget() {
  try {
    const res = budgetMode.value === 'annual'
      ? await annualBudgetApi.progress(selectedYear.value)
      : await budgetApi.progress(selectedMonth.value)
    budget.value = res.data
  } catch (e) {
    budget.value = null
  }
}

function openCreate() {
  if (budget.value) {
    const existing = new Map((budget.value.items || []).map(i => [i.categoryId, Number(i.budget ?? 0)]))
    const items = categoriesForMode.value.map(c => ({
      categoryId: c.id,
      categoryName: c.displayName,
      amount: existing.get(c.id) ?? 0
    }))
    const seen = new Set(items.map(i => i.categoryId))
    ;(budget.value.items || []).forEach(i => {
      if (!seen.has(i.categoryId)) {
        const known = categoriesForMode.value.find(c => c.id === i.categoryId)
        items.push({
          categoryId: i.categoryId,
          categoryName: known ? known.displayName : i.categoryName,
          amount: Number(i.budget ?? 0)
        })
      }
    })
    createForm.value = {
      totalAmount: Number(budget.value.totalBudget ?? 0),
      note: budget.value.note || '',
      items
    }
  } else {
    createForm.value = {
      totalAmount: 0,
      note: '',
      items: categoriesForMode.value.map(c => ({ categoryId: c.id, categoryName: c.displayName, amount: 0 }))
    }
  }
  showCreate.value = true
}

function validateItemTree() {
  const selected = new Set(
    createForm.value.items.filter(i => Number(i.amount) > 0).map(i => i.categoryId)
  )
  const byId = new Map(categoriesForMode.value.map(c => [c.id, c]))
  for (const id of selected) {
    let parentId = byId.get(id)?.parentId
    while (parentId && parentId > 0) {
      if (selected.has(parentId)) {
        const parent = byId.get(parentId)
        const child = byId.get(id)
        message.error(`父分类「${parent?.name || parentId}」与子分类「${child?.name || id}」不能同时设置预算，请只保留一层`)
        return false
      }
      parentId = byId.get(parentId)?.parentId
    }
  }
  return true
}

async function handleSave() {
  if (!itemsTotal.value) { message.error('请至少填写一个分类预算'); return }
  if (!validateItemTree()) return
  createForm.value.totalAmount = itemsTotal.value
  submitting.value = true
  try {
    const items = createForm.value.items.filter(i => Number(i.amount) > 0)
    if (budgetMode.value === 'annual') {
      const data = {
        totalAmount: createForm.value.totalAmount,
        note: createForm.value.note,
        items
      }
      if (budget.value) {
        await annualBudgetApi.update(selectedYear.value, data)
      } else {
        await annualBudgetApi.create({ year: selectedYear.value, ...data })
      }
    } else {
      const data = {
        yearMonth: selectedMonth.value,
        totalAmount: createForm.value.totalAmount,
        note: createForm.value.note,
        items
      }
      if (budget.value) {
        await budgetApi.update(selectedMonth.value, data)
      } else {
        await budgetApi.create(data)
      }
    }
    message.success('保存成功')
    showCreate.value = false
    loadBudget()
  } catch (e) {
    message.error(e?.response?.data?.message || '保存失败')
  } finally { submitting.value = false }
}

async function handleCopy() {
  const prev = dayjs(selectedMonth.value).subtract(1, 'month').format('YYYY-MM')
  try {
    await budgetApi.copy(selectedMonth.value, prev)
    message.success('复制成功')
    loadBudget()
  } catch (e) { message.error('复制失败') }
}

function getStatusType(status) {
  return { EXCEEDED: 'error', WARNING: 'warning', CAUTION: 'warning', NORMAL: 'success' }[status] || 'default'
}
function getProgressStatus(status) {
  return { EXCEEDED: 'error', WARNING: 'warning', CAUTION: 'warning', NORMAL: 'success' }[status] || 'success'
}
function formatNum(n) { return n == null ? '0.00' : Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }

watch([budgetMode, selectedMonth, selectedYear], loadBudget)

onMounted(async () => {
  loadBudget()
  try {
    const res = await categoryApi.tree(1)
    const flat = []
    ;(res.data || []).filter(c => c.parentId === 0).forEach(parent => {
      flat.push({
        id: parent.id,
        name: parent.name,
        displayName: parent.name,
        parentId: 0
      })
      ;(parent.children || []).forEach(child => {
        flat.push({
          id: child.id,
          name: child.name,
          displayName: `　${child.name}`,
          parentId: parent.id
        })
      })
    })
    expenseCategories.value = flat
    const specialParent = flat.find(c => c.parentId === 0 && c.name === '专项支出')
    annualCategories.value = specialParent
      ? flat.filter(c => c.parentId === specialParent.id)
      : []
    createForm.value.items = flat.map(c => ({
      categoryId: c.id,
      categoryName: c.displayName,
      amount: 0
    }))
    createForm.value.totalAmount = 0
  } catch (e) {}
})
</script>

<style scoped>
.budget-items { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 12px; }
.budget-item-card { background: #fff; border-radius: 8px; padding: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }
.item-header { display: flex; justify-content: space-between; align-items: center; }
.item-title { display: flex; align-items: center; font-size: 14px; }
.item-footer { display: flex; justify-content: space-between; font-size: 12px; color: #888; }
.over { color: #e74c3c; font-weight: 600; }
.items-total { margin-top: 8px; font-size: 13px; color: #555; display: flex; justify-content: space-between; gap: 12px; }
.items-tip { color: #999; font-size: 11px; }
@media (max-width: 480px) {
  .budget-item-card { padding: 10px; }
  .item-title { font-size: 13px; }
  .item-footer { font-size: 11px; }
}
</style>
