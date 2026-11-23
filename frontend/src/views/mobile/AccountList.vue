<template>
  <div class="mobile-accounts m-page">
    <div class="snapshot-bar">
      <n-button secondary block class="snapshot-btn" @click="showSnapshots = true">
        <template #icon><n-icon><TrendingUpOutline /></n-icon></template>
        查看净资产月度快照
      </n-button>
    </div>

    <!-- 账户列表 -->
    <div v-if="accounts.length" class="account-list">
      <div v-for="acc in accounts" :key="acc.id" class="account-row" :class="{ inactive: !acc.isActive }">
        <div class="acc-mark" :class="'type-' + acc.type">{{ typeShort(acc.type) }}</div>
        <div class="m-grow">
          <div class="acc-name m-ellipsis">{{ acc.name }}</div>
          <div class="acc-meta m-ellipsis">
            {{ typeLabel(acc.type) }}
            <span v-if="acc.type === 3 && acc.creditLimit"> · 额度 {{ formatNum(acc.creditLimit) }}</span>
            <span v-if="!acc.isActive"> · 已停用</span>
          </div>
        </div>
        <div class="acc-balance">
          <div class="balance-amount m-amount" :class="{ negative: Number(acc.currentBalance) < 0 }">
            {{ formatNum(acc.currentBalance) }}
          </div>
          <div class="acc-currency">{{ acc.currency }}</div>
        </div>
        <n-dropdown trigger="click" placement="bottom-end" :options="actionOptions(acc)" @select="key => handleAction(key, acc)">
          <button class="m-more" aria-label="账户操作">
            <n-icon :size="18"><EllipsisHorizontal /></n-icon>
          </button>
        </n-dropdown>
      </div>
    </div>
    <div v-else-if="!loading" class="m-card">
      <div class="m-empty-cta">
        <div class="cta-icon"><n-icon :size="26"><WalletOutline /></n-icon></div>
        <div class="cta-text">还没有账户<br />添加银行卡、信用卡或钱包后开始记账</div>
        <n-button type="primary" round @click="openCreate">新增账户</n-button>
      </div>
    </div>

    <ReceivableEntry />
    <PrepaidCardEntry />

    <!-- 新增/编辑弹窗 -->
    <n-drawer v-model:show="showCreate" :height="'75vh'" placement="bottom">
      <n-drawer-content :title="editingId ? '编辑账户' : '新增账户'" closable>
        <n-form ref="formRef" :model="form" label-placement="top" label-width="auto" class="m-form">
          <n-form-item label="名称" required>
            <n-input v-model:value="form.name" placeholder="如：招商银行储蓄卡" />
          </n-form-item>
          <n-form-item label="类型" required>
            <n-select v-model:value="form.type" :options="typeOptions" />
          </n-form-item>
          <n-form-item label="币种">
            <n-select v-model:value="form.currency" :options="currencyOptions" />
          </n-form-item>
          <n-form-item v-if="editingId" label="当前余额">
            <n-input-number v-model:value="form.currentBalance" :precision="2" :show-button="false" style="width: 100%">
              <template #prefix>¥</template>
            </n-input-number>
          </n-form-item>
          <n-form-item v-if="form.type === 3" label="信用额度">
            <n-input-number v-model:value="form.creditLimit" :min="0" :precision="2" :show-button="false" style="width: 100%">
              <template #prefix>¥</template>
            </n-input-number>
          </n-form-item>
          <div v-if="form.type === 3" class="inline-row">
            <n-form-item label="账单日" class="inline-item">
              <n-input-number v-model:value="form.billingDay" :min="1" :max="28" />
            </n-form-item>
            <n-form-item label="还款日" class="inline-item">
              <n-input-number v-model:value="form.paymentDay" :min="1" :max="28" />
            </n-form-item>
          </div>
          <n-form-item label="排序">
            <n-input-number v-model:value="form.sortOrder" :min="0" />
          </n-form-item>
          <n-form-item label="扩展字段">
            <n-dynamic-input v-model:value="extFieldsList" :on-create="() => ({ key: '', value: '' })">
              <template #default="{ index }">
                <n-input v-model:value="extFieldsList[index].key" placeholder="键" style="width: 40%" />
                <n-input v-model:value="extFieldsList[index].value" placeholder="值" style="width: 55%; margin-left: 2%" />
              </template>
            </n-dynamic-input>
          </n-form-item>
        </n-form>
        <template #footer>
          <div class="m-sheet-actions">
            <n-button type="primary" :loading="submitting" @click="handleSubmit">保存</n-button>
            <n-button quaternary @click="showCreate = false">取消</n-button>
          </div>
        </template>
      </n-drawer-content>
    </n-drawer>

    <!-- 余额调整弹窗 -->
    <n-modal v-model:show="showBalanceAdjust" preset="card" title="确认余额调整" style="width: 90vw; max-width: 400px">
      <n-alert type="warning" style="margin-bottom: 16px">
        余额变动将通过交易记录，请填写调整原因
      </n-alert>
      <div class="adjust-info">
        <div class="adjust-row"><span>原余额</span><span>¥ {{ formatNum(originalBalance) }}</span></div>
        <div class="adjust-row"><span>新余额</span><span>¥ {{ formatNum(form.currentBalance) }}</span></div>
        <div class="adjust-row highlight">
          <span>变动金额</span>
          <span :style="{ color: balanceDiff > 0 ? '#27ae60' : '#e74c3c' }">
            {{ balanceDiff > 0 ? '+' : '' }}¥ {{ formatNum(Math.abs(balanceDiff)) }}
          </span>
        </div>
      </div>
      <n-form-item label="调整原因" style="margin-top: 12px">
        <n-input v-model:value="adjustReason" placeholder="如：账单对账修正" />
      </n-form-item>
      <template #footer>
        <n-space>
          <n-button type="primary" :loading="adjusting" @click="confirmBalanceAdjust">确认调整</n-button>
          <n-button @click="showBalanceAdjust = false">取消</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 底部新增按钮 -->
    <div v-if="accounts.length" class="fab">
      <n-button type="primary" circle size="large" aria-label="新增账户" @click="openCreate">
        <template #icon><n-icon :size="24"><AddOutline /></n-icon></template>
      </n-button>
    </div>

    <NetWorthSnapshot v-model:show="showSnapshots" mobile />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useDialog, useMessage } from 'naive-ui'
