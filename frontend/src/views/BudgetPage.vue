<template>
  <n-card title="预算管理">
    <template #header-extra>
      <n-space :size="8" :wrap="true">
        <n-select v-model:value="selectedMonth" :options="monthOptions" style="min-width: 120px; max-width: 140px" />
        <n-button type="primary" size="small" @click="showCreate = !budget">
          {{ budget ? '编辑' : '创建' }}
        </n-button>
        <n-button size="small" @click="handleCopy" :disabled="!budget">复制</n-button>
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
      <n-gi span="2 m:1">
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

    <n-empty v-if="!budget" description="该月未设置预算，点击「创建」开始" />

    <!-- 创建/编辑弹窗 -->
    <n-modal v-model:show="showCreate" preset="card" title="设定预算" style="max-width: 90vw; width: 550px">
      <n-form ref="formRef" :model="createForm" label-placement="left" label-width="auto">
        <n-form-item label="总预算" required>
          <n-input-number v-model:value="createForm.totalAmount" :min="0" :precision="2" style="width: 100%">
            <template #prefix>¥</template>
          </n-input-number>
        </n-form-item>
        <n-form-item label="分类明细">
          <n-data-table
            :columns="itemColumns"
            :data="createForm.items"
            :pagination="false"
            size="small"
          />
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
import { ref, h, onMounted, watch } from 'vue'
import { useMessage } from 'naive-ui'
import { budgetApi, categoryApi } from '@/api'
import dayjs from 'dayjs'

const message = useMessage()
const selectedMonth = ref(dayjs().format('YYYY-MM'))
const budget = ref(null)
const showCreate = ref(false)
const submitting = ref(false)
const expenseCategories = ref([])

const monthOptions = ref([])
for (let i = -6; i <= 6; i++) {
  const m = dayjs().add(i, 'month')
  monthOptions.value.push({ label: m.format('YYYY年MM月'), value: m.format('YYYY-MM') })
}

const createForm = ref({
  totalAmount: null,
  note: '',
  items: []
})

const itemColumns = [
  { title: '分类', key: 'categoryName' },
  {
    title: '预算金额', key: 'amount', width: 150,
    render: (row) => h('n-input-number', {
      value: row.amount,
      onUpdateValue: (v) => { row.amount = v }
    })
  }
]

async function loadBudget() {
  try {
    const res = await budgetApi.progress(selectedMonth.value)
    budget.value = res.data
  } catch (e) {
    budget.value = null
  }
}

async function handleSave() {
  if (!createForm.value.totalAmount) { message.error('请输入总预算'); return }
  submitting.value = true
  try {
    const data = {
      yearMonth: selectedMonth.value,
      totalAmount: createForm.value.totalAmount,
      note: createForm.value.note,
      items: createForm.value.items.filter(i => i.amount > 0)
    }
    if (budget.value) {
      await budgetApi.update(selectedMonth.value, data)
    } else {
      await budgetApi.create(data)
    }
    message.success('保存成功')
    showCreate.value = false
    loadBudget()
  } catch (e) { message.error('保存失败') } finally { submitting.value = false }
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

watch(selectedMonth, loadBudget)

onMounted(async () => {
  loadBudget()
  try {
    const res = await categoryApi.tree(1)
    expenseCategories.value = res.data.filter(c => c.parentId === 0)
    createForm.value.items = expenseCategories.value.map(c => ({
      categoryId: c.id,
      categoryName: c.name,
      amount: 0
    }))
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
@media (max-width: 480px) {
  .budget-item-card { padding: 10px; }
  .item-title { font-size: 13px; }
  .item-footer { font-size: 11px; }
}
</style>
