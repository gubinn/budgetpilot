<template>
  <div class="mobile-layout">
    <!-- 顶部 Header -->
    <header class="mobile-header">
      <div class="header-left">
        <img src="/budgetpilot-logo.png" alt="BudgetPilot" class="logo" />
      </div>
      <div class="header-right">
        <n-badge :value="alertCount" :show="alertCount > 0">
          <n-button quaternary circle size="small" @click="router.push('/alerts')">
            <template #icon><n-icon><NotificationsOutline /></n-icon></template>
          </n-button>
        </n-badge>
        <n-button type="primary" size="small" round @click="router.push('/transactions/add')">
          <template #icon><n-icon><AddOutline /></n-icon></template>
        </n-button>
        <n-dropdown :options="userDropdownOptions" @select="handleUserSelect" trigger="click">
          <n-button quaternary circle size="small">
            <template #icon><n-icon><PersonOutline /></n-icon></template>
          </n-button>
        </n-dropdown>
      </div>
    </header>

    <!-- 页面标题 -->
    <div class="page-title-bar">
      <h3>{{ pageTitle }}</h3>
    </div>

    <!-- 内容区域 -->
    <main class="mobile-content">
      <router-view />
    </main>

    <!-- 底部 Tab Bar -->
    <nav class="tab-bar">
      <div
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-item"
        :class="{ active: currentRoute === tab.key }"
        @click="router.push({ name: tab.key })"
      >
        <n-icon :size="22">
          <component :is="tab.icon" />
        </n-icon>
        <span class="tab-label">{{ tab.label }}</span>
      </div>
    </nav>
  </div>
</template>

<script setup>
import { ref, computed, h, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { NIcon, useDialog } from 'naive-ui'
import {
  HomeOutline, ReceiptOutline, WalletOutline,
  BarChartOutline, FolderOutline, AddOutline,
  NotificationsOutline, PersonOutline, LogOutOutline
} from '@vicons/ionicons5'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const dialog = useDialog()

const tabs = [
  { label: '首页', key: 'Dashboard', icon: HomeOutline },
  { label: '交易', key: 'Transactions', icon: ReceiptOutline },
  { label: '账户', key: 'Accounts', icon: WalletOutline },
  { label: '报表', key: 'Reports', icon: BarChartOutline },
  { label: '分类', key: 'Categories', icon: FolderOutline }
]

const currentRoute = computed(() => route.name || 'Dashboard')
const alertCount = ref(0)

const pageTitle = computed(() => {
  const map = {
    Dashboard: '首页',
    Transactions: '交易记录',
    AddTransaction: '新增交易',
    EditTransaction: '编辑交易',
    Accounts: '账户管理',
    Reports: '统计报表',
    Settings: '系统设置',
    Alerts: '告警通知',
    Categories: '分类管理',
    Merchants: '商户管理',
    Budget: '预算管理',
    Recurring: '周期交易',
    AlertRules: '预警规则',
    Users: '用户管理'
  }
  return map[route.name] || 'BudgetPilot'
})

const userDropdownOptions = computed(() => [
  { label: `${auth.nickname}`, key: 'profile', disabled: true },
  { label: '---', key: 'divider', type: 'divider' },
  { label: '退出登录', key: 'logout', icon: () => h(NIcon, null, { default: () => h(LogOutOutline) }) }
])

async function handleUserSelect(key) {
  if (key === 'logout') {
    dialog.warning({
      title: '确认退出',
      content: '确定要退出登录吗？',
      positiveText: '退出',
      negativeText: '取消',
      onPositiveClick: async () => {
        await auth.logout()
        router.push('/login')
      }
    })
  }
}
</script>

<style scoped>
.mobile-layout {
  min-height: 100vh;
  min-height: 100dvh;
  background: #f5f7fa;
  padding-bottom: calc(60px + env(safe-area-inset-bottom, 0px));
}

.mobile-header {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: calc(10px + env(safe-area-inset-top, 0px)) 16px 10px;
  background: #fff;
  border-bottom: 1px solid #eee;
}

.logo {
  height: 22px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.page-title-bar {
  padding: 8px 16px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
}

.page-title-bar h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.mobile-content {
  padding: 12px;
}

.tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 100;
  display: flex;
  background: #fff;
  border-top: 1px solid #eee;
  padding-bottom: env(safe-area-inset-bottom);
}

.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 8px 0;
  color: #888;
  transition: color 0.2s;
  -webkit-tap-highlight-color: transparent;
}

.tab-item.active {
  color: #2c3e50;
}

.tab-label {
  font-size: 10px;
  margin-top: 2px;
  line-height: 1;
}
</style>
