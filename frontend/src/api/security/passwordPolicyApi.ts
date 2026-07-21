import { httpClient } from '../httpClient'

export interface PasswordPolicy {
  id: string
  version: number
  minLength: number
  requireUppercase: boolean
  requireLowercase: boolean
  requireDigit: boolean
  requireSpecial: boolean
}

/** Open to any authenticated user - a login/create-user form needs the live rules to validate against. */
export function fetchPasswordPolicy() {
  return httpClient.get<PasswordPolicy>('/security/password-policy').then((r) => r.data)
}

export interface PasswordPolicyUpdatePayload {
  minLength?: number
  requireUppercase?: boolean
  requireLowercase?: boolean
  requireDigit?: boolean
  requireSpecial?: boolean
  version: number
}

// US-SEC-05: previously had no admin UI to reach it at all.
export function updatePasswordPolicy(payload: PasswordPolicyUpdatePayload) {
  return httpClient.patch<PasswordPolicy>('/security/password-policy', payload).then((r) => r.data)
}

/**
 * The one shared validator - used both by UserListPage's create-user dialog
 * (previously hardcoded to length < 8, ignoring the actual configured
 * policy entirely) and anywhere else a password is captured client-side.
 * The backend (PasswordValidator.java) remains the real enforcement; this
 * only avoids a round-trip for an obviously-invalid password.
 */
export function passwordViolations(password: string, policy: PasswordPolicy | undefined): string[] {
  if (!policy) return []
  const violations: string[] = []
  if (password.length < policy.minLength) violations.push(`at least ${policy.minLength} characters`)
  if (policy.requireUppercase && !/[A-Z]/.test(password)) violations.push('an uppercase letter')
  if (policy.requireLowercase && !/[a-z]/.test(password)) violations.push('a lowercase letter')
  if (policy.requireDigit && !/[0-9]/.test(password)) violations.push('a digit')
  if (policy.requireSpecial && !/[^A-Za-z0-9]/.test(password)) violations.push('a special character')
  return violations
}
