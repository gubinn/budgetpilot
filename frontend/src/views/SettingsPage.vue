<template>
  <n-grid :cols="2" :x-gap="16" :y-gap="16">
    <!-- 定时提醒渠道（全局配置，独立于各通知渠道） -->
    <n-gi :span="2" v-if="auth.isAdmin">
      <n-card title="定时提醒渠道" hoverable>
        <div class="channel-row">
          <n-select v-model:value="config.notify_channel" :options="notifyChannelOptions" class="channel-select" />
          <n-button type="primary" @click="saveNotifyChannel">保存</n-button>
        </div>
        <div class="config-hint">信用卡还款、周期账单、预算超支、周消费异常、月度总结、预算未设定等系统定时提醒统一使用该渠道；单条预警规则的渠道在「预警规则」页单独设置。</div>
      </n-card>
    </n-gi>

    <!-- Telegram 配置 -->
    <n-gi v-if="auth.isAdmin">
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

    <!-- WhatsApp 配置 -->
    <n-gi v-if="auth.isAdmin">
      <n-card title="WhatsApp 通知" hoverable>
        <n-form label-placement="left" label-width="96">
          <n-form-item label="API Key">
            <n-input v-model:value="config.whatsapp_api_key" type="password" show-password-on="click" placeholder="Agent API Key（留空则使用环境变量）" />
          </n-form-item>
          <n-form-item label="接收者 ID">
            <n-input v-model:value="config.whatsapp_to" placeholder="user:<id>（不是手机号）" />
          </n-form-item>
          <n-form-item>
            <n-button type="primary" @click="saveWhatsApp">保存配置</n-button>
            <n-button @click="testWhatsApp" style="margin-left: 8px">推送测试</n-button>
          </n-form-item>
        </n-form>
      </n-card>
    </n-gi>

    <!-- 公网登录验证：每个用户配置自己的验证码接收渠道 -->
    <n-gi :span="2">
      <n-card title="登录验证（公网访问）" hoverable>
        <n-form label-placement="left" label-width="130">
          <n-form-item label="验证码接收渠道">
            <n-select v-model:value="myConfig.notify_channel" :options="myChannelOptions" class="channel-select" />
          </n-form-item>
          <n-form-item label="Telegram Chat ID">
            <n-input v-model:value="myConfig.telegram_chat_id" placeholder="留空则使用系统全局配置" />
          </n-form-item>
          <n-form-item label="WhatsApp 接收者">
            <n-input v-model:value="myConfig.whatsapp_to" placeholder="user:<id>；留空则使用系统全局配置" />
          </n-form-item>
          <n-form-item>
            <n-button type="primary" @click="saveMyNotify">保存</n-button>
            <span class="config-inline-hint">公网登录时验证码发到这里；WhatsApp 只能发给 Agent 创建者，家人建议用 Telegram。</span>
          </n-form-item>
        </n-form>
      </n-card>
    </n-gi>

    <!-- 汇率配置 -->
    <n-gi v-if="auth.isAdmin">
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
import { systemApi, authApi, userApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

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
  telegram_chat_id: '',
  whatsapp_to: ''
})
const myChannelOptions = [
  { label: '跟随系统全局配置', value: '' },
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
    myConfig.value.telegram_chat_id = res.data.telegram_chat_id || ''
    myConfig.value.whatsapp_to = res.data.whatsapp_to || ''
  } catch (e) { console.error('Failed to load my notify config', e) }
}

async function saveMyNotify() {
  try {
    await userApi.setMyConfig('notify_channel', myConfig.value.notify_channel || '')
    await userApi.setMyConfig('telegram_chat_id', myConfig.value.telegram_chat_id || '')
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
  try {
    const res = await systemApi.rates()
    rates.value = res.data || []
  } catch (e) { console.error('Failed to load rates', e) }
}

async function loadAlerts() {
  try {
    const res = await systemApi.alerts()
    alerts.value = res.data || []
  } catch (e) { console.error('Failed to load alerts', e) }
}

async function markRead(id) {
  try {
    await systemApi.markAlertRead(id)
    loadAlerts()
  } catch (e) { console.error('Failed to mark alert as read', e) }
}

const apiKeyMasked = computed(() => {
  if (!apiKey.value) return '尚未生成'
  if (apiKeyVisible.value) return apiKey.value
  return apiKey.value.slice(0, 6) + '•'.repeat(20) + apiKey.value.slice(-4)
})

async function loadApiKey() {
  try {
    const res = await authApi.getApiKey()
    apiKey.value = res.data
  } catch (e) { console.error('Failed to load API key', e) }
}

async function regenerateApiKey() {
  try {
    const res = await authApi.generateApiKey()
    apiKey.value = res.data
    message.success('API Key 已重新生成')
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
  if (auth.isAdmin) loadConfig()
  loadMyNotify()
  loadRates()
  loadAlerts()
  loadApiKey()
})
</script>

<style scoped>
.config-hint {
  font-size: 12px;
  line-height: 1.6;
  color: var(--bp-muted, #888);
}
.channel-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.channel-select {
  width: 260px;
}
.config-inline-hint {
  margin-left: 12px;
  font-size: 12px;
  color: var(--bp-muted, #888);
  line-height: 1.6;
}
@media (max-width: 640px) {
  .channel-row {
    flex-direction: column;
    align-items: stretch;
  }
  .channel-select {
    width: 100%;
  }
}
</style>
