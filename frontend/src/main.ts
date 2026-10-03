import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ui from '@nuxt/ui/vue-plugin'

import App from './App.vue'
import router from './router'
import { setupDirectives } from './directives/perm'
import './assets/main.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ui)
setupDirectives(app)

router.isReady().then(() => app.mount('#app'))
