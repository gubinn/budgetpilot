<template>
  <div class="prepaid-page">
    <n-card :bordered="false" class="cards-shell">
      <div class="page-head">
        <div class="head-text">
          <div class="head-title">卡券包</div>
          <div class="head-sub">表外备忘 · 不计入总资产</div>
        </div>
        <n-button v-if="!auth.isReadOnly" type="primary" @click="openCreate">
          <template #icon><n-icon><AddOutline /></n-icon></template>
          记一张卡券
        </n-button>
      </div>

      <!-- 很轻的汇总，不做大屏 -->
      <div v-if="summary.totalCount" class="summary-strip">
        <div class="sum-item">
          <div class="sum-label">可用余额</div>
          <div class="sum-value">¥ {{ formatNum(summary.moneyBalanceTotal) }}</div>
          <div class="sum-sub">
            {{ summary.activeCount }} 张有效
            <span v-if="Number(summary.countBalanceTotal) > 0">
              · 另有 {{ formatCount(summary.countBalanceTotal) }} 次 / 张
            </span>
          </div>
        </div>
        <div v-if="summary.expiringSoonCount" class="sum-item soon">
          <div class="sum-label">30 天内到期</div>
          <div class="sum-value">{{ summary.expiringSoonCount }} 张</div>
          <div class="sum-sub">{{ moneyAndCount(summary.expiringSoonMoney, summary.expiringSoonCountValue) }}</div>
        </div>
        <div v-if="summary.expiredCount" class="sum-item expired">
          <div class="sum-label">已过期未用</div>
          <div class="sum-value">{{ summary.expiredCount }} 张</div>
          <div class="sum-sub">{{ moneyAndCount(summary.expiredMoney, summary.expiredCountValue) }}</div>
        </div>
        <div v-if="summary.dormantCount" class="sum-item muted">
          <div class="sum-label">长期未用</div>
          <div class="sum-value">{{ summary.dormantCount }} 张</div>
          <div class="sum-sub">90 天以上没动过</div>
        </div>
      </div>

      <!-- 快捷筛选，复杂条件收起 -->
      <div class="filter-bar">
        <div class="chip-line">
          <button
            v-for="opt in stateOptions"
            :key="opt.value"
            class="q-chip"
            :class="{ active: stateFilter === opt.value }"
            @click="setStateFilter(opt.value)"
          >{{ opt.label }}</button>
        </div>

        <div class="search-line">
          <n-input
            v-model:value="keyword"
            placeholder="搜名称 / 发卡方 / 账号"
            clearable
            size="small"
            class="search-input"
          >
            <template #prefix><n-icon :size="15"><SearchOutline /></n-icon></template>
          </n-input>
          <button class="q-chip filter-switch" :class="{ active: moreOpen }" @click="moreOpen = !moreOpen">
            <n-icon :size="15"><OptionsOutline /></n-icon>
            筛选
            <span v-if="advancedCount" class="filter-count">{{ advancedCount }}</span>
          </button>
        </div>

        <div v-if="activeChips.length" class="chip-line applied-line">
          <button v-for="chip in activeChips" :key="chip.key" class="q-chip ghost" @click="clearChip(chip.key)">
            {{ chip.label }}
            <span class="chip-x">×</span>
          </button>
          <button class="q-chip ghost clear-chip" @click="resetFilters">清空筛选</button>
        </div>

        <n-collapse-transition :show="moreOpen">
          <div class="more-panel">
            <n-form :model="filters" label-placement="left" :show-feedback="false">
              <div class="filter-grid">
                <n-form-item label="渠道" :label-width="60" class="filter-field">
                  <n-select v-model:value="filters.channel" :options="channelOptions" clearable placeholder="全部渠道" />
                </n-form-item>
                <n-form-item label="类型" :label-width="60" class="filter-field">
                  <n-select v-model:value="filters.kind" :options="kindOptions" clearable placeholder="全部类型" />
                </n-form-item>
                <n-form-item label="来源" :label-width="60" class="filter-field">
                  <n-select v-model:value="filters.source" :options="sourceOptions" clearable placeholder="全部来源" />
                </n-form-item>
              </div>
            </n-form>
          </div>
        </n-collapse-transition>
      </div>

      <!-- 列表 -->
      <div v-if="loading" class="loading-block">
        <n-spin size="small" /> 正在整理卡券…
      </div>

      <template v-else-if="cards.length">
        <div v-if="!filteredCards.length" class="empty-state small">
          <div class="empty-title">没有符合条件的卡券</div>
          <n-button quaternary size="small" @click="resetFilters">清空筛选</n-button>
        </div>

        <template v-else>
          <section v-for="group in visibleGroups" :key="group.key" class="card-group">
            <div class="group-head">
              <span class="group-title" :class="group.key">{{ group.title }}</span>
              <span class="group-note">{{ group.note }}</span>
            </div>
            <div class="card-grid">
              <div
                v-for="card in group.items"
                :key="card.id"
                class="p-card"
                :class="cardClass(card)"
                role="button"
                tabindex="0"
                @click="openDetail(card)"
                @keydown.enter="openDetail(card)"
              >
                <div class="p-top">
                  <div class="p-avatar" :class="issuerTone(card.issuer)">{{ issuerInitial(card.issuer) }}</div>
                  <div class="p-name-wrap">
                    <div class="p-name">{{ card.name }}</div>
                    <div class="p-issuer">
                      {{ card.issuer }}<span v-if="card.channel"> · {{ card.channel }}</span>
                    </div>
                  </div>
                  <n-dropdown
                    v-if="!auth.isReadOnly"
                    trigger="click"
                    placement="bottom-end"
                    :options="tileOptions(card)"
                    @select="key => onTileAction(key, card)"
                  >
                    <button class="p-more" aria-label="卡券操作" @click.stop>
                      <n-icon :size="18"><EllipsisHorizontal /></n-icon>
                    </button>
                  </n-dropdown>
                </div>

                <div class="p-value-row">
                  <div class="p-value">{{ balanceText(card) }}</div>
                  <div v-if="faceText(card)" class="p-face">{{ faceText(card) }}</div>
                </div>

                <div class="p-meta">
                  <span v-if="card.accountHint" class="p-account">{{ card.accountHint }}</span>
                  <span class="p-expiry" :class="expiryClass(card)">{{ expiryText(card) }}</span>
                </div>

                <div v-if="!auth.isReadOnly" class="p-actions" @click.stop>
                  <n-button size="tiny" secondary :disabled="card.state !== 'ACTIVE'" @click="openAdjust(card, 'use')">
                    用一次
                  </n-button>
                  <n-button size="tiny" secondary :disabled="card.state === 'EXPIRED' || card.state === 'VOID'" @click="openAdjust(card, 'recharge')">
                    充值
                  </n-button>
                </div>
              </div>
            </div>
          </section>

          <button v-if="inactiveCount && !isInactiveFilter && !showInactiveEffective" class="inactive-toggle" @click="showInactive = true">
            查看已用完 / 已过期 / 已作废（{{ inactiveCount }}）
            <n-icon :size="15"><ChevronDownOutline /></n-icon>
          </button>
          <button v-else-if="inactiveCount && !isInactiveFilter && showInactive" class="inactive-toggle" @click="showInactive = false">
            收起已用完 / 已过期 / 已作废
            <n-icon :size="15"><ChevronUpOutline /></n-icon>
          </button>
        </template>
      </template>

      <div v-else class="empty-state">
        <div class="empty-icon"><n-icon :size="30"><TicketOutline /></n-icon></div>
        <div class="empty-title">把散落在各个小程序里的钱，收进这里</div>
        <div class="empty-sub">购物卡、电影票、储值余额……记一笔有效期，到期前我会提醒你。</div>
        <n-button v-if="!auth.isReadOnly" type="primary" round @click="openCreate">记一张卡券</n-button>
      </div>
    </n-card>

    <!-- 详情 -->
    <n-drawer
      v-model:show="showDetail"
      :placement="isMobile ? 'bottom' : 'right'"
      :width="isMobile ? undefined : 560"
      :height="isMobile ? '88vh' : undefined"
    >
      <n-drawer-content :title="detailCard?.name || '卡券详情'" closable>
        <template v-if="detailCard">
          <div class="detail-hero">
            <div class="p-avatar large" :class="issuerTone(detailCard.issuer)">{{ issuerInitial(detailCard.issuer) }}</div>
            <div class="detail-hero-main">
              <div class="detail-hero-name">{{ detailCard.name }}</div>
              <div class="detail-hero-issuer">{{ detailCard.issuer }}<span v-if="detailCard.channel"> · {{ detailCard.channel }}</span></div>
            </div>
            <n-tag :type="stateTagType(detailCard)" size="small" :bordered="false">{{ detailCard.stateName }}</n-tag>
          </div>

          <div class="detail-balance">
            <div class="detail-balance-value">{{ balanceText(detailCard) }}</div>
            <div class="detail-balance-sub">
              <span v-if="faceText(detailCard)">{{ faceText(detailCard) }}</span>
              <span class="detail-expiry" :class="expiryClass(detailCard)">{{ expiryText(detailCard) }}</span>
            </div>
          </div>

          <div class="detail-grid">
            <div class="detail-cell">
              <div class="cell-label">发卡方</div>
              <div class="cell-value">{{ detailCard.issuer }}</div>
            </div>
            <div class="detail-cell">
              <div class="cell-label">类型 / 单位</div>
              <div class="cell-value">{{ detailCard.kindName }} · {{ detailCard.unitName }}</div>
            </div>
            <div class="detail-cell">
              <div class="cell-label">存放渠道</div>
              <div class="cell-value">{{ detailCard.channel || '未填写' }}</div>
            </div>
            <div class="detail-cell">
              <div class="cell-label">所在账号</div>
              <div class="cell-value">{{ detailCard.accountHint || '未填写' }}</div>
            </div>
            <div class="detail-cell">
              <div class="cell-label">有效期至</div>
              <div class="cell-value" :class="expiryClass(detailCard)">{{ detailCard.expiryDate || '长期有效' }}</div>
            </div>
            <div class="detail-cell">
              <div class="cell-label">来源</div>
              <div class="cell-value">{{ detailCard.sourceName }}</div>
            </div>
            <div v-if="detailCard.lastUsedAt" class="detail-cell" :class="{ wide: !detailCard.note }">
              <div class="cell-label">最近使用</div>
              <div class="cell-value">{{ detailCard.lastUsedAt }}</div>
            </div>
            <div v-if="detailCard.note" class="detail-cell wide">
              <div class="cell-label">备注</div>
              <div class="cell-value">{{ detailCard.note }}</div>
            </div>
          </div>

          <div class="usage-head">
            <span>使用流水</span>
            <span class="usage-count">{{ usages.length }} 条</span>
          </div>
          <div v-if="usageLoading" class="usage-loading"><n-spin size="small" /></div>
          <div v-else-if="usages.length" class="usage-list">
            <div v-for="u in usages" :key="u.id" class="usage-row">
              <div class="usage-date">{{ u.occurredAt }}</div>
              <div class="usage-main">
                <div class="usage-line">
                  <span class="usage-action">{{ u.actionName }}</span>
                  <span v-if="Number(u.delta) !== 0" class="usage-delta" :class="{ plus: Number(u.delta) > 0 }">
                    {{ signedValue(u.delta, detailCard.unit) }}
                  </span>
                  <span class="usage-after">余 {{ valueText(u.balanceAfter, detailCard.unit) }}</span>
                </div>
                <div v-if="u.note" class="usage-note">{{ u.note }}</div>
              </div>
            </div>
          </div>
          <div v-else class="usage-empty">还没有流水</div>
        </template>

        <template #footer>
          <div v-if="detailCard && !auth.isReadOnly" class="detail-actions">
            <n-button type="primary" :disabled="detailCard.state !== 'ACTIVE'" @click="openAdjust(detailCard, 'use')">用一次</n-button>
            <n-button :disabled="detailCard.state === 'EXPIRED' || detailCard.state === 'VOID'" @click="openAdjust(detailCard, 'recharge')">充值</n-button>
            <n-button quaternary @click="editFromDetail">编辑</n-button>
            <n-dropdown trigger="click" placement="top-end" :options="detailOptions" @select="onDetailAction">
              <n-button quaternary>更多</n-button>
            </n-dropdown>
          </div>
        </template>
      </n-drawer-content>
    </n-drawer>

    <!-- 新增/编辑（桌面弹窗、移动端抽屉，不跳页） -->
    <n-modal
      v-if="!isMobile"
      v-model:show="showForm"
      preset="card"
      :title="editingId ? '编辑卡券' : '记一张卡券'"
      class="prepaid-form-modal"
      style="width: 640px"
      :mask-closable="false"
    >
      <PrepaidCardFormBody
        v-if="showForm"
        ref="formRef"
        :card-id="editingId"
        :actions="false"
        @saved="onFormSaved"
        @cancel="showForm = false"
      />
      <template #footer>
        <n-space justify="end">
          <n-button @click="showForm = false">取消</n-button>
          <n-button type="primary" :loading="formRef?.submitting" @click="formRef?.submit()">
            {{ editingId ? '保存' : '记一张' }}
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <n-drawer v-else v-model:show="showForm" placement="bottom" height="92vh">
      <n-drawer-content :title="editingId ? '编辑卡券' : '记一张卡券'" closable>
        <PrepaidCardFormBody
          v-if="showForm"
          ref="formRef"
          :card-id="editingId"
          :actions="false"
          @saved="onFormSaved"
          @cancel="showForm = false"
        />
        <template #footer>
          <div class="m-sheet-actions">
            <n-button type="primary" :loading="formRef?.submitting" @click="formRef?.submit()">
              {{ editingId ? '保存' : '记一张' }}
            </n-button>
            <n-button quaternary @click="showForm = false">取消</n-button>
          </div>
        </template>
      </n-drawer-content>
    </n-drawer>

    <!-- 使用 / 充值 -->
    <n-modal
      v-model:show="showAdjust"
      preset="card"
      :title="adjustMode === 'use' ? `用一次 · ${adjustCard?.name || ''}` : `充值 · ${adjustCard?.name || ''}`"
      :style="{ width: isMobile ? '92vw' : '420px' }"
      :mask-closable="false"
    >
      <div v-if="adjustCard" class="adjust-hint">
        <span>当前{{ adjustCard.unit === 2 ? '剩余' : '余额' }}</span>
        <strong>{{ balanceText(adjustCard) }}</strong>
      </div>
      <n-form label-placement="top" :show-feedback="false">
        <n-form-item :label="adjustMode === 'use' ? (adjustCard?.unit === 2 ? '用掉几次' : '用掉多少') : (adjustCard?.unit === 2 ? '增加几次' : '充值金额')">
          <n-input-number
            v-model:value="adjustValue"
            :min="0.01"
            :max="adjustMode === 'use' ? Number(adjustCard?.balance || 0) : undefined"
            :precision="adjustCard?.unit === 2 ? 0 : 2"
            :step="adjustCard?.unit === 2 ? 1 : 0.01"
            :show-button="false"
            placeholder="0.00"
            style="width: 100%"
          >
            <template v-if="adjustCard?.unit === 1" #prefix>¥</template>
            <template v-else #suffix>次</template>
          </n-input-number>
        </n-form-item>
        <n-form-item label="日期">
          <n-date-picker
            v-model:formatted-value="adjustDate"
            type="date"
            value-format="yyyy-MM-dd"
            style="width: 100%"
          />
        </n-form-item>
        <n-form-item label="备注">
          <n-input v-model:value="adjustNote" maxlength="100" placeholder="可选，比如：和同事看电影" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showAdjust = false">取消</n-button>
          <n-button type="primary" :loading="adjusting" @click="submitAdjust">
            {{ adjustMode === 'use' ? '确认使用' : '确认充值' }}
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useDialog, useMessage } from 'naive-ui'
import {
  AddOutline, ChevronDownOutline, ChevronUpOutline,
  EllipsisHorizontal, OptionsOutline, SearchOutline, TicketOutline
} from '@vicons/ionicons5'
import dayjs from 'dayjs'
import { prepaidCardApi } from '@/api'
import PrepaidCardFormBody from '@/components/prepaid/PrepaidCardFormBody.vue'
import { formatNum } from '@/utils/display'
import { useCompactViewport } from '@/composables/useCompactViewport'
import { useAuthStore } from '@/stores/auth'

