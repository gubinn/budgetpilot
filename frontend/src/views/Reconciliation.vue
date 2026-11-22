<template>
  <n-space vertical :size="16">
    <n-card title="导入渠道账单">
      <n-form label-placement="left" label-width="90" :show-feedback="false">
        <n-grid :cols="24" :x-gap="12" :y-gap="12" responsive="screen" item-responsive>
          <n-form-item-gi :span="24" :md="6" label="渠道">
            <n-select v-model:value="form.channel" :options="channelOptions" />
          </n-form-item-gi>
          <n-form-item-gi :span="24" :md="7" label="账户范围">
            <n-select v-model:value="form.accountId" :options="accountOptions" clearable
                      placeholder="全部账户（可限定）" />
          </n-form-item-gi>
          <n-form-item-gi :span="24" :md="7" label="账单文件">
            <n-upload :default-upload="false" :show-file-list="false" accept=".csv"
                      @change="onFileChange">
              <n-button>{{ fileName || '选择支付宝账单 CSV' }}</n-button>
            </n-upload>
          </n-form-item-gi>
          <n-form-item-gi :span="24" :md="4" label=" ">
            <n-button type="primary" :loading="importing" :disabled="!file || !form.channel"
                      @click="submitImport">开始对账</n-button>
          </n-form-item-gi>
        </n-grid>
      </n-form>
      <n-alert v-if="importResult" :type="importResult.duplicate ? 'warning' : 'success'"
               :show-icon="true" closable style="margin-top: 12px" @close="importResult = null">
        {{ importResult.message }}
        <template v-if="importResult.batch">
          ：共 {{ importResult.batch.totalCount }} 条，已匹配 {{ importResult.batch.matchedCount }} 条，
          待处理 {{ importResult.batch.unmatchedCount }} 条，忽略 {{ importResult.batch.ignoredCount }} 条。
        </template>
      </n-alert>
      <n-text depth="3" style="display: block; margin-top: 8px; font-size: 12px">
        当前支持支付宝「交易记录明细查询」CSV。微信、招商银行账单后续开放。
        账单只用于核对，不会修改交易金额、分类或余额。
      </n-text>
    </n-card>

    <n-card title="对账批次">
      <n-data-table :columns="batchColumns" :data="batches" :loading="loadingBatches"
                    :pagination="{ pageSize: 10 }" :bordered="false" />
    </n-card>

    <n-drawer v-model:show="showDetail" :width="detailWidth" placement="right">
      <n-drawer-content :title="`对账明细 · ${detail?.fileName || ''}`" closable>
        <template v-if="detail">
          <n-descriptions :column="3" size="small" bordered>
            <n-descriptions-item label="渠道">{{ detail.channelLabel }}</n-descriptions-item>
            <n-descriptions-item label="账单周期">
              {{ detail.periodStart }} ~ {{ detail.periodEnd }}
            </n-descriptions-item>
            <n-descriptions-item label="账单账号">{{ detail.billAccount || '-' }}</n-descriptions-item>
            <n-descriptions-item label="账单笔数">{{ detail.totalCount }}</n-descriptions-item>
            <n-descriptions-item label="账单支出净额">¥{{ formatNum(detail.totalAmount) }}</n-descriptions-item>
            <n-descriptions-item label="已匹配">{{ detail.matchedCount }}</n-descriptions-item>
            <n-descriptions-item label="待处理">{{ detail.unmatchedCount }}</n-descriptions-item>
            <n-descriptions-item label="其中歧义">{{ detail.ambiguousCount }}</n-descriptions-item>
            <n-descriptions-item label="已忽略">{{ detail.ignoredCount }}</n-descriptions-item>
          </n-descriptions>

          <n-alert v-if="warnings.length" type="warning" style="margin-top: 12px">
            <div v-for="(w, i) in warnings" :key="i">{{ w }}</div>
          </n-alert>

          <n-space justify="space-between" align="center" style="margin-top: 16px">
            <n-tabs v-model:value="tab" type="line" @update:value="loadTab">
              <n-tab name="ALL">全部</n-tab>
              <n-tab name="MATCHED">已匹配</n-tab>
              <n-tab name="PENDING">
                待处理{{ detail.unmatchedCount ? ` (${detail.unmatchedCount})` : '' }}
              </n-tab>
              <n-tab name="IGNORED">已忽略</n-tab>
              <n-tab name="BANK_ONLY">流水有·账单无</n-tab>
            </n-tabs>
            <n-space>
              <n-button size="small" @click="doRematch" :loading="rematching">重新自动匹配</n-button>
              <n-button size="small" :loading="loadingItems" @click="loadTab(tab)">刷新</n-button>
            </n-space>
          </n-space>

          <n-data-table v-if="tab !== 'BANK_ONLY'" :columns="itemColumns" :data="items"
                        :loading="loadingItems" :pagination="itemPagination" remote
                        :scroll-x="1200" size="small"
                        @update:page="onItemPage" style="margin-top: 8px" />
          <n-data-table v-else :columns="bankColumns" :data="bankOnly"
                        :loading="loadingBank" :pagination="{ pageSize: 20 }"
                        :scroll-x="900" size="small" style="margin-top: 8px" />
        </template>
      </n-drawer-content>
    </n-drawer>

    <!-- 手工匹配 -->
    <n-modal v-model:show="showMatch" preset="card" title="手工匹配流水" style="width: 900px">
      <n-descriptions v-if="activeItem" :column="2" size="small" bordered>
        <n-descriptions-item label="账单时间">
          {{ activeItem.paidTime || activeItem.createdTime || activeItem.tradeDate }}
        </n-descriptions-item>
        <n-descriptions-item label="账单金额">¥{{ formatNum(activeItem.amount) }}</n-descriptions-item>
        <n-descriptions-item label="交易对方">{{ activeItem.counterparty || '-' }}</n-descriptions-item>
        <n-descriptions-item label="商品">{{ activeItem.subject || '-' }}</n-descriptions-item>
      </n-descriptions>

      <n-space align="center" style="margin-top: 12px">
        <n-input v-model:value="searchKeyword" placeholder="按备注/商户关键字搜索" clearable
                 style="width: 260px" @keyup.enter="searchTransactions" />
        <n-date-picker v-model:value="searchRange" type="daterange" value-format="yyyy-MM-dd"
                       style="width: 260px" />
        <n-button @click="searchTransactions" :loading="searching">搜索流水</n-button>
        <n-button quaternary @click="loadCandidates">仅看附近候选</n-button>
      </n-space>

      <n-data-table :columns="candidateColumns" :data="candidates" :loading="searching"
                    :pagination="{ pageSize: 8 }" size="small" :scroll-x="800"
                    style="margin-top: 12px" />
    </n-modal>

    <!-- 补录交易 -->
    <n-modal v-model:show="showCreate" preset="card" title="补录为交易" style="width: 560px">
      <n-alert type="warning" :show-icon="true" style="margin-bottom: 12px">
        补录会按所选账户实际增减余额，请确认这笔钱确实从该账户支出/退回。
      </n-alert>
      <n-descriptions v-if="activeItem" :column="1" size="small" bordered>
        <n-descriptions-item label="账单时间">
          {{ activeItem.paidTime || activeItem.createdTime || activeItem.tradeDate }}
        </n-descriptions-item>
        <n-descriptions-item label="交易对方">{{ activeItem.counterparty || '-' }}</n-descriptions-item>
        <n-descriptions-item label="商品">{{ activeItem.subject || '-' }}</n-descriptions-item>
        <n-descriptions-item label="金额">¥{{ formatNum(activeItem.amount) }}</n-descriptions-item>
        <n-descriptions-item label="补录类型">
          <n-tag size="small" :type="createTypeTagType" :bordered="false">{{ createTypeLabel }}</n-tag>
        </n-descriptions-item>
      </n-descriptions>
      <n-text v-if="activeItem?.direction === 'NEUTRAL'" depth="3" style="display: block; margin-top: 8px; font-size: 12px">
        退款账单会按交易类型「退款」补录（冲减支出），分类请选择支出分类。
      </n-text>
      <n-form label-placement="left" label-width="80" style="margin-top: 12px">
        <n-form-item label="账户">
          <n-select v-model:value="createForm.accountId" :options="accountOptions" placeholder="选择入账账户" />
        </n-form-item>
        <n-form-item label="分类">
          <n-tree-select v-model:value="createForm.categoryId" :options="createCategoryOptions"
                         key-field="value" label-field="label" children-field="children"
                         placeholder="选择分类" filterable />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showCreate = false">取消</n-button>
          <n-button type="primary" :loading="creating"
                    :disabled="!createForm.accountId || !createForm.categoryId || !canCreate(activeItem)"
                    @click="submitCreate">确认补录</n-button>
        </n-space>
      </template>
    </n-modal>
  </n-space>
