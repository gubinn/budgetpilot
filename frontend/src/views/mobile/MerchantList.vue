<template>
  <div class="mobile-merchants m-page">
    <!-- 搜索 -->
    <n-input v-model:value="searchKeyword" placeholder="搜索商户名称或原始名称" clearable size="large" class="search-input">
      <template #prefix><n-icon :size="16"><SearchOutline /></n-icon></template>
    </n-input>

    <!-- 商户列表 -->
    <n-spin :show="loading && !merchants.length">
      <div v-if="merchants.length" class="merchant-list">
        <div v-for="m in merchants" :key="m.id" class="merchant-row">
          <span class="color-dot" :style="{ background: m.color || '#cbd5e1' }"></span>
          <div class="m-grow">
            <div class="merchant-name m-ellipsis">
              {{ m.name }}
              <n-tag v-if="m.isSystem" size="tiny" type="info" :bordered="false">系统</n-tag>
            </div>
            <div class="merchant-meta m-ellipsis">
              <span v-if="m.categoryName">{{ m.categoryName }} · </span>
              <span v-if="m.aliasCount">{{ m.aliasCount }} 个原始名</span>
              <span v-if="m.description"> · {{ m.description }}</span>
              <span v-if="!m.categoryName && !m.aliasCount && !m.description">暂无关联信息</span>
            </div>
          </div>
          <span class="usage-count">{{ m.transactionCount ?? m.usageCount ?? 0 }} 笔</span>
          <n-dropdown trigger="click" placement="bottom-end" :options="merchantActions(m)" @select="key => handleAction(key, m)">
            <button class="m-more" aria-label="商户操作">
              <n-icon :size="18"><EllipsisHorizontal /></n-icon>
            </button>
          </n-dropdown>
        </div>

        <div v-if="total > merchants.length" class="m-load-more">
          <n-button secondary :loading="loading" @click="loadMore">
            加载更多（还有 {{ total - merchants.length }} 个）
          </n-button>
        </div>
      </div>

      <div v-else-if="!loading" class="m-card">
        <div class="m-empty-cta">
          <div class="cta-icon"><n-icon :size="26"><StorefrontOutline /></n-icon></div>
          <div class="cta-text">{{ searchKeyword ? '没有匹配的商户' : '还没有商户' }}</div>
          <n-button v-if="!searchKeyword" type="primary" round @click="openCreateModal">新增商户</n-button>
        </div>
      </div>
    </n-spin>

    <!-- 新增/编辑弹窗 -->
    <n-drawer v-model:show="showModal" :height="'85vh'" placement="bottom">
      <n-drawer-content :title="editingId ? '编辑商户' : '新增商户'" closable>
        <n-form ref="formRef" :model="form" label-placement="top" label-width="auto" class="m-form">
          <n-form-item label="名称" required>
            <n-input v-model:value="form.name" placeholder="如：麦当劳" size="large" />
          </n-form-item>
          <n-form-item label="别名">
            <n-input v-model:value="form.alias" placeholder="用于模糊匹配" size="large" />
          </n-form-item>
          <n-form-item label="关联分类">
            <n-cascader
              v-model:value="form.categoryId"
              :options="categoryTree"
              placeholder="可选"
              check-strategy="child"
              clearable
              filterable
              size="large"
              style="width: 100%"
            />
          </n-form-item>
          <n-form-item label="颜色">
            <n-color-picker v-model:value="form.color" :show-alpha="false" />
          </n-form-item>
          <n-form-item label="图标">
            <n-input v-model:value="form.icon" placeholder="图标标识" size="large" />
          </n-form-item>
          <n-form-item label="描述">
            <n-input v-model:value="form.description" type="textarea" :autosize="{ minRows: 2 }" placeholder="可选" />
          </n-form-item>
          <n-form-item label="标签">
            <n-dynamic-tags v-model:value="form.tags" />
          </n-form-item>
          <n-form-item v-if="editingId" label="状态">
            <n-switch v-model:value="form.isActive" />
          </n-form-item>
        </n-form>
        <div class="m-sheet-actions">
          <n-button type="primary" :loading="submitting" @click="handleSubmit">保存</n-button>
          <n-button quaternary @click="showModal = false">取消</n-button>
        </div>
      </n-drawer-content>
    </n-drawer>

    <div v-if="merchants.length" class="fab">
      <n-button type="primary" circle size="large" aria-label="新增商户" @click="openCreateModal">
        <template #icon><n-icon :size="24"><AddOutline /></n-icon></template>
      </n-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useDialog, useMessage } from 'naive-ui'
import { merchantApi, categoryApi } from '@/api'
import { AddOutline, SearchOutline, EllipsisHorizontal, StorefrontOutline } from '@vicons/ionicons5'