const message = useMessage()
const auth = useAuthStore()
const dialog = useDialog()
const isMobile = useCompactViewport(767)

const loading = ref(false)
const cards = ref([])
const summary = ref(emptySummary())

const stateFilter = ref('ALL')
const keyword = ref('')
const moreOpen = ref(false)
const showInactive = ref(false)
const filters = ref({ channel: null, kind: null, source: null })

const showDetail = ref(false)
const detailCard = ref(null)
const usages = ref([])
const usageLoading = ref(false)

const showForm = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const showAdjust = ref(false)
const adjustMode = ref('use')
const adjustCard = ref(null)
const adjustValue = ref(null)
const adjustDate = ref(dayjs().format('YYYY-MM-DD'))
const adjustNote = ref('')
const adjusting = ref(false)

const stateOptions = [
  { label: '全部', value: 'ALL' },
  { label: '有余额', value: 'HAS_BALANCE' },
  { label: '30 天内到期', value: 'EXPIRING' },
  { label: '已过期', value: 'EXPIRED' },
  { label: '已用完', value: 'USED_UP' },
  { label: '已作废', value: 'VOID' }
]

const kindOptions = [
  { label: '储值卡', value: 1 },
  { label: '礼品卡', value: 2 },
  { label: '兑换券', value: 3 },
  { label: '次数卡', value: 4 }
]

