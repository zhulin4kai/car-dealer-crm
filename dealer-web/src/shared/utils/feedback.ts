import { ElMessage, ElMessageBox } from 'element-plus'
import type { Action } from 'element-plus'

import type { MessageType } from '@/shared/types/common'

export function messageTip(message: string, type: MessageType): void {
  ElMessage({
    showClose: true,
    center: true,
    duration: 3000,
    message,
    type,
  })
}

export function messageConfirm(message: string): Promise<Action> {
  return ElMessageBox.confirm(message, '系统提醒', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
}
