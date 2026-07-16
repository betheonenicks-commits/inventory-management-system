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
import { PurchaseRequestListPage } from '../features/procurement/PurchaseRequestListPage'
import { PurchaseOrderListPage } from '../features/procurement/PurchaseOrderListPage'
import { PurchaseOrderDetailPage } from '../features/procurement/PurchaseOrderDetailPage'
import { DashboardPage } from '../features/dashboard/DashboardPage'
import { ReportsPage } from '../features/reports/ReportsPage'
import { SearchPage } from '../features/search/SearchPage'
import { InventoryItemListPage } from '../features/inventory/InventoryItemListPage'
import { InventoryItemDetailPage } from '../features/inventory/InventoryItemDetailPage'
import { WarehouseListPage } from '../features/inventory/WarehouseListPage'
import { VendorListPage } from '../features/inventory/VendorListPage'
import { ManualAdjustmentListPage } from '../features/inventory/ManualAdjustmentListPage'
import { NotificationPreferencesPage } from '../features/notifications/NotificationPreferencesPage'
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
          { path: 'dashboard', element: <DashboardPage /> },
          { path: 'reports', element: <ReportsPage /> },
          { path: 'search', element: <SearchPage /> },
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
          { path: 'procurement/purchase-requests', element: <PurchaseRequestListPage /> },
          { path: 'procurement/purchase-orders', element: <PurchaseOrderListPage /> },
          { path: 'procurement/purchase-orders/:orderId', element: <PurchaseOrderDetailPage /> },
          { path: 'inventory/items', element: <InventoryItemListPage /> },
          { path: 'inventory/items/:itemId', element: <InventoryItemDetailPage /> },
          { path: 'inventory/warehouses', element: <WarehouseListPage /> },
          { path: 'inventory/vendors', element: <VendorListPage /> },
          { path: 'inventory/adjustments', element: <ManualAdjustmentListPage /> },
          { path: 'settings/notifications', element: <NotificationPreferencesPage /> },
        ],
      },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
])
