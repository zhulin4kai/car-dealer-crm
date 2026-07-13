<template>
  <div
    class="login-page box-border min-h-screen overflow-hidden bg-[var(--crm-bg-page)] text-[var(--crm-text-primary)]"
    @pointermove="handleHeroPointer"
    @pointerleave="resetHeroPointer"
  >
    <div class="grid min-h-screen w-full overflow-hidden lg:grid-cols-[1.08fr_0.92fr]">
      <aside
        class="login-hero hidden overflow-hidden border-r border-[var(--crm-border-light)] lg:flex"
      >
        <div class="relative flex h-full w-full flex-col justify-between p-10">
          <div class="login-brand">
            <span
              class="flex h-10 w-10 items-center justify-center rounded-xl bg-white/85 text-[var(--crm-primary)] shadow-[var(--crm-shadow-card)]"
            >
              <Car class="h-5 w-5" />
            </span>
            <div>
              <div class="text-sm font-semibold text-[var(--crm-text-primary)]">
                汽车销售管理系统
              </div>
              <div class="text-xs text-[var(--crm-text-tertiary)]">销售运营工作台</div>
            </div>
          </div>

          <div class="login-hero-copy">线索、客户、交易，统一进入工作台。</div>

          <div class="characters-wrapper">
            <div ref="charactersSceneRef" class="characters-scene">
              <div ref="purpleRef" class="character char-purple" :style="purpleStyle">
                <div class="eyes" :class="{ 'shake-head': shakeActive }" :style="purpleEyesStyle">
                  <div ref="purpleEyeRef" class="eyeball" :style="purpleEyeStyle">
                    <div class="pupil" :style="purplePupilStyle" />
                  </div>
                  <div class="eyeball" :style="purpleEyeStyle">
                    <div class="pupil" :style="purplePupilStyle" />
                  </div>
                </div>
              </div>

              <div ref="blackRef" class="character char-black" :style="blackStyle">
                <div class="eyes" :class="{ 'shake-head': shakeActive }" :style="blackEyesStyle">
                  <div ref="blackEyeRef" class="eyeball" :style="blackEyeStyle">
                    <div class="pupil" :style="blackPupilStyle" />
                  </div>
                  <div class="eyeball" :style="blackEyeStyle">
                    <div class="pupil" :style="blackPupilStyle" />
                  </div>
                </div>
              </div>

              <div ref="orangeRef" class="character char-orange" :style="orangeStyle">
                <div
                  ref="orangeEyesRef"
                  class="eyes"
                  :class="{ 'shake-head': shakeActive }"
                  :style="orangeEyesStyle"
                >
                  <div class="bare-pupil" :style="orangePupilStyle" />
                  <div class="bare-pupil" :style="orangePupilStyle" />
                </div>
                <div
                  class="orange-mouth"
                  :class="{ visible: orangeMouthVisible, 'shake-head': shakeActive }"
                  :style="orangeMouthStyle"
                />
              </div>

              <div ref="yellowRef" class="character char-yellow" :style="yellowStyle">
                <div
                  ref="yellowEyesRef"
                  class="eyes"
                  :class="{ 'shake-head': shakeActive }"
                  :style="yellowEyesStyle"
                >
                  <div class="bare-pupil" :style="yellowPupilStyle" />
                  <div class="bare-pupil" :style="yellowPupilStyle" />
                </div>
                <div
                  class="yellow-mouth"
                  :class="{ 'shake-head': shakeActive }"
                  :style="yellowMouthStyle"
                />
              </div>
            </div>
          </div>

          <div class="login-hero-links">
            <span>线索流转</span>
            <span>客户协作</span>
            <span>交易履约</span>
          </div>
        </div>
      </aside>

      <main
        class="box-border flex min-h-screen w-full min-w-0 items-center justify-center bg-[var(--crm-bg-surface)] px-6 py-10"
      >
        <section class="box-border w-full min-w-0 max-w-[calc(100vw-48px)] sm:max-w-[400px]">
          <div class="mb-8 lg:hidden">
            <div class="flex items-center gap-3">
              <span
                class="flex h-10 w-10 items-center justify-center rounded-xl bg-[var(--crm-primary)] text-white"
              >
                <Car class="h-5 w-5" />
              </span>
              <div>
                <div class="text-base font-semibold">汽车销售管理系统</div>
                <div class="text-xs text-[var(--crm-text-tertiary)]">销售运营工作台</div>
              </div>
            </div>
          </div>

          <div class="mb-8 text-center lg:text-left">
            <h2 class="text-3xl font-semibold leading-tight">登录工作台</h2>
            <p class="mt-2 text-sm text-[var(--crm-text-tertiary)]">
              使用系统账号登录，进入销售管理工作台。
            </p>
          </div>

          <form class="space-y-5" @submit.prevent="onSubmit">
            <div
              class="space-y-2"
              @focusin="handleAccountFocus"
              @focusout="handleFieldBlur('account', $event)"
            >
              <Label for="loginAct" class="text-sm font-semibold text-[var(--crm-text-secondary)]"
                >账号</Label
              >
              <Input
                id="loginAct"
                v-model="loginAct"
                class="box-border h-11 w-full rounded-lg border-[var(--crm-border)] bg-[var(--crm-bg-surface)]"
                autocomplete="username"
                placeholder="请输入登录账号"
                @update:model-value="handleAccountInput"
              />
              <p v-if="errors.loginAct" class="text-sm text-destructive">{{ errors.loginAct }}</p>
            </div>

            <div
              class="space-y-2"
              @focusin="activeField = 'password'"
              @focusout="handleFieldBlur('password', $event)"
            >
              <Label for="loginPwd" class="text-sm font-semibold text-[var(--crm-text-secondary)]"
                >密码</Label
              >
              <div class="relative box-border w-full min-w-0">
                <Input
                  id="loginPwd"
                  v-model="loginPwd"
                  class="box-border h-11 w-full rounded-lg border-[var(--crm-border)] bg-[var(--crm-bg-surface)] pr-10"
                  :type="passwordVisible ? 'text' : 'password'"
                  autocomplete="current-password"
                  placeholder="请输入登录密码"
                />
                <button
                  class="absolute right-2 top-1/2 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-md text-[var(--crm-text-tertiary)] transition-colors hover:bg-[var(--crm-bg-hover)] hover:text-[var(--crm-primary)]"
                  type="button"
                  :aria-label="passwordVisible ? '隐藏密码' : '显示密码'"
                  @click="togglePasswordVisibility"
                >
                  <EyeOff v-if="passwordVisible" class="h-4 w-4" />
                  <Eye v-else class="h-4 w-4" />
                </button>
              </div>
              <p v-if="errors.loginPwd" class="text-sm text-destructive">{{ errors.loginPwd }}</p>
            </div>

            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2">
                <Checkbox
                  id="rememberMe"
                  :checked="rememberMe"
                  @update:checked="(v: boolean) => (rememberMe = v)"
                />
                <Label
                  for="rememberMe"
                  class="cursor-pointer text-sm font-normal text-[var(--crm-text-secondary)]"
                >
                  记住我
                </Label>
              </div>
              <button
                type="button"
                class="text-sm text-[var(--crm-primary)] hover:underline"
                @click="router.push('/forgot-password')"
              >
                忘记密码？
              </button>
            </div>

            <Button
              type="submit"
              class="box-border h-11 w-full rounded-lg bg-[var(--crm-primary)] font-semibold hover:bg-[var(--crm-primary-hover)]"
              :disabled="isSubmitting"
            >
              {{ isSubmitting ? '登录中...' : '登录' }}
            </Button>
          </form>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import { useRouter } from 'vue-router'
