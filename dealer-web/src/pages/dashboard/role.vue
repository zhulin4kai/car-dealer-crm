<template>
  <div class="crm-data-page">
    <div class="grid min-h-[700px] gap-4 lg:grid-cols-[340px_minmax(0,1fr)]">
      <section class="crm-panel flex min-h-0 flex-col">
        <div class="min-h-0 flex-1">
          <RoleList
            :roles="roles"
            :selected-id="selectedRole?.id"
            :loading="roleLoading"
            :can-create="permissionStore.hasPermission(PERMISSIONS.role.add)"
            :can-edit="permissionStore.hasPermission(PERMISSIONS.role.edit)"
            :can-copy="permissionStore.hasPermission(PERMISSIONS.role.copy)"
            :can-change-status="permissionStore.hasPermission(PERMISSIONS.role.status)"
            @select="selectRole"
            @create="openCreateRole"
            @edit="openEditRole"
            @copy="openCopyRole"
            @change-status="openStatusDialog"
          />
        </div>
        <div class="border-t p-3">
          <DataTablePagination
            :page="currentRolePage"
            :page-size="rolePageSize"
            :total="roleTotal"
            @change="changeRolePage"
          />
        </div>
      </section>

      <section class="crm-panel flex min-h-0 min-w-0 flex-col">
        <div
          v-if="selectedRole"
          class="flex flex-wrap items-start justify-between gap-3 border-b p-4"
        >
          <div>
            <div class="flex flex-wrap items-center gap-2">
              <h2 class="text-lg font-semibold">{{ selectedRole.name }}</h2>
              <Badge v-if="selectedRole.protectedRole" variant="outline">受保护角色</Badge>
              <StatusBadge
                :label="selectedRole.enabled ? '启用' : '停用'"
                :tone="selectedRole.enabled ? 'success' : 'muted'"
              />
            </div>
            <p class="mt-1 text-sm text-muted-foreground">
              {{ selectedRole.code }} · 授权级别 {{ selectedRole.authorizationLevel }} ·
              {{ DATA_SCOPE_LABEL[selectedRole.defaultDataScope] }}
            </p>
            <p v-if="selectedRole.protectedReason" class="mt-2 text-sm text-amber-700">
              {{ selectedRole.protectedReason }}
            </p>
          </div>
          <Button
            v-has-permission="PERMISSIONS.permission.list"
            variant="outline"
            @click="router.push({ name: 'permission-catalog' })"
          >
            <BookOpen class="h-4 w-4" />查看权限目录
          </Button>
        </div>

        <div v-if="detailLoading" class="py-24 text-center text-muted-foreground">
          加载角色权限...
        </div>
        <div v-else-if="selectedRole && matrix" class="min-h-0 flex-1 p-4">
          <RolePermissionMatrix
            :catalog="permissionCatalog"
            :matrix="displayMatrix"
            :preview="matrixPreview"
            :previewing="previewing"
            :saving="matrixSaving"
            @preview="previewMatrix"
            @save="saveMatrix"
          />
        </div>
        <div v-else-if="selectedRole && !canViewPermissionMatrix" class="py-24 text-center text-muted-foreground">
          当前账号可以查看角色资料，但没有权限目录读取权限，不能查看权限矩阵
        </div>
        <div v-else class="py-24 text-center text-muted-foreground">
          {{
            permissionStore.hasPermission(PERMISSIONS.role.view)
              ? '请选择一个角色'
              : '当前账号只有角色列表权限，不能查看角色详情和权限矩阵'
          }}
        </div>
      </section>
    </div>

    <RoleFormDialog
      v-model:open="roleFormOpen"
      :role="editingRole"
      :copy-source="copySource"
      :organization-options="organizationOptions"
      :submitting="roleSubmitting"
      @submit="submitRole"
    />

    <Dialog :open="Boolean(statusRole)" @update:open="!$event && closeStatusDialog()">
      <DialogContent class="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{{ statusRole?.enabled ? '停用角色' : '启用角色' }}</DialogTitle>
          <DialogDescription>
            状态变化会影响角色成员的有效权限，并触发相关用户安全版本更新。
          </DialogDescription>
        </DialogHeader>
        <div class="space-y-2">
          <Label for="role-status-reason">调整原因</Label>
          <Textarea id="role-status-reason" v-model="statusReason" :rows="4" />
        </div>
        <DialogFooter>
          <Button variant="outline" :disabled="roleSubmitting" @click="closeStatusDialog()">
            取消
          </Button>
          <Button :disabled="!statusReason.trim() || roleSubmitting" @click="changeRoleStatus">
            确认
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { BookOpen } from '@lucide/vue'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { Badge } from '@/components/ui/badge'
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
import { Textarea } from '@/components/ui/textarea'
import {
  copyRole,
  createRole,
  disableRole,
  enableRole,
  fetchPermissionCatalog,
  fetchRoleDetail,
  fetchRoleOrganizationOptions,
  fetchRolePage,
  fetchRolePermissionMatrix,
  previewRolePermissionMatrix,
  updateRole,
  updateRolePermissionMatrix,
} from '@/modules/access/api/access-api'
import RoleFormDialog from '@/modules/access/components/RoleFormDialog.vue'
import RoleList from '@/modules/access/components/RoleList.vue'
import RolePermissionMatrix from '@/modules/access/components/RolePermissionMatrix.vue'
import {
  DATA_SCOPE_LABEL,
  ROLE_ACTION,
  type AccessOrganizationOption,
  type PermissionCatalogItem,
  type PreviewRolePermissionRequest,
  type RoleDetail,
  type RoleFormSubmission,
  type RolePermissionMatrix as RolePermissionMatrixData,
  type RolePermissionPreview,
  type RoleSummary,
  type UpdateRolePermissionRequest,
} from '@/modules/access/model/access.types'
import { getAccessErrorMessage, isAccessVersionConflict } from '@/modules/access/model/access-error'
import { PERMISSIONS } from '@/shared/constants/permissions'
import { messageTip } from '@/shared/utils/feedback'
import StatusBadge from '@/shared/ui/StatusBadge.vue'
import DataTablePagination from '@/shared/ui/DataTablePagination.vue'
import { usePermissionStore } from '@/stores/permission.store'

