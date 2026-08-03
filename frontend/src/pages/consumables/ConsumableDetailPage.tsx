import { useState } from 'react';
import { useParams, Link as RouterLink } from 'react-router-dom';
import {
  Box, Button, Card, CardContent, Chip, Grid, Skeleton, Stack, Tab, Table, TableBody,
  TableCell, TableHead, TableRow, Tabs, Typography,
} from '@mui/material';
import AddShoppingCartOutlinedIcon from '@mui/icons-material/AddShoppingCartOutlined';
import EventAvailableOutlinedIcon from '@mui/icons-material/EventAvailableOutlined';
import OutboundOutlinedIcon from '@mui/icons-material/OutboundOutlined';
import TuneOutlinedIcon from '@mui/icons-material/TuneOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../api/client';
import type { ApiEnvelope } from '../../api/client';
import type { Batch, ConsumableDetail, StockTransaction } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import PageHeader from '../../components/common/PageHeader';
import CodeTag from '../../components/common/CodeTag';
import EmptyState from '../../components/common/EmptyState';
import BookingDialog from '../../components/common/BookingDialog';
import ConsumableFormDialog from './ConsumableFormDialog';
import { AdjustStockDialog, IssueStockDialog, ReceiveStockDialog } from './StockDialogs';
import { formatDate, formatDateTime, formatMoney, titleCase } from '../../utils/format';
import { monoFont } from '../../theme';

function Metric({ label, value, accent }: { label: string; value: string; accent?: boolean }) {
  return (
    <Grid size={{ xs: 6, sm: 3 }}>
      <Card variant="outlined" sx={{ height: '100%' }}>
        <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
          <Typography sx={{ fontFamily: monoFont, fontSize: '1.3rem', fontWeight: 500,
                           color: accent ? '#93332C' : 'text.primary' }}>
            {value}
          </Typography>
          <Typography variant="body2" color="text.secondary">{label}</Typography>
        </CardContent>
      </Card>
    </Grid>
  );
}

