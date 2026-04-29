<template>
  <n-layout has-sider style="height: 100vh" :native-scrollbar="false">
    <n-layout-sider
      bordered
      :collapsed="collapsed"
      :width="220"
      :collapsed-width="0"
      show-trigger="bar"
      collapse-mode="transform"
      @collapse="collapsed = true"
      @expand="collapsed = false"
      :native-scrollbar="false"
      class="sider-wrapper"
    >
      <div class="logo">
        <img src="/budgetpilot-logo.png" alt="BudgetPilot" class="logo-img" v-if="!collapsed" />
      </div>
      <n-menu
        :collapsed="collapsed"
        :collapsed-width="0"
        :collapsed-icon-size="22"
        :options="menuOptions"
        :value="currentRoute"
        @update:value="navigate"
      />
    </n-layout-sider>
    <n-layout style="height: 100vh" :native-scrollbar="false">
      <n-layout-header bordered style="padding: 12px 16px; background: #fff; display: flex; justify-content: space-between; align-items: center;">
        <n-space :size="12" align="center">
          <n-button quaternary circle size="small" @click="collapsed = !collapsed" class="menu-toggle">
            <template #icon><n-icon><menu-outline /></n-icon></template>
          </n-button>
          <h3 style="font-size: 16px">{{ pageTitle }}</h3>
        </n-space>
        <n-space :size="8" align="center">
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
            <n-button quaternary circle size="small">
              <template #icon><n-icon><person-outline /></n-icon></template>
            </n-button>
          </n-dropdown>
        </n-space>
      </n-layout-header>
      <n-layout-content content-style="padding: 16px;">
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
  PersonOutline, LogOutOutline, PeopleOutline
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

const pageTitle = computed(() => {
  const map = {
    Dashboard: '首页',
    Transactions: '交易记录',
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
.logo {
  padding: 12px 16px;
  border-bottom: 1px solid #eee;
}
.logo-img {
  width: 140px;
  display: block;
  margin: 0 auto;
}
.add-btn {
  box-shadow: 0 2px 8px rgba(44, 62, 80, 0.2);
}
.sider-wrapper {
  z-index: 100;
}
@media (max-width: 768px) {
  .sider-wrapper {
    position: fixed !important;
    left: 0;
    top: 0;
    height: 100vh;
  }
  .add-text {
    display: none;
  }
  .n-layout-content {
    padding: 12px !important;
  }
}
</style>
