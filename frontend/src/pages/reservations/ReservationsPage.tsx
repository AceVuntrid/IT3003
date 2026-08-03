import { useEffect, useState } from 'react';
import {
  Alert, Box, Button, Card, Chip, Dialog, DialogActions, DialogContent, DialogTitle,
  Grid, MenuItem, Stack, TextField, Tooltip, Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import CheckIcon from '@mui/icons-material/Check';
import CloseIcon from '@mui/icons-material/Close';
import DoDisturbOutlinedIcon from '@mui/icons-material/DoDisturbOutlined';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import { useForm } from 'react-hook-form';
import { useSnackbar } from 'notistack';
import { api, errorMessage } from '../../api/client';
import type { ApiEnvelope, Page } from '../../api/client';
import type { Reservation } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import PageHeader from '../../components/common/PageHeader';
import StatusChip from '../../components/common/StatusChip';
import CodeTag from '../../components/common/CodeTag';
import ServerDataGrid from '../../components/tables/ServerDataGrid';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import BookingDialog from '../../components/common/BookingDialog';
import { statusColors } from '../../theme';
import { formatDateTime, formatMoney } from '../../utils/format';

const STATUS_OPTIONS = [
  'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'CANCELLED', 'CHECKED_OUT', 'COMPLETED', 'OVERDUE',
];

/** Item name regardless of reservation kind: equipment, venue or consumable. */
function reservationItemName(r: Reservation | null | undefined): string {
  return r?.assetName ?? r?.locationName ?? r?.consumableItemName ?? '—';
}

/**
 * The list/detail endpoints include the pending approver (who the request is
 * waiting on) for PENDING_APPROVAL rows. Typed locally until it lands in the
 * shared Reservation type.
 */
type ReservationRow = Reservation & {
  pendingApprover?: { name: string; role: string } | null;
};

interface ApproveValues {
  approvedQuantity: string;
  notes: string;
}

function ApproveReservationDialog({ reservation, onClose }: {
  reservation: Reservation | null;
  onClose: () => void;
}) {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();
  // Validated form values held while the confirmation dialog is open.
  const [confirmValues, setConfirmValues] = useState<ApproveValues | null>(null);
  const { register, handleSubmit, reset, watch, formState: { errors } } = useForm<ApproveValues>({
    defaultValues: { approvedQuantity: '1', notes: '' },
  });
  useEffect(() => {
    if (reservation) {
      reset({ approvedQuantity: String(reservation.quantity), notes: '' });
    }
    setConfirmValues(null);
  }, [reservation, reset]);

  // Venue bookings are whole-room: there is no quantity to trim at approval.
  const isVenue = !!reservation?.locationId;
  const requested = reservation?.requestedQuantity ?? reservation?.quantity ?? 1;
  const maxApprovable = reservation?.quantity ?? 1;
  // The fee is charged at the FINAL approval; level 1 of a two-hop flow only verifies.
  const isFinal = !reservation
    || !reservation.requiredApprovalTier
    || reservation.requiredApprovalTier === 'TIER_1_OFFICER'
    || reservation.currentApprovalStep === 'PENDING_LEVEL_2';

  // Read-only price-list preview. applicableFee = unit fee x current quantity for
  // consumables (flat otherwise), so scale it live when the approver reduces the
  // quantity — the backend recomputes from the price list at final approval.
  const approvedQuantityValue = Number(watch('approvedQuantity'));
  const applicableFee = reservation?.applicableFee ?? null;
  const previewFee = (() => {
    if (!reservation || applicableFee == null || applicableFee <= 0) return null;
    if (reservation.consumableItemId && reservation.quantity > 0
        && Number.isInteger(approvedQuantityValue)
        && approvedQuantityValue >= 1 && approvedQuantityValue <= maxApprovable) {
      return (applicableFee / reservation.quantity) * approvedQuantityValue;
    }
    return applicableFee;
  })();

  const mutation = useMutation({
    mutationFn: async (values: ApproveValues) => {
      if (!reservation) return null;
      return (await api.post<ApiEnvelope<Reservation>>(`/reservations/${reservation.id}/approve`, {
        notes: values.notes || null,
        approvedQuantity: isVenue ? null : Number(values.approvedQuantity),
      })).data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reservations'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      enqueueSnackbar(isFinal ? 'Reservation approved'
        : 'Level 1 approval recorded — awaiting final approval', { variant: 'success' });
      setConfirmValues(null);
      onClose();
    },
    onError: (error) => {
      enqueueSnackbar(errorMessage(error), { variant: 'error' });
      // Back to the form so the approver can adjust and retry.
      setConfirmValues(null);
    },
  });

  return (
    <Dialog open={!!reservation} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Approve {reservation?.reservationNumber}</DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          {reservationItemName(reservation)} ·{' '}
          {reservation && formatDateTime(reservation.startAt)} →{' '}
          {reservation && formatDateTime(reservation.endAt)}
          <br />Purpose: {reservation?.purpose}
        </Typography>
        <Grid container spacing={2} sx={{ mt: 0 }}>
          {!isVenue && (
            <Grid size={{ xs: 12 }}>
              <TextField label="Approved quantity" type="number" fullWidth autoFocus
                         inputProps={{ min: 1, max: maxApprovable }}
                         error={!!errors.approvedQuantity}
                         helperText={errors.approvedQuantity?.message ?? `Requested: ${requested}`}
                         {...register('approvedQuantity', {
                           required: 'Approved quantity is required',
                           validate: (value) => {
                             const n = Number(value);
                             return (Number.isInteger(n) && n >= 1 && n <= maxApprovable)
                               || `Enter a whole number between 1 and ${maxApprovable}`;
                           },
                         })} />
            </Grid>
          )}
          <Grid size={{ xs: 12 }}>
            <Typography variant="body2" sx={{ fontWeight: 600 }}>
              Fee: {previewFee != null ? `${formatMoney(previewFee)} (from price list)` : 'Free'}
            </Typography>
          </Grid>
          {!isFinal && (
            <Grid size={{ xs: 12 }}>
              <Alert severity="info">
                Level 1 verification — the price-list fee is applied at the final
                (Level 2) approval.
              </Alert>
            </Grid>
          )}
          <Grid size={{ xs: 12 }}>
            <TextField label="Conditions or notes" fullWidth multiline minRows={2}
                       {...register('notes')} />
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button color="inherit" onClick={onClose}>Cancel</Button>
        <Button variant="contained" disabled={mutation.isPending}
                onClick={handleSubmit((values) => setConfirmValues(values))}>
          Approve
        </Button>
      </DialogActions>
      {/* Final check before committing: the fee shown is the price-list preview
          for the quantity being approved. */}
      <ConfirmDialog
        open={!!confirmValues}
        title={`Approve ${reservation?.reservationNumber}`}
        message={`Approve ${reservation?.reservationNumber} — ${reservationItemName(reservation)}`
          + (isVenue ? '' : ` ×${Number(confirmValues?.approvedQuantity) || maxApprovable}`)
          + ` for ${reservation?.requestedByName}?`
          + ` Fee: ${previewFee != null ? formatMoney(previewFee) : 'Free'} (from price list).`
          + ' The requester and administrators can cancel until collection.'}
        confirmLabel="Approve"
        busy={mutation.isPending}
        onConfirm={() => confirmValues && mutation.mutate(confirmValues)}
        onClose={() => setConfirmValues(null)}
      />
    </Dialog>
  );
}

