<template>
  <div class="crm-data-page space-y-4">
    <section v-if="loading" class="crm-panel py-20 text-center text-muted-foreground">
      加载个人资料...
    </section>
    <section v-else-if="loadError" class="crm-panel space-y-4 py-20 text-center">
      <p class="text-destructive">{{ loadError }}</p>
      <Button variant="outline" @click="loadProfile">重新加载</Button>
    </section>
    <template v-else-if="profile">
      <Card>
        <CardHeader>
          <div class="flex items-center gap-4">
            <img
              v-if="profile.avatarUrl"
              :src="profile.avatarUrl"
              alt="当前用户头像"
              class="h-14 w-14 rounded-full object-cover"
            />
            <div
              v-else
              class="flex h-14 w-14 items-center justify-center rounded-full bg-[var(--crm-primary-light)] text-lg font-semibold text-[var(--crm-primary)]"
            >
              {{ profile.name.charAt(0) || '用' }}
            </div>
            <div>
              <CardTitle>{{ profile.name }}</CardTitle>
              <CardDescription>{{ profile.loginAct }} · 个人中心</CardDescription>
            </div>
          </div>
        </CardHeader>
      </Card>

      <div class="grid gap-4 xl:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>个人资料</CardTitle>
            <CardDescription>管理员和普通用户都只能修改本人的非授权资料。</CardDescription>
          </CardHeader>
          <CardContent>
            <ProfileForm :profile="profile" :submitting="profileSubmitting" @save="saveProfile" />
          </CardContent>
        </Card>

        <Card id="change-password">
          <CardHeader>
            <CardTitle>修改密码</CardTitle>
            <CardDescription>必须验证当前密码；成功后所有旧会话失效。</CardDescription>
          </CardHeader>
          <CardContent>
            <ChangeOwnPasswordForm
              :submitting="passwordSubmitting"
              :error-message="passwordError"
              :reset-key="passwordResetKey"
              @save="savePassword"
            />
          </CardContent>
        </Card>
      </div>


      <Card>
        <CardHeader>
          <CardTitle>联系方式验证</CardTitle>
          <CardDescription>
            已验证的手机或邮箱可用于本人找回密码；修改联系方式后必须重新验证。
          </CardDescription>
        </CardHeader>
        <CardContent class="grid gap-3 md:grid-cols-2">
          <div class="flex items-center justify-between gap-3 rounded-lg border p-3">
            <div class="min-w-0 text-sm">
              <div class="font-medium">手机</div>
              <div class="truncate text-muted-foreground">{{ profile.phone || '未设置' }}</div>
              <div :class="profile.phoneVerified ? 'text-emerald-600' : 'text-amber-600'">
                {{ profile.phoneVerified ? '已验证' : '未验证' }}
              </div>
            </div>
            <Button
              variant="outline"
              :disabled="!profile.phone || profile.phoneVerified || verificationBusy !== ''"
              @click="requestVerification('PHONE')"
            >
              {{ verificationBusy === 'PHONE' ? '发送中...' : '发送验证' }}
            </Button>
          </div>
          <div class="flex items-center justify-between gap-3 rounded-lg border p-3">
            <div class="min-w-0 text-sm">
              <div class="font-medium">邮箱</div>
              <div class="truncate text-muted-foreground">{{ profile.email || '未设置' }}</div>
              <div :class="profile.emailVerified ? 'text-emerald-600' : 'text-amber-600'">
                {{ profile.emailVerified ? '已验证' : '未验证' }}
              </div>
            </div>
            <Button
              variant="outline"
              :disabled="!profile.email || profile.emailVerified || verificationBusy !== ''"
              @click="requestVerification('EMAIL')"
            >
              {{ verificationBusy === 'EMAIL' ? '发送中...' : '发送验证' }}
            </Button>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>任职与授权（只读）</CardTitle>
          <CardDescription>
            本人不能在个人中心修改组织、岗位、管理者、角色、权限或数据范围。
          </CardDescription>
        </CardHeader>
        <CardContent class="space-y-5">
          <div class="grid gap-3 text-sm sm:grid-cols-2 lg:grid-cols-4">
            <div>员工编号：{{ profile.employeeNo || '未设置' }}</div>
            <div>组织：{{ profile.organizationName || '未设置' }}</div>
            <div>岗位：{{ profile.positionName || '未设置' }}</div>
            <div>直属管理者：{{ profile.managerName || '未设置' }}</div>
          </div>
          <div class="space-y-2">
            <h3 class="font-medium">当前角色</h3>
            <p v-if="!profile.roles.length" class="text-sm text-muted-foreground">暂无角色</p>
            <div v-else class="flex flex-wrap gap-2">
              <Badge v-for="role in profile.roles" :key="role.id" variant="outline">
                {{ role.name }}
                <template v-if="role.sourceDescription"> · {{ role.sourceDescription }}</template>
              </Badge>
            </div>
          </div>
          <div class="space-y-2">
            <h3 class="font-medium">有效权限及来源</h3>
            <p v-if="!profile.effectivePermissions.length" class="text-sm text-muted-foreground">
              暂无有效权限
            </p>
            <div v-else class="max-h-72 space-y-2 overflow-y-auto rounded-lg border p-3">
              <div
                v-for="permission in profile.effectivePermissions"
                :key="permission.permissionCode"
                class="space-y-2 rounded-lg border bg-muted/20 p-3 text-sm"
              >
                <div class="flex flex-wrap items-center gap-2">
                  <span class="font-medium">{{ permission.permissionName }}</span>
                  <code class="text-xs text-muted-foreground">{{ permission.permissionCode }}</code>
                </div>
                <div
                  v-for="source in permission.sources"
                  :key="permissionSourceKey(source)"
                  class="space-y-1 rounded-md bg-background p-2 text-muted-foreground"
                >
                  <p>
                    <span class="font-medium text-foreground">来源：</span>
                    {{ permissionSourceDescription(source) }}
                  </p>
                  <p>
                    <span class="font-medium text-foreground">数据范围：</span>
                    {{ source.dataScopeLabel || source.dataScopeCode || '未声明' }}
                  </p>
                  <div v-if="source.dataScopeCode === 'CUSTOM_ORGS'" class="flex flex-wrap gap-1">
                    <span class="font-medium text-foreground">具体组织：</span>
                    <span
                      v-for="organization in source.organizations"
                      :key="organization.id"
                      class="rounded border px-1.5 py-0.5"
                    >
                      {{ organization.name }}（{{ organization.code }} · ID {{ organization.id }}）
                    </span>
                    <span v-if="!source.organizations.length" class="text-destructive">
                      未返回具体组织范围
                    </span>
                  </div>
                  <p>
                    <span class="font-medium text-foreground">有效期：</span>
                    {{ permissionSourceValidity(source) }}
                  </p>
                </div>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>会话与安全</CardTitle>
          <CardDescription>查看当前设备会话，选择撤销指定、其他或全部会话。</CardDescription>
        </CardHeader>
        <CardContent>
          <UserSessionList
            :collection="sessions"
            :loading="sessionLoading"
            :error-message="sessionError"
            :busy-action="sessionBusyAction"
            @retry="loadSessions"
            @revoke="revokeSession"
            @revoke-others="revokeOtherSessions"
            @revoke-all="revokeAllSessions"
          />
        </CardContent>
      </Card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { changeOwnPassword, requestContactVerification } from '@/modules/user/api/credential-api'