import * as z from 'zod'

import { freeLogin } from '@/modules/user/api/user-api'
import type { LoginForm } from '@/modules/user/model/user.types'
import { ApiError } from '@/shared/api/api-error'
import { API_ERROR_CODE } from '@/shared/api/error-codes'
import { messageTip } from '@/shared/utils/feedback'
import { useAuthStore } from '@/stores/auth.store'

import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Car, Eye, EyeOff } from '@lucide/vue'

defineOptions({
  name: 'LoginView',
})

const router = useRouter()
const authStore = useAuthStore()
const activeField = ref<'idle' | 'account' | 'password'>('idle')
const heroPointer = ref({ x: 0, y: 0 })
const passwordVisible = ref(false)
const charactersSceneRef = ref<HTMLElement | null>(null)
const purpleRef = ref<HTMLElement | null>(null)
const blackRef = ref<HTMLElement | null>(null)
const orangeRef = ref<HTMLElement | null>(null)
const yellowRef = ref<HTMLElement | null>(null)
const purpleEyeRef = ref<HTMLElement | null>(null)
const blackEyeRef = ref<HTMLElement | null>(null)
const orangeEyesRef = ref<HTMLElement | null>(null)
const yellowEyesRef = ref<HTMLElement | null>(null)
const isLookingAtEachOther = ref(false)
const isPurpleBlinking = ref(false)
const isBlackBlinking = ref(false)
const isPurplePeeking = ref(false)
const isLoginError = ref(false)
const orangeMouthVisible = ref(false)
const shakeActive = ref(false)
const activeTimers = new Set<number>()
let accountLookTimer: number | null = null
let errorRecoverTimer: number | null = null
let shakeTimer: number | null = null
let peekTimer: number | null = null
let peekReleaseTimer: number | null = null

