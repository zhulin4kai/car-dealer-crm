<template>
  <div class="crm-data-page space-y-4">
    <section class="crm-panel p-4">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 class="text-xl font-semibold">用户详情</h1>
          <p class="text-sm text-muted-foreground">资料、任职、授权、账号安全与会话分区管理</p>
        </div>
        <Button variant="outline" @click="router.push({ name: 'user' })">返回用户列表</Button>
      </div>
    </section>
    <section v-if="targetUserId === null" class="crm-panel py-20 text-center text-destructive">
      用户 ID 无效，无法加载详情。
    </section>
    <section v-else-if="loading" class="crm-panel py-20 text-center text-muted-foreground">
      加载用户详情...
    </section>
    <section v-else-if="loadError" class="crm-panel space-y-4 py-20 text-center">
      <p class="text-destructive">{{ loadError }}</p>
      <Button variant="outline" @click="loadDetail">重新加载</Button>
    </section>
    <template v-else-if="detail">
      <Card
        ><CardHeader
          ><div class="flex flex-wrap items-start justify-between gap-3">
            <div>
              <CardTitle>{{ detail.name }}</CardTitle
              ><CardDescription
                >{{ detail.loginAct
                }}<template v-if="detail.employeeNo">
                  · {{ detail.employeeNo }}</template
                ></CardDescription
              >
            </div>
            <div class="flex flex-wrap gap-2">
              <Button
                size="sm"
                variant="outline"
                :disabled="!hasManagedAction(MANAGED_USER_ACTION.PROFILE_UPDATE)"
                :title="managedReason(MANAGED_USER_ACTION.PROFILE_UPDATE)"
                @click="profileDialogOpen = true"
                >编辑资料</Button
              >
              <Button
                v-if="hasManagedAction(MANAGED_USER_ACTION.TRANSFER)"
                size="sm"
                variant="outline"
                @click="transferDialogOpen = true"
                >调岗</Button
              >
              <Button
                size="sm"
                variant="outline"
                :disabled="!hasManagedAction(MANAGED_USER_ACTION.ACCOUNT_IDENTITY_UPDATE)"
                :title="managedReason(MANAGED_USER_ACTION.ACCOUNT_IDENTITY_UPDATE)"
                @click="loginAccountDialogOpen = true"
                >登录账号</Button
              >
              <Button
                size="sm"
                variant="outline"
                :disabled="!hasManagedAction(MANAGED_USER_ACTION.SECURITY_EXPIRATION_UPDATE)"
                :title="managedReason(MANAGED_USER_ACTION.SECURITY_EXPIRATION_UPDATE)"
                @click="securityExpirationDialogOpen = true"
                >安全到期</Button
              >
              <Button
                size="sm"
                variant="outline"
                :disabled="!hasManagedAction(MANAGED_USER_ACTION.STATUS_UPDATE)"
                :title="managedReason(MANAGED_USER_ACTION.STATUS_UPDATE)"
                @click="statusDialogOpen = true"
                >账号状态</Button
              >
              <Button
                size="sm"
                variant="outline"
                :disabled="!hasManagedAction(MANAGED_USER_ACTION.PASSWORD_RESET)"
                :title="managedReason(MANAGED_USER_ACTION.PASSWORD_RESET)"
                @click="passwordDialogOpen = true"
                >重置密码</Button
              >
              <Button
                size="sm"
                variant="outline"
                :disabled="!hasManagedAction(MANAGED_USER_ACTION.REINVITE)"
                :title="managedReason(MANAGED_USER_ACTION.REINVITE)"
                @click="openReinviteDialog"
                >重新邀请</Button
              >
              <Button
                v-if="hasManagedAction(MANAGED_USER_ACTION.DEPARTURE)"
                size="sm"
                variant="outline"
                @click="departureDialogOpen = true"
                >离职闭环</Button
              >
              <Button
                v-if="hasManagedAction(MANAGED_USER_ACTION.REHIRE)"
                size="sm"
                variant="outline"
                @click="rehireDialogOpen = true"
                >返聘</Button
              >
            </div>
          </div></CardHeader
        ><CardContent class="grid gap-3 text-sm sm:grid-cols-3"
          ><div>手机：{{ detail.phone || '未设置' }}</div>
          <div>邮箱：{{ detail.email || '未设置' }}</div>
          <div>最近登录：{{ formatDateTime(detail.lastLoginTime) }}</div></CardContent
        ></Card
      >

      <section class="grid gap-4 lg:grid-cols-2">
        <Card
          ><CardHeader><CardTitle>任职信息</CardTitle></CardHeader
          ><CardContent class="grid gap-3 text-sm sm:grid-cols-2"
            ><div>组织：{{ detail.organizationName || '未设置' }}</div>
            <div>岗位：{{ detail.positionName || '未设置' }}</div>
            <div>直属管理者：{{ detail.managerName || '未设置' }}</div>
            <div>任职状态：{{ detail.employmentStatus }}</div></CardContent
          ></Card
        ><Card
          ><CardHeader><CardTitle>账号状态</CardTitle></CardHeader
          ><CardContent class="grid gap-3 text-sm sm:grid-cols-2"
            ><div>账号：{{ detail.accountStatus }}</div>
            <div>锁定：{{ detail.lockStatus }}</div>
            <div>账号到期：{{ detail.accountExpired ? '已到期' : formatDateTime(detail.accountExpiresAt) }}</div>
            <div>凭证到期：{{ detail.credentialExpired ? '已到期' : formatDateTime(detail.credentialExpiresAt) }}</div>
            <div v-if="detail.lockReason" class="sm:col-span-2">
              锁定原因：{{ detail.lockReason }}
            </div></CardContent
          ></Card
        >
      </section>

      <template v-if="hasManagedAction(MANAGED_USER_ACTION.AUTHORIZATION_VIEW) && authorization">
        <section class="crm-panel p-4">
          <UserRoleAssignment
            :assignments="authorization.roleAssignments"
            :candidates="authorization.roleCandidates"
            :authorization-version="authorization.authorizationVersion"
            :editable="canUpdateRoles"
            :disabled-reason="authorizationReason(USER_AUTHORIZATION_ACTION.ROLE_UPDATE)"
            :submitting="roleSubmitting"
            @save="saveRoles"
          />
        </section>
        <section class="crm-panel p-4">
          <UserPermissionEditor
            :permissions="authorization.permissions"
            :authorization-version="authorization.authorizationVersion"
            :editable="canUpdatePermissions"
            :disabled-reason="authorizationReason(USER_AUTHORIZATION_ACTION.PERMISSION_UPDATE)"
            :submitting="permissionSubmitting"
            @save="savePermissions"
          />
        </section>
      </template>
      <section v-else class="crm-panel p-4 text-sm text-muted-foreground">
        {{ authorizationLoadError || managedReason(MANAGED_USER_ACTION.AUTHORIZATION_VIEW) }}
      </section>

      <section v-if="canViewSessions" class="crm-panel p-4">
        <UserSessionList
          :collection="managedSessions"
          :loading="sessionLoading"
          :error-message="sessionError"
          :busy-action="sessionBusyAction"
          :mutation-allowed="canRevokeSessions"
          :disabled-reason="sessionMutationReason"
          require-reason
          @retry="loadManagedSessions"
          @revoke="revokeManagedSession"
          @revoke-all="revokeAllManagedSessions"
        />
      </section>

      <section v-if="canViewHistory" class="crm-panel p-4">
        <UserHistoryTimeline :user-id="detail.id" enabled />
      </section>
      <section
        v-else-if="hasAuditDetailPermission && !hasManagedAction(MANAGED_USER_ACTION.HISTORY_VIEW)"
        class="crm-panel p-4 text-sm text-muted-foreground"
      >
        变更历史：{{ managedReason(MANAGED_USER_ACTION.HISTORY_VIEW) }}
      </section>

      <UserFormDialog
        v-model:open="profileDialogOpen"
        mode="edit"
        :user="detail"
        :options="emptyOptions"
        :submitting="profileSubmitting"
        @update="saveProfile"
      />
      <UserStatusDialog
        v-model:open="statusDialogOpen"
        :account-version="detail.accountVersion"
        :commands="detail.statusCommands"
        :submitting="statusSubmitting"
        @submit="saveStatus"
      />
      <UserLoginAccountDialog
        v-model:open="loginAccountDialogOpen"
        :account-version="detail.accountVersion"
        :current-login-act="detail.loginAct"
        :submitting="loginAccountSubmitting"
        @submit="saveLoginAccount"
      />
      <UserSecurityExpirationDialog
        v-model:open="securityExpirationDialogOpen"
        :account-version="detail.accountVersion"
        :account-expires-at="detail.accountExpiresAt"
        :credential-expires-at="detail.credentialExpiresAt"
        :submitting="securityExpirationSubmitting"
        @submit="saveSecurityExpiration"
      />
      <UserPasswordResetDialog
        v-model:open="passwordDialogOpen"
        :account-version="detail.accountVersion"
        :submitting="passwordSubmitting"
        @submit="resetPassword"
      />
      <UserReinviteCredentialDialog
        v-model:open="reinviteCredentialDialogOpen"
        :account-version="detail.accountVersion"
        :submitting="reinviteCredentialSubmitting"
        :result="reinviteCredentialResult"
        @submit="sendReinvite"
      />
      <UserTransferDialog
        v-model:open="transferDialogOpen"
        :user-id="detail.id"
        @completed="handleTransferCompleted"
      />
      <UserDepartureDialog
        v-model:open="departureDialogOpen"
        :user-id="detail.id"
        @completed="handleDepartureCompleted"
      />
      <UserRehireDialog
        v-model:open="rehireDialogOpen"
        :user-id="detail.id"
        @completed="handleRehireCompleted"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import {
  fetchUserAuthorizationDetail,
  updateUserPermissions,
  updateUserRoleAssignments,
} from '@/modules/access/api/user-authorization-api'
import UserPermissionEditor from '@/modules/access/components/UserPermissionEditor.vue'
import UserRoleAssignment from '@/modules/access/components/UserRoleAssignment.vue'
import { getUserAuthorizationErrorMessage } from '@/modules/access/model/access-error'
import {
  USER_AUTHORIZATION_ACTION,
  type UpdateUserPermissionsRequest,
  type UpdateUserRoleAssignmentsRequest,
  type UserAuthorizationAction,
  type UserAuthorizationDetail,
} from '@/modules/access/model/user-permission.types'
import {
  changeManagedUserStatus,
  changeManagedUserLoginAccount,
  changeManagedUserSecurityExpiration,
  fetchManagedUserDetail,
  resetManagedUserPassword,
  updateManagedUserProfile,
} from '@/modules/user/api/user-api'
import { reinviteManagedUser } from '@/modules/user/api/credential-api'
import {
  fetchManagedUserSessions,
  revokeAllManagedUserSessions,
  revokeManagedUserSession,
} from '@/modules/user/api/user-session-api'
import UserFormDialog from '@/modules/user/components/UserFormDialog.vue'
import UserDepartureDialog from '@/modules/user/components/UserDepartureDialog.vue'
import UserHistoryTimeline from '@/modules/user/components/UserHistoryTimeline.vue'
import UserPasswordResetDialog from '@/modules/user/components/UserPasswordResetDialog.vue'
import UserReinviteCredentialDialog from '@/modules/user/components/UserReinviteCredentialDialog.vue'
import UserSessionList from '@/modules/user/components/UserSessionList.vue'
import UserStatusDialog from '@/modules/user/components/UserStatusDialog.vue'
import UserLoginAccountDialog from '@/modules/user/components/UserLoginAccountDialog.vue'
import UserSecurityExpirationDialog from '@/modules/user/components/UserSecurityExpirationDialog.vue'
import UserTransferDialog from '@/modules/user/components/UserTransferDialog.vue'
import UserRehireDialog from '@/modules/user/components/UserRehireDialog.vue'
import type { RehireResult, UserLifecycleContext } from '@/modules/user/model/user-lifecycle.types'
import type {
  ManagedCredentialDeliveryResult,
  ReinviteManagedUserRequest,
} from '@/modules/user/model/credential.types'
import {
  MANAGED_USER_ACTION,
  type ChangeManagedUserStatusRequest,
  type ChangeManagedUserLoginAccountRequest,
  type ChangeManagedUserSecurityExpirationRequest,
  type ManagedUserAction,
  type ManagedUserDetail,
  type ResetManagedUserPasswordRequest,
  type UpdateManagedUserProfileRequest,
  type UserFilterOptions,
} from '@/modules/user/model/user.types'
import {
  USER_SESSION_ACTION,
  USER_SESSION_ITEM_ACTION,
  type UserSessionCollection,
  type UserSessionItem,
} from '@/modules/user/model/user-session.types'
import {
  getSessionCommandErrorMessage,
  isRefreshableSessionError,
} from '@/modules/user/model/user-session-error'
import { ApiError } from '@/shared/api/api-error'
import { API_ERROR_CODE } from '@/shared/api/error-codes'
import { formatDateTime } from '@/shared/utils/display-format'
import { messageConfirm, messageTip } from '@/shared/utils/feedback'
import { usePermissionStore } from '@/stores/permission.store'
import { useAuthStore } from '@/stores/auth.store'
import { PERMISSIONS } from '@/shared/constants/permissions'

