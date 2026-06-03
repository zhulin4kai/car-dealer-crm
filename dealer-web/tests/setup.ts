import { vi } from 'vitest'

type StorageState = Record<string, string>

function createStorageMock() {
  let store: StorageState = {}

  return {
    getItem: vi.fn((key: string) => store[key] ?? null),
    setItem: vi.fn((key: string, value: string) => {
      store[key] = value
    }),
    removeItem: vi.fn((key: string) => {
      delete store[key]
    }),
    clear: vi.fn(() => {
      store = {}
    }),
  }
}

Object.defineProperty(globalThis, 'localStorage', {
  value: createStorageMock(),
  configurable: true,
})

Object.defineProperty(globalThis, 'sessionStorage', {
  value: createStorageMock(),
  configurable: true,
})

const mockElMessage = Object.assign(vi.fn(), {
  success: vi.fn(),
  error: vi.fn(),
  warning: vi.fn(),
  info: vi.fn(),
})

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: mockElMessage,
    ElMessageBox: {
      confirm: vi.fn(() => Promise.resolve('confirm')),
    },
  }
})

vi.mock('axios', () => {
  const mockAxios = Object.assign(
    vi.fn(() => Promise.resolve({ data: { code: 200, msg: 'OK', data: {} } })),
    {
      create: vi.fn(),
      defaults: { baseURL: '' },
      get: vi.fn(() => Promise.resolve({ data: { code: 200, msg: 'OK', data: {} } })),
      post: vi.fn(() => Promise.resolve({ data: { code: 200, msg: 'OK', data: {} } })),
      put: vi.fn(() => Promise.resolve({ data: { code: 200, msg: 'OK', data: {} } })),
      delete: vi.fn(() => Promise.resolve({ data: { code: 200, msg: 'OK', data: {} } })),
      interceptors: {
        request: { use: vi.fn() },
        response: { use: vi.fn() },
      },
      request: vi.fn(() => Promise.resolve({ data: { code: 200, msg: 'OK', data: {} } })),
    },
  )
  mockAxios.create.mockReturnValue(mockAxios)
  return { default: mockAxios }
})
