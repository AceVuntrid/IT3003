import { Link as RouterLink } from 'react-router-dom';
import {
  Card, CardContent, CardHeader, Divider, Grid, Link, List, ListItem,
  ListItemText, Skeleton, Stack, Typography, Chip
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import {
  Bar, BarChart, CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts';
import { api } from '../../../api/client';
import type { ApiEnvelope, Page } from '../../../api/client';
import type {
  Checkout, ConsumableSummary, DashboardCharts, Reservation,
} from '../../../api/types';
import { useAuth } from '../../../auth/AuthContext';
import StatusChip from '../../../components/common/StatusChip';
import CodeTag from '../../../components/common/CodeTag';
import { formatDateTime, titleCase } from '../../../utils/format';

const CHART_PURPLE_LIGHT = '#A855F7';
const CHART_PURPLE_PRIMARY = '#7C3AED';

function ChartCard({ title, children, loading }: {
  title: string;
  children: React.ReactNode;
  loading?: boolean;
}) {
  return (
    <Card variant="outlined" sx={{ height: '100%' }}>
      <CardHeader title={title} titleTypographyProps={{ variant: 'h6' }} sx={{ pb: 0 }} />
      <CardContent sx={{ height: 280 }}>
        {loading ? <Skeleton variant="rounded" height="100%" /> : children}
      </CardContent>
    </Card>
  );
}

export default function AdminExecutiveWidgets() {
  const { hasPermission } = useAuth();

  const chartsQuery = useQuery({
    queryKey: ['dashboard', 'charts-admin'],
    queryFn: async () => (await api.get<ApiEnvelope<DashboardCharts>>('/dashboard/charts')).data.data,
  });

  const pendingQuery = useQuery({
    queryKey: ['dashboard', 'pending-admin'],
    enabled: hasPermission('RESERVATION_APPROVE'),
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<Reservation>>>('/reservations', {
        params: { status: 'PENDING_APPROVAL', size: 5 },
      })).data.data.content,
  });

  const overdueQuery = useQuery({
    queryKey: ['dashboard', 'overdue-admin'],
    enabled: hasPermission('CHECKOUT_VIEW'),
    queryFn: async () =>
      (await api.get<ApiEnvelope<Checkout[]>>('/checkouts/overdue')).data.data.slice(0, 5),
  });

  const lowStockQuery = useQuery({
    queryKey: ['dashboard', 'low-stock-admin'],
    enabled: hasPermission('CONSUMABLE_VIEW'),
    queryFn: async () =>
      (await api.get<ApiEnvelope<ConsumableSummary[]>>('/consumables/low-stock')).data.data.slice(0, 5),
  });

  const charts = chartsQuery.data;

  return (
    <Stack spacing={2}>
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 7 }}>
          <ChartCard title="Assets by category" loading={chartsQuery.isLoading}>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={charts?.assetsByCategory ?? []} layout="vertical"
                        margin={{ left: 8, right: 24, top: 4, bottom: 4 }}>
                <CartesianGrid horizontal={false} stroke="#EDF1EF" />
                <XAxis type="number" allowDecimals={false} tickLine={false} axisLine={false}
                       tick={{ fontSize: 12, fill: '#5A6B72' }} />
                <YAxis type="category" dataKey="name" width={150} tickLine={false} axisLine={false}
                       tick={{ fontSize: 12, fill: '#1C2B33' }} />
                <Tooltip cursor={{ fill: 'rgba(14,140,106,0.06)' }}
                         contentStyle={{ borderRadius: 8, border: '1px solid #E1E7E5', fontSize: 13 }} />
                <Bar dataKey="value" name="Assets" fill={CHART_PURPLE_LIGHT} radius={[0, 4, 4, 0]} barSize={16} />
              </BarChart>
            </ResponsiveContainer>
          </ChartCard>
        </Grid>

        <Grid size={{ xs: 12, md: 5 }}>
          <ChartCard title="Reservations per month" loading={chartsQuery.isLoading}>
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={charts?.monthlyReservations ?? []}
                         margin={{ left: 0, right: 16, top: 8, bottom: 4 }}>
                <CartesianGrid vertical={false} stroke="#EDF1EF" />
                <XAxis dataKey="name" tickLine={false} axisLine={false}
                       tick={{ fontSize: 12, fill: '#5A6B72' }} />
                <YAxis allowDecimals={false} tickLine={false} axisLine={false} width={32}
                       tick={{ fontSize: 12, fill: '#5A6B72' }} />
                <Tooltip contentStyle={{ borderRadius: 8, border: '1px solid #E1E7E5', fontSize: 13 }} />
                <Line type="monotone" dataKey="value" name="Reservations" stroke={CHART_PURPLE_PRIMARY}
                      strokeWidth={2} dot={{ r: 3.5, fill: CHART_PURPLE_PRIMARY, strokeWidth: 0 }}
                      activeDot={{ r: 5 }} />
              </LineChart>
            </ResponsiveContainer>
          </ChartCard>
        </Grid>
      </Grid>

      <Grid container spacing={2}>
        {hasPermission('RESERVATION_APPROVE') && (
          <Grid size={{ xs: 12, md: 4 }}>
            <Card variant="outlined" sx={{ height: '100%' }}>
              <CardHeader
                title="Pending approvals"
                titleTypographyProps={{ variant: 'h6' }}
                action={<Link component={RouterLink} to="/reservations" variant="body2" sx={{ mt: 1, mr: 1 }}>View all</Link>}
              />
              <Divider />
              {pendingQuery.data?.length === 0 ? (
                <Typography variant="body2" color="text.secondary" sx={{ p: 2.5 }}>
                  Nothing waiting on approval.
                </Typography>
              ) : (
                <List dense disablePadding>
                  {(pendingQuery.data ?? []).map((r) => (
                    <ListItem key={r.id} divider sx={{ py: 1, px: 2 }}>
                      <ListItemText
                        primary={<Stack direction="row" spacing={1} alignItems="center">
                          <CodeTag>{r.reservationNumber}</CodeTag>
                          <Typography variant="body2" noWrap>{r.assetName ?? r.locationName}</Typography>
                        </Stack>}
                        secondary={`${r.requestedByName} · ${formatDateTime(r.startAt)}`}
                      />
                    </ListItem>
                  ))}
                </List>
              )}
            </Card>
          </Grid>
        )}

        {hasPermission('CHECKOUT_VIEW') && (
          <Grid size={{ xs: 12, md: 4 }}>
            <Card variant="outlined" sx={{ height: '100%' }}>
              <CardHeader
                title="Overdue returns"
                titleTypographyProps={{ variant: 'h6' }}
                action={<Link component={RouterLink} to="/checkouts" variant="body2" sx={{ mt: 1, mr: 1 }}>View all</Link>}
              />
              <Divider />
              {overdueQuery.data?.length === 0 ? (
                <Typography variant="body2" color="text.secondary" sx={{ p: 2.5 }}>
                  No overdue items. Everything is on schedule.
                </Typography>
              ) : (
                <List dense disablePadding>
                  {(overdueQuery.data ?? []).map((c) => (
                    <ListItem key={c.id} divider sx={{ py: 1, px: 2 }}>
                      <ListItemText
                        primary={<Stack direction="row" spacing={1} alignItems="center">
                          <CodeTag>{c.assetCode}</CodeTag>
                          <Typography variant="body2" noWrap>{c.assetName}</Typography>
                        </Stack>}
                        secondary={`${c.userName} · ${c.daysOverdue} day(s) overdue`}
                      />
                      <Chip label={`${c.daysOverdue}d`} size="small" color="error" variant="outlined" />
                    </ListItem>
                  ))}
                </List>
              )}
            </Card>
          </Grid>
        )}

        {hasPermission('CONSUMABLE_VIEW') && (
          <Grid size={{ xs: 12, md: 4 }}>
            <Card variant="outlined" sx={{ height: '100%' }}>
              <CardHeader
                title="Low stock"
                titleTypographyProps={{ variant: 'h6' }}
                action={<Link component={RouterLink} to="/consumables" variant="body2" sx={{ mt: 1, mr: 1 }}>View all</Link>}
              />
              <Divider />
              {lowStockQuery.data?.length === 0 ? (
                <Typography variant="body2" color="text.secondary" sx={{ p: 2.5 }}>
                  All consumables are above reorder levels.
                </Typography>
              ) : (
                <List dense disablePadding>
                  {(lowStockQuery.data ?? []).map((item) => (
                    <ListItem key={item.id} divider sx={{ py: 1, px: 2 }}>
                      <ListItemText
                        primary={<Stack direction="row" spacing={1} alignItems="center">
                          <CodeTag>{item.itemCode}</CodeTag>
                          <Typography variant="body2" noWrap>{item.name}</Typography>
                        </Stack>}
                        secondary={`${item.currentQuantity} ${item.unitOfMeasure} on hand · reorder at ${item.reorderLevel}`}
                      />
                      <StatusChip value="PENDING" />
                    </ListItem>
                  ))}
                </List>
              )}
            </Card>
          </Grid>
        )}
      </Grid>

      {charts && charts.assetsByCondition.length > 0 && (
        <Card variant="outlined">
          <CardContent sx={{ py: 2 }}>
            <Stack direction="row" spacing={3} flexWrap="wrap" alignItems="center">
              <Typography variant="h6" sx={{ mr: 1 }}>Condition</Typography>
              {charts.assetsByCondition.map((entry) => (
                <Stack key={entry.name} direction="row" spacing={1} alignItems="center">
                  <StatusChip value={entry.name === 'NEW' || entry.name === 'EXCELLENT' || entry.name === 'GOOD'
                    ? 'AVAILABLE' : entry.name === 'FAIR' || entry.name === 'POOR' ? 'PENDING' : 'DAMAGED'} />
                  <Typography variant="body2" color="text.secondary">
                    {titleCase(entry.name)}: <b>{entry.value}</b>
                  </Typography>
                </Stack>
              ))}
            </Stack>
          </CardContent>
        </Card>
      )}
    </Stack>
  );
}
