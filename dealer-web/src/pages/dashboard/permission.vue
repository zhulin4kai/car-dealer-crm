<template>
  <div class="crm-data-page">
    <section class="crm-panel">
      <div class="flex flex-wrap items-start justify-between gap-3 border-b p-4">
        <div>
          <h2 class="text-lg font-semibold">权限目录</h2>
          <p class="mt-1 text-sm text-muted-foreground">
            这里只读展示代码已支持的权限资源、敏感级别和可委派性，不能创建或修改权限 code。
          </p>
        </div>
        <Button variant="outline" @click="router.push({ name: 'role-management' })">
          返回角色管理
        </Button>
      </div>
      <div v-if="loading" class="py-24 text-center text-muted-foreground">加载权限目录...</div>
      <div v-else-if="errorMessage" class="py-24 text-center text-destructive">
        {{ errorMessage }}
      </div>
      <div v-else class="h-[calc(100vh-190px)] p-4">
        <PermissionCatalog :nodes="catalog" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { Button } from '@/components/ui/button'
import { fetchPermissionCatalog } from '@/modules/access/api/access-api'
import PermissionCatalog from '@/modules/access/components/PermissionCatalog.vue'
import { getAccessErrorMessage } from '@/modules/access/model/access-error'
import type { PermissionCatalogItem } from '@/modules/access/model/access.types'

const router = useRouter()
const catalog = ref<PermissionCatalogItem[]>([])
const loading = ref(false)
const errorMessage = ref('')

async function loadCatalog(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    catalog.value = await fetchPermissionCatalog()
  } catch (error: unknown) {
    errorMessage.value = getAccessErrorMessage(error, '加载权限目录失败', '没有权限查看权限目录')
  } finally {
    loading.value = false
  }
}

onMounted(() => void loadCatalog())
</script>
