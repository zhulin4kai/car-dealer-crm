import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { defineComponent, h } from 'vue'

// Ensure component code that uses window.location.href does not actually
// navigate the test runner.
const originalLocation = window.location
beforeEach(() => {
  delete window.location
  window.location = Object.assign({}, originalLocation, { href: '' })
})
afterEach(() => {
  window.location = originalLocation
  vi.clearAllMocks()
  localStorage.clear()
  sessionStorage.clear()
})

async function importLoginView() {
  const mod = await import('../src/view/LoginView.vue')
  return mod.default
}

async function importDashboardView() {
  const mod = await import('../src/view/DashboardView.vue')
  return mod.default
}

const StubElFormItem = defineComponent({
  name: 'ElFormItem',
  props: ['label', 'prop'],
  setup(props, { slots }) {
    return () =>
      h('div', [
        props.label ? h('label', String(props.label)) : null,
        slots.default ? slots.default() : null,
      ])
  },
})

const StubElInput = defineComponent({
  name: 'ElInput',
  props: ['modelValue', 'type'],
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    return () =>
      h('input', {
        type: props.type || 'text',
        value: props.modelValue,
        onInput: (e) => emit('update:modelValue', e.target.value),
      })
  },
})

// Generic passthrough stub for any other el-* component.
function makeElPassthrough(name) {
  return defineComponent({
    name,
    props: ['width'],
    setup(_props, { slots }) {
      return () => h('div', slots.default ? slots.default() : null)
    },
  })
}

const StubElButton = defineComponent({
  name: 'ElButton',
  props: ['type'],
  emits: ['click'],
  setup(_props, { emit, slots }) {
    return () =>
      h(
        'button',
        { onClick: () => emit('click') },
        slots.default ? slots.default() : null
      )
  },
})

const StubElCheckbox = defineComponent({
  name: 'ElCheckbox',
  props: ['modelValue', 'label'],
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    return () =>
      h('input', {
        type: 'checkbox',
        checked: props.modelValue,
        onChange: (e) => emit('update:modelValue', e.target.checked),
      })
  },
})

function makeElFormStub(validateResult) {
  return defineComponent({
    name: 'ElForm',
    props: ['model', 'rules'],
    methods: {
      validate(cb) {
        cb(validateResult)
      },
      resetFields() {},
      clearValidate() {},
    },
    setup(_props, { slots }) {
      return () => h('div', slots.default ? slots.default() : null)
    },
  })
}

function mountLoginView(LoginView, validateResult) {
  if (validateResult === undefined) validateResult = true
  return mount(LoginView, {
    global: {
      stubs: {
        'el-form': makeElFormStub(validateResult),
        'el-form-item': StubElFormItem,
        'el-input': StubElInput,
        'el-button': StubElButton,
        'el-checkbox': StubElCheckbox,
        'el-aside': makeElPassthrough('ElAside'),
        'el-main': makeElPassthrough('ElMain'),
        'el-container': makeElPassthrough('ElContainer'),
      },
    },
  })
}

// Comprehensive stub set for DashboardView. DashboardView's template
// uses many el-* components and the el-icon dynamic component
// (`<component :is="icon">`) plus the named icons Fold/arrow-down.
// Stubbing them all keeps Vue from emitting "Failed to resolve
// component" warnings that would otherwise drown the test output.
const DASHBOARD_STUBS = {
  'el-container': makeElPassthrough('ElContainer'),
  'el-aside': makeElPassthrough('ElAside'),
  'el-header': makeElPassthrough('ElHeader'),
  'el-main': makeElPassthrough('ElMain'),
  'el-footer': makeElPassthrough('ElFooter'),
  'el-menu': makeElPassthrough('ElMenu'),
  'el-sub-menu': makeElPassthrough('ElSubMenu'),
  'el-menu-item': makeElPassthrough('ElMenuItem'),
  'el-icon': defineComponent({
    name: 'ElIcon',
    setup(_props, { slots }) {
      return () => h('i', slots.default ? slots.default() : null)
    },
  }),
  'el-dropdown': defineComponent({
    name: 'ElDropdown',
    setup(_props, { slots }) {
      return () => h('div', slots.default ? slots.default() : null)
    },
  }),
  'el-dropdown-menu': makeElPassthrough('ElDropdownMenu'),
  'el-dropdown-item': defineComponent({
    name: 'ElDropdownItem',
    emits: ['click'],
    setup(_props, { emit, slots }) {
      return () =>
        h(
          'div',
          {
            onClick: (e) => {
              e.stopPropagation()
              emit('click', e)
            },
          },
          slots.default ? slots.default() : null
        )
    },
  }),
  Fold: defineComponent({ name: 'Fold', setup: () => () => h('span') }),
  'arrow-down': defineComponent({ name: 'ArrowDown', setup: () => () => h('span') }),
  ArrowDown: defineComponent({ name: 'ArrowDown', setup: () => () => h('span') }),
}

