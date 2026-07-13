<template>
  <div class="crm-data-page">
    <div class="grid min-h-[650px] gap-4 lg:grid-cols-[320px_minmax(0,1fr)]">
      <section class="crm-panel flex min-h-0 flex-col">
        <div class="flex items-center justify-between border-b p-4">
          <div>
            <h2 class="font-semibold">组织架构</h2>
            <p class="text-xs text-muted-foreground">公司、门店、部门与团队</p>
          </div>
          <Button
            v-has-permission="PERMISSIONS.organization.add"
            size="sm"
            :disabled="organizationTree.length > 0"
            :title="organizationTree.length > 0 ? '系统只允许一个根组织' : '新增根组织'"
            @click="openCreateRoot"
          >
            <Plus class="h-4 w-4" />新增
          </Button>
        </div>
        <div v-if="foundationLoading" class="py-16 text-center text-muted-foreground">
          加载中...
        </div>
        <OrganizationTree
          v-else
          class="min-h-0 flex-1 p-3"
          :nodes="organizationTree"
          :selected-id="selectedUnit?.id"
          @select="selectOrganization"
        />
      </section>

      <section class="crm-panel min-w-0">
        <div class="flex flex-wrap items-start justify-between gap-3 border-b p-4">
          <div v-if="selectedUnit">
            <div class="flex items-center gap-2">
              <h2 class="text-lg font-semibold">{{ selectedUnit.name }}</h2>
              <StatusBadge
                :label="selectedUnit.enabled ? '启用' : '停用'"
                :tone="selectedUnit.enabled ? 'success' : 'muted'"
              />
            </div>
            <p class="mt-1 text-sm text-muted-foreground">
              {{ ORGANIZATION_UNIT_TYPE_LABEL[selectedUnit.type] }} · {{ selectedUnit.code }} ·
              负责人：{{ selectedUnit.leaderEmployeeName || '未设置' }}
            </p>
          </div>
          <div v-else>
            <h2 class="text-lg font-semibold">组织管理</h2>
            <p class="text-sm text-muted-foreground">选择左侧组织节点查看员工。</p>
          </div>
          <div class="flex flex-wrap gap-2">
            <Button
              v-has-permission="PERMISSIONS.position.list"
              variant="outline"
              @click="positionDialogOpen = true"
            >
              <BriefcaseBusiness class="h-4 w-4" />岗位目录
            </Button>
            <Button
              v-if="selectedUnit"
              v-has-permission="PERMISSIONS.organization.add"
              variant="outline"
              :disabled="selectedUnit.type === 'TEAM'"
              :title="selectedUnit.type === 'TEAM' ? '团队不能再新增下级组织' : '新增下级组织'"
              @click="openCreateChild"
            >
              <FolderPlus class="h-4 w-4" />新增下级
            </Button>
            <Button
              v-if="selectedUnit"
              v-has-permission="PERMISSIONS.organization.edit"
              variant="outline"
              @click="openEditOrganization"
            >
              <Pencil class="h-4 w-4" />编辑
            </Button>
            <Button
              v-if="selectedUnit"
              v-has-permission="PERMISSIONS.organization.status"
              :variant="selectedUnit.enabled ? 'destructive' : 'outline'"
              @click="openOrganizationStatus"
            >
              {{ selectedUnit.enabled ? '停用' : '启用' }}
            </Button>
          </div>
        </div>

        <div v-if="!selectedUnit" class="py-24 text-center text-muted-foreground">
          请先选择一个组织节点
        </div>
        <div v-else class="crm-table-shell">
          <Table class="min-w-[920px]">
            <TableHeader>
              <TableRow>
                <TableHead>员工编号</TableHead>
                <TableHead>姓名</TableHead>
                <TableHead>岗位</TableHead>
                <TableHead>直属管理者</TableHead>
                <TableHead>任职状态</TableHead>
                <TableHead class="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-if="employeeLoading">
                <TableCell colspan="6" class="h-32 text-center text-muted-foreground"
                  >加载员工...</TableCell
                >
              </TableRow>
              <TableRow v-else-if="!employees.length">
                <TableCell colspan="6" class="h-32 text-center text-muted-foreground"
                  >当前组织暂无员工</TableCell
                >
              </TableRow>
              <template v-else>
                <TableRow v-for="employee in employees" :key="employee.id">
                  <TableCell class="font-mono text-xs">{{ employee.employeeNo }}</TableCell>
                  <TableCell class="font-medium">{{ employee.name }}</TableCell>
                  <TableCell>{{ employee.positionName || '未设置' }}</TableCell>
                  <TableCell>{{ employee.managerEmployeeName || '未设置' }}</TableCell>
                  <TableCell>
                    <StatusBadge
                      :label="EMPLOYEE_STATUS_LABEL[employee.employmentStatus]"
                      :tone="employee.employmentStatus === 'ACTIVE' ? 'success' : 'muted'"
                    />
                  </TableCell>
                  <TableCell>
                    <div class="flex justify-end gap-2">
                      <Button
                        v-has-permission="PERMISSIONS.employee.assignment"
                        size="sm"
                        variant="outline"
                        :disabled="
                          !canEmployeeAction(
                            employee,
                            EMPLOYEE_ORGANIZATION_ACTION.ASSIGNMENT_UPDATE,
                          )
                        "
                        :title="
                          employeeActionReason(
                            employee,
                            EMPLOYEE_ORGANIZATION_ACTION.ASSIGNMENT_UPDATE,
                          )
                        "
                        @click="openEmployeeDialog(employee, 'assignment')"
                      >
                        调整任职
                      </Button>
                      <Button
                        v-has-permission="PERMISSIONS.employee.reporting"
                        size="sm"
                        variant="outline"
                        :disabled="
                          !canEmployeeAction(
                            employee,
                            EMPLOYEE_ORGANIZATION_ACTION.REPORTING_UPDATE,
                          )
                        "
                        :title="
                          employeeActionReason(
                            employee,
                            EMPLOYEE_ORGANIZATION_ACTION.REPORTING_UPDATE,
                          )
                        "
                        @click="openEmployeeDialog(employee, 'reporting')"
                      >
                        调整汇报
                      </Button>
                      <Button
                        v-has-permission="PERMISSIONS.employee.reporting"
                        size="sm"
                        variant="outline"
                        :disabled="!canEmployeeAction(employee, EMPLOYEE_ORGANIZATION_ACTION.REPORTING_UPDATE)"
                        :title="employeeActionReason(employee, EMPLOYEE_ORGANIZATION_ACTION.REPORTING_UPDATE)"
                        @click="openActingDialog(employee)"
                      >
                        代理主管
                      </Button>
                      <Button
                        v-has-permission="PERMISSIONS.organization.view"
                        size="sm"
                        variant="ghost"
                        :disabled="
                          !canEmployeeAction(employee, EMPLOYEE_ORGANIZATION_ACTION.HISTORY_VIEW)
                        "
                        :title="
                          employeeActionReason(employee, EMPLOYEE_ORGANIZATION_ACTION.HISTORY_VIEW)
                        "
                        @click="openEmployeeDialog(employee, 'history')"
                      >
                        查看历史
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              </template>
            </TableBody>
          </Table>
        </div>
      </section>
    </div>

    <OrganizationFormDialog
      v-model:open="organizationFormOpen"
      :unit="editingUnit"
      :parent="creatingParent"
      :leader-candidates="organizationLeaderCandidates"
      :parent-candidates="organizationParentCandidates"
      :candidates-loading="organizationCandidatesLoading"
      :candidate-error="organizationCandidateError"
      :submitting="organizationSubmitting"
      @retry-candidates="loadOrganizationCandidates"
      @submit="submitOrganization"
    />

    <PositionManagement
      v-model:open="positionDialogOpen"
      :positions="positions"
      :loading="foundationLoading"
      :submitting="positionSubmitting"
      :can-create="permissionStore.hasPermission(PERMISSIONS.position.add)"
      :can-edit="permissionStore.hasPermission(PERMISSIONS.position.edit)"
      :can-change-status="permissionStore.hasPermission(PERMISSIONS.position.status)"
      @submit="submitPosition"
      @change-status="changePositionStatus"
    />

    <EmployeeAssignmentDialog
      :open="assignmentDialogOpen"
      :mode="assignmentDialogMode"
      :membership="employeeMembership"
      :organization-units="organizationTree"
      :positions="positions"
      :manager-candidates="managerCandidates"
      :history="employeeHistory"
      :load-error="assignmentLoadError"
      :loading="assignmentLoading"
      :submitting="assignmentSubmitting"
      @update:open="handleAssignmentDialogOpenChange"
      @retry="retryEmployeeDialog"
      @organization-change="loadAssignmentManagerCandidates"
      @submit="submitEmployeeAssignment"
    />

    <ActingReportingDialog
      :open="actingDialogOpen"
      :employee="actingEmployee"
      :collection="actingCollection"
      :candidates="actingCandidates"
      :loading="actingLoading"
      :load-error="actingLoadError"
      :submitting="actingSubmitting"
      @update:open="handleActingDialogOpenChange"
      @retry="retryActingDialog"
      @submit="submitActingReportings"
    />

    <Dialog :open="organizationStatusOpen" @update:open="handleOrganizationStatusOpenChange">
      <DialogContent class="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{{ selectedUnit?.enabled ? '停用组织' : '启用组织' }}</DialogTitle>
          <DialogDescription>
            停用前服务端会检查有效下级组织和在职员工，不满足条件时不会修改状态。
          </DialogDescription>
        </DialogHeader>
        <div class="space-y-2">
          <Label for="organization-status-reason">调整原因</Label>
          <Textarea id="organization-status-reason" v-model="organizationStatusReason" :rows="4" />
        </div>
        <DialogFooter>
          <Button
            variant="outline"
            :disabled="organizationSubmitting"
            @click="handleOrganizationStatusOpenChange(false)"
            >取消</Button
          >
          <Button
            :disabled="!organizationStatusReason.trim() || organizationSubmitting"
            @click="changeOrganizationStatus"
            >确认</Button
          >
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { BriefcaseBusiness, FolderPlus, Pencil, Plus } from '@lucide/vue'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Textarea } from '@/components/ui/textarea'
import {
  createOrganizationUnit,
  createPosition,
  disableOrganizationUnit,
  disablePosition,
  enableOrganizationUnit,
  enablePosition,
  fetchActingManagerCandidates,
  fetchActingReportings,
  fetchEmployeeOrganizationHistory,
  fetchEmployeeOrganizationMembership,
  fetchManagerCandidates,
  fetchOrganizationEmployees,
  fetchOrganizationLeaderCandidates,
  fetchOrganizationParentCandidates,
  fetchOrganizationTree,
  fetchPositions,
  replaceActingReportings,
  updateEmployeeOrganizationMembership,
  updateOrganizationUnit,
  updatePosition,
} from '@/modules/organization/api/organization-api'
import ActingReportingDialog from '@/modules/organization/components/ActingReportingDialog.vue'
import EmployeeAssignmentDialog, {
  type EmployeeAssignmentDialogMode,
} from '@/modules/organization/components/EmployeeAssignmentDialog.vue'
import OrganizationFormDialog from '@/modules/organization/components/OrganizationFormDialog.vue'
import OrganizationTree from '@/modules/organization/components/OrganizationTree.vue'
import PositionManagement from '@/modules/organization/components/PositionManagement.vue'
import {
  EMPLOYEE_STATUS_LABEL,
  EMPLOYEE_ORGANIZATION_ACTION,
  ORGANIZATION_UNIT_TYPE_LABEL,
  flattenOrganizationTree,
  type ActingReportingCollection,
  type EmployeeOrganizationAction,
  type EmployeeOrganizationMembership,
  type EmployeeSummary,
  type ManagerCandidate,
  type OrganizationChangeHistory,
  type OrganizationFormSubmission,
  type OrganizationParentCandidate,
  type OrganizationUnit,
  type Position,
  type PositionFormSubmission,
  type ReplaceActingReportingsRequest,
  type UpdateEmployeeOrganizationRequest,
} from '@/modules/organization/model/organization.types'
import { ApiError } from '@/shared/api/api-error'
import { API_ERROR_CODE } from '@/shared/api/error-codes'
import { useLatestRequest } from '@/shared/composables/use-latest-request'
import { PERMISSIONS } from '@/shared/constants/permissions'
import { messageTip } from '@/shared/utils/feedback'
import StatusBadge from '@/shared/ui/StatusBadge.vue'
import { usePermissionStore } from '@/stores/permission.store'

