import { useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import {
  Card, CardHeader, CardContent, Divider, List, ListItem, ListItemText,
  Typography, Stack, Button, Chip, Grid, IconButton, Tooltip
} from '@mui/material';
import CheckIcon from '@mui/icons-material/Check';
import CloseIcon from '@mui/icons-material/Close';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import NotificationsNoneOutlinedIcon from '@mui/icons-material/NotificationsNoneOutlined';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSnackbar } from 'notistack';
import { api, errorMessage } from '../../../api/client';
import type { ApiEnvelope, Page } from '../../../api/client';
import type { NotificationRow, Reservation } from '../../../api/types';
import CodeTag from '../../../components/common/CodeTag';
import { formatDateTime } from '../../../utils/format';
import type { ApproverPersonaType } from '../personaUtils';

interface Props {
  persona: ApproverPersonaType;
}

/** Where the viewer's approval authority comes from, for card copy only. */
const SCOPE_COPY: Record<ApproverPersonaType, string> = {
  DEPT_ADMIN_USER: "Requests for your department's equipment, venues, and supplies",
  FACULTY_DEAN_USER: 'Requests for faculty-owned assets and venues',
  CARETAKER_USER: 'Booking requests for the venues and buildings in your care',
};

/**
 * Shared dashboard widgets for the unit-scoped approver personas
 * (department admin, faculty dean, caretaker). The reservations API already
 * limits PENDING_APPROVAL rows to the viewer's unit, so the same component
 * serves all three roles with only the copy parameterized.
 */
export default function ApproverWidgets({ persona }: Props) {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();
  const [processingId, setProcessingId] = useState<string | null>(null);

  const pendingQuery = useQuery({
    queryKey: ['dashboard', 'pending-approver'],
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<Reservation>>>('/reservations', {
        params: { status: 'PENDING_APPROVAL', size: 6 },
      })).data.data.content,
  });

  const notificationsQuery = useQuery({
    queryKey: ['dashboard', 'notifications-approver'],
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<NotificationRow>>>('/notifications', {
        params: { size: 5 },
      })).data.data.content,
  });

  const decideMutation = useMutation({
    mutationFn: async ({ id, action }: { id: string; action: 'approve' | 'reject' }) => {
      setProcessingId(id);
      return (await api.post(`/reservations/${id}/${action}`, {
        notes: action === 'approve' ? 'Approved from dashboard' : 'Rejected from dashboard',
      })).data;
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
  const notifications = notificationsQuery.data ?? [];

  return (
    <Grid container spacing={2}>
      <Grid size={{ xs: 12, md: 7 }}>
        <Card variant="outlined" sx={{ height: '100%', borderColor: pendingItems.length > 0 ? 'warning.main' : undefined }}>
          <CardHeader
            title={`Awaiting Your Approval (${pendingItems.length})`}
            titleTypographyProps={{ variant: 'h6' }}
            subheader={SCOPE_COPY[persona]}
            action={
              <Button component={RouterLink} to="/reservations?status=PENDING_APPROVAL" size="small" endIcon={<ArrowForwardIcon />}>
                Review All
              </Button>
            }
          />
          <Divider />
          {pendingQuery.isLoading ? (
            <Typography variant="body2" color="text.secondary" sx={{ p: 3 }}>Loading pending requests...</Typography>
          ) : pendingItems.length === 0 ? (
            <CardContent sx={{ py: 4, textAlign: 'center' }}>
              <Typography variant="body1" color="text.primary" sx={{ fontWeight: 500 }}>
                All clear! Nothing is waiting on you.
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                New reservation requests routed to you will appear here for one-click action.
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
                      <Tooltip title="Approve Request">
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
                          {r.assetName ?? r.locationName ?? r.consumableItemName}
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
        <Card variant="outlined" sx={{ height: '100%' }}>
          <CardHeader
            title="Recent Notifications"
            titleTypographyProps={{ variant: 'h6' }}
            avatar={<NotificationsNoneOutlinedIcon color="primary" />}
            action={
              <Button component={RouterLink} to="/notifications" size="small" endIcon={<ArrowForwardIcon />}>
                Inbox
              </Button>
            }
          />
          <Divider />
          {notificationsQuery.isLoading ? (
            <Typography variant="body2" color="text.secondary" sx={{ p: 3 }}>Loading notifications...</Typography>
          ) : notifications.length === 0 ? (
            <CardContent sx={{ py: 4, textAlign: 'center' }}>
              <Typography variant="body1" color="text.primary" sx={{ fontWeight: 500 }}>
                You're all caught up.
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                Approval requests and reservation updates will land here.
              </Typography>
            </CardContent>
          ) : (
            <List dense disablePadding>
              {notifications.map((n) => (
                <ListItem key={n.id} divider sx={{ py: 1.25, px: 2 }}>
                  <ListItemText
                    primary={
                      <Typography variant="body2" sx={{ fontWeight: n.readAt ? 400 : 600 }} noWrap>
                        {n.title}
                      </Typography>
                    }
                    secondary={
                      <Typography variant="caption" color="text.secondary" display="block" noWrap>
                        {n.message} • {formatDateTime(n.createdAt)}
                      </Typography>
                    }
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
