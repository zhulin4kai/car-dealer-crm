<template>
  <Dialog :open="open" @update:open="handleOpenChange">
    <DialogContent class="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
      <DialogHeader>
        <DialogTitle>{{ title }}</DialogTitle>
        <DialogDescription>
          角色是权限集合，不代表员工岗位或汇报关系。
          <template v-if="copySource">
            复制会保留来源角色现有的逐权限数据范围；这里的默认数据范围只用于以后新增权限，修改它不会批量改写已复制范围。
          </template>
        </DialogDescription>
      </DialogHeader>
      <form class="space-y-4" @submit.prevent="submitForm">
        <div class="grid gap-4 sm:grid-cols-2">
          <div class="space-y-2">
            <Label for="role-code">角色编码</Label>
            <Input id="role-code" v-model="code" :disabled="Boolean(role) && !copySource" />
            <p v-if="errors.code" class="text-sm text-destructive">{{ errors.code }}</p>
            <p v-if="role && !copySource" class="text-xs text-muted-foreground">
              角色编码创建后不可修改。
            </p>
          </div>
          <div class="space-y-2">
            <Label for="role-name">角色名称</Label>
            <Input id="role-name" v-model="name" />
            <p v-if="errors.name" class="text-sm text-destructive">{{ errors.name }}</p>
          </div>
          <div class="space-y-2">
            <Label for="authorization-level">授权级别</Label>
            <Input id="authorization-level" v-model="authorizationLevel" type="number" min="0" />
            <p v-if="errors.authorizationLevel" class="text-sm text-destructive">
              {{ errors.authorizationLevel }}
            </p>
          </div>
          <div class="space-y-2">
            <Label for="default-data-scope">默认数据范围</Label>
            <Select v-model="defaultDataScope">
              <SelectTrigger id="default-data-scope" class="w-full"><SelectValue /></SelectTrigger>
              <SelectContent>
                <SelectItem v-for="item in dataScopeOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div class="space-y-2 sm:col-span-2">
            <Label for="role-description">角色说明</Label>
            <Textarea id="role-description" v-model="description" :rows="3" />
          </div>
          <div class="space-y-2 sm:col-span-2">
            <Label for="role-scope-type">适用范围</Label>
            <Select v-model="scopeType">
              <SelectTrigger id="role-scope-type" class="w-full"><SelectValue /></SelectTrigger>
              <SelectContent>
                <SelectItem value="GLOBAL">全局共享角色</SelectItem>
                <SelectItem value="ORGANIZATION">指定组织角色</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>

        <div v-if="scopeType === 'ORGANIZATION'" class="space-y-2 rounded-lg border p-3">
          <Label>适用组织</Label>
          <div v-if="organizationOptions.length" class="grid gap-2 sm:grid-cols-2">
            <label
              v-for="option in organizationOptions"
              :key="option.id"
              class="flex items-start gap-2 rounded-md p-2 hover:bg-muted"
            >
              <Checkbox
                :checked="organizationUnitIds.includes(option.id)"
                @update:checked="toggleOrganization(option.id, $event === true)"
              />
              <span class="text-sm">{{ option.pathName || option.name }}</span>
            </label>
          </div>
          <p v-else class="text-sm text-muted-foreground">暂无可选组织</p>
          <p v-if="organizationError" class="text-sm text-destructive">{{ organizationError }}</p>
        </div>

        <div v-if="copySource" class="space-y-2">
          <Label for="copy-reason">复制原因</Label>
          <Textarea id="copy-reason" v-model="copyReason" :rows="3" />
          <p v-if="!copyReason.trim()" class="text-xs text-muted-foreground">
            复制角色必须记录原因。
          </p>
        </div>
      </form>
      <DialogFooter>
        <Button variant="outline" :disabled="submitting" @click="handleOpenChange(false)"
          >取消</Button
        >
        <Button :disabled="submitting" @click="submitForm">{{
          submitting ? '保存中...' : '保存'
        }}</Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { toTypedSchema } from '@vee-validate/zod'
import { useForm } from 'vee-validate'
import { computed, ref, watch } from 'vue'
import * as z from 'zod'

import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
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
import { Textarea } from '@/components/ui/textarea'
import {
  DATA_SCOPE_CODE,
  DATA_SCOPE_LABEL,
  type AccessOrganizationOption,
  type DataScopeCode,
  type RoleDetail,
  type RoleFormSubmission,
  type RoleScopeType,
} from '@/modules/access/model/access.types'
import type { EntityId } from '@/shared/types/id'

