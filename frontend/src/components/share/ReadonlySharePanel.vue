<template>
  <div class="share-panel">
    <div class="share-panel-head">
      <div class="share-panel-hint">
        生成只读链接给家人看账本，不共享密码；可随时撤销。对方只能看，不能改。
      </div>
      <n-button type="primary" size="small" @click="openCreate">生成只读链接</n-button>
    </div>

    <div v-if="loading" class="share-loading"><n-spin size="small" /></div>
    <div v-else-if="!shares.length" class="share-empty">还没有分享链接</div>
    <div v-else class="share-list">
      <div v-for="share in shares" :key="share.id" class="share-row">
        <div class="share-row-main">
          <div class="share-row-title">
            <span class="share-row-label">{{ share.label }}</span>
            <n-tag :type="statusType(share)" size="tiny" :bordered="false">{{ share.statusName }}</n-tag>
          </div>
          <div class="share-row-meta">
            链接尾号 {{ share.codeHint }}
            · {{ share.pinRequired ? '需要 PIN' : '免 PIN' }}
            · {{ share.expiresAt }} 到期
            <span v-if="share.lastUsedAt"> · 最近 {{ share.lastUsedAt }}</span>
            <span v-if="share.viewCount"> · 已进入 {{ share.viewCount }} 次</span>
          </div>
        </div>
        <div class="share-row-actions">
          <n-button
            v-if="share.status === 'ACTIVE' && cachedCode(share.id)"
            size="tiny"
            quaternary
            @click="showCached(share)"
          >查看链接</n-button>
          <n-button
            v-if="share.status === 'ACTIVE'"
            size="tiny"
            quaternary
            @click="openRotate(share)"
          >重新生成</n-button>
          <n-button
            v-if="share.status === 'ACTIVE'"
            size="tiny"
            quaternary
            type="error"
            @click="confirmRevoke(share)"
          >撤销</n-button>
        </div>
      </div>
    </div>

    <!-- 创建 / 重新生成 -->
    <n-modal
      v-model:show="showCreate"
      preset="card"
      :title="formMode === 'create' ? '生成只读链接' : '重新生成链接'"
      :style="{ width: isMobile ? '92vw' : '460px' }"
      :mask-closable="false"
    >
      <n-alert v-if="formMode === 'rotate'" type="warning" :show-icon="true" class="share-tip">
        重新生成后，旧链接会立即失效，已经发出去的旧链接不能再打开。
      </n-alert>
      <n-form label-placement="top" :show-feedback="false">
        <n-form-item label="备注">
          <n-input v-model:value="form.label" maxlength="60" placeholder="如：给媳妇看" />
        </n-form-item>
        <n-form-item label="有效期">
          <n-select v-model:value="form.expiresInDays" :options="expiryOptions" />
        </n-form-item>
        <n-form-item label="访问 PIN">
          <n-input
            v-model:value="form.pin"
            maxlength="6"
            inputmode="numeric"
            placeholder="4-6 位数字，留空则免 PIN"
            show-password-on="click"
            type="password"
          />
        </n-form-item>
      </n-form>
      <n-alert v-if="!form.pin" type="warning" :show-icon="true" class="share-tip">
        {{ formMode === 'rotate'
          ? '重新生成不会保留原 PIN；留空会创建一条免 PIN 的新链接。'
          : '不设 PIN 时，拿到链接的人可以直接查看。建议设置 PIN，并把链接和 PIN 分开发送。' }}
      </n-alert>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showCreate = false">取消</n-button>
          <n-button type="primary" :loading="creating" @click="handleCreate">
            {{ formMode === 'create' ? '生成链接' : '重新生成' }}
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 生成结果 -->
    <n-modal
      v-model:show="showResult"
      preset="card"
      title="链接已生成"
      :style="{ width: isMobile ? '92vw' : '480px' }"
      :mask-closable="false"
    >
      <n-alert :type="resultIsReopen ? 'info' : 'success'" :show-icon="true" class="share-tip">
        {{ resultNotice }}
      </n-alert>
      <div class="result-row">
        <span class="result-label">链接</span>
        <input
          class="result-link-input"
          :value="resultUrl"
          readonly
          @focus="$event.target.select()"
          @click="$event.target.select()"
        />
        <n-button size="small" @click="copy(resultUrl)">复制</n-button>
      </div>
      <div v-if="resultPin" class="result-row">
        <span class="result-label">PIN</span>
        <input
          class="result-link-input"
          :value="resultPin"
          readonly
          @focus="$event.target.select()"
          @click="$event.target.select()"
        />
        <n-button size="small" @click="copy(resultPin)">复制</n-button>
      </div>
      <div v-else class="result-nopin">{{ resultNoPinText }}</div>
      <div class="result-hint">复制失败？点击链接框全选后手动复制即可。</div>
      <template #footer>
        <n-space justify="end">
          <n-button type="primary" @click="showResult = false">知道了</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useDialog, useMessage } from 'naive-ui'
