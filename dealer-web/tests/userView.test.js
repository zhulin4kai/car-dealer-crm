import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import axios from 'axios'
import { messageConfirm, messageTip } from '../src/util/util.js'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
}))

vi.mock('../src/util/util.js', () => ({
  messageConfirm: vi.fn(() => Promise.resolve()),
  messageTip: vi.fn(),
}))

async function importUserView() {
  const mod = await import('../src/view/UserView.vue')
  return mod.default
}

const StubElForm = defineComponent({
  name: 'ElForm',
  methods: {
    validate(cb) {
      cb(true)
    },
  },
  setup(_props, { slots }) {
    return () => h('form', slots.default ? slots.default() : null)
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
        onInput: (event) => emit('update:modelValue', event.target.value),
      })
  },
})

const StubElButton = defineComponent({
  name: 'ElButton',
  props: ['disabled'],
  emits: ['click'],
  setup(props, { emit, slots }) {
    return () =>
      h(
        'button',
        {
          disabled: props.disabled,
          onClick: () => {
            if (!props.disabled) emit('click')
          },
        },
        slots.default ? slots.default() : null
      )
  },
})

const StubElTable = defineComponent({
  name: 'ElTable',
  props: ['data'],
  setup(props) {
    return () =>
      h(
        'div',
        { class: 'user-table' },
        (props.data || []).map((user) =>
          h('div', { class: 'user-row', key: user.id }, [
            h('span', user.loginAct || ''),
            h('span', user.name || ''),
            h('span', user.phone || ''),
            h('span', user.email || ''),
          ])
        )
      )
  },
})

const StubElDialog = defineComponent({
  name: 'ElDialog',
  props: ['modelValue', 'title'],
  setup(props, { slots }) {
    return () =>
      h(
        'section',
        { style: { display: props.modelValue ? '' : 'none' } },
        [h('h2', props.title || ''), slots.default?.(), slots.footer?.()]
      )
  },
})

function passthrough(name) {
  return defineComponent({
    name,
    setup(_props, { slots }) {
      return () => h('div', slots.default ? slots.default() : null)
    },
  })
}

function successList(users, pageSize = 10, total = users.length) {
  return {
    data: {
      code: 200,
      data: {
        list: users,
        pageSize,
        total,
      },
    },
  }
}

async function mountUserView() {
  const UserView = await importUserView()
  return mount(UserView, {
    global: {
      directives: {
        hasPermission: {},
      },
      stubs: {
        'el-card': passthrough('ElCard'),
        'el-button': StubElButton,
        'el-table': StubElTable,
        'el-table-column': passthrough('ElTableColumn'),
        'el-pagination': passthrough('ElPagination'),
        'el-dialog': StubElDialog,
        'el-form': StubElForm,
        'el-form-item': passthrough('ElFormItem'),
        'el-input': StubElInput,
        'el-select': passthrough('ElSelect'),
        'el-option': passthrough('ElOption'),
      },
    },
  })
}

describe('UserView - behavior', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    axios.mockReset()
    axios.mockResolvedValue({ data: {} })
    messageConfirm.mockReset()
    messageConfirm.mockResolvedValue()
  })

  it('loads /api/users on mount and renders the returned user list', async () => {
    axios.mockResolvedValueOnce(
      successList([
        { id: 1, loginAct: 'admin', name: '管理员', phone: '13800138000', email: 'admin@example.com' },
        { id: 2, loginAct: 'zhangsan', name: '张三', phone: '13900139000', email: 'zhangsan@example.com' },
      ], 10, 2)
    )

    const wrapper = await mountUserView()
    await flushPromises()

    expect(axios).toHaveBeenCalledTimes(1)
    expect(axios.mock.calls[0][0]).toMatchObject({
      method: 'get',
      url: '/api/users',
      params: { current: 1 },
    })
    expect(wrapper.text()).toContain('管理员')
    expect(wrapper.text()).toContain('张三')
    expect(wrapper.vm.total).toBe(2)
  })

  it('submits a new user as FormData, shows success, closes the dialog, and refreshes the list', async () => {
    axios
      .mockResolvedValueOnce(successList([]))
      .mockResolvedValueOnce({ data: { code: 200 } })
      .mockResolvedValueOnce(successList([{ id: 9, loginAct: 'newuser', name: '新用户' }], 10, 1))

    const wrapper = await mountUserView()
    await flushPromises()

    wrapper.vm.add()
    wrapper.vm.userQuery = {
      loginAct: 'newuser',
      loginPwd: '123456',
      name: '新用户',
      phone: '13800138001',
      email: 'newuser@example.com',
      accountNoExpired: 1,
      credentialsNoExpired: 1,
      accountNoLocked: 1,
      accountEnabled: 1,
    }

    await wrapper.vm.userSubmit()
    await flushPromises()
    await flushPromises()

    const submitConfig = axios.mock.calls[1][0]
    expect(submitConfig.method).toBe('post')
    expect(submitConfig.url).toBe('/api/user')
    expect(submitConfig.data).toBeInstanceOf(FormData)
    expect(submitConfig.data.get('loginAct')).toBe('newuser')
    expect(submitConfig.data.get('loginPwd')).toBe('123456')
    expect(messageTip).toHaveBeenCalledWith('提交成功', 'success')
    expect(wrapper.vm.userDialogVisible).toBe(false)
    expect(axios.mock.calls[2][0]).toMatchObject({
      method: 'get',
      url: '/api/users',
      params: { current: 1 },
    })
    expect(wrapper.text()).toContain('新用户')
  })

  it('deletes a user only after confirmation and refreshes the current list', async () => {
    messageConfirm.mockResolvedValueOnce()
    axios
      .mockResolvedValueOnce(successList([{ id: 7, loginAct: 'olduser', name: '待删除' }], 10, 1))
      .mockResolvedValueOnce({ data: { code: 200 } })
      .mockResolvedValueOnce(successList([], 10, 0))

    const wrapper = await mountUserView()
    await flushPromises()

    await wrapper.vm.del(7)
    await flushPromises()
    await flushPromises()

    expect(messageConfirm).toHaveBeenCalledWith('您确定要删除该数据吗？')
    expect(axios.mock.calls[1][0]).toMatchObject({
      method: 'delete',
      url: '/api/user/7',
      data: {},
    })
    expect(messageTip).toHaveBeenCalledWith('删除成功', 'success')
    expect(axios.mock.calls[2][0]).toMatchObject({
      method: 'get',
      url: '/api/users',
      params: { current: 1 },
    })
    expect(wrapper.text()).not.toContain('待删除')
  })
})
