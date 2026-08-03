import { useEffect, useState } from 'react';
import {
  Box, Button, Card, Dialog, DialogActions, DialogContent, DialogTitle,
  Grid, MenuItem, Stack, TextField, Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import DoneIcon from '@mui/icons-material/Done';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import { Controller, useForm } from 'react-hook-form';
import { useSnackbar } from 'notistack';
import { api, errorMessage } from '../../api/client';
import type { ApiEnvelope, Page } from '../../api/client';
import type { AssetSummary, MaintenanceRequest } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import PageHeader from '../../components/common/PageHeader';
import StatusChip from '../../components/common/StatusChip';
import CodeTag from '../../components/common/CodeTag';
import ServerDataGrid from '../../components/tables/ServerDataGrid';
import { formatDateTime, formatMoney, titleCase } from '../../utils/format';

const ISSUE_TYPES = ['FAULT', 'PREVENTIVE', 'CALIBRATION', 'INSPECTION', 'CLEANING', 'SOFTWARE_UPDATE', 'OTHER'];
const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];
const STATUSES = ['OPEN', 'ASSIGNED', 'IN_PROGRESS', 'WAITING_FOR_PARTS', 'WAITING_FOR_VENDOR', 'COMPLETED', 'CANCELLED', 'UNREPAIRABLE'];
const CONDITIONS = ['NEW', 'EXCELLENT', 'GOOD', 'FAIR', 'POOR', 'DAMAGED', 'UNSERVICEABLE'];

interface CreateValues {
  assetId: string;
  issueType: string;
  description: string;
  priority: string;
  assetOutOfService: boolean;
}

function CreateRequestDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();
  const { register, control, handleSubmit, reset, formState: { errors } } = useForm<CreateValues>({
    defaultValues: { assetId: '', issueType: 'FAULT', description: '', priority: 'MEDIUM', assetOutOfService: false },
  });
  useEffect(() => { if (open) reset(); }, [open, reset]);

  const assetsQuery = useQuery({
    queryKey: ['maintenance-assets'],
    enabled: open,
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<AssetSummary>>>('/assets', {
        params: { size: 100, sort: 'name,asc' },
      })).data.data.content,
  });

  const mutation = useMutation({
    mutationFn: async (values: CreateValues) =>
      (await api.post<ApiEnvelope<MaintenanceRequest>>('/maintenance-requests', {
        assetId: values.assetId,
        issueType: values.issueType,
        description: values.description,
        priority: values.priority,
        assetOutOfService: values.assetOutOfService,
      })).data.data,
    onSuccess: (request) => {
      queryClient.invalidateQueries({ queryKey: ['maintenance'] });
      enqueueSnackbar(`Request ${request.requestNumber} created`, { variant: 'success' });
      onClose();
    },
    onError: (error) => enqueueSnackbar(errorMessage(error), { variant: 'error' }),
  });

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Report a fault or request maintenance</DialogTitle>
      <DialogContent>
        <Grid container spacing={2} sx={{ mt: 0.25 }}>
          <Grid size={{ xs: 12 }}>
            <Controller name="assetId" control={control} rules={{ required: 'Select an asset' }}
                        render={({ field, fieldState }) => (
              <TextField {...field} select label="Asset" fullWidth required autoFocus
                         error={!!fieldState.error} helperText={fieldState.error?.message}>
                {(assetsQuery.data ?? []).map((a) => (
                  <MenuItem key={a.id} value={a.id}>{a.name} — {a.assetCode}</MenuItem>
                ))}
              </TextField>
            )} />
          </Grid>
          <Grid size={{ xs: 6 }}>
            <Controller name="issueType" control={control} render={({ field }) => (
              <TextField {...field} select label="Issue type" fullWidth>
                {ISSUE_TYPES.map((t) => <MenuItem key={t} value={t}>{titleCase(t)}</MenuItem>)}
              </TextField>
            )} />
          </Grid>
          <Grid size={{ xs: 6 }}>
            <Controller name="priority" control={control} render={({ field }) => (
              <TextField {...field} select label="Priority" fullWidth>
                {PRIORITIES.map((p) => <MenuItem key={p} value={p}>{titleCase(p)}</MenuItem>)}
              </TextField>
            )} />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <TextField label="Describe the issue" fullWidth required multiline minRows={3}
                       error={!!errors.description} helperText={errors.description?.message}
                       {...register('description', { required: 'A description is required' })} />
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button color="inherit" onClick={onClose}>Cancel</Button>
        <Button variant="contained" disabled={mutation.isPending}
                onClick={handleSubmit((values) => mutation.mutate(values))}>
          Submit request
        </Button>
      </DialogActions>
    </Dialog>
  );
}