</template>

<script setup>
import { computed, h, onMounted, ref } from 'vue'
import { useDialog, useMessage, NButton, NSpace, NTag } from 'naive-ui'
import { accountApi, categoryApi, reconciliationApi, transactionApi } from '@/api'
import { formatNum } from '@/utils/display'
import dayjs from 'dayjs'

const message = useMessage()
const dialog = useDialog()

const batches = ref([])
const loadingBatches = ref(false)
const accounts = ref([])
const accountOptions = ref([])
const form = ref({ channel: 'ALIPAY', accountId: null })
const file = ref(null)
const fileName = ref('')
const importing = ref(false)
const importResult = ref(null)

const channelOptions = [
  { label: '支付宝', value: 'ALIPAY' },
  { label: '微信支付（后续开放）', value: 'WECHAT', disabled: true },
  { label: '招商银行（后续开放）', value: 'CMB', disabled: true }
]

const showDetail = ref(false)
const detail = ref(null)
const tab = ref('ALL')
const items = ref([])
const bankOnly = ref([])
const loadingItems = ref(false)
const loadingBank = ref(false)
const rematching = ref(false)
const itemPagination = ref({ page: 1, pageSize: 20, itemCount: 0 })
const detailWidth = computed(() => (window.innerWidth < 768 ? '100%' : 1100))

