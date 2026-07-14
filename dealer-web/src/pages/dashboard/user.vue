<template>
  <div class="crm-data-page space-y-4">
    <section class="crm-panel p-4">
      <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 class="text-xl font-semibold">用户管理工作台</h1>
          <p class="text-sm text-muted-foreground">
            按人员、组织、授权与账号状态查询；个人资料请在个人中心维护。
          </p>
        </div>
        <Button v-has-permission="PERMISSIONS.user.add" @click="openCreateDialog"
          ><Plus class="h-4 w-4" />新增用户</Button
        >
      </div>
      <div
        v-if="filterError"
        class="mb-3 flex items-center justify-between rounded border border-destructive/40 p-3 text-sm text-destructive"
      >
        <span>{{ filterError }}</span
        ><Button variant="outline" size="sm" @click="loadFilters">重试</Button>
      </div>
      <UserFilterBar
        :model-value="filters"
        :options="filterOptions"
        :loading="loading || filterLoading"
        @search="search"
        @reset="reset"
      />
    </section>

    <section
      v-if="createdResult"
      class="crm-panel flex flex-wrap items-center justify-between gap-3 p-4"
      aria-live="polite"
    >
      <div class="space-y-1 text-sm">
        <p class="font-medium">用户 {{ createdResult.user.name }} 已创建</p>
        <p>邀请凭证已排队，等待安全通知服务投递</p>
      </div>
      <Button
        v-if="permissionStore.hasPermission(PERMISSIONS.user.view)"
        variant="outline"
        @click="openCreatedUser"
      >查看用户详情</Button>
      <span v-else class="text-xs text-muted-foreground">当前账号没有用户详情读取权限</span>
    </section>

    <section class="crm-panel">
      <div
        v-if="selectedRows.length"
        class="flex flex-wrap items-center justify-between gap-3 border-b p-4"
      >
        <p class="text-sm">已选择 {{ selectedRows.length }} 人（单次最多 50 人）</p>
        <div class="flex flex-wrap gap-2">
          <Button
            v-has-permission="PERMISSIONS.user.role"
            variant="outline"
            :disabled="batchLoading || selectedRows.length > 50"
            @click="openBatchDialog('roles')"
          >
            批量调整角色
          </Button>
          <Button
            v-has-permission="PERMISSIONS.user.permission"
            variant="outline"
            :disabled="batchLoading || selectedRows.length > 50"
            @click="openBatchDialog('permissions')"
          >
            批量调整个人权限
          </Button>
        </div>
      </div>
      <div v-if="errorMessage" class="space-y-3 py-16 text-center">
        <p class="text-destructive">{{ errorMessage }}</p>
        <Button variant="outline" @click="load">重新加载</Button>
      </div>
      <div v-else class="crm-table-shell">
        <Table class="min-w-[1280px]">
          <TableHeader class="bg-[var(--crm-bg-muted)]"
            ><TableRow>
              <TableHead class="w-[48px]">
                <Checkbox
                  aria-label="选择本页可授权用户"
                  :checked="allSelectableSelected"
                  :disabled="!selectableRows.length"
                  @update:checked="toggleAllRows($event === true)"
                />
              </TableHead>
              <TableHead
                sortable
                sort-key="employeeNo"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="changeSort"
                >员工编号</TableHead
              >
              <TableHead
                sortable
                sort-key="name"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="changeSort"
                >姓名</TableHead
              >
              <TableHead
                sortable
                sort-key="loginAct"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="changeSort"
                >账号</TableHead
              >
              <TableHead>组织 / 岗位</TableHead><TableHead>直属管理者</TableHead
              ><TableHead>角色</TableHead>
              <TableHead
                sortable
                sort-key="employmentStatus"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="changeSort"
                >任职状态</TableHead
              >
              <TableHead
                sortable
                sort-key="accountStatus"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="changeSort"
                >账号状态</TableHead
              >
              <TableHead
                sortable
                sort-key="lockStatus"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="changeSort"
                >锁定状态</TableHead
              >
              <TableHead
                sortable
                sort-key="lastLoginTime"
                :sort-by="sortBy"
                :sort-direction="sortDirection"
                @sort="changeSort"
                >最近登录</TableHead
              ><TableHead>操作</TableHead>
            </TableRow></TableHeader
          >
          <TableBody>
            <TableRow v-if="loading"
              ><TableCell colspan="12" class="h-32 text-center text-muted-foreground"
                >加载用户列表...</TableCell
              ></TableRow
            >
            <TableRow v-else-if="rows.length === 0"
              ><TableCell colspan="12" class="h-32 text-center text-muted-foreground"
                >没有符合条件的用户</TableCell
              ></TableRow
            >
            <TableRow v-for="row in rows" v-else :key="row.id">
              <TableCell>
                <Checkbox
                  :aria-label="`选择用户${row.name}`"
                  :checked="selectedIds.includes(row.id)"
                  :disabled="!can(row, MANAGED_USER_ACTION.AUTHORIZATION_VIEW)"
                  :title="reason(row, MANAGED_USER_ACTION.AUTHORIZATION_VIEW)"
                  @update:checked="toggleRow(row, $event === true)"
                />
              </TableCell>
              <TableCell>{{ row.employeeNo || '--' }}</TableCell
              ><TableCell class="font-medium">{{ row.name }}</TableCell
              ><TableCell class="font-mono text-xs">{{ row.loginAct }}</TableCell>
              <TableCell
                ><div>{{ row.organizationName || '未设置' }}</div>
                <div class="text-xs text-muted-foreground">
                  {{ row.positionName || '未设置岗位' }}
                </div></TableCell
              >
              <TableCell>{{ row.managerName || '未设置' }}</TableCell
              ><TableCell>{{
                row.roleNames.length ? row.roleNames.join('、') : '未分配'
              }}</TableCell>
              <TableCell>{{ row.employmentStatus }}</TableCell
              ><TableCell>{{ row.accountStatus }}</TableCell
              ><TableCell>{{ row.lockStatus }}</TableCell
              ><TableCell>{{ formatDateTime(row.lastLoginTime) }}</TableCell>
              <TableCell
                ><Button
                  size="sm"
                  variant="ghost"
                  :disabled="!can(row, MANAGED_USER_ACTION.VIEW)"
                  :title="reason(row, MANAGED_USER_ACTION.VIEW)"
                  @click="openDetail(row)"
                  >详情</Button
                ></TableCell
              >
            </TableRow>
          </TableBody>
        </Table>
      </div>
      <div class="crm-table-footer">
        <DataTablePagination
          :page="page"
          :page-size="pageSize"
          :total="total"
          @change="changePage"
        />
      </div>
    </section>

    <UserFormDialog
      v-model:open="createDialogOpen"
      mode="create"
      :options="createOptions"
      :submitting="createSubmitting"
      :role-options-loading="assignableRoleLoading"
      :role-options-error="assignableRoleError"
      @organization-change="loadAssignableRoles"
      @create="submitCreate"
    />
    <BatchAuthorizationDialog
      v-model:open="batchDialogOpen"
      :mode="batchMode"
      :details="batchDetails"
      :submitting="batchSubmitting"
      @save-roles="submitBatchRoles"
      @save-permissions="submitBatchPermissions"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@lucide/vue'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  batchUpdateUserPermissions,
  batchUpdateUserRoleAssignments,
  fetchUserAuthorizationDetail,
} from '@/modules/access/api/user-authorization-api'
import BatchAuthorizationDialog from '@/modules/access/components/BatchAuthorizationDialog.vue'
import {
  getUserAuthorizationErrorMessage,
  isAccessVersionConflict,
} from '@/modules/access/model/access-error'
import {
  USER_AUTHORIZATION_ACTION,
  type BatchUpdateUserPermissionsRequest,
  type BatchUpdateUserRolesRequest,
  type UserAuthorizationDetail,
} from '@/modules/access/model/user-permission.types'
import UserFilterBar from '@/modules/user/components/UserFilterBar.vue'
import UserFormDialog from '@/modules/user/components/UserFormDialog.vue'
import { createManagedUser, fetchUserFilterOptions } from '@/modules/user/api/user-api'
import { useUserList } from '@/modules/user/composables/use-user-list'
import {
  MANAGED_USER_ACTION,
  type CreateManagedUserRequest,
  type CreateManagedUserResult,
  type ManagedUserAction,
  type UserFilterOptions,
  type UserListSummary,
} from '@/modules/user/model/user.types'
import DataTablePagination from '@/shared/ui/DataTablePagination.vue'
import { PERMISSIONS } from '@/shared/constants/permissions'
import type { EntityId } from '@/shared/types/id'
import { formatDateTime } from '@/shared/utils/display-format'
import { messageTip } from '@/shared/utils/feedback'
import { usePermissionStore } from '@/stores/permission.store'