interface CompleteValues {
  workPerformed: string;
  result: string;
  newCondition: string;
  labourCost: string;
  partsCost: string;
  externalCost: string;
  nextServiceDate: string;
}

function CompleteDialog({ request, onClose }: { request: MaintenanceRequest | null; onClose: () => void }) {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();
  const { register, control, handleSubmit, reset } = useForm<CompleteValues>({
    defaultValues: {
      workPerformed: '', result: '', newCondition: 'GOOD',
      labourCost: '', partsCost: '', externalCost: '', nextServiceDate: '',
    },
  });
  useEffect(() => { if (request) reset(); }, [request, reset]);

  const mutation = useMutation({
    mutationFn: async (values: CompleteValues) =>
      (await api.post(`/maintenance-requests/${request!.id}/complete`, {
        workPerformed: values.workPerformed || null,
        result: values.result || null,
        newCondition: values.newCondition,
        labourCost: values.labourCost ? Number(values.labourCost) : null,
        partsCost: values.partsCost ? Number(values.partsCost) : null,
        externalCost: values.externalCost ? Number(values.externalCost) : null,
        nextServiceDate: values.nextServiceDate || null,
      })).data,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['maintenance'] });
      enqueueSnackbar('Maintenance job completed', { variant: 'success' });
      onClose();
    },
    onError: (error) => enqueueSnackbar(errorMessage(error), { variant: 'error' }),
  });

  return (
    <Dialog open={!!request} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Complete job — {request?.requestNumber}</DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          {request?.assetName}: {request?.description}
        </Typography>
        <Grid container spacing={2}>
          <Grid size={{ xs: 12 }}>
            <TextField label="Work performed" fullWidth multiline minRows={2} {...register('workPerformed')} />
          </Grid>
          <Grid size={{ xs: 6 }}>
            <TextField label="Result" fullWidth {...register('result')} />
          </Grid>
          <Grid size={{ xs: 6 }}>
            <Controller name="newCondition" control={control} render={({ field }) => (
              <TextField {...field} select label="New condition" fullWidth>
                {CONDITIONS.map((c) => <MenuItem key={c} value={c}>{c}</MenuItem>)}
              </TextField>
            )} />
          </Grid>
          <Grid size={{ xs: 4 }}>
            <TextField label="Labour cost" type="number" fullWidth inputProps={{ min: 0, step: '0.01' }}
                       {...register('labourCost')} />
          </Grid>
          <Grid size={{ xs: 4 }}>
            <TextField label="Parts cost" type="number" fullWidth inputProps={{ min: 0, step: '0.01' }}
                       {...register('partsCost')} />
          </Grid>
          <Grid size={{ xs: 4 }}>
            <TextField label="External cost" type="number" fullWidth inputProps={{ min: 0, step: '0.01' }}
                       {...register('externalCost')} />
          </Grid>
          <Grid size={{ xs: 6 }}>
            <TextField label="Next service date" type="date" fullWidth InputLabelProps={{ shrink: true }}
                       {...register('nextServiceDate')} />
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button color="inherit" onClick={onClose}>Cancel</Button>
        <Button variant="contained" disabled={mutation.isPending}
                onClick={handleSubmit((values) => mutation.mutate(values))}>
          Complete job
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export default function MaintenancePage() {
  const { hasPermission } = useAuth();
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();
  const [status, setStatus] = useState('');
  const [pagination, setPagination] = useState<GridPaginationModel>({ page: 0, pageSize: 20 });
  const [createOpen, setCreateOpen] = useState(false);
  const [completing, setCompleting] = useState<MaintenanceRequest | null>(null);

  const params = {
    status: status || undefined,
    page: pagination.page,
    size: pagination.pageSize,
  };

  const query = useQuery({
    queryKey: ['maintenance', params],
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<MaintenanceRequest>>>('/maintenance-requests', { params })).data.data,
    placeholderData: keepPreviousData,
  });

  const startMutation = useMutation({
    mutationFn: async (request: MaintenanceRequest) =>
      (await api.post(`/maintenance-requests/${request.id}/start`)).data,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['maintenance'] });
      enqueueSnackbar('Work started — asset marked under maintenance', { variant: 'success' });
    },
    onError: (error) => enqueueSnackbar(errorMessage(error), { variant: 'error' }),
  });

  const canManage = hasPermission('MAINTENANCE_MANAGE');
  const openStatuses = ['OPEN', 'ASSIGNED', 'IN_PROGRESS', 'WAITING_FOR_PARTS', 'WAITING_FOR_VENDOR'];

  const columns: GridColDef<MaintenanceRequest>[] = [
    {
      field: 'requestNumber', headerName: 'Number', width: 125, sortable: false,
      renderCell: ({ row }) => <CodeTag>{row.requestNumber}</CodeTag>,
    },
    { field: 'assetName', headerName: 'Asset', flex: 1.1, minWidth: 150, sortable: false },
    {
      field: 'issueType', headerName: 'Issue', width: 130, sortable: false,
      valueFormatter: (value: string) => titleCase(value),
    },
    { field: 'description', headerName: 'Description', flex: 1.4, minWidth: 180, sortable: false },
    {
      field: 'priority', headerName: 'Priority', width: 95, sortable: false,
      valueFormatter: (value: string) => titleCase(value),
    },
    {
      field: 'openedAt', headerName: 'Opened', width: 150,
      valueFormatter: (value: string) => formatDateTime(value),
    },
    {
      field: 'totalCost', headerName: 'Cost', width: 100, align: 'right', headerAlign: 'right', sortable: false,
      valueFormatter: (value: number | undefined) => (value ? formatMoney(value) : '—'),
    },
    {
      field: 'status', headerName: 'Status', width: 150, sortable: false,
      renderCell: ({ row }) => <StatusChip value={row.status} />,
    },
    ...(canManage ? [{
      field: 'actions', headerName: '', width: 190, sortable: false,
      renderCell: ({ row }: { row: MaintenanceRequest }) => openStatuses.includes(row.status) ? (
        <Stack direction="row" spacing={0.5} sx={{ alignItems: 'center', height: '100%' }}>
          {row.status !== 'IN_PROGRESS' && (
            <Button size="small" startIcon={<PlayArrowIcon />}
                    onClick={(e) => { e.stopPropagation(); startMutation.mutate(row); }}>
              Start
            </Button>
          )}
          <Button size="small" color="primary" startIcon={<DoneIcon />}
                  onClick={(e) => { e.stopPropagation(); setCompleting(row); }}>
            Complete
          </Button>
        </Stack>
      ) : null,
    } satisfies GridColDef<MaintenanceRequest>] : []),
  ];

  return (
    <Box>
      <PageHeader
        eyebrow="OPERATIONS"
        title="Maintenance & Calibration"
        crumbs={[{ label: 'Maintenance' }]}
        subtitle="Fault reports, preventive maintenance, calibration and repairs."
        actions={hasPermission('MAINTENANCE_CREATE') && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
            New request
          </Button>
        )}
      />

      <Card variant="outlined" sx={{ p: 2, mb: 2 }}>
        <TextField select size="small" label="Status" value={status} sx={{ minWidth: 200 }}
                   onChange={(e) => setStatus(e.target.value)}>
          <MenuItem value="">All statuses</MenuItem>
          {STATUSES.map((s) => <MenuItem key={s} value={s}>{s.replaceAll('_', ' ')}</MenuItem>)}
        </TextField>
      </Card>

      <ServerDataGrid<MaintenanceRequest>
        columns={columns}
        page={query.data}
        loading={query.isLoading || query.isFetching}
        paginationModel={pagination}
        onPaginationModelChange={setPagination}
        emptyTitle="No maintenance requests"
        emptyHint="Report a fault or schedule preventive maintenance with New request."
      />

      <CreateRequestDialog open={createOpen} onClose={() => setCreateOpen(false)} />
      <CompleteDialog request={completing} onClose={() => setCompleting(null)} />
    </Box>
  );
}
