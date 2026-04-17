<template>
  <n-card title="周期交易">
    <template #header-extra>
      <n-button type="primary" size="small" @click="showCreate = true">
        + 新增规则
      </n-button>
    </template>

    <n-empty v-if="!rules.length" description="暂无周期交易规则" />

    <div v-else class="rule-list">
      <div v-for="rule in rules" :key="rule.id" class="rule-card" :class="{ inactive: !rule.isActive }">
        <div class="rule-header">
          <div class="rule-info">
            <span class="rule-name">{{ rule.name }}</span>
            <n-tag :type="rule.type === 1 ? 'warning' : 'success'" size="small">
              {{ rule.type === 1 ? '支出' : '收入' }}
            </n-tag>
            <n-tag size="small">{{ frequencyLabel(rule.frequency) }}</n-tag>
          </div>
          <n-space :size="8">
            <n-button size="tiny" @click="handleExecute(rule)" :disabled="!rule.isActive">
              立即执行
            </n-button>
            <n-button size="tiny" @click="handleToggle(rule)">
              {{ rule.isActive ? '暂停' : '启用' }}
            </n-button>
            <n-button size="tiny" @click="openEdit(rule)">编辑</n-button>
            <n-button size="tiny" type="error" @click="handleDelete(rule)">删除</n-button>
          </n-space>
        </div>

        <div class="rule-detail">
          <div class="detail-item">
            <span class="label">金额</span>
            <span class="value">{{ rule.currency }} {{ formatNum(rule.amount) }}</span>
          </div>
          <div class="detail-item">
            <span class="label">账户</span>
            <span class="value">{{ rule.accountName || `ID:${rule.accountId}` }}</span>
          </div>
          <div class="detail-item">
            <span class="label">分类</span>
            <span class="value">{{ rule.categoryName || `ID:${rule.categoryId}` }}</span>
          </div>
          <div class="detail-item">
            <span class="label">下次执行</span>
            <span class="value">{{ rule.nextExecute || '-' }}</span>
          </div>
        </div>

        <div class="rule-footer">
          <span v-if="rule.autoConfirm">
            <n-icon color="#52c41a"><checkmark-circle-outline /></n-icon> 自动确认
          </span>
          <span v-else>
            <n-icon color="#faad14"><alarm-outline /></n-icon> 待确认
          </span>
          <span v-if="rule.endDate">截止: {{ rule.endDate }}</span>
          <span v-else>永久有效</span>
        </div>
      </div>
    </div>

    <!-- 创建/编辑弹窗 -->
    <n-modal v-model:show="showCreate" preset="card" title="新增周期规则" style="max-width: 90vw; width: 500px">
      <n-form ref="formRef" :model="createForm" label-placement="left" label-width="auto">
        <n-form-item label="名称" required>
          <n-input v-model:value="createForm.name" placeholder="如: 房租、工资" />
        </n-form-item>

        <n-form-item label="类型" required>
          <n-radio-group v-model:value="createForm.type">
            <n-radio :value="1">支出</n-radio>
            <n-radio :value="2">收入</n-radio>
          </n-radio-group>
        </n-form-item>

        <n-form-item label="金额" required>
          <n-input-number v-model:value="createForm.amount" :min="0" :precision="2" style="width: 100%">
            <template #prefix>{{ createForm.currency }}</template>
          </n-input-number>
        </n-form-item>

        <n-form-item label="币种">
          <n-select v-model:value="createForm.currency" :options="currencyOptions" style="width: 100%" />
        </n-form-item>

        <n-form-item label="账户" required>
          <n-select v-model:value="createForm.accountId" :options="accountOptions" placeholder="选择账户" style="width: 100%" />
        </n-form-item>

        <n-form-item label="分类" required>
          <n-tree-select
            v-model:value="createForm.categoryId"
            :options="categoryTree"
            placeholder="选择分类"
            style="width: 100%"
          />
        </n-form-item>

        <n-form-item label="频率" required>
          <n-select v-model:value="createForm.frequency" :options="frequencyOptions" style="width: 100%" />
        </n-form-item>

        <n-form-item v-if="needExecuteDay" label="执行日">
          <n-input-number v-model:value="createForm.executeDay" :min="1" :max="maxExecuteDay" style="width: 100%">
            <template #suffix>{{ executeDayHint }}</template>
          </n-input-number>
        </n-form-item>

        <n-form-item label="开始日期" required>
          <n-date-picker v-model:value="createForm.startDate" type="date" style="width: 100%" />
        </n-form-item>

        <n-form-item label="截止日期">
          <n-date-picker v-model:value="createForm.endDate" type="date" style="width: 100%" clearable />
        </n-form-item>

        <n-form-item label="自动确认">
          <n-switch v-model:value="createForm.autoConfirm">
            <template #checked>是</template>
            <template #unchecked>否</template>
          </n-switch>
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
import { ref, computed, onMounted, h } from 'vue'
import { useMessage, useDialog } from 'naive-ui'
import { CheckmarkCircleOutline, AlarmOutline } from '@vicons/ionicons5'
import { recurringApi, accountApi, categoryApi, systemApi } from '@/api'
import dayjs from 'dayjs'

const message = useMessage()
const dialog = useDialog()
const rules = ref([])
const showCreate = ref(false)
const submitting = ref(false)
const editingId = ref(null)

const accounts = ref([])
const categories = ref([])
const currencies = ref([])

const createForm = ref({
  name: '',
  type: 1,
  amount: null,
  currency: 'CNY',
  accountId: null,
  categoryId: null,
  frequency: 'MONTHLY',
  executeDay: null,
  startDate: Date.now(),
  endDate: null,
  autoConfirm: false,
  note: ''
})

