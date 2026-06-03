import { toast } from 'vue-sonner'

import type { MessageType } from '@/shared/types/common'

import { openConfirmDialog } from '@/shared/ui/confirmDialogService'

export function messageTip(message: string, type: MessageType): void {
  switch (type) {
    case 'success':
      toast.success(message)
      break
    case 'error':
      toast.error(message)
      break
    case 'warning':
      toast.warning(message)
      break
    case 'info':
      toast.info(message)
      break
    default:
      toast(message)
  }
}

/**
 * Show a confirm dialog. Resolves on confirm, rejects on cancel.
 * This matches the original ElMessageBox.confirm behavior where
 * callers use `const confirmed = await messageConfirm(...)` and
 * check `if (confirmed === 'confirm')`.
 */
export async function messageConfirm(message: string): Promise<'confirm'> {
  const result = await openConfirmDialog({ title: '系统提醒', description: message })
  if (result) {
    return 'confirm'
  }
  throw new Error('cancel')
}
