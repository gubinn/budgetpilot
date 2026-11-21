<template>
  <div class="login-page">
    <div class="login-shell">
      <aside class="brand-panel">
        <div class="brand-head">
          <img src="/logo-mark.svg" class="brand-mark" alt="BudgetPilot" />
          <span class="brand-name">BudgetPilot</span>
        </div>

        <div class="brand-copy">
          <span class="eyebrow">PERSONAL FINANCE</span>
          <h1>让每一笔钱，<br />都有方向。</h1>
          <p>自动记账、预算预警、多账户统计，一处掌握你的财务节奏。</p>
        </div>

        <ul class="brand-points">
          <li><span></span>招行邮件自动入账</li>
          <li><span></span>多账户与信用卡管理</li>
          <li><span></span>分类预算与可视化报表</li>
        </ul>

        <div class="brand-foot">BudgetPilot · 个人预算管理</div>
      </aside>

      <main class="form-panel">
        <div class="form-inner">
          <div class="mobile-brand">
            <img src="/logo-mark.svg" alt="BudgetPilot" />
            <span>BudgetPilot</span>
          </div>

          <template v-if="step === 'credentials'">
            <h2>欢迎回来</h2>
            <p class="form-sub">登录你的账本，继续掌握每一笔收支。</p>

            <n-form ref="formRef" :model="form" :rules="rules" label-placement="top" size="large">
              <n-form-item label="用户名" path="username">
                <n-input v-model:value="form.username" placeholder="请输入用户名" @keyup.enter="handleLogin">
                  <template #prefix><n-icon><person-outline /></n-icon></template>
                </n-input>
              </n-form-item>
              <n-form-item label="密码" path="password">
                <n-input v-model:value="form.password" type="password" placeholder="请输入密码" @keyup.enter="handleLogin" show-password-on="click">
                  <template #prefix><n-icon><lock-closed-outline /></n-icon></template>
                </n-input>
              </n-form-item>
              <n-button type="primary" size="large" block :loading="loading" @click="handleLogin" class="login-button">
                登 录
              </n-button>
            </n-form>

            <p class="login-hint">数据存储在你自己的服务器 · 安全登录</p>
          </template>

          <template v-else>
            <h2>验证身份</h2>
            <p class="form-sub">
              验证码已发送到 {{ otpChannel }}<span v-if="otpRecipient">（{{ otpRecipient }}）</span>
            </p>

            <n-form label-placement="top" size="large">
              <n-form-item label="验证码">
                <n-input
                  v-model:value="code"
                  maxlength="6"
                  placeholder="请输入 6 位验证码"
                  :input-props="{ inputmode: 'numeric', autocomplete: 'one-time-code' }"
                  @keyup.enter="handleVerify"
                />
              </n-form-item>
              <n-button type="primary" size="large" block :loading="verifying" @click="handleVerify" class="login-button">
                验证并登录
              </n-button>
            </n-form>

            <div class="otp-actions">
              <n-button text type="primary" :disabled="resendIn > 0 || resending" @click="handleResend">
                {{ resendIn > 0 ? `重新发送（${resendIn}s）` : '重新发送验证码' }}
              </n-button>
              <n-button text @click="backToCredentials">返回重新登录</n-button>
            </div>

            <p class="login-hint">公网登录需要验证码确认 · 有效期 {{ Math.round(otpExpireIn / 60) }} 分钟</p>
          </template>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { PersonOutline, LockClosedOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const message = useMessage()
const formRef = ref(null)
const loading = ref(false)

const step = ref('credentials')
const challengeId = ref('')
const code = ref('')
const otpChannel = ref('')
const otpRecipient = ref('')
const otpExpireIn = ref(300)
const verifying = ref(false)
const resending = ref(false)
const resendIn = ref(0)
let countdownTimer = null

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: { required: true, message: '请输入用户名', trigger: 'blur' },
  password: { required: true, message: '请输入密码', trigger: 'blur' }
}

function startCountdown(seconds) {
  clearInterval(countdownTimer)
  resendIn.value = seconds
  countdownTimer = setInterval(() => {
    if (resendIn.value > 0) {
      resendIn.value -= 1
    } else {
      clearInterval(countdownTimer)
    }
  }, 1000)
}

function applyChallenge(data) {
  challengeId.value = data.challengeId
  otpChannel.value = data.channel || '通知渠道'
  otpRecipient.value = data.recipient || ''
  otpExpireIn.value = data.expireIn || 300
  code.value = ''
  step.value = 'otp'
  startCountdown(60)
}

