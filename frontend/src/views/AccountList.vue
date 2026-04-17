<template>
  <n-card title="账户管理">
    <template #header-extra>
      <n-button type="primary" @click="showCreate = true">新增账户</n-button>
    </template>

    <n-data-table :columns="columns" :data="accounts" :loading="loading" />

    <!-- 新增/编辑弹窗 -->
    <n-modal v-model:show="showCreate" preset="card" :title="editingId ? '编辑账户' : '新增账户'" style="width: 500px">
      <n-form ref="formRef" :model="form" label-placement="left" label-width="80">
        <n-form-item label="名称" required>
          <n-input v-model:value="form.name" placeholder="如：招商银行储蓄卡" />
        </n-form-item>
        <n-form-item label="类型" required>
          <n-select v-model:value="form.type" :options="typeOptions" />
        </n-form-item>
        <n-form-item label="币种">
          <n-select v-model:value="form.currency" :options="currencyOptions" style="width: 120px" />
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
        <n-form-item v-if="form.type === 3" label="账单日">
          <n-input-number v-model:value="form.billingDay" :min="1" :max="28" style="width: 100px" />
        </n-form-item>
        <n-form-item v-if="form.type === 3" label="还款日">
          <n-input-number v-model:value="form.paymentDay" :min="1" :max="28" style="width: 100px" />
        </n-form-item>
        <n-form-item label="排序">
          <n-input-number v-model:value="form.sortOrder" :min="0" style="width: 100px" />
        </n-form-item>
        <n-form-item>
          <n-space>
            <n-button type="primary" :loading="submitting" @click="handleSubmit">保存</n-button>
            <n-button @click="showCreate = false">取消</n-button>
          </n-space>
        </n-form-item>
      </n-form>
    </n-modal>

    <!-- 余额调整确认弹窗 -->
    <n-modal v-model:show="showBalanceAdjust" preset="card" title="确认余额调整" style="width: 400px">
      <n-alert type="warning" style="margin-bottom: 16px">
        余额变动将通过交易记录，请填写调整原因
      </n-alert>
      <n-form label-placement="left" label-width="80">
        <n-form-item label="原余额">
          <n-text>¥ {{ formatNum(originalBalance) }}</n-text>
        </n-form-item>
        <n-form-item label="新余额">
          <n-text :type="balanceDiff > 0 ? 'success' : 'error'">¥ {{ formatNum(form.currentBalance) }}</n-text>
        </n-form-item>
        <n-form-item label="变动金额">
          <n-text :type="balanceDiff > 0 ? 'success' : 'error'">
            {{ balanceDiff > 0 ? '+' : '' }}¥ {{ formatNum(Math.abs(balanceDiff)) }}
          </n-text>
        </n-form-item>
        <n-form-item label="调整原因" required>
          <n-input v-model:value="adjustReason" placeholder="如：账单对账修正" />
        </n-form-item>
        <n-form-item>
          <n-space>
            <n-button type="primary" :loading="adjusting" @click="confirmBalanceAdjust">确认调整</n-button>
            <n-button @click="showBalanceAdjust = false">取消</n-button>
          </n-space>
        </n-form-item>
      </n-form>
    </n-modal>
  </n-card>
</template>

<script setup>
import { ref, h, onMounted, computed } from 'vue'
import { useDialog, useMessage } from 'naive-ui'
import { accountApi } from '@/api'

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
  sortOrder: 0
})

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

const columns = [
  { title: '名称', key: 'name' },
  {
    title: '类型', key: 'type', width: 90,
    render: (row) => typeOptions.find(t => t.value === row.type)?.label || '-'
  },
  {
    title: '余额', key: 'currentBalance', width: 140, align: 'right',
    render: (row) => `${row.currency} ${formatNum(row.currentBalance)}`
  },
  {
    title: '信用额度', key: 'creditLimit', width: 120, align: 'right',
    render: (row) => row.creditLimit ? formatNum(row.creditLimit) : '-'
  },
  {
    title: '状态', key: 'isActive', width: 70,
    render: (row) => row.isActive
      ? h('n-tag', { type: 'success', size: 'small', bordered: false }, { default: () => '正常' })
      : h('n-tag', { type: 'default', size: 'small', bordered: false }, { default: () => '停用' })
  },
  {
    title: '操作', key: 'actions', width: 140,
    render: (row) => h('n-space', { size: 4 }, [
      h('n-button', { size: 'tiny', text: true, type: 'primary', onClick: () => handleEdit(row) }, { default: () => '编辑' }),
      row.isActive
        ? h('n-button', { size: 'tiny', text: true, type: 'error', onClick: () => handleDelete(row.id) }, { default: () => '停用' })
        : h('n-button', { size: 'tiny', text: true, type: 'success', onClick: () => handleActivate(row.id) }, { default: () => '启用' })
    ])
  }
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
    sortOrder: row.sortOrder
  }
  showCreate.value = true
}

async function handleSubmit() {
  if (!form.value.name) {
    message.error('请输入账户名称')
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      // 检查余额是否有变化
      if (balanceDiff.value !== 0) {
        adjustReason.value = ''
        showBalanceAdjust.value = true
        submitting.value = false
        return
      }
      // 余额没变化，直接更新其他字段
      await accountApi.update(editingId.value, form.value)
      message.success('更新成功')
    } else {
      await accountApi.create(form.value)
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
  if (!adjustReason.value.trim()) {
    message.error('请填写调整原因')
    return
  }
  adjusting.value = true
  try {
    // 调用余额调整接口
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
  form.value = { name: '', type: 2, currency: 'CNY', currentBalance: 0, creditLimit: null, billingDay: null, paymentDay: null, sortOrder: 0 }
}

function formatNum(n) {
  return n == null ? '0.00' : Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

onMounted(loadData)
</script>
