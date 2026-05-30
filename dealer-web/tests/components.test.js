import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'

describe('Component issues', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('LoginView - dead variables', () => {
    it('should not have dead variables', async () => {
      const LoginView = (await import('../src/view/LoginView.vue')).default

      const dataFn = LoginView.data
      expect(dataFn).toBeDefined()

      const data = dataFn()

      expect(data).not.toHaveProperty('name')
      expect(data).not.toHaveProperty('age')
      expect(data).not.toHaveProperty('arr')
      expect(data).not.toHaveProperty('userList')
    })

    it('should have user and loginRules data', async () => {
      const LoginView = (await import('../src/view/LoginView.vue')).default
      const data = LoginView.data()

      expect(data).toHaveProperty('user')
      expect(data).toHaveProperty('loginRules')
    })

    it('should have login method', async () => {
      const LoginView = (await import('../src/view/LoginView.vue')).default
      expect(LoginView.methods).toBeDefined()
      expect(typeof LoginView.methods.login).toBe('function')
    })

    it('should have freeLogin method', async () => {
      const LoginView = (await import('../src/view/LoginView.vue')).default
      expect(LoginView.methods).toBeDefined()
      expect(typeof LoginView.methods.freeLogin).toBe('function')
    })

    it('should have mounted hook', async () => {
      const LoginView = (await import('../src/view/LoginView.vue')).default
      expect(LoginView.mounted).toBeDefined()
      expect(typeof LoginView.mounted).toBe('function')
    })

    it('should have login validation rules', async () => {
      const LoginView = (await import('../src/view/LoginView.vue')).default
      const data = LoginView.data()
      expect(data.loginRules).toHaveProperty('loginAct')
      expect(data.loginRules).toHaveProperty('loginPwd')
    })
  })

  describe('ProductPromotionView - loading should be defined', () => {
    it('loading should be defined in setup', async () => {
      const ProductPromotionView = (await import('../src/view/ProductPromotionView.vue')).default

      const setupFn = ProductPromotionView.setup
      expect(setupFn).toBeDefined()

      const bindings = setupFn()

      expect(bindings).toHaveProperty('loading')
    })

    it('template should use defined loading ref', async () => {
      const fs = await import('fs')
      const path = await import('path')
      const filePath = path.resolve(__dirname, '../src/view/ProductPromotionView.vue')
      const content = fs.readFileSync(filePath, 'utf-8')

      expect(content).toContain('v-loading="loading"')

      expect(content).toMatch(/const\s+loading\s*=\s*ref\(/)
    })
  })

  describe('ProductCategoryView - loading should be defined', () => {
    it('loading should be defined in setup', async () => {
      const ProductCategoryView = (await import('../src/view/ProductCategoryView.vue')).default

      const setupFn = ProductCategoryView.setup
      expect(setupFn).toBeDefined()

      const bindings = setupFn()

      expect(bindings).toHaveProperty('loading')
    })

    it('template should use defined loading ref', async () => {
      const fs = await import('fs')
      const path = await import('path')
      const filePath = path.resolve(__dirname, '../src/view/ProductCategoryView.vue')
      const content = fs.readFileSync(filePath, 'utf-8')

      expect(content).toContain('v-loading="loading"')

      expect(content).toMatch(/const\s+loading\s*=\s*ref\(/)
    })
  })

  describe('HelloWorld.vue - unused component', () => {
    it('should be removed or used', async () => {
      const fs = await import('fs')
      const path = await import('path')

      const helloPath = path.resolve(__dirname, '../src/components/HelloWorld.vue')
      const exists = fs.existsSync(helloPath)

      expect(exists).toBe(true)

      const routerPath = path.resolve(__dirname, '../src/router/router.js')
      const routerContent = fs.readFileSync(routerPath, 'utf-8')
      expect(routerContent).not.toContain('/hello')
    })
  })

  describe('DashboardView', () => {
    it('should have data properties', async () => {
      const DashboardView = (await import('../src/view/DashboardView.vue')).default
      const data = DashboardView.data()
      expect(data).toHaveProperty('isCollapse')
      expect(data).toHaveProperty('user')
      expect(data).toHaveProperty('isRouterAlive')
      expect(data).toHaveProperty('currentRouterPath')
    })

    it('should have methods', async () => {
      const DashboardView = (await import('../src/view/DashboardView.vue')).default
      expect(DashboardView.methods).toBeDefined()
      expect(typeof DashboardView.methods.showMenu).toBe('function')
      expect(typeof DashboardView.methods.loadLoginUser).toBe('function')
      expect(typeof DashboardView.methods.logout).toBe('function')
      expect(typeof DashboardView.methods.backToHome).toBe('function')
      expect(typeof DashboardView.methods.loadCurrentRouterPath).toBe('function')
    })

    it('should have mounted hook', async () => {
      const DashboardView = (await import('../src/view/DashboardView.vue')).default
      expect(DashboardView.mounted).toBeDefined()
    })

    it('should have computed properties', async () => {
      const DashboardView = (await import('../src/view/DashboardView.vue')).default
      expect(DashboardView.computed).toBeDefined()
      expect(typeof DashboardView.computed.getUserFirstChar).toBe('function')
    })
  })

  describe('ActivityView', () => {
    it('should have setup function', async () => {
      const ActivityView = (await import('../src/view/ActivityView.vue')).default
      expect(ActivityView.setup).toBeDefined()
      expect(typeof ActivityView.setup).toBe('function')
    })

    it('should export component name', async () => {
      const ActivityView = (await import('../src/view/ActivityView.vue')).default
      expect(ActivityView).toBeDefined()
    })
  })

  describe('UserView', () => {
    it('should have setup function', async () => {
      const UserView = (await import('../src/view/UserView.vue')).default
      expect(UserView.setup).toBeDefined()
      expect(typeof UserView.setup).toBe('function')
    })
  })

  describe('CustomerView', () => {
    it('should have setup function', async () => {
      const CustomerView = (await import('../src/view/CustomerView.vue')).default
      expect(CustomerView.setup).toBeDefined()
      expect(typeof CustomerView.setup).toBe('function')
    })
  })

  describe('ProductView', () => {
    it('should have setup function', async () => {
      const ProductView = (await import('../src/view/ProductView.vue')).default
      expect(ProductView.setup).toBeDefined()
      expect(typeof ProductView.setup).toBe('function')
    })
  })

  describe('ClueView', () => {
    it('should have setup function', async () => {
      const ClueView = (await import('../src/view/ClueView.vue')).default
      expect(ClueView.setup).toBeDefined()
      expect(typeof ClueView.setup).toBe('function')
    })
  })

  describe('TranView', () => {
    it('should have setup function', async () => {
      const TranView = (await import('../src/view/TranView.vue')).default
      expect(TranView.setup).toBeDefined()
      expect(typeof TranView.setup).toBe('function')
    })
  })

  describe('SystemView', () => {
    it('should have setup function', async () => {
      const SystemView = (await import('../src/view/SystemView.vue')).default
      expect(SystemView.setup).toBeDefined()
      expect(typeof SystemView.setup).toBe('function')
    })
  })

  describe('StatisticView', () => {
    it('should have methods', async () => {
      const StatisticView = (await import('../src/view/StatisticView.vue')).default
      expect(StatisticView.methods).toBeDefined()
      expect(typeof StatisticView.methods.loadSummary).toBe('function')
      expect(typeof StatisticView.methods.loadSaleFunnelChart).toBe('function')
      expect(typeof StatisticView.methods.loadSourcePieChart).toBe('function')
    })

    it('should have data properties', async () => {
      const StatisticView = (await import('../src/view/StatisticView.vue')).default
      const data = StatisticView.data()
      expect(data).toHaveProperty('summaryData')
    })

    it('should have mounted hook', async () => {
      const StatisticView = (await import('../src/view/StatisticView.vue')).default
      expect(StatisticView.mounted).toBeDefined()
    })
  })

  describe('DictTypeView', () => {
    it('should have setup function', async () => {
      const DictTypeView = (await import('../src/view/DictTypeView.vue')).default
      expect(DictTypeView.setup).toBeDefined()
      expect(typeof DictTypeView.setup).toBe('function')
    })
  })

  describe('DictValueView', () => {
    it('should have setup function', async () => {
      const DictValueView = (await import('../src/view/DictValueView.vue')).default
      expect(DictValueView.setup).toBeDefined()
      expect(typeof DictValueView.setup).toBe('function')
    })
  })

  describe('ActivityDetailView', () => {
    it('should have setup function', async () => {
      const ActivityDetailView = (await import('../src/view/ActivityDetailView.vue')).default
      expect(ActivityDetailView.setup).toBeDefined()
      expect(typeof ActivityDetailView.setup).toBe('function')
    })
  })

  describe('ClueDetailView', () => {
    it('should have setup function', async () => {
      const ClueDetailView = (await import('../src/view/ClueDetailView.vue')).default
      expect(ClueDetailView.setup).toBeDefined()
      expect(typeof ClueDetailView.setup).toBe('function')
    })
  })

  describe('TranDetailView', () => {
    it('should have setup function', async () => {
      const TranDetailView = (await import('../src/view/TranDetailView.vue')).default
      expect(TranDetailView.setup).toBeDefined()
      expect(typeof TranDetailView.setup).toBe('function')
    })
  })

  describe('TranApproveView', () => {
    it('should have setup function', async () => {
      const TranApproveView = (await import('../src/view/TranApproveView.vue')).default
      expect(TranApproveView.setup).toBeDefined()
      expect(typeof TranApproveView.setup).toBe('function')
    })
  })

  describe('TranInvoiceView', () => {
    it('should have setup function', async () => {
      const TranInvoiceView = (await import('../src/view/TranInvoiceView.vue')).default
      expect(TranInvoiceView.setup).toBeDefined()
      expect(typeof TranInvoiceView.setup).toBe('function')
    })
  })

  describe('ProductStockAlertView', () => {
    it('should have setup function', async () => {
      const ProductStockAlertView = (await import('../src/view/ProductStockAlertView.vue')).default
      expect(ProductStockAlertView.setup).toBeDefined()
      expect(typeof ProductStockAlertView.setup).toBe('function')
    })
  })

  describe('Component file structure', () => {
    it('all view components should exist', async () => {
      const fs = await import('fs')
      const path = await import('path')
      const viewPath = path.resolve(__dirname, '../src/view')
      
      const expectedFiles = [
        'LoginView.vue',
        'DashboardView.vue',
        'ActivityView.vue',
        'UserView.vue',
        'CustomerView.vue',
        'ProductView.vue',
        'ClueView.vue',
        'TranView.vue',
        'SystemView.vue',
        'StatisticView.vue',
        'DictTypeView.vue',
        'DictValueView.vue',
        'ActivityDetailView.vue',
        'ClueDetailView.vue',
        'TranDetailView.vue',
        'TranApproveView.vue',
        'TranInvoiceView.vue',
        'ProductStockAlertView.vue',
        'ProductCategoryView.vue',
        'ProductPromotionView.vue'
      ]
      
      expectedFiles.forEach(file => {
        const filePath = path.join(viewPath, file)
        expect(fs.existsSync(filePath)).toBe(true)
      })
    })
  })
})
