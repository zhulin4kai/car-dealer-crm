<template>
  <form class="space-y-4" @submit.prevent="submitForm">
    <Alert v-if="errorMessage" variant="destructive">
      <AlertDescription>{{ errorMessage }}</AlertDescription>
    </Alert>
    <div class="space-y-2">
      <Label for="own-current-password">当前密码</Label>
      <Input
        id="own-current-password"
        v-model="currentPassword"
        type="password"
        autocomplete="current-password"
      />
      <p v-if="errors.currentPassword" class="text-sm text-destructive">
        {{ errors.currentPassword }}
      </p>
    </div>
    <div class="space-y-2">
      <Label for="own-new-password">新密码</Label>
      <Input
        id="own-new-password"
        v-model="newPassword"
        type="password"
        autocomplete="new-password"
      />
      <p v-if="errors.newPassword" class="text-sm text-destructive">
        {{ errors.newPassword }}
      </p>
    </div>
    <div class="space-y-2">
      <Label for="own-confirm-password">确认新密码</Label>
      <Input
        id="own-confirm-password"
        v-model="confirmPassword"
        type="password"
        autocomplete="new-password"
      />
      <p v-if="errors.confirmPassword" class="text-sm text-destructive">
        {{ errors.confirmPassword }}
      </p>
    </div>
    <p class="text-xs text-muted-foreground">
      密码长度为 6-16 位，并同时包含大写字母、小写字母和数字。修改成功后需重新登录。
    </p>
    <div class="flex justify-end">
      <Button type="submit" :disabled="submitting">
        {{ submitting ? '修改中...' : '修改自己的密码' }}
      </Button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { toTypedSchema } from '@vee-validate/zod'
import { useForm } from 'vee-validate'
import { watch } from 'vue'
import * as z from 'zod'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  meetsPasswordInputPolicy,
  type ChangeOwnPasswordRequest,
} from '@/modules/user/model/credential.types'

interface ChangeOwnPasswordFormValues {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

const props = withDefaults(
  defineProps<{
    submitting?: boolean
    errorMessage?: string
    resetKey?: number
  }>(),
  { submitting: false, errorMessage: '', resetKey: 0 },
)

const emit = defineEmits<{
  save: [request: ChangeOwnPasswordRequest]
}>()

const schema = toTypedSchema(
  z
    .object({
      currentPassword: z.string().min(1, '请输入当前密码'),
      newPassword: z.string().refine(meetsPasswordInputPolicy, '密码强度不符合要求'),
      confirmPassword: z.string(),
    })
    .refine((value) => value.newPassword === value.confirmPassword, {
      path: ['confirmPassword'],
      message: '两次输入的密码不一致',
    })
    .refine((value) => value.currentPassword !== value.newPassword, {
      path: ['newPassword'],
      message: '新密码不能与当前密码相同',
    }),
)
const { defineField, errors, handleSubmit, resetForm } = useForm<ChangeOwnPasswordFormValues>({
  validationSchema: schema,
  initialValues: { currentPassword: '', newPassword: '', confirmPassword: '' },
})
const [currentPassword] = defineField('currentPassword')
const [newPassword] = defineField('newPassword')
const [confirmPassword] = defineField('confirmPassword')

const submitForm = handleSubmit((values) => {
  emit('save', { currentPassword: values.currentPassword, newPassword: values.newPassword })
})

watch(
  () => props.resetKey,
  () => {
    resetForm({ values: { currentPassword: '', newPassword: '', confirmPassword: '' } })
  },
)
</script>
