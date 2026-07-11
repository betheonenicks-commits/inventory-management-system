**Frontend / UX Design Specification**

**Inventory Audit Management System (IAMS)**

*Implementation-Ready Specification for React/Material UI Engineering and Design*

Document ID: IAMS-FUX-1.0

Document Version: 1.0

Status: Draft for Engineering Review

Date: July 2026

Related Documents: IAMS-BRD-4.0 (Business Requirements) · IAMS-FRS-4.0 (Functional Requirements) · IAMS-SRS-4.0 (Software/Architecture Requirements) · IAMS-PUC-1.0 (Personas & Use Cases)

# Document Control

## How to Read This Document

This is a build specification, not a restatement of requirements. Every screen, component, and pattern below cites the FR-ID / UC-ID / NFR-ID it satisfies so it can be traced back to IAMS-FRS-4.0, IAMS-PUC-1.0, and IAMS-SRS-4.0. Where the source documents left a UX decision open, this document makes the call explicitly and states the rationale — marked **[DECISION]**. A consolidated list of these calls is in Section 13 for anyone who wants to re-litigate one later.

This spec assumes the fixed stack from IAMS-SRS-4.0 Section 3: React SPA, Material UI (MUI), React Router, Axios, PWA packaging, served via Nginx or backend-served static build. It does not revisit that choice.

## Scope Note on Releases

Per BRD Section 8.2 and FRS Section 5, this spec designs the full product but tags every screen **R1**, **R2**, or **R3** so engineering can sequence the build:

- **R1**: Asset registration (AST), Organization (ORG), Users/RBAC (USR) incl. separation-of-duties (FR-USR-06/07), basic Reporting (Asset Register, Employee Asset List, PDF/Excel/CSV export only), basic bulk import with dry-run (MIG).
- **R2**: Audit module (AUD, SCN), full Lifecycle (LIF), Inventory (INV), full Reporting/Dashboards (RPT, DSH), Notifications (NTF), Product Analytics (ANL).
- **R3**: Full data export/portability (MIG bulk export), external-integration-facing screens (INT), remaining Compliance (CMP) tooling depth.

# 1. Information Architecture

## 1.1 Design Principle

IAMS has two structurally different information architectures, not one responsive layout serving two purposes (SRS 7.1, NFR-ACC-03):

1. **Admin/Desktop IA** — a persistent left-nav, module-oriented, dense, built for people who sit down to configure, approve, and report (Priya, Marcus, Elena, Grace, Officer Reyes, the Viewer).
2. **Audit/Mobile IA** — a task-oriented, single-purpose-per-screen, bottom-anchored IA built for people who are standing, walking, and holding a phone in one hand while the other hand holds a box, a clipboard, or a door (Devon primarily; Father Thomas and Sam get a lightweight subset).

A given user may see both IAs depending on device and task — e.g., Devon plans an audit on desktop (Admin IA) but executes it on his phone (Audit IA). The IA a user lands in is decided by route, not by a global "mode switch": `/audits/:id/scan` always renders the Audit/Mobile shell regardless of viewport, and everything else always renders the Admin shell responsively. **[DECISION]** A responsive-only approach (one shell that "reflows") was rejected because Continuous Scan Mode (FR-AUD-05) needs a fundamentally different information density and interaction model than a data table — reflowing a data-table-oriented shell down to phone width produces a worse scanning experience than designing the scan screen as its own shell. See Section 6 for both shells in full.

## 1.2 Server-Side Authority, Client-Side Convenience

Per SRS NFR-SEC-02 and FRS FR-USR-03, all navigation/menu-item visibility below is a UX convenience computed from the user's role + org-scope claims returned at login. It is never the sole enforcement mechanism. Every route and every mutating action also handles a `403 Forbidden` from the API gracefully (see Section 12.5) — the frontend must assume a role can change mid-session (e.g., a Super Administrator revokes a role while the user has a tab open) and degrade to a clear "you no longer have access" state rather than a broken UI.

## 1.3 Navigation by Role (Admin/Desktop Shell)

| Nav Section | FRS Module(s) | Priya (Super Admin) | Marcus (Admin) | Elena (Inv. Mgr) | Grace (Read-only Auditor) | Father Thomas (Dept Head) | Viewer (Board/Finance) | Officer Reyes (Sec/Compliance) |
|---|---|---|---|---|---|---|---|---|
| Dashboard | DSH | Full, org-wide | Full, org-wide | Inventory-scoped widgets | Audit-scoped, read-only | Dept-scoped | Financial/asset widgets only | Security/compliance widgets |
| Assets | AST | Full CRUD | Full CRUD | Full CRUD (primary workspace) | Read-only | Read-only, dept scope | Read-only, reports only | Read-only |
| Inventory | INV | Full CRUD | View | Full CRUD (primary workspace) | — | — | Read-only via reports | — |
| Organization | ORG | Full CRUD (hierarchy, employees) | Employees only (not hierarchy structure) | View | — | View own node | — | View |
| Lifecycle | LIF | Full | Approve/initiate | Initiate (primary workspace) | View | **Approvals inbox** (primary use) | — | View |
| Audits | AUD | Full | Assign/reassign, view all | View own-scope | **View all, no edit** (primary use) | **Approvals inbox** (primary use) | — | View evidence, read-only |
| Reports | RPT | Full | Full | Asset/inventory/vendor reports | Audit/compliance reports | Dept-scoped reports | **Primary use** | Security/compliance reports |
| Users & Roles | USR | Full incl. security policy | Provision/offboard (not policy) | — | — | — | — | View SoD waivers |
| Notifications | NTF | Configure org-mandatory types | Configure org-mandatory types | Personal prefs | Personal prefs | Personal prefs | Personal prefs | Personal prefs |
| Search | SRC | Global | Global | Global | Global | Global | Scoped to reports | Global |
| Security | SEC | Full | — | — | — | — | — | **Primary use** |
| Data Migration | MIG | Full | — | — | — | — | — | View import logs |
| Integrations | INT | Enable/disable (with Compliance sign-off) | — | — | — | — | View status (R3) | Review before enable |
| Compliance | CMP | Configure retention | — | — | — | — | — | **Primary use** |
| Analytics | ANL | Full | **Primary use** (usage review) | — | — | — | — | — |
| Feedback | ANL | Available | Available | Available | Available | Available | Available | Available |

Sam (Employee/Volunteer) does not get the Admin shell's full nav at all — see 1.4. Devon (Auditor) gets the Admin shell only for audit *planning*; execution is always the Audit/Mobile shell (1.5).

## 1.4 Sam's Nav (Employee/Volunteer — Lightweight Portal)

Sam's goals (PUC: "see what's assigned to me," "acknowledge without confusion," "control my notifications") do not require the module-oriented Admin IA at all. Sam gets a **3-item nav**, available in both desktop and mobile responsive layout (no separate shell needed — the surface area is small enough that one responsive layout suffices):

- **My Assets** — read-only list of assets assigned to Sam (LIF), with an **Acknowledge** action on any pending assignment/transfer (UC-LIF-01 pattern, Sam-side).
- **Notifications** — in-app feed + preferences (NTF, UC-NTF-01).
- **Feedback** — the in-app feedback form (FR-ANL-04).

**[DECISION]** Sam does not get a Dashboard, Reports, or Search entry. Giving a low-engagement, wide-technical-range persona a full module nav he'll use twice a year is a discoverability tax, not a feature. If Sam needs to find a specific asset, "My Assets" already scopes to what's relevant to him; anything else is out of his role's permission set anyway and would 403.

## 1.5 Devon's Nav (Auditor — Mobile/Audit Shell)

On a phone, Devon's nav is a **bottom navigation bar**, 3 destinations, thumb-reachable:

- **My Audits** (home) — list of audits assigned to Devon, grouped In Progress / Not Started / Awaiting My Disposition (FR-AUD-23 scope-change items surface here too).
- **Scan** — jumps directly into Continuous Scan Mode for the most recently opened in-progress audit, or prompts audit selection if none is in progress. This is the single most-used destination and is the default landing tab during audit season.
- **Search** — global search (FR-SRC-01/02), used per UC-SRC-01 to locate an out-of-scope asset mid-walk.

On desktop, Devon sees the standard Admin shell with Audits, Reports, Search in the left nav (for planning audits, reviewing past findings) — same as Grace's view but with write access to his own audits.

## 1.6 Site Map (Admin/Desktop)

```
/ (redirect to /dashboard)
├── /dashboard                                    [DSH]
├── /assets                                       [AST]
│   ├── /assets/new
│   ├── /assets/:assetId
│   ├── /assets/:assetId/edit
│   ├── /assets/categories
│   └── /assets/labels/print
├── /inventory                                     [INV]
│   ├── /inventory/items/:itemId
│   ├── /inventory/warehouses
│   ├── /inventory/transfers/new
│   └── /inventory/vendors
├── /organization                                  [ORG]
│   ├── /organization/hierarchy
│   ├── /organization/employees
│   └── /organization/employees/:personId
├── /lifecycle                                     [LIF]
│   ├── /lifecycle/purchase-requests
│   ├── /lifecycle/purchase-orders
│   ├── /lifecycle/assignments
│   ├── /lifecycle/transfers
│   ├── /lifecycle/transfers/:transferId
│   ├── /lifecycle/maintenance
│   └── /lifecycle/disposals
├── /audits                                        [AUD]
│   ├── /audits/new
│   ├── /audits/:auditId                           (desktop review/detail)
│   ├── /audits/:auditId/scope-changes              (FR-AUD-23)
│   ├── /audits/:auditId/certificate
│   └── /audits/:auditId/scan                       → renders Audit/Mobile shell regardless of viewport
├── /reports                                        [RPT]
│   └── /reports/:reportType
├── /admin
│   ├── /admin/users                                [USR]
│   ├── /admin/users/:userId
│   ├── /admin/roles                                [USR]
│   ├── /admin/security/activity-log                [SEC]
│   ├── /admin/security/sessions                    [SEC]
│   ├── /admin/security/password-policy              [SEC]
│   ├── /admin/migration/import                       [MIG]
│   ├── /admin/migration/import/:jobId
│   ├── /admin/integrations                          [INT] (R3)
│   ├── /admin/compliance/retention                  [CMP]
│   ├── /admin/compliance/legal-holds                 [CMP]
│   ├── /admin/compliance/anonymization-queue          [CMP]
│   ├── /admin/compliance/waivers                     [CMP/USR — SoD waivers, FR-USR-07]
│   ├── /admin/compliance/privacy-notice                [CMP]
│   └── /admin/analytics/usage                        [ANL]
├── /account
│   ├── /account/profile
│   ├── /account/notification-preferences             [NTF]
│   └── /account/my-assets                            [LIF, Sam's portal]
├── /feedback                                         [ANL]
├── /search                                           [SRC]
└── /login, /logout, /forbidden, /offline
```

# 2. Design System Foundation

## 2.1 MUI Theme Approach

