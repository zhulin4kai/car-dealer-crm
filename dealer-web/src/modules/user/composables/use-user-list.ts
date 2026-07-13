import { computed, onBeforeUnmount, ref } from 'vue'

import { fetchUserPage } from '@/modules/user/api/user-api'
import type { UserListQuery, UserListSummary } from '@/modules/user/model/user.types'

export interface UserFilterValues {
  keyword: string
  organizationUnitId: string
  positionId: string
  managerEmployeeId: string
  roleId: string
  employmentStatus: string
  accountStatus: string
  lockStatus: string
}

const DEFAULT_FILTERS: UserFilterValues = {
  keyword: '',
  organizationUnitId: '',
  positionId: '',
  managerEmployeeId: '',
  roleId: '',
  employmentStatus: '',
  accountStatus: '',
  lockStatus: '',
}

export function useUserList() {
  const filters = ref<UserFilterValues>({ ...DEFAULT_FILTERS })
  const rows = ref<UserListSummary[]>([])
  const page = ref(1)
  const pageSize = ref(10)
  const total = ref(0)
  const sortBy = ref('employeeNo')
  const sortDirection = ref<'asc' | 'desc'>('asc')
  const loading = ref(false)
  const errorMessage = ref('')
  let requestId = 0
  let abortController: AbortController | null = null

  const query = computed<UserListQuery>(() => ({
    page: page.value,
    size: pageSize.value,
    sortBy: sortBy.value,
    sortDirection: sortDirection.value,
    ...(filters.value.keyword.trim() ? { keyword: filters.value.keyword.trim() } : {}),
    ...(filters.value.organizationUnitId
      ? { organizationUnitId: filters.value.organizationUnitId }
      : {}),
    ...(filters.value.positionId ? { positionId: filters.value.positionId } : {}),
    ...(filters.value.managerEmployeeId
      ? { managerEmployeeId: filters.value.managerEmployeeId }
      : {}),
    ...(filters.value.roleId ? { roleId: filters.value.roleId } : {}),
    ...(filters.value.employmentStatus
      ? { employmentStatus: filters.value.employmentStatus }
      : {}),
    ...(filters.value.accountStatus ? { accountStatus: filters.value.accountStatus } : {}),
    ...(filters.value.lockStatus ? { lockStatus: filters.value.lockStatus } : {}),
  }))

  async function load(): Promise<void> {
    const currentRequestId = ++requestId
    abortController?.abort()
    const controller = new AbortController()
    abortController = controller
    loading.value = true
    errorMessage.value = ''
    try {
      const result = await fetchUserPage(query.value, controller.signal)
      if (currentRequestId !== requestId || controller.signal.aborted) return
      rows.value = result.list
      total.value = result.total
      pageSize.value = result.pageSize || pageSize.value
      page.value = result.pageNum || page.value
    } catch {
      if (currentRequestId !== requestId || controller.signal.aborted) return
      rows.value = []
      total.value = 0
      errorMessage.value = '加载用户列表失败'
    } finally {
      if (currentRequestId === requestId) {
        loading.value = false
        abortController = null
      }
    }
  }

  function search(nextFilters: UserFilterValues): void {
    filters.value = { ...nextFilters }
    page.value = 1
    void load()
  }

  function reset(): void {
    filters.value = { ...DEFAULT_FILTERS }
    page.value = 1
    sortBy.value = 'employeeNo'
    sortDirection.value = 'asc'
    void load()
  }

  function changePage(nextPage: number): void {
    page.value = nextPage
    void load()
  }

  function changeSort(key: string): void {
    if (sortBy.value === key) {
      sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc'
    } else {
      sortBy.value = key
      sortDirection.value = 'asc'
    }
    page.value = 1
    void load()
  }

  onBeforeUnmount(() => abortController?.abort())

  return {
    filters,
    rows,
    page,
    pageSize,
    total,
    sortBy,
    sortDirection,
    loading,
    errorMessage,
    load,
    search,
    reset,
    changePage,
    changeSort,
  }
}
