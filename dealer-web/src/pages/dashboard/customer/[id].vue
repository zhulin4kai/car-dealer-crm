<template>
  <div class="p-5 space-y-5">
    <Card>
      <CardContent class="flex gap-2.5 pt-6">
        <Button variant="outline" @click="goBack">返 回</Button>
        <Button variant="outline" :disabled="loading" @click="loadCustomer(customerId)">刷新</Button>
      </CardContent>
    </Card>

    <Card>
      <CardContent class="pt-6">
        <div v-if="loading" class="py-10 text-center text-muted-foreground">加载中...</div>
        <div v-else-if="errorKind === 'invalid-id'" class="py-10 text-center text-muted-foreground">
          客户 ID 格式有误
        </div>
        <div v-else-if="errorKind === 'not-found'" class="py-10 text-center text-muted-foreground">
          客户不存在或已被删除
        </div>
        <div v-else-if="errorKind === 'forbidden'" class="py-10 text-center text-muted-foreground">
          无权限查看该客户
        </div>
        <div v-else-if="errorKind === 'network'" class="py-10 text-center text-muted-foreground">
          加载失败，请重试
        </div>
        <div v-else-if="customer" class="space-y-4">
          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-1">
              <Label>客户姓名</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ customer.customerName }}</div>
            </div>
            <div class="space-y-1">
              <Label>手机号</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ customer.phone }}</div>
            </div>
            <div class="space-y-1">
              <Label>微信</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ customer.weixin }}</div>
            </div>
            <div class="space-y-1">
              <Label>QQ</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ customer.qq }}</div>
            </div>
            <div class="space-y-1">
              <Label>邮箱</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ customer.email }}</div>
            </div>
            <div class="space-y-1">
              <Label>年龄</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ customer.age }}</div>
            </div>
            <div class="space-y-1">
              <Label>职业</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ customer.job }}</div>
            </div>
            <div class="space-y-1">
              <Label>年收入</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ customer.yearIncome }}</div>
            </div>
            <div class="space-y-1">
              <Label>地址</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ customer.address }}</div>
            </div>
            <div class="space-y-1">
              <Label>称呼</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ customer.appellationName }}</div>
            </div>
            <div class="space-y-1">
              <Label>负责人</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ customer.ownerName }}</div>
            </div>
            <div class="space-y-1">
              <Label>所属活动</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ customer.activityName }}</div>
            </div>
            <div class="space-y-1">
              <Label>是否需要贷款</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ customer.needLoanName }}</div>
            </div>
            <div class="space-y-1">
              <Label>线索来源</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ customer.sourceName }}</div>
            </div>
            <div class="space-y-1">
              <Label>意向产品</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ customer.productName }}</div>
            </div>
            <div class="space-y-1">
              <Label>描述</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ customer.description }}</div>
            </div>
            <div class="space-y-1">
              <Label>下次联系时间</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ customer.nextContactTime }}</div>
            </div>
            <div class="space-y-1">
              <Label>创建时间</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ customer.createTime }}</div>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { fetchCustomerDetail } from '@/modules/customer/api/customer-api'
import type { CustomerDetail } from '@/modules/customer/model/customer.types'
import { ApiError } from '@/shared/api/api-error'
import { useLatestRequest } from '@/shared/composables/use-latest-request'
import { toRouteId } from '@/shared/types/id'

import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Label } from '@/components/ui/label'

defineOptions({ name: 'CustomerDetailView' })

const route = useRoute()
const router = useRouter()

const customer = ref<CustomerDetail | null>(null)
const { run: runCustomerDetail, loading } = useLatestRequest<CustomerDetail>()
const errorKind = ref<'invalid-id' | 'not-found' | 'forbidden' | 'network' | null>(null)
const customerId = ref('')

function parseCustomerId(raw: string | string[]): string | null {
  const id = toRouteId(raw)
  if (!id || !/^\d+$/.test(id)) {
    return null
  }
  return id
}

async function loadCustomer(id: string): Promise<void> {
  customerId.value = id
  errorKind.value = null
  customer.value = null
  try {
    const result = await runCustomerDetail(signal => fetchCustomerDetail(id, signal))
    if (result) customer.value = result
  } catch (error) {
    if (error instanceof ApiError) {
      if (error.code === 404 || error.code === 500) {
        errorKind.value = 'not-found'
      } else if (error.code === 520) {
        errorKind.value = 'forbidden'
      } else {
        errorKind.value = 'network'
      }
    } else {
      errorKind.value = 'network'
    }
  }
}

function goBack(): void {
  if (window.history.length > 1) {
    window.history.back()
  } else {
    router.push('/dashboard/customer')
  }
}

watch(
  () => route.params.id,
  (rawId) => {
    const id = parseCustomerId(rawId ?? '')
    if (id === null) {
      errorKind.value = 'invalid-id'
      customer.value = null
      return
    }
    void loadCustomer(id)
  },
  { immediate: true },
)
</script>