const dialog = useDialog()
const message = useMessage()
const loading = ref(false)
const merchants = ref([])
const categories = ref([])
const searchKeyword = ref('')
const showModal = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const formRef = ref(null)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
let searchTimer = null

const form = ref({ name: '', alias: '', categoryId: null, color: '#3498db', icon: '', description: '', tags: [], isActive: true })

const categoryTree = computed(() => {
  const expenseCategories = categories.value.filter(c => c.type === 1 && c.parentId === 0)
  return expenseCategories.map(buildCascaderOption)
})

function buildCascaderOption(c) {
  const opt = { label: c.name, value: c.id }
  const children = categories.value.filter(child => child.parentId === c.id)
  if (children.length) opt.children = children.map(buildCascaderOption)
  return opt
}

async function loadCategories() {
  try {
    const res = await categoryApi.tree(1)
    const flat = []
    function flatten(list) {
      list.forEach(c => { flat.push(c); if (c.children?.length) flatten(c.children) })
    }
    flatten(res.data || [])
    categories.value = flat
  } catch (e) {
    message.error('分类加载失败')
  }
}

function merchantActions(m) {
  const actions = [{ label: '编辑商户', key: 'edit' }]
  if (!m.isSystem) actions.push({ label: '删除商户', key: 'delete' })
  return actions
}

function handleAction(key, m) {
  if (key === 'edit') handleEdit(m)
  else if (key === 'delete') handleDelete(m.id)
}

async function loadData(reset = true) {
  loading.value = true
  try {
    const res = await merchantApi.page({
      page: page.value,
      size: pageSize.value,
      keyword: searchKeyword.value?.trim() || undefined
    })
    const data = res.data || {}
    const items = data.items || []
    merchants.value = reset ? items : merchants.value.concat(items)
    total.value = data.total || 0
  } catch (e) {
    message.error('加载失败')
    if (reset) { merchants.value = []; total.value = 0 }
  } finally {
    loading.value = false
  }
}

function reload() {
  page.value = 1
  loadData(true)
}

function loadMore() {
  page.value += 1
  loadData(false)
}

watch(searchKeyword, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(reload, 300)
})

function openCreateModal() {
  editingId.value = null
  form.value = { name: '', alias: '', categoryId: null, color: '#3498db', icon: '', description: '', tags: [], isActive: true }
  showModal.value = true
}

function handleEdit(row) {
  editingId.value = row.id
  form.value = {
    name: row.name, alias: row.alias || '', categoryId: row.categoryId,
    color: row.color || '#3498db', icon: row.icon || '', description: row.description || '',
    tags: row.tags || [], isActive: row.isActive
  }
  showModal.value = true
}

async function handleSubmit() {
  if (!form.value.name) { message.error('请输入商户名称'); return }
  submitting.value = true
  try {
    const data = { ...form.value }
    if (Array.isArray(data.categoryId)) {
      data.categoryId = data.categoryId[data.categoryId.length - 1] || null
    }
    if (editingId.value) {
      await merchantApi.update(editingId.value, data)
      message.success('更新成功')
    } else {
      await merchantApi.create(data)
      message.success('创建成功')
    }
    showModal.value = false
    editingId.value = null
    reload()
  } catch (e) {
    message.error(e.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

function handleDelete(id) {
  dialog.warning({
    title: '确认删除', content: '删除后不可恢复，是否继续？',
    positiveText: '删除', negativeText: '取消',
    onPositiveClick: async () => {
      try { await merchantApi.remove(id); message.success('已删除'); reload() }
      catch (e) { message.error(e.response?.data?.message || '删除失败') }
    }
  })
}

onMounted(async () => {
  await loadCategories()
  await loadData(true)
})
</script>

<style scoped>
.mobile-merchants {
  padding-bottom: calc(88px + env(safe-area-inset-bottom, 0px));
}

.search-input :deep(.n-input__input-el) {
  height: 42px;
}

.merchant-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.merchant-row {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  border-radius: var(--m-radius);
  padding: 12px 12px 12px 16px;
  box-shadow: var(--bp-shadow);
}

.color-dot {
  flex-shrink: 0;
  width: 12px;
  height: 12px;
  border-radius: 4px;
}

.merchant-name {
  font-size: 15px;
  font-weight: 550;
  color: var(--bp-ink);
  display: flex;
  align-items: center;
  gap: 6px;
}
.merchant-meta {
  margin-top: 3px;
  font-size: 12px;
  color: #94a3b8;
}

.usage-count {
  flex-shrink: 0;
  font-size: 12px;
  color: #b0b8c4;
  font-variant-numeric: tabular-nums;
}

.fab {
  position: fixed;
  bottom: calc(76px + env(safe-area-inset-bottom));
  right: 16px;
  z-index: 80;
}
</style>