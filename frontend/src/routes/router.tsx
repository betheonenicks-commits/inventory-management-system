import { createBrowserRouter, Navigate } from 'react-router-dom'
import { RequireAuth } from '../auth/RequireAuth'
import { LoginPage } from '../auth/LoginPage'
import { AdminShell } from '../shells/AdminShell'
import { AssetListPage } from '../features/assets/AssetListPage'
import { AssetRegisterWizardPage } from '../features/assets/AssetRegisterWizardPage'
import { AssetDetailPage } from '../features/assets/AssetDetailPage'
import { AssetEditPage } from '../features/assets/AssetEditPage'
import { CategoryConfigPage } from '../features/assets/CategoryConfigPage'
import { UserListPage } from '../features/users/UserListPage'
import { RoleListPage } from '../features/roles/RoleListPage'
import { AuditListPage } from '../features/audits/AuditListPage'
import { AuditDetailPage } from '../features/audits/AuditDetailPage'
import { CompliancePage } from '../features/compliance/CompliancePage'
import { NotFoundPage } from '../components/common/NotFoundPage'

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  {
    element: <RequireAuth />,
    children: [
      {
        element: <AdminShell />,
        children: [
          { index: true, element: <Navigate to="/assets" replace /> },
          { path: 'assets', element: <AssetListPage /> },
          { path: 'assets/new', element: <AssetRegisterWizardPage /> },
          { path: 'assets/categories', element: <CategoryConfigPage /> },
          { path: 'assets/:assetId', element: <AssetDetailPage /> },
          { path: 'assets/:assetId/edit', element: <AssetEditPage /> },
          { path: 'users', element: <UserListPage /> },
          { path: 'roles', element: <RoleListPage /> },
          { path: 'audits', element: <AuditListPage /> },
          { path: 'audits/:auditId', element: <AuditDetailPage /> },
          { path: 'compliance', element: <CompliancePage /> },
        ],
      },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
])
