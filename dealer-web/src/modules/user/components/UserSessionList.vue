<template>
  <section class="space-y-4">
    <div class="flex flex-wrap items-start justify-between gap-3">
      <div>
        <h3 class="font-medium">登录会话</h3>
        <p class="text-sm text-muted-foreground">
          仅展示服务端生成的脱敏设备、客户端和网络摘要，不展示 Token 或完整网络信息。
        </p>
      </div>
      <Button
        variant="outline"
        size="sm"
        :disabled="loading || Boolean(busyAction)"
        @click="emit('retry')"
      >
        刷新
      </Button>
    </div>

    <Alert v-if="errorMessage" variant="destructive">
      <AlertDescription>{{ errorMessage }}</AlertDescription>
    </Alert>
    <Alert v-else-if="collection && !mutationAllowed">
      <AlertDescription>{{ disabledReason || '当前管理范围只允许查看会话。' }}</AlertDescription>
    </Alert>
    <div v-if="loading" class="py-8 text-center text-muted-foreground">加载会话...</div>
    <div v-else-if="!collection" class="py-8 text-center text-muted-foreground">暂无会话信息</div>
    <template v-else>
      <div v-if="requireReason" class="space-y-2 rounded-lg border p-3">
        <Label for="managed-session-reason">撤销原因</Label>
        <Textarea
          id="managed-session-reason"
          v-model="reason"
          :rows="2"
          placeholder="管理者撤销下属会话必须记录原因"
        />
      </div>

      <div class="flex flex-wrap justify-end gap-2">
        <Button
          v-if="collection.allowedActions.includes(USER_SESSION_ACTION.REVOKE_OTHERS)"
          variant="outline"
          :disabled="isActionDisabled"
          @click="emit('revoke-others', actionReason('others'))"
        >
          {{ busyAction === 'others' ? '撤销中...' : '撤销其他会话' }}
        </Button>
        <Button
          v-if="collection.allowedActions.includes(USER_SESSION_ACTION.REVOKE_ALL)"
          variant="destructive"
          :disabled="isActionDisabled"
          @click="emit('revoke-all', actionReason('all'))"
        >
          {{ busyAction === 'all' ? '撤销中...' : '撤销全部会话' }}
        </Button>
      </div>

      <div
        v-if="!collection.sessions.length"
        class="rounded-lg border py-8 text-center text-muted-foreground"
      >
        当前没有有效会话
      </div>
      <div v-else class="space-y-3">
        <article
          v-for="session in collection.sessions"
          :key="session.id"
          class="rounded-lg border p-4"
        >
          <div class="flex flex-wrap items-start justify-between gap-3">
            <div class="space-y-2">
              <div class="flex flex-wrap items-center gap-2">
                <span class="font-medium">{{ session.deviceSummary }}</span>
                <Badge v-if="session.current">当前会话</Badge>
                <Badge v-if="session.rememberMe" variant="outline">记住登录</Badge>
              </div>
              <div class="grid gap-1 text-sm text-muted-foreground sm:grid-cols-2">
                <span v-if="session.clientSummary">客户端：{{ session.clientSummary }}</span>
                <span v-if="session.networkSummary">网络：{{ session.networkSummary }}</span>
                <span>登录：{{ formatDateTime(session.loginTime) }}</span>
                <span>最近活动：{{ formatDateTime(session.lastActivityTime) }}</span>
                <span>最晚有效：{{ formatDateTime(session.expiresAt) }}</span>
              </div>
              <p
                v-if="session.unavailableReasons[USER_SESSION_ITEM_ACTION.REVOKE]"
                class="text-xs text-amber-700"
              >
                {{ session.unavailableReasons[USER_SESSION_ITEM_ACTION.REVOKE] }}
              </p>
            </div>
            <Button
              v-if="session.allowedActions.includes(USER_SESSION_ITEM_ACTION.REVOKE)"
              :variant="session.current ? 'destructive' : 'outline'"
              size="sm"
              :disabled="isActionDisabled"
              @click="emit('revoke', session, actionReason(session.current ? 'current' : 'one'))"
            >
              {{
                busyAction === session.id
                  ? '撤销中...'
                  : session.current
                    ? '退出当前会话'
                    : '撤销会话'
              }}
            </Button>
          </div>
        </article>
      </div>
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import {
  USER_SESSION_ACTION,
  USER_SESSION_ITEM_ACTION,
  type UserSessionCollection,
  type UserSessionItem,
} from '@/modules/user/model/user-session.types'
import { formatDateTime } from '@/shared/utils/display-format'

const props = withDefaults(
  defineProps<{
    collection?: UserSessionCollection | null
    loading?: boolean
    errorMessage?: string
    busyAction?: string
    requireReason?: boolean
    mutationAllowed?: boolean
    disabledReason?: string
  }>(),
  {
    collection: null,
    loading: false,
    errorMessage: '',
    busyAction: '',
    requireReason: false,
    mutationAllowed: true,
    disabledReason: '',
  },
)

const emit = defineEmits<{
  retry: []
  revoke: [session: UserSessionItem, reason: string]
  'revoke-others': [reason: string]
  'revoke-all': [reason: string]
}>()

const reason = ref('')
const isActionDisabled = computed(
  () =>
    !props.mutationAllowed ||
    Boolean(props.busyAction) ||
    (props.requireReason && !reason.value.trim()),
)

function actionReason(action: 'one' | 'current' | 'others' | 'all'): string {
  if (props.requireReason) return reason.value.trim()
  switch (action) {
    case 'current':
      return '用户主动退出当前会话'
    case 'others':
      return '用户主动撤销其他会话'
    case 'all':
      return '用户主动撤销全部会话'
    default:
      return '用户主动撤销指定会话'
  }
}

watch(
  () => props.collection?.sessionRevision,
  () => {
    reason.value = ''
  },
)
</script>
