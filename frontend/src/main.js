import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useAuthStore } from '@/stores/auth'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
app.use(router)

// Fetch user info on startup if token exists
const auth = useAuthStore(pinia)
auth.fetchUser()

app.mount('#app')
