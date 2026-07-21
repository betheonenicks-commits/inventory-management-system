import { useMemo, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import IconButton from '@mui/material/IconButton'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import MenuItem from '@mui/material/MenuItem'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import EditIcon from '@mui/icons-material/Edit'
import { PageHeader } from '../../components/common/PageHeader'
import { ErrorPanel } from '../../components/common/ErrorPanel'
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton'
import { useAuthStore, hasPermission } from '../../auth/authStore'
import { isApiProblem } from '../../api/errors'
import {
  useCreateOrgNodeMutation,
  useDeleteOrgNodeMutation,
  useOrgLevelsQuery,
  useOrgNodesQuery,
  useRenameOrgLevelMutation,
} from './hooks/useOrgHierarchyQuery'
import type { OrgLevel, OrgNode } from '../../api/org/orgNodeApi'

/**
 * US-ORG-01 (build the multi-level hierarchy), US-ORG-02 (relabel level
 * names), US-ORG-06 (tag a Room node with a configured variant, e.g.
 * Classroom/Laboratory - the two variant options themselves are seeded
 * per-level data (V19__create_org_hierarchy.sql), not user-defined text, so
 * "configuring" here means picking one at node-creation time, matching the
 * story's own concrete example and its "left unconfigured, Room behaves
 * exactly as the base level" second clause - not a from-scratch taxonomy
 * editor the AC never actually asks for). The backend had all three built
 * with no admin screen at all before this.
 */
export function OrgHierarchyPage() {
  const canWrite = hasPermission(useAuthStore((s) => s.user), 'org:write')
  const nodesQuery = useOrgNodesQuery()
  const levelsQuery = useOrgLevelsQuery()
  const createNode = useCreateOrgNodeMutation()
  const deleteNode = useDeleteOrgNodeMutation()
  const renameLevel = useRenameOrgLevelMutation()

  const [editingLevelId, setEditingLevelId] = useState<string | null>(null)
  const [levelNameDraft, setLevelNameDraft] = useState('')
  const [levelError, setLevelError] = useState<string | null>(null)

  const [createParent, setCreateParent] = useState<OrgNode | 'ROOT' | null>(null)
  const [nodeName, setNodeName] = useState('')
  const [nodeCode, setNodeCode] = useState('')
  const [roomVariant, setRoomVariant] = useState('')
  const [createError, setCreateError] = useState<string | null>(null)
  const [deleteBlockedMessage, setDeleteBlockedMessage] = useState<string | null>(null)

  const levels = levelsQuery.data ?? []
  const levelById = useMemo(() => new Map((levelsQuery.data ?? []).map((l) => [l.id, l])), [levelsQuery.data])

  const childrenByParent = useMemo(() => {
    const map = new Map<string, OrgNode[]>()
    for (const node of nodesQuery.data ?? []) {
      const key = node.parentId ?? 'ROOT'
      const list = map.get(key) ?? []
      list.push(node)
      map.set(key, list)
    }
    return map
  }, [nodesQuery.data])

  const rootNodes = childrenByParent.get('ROOT') ?? []

  // The level a new child must be at, given the parent's level rank (backend requires exactly +1).
  const targetLevel: OrgLevel | undefined =
    createParent === 'ROOT'
      ? levels.find((l) => l.rank === 0)
      : createParent
        ? levels.find((l) => l.rank === (levelById.get(createParent.levelId)?.rank ?? -2) + 1)
        : undefined

  function openCreateDialog(parent: OrgNode | 'ROOT') {
    setCreateParent(parent)
    setNodeName('')
    setNodeCode('')
    setRoomVariant('')
    setCreateError(null)
  }

  function startRenameLevel(level: OrgLevel) {
    setEditingLevelId(level.id)
    setLevelNameDraft(level.name)
    setLevelError(null)
  }

  async function handleRenameLevel(level: OrgLevel) {
    if (!levelNameDraft.trim()) return
    setLevelError(null)
    try {
      await renameLevel.mutateAsync({ id: level.id, name: levelNameDraft.trim(), version: level.version })
      setEditingLevelId(null)
    } catch (err) {
      setLevelError(isApiProblem(err) ? err.detail : 'Failed to rename level')
    }
  }

  async function handleCreateNode() {
    if (!targetLevel) return
    setCreateError(null)
    try {
      await createNode.mutateAsync({
        name: nodeName,
        code: nodeCode,
        parentId: createParent === 'ROOT' || !createParent ? undefined : createParent.id,
        levelId: targetLevel.id,
        roomVariant: roomVariant || undefined,
      })
      setCreateParent(null)
    } catch (err) {
      setCreateError(isApiProblem(err) ? err.detail : 'Failed to create org node')
    }
  }

  async function handleDelete(node: OrgNode) {
    setDeleteBlockedMessage(null)
    try {
      await deleteNode.mutateAsync(node.id)
    } catch (err) {
      if (isApiProblem(err) && err.status === 409) {
        setDeleteBlockedMessage(err.detail)
      }
    }
  }

  function renderNode(node: OrgNode, depth: number) {
    const children = childrenByParent.get(node.id) ?? []
    return (
      <Box key={node.id}>
        <ListItem
          divider
          sx={{ pl: depth * 3 }}
          secondaryAction={
            canWrite && (
              <Stack direction="row" spacing={0.5}>
                <IconButton size="small" onClick={() => openCreateDialog(node)} title="Add child node">
                  <AddIcon fontSize="small" />
                </IconButton>
                <IconButton size="small" onClick={() => handleDelete(node)} title="Delete this node">
                  <DeleteIcon fontSize="small" />
                </IconButton>
              </Stack>
            )
          }
        >
          <ListItemText
            primary={
              <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                <Typography variant="body2">{node.name}</Typography>
                <Chip size="small" variant="outlined" label={node.code} />
                <Chip size="small" label={node.levelName} />
                {node.roomVariant && <Chip size="small" color="info" label={node.roomVariant} />}
              </Stack>
            }
          />
        </ListItem>
        {children.map((child) => renderNode(child, depth + 1))}
      </Box>
    )
  }

  return (
    <Box>
      <PageHeader
        title="Organization Hierarchy"
        actions={
          canWrite && (
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => openCreateDialog('ROOT')}>
              New Root Node
            </Button>
          )
        }
      />

      <Typography variant="subtitle1" sx={{ mb: 1 }}>
        Hierarchy Levels
      </Typography>
      {levelsQuery.isLoading && <LoadingSkeleton rows={3} />}
      {levelsQuery.isError && <ErrorPanel error={levelsQuery.error} onRetry={() => levelsQuery.refetch()} />}
      {levelsQuery.isSuccess && (
        <Paper variant="outlined" sx={{ mb: 3 }}>
          <List dense>
            {levelError && (
              <Alert severity="error" sx={{ m: 1 }} onClose={() => setLevelError(null)}>
                {levelError}
              </Alert>
            )}
            {[...levels]
              .sort((a, b) => a.rank - b.rank)
              .map((level) => (
                <ListItem
                  key={level.id}
                  divider
                  secondaryAction={
                    canWrite &&
                    editingLevelId !== level.id && (
                      <IconButton size="small" onClick={() => startRenameLevel(level)} title="Rename this level">
                        <EditIcon fontSize="small" />
                      </IconButton>
                    )
                  }
                >
                  {editingLevelId === level.id ? (
                    <Stack direction="row" spacing={1} sx={{ alignItems: 'center', width: '100%' }}>
                      <TextField
                        size="small"
                        value={levelNameDraft}
                        onChange={(e) => setLevelNameDraft(e.target.value)}
                        autoFocus
                      />
                      <Button size="small" variant="contained" onClick={() => handleRenameLevel(level)} disabled={renameLevel.isPending}>
                        Save
                      </Button>
                      <Button size="small" onClick={() => setEditingLevelId(null)}>
                        Cancel
                      </Button>
                    </Stack>
                  ) : (
                    <ListItemText
                      primary={
                        <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                          <Typography variant="body2">
                            Rank {level.rank}: {level.name}
                          </Typography>
                          <Chip size="small" variant="outlined" label={level.code} />
                        </Stack>
                      }
                      secondary={
                        level.roomVariants.length > 0
                          ? `Configured variants: ${level.roomVariants.join(', ')}`
                          : undefined
                      }
                    />
                  )}
                </ListItem>
              ))}
          </List>
        </Paper>
      )}

      <Typography variant="subtitle1" sx={{ mb: 1 }}>
        Organization Tree
      </Typography>
      {deleteBlockedMessage && (
        <Alert severity="warning" sx={{ mb: 2 }} onClose={() => setDeleteBlockedMessage(null)}>
          {deleteBlockedMessage}
        </Alert>
      )}
      {nodesQuery.isLoading && <LoadingSkeleton rows={6} />}
      {nodesQuery.isError && <ErrorPanel error={nodesQuery.error} onRetry={() => nodesQuery.refetch()} />}
      {nodesQuery.isSuccess && (
        <Paper variant="outlined">
          {rootNodes.length === 0 ? (
            <Box sx={{ p: 3 }}>
              <Typography color="text.secondary">No org nodes yet. Start by creating a root node.</Typography>
            </Box>
          ) : (
            <List dense disablePadding>
              {rootNodes.map((node) => renderNode(node, 0))}
            </List>
          )}
        </Paper>
      )}

      <Dialog open={createParent !== null} onClose={() => setCreateParent(null)} maxWidth="sm" fullWidth>
        <DialogTitle>{createParent === 'ROOT' ? 'New Root Node' : `New child of ${(createParent as OrgNode | null)?.name ?? ''}`}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {createError && <Alert severity="error">{createError}</Alert>}
            {!targetLevel ? (
              <Alert severity="warning">
                No hierarchy level is configured for this position (the next rank down doesn't exist yet).
              </Alert>
            ) : (
              <Typography variant="body2" color="text.secondary">
                Level: <strong>{targetLevel.name}</strong>
              </Typography>
            )}
            <TextField label="Name" fullWidth required value={nodeName} onChange={(e) => setNodeName(e.target.value)} />
            <TextField label="Code" fullWidth required value={nodeCode} onChange={(e) => setNodeCode(e.target.value)} />
            {targetLevel && targetLevel.roomVariants.length > 0 && (
              <TextField
                select
                label="Room variant (optional)"
                fullWidth
                value={roomVariant}
                onChange={(e) => setRoomVariant(e.target.value)}
              >
                <MenuItem value="">None</MenuItem>
                {targetLevel.roomVariants.map((v) => (
                  <MenuItem key={v} value={v}>
                    {v}
                  </MenuItem>
                ))}
              </TextField>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateParent(null)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleCreateNode}
            disabled={!nodeName.trim() || !nodeCode.trim() || !targetLevel || createNode.isPending}
          >
            Create
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