const EMPTY_OPTIONS: UserFilterOptions = {
  organizations: [],
  positions: [],
  managers: [],
  roles: [],
  assignableRoles: [],
  employmentStatuses: [],
  accountStatuses: [],
  lockStatuses: [],
  bootstrapRequired: false,
  bootstrapAllowed: false,
  bootstrapRootOrganizationId: null,
  bootstrapRootOrganizationVersion: null,
}
const router = useRouter()
const permissionStore = usePermissionStore()
const {
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
} = useUserList()
const filterOptions = ref<UserFilterOptions>(EMPTY_OPTIONS)
const createOptions = ref<UserFilterOptions>(EMPTY_OPTIONS)
const filterLoading = ref(false)
const filterError = ref('')
const createDialogOpen = ref(false)
const createSubmitting = ref(false)
const createdResult = ref<CreateManagedUserResult | null>(null)
const selectedIds = ref<EntityId[]>([])
const batchDialogOpen = ref(false)
const batchMode = ref<'roles' | 'permissions'>('roles')
const batchDetails = ref<UserAuthorizationDetail[]>([])
const batchLoading = ref(false)
const batchSubmitting = ref(false)
let filterController: AbortController | null = null
let assignableRoleController: AbortController | null = null
let batchController: AbortController | null = null
let assignableRoleRequestId = 0
const assignableRoleLoading = ref(false)
const assignableRoleError = ref('')
const selectableRows = computed(() =>
  rows.value.filter((row) => can(row, MANAGED_USER_ACTION.AUTHORIZATION_VIEW)),
)
const selectedRows = computed(() => rows.value.filter((row) => selectedIds.value.includes(row.id)))
const allSelectableSelected = computed(
  () =>
    selectableRows.value.length > 0 &&
    selectableRows.value.every((row) => selectedIds.value.includes(row.id)),
)

