<template>
  <div class="p-5 bg-muted/30 min-h-screen">
    <!-- 活动详情卡片 -->
    <Card class="mb-5">
      <CardHeader class="border-b bg-muted/50 flex-row justify-between items-center">
        <CardTitle class="text-lg font-semibold">活动详情</CardTitle>
        <div class="flex gap-2">
          <Button variant="secondary" size="sm" @click="goBack">返 回</Button>
        </div>
      </CardHeader>
      <CardContent>
        <!-- 基本信息区域 -->
        <div class="mb-8 last:mb-0">
          <h4 class="text-base font-semibold mb-5 pb-2 border-b-2 border-border relative before:absolute before:bottom-[-2px] before:left-0 before:w-[60px] before:h-[2px] before:bg-primary">基本信息</h4>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div class="flex items-start p-3 bg-background rounded-md border border-border transition-all hover:border-primary/50 hover:shadow-sm">
              <span class="font-semibold text-muted-foreground whitespace-nowrap mr-3 min-w-[80px]">活动ID：</span>
              <span class="flex-1 break-all font-semibold text-primary">{{ activityDetail.id || '暂无' }}</span>
            </div>
            <div class="flex items-start p-3 bg-background rounded-md border border-border transition-all hover:border-primary/50 hover:shadow-sm">
              <span class="font-semibold text-muted-foreground whitespace-nowrap mr-3 min-w-[80px]">负责人：</span>
              <span class="flex-1 break-all">{{ activityDetail.ownerDO?.name || '暂无' }}</span>
            </div>
            <div class="flex items-start p-3 bg-background rounded-md border border-border transition-all hover:border-primary/50 hover:shadow-sm">
              <span class="font-semibold text-muted-foreground whitespace-nowrap mr-3 min-w-[80px]">活动预算：</span>
              <span class="flex-1 break-all font-semibold text-primary">{{ activityDetail.cost || '暂无' }}</span>
            </div>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-12 gap-4 mt-4">
            <div class="md:col-span-6 flex items-start p-3 bg-background rounded-md border border-border transition-all hover:border-primary/50 hover:shadow-sm">
              <span class="font-semibold text-muted-foreground whitespace-nowrap mr-3 min-w-[80px]">活动名称：</span>
              <span class="flex-1 break-all">{{ activityDetail.name || '暂无' }}</span>
            </div>
            <div class="md:col-span-3 flex items-start p-3 bg-background rounded-md border border-border transition-all hover:border-primary/50 hover:shadow-sm">
              <span class="font-semibold text-muted-foreground whitespace-nowrap mr-3 min-w-[80px]">开始时间：</span>
              <span class="flex-1 break-all">{{ activityDetail.startTime || '暂无' }}</span>
            </div>
            <div class="md:col-span-3 flex items-start p-3 bg-background rounded-md border border-border transition-all hover:border-primary/50 hover:shadow-sm">
              <span class="font-semibold text-muted-foreground whitespace-nowrap mr-3 min-w-[80px]">结束时间：</span>
              <span class="flex-1 break-all">{{ activityDetail.endTime || '暂无' }}</span>
            </div>
          </div>
          <div v-if="activityDetail.description" class="mt-4">
            <div class="flex flex-col items-start p-3 bg-background rounded-md border border-border transition-all hover:border-primary/50 hover:shadow-sm">
              <span class="font-semibold text-muted-foreground whitespace-nowrap mb-2">活动描述：</span>
              <div class="mt-2 p-3 bg-muted rounded w-full leading-relaxed">{{ activityDetail.description }}</div>
            </div>
          </div>
        </div>

        <!-- 管理信息区域 -->
        <div class="mb-8 last:mb-0">
          <h4 class="text-base font-semibold mb-5 pb-2 border-b-2 border-border relative before:absolute before:bottom-[-2px] before:left-0 before:w-[60px] before:h-[2px] before:bg-primary">管理信息</h4>
          <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div class="flex items-start p-3 bg-background rounded-md border border-border transition-all hover:border-primary/50 hover:shadow-sm">
              <span class="font-semibold text-muted-foreground whitespace-nowrap mr-3 min-w-[80px]">创建时间：</span>
              <span class="flex-1 break-all">{{ activityDetail.createTime || '暂无' }}</span>
            </div>
            <div class="flex items-start p-3 bg-background rounded-md border border-border transition-all hover:border-primary/50 hover:shadow-sm">
              <span class="font-semibold text-muted-foreground whitespace-nowrap mr-3 min-w-[80px]">创建人：</span>
              <span class="flex-1 break-all">{{ activityDetail.createByDO?.name || '暂无' }}</span>
            </div>
            <div class="flex items-start p-3 bg-background rounded-md border border-border transition-all hover:border-primary/50 hover:shadow-sm">
              <span class="font-semibold text-muted-foreground whitespace-nowrap mr-3 min-w-[80px]">编辑时间：</span>
              <span class="flex-1 break-all">{{ activityDetail.editTime || '暂无' }}</span>
            </div>
            <div class="flex items-start p-3 bg-background rounded-md border border-border transition-all hover:border-primary/50 hover:shadow-sm">
              <span class="font-semibold text-muted-foreground whitespace-nowrap mr-3 min-w-[80px]">编辑人：</span>
              <span class="flex-1 break-all">{{ activityDetail.editByDO?.name || '暂无' }}</span>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>

    <!-- 备注记录表单卡片 -->
    <Card class="mb-5">
      <CardHeader class="border-b bg-muted/50">
        <CardTitle class="text-lg font-semibold">添加活动备注</CardTitle>
      </CardHeader>
      <CardContent>
        <form @submit.prevent="onSubmitRemark" class="space-y-4">
          <div class="space-y-2">
            <Label>活动备注</Label>
            <Textarea
              v-model="values.noteContent"
              :rows="6"
              placeholder="请输入详细的活动备注内容..."
            />
            <p v-if="errors.noteContent" class="text-sm text-destructive">{{ errors.noteContent }}</p>
          </div>

          <div class="flex gap-2">
            <Button type="submit" :disabled="submitting">
              <Loader2 v-if="submitting" class="size-4 animate-spin mr-1" />
              <Pencil v-else class="size-4 mr-1" />
              提交备注
            </Button>
            <Button type="button" variant="outline" @click="resetRemarkForm">
              <RotateCw class="size-4 mr-1" />
              重置
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>

    <!-- 备注记录列表卡片 -->
    <Card class="mb-5">
      <CardHeader class="border-b bg-muted/50">
        <CardTitle class="text-lg font-semibold">活动备注记录</CardTitle>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead class="w-[60px] text-center">序号</TableHead>
              <TableHead class="min-w-[200px]">备注内容</TableHead>
              <TableHead class="w-[160px] text-center">备注时间</TableHead>
              <TableHead class="w-[100px] text-center">备注人</TableHead>
              <TableHead class="w-[160px] text-center">编辑时间</TableHead>
              <TableHead class="w-[100px] text-center">编辑人</TableHead>
              <TableHead class="w-[120px] text-center">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-for="(remark, index) in activityRemarkList" :key="remark.id">
              <TableCell class="text-center">{{ index + 1 }}</TableCell>
              <TableCell class="truncate max-w-[300px]">{{ remark.noteContent }}</TableCell>
              <TableCell class="text-center">{{ remark.createTime }}</TableCell>
              <TableCell class="text-center">{{ remark.createByDO?.name }}</TableCell>
              <TableCell class="text-center">{{ remark.editTime }}</TableCell>
              <TableCell class="text-center">{{ remark.editByDO?.name }}</TableCell>
              <TableCell class="text-center">
                <div class="flex justify-center gap-1">
                  <Button variant="link" size="sm" class="text-destructive" @click="del(remark.id)">删除</Button>
                </div>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>

        <div class="flex justify-center mt-5">
          <DataTablePagination :page-size="pageSize" :total="total" @change="toPage" />
        </div>
      </CardContent>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import * as z from 'zod'
