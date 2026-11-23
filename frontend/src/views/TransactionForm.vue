<template>
  <n-card :title="isEdit ? '编辑交易' : '新增交易'" style="max-width: 600px; margin: 0 auto">
    <TransactionFormBody
      :transaction-id="route.params.id"
      @saved="onSaved"
      @cancel="router.back()"
    />
  </n-card>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TransactionFormBody from '@/components/transaction/TransactionFormBody.vue'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)

function onSaved() {
  router.push('/transactions')
}
</script>