import { shareApi } from '@/api'
import { useCompactViewport } from '@/composables/useCompactViewport'

const message = useMessage()
const dialog = useDialog()
const isMobile = useCompactViewport(767)

const loading = ref(false)
const shares = ref([])
const showCreate = ref(false)
const creating = ref(false)
const formMode = ref('create')
const rotateSource = ref(null)
const showResult = ref(false)
const resultUrl = ref('')
const resultPin = ref('')
const resultNotice = ref('')
const resultNoPinText = ref('')
const resultIsReopen = ref(false)

const CODE_CACHE_PREFIX = 'bp_share_code_'

const form = ref({ label: '给家人看', expiresInDays: 7, pin: '' })
const expiryOptions = [
  { label: '1 天', value: 1 },
  { label: '7 天', value: 7 },
  { label: '30 天', value: 30 }
]

async function loadShares() {
  loading.value = true
  try {
    const res = await shareApi.list()
    shares.value = res.data || []
  } catch {
    message.error('加载分享链接失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  formMode.value = 'create'
  rotateSource.value = null
  form.value = { label: '给家人看', expiresInDays: 7, pin: '' }
  showCreate.value = true
}

function openRotate(share) {
  formMode.value = 'rotate'
  rotateSource.value = share
  const daysLeft = Math.max(1,
    Math.ceil((new Date(String(share.expiresAt || '').replace(' ', 'T')).getTime() - Date.now()) / 86400000))
  const days = [1, 7, 30].find(item => item >= daysLeft) || 30
  form.value = { label: share.label, expiresInDays: days, pin: '' }
  showCreate.value = true
}

async function handleCreate() {
  const pin = form.value.pin.trim()
  if (pin && !/^\d{4,6}$/.test(pin)) {
    message.warning('PIN 需为 4-6 位数字')
    return
  }
  const isRotate = formMode.value === 'rotate'
  creating.value = true
  try {
    const res = await shareApi.create({
      label: form.value.label?.trim() || '只读分享',
      expiresInDays: form.value.expiresInDays,
      pin
    })
    cacheCode(res.data.id, res.data.code)

    if (isRotate && rotateSource.value) {
      try {
        await shareApi.revoke(rotateSource.value.id)
        removeCachedCode(rotateSource.value.id)
      } catch {
        message.warning('新链接已生成，但旧链接撤销失败，请手动撤销旧链接')
      }
    }

    resultUrl.value = window.location.origin + res.data.path
    resultPin.value = res.data.pin || ''
    resultNotice.value = isRotate
      ? '旧链接已失效。新的链接和 PIN 请分开发送，别放在同一条消息里。'
      : '把链接和 PIN 分开发送，别放在同一条消息里。链接已保存在本机，之后还能回来查看。'
    resultNoPinText.value = '未设置 PIN：拿到链接即可查看。'
    resultIsReopen.value = false
    showCreate.value = false
    showResult.value = true
    await loadShares()
  } catch (e) {
    message.error(e?.message || '生成失败')
  } finally {
    creating.value = false
  }
}

function showCached(share) {
  const code = cachedCode(share.id)
  if (!code) {
    message.warning('本机没有保存这条链接，请点「重新生成」')
    return
  }
  resultUrl.value = window.location.origin + '/share/' + code
  resultPin.value = ''
  resultNotice.value = '这是保存在本机浏览器里的链接。'
  resultNoPinText.value = share.pinRequired
    ? 'PIN 不会再次显示；忘记 PIN 请点「重新生成」设置新 PIN。'
    : '这条链接没有设置 PIN，拿到链接即可查看。'
  resultIsReopen.value = true
  showResult.value = true
}

function cacheCode(id, code) {
  try {
    localStorage.setItem(CODE_CACHE_PREFIX + id, code)
  } catch {
    // 浏览器禁用存储时忽略
  }
}

function cachedCode(id) {
  try {
    return localStorage.getItem(CODE_CACHE_PREFIX + id) || ''
  } catch {
    return ''
  }
}

function removeCachedCode(id) {
  try {
    localStorage.removeItem(CODE_CACHE_PREFIX + id)
  } catch {
    // 忽略
  }
}

function confirmRevoke(share) {
  dialog.warning({
    title: '撤销这条只读链接？',
    content: '撤销后，正在查看的只读会话也会立即失效。',
    positiveText: '撤销',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await shareApi.revoke(share.id)
        removeCachedCode(share.id)
        message.success('已撤销')
        await loadShares()
      } catch (e) {
        message.error(e?.message || '撤销失败')
      }
    }
  })
}

