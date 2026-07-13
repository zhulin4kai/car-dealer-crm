<template>
  <Dialog :open="open" @update:open="changeOpen">
    <DialogContent class="sm:max-w-md">
      <DialogHeader>
        <DialogTitle>修改登录账号</DialogTitle>
        <DialogDescription>保存后旧登录账号和全部旧会话立即失效。</DialogDescription>
      </DialogHeader>
      <div class="space-y-4">
        <div class="space-y-2">
          <Label for="managed-login-account">新登录账号</Label>
          <Input id="managed-login-account" v-model="loginAct" autocomplete="off" maxlength="32" />
          <p class="text-xs text-muted-foreground">仅支持字母、数字及 . _ @ -，长度 3—32 位。</p>
        </div>
        <div class="space-y-2">
          <Label for="managed-login-account-reason">变更原因</Label>
          <Textarea id="managed-login-account-reason" v-model="reason" :rows="3" maxlength="500" />
        </div>
        <p v-if="errorMessage" class="text-sm text-destructive">{{ errorMessage }}</p>
      </div>
      <DialogFooter>
        <Button variant="outline" :disabled="submitting" @click="emit('update:open', false)">取消</Button>
        <Button :disabled="submitting" @click="submit">{{ submitting ? '提交中...' : '确认修改' }}</Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import type { ChangeManagedUserLoginAccountRequest } from '@/modules/user/model/user.types'

const props = defineProps<{ open: boolean; accountVersion: number; currentLoginAct: string; submitting?: boolean }>()
const emit = defineEmits<{ 'update:open': [open: boolean]; submit: [request: ChangeManagedUserLoginAccountRequest] }>()
const loginAct = ref('')
const reason = ref('')
const errorMessage = ref('')

watch(() => props.open, (open) => {
  if (!open) return
  loginAct.value = props.currentLoginAct
  reason.value = ''
  errorMessage.value = ''
})

function changeOpen(open: boolean) { if (!props.submitting) emit('update:open', open) }
function submit() {
  const normalized = loginAct.value.trim().toLowerCase()
  if (!/^[a-z0-9._@-]{3,32}$/.test(normalized)) { errorMessage.value = '登录账号格式不正确'; return }
  if (normalized === props.currentLoginAct.toLowerCase()) { errorMessage.value = '新登录账号必须与当前账号不同'; return }
  if (!reason.value.trim()) { errorMessage.value = '请填写变更原因'; return }
  emit('submit', { accountVersion: props.accountVersion, loginAct: normalized, reason: reason.value.trim() })
}
</script>
