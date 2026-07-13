<template>
  <Dialog :open="open" @update:open="changeOpen">
    <DialogContent class="sm:max-w-md">
      <DialogHeader><DialogTitle>调整账号状态</DialogTitle><DialogDescription>仅可选择服务端返回的允许操作。</DialogDescription></DialogHeader>
      <div class="space-y-4">
        <select v-model="command" aria-label="状态操作" class="form-select">
          <option value="">请选择操作</option>
          <option v-for="item in commands" :key="item.command" :value="item.command" :disabled="Boolean(item.disabledReason)">{{ item.label }}{{ item.disabledReason ? `（${item.disabledReason}）` : '' }}</option>
        </select>
        <Textarea v-model="reason" aria-label="状态调整原因" :rows="4" placeholder="请输入调整原因" />
        <p v-if="errorMessage" class="text-sm text-destructive">{{ errorMessage }}</p>
      </div>
      <DialogFooter><Button variant="outline" :disabled="submitting" @click="emit('update:open', false)">取消</Button><Button :disabled="submitting" @click="submit">{{ submitting ? '提交中...' : '确认' }}</Button></DialogFooter>
    </DialogContent>
  </Dialog>
</template>
<script setup lang="ts">
import { ref, watch } from 'vue'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Textarea } from '@/components/ui/textarea'
import type { ChangeManagedUserStatusRequest, UserStatusCommand, UserStatusCommandOption } from '@/modules/user/model/user.types'
const props = defineProps<{ open: boolean; accountVersion: number; commands: UserStatusCommandOption[]; submitting?: boolean }>()
const emit = defineEmits<{ 'update:open': [open: boolean]; submit: [request: ChangeManagedUserStatusRequest] }>()
const command = ref<UserStatusCommand | ''>('')
const reason = ref('')
const errorMessage = ref('')
watch(() => props.open, (open) => { if (open) { command.value = ''; reason.value = ''; errorMessage.value = '' } })
function changeOpen(open: boolean) { if (!props.submitting) emit('update:open', open) }
function submit() { if (!command.value || !reason.value.trim()) { errorMessage.value = '请选择操作并填写原因'; return } emit('submit', { accountVersion: props.accountVersion, command: command.value, reason: reason.value.trim() }) }
</script>
<style scoped>.form-select { height: 2.25rem; width: 100%; border-radius: .375rem; border: 1px solid var(--crm-border-light); background: var(--crm-bg-panel); padding: 0 .75rem; font-size: .875rem; }</style>