import { fetchOwnProfile, updateOwnProfile } from '@/modules/user/api/user-profile-api'
import {
  fetchOwnSessions,
  revokeAllOwnSessions,
  revokeOwnOtherSessions,
  revokeOwnSession,
} from '@/modules/user/api/user-session-api'
import ChangeOwnPasswordForm from '@/modules/user/components/ChangeOwnPasswordForm.vue'
import ProfileForm from '@/modules/user/components/ProfileForm.vue'
import UserSessionList from '@/modules/user/components/UserSessionList.vue'
import type {
  ChangeOwnPasswordRequest,
  ContactVerificationChannel,
} from '@/modules/user/model/credential.types'
import type {
  UpdateOwnProfileRequest,
  UserProfile,
  UserProfilePermissionSourceDetail,
} from '@/modules/user/model/user-profile.types'
import type {
  UserSessionCollection,
  UserSessionItem,
} from '@/modules/user/model/user-session.types'
import {
  getSessionCommandErrorMessage,
  isRefreshableSessionError,
} from '@/modules/user/model/user-session-error'
import { ApiError } from '@/shared/api/api-error'
import { API_ERROR_CODE } from '@/shared/api/error-codes'
import { formatDateTime } from '@/shared/utils/display-format'
import { messageConfirm, messageTip } from '@/shared/utils/feedback'
import { useAuthStore } from '@/stores/auth.store'
import { usePermissionStore } from '@/stores/permission.store'

