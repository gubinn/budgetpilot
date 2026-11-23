<template>
  <div class="card-entry" role="button" tabindex="0" @click="open" @keydown.enter="open">
    <div class="ce-main">
      <div class="ce-title">
        卡券包
        <span class="ce-note">表外备忘 · 不计入总资产</span>
      </div>
      <div class="ce-meta">
        <span v-if="summary.activeCount">{{ summary.activeCount }} 张有效</span>
        <span v-else>暂无有余额的卡券</span>
        <span v-if="summary.expiringSoonCount" class="soon">{{ summary.expiringSoonCount }} 张将到期</span>
        <span v-if="summary.expiredCount" class="expired">{{ summary.expiredCount }} 张已过期</span>
      </div>
    </div>
    <div class="ce-amount">
      <template v-if="moneyBalance > 0">¥ {{ formatNum(moneyBalance) }}</template>
      <template v-else-if="countBalance > 0">{{ formatCount(countBalance) }} 次 / 张</template>
      <template v-else>—</template>
    </div>
    <div class="ce-arrow">明细 →</div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { prepaidCardApi } from '@/api'
import { formatNum } from '@/utils/display'

const router = useRouter()
const summary = ref({
  moneyBalanceTotal: 0,
  countBalanceTotal: 0,
  activeCount: 0,
  expiringSoonCount: 0,
  expiredCount: 0
})

const moneyBalance = computed(() => Number(summary.value.moneyBalanceTotal || 0))
const countBalance = computed(() => Number(summary.value.countBalanceTotal || 0))

function formatCount(value) {
  return Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 0 })
}

function open() {
  router.push({ name: 'PrepaidCards' })
}

onMounted(async () => {
  try {
    const res = await prepaidCardApi.summary()
    if (res.data) {
      summary.value = res.data
    }
  } catch {
    // 账户页不因卡券接口失败而报错
  }
})
</script>

<style scoped>
.card-entry {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 2px;
  padding: 12px 4px 6px;
  border-top: 1px solid #eef2f7;
  border-radius: 0 0 8px 8px;
  color: #64748b;
  cursor: pointer;
  transition: background 0.15s;
}
.card-entry:hover {
  background: #f8fafc;
}
.card-entry:focus-visible {
  outline: 2px solid #cbd5e1;
  outline-offset: 2px;
}
.ce-main {
  flex: 1;
  min-width: 0;
}
.ce-title {
  display: flex;
  align-items: baseline;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
}
.ce-note {
  font-size: 11px;
  font-weight: 400;
  color: #94a3b8;
}
.ce-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 2px 10px;
  margin-top: 2px;
  font-size: 11px;
  color: #94a3b8;
}
.ce-meta .soon {
  color: #d97706;
}
.ce-meta .expired {
  color: #dc2626;
}
.ce-amount {
  flex-shrink: 0;
  font-size: 15px;
  font-weight: 600;
  color: #475569;
  font-variant-numeric: tabular-nums;
}
.ce-arrow {
  flex-shrink: 0;
  font-size: 12px;
  color: #94a3b8;
}

@media (max-width: 640px) {
  .card-entry {
    align-items: flex-start;
    margin-top: 0;
    padding: 12px 2px 4px;
  }
  .ce-note {
    display: none;
  }
  .ce-arrow {
    display: none;
  }
  .ce-amount {
    font-size: 14px;
  }
}
</style>