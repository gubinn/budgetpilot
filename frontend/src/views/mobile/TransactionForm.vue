<template>
  <div class="mobile-form">
    <n-card :title="isEdit ? '编辑交易' : '新增交易'" size="small">
      <n-form ref="formRef" :model="form" label-placement="top" label-width="auto">
        <!-- 类型选择 -->
        <n-form-item label="类型">
          <n-radio-group v-model:value="form.type" class="type-group">
            <n-radio-button :value="1" class="type-btn">支出</n-radio-button>
            <n-radio-button :value="2" class="type-btn">收入</n-radio-button>
            <n-radio-button :value="3" class="type-btn">转账</n-radio-button>
          </n-radio-group>
        </n-form-item>

        <!-- 金额 -->
        <n-form-item label="金额">
          <n-input-number v-model:value="form.amount" :min="0.01" :precision="2" placeholder="0.00" size="large" style="width: 100%">
            <template #prefix>¥</template>
          </n-input-number>
        </n-form-item>

        <!-- 币种 + 账户 -->
        <div class="inline-row">
          <n-form-item label="币种" class="inline-item">
            <n-select v-model:value="form.currency" :options="currencyOptions" />
          </n-form-item>
          <n-form-item label="账户" class="inline-item">
            <n-select v-model:value="form.accountId" :options="accountOptions" placeholder="选择" filterable />
          </n-form-item>
        </div>

        <!-- 目标账户（转账时显示） -->
        <n-form-item v-if="form.type === 3" label="目标账户">
          <n-select v-model:value="form.targetAccountId" :options="accountOptions" placeholder="选择目标账户" filterable />
        </n-form-item>

        <!-- 分类 -->
        <n-form-item label="分类">
          <n-cascader
            v-model:value="form.categoryId"
            :options="categoryTree"
            placeholder="选择分类"
            check-strategy="child"
            cascade="false"
            show-path
            filterable
            style="width: 100%"
          />
        </n-form-item>

        <!-- 商户（非转账时显示） -->
        <n-form-item v-if="form.type !== 3" label="商户">
          <n-select
            v-model:value="form.merchantId"
            :options="merchantOptions"
            placeholder="输入或选择"
            filterable
            clearable
            remote
            :loading="merchantLoading"
            @search="handleMerchantSearch"
          />
        </n-form-item>

        <!-- 日期时间 -->
        <n-form-item label="日期时间">
          <n-date-picker v-model:formatted-value="form.transactionDate" type="datetime" format="yyyy-MM-dd HH:mm:ss" style="width: 100%" />
        </n-form-item>

        <!-- 备注 -->
        <n-form-item label="备注">
          <n-input v-model:value="form.note" placeholder="可选" type="textarea" :autosize="{ minRows: 2 }" />
        </n-form-item>

        <!-- 标签 -->
        <n-form-item label="标签">
          <n-dynamic-tags v-model:value="form.tags" />
        </n-form-item>

        <!-- 扩展字段 -->
        <n-form-item label="扩展字段">
          <n-dynamic-input v-model:value="extFieldsList" :on-create="() => ({ key: '', value: '' })">
            <template #default="{ index }">
              <n-input v-model:value="extFieldsList[index].key" placeholder="键" style="width: 40%" />
              <n-input v-model:value="extFieldsList[index].value" placeholder="值" style="width: 55%; margin-left: 2%" />
            </template>
          </n-dynamic-input>
        </n-form-item>

        <!-- 按钮 -->
        <n-form-item>
          <n-space>
            <n-button type="primary" :loading="submitting" @click="handleSubmit" block>
              {{ isEdit ? '保存' : '提交' }}
            </n-button>
            <n-button @click="router.back()" block>取消</n-button>
          </n-space>
        </n-form-item>
      </n-form>
    </n-card>
  </div>
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
  transactionDate: dayjs().format('YYYY-MM-DD HH:mm:ss'),
  note: '',
  tags: [],
  extFields: {}
})

const extFieldsList = ref([])

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

watch(() => form.value.type, () => {
  if (form.value.categoryId) {
    const found = findInTree(categoryTree.value, form.value.categoryId)
    if (!found) form.value.categoryId = null
  }
})

function findInTree(tree, id) {
  for (const node of tree) {
    if (node.value === id) return true
    if (node.children?.length && findInTree(node.children, id)) return true
  }
  return false
}

async function handleMerchantSearch(keyword) {
  if (merchantSearchTimer.value) clearTimeout(merchantSearchTimer.value)
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
      if (options.length === 0) {
        options.push({ label: `创建新商户 "${keyword.trim()}"`, value: 'new' })
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
  submitting.value = true
  try {
    const data = { ...form.value }
    if (Array.isArray(data.categoryId)) {
      data.categoryId = data.categoryId[data.categoryId.length - 1]
    }
    if (data.transactionDate) {
      const parts = data.transactionDate.split(' ')
      data.transactionDate = parts[0]
      data.transactionTime = parts[1] || null
    }
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
    if (e?.errors) return
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
  allCategoryTree.value = catRes.data

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
        transactionDate: t.transactionDate + (t.transactionTime ? ' ' + t.transactionTime : ' 00:00:00'),
        note: t.note,
        tags: t.tags || [],
        extFields: t.extFields || {}
      }
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

<style scoped>
.mobile-form {
  padding: 0;
}

.type-group {
  display: flex;
  width: 100%;
}
.type-btn {
  flex: 1;
  text-align: center;
}

.inline-row {
  display: flex;
  gap: 12px;
}
.inline-item {
  flex: 1;
  margin-bottom: 16px;
}

:deep(.n-card__header) {
  padding-bottom: 8px;
}
:deep(.n-card__content) {
  padding: 12px;
}
</style>
