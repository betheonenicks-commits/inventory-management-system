import { useState } from 'react'
import type { ReactNode } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import AppBar from '@mui/material/AppBar'
import Toolbar from '@mui/material/Toolbar'
import Typography from '@mui/material/Typography'
import Drawer from '@mui/material/Drawer'
import List from '@mui/material/List'
import ListItemButton from '@mui/material/ListItemButton'
import ListItemIcon from '@mui/material/ListItemIcon'
import ListItemText from '@mui/material/ListItemText'
import Box from '@mui/material/Box'
import IconButton from '@mui/material/IconButton'
import Menu from '@mui/material/Menu'
import MenuItem from '@mui/material/MenuItem'
import AssessmentIcon from '@mui/icons-material/Assessment'
import SearchIcon from '@mui/icons-material/Search'
import DashboardIcon from '@mui/icons-material/Dashboard'
import Inventory2Icon from '@mui/icons-material/Inventory2'
import CategoryIcon from '@mui/icons-material/Category'
import AccountCircleIcon from '@mui/icons-material/AccountCircle'
import FeedbackOutlinedIcon from '@mui/icons-material/FeedbackOutlined'
import Tooltip from '@mui/material/Tooltip'
import { NotificationBell } from '../features/notifications/NotificationBell'
import { FeedbackDialog } from '../features/feedback/FeedbackDialog'
import PeopleIcon from '@mui/icons-material/People'
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings'
import FactCheckIcon from '@mui/icons-material/FactCheck'
import GavelIcon from '@mui/icons-material/Gavel'
import RequestQuoteIcon from '@mui/icons-material/RequestQuote'
import LocalShippingIcon from '@mui/icons-material/LocalShipping'
import WarehouseIcon from '@mui/icons-material/Warehouse'
import StoreIcon from '@mui/icons-material/Store'
import RuleIcon from '@mui/icons-material/Rule'
import InventoryIcon from '@mui/icons-material/Inventory'
import EventBusyIcon from '@mui/icons-material/EventBusy'
import AccountTreeIcon from '@mui/icons-material/AccountTree'
import BusinessIcon from '@mui/icons-material/Business'
import SecurityIcon from '@mui/icons-material/Security'
import VpnKeyIcon from '@mui/icons-material/VpnKey'
import { useAuthStore, hasPermission } from '../auth/authStore'
import { logout as logoutApi } from '../api/authApi'

const DRAWER_WIDTH = 240

interface NavItem {
  label: string
  to: string
  icon: ReactNode
  // Omit to show to every authenticated user; UI hiding is a convenience, not the
  // control - the backend's own @PreAuthorize/@perm.has checks are what actually
  // enforce this (US-USR-03), so this list only needs to roughly match, not be
  // airtight. Permission-based (not role-name-based) so a custom role (US-USR-02)
  // that happens to carry these permissions sees the right nav automatically.
  requiresPermission?: string
  // For a page whose backend endpoint accepts more than one permission (an
  // OR-composed @PreAuthorize, e.g. inventory:read OR approvals:read for a
  // Department Head reviewing what's routed to them) - any one match shows the item.
  requiresAnyPermission?: string[]
}

const NAV_ITEMS: NavItem[] = [
  // No permission gate: the search endpoint is open to any authenticated user
  // (same precedent as GET /assets); groups are permission-filtered server-side.
  { label: 'Search', to: '/search', icon: <SearchIcon /> },
  { label: 'Dashboard', to: '/dashboard', icon: <DashboardIcon />, requiresPermission: 'dashboards:read' },
  { label: 'Reports', to: '/reports', icon: <AssessmentIcon />, requiresPermission: 'reports:read' },
  { label: 'Assets', to: '/assets', icon: <Inventory2Icon /> },
  { label: 'Categories', to: '/assets/categories', icon: <CategoryIcon /> },
  { label: 'Audits', to: '/audits', icon: <FactCheckIcon />, requiresPermission: 'audits:read' },
  {
    label: 'Purchase Requests',
    to: '/procurement/purchase-requests',
    icon: <RequestQuoteIcon />,
    requiresPermission: 'assets:read',
  },
  { label: 'Purchase Orders', to: '/procurement/purchase-orders', icon: <LocalShippingIcon />, requiresPermission: 'assets:read' },
  { label: 'Inventory Items', to: '/inventory/items', icon: <InventoryIcon />, requiresPermission: 'inventory:read' },
  {
    label: 'Expiring Stock',
    to: '/inventory/expiring-stock',
    icon: <EventBusyIcon />,
    requiresPermission: 'inventory:read',
  },
  { label: 'Warehouses', to: '/inventory/warehouses', icon: <WarehouseIcon />, requiresPermission: 'inventory:read' },
  { label: 'Vendors', to: '/inventory/vendors', icon: <StoreIcon />, requiresPermission: 'inventory:read' },
  {
    label: 'Adjustments',
    to: '/inventory/adjustments',
    icon: <RuleIcon />,
    // Matches ManualAdjustmentController's own OR-composed @PreAuthorize - a Department
    // Head (approvals:read/write, no inventory:read) still needs to see this to review and
    // decide on requests routed to them, not just an Inventory Manager.
    requiresAnyPermission: ['inventory:read', 'approvals:read'],
  },
  { label: 'Organization', to: '/organization', icon: <AccountTreeIcon />, requiresPermission: 'org:write' },
  { label: 'Departments', to: '/organization/departments', icon: <BusinessIcon />, requiresPermission: 'org:write' },
  { label: 'Users', to: '/users', icon: <PeopleIcon />, requiresPermission: 'users:read' },
  { label: 'Roles', to: '/roles', icon: <AdminPanelSettingsIcon />, requiresPermission: 'roles:read' },
  { label: 'Compliance', to: '/compliance', icon: <GavelIcon />, requiresPermission: 'compliance:read' },
  { label: 'Security Log', to: '/security-events', icon: <SecurityIcon />, requiresPermission: 'security:read' },
  {
    label: 'Password Policy',
    to: '/settings/password-policy',
    icon: <VpnKeyIcon />,
    requiresPermission: 'security:write',
  },
]