const loginSchema = toTypedSchema(
  z.object({
    loginAct: z.string().min(1, '请输入登录账号'),
    loginPwd: z.string().min(6, '登录密码长度为6-16位').max(16, '登录密码长度为6-16位'),
    rememberMe: z.boolean(),
  }),
)

const { handleSubmit, errors, isSubmitting, defineField } = useForm<LoginForm>({
  validationSchema: loginSchema,
  initialValues: {
    loginAct: '',
    loginPwd: '',
    rememberMe: false,
  },
})

const [loginAct] = defineField('loginAct')
const [loginPwd] = defineField('loginPwd')
const [rememberMe] = defineField('rememberMe')

const passwordValue = computed(() => String(loginPwd.value ?? ''))
const isShowingPassword = computed(() => passwordValue.value.length > 0 && passwordVisible.value)
const isLookingAway = computed(() => activeField.value === 'password' && !passwordVisible.value)
const purplePosition = computed(() => calcPosition(purpleRef.value))
const blackPosition = computed(() => calcPosition(blackRef.value))
const orangePosition = computed(() => calcPosition(orangeRef.value))
const yellowPosition = computed(() => calcPosition(yellowRef.value))

const purpleStyle = computed(() => {
  const { bodySkew } = purplePosition.value

  if (isShowingPassword.value) {
    return { height: '370px', transform: 'skewX(0deg)' }
  }
  if (isLookingAway.value) {
    return { height: '410px', transform: 'skewX(-14deg) translateX(-20px)' }
  }
  if (activeField.value === 'account') {
    return { height: '410px', transform: `skewX(${bodySkew - 12}deg) translateX(40px)` }
  }
  return { height: '370px', transform: `skewX(${bodySkew}deg)` }
})

const blackStyle = computed(() => {
  const { bodySkew } = blackPosition.value

  if (isShowingPassword.value) {
    return { transform: 'skewX(0deg)' }
  }
  if (isLookingAway.value) {
    return { transform: 'skewX(12deg) translateX(-10px)' }
  }
  if (isLookingAtEachOther.value) {
    return { transform: `skewX(${bodySkew * 1.5 + 10}deg) translateX(20px)` }
  }
  if (activeField.value === 'account') {
    return { transform: `skewX(${bodySkew * 1.5}deg)` }
  }
  return { transform: `skewX(${bodySkew}deg)` }
})

const orangeStyle = computed(() => ({
  transform: isShowingPassword.value ? 'skewX(0deg)' : `skewX(${orangePosition.value.bodySkew}deg)`,
}))

