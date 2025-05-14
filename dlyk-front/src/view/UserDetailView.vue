<template>
  <el-form ref="loginRefForm" :model="userDetail" label-width="120px">
    <el-form-item label="ID">
      <div class="detail">&nbsp;{{userDetail.id}}</div>
    </el-form-item>

    <el-form-item label="账号">
      <div class="detail">&nbsp;{{userDetail.loginAct}}</div>
    </el-form-item>

    <el-form-item label="密码">
      <div class="detail">&nbsp;******</div>
    </el-form-item>

    <el-form-item label="姓名">
      <div class="detail">&nbsp;{{userDetail.name}}</div>
    </el-form-item>

    <el-form-item label="手机">
      <div class="detail">&nbsp;{{userDetail.phone}}</div>
    </el-form-item>

    <el-form-item label="邮箱">
      <div class="detail">&nbsp;{{userDetail.email}}</div>
    </el-form-item>

    <el-form-item label="账号未过期">
      <div class="detail">&nbsp;{{userDetail.accountNoExpired === 1 ? '是' : '否'}}</div>
    </el-form-item>

    <el-form-item label="密码未过期">
      <div class="detail">&nbsp;{{userDetail.credentialsNoExpired === 1 ? '是' : '否'}}</div>
    </el-form-item>

    <el-form-item label="账号未锁定">
      <div class="detail">&nbsp;{{userDetail.accountNoLocked === 1 ? '是' : '否'}}</div>
    </el-form-item>

    <el-form-item label="账号是否启用">
      <div class="detail">&nbsp;{{userDetail.accountEnabled  === 1 ? '是' : '否'}}</div>
    </el-form-item>

    <el-form-item label="创建时间">
      <div class="detail">&nbsp;{{userDetail.createTime}}</div>
    </el-form-item>

    <el-form-item label="创建人">
      <div class="detail">&nbsp;{{userDetail.createByDO.name}}</div>
    </el-form-item>

    <el-form-item label="编辑时间">
      <div class="detail">&nbsp;{{userDetail.editTime}}</div>
    </el-form-item>

    <el-form-item label="编辑人">
      <div class="detail">&nbsp;{{userDetail.editByDO.name}}</div>
    </el-form-item>

    <el-form-item label="最近登录时间">
      <div class="detail">&nbsp;{{userDetail.lastLoginTime}}</div>
    </el-form-item>

    <el-form-item>
      <el-button type="success" @click="goBack">返 回</el-button>
    </el-form-item>
  </el-form>

</template>

<script>
import {defineComponent} from 'vue'
import {doGet} from "../http/httpRequest.js";

export default defineComponent({
  name: "UserDetailView",

  data() {
    return {
      //用户详情对象，初始值是空
      userDetail : {
        createByDO : {},
        editByDO : {}
      },
    }
  },

  mounted() {
    this.loadUserDetail();
  },

  methods : {
    //加载用户详情
    loadUserDetail() {
      let id = this.$route.params.id; //id名字要和动态路由中配置的 path: 'user/:id',名字相同
      doGet("/api/user/" + id, {}).then(resp => {
        if (resp.data.code === 200) {
          this.userDetail = resp.data.data;
          if (!this.userDetail.createByDO) {
            this.userDetail.createByDO = {};
          }
          if (!this.userDetail.editByDO) {
            this.userDetail.editByDO = {};
          }
        }
      })
    },

    //返回
    goBack() {
      this.$router.go(-1);
    }
  }
})
</script>

<style scoped>
.detail {
  background-color: #e9eae3;
  width: 20%;
  padding-left: 15px;
}
</style>