<template>
  <div class="mobile-settings">
    <!-- 定时提醒渠道（全局） -->
    <div class="section-card" v-if="auth.isAdmin">
      <h3 class="section-title">定时提醒渠道</h3>
      <n-form label-placement="top" label-width="auto" class="m-form">
        <n-form-item label="渠道">
          <n-select v-model:value="config.notify_channel" :options="notifyChannelOptions" />
        </n-form-item>
        <n-button type="primary" size="small" @click="saveNotifyChannel">保存</n-button>
      </n-form>
      <div class="config-hint">系统定时提醒默认使用该渠道；单条预警规则可在「预警规则」页覆盖。月度总结固定使用该渠道。</div>
    </div>

    <!-- 我的默认通知渠道 -->
    <div class="section-card">
      <h3 class="section-title">默认通知渠道</h3>
      <n-form label-placement="top" label-width="auto" class="m-form">
        <n-form-item label="渠道">
          <n-select v-model:value="myConfig.notify_channel" :options="myNotifyChannelOptions" placeholder="请选择默认渠道" />
        </n-form-item>
        <n-button type="primary" size="small" @click="saveMyNotifyChannel">保存</n-button>
      </n-form>
      <div class="config-hint">公网登录验证码按该渠道发送；选择「两者都发」时需要同时配置 Telegram 和 WhatsApp。</div>
    </div>

    <!-- 只读共享账本 -->
    <div class="section-card">
      <h3 class="section-title">只读共享</h3>
      <ReadonlySharePanel />
    </div>

    <!-- 我的 Telegram -->
    <div class="section-card">
      <h3 class="section-title">我的 Telegram 通知</h3>
      <n-form label-placement="top" label-width="auto" class="m-form">
        <n-form-item label="Bot Token">
          <n-input v-model:value="myConfig.telegram_bot_token" type="password" show-password-on="click" placeholder="你自己的 Bot Token" />
        </n-form-item>
        <n-form-item label="Chat ID">
          <n-input v-model:value="myConfig.telegram_chat_id" placeholder="你自己的 Chat ID" />
        </n-form-item>
        <n-button type="primary" size="small" @click="saveMyTelegram">保存</n-button>
      </n-form>
      <div class="config-hint">未配置则无法通过 Telegram 收公网验证码。</div>
    </div>

    <!-- 我的 WhatsApp -->
    <div class="section-card">
      <h3 class="section-title">我的 WhatsApp 通知</h3>
      <n-form label-placement="top" label-width="auto" class="m-form">
        <n-form-item label="API Key">
          <n-input v-model:value="myConfig.whatsapp_api_key" type="password" show-password-on="click" placeholder="你自己的 Agent API Key" />
        </n-form-item>
        <n-form-item label="接收者 ID">
          <n-input v-model:value="myConfig.whatsapp_to" placeholder="user:<id>（不是手机号）" />
        </n-form-item>
        <n-button type="primary" size="small" @click="saveMyWhatsApp">保存</n-button>
      </n-form>
      <div class="config-hint">未配置则无法通过 WhatsApp 收公网验证码；WhatsApp 只能发给 Agent 创建者。</div>
    </div>

    <!-- 系统 Telegram（定时提醒，ADMIN） -->
    <div class="section-card" v-if="auth.isAdmin">
      <h3 class="section-title">系统 Telegram（定时提醒）</h3>
      <n-form label-placement="top" label-width="auto" class="m-form">
        <n-form-item label="Bot Token">
          <n-input v-model:value="config.telegram_bot_token" type="password" show-password-on="click" placeholder="输入 Bot Token" />
        </n-form-item>
        <n-form-item label="Chat ID">
          <n-input v-model:value="config.telegram_chat_id" placeholder="输入 Chat ID" />
        </n-form-item>
        <n-space>
          <n-button type="primary" size="small" @click="saveConfig('telegram_bot_token', config.telegram_bot_token); saveConfig('telegram_chat_id', config.telegram_chat_id)">保存</n-button>
          <n-button size="small" @click="testTelegram">推送测试</n-button>
        </n-space>
      </n-form>
      <div class="config-hint">仅用于系统定时提醒，不用于用户公网登录验证码。</div>
    </div>

    <!-- 系统 WhatsApp（定时提醒，ADMIN） -->
    <div class="section-card" v-if="auth.isAdmin">
      <h3 class="section-title">系统 WhatsApp（定时提醒）</h3>
      <n-form label-placement="top" label-width="auto" class="m-form">
        <n-form-item label="API Key">
          <n-input v-model:value="config.whatsapp_api_key" type="password" show-password-on="click" placeholder="Agent API Key（留空则使用环境变量）" />
        </n-form-item>
        <n-form-item label="接收者 ID">
          <n-input v-model:value="config.whatsapp_to" placeholder="user:<id>（不是手机号）" />
        </n-form-item>
        <n-space>
          <n-button type="primary" size="small" @click="saveWhatsApp">保存</n-button>
          <n-button size="small" @click="testWhatsApp">推送测试</n-button>
        </n-space>
      </n-form>
      <div class="config-hint">仅用于系统定时提醒，不用于用户公网登录验证码。</div>
    </div>

    <!-- 汇率 -->
    <div class="section-card" v-if="auth.isAdmin">
      <h3 class="section-title">汇率设置</h3>
      <n-form label-placement="top" label-width="auto" class="m-form">
        <n-form-item label="API Key">
          <n-input v-model:value="config.exchange_rate_api_key" type="password" show-password-on="click" placeholder="Key" />
        </n-form-item>
        <n-form-item label="支持币种">
          <n-dynamic-tags v-model:value="supportedCurrencies" />
        </n-form-item>
        <n-space>
          <n-button type="primary" size="small" @click="saveConfig('exchange_rate_api_key', config.exchange_rate_api_key); saveConfig('supported_currencies', supportedCurrencies.join(','))">保存</n-button>
          <n-button size="small" @click="refreshRates" :loading="refreshing">刷新汇率</n-button>
        </n-space>
      </n-form>
    </div>

    <!-- 汇率列表 -->
    <div v-if="rates.length" class="section-card">
      <h3 class="section-title">当前汇率</h3>
      <div v-for="r in rates" :key="r.targetCurrency" class="rate-row">
        <span>1 {{ r.targetCurrency }}</span>
        <span class="rate-value">¥ {{ Number(r.rate).toFixed(4) }}</span>
      </div>
    </div>

    <!-- API Key -->
    <div class="section-card">
      <h3 class="section-title">API Key</h3>
      <div class="api-key-display">
        <code>{{ apiKeyMasked }}</code>
      </div>
      <n-space style="margin-top: 8px">
        <n-button size="small" @click="toggleApiKeyVisibility">{{ apiKeyVisible ? '隐藏' : '显示' }}</n-button>
        <n-button size="small" @click="copyApiKey" :disabled="!apiKey">复制</n-button>
        <n-button size="small" type="warning" @click="regenerateApiKey">重新生成</n-button>
      </n-space>
    </div>

    <!-- 预警日志 -->
    <div class="section-card">
      <div class="section-header">
        <h3 class="section-title" style="margin: 0">预警日志</h3>
        <n-button size="tiny" @click="loadAlerts">刷新</n-button>
      </div>
      <div v-if="alerts.length > 0" class="alert-log-list">
        <div v-for="alert in alerts" :key="alert.id" class="alert-log-item">
          <n-tag :type="alert.isSent ? 'success' : 'error'" size="tiny">{{ alert.isSent ? '已推送' : '未推送' }}</n-tag>
          <div class="alert-log-content">
            <div class="alert-log-title">{{ alert.title }}</div>
            <div class="alert-log-desc">{{ alert.content }}</div>
          </div>
          <n-button v-if="!alert.isRead" size="tiny" @click="markRead(alert.id)">已读</n-button>
        </div>
      </div>
      <n-empty v-else description="暂无预警" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useMessage } from 'naive-ui'