const yellowStyle = computed(() => ({
  transform: isShowingPassword.value ? 'skewX(0deg)' : `skewX(${yellowPosition.value.bodySkew}deg)`,
}))

const purpleEyeStyle = computed(() => ({
  height: isPurpleBlinking.value ? '2px' : '18px',
  width: '18px',
}))

const blackEyeStyle = computed(() => ({
  height: isBlackBlinking.value ? '2px' : '16px',
  width: '16px',
}))

const purpleEyesStyle = computed(() => {
  const { faceX, faceY } = purplePosition.value

  if (isLoginError.value) {
    return eyesStyle(30, 55, 28)
  }
  if (isLookingAway.value) {
    return eyesStyle(20, 25, 28)
  }
  if (isShowingPassword.value) {
    return eyesStyle(20, 35, 28)
  }
  if (isLookingAtEachOther.value) {
    return eyesStyle(55, 65, 28)
  }
  return eyesStyle(45 + faceX, 40 + faceY, 28)
})

const purplePupilStyle = computed(() => {
  if (isLoginError.value) {
    return translateStyle(-3, 4)
  }
  if (isLookingAway.value) {
    return translateStyle(-5, -5)
  }
  if (isShowingPassword.value) {
    return translateStyle(isPurplePeeking.value ? 4 : -4, isPurplePeeking.value ? 5 : -4)
  }
  if (isLookingAtEachOther.value) {
    return translateStyle(3, 4)
  }
  return translateStyleFromOffset(calcPupilOffset(purpleEyeRef.value, 5))
})

const blackEyesStyle = computed(() => {
  const { faceX, faceY } = blackPosition.value

  if (isLoginError.value) {
    return eyesStyle(15, 40, 20)
  }
  if (isLookingAway.value) {
    return eyesStyle(10, 20, 20)
  }
  if (isShowingPassword.value) {
    return eyesStyle(10, 28, 20)
  }
  if (isLookingAtEachOther.value) {
    return eyesStyle(32, 12, 20)
  }
  return eyesStyle(26 + faceX, 32 + faceY, 20)
})

const blackPupilStyle = computed(() => {
  if (isLoginError.value) {
    return translateStyle(-3, 4)
  }
  if (isLookingAway.value || isShowingPassword.value) {
    return translateStyle(-4, -4)
  }
  if (isLookingAtEachOther.value) {
    return translateStyle(0, -4)
  }
  return translateStyleFromOffset(calcPupilOffset(blackEyeRef.value, 4))
})

const orangeEyesStyle = computed(() => {
  const { faceX, faceY } = orangePosition.value

  if (isLoginError.value) {
    return eyesStyle(60, 95, 28)
  }
  if (isLookingAway.value) {
    return eyesStyle(50, 75, 28)
  }
  if (isShowingPassword.value) {
    return eyesStyle(50, 85, 28)
  }
  return eyesStyle(82 + faceX, 90 + faceY, 28)
})

const orangePupilStyle = computed(() => {
  if (isLoginError.value) {
    return translateStyle(-3, 4)
  }
  if (isLookingAway.value) {
    return translateStyle(-5, -5)
  }
  if (isShowingPassword.value) {
    return translateStyle(-5, -4)
  }
  return translateStyleFromOffset(calcPupilOffset(orangeEyesRef.value, 5))
})

const orangeMouthStyle = computed(() => {
  if (isLoginError.value) {
    return {
      left: `${80 + orangePosition.value.faceX}px`,
      top: '130px',
    }
  }
  return { left: '90px', top: '120px' }
})

const yellowEyesStyle = computed(() => {
  const { faceX, faceY } = yellowPosition.value

  if (isLoginError.value) {
    return eyesStyle(35, 45, 20)
  }
  if (isLookingAway.value) {
    return eyesStyle(20, 30, 20)
  }
  if (isShowingPassword.value) {
    return eyesStyle(20, 35, 20)
  }
  return eyesStyle(52 + faceX, 40 + faceY, 20)
})

