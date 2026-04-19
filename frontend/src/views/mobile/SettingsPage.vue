<template>
  <div class="mobile-settings">
    <!-- Telegram -->
    <div class="section-card">
      <h3 class="section-title">Telegram 通知</h3>
      <n-form label-placement="top" label-width="auto">
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
    </div>

    <!-- 汇率 -->
    <div class="section-card">
      <h3 class="section-title">汇率设置</h3>
      <n-form label-placement="top" label-width="auto">
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
import { systemApi, authApi } from '@/api'

const message = useMessage()
const config = ref({ telegram_bot_token: '', telegram_chat_id: '', exchange_rate_api_key: '' })
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

onMounted(() => { loadConfig(); loadRates(); loadAlerts(); loadApiKey() })
</script>

<style scoped>
.mobile-settings { display: flex; flex-direction: column; gap: 12px; }

.section-card { background: #fff; border-radius: 10px; padding: 14px; }
.section-title { font-size: 14px; font-weight: 600; margin: 0 0 10px; color: #333; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }

.rate-row { display: flex; justify-content: space-between; padding: 6px 0; font-size: 13px; border-bottom: 1px solid #f5f5f5; }
.rate-row:last-child { border-bottom: none; }
.rate-value { font-weight: 600; }

.api-key-display { background: #f5f5f5; border-radius: 6px; padding: 8px; font-size: 12px; word-break: break-all; }

.alert-log-list { display: flex; flex-direction: column; gap: 8px; }
.alert-log-item { display: flex; gap: 8px; align-items: flex-start; padding: 8px; background: #f9f9f9; border-radius: 8px; }
.alert-log-content { flex: 1; min-width: 0; }
.alert-log-title { font-size: 13px; font-weight: 500; }
.alert-log-desc { font-size: 12px; color: #666; margin-top: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
