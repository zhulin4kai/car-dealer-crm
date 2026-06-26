<template>
  <Dialog :open="open" @update:open="handleOpenChange">
    <DialogContent class="sm:max-w-[560px]">
      <DialogHeader>
        <DialogTitle>交易结算</DialogTitle>
        <DialogDescription v-if="tranNo || customerName">
          {{ [tranNo, customerName].filter(Boolean).join(' · ') }}
        </DialogDescription>
      </DialogHeader>

      <div class="space-y-4">
        <div class="space-y-2">
          <Label>促销活动</Label>
          <Select
            v-model="selectedPromotionId"
            :disabled="loading || settling"
            @update:model-value="handlePromotionChange"
          >
            <SelectTrigger class="w-full">
              <SelectValue placeholder="不使用促销" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem :value="NO_PROMOTION">不使用促销</SelectItem>
              <SelectItem
                v-for="promotion in promotions"
                :key="promotion.id"
                :value="String(promotion.id)"
              >
                {{ promotion.name }}（{{ formatPromotionText(promotion) }}）
              </SelectItem>
            </SelectContent>
          </Select>
          <p v-if="!loading && promotions.length === 0" class="text-xs text-muted-foreground">
            当前交易暂无可用促销。
          </p>
        </div>

        <div v-if="loading" class="rounded-md border p-4 text-sm text-muted-foreground">
          正在计算结算金额...
        </div>
        <div v-else-if="previewError" class="rounded-md border border-destructive/30 bg-destructive/5 p-4 text-sm text-destructive">
          {{ previewError }}
        </div>
        <div v-else-if="previewData" class="rounded-md border p-4 text-sm">
          <div class="flex items-center justify-between py-1">
            <span class="text-muted-foreground">商品原价</span>
            <span class="font-medium">{{ formatAmount(previewData.originalAmount) }}</span>
          </div>
          <div class="flex items-center justify-between py-1">
            <span class="text-muted-foreground">促销优惠</span>
            <span class="font-medium text-green-600">-{{ formatAmount(previewData.discountAmount) }}</span>
          </div>
          <div class="mt-2 flex items-center justify-between border-t pt-3">
            <span class="font-medium">结算金额</span>
            <span class="text-lg font-semibold text-[var(--crm-primary)]">
              {{ formatAmount(previewData.finalAmount) }}
            </span>
          </div>
        </div>
      </div>

      <DialogFooter>
        <Button variant="outline" :disabled="settling" @click="handleOpenChange(false)">取消</Button>
        <Button :disabled="!previewData || loading || settling" @click="submitSettlement">
          <Loader2 v-if="settling" class="mr-2 h-4 w-4 animate-spin" />
          确认结算
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Loader2 } from '@lucide/vue'

import {
  fetchAvailableSettlementPromotions,
  fetchSettlementPreview,
  settleTran,
} from '@/modules/tran/api/tran-api'
import type {
  SettlementPreviewResponse,
  SettlementPromotionOption,
} from '@/modules/tran/model/tran.types'
import { PRODUCT_PROMOTION_TYPE_LABEL } from '@/modules/product/model/product.types'
import type { EntityId } from '@/shared/types/id'
import { formatCurrency, toNumber } from '@/shared/utils/display-format'
import { messageTip } from '@/shared/utils/feedback'

import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

const NO_PROMOTION = '__NO_PROMOTION__'

const props = defineProps<{
  open: boolean
  tranId: EntityId | null
  tranNo?: string
  customerName?: string
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  settled: []
}>()

const promotions = ref<SettlementPromotionOption[]>([])
const selectedPromotionId = ref(NO_PROMOTION)
const previewData = ref<SettlementPreviewResponse | null>(null)
const previewError = ref('')
const loading = ref(false)
const settling = ref(false)
let requestToken = 0

watch(
  () => [props.open, props.tranId] as const,
  ([open]) => {
    if (open) {
      void loadSettlementData()
    }
  },
)

function handleOpenChange(value: boolean): void {
  emit('update:open', value)
}

async function loadSettlementData(): Promise<void> {
  if (!props.tranId) return
  const currentToken = ++requestToken
  loading.value = true
  previewError.value = ''
  previewData.value = null
  selectedPromotionId.value = NO_PROMOTION
  try {
    const [promotionOptions, preview] = await Promise.all([
      fetchAvailableSettlementPromotions(props.tranId),
      fetchSettlementPreview(props.tranId, {}),
    ])
    if (currentToken !== requestToken) return
    promotions.value = promotionOptions
    previewData.value = preview
  } catch (error) {
    if (currentToken !== requestToken) return
    promotions.value = []
    previewError.value = error instanceof Error ? error.message : '结算预览失败'
  } finally {
    if (currentToken === requestToken) {
      loading.value = false
    }
  }
}

async function handlePromotionChange(): Promise<void> {
  await refreshPreview()
}

async function refreshPreview(): Promise<void> {
  if (!props.tranId) return
  const currentToken = ++requestToken
  loading.value = true
  previewError.value = ''
  previewData.value = null
  try {
    const promotionId = selectedPromotionId.value === NO_PROMOTION
      ? undefined
      : selectedPromotionId.value
    const preview = await fetchSettlementPreview(props.tranId, { promotionId })
    if (currentToken !== requestToken) return
    previewData.value = preview
  } catch (error) {
    if (currentToken !== requestToken) return
    previewError.value = error instanceof Error ? error.message : '促销预览失败'
  } finally {
    if (currentToken === requestToken) {
      loading.value = false
    }
  }
}

async function submitSettlement(): Promise<void> {
  if (!props.tranId || !previewData.value) return
  settling.value = true
  try {
    await settleTran(props.tranId, {
      promotionId: previewData.value.promotionId ?? undefined,
      expectedVersion: previewData.value.transactionVersion,
      pricingFingerprint: previewData.value.pricingFingerprint,
    })
    messageTip('结算成功，交易状态已更新为待审批', 'success')
    emit('settled')
    handleOpenChange(false)
  } catch (error) {
    previewData.value = null
    previewError.value = error instanceof Error ? error.message : '结算失败，请重新获取结算预览'
  } finally {
    settling.value = false
  }
}

function formatAmount(value: number | string | null | undefined): string {
  return formatCurrency(value, { fractionDigits: 2 })
}

function formatPromotionText(promotion: SettlementPromotionOption): string {
  const discount = toNumber(promotion.discount)
  const type = (promotion.type || '').toUpperCase()
  if (discount === null) {
    return PRODUCT_PROMOTION_TYPE_LABEL[type as keyof typeof PRODUCT_PROMOTION_TYPE_LABEL] ?? '促销'
  }
  if (type === 'PERCENTAGE') {
    const folded = discount <= 1 ? discount * 10 : discount
    return `${formatPlainNumber(folded)}折`
  }
  const label = PRODUCT_PROMOTION_TYPE_LABEL[type as keyof typeof PRODUCT_PROMOTION_TYPE_LABEL] ?? '促销'
  if (type === 'GIFT' || type === 'MAINTENANCE') {
    return `${label} 成本 ${formatAmount(discount)}`
  }
  return `${label} ${formatAmount(discount)}`
}

function formatPlainNumber(value: number): string {
  return Number.isInteger(value) ? String(value) : value.toFixed(1)
}
</script>