const yellowPupilStyle = computed(() => {
  if (isLoginError.value) {
    return translateStyle(-3, 4)
  }
  if (isLookingAway.value) {
    return translateStyle(-5, -5)
  }
  if (isShowingPassword.value) {
    return translateStyle(-5, -4)
  }
  return translateStyleFromOffset(calcPupilOffset(yellowEyesRef.value, 5))
})

const yellowMouthStyle = computed(() => {
  const { faceX, faceY } = yellowPosition.value

  if (isLoginError.value) {
    return mouthStyle(30, 92, -8)
  }
  if (isLookingAway.value) {
    return mouthStyle(15, 78, 0)
  }
  if (isShowingPassword.value) {
    return mouthStyle(10, 88, 0)
  }
  return mouthStyle(40 + faceX, 88 + faceY, 0)
})

watch(isShowingPassword, () => {
  schedulePurplePeek()
})

function eyesStyle(left: number, top: number, gap: number): Record<string, string> {
  return {
    gap: `${gap}px`,
    left: `${left}px`,
    top: `${top}px`,
  }
}

function mouthStyle(left: number, top: number, rotate: number): Record<string, string> {
  return {
    left: `${left}px`,
    top: `${top}px`,
    transform: `rotate(${rotate}deg)`,
  }
}

function translateStyle(x: number, y: number): Record<string, string> {
  return {
    transform: `translate(${x}px, ${y}px)`,
  }
}

function translateStyleFromOffset(offset: { x: number; y: number }): Record<string, string> {
  return translateStyle(offset.x, offset.y)
}

function calcPosition(el: HTMLElement | null): { faceX: number; faceY: number; bodySkew: number } {
  if (!el) {
    return { bodySkew: 0, faceX: 0, faceY: 0 }
  }

  const rect = el.getBoundingClientRect()
  const cx = rect.left + rect.width / 2
  const cy = rect.top + rect.height / 3
  const dx = heroPointer.value.x - cx
  const dy = heroPointer.value.y - cy

  return {
    bodySkew: clamp(-dx / 120, -6, 6),
    faceX: clamp(dx / 20, -15, 15),
    faceY: clamp(dy / 30, -10, 10),
  }
}

function calcPupilOffset(el: HTMLElement | null, maxDist: number): { x: number; y: number } {
  if (!el) {
    return { x: 0, y: 0 }
  }

  const rect = el.getBoundingClientRect()
  const cx = rect.left + rect.width / 2
  const cy = rect.top + rect.height / 2
  const dx = heroPointer.value.x - cx
  const dy = heroPointer.value.y - cy
  const dist = Math.min(Math.sqrt(dx * dx + dy * dy), maxDist)
  const angle = Math.atan2(dy, dx)

  return {
    x: Math.cos(angle) * dist,
    y: Math.sin(angle) * dist,
  }
}

function clamp(value: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, value))
}

function addTimer(callback: () => void, delay: number): number {
  const timer = window.setTimeout(() => {
    activeTimers.delete(timer)
    callback()
  }, delay)
  activeTimers.add(timer)
  return timer
}

function clearTrackedTimer(timer: number | null): void {
  if (timer === null) {
    return
  }
  window.clearTimeout(timer)
  activeTimers.delete(timer)
}

function setPointerToSceneCenter(): void {
  const scene = charactersSceneRef.value
  if (!scene) {
    return
  }

  const rect = scene.getBoundingClientRect()
  heroPointer.value = {
    x: rect.left + rect.width / 2,
    y: rect.top + rect.height / 2,
  }
}

function handleHeroPointer(event: PointerEvent): void {
  if (isLoginError.value) {
    return
  }

  heroPointer.value = {
    x: event.clientX,
    y: event.clientY,
  }
}

function resetHeroPointer(): void {
  setPointerToSceneCenter()
}

function handleAccountFocus(): void {
  activeField.value = 'account'
  triggerAccountLook()
}

function handleAccountInput(): void {
  triggerAccountLook()
}

