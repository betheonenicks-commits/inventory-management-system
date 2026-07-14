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
import Inventory2Icon from '@mui/icons-material/Inventory2'
import CategoryIcon from '@mui/icons-material/Category'
import AccountCircleIcon from '@mui/icons-material/AccountCircle'
import PeopleIcon from '@mui/icons-material/People'
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings'
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
}

const NAV_ITEMS: NavItem[] = [
  { label: 'Assets', to: '/assets', icon: <Inventory2Icon /> },
  { label: 'Categories', to: '/assets/categories', icon: <CategoryIcon /> },
  { label: 'Users', to: '/users', icon: <PeopleIcon />, requiresPermission: 'users:read' },
  { label: 'Roles', to: '/roles', icon: <AdminPanelSettingsIcon />, requiresPermission: 'roles:read' },
]

export function AdminShell() {
  const user = useAuthStore((s) => s.user)
  const refreshToken = useAuthStore((s) => s.refreshToken)
  const clearSession = useAuthStore((s) => s.clearSession)
  const navigate = useNavigate()
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null)

  const visibleNavItems = NAV_ITEMS.filter(
    (item) => !item.requiresPermission || hasPermission(user, item.requiresPermission),
  )

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
          <IconButton color="inherit" onClick={(e) => setAnchorEl(e.currentTarget)}>
            <AccountCircleIcon />
          </IconButton>
          <Menu anchorEl={anchorEl} open={!!anchorEl} onClose={() => setAnchorEl(null)}>
            <MenuItem disabled>{user?.username}</MenuItem>
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
    </Box>
  )
}
