import { vi } from 'vitest'

// Mock localStorage
const localStorageMock = (() => {
  let store = {}
  return {
    getItem: vi.fn((key) => store[key] || null),
    setItem: vi.fn((key, value) => { store[key] = value }),
    removeItem: vi.fn((key) => { delete store[key] }),
    clear: vi.fn(() => { store = {} }),
  }
})()
global.localStorage = localStorageMock

// Mock sessionStorage
const sessionStorageMock = (() => {
  let store = {}
  return {
    getItem: vi.fn((key) => store[key] || null),
    setItem: vi.fn((key, value) => { store[key] = value }),
    removeItem: vi.fn((key) => { delete store[key] }),
    clear: vi.fn(() => { store = {} }),
  }
})()
global.sessionStorage = sessionStorageMock

// Mock ElMessage - it's called as ElMessage({...}), so it must be a function
const mockElMessage = vi.fn()
mockElMessage.success = vi.fn()
mockElMessage.error = vi.fn()
mockElMessage.warning = vi.fn()
mockElMessage.info = vi.fn()

vi.mock('element-plus', () => ({
  ElMessage: mockElMessage,
  ElMessageBox: {
    confirm: vi.fn(() => Promise.resolve()),
  },
}))

// Mock axios - it's called as axios({...}), so default must be a callable function
vi.mock('axios', () => {
  const mockAxios = vi.fn(() => Promise.resolve({ data: {} }))
  mockAxios.create = vi.fn(() => mockAxios)
  mockAxios.defaults = { baseURL: '' }
  mockAxios.get = vi.fn(() => Promise.resolve({ data: {} }))
  mockAxios.post = vi.fn(() => Promise.resolve({ data: {} }))
  mockAxios.put = vi.fn(() => Promise.resolve({ data: {} }))
  mockAxios.delete = vi.fn(() => Promise.resolve({ data: {} }))
  mockAxios.interceptors = {
    request: { use: vi.fn() },
    response: { use: vi.fn() },
  }
  return { default: mockAxios }
})
