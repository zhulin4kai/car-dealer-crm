import { createApp } from 'vue'

import App from '@/App.vue'
import { installPermissionDirective } from '@/app/directives/has-permission'
import { installElementPlus } from '@/app/plugins/element-plus'
import { pinia } from '@/app/plugins/pinia'
import router from '@/router'

import '@/assets/global.css'

const app = createApp(App)

app.use(pinia)
installElementPlus(app)
installPermissionDirective(app)
app.use(router)

app.mount('#app')
