<template>
  <div class="receivable-entry" :class="{ empty: !hasBalance }" role="button" tabindex="0"
       @click="open" @keydown.enter="open">
    <div class="re-main">
      <div class="re-title">
        应收款
        <span class="re-note">往来款 · 不计入账户余额</span>
      </div>
      <div v-if="hasBalance" class="re-meta">
        <span>{{ summary.counterpartyCount }} 人未结清</span>
        <span v-if="summary.oldestDays > 0">最长 {{ summary.oldestDays }} 天</span>
        <span v-if="summary.payableTotal > 0">应付 ¥{{ formatNum(summary.payableTotal) }}</span>
        <span v-if="summary.overdueCount > 0" class="overdue">{{ summary.overdueCount }} 项逾期</span>
      </div>
      <div v-else class="re-meta">暂无未结清往来款</div>
    </div>
    <div class="re-amount" :class="{ zero: !hasBalance }">
      ¥ {{ formatNum(summary.receivableTotal) }}
    </div>
    <div class="re-arrow">明细 →</div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { receivableApi } from '@/api'
import { formatNum } from '@/utils/display'

const router = useRouter()
const summary = ref({
  receivableTotal: 0,
  payableTotal: 0,
  counterpartyCount: 0,
  oldestDays: 0,
  overdueCount: 0
})

const hasBalance = computed(() =>
  Number(summary.value.receivableTotal || 0) > 0
  || Number(summary.value.payableTotal || 0) > 0
)

function open() {
  router.push({ name: 'Receivables' })
}

onMounted(async () => {
  try {
    const res = await receivableApi.summary()
    if (res.data) {
      summary.value = res.data
    }
  } catch {
    // 账户页不因往来款接口失败而报错
  }
})
</script>

<style scoped>
.receivable-entry {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  padding: 14px 4px 6px;
  border-top: 1px solid #eef2f7;
  border-radius: 0 0 8px 8px;
  color: #64748b;
  cursor: pointer;
  transition: background 0.15s;
}
.receivable-entry:hover {
  background: #f8fafc;
}
.receivable-entry:focus-visible {
  outline: 2px solid #cbd5e1;
  outline-offset: 2px;
}
.receivable-entry.empty {
  color: #94a3b8;
}
.re-main {
  flex: 1;
  min-width: 0;
}
.re-title {
  display: flex;
  align-items: baseline;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
}
.receivable-entry.empty .re-title {
  color: #94a3b8;
}
.re-note {
  font-size: 11px;
  font-weight: 400;
  color: #94a3b8;
}
.re-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 2px 10px;
  margin-top: 2px;
  font-size: 11px;
  color: #94a3b8;
}
.re-meta .overdue {
  color: #dc2626;
}
.re-amount {
  flex-shrink: 0;
  font-size: 15px;
  font-weight: 600;
  color: #475569;
  font-variant-numeric: tabular-nums;
}
.re-amount.zero {
  font-weight: 400;
  color: #94a3b8;
}
.re-arrow {
  flex-shrink: 0;
  font-size: 12px;
  color: #94a3b8;
}

@media (max-width: 640px) {
  .receivable-entry {
    align-items: flex-start;
    margin-top: 12px;
    padding: 12px 2px 4px;
  }
  .re-note {
    display: none;
  }
  .re-arrow {
    display: none;
  }
  .re-amount {
    font-size: 14px;
  }
}
</style>