function can(row: UserListSummary, action: ManagedUserAction): boolean {
  return row.allowedActions.includes(action)
}
function reason(row: UserListSummary, action: ManagedUserAction): string {
  return can(row, action) ? '' : (row.unavailableReasons[action] ?? '服务端未允许此操作')
}
function openDetail(row: UserListSummary): void {
  if (can(row, MANAGED_USER_ACTION.VIEW))
    void router.push({ name: 'user-detail', params: { id: String(row.id) } })
}
function openCreatedUser(): void {
  if (createdResult.value && permissionStore.hasPermission(PERMISSIONS.user.view))
    void router.push({ name: 'user-detail', params: { id: String(createdResult.value.user.id) } })
}
function toggleRow(row: UserListSummary, checked: boolean): void {
  if (!can(row, MANAGED_USER_ACTION.AUTHORIZATION_VIEW)) return
  if (checked && selectedIds.value.length >= 50) {
    messageTip('单次最多选择 50 名用户', 'warning')
    return
  }
  selectedIds.value = checked
    ? [...selectedIds.value, row.id]
    : selectedIds.value.filter((id) => String(id) !== String(row.id))
}
function toggleAllRows(checked: boolean): void {
  if (!checked) {
    selectedIds.value = []
    return
  }
  selectedIds.value = selectableRows.value.slice(0, 50).map((row) => row.id)
  if (selectableRows.value.length > 50)
    messageTip('本页可授权用户超过 50 人，已选择前 50 人', 'warning')
}
function cloneOptions(options: UserFilterOptions): UserFilterOptions {
  return {
    ...options,
    organizations: [...options.organizations],
    positions: [...options.positions],
    managers: [...options.managers],
    roles: [...options.roles],
    assignableRoles: [...options.assignableRoles],
    employmentStatuses: [...options.employmentStatuses],
    accountStatuses: [...options.accountStatuses],
    lockStatuses: [...options.lockStatuses],
  }
}
function openCreateDialog(): void {
  assignableRoleController?.abort()
  assignableRoleRequestId += 1
  assignableRoleLoading.value = false
  assignableRoleError.value = ''
  createOptions.value = cloneOptions(filterOptions.value)
  createDialogOpen.value = true
}

async function loadFilters(): Promise<void> {
  filterController?.abort()
  const controller = new AbortController()
  filterController = controller
  filterLoading.value = true
  filterError.value = ''
  try {
    const result = await fetchUserFilterOptions(undefined, controller.signal)
    if (controller.signal.aborted || filterController !== controller) return
    filterOptions.value = result
    if (!createDialogOpen.value) createOptions.value = cloneOptions(result)
  } catch {
    if (!controller.signal.aborted) {
      filterOptions.value = EMPTY_OPTIONS
      if (!createDialogOpen.value) createOptions.value = EMPTY_OPTIONS
      filterError.value = '加载筛选候选项失败'
    }
  } finally {
    if (filterController === controller) {
      filterLoading.value = false
      filterController = null
    }
  }
}

