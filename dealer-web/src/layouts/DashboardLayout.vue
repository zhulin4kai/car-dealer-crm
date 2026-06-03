<template>
  <el-container>
    <!--左侧-->
    <el-aside :width="isCollapse ? '64px' : '200px'">
      <div class="menuTitle" @click="backToHome()">@汽车销售管理系统</div>
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

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import type { User } from '@/modules/user/model/user.types'
import { messageTip } from '@/shared/utils/feedback'
import { useAppStore } from '@/stores/app.store'
import { useAuthStore } from '@/stores/auth.store'
import { usePermissionStore } from '@/stores/permission.store'

defineOptions({
  name: 'DashboardLayout',
})

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const authStore = useAuthStore()
const permissionStore = usePermissionStore()

const user = computed<User>(() => authStore.currentUser ?? {})
const isCollapse = computed(() => appStore.sidebarCollapsed)
const isRouterAlive = computed(() => true)
const currentRouterPath = computed(() => route.meta.activeMenu ?? route.path)
const getUserFirstChar = computed(() => {
  const name = user.value.name?.trim() ?? ''
  return name ? name.charAt(0).toUpperCase() : ''
})

function showMenu(): void {
  appStore.toggleSidebar()
}

async function logout(): Promise<void> {
  await authStore.logout()
  permissionStore.clearPermissions()
  messageTip('退出成功', 'success')
  await router.push('/')
}

function backToHome(): void {
  void router.push('/dashboard')
}

onMounted(async () => {
  if (!authStore.currentUser) {
    await authStore.loadCurrentUser()
  }
  if (!permissionStore.hasMenu) {
    await permissionStore.loadPermissions()
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