const permissionStore = usePermissionStore()
const organizationTree = ref<OrganizationUnit[]>([])
const positions = ref<Position[]>([])
const selectedUnit = ref<OrganizationUnit | null>(null)
const foundationLoading = ref(false)
const organizationFormOpen = ref(false)
const editingUnit = ref<OrganizationUnit | null>(null)
const creatingParent = ref<OrganizationUnit | null>(null)
const organizationSubmitting = ref(false)
const organizationStatusOpen = ref(false)
const organizationStatusReason = ref('')
const organizationLeaderCandidates = ref<ManagerCandidate[]>([])
const organizationParentCandidates = ref<OrganizationParentCandidate[]>([])
const organizationCandidatesLoading = ref(false)
const organizationCandidateError = ref('')
const positionDialogOpen = ref(false)
const positionSubmitting = ref(false)
const assignmentDialogOpen = ref(false)
const assignmentDialogMode = ref<EmployeeAssignmentDialogMode>('assignment')
const assignmentLoading = ref(false)
const assignmentLoadError = ref('')
const assignmentSubmitting = ref(false)
const selectedEmployee = ref<EmployeeSummary | null>(null)
const employeeMembership = ref<EmployeeOrganizationMembership | null>(null)
const managerCandidates = ref<ManagerCandidate[]>([])
const employeeHistory = ref<OrganizationChangeHistory[]>([])
const actingDialogOpen = ref(false)
const actingEmployee = ref<EmployeeSummary | null>(null)
const actingCollection = ref<ActingReportingCollection | null>(null)
const actingCandidates = ref<ManagerCandidate[]>([])
const actingLoading = ref(false)
const actingLoadError = ref('')
const actingSubmitting = ref(false)
const employeeRequest = useLatestRequest<EmployeeSummary[]>()
const employees = computed(() => employeeRequest.data.value ?? [])
const employeeLoading = computed(() => employeeRequest.loading.value)
let assignmentRequestId = 0
let assignmentAbortController: AbortController | null = null
let managerCandidateRequestId = 0
let managerCandidateAbortController: AbortController | null = null
let organizationCandidateRequestId = 0
let organizationCandidateAbortController: AbortController | null = null
let actingRequestId = 0
let actingAbortController: AbortController | null = null

