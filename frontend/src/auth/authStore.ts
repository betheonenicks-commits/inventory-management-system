import { create } from 'zustand'

export interface AuthUser {
  username: string
  roles: string[]
}

interface AuthState {
  token: string | null
  user: AuthUser | null
  setSession: (token: string, user: AuthUser) => void
  clearSession: () => void
}

// Token is held in memory only - never localStorage/sessionStorage - per the
// UX spec's security posture (this is the dev-stub auth's client half; a
// refresh-token cookie flow is EPIC-USR/SEC's job, not this store's).
export const useAuthStore = create<AuthState>((set) => ({
  token: null,
  user: null,
  setSession: (token, user) => set({ token, user }),
  clearSession: () => set({ token: null, user: null }),
}))
