<template>
  <el-container>
    <!--左侧-->
    <el-aside width="200px">
      <img src="../assets/logo-2.svg" class="login_img">
      <p class="imgTitle">
        欢迎使用
        <br>
        徐州工程学院汽车销售管理系统
      </p>
    </el-aside>

    <!--右侧-->
    <el-main>
      <div class="loginTile">登录您的账号</div>

      <el-form ref="loginRefForm" :model="user" :rules="loginRules" label-width="auto">
        <el-form-item label="账号" prop="loginAct">
          <el-input v-model="user.loginAct" />
        </el-form-item>

        <el-form-item label="密码" prop="loginPwd">
          <el-input type="password" v-model="user.loginPwd" />
        </el-form-item>

        <el-form-item label-position="left" style="margin-left: 50px">
          <el-button type="primary" @click="login">登 录</el-button>
        </el-form-item>

        <el-form-item style="margin-left: 50px">
          <el-checkbox label="记住我" v-model="user.rememberMe" />
        </el-form-item>
      </el-form>

    </el-main>
  </el-container>
</template>

<script>
import {defineComponent} from 'vue'
import {doGet, doPost} from "../http/httpRequest.js";
import {getTokenName, messageTip, removeToken} from "../util/util.js";


export default defineComponent({
  //组件的名字
  name: "LoginView",

  //定义页面使用到的变量，定义时初始值都是给个空的
  data() {
    return {
      //对象变量定义，是{}
      user : {},
      //定义登录表单的验证规则
      loginRules : {
        //定义账号的验证规则，规则可以有多个，所以是数组
        loginAct : [
          { required: true, message: '请输入登录账号', trigger: 'blur' }
        ],
        //定义密码的验证规则，规则可以有多个，所以是数组
        loginPwd : [
          { required: true, message: '请输入登录密码', trigger: 'blur' },
          { min: 6, max: 16, message: '登录密码长度为6-16位', trigger: 'blur' }
        ]
      }
    }
  },

  //页面渲染dom元素后会触发调用该函数（函数钩子）
  mounted() {
    this.freeLogin();
  },

  //页面上需要使用功能的js函数，都在methods属性中定义
  methods: {
    //登录函数
    login() {
      //提交前验证输入框的合法性
      this.$refs.loginRefForm.validate( (isValid) => {
        //isValid是验证后的结果，如果是true表示验证通过，否则未通过
        if (isValid) {
          //验证通过，可以提交登录
          let formData = new FormData();
          formData.append("loginAct", this.user.loginAct);
          formData.append("loginPwd", this.user.loginPwd);
          formData.append("rememberMe", this.user.rememberMe);

          doPost("/api/login", formData).then( (resp) => {
            if (resp.data.code === 200) {
              //登录成功，提示一下
              messageTip("登录成功", "success");

              //删除一下历史localStorage和sessionStorage中存储的token
              removeToken();

              //前端存储jwt
              if (this.user.rememberMe === true) {
                window.localStorage.setItem(getTokenName(), resp.data.data);
              } else {
                window.sessionStorage.setItem(getTokenName(), resp.data.data);
              }
              //跳转到系统的主页面
              window.location.href = "/dashboard";
            } else {
              //登录失败，也提示一下
              messageTip("登录失败", "error");
            }
          });
        }
      })
    },

    //免登录（自动登录）
    freeLogin() {
      let token = window.localStorage.getItem(getTokenName());
      if (token) { //表示token有值，token不是空，token存在
        doGet("/api/login/free", {}).then(resp => {
          if (resp.data.code === 200)  {
            //token验证通过了，那么可以免登录
            window.location.href = "/dashboard";
          }
        })
      }
    }
  }
})
</script>

<style scoped>
.login_img {
  width: 80%;
  height: 50%;
}
.el-aside {
  background: #871d1f;
  width: 40%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
}
.el-main {
  height: calc(100vh);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}
img {
  height: 413px;
}
.imgTitle {
  color: #ebeef5;
  font-size: 40px;
}
.el-form {
  width: 25%; /* 原来是60%，缩短到三分之一，即20% */
  margin: 0; /* 移除auto，因为flex会处理居中 */
  padding: 50px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: left;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.2);
}
.loginTile {
  text-align: center;
  /* margin-top: 100px;  移除，因为flex会处理居中 */
  margin-bottom: 25px;
  font-size: 30px;
  font-weight: bold;
}
.el-button {
  width: 100%;
  background-color: #1a1a1a;
  border-color: #1a1a1a;
}
</style>