const channelOptions = [
  { label: '微信小程序', value: '微信小程序' },
  { label: '支付宝', value: '支付宝' },
  { label: '实体卡', value: '实体卡' },
  { label: '短信 / 邮件', value: '短信/邮件' },
  { label: 'APP', value: 'APP' },
  { label: '其他', value: '其他' }
]

const sourceOptions = [
  { label: '单位发放', value: 1 },
  { label: '自己购买', value: 2 },
  { label: '他人赠送', value: 3 },
  { label: '其他', value: 4 }
]

const isInactiveFilter = computed(() =>
  ['EXPIRED', 'USED_UP', 'VOID'].includes(stateFilter.value)
)

const hasSearchConditions = computed(() =>
  !!keyword.value.trim() || !!filters.value.channel || !!filters.value.kind || !!filters.value.source
)

const showInactiveEffective = computed(() =>
  showInactive.value || isInactiveFilter.value || stateFilter.value === 'HAS_BALANCE' || hasSearchConditions.value
)

const filteredCards = computed(() => {
  const search = keyword.value.trim().toLowerCase()
  return cards.value.filter((card) => {
    if (!matchState(card, stateFilter.value)) return false
    if (search) {
      const haystack = [card.name, card.issuer, card.channel, card.accountHint, card.note]
        .filter(Boolean).join(' ').toLowerCase()
      if (!haystack.includes(search)) return false
    }
    if (filters.value.channel && card.channel !== filters.value.channel) return false
    if (filters.value.kind && card.kind !== filters.value.kind) return false
    if (filters.value.source && card.source !== filters.value.source) return false
    return true
  })
})

