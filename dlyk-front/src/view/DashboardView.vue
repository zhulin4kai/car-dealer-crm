<template>
  <el-container>
    <!--左侧-->
    <el-aside :width="isCollapse ? '64px' : '200px'">
      <div class="menuTitle" @click="backToHome()">@电子商务管理系统</div>
      <el-menu
          active-text-color="#ffd04b"
          background-color="#4c393b"
          class="el-menu-vertical-demo"
          :default-active="currentRouterPath"
          text-color="#fff"
          style="border-right: solid 0px;"
          :collapse="isCollapse"
          :collapse-transition="false"
          :router="true"
          :unique-opened="false">

        <el-sub-menu :index="String(index)" v-for="(menuPermission, index) in user.menuPermissionList" :key="menuPermission.id">
          <template #title>
            <el-icon><component :is="menuPermission.icon"></component></el-icon>
            <span> {{menuPermission.name}} </span>
          </template>
          <el-menu-item v-for="subPermission in menuPermission.subPermissionList" :key="subPermission.id" :index="subPermission.url">
            <el-icon><component :is="subPermission.icon"></component></el-icon>
            {{subPermission.name}}
          </el-menu-item>
        </el-sub-menu>
      </el-menu>

    </el-aside>

    <!--右侧-->
    <el-container class="rightContent">
      <!--右侧：上-->
      <el-header>
        <el-icon class="show" @click="showMenu"><Fold /></el-icon>

        <el-dropdown :hide-on-click="false">
          <span class="el-dropdown-link">
            <div class="avatar">{{ getUserFirstChar }}</div>
            {{ user.name }}
            <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <!--
              <el-dropdown-item>我的资料</el-dropdown-item>
              <el-dropdown-item>修改密码</el-dropdown-item>
              -->
              <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

      </el-header>

      <!--右侧：中-->
      <el-main>
        <router-view v-if="isRouterAlive"/>
      </el-main>

      <!--右侧：下-->
      <el-footer>徐州工程学院@信息工程学院（大数据学院）</el-footer>
    </el-container>
  </el-container>
</template>

<script>
import {defineComponent} from 'vue'
import {doGet} from "../http/httpRequest.js";
import {messageConfirm, messageTip, removeToken} from "../util/util.js";
import { backtopEmits } from 'element-plus';

export default defineComponent({
  //定义组件名
  name: "DashboardView",

  data() {
    return {
      //控制左侧菜单左右的展开和折叠，true是折叠，false是展开
      isCollapse : false,
      //登录用户对象，初始值是空
      user : {},
      //控制仪表盘页面右侧内容体是否显示，true显示，false不显示
      isRouterAlive : true,
      //当前访问的路由路径
      currentRouterPath : ''
    }
  },

  //vue的生命周期中的一个函数钩子，该钩子是在页面渲染后执行
  mounted() {
    //加载当前登录用户
    this.loadLoginUser();
    this.loadCurrentRouterPath();
  },

  methods : {
    //左侧菜单左右展开和折叠
    showMenu() {
      this.isCollapse = !this.isCollapse;
    },

    //加载当前登录用户
    loadLoginUser() {
      doGet("/api/login/info", {}).then( (resp) => {
        console.log(resp)
        this.user = resp.data.data;
      })
    },

    //退出登录
    logout() {
      doGet("/api/logout", {}).then(resp => {
        if (resp.data.code === 200) {
          removeToken();
          messageTip("退出成功", "success");
          //跳到登录页
          window.location.href = "/";
        } else {
          messageConfirm("退出异常，是否要强制退出？").then(() => { //用户点击"确定"按钮就会触发then函数
            //既然后端验证token未通过，那么前端的token肯定是有问题的，那没必要存储在浏览器中，直接删除一下
            removeToken();
            //跳到登录页
            window.location.href = "/";
          }).catch(() => { //用户点击"取消"按钮就会触发then函数
            messageTip("取消强制退出", "warning");
          })
        }
      })
    },

    backToHome() {
      this.$router.push("/dashboard")
    },

    //加载当前路由路径
    loadCurrentRouterPath() {
      let path = this.$route.path; //   /dashboard/activity/add
      let arr = path.split("/"); //   [  ,dashboard, activity, add]
      if (arr.length > 3) {
        this.currentRouterPath = "/" + arr[1] + "/" + arr[2];
      } else {
        this.currentRouterPath = path;
      }
    }
  },

  computed: {
    getUserFirstChar() {
      if (!this.user.name) return '';
      const name = this.user.name.trim();
      return name.charAt(0).toUpperCase();
    }
  }
})
</script>

<style scoped>
.el-aside {
  background: #4b1011;
}
.el-header {
  background: #871d1f;
  height: 35px;
  line-height: 35px;
}
.el-footer {
  background: #871d1f;
  color: #d6a5a5;
  height: 35px;
  line-height: 35px;
  text-align: center;
  padding-bottom: 10px;
}
.rightContent {
  height: calc(100vh);
}
.menuTitle {
  height: 35px;
  line-height: 35px;
  margin-top: 10px;
  margin-bottom: 10px;
  color: #d6a5a5;
  text-align: center;
  cursor: pointer;
}
.show {
  cursor: pointer;
  color: #d6a5a5;
}
.el-dropdown {
  float: right;
  line-height: 35px;
}
.el-dropdown-link {
  display: flex;
  align-items: center;
  color: #d6a5a5;
  cursor: pointer;
}
.avatar {
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  background-color: #d6a5a5;
  color: #871d1f;
  border-radius: 50%;
  margin-right: 8px;
  font-weight: bold;
  font-size: 14px;
}
</style>