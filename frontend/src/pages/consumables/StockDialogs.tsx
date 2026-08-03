import { useEffect } from 'react';
import {
  Alert, Button, Checkbox, Dialog, DialogActions, DialogContent, DialogTitle,
  FormControlLabel, Grid, MenuItem, TextField, Typography,
} from '@mui/material';
import { Controller, useForm } from 'react-hook-form';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSnackbar } from 'notistack';
import { api, errorMessage } from '../../api/client';
import type { ApiEnvelope, Page } from '../../api/client';
import type { ConsumableDetail, Reservation } from '../../api/types';
import { useDepartments, useUserOptions } from '../../api/referenceData';
import { useAuth } from '../../auth/AuthContext';

function useStockMutation(itemId: string, path: string, message: string, onDone: () => void) {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();
  return useMutation({
    mutationFn: async (payload: unknown) => (await api.post(`/consumables/${itemId}/${path}`, payload)).data,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['consumables'] });
      queryClient.invalidateQueries({ queryKey: ['consumable', itemId] });
      enqueueSnackbar(message, { variant: 'success' });
      onDone();
    },
    onError: (error) => enqueueSnackbar(errorMessage(error), { variant: 'error' }),
  });
}

// ---------------------------------------------------------------- Receive
interface ReceiveValues {
  quantity: string;
  batchNumber: string;
  purchaseOrderNumber: string;
  invoiceNumber: string;
  unitCost: string;
  manufactureDate: string;
  expiryDate: string;
  receivedDate: string;
  notes: string;
}

export function ReceiveStockDialog({ open, onClose, item }: {
  open: boolean; onClose: () => void; item: ConsumableDetail;
}) {
  const { register, handleSubmit, reset, formState: { errors } } = useForm<ReceiveValues>({
    defaultValues: {
      quantity: '', batchNumber: '', purchaseOrderNumber: '', invoiceNumber: '',
      unitCost: '', manufactureDate: '', expiryDate: '', receivedDate: '', notes: '',
    },
  });
  useEffect(() => { if (open) reset(); }, [open, reset]);
  const mutation = useStockMutation(item.id, 'receive', 'Stock received', onClose);

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Receive stock — {item.name}</DialogTitle>
      <DialogContent>
        <Grid container spacing={2} sx={{ mt: 0.25 }}>
          <Grid size={{ xs: 6 }}>
            <TextField label={`Quantity (${item.unitOfMeasure})`} type="number" fullWidth required autoFocus
                       inputProps={{ min: 0.001, step: '0.001' }}
                       error={!!errors.quantity} helperText={errors.quantity?.message}
                       {...register('quantity', { required: 'Quantity is required' })} />
          </Grid>
          <Grid size={{ xs: 6 }}>
            <TextField label="Batch number" fullWidth required
                       error={!!errors.batchNumber} helperText={errors.batchNumber?.message}
                       {...register('batchNumber', { required: 'Batch number is required' })} />
          </Grid>
          <Grid size={{ xs: 6 }}>
            <TextField label="Unit cost" type="number" fullWidth inputProps={{ min: 0, step: '0.01' }}
                       {...register('unitCost')} />
          </Grid>
          <Grid size={{ xs: 6 }}>
            <TextField label="Purchase order no." fullWidth {...register('purchaseOrderNumber')} />
          </Grid>
          <Grid size={{ xs: 6 }}>
            <TextField label="Invoice no." fullWidth {...register('invoiceNumber')} />
          </Grid>
          <Grid size={{ xs: 4 }}>
            <TextField label="Manufacture date" type="date" fullWidth InputLabelProps={{ shrink: true }}
                       {...register('manufactureDate')} />
          </Grid>
          <Grid size={{ xs: 4 }}>
            <TextField label="Expiry date" type="date" fullWidth InputLabelProps={{ shrink: true }}
                       {...register('expiryDate')} />
          </Grid>
          <Grid size={{ xs: 4 }}>
            <TextField label="Received date" type="date" fullWidth InputLabelProps={{ shrink: true }}
                       {...register('receivedDate')} />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <TextField label="Notes" fullWidth {...register('notes')} />
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button color="inherit" onClick={onClose}>Cancel</Button>
        <Button variant="contained" disabled={mutation.isPending}
                onClick={handleSubmit((v) => mutation.mutate({
                  quantity: Number(v.quantity),
                  batchNumber: v.batchNumber,
                  purchaseOrderNumber: v.purchaseOrderNumber || null,
                  invoiceNumber: v.invoiceNumber || null,
                  unitCost: v.unitCost ? Number(v.unitCost) : null,
                  manufactureDate: v.manufactureDate || null,
                  expiryDate: v.expiryDate || null,
                  receivedDate: v.receivedDate || null,
                  notes: v.notes || null,
                }))}>
          Confirm receipt
        </Button>
      </DialogActions>
    </Dialog>
  );
}

// ------------------------------------------------------------------- Issue
interface IssueValues {
  quantity: string;
  reservationId: string;
  collectionCode: string;
  issuedToUserId: string;
  departmentId: string;
  courseOrProject: string;
  purpose: string;
  chargeable: boolean;
  chargeAmount: string;
  notes: string;
}

