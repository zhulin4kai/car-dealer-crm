<template>
  <div class="p-5 space-y-5">
    <Card>
      <CardContent class="flex flex-wrap gap-2.5 pt-6">
        <Button variant="outline" class="gap-2" @click="goBack">返 回</Button>
        <Button variant="outline" class="gap-2" :disabled="loading" @click="loadCustomer(customerId)">
          <RefreshCw class="size-4" />
          刷新
        </Button>
        <Button
          v-has-permission="PERMISSIONS.customer.transfer"
          variant="outline"
          class="gap-2"
          :disabled="loading || transferring"
          @click="openTransferDialog"
        >
          <UserRound class="size-4" />
          转移
        </Button>
        <Button
          v-has-permission="PERMISSIONS.customer.merge"
          variant="outline"
          class="gap-2"
          :disabled="loading || merging"
          @click="openMergeDialog"
        >
          <Merge class="size-4" />
          合并
        </Button>
        <Button
          v-has-permission="PERMISSIONS.customer.delete"
          variant="destructive"
          class="gap-2"
          :disabled="loading || deleting"
          @click="handleDeleteCustomer"
        >
          <Loader2 v-if="deleting" class="size-4 animate-spin" />
          <Trash2 v-else class="size-4" />
          删除
        </Button>
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
              <div class="w-full bg-muted rounded px-4 py-2">{{ displayValue(customer.customerName) }}</div>
            </div>
            <div class="space-y-1">
              <Label>手机号</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ displayValue(customer.phone) }}</div>
            </div>
            <div class="space-y-1">
              <Label>微信</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ displayValue(customer.weixin) }}</div>
            </div>
            <div class="space-y-1">
              <Label>QQ</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ displayValue(customer.qq) }}</div>
            </div>
            <div class="space-y-1">
              <Label>邮箱</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ displayValue(customer.email) }}</div>
            </div>
            <div class="space-y-1">
              <Label>年龄</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ displayValue(customer.age) }}</div>
            </div>
            <div class="space-y-1">
              <Label>职业</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ displayValue(customer.job) }}</div>
            </div>
            <div class="space-y-1">
              <Label>年收入</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ displayValue(customer.yearIncome) }}</div>
            </div>
            <div class="space-y-1">
              <Label>地址</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ displayValue(customer.address) }}</div>
            </div>
            <div class="space-y-1">
              <Label>称呼</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ displayValue(customer.appellationName) }}</div>
            </div>
            <div class="space-y-1">
              <Label>负责人</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ displayValue(customer.ownerName) }}</div>
            </div>
            <div class="space-y-1">
              <Label>所属活动</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ displayValue(customer.activityName) }}</div>
            </div>
            <div class="space-y-1">
              <Label>是否需要贷款</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ displayValue(customer.needLoanName) }}</div>
            </div>
            <div class="space-y-1">
              <Label>客户来源</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ displayValue(customer.sourceName) }}</div>
            </div>
            <div class="space-y-1">
              <Label>原始线索来源</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ displayValue(customer.originalSourceName) }}</div>
            </div>
            <div class="space-y-1">
              <Label>意向状态</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ displayValue(customer.intentionStateName) }}</div>
            </div>
            <div class="space-y-1">
              <Label>客户状态</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ displayValue(customer.customerStatusName ?? customer.stateName) }}</div>
            </div>
            <div class="space-y-1">
              <Label>意向产品</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ displayValue(customer.productName ?? customer.intentionProductName) }}</div>
            </div>
            <div class="space-y-1">
              <Label>描述</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ displayValue(customer.description) }}</div>
            </div>
            <div class="space-y-1">
              <Label>下次联系时间</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ displayValue(customer.nextContactTime) }}</div>
            </div>
            <div class="space-y-1">
              <Label>创建时间</Label>
              <div class="w-full bg-muted rounded px-4 py-2">{{ displayValue(customer.createTime) }}</div>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>

    <Dialog v-model:open="transferDialogOpen">
      <DialogContent class="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>转移客户</DialogTitle>
        </DialogHeader>
        <div class="space-y-4">
          <div class="space-y-2">
            <Label>目标负责人</Label>
            <Select v-model="transferOwnerId">
              <SelectTrigger class="w-full">
                <SelectValue placeholder="请选择目标负责人" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem
                  v-for="item in ownerOptions"
                  :key="item.userId"
                  :value="String(item.userId)"
                >
                  {{ item.name }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div class="space-y-2">
            <Label>转移原因</Label>
            <Textarea v-model="transferReason" :rows="5" placeholder="请输入转移原因" />
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" @click="transferDialogOpen = false">取 消</Button>
            <Button type="button" :disabled="transferring" @click="submitTransferOwner">
              <Loader2 v-if="transferring" class="size-4 animate-spin mr-1" />
              确认转移
            </Button>
          </DialogFooter>
        </div>
      </DialogContent>
    </Dialog>

    <Dialog v-model:open="mergeDialogOpen">
      <DialogContent class="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>合并客户</DialogTitle>
        </DialogHeader>
        <div class="space-y-4">
          <div class="space-y-2">
            <Label>被合并客户 ID</Label>
            <Input v-model="mergeSourceCustomerId" inputmode="numeric" placeholder="请输入客户 ID" />
          </div>
          <div class="space-y-2">
            <Label>合并原因</Label>
            <Textarea v-model="mergeReason" :rows="5" placeholder="请输入合并原因" />
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" @click="mergeDialogOpen = false">取 消</Button>
            <Button type="button" :disabled="merging" @click="submitMergeCustomer">
              <Loader2 v-if="merging" class="size-4 animate-spin mr-1" />
              确认合并
            </Button>
          </DialogFooter>
        </div>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { PERMISSIONS } from '@/shared/constants/permissions'
import {
  deleteCustomer,
  fetchCustomerDetail,
  mergeCustomer,
  transferCustomerOwner,
} from '@/modules/customer/api/customer-api'
import type { CustomerDetail } from '@/modules/customer/model/customer.types'
import { fetchOwnerList } from '@/modules/user/api/user-api'
import {
  OWNER_QUALIFICATION_CONTEXT,
  type OwnerCandidate,
} from '@/modules/user/model/owner.types'
import { ApiError } from '@/shared/api/api-error'
import { useLatestRequest } from '@/shared/composables/use-latest-request'
import { toRouteId } from '@/shared/types/id'
import { messageTip } from '@/shared/utils/feedback'
import { Loader2, Merge, RefreshCw, Trash2, UserRound } from '@lucide/vue'

import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

defineOptions({ name: 'CustomerDetailView' })

const route = useRoute()
const router = useRouter()

const customer = ref<CustomerDetail | null>(null)
const { run: runCustomerDetail, loading } = useLatestRequest<CustomerDetail>()
const errorKind = ref<'invalid-id' | 'not-found' | 'forbidden' | 'network' | null>(null)
const customerId = ref('')
const ownerOptions = ref<OwnerCandidate[]>([])
const transferDialogOpen = ref(false)
const transferOwnerId = ref('')
const transferReason = ref('')
const transferring = ref(false)
const mergeDialogOpen = ref(false)
const mergeSourceCustomerId = ref('')
const mergeReason = ref('')
const merging = ref(false)
const deleting = ref(false)

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

async function loadOwnerOptions(): Promise<void> {
  try {
    ownerOptions.value = await fetchOwnerList({
      permissionCode: PERMISSIONS.customer.transfer,
      qualificationContext: OWNER_QUALIFICATION_CONTEXT.CUSTOMER_OWNER,
    })
  } catch {
    messageTip('加载负责人失败', 'error')
  }
}

async function openTransferDialog(): Promise<void> {
  await loadOwnerOptions()
  transferOwnerId.value = ''
  transferReason.value = ''
  transferDialogOpen.value = true
}

async function submitTransferOwner(): Promise<void> {
  if (!customer.value?.id) {
    messageTip('客户ID不存在', 'error')
    return
  }
  if (!transferOwnerId.value) {
    messageTip('请选择目标负责人', 'warning')
    return
  }
  const reason = transferReason.value.trim()
  if (!reason) {
    messageTip('请输入转移原因', 'warning')
    return
  }
  transferring.value = true
  try {
    await transferCustomerOwner(customer.value.id, {
      newOwnerId: Number(transferOwnerId.value),
      reason,
    })
    messageTip('转移成功', 'success')
    transferDialogOpen.value = false
    await loadCustomer(customerId.value)
  } catch (error) {
    if (error instanceof ApiError && error.code === 409) {
      messageTip('客户归属已变化，请刷新后重试', 'error')
    } else {
      messageTip('转移失败', 'error')
    }
  } finally {
    transferring.value = false
  }
}

function openMergeDialog(): void {
  mergeSourceCustomerId.value = ''
  mergeReason.value = ''
  mergeDialogOpen.value = true
}

async function submitMergeCustomer(): Promise<void> {
  if (!customer.value?.id) {
    messageTip('客户ID不存在', 'error')
    return
  }
  if (!/^\d+$/.test(mergeSourceCustomerId.value)) {
    messageTip('请输入正确的客户ID', 'warning')
    return
  }
  const sourceCustomerId = Number(mergeSourceCustomerId.value)
  if (String(sourceCustomerId) === String(customer.value.id)) {
    messageTip('主客户和被合并客户不能相同', 'warning')
    return
  }
  const reason = mergeReason.value.trim()
  if (!reason) {
    messageTip('请输入合并原因', 'warning')
    return
  }
  merging.value = true
  try {
    const result = await mergeCustomer(customer.value.id, { sourceCustomerId, reason })
    messageTip(
      `合并成功，迁移跟进${result.migratedRemarkCount}条、交易${result.migratedTranCount}条、报价${result.migratedQuoteCount}条`,
      'success',
    )
    mergeDialogOpen.value = false
    await loadCustomer(customerId.value)
  } catch (error) {
    if (error instanceof ApiError && error.code === 404) {
      messageTip('客户不存在或无权限操作', 'error')
    } else if (error instanceof ApiError && error.code === 409) {
      messageTip('客户合并状态已变化，请刷新后重试', 'error')
    } else {
      messageTip('合并失败', 'error')
    }
  } finally {
    merging.value = false
  }
}

async function handleDeleteCustomer(): Promise<void> {
  if (!customer.value?.id || deleting.value) {
    return
  }
  if (!window.confirm('确认删除该客户？')) {
    return
  }
  deleting.value = true
  try {
    await deleteCustomer(customer.value.id)
    messageTip('删除成功', 'success')
    router.push('/dashboard/customer')
  } catch (error) {
    if (error instanceof ApiError && error.code === 422) {
      messageTip('该客户存在业务关系，无法删除', 'error')
    } else if (error instanceof ApiError && error.code === 404) {
      messageTip('客户不存在', 'error')
    } else {
      messageTip('删除失败', 'error')
    }
  } finally {
    deleting.value = false
  }
}

function displayValue(value: unknown): string {
  if (value === null || value === undefined || value === '') {
    return '暂无'
  }
  return String(value)
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