A single custom MUI theme (`iamsTheme`) is defined centrally and consumed via `ThemeProvider` at the app root, with no per-page theme overrides — consistency across 16 modules depends on this discipline. The theme is built from design tokens (a plain JS object of colors, spacing, radii, typography) so the same tokens can drive both the MUI theme and any non-MUI custom SVG/CSS (e.g., the scan-cue animation, condition-scale icons).

## 2.2 Palette and Contrast

**[DECISION]** IAMS uses one light theme only at launch — see 2.6 for the dark-mode call. All pairings below target WCAG 2.1 AA (4.5:1 normal text, 3:1 large text ≥18pt/14pt-bold, 3:1 for UI component boundaries/graphical objects per SC 1.4.11), computed against the documented background. These are the starting hex values for the design token file; **every pairing shall be re-verified with an automated contrast tool (axe DevTools or Stark) during implementation** as a gate before the pre-go-live accessibility audit (NFR-ACC-01) — treat the numbers below as the design target, not a substitute for that verification.

| Token | Hex | On | Approx. Ratio | Usage |
|---|---|---|---|---|
| `color.primary.main` | #0A54A3 | #FFFFFF | ~7.1:1 | Primary actions, active nav item, links |
| `color.primary.dark` | #073B73 | #FFFFFF | ~9.8:1 | Hover/pressed state |
| `color.primary.contrastText` | #FFFFFF | #0A54A3 | ~7.1:1 | Text/icons on primary-filled surfaces |
| `color.success.main` (Verified) | #1E7B34 | #FFFFFF | ~4.6:1 | Verified scan cue, success banners |
| `color.error.main` (Missing/Damaged) | #B3261E | #FFFFFF | ~5.9:1 | Missing/Damaged status, destructive actions |
| `color.warning.dark` (Duplicate/Scope-Changed) | #8A5300 | #FFFFFF | ~5.3:1 | Duplicate-scan toast, Scope-Changed banner — deliberately a dark amber, never a pale yellow, so it clears AA at normal text size |
| `color.text.primary` | #1B1B1F | #FFFFFF | ~16.1:1 | Body text |
| `color.text.secondary` | #49454E | #FFFFFF | ~8.4:1 | Metadata, captions used for required info |
| `color.text.disabledInformative` | #5B5761 | #FFFFFF | ~6.0:1 | Text inside a *locked* (not disabled-and-irrelevant) control, e.g. LockedToggleWithExplanation — kept well above the 4.5:1 floor because it still conveys required information (FR-NTF-05, FR-USR-06) |
| `color.divider` | #C9C5D0 | #FFFFFF | 3:1 (non-text UI boundary) | Table borders, dividers |
| `color.background.default` | #FFFFFF | — | — | App background |
| `color.background.surface` | #F7F6FA | — | — | Cards, table zebra striping |

**Color is never the sole signal.** Every status (Missing, Damaged, Scope Changed, Duplicate, Offline) pairs a color with an icon and a text label — this is load-bearing for FR-CMP-04/NFR-ACC-01 (SC 1.4.1 Use of Color) and is repeated per-component in Section 3 and Section 7.

## 2.3 Typography Scale

Base font size is 16px (`1rem`); the app never ships a root font-size below 16px, so that a user's OS/browser 200% zoom setting (WCAG 1.4.4) behaves predictably. Type scale (Roboto, self-hosted — see 2.5 for rationale):

