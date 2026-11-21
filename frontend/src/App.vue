<template>
  <n-config-provider :theme-overrides="themeOverrides" :locale="zhCN" :date-locale="dateZhCN">
    <n-message-provider>
      <n-dialog-provider>
        <n-notification-provider>
          <n-loading-bar-provider>
            <router-view />
          </n-loading-bar-provider>
        </n-notification-provider>
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup>
import { zhCN, dateZhCN } from 'naive-ui'

const themeOverrides = {
  common: {
    primaryColor: '#2563EB',
    primaryColorHover: '#1D4ED8',
    primaryColorPressed: '#1E40AF',
    primaryColorSuppl: '#2563EB',
    infoColor: '#2563EB',
    successColor: '#10B981',
    warningColor: '#F59E0B',
    errorColor: '#EF4444',
    borderRadius: '10px',
    borderRadiusSmall: '8px',
    fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif",
    textColorBase: '#0F172A',
    bodyColor: '#F5F7FB'
  },
  Card: {
    borderRadius: '16px',
    color: '#FFFFFF',
    boxShadow: '0 1px 2px rgba(15, 23, 42, 0.04), 0 12px 32px -12px rgba(15, 23, 42, 0.12)',
    paddingMedium: '20px 22px',
    titleFontSizeMedium: '15px',
    titleFontWeight: '600'
  },
  Button: {
    borderRadiusMedium: '10px',
    borderRadiusLarge: '12px',
    fontWeight: '600'
  },
  DataTable: {
    thColor: '#F8FAFC',
    thTextColor: '#64748B',
    tdColorHover: '#F8FAFC',
    borderColor: '#EEF2F7'
  },
  Menu: {
    itemHeight: '42px',
    borderRadius: '10px'
  }
}
</script>

<style>
:root {
  --bp-bg: #f5f7fb;
  --bp-surface: #ffffff;
  --bp-ink: #0f172a;
  --bp-muted: #64748b;
  --bp-line: #eef2f7;
  --bp-primary: #2563eb;
  --bp-primary-soft: rgba(37, 99, 235, 0.1);
  --bp-teal: #0ea5a4;
  --bp-shadow: 0 1px 2px rgba(15, 23, 42, 0.04), 0 12px 32px -12px rgba(15, 23, 42, 0.12);
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html,
body,
#app {
  min-height: 100%;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC',
    'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  color: var(--bp-ink);
  background:
    radial-gradient(1100px 560px at 100% -8%, rgba(37, 99, 235, 0.08), transparent 60%),
    radial-gradient(900px 520px at -10% 110%, rgba(14, 165, 164, 0.07), transparent 55%),
    var(--bp-bg);
  -webkit-font-smoothing: antialiased;
  text-rendering: optimizeLegibility;
}

a {
  color: var(--bp-primary);
}

::selection {
  background: rgba(37, 99, 235, 0.18);
}

::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}
::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 999px;
  border: 3px solid transparent;
  background-clip: content-box;
}
::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
  background-clip: content-box;
}

.n-card {
  overflow: hidden;
}
.n-card > .n-card-header {
  border-bottom: 1px solid var(--bp-line);
  padding-bottom: 14px;
  margin-bottom: 16px;
}

/* 移动端自定义卡片统一圆角与阴影 */
.account-card,
.budget-card,
.chart-card,
.section-card,
.rule-card,
.merchant-card,
.cat-item {
  border-radius: 14px !important;
  box-shadow: var(--bp-shadow);
}
</style>