import { Link as RouterLink } from 'react-router-dom';
import {
  Card, CardHeader, CardContent, Divider, List, ListItem, ListItemText,
  Typography, Stack, Button, Chip, Grid
} from '@mui/material';
import BuildOutlinedIcon from '@mui/icons-material/BuildOutlined';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../../api/client';
import type { ApiEnvelope, Page } from '../../../api/client';
import type { MaintenanceRequest, DashboardCharts } from '../../../api/types';
import CodeTag from '../../../components/common/CodeTag';
import StatusChip from '../../../components/common/StatusChip';
import { formatDateTime } from '../../../utils/format';

export default function MaintenanceWidgets() {
  const jobsQuery = useQuery({
    queryKey: ['dashboard', 'maintenance-jobs'],
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<MaintenanceRequest>>>('/maintenance-requests', {
        params: { size: 6, sort: 'openedAt,desc' },
      })).data.data.content,
  });

  const chartsQuery = useQuery({
    queryKey: ['dashboard', 'charts-maint'],
    queryFn: async () => (await api.get<ApiEnvelope<DashboardCharts>>('/dashboard/charts')).data.data,
  });

  const jobs = jobsQuery.data ?? [];
  const conditionChart = chartsQuery.data?.assetsByCondition ?? [];

  return (
    <Grid container spacing={2}>
      <Grid size={{ xs: 12, md: 7 }}>
        <Card variant="outlined" sx={{ height: '100%' }}>
          <CardHeader
            title={`Active Maintenance Jobs & Work Orders (${jobs.length})`}
            titleTypographyProps={{ variant: 'h6' }}
            subheader="Equipment repairs, servicing, and scheduled calibrations"
            action={
              <Button component={RouterLink} to="/maintenance" size="small" endIcon={<ArrowForwardIcon />}>
                Work Orders
              </Button>
            }
          />
          <Divider />
          {jobsQuery.isLoading ? (
            <Typography variant="body2" color="text.secondary" sx={{ p: 3 }}>Loading maintenance work orders...</Typography>
          ) : jobs.length === 0 ? (
            <CardContent sx={{ py: 4, textAlign: 'center' }}>
              <Typography variant="body1" color="text.primary" sx={{ fontWeight: 500 }}>
                No active maintenance jobs.
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                Fault reports submitted by lab staff will appear here.
              </Typography>
            </CardContent>
          ) : (
            <List dense disablePadding>
              {jobs.map((job) => (
                <ListItem key={job.id} divider sx={{ py: 1.25, px: 2 }}>
                  <ListItemText
                    primary={
                      <Stack direction="row" spacing={1} alignItems="center">
                        <CodeTag>{job.requestNumber}</CodeTag>
                        <Typography variant="body2" sx={{ fontWeight: 600 }} noWrap>
                          {job.assetName}
                        </Typography>
                        <StatusChip value={job.status} />
                      </Stack>
                    }
                    secondary={
                      <Typography variant="caption" color="text.secondary" display="block">
                        Issue: {job.issueType} • Reported: {formatDateTime(job.openedAt)}
                      </Typography>
                    }
                  />
                  <Button component={RouterLink} to="/maintenance" size="small" variant="outlined" sx={{ textTransform: 'none' }}>
                    View Work Order
                  </Button>
                </ListItem>
              ))}
            </List>
          )}
        </Card>
      </Grid>

      <Grid size={{ xs: 12, md: 5 }}>
        <Card variant="outlined" sx={{ height: '100%' }}>
          <CardHeader
            title="Equipment Health & Physical Condition"
            titleTypographyProps={{ variant: 'h6' }}
            avatar={<BuildOutlinedIcon color="primary" />}
          />
          <Divider />
          <CardContent sx={{ py: 2 }}>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              Overview of hardware asset physical conditions across faculties:
            </Typography>
            <Stack spacing={1.5}>
              {conditionChart.map((entry) => (
                <Stack key={entry.name} direction="row" justifyContent="space-between" alignItems="center" sx={{ p: 1, borderRadius: 1.5, border: '1px solid #EAEAEA' }}>
                  <Stack direction="row" spacing={1} alignItems="center">
                    <StatusChip value={entry.name === 'NEW' || entry.name === 'EXCELLENT' || entry.name === 'GOOD' ? 'AVAILABLE' : entry.name === 'FAIR' || entry.name === 'POOR' ? 'PENDING' : 'DAMAGED'} />
                    <Typography variant="body2" sx={{ fontWeight: 500 }}>{entry.name}</Typography>
                  </Stack>
                  <Chip label={`${entry.value} items`} size="small" variant="outlined" sx={{ fontWeight: 600 }} />
                </Stack>
              ))}
            </Stack>
          </CardContent>
        </Card>
      </Grid>
    </Grid>
  );
}
