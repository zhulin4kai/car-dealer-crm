export function goBack(): void {
  window.history.back()
}

export function redirectToLogin(): void {
  window.location.href = '/'
}

export function redirectToDashboard(): void {
  window.location.href = '/dashboard'
}
