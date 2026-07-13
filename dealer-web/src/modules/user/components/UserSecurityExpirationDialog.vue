<template>
  <Dialog :open="open" @update:open="changeOpen">
    <DialogContent class="sm:max-w-md">
      <DialogHeader>
        <DialogTitle>账号安全到期</DialogTitle>
        <DialogDescription
          >账号和凭证到期时间均可独立设置或清除，过去时间会立即失效。</DialogDescription
        >
      </DialogHeader>
      <div class="space-y-4">
        <div class="space-y-2">
          <Label for="managed-account-expiration">账号到期时间</Label>
          <Input
            id="managed-account-expiration"
            v-model="formAccountExpiresAt"
            type="datetime-local"
          />
          <p class="text-xs text-muted-foreground">
            留空表示清除账号到期时间；过去时间会立即使账号到期。
          </p>
        </div>
        <div class="space-y-2">
          <Label for="managed-credential-expiration">凭证到期时间</Label>
          <Input
            id="managed-credential-expiration"
            v-model="formCredentialExpiresAt"
            type="datetime-local"
          />
          <p class="text-xs text-muted-foreground">
            留空表示清除凭证到期时间；过去时间会立即使凭证到期。
          </p>
        </div>
        <div class="space-y-2">
          <Label for="managed-security-expiration-reason">变更原因</Label>
          <Textarea
            id="managed-security-expiration-reason"
            v-model="reason"
            :rows="3"
            maxlength="500"
          />
        </div>
        <p v-if="errorMessage" class="text-sm text-destructive">{{ errorMessage }}</p>
      </div>
      <DialogFooter>
        <Button variant="outline" :disabled="submitting" @click="emit('update:open', false)"
          >取消</Button
        >
        <Button :disabled="submitting" @click="submit">{{
          submitting ? '提交中...' : '保存安全设置'
        }}</Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import type { ChangeManagedUserSecurityExpirationRequest } from '@/modules/user/model/user.types'

const props = defineProps<{
  open: boolean
  accountVersion: number
  accountExpiresAt?: string | null
  credentialExpiresAt?: string | null
  submitting?: boolean
}>()
const emit = defineEmits<{
  'update:open': [open: boolean]
  submit: [request: ChangeManagedUserSecurityExpirationRequest]
}>()
const formAccountExpiresAt = ref('')
const formCredentialExpiresAt = ref('')
const reason = ref('')
const errorMessage = ref('')

watch(
  () => props.open,
  (open) => {
    if (!open) return
    formAccountExpiresAt.value = toLocalInput(props.accountExpiresAt)
    formCredentialExpiresAt.value = toLocalInput(props.credentialExpiresAt)
    reason.value = ''
    errorMessage.value = ''
  },
)

function changeOpen(open: boolean) {
  if (!props.submitting) emit('update:open', open)
}
function submit() {
  if (!reason.value.trim()) {
    errorMessage.value = '请填写变更原因'
    return
  }
  emit('submit', {
    accountVersion: props.accountVersion,
    accountExpiresAt: toOffsetIso(formAccountExpiresAt.value),
    credentialExpiresAt: toOffsetIso(formCredentialExpiresAt.value),
    reason: reason.value.trim(),
  })
}

function toOffsetIso(value: string): string | null {
  return value ? new Date(value).toISOString() : null
}
function toLocalInput(value?: string | null): string {
  if (!value) return ''
  const date = new Date(value)
  return new Date(date.getTime() - date.getTimezoneOffset() * 60_000).toISOString().slice(0, 16)
}
</script>