const inactiveCards = computed(() => filteredCards.value.filter(card => card.state !== 'ACTIVE'))
const inactiveCount = computed(() => inactiveCards.value.length)

const visibleGroups = computed(() => {
  const list = filteredCards.value
  if (isInactiveFilter.value) {
    return [{
      key: stateFilter.value.toLowerCase(),
      title: stateLabel(stateFilter.value),
      note: `${list.length} 张`,
      items: list
    }]
  }

  const groups = []
  const expiring = list.filter(card => card.state === 'ACTIVE' && card.expiringSoon)
  const active = list.filter(card => card.state === 'ACTIVE' && !card.expiringSoon)
  const inactive = list.filter(card => card.state !== 'ACTIVE')

  if (expiring.length) {
    groups.push({ key: 'expiring', title: '即将到期', note: '30 天内 · 别让它们悄悄作废', items: expiring })
  }
  if (active.length) {
    groups.push({ key: 'active', title: '有余额', note: `${active.length} 张`, items: active })
  }
  if (inactive.length && showInactiveEffective.value) {
    const expired = inactive.filter(card => card.state === 'EXPIRED')
    const usedUp = inactive.filter(card => card.state === 'USED_UP')
    const voided = inactive.filter(card => card.state === 'VOID')
    if (expired.length) {
      groups.push({ key: 'expired', title: '已过期', note: `${expired.length} 张 · 里面可能还有没花完的钱`, items: expired })
    }
    if (usedUp.length) {
      groups.push({ key: 'usedUp', title: '已用完', note: `${usedUp.length} 张`, items: usedUp })
    }
    if (voided.length) {
      groups.push({ key: 'void', title: '已作废', note: `${voided.length} 张`, items: voided })
    }
  }
  return groups
})