async function loadFoundation(preferredUnitId?: string): Promise<void> {
  foundationLoading.value = true
  try {
    const tree = await fetchOrganizationTree()
    organizationTree.value = tree
    const flatUnits = flattenOrganizationTree(tree)
    const nextUnit =
      flatUnits.find((unit) => String(unit.id) === preferredUnitId) ?? flatUnits[0] ?? null
    selectedUnit.value = nextUnit
    if (nextUnit) await loadEmployees(nextUnit)
  } catch (error: unknown) {
    messageTip(organizationErrorMessage(error, '加载组织架构失败'), 'error')
  } finally {
    foundationLoading.value = false
  }
  if (permissionStore.hasPermission(PERMISSIONS.position.list)) {
    try {
      positions.value = await fetchPositions()
    } catch (error: unknown) {
      positions.value = []
      messageTip(organizationErrorMessage(error, '加载岗位目录失败'), 'error')
    }
  } else {
    positions.value = []
  }
}

async function loadEmployees(unit: OrganizationUnit): Promise<void> {
  employeeRequest.data.value = []
  await employeeRequest.run((signal) => fetchOrganizationEmployees(unit.id, signal))
  if (employeeRequest.error.value) {
    messageTip(organizationErrorMessage(employeeRequest.error.value, '加载组织员工失败'), 'error')
  }
}

