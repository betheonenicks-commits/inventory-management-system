# IAMS — Inventory Audit Management System

Production-ready UI built with **Next.js 14 (App Router) + TypeScript + Tailwind CSS + shadcn/ui-style components + Framer Motion**.

## Quick start

```bash
node scripts/restore-paths.mjs   # restores (app) route group + [id] segment names
npm install
npm run dev
```

Sign in with any credentials (auth is a visual scaffold — see below).

## Architecture

```
app/
  page.tsx              # Login (SCR-GLB-01)
  (app)/                # Authenticated shell: sidebar + topbar
    dashboard/          # SCR-DSH-01
    assets/             # SCR-AST-01 (list), [id] detail, new (register)
    labels/             # SCR-AST-04
    reports/            # SCR-RPT-01
    hierarchy/          # SCR-ORG-01
    users/              # SCR-USR-01
    activity/           # SCR-SEC-01
    retention/          # SCR-CMP-01
    notifications/      # SCR-GLB-03
    stock|transfers|audits|integrations/  # R2 — locked placeholder
components/
  ui/                   # shadcn-style primitives (button, card, input, table, badge…)
  app/                  # Composed app components (sidebar, topbar, metric-card…)
lib/
  types.ts              # Domain types
  data.ts               # Typed fixtures — swap for API hooks, same shapes
  utils.ts              # cn() helper
```

## Design tokens

All colors are CSS variables in `app/globals.css`, exposed to Tailwind in `tailwind.config.ts` (`bg-verify`, `text-slate-dim`, `border-hairline`, …). Dark mode is a class-strategy variable swap driven by `next-themes` — toggle in the topbar.

## Data layer

Components import typed fixtures from `lib/data.ts`. To go live, replace that module with server fetches or React Query hooks returning the same `Asset` / `User` shapes — no component changes needed.

## Auth

The login page is a visual scaffold that routes to `/dashboard`. To productionize: add `next-auth`, wrap the `(app)` group with session middleware, and point the credential + SSO buttons at your providers. The layout already isolates authenticated routes in the `(app)` route group.

## Accessibility

Semantic landmarks (`nav`, `main`, `header`), labeled form fields, focus-visible rings, `aria-current` on active nav, and reduced-motion-safe animations.
