import { useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import {
  Card, CardHeader, CardContent, Divider, List, ListItem, ListItemText,
  Typography, Stack, Button, Chip, Link, Grid, IconButton, Tooltip
} from '@mui/material';
import CheckIcon from '@mui/icons-material/Check';
import CloseIcon from '@mui/icons-material/Close';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSnackbar } from 'notistack';
import { api, errorMessage } from '../../../api/client';
import type { ApiEnvelope, Page } from '../../../api/client';
import type { Checkout, Reservation } from '../../../api/types';
import CodeTag from '../../../components/common/CodeTag';
import { formatDateTime } from '../../../utils/format';

export default function LabManagerWidgets() {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();
  const [processingId, setProcessingId] = useState<string | null>(null);

  const pendingQuery = useQuery({
    queryKey: ['dashboard', 'pending-lab'],
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<Reservation>>>('/reservations', {
        params: { status: 'PENDING_APPROVAL', size: 6 },
      })).data.data.content,
  });

  const overdueQuery = useQuery({
    queryKey: ['dashboard', 'overdue-lab'],
    queryFn: async () =>
      (await api.get<ApiEnvelope<Checkout[]>>('/checkouts/overdue')).data.data.slice(0, 6),
  });

  const decideMutation = useMutation({
    mutationFn: async ({ id, action }: { id: string; action: 'approve' | 'reject' }) => {
      setProcessingId(id);
      return (await api.post(`/reservations/${id}/${action}`, { notes: action === 'approve' ? 'Approved from Lab Manager Dashboard' : 'Rejected from Lab Manager Dashboard' })).data;
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['reservations'] });
      enqueueSnackbar(
        variables.action === 'approve' ? 'Reservation approved' : 'Reservation rejected',
        { variant: 'success' }
      );
      setProcessingId(null);
    },
    onError: (error) => {
      enqueueSnackbar(errorMessage(error), { variant: 'error' });
      setProcessingId(null);
    },
  });

  const pendingItems = pendingQuery.data ?? [];
  const overdueItems = overdueQuery.data ?? [];

  return (
    <Grid container spacing={2}>
      <Grid size={{ xs: 12, md: 7 }}>
        <Card variant="outlined" sx={{ height: '100%', borderColor: pendingItems.length > 0 ? 'warning.main' : undefined }}>
          <CardHeader
            title={`Pending Approval Queue (${pendingItems.length})`}
            titleTypographyProps={{ variant: 'h6' }}
            subheader="Reservations requiring lab manager clearance"
            action={
              <Button component={RouterLink} to="/reservations?status=PENDING_APPROVAL" size="small" endIcon={<ArrowForwardIcon />}>
                View All Queue
              </Button>
            }
          />
          <Divider />
          {pendingQuery.isLoading ? (
            <Typography variant="body2" color="text.secondary" sx={{ p: 3 }}>Loading pending requests...</Typography>
          ) : pendingItems.length === 0 ? (
            <CardContent sx={{ py: 4, textAlign: 'center' }}>
              <Typography variant="body1" color="text.primary" sx={{ fontWeight: 500 }}>
                All clear! No pending approvals.
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                Equipment requests submitted by staff and students will show up here for 1-click action.
              </Typography>
            </CardContent>
          ) : (
            <List dense disablePadding>
              {pendingItems.map((r) => (
                <ListItem
                  key={r.id}
                  divider
                  sx={{ py: 1.25, px: 2 }}
                  secondaryAction={
                    <Stack direction="row" spacing={0.75}>
                      <Tooltip title="Approve Reservation">
                        <span>
                          <IconButton
                            size="small"
                            color="success"
                            disabled={processingId === r.id}
                            onClick={() => decideMutation.mutate({ id: r.id, action: 'approve' })}
                            sx={{ border: '1px solid', borderColor: 'success.main' }}
                          >
                            <CheckIcon fontSize="small" />
                          </IconButton>
                        </span>
                      </Tooltip>
                      <Tooltip title="Reject Request">
                        <span>
                          <IconButton
                            size="small"
                            color="error"
                            disabled={processingId === r.id}
                            onClick={() => decideMutation.mutate({ id: r.id, action: 'reject' })}
                            sx={{ border: '1px solid', borderColor: 'error.main' }}
                          >
                            <CloseIcon fontSize="small" />
                          </IconButton>
                        </span>
                      </Tooltip>
                    </Stack>
                  }
                >
                  <ListItemText
                    primary={
                      <Stack direction="row" spacing={1} alignItems="center">
                        <CodeTag>{r.reservationNumber}</CodeTag>
                        <Typography variant="body2" sx={{ fontWeight: 600 }} noWrap>
                          {r.assetName ?? r.locationName}
                        </Typography>
                        {r.quantity > 1 && <Chip label={`Qty: ${r.quantity}`} size="small" variant="outlined" />}
                      </Stack>
                    }
                    secondary={
                      <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.5 }}>
                        Requested by <strong>{r.requestedByName}</strong> • Start: {formatDateTime(r.startAt)}
                      </Typography>
                    }
                  />
                </ListItem>
              ))}
            </List>
          )}
        </Card>
      </Grid>

      <Grid size={{ xs: 12, md: 5 }}>
        <Card variant="outlined" sx={{ height: '100%', borderColor: overdueItems.length > 0 ? 'error.main' : undefined }}>
          <CardHeader
            title={`Overdue Returns Alert (${overdueItems.length})`}
            titleTypographyProps={{ variant: 'h6' }}
            subheader="Equipment not returned on schedule"
            action={
              <Link component={RouterLink} to="/checkouts" variant="body2" sx={{ mt: 1, mr: 1 }}>
                Manage Returns
              </Link>
            }
          />
          <Divider />
          {overdueQuery.isLoading ? (
            <Typography variant="body2" color="text.secondary" sx={{ p: 3 }}>Loading overdue checkouts...</Typography>
          ) : overdueItems.length === 0 ? (
            <CardContent sx={{ py: 4, textAlign: 'center' }}>
              <Typography variant="body1" color="text.primary" sx={{ fontWeight: 500 }}>
                No overdue equipment.
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                All checked-out assets are currently within their expected return date windows.
              </Typography>
            </CardContent>
          ) : (
            <List dense disablePadding>
              {overdueItems.map((c) => (
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
                    secondary={`Borrowed by ${c.userName}`}
                  />
                  <Chip label={`${c.daysOverdue}d overdue`} size="small" color="error" variant="outlined" sx={{ fontWeight: 600 }} />
                </ListItem>
              ))}
            </List>
          )}
        </Card>
      </Grid>
    </Grid>
  );
}
