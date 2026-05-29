<template>
  <el-card class="action-card">
    <el-button type="primary" class="btn" @click="batchExportExcel">全部导出(Excel)</el-button>
    <el-button type="success" class="btn" @click="chooseExportExcel">选择导出(Excel)</el-button>
  </el-card>

  <el-card class="table-card">
    <el-table
        :data="customerList"
        style="width: 100%"
        @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50"/>
      <el-table-column type="index" label="序号" width="80" :index="startIndex"/>
      <el-table-column property="ownerDO.name" label="负责人" show-overflow-tooltip />
      <el-table-column property="activityDO.name" label="所属活动" show-overflow-tooltip />
      <el-table-column label="姓名" show-overflow-tooltip>
        <template #default="scope">
          <a href="javascript:" @click="view(scope.row.id)">{{ scope.row.clueDO.fullName }}</a>
        </template>
      </el-table-column>
      <el-table-column property="appellationDO.typeValue" label="称呼" show-overflow-tooltip />
      <el-table-column property="clueDO.phone" label="手机" show-overflow-tooltip />
      <el-table-column property="clueDO.weixin" label="微信" show-overflow-tooltip />
      <el-table-column property="needLoanDO.typeValue" label="是否贷款" show-overflow-tooltip />
      <el-table-column property="intentionStateDO.typeValue" label="意向状态" show-overflow-tooltip />
      <el-table-column property="stateDO.typeValue" label="线索状态" show-overflow-tooltip />
      <el-table-column property="sourceDO.typeValue" label="线索来源" show-overflow-tooltip />
      <el-table-column property="intentionProductDO.name" label="意向产品" show-overflow-tooltip />
      <el-table-column property="nextContactTime" label="下次联系时间" show-overflow-tooltip />
    </el-table>
  </el-card>
  <p>
    <el-pagination
        background
        layout="prev, pager, next"
        :page-size="pageSize"
        :total="total"
        @prev-click="page"
        @next-click="page"
        @current-change="page"/>
  </p>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { doGet } from "../http/httpRequest.js"
import axios from "axios"
import { getToken, messageTip } from "../util/util.js"

// 响应式数据
const customerList = ref([{
  clueDO: {},
  ownerDO: {},
  activityDO: {},
  appellationDO: {},
  needLoanDO: {},
  intentionStateDO: {},
  stateDO: {},
  sourceDO: {},
  intentionProductDO: {},
}])
const pageSize = ref(0)
const total = ref(0)
const customerIdArray = ref([])
const currentPage = ref(1)

// 计算序号起始值
const startIndex = (index) => {
  return (currentPage.value - 1) * pageSize.value + index + 1
}

// 获取客户分页列表数据
const getData = (current) => {
  doGet("/api/customers", { current }).then(resp => {
    if (resp.data.code === 200) {
      customerList.value = resp.data.data.list
      pageSize.value = resp.data.data.pageSize
      total.value = resp.data.data.total
    }
  })
}

// 处理勾选或取消勾选
const handleSelectionChange = (selectionnDataArray) => {
  customerIdArray.value = selectionnDataArray.map(data => data.id)
}

// 分页函数
const page = (number) => {
  getData(number)
  currentPage.value = number
}

// 导出 Excel
const exportExcel = (ids) => {
  const token = getToken()
  const iframe = document.createElement("iframe")
  iframe.src = ids
    ? `${axios.defaults.baseURL}/api/exportExcel?Authorization=${token}&ids=${ids}`
    : `${axios.defaults.baseURL}/api/exportExcel?Authorization=${token}`
  iframe.style.display = "none"
  document.body.appendChild(iframe)
}

// 批量导出
const batchExportExcel = () => {
  exportExcel(null)
}

// 选择导出
const chooseExportExcel = () => {
  if (customerIdArray.value.length <= 0) {
    messageTip("请选择要导出的数据", "warning")
    return
  }
  const ids = customerIdArray.value.join(",")
  exportExcel(ids)
}

// 组件挂载时加载数据
onMounted(() => {
  getData(1)
})
</script>

<style scoped>
.action-card {
  margin-bottom: 20px;
}
.table-card {
  margin-top: 20px;
}
</style>