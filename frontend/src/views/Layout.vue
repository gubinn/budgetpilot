<template>
  <n-layout has-sider style="height: 100vh" :native-scrollbar="false">
    <n-layout-sider
      :collapsed="collapsed"
      :width="232"
      :collapsed-width="isCompact ? 72 : 0"
      show-trigger="bar"
      :collapse-mode="isCompact ? 'width' : 'transform'"
      @collapse="collapsed = true"
      @expand="collapsed = false"
      :native-scrollbar="false"
      :class="['sider-wrapper', { 'is-collapsed': collapsed, 'is-overlay-open': isCompact && !collapsed }]"
      style="background: #0b1220"
    >
      <div class="logo" :class="{ compact: collapsed }">
        <img src="/logo-mark.svg" alt="BudgetPilot" class="logo-mark" />
        <div class="logo-text" v-if="!collapsed">
          <span class="logo-name">BudgetPilot</span>
          <span class="logo-sub">PERSONAL FINANCE</span>
        </div>
      </div>
      <n-menu
        inverted
        class="side-menu"
        :collapsed="collapsed"
        :collapsed-width="isCompact ? 72 : 0"
        :collapsed-icon-size="22"
        :options="menuOptions"
        :value="currentRoute"
        :render-label="renderMenuLabel"
        @update:value="navigate"
      />
    </n-layout-sider>
    <div v-if="isCompact && !collapsed" class="sider-scrim" @click="collapsed = true"></div>
    <n-layout style="height: 100vh" :native-scrollbar="false">
      <n-layout-header class="topbar">
        <div class="topbar-left">
          <n-button quaternary circle size="small" @click="collapsed = !collapsed" class="menu-toggle" aria-label="切换菜单">
            <template #icon><n-icon><menu-outline /></n-icon></template>
          </n-button>
          <div class="page-heading">
            <h1>{{ pageTitle }}</h1>
            <span>让每一笔钱，都有方向</span>
          </div>
        </div>
        <div class="topbar-right">
          <n-badge v-if="!auth.isReadOnly" :value="alertCount" :show="alertCount > 0">
            <n-button quaternary circle size="small" @click="router.push('/alerts')">
              <template #icon><n-icon><notifications-outline /></n-icon></template>
            </n-button>
          </n-badge>
          <n-button v-if="!auth.isReadOnly" type="primary" size="small" @click="router.push('/transactions/add')" class="add-btn">
            <template #icon><n-icon><add-outline /></n-icon></template>
            <span class="add-text">记一笔</span>
          </n-button>
          <n-dropdown v-if="!auth.isReadOnly" :options="userDropdownOptions" @select="handleUserSelect" trigger="click">
            <button class="avatar-trigger" type="button" :aria-label="auth.nickname || '账户'">
              {{ userInitial }}
            </button>
          </n-dropdown>
        </div>
      </n-layout-header>
      <div v-if="auth.isReadOnly" class="readonly-strip">
        <n-icon :size="15"><EyeOutline /></n-icon>
        <span>只读查看 · {{ auth.readonlyOwnerName }} 的账本</span>
        <n-button text size="tiny" class="readonly-exit" @click="exitReadonly">退出</n-button>
      </div>
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
import { systemApi, prepaidCardApi } from '@/api'
import {
  HomeOutline, ReceiptOutline, WalletOutline, FolderOutline,
  BarChartOutline, SettingsOutline, AddOutline, NotificationsOutline, MenuOutline, RepeatOutline, AlertOutline, BusinessOutline,
  PersonOutline, LogOutOutline, PeopleOutline, GitCompareOutline, SwapHorizontalOutline, TicketOutline, EyeOutline
} from '@vicons/ionicons5'
import { NIcon, useDialog } from 'naive-ui'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const dialog = useDialog()
const COMPACT_BREAKPOINT = 1024
const isCompact = ref(window.innerWidth <= COMPACT_BREAKPOINT)
const collapsed = ref(isCompact.value)

const alertCount = ref(0)
const alertRefreshTimer = ref(null)
const expiringCardCount = ref(0)
const cardRefreshTimer = ref(null)

