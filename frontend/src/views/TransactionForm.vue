<template>
  <n-card :title="isEdit ? '编辑交易' : '新增交易'" style="max-width: 600px; margin: 0 auto">
    <n-form ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="80">
      <n-form-item label="类型" path="type">
        <n-radio-group v-model:value="form.type">
          <n-radio-button :value="1">支出</n-radio-button>
          <n-radio-button :value="2">收入</n-radio-button>
          <n-radio-button :value="3">转账</n-radio-button>
        </n-radio-group>
      </n-form-item>

      <n-form-item label="金额" path="amount">
        <n-input-number v-model:value="form.amount" :min="0.01" :precision="2" placeholder="0.00" size="large" style="width: 100%">
          <template #prefix>¥</template>
        </n-input-number>
      </n-form-item>

      <n-form-item label="币种" path="currency">
        <n-select v-model:value="form.currency" :options="currencyOptions" style="width: 120px" />
      </n-form-item>

      <n-form-item label="账户" path="accountId">
        <n-select v-model:value="form.accountId" :options="accountOptions" placeholder="选择账户" filterable />
      </n-form-item>

      <n-form-item v-if="form.type === 3" label="目标账户" path="targetAccountId">
        <n-select v-model:value="form.targetAccountId" :options="accountOptions" placeholder="选择目标账户" filterable />
      </n-form-item>

      <n-form-item label="分类" path="categoryId">
        <n-cascader
          v-model:value="form.categoryId"
          :options="categoryTree"
          placeholder="选择分类"
          check-strategy="child"
          cascade="false"
          show-path
          filterable
        />
      </n-form-item>

      <n-form-item v-if="form.type !== 3" label="商户" path="merchantId">
        <n-select
          v-model:value="form.merchantId"
          :options="merchantOptions"
          placeholder="输入商户名搜索或创建"
          filterable
          clearable
          remote
          :loading="merchantLoading"
          @search="handleMerchantSearch"
        />
      </n-form-item>

      <n-form-item label="日期时间" path="transactionDate">
        <n-date-picker v-model:value="form.transactionDate" type="datetime" style="width: 100%" />
      </n-form-item>

      <n-form-item label="备注" path="note">
        <n-input v-model:value="form.note" placeholder="可选备注" />
      </n-form-item>

      <n-form-item label="标签" path="tags">
        <n-dynamic-tags v-model:value="form.tags" />
      </n-form-item>

      <n-form-item label="扩展字段">
        <n-dynamic-input v-model:value="extFieldsList" :on-create="() => ({ key: '', value: '' })">
          <template #default="{ index }">
            <n-input v-model:value="extFieldsList[index].key" placeholder="键" style="width: 40%" />
            <n-input v-model:value="extFieldsList[index].value" placeholder="值" style="width: 55%; margin-left: 2%" />
          </template>
        </n-dynamic-input>
      </n-form-item>

      <n-form-item>
        <n-space>
          <n-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ isEdit ? '保存' : '提交' }}
          </n-button>
          <n-button @click="router.back()">取消</n-button>
        </n-space>
      </n-form-item>
    </n-form>
  </n-card>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useMessage } from 'naive-ui'
import { transactionApi, accountApi, categoryApi, merchantApi } from '@/api'
import dayjs from 'dayjs'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const formRef = ref(null)
const submitting = ref(false)

const isEdit = computed(() => !!route.params.id)

const form = ref({
  type: 1,
  amount: null,
  currency: 'CNY',
  accountId: null,
  targetAccountId: null,
  categoryId: null,
  merchantId: null,
  merchantName: '',
  transactionDate: dayjs().format('YYYY-MM-DDTHH:mm:ss'),
  note: '',
  tags: [],
  extFields: {}
})

const extFieldsList = ref([])

const rules = {
  type: { required: true, type: 'number', message: '请选择类型', trigger: 'change' },
  amount: { required: true, type: 'number', message: '请输入金额', trigger: 'blur' },
  accountId: { required: true, type: 'number', message: '请选择账户', trigger: 'change' },
  categoryId: {
    required: true,
    validator: (rule, value) => {
      if (!value) return new Error('请选择分类')
      if (Array.isArray(value) && value.length === 0) return new Error('请选择分类')
      return true
    },
    trigger: 'change'
  },
  transactionDate: { required: true, message: '请选择日期', trigger: 'change' }
}

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

const accountOptions = ref([])
const allCategoryTree = ref([])
const merchantOptions = ref([])
const merchantLoading = ref(false)
const merchantSearchTimer = ref(null)