const showMatch = ref(false)
const activeItem = ref(null)
const candidates = ref([])
const searching = ref(false)
const searchKeyword = ref('')
const searchRange = ref(null)

const showCreate = ref(false)
const creating = ref(false)
const createForm = ref({ accountId: null, categoryId: null })
const createCategories = ref([])

const warnings = computed(() => {
  const list = detail.value?.summary?.warnings
  return Array.isArray(list) ? list : []
})

const matchStatusMeta = {
  MATCHED: { label: '已匹配', type: 'success' },
  AMBIGUOUS: { label: '待确认', type: 'warning' },
  UNMATCHED: { label: '未匹配', type: 'default' },
  IGNORED: { label: '已忽略', type: 'info' }
}
const matchTypeLabel = {
  AUTO: '自动',
  AGGREGATE: '合并匹配',
  MANUAL: '手工',
  CREATED: '补录'
}

const batchColumns = [
  { title: '渠道', key: 'channelLabel', width: 90 },
  { title: '文件', key: 'fileName', ellipsis: { tooltip: true } },
  { title: '周期', key: 'period', width: 190, render: row => `${row.periodStart || '-'} ~ ${row.periodEnd || '-'}` },
  { title: '笔数', key: 'totalCount', width: 70 },
  { title: '支出净额', key: 'totalAmount', width: 110, align: 'right', render: row => `¥${formatNum(row.totalAmount)}` },
  {
    title: '对账结果', key: 'result', width: 190,
    render: row => h(NSpace, { size: 4 }, [
      h(NTag, { size: 'small', type: 'success', bordered: false }, { default: () => `已匹配 ${row.matchedCount}` }),
      h(NTag, { size: 'small', type: row.unmatchedCount ? 'warning' : 'default', bordered: false },
        { default: () => `待处理 ${row.unmatchedCount}` }),
      row.ignoredCount ? h(NTag, { size: 'small', type: 'info', bordered: false },
        { default: () => `忽略 ${row.ignoredCount}` }) : null
    ].filter(Boolean))
  },
  {
    title: '状态', key: 'status', width: 80,
    render: row => h(NTag, { size: 'small', type: row.status === 'DONE' ? 'success' : 'warning', bordered: false },
      { default: () => row.status === 'DONE' ? '完成' : '待处理' })
  },
  { title: '导入时间', key: 'createdAt', width: 160 },
  {
    title: '操作', key: 'actions', width: 170, fixed: 'right',
    render: row => h(NSpace, { size: 6 }, [
      h(NButton, { size: 'tiny', type: 'primary', onClick: () => openDetail(row) }, { default: () => '查看' }),
      h(NButton, { size: 'tiny', onClick: () => confirmRematch(row) }, { default: () => '重匹配' }),
      h(NButton, { size: 'tiny', type: 'error', onClick: () => confirmDeleteBatch(row) }, { default: () => '删除' })
    ])
  }
]