import { systemApi, authApi, userApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import ReadonlySharePanel from '@/components/share/ReadonlySharePanel.vue'

const message = useMessage()
const auth = useAuthStore()
const config = ref({
  telegram_bot_token: '',
  telegram_chat_id: '',
  whatsapp_api_key: '',
  whatsapp_to: '',
  notify_channel: 'TELEGRAM',
  exchange_rate_api_key: ''
})
const notifyChannelOptions = [
  { label: 'Telegram', value: 'TELEGRAM' },
  { label: 'WhatsApp', value: 'WHATSAPP' },
  { label: '两者都发', value: 'BOTH' },
  { label: '仅应用内', value: 'APP' }
]
const myConfig = ref({
  notify_channel: '',
  telegram_bot_token: '',
  telegram_chat_id: '',
  whatsapp_api_key: '',
  whatsapp_to: ''
})
const myNotifyChannelOptions = [
  { label: 'Telegram', value: 'TELEGRAM' },
  { label: 'WhatsApp', value: 'WHATSAPP' },
  { label: '两者都发', value: 'BOTH' }
]
const supportedCurrencies = ref([])
const rates = ref([])
const alerts = ref([])
const refreshing = ref(false)
const apiKey = ref('')
const apiKeyVisible = ref(false)

async function loadConfig() {
  try {
    const res = await systemApi.config()
    config.value.telegram_bot_token = res.data.telegram_bot_token || ''
    config.value.telegram_chat_id = res.data.telegram_chat_id || ''
    config.value.whatsapp_api_key = res.data.whatsapp_api_key || ''
    config.value.whatsapp_to = res.data.whatsapp_to || ''
    config.value.notify_channel = res.data.notify_channel || 'TELEGRAM'
    config.value.exchange_rate_api_key = res.data.exchange_rate_api_key || ''
    const csv = res.data.supported_currencies || 'CNY,USD,EUR,GBP,JPY,HKD,SGD,THB,KRW'
    supportedCurrencies.value = csv.split(',').map(s => s.trim()).filter(Boolean)
  } catch (e) { console.error('Failed to load config', e) }
}

async function saveConfig(key, value) {
  try {
    await systemApi.setConfig(key, value)
    message.success('保存成功')
  } catch (e) { message.error('保存失败') }
}

async function testTelegram() {
  try {
    const res = await systemApi.testTelegram()
    message.success(res.message || '推送成功')
  } catch (e) { message.error('推送失败') }
}

async function saveWhatsApp() {
  try {
    await systemApi.setConfig('whatsapp_api_key', config.value.whatsapp_api_key || '')
    await systemApi.setConfig('whatsapp_to', config.value.whatsapp_to || '')
    message.success('保存成功')
  } catch (e) { message.error('保存失败') }
}

async function saveNotifyChannel() {
  try {
    await systemApi.setConfig('notify_channel', config.value.notify_channel || 'TELEGRAM')
    message.success('保存成功')
  } catch (e) { message.error('保存失败') }
}

async function loadMyNotify() {
  try {
    const res = await userApi.getMyConfig()
    myConfig.value.notify_channel = res.data.notify_channel || ''
    myConfig.value.telegram_bot_token = res.data.telegram_bot_token || ''
    myConfig.value.telegram_chat_id = res.data.telegram_chat_id || ''
    myConfig.value.whatsapp_api_key = res.data.whatsapp_api_key || ''
    myConfig.value.whatsapp_to = res.data.whatsapp_to || ''
  } catch (e) { console.error('Failed to load my notify config', e) }
}

async function saveMyNotifyChannel() {
  try {
    await userApi.setMyConfig('notify_channel', myConfig.value.notify_channel || '')
    message.success('保存成功')
  } catch (e) { message.error('保存失败') }
}

async function saveMyTelegram() {
  try {
    await userApi.setMyConfig('telegram_bot_token', myConfig.value.telegram_bot_token || '')
    await userApi.setMyConfig('telegram_chat_id', myConfig.value.telegram_chat_id || '')
    message.success('保存成功')
  } catch (e) { message.error('保存失败') }
}

async function saveMyWhatsApp() {
  try {
    await userApi.setMyConfig('whatsapp_api_key', myConfig.value.whatsapp_api_key || '')
    await userApi.setMyConfig('whatsapp_to', myConfig.value.whatsapp_to || '')
    message.success('保存成功')
  } catch (e) { message.error('保存失败') }
}

async function testWhatsApp() {
  try {
    const res = await systemApi.testWhatsApp()
    message.success(res.message || '推送成功')
  } catch (e) { message.error('推送失败') }
}

async function refreshRates() {
  refreshing.value = true
  try {
    await systemApi.refreshRates()
    message.success('刷新成功')
    loadRates()
  } catch (e) { message.error('刷新失败') } finally { refreshing.value = false }
}

async function loadRates() {
  try { const res = await systemApi.rates(); rates.value = res.data || [] }
  catch (e) { console.error('Failed to load rates', e) }
}

async function loadAlerts() {
  try { const res = await systemApi.alerts(); alerts.value = res.data || [] }
  catch (e) { console.error('Failed to load alerts', e) }
}

async function markRead(id) {
  try { await systemApi.markAlertRead(id); loadAlerts() }
  catch (e) { console.error('Failed to mark alert as read', e) }
}

const apiKeyMasked = computed(() => {
  if (!apiKey.value) return '尚未生成'
  if (apiKeyVisible.value) return apiKey.value
  return apiKey.value.slice(0, 6) + '•'.repeat(20) + apiKey.value.slice(-4)
})

async function loadApiKey() {
  try { const res = await authApi.getApiKey(); apiKey.value = res.data }
  catch (e) { console.error('Failed to load API key', e) }
}

async function regenerateApiKey() {
  try { const res = await authApi.generateApiKey(); apiKey.value = res.data; message.success('API Key 已重新生成') }
  catch (e) { message.error('生成失败') }
}

function copyApiKey() {
  if (!apiKey.value) return
  navigator.clipboard.writeText(apiKey.value)
  message.success('已复制到剪贴板')
}

function toggleApiKeyVisibility() { apiKeyVisible.value = !apiKeyVisible.value }

onMounted(() => {
  if (auth.isAdmin) loadConfig()
  loadMyNotify()
  loadRates()
  loadAlerts()
  loadApiKey()
})
</script>

<style scoped>
.mobile-settings { display: flex; flex-direction: column; gap: 12px; }

.section-card {
  background: #fff;
  border-radius: var(--m-radius);
  padding: 16px;
  box-shadow: var(--bp-shadow);
}
.section-title { font-size: 15px; font-weight: 650; margin: 0 0 12px; color: var(--bp-ink); }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }

/* 移动端按钮统一触控高度 */
.section-card :deep(.n-button:not(.n-button--text)) {
  height: 40px;
}
.section-card > .n-button,
.section-card .n-form > .n-button {
  width: 100%;
}
.section-card :deep(.n-space) {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.rate-row { display: flex; justify-content: space-between; padding: 6px 0; font-size: 13px; border-bottom: 1px solid #f5f5f5; }
.rate-row:last-child { border-bottom: none; }
.rate-value { font-weight: 600; }

.api-key-display { background: #f5f5f5; border-radius: 6px; padding: 8px; font-size: 12px; word-break: break-all; }

.alert-log-list { display: flex; flex-direction: column; gap: 8px; }
.alert-log-item { display: flex; gap: 8px; align-items: flex-start; padding: 8px; background: #f9f9f9; border-radius: 8px; }
.alert-log-content { flex: 1; min-width: 0; }
.alert-log-title { font-size: 13px; font-weight: 500; }
.alert-log-desc { font-size: 12px; color: #666; margin-top: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.config-hint { font-size: 12px; line-height: 1.6; color: #888; margin-top: 8px; }
</style>
