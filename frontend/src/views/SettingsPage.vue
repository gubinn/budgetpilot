<template>
  <n-grid :cols="2" :x-gap="16" :y-gap="16">
    <!-- Telegram 配置 -->
    <n-gi>
      <n-card title="Telegram 通知" hoverable>
        <n-form label-placement="left" label-width="80">
          <n-form-item label="Bot Token">
            <n-input v-model:value="config.telegram_bot_token" type="password" show-password-on="click" placeholder="输入 Bot Token" />
          </n-form-item>
          <n-form-item label="Chat ID">
            <n-input v-model:value="config.telegram_chat_id" placeholder="输入 Chat ID" />
          </n-form-item>
          <n-form-item>
            <n-button type="primary" @click="saveConfig('telegram_bot_token', config.telegram_bot_token); saveConfig('telegram_chat_id', config.telegram_chat_id)">
              保存配置
            </n-button>
            <n-button @click="testTelegram" style="margin-left: 8px">推送测试</n-button>
          </n-form-item>
        </n-form>
      </n-card>
    </n-gi>

    <!-- 汇率配置 -->
    <n-gi>
      <n-card title="汇率设置" hoverable>
        <n-form label-placement="left" label-width="80">
          <n-form-item label="API Key">
            <n-input v-model:value="config.exchange_rate_api_key" type="password" show-password-on="click" placeholder="ExchangeRate-API Key" />
          </n-form-item>
          <n-form-item label="支持的币种">
            <n-dynamic-tags v-model:value="supportedCurrencies" />
          </n-form-item>
          <n-form-item>
            <n-button type="primary" @click="saveConfig('exchange_rate_api_key', config.exchange_rate_api_key); saveConfig('supported_currencies', supportedCurrencies.join(','))">
              保存配置
            </n-button>
            <n-button @click="refreshRates" :loading="refreshing" style="margin-left: 8px">手动刷新</n-button>
          </n-form-item>
        </n-form>
      </n-card>
    </n-gi>

    <!-- 系统信息 -->
    <n-gi>
      <n-card title="系统信息" hoverable>
        <n-descriptions bordered :column="1" size="small">
          <n-descriptions-item label="本位币">CNY</n-descriptions-item>
          <n-descriptions-item label="月度起始日">每月 1 日</n-descriptions-item>
          <n-descriptions-item label="当前汇率" v-if="rates.length > 0">
            <div v-for="r in rates.slice(0, 5)" :key="r.targetCurrency" style="display: flex; justify-content: space-between; padding: 4px 0">
              <span>1 {{ r.targetCurrency }}</span>
              <span>¥ {{ Number(r.rate).toFixed(4) }}</span>
            </div>
          </n-descriptions-item>
        </n-descriptions>
      </n-card>
    </n-gi>

    <!-- API Key -->
    <n-gi>
      <n-card title="API Key" hoverable>
        <n-alert type="info" v-if="!apiKey" style="margin-bottom: 12px">
          API Key 用于程序调用接口，无需登录即可使用
        </n-alert>
        <n-input-group>
          <n-input :value="apiKeyMasked" readonly />
          <n-button @click="toggleApiKeyVisibility">{{ apiKeyVisible ? '隐藏' : '显示' }}</n-button>
        </n-input-group>
        <n-space style="margin-top: 12px">
          <n-button @click="copyApiKey" :disabled="!apiKey">复制 Key</n-button>
          <n-button type="warning" @click="regenerateApiKey">重新生成</n-button>
        </n-space>
      </n-card>
    </n-gi>

    <!-- 预警日志 -->
    <n-gi>
      <n-card title="预警日志" hoverable>
        <template #header-extra>
          <n-button size="small" @click="loadAlerts">刷新</n-button>
        </template>
        <n-list v-if="alerts.length > 0">
          <n-list-item v-for="alert in alerts" :key="alert.id">
            <template #prefix>
              <n-tag :type="alert.isSent ? 'success' : 'error'" size="small">{{ alert.isSent ? '已推送' : '未推送' }}</n-tag>
            </template>
            <n-thing :title="alert.title" :description="alert.content" />
            <template #suffix>
              <n-button size="tiny" text @click="markRead(alert.id)" v-if="!alert.isRead">标记已读</n-button>
            </template>
          </n-list-item>
        </n-list>
        <n-empty v-else description="暂无预警" />
      </n-card>
    </n-gi>
  </n-grid>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useMessage } from 'naive-ui'
import { systemApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import dayjs from 'dayjs'
import axios from 'axios'

const message = useMessage()
const auth = useAuthStore()
const config = ref({
  telegram_bot_token: '',
  telegram_chat_id: '',
  exchange_rate_api_key: ''
})
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
  } catch (e) {}
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
  try {
    const res = await systemApi.rates()
    rates.value = res.data || []
  } catch (e) {}
}

async function loadAlerts() {
  try {
    const res = await systemApi.alerts()
    alerts.value = res.data || []
  } catch (e) {}
}

async function markRead(id) {
  try {
    await systemApi.markAlertRead(id)
    loadAlerts()
  } catch (e) {}
}

const apiKeyMasked = computed(() => {
  if (!apiKey.value) return '尚未生成'
  if (apiKeyVisible.value) return apiKey.value
  return apiKey.value.slice(0, 6) + '•'.repeat(20) + apiKey.value.slice(-4)
})

async function loadApiKey() {
  try {
    const res = await axios.get('/api/v1/auth/api-key', {
      headers: { Authorization: auth.token }
    })
    if (res.data.code === 0) {
      apiKey.value = res.data.data
    }
  } catch (e) {}
}

async function regenerateApiKey() {
  try {
    const res = await axios.post('/api/v1/auth/api-key/generate', null, {
      headers: { Authorization: auth.token }
    })
    if (res.data.code === 0) {
      apiKey.value = res.data.data
      message.success('API Key 已重新生成')
    }
  } catch (e) { message.error('生成失败') }
}

function copyApiKey() {
  if (!apiKey.value) return
  navigator.clipboard.writeText(apiKey.value)
  message.success('已复制到剪贴板')
}

function toggleApiKeyVisibility() {
  apiKeyVisible.value = !apiKeyVisible.value
}

onMounted(() => {
  loadConfig()
  loadRates()
  loadAlerts()
  loadApiKey()
})
</script>
