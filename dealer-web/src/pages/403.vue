<template>
  <main class="flex min-h-screen items-center justify-center bg-background">
    <div class="text-center">
      <h1 class="text-4xl font-bold">403</h1>
      <p class="mt-3 text-muted-foreground">当前账号无权访问此页面</p>
      <Button class="mt-6" @click="backToAccessiblePage">返回可访问页面</Button>
    </div>
  </main>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

import { Button } from '@/components/ui/button'
import { usePermissionStore } from '@/stores/permission.store'

const router = useRouter()
const permissionStore = usePermissionStore()

function backToAccessiblePage(): void {
  void router.push(permissionStore.firstAccessibleMenuUrl ?? '/')
}
</script>
