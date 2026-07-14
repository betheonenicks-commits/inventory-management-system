// Minimal, dependency-free JWT payload decode for reading the username/roles
// claims the backend embeds (JwtService.issue). This is NOT verification -
// the backend is the only party that ever validates the signature; the
// frontend only reads already-trusted claims to render the UI (e.g. which
// nav items to show - the backend's @PreAuthorize checks are the real gate).
export function decodeJwtPayload(token: string): { username: string; roles: string[]; permissions: string[] } | null {
  try {
    const [, payload] = token.split('.')
    const json = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')))
    return { username: json.username, roles: json.roles ?? [], permissions: json.permissions ?? [] }
  } catch {
    return null
  }
}
