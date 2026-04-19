<template>
  <div class="mobile-alerts">
    <div v-if="alerts.length > 0" class="alert-actions">
      <n-button size="small" type="primary" @click="markAllRead" :loading="loading">全部标记已读</n-button>
    </div>

    <n-empty v-if="alerts.length === 0" description="暂无告警通知" />

    <div v-else class="alert-list">
      <div v-for="alert in alerts" :key="alert.id" class="alert-card" :class="{ read: alert.isRead }">
        <div class="alert-header">
          <div class="alert-left">
            <div class="alert-dot" :style="{ background: alert.isRead ? '#ccc' : '#f0a020' }"></div>
            <span class="alert-title">{{ alert.title }}</span>
          </div>
          <n-tag :type="alert.isRead ? 'default' : 'warning'" size="tiny" :bordered="false">
            {{ alert.isRead ? '已读' : '未读' }}
          </n-tag>
        </div>
        <div class="alert-content">{{ alert.content }}</div>
        <div class="alert-footer">
          <span class="alert-time">{{ formatTime(alert.triggeredAt) }}</span>
          <n-button v-if="!alert.isRead" size="tiny" @click="markRead(alert.id)">标记已读</n-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useMessage } from 'naive-ui'
import { systemApi } from '@/api'

const message = useMessage()
const alerts = ref([])
const loading = ref(false)

async function loadAlerts() {
  loading.value = true
  try {
    const res = await systemApi.alerts()
    alerts.value = res.data || []
  } catch (e) { message.error('加载失败') } finally { loading.value = false }
}

async function markRead(id) {
  try {
    await systemApi.markAlertRead(id)
    const alert = alerts.value.find(a => a.id === id)
    if (alert) alert.isRead = true
    message.success('已标记')
  } catch (e) { message.error('操作失败') }
}

async function markAllRead() {
  loading.value = true
  try {
    for (const alert of alerts.value) {
      if (!alert.isRead) { await systemApi.markAlertRead(alert.id); alert.isRead = true }
    }
    message.success('全部已标记')
  } catch (e) { message.error('操作失败') } finally { loading.value = false }
}

function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  return date.toLocaleDateString()
}

onMounted(loadAlerts)
</script>

<style scoped>
.mobile-alerts { padding-bottom: 12px; }

.alert-actions { margin-bottom: 12px; }

.alert-list { display: flex; flex-direction: column; gap: 8px; }

.alert-card {
  background: #fff;
  border-radius: 10px;
  padding: 12px;
  border-left: 3px solid #f0a020;
}
.alert-card.read { border-left-color: #ddd; }

.alert-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.alert-left { display: flex; align-items: center; gap: 8px; }
.alert-dot { width: 8px; height: 8px; border-radius: 50%; }
.alert-title { font-size: 14px; font-weight: 600; }

.alert-content { font-size: 13px; color: #555; margin-bottom: 8px; line-height: 1.5; }

.alert-footer { display: flex; justify-content: space-between; align-items: center; }
.alert-time { font-size: 11px; color: #999; }
</style>
