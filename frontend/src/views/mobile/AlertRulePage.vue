<template>
  <div class="mobile-alert-rules m-page">
    <div v-if="rules.length" class="rule-list">
      <div v-for="rule in rules" :key="rule.id" class="rule-card">
        <div class="rule-header">
          <span class="rule-name m-ellipsis">{{ rule.name }}</span>
          <div class="rule-header-right">
            <n-tag :type="rule.isActive ? 'success' : 'default'" size="tiny" :bordered="false">
              {{ rule.isActive ? '启用' : '停用' }}
            </n-tag>
            <n-dropdown trigger="click" placement="bottom-end" :options="ruleActions(rule)" @select="key => handleAction(key, rule)">
              <button class="m-more" aria-label="规则操作">
                <n-icon :size="18"><EllipsisHorizontal /></n-icon>
              </button>
            </n-dropdown>
          </div>
        </div>
        <div class="rule-body">
          <div class="rule-desc">
            <template v-if="rule.typeName !== rule.name">{{ rule.typeName }}<template v-if="formatConfig(rule)"> · </template></template>
            {{ formatConfig(rule) }}
          </div>
          <div class="rule-channel">通知 · {{ channelLabel(rule.notifyChannel) }}</div>
        </div>
      </div>
    </div>
    <div v-else class="m-card">
      <div class="m-empty-cta">
        <div class="cta-icon"><n-icon :size="26"><NotificationsOutline /></n-icon></div>
        <div class="cta-text">还没有预警规则<br />预算超支、大额支出都可以自动提醒</div>
        <n-button type="primary" round @click="showCreateModal = true">新增规则</n-button>
      </div>
    </div>

    <!-- 创建弹窗 -->
    <n-drawer v-model:show="showCreateModal" :height="'85vh'" placement="bottom">
      <n-drawer-content title="新增预警规则" closable>
        <n-form ref="formRef" :model="formData" label-placement="top" label-width="auto" class="m-form">
          <n-form-item label="规则名称">
            <n-input v-model:value="formData.name" placeholder="请输入规则名称" size="large" />
          </n-form-item>
          <n-form-item label="规则类型">
            <n-select v-model:value="formData.type" :options="typeOptions" size="large" />
          </n-form-item>
          <n-form-item label="通知渠道">
            <n-select v-model:value="formData.notifyChannel" :options="channelOptions" size="large" />
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
          <n-form-item v-if="formData.type === 4" label="偏离阈值">
            <n-input-number v-model:value="formData.deviationPct" :min="1" :max="500" placeholder="如 50 表示偏离 50%" />
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
          <div class="m-sheet-actions">
            <n-button type="primary" :loading="submitting" @click="handleCreate">保存</n-button>
            <n-button quaternary @click="showCreateModal = false">取消</n-button>
          </div>
        </template>
      </n-drawer-content>
    </n-drawer>

    <div v-if="rules.length" class="fab">
      <n-button type="primary" circle size="large" aria-label="新增规则" @click="showCreateModal = true">
        <template #icon><n-icon :size="24"><AddOutline /></n-icon></template>
      </n-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useMessage } from 'naive-ui'
import { PlusOutlined } from '@vicons/antd'
import { AddOutline, EllipsisHorizontal, NotificationsOutline } from '@vicons/ionicons5'
import request from '@/api/request'

const message = useMessage()
const loading = ref(false)
const rules = ref([])
const showCreateModal = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const formData = ref({
  name: '', type: null, notifyChannel: '',
  threshold: 80, maxAmount: 1000, dailyLimit: 500,
  deviationPct: 50, advanceDays: 3, checkDay: 25, minCount: 1
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
  { label: '跟随系统默认', value: '' },
  { label: 'Telegram', value: 'TELEGRAM' },
  { label: 'WhatsApp', value: 'WHATSAPP' },
  { label: '两者都发', value: 'BOTH' },
  { label: '应用内通知', value: 'APP' }
]
const channelLabel = (value) => ({
  TELEGRAM: 'Telegram',
  WHATSAPP: 'WhatsApp',
  BOTH: '两者都发',
  APP: '应用内'
}[value] || value || '-')

function formatConfig(rule) {
  let config = rule.config
  if (typeof config === 'string') {
    try { config = JSON.parse(config) } catch { return '' }
  }
  if (!config || typeof config !== 'object') return ''
  switch (rule.type) {
    case 1: return `预算使用达到 ${config.threshold_pct}%`
    case 2: return `单笔超过 ¥${config.max_amount}`
    case 3: return `日消费超过 ¥${config.daily_limit}`
    case 4: return `周消费偏离 ${config.deviation_pct}%`
    case 5:
    case 6: return `提前 ${config.advance_days} 天提醒`
    case 7: return `每月 ${config.check_day} 日检查`
    case 8: return `待确认超过 ${config.min_count} 笔`
    default: return ''
  }
}

const ruleActions = (rule) => [
  { label: rule.isActive ? '停用规则' : '启用规则', key: 'toggle' },
  { label: '删除规则', key: 'delete' }
]
const handleAction = (key, rule) => {
  if (key === 'toggle') handleToggle(rule)
  else if (key === 'delete') handleDelete(rule)
}

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
    else if (formData.value.type === 4) config = { deviation_pct: formData.value.deviationPct }
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
    formData.value = { name: '', type: null, notifyChannel: '', threshold: 80, maxAmount: 1000, dailyLimit: 500, deviationPct: 50, advanceDays: 3, checkDay: 25, minCount: 1 }
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
.mobile-alert-rules { padding-bottom: calc(88px + env(safe-area-inset-bottom, 0px)); }

.rule-list { display: flex; flex-direction: column; gap: 10px; }

.rule-card {
  background: #fff;
  border-radius: var(--m-radius);
  padding: 14px 12px 14px 16px;
  box-shadow: var(--bp-shadow);
}
.rule-header { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.rule-header-right { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
.rule-name { font-size: 15px; font-weight: 600; }
.rule-body { margin-top: 8px; display: flex; flex-direction: column; gap: 4px; }
.rule-desc { font-size: 13px; color: #64748b; }
.rule-channel { font-size: 12px; color: #b0b8c4; }

.fab {
  position: fixed;
  bottom: calc(76px + env(safe-area-inset-bottom));
  right: 16px;
  z-index: 80;
}
</style>