import { accountApi } from '@/api'
import NetWorthSnapshot from '@/components/account/NetWorthSnapshot.vue'
import ReceivableEntry from '@/components/receivable/ReceivableEntry.vue'
import PrepaidCardEntry from '@/components/prepaid/PrepaidCardEntry.vue'
import { AddOutline, EllipsisHorizontal, TrendingUpOutline, WalletOutline } from '@vicons/ionicons5'
import { formatNum } from '@/utils/display'

const dialog = useDialog()
const message = useMessage()
const loading = ref(false)
const accounts = ref([])
const showCreate = ref(false)
const showSnapshots = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const formRef = ref(null)
const originalBalance = ref(0)
const showBalanceAdjust = ref(false)
const adjustReason = ref('')
const adjusting = ref(false)

const balanceDiff = computed(() => {
  return (form.value.currentBalance || 0) - originalBalance.value
})

const form = ref({
  name: '',
  type: 2,
  currency: 'CNY',
  currentBalance: 0,
  creditLimit: null,
  billingDay: null,
  paymentDay: null,
  sortOrder: 0,
  extFields: {}
})

const extFieldsList = ref([])

const typeOptions = [
  { label: '现金', value: 1 },
  { label: '储蓄卡', value: 2 },
  { label: '信用卡', value: 3 },
  { label: '电子钱包', value: 4 },
  { label: '投资账户', value: 5 }
]
const currencyOptions = [
  { label: 'CNY', value: 'CNY' },
  { label: 'USD', value: 'USD' },
  { label: 'EUR', value: 'EUR' },
  { label: 'GBP', value: 'GBP' },
  { label: 'JPY', value: 'JPY' },
  { label: 'HKD', value: 'HKD' },
  { label: 'SGD', value: 'SGD' },
  { label: 'THB', value: 'THB' },
  { label: 'KRW', value: 'KRW' }
]

function typeLabel(type) {
  return typeOptions.find(t => t.value === type)?.label || '未知'
}
function typeShort(type) {
  return { 1: '现', 2: '储', 3: '信', 4: '钱', 5: '投' }[type] || '账'
}
function actionOptions(acc) {
  return [
    { label: '编辑账户', key: 'edit' },
    { label: acc.isActive ? '停用账户' : '启用账户', key: 'toggle' }
  ]
}
function handleAction(key, acc) {
  if (key === 'edit') handleEdit(acc)
  else if (acc.isActive) handleDelete(acc.id)
  else handleActivate(acc.id)
}

async function loadData() {
  loading.value = true
  try {
    const res = await accountApi.list(false)
    accounts.value = res.data
  } catch (e) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  resetForm()
  showCreate.value = true
}

function handleEdit(row) {
  editingId.value = row.id
  originalBalance.value = row.currentBalance
  form.value = {
    name: row.name,
    type: row.type,
    currency: row.currency,
    currentBalance: row.currentBalance,
    creditLimit: row.creditLimit,
    billingDay: row.billingDay,
    paymentDay: row.paymentDay,
    sortOrder: row.sortOrder,
    extFields: row.extFields || {}
  }
  extFieldsList.value = row.extFields ? Object.entries(row.extFields).map(([k, v]) => ({ key: k, value: String(v) })) : []
  showCreate.value = true
}

