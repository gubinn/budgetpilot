<template>
  <div class="mobile-users m-page">
    <!-- 用户列表 -->
    <n-spin :show="loading">
      <div v-if="users.length" class="user-list">
        <div v-for="user in users" :key="user.id" class="user-row">
          <div class="avatar">{{ (user.nickname || user.username || '?').slice(0, 1) }}</div>
          <div class="m-grow">
            <div class="user-name m-ellipsis">{{ user.nickname || user.username }}</div>
            <div class="user-meta">
              <n-tag :type="user.role === 'ADMIN' ? 'error' : 'info'" size="tiny" :bordered="false">
                {{ user.role === 'ADMIN' ? '管理员' : '普通用户' }}
              </n-tag>
              <n-tag :type="user.isActive ? 'success' : 'default'" size="tiny" :bordered="false">
                {{ user.isActive ? '启用' : '禁用' }}
              </n-tag>
              <span class="last-login">{{ user.lastLogin || '从未登录' }}</span>
            </div>
          </div>
          <n-dropdown trigger="click" placement="bottom-end" :options="userActions" @select="key => handleAction(key, user)">
            <button class="m-more" aria-label="用户操作">
              <n-icon :size="18"><EllipsisHorizontal /></n-icon>
            </button>
          </n-dropdown>
        </div>
      </div>
      <div v-else-if="!loading" class="m-card">
        <div class="m-empty-cta">
          <div class="cta-icon"><n-icon :size="26"><PeopleOutline /></n-icon></div>
          <div class="cta-text">暂无用户</div>
          <n-button type="primary" round @click="openCreate">新增用户</n-button>
        </div>
      </div>
    </n-spin>

    <!-- 新增/编辑弹窗 -->
    <n-drawer v-model:show="showModal" :height="'65vh'" placement="bottom">
      <n-drawer-content :title="editId ? '编辑用户' : '新增用户'" closable>
        <n-form ref="formRef" :model="form" :rules="rules" label-placement="top" label-width="auto" class="m-form">
          <n-form-item label="用户名" path="username">
            <n-input v-model:value="form.username" :disabled="!!editId" placeholder="用户名" size="large" />
          </n-form-item>
          <n-form-item v-if="!editId" label="密码" path="password">
            <n-input v-model:value="form.password" type="password" placeholder="密码" show-password-on="click" size="large" />
          </n-form-item>
          <n-form-item label="昵称" path="nickname">
            <n-input v-model:value="form.nickname" placeholder="昵称" size="large" />
          </n-form-item>
          <n-form-item label="角色" path="role">
            <n-select v-model:value="form.role" :options="roleOptions" size="large" />
          </n-form-item>
          <n-form-item label="启用">
            <n-switch v-model:value="form.isActive" />
          </n-form-item>
        </n-form>
        <div class="m-sheet-actions">
          <n-button type="primary" :loading="saving" @click="handleSave">保存</n-button>
          <n-button quaternary @click="showModal = false">取消</n-button>
        </div>
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

    <div v-if="users.length" class="fab">
      <n-button type="primary" circle size="large" aria-label="新增用户" @click="openCreate">
        <template #icon><n-icon :size="24"><AddOutline /></n-icon></template>
      </n-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { NTag, useMessage, useDialog } from 'naive-ui'
import { userApi } from '@/api'
import { AddOutline, EllipsisHorizontal, PeopleOutline } from '@vicons/ionicons5'

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

const userActions = [
  { label: '编辑用户', key: 'edit' },
  { label: '重置密码', key: 'password' },
  { label: '删除用户', key: 'delete' }
]
function handleAction(key, user) {
  if (key === 'edit') openEdit(user)
  else if (key === 'password') openResetPwd(user)
  else if (key === 'delete') handleDelete(user)
}

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
.mobile-users { padding-bottom: calc(88px + env(safe-area-inset-bottom, 0px)); }

.user-list { display: flex; flex-direction: column; gap: 8px; }

.user-row {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  border-radius: var(--m-radius);
  padding: 12px 12px 12px 14px;
  box-shadow: var(--bp-shadow);
}

.avatar {
  flex-shrink: 0;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #2563eb, #0ea5a4);
  color: #fff;
  font-size: 17px;
  font-weight: 700;
}

.user-name { font-size: 15px; font-weight: 600; }
.user-meta { display: flex; align-items: center; gap: 6px; margin-top: 5px; }
.last-login { font-size: 11px; color: #b0b8c4; margin-left: 2px; }

.fab {
  position: fixed;
  bottom: calc(76px + env(safe-area-inset-bottom));
  right: 16px;
  z-index: 80;
}
</style>
