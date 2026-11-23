<template>
  <div class="share-page">
    <div class="share-card">
      <div class="share-brand">
        <img src="/logo-mark.svg" alt="BudgetPilot" class="share-logo" />
        <span>BudgetPilot</span>
      </div>

      <h1>只读共享账本</h1>
      <p v-if="label" class="share-label">{{ label }}</p>

      <n-alert v-if="expired" type="warning" :show-icon="true" class="share-alert">
        只读会话已过期，请重新验证。
      </n-alert>

      <div v-if="checking" class="share-loading">
        <n-spin size="small" /> 正在检查链接…
      </div>

      <n-result
        v-else-if="!valid"
        status="error"
        title="链接无效或已过期"
        description="请让账本主人重新生成一条只读分享链接。"
      />

      <template v-else>
        <n-form v-if="needsPin" label-placement="top" :show-feedback="false" class="share-form">
          <n-form-item label="访问 PIN">
            <n-input
              v-model:value="pin"
              size="large"
              maxlength="6"
              inputmode="numeric"
              placeholder="请输入 4-6 位 PIN"
              show-password-on="click"
              type="password"
              @keyup.enter="doVerify"
            />
          </n-form-item>
        </n-form>

        <n-button type="primary" size="large" block :loading="verifying" @click="doVerify">
          查看账本
        </n-button>
        <div class="share-hint">只读访问，不能修改任何数据。</div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { shareApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const auth = useAuthStore()

const code = String(route.params.code || '')
const expired = route.query.expired === '1'
const checking = ref(true)
const valid = ref(false)
const needsPin = ref(false)
const label = ref('')
const pin = ref('')
const verifying = ref(false)

onMounted(async () => {
  // 已经验证过同一个分享，直接回到账本
  if (auth.token && auth.readonlyShareCode === code) {
    router.replace('/dashboard')
    return
  }
  try {
    const res = await shareApi.check(code)
    valid.value = !!res.data?.valid
    needsPin.value = !!res.data?.needsPin
    label.value = res.data?.label || ''
    if (!valid.value) {
      auth.clearReadonly()
    }
  } catch {
    valid.value = false
    auth.clearReadonly()
  } finally {
    checking.value = false
  }
})

async function doVerify() {
  if (needsPin.value && pin.value.trim().length < 4) {
    message.warning('请输入 4-6 位 PIN')
    return
  }
  verifying.value = true
  try {
    const res = await shareApi.verify(code, { pin: pin.value.trim() })
    auth.setReadonly(res.data, code)
    router.push('/dashboard')
  } catch (e) {
    message.error(e?.message || '验证失败')
  } finally {
    verifying.value = false
  }
}
</script>

<style scoped>
.share-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: linear-gradient(160deg, #f8fafc 0%, #eef2f7 100%);
}
.share-card {
  width: 100%;
  max-width: 400px;
  padding: 30px 26px 26px;
  border-radius: 20px;
  background: #fff;
  border: 1px solid #e8edf4;
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.08);
}
.share-brand {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: -0.01em;
}
.share-logo {
  width: 28px;
  height: 28px;
  border-radius: 8px;
}
h1 {
  margin: 22px 0 6px;
  font-size: 22px;
  color: #0f172a;
  letter-spacing: -0.02em;
}
.share-label {
  margin: 0 0 18px;
  font-size: 13px;
  color: #64748b;
}
.share-alert {
  margin-bottom: 16px;
}
.share-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 34px 0;
  color: #94a3b8;
  font-size: 13px;
}
.share-form {
  margin-bottom: 6px;
}
.share-hint {
  margin-top: 14px;
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
}
:deep(.n-result) {
  padding: 16px 0 8px;
}
:deep(.n-result .n-result-header__title) {
  font-size: 16px;
}
</style>