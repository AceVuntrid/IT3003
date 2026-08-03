import { useState } from 'react';
import {
  Box, Button, Card, CardContent, Grid, Stack, Typography,
} from '@mui/material';
import TableViewOutlinedIcon from '@mui/icons-material/TableViewOutlined';
import FileDownloadOutlinedIcon from '@mui/icons-material/FileDownloadOutlined';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../api/client';
import type { ApiEnvelope } from '../../api/client';
import { useAuth } from '../../auth/AuthContext';
import PageHeader from '../../components/common/PageHeader';
import EmptyState from '../../components/common/EmptyState';

interface ReportDef {
  key: string;
  title: string;
  description: string;
  endpoint: string;
}

const REPORTS: ReportDef[] = [
  { key: 'assets', title: 'Asset register', endpoint: '/reports/assets', description: 'Every active asset with category, location, condition, status and value.' },
  { key: 'consumables', title: 'Consumable stock', endpoint: '/reports/consumables', description: 'Stock levels, reorder points and low-stock flags for every item.' },
  { key: 'expiry', title: 'Expiry report', endpoint: '/reports/expiry', description: 'Batches expiring in the next 90 days, including already-expired stock.' },
  { key: 'checkouts', title: 'Checked-out assets', endpoint: '/reports/checkouts', description: 'Items currently out, who has them and how overdue they are.' },
  { key: 'maintenance', title: 'Maintenance history', endpoint: '/reports/maintenance', description: 'All maintenance requests with status, dates and costs.' },
  { key: 'payments', title: 'Payments and charges', endpoint: '/reports/payments', description: 'All recorded transactions, refunds and their status.' },
];

export default function ReportsPage() {
  const { hasPermission } = useAuth();
  const [active, setActive] = useState<ReportDef | null>(null);

  const preview = useQuery({
    queryKey: ['report', active?.key],
    enabled: !!active,
    queryFn: async () =>
      (await api.get<ApiEnvelope<Record<string, unknown>[]>>(active!.endpoint)).data.data,
  });

  const download = async (report: ReportDef) => {
    const response = await api.get(`${report.endpoint}?format=csv`, { responseType: 'blob' });
    const url = URL.createObjectURL(response.data as Blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${report.key}-report.csv`;
    link.click();
    URL.revokeObjectURL(url);
  };

  const rows = preview.data ?? [];
  const headers = rows.length > 0 ? Object.keys(rows[0]) : [];

  return (
    <Box>
      <PageHeader
        eyebrow="ADMINISTRATION"
        title="Reports"
        crumbs={[{ label: 'Reports' }]}
        subtitle="Operational and management reports. Preview on screen or export as CSV."
      />

      <Grid container spacing={2} sx={{ mb: 3 }}>
        {REPORTS.map((report) => (
          <Grid key={report.key} size={{ xs: 12, sm: 6, md: 4 }}>
            <Card variant="outlined" sx={{
              height: '100%',
              borderColor: active?.key === report.key ? 'primary.main' : 'divider',
            }}>
              <CardContent>
                <Typography variant="h6" gutterBottom>{report.title}</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 2, minHeight: 40 }}>
                  {report.description}
                </Typography>
                <Stack direction="row" spacing={1}>
                  <Button size="small" variant="outlined" startIcon={<TableViewOutlinedIcon />}
                          onClick={() => setActive(report)}>
                    Preview
                  </Button>
                  {hasPermission('REPORT_EXPORT') && (
                    <Button size="small" startIcon={<FileDownloadOutlinedIcon />}
                            onClick={() => download(report)}>
                      CSV
                    </Button>
                  )}
                </Stack>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      {active && (
        <Card variant="outlined">
          <CardContent>
            <Typography variant="h6" gutterBottom>{active.title} — preview</Typography>
            {preview.isLoading ? (
              <Typography variant="body2" color="text.secondary">Loading report…</Typography>
            ) : rows.length === 0 ? (
              <EmptyState title="This report has no rows yet" />
            ) : (
              <Box sx={{ overflowX: 'auto' }}>
                <table style={{ borderCollapse: 'collapse', width: '100%', fontSize: '0.82rem' }}>
                  <thead>
                    <tr>
                      {headers.map((header) => (
                        <th key={header} style={{
                          textAlign: 'left', padding: '6px 12px', borderBottom: '2px solid #E1E7E5',
                          color: '#5A6B72', fontWeight: 600, whiteSpace: 'nowrap',
                        }}>
                          {header}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {rows.slice(0, 50).map((row, index) => (
                      <tr key={index}>
                        {headers.map((header) => (
                          <td key={header} style={{
                            padding: '6px 12px', borderBottom: '1px solid #EDF1EF', whiteSpace: 'nowrap',
                          }}>
                            {String(row[header] ?? '')}
                          </td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
                {rows.length > 50 && (
                  <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
                    Showing the first 50 of {rows.length} rows. Export CSV for the full report.
                  </Typography>
                )}
              </Box>
            )}
          </CardContent>
        </Card>
      )}
    </Box>
  );
}
