<template>
  <Dialog :open="open" @update:open="changeOpen">
    <DialogContent class="max-h-[90vh] overflow-y-auto sm:max-w-xl">
      <DialogHeader>
        <DialogTitle>{{ mode === 'create' ? '新增用户' : '编辑用户资料' }}</DialogTitle>
        <DialogDescription v-if="mode === 'create'">
          创建后由邀请激活流程设置密码，页面不会生成或展示明文密码。
        </DialogDescription>
      </DialogHeader>
      <form class="grid gap-4 sm:grid-cols-2" @submit.prevent="submit">
        <div v-if="mode === 'create' && options.bootstrapRequired" class="rounded-md border border-amber-300 bg-amber-50 p-3 text-sm sm:col-span-2">
          <template v-if="options.bootstrapAllowed">
            <label class="flex items-start gap-2 font-medium">
              <Checkbox :checked="form.bootstrapRootLeader" @update:checked="toggleBootstrap" />
              <span>初始化首个根公司负责人和普通管理员</span>
            </label>
            <p class="mt-1 text-muted-foreground">该操作只能由固定恢复账号执行；将以根公司版本做并发校验，并要求授予管理员角色。</p>
          </template>
          <p v-else class="text-destructive">当前组织数据不满足首次初始化条件：必须只有一个启用且尚未设置负责人的根公司。</p>
        </div>
        <div v-if="mode === 'create'" class="space-y-2">
          <Label for="managed-login">登录账号</Label>
          <Input id="managed-login" v-model="form.loginAct" autocomplete="off" />
        </div>
        <div class="space-y-2">
          <Label for="managed-name">姓名</Label>
          <Input id="managed-name" v-model="form.name" />
        </div>
        <div class="space-y-2">
          <Label for="managed-phone">手机</Label>
          <Input id="managed-phone" v-model="form.phone" />
        </div>
        <div class="space-y-2">
          <Label for="managed-email">邮箱</Label>
          <Input id="managed-email" v-model="form.email" type="email" />
        </div>
        <template v-if="mode === 'create'">
          <div class="space-y-2">
            <Label for="employee-no">员工编号</Label>
            <Input id="employee-no" v-model="form.employeeNo" />
          </div>
          <div class="space-y-2">
            <Label for="create-organization">组织</Label>
            <select id="create-organization" v-model="form.organizationUnitId" class="form-select" :disabled="form.bootstrapRootLeader" @change="organizationChanged">
              <option value="">请选择组织</option>
              <option v-for="item in options.organizations" :key="item.id" :value="String(item.id)">{{ item.label }}</option>
            </select>
          </div>
          <div class="space-y-2">
            <Label for="create-position">岗位</Label>
            <select id="create-position" v-model="form.positionId" class="form-select">
              <option value="">请选择岗位</option>
              <option v-for="item in options.positions" :key="item.id" :value="String(item.id)">{{ item.label }}</option>
            </select>
          </div>
          <div class="space-y-2">
            <Label for="create-manager">直属管理者</Label>
            <select id="create-manager" v-model="form.managerEmployeeId" class="form-select" :disabled="form.bootstrapRootLeader">
              <option value="">{{ form.bootstrapRootLeader ? '根公司负责人无需直属管理者' : '请选择直属管理者' }}</option>
              <option v-for="item in options.managers" :key="item.id" :value="String(item.id)">{{ item.label }}</option>
            </select>
          </div>
          <fieldset class="space-y-2 sm:col-span-2">
            <legend class="text-sm font-medium">初始角色</legend>
            <p v-if="!form.organizationUnitId" class="text-sm text-muted-foreground">请先选择组织，再加载可委派角色。</p>
            <p v-else-if="roleOptionsLoading" class="text-sm text-muted-foreground">正在加载当前组织可委派角色...</p>
            <p v-else-if="roleOptionsError" class="text-sm text-destructive">{{ roleOptionsError }}</p>
            <p v-else-if="!options.assignableRoles.length" class="text-sm text-muted-foreground">当前组织没有可委派角色。</p>
            <div v-else class="grid gap-2 sm:grid-cols-2">
              <label v-for="item in options.assignableRoles" :key="item.id" class="flex items-center gap-2 rounded border p-2 text-sm">
                <Checkbox :checked="form.roleIds.includes(String(item.id))" :disabled="roleOptionsLoading" @update:checked="(checked) => toggleRole(String(item.id), checked)" />
                {{ item.label }}
              </label>
            </div>
          </fieldset>
        </template>
        <p v-if="errorMessage" class="text-sm text-destructive sm:col-span-2">{{ errorMessage }}</p>
      </form>
      <DialogFooter>
        <Button type="button" variant="outline" :disabled="submitting" @click="emit('update:open', false)">取消</Button>
        <Button type="button" :disabled="submitting" @click="submit">{{ submitting ? '提交中...' : '保存' }}</Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'

