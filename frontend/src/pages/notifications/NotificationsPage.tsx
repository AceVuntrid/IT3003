import { Box, Button, Card, IconButton, List, ListItem, ListItemText, Stack, Typography } from '@mui/material';
import DoneAllIcon from '@mui/icons-material/DoneAll';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import NotificationsNoneIcon from '@mui/icons-material/NotificationsNone';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../../api/client';
import type { ApiEnvelope, Page } from '../../api/client';
import type { NotificationRow } from '../../api/types';
import PageHeader from '../../components/common/PageHeader';
import EmptyState from '../../components/common/EmptyState';
import { formatDateTime, titleCase } from '../../utils/format';

export default function NotificationsPage() {
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: ['notifications', 'list'],
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<NotificationRow>>>('/notifications', {
        params: { size: 50 },
      })).data.data.content,
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['notifications'] });
  };

  const markRead = useMutation({
    mutationFn: async (id: string) => api.post(`/notifications/${id}/read`),
    onSuccess: invalidate,
  });
  const markAll = useMutation({
    mutationFn: async () => api.post('/notifications/read-all'),
    onSuccess: invalidate,
  });
  const remove = useMutation({
    mutationFn: async (id: string) => api.delete(`/notifications/${id}`),
    onSuccess: invalidate,
  });

  const notifications = query.data ?? [];

  return (
    <Box>
      <PageHeader
        eyebrow="INBOX"
        title="Notifications"
        crumbs={[{ label: 'Notifications' }]}
        actions={notifications.some((n) => !n.readAt) && (
          <Button variant="outlined" startIcon={<DoneAllIcon />} onClick={() => markAll.mutate()}>
            Mark all as read
          </Button>
        )}
      />
      <Card variant="outlined">
        {notifications.length === 0 ? (
          <EmptyState icon={<NotificationsNoneIcon />} title="You're all caught up"
                      hint="Reservation updates, due-date reminders and stock alerts will appear here." />
        ) : (
          <List>
            {notifications.map((notification) => (
              <ListItem
                key={notification.id}
                divider
                sx={{ backgroundColor: notification.readAt ? 'transparent' : 'rgba(14,124,102,0.05)' }}
                secondaryAction={
                  <IconButton aria-label="Delete notification"
                              onClick={() => remove.mutate(notification.id)}>
                    <DeleteOutlineIcon />
                  </IconButton>
                }
                onClick={() => !notification.readAt && markRead.mutate(notification.id)}
              >
                <ListItemText
                  primary={
                    <Stack direction="row" spacing={1} alignItems="baseline">
                      <Typography variant="body2" fontWeight={notification.readAt ? 400 : 600}>
                        {notification.title}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {titleCase(notification.type)}
                      </Typography>
                    </Stack>
                  }
                  secondary={`${notification.message} · ${formatDateTime(notification.createdAt)}`}
                />
              </ListItem>
            ))}
          </List>
        )}
      </Card>
    </Box>
  );
}