describe('LoginView - behavior', () => {
  it('renders the account and password labels and the login button', async () => {
    const LoginView = await importLoginView()
    const wrapper = mountLoginView(LoginView)
    expect(wrapper.text()).toContain('账号')
    expect(wrapper.text()).toContain('密码')
    expect(wrapper.text()).toContain('登 录')
  })

  it('LoginView data should NOT contain dead variables (regression: name/age/arr/userList)', async () => {
    // shape-only: doc-allowed — verifies a regression: a prior version of
    // LoginView had stray fields (name, age, arr, userList) that polluted
    // the data() shape and could mask the real login form. Confirms only
    // `user` and `loginRules` are exposed.
    const LoginView = await importLoginView()
    const data = LoginView.data()
    expect(data).not.toHaveProperty('name')
    expect(data).not.toHaveProperty('age')
    expect(data).not.toHaveProperty('arr')
    expect(data).not.toHaveProperty('userList')
    expect(data).toHaveProperty('user')
    expect(data).toHaveProperty('loginRules')
  })

  it('LoginView.loginRules has the documented required rules for loginAct and loginPwd', async () => {
    const LoginView = await importLoginView()
    const rules = LoginView.data().loginRules
    expect(rules.loginAct).toEqual([
      { required: true, message: '请输入登录账号', trigger: 'blur' },
    ])
    expect(rules.loginPwd).toEqual([
      { required: true, message: '请输入登录密码', trigger: 'blur' },
      { min: 6, max: 16, message: '登录密码长度为6-16位', trigger: 'blur' },
    ])
  })

  it('LoginView.login() should call doPost("/api/login") with FormData containing loginAct, loginPwd, rememberMe', async () => {
    const LoginView = await importLoginView()
    axios.mockResolvedValue({ data: { code: 200, data: 'jwt-token' } })

    const wrapper = mountLoginView(LoginView, true)
    wrapper.vm.user.loginAct = 'admin'
    wrapper.vm.user.loginPwd = '123456'
    wrapper.vm.user.rememberMe = false

    await wrapper.vm.login()
    await flushPromises()

    expect(axios).toHaveBeenCalled()
    const callConfig = axios.mock.calls[0][0]
    expect(callConfig.method).toBe('post')
    expect(callConfig.url).toBe('/api/login')
    const formData = callConfig.data
    expect(formData).toBeInstanceOf(FormData)
    expect(formData.get('loginAct')).toBe('admin')
    expect(formData.get('loginPwd')).toBe('123456')
    expect(formData.get('rememberMe')).toBe('false')
  })

  it('LoginView stores the JWT in sessionStorage on successful login when rememberMe is false', async () => {
    const LoginView = await importLoginView()
    axios.mockResolvedValue({ data: { code: 200, data: 'jwt-token' } })

    const wrapper = mountLoginView(LoginView, true)
    wrapper.vm.user.loginAct = 'admin'
    wrapper.vm.user.loginPwd = '123456'
    wrapper.vm.user.rememberMe = false

    await wrapper.vm.login()
    await flushPromises()

    expect(sessionStorage.getItem('dlyk_token')).toBe('jwt-token')
    expect(localStorage.getItem('dlyk_token')).toBeNull()
  })

  it('LoginView stores the JWT in localStorage on successful login when rememberMe is true', async () => {
    const LoginView = await importLoginView()
    axios.mockResolvedValue({ data: { code: 200, data: 'jwt-token' } })

    const wrapper = mountLoginView(LoginView, true)
    wrapper.vm.user.loginAct = 'admin'
    wrapper.vm.user.loginPwd = '123456'
    wrapper.vm.user.rememberMe = true

    await wrapper.vm.login()
    await flushPromises()

    expect(localStorage.getItem('dlyk_token')).toBe('jwt-token')
    expect(sessionStorage.getItem('dlyk_token')).toBeNull()
  })

  it('LoginView shows ElMessage.error on login failure (non-200 code)', async () => {
    const LoginView = await importLoginView()
    axios.mockResolvedValue({ data: { code: 502, msg: '密码错误' } })

    const wrapper = mountLoginView(LoginView, true)
    wrapper.vm.user.loginAct = 'admin'
    wrapper.vm.user.loginPwd = 'wrong'
    wrapper.vm.user.rememberMe = false

    await wrapper.vm.login()
    await flushPromises()

    expect(ElMessage).toHaveBeenCalled()
    const cfg = ElMessage.mock.calls[0][0]
    expect(cfg.type).toBe('error')
  })

  it('LoginView does NOT submit when form validation fails', async () => {
    const LoginView = await importLoginView()
    const wrapper = mountLoginView(LoginView, false)
    await wrapper.vm.login()
    await flushPromises()
    expect(axios).not.toHaveBeenCalled()
  })

  it('LoginView.freeLogin navigates to /dashboard when /api/login/free returns 200 and a token is in localStorage', async () => {
    const LoginView = await importLoginView()
    localStorage.setItem('dlyk_token', 'existing-jwt')
    axios.mockResolvedValue({ data: { code: 200 } })

    mountLoginView(LoginView)
    await flushPromises()

    expect(axios).toHaveBeenCalled()
    const callConfig = axios.mock.calls[0][0]
    expect(callConfig.method).toBe('get')
    expect(callConfig.url).toBe('/api/login/free')
    expect(window.location.href).toBe('/dashboard')
  })

  it('LoginView.freeLogin does nothing when no token is in localStorage', async () => {
    const LoginView = await importLoginView()
    localStorage.clear()
    const wrapper = mountLoginView(LoginView)
    await flushPromises()
    vi.clearAllMocks()
    await wrapper.vm.freeLogin()
    await flushPromises()
    expect(axios).not.toHaveBeenCalled()
    expect(window.location.href).toBe('')
  })
})

