import { useState } from 'react';
import { Box, Card, Chip, MenuItem, Stack, TextField, Typography } from '@mui/material';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import { api } from '../../api/client';
import type { ApiEnvelope, Page } from '../../api/client';
import type { AuditLogRow } from '../../api/types';
import PageHeader from '../../components/common/PageHeader';
import ServerDataGrid from '../../components/tables/ServerDataGrid';
import { formatDateTime, titleCase } from '../../utils/format';
import { monoFont } from '../../theme';

const MODULES = ['AUTH', 'ASSET', 'CONSUMABLE', 'RESERVATION', 'CHECKOUT', 'MAINTENANCE',
  'TRANSFER', 'PAYMENT', 'USER', 'ROLE', 'ORGANIZATION', 'LOCATION', 'CATEGORY'];

export default function AuditPage() {
  const [module, setModule] = useState('');
  const [userEmail, setUserEmail] = useState('');
  const [pagination, setPagination] = useState<GridPaginationModel>({ page: 0, pageSize: 25 });

  const params = {
    module: module || undefined,
    userEmail: userEmail || undefined,
    page: pagination.page,
    size: pagination.pageSize,
  };

  const query = useQuery({
    queryKey: ['audit', params],
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<AuditLogRow>>>('/audit-logs', { params })).data.data,
    placeholderData: keepPreviousData,
  });

  const columns: GridColDef<AuditLogRow>[] = [
    {
      field: 'createdAt', headerName: 'Timestamp', width: 160,
      valueFormatter: (value: string) => formatDateTime(value),
    },
    { field: 'userEmail', headerName: 'User', flex: 1, minWidth: 170, sortable: false },
    {
      field: 'action', headerName: 'Action', width: 170, sortable: false,
      renderCell: ({ row }) => (
        <Chip label={titleCase(row.action)} size="small" variant="outlined"
              color={row.success ? 'default' : 'error'} />
      ),
    },
    { field: 'module', headerName: 'Module', width: 120, sortable: false },
    { field: 'entityType', headerName: 'Record', width: 140, sortable: false },
    {
      field: 'newValues', headerName: 'Change', flex: 1.6, minWidth: 220, sortable: false,
      renderCell: ({ row }) => (
        <Typography sx={{ fontFamily: monoFont, fontSize: '0.72rem', color: 'text.secondary',
                         overflow: 'hidden', textOverflow: 'ellipsis', alignSelf: 'center' }}>
          {row.newValues ?? row.oldValues ?? '—'}
        </Typography>
      ),
    },
    { field: 'ipAddress', headerName: 'IP', width: 120, sortable: false },
  ];

  return (
    <Box>
      <PageHeader
        eyebrow="ADMINISTRATION"
        title="Audit Log"
        crumbs={[{ label: 'Audit Log' }]}
        subtitle="Immutable trail of every important action. Records cannot be edited or deleted."
      />
      <Card variant="outlined" sx={{ p: 2, mb: 2 }}>
        <Stack direction="row" spacing={1.5} flexWrap="wrap">
          <TextField select size="small" label="Module" value={module} sx={{ minWidth: 180 }}
                     onChange={(e) => { setModule(e.target.value); setPagination((p) => ({ ...p, page: 0 })); }}>
            <MenuItem value="">All modules</MenuItem>
            {MODULES.map((m) => <MenuItem key={m} value={m}>{titleCase(m)}</MenuItem>)}
          </TextField>
          <TextField size="small" label="User email" value={userEmail} sx={{ minWidth: 220 }}
                     onChange={(e) => { setUserEmail(e.target.value); setPagination((p) => ({ ...p, page: 0 })); }} />
        </Stack>
      </Card>
      <ServerDataGrid<AuditLogRow>
        columns={columns}
        page={query.data}
        loading={query.isLoading || query.isFetching}
        paginationModel={pagination}
        onPaginationModelChange={setPagination}
        emptyTitle="No audit records match these filters"
      />
    </Box>
  );
}
