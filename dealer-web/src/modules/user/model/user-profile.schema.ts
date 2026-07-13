import * as z from 'zod'

const COMMON_PHONE_SEPARATORS = /[\s\-()（）]+/g

export function normalizeMainlandMobile(value: string): string {
  return value.trim().replace(COMMON_PHONE_SEPARATORS, '')
}

export function isMainlandMobile(value: string): boolean {
  const normalized = normalizeMainlandMobile(value)
  return !normalized || /^1[3-9]\d{9}$/.test(normalized)
}

export const userProfileSchema = z.object({
  name: z.string().trim().min(1, '请输入姓名').max(50, '姓名最多 50 个字符'),
  phone: z
    .string()
    .transform(normalizeMainlandMobile)
    .refine((value) => !value || /^1[3-9]\d{9}$/.test(value), '手机号码格式有误'),
  email: z
    .string()
    .trim()
    .refine(
      (value) => !value || /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(value),
      '邮箱格式有误',
    ),
  avatarUrl: z
    .string()
    .trim()
    .max(500, '头像地址最多 500 个字符')
    .refine((value) => !value || /^https?:\/\//.test(value), '头像地址必须使用 HTTP 或 HTTPS'),
})

export type UserProfileFormValues = z.infer<typeof userProfileSchema>
