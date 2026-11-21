<template>
  <div class="mobile-budget">
    <!-- 月份选择 -->
    <div class="month-selector">
      <n-radio-group v-model:value="budgetMode" size="small" style="flex: 1">
        <n-radio-button value="monthly">月度</n-radio-button>
        <n-radio-button value="annual">年度专项</n-radio-button>
      </n-radio-group>
    </div>
    <div class="month-selector">
      <n-select v-if="budgetMode === 'monthly'" v-model:value="selectedMonth" :options="monthOptions" size="small" style="flex: 1" />
      <n-select v-else v-model:value="selectedYear" :options="yearOptions" size="small" style="flex: 1" />
      <n-button type="primary" size="small" @click="openCreate" style="margin-left: 8px">
        {{ budget ? '编辑' : '创建' }}
      </n-button>
      <n-button v-if="budgetMode === 'monthly' && budget" size="small" @click="handleCopy" style="margin-left: 8px">复制</n-button>
    </div>

    <!-- 总览 -->
    <div v-if="budget" class="overview-grid">
      <div class="overview-item">
        <div class="label">总预算</div>
        <div class="value">¥{{ formatNum(budget.totalBudget) }}</div>
      </div>
      <div class="overview-item">
        <div class="label">已消费</div>
        <div class="value red">¥{{ formatNum(budget.totalSpent) }} <span class="pct-tag" :class="pctClass">{{ budget.progressPct }}%</span></div>
      </div>
      <div class="overview-item">
        <div class="label">剩余</div>
        <div class="value" :class="budget.remaining < 0 ? 'red' : 'green'">¥{{ formatNum(budget.remaining) }}</div>
      </div>
      <div v-if="budgetMode === 'monthly'" class="overview-item">
        <div class="label">日均剩余</div>
        <div class="value">¥{{ formatNum(budget.dailyAvgRemaining) }}</div>
      </div>
    </div>

    <!-- 分类预算卡片 -->
    <div v-if="budget?.items" class="budget-list">
      <div v-for="item in budget.items" :key="item.categoryId" class="budget-card">
        <div class="budget-header">
          <div class="budget-title">
            <span class="dot" :style="{ background: item.categoryColor }"></span>
            <span>{{ item.categoryName }}</span>
          </div>
          <n-tag :type="getStatusType(item.status)" size="small" bordered>{{ item.status }}</n-tag>
        </div>
        <n-progress
          type="line"
          :percentage="Math.min(item.progressPct, 100)"
          :status="getProgressStatus(item.status)"
          :show-indicator="true"
          indicator-placement="inside"
          :height="8"
          style="margin: 8px 0"
        />
        <div class="budget-footer">
          <span>已花 ¥{{ formatNum(item.spent) }}</span>
          <span>预算 ¥{{ formatNum(item.budget) }}</span>
          <span :class="item.remaining < 0 ? 'red' : ''">余 ¥{{ formatNum(item.remaining) }}</span>
        </div>
      </div>
    </div>

    <n-empty v-if="!budget" :description="budgetMode === 'annual' ? '该年度未设置专项预算' : '该月未设置预算'" style="margin-top: 40px" />

    <!-- 创建/编辑弹窗 -->
    <n-drawer v-model:show="showCreate" :height="'85vh'" placement="bottom">
      <n-drawer-content :title="budgetMode === 'annual' ? '设定年度专项预算' : '设定月度预算'" closable>
        <n-form label-placement="top" label-width="auto">
          <n-form-item label="总预算（按明细自动合计）" required>
            <n-input-number v-model:value="createForm.totalAmount" :min="0" :precision="2" disabled style="width: 100%">
              <template #prefix>¥</template>
            </n-input-number>
          </n-form-item>
          <n-form-item label="分类明细">
            <div class="budget-items-list">
              <div v-for="item in createForm.items" :key="item.categoryId" class="budget-row">
                <span class="row-name">{{ item.categoryName }}</span>
                <n-input-number v-model:value="item.amount" :min="0" :precision="2" placeholder="0.00" style="width: 100px">
                  <template #prefix>¥</template>
                </n-input-number>
              </div>
              <div class="items-total">
                <span>明细合计</span>
                <strong>¥{{ formatNum(itemsTotal) }}</strong>
              </div>
              <div class="items-tip">父分类和子分类只能选一层，否则会重复计算</div>
            </div>
          </n-form-item>
          <n-form-item label="备注">
            <n-input v-model:value="createForm.note" type="textarea" :autosize="{ minRows: 2 }" />
          </n-form-item>
        </n-form>
        <template #footer>
          <n-space>
            <n-button type="primary" :loading="submitting" @click="handleSave">保存</n-button>
            <n-button @click="showCreate = false">取消</n-button>
          </n-space>
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, computed } from 'vue'
import { useMessage } from 'naive-ui'
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
  monthOptions.value.push({ label: m.format('YYYY-MM'), value: m.format('YYYY-MM') })
}

