<template>
  <Dialog :open="open" @update:open="changeOpen">
    <DialogContent class="sm:max-w-md">
      <DialogHeader>
        <DialogTitle>重新邀请用户</DialogTitle>
        <DialogDescription>
          旧邀请凭证将失效。页面只展示服务端返回的投递状态，不展示原始凭证。
        </DialogDescription>
      </DialogHeader>
      <div v-if="result" class="space-y-2 rounded-md border p-3 text-sm" aria-live="polite">
        <p>重新邀请凭证已排队</p>
        <p class="text-muted-foreground">安全通知服务将在事务提交后异步投递。</p>
      </div>
      <div v-else class="space-y-2">
        <Label for="reinvite-reason">重新邀请原因</Label>
        <Textarea id="reinvite-reason" v-model="reason" :rows="4" />
        <p v-if="errorMessage" class="text-sm text-destructive">{{ errorMessage }}</p>
      </div>
      <DialogFooter>
        <Button variant="outline" :disabled="submitting" @click="emit('update:open', false)">
          {{ result ? '关闭' : '取消' }}
        </Button>
        <Button v-if="!result" :disabled="submitting" @click="submit">
          {{ submitting ? '提交中...' : '提交重新邀请' }}
        </Button>
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
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import type {
  ManagedCredentialDeliveryResult,
  ReinviteManagedUserRequest,
} from '@/modules/user/model/credential.types'

const props = defineProps<{
  open: boolean
  accountVersion: number
  submitting?: boolean
  result?: ManagedCredentialDeliveryResult | null
}>()
const emit = defineEmits<{
  'update:open': [open: boolean]
  submit: [request: ReinviteManagedUserRequest]
}>()
const reason = ref('')
const errorMessage = ref('')

watch(
  () => props.open,
  (open) => {
    if (open) {
      reason.value = ''
      errorMessage.value = ''
    }
  },
)

function changeOpen(open: boolean): void {
  if (!props.submitting) emit('update:open', open)
}

function submit(): void {
  if (!reason.value.trim()) {
    errorMessage.value = '请输入重新邀请原因'
    return
  }
  emit('submit', { accountVersion: props.accountVersion, reason: reason.value.trim() })
}
</script>