const router = useRouter()
const authStore = useAuthStore()
const permissionStore = usePermissionStore()
const profile = ref<UserProfile | null>(null)
const loading = ref(false)
const loadError = ref('')
const profileSubmitting = ref(false)
const passwordSubmitting = ref(false)
const passwordError = ref('')
const passwordResetKey = ref(0)
const verificationBusy = ref<ContactVerificationChannel | ''>('')
const sessions = ref<UserSessionCollection | null>(null)
const sessionLoading = ref(false)
const sessionError = ref('')
const sessionBusyAction = ref('')
let loadAbortController: AbortController | null = null
let sessionAbortController: AbortController | null = null

function permissionSourceDescription(source: UserProfilePermissionSourceDetail): string {
  if (source.sourceType === 'ROLE') return `角色：${source.sourceName}`
  if (source.sourceType === 'PERSONAL_GRANT') return '直接个人授权'
  return '系统计算的有效权限'
}

function permissionSourceValidity(source: UserProfilePermissionSourceDetail): string {
  const effectiveFrom = source.effectiveFrom
    ? `自 ${formatPermissionSourceDateTime(source.effectiveFrom)}`
    : '当前已生效'
  const effectiveTo = source.effectiveTo
    ? `至 ${formatPermissionSourceDateTime(source.effectiveTo)}`
    : '无固定结束时间'
  return `${effectiveFrom}，${effectiveTo}`
}

function formatPermissionSourceDateTime(value: string): string {
  const match = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/.exec(value)
  if (!match) return formatDateTime(value)
  return `${Number(match[2])}月${Number(match[3])}日 ${match[4]}:${match[5]}`
}

function permissionSourceKey(source: UserProfilePermissionSourceDetail): string {
  const organizationKey = source.organizations.map((organization) => organization.id).join(',')
  return `${source.sourceType}:${source.sourceName}:${source.dataScopeCode ?? ''}:${source.effectiveFrom ?? ''}:${source.effectiveTo ?? ''}:${organizationKey}`
}

async function loadProfile(): Promise<void> {
  loadAbortController?.abort()
  const controller = new AbortController()
  loadAbortController = controller
  loading.value = true
  loadError.value = ''
  try {
    const result = await fetchOwnProfile(controller.signal)
    if (controller.signal.aborted || loadAbortController !== controller) return
    profile.value = result
  } catch (error: unknown) {
    if (controller.signal.aborted) return
    profile.value = null
    loadError.value = profileErrorMessage(error, '加载个人资料失败')
  } finally {
    if (loadAbortController === controller) {
      loading.value = false
      loadAbortController = null
    }
  }
}

async function saveProfile(request: UpdateOwnProfileRequest): Promise<void> {
  if (profileSubmitting.value) return
  profileSubmitting.value = true
  try {
    const updated = await updateOwnProfile(request)
    profile.value = updated
    authStore.applyCurrentUserProfile(updated)
    messageTip('个人资料已更新', 'success')
  } catch (error: unknown) {
    messageTip(profileErrorMessage(error, '个人资料保存失败'), 'error')
    if (isProfileVersionConflict(error)) {
      await loadProfile()
    }
  } finally {
    profileSubmitting.value = false
  }
}

async function savePassword(request: ChangeOwnPasswordRequest): Promise<void> {
  if (passwordSubmitting.value) return
  passwordSubmitting.value = true
  passwordError.value = ''
  try {
    await changeOwnPassword(request)
    passwordResetKey.value += 1
    authStore.forceLogout()
    permissionStore.clearPermissions()
    messageTip('密码已修改，请重新登录', 'success')
    await router.push('/')
  } catch {
    passwordError.value = '密码修改失败，请确认当前密码，并使用未在近期使用过的新密码。'
  } finally {
    passwordSubmitting.value = false
  }
}

async function requestVerification(channel: ContactVerificationChannel): Promise<void> {
  if (verificationBusy.value) return
  verificationBusy.value = channel
  try {
    await requestContactVerification({ channel })
    messageTip('如果当前渠道可用，系统将发送一次性验证链接', 'success')
  } catch (error: unknown) {
    messageTip(
      error instanceof ApiError && error.code === API_ERROR_CODE.CREDENTIAL_RATE_LIMITED
        ? '验证请求过于频繁，请稍后重试'
        : '验证请求发送失败，请稍后重试',
      'error',
    )
  } finally {
    verificationBusy.value = ''
  }
}

