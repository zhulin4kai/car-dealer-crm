<template>
  <form class="space-y-4" @submit.prevent="submitForm">
    <div class="grid gap-4 sm:grid-cols-2">
      <div class="space-y-2">
        <Label for="profile-name">姓名</Label>
        <Input id="profile-name" v-model="name" autocomplete="name" />
        <p v-if="errors.name" class="text-sm text-destructive">{{ errors.name }}</p>
      </div>
      <div class="space-y-2">
        <Label for="profile-phone">手机</Label>
        <Input id="profile-phone" v-model="phone" autocomplete="tel" />
        <p v-if="errors.phone" class="text-sm text-destructive">{{ errors.phone }}</p>
      </div>
      <div class="space-y-2">
        <Label for="profile-email">邮箱</Label>
        <Input id="profile-email" v-model="email" autocomplete="email" />
        <p v-if="errors.email" class="text-sm text-destructive">{{ errors.email }}</p>
      </div>
      <div class="space-y-2">
        <Label for="profile-avatar-url">头像地址</Label>
        <Input id="profile-avatar-url" v-model="avatarUrl" autocomplete="url" />
        <p v-if="errors.avatarUrl" class="text-sm text-destructive">{{ errors.avatarUrl }}</p>
      </div>
    </div>
    <p class="text-xs text-muted-foreground">
      此处只能修改普通个人资料，不能修改登录账号、任职、账号状态、角色、权限或数据范围。
    </p>
    <div class="flex justify-end">
      <Button type="submit" :disabled="submitting">
        {{ submitting ? '保存中...' : '保存个人资料' }}
      </Button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { toTypedSchema } from '@vee-validate/zod'
import { useForm } from 'vee-validate'
import { watch } from 'vue'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  normalizeMainlandMobile,
  userProfileSchema,
  type UserProfileFormValues,
} from '@/modules/user/model/user-profile.schema'
import type { UpdateOwnProfileRequest, UserProfile } from '@/modules/user/model/user-profile.types'

const props = withDefaults(
  defineProps<{
    profile: UserProfile
    submitting?: boolean
  }>(),
  { submitting: false },
)

const emit = defineEmits<{
  save: [request: UpdateOwnProfileRequest]
}>()

const { defineField, errors, handleSubmit, resetForm } = useForm<UserProfileFormValues>({
  validationSchema: toTypedSchema(userProfileSchema),
  initialValues: { name: '', phone: '', email: '', avatarUrl: '' },
})
const [name] = defineField('name')
const [phone] = defineField('phone')
const [email] = defineField('email')
const [avatarUrl] = defineField('avatarUrl')

const submitForm = handleSubmit((values) => {
  emit('save', {
    profileVersion: props.profile.profileVersion,
    name: values.name.trim(),
    phone: normalizeMainlandMobile(values.phone) || null,
    email: values.email.trim() || null,
    avatarUrl: values.avatarUrl.trim() || null,
  })
})

watch(
  () => props.profile,
  (profile) => {
    resetForm({
      values: {
        name: profile.name,
        phone: profile.phone ?? '',
        email: profile.email ?? '',
        avatarUrl: profile.avatarUrl ?? '',
      },
    })
  },
  { immediate: true },
)
</script>