const activeChips = computed(() => {
  const chips = []
  if (filters.value.channel) chips.push({ key: 'channel', label: `渠道：${filters.value.channel}` })
  if (filters.value.kind) chips.push({ key: 'kind', label: `类型：${labelOf(kindOptions, filters.value.kind)}` })
  if (filters.value.source) chips.push({ key: 'source', label: `来源：${labelOf(sourceOptions, filters.value.source)}` })
  return chips
})

const advancedCount = computed(() => activeChips.value.length)

const tileOptions = (card) => {
  const options = [
    { label: '编辑', key: 'edit' },
    { label: '用一次', key: 'use', disabled: card.state !== 'ACTIVE' },
    { label: '充值', key: 'recharge', disabled: card.state === 'EXPIRED' || card.state === 'VOID' },
    { type: 'divider', key: 'divider' }
  ]
  if (card.state === 'VOID') {
    options.push({ label: '恢复', key: 'restore' })
  } else {
    options.push({ label: '作废', key: 'void' })
  }
  options.push({ label: '彻底删除', key: 'delete' })
  return options
}

const detailOptions = computed(() => {
  if (!detailCard.value) return []
  const options = []
  if (detailCard.value.state === 'VOID') {
    options.push({ label: '恢复', key: 'restore' })
  } else {
    options.push({ label: '作废', key: 'void' })
  }
  options.push({ label: '彻底删除', key: 'delete' })
  return options
})

function emptySummary() {
  return {
    moneyBalanceTotal: 0,
    countBalanceTotal: 0,
    activeCount: 0,
    expiringSoonCount: 0,
    expiringSoonMoney: 0,
    expiringSoonCountValue: 0,
    expiredCount: 0,
    expiredMoney: 0,
    expiredCountValue: 0,
    dormantCount: 0,
    totalCount: 0
  }
}

async function loadData() {
  loading.value = true
  try {
    const [listRes, summaryRes] = await Promise.all([
      prepaidCardApi.list(),
      prepaidCardApi.summary()
    ])
    cards.value = listRes.data || []
    summary.value = summaryRes.data || emptySummary()
  } catch {
    message.error('加载卡券失败')
  } finally {
    loading.value = false
  }
}

function matchState(card, state) {
  if (!state || state === 'ALL') return true
  if (state === 'HAS_BALANCE') return Number(card.balance) > 0 && card.status === 1
  if (state === 'EXPIRING') return card.expiringSoon === true
  return card.state === state
}

function setStateFilter(value) {
  stateFilter.value = value
  showInactive.value = false
}

function clearChip(key) {
  filters.value[key] = null
}

function resetFilters() {
  stateFilter.value = 'ALL'
  keyword.value = ''
  filters.value = { channel: null, kind: null, source: null }
  moreOpen.value = false
  showInactive.value = false
}

function openCreate() {
  editingId.value = null
  showForm.value = true
}

function onFormSaved() {
  showForm.value = false
  editingId.value = null
  loadData()
}

async function openDetail(card) {
  detailCard.value = card
  showDetail.value = true
  await loadUsages(card.id)
}

async function loadUsages(cardId) {
  usageLoading.value = true
  try {
    const res = await prepaidCardApi.usages(cardId)
    usages.value = res.data || []
  } catch {
    usages.value = []
  } finally {
    usageLoading.value = false
  }
}

async function refreshDetail() {
  if (!detailCard.value) return
  try {
    const [cardRes] = await Promise.all([prepaidCardApi.get(detailCard.value.id)])
    detailCard.value = cardRes.data
    await loadUsages(detailCard.value.id)
  } catch {
    // 详情刷新失败不影响列表
  }
}

function editFromDetail() {
  if (!detailCard.value) return
  editingId.value = detailCard.value.id
  showDetail.value = false
  showForm.value = true
}

function openAdjust(card, mode) {
  if (!card) return
  adjustCard.value = card
  adjustMode.value = mode
  adjustValue.value = mode === 'use' && card.unit === 2 ? 1 : null
  adjustDate.value = dayjs().format('YYYY-MM-DD')
  adjustNote.value = ''
  showAdjust.value = true
}

async function submitAdjust() {
  if (!adjustValue.value || Number(adjustValue.value) <= 0) {
    message.error(adjustCard.value?.unit === 2 ? '请输入次数' : '请输入金额')
    return
  }
  adjusting.value = true
  try {
    const payload = {
      value: adjustValue.value,
      occurredAt: adjustDate.value,
      note: adjustNote.value.trim() || null
    }
    if (adjustMode.value === 'use') {
      await prepaidCardApi.use(adjustCard.value.id, payload)
      message.success('已记录使用')
    } else {
      await prepaidCardApi.recharge(adjustCard.value.id, payload)
      message.success('已充值')
    }
    showAdjust.value = false
    await loadData()
    if (showDetail.value) {
      await refreshDetail()
    }
  } catch (e) {
    message.error(e?.response?.data?.message || '操作失败')
  } finally {
    adjusting.value = false
  }
}

