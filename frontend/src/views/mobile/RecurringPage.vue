<template>
  <div class="mobile-recurring m-page">
    <!-- 规则列表 -->
    <div v-if="rules.length" class="rule-list">
      <div v-for="rule in rules" :key="rule.id" class="rule-card" :class="{ inactive: !rule.isActive }">
        <div class="rule-header">
          <div class="rule-title">
            <span class="rule-name">{{ rule.name }}</span>
            <n-tag v-if="!rule.isActive" size="tiny" :bordered="false">已暂停</n-tag>
            <n-tag v-else-if="!rule.autoConfirm" size="tiny" type="warning" :bordered="false">待确认</n-tag>
          </div>
          <n-dropdown trigger="click" placement="bottom-end" :options="ruleActions(rule)" @select="key => handleAction(key, rule)">
            <button class="m-more" aria-label="规则操作">
              <n-icon :size="18"><EllipsisHorizontal /></n-icon>
            </button>
          </n-dropdown>
        </div>
        <div class="rule-main">
          <div class="rule-amount m-amount" :class="rule.type === 1 ? 'expense' : 'income'">
            {{ rule.type === 1 ? '-' : '+' }}{{ rule.currency }} {{ formatNum(rule.amount) }}
          </div>
          <div class="rule-freq">{{ frequencyLabel(rule.frequency) }}</div>
        </div>
        <div class="rule-detail-grid">
          <div class="detail-item"><span class="label">账户</span><span class="value m-ellipsis">{{ rule.accountName || '-' }}</span></div>
          <div class="detail-item"><span class="label">分类</span><span class="value m-ellipsis">{{ rule.categoryName || '-' }}</span></div>
          <div class="detail-item"><span class="label">下次</span><span class="value">{{ rule.nextExecute || '-' }}</span></div>
          <div class="detail-item"><span class="label">截止</span><span class="value">{{ rule.endDate || '长期' }}</span></div>
        </div>
      </div>
    </div>
    <div v-else class="m-card">
      <div class="m-empty-cta">
        <div class="cta-icon"><n-icon :size="26"><RepeatOutline /></n-icon></div>
        <div class="cta-text">还没有周期交易规则<br />房租、工资、订阅可以自动记账</div>
        <n-button type="primary" round @click="openCreate">新增规则</n-button>
      </div>
    </div>

    <!-- 创建/编辑弹窗 -->
    <n-drawer v-model:show="showCreate" :height="'85vh'" placement="bottom">
      <n-drawer-content :title="editingId ? '编辑规则' : '新增规则'" closable>
        <n-form ref="formRef" :model="createForm" label-placement="top" label-width="auto" class="m-form">
          <n-form-item label="名称" required>
            <n-input v-model:value="createForm.name" placeholder="如：房租、工资" size="large" />
          </n-form-item>
          <n-form-item label="类型" required>
            <n-radio-group v-model:value="createForm.type">
              <n-radio-button :value="1">支出</n-radio-button>
              <n-radio-button :value="2">收入</n-radio-button>
            </n-radio-group>
          </n-form-item>
          <n-form-item label="金额" required>
            <n-input-number v-model:value="createForm.amount" :min="0" :precision="2" size="large" :show-button="false" style="width: 100%">
              <template #prefix>{{ createForm.currency }}</template>
            </n-input-number>
          </n-form-item>
          <n-form-item label="币种">
            <n-select v-model:value="createForm.currency" :options="currencyOptions" size="large" />
          </n-form-item>
          <n-form-item label="账户" required>
            <n-select v-model:value="createForm.accountId" :options="accountOptions" placeholder="选择" filterable size="large" />
          </n-form-item>
          <n-form-item label="分类" required>
            <n-select v-model:value="createForm.categoryId" :options="categoryFlatOptions" placeholder="选择" filterable size="large" />
          </n-form-item>
          <n-form-item v-if="createForm.type === 1" label="商户">
            <n-select v-model:value="createForm.merchantId" :options="merchantOptions" placeholder="可选" filterable clearable size="large" />
          </n-form-item>
          <n-form-item label="频率" required>
            <n-select v-model:value="createForm.frequency" :options="frequencyOptions" size="large" />
          </n-form-item>
          <n-form-item v-if="needExecuteDay" label="执行日">
            <n-input-number v-model:value="createForm.executeDay" :min="1" :max="maxExecuteDay" />
            <span class="hint">{{ executeDayHint }}</span>
          </n-form-item>
          <n-form-item label="开始日期" required>
            <n-date-picker v-model:value="createForm.startDate" type="date" size="large" style="width: 100%" />
          </n-form-item>
          <n-form-item label="截止日期">
            <n-date-picker v-model:value="createForm.endDate" type="date" size="large" style="width: 100%" clearable />
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
        </n-form>
        <template #footer>
          <div class="m-sheet-actions">
            <n-button type="primary" :loading="submitting" @click="handleSave">保存</n-button>
            <n-button quaternary @click="showCreate = false">取消</n-button>
          </div>
        </template>
      </n-drawer-content>
    </n-drawer>

    <div v-if="rules.length" class="fab">
      <n-button type="primary" circle size="large" aria-label="新增规则" @click="openCreate">
        <template #icon><n-icon :size="24"><AddOutline /></n-icon></template>
      </n-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useMessage, useDialog } from 'naive-ui'
