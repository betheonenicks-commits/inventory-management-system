import { create } from 'zustand'

export interface AuthUser {
  username: string
  roles: string[]
  permissions: string[]
}

// US-USR-03: UI write-control visibility follows the same permission set the
// backend's @PreAuthorize checks now use (PermissionChecker), not a
// hardcoded role-name list - a custom role (US-USR-02) shows the right
// controls automatically. This is a convenience only; the backend is the
// real gate, same as role-based hiding always was.
export function hasPermission(user: AuthUser | null, permission: string): boolean {
  if (!user) return false
  return user.permissions.includes('*') || user.permissions.includes(permission)
}

interface AuthState {
  token: string | null
  refreshToken: string | null
  user: AuthUser | null
  setSession: (token: string, refreshToken: string, user: AuthUser) => void
  clearSession: () => void
}

// Both tokens are held in memory only - never localStorage/sessionStorage - per
// the UX spec's security posture. Losing them on reload forces a fresh login
// rather than silently staying signed in indefinitely. US-SEC-01's refresh
// token now exists backend-side (rotation-based, revocable), but there's no
// silent-refresh-on-401 interceptor in httpClient yet - the token is stored
// and sent to /auth/logout so the server-side session actually ends, but a
// page reload still requires a fresh login the same as before.
export const useAuthStore = create<AuthState>((set) => ({
  token: null,
  refreshToken: null,
  user: null,
  setSession: (token, refreshToken, user) => set({ token, refreshToken, user }),
  clearSession: () => set({ token: null, refreshToken: null, user: null }),
}))
