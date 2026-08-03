import { useEffect, useState } from 'react';
import {
  Alert, Box, Button, Card, Checkbox, Dialog, DialogActions, DialogContent, DialogTitle,
  FormControlLabel, Grid, MenuItem, Stack, TextField,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import KeyboardReturnIcon from '@mui/icons-material/KeyboardReturn';
import MoreTimeIcon from '@mui/icons-material/MoreTime';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import { Controller, useForm } from 'react-hook-form';
import { useSnackbar } from 'notistack';
import { api, errorMessage } from '../../api/client';
import type { ApiEnvelope, Page } from '../../api/client';
import type { AssetDetail, AssetSummary, Checkout, Reservation } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import PageHeader from '../../components/common/PageHeader';
import StatusChip from '../../components/common/StatusChip';
import CodeTag from '../../components/common/CodeTag';
import ServerDataGrid from '../../components/tables/ServerDataGrid';
import { formatDateTime, localInputToIso, localInputValue } from '../../utils/format';

const CONDITIONS = ['NEW', 'EXCELLENT', 'GOOD', 'FAIR', 'POOR', 'DAMAGED', 'UNSERVICEABLE'];

function CheckoutDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();
  const [reservationId, setReservationId] = useState('');
  const [sourceAssetId, setSourceAssetId] = useState('');
  const [quantity, setQuantity] = useState('');
  const [collectionCode, setCollectionCode] = useState('');
  const [depositPaid, setDepositPaid] = useState('');
  const [accessories, setAccessories] = useState('');
  const [notes, setNotes] = useState('');
  const [acknowledged, setAcknowledged] = useState(false);

  useEffect(() => {
    if (open) {
      setReservationId(''); setSourceAssetId(''); setQuantity(''); setCollectionCode('');
      setDepositPaid(''); setAccessories(''); setNotes(''); setAcknowledged(false);
    }
  }, [open]);

  // Reservations that can still be issued: approved ones plus partially issued
  // ones (status CHECKED_OUT) that still have quantity remaining to hand over.
  const issuableQuery = useQuery({
    queryKey: ['issuable-reservations'],
    enabled: open,
    queryFn: async () => {
      const statuses = ['APPROVED', 'READY_FOR_COLLECTION', 'CHECKED_OUT'];
      const pages = await Promise.all(statuses.map((status) =>
        api.get<ApiEnvelope<Page<Reservation>>>('/reservations', {
          params: { status, size: 100 },
        })));
      const byId = new Map<string, Reservation>();
      pages.flatMap((p) => p.data.data.content)
        .filter((r) => !!r.assetId) // room bookings are not collected here
        .forEach((r) => byId.set(r.id, r));
      return [...byId.values()].sort((a, b) => a.reservationNumber.localeCompare(b.reservationNumber));
    },
  });

  // Quantity already issued per reservation comes from the server (any slip
  // status — returned slips still count as issued), so it stays correct no
  // matter how many checkout slips exist system-wide.
  const issuedFor = (r: Reservation) => r.issuedQuantity ?? 0;
  const remainingFor = (r: Reservation) => Math.max(r.quantity - issuedFor(r), 0);

  const options = (issuableQuery.data ?? []).filter((r) => remainingFor(r) > 0);
  const selected = (issuableQuery.data ?? []).find((r) => r.id === reservationId) ?? null;
  const menuReservations = selected && !options.some((r) => r.id === selected.id)
    ? [...options, selected] : options;
  const issued = selected ? issuedFor(selected) : 0;
  const remaining = selected ? remainingFor(selected) : 0;

  // The reserved asset's detail gives us its category, so we can offer the same
  // item from other locations ("issue from" alternatives).
  const reservedAssetQuery = useQuery({
    queryKey: ['assets', 'detail', selected?.assetId],
    enabled: open && !!selected?.assetId,
    queryFn: async () =>
      (await api.get<ApiEnvelope<AssetDetail>>(`/assets/${selected!.assetId}`)).data.data,
  });
  const alternativesQuery = useQuery({
    queryKey: ['assets', 'same-category-available', reservedAssetQuery.data?.categoryId],
    enabled: open && !!reservedAssetQuery.data?.categoryId,
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<AssetSummary>>>('/assets', {
        params: { categoryId: reservedAssetQuery.data!.categoryId, availableOnly: true, size: 100 },
      })).data.data.content,
  });

  const sourceOptions: { id: string; label: string }[] = [];
  if (selected?.assetId) {
    const reserved = reservedAssetQuery.data;
    sourceOptions.push({
      id: selected.assetId,
      label: reserved
        ? `${reserved.assetCode} — ${reserved.name} (avail ${reserved.availableQuantity}) @ ${reserved.locationName} · reserved`
        : `${selected.assetCode} — ${selected.assetName} · reserved`,
    });
    (alternativesQuery.data ?? [])
      .filter((a) => a.id !== selected.assetId)
      .forEach((a) => sourceOptions.push({
        id: a.id,
        label: `${a.assetCode} — ${a.name} (avail ${a.availableQuantity}) @ ${a.locationName}`,
      }));
  }
  const sourceAvailable = sourceAssetId && sourceAssetId === selected?.assetId
    ? reservedAssetQuery.data?.availableQuantity
    : (alternativesQuery.data ?? []).find((a) => a.id === sourceAssetId)?.availableQuantity;

  const quantityNum = Number(quantity);
  const quantityInvalid = !!selected
    && (!Number.isInteger(quantityNum) || quantityNum < 1 || quantityNum > remaining);
  const sourceShort = sourceAvailable != null && !quantityInvalid && quantityNum > sourceAvailable;
  // Reservation-based checkouts require the borrower's 4-digit collection code.
  // Blank is allowed and arbitrated server-side: reservations approved before
  // codes existed store no code and are only accepted with a blank one.
  const codeInvalid = !!selected && !/^(\d{4})?$/.test(collectionCode);

  const mutation = useMutation({
    mutationFn: async () =>
      (await api.post<ApiEnvelope<Checkout>>('/checkouts', {
        reservationId,
        assetId: sourceAssetId || null,
        quantity: quantity ? Number(quantity) : null,
        collectionCode: collectionCode || null,
        depositPaid: depositPaid ? Number(depositPaid) : null,
        accessories: accessories || null,
        notes: notes || null,
      })).data.data,
    onSuccess: (checkout) => {
      queryClient.invalidateQueries({ queryKey: ['checkouts'] });
      queryClient.invalidateQueries({ queryKey: ['reservations'] });
      queryClient.invalidateQueries({ queryKey: ['issuable-reservations'] });
      queryClient.invalidateQueries({ queryKey: ['assets'] });
      const stillToIssue = remaining - checkout.quantity;
      enqueueSnackbar(stillToIssue > 0 && selected
        ? `Checked out as ${checkout.checkoutNumber} — ${stillToIssue} of ${selected.quantity} still to issue`
        : `Checked out as ${checkout.checkoutNumber}`, { variant: 'success' });
      onClose();
    },
    onError: (error) => enqueueSnackbar(errorMessage(error), { variant: 'error' }),
  });

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Check out asset</DialogTitle>
      <DialogContent>
        <Grid container spacing={2} sx={{ mt: 0.25 }}>
          <Grid size={{ xs: 12 }}>
            <TextField select label="Approved reservation" fullWidth required autoFocus
                       value={reservationId}
                       onChange={(e) => {
                         const id = e.target.value;
                         setReservationId(id);
                         const r = (issuableQuery.data ?? []).find((x) => x.id === id);
                         setSourceAssetId(r?.assetId ?? '');
                         setQuantity(r ? String(remainingFor(r)) : '');
                         setCollectionCode('');
                       }}
                       helperText="Approved reservations — partially issued ones stay listed until fully issued.">
              {menuReservations.map((r) => {
                const left = remainingFor(r);
                return (
                  <MenuItem key={r.id} value={r.id}>
                    {r.reservationNumber} — {r.assetName} · {r.requestedByName}
                    {issuedFor(r) > 0 ? ` (${left} of ${r.quantity} left)` : ` (qty ${r.quantity})`}
                  </MenuItem>
                );
              })}
            </TextField>
          </Grid>
          {selected && (
            <Grid size={{ xs: 12 }}>
              <Alert severity="info" sx={{ py: 0.25 }}>
                Remaining to issue: {remaining} of {selected.quantity}
                {issued > 0 ? ` — ${issued} already issued` : ''}
              </Alert>
            </Grid>
          )}
          {selected && (
            <Grid size={{ xs: 12 }}>
              <TextField select label="Issue from" fullWidth required
                         value={sourceAssetId} onChange={(e) => setSourceAssetId(e.target.value)}
                         error={sourceShort}
                         helperText={sourceShort
                           ? `Only ${sourceAvailable} available at this source`
                           : 'The same item can be issued from another location (same category).'}>
                {sourceOptions.map((o) => (
                  <MenuItem key={o.id} value={o.id}>{o.label}</MenuItem>
                ))}
              </TextField>
            </Grid>
          )}
          <Grid size={{ xs: 4 }}>
            <TextField label="Quantity" type="number" fullWidth
                       inputProps={{ min: 1, max: remaining || undefined }}
                       disabled={!selected}
                       error={!!quantity && quantityInvalid}
                       helperText={selected ? `Max ${remaining}` : undefined}
                       value={quantity} onChange={(e) => setQuantity(e.target.value)} />
          </Grid>
          {selected && (
            <Grid size={{ xs: 4 }}>
              <TextField label="Collection code" fullWidth
                         inputProps={{ inputMode: 'numeric', pattern: '[0-9]*', maxLength: 4 }}
                         error={!!collectionCode && codeInvalid}
                         helperText="Ask the borrower for their code — blank only if the reservation predates codes"
                         value={collectionCode}
                         onChange={(e) => setCollectionCode(e.target.value.replace(/\D/g, ''))} />
            </Grid>
          )}
          <Grid size={{ xs: selected ? 4 : 8 }}>
            <TextField label="Deposit paid" type="number" fullWidth
                       inputProps={{ min: 0, step: '0.01' }}
                       value={depositPaid} onChange={(e) => setDepositPaid(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <TextField label="Accessories included" fullWidth placeholder="Cables, case, charger…"
                       value={accessories} onChange={(e) => setAccessories(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <TextField label="Notes" fullWidth value={notes} onChange={(e) => setNotes(e.target.value)} />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <FormControlLabel
              control={<Checkbox checked={acknowledged} onChange={(e) => setAcknowledged(e.target.checked)} />}
              label="The borrower acknowledges responsibility for the item until it is returned."
            />
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button color="inherit" onClick={onClose}>Cancel</Button>
        <Button variant="contained"
                disabled={!reservationId || !sourceAssetId || quantityInvalid || codeInvalid
                  || !acknowledged || mutation.isPending}
                onClick={() => mutation.mutate()}>
          Confirm check-out
        </Button>
      </DialogActions>
    </Dialog>
  );
}

interface ReturnValues {
  conditionAfter: string;
  missingAccessories: string;
  damageDetected: boolean;
  damageDescription: string;
  sendToMaintenance: boolean;
  notes: string;
}

function ReturnDialog({ checkout, onClose }: { checkout: Checkout | null; onClose: () => void }) {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();
  const { register, control, handleSubmit, reset, watch } = useForm<ReturnValues>({
    defaultValues: {
      conditionAfter: 'GOOD', missingAccessories: '', damageDetected: false,
      damageDescription: '', sendToMaintenance: false, notes: '',
    },
  });
  useEffect(() => {
    if (checkout) {
      reset({
        conditionAfter: checkout.conditionBefore, missingAccessories: '',
        damageDetected: false, damageDescription: '', sendToMaintenance: false, notes: '',
      });
    }
  }, [checkout, reset]);
  const damageDetected = watch('damageDetected');

  const mutation = useMutation({
    mutationFn: async (values: ReturnValues) =>
      (await api.post<ApiEnvelope<Checkout>>(`/checkouts/${checkout!.id}/return`, {
        conditionAfter: values.conditionAfter,
        missingAccessories: values.missingAccessories || null,
        damageDetected: values.damageDetected,
        damageDescription: values.damageDescription || null,
        sendToMaintenance: values.sendToMaintenance,
        notes: values.notes || null,
      })).data.data,
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ['checkouts'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      enqueueSnackbar(result.penaltyAmount
        ? `Return recorded — late penalty ${result.penaltyAmount}`
        : 'Return recorded', { variant: 'success' });
      onClose();
    },
    onError: (error) => enqueueSnackbar(errorMessage(error), { variant: 'error' }),
  });

  return (
    <Dialog open={!!checkout} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Record return — {checkout?.checkoutNumber}</DialogTitle>
      <DialogContent>
        {checkout && checkout.daysOverdue > 0 && (
          <Alert severity="warning" sx={{ mb: 2, mt: 0.5 }}>
            This item is {checkout.daysOverdue} day(s) overdue. A late penalty will be calculated automatically.
          </Alert>
        )}
        <Grid container spacing={2} sx={{ mt: 0.25 }}>
          <Grid size={{ xs: 6 }}>
            <Controller name="conditionAfter" control={control} render={({ field }) => (
              <TextField {...field} select label="Condition on return" fullWidth>
                {CONDITIONS.map((c) => <MenuItem key={c} value={c}>{c}</MenuItem>)}
              </TextField>
            )} />
          </Grid>
          <Grid size={{ xs: 6 }}>
            <TextField label="Missing accessories" fullWidth {...register('missingAccessories')} />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <FormControlLabel control={
              <Controller name="damageDetected" control={control}
                          render={({ field }) => <Checkbox checked={field.value} onChange={field.onChange} />} />}
              label="Damage detected" />
          </Grid>
          {damageDetected && (
            <>
              <Grid size={{ xs: 12 }}>
                <TextField label="Damage description" fullWidth required multiline minRows={2}
                           {...register('damageDescription')} />
              </Grid>
              <Grid size={{ xs: 12 }}>
                <FormControlLabel control={
                  <Controller name="sendToMaintenance" control={control}
                              render={({ field }) => <Checkbox checked={field.value} onChange={field.onChange} />} />}
                  label="Send asset to maintenance" />
              </Grid>
            </>
          )}
          <Grid size={{ xs: 12 }}>
            <TextField label="Notes" fullWidth {...register('notes')} />
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button color="inherit" onClick={onClose}>Cancel</Button>
        <Button variant="contained" disabled={mutation.isPending}
                onClick={handleSubmit((values) => mutation.mutate(values))}>
          Complete return
        </Button>
      </DialogActions>
    </Dialog>
  );
}

function ExtendDialog({ checkout, onClose }: { checkout: Checkout | null; onClose: () => void }) {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();
  const [newDate, setNewDate] = useState('');
  useEffect(() => { if (checkout) setNewDate(localInputValue(48)); }, [checkout]);
  const mutation = useMutation({
    mutationFn: async () =>
      (await api.post(`/checkouts/${checkout!.id}/extend`, {
        newExpectedReturnAt: localInputToIso(newDate),
      })).data,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['checkouts'] });
      enqueueSnackbar('Return date extended', { variant: 'success' });
      onClose();
    },
    onError: (error) => enqueueSnackbar(errorMessage(error), { variant: 'error' }),
  });
  return (
    <Dialog open={!!checkout} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Extend return date — {checkout?.checkoutNumber}</DialogTitle>
      <DialogContent>
        <TextField label="New expected return" type="datetime-local" fullWidth sx={{ mt: 1 }}
                   InputLabelProps={{ shrink: true }}
                   value={newDate} onChange={(e) => setNewDate(e.target.value)} />
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button color="inherit" onClick={onClose}>Cancel</Button>
        <Button variant="contained" disabled={!newDate || mutation.isPending} onClick={() => mutation.mutate()}>
          Extend return date
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export default function CheckoutsPage() {
  const { hasPermission } = useAuth();
  const [statusFilter, setStatusFilter] = useState('');
  const [overdueOnly, setOverdueOnly] = useState(false);
  const [pagination, setPagination] = useState<GridPaginationModel>({ page: 0, pageSize: 20 });
  const [checkoutOpen, setCheckoutOpen] = useState(false);
  const [returning, setReturning] = useState<Checkout | null>(null);
  const [extending, setExtending] = useState<Checkout | null>(null);

  const params = {
    status: statusFilter || undefined,
    overdueOnly: overdueOnly || undefined,
    page: pagination.page,
    size: pagination.pageSize,
  };

  const query = useQuery({
    queryKey: ['checkouts', params],
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<Checkout>>>('/checkouts', { params })).data.data,
    placeholderData: keepPreviousData,
  });

  const canManage = hasPermission('CHECKOUT_MANAGE');

  const columns: GridColDef<Checkout>[] = [
    {
      field: 'checkoutNumber', headerName: 'Number', width: 125, sortable: false,
      renderCell: ({ row }) => <CodeTag>{row.checkoutNumber}</CodeTag>,
    },
    { field: 'assetName', headerName: 'Asset', flex: 1.2, minWidth: 160, sortable: false },
    { field: 'quantity', headerName: 'Qty', width: 60, sortable: false },
    { field: 'userName', headerName: 'Borrower', flex: 1, minWidth: 130, sortable: false },
    {
      field: 'checkedOutAt', headerName: 'Checked out', width: 150,
      valueFormatter: (value: string) => formatDateTime(value),
    },
    {
      field: 'expectedReturnAt', headerName: 'Due back', width: 150, sortable: false,
      valueFormatter: (value: string) => formatDateTime(value),
    },
    {
      field: 'status', headerName: 'Status', width: 130, sortable: false,
      renderCell: ({ row }) => <StatusChip value={row.status} />,
    },
    {
      field: 'daysOverdue', headerName: 'Overdue', width: 85, sortable: false,
      valueFormatter: (value: number) => (value > 0 ? `${value}d` : '—'),
    },
    ...(canManage ? [{
      field: 'actions', headerName: '', width: 220, sortable: false,
      renderCell: ({ row }: { row: Checkout }) => row.returnedAt ? null : (
        <Stack direction="row" spacing={0.5} sx={{ alignItems: 'center', height: '100%' }}>
          <Button size="small" startIcon={<KeyboardReturnIcon />}
                  onClick={(e) => { e.stopPropagation(); setReturning(row); }}>
            Return
          </Button>
          <Button size="small" color="inherit" startIcon={<MoreTimeIcon />}
                  onClick={(e) => { e.stopPropagation(); setExtending(row); }}>
            Extend
          </Button>
        </Stack>
      ),
    } satisfies GridColDef<Checkout>] : []),
  ];

  return (
    <Box>
      <PageHeader
        eyebrow="OPERATIONS"
        title="Check-Out & Returns"
        crumbs={[{ label: 'Check-Out & Returns' }]}
        subtitle="Issue approved reservations, record returns, track overdue items."
        actions={hasPermission('CHECKOUT_CREATE') && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCheckoutOpen(true)}>
            Check out
          </Button>
        )}
      />

      <Card variant="outlined" sx={{ p: 2, mb: 2 }}>
        <Stack direction="row" spacing={1.5} alignItems="center" flexWrap="wrap">
          <TextField select size="small" label="Status" value={statusFilter} sx={{ minWidth: 180 }}
                     onChange={(e) => setStatusFilter(e.target.value)}>
            <MenuItem value="">All</MenuItem>
            <MenuItem value="CHECKED_OUT">Checked out</MenuItem>
            <MenuItem value="OVERDUE">Overdue</MenuItem>
            <MenuItem value="RETURNED">Returned</MenuItem>
          </TextField>
          <Button variant={overdueOnly ? 'contained' : 'outlined'} color="error" size="small"
                  onClick={() => setOverdueOnly((v) => !v)}>
            Overdue only
          </Button>
        </Stack>
      </Card>

      <ServerDataGrid<Checkout>
        columns={columns}
        page={query.data}
        loading={query.isLoading || query.isFetching}
        paginationModel={pagination}
        onPaginationModelChange={setPagination}
        emptyTitle="No check-outs found"
        emptyHint="Approved reservations can be collected here with Check out."
      />

      <CheckoutDialog open={checkoutOpen} onClose={() => setCheckoutOpen(false)} />
      <ReturnDialog checkout={returning} onClose={() => setReturning(null)} />
      <ExtendDialog checkout={extending} onClose={() => setExtending(null)} />
    </Box>
  );
}
