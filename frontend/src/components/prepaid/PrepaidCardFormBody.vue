<template>
  <n-form ref="formRef" :model="form" :rules="rules" label-placement="top" class="card-form">
    <div class="form-grid">
      <n-form-item label="卡券名称" path="name" class="span-2">
        <n-input
          v-model:value="form.name"
          placeholder="如：星巴克星礼卡、全国电影通兑券"
          maxlength="60"
        />
      </n-form-item>

      <n-form-item label="发卡方" path="issuer">
        <n-input v-model:value="form.issuer" placeholder="如：星巴克、万达影城" maxlength="60" />
      </n-form-item>

      <n-form-item label="类型">
        <n-select v-model:value="form.kind" :options="kindOptions" />
      </n-form-item>

      <n-form-item label="计量单位">
        <n-radio-group v-model:value="form.unit">
          <n-radio-button :value="1">金额</n-radio-button>
          <n-radio-button :value="2">次数 / 张</n-radio-button>
        </n-radio-group>
      </n-form-item>

      <n-form-item :label="faceLabel">
        <n-input-number
          v-model:value="form.faceValue"
          :min="0"
          :precision="unitPrecision"
          :step="unitStep"
          :show-button="false"
          placeholder="可选"
          style="width: 100%"
          @update:value="onFaceChange"
        >
          <template v-if="form.unit === 1" #prefix>¥</template>
          <template v-else #suffix>次</template>
        </n-input-number>
      </n-form-item>

      <n-form-item :label="balanceLabel" path="balance">
        <n-input-number
          v-model:value="form.balance"
          :min="0"
          :precision="unitPrecision"
          :step="unitStep"
          :show-button="false"
          style="width: 100%"
          @update:value="balanceTouched = true"
        >
          <template v-if="form.unit === 1" #prefix>¥</template>
          <template v-else #suffix>次</template>
        </n-input-number>
      </n-form-item>

      <n-form-item label="有效期至">
        <n-date-picker
          v-model:formatted-value="form.expiryDate"
          type="date"
          value-format="yyyy-MM-dd"
          clearable
          placeholder="不填 = 长期有效"
          style="width: 100%"
        />
      </n-form-item>

      <n-form-item label="发放 / 购买日期">
        <n-date-picker
          v-model:formatted-value="form.issueDate"
          type="date"
          value-format="yyyy-MM-dd"
          clearable
          placeholder="可选"
          style="width: 100%"
        />
      </n-form-item>

      <n-form-item label="存放渠道">
        <n-select
          v-model:value="form.channel"
          :options="channelOptions"
          filterable
          tag
          clearable
          placeholder="微信小程序 / 支付宝 / 实体卡…"
        />
      </n-form-item>

      <n-form-item label="所在账号">
        <n-input
          v-model:value="form.accountHint"
          placeholder="如：尾号 1234 的手机号"
          maxlength="60"
        />
      </n-form-item>

      <n-form-item label="来源">
        <n-select v-model:value="form.source" :options="sourceOptions" />
      </n-form-item>

      <n-form-item label="备注" class="span-2">
        <n-input
          v-model:value="form.note"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 3 }"
          maxlength="200"
          show-count
          placeholder="比如兑换码位置、使用限制，别放完整卡号和密码"
        />
      </n-form-item>
    </div>

    <n-form-item v-if="actions">
      <n-space>
        <n-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存' : '记一张' }}
        </n-button>
        <n-button @click="emit('cancel')">取消</n-button>
      </n-space>
    </n-form-item>
  </n-form>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useMessage } from 'naive-ui'
import { prepaidCardApi } from '@/api'

const props = defineProps({
  // 传入卡券 ID 表示编辑，空值表示新增
  cardId: { type: [String, Number], default: null },
  // 弹窗场景把按钮放到 footer，传 false
  actions: { type: Boolean, default: true }
})
const emit = defineEmits(['saved', 'cancel'])

const message = useMessage()
const formRef = ref(null)
const submitting = ref(false)
const balanceTouched = ref(false)

const isEdit = computed(() => props.cardId != null && props.cardId !== '')