export default function ReservationsPage() {
  const { hasPermission, hasRole, user } = useAuth();
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();
  const [status, setStatus] = useState('');
  const [mineOnly, setMineOnly] = useState(false);
  const [pagination, setPagination] = useState<GridPaginationModel>({ page: 0, pageSize: 20 });
  const [createOpen, setCreateOpen] = useState(false);
  const [approveTarget, setApproveTarget] = useState<Reservation | null>(null);
  const [rejectTarget, setRejectTarget] = useState<Reservation | null>(null);
  const [rejectNotes, setRejectNotes] = useState('');
  const [cancelTarget, setCancelTarget] = useState<Reservation | null>(null);

  const params = {
    status: status || undefined,
    mineOnly: mineOnly || undefined,
    page: pagination.page,
    size: pagination.pageSize,
  };

  const query = useQuery({
    queryKey: ['reservations', params],
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<Reservation>>>('/reservations', { params })).data.data,
    placeholderData: keepPreviousData,
  });

  const rejectMutation = useMutation({
    mutationFn: async (reservation: Reservation) =>
      (await api.post(`/reservations/${reservation.id}/reject`, { notes: rejectNotes || null })).data,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reservations'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      enqueueSnackbar('Reservation rejected', { variant: 'success' });
      setRejectTarget(null);
      setRejectNotes('');
    },
    onError: (error) => enqueueSnackbar(errorMessage(error), { variant: 'error' }),
  });

  const cancelMutation = useMutation({
    mutationFn: async (reservation: Reservation) =>
      (await api.post(`/reservations/${reservation.id}/cancel`)).data,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reservations'] });
      enqueueSnackbar('Reservation cancelled', { variant: 'success' });
      setCancelTarget(null);
    },
    onError: (error) => enqueueSnackbar(errorMessage(error), { variant: 'error' }),
  });

  const canApprove = hasPermission('RESERVATION_APPROVE');
  // Mirrors the backend cancel gate (owner OR RESERVATION_MANAGE OR SUPER_ADMIN).
  // The button is a best-effort hint — the backend is authoritative and any
  // 403/400 it returns surfaces through the mutation's error snackbar.
  const canManageReservations = hasPermission('RESERVATION_MANAGE') || hasRole('SUPER_ADMIN');

  const columns: GridColDef<Reservation>[] = [
    {
      field: 'reservationNumber', headerName: 'Number', width: 130, sortable: false,
      renderCell: ({ row }) => <CodeTag>{row.reservationNumber}</CodeTag>,
    },
    {
      field: 'assetName', headerName: 'Item / facility', flex: 1.2, minWidth: 170, sortable: false,
      valueGetter: (_, row) => reservationItemName(row),
      // The collection code is only present when the viewer is the requester —
      // the server strips it for everyone else.
      renderCell: ({ row }) => (
        <Stack spacing={0.25} sx={{ height: '100%', justifyContent: 'center' }}>
          <Typography variant="body2">{reservationItemName(row)}</Typography>
          {row.collectionCode && (
            <Tooltip title="Give this code to staff when collecting">
              <Box component="span" sx={{ lineHeight: 1 }}>
                <CodeTag>{`Collection code: ${row.collectionCode}`}</CodeTag>
              </Box>
            </Tooltip>
          )}
        </Stack>
      ),
    },
    { field: 'requestedByName', headerName: 'Requested by', flex: 1, minWidth: 140, sortable: false },
    {
      field: 'startAt', headerName: 'From', width: 155,
      valueFormatter: (value: string) => formatDateTime(value),
    },
    {
      field: 'endAt', headerName: 'To', width: 155, sortable: false,
      valueFormatter: (value: string) => formatDateTime(value),
    },
    {
      field: 'quantity', headerName: 'Qty', width: 105, sortable: false,
      renderCell: ({ row }) => {
        const requested = row.requestedQuantity ?? row.quantity;
        return (
          <Stack spacing={0} sx={{ height: '100%', justifyContent: 'center' }}>
            <Typography variant="body2">
              {row.quantity}{row.consumableUnit ? ` ${row.consumableUnit}` : ''}
            </Typography>
            {requested > row.quantity && (
              <Typography variant="caption" color="text.secondary">
                of {requested} requested
              </Typography>
            )}
          </Stack>
        );
      },
    },
    {
      field: 'feeAmount', headerName: 'Fee', width: 110, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" sx={{ height: '100%', alignItems: 'center' }}>
          {row.feeWaived ? (
            <Chip label="Free" size="small"
                  sx={{ backgroundColor: statusColors.APPROVED.bg, color: statusColors.APPROVED.fg,
                        fontWeight: 600 }} />
          ) : row.feeAmount != null ? (
            <Typography variant="body2">{formatMoney(row.feeAmount)}</Typography>
          ) : (
            <Typography variant="body2" color="text.secondary">—</Typography>
          )}
        </Stack>
      ),
    },
    {
      field: 'status', headerName: 'Status', width: 185, sortable: false,
      renderCell: ({ row }) => {
        // Tell the requester who their pending request is waiting on.
        const pendingApprover = row.status === 'PENDING_APPROVAL' && row.requestedById === user?.id
          ? (row as ReservationRow).pendingApprover
          : null;
        const awaiting = pendingApprover
          ? `Awaiting approval — ${pendingApprover.role} (${pendingApprover.name})`
          : null;
        return (
          <Stack spacing={0.25}
                 sx={{ height: '100%', justifyContent: 'center', minWidth: 0, width: '100%' }}>
            <Box><StatusChip value={row.status} /></Box>
            {awaiting && (
              <Tooltip title={awaiting}>
                <Typography variant="caption" color="text.secondary" noWrap
                            sx={{ maxWidth: '100%' }}>
                  {awaiting}
                </Typography>
              </Tooltip>
            )}
          </Stack>
        );
      },
    },
    {
      field: 'actions', headerName: '', width: canApprove ? 290 : 120, sortable: false,
      renderCell: ({ row }) => {
        // Requesters may cancel their own rows up to collection; managers and
        // super admins additionally see Cancel on other users' rows and on
        // CHECKED_OUT ones (the backend closes those once all items are back,
        // or rejects while items are still out).
        const ownRow = row.requestedById === user?.id;
        const showCancel = canManageReservations
          ? ['PENDING_APPROVAL', 'SUBMITTED', 'APPROVED', 'READY_FOR_COLLECTION', 'CHECKED_OUT']
            .includes(row.status)
          : ownRow && ['PENDING_APPROVAL', 'SUBMITTED', 'APPROVED', 'READY_FOR_COLLECTION']
            .includes(row.status);
        return (
          <Stack direction="row" spacing={0.5} sx={{ alignItems: 'center', height: '100%' }}>
            {canApprove && (row.status === 'PENDING_APPROVAL' || row.status === 'SUBMITTED') && (
              <>
                <Button size="small" color="primary" startIcon={<CheckIcon />}
                        onClick={(e) => { e.stopPropagation(); setApproveTarget(row); }}>
                  Approve
                </Button>
                <Button size="small" color="error" startIcon={<CloseIcon />}
                        onClick={(e) => { e.stopPropagation(); setRejectTarget(row); }}>
                  Reject
                </Button>
              </>
            )}
            {showCancel && (
              <Button size="small" color="inherit" startIcon={<DoDisturbOutlinedIcon />}
                      onClick={(e) => { e.stopPropagation(); setCancelTarget(row); }}>
                Cancel
              </Button>
            )}
          </Stack>
        );
      },
    },
  ];

  return (
    <Box>
      <PageHeader
        eyebrow="OPERATIONS"
        title="Reservations"
        crumbs={[{ label: 'Reservations' }]}
        subtitle="Equipment, venue and consumable bookings with approval workflow."
        actions={hasPermission('RESERVATION_CREATE') && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
            New reservation
          </Button>
        )}
      />

      <Card variant="outlined" sx={{ p: 2, mb: 2 }}>
        <Stack direction="row" spacing={1.5} alignItems="center" flexWrap="wrap">
          <TextField select size="small" label="Status" value={status} sx={{ minWidth: 200 }}
                     onChange={(e) => { setStatus(e.target.value); setPagination((p) => ({ ...p, page: 0 })); }}>
            <MenuItem value="">All statuses</MenuItem>
            {STATUS_OPTIONS.map((s) => (
              <MenuItem key={s} value={s}>{s.replaceAll('_', ' ')}</MenuItem>
            ))}
          </TextField>
          <Button variant={mineOnly ? 'contained' : 'outlined'} size="small"
                  onClick={() => setMineOnly((v) => !v)}>
            My reservations
          </Button>
        </Stack>
      </Card>

      <ServerDataGrid<Reservation>
        columns={columns}
        page={query.data}
        loading={query.isLoading || query.isFetching}
        paginationModel={pagination}
        onPaginationModelChange={setPagination}
        emptyTitle="No reservations found"
        emptyHint="Create a reservation to book equipment, a venue or reserve consumables."
      />

      <BookingDialog open={createOpen} onClose={() => setCreateOpen(false)} />

      <ApproveReservationDialog reservation={approveTarget} onClose={() => setApproveTarget(null)} />

      <Dialog open={!!rejectTarget} onClose={() => setRejectTarget(null)} maxWidth="xs" fullWidth>
        <DialogTitle>Reject {rejectTarget?.reservationNumber}</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            {reservationItemName(rejectTarget)} ·{' '}
            {rejectTarget && formatDateTime(rejectTarget.startAt)} →{' '}
            {rejectTarget && formatDateTime(rejectTarget.endAt)}
            <br />Purpose: {rejectTarget?.purpose}
          </Typography>
          <TextField label="Reason" fullWidth multiline minRows={2} value={rejectNotes}
                     onChange={(e) => setRejectNotes(e.target.value)} />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button color="inherit" onClick={() => setRejectTarget(null)}>Cancel</Button>
          <Button variant="contained" color="error"
                  disabled={rejectMutation.isPending}
                  onClick={() => rejectTarget && rejectMutation.mutate(rejectTarget)}>
            Reject
          </Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog
        open={!!cancelTarget}
        title="Cancel reservation"
        message={`Cancel ${cancelTarget?.reservationNumber}? The reserved capacity is released immediately.`}
        confirmLabel="Cancel reservation"
        destructive
        busy={cancelMutation.isPending}
        onConfirm={() => cancelTarget && cancelMutation.mutate(cancelTarget)}
        onClose={() => setCancelTarget(null)}
      />
    </Box>
  );
}