function selectOrganization(unit: OrganizationUnit): void {
  selectedUnit.value = unit
  void loadEmployees(unit)
}

function openCreateRoot(): void {
  if (organizationTree.value.length > 0) return
  editingUnit.value = null
  creatingParent.value = null
  organizationFormOpen.value = true
  void loadOrganizationCandidates()
}

function openCreateChild(): void {
  if (!selectedUnit.value || selectedUnit.value.type === 'TEAM') return
  editingUnit.value = null
  creatingParent.value = selectedUnit.value
  organizationFormOpen.value = true
  void loadOrganizationCandidates()
}

function openEditOrganization(): void {
  editingUnit.value = selectedUnit.value
  creatingParent.value = null
  organizationFormOpen.value = true
  void loadOrganizationCandidates()
}

async function loadOrganizationCandidates(): Promise<void> {
  const requestId = ++organizationCandidateRequestId
  organizationCandidateAbortController?.abort()
  const controller = new AbortController()
  organizationCandidateAbortController = controller
  organizationCandidatesLoading.value = true
  organizationCandidateError.value = ''
  organizationLeaderCandidates.value = []
  organizationParentCandidates.value = []
  try {
    const parentRequest = editingUnit.value
      ? fetchOrganizationParentCandidates(
          { type: editingUnit.value.type, excludeId: editingUnit.value.id },
          controller.signal,
        )
      : Promise.resolve([])
    const [leaders, parents] = await Promise.all([
      fetchOrganizationLeaderCandidates({}, controller.signal),
      parentRequest,
    ])
    if (requestId !== organizationCandidateRequestId || controller.signal.aborted) return
    organizationLeaderCandidates.value = leaders
    organizationParentCandidates.value = parents
  } catch (error: unknown) {
    if (requestId !== organizationCandidateRequestId || controller.signal.aborted) return
    organizationCandidateError.value = organizationErrorMessage(
      error,
      '加载组织负责人或上级候选失败',
    )
  } finally {
    if (requestId === organizationCandidateRequestId) {
      organizationCandidatesLoading.value = false
      organizationCandidateAbortController = null
    }
  }
}

