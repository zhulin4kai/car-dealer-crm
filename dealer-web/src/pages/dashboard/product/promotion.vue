<template>
  <div class="promotion-container">
    <el-card class="operation-bar">
      <el-button type="primary" plain @click="goBack">返 回</el-button>
      <el-button type="primary" @click="handleAdd">新增促销</el-button>
    </el-card>

    <el-card class="table-card">
      <el-table 
        :data="promotionList" 
        style="width: 100%"
        v-loading="loading"
        element-loading-text="加载中..."
      >
        <el-table-column prop="id" label="ID" show-overflow-tooltip />
        <el-table-column prop="name" label="促销名称" show-overflow-tooltip />
        <el-table-column prop="type" label="促销类型" show-overflow-tooltip>
          <template #default="scope">
            <el-tag :type="getPromotionTypeTag(scope.row.type)">
              {{ scope.row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="discount" label="折扣/金额" show-overflow-tooltip>
          <template #default="scope">
            {{ scope.row.type === '折扣' ? scope.row.discount + '折' : '¥' + scope.row.discount }}
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" show-overflow-tooltip>
          <template #default="scope">
            {{ formatDateTime(scope.row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="endTime" label="结束时间" show-overflow-tooltip>
          <template #default="scope">
            {{ formatDateTime(scope.row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" show-overflow-tooltip>
          <template #default="scope">
            <el-tag :type="getStatusTag(scope.row.status)">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" show-overflow-tooltip>
          <template #default="scope">
            <el-button type="default" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-pagination
      background
      layout="prev, pager, next"
      :page-size="pageSize"
      :total="total"
      @prev-click="handleCurrentChange"
      @next-click="handleCurrentChange"
      @current-change="handleCurrentChange"
        style="margin-top: 12px; width: 100%;"
    />
    

    <!-- 促销表单对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '新增促销' : '编辑促销'"
      width="30%"
    >
      <el-form :model="promotionForm" label-width="100px">
        <el-form-item label="促销名称">
          <el-input v-model="promotionForm.name" />
        </el-form-item>
        <el-form-item label="促销类型">
          <el-select v-model="promotionForm.type">
            <el-option label="折扣" value="折扣" />
            <el-option label="满减" value="满减" />
            <el-option label="直降" value="直降" />
          </el-select>
        </el-form-item>
        <el-form-item label="折扣/金额">
          <el-input-number 
            v-model="promotionForm.discount" 
            :precision="promotionForm.type === '折扣' ? 1 : 2"
            :step="promotionForm.type === '折扣' ? 0.1 : 1"
            :min="0"
            :max="promotionForm.type === '折扣' ? 10 : 999999"
          />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="promotionForm.startTime"
            type="datetime"
            placeholder="选择开始时间"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="promotionForm.endTime"
            type="datetime"
            placeholder="选择结束时间"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="promotionForm.status">
            <el-option label="未开始" value="未开始" />
            <el-option label="进行中" value="进行中" />
            <el-option label="已结束" value="已结束" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { createPromotion, deletePromotion, getPromotionList, updatePromotion } from '@/modules/product/api/product-api'
import type { ProductForm, ProductPromotion } from '@/modules/product/model/product.types'
import { messageConfirm, messageTip } from '@/shared/utils/feedback'

defineOptions({ name: 'ProductPromotionView' })

const promotionList = ref<ProductPromotion[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const dialogType = ref<'add' | 'edit'>('add')
const promotionForm = ref<ProductForm>({ name: '', type: '折扣', discount: 0, startTime: '', endTime: '', status: '未开始' })

function getPromotionTypeTag(type: string): string {
  return ({ 折扣: 'success', 满减: 'warning', 直降: 'danger' } as Record<string, string>)[type] ?? 'info'
}

function getStatusTag(status: string): string {
  return ({ 未开始: 'info', 进行中: 'success', 已结束: 'danger' } as Record<string, string>)[status] ?? 'info'
}

function formatDateTime(dateTimeStr: string): string {
  if (!dateTimeStr) return ''
  const date = new Date(dateTimeStr)
  const year = String(date.getFullYear())
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return year + '-' + month + '-' + day + ' ' + hours + ':' + minutes + ':' + seconds
}

async function loadPromotions(): Promise<void> {
  try {
    loading.value = true
    const res = await getPromotionList({ page: currentPage.value, size: pageSize.value })
    promotionList.value = res.list
    total.value = res.total
  } catch {
    messageTip('加载促销列表失败', 'error')
  } finally {
    loading.value = false
  }
}

function handleAdd(): void {
  dialogType.value = 'add'
  promotionForm.value = { name: '', type: '折扣', discount: 0, startTime: '', endTime: '', status: '未开始' }
  dialogVisible.value = true
}

function handleEdit(row: ProductPromotion): void {
  dialogType.value = 'edit'
  promotionForm.value = { ...row }
  dialogVisible.value = true
}

async function handleDelete(row: ProductPromotion): Promise<void> {
  try {
    await messageConfirm('确认删除该促销活动？')
    if (row.id === undefined) return
    await deletePromotion(row.id)
    messageTip('删除成功', 'success')
    await loadPromotions()
  } catch (error) {
    if (error !== 'cancel') messageTip('删除失败', 'error')
  }
}

async function handleSubmit(): Promise<void> {
  try {
    if (dialogType.value === 'add') {
      await createPromotion(promotionForm.value)
      messageTip('新增成功', 'success')
    } else if (promotionForm.value.id !== undefined) {
      await updatePromotion(String(promotionForm.value.id), promotionForm.value)
      messageTip('编辑成功', 'success')
    }
    dialogVisible.value = false
    await loadPromotions()
  } catch {
    messageTip('操作失败', 'error')
  }
}

function handleCurrentChange(val: number): void {
  currentPage.value = val
  void loadPromotions()
}

function goBack(): void {
  window.history.length > 1 ? window.history.back() : (window.location.href = '/dashboard/product')
}

onMounted(() => { void loadPromotions() })
</script>

<style scoped>
.promotion-container {
  padding: 20px;
}

.operation-bar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

.el-table {
  margin-top: 10px;
  width: 100%;
}

.el-pagination {
  margin-top: 12px;
  width: 100%;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style> 