/** Units still collectable on an approved consumable reservation. */
function remainingOn(r: Reservation): number {
  return Math.max(0, (r.quantity ?? 0) - (r.issuedQuantity ?? 0));
}

export function IssueStockDialog({ open, onClose, item }: {
  open: boolean; onClose: () => void; item: ConsumableDetail;
}) {
  const { hasPermission } = useAuth();
  const queryClient = useQueryClient();
  const users = useUserOptions('', hasPermission('USER_VIEW'));
  const departments = useDepartments();
  const { register, control, handleSubmit, reset, watch, setValue, formState: { errors } } =
    useForm<IssueValues>({
      defaultValues: {
        quantity: '', reservationId: '', collectionCode: '', issuedToUserId: '', departmentId: '',
        courseOrProject: '', purpose: '', chargeable: false, chargeAmount: '', notes: '',
      },
    });
  useEffect(() => { if (open) reset(); }, [open, reset]);
  const chargeable = watch('chargeable');
  const reservationId = watch('reservationId');

  // Approved consumable reservations awaiting collection for THIS item. The
  // list endpoint has no consumableItemId filter, so filter client-side.
  const reservationsQuery = useQuery({
    queryKey: ['approved-consumable-reservations', item.id],
    enabled: open && hasPermission('RESERVATION_VIEW'),
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<Reservation>>>('/reservations', {
        params: { status: 'APPROVED', size: 200, sort: 'startAt,asc' },
      })).data.data.content.filter((r) => r.consumableItemId === item.id && remainingOn(r) > 0),
  });
  const reservations = reservationsQuery.data ?? [];
  const selectedReservation = reservations.find((r) => r.id === reservationId);

  const mutation = useStockMutation(item.id, 'issue', 'Stock issued', () => {
    // Reservation-linked issues change reservation state (COMPLETED when
    // fully issued), so refresh reservation lists too.
    queryClient.invalidateQueries({ queryKey: ['reservations'] });
    onClose();
  });

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Issue stock — {item.name}</DialogTitle>
      <DialogContent>
        <Alert severity="info" sx={{ mb: 2, mt: 0.5 }}>
          Available: <b>{item.availableQuantity} {item.unitOfMeasure}</b>.
          Batches are issued earliest-expiry first; expired batches are never issued.
        </Alert>
        <Grid container spacing={2}>
          {reservations.length > 0 && (
            <Grid size={{ xs: 12 }}>
              <Controller name="reservationId" control={control} render={({ field }) => (
                <TextField {...field} select label="Against reservation" fullWidth
                           helperText={selectedReservation
                             ? `Approved for ${selectedReservation.requestedByName} — `
                               + `${remainingOn(selectedReservation)} ${item.unitOfMeasure} left to issue`
                             : 'Optional — fulfil an approved reservation for this item'}
                           onChange={(event) => {
                             field.onChange(event);
                             const chosen = reservations.find((r) => r.id === event.target.value);
                             if (chosen) {
                               setValue('quantity', String(remainingOn(chosen)));
                               // Price-list fee is charged via the reservation —
                               // no manual charge on linked issues.
                               setValue('chargeable', false);
                               setValue('chargeAmount', '');
                             }
                             setValue('collectionCode', '');
                           }}>
                  <MenuItem value="">No linked reservation</MenuItem>
                  {reservations.map((r) => (
                    <MenuItem key={r.id} value={r.id}>
                      {r.reservationNumber} — {r.requestedByName} ({remainingOn(r)} {item.unitOfMeasure} left)
                    </MenuItem>
                  ))}
                </TextField>
              )} />
            </Grid>
          )}
          {reservationId && (
            <Grid size={{ xs: 6 }}>
              <TextField label="Collection code" fullWidth
                         inputProps={{ inputMode: 'numeric', maxLength: 4 }}
                         error={!!errors.collectionCode}
                         helperText={errors.collectionCode?.message
                           ?? 'Ask the requester for their 4-digit code — blank only if the reservation predates codes'}
                         {...register('collectionCode', {
                           // Blank is allowed and arbitrated server-side:
                           // pre-code reservations are only accepted with a
                           // blank code (grace path).
                           validate: (value) => !reservationId || /^(\d{4})?$/.test(value)
                             || 'Enter the 4-digit collection code',
                         })} />
            </Grid>
          )}
          <Grid size={{ xs: 6 }}>
            <TextField label={`Quantity (${item.unitOfMeasure})`} type="number" fullWidth required
                       autoFocus={reservations.length === 0}
                       inputProps={{ min: 0.001, step: '0.001' }}
                       error={!!errors.quantity}
                       helperText={errors.quantity?.message
                         ?? (selectedReservation
                           ? `Capped at the reservation's remaining ${remainingOn(selectedReservation)}`
                           : undefined)}
                       {...register('quantity', { required: 'Quantity is required' })} />
          </Grid>
          <Grid size={{ xs: 6 }}>
            <Controller name="issuedToUserId" control={control} render={({ field }) => (
              <TextField {...field} select label="Issued to" fullWidth disabled={!users.data}>
                <MenuItem value="">Not specified</MenuItem>
                {(users.data ?? []).map((u) => (
                  <MenuItem key={u.id} value={u.id}>{u.fullName} ({u.universityId})</MenuItem>
                ))}
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
          <Grid size={{ xs: 6 }}>
            <TextField label="Course or project" fullWidth {...register('courseOrProject')} />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <TextField label="Purpose" fullWidth {...register('purpose')} />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <FormControlLabel control={
              <Controller name="chargeable" control={control}
                          render={({ field }) => <Checkbox checked={field.value} onChange={field.onChange}
                                                           disabled={!!reservationId} />} />}
              label="Chargeable to the recipient or department" />
            {reservationId && (
              <Typography variant="caption" color="text.secondary" display="block">
                Reservation issues are charged from the price list — no manual charge here.
              </Typography>
            )}
          </Grid>
          {chargeable && !reservationId && (
            <Grid size={{ xs: 6 }}>
              <TextField label="Charge amount" type="number" fullWidth inputProps={{ min: 0, step: '0.01' }}
                         {...register('chargeAmount')} />
            </Grid>
          )}
          <Grid size={{ xs: 12 }}>
            <TextField label="Notes" fullWidth {...register('notes')} />
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button color="inherit" onClick={onClose}>Cancel</Button>
        <Button variant="contained" disabled={mutation.isPending}
                onClick={handleSubmit((v) => mutation.mutate({
                  quantity: Number(v.quantity),
                  reservationId: v.reservationId || null,
                  collectionCode: v.reservationId ? v.collectionCode : null,
                  issuedToUserId: v.issuedToUserId || null,
                  departmentId: v.departmentId || null,
                  courseOrProject: v.courseOrProject || null,
                  purpose: v.purpose || null,
                  // Reservation-linked issues are charged via the price list.
                  chargeable: v.reservationId ? false : v.chargeable,
                  chargeAmount: !v.reservationId && v.chargeable && v.chargeAmount
                    ? Number(v.chargeAmount) : null,
                  notes: v.notes || null,
                }))}>
          Issue stock
        </Button>
      </DialogActions>
    </Dialog>
  );
}

