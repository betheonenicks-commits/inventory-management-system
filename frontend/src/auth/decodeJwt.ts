// Minimal, dependency-free JWT payload decode for reading the username/roles
// claims the dev-stub backend embeds. This is NOT verification - the backend
// is the only party that ever validates the signature; the frontend only
// reads already-trusted claims to render the UI.
export function decodeJwtPayload(token: string): { username: string; roles: string[] } | null {
  try {
    const [, payload] = token.split('.')
    const json = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')))
    return { username: json.username, roles: json.roles ?? [] }
  } catch {
    return null
  }
}