import { recurringApi, accountApi, categoryApi, systemApi, merchantApi } from '@/api'
import { AddOutline, EllipsisHorizontal, RepeatOutline } from '@vicons/ionicons5'
import dayjs from 'dayjs'
import { formatNum } from '@/utils/display'

const message = useMessage()
const dialog = useDialog()
const rules = ref([])
const showCreate = ref(false)
const submitting = ref(false)
const editingId = ref(null)

const accounts = ref([])
const categories = ref([])
const currencies = ref([])
const merchants = ref([])

const createForm = ref({
  name: '', type: 1, amount: null, currency: 'CNY',
  accountId: null, categoryId: null, merchantId: null,
  frequency: 'MONTHLY', executeDay: null,
  startDate: Date.now(), endDate: null, autoConfirm: false, note: ''
})

const currencyOptions = computed(() => currencies.value.map(c => ({ label: c, value: c })))
const accountOptions = computed(() => accounts.value.map(a => ({ label: a.name, value: a.id })))
const merchantOptions = computed(() => merchants.value.map(m => ({ label: m.name, value: m.id })))
const categoryFlatOptions = computed(() => categories.value.map(c => ({ label: c.name, value: c.id })))

const frequencyOptions = [
  { label: '每天', value: 'DAILY' },
  { label: '每周', value: 'WEEKLY' },
  { label: '每月', value: 'MONTHLY' },
  { label: '每年', value: 'YEARLY' }
]

const needExecuteDay = computed(() => ['WEEKLY', 'MONTHLY'].includes(createForm.value.frequency))
const maxExecuteDay = computed(() => createForm.value.frequency === 'WEEKLY' ? 7 : 31)
const executeDayHint = computed(() => createForm.value.frequency === 'WEEKLY' ? '周一=1, 周日=7' : '1-31日，小月按月底')

function frequencyLabel(f) { return { DAILY: '每天', WEEKLY: '每周', MONTHLY: '每月', YEARLY: '每年' }[f] || f }

function ruleActions(rule) {
  return [
    { label: '立即执行一次', key: 'execute', disabled: !rule.isActive },
    { label: rule.isActive ? '暂停规则' : '启用规则', key: 'toggle' },
    { label: '编辑规则', key: 'edit' },
    { label: '删除规则', key: 'delete' }
  ]
}
function handleAction(key, rule) {
  if (key === 'execute') handleExecute(rule)
  else if (key === 'toggle') handleToggle(rule)
  else if (key === 'edit') openEdit(rule)
  else if (key === 'delete') handleDelete(rule)
}
async function loadRules() {
  try {
    const res = await recurringApi.list()
    rules.value = res.data || []
  } catch (e) { message.error('加载失败') }
}

async function loadOptions() {
  try {
    const [accRes, catRes, sysRes, merchRes] = await Promise.all([
      accountApi.list(), categoryApi.tree(), systemApi.currencies(), merchantApi.list({ isActive: true })
    ])
    accounts.value = accRes.data || []
    const flatCats = []
    const flatten = (items) => { items.forEach(item => { flatCats.push(item); if (item.children) flatten(item.children) }) }
    flatten(catRes.data || [])
    categories.value = flatCats
    currencies.value = sysRes.data || ['CNY', 'USD', 'EUR']
    merchants.value = merchRes.data || []
  } catch (e) {}
}

