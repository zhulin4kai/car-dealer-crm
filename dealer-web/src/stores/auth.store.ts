import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import { fetchLoginInfo, login as loginRequest, logout as logoutRequest } from '@/modules/user/api/user-api'
import type { LoginForm, User } from '@/modules/user/model/user.types'
import { clearPermissionCodes } from '@/shared/storage/permission-storage'
import { clearStoredToken, readStoredToken, writeStoredToken } from '@/shared/storage/token-storage'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const rememberMe = ref(false)
  const currentUser = ref<User | null>(null)

  const isAuthenticated = computed(() => Boolean(token.value))

  function restoreSession(): void {
    const storedToken = readStoredToken()
    token.value = storedToken?.token ?? null
    rememberMe.value = storedToken?.rememberMe ?? false
  }

  async function login(form: LoginForm): Promise<void> {
    const loginParams = new URLSearchParams()
    loginParams.append('loginAct', form.loginAct)
    loginParams.append('loginPwd', form.loginPwd)
    loginParams.append('rememberMe', String(form.rememberMe))

    const jwt = await loginRequest(loginParams)
    token.value = jwt
    rememberMe.value = form.rememberMe
    writeStoredToken(jwt, form.rememberMe)
  }

  async function loadCurrentUser(): Promise<User> {
    const user = await fetchLoginInfo()
    currentUser.value = user
    return user
  }

  async function logout(): Promise<void> {
    await logoutRequest()
    clearStoredToken()
    clearPermissionCodes()
    token.value = null
    rememberMe.value = false
    currentUser.value = null
  }

  function forceLogout(): void {
    clearStoredToken()
    clearPermissionCodes()
    token.value = null
    rememberMe.value = false
    currentUser.value = null
  }

  return {
    token,
    rememberMe,
    currentUser,
    isAuthenticated,
    restoreSession,
    login,
    loadCurrentUser,
    logout,
    forceLogout,
  }
})