const yearOptions = ref([])
for (let i = -1; i <= 1; i++) {
  const y = dayjs().add(i, 'year').format('YYYY')
  yearOptions.value.push({ label: y, value: y })
}

const categoriesForMode = computed(() =>
  budgetMode.value === 'annual' ? annualCategories.value : expenseCategories.value
)

const createForm = ref({ totalAmount: null, note: '', items: [] })

const itemsTotal = computed(() =>
  createForm.value.items.reduce((sum, item) => sum + Number(item.amount || 0), 0)
)

watch(itemsTotal, (v) => {
  if (showCreate.value) createForm.value.totalAmount = v
})

async function loadBudget() {
  try {
    const res = budgetMode.value === 'annual'
      ? await annualBudgetApi.progress(selectedYear.value)
      : await budgetApi.progress(selectedMonth.value)
    budget.value = res.data
  } catch (e) { budget.value = null }
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

const pctClass = computed(() => {
  if (!budget.value) return ''
  if (budget.value.progressPct > 100) return 'red'
  if (budget.value.progressPct > 80) return 'orange'
  return 'green'
})

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
    createForm.value.items = flat.map(c => ({ categoryId: c.id, categoryName: c.displayName, amount: 0 }))
    createForm.value.totalAmount = 0
  } catch (e) {}
})
</script>

<style scoped>
.mobile-budget { padding-bottom: 12px; }

.month-selector { display: flex; align-items: center; margin-bottom: 12px; }

.overview-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 12px;
}
.overview-item {
  background: #fff;
  border-radius: 10px;
  padding: 12px;
}
.overview-item .label { font-size: 12px; color: #888; margin-bottom: 4px; }
.overview-item .value { font-size: 16px; font-weight: 700; }
.overview-item .value.red { color: #e74c3c; }
.overview-item .value.green { color: #27ae60; }
.pct-tag { font-size: 11px; padding: 2px 6px; border-radius: 8px; color: #fff; }
.pct-tag.red { background: #e74c3c; }
.pct-tag.orange { background: #f39c12; }
.pct-tag.green { background: #27ae60; }

.budget-list { display: flex; flex-direction: column; gap: 10px; }
.budget-card { background: #fff; border-radius: 10px; padding: 14px; }
.budget-header { display: flex; justify-content: space-between; align-items: center; }
.budget-title { display: flex; align-items: center; gap: 8px; font-size: 14px; font-weight: 600; }
.dot { width: 12px; height: 12px; border-radius: 50%; }
.budget-footer { display: flex; justify-content: space-between; font-size: 12px; color: #888; }
.budget-footer .red { color: #e74c3c; font-weight: 600; }

.budget-items-list { display: flex; flex-direction: column; gap: 8px; max-height: 50vh; overflow-y: auto; }
.budget-row { display: flex; justify-content: space-between; align-items: center; padding: 6px 0; border-bottom: 1px solid #f5f5f5; }
.row-name { font-size: 14px; }
.items-total { display: flex; justify-content: space-between; padding: 10px 0 4px; font-size: 14px; }
.items-tip { color: #999; font-size: 11px; padding-bottom: 4px; }
</style>
