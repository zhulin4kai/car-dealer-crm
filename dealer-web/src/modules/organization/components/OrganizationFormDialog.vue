<template>
  <Dialog :open="open" @update:open="handleOpenChange">
    <DialogContent class="sm:max-w-xl">
      <DialogHeader>
        <DialogTitle>{{ unit ? '编辑组织节点' : '新增组织节点' }}</DialogTitle>
        <DialogDescription>
          {{
            unit
              ? '修改组织基本信息，保存时会校验版本。'
              : parent
                ? `在“${parent.name}”下新增节点。`
                : '新增根组织。'
          }}
        </DialogDescription>
      </DialogHeader>

      <form class="grid gap-4 sm:grid-cols-2" @submit.prevent="submitForm">
        <div class="space-y-2">
          <Label for="organization-code">组织编码</Label>
          <Input
            id="organization-code"
            v-model="code"
            autocomplete="off"
            :disabled="Boolean(unit)"
          />
          <p v-if="errors.code" class="text-sm text-destructive">{{ errors.code }}</p>
        </div>
        <div class="space-y-2">
          <Label for="organization-name">组织名称</Label>
          <Input id="organization-name" v-model="name" autocomplete="off" />
          <p v-if="errors.name" class="text-sm text-destructive">{{ errors.name }}</p>
        </div>
        <div class="space-y-2">
          <Label for="organization-type">组织类型</Label>
          <Select v-model="unitType" :disabled="Boolean(unit)">
            <SelectTrigger id="organization-type" class="w-full">
              <SelectValue placeholder="请选择组织类型" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="item in typeOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </SelectItem>
            </SelectContent>
          </Select>
          <p v-if="errors.unitType" class="text-sm text-destructive">{{ errors.unitType }}</p>
        </div>
        <div class="space-y-2">
          <Label for="organization-order">排序号</Label>
          <Input id="organization-order" v-model="orderNo" type="number" min="0" />
          <p v-if="errors.orderNo" class="text-sm text-destructive">{{ errors.orderNo }}</p>
        </div>
        <div v-if="unit" class="space-y-2 sm:col-span-2">
          <Label for="organization-parent">上级组织</Label>
          <Select v-model="parentId" :disabled="candidatesLoading">
            <SelectTrigger id="organization-parent" class="w-full">
              <SelectValue placeholder="请选择上级组织" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="root">无上级（根组织）</SelectItem>
              <SelectItem
                v-for="candidate in parentCandidates"
                :key="candidate.id"
                :value="String(candidate.id)"
              >
                {{ candidate.pathName || candidate.name }}
              </SelectItem>
            </SelectContent>
          </Select>
          <p class="text-xs text-muted-foreground">
            候选由服务端按组织类型、祖先关系和管理范围过滤。
          </p>
        </div>
        <div class="space-y-2 sm:col-span-2">
          <Label for="organization-leader">组织负责人（可选）</Label>
          <Select v-model="leaderEmployeeId" :disabled="candidatesLoading">
            <SelectTrigger id="organization-leader" class="w-full">
              <SelectValue placeholder="暂不设置负责人" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="none">暂不设置</SelectItem>
              <SelectItem
                v-for="candidate in leaderCandidates"
                :key="candidate.employeeId"
                :value="String(candidate.employeeId)"
              >
                {{ candidate.name }} · {{ candidate.positionName || '未设置岗位' }}
              </SelectItem>
            </SelectContent>
          </Select>
          <p class="text-xs text-muted-foreground">
            组织负责人不会自动成为节点内员工的直属管理者。
          </p>
          <div
            v-if="candidateError"
            class="flex items-center justify-between rounded-md bg-amber-50 p-2 text-sm text-amber-800"
          >
            <span>{{ candidateError }}</span>
            <Button type="button" size="xs" variant="outline" @click="emit('retry-candidates')">
              重试
            </Button>
          </div>
        </div>
      </form>

      <DialogFooter>
        <Button
          type="button"
          variant="outline"
          :disabled="submitting"
          @click="handleOpenChange(false)"
        >
          取消
        </Button>
        <Button type="button" :disabled="submitting" @click="submitForm">
          {{ submitting ? '保存中...' : '保存' }}
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { toTypedSchema } from '@vee-validate/zod'
import { useForm } from 'vee-validate'
import { computed, watch } from 'vue'
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
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  ORGANIZATION_UNIT_TYPE,
  ORGANIZATION_UNIT_TYPE_LABEL,
  type OrganizationFormSubmission,
  type ManagerCandidate,
  type OrganizationParentCandidate,
  type OrganizationUnit,
  type OrganizationUnitType,
} from '@/modules/organization/model/organization.types'

