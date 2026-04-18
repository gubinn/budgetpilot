<template>
  <div>
    <n-space justify="space-between" style="margin-bottom: 16px">
      <h3>用户管理</h3>
      <n-button type="primary" @click="openCreate">新增用户</n-button>
    </n-space>

    <n-data-table :columns="columns" :data="users" :loading="loading" :bordered="false" />

    <!-- 新增 / 编辑弹窗 -->
    <n-modal v-model:show="showModal" preset="card" :title="editId ? '编辑用户' : '新增用户'" style="width: 480px">
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="80">
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
        <n-form-item label="启用" path="isActive">
          <n-switch v-model:value="form.isActive" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showModal = false">取消</n-button>
          <n-button type="primary" :loading="saving" @click="handleSave">保存</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 重置密码弹窗 -->
    <n-modal v-model:show="showPwdModal" preset="card" title="重置密码" style="width: 400px">
      <n-form :model="pwdForm" label-placement="left" label-width="80">
        <n-form-item label="新密码">
          <n-input v-model:value="pwdForm.password" type="password" placeholder="新密码" show-password-on="click" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showPwdModal = false">取消</n-button>
          <n-button type="primary" :loading="saving" @click="handleResetPassword">确认重置</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, reactive, h, onMounted } from 'vue'
import { NButton, NSpace, NTag, useMessage, useDialog } from 'naive-ui'
import { userApi } from '@/api'

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

const columns = [
  { title: 'ID', key: 'id', width: 60 },
  { title: '用户名', key: 'username' },
  { title: '昵称', key: 'nickname' },
  {
    title: '角色',
    key: 'role',
    render(row) {
      return h(NTag, { type: row.role === 'ADMIN' ? 'error' : 'info', size: 'small' }, { default: () => row.role })
    }
  },
  {
    title: '状态',
    key: 'isActive',
    width: 80,
    render(row) {
      return h(NTag, { type: row.isActive ? 'success' : 'default', size: 'small' }, { default: () => row.isActive ? '启用' : '禁用' })
    }
  },
  { title: '最后登录', key: 'lastLogin', width: 180, render(row) { return row.lastLogin || '-' } },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    render(row) {
      return h(NSpace, { size: 'small' }, {
        default: () => [
          h(NButton, { size: 'small', onClick: () => openEdit(row) }, { default: () => '编辑' }),
          h(NButton, { size: 'small', type: 'warning', onClick: () => openResetPwd(row) }, { default: () => '重置密码' }),
          h(NButton, { size: 'small', type: 'error', onClick: () => handleDelete(row) }, { default: () => '删除' })
        ]
      })
    }
  }
]

async function loadUsers() {
  loading.value = true
  try {
    const res = await userApi.list()
    users.value = res
  } catch {
    message.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
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
  const valid = await formRef.value?.validate()
  if (valid) return

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
  } catch (e) {
    message.error(e.message || '操作失败')
  } finally {
    saving.value = false
  }
}

function openResetPwd(row) {
  pwdTargetId.value = row.id
  pwdForm.password = ''
  showPwdModal.value = true
}

async function handleResetPassword() {
  if (!pwdForm.password) {
    message.error('请输入新密码')
    return
  }
  saving.value = true
  try {
    await userApi.resetPassword(pwdTargetId.value, pwdForm.password)
    message.success('密码重置成功')
    showPwdModal.value = false
  } catch (e) {
    message.error(e.message || '重置失败')
  } finally {
    saving.value = false
  }
}

function handleDelete(row) {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除用户 "${row.username}" 吗？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await userApi.remove(row.id)
        message.success('删除成功')
        loadUsers()
      } catch {
        message.error('删除失败')
      }
    }
  })
}

onMounted(loadUsers)
</script>
