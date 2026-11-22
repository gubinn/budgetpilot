<template>
  <n-layout has-sider style="height: 100vh" :native-scrollbar="false">
    <n-layout-sider
      :collapsed="collapsed"
      :width="232"
      :collapsed-width="0"
      show-trigger="bar"
      collapse-mode="transform"
      @collapse="collapsed = true"
      @expand="collapsed = false"
      :native-scrollbar="false"
      class="sider-wrapper"
      style="background: #0b1220"
    >
      <div class="logo">
        <img src="/logo-mark.svg" alt="BudgetPilot" class="logo-mark" v-if="!collapsed" />
        <div class="logo-text" v-if="!collapsed">
          <span class="logo-name">BudgetPilot</span>
          <span class="logo-sub">PERSONAL FINANCE</span>
        </div>
      </div>
      <n-menu
        inverted
        class="side-menu"
        :collapsed="collapsed"
        :collapsed-width="0"
        :collapsed-icon-size="22"
        :options="menuOptions"
        :value="currentRoute"
        @update:value="navigate"
      />
    </n-layout-sider>
    <n-layout style="height: 100vh" :native-scrollbar="false">
      <n-layout-header class="topbar">
        <div class="topbar-left">
          <n-button quaternary circle size="small" @click="collapsed = !collapsed" class="menu-toggle">
            <template #icon><n-icon><menu-outline /></n-icon></template>
          </n-button>
          <div class="page-heading">
            <h1>{{ pageTitle }}</h1>
            <span>让每一笔钱，都有方向</span>
          </div>
        </div>
        <div class="topbar-right">
          <n-badge :value="alertCount" :show="alertCount > 0">
            <n-button quaternary circle size="small" @click="router.push('/alerts')">
              <template #icon><n-icon><notifications-outline /></n-icon></template>
            </n-button>
          </n-badge>
          <n-button type="primary" size="small" @click="router.push('/transactions/add')" class="add-btn">
            <template #icon><n-icon><add-outline /></n-icon></template>
            <span class="add-text">记一笔</span>
          </n-button>
          <n-dropdown :options="userDropdownOptions" @select="handleUserSelect" trigger="click">
            <button class="avatar-trigger" type="button" :aria-label="auth.nickname || '账户'">
              {{ userInitial }}
            </button>
          </n-dropdown>
        </div>
      </n-layout-header>
      <n-layout-content class="content">
        <router-view />
      </n-layout-content>
    </n-layout>
  </n-layout>
</template>

