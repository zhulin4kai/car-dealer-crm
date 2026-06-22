export class ApiError extends Error {
  readonly code: number
  readonly raw: unknown
  readonly isSessionInvalid: boolean

  constructor(code: number, message: string, raw: unknown, isSessionInvalid = false) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.raw = raw
    this.isSessionInvalid = isSessionInvalid
  }
}