describe('DashboardView - behavior', () => {
  const mountDashboard = async (DashboardView, routePath = '/dashboard') => {
    return mount(DashboardView, {
      global: {
        mocks: {
          $route: { path: routePath },
        },
        stubs: DASHBOARD_STUBS,
      },
    })
  }

  it('DashboardView data has the expected shape', async () => {
    // shape-only: doc-allowed — DashboardView's reactive state must expose
    // the four fields the template binds to. Real behavior (loadLoginUser,
    // logout, backToHome, navigation) is verified in the other DashboardView
    // tests below.
    const DashboardView = await importDashboardView()
    const data = DashboardView.data()
    expect(data).toHaveProperty('isCollapse')
    expect(data).toHaveProperty('user')
    expect(data).toHaveProperty('isRouterAlive')
    expect(data).toHaveProperty('currentRouterPath')
  })

  it('DashboardView.logout calls GET /api/logout (matches SecurityConfig + docs/integration.md)', async () => {
    // Contract pin: SecurityConfig wires /api/logout as a GET via
    // AntPathRequestMatcher("/api/logout", "GET"), and docs/integration.md
    // diagrams the same. DashboardView.logout must therefore send a GET.
    // CrossLayerConsistencyTest#logoutMethodMustBeConsistentAcrossLayers
    // pins the same contract across all three layers.
    const DashboardView = await importDashboardView()
    // The dashboard's mounted hook calls loadLoginUser first, which hits
    // /api/login/info. We give a valid response, then call logout() and
    // assert on the logout call.
    axios.mockResolvedValueOnce({ data: { code: 200, data: { id: 1, loginAct: 'admin', name: '管理员' } } })
    axios.mockResolvedValueOnce({ data: { code: 200 } })

    const wrapper = await mountDashboard(DashboardView)
    await flushPromises()
    vi.clearAllMocks()
    axios.mockResolvedValue({ data: { code: 200 } })

    await wrapper.vm.logout()
    await flushPromises()

    expect(axios).toHaveBeenCalled()
    const cfg = axios.mock.calls[0][0]
    expect(cfg.method).toBe('get')
    expect(cfg.url).toBe('/api/logout')
  })

  it('DashboardView.loadLoginUser GETs /api/login/info and stores the user data', async () => {
    const DashboardView = await importDashboardView()
    const user = { id: 1, loginAct: 'admin', name: '管理员' }
    axios.mockResolvedValue({ data: { code: 200, data: user } })

    const wrapper = await mountDashboard(DashboardView)
    await wrapper.vm.loadLoginUser()
    await flushPromises()

    expect(axios).toHaveBeenCalled()
    const cfg = axios.mock.calls[0][0]
    expect(cfg.method).toBe('get')
    expect(cfg.url).toBe('/api/login/info')
    expect(wrapper.vm.user.id).toBe(1)
  })
})