async function submitOrganization(submission: OrganizationFormSubmission): Promise<void> {
  organizationSubmitting.value = true
  try {
    const saved =
      submission.mode === 'create'
        ? await createOrganizationUnit(submission.request)
        : await updateOrganizationUnit(submission.id, submission.request)
    organizationFormOpen.value = false
    messageTip(submission.mode === 'create' ? '组织创建成功' : '组织更新成功', 'success')
    await loadFoundation(String(saved.id))
  } catch (error: unknown) {
    messageTip(organizationErrorMessage(error, '保存组织失败，请检查组织层级'), 'error')
  } finally {
    organizationSubmitting.value = false
  }
}

function openOrganizationStatus(): void {
  organizationStatusReason.value = ''
  organizationStatusOpen.value = true
}

function handleOrganizationStatusOpenChange(open: boolean): void {
  if (!open && organizationSubmitting.value) return
  organizationStatusOpen.value = open
}

async function changeOrganizationStatus(): Promise<void> {
  const unit = selectedUnit.value
  const reason = organizationStatusReason.value.trim()
  if (!unit || !reason) return
  organizationSubmitting.value = true
  try {
    const request = { expectedVersion: unit.version, reason }
    const saved = unit.enabled
      ? await disableOrganizationUnit(unit.id, request)
      : await enableOrganizationUnit(unit.id, request)
    organizationStatusOpen.value = false
    messageTip(saved.enabled ? '组织已启用' : '组织已停用', 'success')
    await loadFoundation(String(saved.id))
  } catch (error: unknown) {
    messageTip(organizationErrorMessage(error, '组织状态调整失败'), 'error')
  } finally {
    organizationSubmitting.value = false
  }
}

