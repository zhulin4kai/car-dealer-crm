<template>
  <main class="flex min-h-screen items-center justify-center bg-[var(--crm-bg-page)] px-6 py-10">
    <Card class="w-full max-w-md">
      <CardHeader>
        <CardTitle>首次登录修改密码</CardTitle>
        <CardDescription>完成改密前不能进入业务工作台。</CardDescription>
      </CardHeader>
      <CardContent>
        <form class="space-y-4" @submit.prevent="submitForm">
          <Alert v-if="errorMessage" variant="destructive">
            <AlertDescription>{{ errorMessage }}</AlertDescription>
          </Alert>
          <div class="space-y-2">
            <Label for="first-current-password">当前密码</Label>
            <Input
              id="first-current-password"
              v-model="currentPassword"
              type="password"
              autocomplete="current-password"
            />
            <p v-if="errors.currentPassword" class="text-sm text-destructive">
              {{ errors.currentPassword }}
            </p>
          </div>
          <div class="space-y-2">
            <Label for="first-new-password">新密码</Label>
            <Input
              id="first-new-password"
              v-model="newPassword"
              type="password"
              autocomplete="new-password"
            />
            <p v-if="errors.newPassword" class="text-sm text-destructive">
              {{ errors.newPassword }}
            </p>
          </div>
          <div class="space-y-2">
            <Label for="first-confirm-password">确认新密码</Label>
            <Input
              id="first-confirm-password"
              v-model="confirmPassword"
              type="password"
              autocomplete="new-password"
            />
            <p v-if="errors.confirmPassword" class="text-sm text-destructive">
              {{ errors.confirmPassword }}
            </p>
          </div>
          <p class="text-xs text-muted-foreground">
            密码长度为 6-16 位，并同时包含大写字母、小写字母和数字。
          </p>
          <Button class="w-full" type="submit" :disabled="submitting">
            {{ submitting ? '修改中...' : '修改密码并重新登录' }}
          </Button>
          <Button
            class="w-full"
            type="button"
            variant="outline"
            :disabled="submitting"
            @click="logout"
          >
            退出登录
          </Button>
        </form>
      </CardContent>
    </Card>
  </main>
</template>

<script setup lang="ts">
import { toTypedSchema } from '@vee-validate/zod'
import { useForm } from 'vee-validate'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import * as z from 'zod'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { changeFirstPassword } from '@/modules/user/api/credential-api'
import { meetsPasswordInputPolicy } from '@/modules/user/model/credential.types'
import { usePermissionStore } from '@/stores/permission.store'
import { useAuthStore } from '@/stores/auth.store'

interface FirstPasswordChangeFormValues {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

const router = useRouter()
const authStore = useAuthStore()
const permissionStore = usePermissionStore()
const submitting = ref(false)
const errorMessage = ref('')
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
const { defineField, errors, handleSubmit, resetForm } = useForm<FirstPasswordChangeFormValues>({
  validationSchema: schema,
  initialValues: { currentPassword: '', newPassword: '', confirmPassword: '' },
})
const [currentPassword] = defineField('currentPassword')
const [newPassword] = defineField('newPassword')
const [confirmPassword] = defineField('confirmPassword')

const submitForm = handleSubmit(async (values) => {
  if (submitting.value) return
  submitting.value = true
  errorMessage.value = ''
  try {
    await changeFirstPassword({
      currentPassword: values.currentPassword,
      newPassword: values.newPassword,
    })
    resetForm({ values: { currentPassword: '', newPassword: '', confirmPassword: '' } })
    authStore.forceLogout()
    permissionStore.clearPermissions()
    await router.push('/')
  } catch {
    resetForm({ values: { currentPassword: '', newPassword: '', confirmPassword: '' } })
    errorMessage.value = '密码修改失败，请确认当前密码，并使用未在近期使用过的新密码。'
  } finally {
    submitting.value = false
  }
})

async function logout(): Promise<void> {
  if (submitting.value) return
  submitting.value = true
  try {
    await authStore.logout()
  } catch {
    authStore.forceLogout()
  } finally {
    permissionStore.clearPermissions()
    submitting.value = false
    await router.push('/')
  }
}
</script>