import { Pencil, RotateCw, Loader2 } from '@lucide/vue'
import { messageTip, messageConfirm } from '@/shared/utils/feedback'
import {
  createActivityRemark,
  deleteActivityRemark,
  fetchActivityById,
  fetchActivityRemarkPage,
} from '@/modules/activity/api/activity-api'
import type { Activity, ActivityRemark } from '@/modules/activity/model/activity.types'
import { toRouteId } from '@/shared/types/id'

import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Textarea } from '@/components/ui/textarea'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Label } from '@/components/ui/label'
import DataTablePagination from '@/shared/ui/DataTablePagination.vue'

// 路由
const route = useRoute()
const router = useRouter()

// 活动详情对象
const activityDetail = ref<Activity>({
  ownerDO: {},
  createByDO: {},
  editByDO: {}
})

// 活动备注的列表对象
const activityRemarkList = ref<ActivityRemark[]>([])

// 分页
const pageSize = ref(10)
const total = ref(0)

// 加载状态
const submitting = ref(false)

// 备注表单校验规则 (zod)
const remarkSchema = toTypedSchema(z.object({
  noteContent: z.string().min(5, '活动备注长度为5-255个字符').max(255, '活动备注长度为5-255个字符'),
}))

const { handleSubmit, errors, values, resetForm } = useForm({
  validationSchema: remarkSchema,
  initialValues: {
    noteContent: '',
  },
})