// 根据交易类型筛选分类（保持树形结构）
const categoryTree = computed(() => {
  return allCategoryTree.value.filter(c => c.type === form.value.type).map(toCascaderOption)
})

function toCascaderOption(c) {
  const opt = { label: c.name, value: c.id }
  if (c.children?.length) {
    opt.children = c.children.map(toCascaderOption)
  }
  return opt
}

// 类型变化时清空分类选择
watch(() => form.value.type, () => {
  if (form.value.categoryId) {
    // 检查当前分类是否在新类型的树中
    const found = findInTree(categoryTree.value, form.value.categoryId)
    if (!found) {
      form.value.categoryId = null
    }
  }
})

function findInTree(tree, id) {
  for (const node of tree) {
    if (node.value === id) return true
    if (node.children?.length && findInTree(node.children, id)) return true
  }
  return false
}

// 商户模糊搜索（防抖）
async function handleMerchantSearch(keyword) {
  if (merchantSearchTimer.value) {
    clearTimeout(merchantSearchTimer.value)
  }

  if (!keyword || keyword.trim().length < 1) {
    merchantOptions.value = []
    return
  }

  merchantSearchTimer.value = setTimeout(async () => {
    merchantLoading.value = true
    try {
      const res = await merchantApi.search(keyword.trim(), 10)
      const options = (res.data || []).map(m => ({
        label: m.name + (m.categoryName ? ` (${m.categoryName})` : ''),
        value: m.id
      }))

      // 如果没有匹配结果，显示"创建新商户"选项
      if (options.length === 0) {
        options.push({
          label: `创建新商户 "${keyword.trim()}"`,
          value: 'new'
        })
      }

      merchantOptions.value = options
      form.value.merchantName = keyword.trim()
    } catch (e) {
      message.error('搜索失败')
    } finally {
      merchantLoading.value = false
    }
  }, 300)
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
    submitting.value = true

    const data = { ...form.value }
    // cascader 可能返回数组，取最后一个值（叶子节点）
    if (Array.isArray(data.categoryId)) {
      data.categoryId = data.categoryId[data.categoryId.length - 1]
    }

    // 拆分日期时间字段为 date 和 time
    if (data.transactionDate) {
      const dt = data.transactionDate.includes('T') ? data.transactionDate.split('T') : [data.transactionDate, null]
      data.transactionDate = dt[0]
      data.transactionTime = dt[1] || null
    }

    // 商户处理：如果选择的是 'new'，则设置 merchantName 让后端自动创建
    if (data.merchantId === 'new') {
      data.merchantId = null
      data.autoCreateMerchant = true
    } else if (data.merchantId) {
      data.merchantName = undefined
      data.autoCreateMerchant = undefined
    }

    if (extFieldsList.value.length > 0) {
      const valid = extFieldsList.value.filter(item => item.key && item.key.trim())
      if (valid.length > 0) {
        data.extFields = {}
        valid.forEach(item => { data.extFields[item.key.trim()] = item.value })
      }
    } else {
      data.extFields = undefined
    }

    if (isEdit.value) {
      await transactionApi.update(route.params.id, data)
      message.success('更新成功')
    } else {
      await transactionApi.create(data)
      message.success('创建成功')
    }
    router.push('/transactions')
  } catch (e) {
    if (e?.errors) return // validation error
    message.error('操作失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  const [accRes, catRes] = await Promise.all([
    accountApi.list(true),
    categoryApi.tree(null)
  ])
  accountOptions.value = accRes.data.map(a => ({ label: a.name, value: a.id }))

  // 保持分类的树形结构
  allCategoryTree.value = catRes.data

  // 加载编辑数据
  if (isEdit.value) {
    try {
      const res = await transactionApi.get(route.params.id)
      const t = res.data
      form.value = {
        type: t.type,
        amount: t.amount,
        currency: t.currency,
        accountId: t.accountId,
        targetAccountId: t.targetAccountId,
        categoryId: t.categoryId,
        merchantId: t.merchantId,
        merchantName: t.merchantName || '',
        transactionDate: t.transactionDate + (t.transactionTime ? 'T' + t.transactionTime : 'T00:00:00'),
        note: t.note,
        tags: t.tags || [],
        extFields: t.extFields || {}
      }
      // 如果有商户，加载商户选项
      if (t.merchantId && t.merchantName) {
        merchantOptions.value = [{ label: t.merchantName, value: t.merchantId }]
      }
      extFieldsList.value = t.extFields ? Object.entries(t.extFields).map(([k, v]) => ({ key: k, value: String(v) })) : []
    } catch (e) {
      message.error('加载交易详情失败')
    }
  }
})
</script>
