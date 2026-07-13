import type {
  ActivateAccountRequest,
  ChangeOwnPasswordRequest,
  CredentialCommandResult,
  CredentialRequestAccepted,
  FirstPasswordChangeRequest,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  ManagedCredentialDeliveryResult,
  ContactVerificationRequest,
  VerifyContactRequest,
  ReinviteManagedUserRequest,
} from '@/modules/user/model/credential.types'
import { httpClient } from '@/shared/api/http-client'
import type { EntityId } from '@/shared/types/id'

export function activateAccount(request: ActivateAccountRequest): Promise<CredentialCommandResult> {
  return httpClient.post<CredentialCommandResult>('/api/credentials/activate', request)
}

export function requestPasswordReset(
  request: ForgotPasswordRequest,
): Promise<CredentialRequestAccepted> {
  return httpClient.post<CredentialRequestAccepted>('/api/credentials/forgot-password', request)
}

export function resetPassword(request: ResetPasswordRequest): Promise<CredentialCommandResult> {
  return httpClient.post<CredentialCommandResult>('/api/credentials/reset-password', request)
}

export function changeFirstPassword(
  request: FirstPasswordChangeRequest,
): Promise<CredentialCommandResult> {
  return httpClient.put<CredentialCommandResult>('/api/credentials/first-password-change', request)
}

export function changeOwnPassword(
  request: ChangeOwnPasswordRequest,
): Promise<CredentialCommandResult> {
  return httpClient.put<CredentialCommandResult>('/api/credentials/change-password', request)
}

export function requestContactVerification(
  request: ContactVerificationRequest,
): Promise<ManagedCredentialDeliveryResult> {
  return httpClient.post<ManagedCredentialDeliveryResult>(
    '/api/profile/contact-verification',
    request,
  )
}

export function verifyContact(request: VerifyContactRequest): Promise<CredentialCommandResult> {
  return httpClient.post<CredentialCommandResult>('/api/credentials/verify-contact', request)
}

export function reinviteManagedUser(
  userId: EntityId,
  request: ReinviteManagedUserRequest,
): Promise<ManagedCredentialDeliveryResult> {
  return httpClient.post<ManagedCredentialDeliveryResult>(
    `/api/users/${userId}/invitation`,
    request,
  )
}