function onTileAction(key, card) {
  if (key === 'edit') {
    editingId.value = card.id
    showForm.value = true
  } else if (key === 'use' || key === 'recharge') {
    openAdjust(card, key)
  } else if (key === 'void') {
    confirmVoid(card)
  } else if (key === 'restore') {
    confirmRestore(card)
  } else if (key === 'delete') {
    confirmDelete(card)
  }
}

function onDetailAction(key) {
  if (!detailCard.value) return
  if (key === 'edit') {
    editFromDetail()
  } else if (key === 'void') {
    confirmVoid(detailCard.value)
  } else if (key === 'restore') {
    confirmRestore(detailCard.value)
  } else if (key === 'delete') {
    confirmDelete(detailCard.value)
  }
}

function confirmVoid(card) {
  dialog.warning({
    title: '作废这张卡券？',
    content: '作废后不再计入可用余额和到期提醒，历史流水会保留。',
    positiveText: '作废',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await prepaidCardApi.voidCard(card.id)
        message.success('已作废')
        showDetail.value = false
        await loadData()
      } catch (e) {
        message.error(e?.response?.data?.message || '操作失败')
      }
    }
  })
}

function confirmRestore(card) {
  dialog.info({
    title: '恢复这张卡券？',
    content: '恢复后会重新计入可用余额和到期提醒。',
    positiveText: '恢复',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await prepaidCardApi.restore(card.id)
        message.success('已恢复')
        showDetail.value = false
        await loadData()
      } catch (e) {
        message.error(e?.response?.data?.message || '操作失败')
      }
    }
  })
}

function confirmDelete(card) {
  dialog.error({
    title: '彻底删除这张卡券？',
    content: '卡券和它的使用流水都会被删除，无法恢复。只是想让它不再提醒的话，用「作废」更合适。',
    positiveText: '彻底删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await prepaidCardApi.remove(card.id)
        message.success('已删除')
        showDetail.value = false
        await loadData()
      } catch (e) {
        message.error(e?.response?.data?.message || '操作失败')
      }
    }
  })
}

function balanceText(card) {
  if (!card) return ''
  return card.unit === 2
    ? `${formatCount(card.balance)} 次 / 张`
    : `¥ ${formatNum(card.balance)}`
}

function faceText(card) {
  if (!card || card.faceValue == null || Number(card.faceValue) <= 0) return ''
  return card.unit === 2
    ? `共 ${formatCount(card.faceValue)} 次`
    : `面值 ¥${formatNum(card.faceValue)}`
}

function valueText(value, unit) {
  return unit === 2 ? `${formatCount(value)} 次` : `¥${formatNum(value)}`
}

function signedValue(value, unit) {
  const number = Number(value)
  const sign = number > 0 ? '+' : (number < 0 ? '-' : '')
  return unit === 2
    ? `${sign}${formatCount(Math.abs(number))} 次`
    : `${sign}¥${formatNum(Math.abs(number))}`
}

function formatCount(value) {
  return Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 0 })
}

function expiryText(card) {
  if (!card.expiryDate) return '长期有效'
  if (card.state === 'EXPIRED') {
    return `已过期 ${Math.abs(card.daysToExpiry || 0)} 天`
  }
  if (card.daysToExpiry === 0) return '今天到期'
  if (card.daysToExpiry != null && card.daysToExpiry <= 30) {
    return `还有 ${card.daysToExpiry} 天到期`
  }
  return `${card.expiryDate} 到期`
}

function expiryClass(card) {
  if (!card.expiryDate) return 'normal'
  if (card.state === 'EXPIRED') return 'expired'
  if (card.daysToExpiry != null && card.daysToExpiry <= 30) return 'soon'
  return 'normal'
}

function cardClass(card) {
  return {
    expiring: card.state === 'ACTIVE' && card.expiringSoon,
    expired: card.state === 'EXPIRED',
    inactive: card.state === 'USED_UP' || card.state === 'VOID'
  }
}

function stateTagType(card) {
  if (card.state === 'ACTIVE' && card.expiringSoon) return 'warning'
  if (card.state === 'ACTIVE') return 'success'
  if (card.state === 'EXPIRED') return 'error'
  return 'default'
}

function stateLabel(state) {
  return stateOptions.find(item => item.value === state)?.label || '卡券'
}

function labelOf(options, value) {
  return options.find(item => item.value === value)?.label || value
}

function moneyAndCount(money, count) {
  const parts = []
  if (Number(money) > 0) parts.push(`¥ ${formatNum(money)}`)
  if (Number(count) > 0) parts.push(`${formatCount(count)} 次 / 张`)
  return parts.join(' · ') || '—'
}

const tones = ['tone-0', 'tone-1', 'tone-2', 'tone-3', 'tone-4', 'tone-5']

function issuerTone(issuer) {
  const text = issuer || '卡'
  let hash = 0
  for (let i = 0; i < text.length; i++) {
    hash = (hash * 31 + text.charCodeAt(i)) % 997
  }
  return tones[hash % tones.length]
}

function issuerInitial(issuer) {
  return (issuer || '卡').trim().slice(0, 1)
}

onMounted(loadData)
</script>

<style scoped>
.prepaid-page {
  min-height: 100%;
}
.cards-shell {
  border-radius: 14px;
}
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}
.head-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.01em;
}
.head-sub {
  margin-top: 3px;
  font-size: 12px;
  color: #94a3b8;
}