function handleFieldBlur(field: 'account' | 'password', event?: FocusEvent): void {
  const currentTarget = event?.currentTarget
  const nextTarget = event?.relatedTarget
  if (
    currentTarget instanceof HTMLElement &&
    nextTarget instanceof Node &&
    currentTarget.contains(nextTarget)
  ) {
    return
  }

  if (activeField.value === field) {
    activeField.value = 'idle'
  }
  if (field === 'account') {
    isLookingAtEachOther.value = false
  }
}

function triggerAccountLook(): void {
  isLookingAtEachOther.value = true
  clearTrackedTimer(accountLookTimer)
  accountLookTimer = addTimer(() => {
    accountLookTimer = null
    isLookingAtEachOther.value = false
  }, 800)
}

function togglePasswordVisibility(): void {
  passwordVisible.value = !passwordVisible.value
  schedulePurplePeek()
}

function scheduleBlink(target: 'purple' | 'black'): void {
  addTimer(
    () => {
      if (target === 'purple') {
        isPurpleBlinking.value = true
      } else {
        isBlackBlinking.value = true
      }

      addTimer(() => {
        if (target === 'purple') {
          isPurpleBlinking.value = false
        } else {
          isBlackBlinking.value = false
        }
        scheduleBlink(target)
      }, 150)
    },
    Math.random() * 4000 + 3000,
  )
}

function schedulePurplePeek(): void {
  clearTrackedTimer(peekTimer)
  clearTrackedTimer(peekReleaseTimer)
  peekTimer = null
  peekReleaseTimer = null
  isPurplePeeking.value = false

  if (!isShowingPassword.value) {
    return
  }

  peekTimer = addTimer(
    () => {
      peekTimer = null
      if (!isShowingPassword.value) {
        return
      }
      isPurplePeeking.value = true
      peekReleaseTimer = addTimer(() => {
        peekReleaseTimer = null
        isPurplePeeking.value = false
        schedulePurplePeek()
      }, 800)
    },
    Math.random() * 3000 + 2000,
  )
}

function triggerCharacterError(): void {
  clearTrackedTimer(errorRecoverTimer)
  clearTrackedTimer(shakeTimer)
  errorRecoverTimer = null
  shakeTimer = null
  activeField.value = 'idle'
  isLookingAtEachOther.value = false
  isLoginError.value = true
  orangeMouthVisible.value = true
  shakeActive.value = false

  void nextTick(() => {
    shakeTimer = addTimer(() => {
      shakeTimer = null
      shakeActive.value = true
    }, 350)
  })

  errorRecoverTimer = addTimer(() => {
    errorRecoverTimer = null
    isLoginError.value = false
    orangeMouthVisible.value = false
    shakeActive.value = false
    setPointerToSceneCenter()
  }, 2500)
}

const onSubmit = handleSubmit(
  async (formData) => {
    try {
      await authStore.login(formData)
      messageTip('登录成功', 'success')
      await router.push('/dashboard')
    } catch (error) {
      triggerCharacterError()
      if (error instanceof ApiError && error.code === API_ERROR_CODE.AUTH_LOGIN_FAILED) {
        messageTip(error.message, 'error')
        return
      }
      messageTip('登录失败，请稍后重试', 'error')
    }
  },
  () => {
    triggerCharacterError()
  },
)

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
  void nextTick(() => {
    setPointerToSceneCenter()
    scheduleBlink('purple')
    scheduleBlink('black')
  })
  window.addEventListener('resize', setPointerToSceneCenter)
})

onUnmounted(() => {
  activeTimers.forEach((timer) => window.clearTimeout(timer))
  activeTimers.clear()
  window.removeEventListener('resize', setPointerToSceneCenter)
})
</script>

<style scoped>
.login-page,
.login-page * {
  box-sizing: border-box;
}

.login-page :deep(*) {
  box-sizing: border-box;
}

