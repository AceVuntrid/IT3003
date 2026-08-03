import { useEffect, useState } from 'react';
import {
  Box, Button, Card, Chip, Dialog, DialogActions, DialogContent, DialogTitle,
  Grid, InputAdornment, MenuItem, Stack, Tab, Table, TableBody, TableCell,
  TableHead, TableRow, Tabs, TextField, Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import SearchIcon from '@mui/icons-material/Search';
import LockResetIcon from '@mui/icons-material/LockReset';
import BlockIcon from '@mui/icons-material/Block';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import { Controller, useForm } from 'react-hook-form';
import { useSnackbar } from 'notistack';
import { api, errorMessage } from '../../api/client';
import type { ApiEnvelope, Page } from '../../api/client';
import type { Permission, Role, UserRow } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import { useDepartments, useFaculties, useRoles } from '../../api/referenceData';
import PageHeader from '../../components/common/PageHeader';
import StatusChip from '../../components/common/StatusChip';
import CodeTag from '../../components/common/CodeTag';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import ServerDataGrid from '../../components/tables/ServerDataGrid';
import { formatDateTime } from '../../utils/format';

interface UserFormValues {
  firstName: string;
  lastName: string;
  universityId: string;
  email: string;
  phone: string;
  userType: string;
  facultyId: string;
  departmentId: string;
  roleId: string;
  temporaryPassword: string;
}

function UserDialog({ open, onClose, existing }: {
  open: boolean; onClose: () => void; existing: UserRow | null;
}) {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();
  const roles = useRoles();
  const faculties = useFaculties();
  const { register, control, handleSubmit, reset, watch, formState: { errors } } = useForm<UserFormValues>({
    defaultValues: {
      firstName: '', lastName: '', universityId: '', email: '', phone: '',
      userType: 'STAFF', facultyId: '', departmentId: '', roleId: '', temporaryPassword: '',
    },
  });
  const facultyId = watch('facultyId');
  const departments = useDepartments(facultyId || undefined);

  useEffect(() => {
    if (open) {
      const roleId = existing
        ? (roles.data?.find((r) => existing.roles.includes(r.name))?.id ?? '')
        : '';
      reset(existing ? {
        firstName: existing.firstName, lastName: existing.lastName,
        universityId: existing.universityId, email: existing.email, phone: existing.phone ?? '',
        userType: existing.userType ?? 'STAFF', facultyId: existing.facultyId ?? '',
        departmentId: existing.departmentId ?? '', roleId, temporaryPassword: '',
      } : undefined);
    }
  }, [open, existing, reset, roles.data]);

  const mutation = useMutation({
    mutationFn: async (values: UserFormValues) => {
      if (existing) {
        return (await api.put(`/users/${existing.id}`, {
          firstName: values.firstName,
          lastName: values.lastName,
          phone: values.phone || null,
          userType: values.userType || null,
          facultyId: values.facultyId || null,
          departmentId: values.departmentId || null,
          roleIds: values.roleId ? [values.roleId] : null,
        })).data;
      }
      return (await api.post('/users', {
        firstName: values.firstName,
        lastName: values.lastName,
        universityId: values.universityId,
        email: values.email,
        phone: values.phone || null,
        userType: values.userType || null,
        facultyId: values.facultyId || null,
        departmentId: values.departmentId || null,
        roleIds: [values.roleId],
        temporaryPassword: values.temporaryPassword,
        mustChangePassword: true,
      })).data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      enqueueSnackbar(existing ? 'User updated' : 'User created — they must change the temporary password at first sign-in',
        { variant: 'success' });
      onClose();
    },
    onError: (error) => enqueueSnackbar(errorMessage(error), { variant: 'error' }),
  });

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{existing ? `Edit ${existing.fullName}` : 'Add user'}</DialogTitle>
      <DialogContent>
        <Grid container spacing={2} sx={{ mt: 0.25 }}>
          <Grid size={{ xs: 6 }}>
            <TextField label="First name" fullWidth required autoFocus
                       error={!!errors.firstName} helperText={errors.firstName?.message}
                       {...register('firstName', { required: 'First name is required' })} />
          </Grid>
          <Grid size={{ xs: 6 }}>
            <TextField label="Last name" fullWidth required
                       error={!!errors.lastName} helperText={errors.lastName?.message}
                       {...register('lastName', { required: 'Last name is required' })} />
          </Grid>
          <Grid size={{ xs: 6 }}>
            <TextField label="University ID" fullWidth required disabled={!!existing}
                       error={!!errors.universityId} helperText={errors.universityId?.message}
                       {...register('universityId', { required: !existing ? 'University ID is required' : false })} />
          </Grid>
          <Grid size={{ xs: 6 }}>
            <TextField label="Email" type="email" fullWidth required disabled={!!existing}
                       error={!!errors.email} helperText={errors.email?.message}
                       {...register('email', { required: !existing ? 'Email is required' : false })} />
          </Grid>
          <Grid size={{ xs: 6 }}>
            <TextField label="Phone" fullWidth {...register('phone')} />
          </Grid>
          <Grid size={{ xs: 6 }}>
            <Controller name="userType" control={control} render={({ field }) => (
              <TextField {...field} select label="User type" fullWidth>
                {['STAFF', 'ACADEMIC', 'STUDENT', 'CONTRACTOR'].map((t) => (
                  <MenuItem key={t} value={t}>{t}</MenuItem>
                ))}
              </TextField>
            )} />
          </Grid>
          <Grid size={{ xs: 6 }}>
            <Controller name="facultyId" control={control} render={({ field }) => (
              <TextField {...field} select label="Faculty" fullWidth>
                <MenuItem value="">None</MenuItem>
                {(faculties.data ?? []).map((f) => <MenuItem key={f.id} value={f.id}>{f.name}</MenuItem>)}
              </TextField>
            )} />
          </Grid>
          <Grid size={{ xs: 6 }}>
            <Controller name="departmentId" control={control} render={({ field }) => (
              <TextField {...field} select label="Department" fullWidth>
                <MenuItem value="">None</MenuItem>
                {(departments.data ?? []).map((d) => <MenuItem key={d.id} value={d.id}>{d.name}</MenuItem>)}
              </TextField>
            )} />
          </Grid>
          <Grid size={{ xs: existing ? 12 : 6 }}>
            <Controller name="roleId" control={control} rules={{ required: 'Role is required' }}
                        render={({ field, fieldState }) => (
              <TextField {...field} select label="Role" fullWidth required
                         error={!!fieldState.error} helperText={fieldState.error?.message}>
                {(roles.data ?? []).map((r) => <MenuItem key={r.id} value={r.id}>{r.name}</MenuItem>)}
              </TextField>
            )} />
          </Grid>
          {!existing && (
            <Grid size={{ xs: 6 }}>
              <TextField label="Temporary password" fullWidth required
                         error={!!errors.temporaryPassword} helperText={errors.temporaryPassword?.message}
                         {...register('temporaryPassword', { required: 'Temporary password is required' })} />
            </Grid>
          )}
        </Grid>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button color="inherit" onClick={onClose}>Cancel</Button>
        <Button variant="contained" disabled={mutation.isPending}
                onClick={handleSubmit((values) => mutation.mutate(values))}>
          {existing ? 'Save changes' : 'Create user'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

function RolesTab() {
  const { hasPermission } = useAuth();
  const roles = useRoles();
  const canManage = hasPermission('ROLE_MANAGE');
  const permissionsQuery = useQuery({
    queryKey: ['permissions'],
    enabled: canManage,
    queryFn: async () =>
      (await api.get<ApiEnvelope<Permission[]>>('/roles/permissions')).data.data,
  });

  return (
    <Card variant="outlined">
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Role</TableCell>
            <TableCell>Description</TableCell>
            <TableCell>Permissions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {(roles.data ?? []).map((role: Role) => (
            <TableRow key={role.id} hover>
              <TableCell>
                <Stack direction="row" spacing={1} alignItems="center">
                  <Typography variant="body2" fontWeight={600}>{role.name}</Typography>
                  {role.systemRole && <Chip label="System" size="small" variant="outlined" />}
                </Stack>
              </TableCell>
              <TableCell sx={{ maxWidth: 320 }}>
                <Typography variant="body2" color="text.secondary">{role.description}</Typography>
              </TableCell>
              <TableCell>
                <Typography variant="caption" color="text.secondary">
                  {role.permissions.length} permission(s)
                  {canManage && permissionsQuery.data
                    ? ` of ${permissionsQuery.data.length} available`
                    : ''}
                </Typography>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Card>
  );
}

export default function UsersPage() {
  const { hasPermission, user: currentUser } = useAuth();
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();
  const [tab, setTab] = useState(0);
  const [search, setSearch] = useState('');
  const [pagination, setPagination] = useState<GridPaginationModel>({ page: 0, pageSize: 20 });
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<UserRow | null>(null);
  const [pendingActivation, setPendingActivation] =
    useState<{ user: UserRow; action: 'deactivate' | 'activate' } | null>(null);

  // The administration grid must see non-active accounts too, otherwise a
  // deactivated user would vanish and could never be reactivated from the UI.
  const params = {
    search: search || undefined,
    includeInactive: true,
    page: pagination.page,
    size: pagination.pageSize,
  };

  const query = useQuery({
    queryKey: ['users', params],
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<UserRow>>>('/users', { params })).data.data,
    placeholderData: keepPreviousData,
  });

  const activationMutation = useMutation({
    mutationFn: async ({ user, action }: { user: UserRow; action: 'deactivate' | 'activate' }) =>
      (await api.post(`/users/${user.id}/${action}`)).data,
    onSuccess: (_, { user, action }) => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      enqueueSnackbar(action === 'deactivate'
        ? `${user.fullName} deactivated — they can no longer sign in`
        : `${user.fullName} reactivated`, { variant: 'success' });
      setPendingActivation(null);
    },
    onError: (error) => enqueueSnackbar(errorMessage(error), { variant: 'error' }),
  });

  const resetMutation = useMutation({
    mutationFn: async (user: UserRow) =>
      (await api.post<ApiEnvelope<{ temporaryPassword: string }>>(`/users/${user.id}/reset-password`)).data.data,
    onSuccess: (data) => {
      enqueueSnackbar(`Temporary password: ${data.temporaryPassword}`, {
        variant: 'info', autoHideDuration: 12000,
      });
    },
    onError: (error) => enqueueSnackbar(errorMessage(error), { variant: 'error' }),
  });

  const canEdit = hasPermission('USER_EDIT');
  const canDeactivate = hasPermission('USER_DEACTIVATE');

  const columns: GridColDef<UserRow>[] = [
    {
      field: 'universityId', headerName: 'ID', width: 110, sortable: false,
      renderCell: ({ row }) => <CodeTag muted>{row.universityId}</CodeTag>,
    },
    { field: 'fullName', headerName: 'Name', flex: 1, minWidth: 150, sortable: false },
    { field: 'email', headerName: 'Email', flex: 1.2, minWidth: 190, sortable: false },
    {
      field: 'roles', headerName: 'Role', width: 170, sortable: false,
      valueGetter: (_, row) => row.roles.join(', '),
    },
    { field: 'facultyName', headerName: 'Faculty', flex: 0.9, minWidth: 130, sortable: false },
    {
      field: 'accountStatus', headerName: 'Status', width: 110, sortable: false,
      renderCell: ({ row }) => <StatusChip value={row.accountStatus} />,
    },
    {
      field: 'lastLoginAt', headerName: 'Last login', width: 150, sortable: false,
      valueFormatter: (value: string | undefined) => formatDateTime(value),
    },
    ...((canEdit || canDeactivate) ? [{
      field: 'actions', headerName: '', width: 240, sortable: false,
      renderCell: ({ row }: { row: UserRow }) => (
        <Stack direction="row" spacing={0.25} sx={{ alignItems: 'center', height: '100%' }}>
          {canEdit && (
            <>
              <Button size="small" onClick={(e) => { e.stopPropagation(); setEditing(row); setDialogOpen(true); }}>
                Edit
              </Button>
              <Button size="small" color="inherit" startIcon={<LockResetIcon />}
                      onClick={(e) => { e.stopPropagation(); resetMutation.mutate(row); }}>
                Reset
              </Button>
            </>
          )}
          {canDeactivate && row.id !== currentUser?.id && (
            row.accountStatus === 'ACTIVE' ? (
              <Button size="small" color="error" startIcon={<BlockIcon />}
                      onClick={(e) => { e.stopPropagation(); setPendingActivation({ user: row, action: 'deactivate' }); }}>
                Deactivate
              </Button>
            ) : (
              <Button size="small" color="primary" startIcon={<CheckCircleOutlineIcon />}
                      onClick={(e) => { e.stopPropagation(); setPendingActivation({ user: row, action: 'activate' }); }}>
                Activate
              </Button>
            )
          )}
        </Stack>
      ),
    } satisfies GridColDef<UserRow>] : []),
  ];

  return (
    <Box>
      <PageHeader
        eyebrow="ADMINISTRATION"
        title="Users & Roles"
        crumbs={[{ label: 'Users & Roles' }]}
        actions={tab === 0 && hasPermission('USER_CREATE') && (
          <Button variant="contained" startIcon={<AddIcon />}
                  onClick={() => { setEditing(null); setDialogOpen(true); }}>
            Add user
          </Button>
        )}
      />

      <Tabs value={tab} onChange={(_, value) => setTab(value)} sx={{ mb: 2 }}>
        <Tab label="Users" />
        <Tab label="Roles & permissions" />
      </Tabs>

      {tab === 0 && (
        <>
          <Card variant="outlined" sx={{ p: 2, mb: 2 }}>
            <TextField
              size="small" placeholder="Search name, email or university ID…" sx={{ minWidth: 320 }}
              value={search}
              onChange={(e) => { setSearch(e.target.value); setPagination((p) => ({ ...p, page: 0 })); }}
              InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> }}
            />
          </Card>
          <ServerDataGrid<UserRow>
            columns={columns}
            page={query.data}
            loading={query.isLoading || query.isFetching}
            paginationModel={pagination}
            onPaginationModelChange={setPagination}
            emptyTitle="No users match this search"
          />
        </>
      )}

      {tab === 1 && <RolesTab />}

      <UserDialog open={dialogOpen} onClose={() => setDialogOpen(false)} existing={editing} />

      <ConfirmDialog
        open={!!pendingActivation}
        title={pendingActivation?.action === 'deactivate' ? 'Deactivate user' : 'Reactivate user'}
        message={pendingActivation?.action === 'deactivate'
          ? `Deactivate ${pendingActivation.user.fullName}? They will be unable to sign in until reactivated. Their history is preserved.`
          : `Reactivate ${pendingActivation?.user.fullName ?? 'this user'}? They will be able to sign in again.`}
        confirmLabel={pendingActivation?.action === 'deactivate' ? 'Deactivate' : 'Reactivate'}
        destructive={pendingActivation?.action === 'deactivate'}
        busy={activationMutation.isPending}
        onConfirm={() => pendingActivation && activationMutation.mutate(pendingActivation)}
        onClose={() => setPendingActivation(null)}
      />
    </Box>
  );
}
