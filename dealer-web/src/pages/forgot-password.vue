<template>
  <main class="flex min-h-screen items-center justify-center bg-[var(--crm-bg-page)] px-6 py-10">
    <Card class="w-full max-w-md">
      <CardHeader>
        <CardTitle>找回密码</CardTitle>
        <CardDescription>提交登录账号后，系统会按已配置的安全渠道处理。</CardDescription>
      </CardHeader>
      <CardContent>
        <div v-if="accepted" class="space-y-4 text-center">
          <CircleCheck class="mx-auto h-10 w-10 text-emerald-600" />
          <p>{{ genericAcceptedMessage }}</p>
          <Button class="w-full" variant="outline" @click="router.push('/')">返回登录</Button>
        </div>
        <form v-else class="space-y-4" @submit.prevent="submitForm">
          <Alert v-if="errorMessage" variant="destructive">
            <AlertDescription>{{ errorMessage }}</AlertDescription>
          </Alert>
          <div class="space-y-2">
            <Label for="forgot-login-act">登录账号</Label>
            <Input id="forgot-login-act" v-model="loginAct" autocomplete="username" />
            <p v-if="errors.loginAct" class="text-sm text-destructive">
              {{ errors.loginAct }}
            </p>
          </div>
          <Button class="w-full" type="submit" :disabled="submitting">
            {{ submitting ? '提交中...' : '提交找回请求' }}
          </Button>
          <Button class="w-full" type="button" variant="ghost" @click="router.push('/')">
            返回登录
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
import { useRouter } from 'vue-router'
import * as z from 'zod'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { requestPasswordReset } from '@/modules/user/api/credential-api'

interface ForgotPasswordFormValues {
  loginAct: string
}

const genericAcceptedMessage = '如果该账号可以找回密码，系统将通过已配置的安全渠道发送后续指引。'
const router = useRouter()
const submitting = ref(false)
const accepted = ref(false)
const errorMessage = ref('')
const schema = toTypedSchema(
  z.object({ loginAct: z.string().trim().min(1, '请输入登录账号').max(100) }),
)
const { defineField, errors, handleSubmit, resetForm } = useForm<ForgotPasswordFormValues>({
  validationSchema: schema,
  initialValues: { loginAct: '' },
})
const [loginAct] = defineField('loginAct')

const submitForm = handleSubmit(async (values) => {
  if (submitting.value) return
  submitting.value = true
  errorMessage.value = ''
  try {
    await requestPasswordReset({ loginAct: values.loginAct.trim() })
    resetForm({ values: { loginAct: '' } })
    accepted.value = true
  } catch {
    errorMessage.value = '系统暂时无法处理找回请求，请稍后重试。'
  } finally {
    submitting.value = false
  }
})
</script>
