import { useState } from 'react'
import Autocomplete from '@mui/material/Autocomplete'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { useDebouncedValue } from '../../../hooks/useDebouncedValue'
import { usePersonQuery, usePersonsQuery, useCreatePersonMutation } from '../../persons/hooks/usePersonsQuery'
import { useAssignAssetMutation, useUnassignAssetMutation } from '../hooks/useAssetAssignment'
import type { Asset } from '../types'
import type { Person, PersonType } from '../../persons/types'

// Person-only (FR-LIF-04): "or department" is not built here since department
// isn't a real entity yet (FR-ORG-03) - only what asset.assignedToPersonId
// already supports.
export function AssetAssignmentPanel({ asset }: { asset: Asset }) {
  const assignedPersonQuery = usePersonQuery(asset.assignedToPersonId)
  const unassign = useUnassignAssetMutation(asset.id)

  return (
    <Box>
      <Typography variant="subtitle1" sx={{ mb: 1 }}>
        Assigned To
      </Typography>

      {asset.assignedToPersonId ? (
        <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
          <Typography variant="body2" sx={{ flexGrow: 1 }}>
            {assignedPersonQuery.data ? assignedPersonQuery.data.fullName : 'Loading…'}
          </Typography>
          <Button
            size="small"
            color="inherit"
            disabled={unassign.isPending}
            onClick={() => unassign.mutate(asset.version)}
          >
            Unassign
          </Button>
        </Stack>
      ) : (
        <AssignPicker asset={asset} />
      )}
    </Box>
  )
}

function AssignPicker({ asset }: { asset: Asset }) {
  const assign = useAssignAssetMutation(asset.id)
  const createPerson = useCreatePersonMutation()

  const [searchInput, setSearchInput] = useState('')
  const debouncedSearch = useDebouncedValue(searchInput, 300)
  const candidatesQuery = usePersonsQuery(debouncedSearch || undefined)

  const [creating, setCreating] = useState(false)
  const [newName, setNewName] = useState('')
  const [newEmail, setNewEmail] = useState('')
  const [newType, setNewType] = useState<PersonType>('EMPLOYEE')

  async function handleCreateAndAssign() {
    if (!newName.trim()) return
    const person = await createPerson.mutateAsync({
      fullName: newName.trim(),
      email: newEmail.trim() || undefined,
      personType: newType,
    })
    assign.mutate({ personId: person.id, version: asset.version })
    setCreating(false)
    setNewName('')
    setNewEmail('')
  }

  return (
    <Stack spacing={1}>
      <Typography variant="body2" color="text.secondary">
        Unassigned
      </Typography>

      <Autocomplete
        size="small"
        options={candidatesQuery.data ?? []}
        loading={candidatesQuery.isFetching}
        getOptionLabel={(option: Person) => option.fullName}
        isOptionEqualToValue={(option, value) => option.id === value.id}
        inputValue={searchInput}
        onInputChange={(_, value) => setSearchInput(value)}
        onChange={(_, value) => {
          if (value) {
            assign.mutate({ personId: value.id, version: asset.version })
          }
        }}
        value={null}
        renderInput={(params) => <TextField {...params} label="Assign to person" placeholder="Search by name" />}
      />

      {!creating ? (
        <Button size="small" onClick={() => setCreating(true)} sx={{ alignSelf: 'flex-start' }}>
          + New person
        </Button>
      ) : (
        <Stack spacing={1} sx={{ p: 1.25, border: '1px solid', borderColor: 'divider', borderRadius: 1 }}>
          <TextField size="small" label="Full name" value={newName} onChange={(e) => setNewName(e.target.value)} autoFocus />
          <TextField size="small" label="Email (optional)" value={newEmail} onChange={(e) => setNewEmail(e.target.value)} />
          <TextField size="small" select label="Type" value={newType} onChange={(e) => setNewType(e.target.value as PersonType)}>
            <MenuItem value="EMPLOYEE">Employee</MenuItem>
            <MenuItem value="VOLUNTEER">Volunteer</MenuItem>
          </TextField>
          <Stack direction="row" spacing={1}>
            <Button
              size="small"
              variant="contained"
              disabled={!newName.trim() || createPerson.isPending}
              onClick={handleCreateAndAssign}
            >
              Create &amp; assign
            </Button>
            <Button size="small" color="inherit" onClick={() => setCreating(false)}>
              Cancel
            </Button>
          </Stack>
        </Stack>
      )}
    </Stack>
  )
}
