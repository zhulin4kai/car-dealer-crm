import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import type { App } from 'vue'

import 'element-plus/dist/index.css'

export function installElementPlus(app: App): void {
  for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
  }

  app.use(ElementPlus, { locale: zhCn })
}