function buildAccountPayload() {
  const extFields = {}
  extFieldsList.value
    .filter(item => item.key && item.key.trim())
    .forEach(item => { extFields[item.key.trim()] = item.value })
  return {
    name: form.value.name,
    type: form.value.type,
    currency: form.value.currency,
    creditLimit: form.value.creditLimit,
    billingDay: form.value.billingDay,
    paymentDay: form.value.paymentDay,
    sortOrder: form.value.sortOrder,
    extFields
  }
}

async function handleSubmit() {
  if (!form.value.name) { message.error('请输入账户名称'); return }
  submitting.value = true
  try {
    const payload = buildAccountPayload()
    if (editingId.value) {
      await accountApi.update(editingId.value, payload)
      if (balanceDiff.value !== 0) {
        adjustReason.value = ''
        showBalanceAdjust.value = true
        submitting.value = false
        return
      }
      message.success('更新成功')
    } else {
      await accountApi.create({ ...payload, initialBalance: form.value.currentBalance || 0 })
      message.success('创建成功')
    }
    showCreate.value = false
    resetForm()
    loadData()
  } catch (e) {
    message.error('操作失败')
  } finally {
    submitting.value = false
  }
}

async function confirmBalanceAdjust() {
  if (!adjustReason.value.trim()) { message.error('请填写调整原因'); return }
  adjusting.value = true
  try {
    // 其他字段在 handleSubmit 中已保存，这里只调整余额
    await accountApi.adjustBalance(editingId.value, {
      newBalance: form.value.currentBalance,
      reason: adjustReason.value.trim()
    })
    message.success('余额已调整')
    showBalanceAdjust.value = false
    showCreate.value = false
    resetForm()
    loadData()
  } catch (e) {
    message.error('调整失败')
  } finally {
    adjusting.value = false
  }
}

function handleDelete(id) {
  dialog.warning({
    title: '确认停用', content: '停用后该账户不再显示，是否继续？',
    positiveText: '停用', negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await accountApi.remove(id)
        message.success('已停用')
        loadData()
      } catch (e) { message.error('操作失败') }
    }
  })
}

async function handleActivate(id) {
  try {
    await accountApi.update(id, { isActive: true })
    message.success('已启用')
    loadData()
  } catch (e) { message.error('操作失败') }
}

function resetForm() {
  editingId.value = null
  extFieldsList.value = []
  form.value = { name: '', type: 2, currency: 'CNY', currentBalance: 0, creditLimit: null, billingDay: null, paymentDay: null, sortOrder: 0, extFields: {} }
}

onMounted(loadData)
</script>

<style scoped>
.mobile-accounts {
  padding-bottom: calc(88px + env(safe-area-inset-bottom, 0px));
}

.snapshot-bar .snapshot-btn {
  height: 42px;
  border-radius: 12px;
}

.account-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.account-row {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  border-radius: var(--m-radius);
  padding: 14px 12px 14px 14px;
  box-shadow: var(--bp-shadow);
}
.account-row.inactive {
  opacity: 0.62;
}

.acc-mark {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: #fff;
}
.acc-mark.type-1 { background: linear-gradient(135deg, #f59e0b, #f97316); }
.acc-mark.type-2 { background: linear-gradient(135deg, #2563eb, #3b82f6); }
.acc-mark.type-3 { background: linear-gradient(135deg, #ef4444, #f97316); }
.acc-mark.type-4 { background: linear-gradient(135deg, #10b981, #0ea5a4); }
.acc-mark.type-5 { background: linear-gradient(135deg, #8b5cf6, #6366f1); }

.acc-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--bp-ink);
}
.acc-meta {
  margin-top: 3px;
  font-size: 12px;
  color: #94a3b8;
}

.acc-balance {
  flex-shrink: 0;
  text-align: right;
}
.balance-amount {
  font-size: 16px;
  color: var(--bp-ink);
}
.balance-amount.negative {
  color: #ef4444;
}
.acc-currency {
  margin-top: 1px;
  font-size: 11px;
  color: #b0b8c4;
}

.inline-row { display: flex; gap: 12px; }
.inline-item { flex: 1; }

.adjust-info {
  background: #f8f8f8;
  border-radius: 8px;
  padding: 12px;
}
.adjust-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  font-size: 14px;
}
.adjust-row.highlight { font-weight: 600; border-top: 1px solid #ddd; padding-top: 10px; margin-top: 4px; }

.fab {
  position: fixed;
  bottom: calc(70px + env(safe-area-inset-bottom));
  right: 16px;
  z-index: 50;
}
</style>
