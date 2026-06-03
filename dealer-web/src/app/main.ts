import { createApp } from 'vue'

import App from '@/App.vue'
import { installPermissionDirective } from '@/app/directives/has-permission'
import { pinia } from '@/app/plugins/pinia'
import router from '@/router'

import '@/assets/index.css'

const app = createApp(App)

app.use(pinia)
installPermissionDirective(app)
app.use(router)

app.mount('#app')
