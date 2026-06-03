<script lang="ts" setup>
import { onMounted, ref } from 'vue'

import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { registerConfirmDialog } from './confirmDialogService'

const open = ref(false)
const title = ref('')
const description = ref('')
let resolveFn: ((value: boolean) => void) | null = null

function openConfirmDialog(options: { title: string; description: string }): Promise<boolean> {
  title.value = options.title
  description.value = options.description
  open.value = true
  return new Promise<boolean>((resolve) => {
    resolveFn = resolve
  })
}

function handleConfirm() {
  open.value = false
  resolveFn?.(true)
  resolveFn = null
}

function handleCancel() {
  open.value = false
  resolveFn?.(false)
  resolveFn = null
}

onMounted(() => {
  registerConfirmDialog(openConfirmDialog)
})
</script>

<template>
  <AlertDialog v-model:open="open">
    <AlertDialogContent>
      <AlertDialogHeader>
        <AlertDialogTitle>{{ title }}</AlertDialogTitle>
        <AlertDialogDescription>{{ description }}</AlertDialogDescription>
      </AlertDialogHeader>
      <AlertDialogFooter>
        <AlertDialogCancel @click="handleCancel">取消</AlertDialogCancel>
        <AlertDialogAction @click="handleConfirm">确定</AlertDialogAction>
      </AlertDialogFooter>
    </AlertDialogContent>
  </AlertDialog>
</template>
