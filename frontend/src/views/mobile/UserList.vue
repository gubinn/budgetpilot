<template>
  <div class="mobile-users">
    <!-- 用户列表 -->
    <div v-if="users.length" class="user-list">
      <div v-for="user in users" :key="user.id" class="user-card">
        <div class="user-info">
          <div class="user-name">{{ user.nickname || user.username }}</div>
          <div class="user-meta">
            <n-tag :type="user.role === 'ADMIN' ? 'error' : 'info'" size="tiny" :bordered="false">{{ user.role }}</n-tag>
            <n-tag :type="user.isActive ? 'success' : 'default'" size="tiny" :bordered="false">{{ user.isActive ? '启用' : '禁用' }}</n-tag>
            <span class="last-login">{{ user.lastLogin || '-' }}</span>
          </div>
        </div>
        <div class="user-actions">
          <n-button size="small" type="primary" secondary @click="openEdit(user)">编辑</n-button>
          <n-button size="small" secondary @click="openResetPwd(user)">重置密码</n-button>
          <n-button size="small" type="error" secondary @click="handleDelete(user)">删除</n-button>
        </div>
      </div>
    </div>
    <n-empty v-else-if="!loading" description="暂无用户" />
    <n-spin :show="loading" />

    <!-- 新增/编辑弹窗 -->
    <n-drawer v-model:show="showModal" :height="'65vh'" placement="bottom">
      <n-drawer-content :title="editId ? '编辑用户' : '新增用户'" closable>
        <n-form ref="formRef" :model="form" :rules="rules" label-placement="top" label-width="auto">
          <n-form-item label="用户名" path="username">
            <n-input v-model:value="form.username" :disabled="!!editId" placeholder="用户名" />
          </n-form-item>
          <n-form-item v-if="!editId" label="密码" path="password">
            <n-input v-model:value="form.password" type="password" placeholder="密码" show-password-on="click" />
          </n-form-item>
          <n-form-item label="昵称" path="nickname">
            <n-input v-model:value="form.nickname" placeholder="昵称" />
          </n-form-item>
          <n-form-item label="角色" path="role">
            <n-select v-model:value="form.role" :options="roleOptions" />
          </n-form-item>
          <n-form-item label="启用">
            <n-switch v-model:value="form.isActive" />
          </n-form-item>
        </n-form>
        <template #footer>
          <n-space>
            <n-button type="primary" :loading="saving" @click="handleSave">保存</n-button>
            <n-button @click="showModal = false">取消</n-button>
          </n-space>
        </template>
      </n-drawer-content>
    </n-drawer>

    <!-- 重置密码弹窗 -->
    <n-modal v-model:show="showPwdModal" preset="card" title="重置密码" style="width: 90vw; max-width: 400px">
      <n-form-item label="新密码">
        <n-input v-model:value="pwdForm.password" type="password" placeholder="新密码" show-password-on="click" />
      </n-form-item>
      <template #footer>
        <n-space>
          <n-button type="primary" :loading="saving" @click="handleResetPassword">确认重置</n-button>
          <n-button @click="showPwdModal = false">取消</n-button>
        </n-space>
      </template>
    </n-modal>

    <div class="fab">
      <n-button type="primary" round size="large" @click="openCreate">
        <template #icon><n-icon><AddOutline /></n-icon></template>
        新增用户
      </n-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { NTag, useMessage, useDialog } from 'naive-ui'
import { userApi } from '@/api'
import { AddOutline } from '@vicons/ionicons5'

const message = useMessage()
const dialog = useDialog()
const users = ref([])
const loading = ref(false)
const saving = ref(false)
const showModal = ref(false)
const showPwdModal = ref(false)
const editId = ref(null)
const formRef = ref(null)
const pwdTargetId = ref(null)

const form = reactive({ username: '', password: '', nickname: '', role: 'USER', isActive: true })
const pwdForm = reactive({ password: '' })

const roleOptions = [
  { label: '管理员', value: 'ADMIN' },
  { label: '普通用户', value: 'USER' }
]

const rules = {
  username: { required: true, message: '请输入用户名', trigger: 'blur' },
  password: { required: true, message: '请输入密码', trigger: 'blur' },
  role: { required: true, message: '请选择角色', trigger: 'change' }
}

async function loadUsers() {
  loading.value = true
  try {
    const res = await userApi.list()
    users.value = res.data || res
  } catch (e) {
    console.error('loadUsers error:', e)
    message.error('加载用户列表失败')
  } finally { loading.value = false }
}

function openCreate() {
  editId.value = null
  Object.assign(form, { username: '', password: '', nickname: '', role: 'USER', isActive: true })
  showModal.value = true
}

function openEdit(row) {
  editId.value = row.id
  Object.assign(form, { username: row.username, password: '', nickname: row.nickname, role: row.role, isActive: row.isActive })
  showModal.value = true
}

async function handleSave() {
  try { await formRef.value?.validate() } catch { return }
  saving.value = true
  try {
    if (editId.value) {
      await userApi.update(editId.value, { nickname: form.nickname, role: form.role, isActive: form.isActive })
      message.success('更新成功')
    } else {
      await userApi.create(form)
      message.success('创建成功')
    }
    showModal.value = false
    loadUsers()
  } catch (e) { message.error(e.message || '操作失败') } finally { saving.value = false }
}

function openResetPwd(row) {
  pwdTargetId.value = row.id
  pwdForm.password = ''
  showPwdModal.value = true
}

async function handleResetPassword() {
  if (!pwdForm.password) { message.error('请输入新密码'); return }
  saving.value = true
  try {
    await userApi.resetPassword(pwdTargetId.value, pwdForm.password)
    message.success('密码重置成功')
    showPwdModal.value = false
  } catch (e) { message.error(e.message || '重置失败') } finally { saving.value = false }
}

function handleDelete(row) {
  dialog.warning({
    title: '确认删除', content: `确定要删除用户 "${row.username}" 吗？`,
    positiveText: '删除', negativeText: '取消',
    onPositiveClick: async () => {
      try { await userApi.remove(row.id); message.success('删除成功'); loadUsers() }
      catch { message.error('删除失败') }
    }
  })
}

onMounted(loadUsers)
</script>

<style scoped>
.mobile-users { padding-bottom: calc(70px + env(safe-area-inset-bottom, 0px)); }

.user-list { display: flex; flex-direction: column; gap: 10px; }

.user-card { background: #fff; border-radius: 10px; padding: 14px; }

.user-name { font-size: 15px; font-weight: 600; }
.user-meta { display: flex; align-items: center; gap: 6px; margin-top: 4px; }
.last-login { font-size: 11px; color: #999; margin-left: 4px; }

.user-actions { display: flex; gap: 6px; margin-top: 10px; flex-wrap: wrap; }

.fab {
  position: fixed;
  bottom: calc(70px + env(safe-area-inset-bottom));
  right: 16px;
  z-index: 50;
}
</style>
