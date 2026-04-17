<template>
  <div class="alert-rule-page">
    <n-card title="预警规则管理">
      <template #header-extra>
        <n-button type="primary" @click="showCreateModal = true">
          <template #icon>
            <n-icon><PlusOutlined /></n-icon>
          </template>
          新增规则
        </n-button>
      </template>

      <n-data-table
        :columns="columns"
        :data="rules"
        :loading="loading"
        :row-key="row => row.id"
      />
    </n-card>

    <!-- 创建/编辑弹窗 -->
    <n-modal v-model:show="showCreateModal" preset="card" title="新增预警规则" style="width: 500px">
      <n-form ref="formRef" :model="formData" :rules="rules" label-placement="left" label-width="100">
        <n-form-item label="规则名称" path="name">
          <n-input v-model:value="formData.name" placeholder="请输入规则名称" />
        </n-form-item>
        <n-form-item label="规则类型" path="type">
          <n-select v-model:value="formData.type" :options="typeOptions" placeholder="选择规则类型" />
        </n-form-item>
        <n-form-item label="通知渠道" path="notifyChannel">
          <n-select v-model:value="formData.notifyChannel" :options="channelOptions" placeholder="选择通知渠道" />
        </n-form-item>

        <!-- 根据类型显示不同配置 -->
        <n-form-item v-if="formData.type === 1" label="阈值百分比" path="threshold">
          <n-input-number v-model:value="formData.threshold" :min="1" :max="100" placeholder="如 80 表示80%" />
        </n-form-item>
        <n-form-item v-if="formData.type === 2" label="大额金额" path="maxAmount">
          <n-input-number v-model:value="formData.maxAmount" :min="1" placeholder="超过此金额触发预警" />
        </n-form-item>
        <n-form-item v-if="formData.type === 3" label="日消费上限" path="dailyLimit">
          <n-input-number v-model:value="formData.dailyLimit" :min="1" placeholder="每日消费超过此金额预警" />
        </n-form-item>
        <n-form-item v-if="formData.type === 5" label="提前天数" path="advanceDays">
          <n-input-number v-model:value="formData.advanceDays" :min="1" :max="30" placeholder="还款日前几天提醒" />
        </n-form-item>
        <n-form-item v-if="formData.type === 6" label="提前天数" path="advanceDays">
          <n-input-number v-model:value="formData.advanceDays" :min="1" :max="30" placeholder="账单日前几天提醒" />
        </n-form-item>
        <n-form-item v-if="formData.type === 7" label="检查日" path="checkDay">
          <n-input-number v-model:value="formData.checkDay" :min="1" :max="28" placeholder="每月几号检查" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showCreateModal = false">取消</n-button>
          <n-button type="primary" @click="handleCreate" :loading="submitting">保存</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, h, onMounted } from 'vue'
import { NButton, NTag, NSpace, NIcon, useMessage } from 'naive-ui'
import { PlusOutlined } from '@vicons/antd'
import request from '../api/request'

const message = useMessage()
const loading = ref(false)
const rules = ref([])
const showCreateModal = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const formData = ref({
  name: '',
  type: null,
  notifyChannel: 'TELEGRAM',
  threshold: 80,
  maxAmount: 1000,
  dailyLimit: 500,
  advanceDays: 3,
  checkDay: 25
})

const typeOptions = [
  { label: '预算阈值预警', value: 1 },
  { label: '单笔大额预警', value: 2 },
  { label: '日消费上限', value: 3 },
  { label: '周消费异常', value: 4 },
  { label: '信用卡还款提醒', value: 5 },
  { label: '周期账单提醒', value: 6 },
  { label: '预算未设定提醒', value: 7 }
]

const channelOptions = [
  { label: 'Telegram', value: 'TELEGRAM' },
  { label: '应用内通知', value: 'APP' }
]

const columns = [
  { title: 'ID', key: 'id', width: 60 },
  { title: '规则名称', key: 'name' },
  { title: '类型', key: 'typeName' },
  {
    title: '配置',
    key: 'config',
    ellipsis: { tooltip: true },
    render: row => {
      try {
        const config = JSON.parse(row.config)
        if (row.type === 1) return `阈值: ${config.threshold_pct}%`
        if (row.type === 2) return `大额: ¥${config.max_amount}`
        if (row.type === 3) return `日限: ¥${config.daily_limit}`
        if (row.type === 5) return `提前 ${config.advance_days} 天`
        if (row.type === 6) return `提前 ${config.advance_days} 天`
        if (row.type === 7) return `${config.check_day} 号检查`
        return row.config
      } catch {
        return row.config
      }
    }
  },
  { title: '通知渠道', key: 'notifyChannel' },
  {
    title: '状态',
    key: 'isActive',
    render: row => h(NTag, {
      type: row.isActive ? 'success' : 'default',
      bordered: false
    }, { default: () => row.isActive ? '启用' : '停用' })
  },
  {
    title: '操作',
    key: 'actions',
    width: 150,
    render: row => h(NSpace, null, {
      default: () => [
        h(NButton, {
          size: 'small',
          type: row.isActive ? 'warning' : 'success',
          onClick: () => handleToggle(row)
        }, { default: () => row.isActive ? '停用' : '启用' }),
        h(NButton, {
          size: 'small',
          type: 'error',
          onClick: () => handleDelete(row)
        }, { default: () => '删除' })
      ]
    })
  }
]

const loadRules = async () => {
  loading.value = true
  try {
    const res = await request.get('/alert-rules')
    rules.value = res.data || []
  } catch (e) {
    message.error('加载失败: ' + e.message)
  }
  loading.value = false
}

const handleCreate = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    // 根据类型构建配置JSON
    let config = {}
    if (formData.value.type === 1) {
      config = { threshold_pct: formData.value.threshold }
    } else if (formData.value.type === 2) {
      config = { max_amount: String(formData.value.maxAmount) }
    } else if (formData.value.type === 3) {
      config = { daily_limit: String(formData.value.dailyLimit) }
    } else if (formData.value.type === 5) {
      config = { advance_days: formData.value.advanceDays }
    } else if (formData.value.type === 6) {
      config = { advance_days: formData.value.advanceDays }
    } else if (formData.value.type === 7) {
      config = { check_day: formData.value.checkDay }
    }

    const payload = {
      name: formData.value.name,
      type: formData.value.type,
      config: JSON.stringify(config),
      notifyChannel: formData.value.notifyChannel,
      isActive: true
    }

    await request.post('/alert-rules', payload)
    message.success('创建成功')
    showCreateModal.value = false
    loadRules()

    // 重置表单
    formData.value = {
      name: '',
      type: null,
      notifyChannel: 'TELEGRAM',
      threshold: 80,
      maxAmount: 1000,
      dailyLimit: 500,
      advanceDays: 3,
      checkDay: 25
    }
  } catch (e) {
    message.error('创建失败: ' + e.message)
  }
  submitting.value = false
}

const handleToggle = async (row) => {
  try {
    await request.post(`/alert-rules/${row.id}/toggle`)
    message.success(row.isActive ? '已停用' : '已启用')
    loadRules()
  } catch (e) {
    message.error('操作失败: ' + e.message)
  }
}

const handleDelete = async (row) => {
  try {
    await request.delete(`/alert-rules/${row.id}`)
    message.success('已删除')
    loadRules()
  } catch (e) {
    message.error('删除失败: ' + e.message)
  }
}

onMounted(() => {
  loadRules()
})
</script>

<style scoped>
.alert-rule-page {
  padding: 20px;
}
</style>