import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  isMainlandMobile,
  normalizeMainlandMobile,
} from '@/modules/user/model/user-profile.schema'
import type { CreateManagedUserRequest, ManagedUserDetail, UpdateManagedUserProfileRequest, UserFilterOptions } from '@/modules/user/model/user.types'

const props = defineProps<{
  open: boolean
  mode: 'create' | 'edit'
  options: UserFilterOptions
  user?: ManagedUserDetail | null
  submitting?: boolean
  roleOptionsLoading?: boolean
  roleOptionsError?: string
}>()

const emit = defineEmits<{
  'update:open': [open: boolean]
  create: [request: CreateManagedUserRequest]
  update: [request: UpdateManagedUserProfileRequest]
  'organization-change': [organizationUnitId: string | null]
}>()

const form = reactive({ loginAct: '', name: '', phone: '', email: '', employeeNo: '', organizationUnitId: '', positionId: '', managerEmployeeId: '', roleIds: [] as string[], bootstrapRootLeader: false })
const errorMessage = ref('')

watch(() => [props.open, props.mode, props.user] as const, () => {
  if (!props.open) return
  Object.assign(form, { loginAct: props.user?.loginAct ?? '', name: props.user?.name ?? '', phone: props.user?.phone ?? '', email: props.user?.email ?? '', employeeNo: '', organizationUnitId: '', positionId: '', managerEmployeeId: '', roleIds: [], bootstrapRootLeader: false })
  errorMessage.value = ''
}, { immediate: true })

function changeOpen(open: boolean): void {
  if (!props.submitting) emit('update:open', open)
}

function toggleRole(id: string, checked: boolean | 'indeterminate'): void {
  form.roleIds = checked === true ? [...new Set([...form.roleIds, id])] : form.roleIds.filter((item) => item !== id)
}

function organizationChanged(): void {
  form.roleIds = []
  form.managerEmployeeId = ''
  errorMessage.value = ''
  emit('organization-change', form.organizationUnitId || null)
}

function toggleBootstrap(checked: boolean | 'indeterminate'): void {
  form.bootstrapRootLeader = checked === true
  form.roleIds = []
  form.managerEmployeeId = ''
  form.organizationUnitId = form.bootstrapRootLeader && props.options.bootstrapRootOrganizationId != null
    ? String(props.options.bootstrapRootOrganizationId)
    : ''
  errorMessage.value = ''
  emit('organization-change', form.organizationUnitId || null)
}

function submit(): void {
  if (props.submitting) return
  const name = form.name.trim()
  const phone = normalizeMainlandMobile(form.phone)
  const email = form.email.trim()
  if (!name) { errorMessage.value = '请输入姓名'; return }
  if (name.length > 50) { errorMessage.value = '姓名最多 50 个字符'; return }
  if (!isMainlandMobile(form.phone)) { errorMessage.value = '手机号码格式有误'; return }
  if (props.mode === 'edit') {
    if (!props.user) return
    emit('update', { profileVersion: props.user.profileVersion, name, phone: phone || null, email: email || null })
    return
  }
  if (!form.loginAct.trim() || !form.employeeNo.trim() || !form.organizationUnitId || !form.positionId) {
    errorMessage.value = '请完整填写账号、员工编号、组织和岗位'
    return
  }
  if (props.options.bootstrapRequired && !props.options.bootstrapAllowed) {
    errorMessage.value = '当前组织数据不满足首次初始化条件'
    return
  }
  if (props.options.bootstrapRequired && !form.bootstrapRootLeader) {
    errorMessage.value = '请明确勾选首次根公司负责人初始化'
    return
  }
  if (!form.bootstrapRootLeader && !form.managerEmployeeId) {
    errorMessage.value = '请选择直属管理者'
    return
  }
  emit('create', {
    loginAct: form.loginAct.trim(), name, phone: phone || null, email: email || null,
    employeeNo: form.employeeNo.trim(), organizationUnitId: form.organizationUnitId,
    positionId: form.positionId, managerEmployeeId: form.bootstrapRootLeader ? null : form.managerEmployeeId,
    roleIds: [...form.roleIds], bootstrapRootLeader: form.bootstrapRootLeader,
    expectedRootOrganizationVersion: form.bootstrapRootLeader
      ? props.options.bootstrapRootOrganizationVersion ?? null
      : null,
  })
}
</script>

<style scoped>
.form-select { height: 2.25rem; width: 100%; border-radius: .375rem; border: 1px solid var(--crm-border-light); background: var(--crm-bg-panel); padding: 0 .75rem; font-size: .875rem; }
</style>