const itemColumns = [
  {
    title: '账单时间', key: 'tradeDate', width: 160,
    render: row => h('div', [
      h('div', row.tradeDate),
      h('div', { style: { color: '#888', fontSize: '12px' } },
        row.paidTime ? dayjs(row.paidTime).format('HH:mm:ss') : (row.createdTime ? dayjs(row.createdTime).format('HH:mm:ss') : '-'))
    ])
  },
  {
    title: '对方 / 商品', key: 'counterparty', width: 230,
    render: row => h('div', [
      h('div', row.counterparty || '-'),
      h('div', { style: { color: '#888', fontSize: '12px', overflow: 'hidden', textOverflow: 'ellipsis' } },
        row.subject || '-')
    ])
  },
  {
    title: '金额', key: 'amount', width: 100, align: 'right',
    render: row => h('span', {
      style: { color: row.direction === 'EXPENSE' ? '#e74c3c' : row.direction === 'INCOME' ? '#27ae60' : '#2d98da', fontWeight: 600 }
    }, `¥${formatNum(row.amount)}`)
  },
  { title: '收支', key: 'directionLabel', width: 80 },
  {
    title: '账单状态', key: 'tradeStatus', width: 120,
    render: row => h('div', [
      h('div', row.tradeStatus || '-'),
      row.refunded && Number(row.refunded) > 0
        ? h('div', { style: { color: '#e67e22', fontSize: '12px' } }, `已退 ¥${formatNum(row.refunded)}`)
        : null
    ].filter(Boolean))
  },
  {
    title: '匹配流水', key: 'transaction', width: 250,
    render: row => {
      const tx = row.transaction
      if (!tx) return '-'
      return h('div', [
        h('div', `${tx.transactionDate} ${tx.transactionTime || ''} · ${tx.accountName || '-'}`),
        h('div', { style: { color: '#888', fontSize: '12px' } },
          `${tx.merchantName || '-'} · ¥${formatNum(tx.amountBase ?? tx.amount)}`)
      ])
    }
  },
  {
    title: '对账状态', key: 'matchStatus', width: 120,
    render: row => {
      const meta = matchStatusMeta[row.matchStatus] || { label: row.matchStatus, type: 'default' }
      return h(NSpace, { size: 4 }, [
        h(NTag, { size: 'small', type: meta.type, bordered: false }, { default: () => meta.label }),
        row.matchType ? h(NTag, { size: 'tiny', bordered: false }, { default: () => matchTypeLabel[row.matchType] || row.matchType }) : null
      ].filter(Boolean))
    }
  },
  {
    title: '操作', key: 'actions', width: 210, fixed: 'right',
    render: row => {
      const buttons = []
      if (row.matchStatus === 'MATCHED') {
        buttons.push(h(NButton, { size: 'tiny', onClick: () => doUnmatch(row) }, { default: () => '取消匹配' }))
      } else {
        buttons.push(h(NButton, {
          size: 'tiny', type: 'primary',
          onClick: () => openMatch(row)
        }, { default: () => row.matchStatus === 'AMBIGUOUS' ? '选择候选' : '手工匹配' }))
        if (row.matchStatus !== 'IGNORED') {
          if (canCreate(row)) {
            buttons.push(h(NButton, { size: 'tiny', onClick: () => openCreate(row) }, { default: () => '补录' }))
          }
          buttons.push(h(NButton, { size: 'tiny', onClick: () => doIgnore(row) }, { default: () => '忽略' }))
        }
      }
      if (row.matchStatus === 'IGNORED') {
        buttons.push(h(NButton, { size: 'tiny', onClick: () => doUnmatch(row) }, { default: () => '恢复' }))
      }
      return h(NSpace, { size: 4 }, buttons)
    }
  }
]

const bankColumns = [
  { title: '日期', key: 'transactionDate', width: 110 },
  { title: '时间', key: 'transactionTime', width: 90 },
  { title: '账户', key: 'accountName', width: 160 },
  { title: '商户', key: 'merchantName', width: 140 },
  {
    title: '金额', key: 'amountBase', width: 110, align: 'right',
    render: row => `¥${formatNum(row.amountBase ?? row.amount)}`
  },
  { title: '备注', key: 'note', ellipsis: { tooltip: true } }
]

