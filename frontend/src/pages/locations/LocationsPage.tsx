import { useEffect, useState } from 'react';
import {
  Box, Button, Card, Chip, Dialog, DialogActions, DialogContent, DialogTitle,
  Grid, IconButton, MenuItem, Table, TableBody, TableCell, TableHead, TableRow,
  TextField, Tooltip,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import BlockIcon from '@mui/icons-material/Block';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Controller, useForm } from 'react-hook-form';
import { useSnackbar } from 'notistack';
import { api, errorMessage } from '../../api/client';
import type { Location } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import { useDepartments, useFaculties, useLocations } from '../../api/referenceData';
import PageHeader from '../../components/common/PageHeader';
import CodeTag from '../../components/common/CodeTag';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import EmptyState from '../../components/common/EmptyState';
import { titleCase } from '../../utils/format';

const TYPES = ['CAMPUS', 'BUILDING', 'FLOOR', 'ROOM', 'LECTURE_ROOM', 'AUDITORIUM', 'LABORATORY', 'STORAGE_AREA'];

interface FormValues {
  code: string;
  name: string;
  type: string;
  parentId: string;
  facultyId: string;
  departmentId: string;
  address: string;
  capacity: string;
  description: string;
}

function LocationDialog({ open, onClose, existing }: {
  open: boolean; onClose: () => void; existing: Location | null;
}) {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();
  const locations = useLocations();
  const faculties = useFaculties();
  const { register, control, handleSubmit, reset, watch, formState: { errors } } = useForm<FormValues>({
    defaultValues: {
      code: '', name: '', type: 'ROOM', parentId: '', facultyId: '', departmentId: '',
      address: '', capacity: '', description: '',
    },
  });
  const facultyId = watch('facultyId');
  const departments = useDepartments(facultyId || undefined);

  useEffect(() => {
    if (open) {
      reset(existing ? {
        code: existing.code, name: existing.name, type: existing.type,
        parentId: existing.parentId ?? '', facultyId: existing.facultyId ?? '',
        departmentId: existing.departmentId ?? '', address: existing.address ?? '',
        capacity: existing.capacity?.toString() ?? '', description: existing.description ?? '',
      } : undefined);
    }
  }, [open, existing, reset]);

  const mutation = useMutation({
    mutationFn: async (values: FormValues) => {
      const payload = {
        code: values.code,
        name: values.name,
        type: values.type,
        parentId: values.parentId || null,
        facultyId: values.facultyId || null,
        departmentId: values.departmentId || null,
        address: values.address || null,
        capacity: values.capacity ? Number(values.capacity) : null,
        description: values.description || null,
      };
      return existing
        ? (await api.put(`/locations/${existing.id}`, payload)).data
        : (await api.post('/locations', payload)).data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['locations'] });
      enqueueSnackbar(existing ? 'Location updated' : 'Location created', { variant: 'success' });
      onClose();
    },
    onError: (error) => enqueueSnackbar(errorMessage(error), { variant: 'error' }),
  });

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{existing ? `Edit ${existing.code}` : 'Add location'}</DialogTitle>
      <DialogContent>
        <Grid container spacing={2} sx={{ mt: 0.25 }}>
          <Grid size={{ xs: 6 }}>
            <TextField label="Location name" fullWidth required autoFocus
                       error={!!errors.name} helperText={errors.name?.message}
                       {...register('name', { required: 'Name is required' })} />
          </Grid>
          <Grid size={{ xs: 3 }}>
            <TextField label="Code" fullWidth required
                       error={!!errors.code} helperText={errors.code?.message}
                       {...register('code', { required: 'Code is required' })} />
          </Grid>
          <Grid size={{ xs: 3 }}>
            <Controller name="type" control={control} render={({ field }) => (
              <TextField {...field} select label="Type" fullWidth>
                {TYPES.map((t) => <MenuItem key={t} value={t}>{titleCase(t)}</MenuItem>)}
              </TextField>
            )} />
          </Grid>
          <Grid size={{ xs: 12, sm: 4 }}>
            <Controller name="parentId" control={control} render={({ field }) => (
              <TextField {...field} select label="Parent location" fullWidth>
                <MenuItem value="">None</MenuItem>
                {(locations.data ?? [])
                  .filter((l) => l.id !== existing?.id)
                  .map((l) => <MenuItem key={l.id} value={l.id}>{l.name}</MenuItem>)}
              </TextField>
            )} />
          </Grid>
          <Grid size={{ xs: 6, sm: 4 }}>
            <Controller name="facultyId" control={control} render={({ field }) => (
              <TextField {...field} select label="Faculty" fullWidth>
                <MenuItem value="">None</MenuItem>
                {(faculties.data ?? []).map((f) => <MenuItem key={f.id} value={f.id}>{f.name}</MenuItem>)}
              </TextField>
            )} />
          </Grid>
          <Grid size={{ xs: 6, sm: 4 }}>
            <Controller name="departmentId" control={control} render={({ field }) => (
              <TextField {...field} select label="Department" fullWidth>
                <MenuItem value="">None</MenuItem>
                {(departments.data ?? []).map((d) => <MenuItem key={d.id} value={d.id}>{d.name}</MenuItem>)}
              </TextField>
            )} />
          </Grid>
          <Grid size={{ xs: 8 }}>
            <TextField label="Building address" fullWidth {...register('address')} />
          </Grid>
          <Grid size={{ xs: 4 }}>
            <TextField label="Capacity" type="number" fullWidth inputProps={{ min: 0 }}
                       {...register('capacity')} />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <TextField label="Description" fullWidth {...register('description')} />
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button color="inherit" onClick={onClose}>Cancel</Button>
        <Button variant="contained" disabled={mutation.isPending}
                onClick={handleSubmit((values) => mutation.mutate(values))}>
          {existing ? 'Save changes' : 'Create location'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

type RowAction = 'deactivate' | 'activate' | 'delete';

const ACTION_COPY: Record<RowAction, {
  title: string; confirmLabel: string; destructive: boolean; message: (l: Location) => string;
}> = {
  deactivate: {
    title: 'Deactivate location',
    confirmLabel: 'Deactivate',
    destructive: true,
    message: (l) => `Deactivate ${l.name} (${l.code})? It will no longer be offered for new `
      + 'placements or venue bookings. History is preserved and it can be reactivated later.',
  },
  activate: {
    title: 'Reactivate location',
    confirmLabel: 'Reactivate',
    destructive: false,
    message: (l) => `Reactivate ${l.name} (${l.code})? It becomes available for asset placement `
      + 'and venue bookings again.',
  },
  delete: {
    title: 'Delete location',
    confirmLabel: 'Delete permanently',
    destructive: true,
    message: (l) => `Permanently delete ${l.name} (${l.code})? This is only possible when nothing `
      + 'references it — a location with assets, stock, bookings or child locations must be '
      + 'deactivated instead.',
  },
};

export default function LocationsPage() {
  const { hasPermission } = useAuth();
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();
  const locations = useLocations();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<Location | null>(null);
  const [pending, setPending] = useState<{ location: Location; action: RowAction } | null>(null);
  const canManage = hasPermission('LOCATION_MANAGE');

  const actionMutation = useMutation({
    mutationFn: async ({ location, action }: { location: Location; action: RowAction }) =>
      action === 'delete'
        ? (await api.delete(`/locations/${location.id}`)).data
        : (await api.post(`/locations/${location.id}/${action}`)).data,
    onSuccess: (_, { location, action }) => {
      queryClient.invalidateQueries({ queryKey: ['locations'] });
      enqueueSnackbar(
        action === 'delete' ? `${location.name} deleted`
          : action === 'deactivate' ? `${location.name} deactivated`
            : `${location.name} reactivated`,
        { variant: 'success' },
      );
      setPending(null);
    },
    // Backend 400/409 guards (has history, active children, …) surface their message here.
    onError: (error) => enqueueSnackbar(errorMessage(error), { variant: 'error' }),
  });

  return (
    <Box>
      <PageHeader
        eyebrow="INVENTORY"
        title="Locations"
        crumbs={[{ label: 'Locations' }]}
        subtitle="Campus → building → room → laboratory → storage hierarchy."
        actions={canManage && (
          <Button variant="contained" startIcon={<AddIcon />}
                  onClick={() => { setEditing(null); setDialogOpen(true); }}>
            Add location
          </Button>
        )}
      />
      <Card variant="outlined">
        {(locations.data ?? []).length === 0 ? (
          <EmptyState title="No locations yet"
                      hint="Create the campus and building structure so assets can be placed." />
        ) : (
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Code</TableCell>
                <TableCell>Name</TableCell>
                <TableCell>Type</TableCell>
                <TableCell>Parent</TableCell>
                <TableCell>Faculty</TableCell>
                <TableCell>Department</TableCell>
                <TableCell align="right">Capacity</TableCell>
                <TableCell>Status</TableCell>
                {canManage && <TableCell />}
              </TableRow>
            </TableHead>
            <TableBody>
              {(locations.data ?? []).map((location) => (
                <TableRow key={location.id} hover>
                  <TableCell><CodeTag>{location.code}</CodeTag></TableCell>
                  <TableCell>{location.name}</TableCell>
                  <TableCell>{titleCase(location.type)}</TableCell>
                  <TableCell>{location.parentName ?? '—'}</TableCell>
                  <TableCell>{location.facultyName ?? '—'}</TableCell>
                  <TableCell>{location.departmentName ?? '—'}</TableCell>
                  <TableCell align="right">{location.capacity ?? '—'}</TableCell>
                  <TableCell>
                    <Chip label={location.active ? 'Active' : 'Inactive'} size="small"
                          color={location.active ? 'default' : 'warning'}
                          sx={location.active ? { backgroundColor: 'rgba(14,124,102,0.12)', color: '#0A5D4D' } : undefined} />
                  </TableCell>
                  {canManage && (
                    <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                      <Tooltip title="Edit">
                        <IconButton size="small"
                                    onClick={() => { setEditing(location); setDialogOpen(true); }}>
                          <EditOutlinedIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      {location.active ? (
                        <Tooltip title="Deactivate">
                          <IconButton size="small" color="error"
                                      onClick={() => setPending({ location, action: 'deactivate' })}>
                            <BlockIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      ) : (
                        <Tooltip title="Reactivate">
                          <IconButton size="small" color="primary"
                                      onClick={() => setPending({ location, action: 'activate' })}>
                            <CheckCircleOutlineIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      )}
                      <Tooltip title="Delete permanently">
                        <IconButton size="small" color="error"
                                    onClick={() => setPending({ location, action: 'delete' })}>
                          <DeleteOutlineIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </Card>
      <LocationDialog open={dialogOpen} onClose={() => setDialogOpen(false)} existing={editing} />
      <ConfirmDialog
        open={!!pending}
        title={pending ? ACTION_COPY[pending.action].title : ''}
        message={pending ? ACTION_COPY[pending.action].message(pending.location) : ''}
        confirmLabel={pending ? ACTION_COPY[pending.action].confirmLabel : 'Confirm'}
        destructive={pending ? ACTION_COPY[pending.action].destructive : false}
        busy={actionMutation.isPending}
        onConfirm={() => pending && actionMutation.mutate(pending)}
        onClose={() => setPending(null)}
      />
    </Box>
  );
}