async function handleSave() {
  if (!createForm.value.name || !createForm.value.amount || !createForm.value.accountId
      || !createForm.value.categoryId || !createForm.value.startDate) {
    message.error('请填写名称、金额、账户、分类和开始日期'); return
  }
  submitting.value = true
  try {
    const data = {
      ...createForm.value,
      startDate: dayjs(createForm.value.startDate).format('YYYY-MM-DD'),
      endDate: createForm.value.endDate ? dayjs(createForm.value.endDate).format('YYYY-MM-DD') : null
    }
    if (editingId.value) { await recurringApi.update(editingId.value, data) }
    else { await recurringApi.create(data) }
    message.success('保存成功')
    showCreate.value = false
    editingId.value = null
    resetForm()
    loadRules()
  } catch (e) { message.error(e.message || '保存失败') } finally { submitting.value = false }
}

function openCreate() {
  resetForm()
  showCreate.value = true
}

function openEdit(rule) {
  editingId.value = rule.id
  createForm.value = {
    name: rule.name, type: rule.type, amount: rule.amount,
    currency: rule.currency || 'CNY', accountId: rule.accountId, categoryId: rule.categoryId,
    merchantId: rule.merchantId || null, frequency: rule.frequency, executeDay: rule.executeDay,
    startDate: rule.startDate ? new Date(rule.startDate) : Date.now(),
    endDate: rule.endDate ? new Date(rule.endDate) : null,
    autoConfirm: rule.autoConfirm, note: rule.note || ''
  }
  showCreate.value = true
}

function resetForm() {
  editingId.value = null
  createForm.value = { name: '', type: 1, amount: null, currency: 'CNY', accountId: null, categoryId: null, merchantId: null, frequency: 'MONTHLY', executeDay: null, startDate: Date.now(), endDate: null, autoConfirm: false, note: '' }
}

async function handleToggle(rule) {
  try {
    await recurringApi.toggle(rule.id)
    message.success(rule.isActive ? '已暂停' : '已启用')
    loadRules()
  } catch (e) { message.error('操作失败') }
}

async function handleExecute(rule) {
  try {
    await recurringApi.execute(rule.id)
    message.success('已生成一笔今天的交易，下次执行日期不变')
    loadRules()
  } catch (e) { message.error(e.message || '执行失败') }
}

function handleDelete(rule) {
  dialog.warning({
    title: '确认删除', content: `确定删除规则 "${rule.name}"？`,
    positiveText: '删除', negativeText: '取消',
    onPositiveClick: async () => {
      try { await recurringApi.remove(rule.id); message.success('已删除'); loadRules() }
      catch (e) { message.error('删除失败') }
    }
  })
}

onMounted(() => { loadRules(); loadOptions() })
</script>

<style scoped>
.mobile-recurring { padding-bottom: calc(88px + env(safe-area-inset-bottom, 0px)); }

.rule-list { display: flex; flex-direction: column; gap: 10px; }

.rule-card {
  background: #fff;
  border-radius: var(--m-radius);
  padding: 14px 16px;
  box-shadow: var(--bp-shadow);
}
.rule-card.inactive { opacity: 0.62; }

.rule-header { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.rule-title { display: flex; align-items: center; gap: 8px; min-width: 0; }
.rule-name { font-size: 15px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.rule-main {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin: 8px 0 12px;
}
.rule-amount { font-size: 20px; }
.rule-amount.expense { color: #ef4444; }
.rule-amount.income { color: #10b981; }
.rule-freq { font-size: 12px; color: #94a3b8; }

.rule-detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--bp-line);
  font-size: 12px;
}
.detail-item { display: flex; gap: 6px; min-width: 0; }
.detail-item .label { flex-shrink: 0; color: #94a3b8; }
.detail-item .value { color: #475569; min-width: 0; }

.hint { font-size: 12px; color: #999; margin-left: 8px; }

.fab {
  position: fixed;
  bottom: calc(76px + env(safe-area-inset-bottom));
  right: 16px;
  z-index: 80;
}
</style>
