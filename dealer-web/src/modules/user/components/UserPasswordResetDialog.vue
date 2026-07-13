<template>
  <Dialog :open="open" @update:open="changeOpen">
    <DialogContent class="sm:max-w-md">
      <DialogHeader><DialogTitle>重置用户密码</DialogTitle><DialogDescription>系统只返回投递状态，不会在页面展示临时密码或重置凭证。</DialogDescription></DialogHeader>
      <div class="space-y-2"><Label for="reset-reason">重置原因</Label><Textarea id="reset-reason" v-model="reason" :rows="4" /><p v-if="errorMessage" class="text-sm text-destructive">{{ errorMessage }}</p></div>
      <DialogFooter><Button variant="outline" :disabled="submitting" @click="emit('update:open', false)">取消</Button><Button :disabled="submitting" @click="submit">{{ submitting ? '提交中...' : '发送重置通知' }}</Button></DialogFooter>
    </DialogContent>
  </Dialog>
</template>
<script setup lang="ts">
import { ref, watch } from 'vue'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import type { ResetManagedUserPasswordRequest } from '@/modules/user/model/user.types'
const props = defineProps<{ open: boolean; accountVersion: number; submitting?: boolean }>()
const emit = defineEmits<{ 'update:open': [open: boolean]; submit: [request: ResetManagedUserPasswordRequest] }>()
const reason = ref(''); const errorMessage = ref('')
watch(() => props.open, (open) => { if (open) { reason.value = ''; errorMessage.value = '' } })
function changeOpen(open: boolean) { if (!props.submitting) emit('update:open', open) }
function submit() { if (!reason.value.trim()) { errorMessage.value = '请输入重置原因'; return } emit('submit', { accountVersion: props.accountVersion, reason: reason.value.trim() }) }
</script>
