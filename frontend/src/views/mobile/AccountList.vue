<template>
  <div class="mobile-accounts">
    <!-- 账户卡片列表 -->
    <div v-if="accounts.length" class="account-list">
      <div v-for="acc in accounts" :key="acc.id" class="account-card">
        <div class="account-info">
          <div class="account-name">{{ acc.name }}</div>
          <div class="account-type">
            <span>{{ typeOptions.find(t => t.value === acc.type)?.label }}</span>
            <n-tag v-if="!acc.isActive" size="tiny" type="default" :bordered="false" style="margin-left: 6px">已停用</n-tag>
          </div>
        </div>
        <div class="account-balance">
          <div class="balance-amount">{{ acc.currency }} {{ formatNum(acc.currentBalance) }}</div>
          <div v-if="acc.type === 3 && acc.creditLimit" class="credit-info">
            额度: {{ formatNum(acc.creditLimit) }}
          </div>
        </div>
        <div class="account-actions">
          <n-button size="small" type="primary" secondary @click="handleEdit(acc)">编辑</n-button>
          <n-button v-if="acc.isActive" size="small" type="error" secondary @click="handleDelete(acc.id)">停用</n-button>
          <n-button v-else size="small" type="success" secondary @click="handleActivate(acc.id)">启用</n-button>
        </div>
      </div>
    </div>
    <n-empty v-else description="暂无账户" />

    <!-- 新增/编辑弹窗 -->
    <n-drawer v-model:show="showCreate" :height="'75vh'" placement="bottom">
      <n-drawer-content :title="editingId ? '编辑账户' : '新增账户'" closable>
        <n-form ref="formRef" :model="form" label-placement="top" label-width="auto">
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
            <n-input-number v-model:value="form.currentBalance" :precision="2" style="width: 100%">
              <template #prefix>¥</template>
            </n-input-number>
          </n-form-item>
          <n-form-item v-if="form.type === 3" label="信用额度">
            <n-input-number v-model:value="form.creditLimit" :min="0" :precision="2" style="width: 100%">
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
          <n-space>
            <n-button type="primary" :loading="submitting" @click="handleSubmit">保存</n-button>
            <n-button @click="showCreate = false">取消</n-button>
          </n-space>
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
    <div class="fab">
      <n-button type="primary" round size="large" @click="showCreate = true">
        <template #icon><n-icon><AddOutline /></n-icon></template>
        新增账户
      </n-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useDialog, useMessage } from 'naive-ui'
import { accountApi } from '@/api'
import { AddOutline } from '@vicons/ionicons5'

const dialog = useDialog()
const message = useMessage()
const loading = ref(false)
const accounts = ref([])
const showCreate = ref(false)
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

async function handleSubmit() {
  if (!form.value.name) { message.error('请输入账户名称'); return }
  submitting.value = true
  try {
    if (editingId.value) {
      if (balanceDiff.value !== 0) {
        adjustReason.value = ''
        showBalanceAdjust.value = true
        submitting.value = false
        return
      }
      const data = { ...form.value }
      if (extFieldsList.value.length > 0) {
        const valid = extFieldsList.value.filter(item => item.key && item.key.trim())
        if (valid.length > 0) {
          data.extFields = {}
          valid.forEach(item => { data.extFields[item.key.trim()] = item.value })
        }
      } else {
        data.extFields = undefined
      }
      await accountApi.update(editingId.value, data)
      message.success('更新成功')
    } else {
      const data = { ...form.value }
      if (extFieldsList.value.length > 0) {
        const valid = extFieldsList.value.filter(item => item.key && item.key.trim())
        if (valid.length > 0) {
          data.extFields = {}
          valid.forEach(item => { data.extFields[item.key.trim()] = item.value })
        }
      } else {
        data.extFields = undefined
      }
      await accountApi.create(data)
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
    const updateData = {}
    if (extFieldsList.value.length > 0) {
      const valid = extFieldsList.value.filter(item => item.key && item.key.trim())
      if (valid.length > 0) {
        updateData.extFields = {}
        valid.forEach(item => { updateData.extFields[item.key.trim()] = item.value })
      }
    }
    if (Object.keys(updateData).length > 0) {
      await accountApi.update(editingId.value, updateData)
    }
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

function formatNum(n) {
  return n == null ? '0.00' : Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

onMounted(loadData)
</script>

<style scoped>
.mobile-accounts {
  padding-bottom: calc(70px + env(safe-area-inset-bottom, 0px));
}

.account-list { display: flex; flex-direction: column; gap: 10px; }

.account-card {
  background: #fff;
  border-radius: 10px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.account-info { display: flex; justify-content: space-between; align-items: flex-start; }
.account-name { font-size: 15px; font-weight: 600; }
.account-type { font-size: 12px; color: #888; margin-top: 3px; display: flex; align-items: center; }

.account-balance { text-align: right; }
.balance-amount { font-size: 18px; font-weight: 700; color: #2c3e50; }
.credit-info { font-size: 12px; color: #999; margin-top: 2px; }

.account-actions { display: flex; gap: 8px; justify-content: flex-end; }

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