const route = useRoute()
const router = useRouter()
const permissionStore = usePermissionStore()
const authStore = useAuthStore()
const emptyOptions: UserFilterOptions = {
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
const detail = ref<ManagedUserDetail | null>(null)
const authorization = ref<UserAuthorizationDetail | null>(null)
const authorizationLoadError = ref('')
const loading = ref(false)
const loadError = ref('')
const roleSubmitting = ref(false)
const permissionSubmitting = ref(false)
const profileSubmitting = ref(false)
const statusSubmitting = ref(false)
const loginAccountSubmitting = ref(false)
const securityExpirationSubmitting = ref(false)
const passwordSubmitting = ref(false)
const reinviteCredentialSubmitting = ref(false)
const reinviteCredentialResult = ref<ManagedCredentialDeliveryResult | null>(null)
const profileDialogOpen = ref(false)
const statusDialogOpen = ref(false)
const loginAccountDialogOpen = ref(false)
const securityExpirationDialogOpen = ref(false)
const passwordDialogOpen = ref(false)
const reinviteCredentialDialogOpen = ref(false)
const transferDialogOpen = ref(false)
const departureDialogOpen = ref(false)
const rehireDialogOpen = ref(false)
const managedSessions = ref<UserSessionCollection | null>(null)
const sessionLoading = ref(false)
const sessionError = ref('')
const sessionBusyAction = ref('')
let loadRequestId = 0
let loadAbortController: AbortController | null = null
let sessionRequestId = 0
let sessionAbortController: AbortController | null = null

const targetUserId = computed<number | null>(() => {
  const raw = route.params.id
  if (Array.isArray(raw) || typeof raw !== 'string' || !/^\d+$/.test(raw)) return null
  const value = Number(raw)
  return Number.isSafeInteger(value) && value > 0 ? value : null
})
const canUpdateRoles = computed(
  () =>
    hasManagedAction(MANAGED_USER_ACTION.AUTHORIZATION_UPDATE) &&
    hasAuthorizationAction(USER_AUTHORIZATION_ACTION.ROLE_UPDATE),
)
const canUpdatePermissions = computed(
  () =>
    hasManagedAction(MANAGED_USER_ACTION.AUTHORIZATION_UPDATE) &&
    hasAuthorizationAction(USER_AUTHORIZATION_ACTION.PERMISSION_UPDATE),
)
const canViewSessions = computed(() => hasManagedAction(MANAGED_USER_ACTION.SESSION_VIEW))
const canRevokeSessions = computed(
  () => canViewSessions.value && hasManagedAction(MANAGED_USER_ACTION.SESSION_REVOKE),
)
const hasAuditDetailPermission = computed(() =>
  permissionStore.hasPermission(PERMISSIONS.audit.operation.detail),
)
const canViewHistory = computed(
  () => hasAuditDetailPermission.value && hasManagedAction(MANAGED_USER_ACTION.HISTORY_VIEW),
)
const sessionMutationReason = computed(() =>
  canRevokeSessions.value ? '' : managedReason(MANAGED_USER_ACTION.SESSION_REVOKE),
)

function hasManagedAction(action: ManagedUserAction): boolean {
  return detail.value?.allowedActions.includes(action) ?? false
}
function managedReason(action: ManagedUserAction): string {
  return hasManagedAction(action)
    ? ''
    : (detail.value?.unavailableReasons[action] ?? '服务端未允许当前操作')
}
function hasAuthorizationAction(action: UserAuthorizationAction): boolean {
  return authorization.value?.allowedActions.includes(action) ?? false
}
function authorizationReason(action: UserAuthorizationAction): string {
  if (
    hasAuthorizationAction(action) &&
    ((action !== USER_AUTHORIZATION_ACTION.ROLE_UPDATE &&
      action !== USER_AUTHORIZATION_ACTION.PERMISSION_UPDATE) ||
      hasManagedAction(MANAGED_USER_ACTION.AUTHORIZATION_UPDATE))
  )
    return ''
  return (
    authorization.value?.unavailableReasons[action] ??
    managedReason(MANAGED_USER_ACTION.AUTHORIZATION_UPDATE)
  )
}

async function loadDetail(): Promise<void> {
  const userId = targetUserId.value
  if (userId === null) return
  const requestId = ++loadRequestId
  loadAbortController?.abort()
  const controller = new AbortController()
  loadAbortController = controller
  loading.value = true
  loadError.value = ''
  authorizationLoadError.value = ''
  try {
    const managed = await fetchManagedUserDetail(userId, controller.signal)
    if (requestId !== loadRequestId || controller.signal.aborted) return
    detail.value = managed
    if (managed.allowedActions.includes(MANAGED_USER_ACTION.SESSION_VIEW))
      void loadManagedSessions()
    else {
      managedSessions.value = null
      sessionError.value = ''
    }
    try {
      const auth = await fetchUserAuthorizationDetail(userId, controller.signal)
      if (requestId !== loadRequestId || controller.signal.aborted) return
      authorization.value = auth
    } catch (error: unknown) {
      if (requestId !== loadRequestId || controller.signal.aborted) return
      authorization.value = null
      authorizationLoadError.value = getUserAuthorizationErrorMessage(error, '加载用户授权信息失败')
    }
  } catch (error: unknown) {
    if (requestId !== loadRequestId || controller.signal.aborted) return
    detail.value = null
    authorization.value = null
    loadError.value = detailErrorMessage(error)
  } finally {
    if (requestId === loadRequestId) {
      loading.value = false
      loadAbortController = null
    }
  }
}
function detailErrorMessage(error: unknown): string {
  if (error instanceof ApiError && error.code === 404) return '用户不存在或已被删除'
  return getUserAuthorizationErrorMessage(error, '加载用户详情失败')
}
function isConflict(error: unknown): boolean {
  return (
    error instanceof ApiError &&
    (error.httpStatus === 409 ||
      error.code === API_ERROR_CODE.CONFLICT ||
      error.code === API_ERROR_CODE.ROLE_VERSION_CONFLICT ||
      error.code === API_ERROR_CODE.PROFILE_VERSION_CONFLICT ||
      error.code === API_ERROR_CODE.ACCOUNT_VERSION_CONFLICT)
  )
}
async function refreshOnConflict(error: unknown): Promise<void> {
  if (isConflict(error)) await loadDetail()
}

async function saveProfile(request: UpdateManagedUserProfileRequest) {
  const id = targetUserId.value
  if (
    id === null ||
    profileSubmitting.value ||
    !hasManagedAction(MANAGED_USER_ACTION.PROFILE_UPDATE)
  )
    return
  profileSubmitting.value = true
  try {
    detail.value = await updateManagedUserProfile(id, request)
    profileDialogOpen.value = false
    messageTip('用户资料已更新', 'success')
  } catch (error) {
    messageTip(isConflict(error) ? '用户资料已变化，已刷新最新信息' : '用户资料更新失败', 'error')
    await refreshOnConflict(error)
  } finally {
    profileSubmitting.value = false
  }
}
async function saveStatus(request: ChangeManagedUserStatusRequest) {
  const id = targetUserId.value
  if (
    id === null ||
    statusSubmitting.value ||
    !hasManagedAction(MANAGED_USER_ACTION.STATUS_UPDATE) ||
    !detail.value?.statusCommands.some(
      (item) => item.command === request.command && !item.disabledReason,
    )
  )
    return
  statusSubmitting.value = true
  try {
    detail.value = await changeManagedUserStatus(id, request)
    statusDialogOpen.value = false
    messageTip('账号状态已更新', 'success')
  } catch (error) {
    messageTip(isConflict(error) ? '账号状态已变化，已刷新最新信息' : '账号状态更新失败', 'error')
    await refreshOnConflict(error)
  } finally {
    statusSubmitting.value = false
  }
}
async function saveLoginAccount(request: ChangeManagedUserLoginAccountRequest) {
  const id = targetUserId.value
  if (id === null || loginAccountSubmitting.value || !hasManagedAction(MANAGED_USER_ACTION.ACCOUNT_IDENTITY_UPDATE)) return
  loginAccountSubmitting.value = true
  try {
    detail.value = await changeManagedUserLoginAccount(id, request)
    loginAccountDialogOpen.value = false
    messageTip('登录账号已更新，旧会话已失效', 'success')
  } catch (error) {
    messageTip(isConflict(error) ? '账号信息已变化，已刷新最新信息' : '登录账号更新失败', 'error')
    await refreshOnConflict(error)
  } finally {
    loginAccountSubmitting.value = false
  }
}
async function saveSecurityExpiration(request: ChangeManagedUserSecurityExpirationRequest) {
  const id = targetUserId.value
  if (id === null || securityExpirationSubmitting.value || !hasManagedAction(MANAGED_USER_ACTION.SECURITY_EXPIRATION_UPDATE)) return
  securityExpirationSubmitting.value = true
  try {
    detail.value = await changeManagedUserSecurityExpiration(id, request)
    securityExpirationDialogOpen.value = false
    messageTip('账号安全到期设置已更新，旧会话已失效', 'success')
  } catch (error) {
    messageTip(isConflict(error) ? '账号安全状态已变化，已刷新最新信息' : '账号安全到期设置失败', 'error')
    await refreshOnConflict(error)
  } finally {
    securityExpirationSubmitting.value = false
  }
}
async function resetPassword(request: ResetManagedUserPasswordRequest) {
  const id = targetUserId.value
  if (
    id === null ||
    passwordSubmitting.value ||
    !hasManagedAction(MANAGED_USER_ACTION.PASSWORD_RESET)
  )
    return
  passwordSubmitting.value = true
  try {
    await resetManagedUserPassword(id, request)
    passwordDialogOpen.value = false
    messageTip('密码重置凭证已排队，等待安全通知服务投递', 'success')
  } catch (error) {
    messageTip(isConflict(error) ? '用户状态已变化，已刷新最新信息' : '密码重置失败', 'error')
    await refreshOnConflict(error)
  } finally {
    passwordSubmitting.value = false
  }
}
function openReinviteDialog(): void {
  if (!hasManagedAction(MANAGED_USER_ACTION.REINVITE)) return
  reinviteCredentialResult.value = null
  reinviteCredentialDialogOpen.value = true
}
async function sendReinvite(request: ReinviteManagedUserRequest): Promise<void> {
  const id = targetUserId.value
  if (
    id === null ||
    reinviteCredentialSubmitting.value ||
    !hasManagedAction(MANAGED_USER_ACTION.REINVITE)
  )
    return
  reinviteCredentialSubmitting.value = true
  try {
    reinviteCredentialResult.value = await reinviteManagedUser(id, request)
    await loadDetail()
  } catch (error) {
    messageTip(isConflict(error) ? '用户状态已变化，已刷新最新信息' : '重新邀请失败', 'error')
    await refreshOnConflict(error)
  } finally {
    reinviteCredentialSubmitting.value = false
  }
}
async function handleTransferCompleted(_context: UserLifecycleContext) {
  messageTip('调岗已完成，原任职历史已保留', 'success')
  await loadDetail()
}
async function handleDepartureCompleted(_context: UserLifecycleContext) {
  messageTip('离职闭环已完成，账号和旧会话已失效', 'success')
  await loadDetail()
}
async function handleRehireCompleted(result: RehireResult) {
  messageTip(`返聘已完成：${result.credentialDeliveryStatus}`, 'success')
  await loadDetail()
}

async function saveRoles(request: UpdateUserRoleAssignmentsRequest) {
  const id = targetUserId.value
  if (id === null || roleSubmitting.value || !canUpdateRoles.value) return
  roleSubmitting.value = true
  try {
    authorization.value = await updateUserRoleAssignments(id, request)
    messageTip('用户角色已更新，旧会话已失效', 'success')
  } catch (error) {
    messageTip(getUserAuthorizationErrorMessage(error, '用户角色保存失败'), 'error')
    await refreshOnConflict(error)
  } finally {
    roleSubmitting.value = false
  }
}
async function savePermissions(request: UpdateUserPermissionsRequest) {
  const id = targetUserId.value
  if (id === null || permissionSubmitting.value || !canUpdatePermissions.value) return
  permissionSubmitting.value = true
  try {
    authorization.value = await updateUserPermissions(id, request)
    messageTip('用户个人权限已更新，旧会话已失效', 'success')
  } catch (error) {
    messageTip(getUserAuthorizationErrorMessage(error, '用户个人权限保存失败'), 'error')
    await refreshOnConflict(error)
  } finally {
    permissionSubmitting.value = false
  }
}

async function loadManagedSessions() {
  const id = targetUserId.value
  if (id === null || !canViewSessions.value) return
  const requestId = ++sessionRequestId
  sessionAbortController?.abort()
  const controller = new AbortController()
  sessionAbortController = controller
  sessionLoading.value = true
  sessionError.value = ''
  try {
    const result = await fetchManagedUserSessions(id, controller.signal)
    if (requestId !== sessionRequestId || controller.signal.aborted) return
    managedSessions.value = result
  } catch (error) {
    if (requestId !== sessionRequestId || controller.signal.aborted) return
    managedSessions.value = null
    sessionError.value = managedSessionError(error, '加载用户会话失败')
  } finally {
    if (requestId === sessionRequestId) {
      sessionLoading.value = false
      sessionAbortController = null
    }
  }
}
async function revokeManagedSession(session: UserSessionItem, reason: string) {
  const id = targetUserId.value
  const collection = managedSessions.value
  if (
    id === null ||
    !collection ||
    sessionBusyAction.value ||
    !canRevokeSessions.value ||
    !session.allowedActions.includes(USER_SESSION_ITEM_ACTION.REVOKE)
  )
    return
  try {
    await messageConfirm(`确认撤销“${session.deviceSummary}”会话吗？`)
  } catch {
    return
  }
  sessionBusyAction.value = session.id
  try {
    managedSessions.value = await revokeManagedUserSession(id, session.id, {
      sessionRevision: collection.sessionRevision,
      reason,
    })
    messageTip('用户会话已撤销', 'success')
  } catch (error) {
    await handleSessionError(error)
  } finally {
    sessionBusyAction.value = ''
  }
}
async function revokeAllManagedSessions(reason: string) {
  const id = targetUserId.value
  const collection = managedSessions.value
  if (
    id === null ||
    !collection ||
    sessionBusyAction.value ||
    !canRevokeSessions.value ||
    !collection.allowedActions.includes(USER_SESSION_ACTION.REVOKE_ALL)
  )
    return
  try {
    await messageConfirm('确认撤销该用户的全部会话吗？')
  } catch {
    return
  }
  sessionBusyAction.value = 'all'
  try {
    managedSessions.value = await revokeAllManagedUserSessions(id, {
      sessionRevision: collection.sessionRevision,
      reason,
    })
    messageTip('用户全部会话已撤销', 'success')
  } catch (error) {
    await handleSessionError(error)
  } finally {
    sessionBusyAction.value = ''
  }
}
async function handleSessionError(error: unknown) {
  sessionError.value = managedSessionError(error, '用户会话撤销失败')
  messageTip(sessionError.value, 'error')
  if (isRefreshableSessionError(error)) await loadManagedSessions()
}
function managedSessionError(error: unknown, fallback: string): string {
  if (!(error instanceof ApiError)) return fallback
  if (error.code === API_ERROR_CODE.ACCESS_DENIED)
    return '不能查看或撤销本人、同级、上级、范围外或受保护账号的会话'
  return getSessionCommandErrorMessage(error, fallback)
}

watch(
  targetUserId,
  (id) => {
    detail.value = null
    authorization.value = null
    authorizationLoadError.value = ''
    loadError.value = ''
    managedSessions.value = null
    sessionError.value = ''
    sessionAbortController?.abort()
    sessionRequestId += 1
    if (id !== null && String(authStore.currentUser?.id) === String(id)) {
      void router.replace({ name: 'profile' })
      return
    }
    if (id !== null) void loadDetail()
  },
  { immediate: true },
)
onBeforeUnmount(() => {
  loadAbortController?.abort()
  sessionAbortController?.abort()
})
</script>