const currencyOptions = computed(() => currencies.value.map(c => ({ label: c, value: c })))
const accountOptions = computed(() => accounts.value.map(a => ({ label: a.name, value: a.id })))
const categoryTree = computed(() => buildCategoryTree(categories.value))

const frequencyOptions = [
  { label: '每天', value: 'DAILY' },
  { label: '每周', value: 'WEEKLY' },
  { label: '每月', value: 'MONTHLY' },
  { label: '每年', value: 'YEARLY' }
]

const needExecuteDay = computed(() => ['WEEKLY', 'MONTHLY'].includes(createForm.value.frequency))
const maxExecuteDay = computed(() => createForm.value.frequency === 'WEEKLY' ? 7 : 28)
const executeDayHint = computed(() => createForm.value.frequency === 'WEEKLY' ? '周一=1, 周日=7' : '1-28日')

function frequencyLabel(f) {
  return { DAILY: '每天', WEEKLY: '每周', MONTHLY: '每月', YEARLY: '每年' }[f] || f
}

function formatNum(n) {
  return n == null ? '0.00' : Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

function buildCategoryTree(list, parentId = 0) {
  return list
    .filter(c => c.parentId === parentId)
    .map(c => ({
      label: c.name,
      key: c.id,
      children: buildCategoryTree(list, c.id)
    }))
}

async function loadRules() {
  try {
    const res = await recurringApi.list()
    rules.value = res.data || []
  } catch (e) {
    message.error('加载失败')
  }
}

async function loadOptions() {
  try {
    const [accRes, catRes, sysRes] = await Promise.all([
      accountApi.list(),
      categoryApi.tree(),
      systemApi.currencies()
    ])
    accounts.value = accRes.data || []
    // flatten categories for tree
    const flatCats = []
    const flatten = (items) => {
      items.forEach(item => {
        flatCats.push(item)
        if (item.children) flatten(item.children)
      })
    }
    flatten(catRes.data || [])
    categories.value = flatCats
    currencies.value = sysRes.data || ['CNY', 'USD', 'EUR']
  } catch (e) {}
}

async function handleSave() {
  if (!createForm.value.name || !createForm.value.amount || !createForm.value.accountId || !createForm.value.categoryId) {
    message.error('请填写必填项')
    return
  }
  submitting.value = true
  try {
    const data = {
      ...createForm.value,
      startDate: dayjs(createForm.value.startDate).format('YYYY-MM-DD'),
      endDate: createForm.value.endDate ? dayjs(createForm.value.endDate).format('YYYY-MM-DD') : null,
      nextExecute: dayjs(createForm.value.startDate).format('YYYY-MM-DD')
    }
    if (editingId.value) {
      await recurringApi.update(editingId.value, data)
    } else {
      await recurringApi.create(data)
    }
    message.success('保存成功')
    showCreate.value = false
    editingId.value = null
    resetForm()
    loadRules()
  } catch (e) {
    message.error('保存失败')
  } finally {
    submitting.value = false
  }
}

function openEdit(rule) {
  editingId.value = rule.id
  createForm.value = {
    name: rule.name,
    type: rule.type,
    amount: rule.amount,
    currency: rule.currency || 'CNY',
    accountId: rule.accountId,
    categoryId: rule.categoryId,
    frequency: rule.frequency,
    executeDay: rule.executeDay,
    startDate: rule.startDate ? new Date(rule.startDate) : Date.now(),
    endDate: rule.endDate ? new Date(rule.endDate) : null,
    autoConfirm: rule.autoConfirm,
    note: rule.note || ''
  }
  showCreate.value = true
}

function resetForm() {
  createForm.value = {
    name: '',
    type: 1,
    amount: null,
    currency: 'CNY',
    accountId: null,
    categoryId: null,
    frequency: 'MONTHLY',
    executeDay: null,
    startDate: Date.now(),
    endDate: null,
    autoConfirm: false,
    note: ''
  }
}

async function handleToggle(rule) {
  try {
    await recurringApi.toggle(rule.id)
    message.success(rule.isActive ? '已暂停' : '已启用')
    loadRules()
  } catch (e) {
    message.error('操作失败')
  }
}

async function handleExecute(rule) {
  try {
    await recurringApi.execute(rule.id)
    message.success('已执行，请查看交易记录')
    loadRules()
  } catch (e) {
    message.error('执行失败')
  }
}

function handleDelete(rule) {
  dialog.warning({
    title: '确认删除',
    content: `确定删除规则 "${rule.name}"？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await recurringApi.remove(rule.id)
        message.success('已删除')
        loadRules()
      } catch (e) {
        message.error('删除失败')
      }
    }
  })
}

onMounted(() => {
  loadRules()
  loadOptions()
})
</script>

<style scoped>
.rule-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 12px; }
.rule-card {
  background: #fff;
  border-radius: 8px;
  padding: 12px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
  border: 1px solid #eee;
}
.rule-card.inactive { opacity: 0.6; background: #f5f5f5; }
.rule-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.rule-info { display: flex; align-items: center; gap: 8px; }
.rule-name { font-weight: 600; font-size: 14px; }
.rule-detail { display: grid; grid-template-columns: repeat(2, 1fr); gap: 6px; font-size: 12px; margin-bottom: 8px; }
.detail-item { display: flex; gap: 4px; }
.detail-item .label { color: #888; }
.detail-item .value { color: #333; }
.rule-footer { display: flex; justify-content: space-between; font-size: 12px; color: #888; }
.rule-footer span { display: flex; align-items: center; gap: 4px; }

@media (max-width: 480px) {
  .rule-list { grid-template-columns: 1fr; }
  .rule-header { flex-direction: column; gap: 8px; }
  .rule-detail { grid-template-columns: 1fr; }
}
</style>