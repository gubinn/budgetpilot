<template>
  <div class="mobile-alert-rules">
    <div v-if="rules.length" class="rule-list">
      <div v-for="rule in rules" :key="rule.id" class="rule-card">
        <div class="rule-header">
          <span class="rule-name">{{ rule.name }}</span>
          <n-tag :type="rule.isActive ? 'success' : 'default'" size="tiny" :bordered="false">
            {{ rule.isActive ? '启用' : '停用' }}
          </n-tag>
        </div>
        <div class="rule-type">{{ rule.typeName }}</div>
        <div class="rule-config">{{ rule.config }}</div>
        <div class="rule-channel">通知: {{ rule.notifyChannel }}</div>
        <div class="rule-actions">
          <n-button size="small" secondary :type="rule.isActive ? 'warning' : 'success'" @click="handleToggle(rule)">
            {{ rule.isActive ? '停用' : '启用' }}
          </n-button>
          <n-button size="small" secondary type="error" @click="handleDelete(rule)">删除</n-button>
        </div>
      </div>
    </div>
    <n-empty v-else description="暂无预警规则" />

    <!-- 创建弹窗 -->
    <n-drawer v-model:show="showCreateModal" :height="'85vh'" placement="bottom">
      <n-drawer-content title="新增预警规则" closable>
        <n-form ref="formRef" :model="formData" label-placement="top" label-width="auto">
          <n-form-item label="规则名称">
            <n-input v-model:value="formData.name" placeholder="请输入规则名称" />
          </n-form-item>
          <n-form-item label="规则类型">
            <n-select v-model:value="formData.type" :options="typeOptions" />
          </n-form-item>
          <n-form-item label="通知渠道">
            <n-select v-model:value="formData.notifyChannel" :options="channelOptions" />
          </n-form-item>

          <n-form-item v-if="formData.type === 1" label="阈值百分比">
            <n-input-number v-model:value="formData.threshold" :min="1" :max="100" placeholder="如 80" />
          </n-form-item>
          <n-form-item v-if="formData.type === 2" label="大额金额">
            <n-input-number v-model:value="formData.maxAmount" :min="1" placeholder="超过此金额触发" />
          </n-form-item>
          <n-form-item v-if="formData.type === 3" label="日消费上限">
            <n-input-number v-model:value="formData.dailyLimit" :min="1" placeholder="每日消费上限" />
          </n-form-item>
          <n-form-item v-if="formData.type === 5 || formData.type === 6" label="提前天数">
            <n-input-number v-model:value="formData.advanceDays" :min="1" :max="30" />
          </n-form-item>
          <n-form-item v-if="formData.type === 7" label="检查日">
            <n-input-number v-model:value="formData.checkDay" :min="1" :max="28" />
          </n-form-item>
          <n-form-item v-if="formData.type === 8" label="数量阈值">
            <n-input-number v-model:value="formData.minCount" :min="1" />
          </n-form-item>
        </n-form>
        <template #footer>
          <n-space>
            <n-button type="primary" :loading="submitting" @click="handleCreate">保存</n-button>
            <n-button @click="showCreateModal = false">取消</n-button>
          </n-space>
        </template>
      </n-drawer-content>
    </n-drawer>

    <div class="fab">
      <n-button type="primary" round size="large" @click="showCreateModal = true">
        <template #icon><n-icon><AddOutline /></n-icon></template>
        新增规则
      </n-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useMessage } from 'naive-ui'
import { PlusOutlined } from '@vicons/antd'
import { AddOutline } from '@vicons/ionicons5'
import request from '@/api/request'

const message = useMessage()
const loading = ref(false)
const rules = ref([])
const showCreateModal = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const formData = ref({
  name: '', type: null, notifyChannel: 'TELEGRAM',
  threshold: 80, maxAmount: 1000, dailyLimit: 500,
  advanceDays: 3, checkDay: 25, minCount: 1
})

const typeOptions = [
  { label: '预算阈值预警', value: 1 },
  { label: '单笔大额预警', value: 2 },
  { label: '日消费上限', value: 3 },
  { label: '周消费异常', value: 4 },
  { label: '信用卡还款提醒', value: 5 },
  { label: '周期账单提醒', value: 6 },
  { label: '预算未设定提醒', value: 7 },
  { label: '待确认交易提醒', value: 8 }
]

const channelOptions = [
  { label: 'Telegram', value: 'TELEGRAM' },
  { label: '应用内通知', value: 'APP' }
]

const loadRules = async () => {
  loading.value = true
  try {
    const res = await request.get('/alert-rules')
    rules.value = res.data || []
  } catch (e) { message.error('加载失败: ' + e.message) }
  loading.value = false
}

const handleCreate = async () => {
  if (!formData.value.name) { message.error('请输入规则名称'); return }
  submitting.value = true
  try {
    let config = {}
    if (formData.value.type === 1) config = { threshold_pct: formData.value.threshold }
    else if (formData.value.type === 2) config = { max_amount: String(formData.value.maxAmount) }
    else if (formData.value.type === 3) config = { daily_limit: String(formData.value.dailyLimit) }
    else if (formData.value.type === 5) config = { advance_days: formData.value.advanceDays }
    else if (formData.value.type === 6) config = { advance_days: formData.value.advanceDays }
    else if (formData.value.type === 7) config = { check_day: formData.value.checkDay }
    else if (formData.value.type === 8) config = { min_count: formData.value.minCount }

    await request.post('/alert-rules', {
      name: formData.value.name, type: formData.value.type,
      config: JSON.stringify(config), notifyChannel: formData.value.notifyChannel, isActive: true
    })
    message.success('创建成功')
    showCreateModal.value = false
    loadRules()
    formData.value = { name: '', type: null, notifyChannel: 'TELEGRAM', threshold: 80, maxAmount: 1000, dailyLimit: 500, advanceDays: 3, checkDay: 25, minCount: 1 }
  } catch (e) { message.error('创建失败: ' + e.message) } finally { submitting.value = false }
}

const handleToggle = async (row) => {
  try {
    await request.post(`/alert-rules/${row.id}/toggle`)
    message.success(row.isActive ? '已停用' : '已启用')
    loadRules()
  } catch (e) { message.error('操作失败: ' + e.message) }
}

const handleDelete = async (row) => {
  try {
    await request.delete(`/alert-rules/${row.id}`)
    message.success('已删除')
    loadRules()
  } catch (e) { message.error('删除失败: ' + e.message) }
}

onMounted(() => { loadRules() })
</script>

<style scoped>
.mobile-alert-rules { padding-bottom: 70px; }

.rule-list { display: flex; flex-direction: column; gap: 10px; }

.rule-card { background: #fff; border-radius: 10px; padding: 14px; }
.rule-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.rule-name { font-size: 15px; font-weight: 600; }
.rule-type { font-size: 13px; color: #666; }
.rule-config { font-size: 13px; color: #555; margin: 4px 0; }
.rule-channel { font-size: 12px; color: #999; }
.rule-actions { display: flex; gap: 8px; margin-top: 10px; }

.fab {
  position: fixed;
  bottom: calc(70px + env(safe-area-inset-bottom));
  right: 16px;
  z-index: 50;
}
</style>