function statusType(share) {
  if (share.status === 'ACTIVE') return 'success'
  if (share.status === 'REVOKED') return 'error'
  return 'default'
}

async function copy(text) {
  if (!text) return
  // HTTPS/localhost 优先用 Clipboard API；HTTP 局域网等非安全上下文会不可用
  if (navigator.clipboard?.writeText && window.isSecureContext) {
    try {
      await navigator.clipboard.writeText(text)
      message.success('已复制')
      return
    } catch {
      // 继续走降级方案
    }
  }
  try {
    const input = document.createElement('textarea')
    input.value = text
    input.setAttribute('readonly', '')
    input.style.position = 'fixed'
    input.style.left = '-9999px'
    document.body.appendChild(input)
    input.select()
    input.setSelectionRange(0, text.length)
    const ok = document.execCommand('copy')
    document.body.removeChild(input)
    if (ok) {
      message.success('已复制')
    } else {
      message.warning('复制失败，请点击链接框全选后手动复制')
    }
  } catch {
    message.warning('复制失败，请点击链接框全选后手动复制')
  }
}

onMounted(loadShares)
</script>

<style scoped>
.share-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.share-panel-head {
  display: flex;
  align-items: center;
  gap: 12px;
}
.share-panel-hint {
  flex: 1;
  min-width: 0;
  font-size: 12px;
  line-height: 1.6;
  color: #94a3b8;
}
.share-loading,
.share-empty {
  padding: 14px 0;
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
}
.share-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.share-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
}
.share-row-main {
  flex: 1;
  min-width: 0;
}
.share-row-title {
  display: flex;
  align-items: center;
  gap: 8px;
}
.share-row-label {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}
.share-row-meta {
  margin-top: 3px;
  font-size: 11px;
  color: #94a3b8;
  line-height: 1.5;
}
.share-row-actions {
  flex-shrink: 0;
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 2px 4px;
}
.share-tip {
  margin-top: 4px;
}
.result-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}
.result-label {
  flex-shrink: 0;
  width: 34px;
  font-size: 12px;
  color: #64748b;
}
.result-link-input {
  flex: 1;
  min-width: 0;
  height: 30px;
  padding: 0 10px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #f8fafc;
  color: #334155;
  font-size: 12px;
  font-family: inherit;
  outline: none;
}
.result-link-input:focus {
  border-color: #93c5fd;
  background: #fff;
}
.result-nopin {
  margin-top: 10px;
  font-size: 12px;
  color: #b45309;
}
.result-hint {
  margin-top: 10px;
  font-size: 11px;
  color: #94a3b8;
}

@media (max-width: 640px) {
  .share-panel-head {
    align-items: flex-start;
    flex-direction: column;
  }
  .share-row {
    flex-direction: column;
    align-items: stretch;
  }
  .share-row-actions {
    justify-content: flex-start;
  }
}
</style>