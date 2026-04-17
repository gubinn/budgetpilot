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
        <h2 v-if="!collapsed">BudgetPilot</h2>
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
        <n-space :size="8">
          <n-badge :value="alertCount" :show="alertCount > 0">
            <n-button quaternary circle size="small" @click="router.push('/alerts')">
              <template #icon><n-icon><notifications-outline /></n-icon></template>
            </n-button>
          </n-badge>
          <n-button type="primary" size="small" @click="router.push('/transactions/add')" class="add-btn">
            <template #icon><n-icon><add-outline /></n-icon></template>
            <span class="add-text">记一笔</span>
          </n-button>
        </n-space>
      </n-layout-header>
      <n-layout-content content-style="padding: 16px;">
        <router-view />
      </n-layout-content>
    </n-layout>
  </n-layout>
</template>

<script setup>
import { ref, computed, h, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  HomeOutline, ReceiptOutline, WalletOutline, FolderOutline,
  BarChartOutline, SettingsOutline, AddOutline, NotificationsOutline, MenuOutline, RepeatOutline
} from '@vicons/ionicons5'
import { NIcon } from 'naive-ui'

const router = useRouter()
const route = useRoute()
const collapsed = ref(false)

// 移动端默认收起侧边栏
onMounted(() => {
  if (window.innerWidth < 768) {
    collapsed.value = true
  }
})

const renderIcon = (icon) => () => h(NIcon, null, { default: () => h(icon) })

const menuOptions = [
  { label: '首页', key: 'Dashboard', icon: renderIcon(HomeOutline) },
  { label: '交易', key: 'Transactions', icon: renderIcon(ReceiptOutline) },
  { label: '账户', key: 'Accounts', icon: renderIcon(WalletOutline) },
  { label: '分类', key: 'Categories', icon: renderIcon(FolderOutline) },
  { label: '预算', key: 'Budget', icon: renderIcon(BarChartOutline) },
  { label: '周期', key: 'Recurring', icon: renderIcon(RepeatOutline) },
  { label: '报表', key: 'Reports', icon: renderIcon(BarChartOutline) },
  { label: '设置', key: 'Settings', icon: renderIcon(SettingsOutline) }
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
    Categories: '分类管理',
    Budget: '预算管理',
    Recurring: '周期交易',
    Reports: '统计报表',
    Settings: '系统设置'
  }
  return map[route.name] || 'BudgetPilot'
})

const navigate = (key) => {
  router.push({ name: key })
  // 移动端导航后自动收起侧边栏
  if (window.innerWidth < 768) {
    collapsed.value = true
  }
}
</script>

<style scoped>
.logo {
  padding: 16px;
  text-align: center;
  border-bottom: 1px solid #eee;
}
.logo h2 {
  color: #2c3e50;
  font-size: 18px;
  font-weight: 700;
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
