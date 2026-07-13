<template>
  <main class="flex min-h-screen items-center justify-center bg-[var(--crm-bg-page)] px-6 py-10">
    <Card class="w-full max-w-md">
      <CardHeader>
        <CardTitle>验证联系方式</CardTitle>
        <CardDescription>验证凭证仅可使用一次；联系方式变化后旧凭证会自动失效。</CardDescription>
      </CardHeader>
      <CardContent class="space-y-4">
        <Alert v-if="!credential || errorMessage" variant="destructive">
          <AlertDescription>
            {{ errorMessage || '验证链接缺少有效凭证，请回到个人中心重新发起。' }}
          </AlertDescription>
        </Alert>
        <div v-if="completed" class="space-y-4 text-center">
          <CircleCheck class="mx-auto h-10 w-10 text-emerald-600" />
          <p>联系方式验证完成。</p>
          <Button class="w-full" @click="router.push('/')">前往登录</Button>
        </div>
        <Button
          v-else
          class="w-full"
          :disabled="submitting || !credential"
          @click="completeVerification"
        >
          {{ submitting ? '验证中...' : '确认验证' }}
        </Button>
      </CardContent>
    </Card>
  </main>
</template>

<script setup lang="ts">
import { CircleCheck } from '@lucide/vue'
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { verifyContact } from '@/modules/user/api/credential-api'
import { ApiError } from '@/shared/api/api-error'
import { API_ERROR_CODE } from '@/shared/api/error-codes'

const route = useRoute()
const router = useRouter()
const credential = ref(readCredential())
const submitting = ref(false)
const completed = ref(false)
const errorMessage = ref('')

if (route.query.credential !== undefined || route.hash) {
  void router.replace({ path: route.path, query: {}, hash: '' })
}

function readCredential(): string {
  const fragment = route.hash.startsWith('#') ? route.hash.slice(1) : route.hash
  return new URLSearchParams(fragment).get('credential')?.trim() ?? ''
}

async function completeVerification(): Promise<void> {
  if (!credential.value || submitting.value) return
  submitting.value = true
  errorMessage.value = ''
  try {
    await verifyContact({ credential: credential.value })
    credential.value = ''
    completed.value = true
  } catch (error: unknown) {
    if (error instanceof ApiError && error.code === API_ERROR_CODE.PROFILE_VERSION_CONFLICT) {
      credential.value = ''
      errorMessage.value = '联系方式已经变化，原验证链接已失效，请回到个人中心重新发起。'
    } else if (error instanceof ApiError && [
      API_ERROR_CODE.CREDENTIAL_INVALID,
      API_ERROR_CODE.CREDENTIAL_EXPIRED,
      API_ERROR_CODE.CREDENTIAL_ALREADY_USED,
    ].includes(error.code)) {
      credential.value = ''
      errorMessage.value = '验证凭证无效、已过期或已经使用，请重新发起验证。'
    } else if (error instanceof ApiError && error.code === API_ERROR_CODE.CREDENTIAL_RATE_LIMITED) {
      errorMessage.value = '验证尝试过于频繁，请稍后再试。'
    } else {
      errorMessage.value = '系统暂时无法完成验证，请稍后重试。'
    }
  } finally {
    submitting.value = false
  }
}
</script>
