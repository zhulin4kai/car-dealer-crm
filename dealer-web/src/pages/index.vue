<template>
  <el-container>
    <!--左侧-->
    <el-aside width="200px">
      <img src="@/assets/logo-2.svg" class="login_img">
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

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { freeLogin } from '@/modules/user/api/user-api'
import type { LoginForm } from '@/modules/user/model/user.types'
import { messageTip } from '@/shared/utils/feedback'
import { useAuthStore } from '@/stores/auth.store'

defineOptions({
  name: 'LoginView',
})

const router = useRouter()
const authStore = useAuthStore()
const loginRefForm = ref<FormInstance>()

const user = reactive<LoginForm>({
  loginAct: '',
  loginPwd: '',
  rememberMe: false,
})

const loginRules: FormRules<LoginForm> = {
  loginAct: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  loginPwd: [
    { required: true, message: '请输入登录密码', trigger: 'blur' },
    { min: 6, max: 16, message: '登录密码长度为6-16位', trigger: 'blur' },
  ],
}

async function login(): Promise<void> {
  const valid = await loginRefForm.value?.validate()
  if (!valid) {
    return
  }

  await authStore.login(user)
  messageTip('登录成功', 'success')
  await router.push('/dashboard')
}

async function restoreRememberedSession(): Promise<void> {
  authStore.restoreSession()
  if (!authStore.rememberMe || !authStore.token) {
    return
  }

  await freeLogin()
  await router.push('/dashboard')
}

onMounted(() => {
  void restoreRememberedSession()
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