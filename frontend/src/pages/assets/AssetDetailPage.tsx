import { useState } from 'react';
import { useNavigate, useParams, Link as RouterLink } from 'react-router-dom';
import {
  Alert, Box, Button, Card, CardContent, Chip, Divider, Grid, IconButton, List, ListItem,
  ListItemText, Skeleton, Stack, Tab, Tabs, Tooltip, Typography,
} from '@mui/material';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import ArchiveOutlinedIcon from '@mui/icons-material/ArchiveOutlined';
import UnarchiveOutlinedIcon from '@mui/icons-material/UnarchiveOutlined';
import QrCode2OutlinedIcon from '@mui/icons-material/QrCode2Outlined';
import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined';
import DownloadOutlinedIcon from '@mui/icons-material/DownloadOutlined';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import EventAvailableOutlinedIcon from '@mui/icons-material/EventAvailableOutlined';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSnackbar } from 'notistack';
import { api, errorMessage } from '../../api/client';
import type { ApiEnvelope, Page } from '../../api/client';
import type { AssetDetail, Checkout, MaintenanceRequest, Reservation, Transfer } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import PageHeader from '../../components/common/PageHeader';
import StatusChip from '../../components/common/StatusChip';
import CodeTag from '../../components/common/CodeTag';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import EmptyState from '../../components/common/EmptyState';
import BookingDialog from '../../components/common/BookingDialog';
import { formatDate, formatDateTime, formatMoney, titleCase } from '../../utils/format';

interface DocumentRow {
  id: string;
  documentType: string;
  originalFilename: string;
  mimeType: string;
  sizeBytes: number;
  uploadedAt: string;
}

function Fact({ label, value }: { label: string; value?: React.ReactNode }) {
  return (
    <Grid size={{ xs: 6, sm: 4, md: 3 }}>
      <Typography variant="caption" color="text.secondary" display="block">{label}</Typography>
      <Typography variant="body2" sx={{ fontWeight: 500 }}>{value ?? '—'}</Typography>
    </Grid>
  );
}

