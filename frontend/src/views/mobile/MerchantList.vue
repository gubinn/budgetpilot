<template>
  <div class="mobile-merchants">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <n-input v-model:value="searchKeyword" placeholder="搜索商户名称或别名" clearable />
    </div>

    <!-- 商户卡片列表 -->
    <div v-if="filteredMerchants.length" class="merchant-list">
      <div v-for="m in filteredMerchants" :key="m.id" class="merchant-card">
        <div class="merchant-header">
          <div class="merchant-left">
            <span class="color-dot" :style="{ background: m.color || '#ccc' }"></span>
            <span class="merchant-name">{{ m.name }}</span>
            <n-tag v-if="m.isSystem" size="tiny" type="info" :bordered="false">系统</n-tag>
          </div>
          <span class="usage-count">{{ m.usageCount }} 次</span>
        </div>
        <div v-if="m.alias" class="merchant-alias">别名: {{ m.alias }}</div>
        <div v-if="m.categoryName" class="merchant-category">关联: {{ m.categoryName }}</div>
        <div v-if="m.description" class="merchant-desc">{{ m.description }}</div>
        <div class="merchant-actions">
          <n-button size="small" type="primary" secondary @click="handleEdit(m)">编辑</n-button>
          <n-button v-if="!m.isSystem" size="small" type="error" secondary @click="handleDelete(m.id)">删除</n-button>
        </div>
      </div>
    </div>
    <n-empty v-else-if="!loading" description="暂无商户" />
    <n-spin :show="loading" />

    <!-- 新增/编辑弹窗 -->
    <n-drawer v-model:show="showModal" :height="'75vh'" placement="bottom">
      <n-drawer-content :title="editingId ? '编辑商户' : '新增商户'" closable>
        <n-form ref="formRef" :model="form" label-placement="top" label-width="auto">
          <n-form-item label="名称" required>
            <n-input v-model:value="form.name" placeholder="如：麦当劳" />
          </n-form-item>
          <n-form-item label="别名">
            <n-input v-model:value="form.alias" placeholder="用于模糊匹配" />
          </n-form-item>
          <n-form-item label="关联分类">
            <n-cascader
              v-model:value="form.categoryId"
              :options="categoryTree"
              placeholder="可选"
              check-strategy="child"
              clearable
              filterable
              style="width: 100%"
            />
          </n-form-item>
          <n-form-item label="颜色">
            <n-color-picker v-model:value="form.color" :show-alpha="false" />
          </n-form-item>
          <n-form-item label="图标">
            <n-input v-model:value="form.icon" placeholder="图标标识" />
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
        <template #footer>
          <n-space>
            <n-button type="primary" :loading="submitting" @click="handleSubmit">保存</n-button>
            <n-button @click="showModal = false">取消</n-button>
          </n-space>
        </template>
      </n-drawer-content>
    </n-drawer>

    <div class="fab">
      <n-button type="primary" round size="large" @click="openCreateModal">
        <template #icon><n-icon><AddOutline /></n-icon></template>
        新增商户
      </n-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useDialog, useMessage } from 'naive-ui'
import { merchantApi, categoryApi } from '@/api'
import { AddOutline } from '@vicons/ionicons5'

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

const filteredMerchants = computed(() => {
  if (!searchKeyword.value) return merchants.value
  const kw = searchKeyword.value.toLowerCase()
  return merchants.value.filter(m =>
    m.name.toLowerCase().includes(kw) || (m.alias && m.alias.toLowerCase().includes(kw))
  )
})

async function loadData() {
  loading.value = true
  try {
    const [mRes, cRes] = await Promise.all([
      merchantApi.list({ size: 100 }),
      categoryApi.tree(1)
    ])
    merchants.value = mRes.data || []
    const flat = []
    function flatten(list) {
      list.forEach(c => { flat.push(c); if (c.children?.length) flatten(c.children) })
    }
    flatten(cRes.data || [])
    categories.value = flat
  } catch (e) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

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
    loadData()
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
      try { await merchantApi.remove(id); message.success('已删除'); loadData() }
      catch (e) { message.error('删除失败') }
    }
  })
}

onMounted(loadData)
</script>

<style scoped>
.mobile-merchants { padding-bottom: calc(70px + env(safe-area-inset-bottom, 0px)); }

.search-bar { margin-bottom: 12px; }

.merchant-list { display: flex; flex-direction: column; gap: 10px; }

.merchant-card {
  background: #fff;
  border-radius: 10px;
  padding: 14px;
}

.merchant-header { display: flex; justify-content: space-between; align-items: center; }
.merchant-left { display: flex; align-items: center; gap: 8px; }
.color-dot { width: 14px; height: 14px; border-radius: 4px; flex-shrink: 0; }
.merchant-name { font-size: 15px; font-weight: 600; }
.usage-count { font-size: 12px; color: #999; }

.merchant-alias, .merchant-category { font-size: 12px; color: #666; margin-top: 4px; }
.merchant-desc { font-size: 12px; color: #999; margin-top: 4px; }

.merchant-actions { display: flex; gap: 8px; margin-top: 10px; justify-content: flex-end; }

.fab {
  position: fixed;
  bottom: calc(70px + env(safe-area-inset-bottom));
  right: 16px;
  z-index: 50;
}
</style>
