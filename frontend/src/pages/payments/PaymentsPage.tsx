import { useEffect, useState } from 'react';
import {
  Box, Button, Card, Dialog, DialogActions, DialogContent, DialogTitle,
  Grid, MenuItem, Stack, TextField, Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import PriceCheckIcon from '@mui/icons-material/PriceCheck';
import ReceiptLongOutlinedIcon from '@mui/icons-material/ReceiptLongOutlined';
import ReplayIcon from '@mui/icons-material/Replay';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import { Controller, useForm } from 'react-hook-form';
import { useSnackbar } from 'notistack';
import { api, errorMessage } from '../../api/client';
import type { ApiEnvelope, Page } from '../../api/client';
import type { Payment } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import { useDepartments, useUserOptions } from '../../api/referenceData';
import InvoiceDialog from '../../components/common/InvoiceDialog';
import PageHeader from '../../components/common/PageHeader';
import StatusChip from '../../components/common/StatusChip';
import CodeTag from '../../components/common/CodeTag';
import ServerDataGrid from '../../components/tables/ServerDataGrid';
import { formatDateTime, formatMoney, titleCase } from '../../utils/format';

const TRANSACTION_TYPES = [
  'RESERVATION_FEE', 'EQUIPMENT_USAGE_FEE', 'LAB_SETUP_FEE', 'FACILITY_FEE', 'CONSUMABLE_CHARGE',
  'SECURITY_DEPOSIT', 'DAMAGE_CHARGE', 'LATE_PENALTY', 'INTERNAL_CHARGE', 'OTHER',
];
const METHODS = ['Cash', 'Card', 'Bank transfer', 'Internal transfer', 'Other'];

interface PaymentValues {
  transactionType: string;
  payerType: string;
  payerUserId: string;
  payerDepartmentId: string;
  payerName: string;
  description: string;
  amount: string;
  paymentMethod: string;
  referenceNumber: string;
  notes: string;
}

function PaymentDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();
  const { hasPermission } = useAuth();
  const users = useUserOptions('', hasPermission('USER_VIEW'));
  const departments = useDepartments();
  const { register, control, handleSubmit, reset, watch, formState: { errors } } = useForm<PaymentValues>({
    defaultValues: {
      transactionType: 'RESERVATION_FEE', payerType: 'USER', payerUserId: '', payerDepartmentId: '',
      payerName: '', description: '', amount: '', paymentMethod: 'Card', referenceNumber: '', notes: '',
    },
  });
  useEffect(() => { if (open) reset(); }, [open, reset]);
  const payerType = watch('payerType');

  const mutation = useMutation({
    mutationFn: async (values: PaymentValues) =>
      (await api.post<ApiEnvelope<Payment>>('/payments', {
        transactionType: values.transactionType,
        payerType: values.payerType,
        payerUserId: values.payerType === 'USER' ? values.payerUserId || null : null,
        payerDepartmentId: values.payerType === 'DEPARTMENT' ? values.payerDepartmentId || null : null,
        payerName: values.payerName || null,
        description: values.description || null,
        amount: Number(values.amount),
        paymentMethod: values.paymentMethod,
        referenceNumber: values.referenceNumber || null,
        notes: values.notes || null,
      })).data.data,
    onSuccess: (payment) => {
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      enqueueSnackbar(`Payment ${payment.transactionNumber} recorded`, { variant: 'success' });
      onClose();
    },
    onError: (error) => enqueueSnackbar(errorMessage(error), { variant: 'error' }),
  });

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Record payment or charge</DialogTitle>
      <DialogContent>
        <Grid container spacing={2} sx={{ mt: 0.25 }}>
          <Grid size={{ xs: 6 }}>
            <Controller name="transactionType" control={control} render={({ field }) => (
              <TextField {...field} select label="Transaction type" fullWidth>
                {TRANSACTION_TYPES.map((t) => <MenuItem key={t} value={t}>{titleCase(t)}</MenuItem>)}
              </TextField>
            )} />
          </Grid>
          <Grid size={{ xs: 6 }}>
            <Controller name="payerType" control={control} render={({ field }) => (
              <TextField {...field} select label="Payer type" fullWidth>
                {['USER', 'DEPARTMENT', 'FACULTY', 'EXTERNAL'].map((t) => (
                  <MenuItem key={t} value={t}>{titleCase(t)}</MenuItem>
                ))}
              </TextField>
            )} />
          </Grid>
          {payerType === 'USER' && (
            <Grid size={{ xs: 12 }}>
              <Controller name="payerUserId" control={control} render={({ field }) => (
                <TextField {...field} select label="Payer" fullWidth disabled={!users.data}>
                  <MenuItem value="">Not specified</MenuItem>
                  {(users.data ?? []).map((u) => (
                    <MenuItem key={u.id} value={u.id}>{u.fullName} ({u.universityId})</MenuItem>
                  ))}
                </TextField>
              )} />
            </Grid>
          )}
          {payerType === 'DEPARTMENT' && (
            <Grid size={{ xs: 12 }}>
              <Controller name="payerDepartmentId" control={control} render={({ field }) => (
                <TextField {...field} select label="Department" fullWidth>
                  {(departments.data ?? []).map((d) => <MenuItem key={d.id} value={d.id}>{d.name}</MenuItem>)}
                </TextField>
              )} />
            </Grid>
          )}
          {(payerType === 'FACULTY' || payerType === 'EXTERNAL') && (
            <Grid size={{ xs: 12 }}>
              <TextField label="Payer name" fullWidth {...register('payerName')} />
            </Grid>
          )}
          <Grid size={{ xs: 6 }}>
            <TextField label="Amount" type="number" fullWidth required
                       inputProps={{ min: 0.01, step: '0.01' }}
                       error={!!errors.amount} helperText={errors.amount?.message}
                       {...register('amount', { required: 'Amount is required' })} />
          </Grid>
          <Grid size={{ xs: 6 }}>
            <Controller name="paymentMethod" control={control} render={({ field }) => (
              <TextField {...field} select label="Payment method" fullWidth>
                {METHODS.map((m) => <MenuItem key={m} value={m}>{m}</MenuItem>)}
              </TextField>
            )} />
          </Grid>
          <Grid size={{ xs: 6 }}>
            <TextField label="Reference number" fullWidth {...register('referenceNumber')} />
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
          Record payment
        </Button>
      </DialogActions>
    </Dialog>
  );
}

