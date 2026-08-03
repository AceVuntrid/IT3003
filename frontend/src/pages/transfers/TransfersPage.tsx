import { useEffect, useState } from 'react';
import {
  Box, Button, Card, Dialog, DialogActions, DialogContent, DialogTitle,
  Grid, MenuItem, Stack, TextField,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import CheckIcon from '@mui/icons-material/Check';
import CloseIcon from '@mui/icons-material/Close';
import DoneAllIcon from '@mui/icons-material/DoneAll';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import { Controller, useForm } from 'react-hook-form';
import { useSnackbar } from 'notistack';
import { api, errorMessage } from '../../api/client';
import type { ApiEnvelope, Page } from '../../api/client';
import type { AssetSummary, Transfer } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import { useLocations } from '../../api/referenceData';
import PageHeader from '../../components/common/PageHeader';
import StatusChip from '../../components/common/StatusChip';
import CodeTag from '../../components/common/CodeTag';
import ServerDataGrid from '../../components/tables/ServerDataGrid';
import { formatDateTime } from '../../utils/format';

interface CreateValues {
  assetId: string;
  toLocationId: string;
  reason: string;
  notes: string;
}

function CreateTransferDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();
  const locations = useLocations();
  const { control, register, handleSubmit, reset, formState: { errors } } = useForm<CreateValues>({
    defaultValues: { assetId: '', toLocationId: '', reason: '', notes: '' },
  });
  useEffect(() => { if (open) reset(); }, [open, reset]);

  const assetsQuery = useQuery({
    queryKey: ['transfer-assets'],
    enabled: open,
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<AssetSummary>>>('/assets', {
        params: { size: 100, sort: 'name,asc' },
      })).data.data.content,
  });

  const mutation = useMutation({
    mutationFn: async (values: CreateValues) =>
      (await api.post<ApiEnvelope<Transfer>>('/transfers', {
        assetId: values.assetId,
        toLocationId: values.toLocationId,
        reason: values.reason,
        notes: values.notes || null,
      })).data.data,
    onSuccess: (transfer) => {
      queryClient.invalidateQueries({ queryKey: ['transfers'] });
      enqueueSnackbar(`Transfer ${transfer.transferNumber} submitted`, { variant: 'success' });
      onClose();
    },
    onError: (error) => enqueueSnackbar(errorMessage(error), { variant: 'error' }),
  });

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Request asset transfer</DialogTitle>
      <DialogContent>
        <Grid container spacing={2} sx={{ mt: 0.25 }}>
          <Grid size={{ xs: 12 }}>
            <Controller name="assetId" control={control} rules={{ required: 'Select an asset' }}
                        render={({ field, fieldState }) => (
              <TextField {...field} select label="Asset" fullWidth required autoFocus
                         error={!!fieldState.error} helperText={fieldState.error?.message}>
                {(assetsQuery.data ?? []).map((a) => (
                  <MenuItem key={a.id} value={a.id}>
                    {a.name} — {a.assetCode} (now in {a.locationName})
                  </MenuItem>
                ))}
              </TextField>
            )} />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <Controller name="toLocationId" control={control} rules={{ required: 'Select a destination' }}
                        render={({ field, fieldState }) => (
              <TextField {...field} select label="Destination location" fullWidth required
                         error={!!fieldState.error} helperText={fieldState.error?.message}>
                {(locations.data ?? []).map((l) => (
                  <MenuItem key={l.id} value={l.id}>{l.name} ({l.code})</MenuItem>
                ))}
              </TextField>
            )} />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <TextField label="Transfer reason" fullWidth required
                       error={!!errors.reason} helperText={errors.reason?.message}
                       {...register('reason', { required: 'A reason is required' })} />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <TextField label="Notes" fullWidth {...register('notes')} />
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button color="inherit" onClick={onClose}>Cancel</Button>
        <Button variant="contained" disabled={mutation.isPending}
                onClick={handleSubmit((values) => mutation.mutate(values))}>
          Submit transfer request
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export default function TransfersPage() {
  const { hasPermission } = useAuth();
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();
  const [status, setStatus] = useState('');
  const [pagination, setPagination] = useState<GridPaginationModel>({ page: 0, pageSize: 20 });
  const [createOpen, setCreateOpen] = useState(false);

  const params = { status: status || undefined, page: pagination.page, size: pagination.pageSize };

  const query = useQuery({
    queryKey: ['transfers', params],
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<Transfer>>>('/transfers', { params })).data.data,
    placeholderData: keepPreviousData,
  });

  const actionMutation = useMutation({
    mutationFn: async ({ transfer, action }: { transfer: Transfer; action: string }) =>
      (await api.post(`/transfers/${transfer.id}/${action}`, {})).data,
    onSuccess: (_, { action }) => {
      queryClient.invalidateQueries({ queryKey: ['transfers'] });
      queryClient.invalidateQueries({ queryKey: ['assets'] });
      enqueueSnackbar(`Transfer ${action}d`, { variant: 'success' });
    },
    onError: (error) => enqueueSnackbar(errorMessage(error), { variant: 'error' }),
  });

  const canApprove = hasPermission('TRANSFER_APPROVE');

  const columns: GridColDef<Transfer>[] = [
    {
      field: 'transferNumber', headerName: 'Number', width: 125, sortable: false,
      renderCell: ({ row }) => <CodeTag>{row.transferNumber}</CodeTag>,
    },
    { field: 'assetName', headerName: 'Asset', flex: 1.1, minWidth: 150, sortable: false },
    {
      field: 'route', headerName: 'From → To', flex: 1.4, minWidth: 200, sortable: false,
      valueGetter: (_, row) => `${row.fromLocationName} → ${row.toLocationName}`,
    },
    { field: 'reason', headerName: 'Reason', flex: 1.2, minWidth: 150, sortable: false },
    { field: 'requestedByName', headerName: 'Requested by', width: 140, sortable: false },
    {
      field: 'createdAt', headerName: 'Requested', width: 150,
      valueFormatter: (value: string) => formatDateTime(value),
    },
    {
      field: 'status', headerName: 'Status', width: 155, sortable: false,
      renderCell: ({ row }) => <StatusChip value={row.status} />,
    },
    ...(canApprove ? [{
      field: 'actions', headerName: '', width: 230, sortable: false,
      renderCell: ({ row }: { row: Transfer }) => (
        <Stack direction="row" spacing={0.5} sx={{ alignItems: 'center', height: '100%' }}>
          {row.status === 'PENDING_APPROVAL' && (
            <>
              <Button size="small" startIcon={<CheckIcon />}
                      onClick={(e) => { e.stopPropagation(); actionMutation.mutate({ transfer: row, action: 'approve' }); }}>
                Approve
              </Button>
              <Button size="small" color="error" startIcon={<CloseIcon />}
                      onClick={(e) => { e.stopPropagation(); actionMutation.mutate({ transfer: row, action: 'reject' }); }}>
                Reject
              </Button>
            </>
          )}
          {row.status === 'APPROVED' && (
            <Button size="small" color="primary" startIcon={<DoneAllIcon />}
                    onClick={(e) => { e.stopPropagation(); actionMutation.mutate({ transfer: row, action: 'complete' }); }}>
              Complete
            </Button>
          )}
        </Stack>
      ),
    } satisfies GridColDef<Transfer>] : []),
  ];

  return (
    <Box>
      <PageHeader
        eyebrow="OPERATIONS"
        title="Asset Transfers"
        crumbs={[{ label: 'Transfers' }]}
        subtitle="Move assets between locations and custodians with an approval trail."
        actions={hasPermission('TRANSFER_CREATE') && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
            Request transfer
          </Button>
        )}
      />

      <Card variant="outlined" sx={{ p: 2, mb: 2 }}>
        <TextField select size="small" label="Status" value={status} sx={{ minWidth: 200 }}
                   onChange={(e) => setStatus(e.target.value)}>
          <MenuItem value="">All statuses</MenuItem>
          {['PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'COMPLETED', 'CANCELLED'].map((s) => (
            <MenuItem key={s} value={s}>{s.replaceAll('_', ' ')}</MenuItem>
          ))}
        </TextField>
      </Card>

      <ServerDataGrid<Transfer>
        columns={columns}
        page={query.data}
        loading={query.isLoading || query.isFetching}
        paginationModel={pagination}
        onPaginationModelChange={setPagination}
        emptyTitle="No transfers found"
        emptyHint="Request a transfer to move an asset to another laboratory or building."
      />

      <CreateTransferDialog open={createOpen} onClose={() => setCreateOpen(false)} />
    </Box>
  );
}
