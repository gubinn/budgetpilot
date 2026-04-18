<template>
  <n-card title="告警通知">
    <template #header-extra>
      <n-button v-if="alerts.length > 0" size="small" @click="markAllRead" :loading="loading">
        全部标记已读
      </n-button>
    </template>

    <n-empty v-if="alerts.length === 0" description="暂无告警通知" />

    <n-list v-else bordered>
      <n-list-item v-for="alert in alerts" :key="alert.id">
        <n-thing :title="alert.title">
          <template #header-extra>
            <n-tag :type="alert.isRead ? 'default' : 'warning'" size="small">
              {{ alert.isRead ? '已读' : '未读' }}
            </n-tag>
          </template>
          <template #avatar>
            <n-badge dot :show="!alert.isRead">
              <n-avatar :style="{ background: alert.isRead ? '#ccc' : '#f0a020' }">
                <n-icon><warning-outline /></n-icon>
              </n-avatar>
            </n-badge>
          </template>
          <n-ellipsis :line-clamp="2">{{ alert.content }}</n-ellipsis>
          <template #footer>
            <n-space justify="space-between" align="center">
              <n-text depth="3" style="font-size: 12px">
                {{ formatTime(alert.triggeredAt) }}
              </n-text>
              <n-button v-if="!alert.isRead" size="tiny" @click="markRead(alert.id)">
                标记已读
              </n-button>
            </n-space>
          </template>
        </n-thing>
      </n-list-item>
    </n-list>
  </n-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useMessage, NIcon } from 'naive-ui'
import { WarningOutline } from '@vicons/ionicons5'
import { systemApi } from '@/api'

const message = useMessage()
const alerts = ref([])
const loading = ref(false)

async function loadAlerts() {
  loading.value = true
  try {
    const res = await systemApi.alerts()
    alerts.value = res.data || []
  } catch (e) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

async function markRead(id) {
  try {
    await systemApi.markAlertRead(id)
    const alert = alerts.value.find(a => a.id === id)
    if (alert) alert.isRead = true
    message.success('已标记')
  } catch (e) {
    message.error('操作失败')
  }
}

async function markAllRead() {
  loading.value = true
  try {
    for (const alert of alerts.value) {
      if (!alert.isRead) {
        await systemApi.markAlertRead(alert.id)
        alert.isRead = true
      }
    }
    message.success('全部已标记')
  } catch (e) {
    message.error('操作失败')
  } finally {
    loading.value = false
  }
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