export function AdminShell() {
  const user = useAuthStore((s) => s.user)
  const refreshToken = useAuthStore((s) => s.refreshToken)
  const clearSession = useAuthStore((s) => s.clearSession)
  const navigate = useNavigate()
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null)
  const [feedbackOpen, setFeedbackOpen] = useState(false)

  const visibleNavItems = NAV_ITEMS.filter((item) => {
    if (item.requiresAnyPermission) return item.requiresAnyPermission.some((p) => hasPermission(user, p))
    return !item.requiresPermission || hasPermission(user, item.requiresPermission)
  })

  function handleLogout() {
    setAnchorEl(null)
    // Best-effort: the user is signed out locally regardless of whether this
    // network call succeeds - it just also ends the session server-side
    // (US-SEC-01) rather than leaving the refresh token live and unused.
    if (refreshToken) {
      logoutApi(refreshToken).catch(() => undefined)
    }
    clearSession()
    navigate('/login', { replace: true })
  }

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <AppBar position="fixed" sx={{ zIndex: (t) => t.zIndex.drawer + 1 }}>
        <Toolbar>
          <Typography variant="h6" noWrap sx={{ flexGrow: 1 }}>
            IAMS - Inventory Audit Management System
          </Typography>
          {/* US-ANL-04: feedback from anywhere in the app - every role sees this. */}
          <Tooltip title="Send feedback">
            <IconButton color="inherit" aria-label="Send feedback" onClick={() => setFeedbackOpen(true)}>
              <FeedbackOutlinedIcon />
            </IconButton>
          </Tooltip>
          <NotificationBell />
          <IconButton color="inherit" onClick={(e) => setAnchorEl(e.currentTarget)}>
            <AccountCircleIcon />
          </IconButton>
          <Menu anchorEl={anchorEl} open={!!anchorEl} onClose={() => setAnchorEl(null)}>
            <MenuItem disabled>{user?.username}</MenuItem>
            {hasPermission(user, 'approvals:write') && (
              <MenuItem
                onClick={() => {
                  setAnchorEl(null)
                  navigate('/settings/approval-delegations')
                }}
              >
                Approval delegations
              </MenuItem>
            )}
            <MenuItem onClick={handleLogout}>Sign out</MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>

      <Drawer
        variant="permanent"
        sx={{
          width: DRAWER_WIDTH,
          flexShrink: 0,
          [`& .MuiDrawer-paper`]: { width: DRAWER_WIDTH, boxSizing: 'border-box' },
        }}
      >
        <Toolbar />
        <List>
          {visibleNavItems.map((item) => (
            <ListItemButton
              key={item.to}
              component={NavLink}
              to={item.to}
              end={item.to === '/assets'}
              sx={{ '&.active': { bgcolor: 'action.selected' } }}
            >
              <ListItemIcon>{item.icon}</ListItemIcon>
              <ListItemText primary={item.label} />
            </ListItemButton>
          ))}
        </List>
      </Drawer>

      <Box component="main" sx={{ flexGrow: 1, p: 3 }}>
        <Toolbar />
        <Outlet />
      </Box>

      <FeedbackDialog open={feedbackOpen} onClose={() => setFeedbackOpen(false)} />
    </Box>
  )
}
