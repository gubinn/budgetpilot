<template>
  <div class="mobile-form m-page">
    <MobileTransactionFormBody
      :transaction-id="route.params.id"
      @saved="onSaved"
      @cancel="router.back()"
    />
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import MobileTransactionFormBody from '@/components/transaction/MobileTransactionFormBody.vue'

const route = useRoute()
const router = useRouter()

function onSaved() {
  router.push('/transactions')
}
</script>

<style scoped>
.mobile-form {
  padding-bottom: 8px;
}
</style>