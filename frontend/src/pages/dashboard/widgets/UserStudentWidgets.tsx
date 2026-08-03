import { Link as RouterLink } from 'react-router-dom';
import {
  Card, CardHeader, CardContent, Divider, List, ListItem, ListItemText,
  Typography, Stack, Button, Chip, Grid
} from '@mui/material';
import EventAvailableOutlinedIcon from '@mui/icons-material/EventAvailableOutlined';
import OutputOutlinedIcon from '@mui/icons-material/OutputOutlined';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import SearchIcon from '@mui/icons-material/Search';
import ReceiptLongOutlinedIcon from '@mui/icons-material/ReceiptLongOutlined';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../../api/client';
import type { ApiEnvelope, Page } from '../../../api/client';
import type { Checkout, Payment, Reservation } from '../../../api/types';
import { useAuth } from '../../../auth/AuthContext';
import CodeTag from '../../../components/common/CodeTag';
import StatusChip from '../../../components/common/StatusChip';
import { formatDateTime, formatMoney } from '../../../utils/format';

export default function UserStudentWidgets() {
  const { user, hasPermission } = useAuth();
  const canViewCheckouts = hasPermission('CHECKOUT_VIEW');
  const canViewPayments = hasPermission('PAYMENT_VIEW');

  // The checkouts API returns a page of ALL checkouts to any CHECKOUT_VIEW
  // holder, so scope explicitly to the viewer. Students without CHECKOUT_VIEW
  // cannot call it at all — the card is hidden for them.
  const myCheckoutsQuery = useQuery({
    queryKey: ['dashboard', 'my-checkouts', user?.id],
    enabled: canViewCheckouts && !!user,
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<Checkout>>>('/checkouts', {
        params: { status: 'CHECKED_OUT', userId: user!.id, size: 5 },
      })).data.data.content,
  });

  const myReservationsQuery = useQuery({
    queryKey: ['dashboard', 'my-reservations'],
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<Reservation>>>('/reservations', {
        params: { mineOnly: true, size: 5 },
      })).data.data.content,
  });

  // Own unpaid fees: the payments API returns only the viewer's payments for
  // users without PAYMENT_VIEW.
  const paymentsDueQuery = useQuery({
    queryKey: ['dashboard', 'my-payments-due'],
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<Payment>>>('/payments', {
        params: { status: 'PENDING', size: 5 },
      })).data.data.content,
  });

  const checkouts = myCheckoutsQuery.data ?? [];
  const reservations = myReservationsQuery.data ?? [];
  const paymentsDue = paymentsDueQuery.data ?? [];

  return (
    <Grid container spacing={2}>
      {canViewCheckouts && (
      <Grid size={{ xs: 12, md: 6 }}>
        <Card variant="outlined" sx={{ height: '100%' }}>
          <CardHeader
            title="My Active Loans & Checked-out Equipment"
            titleTypographyProps={{ variant: 'h6' }}
            avatar={<OutputOutlinedIcon color="primary" />}
            action={
              <Button component={RouterLink} to="/checkouts" size="small" endIcon={<ArrowForwardIcon />}>
                View All
              </Button>
            }
          />
          <Divider />
          {myCheckoutsQuery.isLoading ? (
            <Typography variant="body2" color="text.secondary" sx={{ p: 3 }}>Loading your checked-out items...</Typography>
          ) : checkouts.length === 0 ? (
            <CardContent sx={{ py: 4, textAlign: 'center' }}>
              <Typography variant="body1" color="text.primary" sx={{ fontWeight: 500 }}>
                You have no active loans right now.
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5, mb: 2 }}>
                Reserve lab equipment or laptops for your courses and research.
              </Typography>
              <Button component={RouterLink} to="/reservations" variant="contained" size="small" startIcon={<EventAvailableOutlinedIcon />}>
                Book Equipment
              </Button>
            </CardContent>
          ) : (
            <List dense disablePadding>
              {checkouts.map((c) => (
                <ListItem key={c.id} divider sx={{ py: 1.25, px: 2 }}>
                  <ListItemText
                    primary={
                      <Stack direction="row" spacing={1} alignItems="center">
                        <CodeTag>{c.assetCode}</CodeTag>
                        <Typography variant="body2" sx={{ fontWeight: 600 }} noWrap>
                          {c.assetName}
                        </Typography>
                      </Stack>
                    }
                    secondary={`Checked out on ${formatDateTime(c.checkedOutAt)}`}
                  />
                  <StatusChip value={c.status} />
                </ListItem>
              ))}
            </List>
          )}
        </Card>
      </Grid>
      )}

      <Grid size={{ xs: 12, md: 6 }}>
        <Card variant="outlined" sx={{ height: '100%' }}>
          <CardHeader
            title="My Upcoming Reservations"
            titleTypographyProps={{ variant: 'h6' }}
            avatar={<EventAvailableOutlinedIcon color="primary" />}
            action={
              <Button component={RouterLink} to="/reservations" size="small" endIcon={<ArrowForwardIcon />}>
                My Reservations
              </Button>
            }
          />
          <Divider />
          {myReservationsQuery.isLoading ? (
            <Typography variant="body2" color="text.secondary" sx={{ p: 3 }}>Loading your reservations schedule...</Typography>
          ) : reservations.length === 0 ? (
            <CardContent sx={{ py: 4, textAlign: 'center' }}>
              <Typography variant="body1" color="text.primary" sx={{ fontWeight: 500 }}>
                No upcoming equipment reservations.
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5, mb: 2 }}>
                Browse available instruments, microscopes, and facility spaces.
              </Typography>
              <Button component={RouterLink} to="/assets" variant="outlined" size="small" startIcon={<SearchIcon />}>
                Search Asset Catalog
              </Button>
            </CardContent>
          ) : (
            <List dense disablePadding>
              {reservations.map((r) => (
                <ListItem key={r.id} divider sx={{ py: 1.25, px: 2 }}>
                  <ListItemText
                    primary={
                      <Stack direction="row" spacing={1} alignItems="center">
                        <CodeTag>{r.reservationNumber}</CodeTag>
                        <Typography variant="body2" sx={{ fontWeight: 600 }} noWrap>
                          {r.assetName ?? r.locationName}
                        </Typography>
                      </Stack>
                    }
                    secondary={`Start: ${formatDateTime(r.startAt)}`}
                  />
                  <StatusChip value={r.status} />
                </ListItem>
              ))}
            </List>
          )}
        </Card>
      </Grid>

      <Grid size={{ xs: 12, md: canViewCheckouts ? 12 : 6 }}>
        <Card variant="outlined" sx={{ height: '100%', borderColor: paymentsDue.length > 0 ? 'warning.main' : undefined }}>
          <CardHeader
            title={`Payments Due (${paymentsDue.length})`}
            titleTypographyProps={{ variant: 'h6' }}
            avatar={<ReceiptLongOutlinedIcon color={paymentsDue.length > 0 ? 'warning' : 'primary'} />}
            action={
              canViewPayments ? (
                <Button component={RouterLink} to="/payments" size="small" endIcon={<ArrowForwardIcon />}>
                  My Payments
                </Button>
              ) : undefined
            }
          />
          <Divider />
          {paymentsDueQuery.isLoading ? (
            <Typography variant="body2" color="text.secondary" sx={{ p: 3 }}>Checking for outstanding fees...</Typography>
          ) : paymentsDue.length === 0 ? (
            <CardContent sx={{ py: 4, textAlign: 'center' }}>
              <Typography variant="body1" color="text.primary" sx={{ fontWeight: 500 }}>
                No outstanding fees.
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                Reservation and booking fees you still owe will be listed here.
              </Typography>
            </CardContent>
          ) : (
            <List dense disablePadding>
              {paymentsDue.map((p) => (
                <ListItem key={p.id} divider sx={{ py: 1.25, px: 2 }}>
                  <ListItemText
                    primary={
                      <Stack direction="row" spacing={1} alignItems="center">
                        <CodeTag>{p.transactionNumber}</CodeTag>
                        <Typography variant="body2" sx={{ fontWeight: 600 }} noWrap>
                          {p.description || p.assetName || p.reservationNumber || 'Reservation fee'}
                        </Typography>
                      </Stack>
                    }
                    secondary={`Due since ${formatDateTime(p.paymentDate)}`}
                  />
                  <Chip
                    label={formatMoney(p.amount, p.currency)}
                    size="small"
                    color="warning"
                    variant="outlined"
                    sx={{ fontWeight: 600 }}
                  />
                </ListItem>
              ))}
            </List>
          )}
        </Card>
      </Grid>
    </Grid>
  );
}
