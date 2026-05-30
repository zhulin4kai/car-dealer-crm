import { describe, it, expect, vi, beforeEach } from 'vitest'
import axios from 'axios'

describe('API module consistency', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('clue.js', () => {
    it('getCurrentClues should use leading slash', async () => {
      const clueModule = await import('../src/api/clue.js')
      clueModule.getCurrentClues(1)
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.url).toBe('/api/clues')
    })

    it('getOwnerList should not exist in clue.js', async () => {
      const clueModule = await import('../src/api/clue.js')
      expect(clueModule.getOwnerList).toBeUndefined()
    })

    it('batchDeleteCluesByIds should call doPost', async () => {
      const clueModule = await import('../src/api/clue.js')
      clueModule.batchDeleteCluesByIds([1, 2, 3])
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('post')
      expect(config.url).toBe('/api/clue/batch')
    })

    it('importExcelAPI should call doPost', async () => {
      const clueModule = await import('../src/api/clue.js')
      const file = new FormData()
      clueModule.importExcelAPI(file)
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('post')
      expect(config.url).toBe('/api/importExcel')
    })

    it('delClueById should call doDelete', async () => {
      const clueModule = await import('../src/api/clue.js')
      clueModule.delClueById(123)
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('delete')
      expect(config.url).toBe('/api/clue/123')
    })

    it('checkPhoneIsExist should call doGet', async () => {
      const clueModule = await import('../src/api/clue.js')
      clueModule.checkPhoneIsExist('13800138000')
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/clue/13800138000')
    })

    it('getLoginInfo should call doGet', async () => {
      const clueModule = await import('../src/api/clue.js')
      clueModule.getLoginInfo()
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/login/info')
    })

    it('getClueDetail should call doGet', async () => {
      const clueModule = await import('../src/api/clue.js')
      clueModule.getClueDetail(456)
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/clue/detail/456')
    })

    it('addClue should call doPost', async () => {
      const clueModule = await import('../src/api/clue.js')
      const formData = new FormData()
      clueModule.addClue(formData)
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('post')
      expect(config.url).toBe('/api/clue')
    })

    it('updateClue should call doPut', async () => {
      const clueModule = await import('../src/api/clue.js')
      const formData = new FormData()
      clueModule.updateClue(formData)
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('put')
      expect(config.url).toBe('/api/clue')
    })

    it('addClueRemark should call doPost', async () => {
      const clueModule = await import('../src/api/clue.js')
      clueModule.addClueRemark(1, 'test note', 'phone')
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('post')
      expect(config.url).toBe('/api/clue/remark')
    })

    it('getClueRemarkList should call doGet', async () => {
      const clueModule = await import('../src/api/clue.js')
      clueModule.getClueRemarkList(1, 100)
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/clue/remark')
    })

    it('convertClueToCustomer should call doPost', async () => {
      const clueModule = await import('../src/api/clue.js')
      clueModule.convertClueToCustomer(1, 'product', 'desc', '2024-01-01')
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('post')
      expect(config.url).toBe('/api/clue/customer')
    })
  })

  describe('dict.js', () => {
    it('getDictValueDetail should match backend path', async () => {
      const dictModule = await import('../src/api/dict.js')
      dictModule.getDictValueDetail(42)
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.url).toBe('/api/dict/value/get/42')
    })

    it('getDictTypeList should call doGet', async () => {
      const dictModule = await import('../src/api/dict.js')
      dictModule.getDictTypeList({ current: 1 })
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/dict/types')
    })

    it('getDictTypeDetail should call doGet', async () => {
      const dictModule = await import('../src/api/dict.js')
      dictModule.getDictTypeDetail(1)
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/dict/type/get/1')
    })

    it('createDictType should call doPost', async () => {
      const dictModule = await import('../src/api/dict.js')
      dictModule.createDictType({ name: 'test' })
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('post')
      expect(config.url).toBe('/api/dict/type/create')
    })

    it('updateDictType should call doPut', async () => {
      const dictModule = await import('../src/api/dict.js')
      dictModule.updateDictType(1, { name: 'test' })
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('put')
      expect(config.url).toBe('/api/dict/type/update/1')
    })

    it('deleteDictType should call doDelete', async () => {
      const dictModule = await import('../src/api/dict.js')
      dictModule.deleteDictType(1)
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('delete')
      expect(config.url).toBe('/api/dict/type/delete/1')
    })

    it('batchDeleteDictTypes should call doDelete', async () => {
      const dictModule = await import('../src/api/dict.js')
      dictModule.batchDeleteDictTypes([1, 2])
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('delete')
      expect(config.url).toBe('/api/dict/types/batch')
    })

    it('getDictValueList should call doGet', async () => {
      const dictModule = await import('../src/api/dict.js')
      dictModule.getDictValueList({ current: 1 })
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/dict/values')
    })

    it('createDictValue should call doPost', async () => {
      const dictModule = await import('../src/api/dict.js')
      dictModule.createDictValue({ value: 'test' })
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('post')
      expect(config.url).toBe('/api/dict/value/create')
    })

    it('updateDictValue should call doPut', async () => {
      const dictModule = await import('../src/api/dict.js')
      dictModule.updateDictValue(1, { value: 'test' })
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('put')
      expect(config.url).toBe('/api/dict/value/update/1')
    })

    it('deleteDictValue should call doDelete', async () => {
      const dictModule = await import('../src/api/dict.js')
      dictModule.deleteDictValue(1)
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('delete')
      expect(config.url).toBe('/api/dict/value/delete/1')
    })

    it('batchDeleteDictValues should call doDelete', async () => {
      const dictModule = await import('../src/api/dict.js')
      dictModule.batchDeleteDictValues([1, 2])
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('delete')
      expect(config.url).toBe('/api/dict/value/batch')
    })

    it('clearCache should call doGet', async () => {
      const dictModule = await import('../src/api/dict.js')
      dictModule.clearCache()
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/dict/clear')
    })
  })

  describe('user.js', () => {
    it('batchDeleteUsers should send plain array', async () => {
      const userModule = await import('../src/api/user.js')
      const ids = [1, 2, 3]
      userModule.batchDeleteUsers(ids)
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.url).toBe('/api/user')
      expect(config.method).toBe('delete')
      expect(config.data).toEqual([1, 2, 3])
      expect(Array.isArray(config.data)).toBe(true)
    })

    it('getUserList should call doGet', async () => {
      const userModule = await import('../src/api/user.js')
      userModule.getUserList({ current: 1 })
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/users')
    })

    it('getUserDetail should call doGet', async () => {
      const userModule = await import('../src/api/user.js')
      userModule.getUserDetail(1)
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/user/1')
    })

    it('createUser should call doPost', async () => {
      const userModule = await import('../src/api/user.js')
      userModule.createUser({ name: 'test' })
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('post')
      expect(config.url).toBe('/api/user')
    })

    it('updateUser should call doPut', async () => {
      const userModule = await import('../src/api/user.js')
      userModule.updateUser({ id: 1, name: 'test' })
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('put')
      expect(config.url).toBe('/api/user')
    })

    it('deleteUser should call doDelete', async () => {
      const userModule = await import('../src/api/user.js')
      userModule.deleteUser(1)
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('delete')
      expect(config.url).toBe('/api/user/1')
    })
  })

  describe('activity.js', () => {
    it('getActivityList should call doGet', async () => {
      const activityModule = await import('../src/api/activity.js')
      activityModule.getActivityList({ current: 1 })
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/activitys')
    })

    it('getOwnerList should call doGet', async () => {
      const activityModule = await import('../src/api/activity.js')
      activityModule.getOwnerList()
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/owner')
    })

    it('batchDeleteActivities should call doPost', async () => {
      const activityModule = await import('../src/api/activity.js')
      activityModule.batchDeleteActivities([1, 2])
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('post')
      expect(config.url).toBe('/api/activity/batch')
    })

    it('deleteActivity should call doDelete', async () => {
      const activityModule = await import('../src/api/activity.js')
      activityModule.deleteActivity(1)
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('delete')
      expect(config.url).toBe('/api/activity/1')
    })

    it('getActivityById should call doGet', async () => {
      const activityModule = await import('../src/api/activity.js')
      activityModule.getActivityById(1)
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/activity/1')
    })

    it('createActivity should call doPost', async () => {
      const activityModule = await import('../src/api/activity.js')
      activityModule.createActivity(new FormData())
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('post')
      expect(config.url).toBe('/api/activity')
    })

    it('updateActivity should call doPut', async () => {
      const activityModule = await import('../src/api/activity.js')
      activityModule.updateActivity(new FormData())
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('put')
      expect(config.url).toBe('/api/activity')
    })
  })

  describe('customer.js', () => {
    it('getCustomerList should call doGet', async () => {
      const customerModule = await import('../src/api/customer.js')
      customerModule.getCustomerList({ current: 1 })
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/customer/list')
    })

    it('getCustomerOptions should call doGet', async () => {
      const customerModule = await import('../src/api/customer.js')
      customerModule.getCustomerOptions()
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/customer/options')
    })
  })

  describe('product.js', () => {
    it('getProductList should call doGet', async () => {
      const productModule = await import('../src/api/product.js')
      productModule.getProductList({ current: 1 })
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/products')
    })

    it('getProductDetail should call doGet', async () => {
      const productModule = await import('../src/api/product.js')
      productModule.getProductDetail(1)
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/products/1')
    })

    it('createProduct should call doPost', async () => {
      const productModule = await import('../src/api/product.js')
      productModule.createProduct({ name: 'test' })
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('post')
      expect(config.url).toBe('/api/products')
    })

    it('updateProduct should call doPut', async () => {
      const productModule = await import('../src/api/product.js')
      productModule.updateProduct(1, { name: 'test' })
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('put')
      expect(config.url).toBe('/api/products/1')
    })

    it('deleteProduct should call doDelete', async () => {
      const productModule = await import('../src/api/product.js')
      productModule.deleteProduct(1)
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('delete')
      expect(config.url).toBe('/api/products/1')
    })

    it('getStockAlerts should call doGet', async () => {
      const productModule = await import('../src/api/product.js')
      productModule.getStockAlerts({ current: 1 })
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/products/stockalerts')
    })

    it('restockProduct should call doPost', async () => {
      const productModule = await import('../src/api/product.js')
      productModule.restockProduct({ productId: 1, quantity: 10 })
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('post')
      expect(config.url).toBe('/api/productstock/restock')
    })

    it('getStockRecords should call doGet', async () => {
      const productModule = await import('../src/api/product.js')
      productModule.getStockRecords(1, { current: 1 })
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/productstock/records/1')
    })

    it('getPromotionList should call doGet', async () => {
      const productModule = await import('../src/api/product.js')
      productModule.getPromotionList({ current: 1 })
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/product-promotions')
    })

    it('createPromotion should call doPost', async () => {
      const productModule = await import('../src/api/product.js')
      productModule.createPromotion({ name: 'test' })
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('post')
      expect(config.url).toBe('/api/product-promotions')
    })

    it('updatePromotion should call doPut', async () => {
      const productModule = await import('../src/api/product.js')
      productModule.updatePromotion(1, { name: 'test' })
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('put')
      expect(config.url).toBe('/api/product-promotions/1')
    })

    it('deletePromotion should call doDelete', async () => {
      const productModule = await import('../src/api/product.js')
      productModule.deletePromotion(1)
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('delete')
      expect(config.url).toBe('/api/product-promotions/1')
    })

    it('getCategoryList should call doGet', async () => {
      const productModule = await import('../src/api/product.js')
      productModule.getCategoryList({ current: 1 })
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/product-categories')
    })

    it('createCategory should call doPost', async () => {
      const productModule = await import('../src/api/product.js')
      productModule.createCategory({ name: 'test' })
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('post')
      expect(config.url).toBe('/api/product-categories')
    })

    it('updateCategory should call doPut', async () => {
      const productModule = await import('../src/api/product.js')
      productModule.updateCategory(1, { name: 'test' })
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('put')
      expect(config.url).toBe('/api/product-categories/1')
    })

    it('deleteCategory should call doDelete', async () => {
      const productModule = await import('../src/api/product.js')
      productModule.deleteCategory(1)
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('delete')
      expect(config.url).toBe('/api/product-categories/1')
    })
  })

  describe('system.js', () => {
    it('getSystemList should call doGet', async () => {
      const systemModule = await import('../src/api/system.js')
      systemModule.getSystemList()
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/system/list')
    })

    it('getSystemDetail should call doGet', async () => {
      const systemModule = await import('../src/api/system.js')
      systemModule.getSystemDetail(1)
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/system/1')
    })

    it('updateSystem should call doPut', async () => {
      const systemModule = await import('../src/api/system.js')
      systemModule.updateSystem(1, { name: 'test' })
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('put')
      expect(config.url).toBe('/api/system/1')
    })

    it('createSystem should call doPost', async () => {
      const systemModule = await import('../src/api/system.js')
      systemModule.createSystem({ name: 'test' })
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('post')
      expect(config.url).toBe('/api/system/create')
    })

    it('deleteSystem should call doDelete', async () => {
      const systemModule = await import('../src/api/system.js')
      systemModule.deleteSystem(1)
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('delete')
      expect(config.url).toBe('/api/system/1')
    })

    it('batchDeleteSystems should call doDelete', async () => {
      const systemModule = await import('../src/api/system.js')
      systemModule.batchDeleteSystems([1, 2])
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('delete')
      expect(config.url).toBe('/api/system/batch')
    })

    it('toggleSystemStatus should call doPut', async () => {
      const systemModule = await import('../src/api/system.js')
      systemModule.toggleSystemStatus(1, true)
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('put')
      expect(config.url).toBe('/api/system/1/status')
    })

    it('getSystemMonitorInfo should call doGet', async () => {
      const systemModule = await import('../src/api/system.js')
      systemModule.getSystemMonitorInfo()
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/monitor/system-info')
    })

    it('getMemoryInfo should call doGet', async () => {
      const systemModule = await import('../src/api/system.js')
      systemModule.getMemoryInfo()
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/monitor/memory-info')
    })

    it('getCpuInfo should call doGet', async () => {
      const systemModule = await import('../src/api/system.js')
      systemModule.getCpuInfo()
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/monitor/cpu-info')
    })

    it('getDiskInfo should call doGet', async () => {
      const systemModule = await import('../src/api/system.js')
      systemModule.getDiskInfo()
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/monitor/disk-info')
    })

    it('getJvmInfo should call doGet', async () => {
      const systemModule = await import('../src/api/system.js')
      systemModule.getJvmInfo()
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/monitor/jvm-info')
    })

    it('getNetworkInfo should call doGet', async () => {
      const systemModule = await import('../src/api/system.js')
      systemModule.getNetworkInfo()
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/monitor/network-info')
    })

    it('getAllMonitorData should call doGet', async () => {
      const systemModule = await import('../src/api/system.js')
      systemModule.getAllMonitorData()
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/monitor/all')
    })
  })

  describe('tran.js', () => {
    it('getTranList should call doGet', async () => {
      const tranModule = await import('../src/api/tran.js')
      tranModule.getTranList({ current: 1 })
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/tran/list')
    })

    it('getTranDetail should call doGet', async () => {
      const tranModule = await import('../src/api/tran.js')
      tranModule.getTranDetail(1)
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/tran/1')
    })

    it('getTranProducts should call doGet', async () => {
      const tranModule = await import('../src/api/tran.js')
      tranModule.getTranProducts(1)
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/tran/products/1')
    })

    it('createTran should call doPost', async () => {
      const tranModule = await import('../src/api/tran.js')
      tranModule.createTran({ name: 'test' })
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('post')
      expect(config.url).toBe('/api/tran/create')
    })

    it('updateTran should call doPut', async () => {
      const tranModule = await import('../src/api/tran.js')
      tranModule.updateTran({ id: 1, name: 'test' })
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('put')
      expect(config.url).toBe('/api/tran/update')
    })

    it('settleTran should call doPut', async () => {
      const tranModule = await import('../src/api/tran.js')
      tranModule.settleTran(1)
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('put')
      expect(config.url).toBe('/api/tran/settle/1')
    })

    it('settleTran with amount should call doPut with amount', async () => {
      const tranModule = await import('../src/api/tran.js')
      tranModule.settleTran(1, 1000)
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('put')
      expect(config.url).toBe('/api/tran/settle/1')
      expect(config.data).toEqual({ amount: 1000 })
    })

    it('approveTran should call doPut', async () => {
      const tranModule = await import('../src/api/tran.js')
      tranModule.approveTran(1, { status: 'approved' })
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('put')
      expect(config.url).toBe('/api/tran/approve/1')
    })

    it('getTranApprove should call doGet', async () => {
      const tranModule = await import('../src/api/tran.js')
      tranModule.getTranApprove(1)
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/tran/approve/info/1')
    })

    it('getTranStatus should call doGet', async () => {
      const tranModule = await import('../src/api/tran.js')
      tranModule.getTranStatus(1)
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/tran/status/1')
    })

    it('createInvoice should call doPost', async () => {
      const tranModule = await import('../src/api/tran.js')
      tranModule.createInvoice({ tranId: 1, amount: 1000 })
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('post')
      expect(config.url).toBe('/api/tran/invoice')
    })

    it('getTranInvoiceList should call doGet', async () => {
      const tranModule = await import('../src/api/tran.js')
      tranModule.getTranInvoiceList(1)
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('get')
      expect(config.url).toBe('/api/tran/invoice/1')
    })

    it('updateInvoiceStatus should call doPut', async () => {
      const tranModule = await import('../src/api/tran.js')
      tranModule.updateInvoiceStatus(1, 'paid')
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('put')
      expect(config.url).toBe('/api/tran/invoice/1/status')
    })

    it('deleteTran should call doDelete', async () => {
      const tranModule = await import('../src/api/tran.js')
      tranModule.deleteTran(1)
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('delete')
      expect(config.url).toBe('/api/tran/1')
    })

    it('batchDeleteTran should call doPost', async () => {
      const tranModule = await import('../src/api/tran.js')
      tranModule.batchDeleteTran([1, 2])
      expect(axios).toHaveBeenCalled()
      const config = axios.mock.calls[0][0]
      expect(config.method).toBe('post')
      expect(config.url).toBe('/api/tran/batch-delete')
    })
  })

  describe('duplicate getOwnerList', () => {
    it('getOwnerList should only exist in activity.js', async () => {
      const activityModule = await import('../src/api/activity.js')
      const clueModule = await import('../src/api/clue.js')
      expect(typeof activityModule.getOwnerList).toBe('function')
      expect(clueModule.getOwnerList).toBeUndefined()
    })
  })

  describe('API path consistency', () => {
    it('all API modules should export functions', async () => {
      const modules = [
        await import('../src/api/activity.js'),
        await import('../src/api/clue.js'),
        await import('../src/api/user.js'),
        await import('../src/api/dict.js'),
        await import('../src/api/product.js'),
        await import('../src/api/tran.js'),
        await import('../src/api/customer.js'),
        await import('../src/api/system.js'),
      ]

      modules.forEach(mod => {
        expect(Object.keys(mod).length).toBeGreaterThan(0)
        Object.values(mod).forEach(fn => {
          expect(typeof fn).toBe('function')
        })
      })
    })
  })
})