async function submitPosition(submission: PositionFormSubmission): Promise<void> {
  positionSubmitting.value = true
  try {
    if (submission.mode === 'create') await createPosition(submission.request)
    else await updatePosition(submission.id, submission.request)
    positions.value = await fetchPositions()
    positionDialogOpen.value = false
    messageTip('岗位保存成功', 'success')
  } catch (error: unknown) {
    messageTip(organizationErrorMessage(error, '岗位保存失败'), 'error')
  } finally {
    positionSubmitting.value = false
  }
}

async function changePositionStatus(payload: {
  position: Position
  reason: string
}): Promise<void> {
  positionSubmitting.value = true
  try {
    const request = { expectedVersion: payload.position.version, reason: payload.reason }
    if (payload.position.enabled) await disablePosition(payload.position.id, request)
    else await enablePosition(payload.position.id, request)
    positions.value = await fetchPositions()
    positionDialogOpen.value = false
    messageTip('岗位状态已更新', 'success')
  } catch (error: unknown) {
    messageTip(organizationErrorMessage(error, '岗位状态调整失败'), 'error')
  } finally {
    positionSubmitting.value = false
  }
}

function canEmployeeAction(employee: EmployeeSummary, action: EmployeeOrganizationAction): boolean {
  return employee.allowedActions?.includes(action) ?? false
}

function employeeActionReason(
  employee: EmployeeSummary,
  action: EmployeeOrganizationAction,
): string {
  if (canEmployeeAction(employee, action)) return ''
  return employee.unavailableReasons?.[action] ?? '当前员工不允许执行此操作'
}

function openEmployeeDialog(employee: EmployeeSummary, mode: EmployeeAssignmentDialogMode): void {
  selectedEmployee.value = employee
  assignmentDialogMode.value = mode
  assignmentDialogOpen.value = true
  void loadEmployeeDialogData(employee, mode)
}

async function retryEmployeeDialog(): Promise<void> {
  if (!selectedEmployee.value) return
  await loadEmployeeDialogData(selectedEmployee.value, assignmentDialogMode.value)
}

async function loadEmployeeDialogData(
  employee: EmployeeSummary,
  mode: EmployeeAssignmentDialogMode,
): Promise<void> {
  const requestId = ++assignmentRequestId
  assignmentAbortController?.abort()
  const controller = new AbortController()
  assignmentAbortController = controller
  employeeMembership.value = null
  managerCandidates.value = []
  employeeHistory.value = []
  assignmentLoadError.value = ''
  assignmentLoading.value = true
  try {
    const [membership, candidates, history] = await Promise.all([
      fetchEmployeeOrganizationMembership(employee.id, controller.signal),
      mode === 'reporting' || mode === 'assignment'
        ? fetchManagerCandidates(employee.id, controller.signal)
        : Promise.resolve([]),
      mode === 'history'
        ? fetchEmployeeOrganizationHistory(employee.id, controller.signal)
        : Promise.resolve([]),
    ])
    if (requestId !== assignmentRequestId || controller.signal.aborted) return
    employeeMembership.value = membership
    managerCandidates.value = candidates
    employeeHistory.value = history
  } catch (error: unknown) {
    if (requestId !== assignmentRequestId || controller.signal.aborted) return
    assignmentLoadError.value = organizationErrorMessage(error, '加载员工组织信息失败')
  } finally {
    if (requestId === assignmentRequestId) {
      assignmentLoading.value = false
      assignmentAbortController = null
    }
  }
}

