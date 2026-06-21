const PERMISSION_STORAGE_KEY = 'user_permissions_v2'
const LEGACY_PERMISSION_STORAGE_KEY = 'user_permissions'

export function readPermissionCodes(): string[] | null {
  window.sessionStorage.removeItem(LEGACY_PERMISSION_STORAGE_KEY)
  const cached = window.sessionStorage.getItem(PERMISSION_STORAGE_KEY)
  if (!cached) {
    return null
  }

  try {
    const parsed = JSON.parse(cached) as unknown
    return Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === 'string') : null
  } catch {
    return null
  }
}

export function writePermissionCodes(permissions: string[]): void {
  window.sessionStorage.setItem(PERMISSION_STORAGE_KEY, JSON.stringify(permissions))
}

export function clearPermissionCodes(): void {
  window.sessionStorage.removeItem(PERMISSION_STORAGE_KEY)
  window.sessionStorage.removeItem(LEGACY_PERMISSION_STORAGE_KEY)
}
