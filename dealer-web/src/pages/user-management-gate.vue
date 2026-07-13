<template>
  <main class="flex min-h-screen items-center justify-center bg-background p-6">
    <section class="w-full max-w-xl space-y-5 rounded-xl border bg-card p-8 shadow-sm">
      <div class="space-y-2">
        <h1 class="text-2xl font-semibold">{{ title }}</h1>
        <p class="text-sm leading-6 text-muted-foreground">{{ description }}</p>
      </div>

      <div class="rounded-lg border bg-muted/30 p-4 text-sm">当前状态：{{ stateLabel }}</div>

      <div class="flex flex-wrap justify-end gap-3">
        <Button v-if="canBootstrap" type="button" @click="router.push({ name: 'user' })">
          创建首个普通管理员
        </Button>
        <Button
          v-if="canCompleteRecoveryChannel"
          type="button"
          @click="router.push({ name: 'profile' })"
        >
          完成恢复渠道验证
        </Button>
        <Button type="button" variant="outline" :disabled="loggingOut" @click="logout">
          {{ loggingOut ? '退出中...' : '退出登录' }}
        </Button>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { Button } from '@/components/ui/button'
import { USER_MANAGEMENT_GATE_STATE } from '@/modules/user/model/user.types'
import { useAuthStore } from '@/stores/auth.store'
import { usePermissionStore } from '@/stores/permission.store'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const permissionStore = usePermissionStore()
const loggingOut = ref(false)
const state = computed(
  () =>
    authStore.currentUser?.userManagementGateState ??
    (route.query.code === '641'
      ? USER_MANAGEMENT_GATE_STATE.PENDING_FIRST_CHANGE
      : USER_MANAGEMENT_GATE_STATE.READY),
)
const isRecoveryAccount = computed(() => authStore.currentUser?.protectedRecoveryAccount === true)
const canBootstrap = computed(
  () => isRecoveryAccount.value && state.value === USER_MANAGEMENT_GATE_STATE.UNINITIALIZED,
)
const canCompleteRecoveryChannel = computed(
  () => !isRecoveryAccount.value && state.value === USER_MANAGEMENT_GATE_STATE.PENDING_FIRST_CHANGE,
)
const stateLabel = computed(() => {
  switch (state.value) {
    case USER_MANAGEMENT_GATE_STATE.UNINITIALIZED:
      return '尚未创建首个普通管理员'
    case USER_MANAGEMENT_GATE_STATE.PENDING_FIRST_CHANGE:
      return '普通管理员待完成首次改密或恢复渠道验证'
    case USER_MANAGEMENT_GATE_STATE.DEGRADED:
      return '普通管理员入口已失效，等待独立恢复'
    default:
      return '系统已就绪，受保护恢复账号保持隔离'
  }
})
const title = computed(() => (isRecoveryAccount.value ? '受保护恢复账号' : '用户管理尚未就绪'))
const description = computed(() => {
  if (canBootstrap.value) {
    return '当前只允许创建首个具有真实任职的普通管理员，不能进入其他治理或日常业务页面。'
  }
  if (state.value === USER_MANAGEMENT_GATE_STATE.DEGRADED) {
    return '请由固定恢复账号通过独立管理员入口恢复流程恢复原有普通管理员。常规用户管理和业务页面保持关闭。'
  }
  if (isRecoveryAccount.value) {
    return '该账号只用于独立恢复流程，不能进入个人中心、常规用户治理或日常业务页面。'
  }
  return '请先完成首次改密并验证至少一个当前联系方式，再进入系统。'
})

async function logout(): Promise<void> {
  if (loggingOut.value) return
  loggingOut.value = true
  try {
    await authStore.logout()
  } catch {
    authStore.forceLogout()
  } finally {
    permissionStore.clearPermissions()
    loggingOut.value = false
    await router.push('/')
  }
}
</script>