<script setup>
import { ref, computed, h, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { systemApi } from '@/api'
import {
  HomeOutline, ReceiptOutline, WalletOutline, FolderOutline,
  BarChartOutline, SettingsOutline, AddOutline, NotificationsOutline, MenuOutline, RepeatOutline, AlertOutline, BusinessOutline,
  PersonOutline, LogOutOutline, PeopleOutline, GitCompareOutline
} from '@vicons/ionicons5'
import { NIcon, useDialog } from 'naive-ui'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const dialog = useDialog()
const collapsed = ref(false)

const alertCount = ref(0)
const alertRefreshTimer = ref(null)

async function refreshAlertCount() {
  if (auth.isAdmin) {
    alertCount.value = 0
    return
  }
  try {
    const res = await systemApi.alerts()
    alertCount.value = res.data?.length || 0
  } catch {
    alertCount.value = 0
  }
}

// 移动端默认收起侧边栏
onMounted(() => {
  if (window.innerWidth < 768) {
    collapsed.value = true
  }
  refreshAlertCount()
  alertRefreshTimer.value = setInterval(refreshAlertCount, 60000)
})

onBeforeUnmount(() => {
  if (alertRefreshTimer.value) {
    clearInterval(alertRefreshTimer.value)
  }
})

const renderIcon = (icon) => () => h(NIcon, null, { default: () => h(icon) })

const menuOptions = computed(() => {
  // Admin: only see Users menu
  if (auth.isAdmin) {
    return [
      { label: '用户', key: 'Users', icon: renderIcon(PeopleOutline) }
    ]
  }
  // Non-admin: all menus except Users
  return [
    { label: '首页', key: 'Dashboard', icon: renderIcon(HomeOutline) },
    { label: '交易', key: 'Transactions', icon: renderIcon(ReceiptOutline) },
    { label: '对账', key: 'Reconciliation', icon: renderIcon(GitCompareOutline) },
    { label: '账户', key: 'Accounts', icon: renderIcon(WalletOutline) },
    { label: '分类', key: 'Categories', icon: renderIcon(FolderOutline) },
    { label: '商户', key: 'Merchants', icon: renderIcon(BusinessOutline) },
    { label: '预算', key: 'Budget', icon: renderIcon(BarChartOutline) },
    { label: '周期', key: 'Recurring', icon: renderIcon(RepeatOutline) },
    { label: '预警', key: 'AlertRules', icon: renderIcon(AlertOutline) },
    { label: '报表', key: 'Reports', icon: renderIcon(BarChartOutline) },
    { label: '设置', key: 'Settings', icon: renderIcon(SettingsOutline) }
  ]
})

const currentRoute = computed(() => route.name || 'Dashboard')

const userInitial = computed(() => {
  const name = auth.nickname || auth.user?.username || 'U'
  return name.slice(0, 1).toUpperCase()
})

const pageTitle = computed(() => {
  const map = {
    Dashboard: '首页',
    Transactions: '交易记录',
    Reconciliation: '账单对账',
    AddTransaction: '新增交易',
    EditTransaction: '编辑交易',
    Accounts: '账户管理',
    Categories: '分类管理',
    Merchants: '商户管理',
    Budget: '预算管理',
    Recurring: '周期交易',
    AlertRules: '预警规则',
    Alerts: '告警通知',
    Reports: '统计报表',
    Settings: '系统设置',
    Users: '用户管理'
  }
  return map[route.name] || 'BudgetPilot'
})

const userDropdownOptions = computed(() => [
  { label: `${auth.nickname}`, key: 'profile', disabled: true },
  { label: '---', key: 'divider', type: 'divider' },
  { label: '退出登录', key: 'logout', icon: renderIcon(LogOutOutline) }
])

const navigate = (key) => {
  router.push({ name: key })
  // 移动端导航后自动收起侧边栏
  if (window.innerWidth < 768) {
    collapsed.value = true
  }
}

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
.sider-wrapper {
  z-index: 100;
  background: #0b1220 !important;
}
.sider-wrapper :deep(.n-layout-sider-scroll-container) {
  background: #0b1220;
}
.logo {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 20px 18px 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
}
.logo-mark {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  box-shadow: 0 8px 24px -6px rgba(37, 99, 235, 0.8);
}
.logo-text {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.logo-name {
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.01em;
}
.logo-sub {
  color: #64748b;
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.16em;
}
.side-menu {
  padding: 12px 10px;
}
.side-menu :deep(.n-menu-item-content) {
  border-radius: 10px;
}
.side-menu :deep(.n-menu-item-content-header) {
  font-weight: 500;
}
.topbar {
  height: 64px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.84);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--bp-line);
  position: sticky;
  top: 0;
  z-index: 50;
}
.topbar-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}
.page-heading {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.page-heading h1 {
  font-size: 18px;
  font-weight: 650;
  letter-spacing: -0.01em;
  line-height: 1.2;
}
.page-heading span {
  font-size: 11px;
  color: var(--bp-muted);
  white-space: nowrap;
}
.topbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.add-btn {
  background: linear-gradient(135deg, #2563eb, #0ea5a4) !important;
  border: none !important;
  box-shadow: 0 8px 18px -8px rgba(37, 99, 235, 0.7);
}
.avatar-trigger {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  background: linear-gradient(135deg, #2563eb, #0ea5a4);
  box-shadow: 0 6px 14px -6px rgba(37, 99, 235, 0.8);
}
.content {
  padding: 24px;
}
@media (max-width: 768px) {
  .sider-wrapper {
    position: fixed !important;
    left: 0;
    top: 0;
    height: 100vh;
  }
  .topbar {
    padding: 0 12px;
  }
  .page-heading span {
    display: none;
  }
  .add-text {
    display: none;
  }
  .content {
    padding: 12px !important;
  }
}
</style>
