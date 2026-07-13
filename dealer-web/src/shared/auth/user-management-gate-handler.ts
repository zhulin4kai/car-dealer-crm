export interface UserManagementGateContext {
  code: number
}

export interface UserManagementGateHandler {
  handleUserManagementGate(context: UserManagementGateContext): Promise<void> | void
}

let registeredHandler: UserManagementGateHandler | null = null
let inFlight: Promise<void> | null = null

export function registerUserManagementGateHandler(handler: UserManagementGateHandler): void {
  registeredHandler = handler
}

export function notifyUserManagementGate(context: UserManagementGateContext): Promise<void> {
  if (inFlight) return inFlight
  const handler = registeredHandler
  if (!handler) return Promise.resolve()
  inFlight = (async () => {
    try {
      await handler.handleUserManagementGate(context)
    } finally {
      inFlight = null
    }
  })()
  return inFlight
}

export function resetUserManagementGateHandler(): void {
  registeredHandler = null
  inFlight = null
}