async function loadAssignmentManagerCandidates(organizationUnitId: string): Promise<void> {
  const employee = selectedEmployee.value
  if (!employee || !organizationUnitId) return
  const requestId = ++managerCandidateRequestId
  managerCandidateAbortController?.abort()
  const controller = new AbortController()
  managerCandidateAbortController = controller
  managerCandidates.value = []
  try {
    const result = await fetchManagerCandidates(employee.id, controller.signal, organizationUnitId)
    if (requestId !== managerCandidateRequestId || controller.signal.aborted) return
    managerCandidates.value = result
  } catch (error: unknown) {
    if (requestId !== managerCandidateRequestId || controller.signal.aborted) return
    messageTip(organizationErrorMessage(error, '加载目标组织直属管理者失败'), 'error')
  } finally {
    if (requestId === managerCandidateRequestId) managerCandidateAbortController = null
  }
}

function handleAssignmentDialogOpenChange(open: boolean): void {
  if (!open && assignmentSubmitting.value) return
  assignmentDialogOpen.value = open
  if (!open) {
    assignmentAbortController?.abort()
    managerCandidateAbortController?.abort()
    assignmentAbortController = null
    managerCandidateAbortController = null
    assignmentRequestId += 1
    managerCandidateRequestId += 1
    assignmentLoading.value = false
  }
}

async function submitEmployeeAssignment(request: UpdateEmployeeOrganizationRequest): Promise<void> {
  const employee = selectedEmployee.value
  if (!employee) return
  assignmentSubmitting.value = true
  try {
    employeeMembership.value = await updateEmployeeOrganizationMembership(employee.id, request)
    handleAssignmentDialogOpenChange(false)
    messageTip(
      assignmentDialogMode.value === 'reporting' ? '员工汇报关系已更新' : '员工任职已更新',
      'success',
    )
    if (selectedUnit.value) await loadEmployees(selectedUnit.value)
  } catch (error: unknown) {
    messageTip(organizationErrorMessage(error, '保存员工任职失败'), 'error')
  } finally {
    assignmentSubmitting.value = false
  }
}

function openActingDialog(employee: EmployeeSummary): void {
  actingEmployee.value = employee
  actingDialogOpen.value = true
  void loadActingDialog(employee)
}

async function retryActingDialog(): Promise<void> {
  if (actingEmployee.value) await loadActingDialog(actingEmployee.value)
}

async function loadActingDialog(employee: EmployeeSummary): Promise<void> {
  const requestId = ++actingRequestId
  actingAbortController?.abort()
  const controller = new AbortController()
  actingAbortController = controller
  actingCollection.value = null
  actingCandidates.value = []
  actingLoadError.value = ''
  actingLoading.value = true
  try {
    const [collection, candidates] = await Promise.all([
      fetchActingReportings(employee.id, controller.signal),
      fetchActingManagerCandidates(employee.id, controller.signal),
    ])
    if (requestId !== actingRequestId || controller.signal.aborted) return
    actingCollection.value = collection
    actingCandidates.value = candidates
  } catch (error: unknown) {
    if (requestId !== actingRequestId || controller.signal.aborted) return
    actingLoadError.value = organizationErrorMessage(error, '加载代理管理关系失败')
  } finally {
    if (requestId === actingRequestId) {
      actingLoading.value = false
      actingAbortController = null
    }
  }
}

