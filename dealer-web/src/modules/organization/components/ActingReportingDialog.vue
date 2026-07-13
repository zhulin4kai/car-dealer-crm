<template>
  <Dialog :open="open" @update:open="changeOpen">
    <DialogContent class="max-h-[90vh] overflow-y-auto sm:max-w-3xl">
      <DialogHeader>
        <DialogTitle>管理代理主管</DialogTitle>
        <DialogDescription>
          {{ employee?.employeeNo }} · {{ employee?.name }}。代理主管可同时存在多名，必须设置一年内的结束时间，不会替代直属主管。
        </DialogDescription>
      </DialogHeader>

      <div v-if="loading" class="py-10 text-center text-muted-foreground">加载代理管理关系...</div>
      <div v-else-if="loadError" class="space-y-3 py-10 text-center">
        <p class="text-sm text-destructive">{{ loadError }}</p>
        <Button type="button" variant="outline" @click="emit('retry')">重新加载</Button>
      </div>
      <form v-else-if="collection" class="space-y-4" @submit.prevent="submit">
        <div class="flex items-center justify-between gap-3">
          <p class="text-sm text-muted-foreground">当前员工版本：{{ collection.employeeVersion }}</p>
          <Button type="button" size="sm" variant="outline" @click="addRow">
            <Plus class="h-4 w-4" />添加代理主管
          </Button>
        </div>
        <div v-if="!rows.length" class="rounded-lg border py-8 text-center text-sm text-muted-foreground">
          当前没有代理主管
        </div>
        <div v-for="(row, index) in rows" :key="row.key" class="grid gap-3 rounded-lg border p-3 sm:grid-cols-[1fr_1fr_auto]">
          <div class="space-y-2">
            <Label :for="`acting-manager-${index}`">代理主管</Label>
            <select
              :id="`acting-manager-${index}`"
              v-model="row.managerEmployeeId"
              class="h-9 w-full rounded-md border bg-background px-3 text-sm"
              :aria-label="`代理主管 ${index + 1}`"
            >
              <option value="">请选择</option>
              <option v-for="candidate in candidates" :key="candidate.employeeId" :value="String(candidate.employeeId)">
                {{ candidate.name }} · {{ candidate.positionName || '未设置岗位' }}
              </option>
            </select>
          </div>
          <div class="space-y-2">
            <Label :for="`acting-end-${index}`">结束时间</Label>
            <Input :id="`acting-end-${index}`" v-model="row.effectiveTo" type="datetime-local" step="1" :aria-label="`代理结束时间 ${index + 1}`" />
          </div>
          <Button type="button" size="icon" variant="ghost" class="self-end" :aria-label="`移除代理主管 ${index + 1}`" @click="removeRow(index)">
            <Trash2 class="h-4 w-4" />
          </Button>
        </div>
        <p v-if="validationError" class="text-sm text-destructive">{{ validationError }}</p>
        <div class="space-y-2">
          <Label for="acting-reason">调整原因</Label>
          <Textarea id="acting-reason" v-model="reason" :rows="3" placeholder="请输入代理管理关系调整原因" />
        </div>
      </form>

      <DialogFooter>
        <Button type="button" variant="outline" :disabled="submitting" @click="changeOpen(false)">取消</Button>
        <Button v-if="collection" type="button" :disabled="loading || submitting || !canUpdate" @click="submit">
          {{ submitting ? '保存中...' : '保存代理关系' }}
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { Plus, Trash2 } from '@lucide/vue'
import { computed, ref, watch } from 'vue'

import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import type { ActingReportingCollection, EmployeeSummary, ManagerCandidate, ReplaceActingReportingsRequest } from '@/modules/organization/model/organization.types'

const props = defineProps<{
  open: boolean
  employee: EmployeeSummary | null
  collection: ActingReportingCollection | null
  candidates: ManagerCandidate[]
  loading: boolean
  loadError: string
  submitting: boolean
}>()
const emit = defineEmits<{
  'update:open': [value: boolean]
  retry: []
  submit: [request: ReplaceActingReportingsRequest]
}>()

interface Row { key: number; managerEmployeeId: string; effectiveTo: string }
const rows = ref<Row[]>([])
const reason = ref('')
const validationError = ref('')
let nextKey = 1
const canUpdate = computed(() => props.collection?.allowedActions.includes('UPDATE') ?? false)

watch(
  () => [props.open, props.collection] as const,
  ([open, collection]) => {
    if (!open || !collection) return
    rows.value = collection.relations.map((relation) => ({
      key: nextKey++,
      managerEmployeeId: String(relation.managerEmployeeId),
      effectiveTo: toLocalInput(relation.effectiveTo),
    }))
    reason.value = ''
    validationError.value = ''
  },
  { immediate: true },
)

function addRow(): void {
  rows.value.push({ key: nextKey++, managerEmployeeId: '', effectiveTo: '' })
}
function removeRow(index: number): void {
  rows.value.splice(index, 1)
}
function changeOpen(value: boolean): void {
  if (!value && props.submitting) return
  emit('update:open', value)
}
function submit(): void {
  if (!props.collection || !canUpdate.value) return
  const trimmedReason = reason.value.trim()
  const ids = rows.value.map((row) => row.managerEmployeeId).filter(Boolean)
  if (ids.length !== rows.value.length) return fail('每条代理关系都必须选择管理者')
  if (new Set(ids).size !== ids.length) return fail('同一代理管理者不能重复')
  const now = Date.now()
  const max = new Date(); max.setFullYear(max.getFullYear() + 1)
  const relations: ReplaceActingReportingsRequest['relations'] = []
  for (const row of rows.value) {
    const end = new Date(row.effectiveTo)
    if (!row.effectiveTo || Number.isNaN(end.getTime()) || end.getTime() <= now)
      return fail('代理结束时间必须晚于当前时间')
    if (end.getTime() > max.getTime()) return fail('代理管理期限不能超过一年')
    relations.push({ managerEmployeeId: row.managerEmployeeId, effectiveTo: end.toISOString() })
  }
  if (!trimmedReason) return fail('请输入调整原因')
  validationError.value = ''
  emit('submit', { expectedEmployeeVersion: props.collection.employeeVersion, relations, reason: trimmedReason })
}
function fail(message: string): void { validationError.value = message }
function toLocalInput(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000)
  return local.toISOString().slice(0, 19)
}
</script>
