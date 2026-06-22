<template>
  <div class="p-5">
    <Card class="max-w-[1000px] mx-auto">
      <CardHeader class="flex flex-row items-center justify-between space-y-0">
        <CardTitle>交易审批</CardTitle>
        <Badge class="bg-yellow-600 text-white">待审批</Badge>
      </CardHeader>
      <CardContent>
        <!-- Transaction Basic Info -->
        <div class="border rounded-md mb-5">
          <div class="grid grid-cols-[120px_1fr_120px_1fr]">
            <div class="px-4 py-2 bg-muted font-medium text-sm border-b border-r">交易编号</div>
            <div class="px-4 py-2 text-sm border-b border-r">{{ tranDetail.tranNo }}</div>
            <div class="px-4 py-2 bg-muted font-medium text-sm border-b border-r">客户名称</div>
            <div class="px-4 py-2 text-sm border-b">{{ tranDetail.customerName }}</div>

            <div class="px-4 py-2 bg-muted font-medium text-sm border-b border-r">交易金额</div>
            <div class="px-4 py-2 text-sm border-b border-r">
              <span v-if="tranDetail.stage === TRAN_STAGE.QUOTATION">?</span>
              <span v-else>&yen;{{ tranDetail.amount }}</span>
            </div>
            <div class="px-4 py-2 bg-muted font-medium text-sm border-b border-r">创建时间</div>
            <div class="px-4 py-2 text-sm border-b">{{ tranDetail.createTime }}</div>

            <div class="px-4 py-2 bg-muted font-medium text-sm border-b border-r">预计交付日期</div>
            <div class="px-4 py-2 text-sm border-b border-r">{{ tranDetail.expectedDeliveryDate }}</div>
            <div class="px-4 py-2 bg-muted font-medium text-sm border-b border-r">最后更新时间</div>
            <div class="px-4 py-2 text-sm border-b">{{ tranDetail.updateTime }}</div>

            <div class="px-4 py-2 bg-muted font-medium text-sm border-r">交易描述</div>
            <div class="px-4 py-2 text-sm col-span-3">{{ tranDetail.description }}</div>
          </div>
        </div>

        <!-- Product Info -->
        <div class="text-base font-bold my-5 pl-2.5 border-l-4 border-primary">产品信息</div>
        <Table class="mb-5">
          <TableHeader>
            <TableRow>
              <TableHead class="w-[80px]">序号</TableHead>
              <TableHead class="min-w-[300px]">产品名称</TableHead>
              <TableHead class="w-[120px]">数量</TableHead>
              <TableHead class="w-[140px]">单价</TableHead>
              <TableHead class="w-[140px]">小计</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-for="(product, index) in tranDetail.products" :key="index">
              <TableCell>{{ index + 1 }}</TableCell>
              <TableCell>{{ product.productName }}</TableCell>
              <TableCell>{{ product.quantity }}</TableCell>
              <TableCell>&yen;{{ product.price }}</TableCell>
              <TableCell>&yen;{{ product.price * product.quantity }}</TableCell>
            </TableRow>
          </TableBody>
        </Table>

        <!-- Approval Form -->
        <form class="mt-8 max-w-[600px] space-y-4" @submit.prevent="onSubmit">
          <div class="space-y-2">
            <Label>审批结果</Label>
            <RadioGroup v-model="values.approved" class="flex items-center gap-6">
              <div class="flex items-center space-x-2">
                <RadioGroupItem id="approve-yes" value="true" />
                <Label for="approve-yes" class="font-normal cursor-pointer">通过</Label>
              </div>
              <div class="flex items-center space-x-2">
                <RadioGroupItem id="approve-no" value="false" />
                <Label for="approve-no" class="font-normal cursor-pointer">拒绝</Label>
              </div>
            </RadioGroup>
            <p v-if="errors.approved" class="text-sm text-destructive">{{ errors.approved }}</p>
          </div>

          <div class="space-y-2">
            <Label>审批意见</Label>
            <Textarea v-model="values.comment" :rows="4" placeholder="请输入审批意见" />
            <p v-if="errors.comment" class="text-sm text-destructive">{{ errors.comment }}</p>
          </div>

          <div class="flex gap-2 pt-2">
            <Button v-has-permission="PERMISSIONS.tran.approve" type="submit" :disabled="isSubmitting">提交审批</Button>
            <Button type="button" variant="outline" @click="goBack">返回</Button>
          </div>
        </form>
      </CardContent>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { PERMISSIONS } from '@/shared/constants/permissions'
import { ref, onMounted, watch } from 'vue'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import * as z from 'zod'
import { useRoute, useRouter } from 'vue-router'
import { messageTip } from '@/shared/utils/feedback'
import { getTranDetail, getTranProducts, approveTran } from '@/modules/tran/api/tran-api'
import { TRAN_STAGE, normalizeTranStage } from '@/modules/tran/model/tran-stage'
import type { TranProduct } from '@/modules/tran/model/tran.types'
import { toRouteId } from '@/shared/types/id'

import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Textarea } from '@/components/ui/textarea'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { Label } from '@/components/ui/label'

const route = useRoute()
const router = useRouter()

interface ApprovalTranView {
  tranNo: string
  customerName: string
  amount: number
  stage: string
  createTime: string
  updateTime: string
  expectedDeliveryDate: string
  description: string
  products: TranProduct[]
}

const tranDetail = ref<ApprovalTranView>({
  tranNo: '',
  customerName: '',
  amount: 0,
  stage: '',
  createTime: '',
  updateTime: '',
  expectedDeliveryDate: '',
  description: '',
  products: []
})

// Form schema
const approveSchema = toTypedSchema(z.object({
  approved: z.string().min(1, '请选择审批结果'),
  comment: z.string().min(5, '审批意见不能少于5个字符'),
}))

const { handleSubmit, errors, values, isSubmitting } = useForm({
  validationSchema: approveSchema,
  initialValues: {
    approved: 'true',
    comment: '',
  },
})

// Fetch transaction detail
const fetchTranDetail = async () => {
  const id = toRouteId(route.params.id)
  if (!id) return
  try {
    const data = await getTranDetail(id)
    tranDetail.value = {
      tranNo: data.tranNo || '',
      customerName: data.customerName || '',
      amount: data.money || 0,
      stage: normalizeTranStage(data.stage),
      createTime: data.createTime || '',
      updateTime: data.editTime || '',
      expectedDeliveryDate: data.expectedDate || '',
      description: data.description || '',
      products: data.products || []
    }
  } catch {
    messageTip('获取交易详情失败', 'error')
  }
}

// Fetch product details
const fetchProducts = async () => {
  const id = toRouteId(route.params.id)
  if (!id) return
  try {
    tranDetail.value.products = await getTranProducts(id)
  } catch {
    messageTip('获取产品详情失败', 'error')
  }
}

// Submit approval
const onSubmit = handleSubmit(async () => {
  const id = toRouteId(route.params.id)
  if (!id) return
  try {
    const approveData = {
      approved: values.approved === 'true',
      comment: values.comment,
    }
    await approveTran(id, approveData)
    messageTip('审批提交成功', 'success')
    goBack()
  } catch {
    messageTip('审批提交失败', 'error')
  }
})

// Go back
const goBack = () => {
  router.push('/dashboard/tran')
}

// Watch route params
watch(() => route.params.id, async (newId) => {
  if (newId) {
    await fetchTranDetail()
    await fetchProducts()
  }
})

onMounted(async () => {
  if (!route.params.id) {
    messageTip('缺少交易ID参数', 'error')
    return
  }

  await fetchTranDetail()
  await fetchProducts()
})
</script>