interface OrganizationFormValues {
  code: string
  name: string
  unitType: OrganizationUnitType
  orderNo: number
  leaderEmployeeId: string
  parentId: string
}

const props = withDefaults(
  defineProps<{
    open: boolean
    unit?: OrganizationUnit | null
    parent?: OrganizationUnit | null
    leaderCandidates: ManagerCandidate[]
    parentCandidates: OrganizationParentCandidate[]
    candidatesLoading?: boolean
    candidateError?: string
    submitting?: boolean
  }>(),
  {
    unit: null,
    parent: null,
    candidatesLoading: false,
    candidateError: '',
    submitting: false,
  },
)

const emit = defineEmits<{
  'update:open': [open: boolean]
  submit: [submission: OrganizationFormSubmission]
  'retry-candidates': []
}>()

const allowedCreateTypesByParent: Record<OrganizationUnitType, OrganizationUnitType[]> = {
  COMPANY: [ORGANIZATION_UNIT_TYPE.STORE, ORGANIZATION_UNIT_TYPE.DEPARTMENT],
  STORE: [ORGANIZATION_UNIT_TYPE.DEPARTMENT, ORGANIZATION_UNIT_TYPE.TEAM],
  DEPARTMENT: [ORGANIZATION_UNIT_TYPE.TEAM],
  TEAM: [],
}
const typeOptions = computed(() => {
  const values = props.unit
    ? [props.unit.type]
    : props.parent
      ? allowedCreateTypesByParent[props.parent.type]
      : [ORGANIZATION_UNIT_TYPE.COMPANY]
  return values.map((value) => ({ value, label: ORGANIZATION_UNIT_TYPE_LABEL[value] }))
})

const schema = toTypedSchema(
  z.object({
    code: z.string().trim().min(1, '请输入组织编码').max(50, '组织编码最多 50 个字符'),
    name: z.string().trim().min(1, '请输入组织名称').max(100, '组织名称最多 100 个字符'),
    unitType: z.enum(['COMPANY', 'STORE', 'DEPARTMENT', 'TEAM']),
    orderNo: z.coerce.number().int().min(0, '排序号不能小于 0'),
    leaderEmployeeId: z.string(),
    parentId: z.string(),
  }),
)

const { defineField, errors, handleSubmit, resetForm } = useForm<OrganizationFormValues>({
  validationSchema: schema,
  initialValues: {
    code: '',
    name: '',
    unitType: ORGANIZATION_UNIT_TYPE.DEPARTMENT,
    orderNo: 0,
    leaderEmployeeId: '',
    parentId: 'root',
  },
})
const [code] = defineField('code')
const [name] = defineField('name')
const [unitType] = defineField('unitType')
const [orderNo] = defineField('orderNo')
const [leaderEmployeeId] = defineField('leaderEmployeeId')
const [parentId] = defineField('parentId')

const submitForm = handleSubmit((values) => {
  const common = {
    name: values.name.trim(),
    type: values.unitType,
    parentId: values.parentId === 'root' ? null : Number(values.parentId),
    leaderEmployeeId: values.leaderEmployeeId === 'none' ? null : Number(values.leaderEmployeeId),
    orderNo: Number(values.orderNo),
  }
  if (props.unit) {
    emit('submit', {
      mode: 'update',
      id: props.unit.id,
      request: { ...common, expectedVersion: props.unit.version },
    })
    return
  }
  emit('submit', {
    mode: 'create',
    request: { code: values.code.trim(), ...common },
  })
})

watch(
  () => [props.open, props.unit, props.parent] as const,
  ([open]) => {
    if (!open) return
    resetForm({
      values: {
        code: props.unit?.code ?? '',
        name: props.unit?.name ?? '',
        unitType:
          props.unit?.type ??
          (props.parent ? allowedCreateTypesByParent[props.parent.type][0] : undefined) ??
          ORGANIZATION_UNIT_TYPE.COMPANY,
        orderNo: props.unit?.orderNo ?? 0,
        leaderEmployeeId: props.unit?.leaderEmployeeId
          ? String(props.unit.leaderEmployeeId)
          : 'none',
        parentId: String(props.unit?.parentId ?? props.parent?.id ?? 'root'),
      },
    })
  },
  { immediate: true },
)

function handleOpenChange(open: boolean): void {
  if (!open && props.submitting) return
  emit('update:open', open)
}
</script>