async function loadSessions(): Promise<void> {
  sessionAbortController?.abort()
  const controller = new AbortController()
  sessionAbortController = controller
  sessionLoading.value = true
  sessionError.value = ''
  try {
    const result = await fetchOwnSessions(controller.signal)
    if (controller.signal.aborted || sessionAbortController !== controller) return
    sessions.value = result
  } catch (error: unknown) {
    if (controller.signal.aborted) return
    sessions.value = null
    sessionError.value = sessionErrorMessage(error, '加载会话失败')
  } finally {
    if (sessionAbortController === controller) {
      sessionLoading.value = false
      sessionAbortController = null
    }
  }
}

async function revokeSession(session: UserSessionItem, reason: string): Promise<void> {
  const collection = sessions.value
  if (!collection || sessionBusyAction.value) return
  try {
    await messageConfirm(
      session.current ? '确认退出当前会话吗？' : `确认撤销“${session.deviceSummary}”会话吗？`,
    )
  } catch {
    return
  }
  sessionBusyAction.value = session.id
  try {
    const updated = await revokeOwnSession(session.id, {
      sessionRevision: collection.sessionRevision,
      reason,
    })
    if (session.current) {
      await finishCurrentSessionRevocation()
      return
    }
    sessions.value = updated
    messageTip('会话已撤销', 'success')
  } catch (error: unknown) {
    await handleSessionMutationError(error)
  } finally {
    sessionBusyAction.value = ''
  }
}

async function revokeOtherSessions(reason: string): Promise<void> {
  const collection = sessions.value
  if (!collection || sessionBusyAction.value) return
  try {
    await messageConfirm('确认撤销除当前会话以外的全部会话吗？')
  } catch {
    return
  }
  sessionBusyAction.value = 'others'
  try {
    sessions.value = await revokeOwnOtherSessions({
      sessionRevision: collection.sessionRevision,
      reason,
    })
    messageTip('其他会话已撤销', 'success')
  } catch (error: unknown) {
    await handleSessionMutationError(error)
  } finally {
    sessionBusyAction.value = ''
  }
}

async function revokeAllSessions(reason: string): Promise<void> {
  const collection = sessions.value
  if (!collection || sessionBusyAction.value) return
  try {
    await messageConfirm('确认撤销包括当前设备在内的全部会话吗？')
  } catch {
    return
  }
  sessionBusyAction.value = 'all'
  try {
    await revokeAllOwnSessions({ sessionRevision: collection.sessionRevision, reason })
    await finishCurrentSessionRevocation()
  } catch (error: unknown) {
    await handleSessionMutationError(error)
  } finally {
    sessionBusyAction.value = ''
  }
}

async function finishCurrentSessionRevocation(): Promise<void> {
  authStore.forceLogout()
  permissionStore.clearPermissions()
  messageTip('当前会话已退出', 'success')
  await router.push('/')
}

async function handleSessionMutationError(error: unknown): Promise<void> {
  sessionError.value = sessionErrorMessage(error, '会话撤销失败，请稍后重试')
  messageTip(sessionError.value, 'error')
  if (isRefreshableSessionError(error)) {
    await loadSessions()
  }
}

function sessionErrorMessage(error: unknown, fallback: string): string {
  if (!(error instanceof ApiError)) return fallback
  if (error.code === API_ERROR_CODE.ACCESS_DENIED) return '当前账号不能查看或撤销该会话'
  return getSessionCommandErrorMessage(error, fallback)
}

function profileErrorMessage(error: unknown, fallback: string): string {
  if (!(error instanceof ApiError)) return fallback
  if (isProfileVersionConflict(error)) return '个人资料已被更新，页面将刷新最新内容'
  if (error.code === API_ERROR_CODE.ACCESS_DENIED) return '当前会话不能维护该个人资料'
  return fallback
}

function isProfileVersionConflict(error: unknown): error is ApiError {
  return error instanceof ApiError && (
    error.httpStatus === 409
    || error.code === API_ERROR_CODE.CONFLICT
    || error.code === API_ERROR_CODE.ROLE_VERSION_CONFLICT
    || error.code === API_ERROR_CODE.PROFILE_VERSION_CONFLICT
  )
}

onMounted(() => {
  void loadProfile()
  void loadSessions()
})
onBeforeUnmount(() => {
  loadAbortController?.abort()
  sessionAbortController?.abort()
})
</script>
