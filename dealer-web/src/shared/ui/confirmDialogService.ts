/**
 * Global confirm dialog service.
 * The ConfirmDialog component registers itself here on mount.
 * feedback.ts calls openConfirmDialog() to show the dialog.
 */

type OpenConfirmFn = (options: { title: string; description: string }) => Promise<boolean>

let openFn: OpenConfirmFn | null = null

export function registerConfirmDialog(fn: OpenConfirmFn): void {
  openFn = fn
}

export function openConfirmDialog(options: { title: string; description: string }): Promise<boolean> {
  if (!openFn) {
    // Fallback: use native confirm if dialog is not registered yet
    return Promise.resolve(window.confirm(options.description))
  }
  return openFn(options)
}
