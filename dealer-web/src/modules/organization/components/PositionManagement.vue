<template>
  <Dialog :open="open" @update:open="handleOpenChange">
    <DialogContent class="max-h-[85vh] overflow-y-auto sm:max-w-3xl">
      <DialogHeader>
        <DialogTitle>岗位目录</DialogTitle>
        <DialogDescription>岗位描述员工职责，不等同于系统权限角色。</DialogDescription>
      </DialogHeader>

      <div class="rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>编码</TableHead>
              <TableHead>名称</TableHead>
              <TableHead>级别</TableHead>
              <TableHead>状态</TableHead>
              <TableHead class="text-right">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-if="loading">
              <TableCell colspan="5" class="h-20 text-center text-muted-foreground"
                >加载中...</TableCell
              >
            </TableRow>
            <TableRow v-else-if="!positions.length">
              <TableCell colspan="5" class="h-20 text-center text-muted-foreground"
                >暂无岗位</TableCell
              >
            </TableRow>
            <template v-else>
              <TableRow v-for="position in positions" :key="position.id">
                <TableCell class="font-mono text-xs">{{ position.code }}</TableCell>
                <TableCell>{{ position.name }}</TableCell>
                <TableCell>{{ position.positionLevel }}</TableCell>
                <TableCell>
                  <StatusBadge
                    :label="position.enabled ? '启用' : '停用'"
                    :tone="position.enabled ? 'success' : 'muted'"
                  />
                </TableCell>
                <TableCell>
                  <div class="flex justify-end gap-2">
                    <Button v-if="canEdit" size="sm" variant="outline" @click="startEdit(position)">
                      编辑
                    </Button>
                    <Button
                      v-if="canChangeStatus"
                      size="sm"
                      :variant="position.enabled ? 'destructive' : 'outline'"
                      @click="openStatusDialog(position)"
                    >
                      {{ position.enabled ? '停用' : '启用' }}
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            </template>
          </TableBody>
        </Table>
      </div>

      <form
        v-if="canCreate || editingPosition"
        class="rounded-lg border p-4"
        @submit.prevent="submitForm"
      >
        <div class="mb-3 font-medium">{{ editingPosition ? '编辑岗位' : '新增岗位' }}</div>
        <div class="grid gap-3 sm:grid-cols-2">
          <div class="space-y-2">
            <Label for="position-code">岗位编码</Label>
            <Input id="position-code" v-model="code" :disabled="Boolean(editingPosition)" />
            <p v-if="errors.code" class="text-sm text-destructive">{{ errors.code }}</p>
          </div>
          <div class="space-y-2">
            <Label for="position-name">岗位名称</Label>
            <Input id="position-name" v-model="name" />
            <p v-if="errors.name" class="text-sm text-destructive">{{ errors.name }}</p>
          </div>
          <div class="space-y-2">
            <Label for="position-level">岗位级别</Label>
            <Input id="position-level" v-model="positionLevel" type="number" min="1" />
            <p v-if="errors.positionLevel" class="text-sm text-destructive">
              {{ errors.positionLevel }}
            </p>
          </div>
          <div class="space-y-2">
            <Label for="position-description">岗位说明</Label>
            <Input id="position-description" v-model="description" />
          </div>
        </div>
        <div class="mt-4 flex justify-end gap-2">
          <Button v-if="editingPosition" type="button" variant="outline" @click="resetEditor"
            >取消编辑</Button
          >
          <Button type="submit" :disabled="submitting">{{
            submitting ? '保存中...' : '保存岗位'
          }}</Button>
        </div>
      </form>

      <DialogFooter>
        <Button variant="outline" :disabled="submitting" @click="handleOpenChange(false)">
          关闭
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>

  <Dialog :open="Boolean(statusPosition)" @update:open="!$event && closeStatusDialog()">
    <DialogContent class="sm:max-w-md">
      <DialogHeader>
        <DialogTitle>{{ statusPosition?.enabled ? '停用岗位' : '启用岗位' }}</DialogTitle>
        <DialogDescription>请输入本次状态调整原因，系统将记录版本与审计历史。</DialogDescription>
      </DialogHeader>
      <div class="space-y-2">
        <Label for="position-status-reason">调整原因</Label>
        <Textarea id="position-status-reason" v-model="statusReason" :rows="4" />
      </div>
      <DialogFooter>
        <Button variant="outline" :disabled="submitting" @click="closeStatusDialog">取消</Button>
        <Button :disabled="!statusReason.trim() || submitting" @click="submitStatus">确认</Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { toTypedSchema } from '@vee-validate/zod'