async function handleLogin() {
  loading.value = true
  try {
    await formRef.value?.validate()
    const res = await auth.login(form.username, form.password)
    if (res.code === 0) {
      if (res.data?.needVerify) {
        applyChallenge(res.data)
        message.success('验证码已发送')
      } else {
        message.success('登录成功')
        const redirect = route.query.redirect || '/'
        router.push(redirect)
      }
    } else {
      message.error(res.message || '登录失败')
    }
  } catch (e) {
    // validate() 失败会 reject (array of errors)，不需要额外提示
    if (Array.isArray(e) && e.length > 0) return
    // API 错误（如密码错误）展示后端返回的 message
    if (e?.message) {
      message.error(e.message)
    } else {
      message.error('登录失败，请检查网络连接')
    }
  } finally {
    loading.value = false
  }
}

async function handleVerify() {
  if (!/^\d{6}$/.test(code.value)) {
    message.warning('请输入 6 位数字验证码')
    return
  }
  verifying.value = true
  try {
    await auth.verifyOtp(challengeId.value, code.value)
    message.success('登录成功')
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch (e) {
    message.error(e?.message || '验证码错误或已过期')
  } finally {
    verifying.value = false
  }
}

async function handleResend() {
  if (resendIn.value > 0 || resending.value) return
  resending.value = true
  try {
    const res = await auth.resendOtp(challengeId.value)
    applyChallenge(res.data)
    message.success('验证码已重新发送')
  } catch (e) {
    message.error(e?.message || '发送失败，请稍后再试')
  } finally {
    resending.value = false
  }
}

function backToCredentials() {
  step.value = 'credentials'
  code.value = ''
  clearInterval(countdownTimer)
  resendIn.value = 0
}

onUnmounted(() => clearInterval(countdownTimer))
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  background:
    radial-gradient(900px 520px at 12% 8%, rgba(37, 99, 235, 0.1), transparent 55%),
    radial-gradient(800px 480px at 92% 92%, rgba(14, 165, 164, 0.09), transparent 55%),
    #f5f7fb;
}

.login-shell {
  width: min(1040px, 100%);
  min-height: 620px;
  display: grid;
  grid-template-columns: 1.05fr 1fr;
  overflow: hidden;
  border-radius: 28px;
  background: #fff;
  box-shadow: 0 30px 80px -30px rgba(15, 23, 42, 0.35);
}

.brand-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 52px 48px;
  color: #fff;
  background: linear-gradient(145deg, #0b1220 0%, #1d4ed8 55%, #0ea5a4 100%);
}

.brand-panel::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 80% 18%, rgba(255, 255, 255, 0.18), transparent 42%);
  pointer-events: none;
}

.brand-head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  box-shadow: 0 12px 28px -10px rgba(0, 0, 0, 0.5);
}

.brand-name {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.brand-copy {
  position: relative;
  z-index: 1;
  margin-top: auto;
}

.eyebrow {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.22em;
  color: rgba(255, 255, 255, 0.58);
}

.brand-copy h1 {
  margin: 14px 0 16px;
  font-size: 38px;
  line-height: 1.22;
  letter-spacing: -0.02em;
}

.brand-copy p {
  max-width: 320px;
  color: rgba(255, 255, 255, 0.74);
  font-size: 14px;
  line-height: 1.7;
}

.brand-points {
  position: relative;
  z-index: 1;
  list-style: none;
  margin: 34px 0 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.brand-points li {
  display: flex;
  align-items: center;
  gap: 10px;
  color: rgba(255, 255, 255, 0.82);
  font-size: 13px;
}

.brand-points li span {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.16);
}

.brand-points li span::after {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #a7f3d0;
}

.brand-foot {
  position: relative;
  z-index: 1;
  margin-top: 36px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
}

.form-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 52px 48px;
  background: #fff;
}

.form-inner {
  width: 100%;
  max-width: 360px;
}

.mobile-brand {
  display: none;
}

.form-inner h2 {
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: #0f172a;
}

.form-sub {
  margin: 8px 0 28px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.login-button {
  height: 46px;
  margin-top: 4px;
  font-size: 15px;
  border: none !important;
  background: linear-gradient(135deg, #2563eb, #0ea5a4) !important;
  box-shadow: 0 12px 24px -12px rgba(37, 99, 235, 0.8);
}

.login-hint {
  margin-top: 28px;
  text-align: center;
  font-size: 11px;
  color: #94a3b8;
}

.otp-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 14px;
}

@media (max-width: 860px) {
  .login-page {
    padding: 20px;
    align-items: center;
  }

  .login-shell {
    min-height: 0;
    grid-template-columns: 1fr;
    border-radius: 24px;
  }

  .brand-panel {
    display: none;
  }

  .form-panel {
    padding: 36px 24px 32px;
  }

  .mobile-brand {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 28px;
  }

  .mobile-brand img {
    width: 34px;
    height: 34px;
    border-radius: 10px;
  }

  .mobile-brand span {
    font-size: 16px;
    font-weight: 700;
    color: #0f172a;
  }
}
</style>