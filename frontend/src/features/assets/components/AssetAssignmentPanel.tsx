import { useState } from 'react'
import Autocomplete from '@mui/material/Autocomplete'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import ToggleButton from '@mui/material/ToggleButton'
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { useDebouncedValue } from '../../../hooks/useDebouncedValue'
import { usePersonQuery, usePersonsQuery, useCreatePersonMutation } from '../../persons/hooks/usePersonsQuery'
import {
  useAssignAssetMutation,
  useAssignAssetToDepartmentMutation,
  useDepartmentsQuery,
  useUnassignAssetMutation,
} from '../hooks/useAssetAssignment'
import type { Asset } from '../types'
import type { Person, PersonType } from '../../persons/types'
import type { Department } from '../../../api/org/departmentApi'

// US-LIF-04: custodian is a Person OR a Department. Assigning either kind
// closes any prior assignment (of either kind); the backend keeps them
// mutually exclusive, so the panel only ever shows one current custodian.
export function AssetAssignmentPanel({ asset }: { asset: Asset }) {
  const assignedPersonQuery = usePersonQuery(asset.assignedToPersonId)
  const departmentsForLabel = useDepartmentsQuery(asset.assignedToDepartmentId !== null)
  const unassign = useUnassignAssetMutation(asset.id)

  const departmentName = asset.assignedToDepartmentId
    ? departmentsForLabel.data?.find((d) => d.id === asset.assignedToDepartmentId)
    : undefined

  const currentCustodian = asset.assignedToPersonId
    ? { label: assignedPersonQuery.data ? assignedPersonQuery.data.fullName : 'Loading…', kind: 'Person' }
    : asset.assignedToDepartmentId
      ? {
          label: departmentName ? `${departmentName.name} (${departmentName.costCenterCode})` : 'Loading…',
          kind: 'Department',
        }
      : null

  return (
    <Box>
      <Typography variant="subtitle1" sx={{ mb: 1 }}>
        Assigned To
      </Typography>

      {currentCustodian ? (
        <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
          <Typography variant="body2" sx={{ flexGrow: 1 }}>
            {currentCustodian.label}
            <Typography component="span" variant="caption" color="text.secondary" sx={{ ml: 1 }}>
              {currentCustodian.kind}
            </Typography>
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
  const [kind, setKind] = useState<'person' | 'department'>('person')
  return (
    <Stack spacing={1.5}>
      <Typography variant="body2" color="text.secondary">
        Unassigned
      </Typography>
      <ToggleButtonGroup
        size="small"
        exclusive
        value={kind}
        onChange={(_, next) => next && setKind(next)}
        aria-label="Custodian kind"
      >
        <ToggleButton value="person">Person</ToggleButton>
        <ToggleButton value="department">Department</ToggleButton>
      </ToggleButtonGroup>
      {kind === 'person' ? <AssignPersonPicker asset={asset} /> : <AssignDepartmentPicker asset={asset} />}
    </Stack>
  )
}

function AssignPersonPicker({ asset }: { asset: Asset }) {
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

function AssignDepartmentPicker({ asset }: { asset: Asset }) {
  const assign = useAssignAssetToDepartmentMutation(asset.id)
  const departmentsQuery = useDepartmentsQuery(true)
  const options = (departmentsQuery.data ?? []).filter((d) => d.active)

  return (
    <Autocomplete
      size="small"
      options={options}
      loading={departmentsQuery.isFetching}
      getOptionLabel={(option: Department) => `${option.name} (${option.costCenterCode})`}
      isOptionEqualToValue={(option, value) => option.id === value.id}
      onChange={(_, value) => {
        if (value) {
          assign.mutate({ departmentId: value.id, version: asset.version })
        }
      }}
      value={null}
      renderInput={(params) => (
        <TextField {...params} label="Assign to department" placeholder="Search by name or cost center" />
      )}
    />
  )
}
