<template>
  <n-drawer
    :show="show"
    :placement="mobile ? 'bottom' : 'right'"
    :height="mobile ? '86%' : undefined"
    :width="mobile ? undefined : 760"
    @update:show="$emit('update:show', $event)"
  >
    <n-drawer-content title="净资产月度快照" closable>
      <div class="nw-root" :class="{ mobile }">
      <div class="nw-head">
        <n-select
          v-model:value="month"
          :options="monthOptions"
          size="small"
          style="width: 210px"
          placeholder="选择月份"
          @update:value="load"
        />
        <div class="nw-summary">
          <div class="nw-label">
            {{ data?.label || '—' }}
            <span v-if="data?.currentMonth" class="nw-badge live">实时余额</span>
            <span v-else class="nw-badge">月末快照</span>
          </div>
          <div class="nw-value">{{ data ? signedMoney(data.netWorth) : '—' }}</div>
          <div v-if="data" class="nw-change" :class="changeTone">
            {{ changeText }}
          </div>
        </div>
      </div>

      <n-spin :show="loading">
        <template v-if="data && data.accounts && data.accounts.length">
          <div class="nw-cap">共 {{ data.accounts.length }} 个账户 · 按余额绝对值排序</div>
          <table class="nw-table">
            <thead>
              <tr>
                <th>账户</th>
                <th class="type">类型</th>
                <th class="n">本期余额</th>
                <th class="n prev">上期余额</th>
                <th class="n">变化</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="a in data.accounts" :key="a.accountId">
                <td class="nw-name">{{ a.name }}</td>
                <td class="type"><span class="nw-badge">{{ a.type }}</span></td>
                <td class="n" :class="{ neg: Number(a.balance) < 0 }">{{ signedMoney(a.balance) }}</td>
                <td class="n prev" :class="{ neg: Number(a.prevBalance) < 0 }">{{ signedMoney(a.prevBalance) }}</td>
                <td class="n" :class="a.change > 0 ? 'pos' : a.change < 0 ? 'neg' : 'flat'">
                  {{ Number(a.change) === 0 ? '—' : (a.change > 0 ? '+' : '') + signedMoney(a.change) }}
                </td>
              </tr>
            </tbody>
            <tfoot>
              <tr>
                <td :colspan="mobile ? 1 : 2">合计</td>
                <td class="n">{{ signedMoney(data.netWorth) }}</td>
                <td class="n prev">{{ signedMoney(data.prevNetWorth) }}</td>
                <td class="n" :class="data.change > 0 ? 'pos' : data.change < 0 ? 'neg' : 'flat'">
                  {{ Number(data.change) === 0 ? '—' : (data.change > 0 ? '+' : '') + signedMoney(data.change) }}
                </td>
              </tr>
            </tfoot>
          </table>
        </template>
        <n-empty v-else-if="!loading" description="这个月还没有快照数据" />
      </n-spin>

      <p class="nw-note">
        历史月份是月末余额快照，当前月是实时余额。快照每天 02:50 按校准后的余额重算最近 12 个月；
        修改历史流水后，第二天会自动修正。
      </p>
      </div>
    </n-drawer-content>
  </n-drawer>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useMessage } from 'naive-ui'
import { reportApi } from '@/api'

const props = defineProps({
  show: { type: Boolean, default: false },
  mobile: { type: Boolean, default: false }
})
const emit = defineEmits(['update:show'])

const message = useMessage()
const month = ref('')
const data = ref(null)
const loading = ref(false)
let seq = 0

const monthOptions = computed(() => (data.value?.months || []).map(m => ({
  label: m.label,
  value: m.month
})))
const changeTone = computed(() => {
  const c = Number(data.value?.change || 0)
  return c > 0 ? 'pos' : c < 0 ? 'neg' : 'flat'
})
const changeText = computed(() => {
  const c = Number(data.value?.change || 0)
  if (c === 0) return '较上期持平'
  return `较上期 ${c > 0 ? '+' : ''}${signedMoney(c)}`
})

const nf0 = new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 0 })
function signedMoney(n) {
  const v = Number(n) || 0
  return (v < 0 ? '-¥' : '¥') + nf0.format(Math.abs(v))
}

function load(value) {
  const target = value || month.value || ''
  const id = ++seq
  loading.value = true
  reportApi.netWorth(target || undefined).then(res => {
    if (id !== seq) return
    data.value = res.data
    month.value = res.data.month
  }).catch(e => {
    if (id === seq) message.error(e.message || '加载净资产快照失败')
  }).finally(() => {
    if (id === seq) loading.value = false
  })
}

watch(() => props.show, v => {
  if (v) load()
})
</script>

<style scoped>
.nw-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}
.nw-summary { text-align: right; }
.nw-label { font-size: 12px; color: var(--bp-muted); display: flex; align-items: center; gap: 6px; justify-content: flex-end; }
.nw-value { margin-top: 4px; font-size: 30px; font-weight: 750; letter-spacing: -0.02em; }
.nw-change { margin-top: 2px; font-size: 12px; font-weight: 600; }
.nw-change.pos { color: #067647; }
.nw-change.neg { color: #b42318; }
.nw-change.flat { color: var(--bp-muted); }
.nw-badge {
  display: inline-block; font-size: 10px; line-height: 16px; padding: 0 5px;
  border-radius: 4px; background: #f1f5f9; color: #475467; font-weight: 500;
}
.nw-badge.live { background: #ecfdf3; color: #067647; }
.nw-cap { margin: 18px 0 6px; font-size: 11px; color: var(--bp-muted); }
.nw-table { width: 100%; border-collapse: collapse; font-size: 12px; }
.nw-table th, .nw-table td { padding: 8px 8px; border-bottom: 1px solid #f2f4f7; text-align: left; white-space: nowrap; }
.nw-table th { color: var(--bp-muted); font-weight: 600; font-size: 11px; background: #fafbfc; }
.nw-table .n { text-align: right; font-variant-numeric: tabular-nums; }
.nw-table .nw-name { max-width: 260px; overflow: hidden; text-overflow: ellipsis; }
.nw-table .pos { color: #067647; font-weight: 600; }
.nw-table .neg { color: #b42318; }
.nw-table .flat { color: var(--bp-muted); }
.nw-table tfoot td { border-top: 1px solid var(--bp-line); border-bottom: 0; font-weight: 700; background: #fafbfc; }
.nw-note { margin-top: 18px; font-size: 11px; line-height: 1.7; color: var(--bp-muted); }
.nw-root.mobile .nw-head { flex-direction: column; }
.nw-root.mobile .nw-summary { text-align: left; width: 100%; }
.nw-root.mobile .nw-label { justify-content: flex-start; }
.nw-root.mobile .nw-table { width: 100%; font-size: 11px; }
.nw-root.mobile .nw-table .prev,
.nw-root.mobile .nw-table .type { display: none; }
.nw-root.mobile .nw-table .nw-name { max-width: 170px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.nw-root.mobile .nw-table th, .nw-root.mobile .nw-table td { padding: 7px 4px; }
.nw-root.mobile .nw-badge { font-size: 9px; padding: 0 4px; }
.nw-root.mobile .nw-value { font-size: 26px; }
</style>