export default function ConsumableDetailPage() {
  const { id } = useParams();
  const { hasPermission } = useAuth();
  const [tab, setTab] = useState(0);
  const [dialog, setDialog] = useState<'reserve' | 'receive' | 'issue' | 'adjust' | 'edit' | null>(null);

  const itemQuery = useQuery({
    queryKey: ['consumable', id],
    queryFn: async () => (await api.get<ApiEnvelope<ConsumableDetail>>(`/consumables/${id}`)).data.data,
  });
  const batchesQuery = useQuery({
    queryKey: ['consumable', id, 'batches'],
    queryFn: async () => (await api.get<ApiEnvelope<Batch[]>>(`/consumables/${id}/batches`)).data.data,
  });
  const transactionsQuery = useQuery({
    queryKey: ['consumable', id, 'transactions'],
    enabled: tab === 1,
    queryFn: async () =>
      (await api.get<ApiEnvelope<StockTransaction[]>>(`/consumables/${id}/transactions`)).data.data,
  });

  if (itemQuery.isLoading) return <Skeleton variant="rounded" height={420} />;
  const item = itemQuery.data;
  if (!item) {
    return <EmptyState title="Consumable not found" action={
      <Button component={RouterLink} to="/consumables" variant="contained">Back to consumables</Button>} />;
  }

  return (
    <Box>
      <PageHeader
        eyebrow="INVENTORY"
        title={
          <Stack direction="row" spacing={1.5} alignItems="center" flexWrap="wrap">
            <span>{item.name}</span>
            <CodeTag>{item.itemCode}</CodeTag>
            {item.hazardous && <Chip label="Hazardous" color="warning" size="small" />}
            {item.lowStock && <Chip label="Low stock" color="error" size="small" />}
          </Stack>
        }
        crumbs={[{ label: 'Consumables', to: '/consumables' }, { label: item.itemCode }]}
        subtitle={`${item.categoryName} · stored in ${item.locationName}`}
        actions={
          <>
            {hasPermission('RESERVATION_CREATE') && (
              <Button variant="outlined" startIcon={<EventAvailableOutlinedIcon />}
                      onClick={() => setDialog('reserve')}>
                Reserve
              </Button>
            )}
            {hasPermission('CONSUMABLE_RECEIVE') && (
              <Button variant="outlined" startIcon={<AddShoppingCartOutlinedIcon />}
                      onClick={() => setDialog('receive')}>
                Receive stock
              </Button>
            )}
            {hasPermission('CONSUMABLE_ISSUE') && (
              <Button variant="contained" startIcon={<OutboundOutlinedIcon />}
                      onClick={() => setDialog('issue')}>
                Issue stock
              </Button>
            )}
            {hasPermission('CONSUMABLE_ADJUST') && (
              <Button variant="outlined" startIcon={<TuneOutlinedIcon />}
                      onClick={() => setDialog('adjust')}>
                Adjust
              </Button>
            )}
            {hasPermission('CONSUMABLE_EDIT') && (
              <Button variant="text" startIcon={<EditOutlinedIcon />} onClick={() => setDialog('edit')}>
                Edit
              </Button>
            )}
          </>
        }
      />

      <Grid container spacing={2} sx={{ mb: 2 }}>
        <Metric label={`On hand (${item.unitOfMeasure})`} value={String(item.currentQuantity)} />
        <Metric label="Available" value={String(item.availableQuantity)} />
        <Metric label="Reorder level" value={String(item.reorderLevel)} accent={item.lowStock} />
        <Metric label="Unit cost" value={formatMoney(item.unitCost)} />
      </Grid>

      {item.hazardous && (
        <Card variant="outlined" sx={{ mb: 2, borderColor: 'warning.main' }}>
          <CardContent sx={{ py: 1.5 }}>
            <Typography variant="subtitle2" gutterBottom>Safety information</Typography>
            <Typography variant="body2" color="text.secondary">
              {item.chemicalClassification && <>Classification: <b>{item.chemicalClassification}</b>. </>}
              {item.storageInstructions && <>Storage: {item.storageInstructions}. </>}
              {item.disposalInstructions && <>Disposal: {item.disposalInstructions}.</>}
            </Typography>
          </CardContent>
        </Card>
      )}

      <Tabs value={tab} onChange={(_, value) => setTab(value)} sx={{ mb: 2 }}>
        <Tab label={`Batches (${batchesQuery.data?.length ?? 0})`} />
        <Tab label="Stock movements" />
      </Tabs>

      {tab === 0 && (
        <Card variant="outlined">
          {(batchesQuery.data ?? []).length === 0 ? (
            <EmptyState title="No batches received yet"
                        hint="Use Receive stock to record the first delivery with its batch number and expiry." />
          ) : (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Batch</TableCell>
                  <TableCell align="right">Received</TableCell>
                  <TableCell align="right">Remaining</TableCell>
                  <TableCell>Received date</TableCell>
                  <TableCell>Expiry</TableCell>
                  <TableCell align="right">Unit cost</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {(batchesQuery.data ?? []).map((batch) => (
                  <TableRow key={batch.id} hover>
                    <TableCell><CodeTag muted>{batch.batchNumber}</CodeTag></TableCell>
                    <TableCell align="right">{batch.quantityReceived}</TableCell>
                    <TableCell align="right"><b>{batch.quantityRemaining}</b></TableCell>
                    <TableCell>{formatDate(batch.receivedDate)}</TableCell>
                    <TableCell>
                      <Stack direction="row" spacing={1} alignItems="center">
                        <span>{formatDate(batch.expiryDate)}</span>
                        {batch.expired && <Chip label="Expired" size="small" color="error" />}
                      </Stack>
                    </TableCell>
                    <TableCell align="right">{formatMoney(batch.unitCost)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </Card>
      )}

      {tab === 1 && (
        <Card variant="outlined">
          {(transactionsQuery.data ?? []).length === 0 ? (
            <EmptyState title="No stock movements yet" />
          ) : (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>When</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell align="right">Quantity</TableCell>
                  <TableCell>Batch</TableCell>
                  <TableCell>Recipient</TableCell>
                  <TableCell>Purpose / reason</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {(transactionsQuery.data ?? []).map((tx) => (
                  <TableRow key={tx.id} hover>
                    <TableCell>{formatDateTime(tx.createdAt)}</TableCell>
                    <TableCell>{titleCase(tx.transactionType)}</TableCell>
                    <TableCell align="right">{tx.quantity} {item.unitOfMeasure}</TableCell>
                    <TableCell>{tx.batchNumber ? <CodeTag muted>{tx.batchNumber}</CodeTag> : '—'}</TableCell>
                    <TableCell>{tx.relatedUserName ?? tx.relatedDepartmentName ?? '—'}</TableCell>
                    <TableCell>{tx.purpose ?? tx.reason ?? '—'}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </Card>
      )}

      {/* Shared unified booking dialog, pre-filled with this consumable item. */}
      <BookingDialog open={dialog === 'reserve'} onClose={() => setDialog(null)}
                     initialType="consumable" initialItemId={item.id} />
      <ReceiveStockDialog open={dialog === 'receive'} onClose={() => setDialog(null)} item={item} />
      <IssueStockDialog open={dialog === 'issue'} onClose={() => setDialog(null)} item={item} />
      <AdjustStockDialog open={dialog === 'adjust'} onClose={() => setDialog(null)} item={item} />
      <ConsumableFormDialog open={dialog === 'edit'} onClose={() => setDialog(null)} existing={item} />
    </Box>
  );
}
