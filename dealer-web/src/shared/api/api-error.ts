export class ApiError extends Error {
  readonly code: number
  readonly raw: unknown

  constructor(code: number, message: string, raw: unknown) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.raw = raw
  }
}