export default function AssetDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();
  const { hasPermission } = useAuth();
  const [tab, setTab] = useState(0);
  const [confirmArchive, setConfirmArchive] = useState(false);
  const [qrOpen, setQrOpen] = useState(false);
  const [qrUrl, setQrUrl] = useState<string | null>(null);
  const [reserveOpen, setReserveOpen] = useState(false);

  const toggleQr = async () => {
    if (!qrOpen && !qrUrl) {
      const response = await api.get(`/assets/${id}/qr-code`, { responseType: 'blob' });
      setQrUrl(URL.createObjectURL(response.data as Blob));
    }
    setQrOpen((open) => !open);
  };

  const assetQuery = useQuery({
    queryKey: ['asset', id],
    queryFn: async () => (await api.get<ApiEnvelope<AssetDetail>>(`/assets/${id}`)).data.data,
  });

  const documentsQuery = useQuery({
    queryKey: ['documents', 'Asset', id],
    queryFn: async () =>
      (await api.get<ApiEnvelope<DocumentRow[]>>('/documents', {
        params: { entityType: 'Asset', entityId: id },
      })).data.data,
  });

  const reservationsQuery = useQuery({
    queryKey: ['asset-reservations', id],
    enabled: tab === 1,
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<Reservation>>>('/reservations', {
        params: { assetId: id, size: 25 },
      })).data.data.content,
  });

  const checkoutsQuery = useQuery({
    queryKey: ['asset-checkouts', id],
    enabled: tab === 2 && hasPermission('CHECKOUT_VIEW'),
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<Checkout>>>('/checkouts', {
        params: { assetId: id, size: 25 },
      })).data.data.content,
  });

  const maintenanceQuery = useQuery({
    queryKey: ['asset-maintenance', id],
    enabled: tab === 3 && hasPermission('MAINTENANCE_VIEW'),
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<MaintenanceRequest>>>('/maintenance-requests', {
        params: { assetId: id, size: 25 },
      })).data.data.content,
  });

  const transfersQuery = useQuery({
    queryKey: ['asset-transfers', id],
    enabled: tab === 4 && hasPermission('TRANSFER_VIEW'),
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<Transfer>>>('/transfers', {
        params: { assetId: id, size: 25 },
      })).data.data.content,
  });

  const archiveMutation = useMutation({
    mutationFn: async () => {
      const path = assetQuery.data?.archived ? 'restore' : 'archive';
      await api.post(`/assets/${id}/${path}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['asset', id] });
      queryClient.invalidateQueries({ queryKey: ['assets'] });
      enqueueSnackbar(assetQuery.data?.archived ? 'Asset restored' : 'Asset archived', { variant: 'success' });
      setConfirmArchive(false);
    },
    onError: (error) => enqueueSnackbar(errorMessage(error), { variant: 'error' }),
  });

  const uploadDocument = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;
    const form = new FormData();
    form.append('file', file);
    try {
      await api.post(`/documents?entityType=Asset&entityId=${id}&documentType=OTHER`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      queryClient.invalidateQueries({ queryKey: ['documents', 'Asset', id] });
      enqueueSnackbar('Document uploaded', { variant: 'success' });
    } catch (error) {
      enqueueSnackbar(errorMessage(error), { variant: 'error' });
    } finally {
      event.target.value = '';
    }
  };

  const downloadDocument = async (doc: DocumentRow) => {
    const response = await api.get(`/documents/${doc.id}/download`, { responseType: 'blob' });
    const url = URL.createObjectURL(response.data as Blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = doc.originalFilename;
    link.click();
    URL.revokeObjectURL(url);
  };

  const deleteDocument = async (doc: DocumentRow) => {
    try {
      await api.delete(`/documents/${doc.id}`);
      queryClient.invalidateQueries({ queryKey: ['documents', 'Asset', id] });
      enqueueSnackbar('Document deleted', { variant: 'success' });
    } catch (error) {
      enqueueSnackbar(errorMessage(error), { variant: 'error' });
    }
  };

  if (assetQuery.isLoading) {
    return <Skeleton variant="rounded" height={480} />;
  }
  const asset = assetQuery.data;
  if (!asset) {
    return <EmptyState title="Asset not found" action={
      <Button component={RouterLink} to="/assets" variant="contained">Back to assets</Button>} />;
  }

  return (
    <Box>
      <PageHeader
        eyebrow="INVENTORY"
        title={
          <Stack direction="row" spacing={1.5} alignItems="center" flexWrap="wrap">
            <span>{asset.name}</span>
            <CodeTag>{asset.assetCode}</CodeTag>
            <StatusChip value={asset.archived ? 'ARCHIVED' : asset.status} size="medium" />
          </Stack>
        }
        crumbs={[{ label: 'Assets', to: '/assets' }, { label: asset.assetCode }]}
        subtitle={`${asset.categoryName} · ${asset.locationName}${asset.custodianName ? ` · Custodian: ${asset.custodianName}` : ''}`}
        actions={
          <>
            {hasPermission('RESERVATION_CREATE') && asset.reservable && !asset.archived && (
              <Button
                variant="contained"
                startIcon={<EventAvailableOutlinedIcon />}
                onClick={() => setReserveOpen(true)}
              >
                Reserve
              </Button>
            )}
            <Tooltip title="QR label">
              <Button variant="outlined" startIcon={<QrCode2OutlinedIcon />} onClick={toggleQr}>
                QR label
              </Button>
            </Tooltip>
            {hasPermission('ASSET_EDIT') && !asset.archived && (
              <Button variant="outlined" startIcon={<EditOutlinedIcon />}
                      onClick={() => navigate(`/assets/${id}/edit`)}>
                Edit
              </Button>
            )}
            {hasPermission('ASSET_ARCHIVE') && (
              <Button
                variant="outlined"
                color={asset.archived ? 'primary' : 'error'}
                startIcon={asset.archived ? <UnarchiveOutlinedIcon /> : <ArchiveOutlinedIcon />}
                onClick={() => setConfirmArchive(true)}
              >
                {asset.archived ? 'Restore' : 'Archive'}
              </Button>
            )}
          </>
        }
      />

      {qrOpen && qrUrl && (
        <Card variant="outlined" sx={{ mb: 2, maxWidth: 360 }}>
          <CardContent sx={{ textAlign: 'center' }}>
            <img src={qrUrl} alt={`QR code for ${asset.assetCode}`}
                 width={200} height={200} style={{ imageRendering: 'pixelated' }} />
            <Typography variant="body2" color="text.secondary">
              Scan to identify <b>{asset.assetCode}</b>
            </Typography>
          </CardContent>
        </Card>
      )}

      <Tabs value={tab} onChange={(_, value) => setTab(value)} sx={{ mb: 2 }}
            variant="scrollable" allowScrollButtonsMobile>
        <Tab label="Overview" />
        <Tab label="Reservations" />
        <Tab label="Check-out history" />
        <Tab label="Maintenance" />
        <Tab label="Transfers" />
        <Tab label={`Documents (${documentsQuery.data?.length ?? 0})`} />
      </Tabs>

      {tab === 0 && (
        <Stack spacing={2}>
          {asset.nextAvailableAt && (
            <Alert severity="info">
              Currently checked out — next available: {formatDateTime(asset.nextAvailableAt)}
            </Alert>
          )}
          <Card variant="outlined">
            <CardContent>
              <Typography variant="h6" gutterBottom>Identification</Typography>
              <Grid container spacing={2}>
                <Fact label="Type" value={titleCase(asset.assetType)} />
                <Fact label="Brand" value={asset.brand} />
                <Fact label="Model" value={asset.model} />
                <Fact label="Manufacturer" value={asset.manufacturer} />
                <Fact label="Serial number" value={asset.serialNumber ? <CodeTag muted>{asset.serialNumber}</CodeTag> : '—'} />
                <Fact label="Barcode" value={asset.barcode ? <CodeTag muted>{asset.barcode}</CodeTag> : '—'} />
                <Fact label="Condition" value={titleCase(asset.condition)} />
                <Fact label="Quantity" value={`${asset.availableQuantity} available of ${asset.quantity}`} />
              </Grid>
              {asset.description && (
                <>
                  <Divider sx={{ my: 2 }} />
                  <Typography variant="body2" color="text.secondary">{asset.description}</Typography>
                </>
              )}
            </CardContent>
          </Card>

          <Card variant="outlined">
            <CardContent>
              <Typography variant="h6" gutterBottom>Location and reservation rules</Typography>
              <Grid container spacing={2}>
                <Fact label="Faculty" value={asset.facultyName} />
                <Fact label="Department" value={asset.departmentName} />
                <Fact label="Location" value={asset.locationName} />
                <Fact label="Location notes" value={asset.locationNotes} />
                <Fact label="Reservable" value={asset.reservable ? 'Yes' : 'No'} />
                <Fact label="Approval required" value={asset.approvalRequired ? 'Yes' : 'No'} />
                <Fact label="External use" value={asset.externalUseAllowed ? 'Allowed' : 'Not allowed'} />
                <Fact label="Max duration" value={asset.maxReservationHours ? `${asset.maxReservationHours} h` : 'No limit'} />
                <Fact label="Deposit" value={asset.depositRequired ? formatMoney(asset.depositAmount, asset.currency) : 'Not required'} />
              </Grid>
            </CardContent>
          </Card>

          <Card variant="outlined">
            <CardContent>
              <Typography variant="h6" gutterBottom>Financial</Typography>
              <Grid container spacing={2}>
                <Fact label="Purchase price" value={formatMoney(asset.purchasePrice, asset.currency)} />
                <Fact label="Current book value" value={formatMoney(asset.currentBookValue, asset.currency)} />
                <Fact label="Purchase date" value={formatDate(asset.purchaseDate)} />
                <Fact label="PO number" value={asset.purchaseOrderNumber} />
                <Fact label="Invoice number" value={asset.invoiceNumber} />
                <Fact label="Funding source" value={asset.fundingSource} />
                <Fact label="Depreciation" value={asset.depreciationMethod} />
              </Grid>
            </CardContent>
          </Card>

          <Card variant="outlined">
            <CardContent>
              <Typography variant="h6" gutterBottom>Warranty, service and calibration</Typography>
              <Grid container spacing={2}>
                <Fact label="Warranty" value={
                  asset.warrantyEndDate
                    ? `${formatDate(asset.warrantyStartDate)} → ${formatDate(asset.warrantyEndDate)}`
                    : 'Not recorded'} />
                <Fact label="Warranty provider" value={asset.warrantyProvider} />
                <Fact label="Last service" value={formatDate(asset.lastServiceDate)} />
                <Fact label="Next service" value={formatDate(asset.nextServiceDate)} />
                <Fact label="Calibration" value={asset.calibrationRequired ? 'Required' : 'Not required'} />
                {asset.calibrationRequired && (
                  <>
                    <Fact label="Last calibration" value={formatDate(asset.lastCalibrationDate)} />
                    <Fact label="Next calibration" value={formatDate(asset.nextCalibrationDate)} />
                  </>
                )}
              </Grid>
            </CardContent>
          </Card>
        </Stack>
      )}

      {tab === 1 && (
        <Card variant="outlined">
          {(reservationsQuery.data ?? []).length === 0 ? (
            <EmptyState title="No reservations for this asset" />
          ) : (
            <List>
              {(reservationsQuery.data ?? []).map((r) => (
                <ListItem key={r.id} divider>
                  <ListItemText
                    primary={<Stack direction="row" spacing={1} alignItems="center">
                      <CodeTag>{r.reservationNumber}</CodeTag>
                      <Typography variant="body2">{r.purpose}</Typography>
                    </Stack>}
                    secondary={`${r.requestedByName} · ${formatDateTime(r.startAt)} → ${formatDateTime(r.endAt)} · qty ${r.quantity}`}
                  />
                  <StatusChip value={r.status} />
                </ListItem>
              ))}
            </List>
          )}
        </Card>
      )}

      {tab === 2 && (
        <Card variant="outlined">
          {(checkoutsQuery.data ?? []).length === 0 ? (
            <EmptyState title="No check-outs recorded" />
          ) : (
            <List>
              {(checkoutsQuery.data ?? []).map((c) => (
                <ListItem key={c.id} divider>
                  <ListItemText
                    primary={<Stack direction="row" spacing={1} alignItems="center">
                      <CodeTag>{c.checkoutNumber}</CodeTag>
                      <Typography variant="body2">{c.userName}</Typography>
                    </Stack>}
                    secondary={`Out ${formatDateTime(c.checkedOutAt)} · due ${formatDateTime(c.expectedReturnAt)}${c.returnedAt ? ` · returned ${formatDateTime(c.returnedAt)}` : ''}`}
                  />
                  <StatusChip value={c.status} />
                </ListItem>
              ))}
            </List>
          )}
        </Card>
      )}

      {tab === 3 && (
        <Card variant="outlined">
          {(maintenanceQuery.data ?? []).length === 0 ? (
            <EmptyState title="No maintenance history" />
          ) : (
            <List>
              {(maintenanceQuery.data ?? []).map((m) => (
                <ListItem key={m.id} divider>
                  <ListItemText
                    primary={<Stack direction="row" spacing={1} alignItems="center">
                      <CodeTag>{m.requestNumber}</CodeTag>
                      <Typography variant="body2">{titleCase(m.issueType)} — {m.description}</Typography>
                    </Stack>}
                    secondary={`Opened ${formatDateTime(m.openedAt)}${m.totalCost ? ` · cost ${formatMoney(m.totalCost)}` : ''}`}
                  />
                  <StatusChip value={m.status} />
                </ListItem>
              ))}
            </List>
          )}
        </Card>
      )}

      {tab === 4 && (
        <Card variant="outlined">
          {(transfersQuery.data ?? []).length === 0 ? (
            <EmptyState title="No transfers recorded" />
          ) : (
            <List>
              {(transfersQuery.data ?? []).map((t) => (
                <ListItem key={t.id} divider>
                  <ListItemText
                    primary={<Stack direction="row" spacing={1} alignItems="center">
                      <CodeTag>{t.transferNumber}</CodeTag>
                      <Typography variant="body2">{t.fromLocationName} → {t.toLocationName}</Typography>
                    </Stack>}
                    secondary={`${t.reason} · requested by ${t.requestedByName}`}
                  />
                  <StatusChip value={t.status} />
                </ListItem>
              ))}
            </List>
          )}
        </Card>
      )}

      {tab === 5 && (
        <Card variant="outlined">
          <CardContent>
            <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
              <Typography variant="h6">Documents</Typography>
              {hasPermission('ASSET_EDIT') && (
                <Button component="label" variant="outlined" startIcon={<UploadFileOutlinedIcon />}>
                  Upload document
                  <input hidden type="file" accept=".pdf,.jpg,.jpeg,.png,.docx,.xlsx" onChange={uploadDocument} />
                </Button>
              )}
            </Stack>
            {(documentsQuery.data ?? []).length === 0 ? (
              <EmptyState title="No documents attached"
                          hint="Attach invoices, warranties, manuals or certificates (PDF, JPG, PNG, DOCX, XLSX up to 10 MB)." />
            ) : (
              <List>
                {(documentsQuery.data ?? []).map((doc) => (
                  <ListItem
                    key={doc.id}
                    divider
                    secondaryAction={
                      <Stack direction="row" spacing={0.5}>
                        <IconButton aria-label={`Download ${doc.originalFilename}`}
                                    onClick={() => downloadDocument(doc)}>
                          <DownloadOutlinedIcon />
                        </IconButton>
                        {hasPermission('ASSET_EDIT') && (
                          <IconButton aria-label={`Delete ${doc.originalFilename}`}
                                      onClick={() => deleteDocument(doc)}>
                            <DeleteOutlineIcon />
                          </IconButton>
                        )}
                      </Stack>
                    }
                  >
                    <ListItemText
                      primary={doc.originalFilename}
                      secondary={`${titleCase(doc.documentType)} · ${(doc.sizeBytes / 1024).toFixed(0)} KB · ${formatDateTime(doc.uploadedAt)}`}
                    />
                    <Chip label={doc.mimeType.split('/')[1]?.toUpperCase() ?? 'FILE'} size="small" sx={{ mr: 6 }} />
                  </ListItem>
                ))}
              </List>
            )}
          </CardContent>
        </Card>
      )}

      <ConfirmDialog
        open={confirmArchive}
        title={asset.archived ? 'Restore asset' : 'Archive asset'}
        message={asset.archived
          ? `Restore ${asset.assetCode}? It becomes available for reservations again.`
          : `Archive ${asset.assetCode}? It is hidden from active lists but its full history is preserved.`}
        confirmLabel={asset.archived ? 'Restore' : 'Archive'}
        destructive={!asset.archived}
        busy={archiveMutation.isPending}
        onConfirm={() => archiveMutation.mutate()}
        onClose={() => setConfirmArchive(false)}
      />

      <BookingDialog
        open={reserveOpen}
        onClose={() => setReserveOpen(false)}
        initialType="equipment"
        initialAsset={asset}
      />
    </Box>
  );
}
