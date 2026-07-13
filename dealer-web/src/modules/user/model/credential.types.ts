export interface ActivateAccountRequest {
  credential: string
  newPassword: string
}

export interface ForgotPasswordRequest {
  loginAct: string
}

export interface ResetPasswordRequest {
  credential: string
  newPassword: string
}

export interface FirstPasswordChangeRequest {
  currentPassword: string
  newPassword: string
}

export interface ChangeOwnPasswordRequest {
  currentPassword: string
  newPassword: string
}

export type ContactVerificationChannel = 'PHONE' | 'EMAIL'

export interface ContactVerificationRequest {
  channel: ContactVerificationChannel
}

export interface VerifyContactRequest {
  credential: string
}

export interface CredentialCommandResult {
  completed: boolean
}

export interface CredentialRequestAccepted {
  accepted: true
  deliveryStatus: 'QUEUED'
}

export interface ManagedCredentialDeliveryResult {
  accepted: true
  deliveryStatus: 'QUEUED'
}

export interface ReinviteManagedUserRequest {
  accountVersion: number
  reason: string
}

export const PASSWORD_INPUT_POLICY = {
  minLength: 6,
  maxLength: 16,
} as const

export function meetsPasswordInputPolicy(value: string): boolean {
  return (
    value.length >= PASSWORD_INPUT_POLICY.minLength &&
    value.length <= PASSWORD_INPUT_POLICY.maxLength &&
    /[a-z]/.test(value) &&
    /[A-Z]/.test(value) &&
    /\d/.test(value)
  )
}