const kindOptions = [
  { label: '储值卡', value: 1 },
  { label: '礼品卡', value: 2 },
  { label: '兑换券', value: 3 },
  { label: '次数卡', value: 4 }
]

const channelOptions = [
  { label: '微信小程序', value: '微信小程序' },
  { label: '支付宝', value: '支付宝' },
  { label: '实体卡', value: '实体卡' },
  { label: '短信 / 邮件', value: '短信/邮件' },
  { label: 'APP', value: 'APP' },
  { label: '其他', value: '其他' }
]

const sourceOptions = [
  { label: '单位发放', value: 1 },
  { label: '自己购买', value: 2 },
  { label: '他人赠送', value: 3 },
  { label: '其他', value: 4 }
]

const form = ref(defaultForm())

function defaultForm() {
  return {
    name: '',
    issuer: '',
    channel: '微信小程序',
    accountHint: '',
    kind: 1,
    unit: 1,
    faceValue: null,
    balance: 0,
    issueDate: null,
    expiryDate: null,
    source: 1,
    note: ''
  }
}

const rules = {
  name: { required: true, message: '请输入卡券名称', trigger: ['input', 'blur'] },
  issuer: { required: true, message: '请输入发卡方', trigger: ['input', 'blur'] }
}

const faceLabel = computed(() => form.value.unit === 2 ? '总次数' : '面值')
const balanceLabel = computed(() => form.value.unit === 2 ? '剩余次数' : '当前余额')
const unitPrecision = computed(() => form.value.unit === 2 ? 0 : 2)
const unitStep = computed(() => form.value.unit === 2 ? 1 : 0.01)

// 新增时，填面值顺手把余额带出来；用户改过余额后就不再覆盖
function onFaceChange(value) {
  if (!isEdit.value && !balanceTouched.value) {
    form.value.balance = value
  }
}

// 切到次数卡时把小数抹掉，避免后端拒绝
watch(() => form.value.unit, (unit) => {
  if (unit !== 2) return
  if (form.value.faceValue != null) {
    form.value.faceValue = Math.floor(Number(form.value.faceValue))
  }
  if (form.value.balance != null) {
    form.value.balance = Math.floor(Number(form.value.balance))
  }
})

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    const payload = {
      name: form.value.name.trim(),
      issuer: form.value.issuer.trim(),
      channel: form.value.channel || null,
      accountHint: form.value.accountHint?.trim() || null,
      kind: form.value.kind,
      unit: form.value.unit,
      currency: 'CNY',
      faceValue: form.value.faceValue,
      balance: form.value.balance ?? 0,
      issueDate: form.value.issueDate || null,
      expiryDate: form.value.expiryDate || null,
      source: form.value.source,
      note: form.value.note?.trim() || null
    }

    if (isEdit.value) {
      await prepaidCardApi.update(props.cardId, payload)
      message.success('已保存')
    } else {
      await prepaidCardApi.create(payload)
      message.success('已记入卡券包')
    }
    emit('saved', payload)
  } catch (e) {
    message.error(e?.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  if (!isEdit.value) return
  try {
    const res = await prepaidCardApi.get(props.cardId)
    const card = res.data
    form.value = {
      name: card.name || '',
      issuer: card.issuer || '',
      channel: card.channel || '',
      accountHint: card.accountHint || '',
      kind: card.kind || 1,
      unit: card.unit || 1,
      faceValue: card.faceValue != null ? Number(card.faceValue) : null,
      balance: card.balance != null ? Number(card.balance) : 0,
      issueDate: card.issueDate || null,
      expiryDate: card.expiryDate || null,
      source: card.source || 4,
      note: card.note || ''
    }
    balanceTouched.value = true
  } catch {
    message.error('加载卡券失败')
  }
})

defineExpose({ submit: handleSubmit, submitting })
</script>

<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 18px;
}
.span-2 {
  grid-column: span 2;
}
@media (max-width: 640px) {
  .form-grid {
    grid-template-columns: minmax(0, 1fr);
  }
  .span-2 {
    grid-column: span 1;
  }
}
</style>