const candidateColumns = [
  { title: '日期', key: 'transactionDate', width: 100 },
  { title: '时间', key: 'transactionTime', width: 90 },
  { title: '账户', key: 'accountName', width: 150 },
  { title: '商户', key: 'merchantName', width: 130 },
  { title: '金额', key: 'amountBase', width: 100, align: 'right', render: row => `¥${formatNum(row.amountBase ?? row.amount)}` },
  { title: '备注', key: 'note', ellipsis: { tooltip: true } },
  {
    title: '操作', key: 'actions', width: 90,
    render: row => h(NButton, {
      size: 'tiny', type: 'primary', disabled: row.reconciled,
      onClick: () => doMatch(row.id)
    }, { default: () => row.reconciled ? '已对账' : '选用' })
  }
]

const createTypeLabel = computed(() => {
  const row = activeItem.value
  if (!row) return '-'
  if (row.direction === 'INCOME') return '收入'
  if (row.direction === 'EXPENSE') return '支出'
  return (row.tradeStatus || '').includes('退款') ? '退款' : '不支持补录'
})
const createTypeTagType = computed(() => {
  if (activeItem.value?.direction === 'INCOME') return 'success'
  if (activeItem.value?.direction === 'EXPENSE') return 'error'
  return 'info'
})
const canCreate = (row) => row?.direction !== 'NEUTRAL'
  || (row?.tradeStatus || '').includes('退款')

const createCategoryOptions = computed(() => {
  function build(nodes) {
    return (nodes || []).map(c => ({
      label: c.name,
      value: c.id,
      children: build(c.children)
    }))
  }
  return build(createCategories.value)
})

async function loadAccounts() {
  try {
    const res = await accountApi.list(true)
    accounts.value = res.data || []
    accountOptions.value = accounts.value.map(a => ({ label: a.name, value: a.id }))
  } catch (e) {
    // 账号加载失败不阻塞对账页
  }
}

async function loadBatches() {
  loadingBatches.value = true
  try {
    const res = await reconciliationApi.batches()
    batches.value = res.data || []
  } catch (e) {
    message.error('加载对账批次失败: ' + e.message)
  } finally {
    loadingBatches.value = false
  }
}

function onFileChange({ file: uploadFile }) {
  file.value = uploadFile.file
  fileName.value = uploadFile.name
}

async function submitImport() {
  if (!file.value) {
    message.warning('请选择账单文件')
    return
  }
  importing.value = true
  try {
    const formData = new FormData()
    formData.append('file', file.value)
    formData.append('channel', form.value.channel)
    if (form.value.accountId) {
      formData.append('accountId', form.value.accountId)
    }
    const res = await reconciliationApi.import(formData)
    importResult.value = res.data
    file.value = null
    fileName.value = ''
    await loadBatches()
    if (res.data?.batch) {
      openDetail(res.data.batch)
    }
  } catch (e) {
    message.error('导入失败: ' + e.message)
  } finally {
    importing.value = false
  }
}

function openDetail(row) {
  detail.value = row
  tab.value = 'ALL'
  showDetail.value = true
  itemPagination.value.page = 1
  loadTab('ALL')
}

async function loadTab(name) {
  if (!detail.value) return
  if (name === 'BANK_ONLY') {
    loadBankOnly()
    return
  }
  loadingItems.value = true
  try {
    const res = await reconciliationApi.items(detail.value.id, {
      status: name === 'ALL' ? undefined : name,
      page: itemPagination.value.page,
      size: itemPagination.value.pageSize
    })
    items.value = res.data.items
    itemPagination.value.itemCount = res.data.total
  } catch (e) {
    message.error('加载账单明细失败: ' + e.message)
  } finally {
    loadingItems.value = false
  }
}

function onItemPage(page) {
  itemPagination.value.page = page
  loadTab(tab.value)
}

async function loadBankOnly() {
  loadingBank.value = true
  try {
    const res = await reconciliationApi.unmatchedTransactions(detail.value.id)
    bankOnly.value = res.data || []
  } catch (e) {
    message.error('加载未匹配流水失败: ' + e.message)
  } finally {
    loadingBank.value = false
  }
}

async function refreshDetail() {
  if (!detail.value) return
  try {
    const res = await reconciliationApi.batch(detail.value.id)
    detail.value = res.data
    await loadBatches()
    await loadTab(tab.value)
  } catch (e) {
    message.error('刷新失败: ' + e.message)
  }
}

