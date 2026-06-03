const TOKEN_NAME = 'dlyk_token'

export interface StoredToken {
  token: string
  rememberMe: boolean
}

export function getTokenName(): string {
  return TOKEN_NAME
}

export function readStoredToken(): StoredToken | null {
  const sessionToken = window.sessionStorage.getItem(TOKEN_NAME)
  if (sessionToken) {
    return { token: sessionToken, rememberMe: false }
  }

  const localToken = window.localStorage.getItem(TOKEN_NAME)
  if (localToken) {
    return { token: localToken, rememberMe: true }
  }

  return null
}

export function writeStoredToken(token: string, rememberMe: boolean): void {
  clearStoredToken()
  const target = rememberMe ? window.localStorage : window.sessionStorage
  target.setItem(TOKEN_NAME, token)
}

export function clearStoredToken(): void {
  window.sessionStorage.removeItem(TOKEN_NAME)
  window.localStorage.removeItem(TOKEN_NAME)
}
