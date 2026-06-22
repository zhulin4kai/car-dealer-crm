export interface SessionInvalidContext {
  code: number
  msg: string
}

export interface SessionInvalidHandler {
  handleSessionInvalid(context: SessionInvalidContext): Promise<void> | void
}

let registeredHandler: SessionInvalidHandler | null = null
let inFlight: Promise<void> | null = null

export function registerSessionInvalidHandler(handler: SessionInvalidHandler): void {
  registeredHandler = handler
}

export function notifySessionInvalid(context: SessionInvalidContext): Promise<void> {
  if (inFlight) {
    return inFlight
  }
  const handler = registeredHandler
  if (!handler) {
    return Promise.resolve()
  }
  inFlight = (async () => {
    try {
      await handler.handleSessionInvalid(context)
    } finally {
      inFlight = null
    }
  })()
  return inFlight
}

export function resetSessionInvalidHandler(): void {
  registeredHandler = null
  inFlight = null
}
