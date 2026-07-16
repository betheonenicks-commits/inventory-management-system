import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import Badge from '@mui/material/Badge'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import IconButton from '@mui/material/IconButton'
import List from '@mui/material/List'
import ListItemButton from '@mui/material/ListItemButton'
import ListItemText from '@mui/material/ListItemText'
import Popover from '@mui/material/Popover'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import NotificationsIcon from '@mui/icons-material/Notifications'
import {
  fetchNotifications,
  fetchUnreadCount,
  markAllNotificationsRead,
  markNotificationRead,
} from '../../api/notifications/notificationApi'
import type { AppNotification } from '../../api/notifications/notificationApi'

/**
 * US-NTF-03: the in-app channel's surface. Unread badge polls every 30s;
 * clicking a notification marks it read and, when it carries a resource
 * path, deep-links to it (US-NTF-10's in-app leg).
 */
export function NotificationBell() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null)

  const unreadQuery = useQuery({
    queryKey: ['NTF', 'unread-count'],
    queryFn: fetchUnreadCount,
    refetchInterval: 30_000,
  })
  const listQuery = useQuery({
    queryKey: ['NTF', 'recent'],
    queryFn: () => fetchNotifications(false, 0, 10),
    enabled: !!anchorEl,
  })
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['NTF'] })
  const markRead = useMutation({ mutationFn: markNotificationRead, onSuccess: invalidate })
  const markAll = useMutation({ mutationFn: markAllNotificationsRead, onSuccess: invalidate })

  function handleClick(notification: AppNotification) {
    if (!notification.readAt) {
      markRead.mutate(notification.id)
    }
    setAnchorEl(null)
    if (notification.resourcePath && notification.resourcePath.startsWith('/')) {
      navigate(notification.resourcePath)
    }
  }

  const items = listQuery.data?.content ?? []

  return (
    <>
      <IconButton color="inherit" aria-label="Notifications" onClick={(e) => setAnchorEl(e.currentTarget)}>
        <Badge badgeContent={unreadQuery.data ?? 0} color="error">
          <NotificationsIcon />
        </Badge>
      </IconButton>
      <Popover
        open={!!anchorEl}
        anchorEl={anchorEl}
        onClose={() => setAnchorEl(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <Box sx={{ width: 380, maxHeight: 480, overflowY: 'auto' }}>
          <Stack direction="row" sx={{ px: 2, pt: 1.5, pb: 0.5, alignItems: 'center' }}>
            <Typography variant="subtitle1" sx={{ flexGrow: 1 }}>
              Notifications
            </Typography>
            <Button size="small" onClick={() => markAll.mutate()} disabled={(unreadQuery.data ?? 0) === 0}>
              Mark all read
            </Button>
          </Stack>
          {items.length === 0 ? (
            <Typography variant="body2" color="text.secondary" sx={{ px: 2, py: 3 }}>
              Nothing yet - you're all caught up.
            </Typography>
          ) : (
            <List dense disablePadding>
              {items.map((n) => (
                <ListItemButton key={n.id} onClick={() => handleClick(n)} divider
                  sx={{ bgcolor: n.readAt ? undefined : 'action.hover' }}>
                  <ListItemText
                    primary={
                      <Typography variant="body2" sx={{ fontWeight: n.readAt ? 400 : 600 }}>
                        {n.title}
                      </Typography>
                    }
                    secondary={
                      <>
                        <Typography component="span" variant="caption" sx={{ display: 'block' }}>
                          {n.body}
                        </Typography>
                        <Typography component="span" variant="caption" color="text.secondary">
                          {new Date(n.createdAt).toLocaleString()}
                        </Typography>
                      </>
                    }
                  />
                </ListItemButton>
              ))}
            </List>
          )}
          <Box sx={{ px: 2, py: 1, borderTop: 1, borderColor: 'divider' }}>
            <Button size="small" onClick={() => { setAnchorEl(null); navigate('/settings/notifications') }}>
              Notification settings
            </Button>
          </Box>
        </Box>
      </Popover>
    </>
  )
}
