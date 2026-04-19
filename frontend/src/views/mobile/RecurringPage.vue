<template>
  <div class="mobile-recurring">
    <!-- 规则列表 -->
    <div v-if="rules.length" class="rule-list">
      <div v-for="rule in rules" :key="rule.id" class="rule-card" :class="{ inactive: !rule.isActive }">
        <div class="rule-header">
          <div class="rule-name">{{ rule.name }}</div>
          <n-tag :type="rule.type === 1 ? 'warning' : 'success'" size="small">
            {{ rule.type === 1 ? '支出' : '收入' }}
          </n-tag>
        </div>
        <div class="rule-tags">
          <n-tag size="tiny">{{ frequencyLabel(rule.frequency) }}</n-tag>
          <n-tag v-if="rule.autoConfirm" size="tiny" type="success">自动确认</n-tag>
          <n-tag v-else size="tiny" type="warning">待确认</n-tag>
        </div>
        <div class="rule-detail-grid">
          <div class="detail-item"><span class="label">金额</span><span class="value">{{ rule.currency }} {{ formatNum(rule.amount) }}</span></div>
          <div class="detail-item"><span class="label">账户</span><span class="value">{{ rule.accountName || '-' }}</span></div>
          <div class="detail-item"><span class="label">分类</span><span class="value">{{ rule.categoryName || '-' }}</span></div>
          <div class="detail-item"><span class="label">下次执行</span><span class="value">{{ rule.nextExecute || '-' }}</span></div>
        </div>
        <div class="rule-footer-info" v-if="rule.endDate">
          <span>截止: {{ rule.endDate }}</span>
        </div>
        <div class="rule-actions">
          <n-button size="small" secondary @click="handleExecute(rule)" :disabled="!rule.isActive">执行</n-button>
          <n-button size="small" secondary @click="handleToggle(rule)">{{ rule.isActive ? '暂停' : '启用' }}</n-button>
          <n-button size="small" secondary type="primary" @click="openEdit(rule)">编辑</n-button>
          <n-button size="small" secondary type="error" @click="handleDelete(rule)">删除</n-button>
        </div>
      </div>
    </div>
    <n-empty v-else description="暂无周期交易规则" />

    <!-- 创建/编辑弹窗 -->
    <n-drawer v-model:show="showCreate" :height="'85vh'" placement="bottom">
      <n-drawer-content :title="editingId ? '编辑规则' : '新增规则'" closable>
        <n-form ref="formRef" :model="createForm" label-placement="top" label-width="auto">
          <n-form-item label="名称" required>
            <n-input v-model:value="createForm.name" placeholder="如：房租、工资" />
          </n-form-item>
          <n-form-item label="类型" required>
            <n-radio-group v-model:value="createForm.type">
              <n-radio-button :value="1">支出</n-radio-button>
              <n-radio-button :value="2">收入</n-radio-button>
            </n-radio-group>
          </n-form-item>
          <n-form-item label="金额" required>
            <n-input-number v-model:value="createForm.amount" :min="0" :precision="2" style="width: 100%">
              <template #prefix>{{ createForm.currency }}</template>
            </n-input-number>
          </n-form-item>
          <n-form-item label="币种">
            <n-select v-model:value="createForm.currency" :options="currencyOptions" />
          </n-form-item>
          <n-form-item label="账户" required>
            <n-select v-model:value="createForm.accountId" :options="accountOptions" placeholder="选择" filterable />
          </n-form-item>
          <n-form-item label="分类" required>
            <n-select v-model:value="createForm.categoryId" :options="categoryFlatOptions" placeholder="选择" filterable />
          </n-form-item>
          <n-form-item v-if="createForm.type === 1" label="商户">
            <n-select v-model:value="createForm.merchantId" :options="merchantOptions" placeholder="可选" filterable clearable />
          </n-form-item>
          <n-form-item label="频率" required>
            <n-select v-model:value="createForm.frequency" :options="frequencyOptions" />
          </n-form-item>
          <n-form-item v-if="needExecuteDay" label="执行日">
            <n-input-number v-model:value="createForm.executeDay" :min="1" :max="maxExecuteDay" />
            <span class="hint">{{ executeDayHint }}</span>
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
        </n-form>
        <template #footer>
          <n-space>
            <n-button type="primary" :loading="submitting" @click="handleSave">保存</n-button>
            <n-button @click="showCreate = false">取消</n-button>
          </n-space>
        </template>
      </n-drawer-content>
    </n-drawer>

    <div class="fab">
      <n-button type="primary" round size="large" @click="showCreate = true">
        <template #icon><n-icon><AddOutline /></n-icon></template>
        新增规则
      </n-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useMessage, useDialog } from 'naive-ui'
import { recurringApi, accountApi, categoryApi, systemApi, merchantApi } from '@/api'
import { AddOutline } from '@vicons/ionicons5'
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
const maxExecuteDay = computed(() => createForm.value.frequency === 'WEEKLY' ? 7 : 28)
const executeDayHint = computed(() => createForm.value.frequency === 'WEEKLY' ? '周一=1, 周日=7' : '1-28日')

function frequencyLabel(f) { return { DAILY: '每天', WEEKLY: '每周', MONTHLY: '每月', YEARLY: '每年' }[f] || f }
function formatNum(n) { return n == null ? '0.00' : Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }

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
  if (!createForm.value.name || !createForm.value.amount || !createForm.value.accountId || !createForm.value.categoryId) {
    message.error('请填写必填项'); return
  }
  submitting.value = true
  try {
    const data = {
      ...createForm.value,
      startDate: dayjs(createForm.value.startDate).format('YYYY-MM-DD'),
      endDate: createForm.value.endDate ? dayjs(createForm.value.endDate).format('YYYY-MM-DD') : null,
      nextExecute: dayjs(createForm.value.startDate).format('YYYY-MM-DD')
    }
    if (editingId.value) { await recurringApi.update(editingId.value, data) }
    else { await recurringApi.create(data) }
    message.success('保存成功')
    showCreate.value = false
    editingId.value = null
    resetForm()
    loadRules()
  } catch (e) { message.error('保存失败') } finally { submitting.value = false }
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
    message.success('已执行')
    loadRules()
  } catch (e) { message.error('执行失败') }
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
.mobile-recurring { padding-bottom: 70px; }

.rule-list { display: flex; flex-direction: column; gap: 10px; }

.rule-card {
  background: #fff;
  border-radius: 10px;
  padding: 14px;
}
.rule-card.inactive { opacity: 0.6; background: #f5f5f5; }

.rule-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.rule-name { font-size: 15px; font-weight: 600; }

.rule-tags { display: flex; gap: 4px; flex-wrap: wrap; margin-bottom: 10px; }

.rule-detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; font-size: 12px; margin-bottom: 8px; }
.detail-item { display: flex; gap: 4px; }
.detail-item .label { color: #888; }
.detail-item .value { color: #333; }

.rule-footer-info { font-size: 12px; color: #999; margin-bottom: 8px; }

.rule-actions { display: flex; gap: 6px; flex-wrap: wrap; }

.hint { font-size: 12px; color: #999; margin-left: 8px; }

.fab {
  position: fixed;
  bottom: calc(70px + env(safe-area-inset-bottom));
  right: 16px;
  z-index: 50;
}
</style>