| Style | Size / Line-height | Weight | Usage |
|---|---|---|---|
| h1 | 32/40 | 700 | Page title (one per page) |
| h2 | 28/36 | 700 | Section heading |
| h3 | 24/32 | 600 | Card/panel heading |
| h4 | 20/28 | 600 | Sub-panel heading |
| h5 | 18/26 | 600 | Dialog title |
| h6 | 16/24 | 600 | Table group header |
| subtitle1 | 16/24 | 500 | Field group label |
| body1 | 16/24 | 400 | Default body text, table cell text |
| body2 | 14/20 | 400 | Secondary body text |
| button | 14/20 | 600 | Buttons; `textTransform: 'none'` (Material's default all-caps buttons are a known readability/translation problem — see 10.2, i18n) |
| caption | 12/16 | 400 | Timestamps, helper text — **never the sole carrier of required information** per 2.2 |

## 2.4 Spacing, Radius, Elevation

- **Spacing unit**: MUI default 8px base (`theme.spacing(1) = 8px`), used exclusively via the `theme.spacing()` function — no hardcoded pixel margins in component code. Touch targets and form-field vertical rhythm are built on multiples of this unit (see 7.6 for the 44×44px minimum touch-target rule, which is 5.5 spacing units).
- **Corner radius**: 8px for cards/inputs, 4px for chips/buttons, 16px for the scan-cue overlay and bottom sheet dialogs on mobile (a rounder, more "touch-friendly" radius signals the mobile-only surface).
- **Elevation**: used sparingly, per Material's semantic-elevation intent, not decoratively. `elevation={0}` for the data-table container (a bordered flat surface, not a floating card, since the table *is* the page). `elevation={1}` for dashboard KPI cards. `elevation={2}` for menus/popovers. `elevation={3}` for dialogs/modals. `elevation={4}`, reserved exclusively for the Continuous Scan Mode's persistent bottom action bar and the Offline Sync Indicator, both of which must read as unambiguously "docked above the content" on a small screen where the user's thumb is nearby.

## 2.5 Iconography and Fonts — No External CDN Dependency

**[DECISION]** All icons are sourced from `@mui/icons-material` (Material Symbols), imported as tree-shaken SVG React components, never loaded from a font-icon CDN. A small bespoke SVG icon set (≤15 icons) covers domain-specific marks the Material set doesn't have: the 5-point condition-scale faces (Good/Fair/Minor/Major/Unusable), the scan-success checkmark-in-circle cue, and the offline/syncing cloud-arrow glyph — all drawn to Material's icon grid and stroke weight so they read as part of the same family.

This same "no external CDN" rule governs the type family: Roboto is **self-hosted** (bundled as static assets in the frontend build, served from the app's own origin), not pulled from Google Fonts. BRD's BO-5 ("zero mandatory outbound internet calls") and SRS NFR-DEPLOY-02 ("zero mandatory outbound internet connectivity") make a webfont CDN a hard violation, not a nice-to-have optimization — an auditor scanning in a basement with no internet (only LAN to the on-prem server) must never have the UI degrade because a font request timed out. The CSS `font-family` stack falls back to `system-ui, -apple-system, "Segoe UI", Roboto, sans-serif` so the app still renders correctly even before/if the self-hosted font finishes loading.

## 2.6 Breakpoints

MUI's default breakpoints are adopted as-is because they already map cleanly onto the SRS 5.12 browser matrix's implied device set and NFR-ACC-02 ("responsive across desktop, tablet, and mobile"):

| Breakpoint | Range | Primary device | Admin shell behavior |
|---|---|---|---|
| `xs` | 0–599px | Phone portrait | Nav drawer hidden behind hamburger; this is also the Audit/Mobile shell's native width |
| `sm` | 600–899px | Phone landscape / small tablet | Nav drawer hidden behind hamburger |
| `md` | 900–1199px | Tablet landscape / small laptop | Nav collapses to icon-only rail (labels on hover/focus) |
| `lg` | 1200–1535px | Desktop | Full nav drawer, standard data-table column set |
| `xl` | 1536px+ | Wide desktop/monitor | Full nav drawer, data tables show additional secondary columns by default |

## 2.7 Dark Mode Stance

**[DECISION — dark mode is out of scope for R1 and R2; revisit in R3.** IAMS ships one WCAG-AA-validated light theme. Rationale: (1) BO-7/NFR-ACC-01 require zero open Critical/High accessibility findings before go-live, and a second theme doubles the contrast-audit surface for no requirement-driven benefit — nothing in the BRD/FRS/SRS asks for dark mode; (2) the audit-scanning shell (Section 6.2) already uses a high-contrast, large-touch-target design that performs fine in bright facility lighting (its actual environment) without a dark variant; (3) MUI's token architecture means adding a dark palette later is additive, not a rewrite — deferring costs little. If a future release adds dark mode, it must reuse the same token names with swapped values and pass the same AA contrast gate before shipping, and `prefers-color-scheme` auto-detection should not be enabled until that palette exists (do not let the OS silently put a user into an unaudited dark theme).

# 3. Component Architecture

## 3.1 Layering

Atomic design over MUI primitives — MUI components (`Button`, `TextField`, `Table`, `Dialog`, …) *are* the atoms/molecules layer where MUI already provides one; IAMS-specific components are built one layer up.

- **Atoms**: thin, styled wrappers over MUI primitives applying IAMS theme conventions — `IamsButton`, `IamsTextField`, `StatusChip` (status→color/icon/label mapping table, single source of truth for every status color used anywhere), `ConditionFace` (one icon in the 5-point condition scale).
- **Molecules**: small compositions with local behavior, no data fetching — `ConditionScaleSelector` (5 `ConditionFace` atoms + selection state), `SearchInput` (debounced, used in both global search and table filter bars), `RoleBadge`, `TimestampLocal` (renders a UTC timestamp in the viewer's configured timezone per NFR-I18N-02), `CurrencyAmount` (renders reporting-currency amount with original-currency tooltip per FR-INV-10).
- **Organisms**: self-contained, often data-fetching, product-specific building blocks. This is where the product's real complexity lives — see 3.2 for the required list.
- **Templates**: page-level layout skeletons with slots, no real data — `AdminListPageTemplate`, `AdminDetailPageTemplate`, `ApprovalInboxTemplate`, `ScanModeTemplate`, `DashboardTemplate`, `WizardTemplate`.
- **Pages**: route-level components (React Router route elements) that wire a template + organisms to live data via the hooks in Section 8.

## 3.2 Required Product-Specific Organisms

| Organism | Purpose | Key FR/NFR |
|---|---|---|
| `ExpectedVerifiedCounter` | Live Expected-vs-Verified tally, the anchor widget of Continuous Scan Mode and the Audit Dashboard | FR-AUD-07, FR-DSH-02 |
| `ScopeChangeBanner` | Non-dismissible-until-actioned banner marking an asset "Scope Changed During Audit," distinct visual treatment from Missing/Damaged, with a required-disposition CTA | FR-AUD-23 |
| `LockedToggleWithExplanation` | A toggle/switch rendered in a locked state with an inline explanation of *why* (mandatory notification, or blocked self-approval) rather than being hidden | FR-NTF-05, FR-USR-06 |
| `OfflineSyncIndicator` | Persistent, always-visible connectivity/sync-queue-depth indicator | FR-AUD-19, NFR-AVAIL-05 |
| `ConditionScaleSelector` | 5-point condition assessment control (Good/Fair/Minor Damage/Major Damage/Unusable) | FR-AUD-09 |
| `AssetHistoryTimeline` | Append-only, chronological rendering of an asset's full history (status/location/assignment/condition changes) | FR-AST-10, FR-AST-11 |
| `ApprovalActionBar` | Standard Approve/Reject/Request-Clarification action row used across Transfers and Audit approvals, with the self-approval block surfaced inline (not a hidden button) | FR-LIF-05, FR-AUD-13, FR-USR-06 |
| `SoDBlockedNotice` | Explains *why* an approval action is unavailable because the current user is the entity's creator/submitter (shares the "locked-with-explanation" visual language of `LockedToggleWithExplanation`) | FR-USR-06, FR-AUD-22 |
| `ScanFeedbackCue` | Full-screen-flash + haptic trigger + auditory-optional cue on successful scan; also renders the duplicate-scan warning variant | FR-AUD-05, FR-SCN-04 |
| `DamagedAssetCaptureForm` | Photo capture + `ConditionScaleSelector` + remarks, inline within the scan flow | FR-AUD-09, FR-AUD-10, FR-AUD-11 |
| `IamsDataTable` | The one filtering/sorting/pagination pattern used everywhere (Section 6.1.2) | FR-RPT-01 pattern, NFR-SCALE-03 |
| `BulkImportWizard` | Upload → dry-run report → commit → reconciliation report, multi-step | FR-MIG-01, FR-MIG-03 |
| `LabelPrintPanel` | Batch label preview/print against the FR-SCN-07 symbology standard | FR-AST-02, FR-RPT-11 |
| `RoleGate` | Non-visual wrapper: hides an action if the role/scope check fails client-side, *and* is always paired with 403-handling at the call site (Section 12.5) | NFR-SEC-02 |
| `ExportProgressToast` | Background/async export job progress + "ready" notification, used by any large report/export | NFR-PERF-04 |
| `WaiverBadge` | Marks an action taken under an active Separation-of-Duties waiver distinctly, wherever that action appears (history, approvals) | FR-USR-07 |

# 4. Route Tree (React Router)

Routes are grouped by module, each annotated with the role(s) permitted, the release it ships in, and which shell it renders. `RoleGate`-wrapped routes redirect to `/forbidden` (a friendly explanation page, not a raw 403) if the client-side role check fails; the underlying API call is still the authority (Section 12.5).

```jsx
<Routes>
  {/* --- Public / Auth --- */}
  <Route path="/login" element={<LoginPage />} />                                  {/* R1, unauthenticated */}
  <Route path="/forbidden" element={<ForbiddenPage />} />                          {/* R1 */}
  <Route path="/offline" element={<OfflineFallbackPage />} />                      {/* R1, PWA */}

  <Route element={<RequireAuth />}>
    {/* ===================== ADMIN / DESKTOP SHELL ===================== */}
    <Route element={<AdminShell />}>
      <Route index element={<Navigate to="/dashboard" />} />
      <Route path="/dashboard" element={<DashboardPage />} />                       {/* R2 full; R1 stub with Asset Register KPI only — DSH */}

      {/* Asset Management — AST — R1 */}
      <Route path="/assets" element={<AssetListPage />} />
      <Route path="/assets/new" element={<RoleGate roles={['InventoryManager','Admin','SuperAdmin']}><AssetFormPage mode="create" /></RoleGate>} />
      <Route path="/assets/:assetId" element={<AssetDetailPage />} />
      <Route path="/assets/:assetId/edit" element={<RoleGate roles={['InventoryManager','Admin','SuperAdmin']}><AssetFormPage mode="edit" /></RoleGate>} />
      <Route path="/assets/categories" element={<RoleGate roles={['Admin','SuperAdmin']}><CategoryConfigPage /></RoleGate>} />
      <Route path="/assets/labels/print" element={<LabelPrintPage />} />

      {/* Inventory Management — INV — R2 */}
      <Route path="/inventory" element={<RoleGate roles={['InventoryManager','Admin','SuperAdmin']}><InventoryListPage /></RoleGate>} />
      <Route path="/inventory/items/:itemId" element={<InventoryItemDetailPage />} />
      <Route path="/inventory/warehouses" element={<WarehouseConfigPage />} />
      <Route path="/inventory/transfers/new" element={<StockTransferFormPage />} />
      <Route path="/inventory/vendors" element={<VendorListPage />} />

      {/* Organization Management — ORG — R1 */}
      <Route path="/organization/hierarchy" element={<RoleGate roles={['SuperAdmin']}><OrgHierarchyPage /></RoleGate>} />
      <Route path="/organization/employees" element={<EmployeeListPage />} />
      <Route path="/organization/employees/:personId" element={<EmployeeDetailPage />} />

      {/* Asset Lifecycle Management — LIF — R2 */}
      <Route path="/lifecycle/purchase-requests" element={<PurchaseRequestListPage />} />
      <Route path="/lifecycle/purchase-orders" element={<PurchaseOrderListPage />} />
      <Route path="/lifecycle/assignments" element={<AssignmentListPage />} />
      <Route path="/lifecycle/transfers" element={<TransferInboxPage />} />          {/* ApprovalActionBar for Dept Head */}
      <Route path="/lifecycle/transfers/:transferId" element={<TransferDetailPage />} />
      <Route path="/lifecycle/maintenance" element={<MaintenanceListPage />} />
      <Route path="/lifecycle/disposals" element={<DisposalListPage />} />

      {/* Audit Management — AUD — R2 (planning/review only; execution is the Audit shell below) */}
      <Route path="/audits" element={<AuditListPage />} />
      <Route path="/audits/new" element={<RoleGate roles={['Auditor','Admin','SuperAdmin']}><AuditPlanningWizardPage /></RoleGate>} />
      <Route path="/audits/:auditId" element={<AuditReviewPage />} />
      <Route path="/audits/:auditId/scope-changes" element={<ScopeChangeReviewPage />} /> {/* FR-AUD-23 */}
      <Route path="/audits/:auditId/certificate" element={<AuditCertificatePage />} />

      {/* Reporting — RPT — R1 basic (3 reports), R2 full */}
      <Route path="/reports" element={<ReportCatalogPage />} />
      <Route path="/reports/:reportType" element={<ReportViewerPage />} />

      {/* Administration */}
      <Route path="/admin/users" element={<RoleGate roles={['Admin','SuperAdmin']}><UserListPage /></RoleGate>} />                          {/* USR — R1 */}
      <Route path="/admin/users/:userId" element={<RoleGate roles={['Admin','SuperAdmin']}><UserDetailPage /></RoleGate>} />
      <Route path="/admin/roles" element={<RoleGate roles={['SuperAdmin']}><RoleConfigPage /></RoleGate>} />
      <Route path="/admin/security/activity-log" element={<RoleGate roles={['SecurityOfficer','SuperAdmin']}><ActivityLogPage /></RoleGate>} />   {/* SEC — R1 */}
      <Route path="/admin/security/sessions" element={<RoleGate roles={['SecurityOfficer','SuperAdmin']}><SessionsPage /></RoleGate>} />
      <Route path="/admin/security/password-policy" element={<RoleGate roles={['SuperAdmin']}><PasswordPolicyPage /></RoleGate>} />
      <Route path="/admin/migration/import" element={<RoleGate roles={['SuperAdmin']}><BulkImportPage /></RoleGate>} />                          {/* MIG — R1 */}
      <Route path="/admin/migration/import/:jobId" element={<RoleGate roles={['SuperAdmin']}><BulkImportJobDetailPage /></RoleGate>} />
      <Route path="/admin/migration/export" element={<RoleGate roles={['SuperAdmin']}><BulkExportPage /></RoleGate>} />                          {/* MIG — R3 */}
      <Route path="/admin/integrations" element={<RoleGate roles={['SuperAdmin','ComplianceOfficer']}><IntegrationsPage /></RoleGate>} />         {/* INT — R3 */}
      <Route path="/admin/compliance/retention" element={<RoleGate roles={['ComplianceOfficer','SuperAdmin']}><RetentionPolicyPage /></RoleGate>} /> {/* CMP — R1 config, R2 automation surfacing */}
      <Route path="/admin/compliance/legal-holds" element={<RoleGate roles={['ComplianceOfficer','SuperAdmin']}><LegalHoldPage /></RoleGate>} />
      <Route path="/admin/compliance/anonymization-queue" element={<RoleGate roles={['ComplianceOfficer']}><AnonymizationQueuePage /></RoleGate>} />
      <Route path="/admin/compliance/waivers" element={<RoleGate roles={['SuperAdmin','SecurityOfficer']}><SoDWaiverPage /></RoleGate>} />         {/* R1 */}
      <Route path="/admin/compliance/privacy-notice" element={<RoleGate roles={['ComplianceOfficer']}><PrivacyNoticeConfigPage /></RoleGate>} />
      <Route path="/admin/analytics/usage" element={<RoleGate roles={['Admin','SuperAdmin']}><UsageReportPage /></RoleGate>} />                    {/* ANL — R2 */}

      {/* Account / personal — all roles, R1 */}
      <Route path="/account/profile" element={<ProfilePage />} />
      <Route path="/account/notification-preferences" element={<NotificationPreferencesPage />} />
      <Route path="/account/my-assets" element={<MyAssetsPage />} />                {/* Sam's primary screen */}
      <Route path="/feedback" element={<FeedbackFormPage />} />                     {/* R2 */}
      <Route path="/search" element={<GlobalSearchResultsPage />} />
    </Route>

    {/* ===================== AUDIT / MOBILE SHELL ===================== */}
    {/* Always this shell regardless of viewport — see Section 1.1 */}
    <Route element={<AuditShell />}>
      <Route path="/audit-mode" element={<RoleGate roles={['Auditor']}><MyAuditsPage /></RoleGate>} />              {/* R2 */}
      <Route path="/audits/:auditId/scan" element={<RoleGate roles={['Auditor']}><ContinuousScanPage /></RoleGate>} /> {/* R2 — the core screen, Section 6.2 */}
      <Route path="/audits/:auditId/scan/finding/:assetId" element={<RoleGate roles={['Auditor']}><DamagedAssetCapturePage /></RoleGate>} />
      <Route path="/audit-mode/search" element={<RoleGate roles={['Auditor']}><MobileSearchPage /></RoleGate>} />
    </Route>
  </Route>

  <Route path="*" element={<NotFoundPage />} />
</Routes>
```

# 5. Page-by-Page Breakdown by Module

Each row: key screen, route, primary components (from Section 3), the FR/UC IDs it satisfies, role access, and release. Screens are grouped by FRS module. "RG" in Role Access = enforced via `RoleGate` + server 403 handling.

## 5.1 Asset Management (AST) — R1

| Screen | Route | Primary Components | FR/UC | Roles |
|---|---|---|---|---|
| Asset Register (list) | `/assets` | `IamsDataTable`, `SearchInput`, filter drawer (category/dept/location/status) | FR-RPT-01, FR-AST-07 | All (scope-limited) |
| Register New Asset | `/assets/new` | `WizardTemplate` (category → custom fields → attachments → review), `ConditionScaleSelector` (initial condition), `LabelPrintPanel` preview | FR-AST-01/02/05/06, UC-AST-01 | RG: Inventory Manager, Admin, SuperAdmin |
| Asset Detail | `/assets/:assetId` | `AssetHistoryTimeline`, `StatusChip`, attachment gallery, `WaiverBadge` where applicable | FR-AST-07/10/11/13/14 | All (scope-limited); edit RG |
| Edit Asset | `/assets/:assetId/edit` | Same form as create, optimistic-locking conflict dialog (Section 8.4) | FR-AST-06/09 | RG: Inventory Manager, Admin, SuperAdmin |
| Category & Custom Field Config | `/assets/categories` | Dynamic field builder (label, type, required, JSONB vs. dedicated-column note surfaced to Admin as read-only info) | FR-AST-03/06 | RG: Admin, SuperAdmin |
| Batch Label Print | `/assets/labels/print` | `LabelPrintPanel`, filtered-set picker reusing `IamsDataTable` selection | FR-AST-02, FR-RPT-11, FR-SCN-07 | RG: Inventory Manager, Admin, SuperAdmin |

## 5.2 Inventory Management (INV) — R2

| Screen | Route | Primary Components | FR/UC | Roles |
|---|---|---|---|---|
| Inventory List | `/inventory` | `IamsDataTable` w/ low-stock `StatusChip`, expiry-approaching flag | FR-INV-01/04/09 | RG: Inventory Manager, Admin, SuperAdmin |
| Item Detail | `/inventory/items/:itemId` | Stock In/Out action bar, batch/lot expiry list, `CurrencyAmount` valuation | FR-INV-02/06/09 | RG |
| Warehouse Config | `/inventory/warehouses` | Shelf/bin tree editor | FR-INV-03 | RG: SuperAdmin, Admin |
| Stock Transfer | `/inventory/transfers/new` | Source/destination pickers, quantity field w/ live available-qty validation, atomic-transfer confirmation | FR-INV-08, UC-INV-01, NFR-CONC-02 | RG: Inventory Manager |
| Vendors | `/inventory/vendors` | `IamsDataTable`, purchase-history drill-in | FR-INV-07 | RG |

## 5.3 Organization Management (ORG) — R1

| Screen | Route | Primary Components | FR/UC | Roles |
|---|---|---|---|---|
| Org Hierarchy Editor | `/organization/hierarchy` | Drag-reorder tree view, inline level-relabeling, delete-blocked-with-dependents dialog | FR-ORG-01/02/05, UC-ORG-01 | RG: SuperAdmin |
| Employee/Volunteer List | `/organization/employees` | `IamsDataTable`, PII fields visually flagged (small lock icon) per CMP tagging | FR-ORG-04 | RG: Admin, SuperAdmin (view); others per scope |
| Employee/Volunteer Detail | `/organization/employees/:personId` | Assigned-assets panel (reuses `MyAssetsPage` list component), offboarding CTA | FR-ORG-04, FR-USR-05, UC-USR-01 | RG: Admin, SuperAdmin |

## 5.4 Asset Lifecycle Management (LIF) — R2

| Screen | Route | Primary Components | FR/UC | Roles |
|---|---|---|---|---|
| Purchase Requests | `/lifecycle/purchase-requests` | `IamsDataTable`, `ApprovalActionBar` | FR-LIF-01 | RG |
| Purchase Orders | `/lifecycle/purchase-orders` | `IamsDataTable`, PO→received-asset linkage | FR-LIF-02/03 | RG: Inventory Manager, Admin |
| Assignments | `/lifecycle/assignments` | Assign-to picker (employee/dept/room), acknowledgment status chip | FR-LIF-04 | RG: Inventory Manager, Admin |
| Transfer Approval Inbox | `/lifecycle/transfers` | `ApprovalActionBar`, `SoDBlockedNotice` when applicable | FR-LIF-05, UC-LIF-01, FR-USR-06 | RG: Dept Head, Admin, SuperAdmin |
| Transfer Detail | `/lifecycle/transfers/:transferId` | Single-screen asset+source+destination review (per UC-LIF-01 "reviews...in a single screen") | FR-LIF-05 | RG |
| Maintenance | `/lifecycle/maintenance` | Repair/Preventive/Corrective tabs, downtime & cost fields | FR-LIF-06/07/08 | RG: Inventory Manager |
| Disposals | `/lifecycle/disposals` | Disposition wizard, open-insurance-claim warning interstitial | FR-LIF-09, UC-LIF-02 | RG: Inventory Manager (initiate), Admin (approve) |

## 5.5 Audit Management (AUD) + Scanning (SCN) — R2, the Core Differentiator

| Screen | Route | Primary Components | FR/UC | Roles |
|---|---|---|---|---|
| Audit List | `/audits` | `IamsDataTable`, `ExpectedVerifiedCounter` per row, overdue flag (UC-DSH-01) | FR-AUD-01/16 | RG: Auditor, Read-only Auditor, Admin, Dept Head (own scope) |
| Audit Planning Wizard | `/audits/new` | `WizardTemplate` (type/scope → sampling method (FR-AUD-20) → assign auditor) | FR-AUD-01/02/03/20 | RG: Auditor, Admin, SuperAdmin |
| My Audits (mobile home) | `/audit-mode` | Grouped list, `ExpectedVerifiedCounter`, `OfflineSyncIndicator` pinned | FR-AUD-01, UC-AUD-01 | RG: Auditor |
| **Continuous Scan Mode** | `/audits/:auditId/scan` | `ExpectedVerifiedCounter`, `ScanFeedbackCue`, `OfflineSyncIndicator` — see full spec Section 6.2 | FR-AUD-04/05/07, FR-SCN-01–05, UC-AUD-01, UC-SCN-01 | RG: Auditor |
| Damaged Asset Capture | `/audits/:auditId/scan/finding/:assetId` | `DamagedAssetCaptureForm` (photo + `ConditionScaleSelector` + remarks) | FR-AUD-09/10/11, UC-AUD-02 | RG: Auditor |
| Audit Review (desktop) | `/audits/:auditId` | `ExpectedVerifiedCounter`, Exception Report table, digital signature capture, `ApprovalActionBar` | FR-AUD-08/12/13/14/15, UC-AUD-03 | RG: Auditor (submit), Dept Head/SuperAdmin (approve) |
| Scope Change Review | `/audits/:auditId/scope-changes` | `ScopeChangeBanner` list, per-item disposition action (confirm/exclude/accept-as-exception), blocks finalize until resolved | FR-AUD-23 | RG: Auditor, Dept Head |
| Audit Certificate | `/audits/:auditId/certificate` | Read-only certificate view/print/PDF | FR-AUD-14 | RG: all with audit view access |
| Reconciliation | (inline on Asset Detail, triggered by scanning a previously-Missing asset from any scan context) | Reconciliation dialog, linked-exception reference | FR-AUD-21, UC-AUD-04 | RG: Auditor |
| Mobile Search | `/audit-mode/search` | `SearchInput`, closest-match fallback list | FR-SRC-01/02, UC-SRC-01 | RG: Auditor |

## 5.6 Reporting (RPT) — R1 basic, R2 full

| Screen | Route | Primary Components | FR/UC | Roles |
|---|---|---|---|---|
| Report Catalog | `/reports` | Card grid grouped by category, R1 shows 3 cards only | FR-RPT-01–13 | All (per-report scope) |
| Report Viewer | `/reports/:reportType` | `IamsDataTable` in "report mode" (totals row, export bar), `ExportProgressToast` for large sets | FR-RPT-01–10/12, UC-RPT-01 | All (per-report scope) |

R1 report types: Asset Register (FR-RPT-01), Employee Asset List (FR-RPT-03). R2 adds: Department/Room/Building Inventory (FR-RPT-02), Missing/Lost/Damaged (FR-RPT-04), Warranty/AMC/Insurance Expiry (FR-RPT-05), Purchase History/Vendor (FR-RPT-06), Asset Movement (FR-RPT-07), Audit Compliance/Summary (FR-RPT-08), Depreciation (FR-RPT-09), Maintenance History (FR-RPT-10), plus scheduled reports (FR-RPT-13).

## 5.7 Dashboard (DSH) — R2 (R1 ships a single Asset Register KPI card as a stub)

| Screen | Route | Primary Components | FR/UC | Roles |
|---|---|---|---|---|
| Dashboard | `/dashboard` | Role-filtered KPI card grid, `ExpectedVerifiedCounter` per active audit, expiry/low-stock alert list, activity feed, audit calendar | FR-DSH-01–07, UC-DSH-01 | All (content filtered per FR-DSH-07) |

## 5.8 User Management & RBAC (USR) — R1

| Screen | Route | Primary Components | FR/UC | Roles |
|---|---|---|---|---|
| User List | `/admin/users` | `IamsDataTable`, role badges (multi-role per user, per PUC 2.1) | FR-USR-01/02 | RG: Admin, SuperAdmin |
| User Detail / Offboarding | `/admin/users/:userId` | Role assignment multi-select, **Offboarding wizard**: blocks deactivation until all assigned assets are reassigned/returned, lists them explicitly | FR-USR-05, UC-USR-01 | RG: Admin, SuperAdmin |
| Role Config | `/admin/roles` | Custom role builder, permission-matrix editor, org-scope binding | FR-USR-02/04 | RG: SuperAdmin |
| SoD Waivers | `/admin/compliance/waivers` | Waiver record form (who/date/scope/Security Officer sign-off flag), active-waiver list w/ `WaiverBadge` cross-reference | FR-USR-06/07 | RG: SuperAdmin, Security Officer |

## 5.9 Notifications (NTF) — R2 (in-app feed is R1-adjacent, ships with USR)

| Screen | Route | Primary Components | FR/UC | Roles |
|---|---|---|---|---|
| Notification Feed | header bell + `/notifications` | Read/unread list, deep-links into source entity (per UC-LIF-01) | FR-NTF-03/04 | All |
| Notification Preferences | `/account/notification-preferences` | Per-event-type Email/In-App/Both/None grid, `LockedToggleWithExplanation` for mandatory types | FR-NTF-05, UC-NTF-01 | All |

## 5.10 Search (SRC) — R1 (basic), R2 (saved searches)

| Screen | Route | Primary Components | FR/UC | Roles |
|---|---|---|---|---|
| Global Search | `/search` (header search box everywhere) | `SearchInput`, faceted results, closest-match fallback | FR-SRC-01–04 | All (result set scope-filtered) |

## 5.11 Security (SEC) — R1

| Screen | Route | Primary Components | FR/UC | Roles |
|---|---|---|---|---|
| Activity Log | `/admin/security/activity-log` | `IamsDataTable`, filter by user/date/event type | FR-SEC-04, UC-SEC-01 | RG: Security Officer, SuperAdmin |
| Sessions | `/admin/security/sessions` | Active session list, force-logout action | FR-SEC-01/06 | RG: SuperAdmin |
| Password Policy | `/admin/security/password-policy` | Policy config form | FR-SEC-05 | RG: SuperAdmin |

## 5.12 Data Migration & Bulk Import/Export (MIG) — R1 import, R3 export

| Screen | Route | Primary Components | FR/UC | Roles |
|---|---|---|---|---|
| Bulk Import | `/admin/migration/import` | `BulkImportWizard` (template download → upload → dry-run report → commit) | FR-MIG-01/03/04, UC-MIG-01 | RG: SuperAdmin |
| Import Job Detail | `/admin/migration/import/:jobId` | Async job status poller, per-row error table, reconciliation summary | FR-MIG-03/04 | RG: SuperAdmin |
| Bulk Export | `/admin/migration/export` | Entity picker, export job trigger, `ExportProgressToast` | FR-MIG-02 | RG: SuperAdmin — **R3** |

## 5.13 External Integrations (INT) — R3

| Screen | Route | Primary Components | FR/UC | Roles |
|---|---|---|---|---|
| Integrations | `/admin/integrations` | Per-integration enable/disable toggle (default off, FR-INT-05), status/last-sync indicator, Compliance-sign-off gate before enable | FR-INT-01–05, UC-INT-01 | RG: SuperAdmin (enable), Compliance Officer (sign-off), Viewer (status read-only) |

## 5.14 Compliance & Data Privacy (CMP) — R1 config, R2 workflow depth

| Screen | Route | Primary Components | FR/UC | Roles |
|---|---|---|---|---|
| Retention Policy | `/admin/compliance/retention` | Per-entity-type retention period config | FR-CMP-01 | RG: Compliance Officer, SuperAdmin |
| Legal Holds | `/admin/compliance/legal-holds` | Hold flag list, place/lift-hold actions with reason capture | FR-CMP-06 | RG: Compliance Officer |
| Anonymization Queue | `/admin/compliance/anonymization-queue` | Flagged-eligible list, legal-hold-blocked notice, approve action | FR-CMP-02, UC-CMP-01 | RG: Compliance Officer |
| Privacy Notice Config | `/admin/compliance/privacy-notice` | Basis-of-collection field mapping, generated-notice preview | FR-CMP-03 | RG: Compliance Officer |
| On-Prem Data Residency Confirmation | (panel within Retention Policy page) | Static confirmation view of data-store locations | FR-CMP-05 | RG: Compliance Officer, Security Officer |

## 5.15 Product Analytics (ANL) — R2

| Screen | Route | Primary Components | FR/UC | Roles |
|---|---|---|---|---|
| Usage Report | `/admin/analytics/usage` | Feature-adoption-by-role chart set, unused-feature flag list | FR-ANL-01/02/03, UC-ANL-01 | RG: Admin, SuperAdmin |
| Feedback Form | `/feedback` | Free-text + category form | FR-ANL-04 | All |

# 6. The Two Experience Modes In Depth

## 6.1 Admin/Desktop Experience

### 6.1.1 Layout Shell

`AdminShell` is a persistent three-region layout: a top `AppBar` (global search, notification bell, org-switcher if multi-node scope, account menu), a left `NavDrawer` (module nav per Section 1.3, collapses to icon rail at `md`, hidden behind a hamburger below `md`), and a content region with a breadcrumb trail (`Module > Sub-section > Entity`) directly under the AppBar so a user is never unsure where they are after a deep link or a role-scoped redirect. The content region has a max-width of `xl` with centered gutters on ultra-wide monitors — data tables are the one exception, which use the full available width because column density is the point.

### 6.1.2 Data-Table Convention (`IamsDataTable`)

One filtering/sorting/pagination pattern is used everywhere a collection is shown at scale (Asset Register, all Reports, Activity Log, User List, Audit List, Inventory List) — consistency here is a direct usability requirement given NFR-SCALE-01/03 (100,000+ assets, mandatory pagination):

- **Toolbar**: a persistent search box (client-debounced, 300ms) on the left; a "Filters" button on the right opens a slide-over filter panel (category/department/location/status/date-range as applicable per screen) rather than inline filter chips cluttering the header row — filters that *are* active render as removable chips below the toolbar so their state stays visible.
- **Pagination**: server-side, page-size selectable (25/50/100), never client-side "load all" — required at 100,000-asset scale (NFR-SCALE-03). Page state, filters, and sort are encoded in the URL query string so a filtered/sorted view is bookmarkable and back-button-safe.
- **Sorting**: single-column sort via clickable column headers with a visible sort-direction icon (never sort-on-hover-only, which fails keyboard/touch); server-side sort for indexed columns.
- **Row density**: "Comfortable" (48px row height) default; a density toggle (Comfortable/Compact) persists per-user in `localStorage`, not sent to the server.
- **Bulk selection**: checkbox column when the screen supports bulk actions (label printing, bulk export selection) — selection state also persists across pagination within a session via row-ID set, not per-page state, so a user can select across 3 pages before acting.
- **Column configuration**: a "Columns" menu lets a user show/hide non-essential columns, persisted per-user, per-screen in `localStorage`. Essential identity columns (asset number, name) are never hideable.
- **Empty and loading states**: see Section 12.

### 6.1.3 Approval-Workflow UI Pattern

Every approval surface (Transfer approvals, Audit approvals, Purchase Request approvals) uses the same `ApprovalActionBar` + `SoDBlockedNotice` pair, matching UC-LIF-01's design intent ("reviews...in a single screen," "taps Approve"):

1. A single-screen summary of the entity under review (no drill-through required for the common case) — e.g., asset + source + destination for a transfer, or Expected/Verified counts + Exception Report for an audit.
2. `ApprovalActionBar` renders **Approve / Reject / Request Clarification** as equally-weighted, clearly labeled buttons (never an icon-only approve action — this is a consequential, auditable action and needs a text label per SC 2.4.6 and general usability).
3. **If the current user is the entity's creator/submitter**, the Approve button is replaced by `SoDBlockedNotice`: a clearly worded inline message ("You registered this asset's valuation, so you can't also approve this finding — routed to [Department Head name] / [Super Administrator] instead," FR-USR-06/FR-AUD-22) rather than the button silently disappearing. This is the single most important pattern in the admin shell per the task's decision #5 — a missing button with no explanation reads as a bug, not a control.
4. Reject requires a reason (short text, required field) — visible later to the submitter per UC-LIF-01's exception flow.
5. Every approve/reject/clarify action writes to `AssetHistoryTimeline` immediately and is reflected in the audit trail (FR-AST-10).
6. If the action was taken under an active SoD waiver (FR-USR-07), `WaiverBadge` renders next to the action in history so a later reviewer can distinguish waiver-covered activity from normal enforcement, per FR-USR-07's explicit distinct-logging requirement.

## 6.2 Mobile/PWA Audit-Scanning Experience — Continuous Scan Mode in Full

This is the single most usage-critical screen in the product (BRD 1.2, FR-AUD-05). It is specified state-by-state.

### 6.2.1 One-Handed Layout Rationale

Devon walks a facility holding a clipboard, a set of keys, or steadying himself; the phone is in one hand, thumb doing all the work (NFR-ACC-03). The layout is therefore organized around a **thumb-reachable bottom zone** (per established mobile ergonomics: the bottom third of a 6"+ phone screen is reachable by the thumb without a grip change) and treats the top third as **glanceable-only, not interactive**:

- **Top zone (glanceable, non-interactive)**: audit name, `ExpectedVerifiedCounter` ("47 / 62 verified"), `OfflineSyncIndicator`. No buttons live here — nothing requires a reach.
- **Middle zone**: the live camera viewport (when camera scanning is active) or the last-scanned-asset confirmation card (when using a USB/Bluetooth hardware scanner, where no camera viewport is needed — see 6.2.4).
- **Bottom zone (primary interaction, thumb-reachable, `elevation=4` docked bar)**: the scan-input toggle (camera / hardware-scanner mode), a large **"Flag Issue"** button for the current/last-scanned asset, and an **"End Scan"** button. Buttons here are a minimum 56×56px touch target (larger than the 44×44px floor — see 7.6 — because this is the highest-frequency, highest-consequence tap sequence in the app).

### 6.2.2 Scan Cycle — States and Transitions

| State | Trigger | Visual | Haptic/Audio | Duration |
|---|---|---|---|---|
| **Idle/Ready** | Screen loaded, Continuous Scan Mode active | Camera viewport live with a scan-target reticle overlay; counter shows current tally | — | Until next scan |
| **Resolving** | A code is decoded from the camera frame or hardware-scanner keyboard-wedge input | Reticle briefly pulses; asset name skeleton-loads under the viewport (see 12.2) | none yet | ≤1s target (FR-SCN-05, NFR-PERF-02) |
| **Scan Success** | Code resolves to an expected, not-yet-verified asset in this audit's scope | Full-viewport green flash (`ScanFeedbackCue`, 150ms) + a checkmark-in-circle icon overlays briefly + asset name/thumbnail confirmation card slides up from bottom zone for 1.5s then auto-dismisses; counter increments immediately | Single short haptic pulse (`navigator.vibrate(40)` where supported); no sound by default (silent facility etiquette), with a user-toggleable audio cue in settings for low-vision users who benefit from an audible confirmation in addition to haptic | Returns to Idle automatically — **no manual confirmation tap required**, this is the entire point of FR-AUD-05 |
| **Duplicate Scan** | Code resolves to an asset already verified in this session | Amber flash (not red — this is a non-blocking warning, not an error) + toast: "Already verified — [asset name]" + `color.warning.dark` per 2.2 | Double short haptic pulse (distinct pattern from success) so Devon can distinguish it without looking, per FR-SCN-04 "clear non-blocking warning" | Toast auto-dismisses after 2.5s; scanning is never paused |
| **Unrecognized Code** | Code decodes but matches no known asset | Neutral/gray flash + toast: "Not recognized — saved for review" + logged to an "Unrecognized Scans" list surfaced at end-of-session review (never silently discarded, per UC-AUD-01's exception flow) | Single distinct haptic pattern | Toast auto-dismisses; scanning continues |
| **Out-of-Scope Asset** | Code resolves to a real asset, but not in this audit's expected list | Blue-tinted informational flash + toast offering "View asset" / "Add as unscheduled finding" | Single short haptic | User choice; does not block the scan flow |
| **Flag Issue (Damaged)** | Devon taps "Flag Issue" for the currently/last-confirmed asset | Transitions to `DamagedAssetCapturePage` (a full-screen sub-flow, Section 6.2.3) — scanning pauses while this sub-flow is open, resumes on return | — | Sub-flow, user-paced |
| **Offline** | Network to the on-prem server is unavailable | `OfflineSyncIndicator` switches to "Offline — N scans queued" persistent state (Section 6.2.5); scan cycle above is unaffected — everything above still works locally | — | Until reconnect |

### 6.2.3 Damaged-Asset Capture Flow

Reached from "Flag Issue" mid-scan (UC-AUD-02). Full-screen, single-column, thumb-scrollable, in this fixed order (order matters: evidence first, judgment second, per how an auditor actually looks at a damaged item):

1. **Photo capture** — opens the device camera directly (not a file picker first) with a large shutter button in the bottom zone; supports retake; multiple photos allowed (FR-AUD-10). Skippable only with an explicit "Skip photo" text link, never the default path.
2. **`ConditionScaleSelector`** — 5 large tap targets (Good / Fair / Minor Damage / Major Damage / Unusable), each a labeled icon+text card, not a dropdown (a dropdown on a walking-and-scanning workflow is a usability regression — FR-AUD-09).
3. **Remarks** — a multiline text field, voice-to-text supported natively via the mobile keyboard's built-in dictation (no custom implementation needed — reduces one-handed typing burden), optional but encouraged with placeholder text ("What happened?").
4. **Save Finding** button (bottom zone, full-width) — writes the finding to both the audit and the asset's `AssetHistoryTimeline` simultaneously (per UC-AUD-02 postcondition) and returns to Continuous Scan Mode, resuming exactly where scanning left off.
5. If offline when "Save Finding" is tapped, the finding (including photo) queues identically to a scan (Section 6.2.5) — the user sees the same "queued, will sync" confirmation, never a hard failure (per UC-AUD-02's alternate flow).

### 6.2.4 Input-Method Parity

The same state machine (6.2.2) governs all three scan-input sources (FR-SCN-01/02/03) so Devon's experience doesn't change based on hardware: a USB/Bluetooth scanner in keyboard-wedge mode fires the identical "code resolved" event as a camera-decoded frame — the UI listens for a fast burst-then-Enter keystroke pattern distinguishable from human typing, rather than requiring a dedicated input field to have focus. When a hardware scanner is active, the middle-zone camera viewport is replaced with a large "Ready to scan" idle card (no need to keep a camera preview running and draining battery when it isn't the input source).

### 6.2.5 Offline Behavior (FR-AUD-19, NFR-AVAIL-05)

`OfflineSyncIndicator` is permanently docked in the top zone, never a transient toast, because a user must always be able to glance up and know their queue state without an action:

- **Online, synced**: small green cloud-check icon, no text (minimizes visual noise in the common case).
- **Online, syncing**: spinner-in-cloud icon + "Syncing 3..." — appears briefly after reconnect while the queue drains.
- **Offline**: solid amber cloud-slash icon + "Offline — 12 scans saved, will sync" — persistent, not dismissible, so it's impossible to mistake "offline-but-queued" for "lost." Tapping it expands a short list of queued items with their local timestamps, reinforcing that nothing was lost, per the task's explicit requirement.
- Scanning, flagging, and finding-capture are **never blocked** waiting on a network round-trip — every write in this shell goes to the local IndexedDB queue first (Section 8.4/11.4) and is optimistically reflected in the `ExpectedVerifiedCounter` immediately; the server sync is a background concern the UI surfaces but never gates on.

### 6.2.6 Ending a Scan Session

"End Scan" leads to a review summary (still mobile-shell, not yet the desktop `AuditReviewPage`): counts, the Unrecognized Scans list, and any Scope-Changed items (`ScopeChangeBanner`, FR-AUD-23) requiring disposition before the audit can be finalized — Devon can disposition these on his phone if he chooses, or leave them for desktop review; the audit cannot be finalized (by anyone, on either shell) with an undispositioned scope-change item, which the UI states explicitly.

# 7. Accessibility Implementation Plan (WCAG 2.1 AA)

Accessibility is designed in per-component, not audited in at the end, per the task's framing and FR-CMP-04/NFR-ACC-01 (Must Have, verified before go-live across registration, audit scanning, and reporting specifically).

## 7.1 Focus Management

- Every interactive element uses MUI's built-in focus-visible styling, themed to a 2px `color.primary.dark` outline with a 2px offset — never `outline: none` without a replacement (a recurring, avoidable AA failure).
- Route transitions (React Router) move focus to the page's `h1` (via a `useEffect` + `ref.focus()` on an otherwise-non-interactive, `tabIndex={-1}` heading element) so screen-reader and keyboard users land somewhere meaningful, not silently at the top of a stale DOM.
- Dialogs and the slide-over filter panel (6.1.2) trap focus (MUI `Dialog`/`Drawer` handle this via `Modal` under the hood) and return focus to the triggering element on close.
- The Damaged Asset Capture flow (6.2.3), despite being a full-screen mobile sub-flow, is still a focus-trapped modal-equivalent region for screen-reader/keyboard users, with a clearly labeled "Cancel and return to scanning" exit.

## 7.2 ARIA for Dynamic/Live Content

- `ExpectedVerifiedCounter` is an `aria-live="polite"` region so a screen-reader user hears "48 of 62 verified" update without needing to re-focus it — critical because this is the one piece of information a low-vision auditor most needs read back automatically.
- `ScanFeedbackCue`'s toast messages (Duplicate, Unrecognized, Out-of-Scope) are `role="status"` + `aria-live="polite"`; they are never `aria-live="assertive"` because they must not interrupt a screen-reader user mid-sentence during a rapid scan sequence — the persistent counter and the queued-list (via `OfflineSyncIndicator`) are the durable record, so a missed toast announcement is not data loss.
- `OfflineSyncIndicator`'s state changes (online→offline, queue depth changing) are `aria-live="polite"` on the count only, not on every single scan, to avoid a screen-reader user being flooded with an announcement per scan during a fast walk-through.
- `LockedToggleWithExplanation` and `SoDBlockedNotice` use `aria-describedby` linking the control to its explanation text, so the *reason* is read immediately after the control's state, not left to be discovered separately.
- `ScopeChangeBanner` uses `role="alert"` (assertive) exactly once per item, on first render, since this is a required-action item that must not be missed — distinct from the polite toasts above.

## 7.3 Keyboard Navigation

- `IamsDataTable`: full keyboard operability — arrow keys move cell focus within the grid (`role="grid"` pattern), Enter opens a row, Space toggles row selection, column-header sort is triggerable via Enter/Space on the header button, the "Filters" slide-over and "Columns" menu are both reachable and operable via Tab/Enter/Escape.
- `ApprovalActionBar`: Approve/Reject/Request Clarification are ordinary tab-stops in visual order; Reject's required-reason field receives focus automatically when the Reject flow opens.
- Continuous Scan Mode's camera-based scanning is inherently a pointer/camera interaction, but every fallback path (manual asset-number entry, hardware-scanner keyboard-wedge input, the "Flag Issue"/"End Scan" buttons) is fully keyboard-operable — a keyboard-only user with a Bluetooth scanner can complete an entire audit without touching the screen.
- Skip-link ("Skip to main content") at the top of `AdminShell`, standard pattern, visible on focus.

## 7.4 Color Contrast

Governed by the token table in 2.2. Component-level rule: any text or icon conveying required information (status chips, `ScopeChangeBanner`, `LockedToggleWithExplanation` explanation text, form error text) must resolve to a token meeting 4.5:1 (normal) or 3:1 (large/UI-boundary) — this is enforced as a lint-time check via an `eslint-plugin` custom rule restricting raw hex usage outside the token file, and verified with automated contrast tooling (2.2) before go-live.

## 7.5 Forms and Error Announcement

- Every `IamsTextField`/select/etc. has a programmatically associated `<label>` (never placeholder-as-label).
- Client-side validation errors (Section 9) render inline below the field, red-text + error icon (never color alone), and are additionally announced via a single page-level `aria-live="assertive"` summary region at the top of the form ("3 fields need attention") that appears on failed submit and receives focus — this satisfies both "errors are visible at the field" and "a screen-reader user isn't left silently at the top of a long form not knowing something failed."
- Required fields are marked with both a visual asterisk and `aria-required="true"`, and the required-marker convention (asterisk = required, not the inverse) is stated once in a form-level legend rather than repeated per field.

## 7.6 Touch-Target Sizing (Mobile Scan Screen)

All interactive controls in `AuditShell` meet a 44×44px CSS-pixel minimum (WCAG 2.1 SC 2.5.5, AAA, adopted here as a hard floor even though the app targets AA overall, because it is the correct call for a walking, one-handed, frequently-gloved-or-cold-handed audit context — **[DECISION]**: exceed the AA floor here deliberately). The primary scan-flow buttons (6.2.1) are sized at 56×56px. Minimum 8px spacing between adjacent tap targets in the bottom action zone prevents mis-taps during a fast walk-through.

## 7.7 Testing Gate

Before go-live: automated scan (axe-core in CI, run against every route including `/audits/:id/scan`), manual keyboard-only pass on every template in Section 3.1, and a screen-reader pass (NVDA/JAWS on Windows for the Admin shell, VoiceOver on iOS for the Audit shell, matching SRS 5.12's actual browser/OS matrix) — this is the concrete mechanism behind FR-CMP-04's "verified before go-live" and BO-7's "zero open Critical/High findings."

# 8. State Management & Data Fetching

## 8.1 Server State — TanStack Query

**[DECISION]** TanStack Query (React Query) is the server-state layer for all REST calls to the Spring Boot API (Axios as the HTTP client per SRS Section 3, wrapped in a single configured instance with interceptors for auth-token attach and centralized 401/403 handling). Rationale: IAMS is data-table- and report-heavy (16 modules, most screens are "fetch a paginated/filtered collection or a detail record"); TanStack Query's built-in caching, background refetch, request de-duplication, and pagination-aware query-key conventions map directly onto the `IamsDataTable` pattern (6.1.2) without hand-rolled loading/error state in every screen. It is the de facto standard for this shape of problem in the React ecosystem and keeps custom state-management code to near zero for the ~90% of the app that is CRUD-over-REST.

- Query keys are structured `[module, resource, filters, page, sort]` (e.g., `['AST','assets',{category,dept,status},page,sort]`) so cache invalidation after a mutation (e.g., approving a transfer invalidates `['LIF','transfers', ...]` and the affected `['AST','assets', assetId]`) is precise, not a blunt refetch-everything.
- Mutations use `useMutation` with optimistic updates **only** where the UX benefit is high and the rollback story is clean — notably the scan-cycle counter (6.2.2, rolled back if a queued scan is later rejected by the server, e.g., as a true duplicate across devices) and notification-preference toggles. Everything else (approvals, registration) waits for server confirmation before updating the UI, since these are consequential, infrequent actions where a half-second wait is not a UX cost worth the rollback complexity.
- Stale time is set per-resource: reference data (categories, org hierarchy, condition scale config) is cached long (15 min+) since it changes rarely; live operational data (audit progress counters, notification feed) uses short stale time + `refetchInterval` polling on the Dashboard and Audit List, since IAMS has no requirement for WebSocket/real-time push and polling is architecturally simpler for an on-prem, small-IT-team deployment (consistent with SRS 2.1's "operational simplicity" framing).

## 8.2 Client (UI-Only) State — Zustand

**[DECISION]** Zustand, not Redux, for UI-only state that TanStack Query doesn't own: nav-drawer open/collapsed state, active theme tokens (post dark-mode-in-R3), the in-progress (not-yet-submitted) Continuous Scan Mode session state (current asset, last-scan-result for the confirmation card, local tally before it's reconciled with the server), and the multi-step wizard state for Asset Registration/Bulk Import/Audit Planning. Rationale: Redux's boilerplate (actions/reducers/selectors) is unjustified overhead for state that is genuinely local and doesn't need time-travel debugging or middleware; React Context alone becomes a re-render-performance problem for the scan-session store specifically, since it updates on every single scan during a fast walk-through and would otherwise re-render the entire `AuditShell` subtree. Zustand's selector-based subscription avoids that without the Redux ceremony.

## 8.3 Offline Queue Persistence

**[DECISION]** IndexedDB via Dexie.js (a thin, well-maintained wrapper that avoids hand-rolling the raw IndexedDB API, which is notoriously painful) backs the offline scan/finding queue (FR-AUD-19, NFR-AVAIL-05). A dedicated `iamsOfflineQueue` object store holds pending mutations (`{id, type: 'SCAN'|'FINDING'|'RECONCILE', auditId, payload, capturedAt, syncStatus}`). This is separate from TanStack Query's own cache (which is an in-memory/optional-persisted read cache, not a write-durability mechanism) — the queue is the durability guarantee, the query cache is a performance optimization. See Section 11.4 for how this survives a browser restart.

## 8.4 Sync-Conflict UI (tied to Optimistic Locking)

Per SRS NFR-CONC-01, entity edits use optimistic locking (a `version` column); NFR-CONC-02 covers high-contention inventory quantity mutations with atomic row-level operations instead — the frontend handles these two cases differently:

- **Optimistic-locking conflict (409)**: on any entity edit (asset record, audit finding, lifecycle event), if the server returns 409 because the `version` the client held is stale, the UI shows a non-destructive **Conflict dialog**: "This [asset/finding] was updated by [user] at [time] while you were editing. Review their change before saving yours." — presenting both the server's current values and the user's unsaved edits side by side, with an explicit "Reload and discard my changes" or "Copy my changes into the latest version" choice. It never silently overwrites, and never silently discards the user's typed work without the reload being an explicit choice.
- **Inventory quantity conflict**: since NFR-CONC-02 makes these atomic at the database level (conditional `UPDATE`), the *only* failure mode the frontend sees is "insufficient quantity" (e.g., someone else's Stock Out already dropped the available quantity below what this Transfer needs) — surfaced not as a version conflict but as a plain validation-style error with the actual current available quantity shown, inline, exactly as UC-INV-01's alternate flow specifies ("the system blocks the transfer and shows the actual available quantity"). This is a fundamentally different UI (immediate re-validation, not a merge decision) because the backend already resolved the concurrency at the data layer — the frontend just needs to report the outcome clearly.
- **Offline-queue sync conflict**: if a queued scan/finding, once synced, turns out to duplicate a scan made from a different device while offline (rare, but possible if two auditors briefly cover overlapping scope), the server accepts the first and rejects the second as a true duplicate on sync; the UI surfaces this the same way as a live Duplicate Scan (6.2.2) but retroactively, in the end-of-session review screen, never as a silent drop.

# 9. Forms & Validation

**[DECISION]** React Hook Form + Zod. React Hook Form is chosen over fully-controlled form state (e.g., Formik or hand-rolled `useState` per field) for performance on the app's larger forms (Asset Registration's dynamic custom-field set per FR-AST-06, the Bulk Import mapping step, the Audit Planning Wizard) — uncontrolled-by-default inputs avoid a re-render per keystroke across a form that can have 20+ dynamic fields. Zod provides schema-based validation with first-class TypeScript inference, which pairs directly with the same schema shape the backend's OpenAPI contract implies, keeping the two in sync by convention (schemas live alongside the API client types, not duplicated ad hoc per form).

- **Client-side validation is a UX accelerant only**, never the authority — per the task's decision #4 and SRS 6.7 ("All API inputs are validated server-side... validation is never enforced client-side only"). Every Zod schema mirrors, but does not replace, the server's own validation; a server 400/422 validation error is always mapped back onto the correct field via a shared error-code-to-field-path convention (the API returns `{field: "purchaseCost", code: "MUST_BE_POSITIVE"}`-shaped errors), so a validation rule that exists server-side but was missed client-side still surfaces correctly rather than as a generic toast.
- **Error-display convention**: inline, below the field, on blur (not on every keystroke — this avoids a wall of red appearing while a user is still mid-typing), red text + error icon, with the field's border also switching to `color.error.main`. A failed submit re-validates all fields, scrolls to and focuses the first error, and announces the count via the `aria-live` summary region (7.5).
- **Dynamic custom fields** (FR-AST-06) build their Zod schema at runtime from the category's field configuration (type, required, min/max) fetched from the ORG/AST config API, rather than hardcoding a schema per category — this is the same "no code changes for new custom fields" requirement reflected in the frontend, not just the backend schema.
- **Dry-run validation** (FR-MIG-03, `BulkImportWizard`) is a distinct pattern from per-field client validation: it's a server-side, file-level validation pass whose output (a per-row error report) is rendered as its own reviewable table, not folded into a single form's field errors.

# 10. Internationalization Implementation

## 10.1 Library and Resource Bundles

**[DECISION]** react-i18next, with `i18next-browser-languagedetector` (detects but does not silently switch — the deployment's configured default locale, set by the Super Administrator per organization, wins over browser auto-detection unless the user explicitly overrides it in their profile, since a shared kiosk-style audit tablet should not silently follow whatever language its last user's browser was set to). Resource bundles are organized per FRS module to keep translation work assignable and to avoid one enormous JSON file:

```
/src/locales/
  en/
    common.json        (nav labels, buttons, status labels, StatusChip text)
    ast.json  inv.json  org.json  lif.json  aud.json  scn.json
    rpt.json  dsh.json  usr.json  ntf.json  src.json  sec.json
    mig.json  int.json  cmp.json  anl.json
    validation.json     (Zod error-message keys, shared across modules)
```

Only English ships at R1 (NFR-I18N-01 requires externalization, not a second language), but every user-facing string is a translation key from day one — there is no "add i18n later" migration path being deferred; hardcoding a string directly in JSX is treated as a lint failure (`eslint-plugin-i18next` or equivalent) from the first commit.

## 10.2 Formatting

- **Numbers/dates**: the browser-native `Intl.NumberFormat` / `Intl.DateTimeFormat` APIs, configured from the deployment's locale setting (NFR-I18N-03) — no third-party formatting library needed, keeping bundle size down and avoiding a library whose locale data can drift from the browser's own.
- **Currency (FR-INV-10)**: `CurrencyAmount` (Section 3.1) renders the stored reporting-currency amount via `Intl.NumberFormat(locale, {style:'currency', currency: reportingCurrency})` as the primary figure, with the original transaction currency/amount/FX-rate-as-of-date available in a tooltip/expandable secondary line — never recomputed client-side from a live rate, per FR-INV-10's explicit "use the stored reporting-currency amount, not a rate looked up at report-generation time."
- **Button label casing**: MUI's default `textTransform: 'uppercase'` on buttons is disabled globally (2.3) specifically because automatic uppercasing breaks correctly-cased strings in many non-English scripts and is a known i18n foot-gun — decided now even though only English ships, since retrofitting it after translated strings exist is wasted rework.

## 10.3 Timezone Rendering

Per NFR-I18N-02 ("timestamps stored in UTC, rendered in the viewing user's configured local timezone" — a user-profile setting, not just the browser's own OS timezone, since an organization may have staff reviewing data from a different site's timezone than they physically sit in): the `TimestampLocal` molecule (3.1) takes a UTC ISO string and the current user's `profile.timezone` (IANA identifier, e.g., `America/Chicago`) from the auth/session context, and renders via `Intl.DateTimeFormat(locale, {timeZone: profile.timezone, ...})`. The user's timezone defaults to the browser's detected timezone at first login but is an editable profile field, not re-detected silently thereafter (consistent with 10.1's "don't silently follow the browser" stance) — this matters directly for cross-site organizations (BRD Section 5.2) where a Super Administrator at one campus reviews an audit timestamped from another campus in a different zone.

# 11. PWA Specifics

## 11.1 Service Worker Strategy

Built with Workbox (via `vite-plugin-pwa` or the CRA/Vite-equivalent Workbox integration — whichever the frontend build tool ultimately is; this spec assumes a Vite-based React build as the modern default, generated at build time rather than hand-written, to avoid maintaining cache-invalidation logic by hand):

| Asset class | Strategy | Rationale |
|---|---|---|
| App shell (JS/CSS bundles, self-hosted fonts, icons) | **Precache** (Workbox `precacheAndRoute`, cache-busted by build hash) | The app must launch and render even with zero connectivity to the on-prem server, per NFR-AVAIL-05's "offline scan queue" implying the *app itself*, not just data, must be available offline |
| GET API calls for reference/config data (org hierarchy, categories, condition-scale config, current user profile) | **Stale-while-revalidate** | These change rarely; showing a possibly-few-minutes-stale cached copy while refetching in the background is a better offline/flaky-network experience than blocking |
| GET API calls for an audit's expected-asset list, once an audit is opened for scanning | **Network-first with cache fallback**, explicitly warmed on audit open | Devon opens the audit while still in range of Wi-Fi/LAN before walking into a dead zone; the expected list is cached at that moment so Continuous Scan Mode still has its full expected-list context even if connectivity drops mid-walk |
| GET API calls elsewhere (Asset Register, Reports, Dashboards) | **Network-only** (not cached by the service worker) | These are Admin-shell, desk-based screens where stale data has real business risk (e.g., approving against a stale transfer list) and the desk-based network is assumed reliable per SRS 9's assumptions — offline tolerance is scoped intentionally to the audit-scanning flow only, not the whole app, per NFR-AVAIL-05's own "short, localized connectivity gaps within a facility," not "the whole app works offline" |
| POST/PUT/DELETE (mutations) | **Never cached by the service worker** — mutations go through the IndexedDB offline queue (Section 8.3), not the HTTP cache layer, since a cached response to a write is meaningless; only the scan/finding-capture mutations get offline queueing at all, everything else in the Admin shell simply fails visibly if offline (Section 12.5) since it isn't a supported offline scenario |

## 11.2 Web App Manifest

Minimal, install-relevant fields: `name: "IAMS"`, `short_name: "IAMS"`, `start_url: "/audit-mode"` for the audit-focused install (see 11.3 — the manifest's start URL deliberately lands an installed PWA on Devon's mobile home, not the admin dashboard, since the install prompt is specifically targeted at auditors), `display: "standalone"`, `theme_color` matching `color.primary.main` (2.2), `background_color: "#FFFFFF"`, and a full icon set (192/512px, maskable variant) generated from the app's own icon asset — no external icon CDN, consistent with 2.5.

**[DECISION]** IAMS ships one manifest, not two, but the `start_url` targets the mobile audit entry point specifically, because the realistic install audience is Devon's persona (someone who repeatedly opens this exact app on a phone in the field) — Priya, Marcus, and Elena work at a desk in a regular browser tab and have no real installability need; a generic "install our app" prompt aimed at everyone would be noise for the majority of the user base.

## 11.3 Install-Prompt UX

The browser's native `beforeinstallprompt` event is captured and deferred (never auto-triggered on first load, which is a widely-recognized bad pattern). A contextual, dismissible install banner appears **after Devon completes his first full audit walk-through** (a real, demonstrated-value moment, not an interruption during onboarding) on the `ContinuousScanPage`'s end-of-session review screen (6.2.6): "Install IAMS for faster access to audits next time." Dismissal is remembered (`localStorage`) and the prompt does not reappear for 30 days. No install prompt is shown in the Admin shell at all — see 11.2's rationale.

## 11.4 Offline Scan Queue Survival Across Browser Restart

This is the concrete mechanism behind the task's "never let a user believe an offline scan was lost":

1. Every scan/finding, the instant it's captured (6.2.2/6.2.3), is written to the Dexie/IndexedDB `iamsOfflineQueue` store **synchronously with the UI's optimistic success state** — the checkmark cue does not fire until the IndexedDB write has resolved, so "Devon saw the success cue" and "the data is durably on-device" are the same guarantee, not a race.
2. IndexedDB (unlike an in-memory store or `sessionStorage`) persists across a tab close, browser crash, or full device restart — this is exactly why it was chosen over any in-memory queue in Section 8.3.
3. On every app launch (cold start, whether from a fresh browser tab or the installed PWA), a queue-check runs before Continuous Scan Mode becomes interactive: if `iamsOfflineQueue` has any `syncStatus: 'pending'` records, `OfflineSyncIndicator` immediately shows the queued count (6.2.5) and a background sync attempt starts — so a scan made yesterday, before a phone died mid-audit, is still visibly "12 scans saved, will sync" the next time Devon opens the app, not silently gone.
4. Once a queued record syncs successfully and the server acknowledges it, it's marked `syncStatus: 'synced'` and pruned from the active queue view (retained briefly for the end-of-session review list, then cleared) — the queue only ever grows unbounded in the genuine extended-outage case, which is explicitly out of scope per NFR-AVAIL-05's "short, localized gaps, not extended disconnected operation"; if the queue exceeds a sanity threshold (e.g., 500 pending records or 24 hours pending), `OfflineSyncIndicator` escalates its messaging to recommend the user find connectivity rather than continuing indefinitely, since that scenario is outside the product's designed guarantee.

# 12. Error / Empty / Loading State Conventions

One consistent pattern per state type, applied identically across all 16 modules — a user should never have to relearn "what does loading look like" screen to screen.

## 12.1 Loading

- **Skeleton loaders**, not spinners, for anything rendering a known layout shape with unknown content — `IamsDataTable` rows, `AssetDetailPage`'s field layout, dashboard KPI cards. Skeletons match the real content's approximate dimensions so the layout doesn't jump on load.
- **Spinners** (MUI `CircularProgress`), reserved for two cases only: (a) a button's own in-flight state (replacing its label temporarily, button stays same size so nothing shifts), and (b) full-page loads where no layout shape is knowable yet (initial app boot before role/nav is resolved).
- **Progress bars** (determinate, MUI `LinearProgress`) for anything with a real percentage — bulk import processing, large export generation (`ExportProgressToast`), audit-scope generation for very large scopes — never a spinner standing in for a genuinely multi-second, trackable operation (NFR-PERF-03/04's async-with-progress-reporting requirement, surfaced in the UI, not just the backend).

## 12.2 Empty States

Every list/table screen has a purpose-specific empty state, not a generic "No data" — this is a small but real UX cost cut across dozens of screens if generic. Convention: an icon (from the same domain icon set, 2.5), a one-line explanation of *why* it's empty in this specific context, and, where the viewing role can act, a primary CTA. Examples: Asset Register with active filters and zero matches → "No assets match these filters" + "Clear filters" button (distinct from a truly-empty register, which instead reads "No assets registered yet" + "Register your first asset" for a role that can create one, or "No assets registered yet" with no CTA for a Viewer). The Notification Feed's empty state ("You're all caught up") is deliberately reassuring rather than neutral, since an empty notification list is a *good* outcome for Sam's persona.

## 12.3 Network-Error Retry Pattern

A failed GET (TanStack Query `isError`) renders an inline error panel in place of the content region: a short explanation, a "Try again" button (re-triggers the query), and — only for the Admin shell, since the Audit shell has its own offline story (Section 6.2.5/11) — a note if the failure looks connectivity-related ("Check your connection and try again") versus server-side ("Something went wrong on our end; if this continues, contact your administrator"), distinguished by HTTP status/network-error type so the message is accurate rather than generic. TanStack Query's automatic retry-with-backoff runs quietly in the background (3 attempts) before this panel even appears, so a single transient blip never surfaces as a visible error at all.

## 12.4 SoD-Blocked and Locked-Toggle Explanation Patterns

Already specified functionally in 3.2/6.1.3/7.2; visually, both `SoDBlockedNotice` and `LockedToggleWithExplanation` share one component skeleton (a muted-background inline panel, a small lock icon, explanation text at `color.text.disabledInformative`, 2.2) so a user learns the pattern once ("greyed-out-with-a-lock-icon-and-a-sentence" = "this is intentionally restricted, here's why") and recognizes it everywhere — in the Transfer/Audit approval flow, in Notification Preferences, and anywhere else a future module introduces a similar system-enforced restriction.

## 12.5 The 403 Case (Server Is Authoritative)

Since `RoleGate` (3.2) is a client-side convenience only (NFR-SEC-02), every Axios response interceptor checks for a `403` and routes it through one shared handler rather than letting each screen handle it ad hoc: if the 403 happens on initial page load/route entry, the user is redirected to `/forbidden` with a message naming what they tried to access and who to contact (their Administrator) — never a raw blank page or a console-only failure. If the 403 happens on an in-page action (e.g., a role was revoked mid-session and a stale-cached UI still showed an Approve button), the action fails with an inline toast ("Your access to this action has changed — refresh the page") rather than a silent no-op, and the relevant TanStack Query cache entries for that user's permission set are invalidated so the next render reflects reality.

# 13. Open Questions & Judgment Calls Log

Every row below is a place this spec resolved an ambiguity the BRD/FRS/SRS/PUC left open, using industry-standard React/MUI/accessibility judgment, so engineering can proceed without waiting on a follow-up decision. Each is flagged **[DECISION]** inline at its source location; this table is the consolidated index for anyone re-litigating one later.

| # | Decision | Section | Why It Was Open | Resolution |
|---|---|---|---|---|
| 1 | Two structurally distinct shells (`AdminShell` / `AuditShell`) rather than one responsive layout | 1.1 | SRS 7.1 mandates "two experience modes" but doesn't say whether that means two layouts or one adaptive one | Route-determined shell selection; Continuous Scan Mode's density/interaction model is incompatible with a reflowed data-table shell |
| 2 | Sam gets a 3-item nav, not the full module nav | 1.4 | FRS/PUC describe Sam's capabilities but not his IA | Minimal-surface portal matching his actual low-engagement usage pattern |
| 3 | Palette hex values and numeric contrast ratios | 2.2 | No visual design existed prior to this document | Hand-selected AA-exceeding pairs; flagged for mandatory automated re-verification before go-live, not treated as final art |
| 4 | Dark mode deferred to R3, no `prefers-color-scheme` auto-detection until then | 2.7 | Not mentioned in any source document | No requirement drives it; doubling the AA-audit surface for zero specified benefit is the wrong trade before go-live |
| 5 | Self-hosted Roboto + MUI icon SVGs, no external font/icon CDN | 2.5 | Not addressed by SRS's tech stack table | Direct consequence of BO-5/NFR-DEPLOY-02 (zero mandatory outbound calls) applied to an implementation detail the SRS didn't drill into |
| 6 | MUI default breakpoints adopted as-is | 2.6 | SRS 5.12 defines browser *versions*, not viewport breakpoints | They already map cleanly to the implied device set; no reason to diverge |
| 7 | TanStack Query + Zustand as the state-management pair | 8.1/8.2 | SRS fixes the stack down to Axios/React Router but is silent on state-management libraries | Standard, low-boilerplate fit for a CRUD-and-real-time-counter-heavy app; Redux's ceremony is unjustified here |
| 8 | Dexie.js/IndexedDB for the offline queue | 8.3/11.4 | FR-AUD-19/NFR-AVAIL-05 require offline persistence but don't name a mechanism | IndexedDB is the only browser-native storage that survives a restart at the needed capacity; Dexie avoids hand-rolling the raw API |
| 9 | React Hook Form + Zod | 9 | SRS/FRS don't specify a forms library | Performance fit for the app's large dynamic-field forms (FR-AST-06); schema validation pairs naturally with typed API contracts |
| 10 | react-i18next, deployment-locale wins over browser auto-detect | 10.1 | NFR-I18N-01 requires externalization but not a library or a detection precedence rule | Prevents a shared/kiosk audit device from silently language-switching based on the last user's browser |
| 11 | `textTransform: 'none'` on all MUI buttons | 2.3/10.2 | Not addressed anywhere in source docs | Material's default auto-uppercase is a known i18n defect; cheaper to decide before any translated strings exist |
| 12 | Service worker offline scope limited to the audit-scanning flow, not the whole Admin shell | 11.1 | NFR-AVAIL-05 scopes offline tolerance to "short, localized gaps," ambiguous on which screens that covers | Admin-shell screens (approvals, reports) carry real business risk if acted on against stale cached data; audit scanning is the one flow explicitly designed for it |
| 13 | PWA manifest `start_url` targets the mobile audit entry point; install prompt shown only to Auditors, after their first completed audit | 11.2/11.3 | BRD/SRS establish PWA packaging but not install-prompt targeting | Devon's persona is the only one with a repeated-use, field-mobile pattern that installability actually serves |
| 14 | Touch targets on the Audit shell set to 44×44px floor / 56×56px primary actions, exceeding the AA-minimum posture used elsewhere | 7.6 | NFR-ACC-03 requires one-handed optimization but not a numeric target | Adopted WCAG 2.1 AAA's SC 2.5.5 value as a deliberate floor for this one shell, given the walking/one-handed/possibly-gloved real-world context |
| 15 | Toast announcements are `aria-live="polite"`, not `"assertive"`, even for duplicate-scan warnings | 7.2 | FR-SCN-04 requires a "clear" warning but doesn't specify assertiveness level | The persistent counter and end-of-session queued-list are the durable record; interrupting a screen-reader user mid-scan-sequence for a non-blocking warning is a worse trade than a possibly-missed toast |

These are working defaults, not final art — several (especially #3, palette hex values, and #7/#8, the specific library choices) should get an explicit nod from whoever owns frontend engineering before sprint planning locks them in, but none of them block starting the build.