import { useForm } from 'vee-validate'
import { ref, watch } from 'vue'
import * as z from 'zod'

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
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Textarea } from '@/components/ui/textarea'
import type {
  Position,
  PositionFormSubmission,
} from '@/modules/organization/model/organization.types'
import StatusBadge from '@/shared/ui/StatusBadge.vue'

interface PositionFormValues {
  code: string
  name: string
  description: string
  positionLevel: number
}

const props = withDefaults(
  defineProps<{
    open: boolean
    positions: Position[]
    loading?: boolean
    submitting?: boolean
    canCreate?: boolean
    canEdit?: boolean
    canChangeStatus?: boolean
  }>(),
  {
    loading: false,
    submitting: false,
    canCreate: false,
    canEdit: false,
    canChangeStatus: false,
  },
)

const emit = defineEmits<{
  'update:open': [open: boolean]
  submit: [submission: PositionFormSubmission]
  'change-status': [payload: { position: Position; reason: string }]
}>()

const editingPosition = ref<Position | null>(null)
const statusPosition = ref<Position | null>(null)
const statusReason = ref('')
const schema = toTypedSchema(
  z.object({
    code: z.string().trim().min(1, '请输入岗位编码').max(50),
    name: z.string().trim().min(1, '请输入岗位名称').max(100),
    description: z.string().max(500),
    positionLevel: z.coerce.number().int().min(1, '岗位级别必须大于 0'),
  }),
)
const { defineField, errors, handleSubmit, resetForm } = useForm<PositionFormValues>({
  validationSchema: schema,
  initialValues: { code: '', name: '', description: '', positionLevel: 1 },
})
const [code] = defineField('code')
const [name] = defineField('name')
const [description] = defineField('description')
const [positionLevel] = defineField('positionLevel')

const submitForm = handleSubmit((values) => {
  const common = {
    name: values.name.trim(),
    description: values.description.trim() || undefined,
    positionLevel: Number(values.positionLevel),
  }
  if (editingPosition.value) {
    emit('submit', {
      mode: 'update',
      id: editingPosition.value.id,
      request: { ...common, expectedVersion: editingPosition.value.version },
    })
    return
  }
  emit('submit', {
    mode: 'create',
    request: { code: values.code.trim(), ...common },
  })
})

function startEdit(position: Position): void {
  editingPosition.value = position
  resetForm({
    values: {
      code: position.code,
      name: position.name,
      description: position.description ?? '',
      positionLevel: position.positionLevel,
    },
  })
}

function resetEditor(): void {
  editingPosition.value = null
  resetForm({ values: { code: '', name: '', description: '', positionLevel: 1 } })
}

function openStatusDialog(position: Position): void {
  statusPosition.value = position
  statusReason.value = ''
}

function closeStatusDialog(): void {
  if (props.submitting) return
  statusPosition.value = null
  statusReason.value = ''
}

function handleOpenChange(open: boolean): void {
  if (!open && props.submitting) return
  emit('update:open', open)
}

function submitStatus(): void {
  if (!statusPosition.value || !statusReason.value.trim()) return
  emit('change-status', { position: statusPosition.value, reason: statusReason.value.trim() })
}

watch(
  () => props.open,
  (open) => {
    if (!open) {
      resetEditor()
      closeStatusDialog()
    }
  },
)
</script>