/* 汇总：轻，不抢戏 */
.summary-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}
.sum-item {
  flex: 1 1 170px;
  max-width: 260px;
  padding: 12px 14px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
}
.sum-item.soon {
  background: #fffbeb;
  border-color: #fde68a;
}
.sum-item.expired {
  background: #fef2f2;
  border-color: #fecaca;
}
.sum-label {
  font-size: 11px;
  color: #94a3b8;
}
.sum-value {
  margin-top: 4px;
  font-size: 19px;
  font-weight: 700;
  color: #0f172a;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.01em;
}
.sum-item.soon .sum-value { color: #b45309; }
.sum-item.expired .sum-value { color: #b91c1c; }
.sum-sub {
  margin-top: 3px;
  font-size: 11px;
  color: #94a3b8;
  line-height: 1.5;
}

/* 筛选 */
.filter-bar {
  margin-bottom: 16px;
  padding: 12px 14px;
  background: #f8fafc;
  border: 1px solid var(--bp-line);
  border-radius: 12px;
}
.chip-line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.search-line {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
}
.search-input {
  max-width: 280px;
}
.filter-switch {
  margin-left: auto;
}
.applied-line {
  margin-top: 10px;
}
.q-chip {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 13px;
  border-radius: 999px;
  border: 1px solid #e2e8f0;
  background: #fff;
  color: #475569;
  font-size: 13px;
  font-weight: 500;
  line-height: 1;
  cursor: pointer;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
}
.q-chip:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
}
.q-chip.active {
  background: var(--bp-primary);
  border-color: var(--bp-primary);
  color: #fff;
}
.q-chip.ghost {
  height: 26px;
  padding: 0 10px;
  border-color: transparent;
  background: #eef2f7;
  color: #475569;
  font-size: 12px;
}
.q-chip.ghost:hover {
  background: #e2e8f0;
}
.chip-x {
  font-size: 14px;
  line-height: 1;
  opacity: 0.55;
  margin-left: 2px;
}
.clear-chip {
  color: #94a3b8;
}
.filter-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: var(--bp-primary);
  color: #fff;
  font-size: 11px;
  line-height: 1;
}
.q-chip.active .filter-count {
  background: rgba(255, 255, 255, 0.24);
}
.more-panel {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #e2e8f0;
}
.filter-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px 20px;
}
.filter-field {
  width: 100%;
}