function handleActingDialogOpenChange(open: boolean): void {
  if (!open && actingSubmitting.value) return
  actingDialogOpen.value = open
  if (!open) {
    actingAbortController?.abort()
    actingAbortController = null
    actingRequestId += 1
    actingLoading.value = false
  }
}

async function submitActingReportings(request: ReplaceActingReportingsRequest): Promise<void> {
  if (!actingEmployee.value || actingSubmitting.value) return
  actingSubmitting.value = true
  try {
    actingCollection.value = await replaceActingReportings(actingEmployee.value.id, request)
    actingSubmitting.value = false
    handleActingDialogOpenChange(false)
    messageTip('代理管理关系已更新', 'success')
    if (selectedUnit.value) await loadEmployees(selectedUnit.value)
  } catch (error: unknown) {
    messageTip(actingReportingErrorMessage(error), 'error')
    if (isOrganizationConflict(error) && actingEmployee.value) {
      await loadActingDialog(actingEmployee.value)
    }
  } finally {
    actingSubmitting.value = false
  }
}

function isOrganizationConflict(error: unknown): error is ApiError {
  return (
    error instanceof ApiError &&
    (error.httpStatus === 409 ||
      error.code === API_ERROR_CODE.CONFLICT ||
      error.code === API_ERROR_CODE.ORGANIZATION_VERSION_CONFLICT ||
      error.code === API_ERROR_CODE.ASSIGNMENT_CONFLICT)
  )
}

function actingReportingErrorMessage(error: unknown): string {
  if (isOrganizationConflict(error)) {
    return '代理管理关系或员工版本已变化，页面将刷新最新信息'
  }
  return organizationErrorMessage(error, '保存代理管理关系失败')
}

function organizationErrorMessage(error: unknown, fallback: string): string {
  if (!(error instanceof ApiError)) return fallback
  switch (error.code) {
    case API_ERROR_CODE.ORGANIZATION_VERSION_CONFLICT:
      return '组织数据已被其他人更新，请刷新后重试'
    case API_ERROR_CODE.ORGANIZATION_PARENT_CYCLE:
      return '不能把组织移动到自身或其下级组织中'
    case API_ERROR_CODE.REPORTING_CYCLE:
      return '该管理关系会形成汇报环路，请重新选择管理者或有效期'
    case API_ERROR_CODE.SELF_MANAGEMENT_FORBIDDEN:
      return '用户不能调整自己的组织关系或管理权限'
    case API_ERROR_CODE.INVALID_MANAGER:
      return '所选管理者已失效或超出当前可管理范围'
    case API_ERROR_CODE.ORGANIZATION_HAS_ACTIVE_CHILDREN:
      return '该组织仍有启用中的下级组织，暂时不能停用'
    case API_ERROR_CODE.ORGANIZATION_HAS_ACTIVE_EMPLOYEES:
      return '该组织仍有在职员工，暂时不能停用'
    case API_ERROR_CODE.POSITION_IN_USE:
      return '该岗位仍被有效任职使用，暂时不能停用'
    case API_ERROR_CODE.ASSIGNMENT_CONFLICT:
      return '任职时间或组织岗位关系与现有任职冲突'
    case API_ERROR_CODE.ORGANIZATION_HIERARCHY_INVALID:
      return '组织类型与上级层级不匹配，或系统中已存在根组织'
    case API_ERROR_CODE.CONFLICT:
      return '数据已被其他人更新或当前状态不允许，请刷新后重试'
    case API_ERROR_CODE.ACCESS_DENIED:
      return '没有权限执行此操作，或目标超出可管理范围'
    case API_ERROR_CODE.RESOURCE_IN_USE:
      return '该组织或岗位仍有关联数据，暂时不能停用'
    default:
      return fallback
  }
}

onMounted(() => void loadFoundation())
onBeforeUnmount(() => {
  assignmentAbortController?.abort()
  managerCandidateAbortController?.abort()
  organizationCandidateAbortController?.abort()
  actingAbortController?.abort()
})
</script>