async function refreshAlertCount() {
  if (auth.isAdmin || auth.isReadOnly) {
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

async function refreshCardCount() {
  if (auth.isAdmin) {
    expiringCardCount.value = 0
    return
  }
  try {
    const res = await prepaidCardApi.summary()
    expiringCardCount.value = res.data?.expiringSoonCount || 0
  } catch {
    expiringCardCount.value = 0
  }
}

// 平板（含 11 寸 iPad 竖屏）默认收成图标栏，避免侧边栏占掉太多横向空间
function syncCompact() {
  const next = window.innerWidth <= COMPACT_BREAKPOINT
  if (next === isCompact.value) return
  isCompact.value = next
  collapsed.value = next
}

onMounted(() => {
  if (isCompact.value) {
    collapsed.value = true
  }
  window.addEventListener('resize', syncCompact)
  refreshAlertCount()
  refreshCardCount()
  alertRefreshTimer.value = setInterval(refreshAlertCount, 60000)
  cardRefreshTimer.value = setInterval(refreshCardCount, 300000)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', syncCompact)
  if (alertRefreshTimer.value) {
    clearInterval(alertRefreshTimer.value)
  }
  if (cardRefreshTimer.value) {
    clearInterval(cardRefreshTimer.value)
  }
})

const renderIcon = (icon) => () => h(NIcon, null, { default: () => h(icon) })

// 卡券包菜单上的到期计数：只在有卡即将到期时出现
function renderMenuLabel(option) {
  if (option.key !== 'PrepaidCards' || !expiringCardCount.value) {
    return option.label
  }
  return h('span', { style: { display: 'inline-flex', alignItems: 'center', gap: '8px' } }, [
    option.label,
    h('span', {
      style: {
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        minWidth: '18px',
        height: '18px',
        padding: '0 5px',
        borderRadius: '9px',
        background: '#f59e0b',
        color: '#fff',
        fontSize: '11px',
        lineHeight: '1'
      }
    }, String(expiringCardCount.value))
  ])
}

const readonlyMenuKeys = new Set([
  'Dashboard', 'Transactions', 'Accounts', 'PrepaidCards', 'Receivables', 'Reports'
])

const menuOptions = computed(() => {
  // Admin: only see Users menu
  if (auth.isAdmin) {
    return [
      { label: '用户', key: 'Users', icon: renderIcon(PeopleOutline) }
    ]
  }
  // Non-admin: all menus except Users
  const options = [
    { label: '首页', key: 'Dashboard', icon: renderIcon(HomeOutline) },
    { label: '交易', key: 'Transactions', icon: renderIcon(ReceiptOutline) },
    { label: '对账', key: 'Reconciliation', icon: renderIcon(GitCompareOutline) },
    { label: '账户', key: 'Accounts', icon: renderIcon(WalletOutline) },
    { label: '卡券包', key: 'PrepaidCards', icon: renderIcon(TicketOutline) },
    { label: '往来款', key: 'Receivables', icon: renderIcon(SwapHorizontalOutline) },
    { label: '分类', key: 'Categories', icon: renderIcon(FolderOutline) },
    { label: '商户', key: 'Merchants', icon: renderIcon(BusinessOutline) },
    { label: '预算', key: 'Budget', icon: renderIcon(BarChartOutline) },
    { label: '周期', key: 'Recurring', icon: renderIcon(RepeatOutline) },
    { label: '预警', key: 'AlertRules', icon: renderIcon(AlertOutline) },
    { label: '报表', key: 'Reports', icon: renderIcon(BarChartOutline) },
    { label: '设置', key: 'Settings', icon: renderIcon(SettingsOutline) }
  ]
  return auth.isReadOnly
    ? options.filter(option => readonlyMenuKeys.has(option.key))
    : options
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
    PrepaidCards: '卡券包',
    Receivables: '往来款',
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
  // 平板导航后自动收起浮层菜单
  if (isCompact.value) {
    collapsed.value = true
  }
}

async function exitReadonly() {
  const code = auth.readonlyShareCode
  try {
    await auth.logout()
  } catch {
    // 即使退出接口失败，本地令牌也已经清掉
  }
  router.push(code ? `/share/${code}` : '/login')
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

.readonly-strip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 24px;
  background: #fffbeb;
  border-bottom: 1px solid #fde68a;
  color: #92400e;
  font-size: 12px;
}
.readonly-exit {
  margin-left: auto;
  color: #b45309;
}

.sider-scrim {
  position: fixed;
  inset: 0;
  z-index: 150;
  background: rgba(15, 23, 42, 0.36);
  backdrop-filter: blur(1px);
}

/* 平板（iPad 竖屏等）：图标栏 + 浮层展开，不再挤压内容 */
@media (max-width: 1024px) {
  .sider-wrapper.is-overlay-open {
    position: fixed !important;
    left: 0;
    top: 0;
    height: 100vh;
    z-index: 200;
    box-shadow: 24px 0 60px -30px rgba(15, 23, 42, 0.55);
  }
  .topbar {
    padding: 0 18px;
  }
  .page-heading span {
    display: none;
  }
  .content {
    padding: 18px;
  }
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