.login-hero {
  position: relative;
  background:
    radial-gradient(circle at 22px 22px, rgba(51, 112, 255, 0.12) 1px, transparent 1px),
    linear-gradient(135deg, #dfe7ff 0%, #edf3ff 52%, #f5f6f7 100%);
  background-size:
    24px 24px,
    auto;
}

.login-brand {
  display: flex;
  position: relative;
  z-index: 10;
  gap: 16px;
  align-items: center;
}

.login-hero-copy {
  position: absolute;
  right: 40px;
  bottom: 86px;
  left: 40px;
  z-index: 10;
  color: var(--crm-text-secondary);
  font-size: 14px;
}

.login-hero-links {
  position: relative;
  z-index: 10;
  display: flex;
  gap: 28px;
  color: var(--crm-text-tertiary);
  font-size: 13px;
}

.characters-wrapper {
  position: relative;
  z-index: 10;
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  min-height: 460px;
}

/* Character scene adapted from guohaolian/animatedlogin (MIT). */
.characters-scene {
  position: relative;
  width: 480px;
  height: 360px;
  flex: 0 0 auto;
  transform-origin: bottom center;
}

.characters-scene::after {
  position: absolute;
  right: 24px;
  bottom: -7px;
  left: 0;
  height: 18px;
  content: '';
  border-radius: 999px;
  background: rgba(31, 35, 41, 0.08);
  filter: blur(8px);
}

.character {
  position: absolute;
  bottom: 0;
  transform-origin: bottom center;
  transition: all 0.7s ease-in-out;
}

.char-purple {
  left: 60px;
  z-index: 1;
  width: 170px;
  height: 370px;
  border-radius: 10px 10px 0 0;
  background: #6c3ff5;
}

.char-black {
  left: 220px;
  z-index: 2;
  width: 115px;
  height: 290px;
  border-radius: 8px 8px 0 0;
  background: #2d2d2d;
}

.char-orange {
  left: 0;
  z-index: 3;
  width: 230px;
  height: 190px;
  border-radius: 115px 115px 0 0;
  background: #ff9b6b;
}

.char-yellow {
  left: 290px;
  z-index: 4;
  width: 135px;
  height: 215px;
  border-radius: 68px 68px 0 0;
  background: #e8d754;
}

.eyes {
  position: absolute;
  display: flex;
  transition: all 0.35s ease-out;
}

.eyeball {
  display: flex;
  overflow: hidden;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #fff;
  transition: height 0.15s ease;
}

.pupil {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: #2d2d2d;
  transition: transform 0.1s ease-out;
}

.char-black .pupil {
  width: 6px;
  height: 6px;
}

.bare-pupil {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  background: #2d2d2d;
  transition: transform 0.35s ease-out;
}

.yellow-mouth {
  position: absolute;
  width: 50px;
  height: 4px;
  border-radius: 2px;
  background: #2d2d2d;
  transition: all 0.7s ease-in-out;
}

.orange-mouth {
  position: absolute;
  width: 28px;
  height: 14px;
  opacity: 0;
  border: 3px solid #2d2d2d;
  border-top: 0;
  border-radius: 0 0 14px 14px;
  transition: all 0.7s ease-in-out;
}

.orange-mouth.visible {
  opacity: 1;
}

@keyframes shake-head {
  0%,
  100% {
    translate: 0 0;
  }

  10% {
    translate: -9px 0;
  }

  20% {
    translate: 7px 0;
  }

  30% {
    translate: -6px 0;
  }

  40% {
    translate: 5px 0;
  }

  50% {
    translate: -4px 0;
  }

  60% {
    translate: 3px 0;
  }

  70% {
    translate: -2px 0;
  }

  80% {
    translate: 1px 0;
  }

  90% {
    translate: -0.5px 0;
  }
}

.shake-head {
  animation: shake-head 0.8s cubic-bezier(0.36, 0.07, 0.19, 0.97) both;
}

@media (min-width: 1024px) and (max-width: 1199px) {
  .characters-scene {
    transform: scale(0.78);
  }
}

@media (min-width: 1200px) and (max-width: 1379px) {
  .characters-scene {
    transform: scale(0.9);
  }
}
</style>
