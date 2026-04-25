import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './style.css'
import App from './App.vue'
import router from './router'
import { localStoragePersistPlugin } from './stores/persist'

const pinia = createPinia()
pinia.use(localStoragePersistPlugin)

createApp(App)
  .use(pinia)
  .use(router)
  .mount('#app')
