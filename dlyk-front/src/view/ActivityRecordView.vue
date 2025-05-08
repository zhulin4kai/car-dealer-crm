<template>
  <el-form ref="activityRefForm" :model="activityQuery" label-width="110px" :rules="activityRules">
    <el-form-item label="负责人" prop="ownerId">
      <el-select v-model="activityQuery.ownerId" placeholder="请选择" class="width">
        <el-option
            v-for="item in ownerOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"/>
      </el-select>
    </el-form-item>

    <el-form-item label="活动名称" prop="name">
      <el-input v-model="activityQuery.name" />
    </el-form-item>

    <el-form-item label="开始时间" prop="startTime">
      <el-date-picker
          v-model="activityQuery.startTime"
          type="datetime"
          placeholder="请选择创建时间"
          value-format="YYYY-MM-DD HH:mm:ss" style="width:100%;"/>
    </el-form-item>

    <el-form-item label="结束时间"  prop="endTime">
      <el-date-picker
          v-model="activityQuery.endTime"
          type="datetime"
          placeholder="请选择创建时间"
          value-format="YYYY-MM-DD HH:mm:ss" style="width:100%;"/>
    </el-form-item>

    <el-form-item label="活动预算" prop="cost">
      <el-input v-model="activityQuery.cost" />
    </el-form-item>

    <el-form-item label="活动描述" prop="description">
      <el-input
          v-model="activityQuery.description"
          :rows="6"
          type="textarea"
          placeholder="请输入活动描述"/>
    </el-form-item>

    <el-form-item>
      <el-button type="primary" @click="activitySubmit">提 交</el-button>
      <el-button @click="goBack">返 回</el-button>
    </el-form-item>

  </el-form>
</template>

<script>
import {defineComponent} from 'vue'
import {doGet, doPost, doPut} from "../http/httpRequest.js";
import {messageTip} from "../util/util.js";

export default defineComponent({
  name: "ActivityRecordView",

  //注入
  inject : ['reload'],

  data() {
    return {
      //市场活动表单对象，初始值是空
      activityQuery : {},
      //市场活动表单验证规则
      activityRules : {
        ownerId : [
          { required: true, message: '请选择负责人', trigger: 'blur' }
        ],
        name : [
          { required: true, message: '请输入活动名称', trigger: 'blur' }
        ],
        startTime : [
          { required: true, message: '请选择开始时间', trigger: 'blur' }
        ],
        endTime : [
          { required: true, message: '请选择结束时间', trigger: 'blur' }
        ],
        cost : [
          { required: true, message: '请输入活动预算', trigger: 'blur' },
          //正则表达式，从网上找，或者AI工具找，找到后需要测试一下，因为有可能找到的正则有问题
          { pattern : /^[0-9]+(\.[0-9]{2})?$/, message: '活动预算必须是整数或者两位小数', trigger: 'blur'}
        ],
        description : [
          { required: true, message: '请输入活动描述', trigger: 'blur' },
          { min: 5, max: 255, message: '活动描述长度为5-255个字符', trigger: 'blur' }
        ]
      },
      //负责人的下拉选项，初始值是空
      ownerOptions : [{}]
    }
  },

  mounted() {
    this.loadOwner();
    //加载要编辑的数据（由于录入和编辑共用一个页面，所以要判断是 编辑还是录入）
    this.loadActivity();
  },

  methods : {
    //加载负责人
    loadOwner() {
      doGet("/api/owner", {}).then(resp => {
        if (resp.data.code === 200)  {
          this.ownerOptions = resp.data.data;
        }
      })
    },

    //返回
    goBack() {
      this.$router.go(-1);
    },

    //市场活动提交
    activitySubmit()  {
      let formData = new FormData();
      for (let field in this.activityQuery) {
        console.log(field  + " -- " + this.activityQuery[field])
        if (this.activityQuery[field])  { //this.activityQuery[field]有值，this.activityQuery[field]不是空，this.activityQuery[field]存在
          formData.append(field, this.activityQuery[field]);
        }
      }
      this.$refs.activityRefForm.validate( (isValid) => {
        if (isValid) {
          if (this.activityQuery.id > 0) { /*编辑*/
            doPut("/api/activity", formData).then(resp => {
              if (resp.data.code === 200) {
                messageTip("编辑成功", "success");
                //跳转到市场活动列表页
                this.$router.push("/dashboard/activity");
              } else {
                messageTip("编辑失败", "error");
              }
            })
          } else  {
            doPost("/api/activity", formData).then(resp => {/*新增*/
              if (resp.data.code === 200) {
                messageTip("提交成功", "success");
                //跳转到市场活动列表页
                this.$router.push("/dashboard/activity");
              } else {
                messageTip("提交失败", "error");
              }
            })
          }
        }
      })
    },

    //加载要编辑的市场活动数据
    loadActivity() {
      let id =  this.$route.params.id;
      if (id) { //id存在，id有值，id不为空，说明的编辑
        doGet("/api/activity/" + id, {}).then(resp => {
          if (resp.data.code === 200) {
            this.activityQuery = resp.data.data;
          }
        })
      }
    }
  }
})
</script>

<style scoped>
.width {
  width: 100%;
}
</style>