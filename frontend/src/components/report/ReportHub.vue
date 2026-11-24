<template>
  <div class="report-hub">
    <div class="report-hub-switch" role="tablist" aria-label="报表视图">
      <button
        type="button"
        role="tab"
        :aria-selected="view === 'monthly'"
        :class="{ active: view === 'monthly' }"
        @click="setView('monthly')"
      >月度复盘</button>
      <button
        type="button"
        role="tab"
        :aria-selected="view === 'annual'"
        :class="{ active: view === 'annual' }"
        @click="setView('annual')"
      >年度复盘</button>
    </div>
    <ReportOverview v-if="view === 'monthly'" :mobile="mobile" />
    <AnnualOverview v-else :mobile="mobile" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ReportOverview from '@/components/report/ReportOverview.vue'
import AnnualOverview from '@/components/report/annual/AnnualOverview.vue'

defineProps({
  mobile: { type: Boolean, default: false }
})

const route = useRoute()
const router = useRouter()

const view = computed(() => route.query.view === 'annual' ? 'annual' : 'monthly')

function setView(next) {
  if (view.value === next) return
  const query = { ...route.query }
  if (next === 'annual') {
    query.view = 'annual'
  } else {
    delete query.view
  }
  router.replace({ query })
}
</script>

<style scoped>
.report-hub {
  min-width: 0;
}
.report-hub-switch {
  display: inline-flex;
  gap: 2px;
  padding: 3px;
  margin-bottom: 14px;
  border-radius: 9px;
  background: #eef2f7;
}
.report-hub-switch button {
  border: 0;
  background: transparent;
  padding: 6px 14px;
  border-radius: 7px;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.report-hub-switch button:hover {
  color: var(--bp-ink);
}
.report-hub-switch button.active {
  background: var(--bp-surface);
  color: var(--bp-ink);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.08);
}
.report-hub-switch button:focus-visible {
  outline: 2px solid var(--bp-primary);
  outline-offset: 1px;
}
@media (max-width: 720px) {
  .report-hub-switch {
    display: flex;
    width: 100%;
  }
  .report-hub-switch button {
    flex: 1;
    padding: 8px;
    font-size: 13px;
  }
}
</style>