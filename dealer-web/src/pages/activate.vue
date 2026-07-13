<template>
  <main class="flex min-h-screen items-center justify-center bg-[var(--crm-bg-page)] px-6 py-10">
    <Card class="w-full max-w-md">
      <CardHeader>
        <CardTitle>激活账号</CardTitle>
        <CardDescription>邀请凭证仅可使用一次，请设置登录密码完成激活。</CardDescription>
      </CardHeader>
      <CardContent>
        <div v-if="completed" class="space-y-4 text-center">
          <CircleCheck class="mx-auto h-10 w-10 text-emerald-600" />
          <p>账号已激活，请使用新密码登录。</p>
          <Button class="w-full" @click="router.push('/')">前往登录</Button>
        </div>
        <form v-else class="space-y-4" @submit.prevent="submitForm">
          <Alert v-if="!credential || errorMessage" variant="destructive">
            <AlertDescription>
              {{ errorMessage || '激活链接缺少有效凭证，请重新获取邀请。' }}
            </AlertDescription>
          </Alert>
          <div class="space-y-2">
            <Label for="activate-password">新密码</Label>
            <Input
              id="activate-password"
              v-model="newPassword"
              type="password"
              autocomplete="new-password"
            />
            <p v-if="errors.newPassword" class="text-sm text-destructive">
              {{ errors.newPassword }}
            </p>
          </div>
          <div class="space-y-2">
            <Label for="activate-confirm-password">确认新密码</Label>
            <Input
              id="activate-confirm-password"
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
          <Button class="w-full" :disabled="submitting || !credential" type="submit">
            {{ submitting ? '激活中...' : '激活账号' }}
          </Button>
        </form>
      </CardContent>
    </Card>
  </main>
</template>

<script setup lang="ts">
import { toTypedSchema } from '@vee-validate/zod'
import { CircleCheck } from '@lucide/vue'
import { useForm } from 'vee-validate'
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as z from 'zod'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { activateAccount } from '@/modules/user/api/credential-api'
import {
  getCredentialCommandErrorMessage,
  isTerminalCredentialError,
} from '@/modules/user/model/credential-error'
import { meetsPasswordInputPolicy } from '@/modules/user/model/credential.types'

interface ActivateFormValues {
  newPassword: string
  confirmPassword: string
}

const route = useRoute()
const router = useRouter()
const submitting = ref(false)
const completed = ref(false)
const errorMessage = ref('')
const credential = ref(readCredential())

if (route.query.credential !== undefined || route.hash) {
  void router.replace({ path: route.path, query: {}, hash: '' })
}

function readCredential(): string {
  const fragment = route.hash.startsWith('#') ? route.hash.slice(1) : route.hash
  return new URLSearchParams(fragment).get('credential')?.trim() ?? ''
}
const schema = toTypedSchema(
  z
    .object({
      newPassword: z.string().refine(meetsPasswordInputPolicy, '密码强度不符合要求'),
      confirmPassword: z.string(),
    })
    .refine((value) => value.newPassword === value.confirmPassword, {
      path: ['confirmPassword'],
      message: '两次输入的密码不一致',
    }),
)
const { defineField, errors, handleSubmit, resetForm } = useForm<ActivateFormValues>({
  validationSchema: schema,
  initialValues: { newPassword: '', confirmPassword: '' },
})
const [newPassword] = defineField('newPassword')
const [confirmPassword] = defineField('confirmPassword')

const submitForm = handleSubmit(async (values) => {
  if (!credential.value || submitting.value) return
  submitting.value = true
  errorMessage.value = ''
  try {
    await activateAccount({ credential: credential.value, newPassword: values.newPassword })
    resetForm({ values: { newPassword: '', confirmPassword: '' } })
    credential.value = ''
    completed.value = true
  } catch (error: unknown) {
    resetForm({ values: { newPassword: '', confirmPassword: '' } })
    errorMessage.value = getCredentialCommandErrorMessage(error, 'activate')
    if (isTerminalCredentialError(error)) credential.value = ''
  } finally {
    submitting.value = false
  }
})
</script>
