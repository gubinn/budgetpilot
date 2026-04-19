<template>
  <div class="login-page">
    <div class="login-wrapper">
      <img src="/budgetpilot-logo.png" alt="BudgetPilot" class="login-logo" />
      <n-card class="login-card" :bordered="false">
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
        <n-button type="primary" size="large" block :loading="loading" @click="handleLogin">
          登 录
        </n-button>
      </n-form>
    </n-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
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

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: { required: true, message: '请输入用户名', trigger: 'blur' },
  password: { required: true, message: '请输入密码', trigger: 'blur' }
}

async function handleLogin() {
  loading.value = true
  try {
    await formRef.value?.validate()
    const res = await auth.login(form.username, form.password)
    if (res.code === 0) {
      message.success('登录成功')
      const redirect = route.query.redirect || '/'
      router.push(redirect)
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
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}
.login-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.login-logo {
  height: 80px;
  margin-bottom: 24px;
}
.login-card {
  width: 400px;
  max-width: 90vw;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
}
</style>
