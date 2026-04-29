<template>
  <div class="mobile-budget">
    <!-- 月份选择 -->
    <div class="month-selector">
      <n-select v-model:value="selectedMonth" :options="monthOptions" size="small" style="flex: 1" />
      <n-button v-if="budget" type="primary" size="small" @click="showCreate = true" style="margin-left: 8px">
        {{ budget ? '编辑' : '创建' }}
      </n-button>
      <n-button v-if="budget" size="small" @click="handleCopy" style="margin-left: 8px">复制</n-button>
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
      <div class="overview-item">
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

    <n-empty v-if="!budget" description="该月未设置预算" style="margin-top: 40px" />

    <!-- 创建/编辑弹窗 -->
    <n-drawer v-model:show="showCreate" :height="'85vh'" placement="bottom">
      <n-drawer-content title="设定预算" closable>
        <n-form label-placement="top" label-width="auto">
          <n-form-item label="总预算" required>
            <n-input-number v-model:value="createForm.totalAmount" :min="0" :precision="2" style="width: 100%">
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
  monthOptions.value.push({ label: m.format('YYYY-MM'), value: m.format('YYYY-MM') })
}

const createForm = ref({ totalAmount: null, note: '', items: [] })

async function loadBudget() {
  try {
    const res = await budgetApi.progress(selectedMonth.value)
    budget.value = res.data
  } catch (e) { budget.value = null }
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

const pctClass = computed(() => {
  if (!budget.value) return ''
  if (budget.value.progressPct > 100) return 'red'
  if (budget.value.progressPct > 80) return 'orange'
  return 'green'
})

watch(selectedMonth, loadBudget)

onMounted(async () => {
  loadBudget()
  try {
    const res = await categoryApi.tree(1)
    expenseCategories.value = res.data.filter(c => c.parentId === 0)
    createForm.value.items = expenseCategories.value.map(c => ({ categoryId: c.id, categoryName: c.name, amount: 0 }))
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
</style>