function RefundDialog({ payment, onClose }: { payment: Payment | null; onClose: () => void }) {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();
  const [amount, setAmount] = useState('');
  const [reason, setReason] = useState('');
  useEffect(() => {
    if (payment) {
      setAmount(String(payment.amount - payment.refundedAmount));
      setReason('');
    }
  }, [payment]);

  const mutation = useMutation({
    mutationFn: async () =>
      (await api.post(`/payments/${payment!.id}/refund`, {
        amount: Number(amount), reason,
      })).data,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      enqueueSnackbar('Refund processed', { variant: 'success' });
      onClose();
    },
    onError: (error) => enqueueSnackbar(errorMessage(error), { variant: 'error' }),
  });

  const refundable = payment ? payment.amount - payment.refundedAmount : 0;

  return (
    <Dialog open={!!payment} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Refund {payment?.transactionNumber}</DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Refundable: <b>{formatMoney(refundable, payment?.currency)}</b>
        </Typography>
        <Stack spacing={2}>
          <TextField label="Refund amount" type="number" fullWidth
                     inputProps={{ min: 0.01, max: refundable, step: '0.01' }}
                     value={amount} onChange={(e) => setAmount(e.target.value)} />
          <TextField label="Refund reason" fullWidth required multiline minRows={2}
                     value={reason} onChange={(e) => setReason(e.target.value)} />
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button color="inherit" onClick={onClose}>Cancel</Button>
        <Button variant="contained" disabled={!amount || !reason || mutation.isPending}
                onClick={() => mutation.mutate()}>
          Process refund
        </Button>
      </DialogActions>
    </Dialog>
  );
}

interface MarkPaidValues {
  method: string;
  referenceNumber: string;
  notes: string;
}