// ------------------------------------------------------------------ Adjust
interface AdjustValues {
  adjustmentType: 'INCREASE' | 'DECREASE';
  quantity: string;
  reason: string;
  approvalReference: string;
  notes: string;
}

export function AdjustStockDialog({ open, onClose, item }: {
  open: boolean; onClose: () => void; item: ConsumableDetail;
}) {
  const { register, control, handleSubmit, reset, formState: { errors } } = useForm<AdjustValues>({
    defaultValues: { adjustmentType: 'DECREASE', quantity: '', reason: '', approvalReference: '', notes: '' },
  });
  useEffect(() => { if (open) reset(); }, [open, reset]);
  const mutation = useStockMutation(item.id, 'adjust', 'Stock adjusted', onClose);

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Stock adjustment — {item.name}</DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2, mt: 0.5 }}>
          Current quantity: <b>{item.currentQuantity} {item.unitOfMeasure}</b>. All adjustments are logged.
        </Typography>
        <Grid container spacing={2}>
          <Grid size={{ xs: 6 }}>
            <Controller name="adjustmentType" control={control} render={({ field }) => (
              <TextField {...field} select label="Adjustment" fullWidth>
                <MenuItem value="INCREASE">Increase</MenuItem>
                <MenuItem value="DECREASE">Decrease</MenuItem>
              </TextField>
            )} />
          </Grid>
          <Grid size={{ xs: 6 }}>
            <TextField label="Quantity" type="number" fullWidth required
                       inputProps={{ min: 0.001, step: '0.001' }}
                       error={!!errors.quantity} helperText={errors.quantity?.message}
                       {...register('quantity', { required: 'Quantity is required' })} />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <Controller name="reason" control={control} rules={{ required: 'Reason is required' }}
                        render={({ field, fieldState }) => (
              <TextField {...field} select label="Reason" fullWidth required
                         error={!!fieldState.error} helperText={fieldState.error?.message}>
                {['Stock count correction', 'Damage', 'Expiry', 'Disposal', 'Data correction', 'Other']
                  .map((r) => <MenuItem key={r} value={r}>{r}</MenuItem>)}
              </TextField>
            )} />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <TextField label="Approval reference" fullWidth {...register('approvalReference')} />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <TextField label="Notes" fullWidth multiline minRows={2} {...register('notes')} />
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button color="inherit" onClick={onClose}>Cancel</Button>
        <Button variant="contained" disabled={mutation.isPending}
                onClick={handleSubmit((v) => mutation.mutate({
                  adjustmentType: v.adjustmentType,
                  quantity: Number(v.quantity),
                  reason: v.reason,
                  approvalReference: v.approvalReference || null,
                  notes: v.notes || null,
                }))}>
          Submit adjustment
        </Button>
      </DialogActions>
    </Dialog>
  );
}
