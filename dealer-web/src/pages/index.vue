<template>
  <div class="login-page flex h-screen">
    <!-- 左侧展示区 -->
    <aside class="hidden md:flex w-[40%] flex-col justify-center items-center text-center bg-muted p-8">
      <img src="@/assets/logo-2.svg" class="w-4/5 h-auto max-h-[413px]">
      <p class="text-3xl md:text-4xl text-muted-foreground mt-4">
        欢迎使用
        <br>
        徐州工程学院汽车销售管理系统
      </p>
    </aside>

    <!-- 右侧登录区 -->
    <main class="flex-1 flex flex-col justify-center items-center">
      <div class="text-2xl font-bold text-center mb-6">登录您的账号</div>

      <form class="w-[90%] sm:w-[60%] md:w-[40%] lg:w-[30%] p-10 border rounded-md shadow-lg space-y-4" @submit.prevent="onSubmit">
        <div class="space-y-2">
          <Label for="loginAct">账号</Label>
          <Input id="loginAct" v-model="loginAct" placeholder="请输入登录账号" />
          <p v-if="errors.loginAct" class="text-sm text-destructive">{{ errors.loginAct }}</p>
        </div>

        <div class="space-y-2">
          <Label for="loginPwd">密码</Label>
          <Input id="loginPwd" type="password" v-model="loginPwd" placeholder="请输入登录密码" />
          <p v-if="errors.loginPwd" class="text-sm text-destructive">{{ errors.loginPwd }}</p>
        </div>

        <Button type="submit" class="w-full" :disabled="isSubmitting">登 录</Button>

        <div class="flex items-center space-x-2">
          <Checkbox id="rememberMe" :checked="rememberMe" @update:checked="(v: boolean) => rememberMe = v" />
          <Label for="rememberMe" class="text-sm font-normal cursor-pointer">记住我</Label>
        </div>
      </form>
    </main>
  </div>
</template>

<script setup lang="ts">
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import * as z from 'zod'

import { freeLogin } from '@/modules/user/api/user-api'
import type { LoginForm } from '@/modules/user/model/user.types'
import { messageTip } from '@/shared/utils/feedback'
import { useAuthStore } from '@/stores/auth.store'
import { ApiError } from '@/shared/api/api-error'
import { API_ERROR_CODE } from '@/shared/api/error-codes'

import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

defineOptions({
  name: 'LoginView',
})

const router = useRouter()
const authStore = useAuthStore()

const loginSchema = toTypedSchema(z.object({
  loginAct: z.string().min(1, '请输入登录账号'),
  loginPwd: z.string().min(6, '登录密码长度为6-16位').max(16, '登录密码长度为6-16位'),
  rememberMe: z.boolean(),
}))

const { handleSubmit, errors, isSubmitting, defineField } = useForm<LoginForm>({
  validationSchema: loginSchema,
  initialValues: {
    loginAct: '',
    loginPwd: '',
    rememberMe: false,
  },
})

const [loginAct] = defineField('loginAct')
const [loginPwd] = defineField('loginPwd')
const [rememberMe] = defineField('rememberMe')

const onSubmit = handleSubmit(async (formData) => {
  try {
    await authStore.login(formData)
    messageTip('登录成功', 'success')
    await router.push('/dashboard')
  } catch (error) {
    if (error instanceof ApiError && error.code === API_ERROR_CODE.AUTH_LOGIN_FAILED) {
      messageTip(error.message, 'error')
      return
    }
    messageTip('登录失败，请稍后重试', 'error')
  }
})

async function restoreRememberedSession(): Promise<void> {
  authStore.restoreSession()
  if (!authStore.rememberMe || !authStore.token) {
    return
  }

  await freeLogin()
  await router.push('/dashboard')
}

onMounted(() => {
  void restoreRememberedSession()
})
</script>

<style scoped>
.login-page {
  background-image: url('@/assets/background.png');
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
}
</style>
