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

      <n-form-item label="日期" path="transactionDate">
        <n-date-picker v-model:value="form.transactionDate" type="date" style="width: 100%" />
      </n-form-item>

      <n-form-item label="时间" path="transactionTime">
        <n-time-picker v-model:value="form.transactionTime" style="width: 100%" />
      </n-form-item>

      <n-form-item label="备注" path="note">
        <n-input v-model:value="form.note" placeholder="可选备注" />
      </n-form-item>

      <n-form-item label="标签" path="tags">
        <n-dynamic-tags v-model:value="form.tags" />
      </n-form-item>

      <n-form-item label="扩展字段">
        <n-input
          v-model:value="extFieldsStr"
          type="textarea"
          placeholder='{"merchant": "商户名", "location": "地点"}'
          :autosize="{ minRows: 2, maxRows: 4 }"
        />
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
import { transactionApi, accountApi, categoryApi } from '@/api'
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
  transactionDate: dayjs().format('YYYY-MM-DD'),
  transactionTime: dayjs().format('HH:mm:ss'),
  note: '',
  tags: [],
  extFields: {}
})

const extFieldsStr = ref('')

const rules = {
  type: { required: true, message: '请选择类型', trigger: 'change' },
  amount: { required: true, type: 'number', message: '请输入金额', trigger: 'blur' },
  accountId: { required: true, type: 'number', message: '请选择账户', trigger: 'change' },
  categoryId: { required: true, message: '请选择分类', trigger: 'change' },
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

async function handleSubmit() {
  try {
    await formRef.value?.validate()
    submitting.value = true

    const data = { ...form.value }
    // cascader 可能返回数组，取最后一个值（叶子节点）
    if (Array.isArray(data.categoryId)) {
      data.categoryId = data.categoryId[data.categoryId.length - 1]
    }
    if (extFieldsStr.value.trim()) {
      try {
        data.extFields = JSON.parse(extFieldsStr.value)
      } catch (e) {
        message.error('扩展字段 JSON 格式错误')
        return
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
        categoryId: t.categoryId, // 后端返回的是数字，cascader 能接受
        transactionDate: t.transactionDate,
        transactionTime: t.transactionTime,
        note: t.note,
        tags: t.tags || [],
        extFields: t.extFields || {}
      }
      extFieldsStr.value = t.extFields ? JSON.stringify(t.extFields, null, 2) : ''
    } catch (e) {
      message.error('加载交易详情失败')
    }
  }
})
</script>