async function loadAssignableRoles(organizationUnitId: string | null): Promise<void> {
  const currentRequestId = ++assignableRoleRequestId
  assignableRoleController?.abort()
  assignableRoleController = null
  assignableRoleError.value = ''
  createOptions.value = { ...cloneOptions(filterOptions.value), managers: [], assignableRoles: [] }
  if (!organizationUnitId) {
    assignableRoleLoading.value = false
    return
  }
  const controller = new AbortController()
  assignableRoleController = controller
  assignableRoleLoading.value = true
  try {
    const result = await fetchUserFilterOptions(organizationUnitId, controller.signal)
    if (controller.signal.aborted || currentRequestId !== assignableRoleRequestId) return
    createOptions.value = {
      ...cloneOptions(filterOptions.value),
      managers: [...result.managers],
      assignableRoles: [...result.assignableRoles],
    }
  } catch {
    if (controller.signal.aborted || currentRequestId !== assignableRoleRequestId) return
    createOptions.value = { ...cloneOptions(filterOptions.value), managers: [], assignableRoles: [] }
    assignableRoleError.value = '加载当前组织的直属管理者和可委派角色失败，请重试选择组织'
  } finally {
    if (currentRequestId === assignableRoleRequestId) {
      assignableRoleLoading.value = false
      assignableRoleController = null
    }
  }
}

async function submitCreate(request: CreateManagedUserRequest): Promise<void> {
  if (createSubmitting.value) return
  createSubmitting.value = true
  try {
    const result = await createManagedUser(request)
    createdResult.value = result
    createDialogOpen.value = false
    messageTip('用户已创建，邀请凭证已排队等待投递', 'success')
    await load()
  } catch {
    messageTip('创建用户失败，请根据服务端提示检查资料', 'error')
  } finally {
    createSubmitting.value = false
  }
}

async function openBatchDialog(mode: 'roles' | 'permissions'): Promise<void> {
  if (!selectedRows.value.length || batchLoading.value) return
  batchController?.abort()
  const controller = new AbortController()
  batchController = controller
  batchLoading.value = true
  try {
    const details = await Promise.all(
      selectedRows.value.map((row) => fetchUserAuthorizationDetail(row.id, controller.signal)),
    )
    if (controller.signal.aborted || batchController !== controller) return
    const requiredAction =
      mode === 'roles'
        ? USER_AUTHORIZATION_ACTION.ROLE_UPDATE
        : USER_AUTHORIZATION_ACTION.PERMISSION_UPDATE
    const denied = details.find((detail) => !detail.allowedActions.includes(requiredAction))
    if (denied) {
      messageTip(
        `${denied.user.name}：${denied.unavailableReasons[requiredAction] ?? '当前不允许调整授权'}`,
        'warning',
      )
      return
    }
    batchMode.value = mode
    batchDetails.value = details
    batchDialogOpen.value = true
  } catch (error) {
    if (!controller.signal.aborted) {
      messageTip(getUserAuthorizationErrorMessage(error, '加载批量授权候选失败'), 'error')
    }
  } finally {
    if (batchController === controller) {
      batchController = null
      batchLoading.value = false
    }
  }
}

const submitBatchRoles = (request: BatchUpdateUserRolesRequest) =>
  submitBatch(() => batchUpdateUserRoleAssignments(request), '角色')

const submitBatchPermissions = (request: BatchUpdateUserPermissionsRequest) =>
  submitBatch(() => batchUpdateUserPermissions(request), '个人权限')

async function submitBatch(
  execute: () => ReturnType<typeof batchUpdateUserPermissions>,
  subject: string,
) {
  if (batchSubmitting.value) return
  batchSubmitting.value = true
  try {
    const result = await execute()
    messageTip(`批量${subject}调整完成，实际变更 ${result.changedTargetCount} 人`, 'success')
    await finishBatch()
  } catch (error) {
    messageTip(getUserAuthorizationErrorMessage(error, `批量${subject}调整失败`), 'error')
    await refreshBatchOnConflict(error)
  } finally {
    batchSubmitting.value = false
  }
}

async function finishBatch(): Promise<void> {
  batchDialogOpen.value = false
  batchDetails.value = []
  selectedIds.value = []
  await load()
}

async function refreshBatchOnConflict(error: unknown): Promise<void> {
  if (!isAccessVersionConflict(error)) return
  try {
    const latestDetails = await Promise.all(
      batchDetails.value.map((detail) => fetchUserAuthorizationDetail(detail.user.id)),
    )
    batchDetails.value = latestDetails
    await load()
  } catch {
    batchDialogOpen.value = false
    batchDetails.value = []
    messageTip('授权事实刷新失败，请重新选择用户后再试', 'error')
    await load()
  }
}

watch(rows, (nextRows) => {
  const visibleIds = new Set(nextRows.map((row) => String(row.id)))
  selectedIds.value = selectedIds.value.filter((id) => visibleIds.has(String(id)))
})

onMounted(() => {
  void Promise.all([loadFilters(), load()])
})
onBeforeUnmount(() => {
  filterController?.abort()
  assignableRoleController?.abort()
  batchController?.abort()
})
</script>