// 重置备注表单
const resetRemarkForm = () => {
  resetForm()
}

// 返回上一页
const goBack = () => {
  router.go(-1)
}

const loadActivityDetail = async () => {
  const id = toRouteId(route.params.id)
  if (!id) return
  try {
    activityDetail.value = await fetchActivityById(id)
    if (!activityDetail.value.ownerDO) {
      activityDetail.value.ownerDO = {}
    }
    if (!activityDetail.value.createByDO) {
      activityDetail.value.createByDO = {}
    }
    if (!activityDetail.value.editByDO) {
      activityDetail.value.editByDO = {}
    }
  } catch (error) {
    messageTip("加载活动详情失败", "error")
  }
}

const onSubmitRemark = handleSubmit(async (formData) => {
  submitting.value = true
  try {
    if (!activityDetail.value.id) throw new Error('活动ID不存在')
    await createActivityRemark(activityDetail.value.id, formData.noteContent)
    messageTip("提交成功", "success")
    await loadActivityRemarkList(1)
    resetRemarkForm()
  } catch (error) {
    messageTip("提交失败", "error")
  } finally {
    submitting.value = false
  }
})

const loadActivityRemarkList = async (current: number) => {
  const activityId = toRouteId(route.params.id)
  if (!activityId) return
  try {
    const resp = await fetchActivityRemarkPage(current, activityId)
    activityRemarkList.value = resp.list || []
    pageSize.value = resp.pageSize || 10
    total.value = resp.total || 0
  } catch (error) {
    messageTip("加载备注列表失败", "error")
  }
}

// 分页函数
const toPage = (current: number) => {
  loadActivityRemarkList(current)
}

const del = async (id: number | string) => {
  try {
    await messageConfirm("您确定要删除该数据吗？")
  } catch {
    messageTip("取消删除", "info")
    return
  }

  try {
    await deleteActivityRemark(id)
    messageTip("删除成功", "success")
    await loadActivityRemarkList(1)
  } catch (error) {
    messageTip("删除失败", "error")
  }
}

// 监听路由参数变化
watch(() => route.params.id, (newId) => {
  if (newId) {
    loadActivityDetail()
    loadActivityRemarkList(1)
  }
})

// 组件挂载时执行
onMounted(() => {
  loadActivityDetail()
  loadActivityRemarkList(1)
})
</script>