function openMatch(row) {
  activeItem.value = row
  showMatch.value = true
  searchKeyword.value = ''
  searchRange.value = row.tradeDate
    ? [dayjs(row.tradeDate).subtract(3, 'day').format('YYYY-MM-DD'), dayjs(row.tradeDate).add(3, 'day').format('YYYY-MM-DD')]
    : null
  loadCandidates()
}

async function loadCandidates() {
  searching.value = true
  try {
    const res = await reconciliationApi.candidates(activeItem.value.id)
    candidates.value = res.data || []
  } catch (e) {
    message.error('加载候选流水失败: ' + e.message)
  } finally {
    searching.value = false
  }
}

async function searchTransactions() {
  searching.value = true
  try {
    const params = { page: 1, size: 50 }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (searchRange.value?.length === 2) {
      params.startDate = searchRange.value[0]
      params.endDate = searchRange.value[1]
    }
    const res = await transactionApi.list(params)
    candidates.value = res.data.items
  } catch (e) {
    message.error('搜索流水失败: ' + e.message)
  } finally {
    searching.value = false
  }
}

async function doMatch(transactionId) {
  try {
    await reconciliationApi.match(activeItem.value.id, transactionId)
    message.success('已匹配')
    showMatch.value = false
    await refreshDetail()
  } catch (e) {
    message.error('匹配失败: ' + e.message)
  }
}

async function doUnmatch(row) {
  try {
    await reconciliationApi.unmatch(row.id)
    message.success(row.matchStatus === 'IGNORED' ? '已恢复为待处理' : '已取消匹配')
    await refreshDetail()
  } catch (e) {
    message.error('操作失败: ' + e.message)
  }
}

function doIgnore(row) {
  dialog.warning({
    title: '忽略账单行',
    content: '忽略后该行不再计入待处理，可在「已忽略」里恢复。',
    positiveText: '忽略',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await reconciliationApi.ignore(row.id, '人工忽略')
        message.success('已忽略')
        await refreshDetail()
      } catch (e) {
        message.error('操作失败: ' + e.message)
      }
    }
  })
}

async function openCreate(row) {
  activeItem.value = row
  createForm.value = { accountId: null, categoryId: null }
  const type = row.direction === 'INCOME' ? 2 : 1
  try {
    const res = await categoryApi.tree(type)
    createCategories.value = res.data || []
  } catch (e) {
    createCategories.value = []
  }
  showCreate.value = true
}

async function submitCreate() {
  creating.value = true
  try {
    await reconciliationApi.createTransaction(activeItem.value.id, createForm.value)
    message.success('已补录并完成对账')
    showCreate.value = false
    await refreshDetail()
  } catch (e) {
    message.error('补录失败: ' + e.message)
  } finally {
    creating.value = false
  }
}

async function doRematch() {
  rematching.value = true
  try {
    await reconciliationApi.rematch(detail.value.id)
    message.success('已重新匹配')
    await refreshDetail()
  } catch (e) {
    message.error('重新匹配失败: ' + e.message)
  } finally {
    rematching.value = false
  }
}

function confirmRematch(row) {
  dialog.warning({
    title: '重新自动匹配',
    content: `将重新匹配批次「${row.fileName}」中自动匹配的账单行，手工匹配和补录结果保留。`,
    positiveText: '开始',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await reconciliationApi.rematch(row.id)
        message.success('已重新匹配')
        await loadBatches()
        if (detail.value?.id === row.id) {
          await refreshDetail()
        }
      } catch (e) {
        message.error('重新匹配失败: ' + e.message)
      }
    }
  })
}

function confirmDeleteBatch(row) {
  dialog.error({
    title: '删除对账批次',
    content: '删除后该批次的账单明细会移除，对应交易的「已对账」标记会取消，交易本身不会删除。',
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await reconciliationApi.removeBatch(row.id)
        message.success('已删除')
        if (detail.value?.id === row.id) {
          showDetail.value = false
        }
        await loadBatches()
      } catch (e) {
        message.error('删除失败: ' + e.message)
      }
    }
  })
}

onMounted(() => {
  loadAccounts()
  loadBatches()
})
</script>

<style scoped>
:deep(.n-drawer-body-content-wrapper) {
  padding-top: 8px;
}
</style>