const router = useRouter()
const permissionStore = usePermissionStore()
const canViewPermissionMatrix = computed(() =>
  permissionStore.hasPermission(PERMISSIONS.permission.list),
)
const roles = ref<RoleSummary[]>([])
const selectedRole = ref<RoleDetail | null>(null)
const permissionCatalog = ref<PermissionCatalogItem[]>([])
const organizationOptions = ref<AccessOrganizationOption[]>([])
const matrix = ref<RolePermissionMatrixData | null>(null)
const displayMatrix = computed<RolePermissionMatrixData>(() => {
  const current = matrix.value
  if (!current) {
    return {
      roleId: 0,
      roleName: '',
      expectedVersion: 0,
      selectedPermissionIds: [],
      permissionScopes: [],
      permissionScopeOptions: [],
      editable: false,
      disabledReason: '角色权限矩阵尚未加载',
    }
  }
  if (!permissionStore.hasPermission(PERMISSIONS.role.permissionManage)) {
    return { ...current, editable: false, disabledReason: '当前账号没有角色权限矩阵管理权限' }
  }
  return current
})
const matrixPreview = ref<RolePermissionPreview | null>(null)
const roleLoading = ref(false)
const currentRolePage = ref(1)
const rolePageSize = ref(10)
const roleTotal = ref(0)
const detailLoading = ref(false)
const roleSubmitting = ref(false)
const previewing = ref(false)
const matrixSaving = ref(false)
const roleFormOpen = ref(false)
const editingRole = ref<RoleDetail | null>(null)
const copySource = ref<RoleDetail | null>(null)
const statusRole = ref<RoleDetail | null>(null)
const statusReason = ref('')
let detailRequestId = 0

async function loadRoleFoundation(preferredRoleId?: string): Promise<void> {
  roleLoading.value = true
  try {
    const canViewRole = permissionStore.hasPermission(PERMISSIONS.role.view)
    const canListPermission = permissionStore.hasPermission(PERMISSIONS.permission.list)
    const [rolePageResult, catalog, options] = await Promise.all([
      fetchRolePage({ page: currentRolePage.value, size: rolePageSize.value }),
      canListPermission ? fetchPermissionCatalog() : Promise.resolve([]),
      canViewRole ? fetchRoleOrganizationOptions() : Promise.resolve([]),
    ])
    roles.value = rolePageResult.list
    roleTotal.value = rolePageResult.total
    rolePageSize.value = rolePageResult.pageSize || rolePageSize.value
    currentRolePage.value = rolePageResult.pageNum || currentRolePage.value
    permissionCatalog.value = catalog
    organizationOptions.value = options
    const nextRole =
      rolePageResult.list.find((role) => String(role.id) === preferredRoleId) ??
      rolePageResult.list[0]
    if (nextRole && canViewRole) await selectRole(nextRole)
  } catch (error: unknown) {
    messageTip(getAccessErrorMessage(error, '加载角色目录失败'), 'error')
  } finally {
    roleLoading.value = false
  }
}

function changeRolePage(page: number): void {
  if (page === currentRolePage.value || roleLoading.value) return
  currentRolePage.value = page
  selectedRole.value = null
  matrix.value = null
  void loadRoleFoundation()
}