function MarkPaidDialog({ payment, onClose }: { payment: Payment | null; onClose: () => void }) {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();
  const { register, control, handleSubmit, reset } = useForm<MarkPaidValues>({
    defaultValues: { method: 'Card', referenceNumber: '', notes: '' },
  });
  useEffect(() => { if (payment) reset(); }, [payment, reset]);

  const mutation = useMutation({
    mutationFn: async (values: MarkPaidValues) =>
      (await api.post(`/payments/${payment!.id}/mark-paid`, {
        method: values.method,
        referenceNumber: values.referenceNumber || null,
        notes: values.notes || null,
      })).data,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      enqueueSnackbar('Payment marked as paid', { variant: 'success' });
      onClose();
    },
    onError: (error) => enqueueSnackbar(errorMessage(error), { variant: 'error' }),
  });

  return (
    <Dialog open={!!payment} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Mark {payment?.transactionNumber} paid</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 0.25 }}>
          <Controller name="method" control={control} render={({ field }) => (
            <TextField {...field} select label="Payment method" fullWidth>
              {METHODS.map((m) => <MenuItem key={m} value={m}>{m}</MenuItem>)}
            </TextField>
          )} />
          <TextField label="Reference number" fullWidth {...register('referenceNumber')} />
          <TextField label="Notes" fullWidth multiline minRows={2} {...register('notes')} />
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button color="inherit" onClick={onClose}>Cancel</Button>
        <Button variant="contained" disabled={mutation.isPending}
                onClick={handleSubmit((values) => mutation.mutate(values))}>
          Mark paid
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export default function PaymentsPage() {
  const { hasPermission } = useAuth();
  const [typeFilter, setTypeFilter] = useState('');
  const [pagination, setPagination] = useState<GridPaginationModel>({ page: 0, pageSize: 20 });
  const [createOpen, setCreateOpen] = useState(false);
  const [refunding, setRefunding] = useState<Payment | null>(null);
  const [markingPaid, setMarkingPaid] = useState<Payment | null>(null);
  const [invoicing, setInvoicing] = useState<Payment | null>(null);

  const params = {
    transactionType: typeFilter || undefined,
    page: pagination.page,
    size: pagination.pageSize,
  };

  const query = useQuery({
    queryKey: ['payments', params],
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<Payment>>>('/payments', { params })).data.data,
    placeholderData: keepPreviousData,
  });

  // Without PAYMENT_VIEW the backend scopes the list to the user's own payments.
  const canViewAll = hasPermission('PAYMENT_VIEW');
  const canRefund = canViewAll && hasPermission('PAYMENT_REFUND');
  const canMarkPaid = canViewAll && hasPermission('PAYMENT_CREATE');

  const columns: GridColDef<Payment>[] = [
    {
      field: 'transactionNumber', headerName: 'Number', width: 125, sortable: false,
      renderCell: ({ row }) => <CodeTag>{row.transactionNumber}</CodeTag>,
    },
    {
      field: 'transactionType', headerName: 'Type', width: 170, sortable: false,
      valueFormatter: (value: string) => titleCase(value),
    },
    { field: 'payerDisplayName', headerName: 'Payer', flex: 1, minWidth: 140, sortable: false },
    {
      field: 'amount', headerName: 'Amount', width: 120, align: 'right', headerAlign: 'right',
      sortable: false,
      valueFormatter: (value: number, row) => formatMoney(value, row.currency),
    },
    { field: 'paymentMethod', headerName: 'Method', width: 120, sortable: false },
    {
      field: 'paymentDate', headerName: 'Date', width: 150,
      valueFormatter: (value: string) => formatDateTime(value),
    },
    {
      field: 'status', headerName: 'Status', width: 150, sortable: false,
      renderCell: ({ row }) => <StatusChip value={row.status} />,
    },
    {
      field: 'actions', headerName: '', sortable: false,
      width: 120 + (canMarkPaid ? 120 : 0) + (canRefund ? 110 : 0),
      renderCell: ({ row }: { row: Payment }) => (
        <Stack direction="row" spacing={0.5}>
          <Button size="small" startIcon={<ReceiptLongOutlinedIcon />}
                  onClick={(e) => { e.stopPropagation(); setInvoicing(row); }}>
            Invoice
          </Button>
          {canMarkPaid && row.status === 'PENDING' && (
            <Button size="small" startIcon={<PriceCheckIcon />}
                    onClick={(e) => { e.stopPropagation(); setMarkingPaid(row); }}>
              Mark paid
            </Button>
          )}
          {canRefund && row.transactionType !== 'REFUND' && row.amount - row.refundedAmount > 0 && (
            <Button size="small" startIcon={<ReplayIcon />}
                    onClick={(e) => { e.stopPropagation(); setRefunding(row); }}>
              Refund
            </Button>
          )}
        </Stack>
      ),
    },
  ];

  return (
    <Box>
      <PageHeader
        eyebrow="COMMERCE"
        title={canViewAll ? 'Payments & Charges' : 'My Payments'}
        crumbs={[{ label: canViewAll ? 'Payments & Charges' : 'My Payments' }]}
        subtitle={canViewAll
          ? 'Fees, deposits, damage charges, penalties and refunds.'
          : 'Your fees and charges. Click a payment to view its invoice.'}
        actions={canViewAll && hasPermission('PAYMENT_CREATE') && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
            Record payment
          </Button>
        )}
      />

      <Card variant="outlined" sx={{ p: 2, mb: 2 }}>
        <TextField select size="small" label="Transaction type" value={typeFilter} sx={{ minWidth: 220 }}
                   onChange={(e) => setTypeFilter(e.target.value)}>
          <MenuItem value="">All types</MenuItem>
          {[...TRANSACTION_TYPES, 'REFUND'].map((t) => (
            <MenuItem key={t} value={t}>{titleCase(t)}</MenuItem>
          ))}
        </TextField>
      </Card>

      <ServerDataGrid<Payment>
        columns={columns}
        page={query.data}
        loading={query.isLoading || query.isFetching}
        paginationModel={pagination}
        onPaginationModelChange={setPagination}
        onRowClick={(row) => setInvoicing(row)}
        emptyTitle={canViewAll ? 'No payments recorded' : 'No payments yet'}
        emptyHint={canViewAll
          ? 'Record fees, deposits and charges connected to reservations and assets.'
          : 'Fees and charges for your reservations will appear here.'}
      />

      <PaymentDialog open={createOpen} onClose={() => setCreateOpen(false)} />
      <RefundDialog payment={refunding} onClose={() => setRefunding(null)} />
      <MarkPaidDialog payment={markingPaid} onClose={() => setMarkingPaid(null)} />
      <InvoiceDialog payment={invoicing} onClose={() => setInvoicing(null)} />
    </Box>
  );
}