interface RoleFormValues {
  code: string
  name: string
  description: string
  authorizationLevel: number
  defaultDataScope: DataScopeCode
  scopeType: RoleScopeType
}

const props = withDefaults(
  defineProps<{
    open: boolean
    role?: RoleDetail | null
    copySource?: RoleDetail | null
    organizationOptions: AccessOrganizationOption[]
    submitting?: boolean
  }>(),
  { role: null, copySource: null, submitting: false },
)

const emit = defineEmits<{
  'update:open': [open: boolean]
  submit: [submission: RoleFormSubmission]
}>()

const organizationUnitIds = ref<EntityId[]>([])
const organizationError = ref('')
const copyReason = ref('')
const title = computed(() => (props.copySource ? '复制角色' : props.role ? '编辑角色' : '新增角色'))
const dataScopeOptions = Object.values(DATA_SCOPE_CODE).map((value) => ({
  value,
  label: DATA_SCOPE_LABEL[value],
}))
const schema = toTypedSchema(
  z.object({
    code: z.string().trim().min(1, '请输入角色编码').max(64),
    name: z.string().trim().min(1, '请输入角色名称').max(64),
    description: z.string().max(255),
    authorizationLevel: z.coerce.number().int().min(0, '授权级别不能小于 0'),
    defaultDataScope: z.enum([
      'SELF',
      'DIRECT_REPORTS',
      'REPORTING_TREE',
      'PRIMARY_ORG',
      'ORG_TREE',
      'CUSTOM_ORGS',
      'GLOBAL',
    ]),
    scopeType: z.enum(['GLOBAL', 'ORGANIZATION']),
  }),
)
const { defineField, errors, handleSubmit, resetForm } = useForm<RoleFormValues>({
  validationSchema: schema,
  initialValues: {
    code: '',
    name: '',
    description: '',
    authorizationLevel: 0,
    defaultDataScope: DATA_SCOPE_CODE.SELF,
    scopeType: 'ORGANIZATION',
  },
})
const [code] = defineField('code')
const [name] = defineField('name')
const [description] = defineField('description')
const [authorizationLevel] = defineField('authorizationLevel')
const [defaultDataScope] = defineField('defaultDataScope')
const [scopeType] = defineField('scopeType')

const submitForm = handleSubmit((values) => {
  if (values.scopeType === 'ORGANIZATION' && !organizationUnitIds.value.length) {
    organizationError.value = '请至少选择一个适用组织'
    return
  }
  if (props.copySource && !copyReason.value.trim()) return
  organizationError.value = ''
  const common = {
    name: values.name.trim(),
    ...(values.description.trim() ? { description: values.description.trim() } : {}),
    authorizationLevel: Number(values.authorizationLevel),
    defaultDataScope: values.defaultDataScope,
    scopeType: values.scopeType,
    organizationUnitIds: values.scopeType === 'GLOBAL' ? [] : organizationUnitIds.value,
  }
  if (props.copySource) {
    emit('submit', {
      mode: 'copy',
      sourceRoleId: props.copySource.id,
      request: { code: values.code.trim(), ...common, reason: copyReason.value.trim() },
    })
    return
  }
  if (props.role) {
    emit('submit', {
      mode: 'update',
      id: props.role.id,
      request: { ...common, expectedVersion: props.role.version },
    })
    return
  }
  emit('submit', { mode: 'create', request: { code: values.code.trim(), ...common } })
})

function toggleOrganization(id: EntityId, checked: boolean): void {
  organizationUnitIds.value = checked
    ? [...new Set([...organizationUnitIds.value, id])]
    : organizationUnitIds.value.filter((item) => String(item) !== String(id))
}

function handleOpenChange(open: boolean): void {
  if (!open && props.submitting) return
  emit('update:open', open)
}

watch(
  () => [props.open, props.role, props.copySource] as const,
  ([open]) => {
    if (!open) return
    const source = props.copySource ?? props.role
    resetForm({
      values: {
        code: props.copySource ? '' : (source?.code ?? ''),
        name: props.copySource ? `${source?.name ?? ''}副本` : (source?.name ?? ''),
        description: source?.description ?? '',
        authorizationLevel: source?.authorizationLevel ?? 0,
        defaultDataScope: source?.defaultDataScope ?? DATA_SCOPE_CODE.SELF,
        scopeType: source?.scopeType ?? 'ORGANIZATION',
      },
    })
    organizationUnitIds.value = source?.applicableOrganizations.map((item) => item.id) ?? []
    organizationError.value = ''
    copyReason.value = ''
  },
  { immediate: true },
)
</script>