async function selectRole(role: RoleSummary): Promise<void> {
  if (!permissionStore.hasPermission(PERMISSIONS.role.view)) return
  const requestId = ++detailRequestId
  detailLoading.value = true
  matrixPreview.value = null
  try {
    const [detail, permissionMatrix] = await Promise.all([
      fetchRoleDetail(role.id),
      canViewPermissionMatrix.value ? fetchRolePermissionMatrix(role.id) : Promise.resolve(null),
    ])
    if (requestId !== detailRequestId) return
    selectedRole.value = detail
    matrix.value = permissionMatrix
  } catch (error: unknown) {
    if (requestId === detailRequestId) {
      messageTip(getAccessErrorMessage(error, '加载角色权限失败'), 'error')
    }
  } finally {
    if (requestId === detailRequestId) detailLoading.value = false
  }
}

function openCreateRole(): void {
  editingRole.value = null
  copySource.value = null
  roleFormOpen.value = true
}

function openEditRole(role: RoleSummary): void {
  if (!role.editable || !role.allowedActions?.includes(ROLE_ACTION.EDIT)) return
  editingRole.value = role
  copySource.value = null
  roleFormOpen.value = true
}

function openCopyRole(role: RoleSummary): void {
  if (!role.editable || !role.allowedActions?.includes(ROLE_ACTION.COPY)) {
    return
  }
  editingRole.value = null
  copySource.value = role
  roleFormOpen.value = true
}

async function submitRole(submission: RoleFormSubmission): Promise<void> {
  roleSubmitting.value = true
  try {
    const saved =
      submission.mode === 'create'
        ? await createRole(submission.request)
        : submission.mode === 'update'
          ? await updateRole(submission.id, submission.request)
          : await copyRole(submission.sourceRoleId, submission.request)
    roleFormOpen.value = false
    messageTip('角色保存成功', 'success')
    await loadRoleFoundation(String(saved.id))
  } catch (error: unknown) {
    messageTip(getAccessErrorMessage(error, '角色保存失败'), 'error')
  } finally {
    roleSubmitting.value = false
  }
}

function openStatusDialog(role: RoleSummary): void {
  if (
    role.protectedRole ||
    !role.editable ||
    !role.allowedActions?.includes(ROLE_ACTION.STATUS_CHANGE)
  ) {
    return
  }
  statusRole.value = role
  statusReason.value = ''
}

function closeStatusDialog(force = false): void {
  if (roleSubmitting.value && !force) return
  statusRole.value = null
  statusReason.value = ''
}

async function changeRoleStatus(): Promise<void> {
  const role = statusRole.value
  const reason = statusReason.value.trim()
  if (!role || !reason) return
  roleSubmitting.value = true
  try {
    const request = { expectedVersion: role.version, reason }
    const saved = role.enabled
      ? await disableRole(role.id, request)
      : await enableRole(role.id, request)
    closeStatusDialog(true)
    messageTip(saved.enabled ? '角色已启用' : '角色已停用', 'success')
    await loadRoleFoundation(String(saved.id))
  } catch (error: unknown) {
    messageTip(getAccessErrorMessage(error, '角色状态调整失败'), 'error')
  } finally {
    roleSubmitting.value = false
  }
}

async function previewMatrix(request: PreviewRolePermissionRequest): Promise<void> {
  const role = selectedRole.value
  if (!role) return
  previewing.value = true
  matrixPreview.value = null
  try {
    matrixPreview.value = await previewRolePermissionMatrix(role.id, request)
  } catch (error: unknown) {
    messageTip(getAccessErrorMessage(error, '权限影响预览失败'), 'error')
    await refreshMatrixOnConflict(error, role.id)
  } finally {
    previewing.value = false
  }
}

async function saveMatrix(request: UpdateRolePermissionRequest): Promise<void> {
  const role = selectedRole.value
  if (!role) return
  matrixSaving.value = true
  try {
    const result = await updateRolePermissionMatrix(role.id, request)
    messageTip(
      `权限矩阵已更新，影响 ${result.affectedUserCount} 名用户`,
      (result.sessionCleanupWarningCount ?? 0) > 0 ? 'warning' : 'success',
    )
    await loadRoleFoundation(String(role.id))
  } catch (error: unknown) {
    messageTip(getAccessErrorMessage(error, '权限矩阵保存失败'), 'error')
    await refreshMatrixOnConflict(error, role.id)
  } finally {
    matrixSaving.value = false
  }
}

async function refreshMatrixOnConflict(error: unknown, roleId: RoleDetail['id']): Promise<void> {
  if (!isAccessVersionConflict(error)) return
  matrixPreview.value = null
  matrix.value = null
  await loadRoleFoundation(String(roleId))
}

onMounted(() => void loadRoleFoundation())
</script>