/* 分组 */
.card-group {
  margin-bottom: 22px;
}
.group-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 10px;
}
.group-title {
  font-size: 14px;
  font-weight: 650;
  color: #334155;
}
.group-title.expiring { color: #b45309; }
.group-title.expired { color: #b91c1c; }
.group-title.usedUp,
.group-title.void { color: #94a3b8; }
.group-note {
  font-size: 11px;
  color: #94a3b8;
}
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(290px, 1fr));
  gap: 14px;
}

/* 票券卡 */
.p-card {
  position: relative;
  padding: 15px 16px 13px;
  border-radius: 14px;
  border: 1px solid #e8edf4;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s, transform 0.15s;
}
.p-card:hover {
  border-color: #cbd5e1;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.06);
  transform: translateY(-1px);
}
.p-card:focus-visible {
  outline: 2px solid #cbd5e1;
  outline-offset: 2px;
}
.p-card.expiring {
  border-color: #fcd34d;
  background: linear-gradient(180deg, #fffdf5, #fff 55%);
}
.p-card.expired {
  border-color: #fecaca;
  background: #fffafa;
}
.p-card.inactive {
  background: #fafafa;
  opacity: 0.82;
}
.p-top {
  display: flex;
  align-items: center;
  gap: 11px;
}
.p-avatar {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  font-weight: 700;
}
.p-avatar.large {
  width: 50px;
  height: 50px;
  border-radius: 15px;
  font-size: 21px;
}
.tone-0 { background: #eff6ff; color: #2563eb; }
.tone-1 { background: #f0fdf4; color: #16a34a; }
.tone-2 { background: #fff7ed; color: #ea580c; }
.tone-3 { background: #fdf2f8; color: #db2777; }
.tone-4 { background: #f5f3ff; color: #7c3aed; }
.tone-5 { background: #f0fdfa; color: #0d9488; }
.p-name-wrap {
  flex: 1;
  min-width: 0;
}
.p-name {
  font-size: 14px;
  font-weight: 650;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.p-issuer {
  margin-top: 2px;
  font-size: 11px;
  color: #94a3b8;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.p-more {
  flex-shrink: 0;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 9px;
  background: transparent;
  color: #94a3b8;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.p-more:hover {
  background: #f1f5f9;
  color: #475569;
}
.p-value-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-top: 14px;
}
.p-value {
  font-size: 22px;
  font-weight: 750;
  color: #0f172a;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
}
.p-card.inactive .p-value {
  color: #94a3b8;
}
.p-face {
  font-size: 11px;
  color: #b0b9c6;
}
.p-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 10px;
  margin-top: 6px;
  font-size: 11px;
  color: #94a3b8;
}
.p-expiry.soon { color: #b45309; font-weight: 600; }
.p-expiry.expired { color: #b91c1c; font-weight: 600; }
.p-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  padding-top: 11px;
  border-top: 1px dashed #eef2f7;
  opacity: 0;
  transition: opacity 0.15s;
}
.p-card:hover .p-actions,
.p-card:focus-within .p-actions {
  opacity: 1;
}

/* 折叠入口 */
.inactive-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  padding: 10px;
  border: 1px dashed #e2e8f0;
  border-radius: 12px;
  background: transparent;
  color: #94a3b8;
  font-size: 12px;
  cursor: pointer;
}
.inactive-toggle:hover {
  background: #f8fafc;
  color: #64748b;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 54px 16px 60px;
  text-align: center;
}
.empty-state.small {
  padding: 32px 16px;
  gap: 8px;
}
.empty-icon {
  width: 62px;
  height: 62px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bp-primary-soft);
  color: var(--bp-primary);
}
.empty-title {
  font-size: 15px;
  font-weight: 600;
  color: #334155;
}
.empty-sub {
  max-width: 360px;
  font-size: 12px;
  line-height: 1.7;
  color: #94a3b8;
}
.loading-block {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 48px 0;
  color: #94a3b8;
  font-size: 12px;
}

/* 详情 */
.detail-hero {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 14px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
}
.detail-hero-main {
  flex: 1;
  min-width: 0;
}
.detail-hero-name {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}
.detail-hero-issuer {
  margin-top: 3px;
  font-size: 12px;
  color: #94a3b8;
}
.detail-balance {
  padding: 18px 4px 8px;
}
.detail-balance-value {
  font-size: 30px;
  font-weight: 750;
  color: #0f172a;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
}
.detail-balance-sub {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 12px;
  margin-top: 4px;
  font-size: 12px;
  color: #94a3b8;
}
.detail-expiry.soon { color: #b45309; font-weight: 600; }
.detail-expiry.expired { color: #b91c1c; font-weight: 600; }
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1px;
  margin: 14px 0 20px;
  background: #eef2f7;
  border: 1px solid #eef2f7;
  border-radius: 12px;
  overflow: hidden;
}
.detail-cell {
  padding: 11px 13px;
  background: #fff;
}
.detail-cell.wide {
  grid-column: span 2;
}
.cell-label {
  font-size: 11px;
  color: #94a3b8;
}
.cell-value {
  margin-top: 3px;
  font-size: 13px;
  color: #334155;
  word-break: break-all;
}
.cell-value.soon { color: #b45309; font-weight: 600; }
.cell-value.expired { color: #b91c1c; font-weight: 600; }
.usage-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding-bottom: 8px;
  border-bottom: 1px solid #f1f5f9;
  font-size: 13px;
  font-weight: 650;
  color: #334155;
}
.usage-count {
  font-size: 11px;
  font-weight: 400;
  color: #94a3b8;
}
.usage-list {
  display: flex;
  flex-direction: column;
}
.usage-row {
  display: flex;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f8fafc;
}
.usage-date {
  flex-shrink: 0;
  width: 76px;
  font-size: 11px;
  color: #94a3b8;
  font-variant-numeric: tabular-nums;
}
.usage-main {
  flex: 1;
  min-width: 0;
}
.usage-line {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px;
  font-size: 12px;
}
.usage-action {
  font-weight: 600;
  color: #334155;
}
.usage-delta {
  color: #dc2626;
  font-variant-numeric: tabular-nums;
}
.usage-delta.plus {
  color: #16a34a;
}
.usage-after {
  color: #94a3b8;
  font-size: 11px;
}
.usage-note {
  margin-top: 3px;
  font-size: 11px;
  color: #94a3b8;
}
.usage-empty,
.usage-loading {
  padding: 20px 0;
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
}
.detail-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 调整弹窗 */
.adjust-hint {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 14px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f8fafc;
  font-size: 12px;
  color: #64748b;
}
.adjust-hint strong {
  font-size: 16px;
  color: #0f172a;
  font-variant-numeric: tabular-nums;
}

/* 弹窗高度：Naive 的 modal 会 teleport 到 body，必须用 global */
:global(.prepaid-form-modal) {
  display: flex;
  flex-direction: column;
  max-height: 90vh;
}
:global(.prepaid-form-modal .n-card-content) {
  overflow-y: auto;
  min-height: 0;
}

@media (max-width: 767px) {
  .cards-shell {
    border-radius: 16px;
  }
  .page-head {
    margin-bottom: 12px;
  }
  .head-title {
    display: none;
  }
  .head-sub {
    margin-top: 0;
  }
  .summary-strip {
    gap: 8px;
  }
  .sum-item {
    flex: 1 1 calc(50% - 4px);
    max-width: none;
    padding: 10px 12px;
  }
  .sum-value {
    font-size: 17px;
  }
  .filter-bar {
    padding: 10px 12px;
  }
  .search-line {
    gap: 8px;
  }
  .search-input {
    flex: 1;
    max-width: none;
  }
  .filter-switch {
    margin-left: 0;
  }
  .filter-grid {
    grid-template-columns: minmax(0, 1fr);
  }
  .card-grid {
    grid-template-columns: minmax(0, 1fr);
    gap: 10px;
  }
  .p-card {
    padding: 14px 14px 12px;
  }
  .p-actions {
    opacity: 1;
  }
  .detail-grid {
    grid-template-columns: 1fr 1fr;
  }
  .detail-actions {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
    width: 100%;
  }
  .detail-actions :deep(.n-button) {
    width: 100%;
  }
}
</style>