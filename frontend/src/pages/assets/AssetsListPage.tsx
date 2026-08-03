import { useMemo, useState } from 'react';
import { useNavigate, Link as RouterLink } from 'react-router-dom';
import {
  Alert, Box, Button, Card, CardActionArea, CardContent,
  Chip, Grid, IconButton, InputAdornment, MenuItem,
  Skeleton, Stack, TextField, Tooltip, Typography,
} from '@mui/material';
import { alpha } from '@mui/material/styles';
import AddIcon from '@mui/icons-material/Add';
import ArrowBackIosNewRoundedIcon from '@mui/icons-material/ArrowBackIosNewRounded';
import SearchIcon from '@mui/icons-material/Search';
import FileDownloadOutlinedIcon from '@mui/icons-material/FileDownloadOutlined';
import BiotechOutlinedIcon from '@mui/icons-material/BiotechOutlined';
import ElectricBoltOutlinedIcon from '@mui/icons-material/ElectricBoltOutlined';
import ComputerOutlinedIcon from '@mui/icons-material/ComputerOutlined';
import CameraOutlinedIcon from '@mui/icons-material/CameraOutlined';
import StraightenOutlinedIcon from '@mui/icons-material/StraightenOutlined';
import ChairOutlinedIcon from '@mui/icons-material/ChairOutlined';
import InventoryOutlinedIcon from '@mui/icons-material/InventoryOutlined';
import EventAvailableOutlinedIcon from '@mui/icons-material/EventAvailableOutlined';
import { useQuery, keepPreviousData } from '@tanstack/react-query';
import type { GridColDef, GridPaginationModel, GridSortModel } from '@mui/x-data-grid';
import type { SvgIconComponent } from '@mui/icons-material';
import { api } from '../../api/client';
import type { ApiEnvelope, Page } from '../../api/client';
import type { AssetSummary } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import { useCategories } from '../../api/referenceData';
import PageHeader from '../../components/common/PageHeader';
import StatusChip from '../../components/common/StatusChip';
import CodeTag from '../../components/common/CodeTag';
import ServerDataGrid from '../../components/tables/ServerDataGrid';
import BookingDialog from '../../components/common/BookingDialog';
import { formatMoney } from '../../utils/format';

// ─── Asset category group configuration ───────────────────────────────────────

interface AssetGroup {
  key: string;
  label: string;
  sublabel: string;
  Icon: SvgIconComponent;
  accent: string;
  /** Partial category name matches (case-insensitive) */
  categoryNameKeywords: string[];
}

const ASSET_GROUPS: AssetGroup[] = [
  {
    key: 'lab',
    label: 'Lab Equipment',
    sublabel: 'Oscilloscopes, balances, centrifuges, autoclaves…',
    Icon: BiotechOutlinedIcon,
    accent: '#2563EB',
    categoryNameKeywords: [
      'oscilloscope', 'multimeter', 'function generator', 'power supply', 'laser',
      'optical bench', 'magnetic stirrer', 'hot plate', 'fume hood', 'ph meter',
      'incubator', 'tissue culture', 'autoclave', 'growth chamber', 'centrifuge',
      'dissection', 'lab equip',
    ],
  },
  {
    key: 'electronics',
    label: 'Electronics & AV',
    sublabel: 'Projectors, PA systems, microphones, amplifiers…',
    Icon: ElectricBoltOutlinedIcon,
    accent: '#D97706',
    categoryNameKeywords: ['av', 'audio', 'visual', 'projector', 'pa system', 'microphone', 'amplifier', 'speaker', 'electronics'],
  },
  {
    key: 'computing',
    label: 'Computing',
    sublabel: 'Desktop computers, laptops, servers…',
    Icon: ComputerOutlinedIcon,
    accent: '#0891B2',
    categoryNameKeywords: ['computing', 'computer', 'laptop', 'server'],
  },
  {
    key: 'optics',
    label: 'Optics & Microscopy',
    sublabel: 'Microscopes, spectrometers, optical benches…',
    Icon: CameraOutlinedIcon,
    accent: '#7C3AED',
    categoryNameKeywords: ['optic', 'microscope', 'spectrometer'],
  },
  {
    key: 'measurement',
    label: 'Measurement Tools',
    sublabel: 'Balances, calipers, micrometers, UV-Vis…',
    Icon: StraightenOutlinedIcon,
    accent: '#059669',
    categoryNameKeywords: ['measure', 'balance', 'caliper', 'micrometer', 'spectrophotometer'],
  },
  {
    key: 'furniture',
    label: 'Furniture',
    sublabel: 'Whiteboards, podiums, desks, chairs…',
    Icon: ChairOutlinedIcon,
    accent: '#6B7280',
    categoryNameKeywords: ['furniture', 'whiteboard', 'podium', 'desk', 'chair'],
  },
  {
    key: 'other',
    label: 'Other Assets',
    sublabel: 'Everything else in the asset register',
    Icon: InventoryOutlinedIcon,
    accent: '#94A3B8',
    categoryNameKeywords: [], // catch-all
  },
];

const STATUSES = ['AVAILABLE', 'RESERVED', 'CHECKED_OUT', 'UNDER_MAINTENANCE', 'DAMAGED', 'LOST', 'ARCHIVED'];

/** Check if an asset's categoryName belongs to the given group */
function assetMatchesGroup(categoryName: string | undefined, group: AssetGroup, knownCategories: Set<string>): boolean {
  const cn = (categoryName ?? '').toLowerCase();
  if (group.key === 'other') {
    // belongs to "other" if it doesn't match any named group
    return !knownCategories.has(cn);
  }
  return group.categoryNameKeywords.some((kw) => cn.includes(kw));
}

// Build set of category names claimed by non-other groups
function buildKnownSet(): Set<string> {
  const s = new Set<string>();
  ASSET_GROUPS.filter((g) => g.key !== 'other').forEach((g) => {
    g.categoryNameKeywords.forEach((kw) => s.add(kw));
  });
  return s;
}
const KNOWN_CATEGORY_KEYWORDS = buildKnownSet();

// ─── Hub card ─────────────────────────────────────────────────────────────────

function AssetHubCard({
  group, count, loading, onClick,
}: { group: AssetGroup; count: number; loading: boolean; onClick: () => void }) {
  const { Icon, accent, label, sublabel } = group;
  return (
    <Card
      variant="outlined"
      sx={{
        height: '100%', borderRadius: 3,
        transition: 'box-shadow 0.18s, transform 0.18s',
        '&:hover': { boxShadow: 4, transform: 'translateY(-2px)' },
        borderColor: alpha(accent, 0.25),
      }}
    >
      <CardActionArea onClick={onClick} sx={{ height: '100%', p: 2.5 }}>
        <Stack spacing={1.5}>
          <Box sx={{ width: 48, height: 48, borderRadius: 2, display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: alpha(accent, 0.1), color: accent }}>
            <Icon sx={{ fontSize: 26 }} />
          </Box>
          <Box>
            <Typography variant="subtitle1" fontWeight={700} lineHeight={1.2}>{label}</Typography>
            <Typography variant="caption" color="text.secondary">{sublabel}</Typography>
          </Box>
          {loading ? <Skeleton width={70} height={20} /> : (
            <Chip label={`${count} asset${count !== 1 ? 's' : ''}`} size="small"
                  sx={{ alignSelf: 'flex-start', backgroundColor: alpha(accent, 0.1), color: accent, fontWeight: 600 }} />
          )}
        </Stack>
      </CardActionArea>
    </Card>
  );
}

// ─── Main page ────────────────────────────────────────────────────────────────

const BLOCKED_ASSET_STATUSES = ['UNDER_MAINTENANCE', 'DAMAGED', 'LOST', 'DISPOSED'];

export default function AssetsListPage() {
  const navigate = useNavigate();
  const { hasPermission } = useAuth();

  const [activeGroup, setActiveGroup] = useState<AssetGroup | null>(null);
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [pagination, setPagination] = useState<GridPaginationModel>({ page: 0, pageSize: 20 });
  const [sortModel, setSortModel] = useState<GridSortModel>([{ field: 'name', sort: 'asc' }]);
  const [reserveTargetAsset, setReserveTargetAsset] = useState<AssetSummary | null>(null);

  const canReserve = hasPermission('RESERVATION_CREATE');
  const categories = useCategories();

  // Resolve active group's category IDs for the API call
  const activeCategoryIds = useMemo(() => {
    if (!activeGroup || activeGroup.key === 'other') return [];
    const keywords = activeGroup.categoryNameKeywords;
    return (categories.data ?? [])
      .filter((c) => keywords.some((kw) => c.name.toLowerCase().includes(kw)))
      .map((c) => c.id);
  }, [activeGroup, categories.data]);

  // Hub pre-fetch (larger page to get good counts)
  const hubQuery = useQuery({
    queryKey: ['assets-hub-counts'],
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<AssetSummary>>>('/assets', { params: { size: 500 } })).data.data.content,
    staleTime: 2 * 60_000,
  });

  const hubAssets = hubQuery.data ?? [];

  const groupCounts = useMemo(
    () => ASSET_GROUPS.map((g) => ({
      key: g.key,
      count: hubAssets.filter((a) => {
        const cn = (a.categoryName ?? '').toLowerCase();
        if (g.key === 'other') {
          return !ASSET_GROUPS.filter((x) => x.key !== 'other')
            .some((x) => x.categoryNameKeywords.some((kw) => cn.includes(kw)));
        }
        return g.categoryNameKeywords.some((kw) => cn.includes(kw));
      }).length,
    })),
    [hubAssets],
  );

  // Build API params for drill-down
  const drillParams = useMemo(() => {
    if (!activeGroup) return null;
    return {
      search: search || undefined,
      status: status || undefined,
      // Pass first matched category ID if available; for "other" no category filter
      categoryId: activeCategoryIds[0] ?? undefined,
      page: pagination.page,
      size: pagination.pageSize,
      sort: sortModel[0] ? `${sortModel[0].field},${sortModel[0].sort}` : undefined,
    };
  }, [activeGroup, search, status, pagination, sortModel, activeCategoryIds]);

  const drillQuery = useQuery({
    queryKey: ['assets', drillParams],
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<AssetSummary>>>('/assets', { params: drillParams! })).data.data,
    enabled: !!drillParams,
    placeholderData: keepPreviousData,
  });

  const exportCsv = () => {
    const q = new URLSearchParams();
    if (search) q.set('search', search);
    if (status) q.set('status', status);
    q.set('format', 'csv');
    api.get(`/reports/assets?${q.toString()}`, { responseType: 'blob' }).then((response) => {
      const url = URL.createObjectURL(response.data as Blob);
      const link = document.createElement('a');
      link.href = url; link.download = 'asset-register.csv'; link.click();
      URL.revokeObjectURL(url);
    });
  };

  const columns: GridColDef<AssetSummary>[] = [
    { field: 'assetCode', headerName: 'Code', width: 140, renderCell: ({ row }) => <CodeTag>{row.assetCode}</CodeTag> },
    { field: 'name', headerName: 'Asset', flex: 1.5, minWidth: 180 },
    { field: 'categoryName', headerName: 'Category', flex: 1, minWidth: 140, sortable: false },
    { field: 'locationName', headerName: 'Location', flex: 1, minWidth: 150, sortable: false },
    { field: 'status', headerName: 'Status', width: 155, renderCell: ({ row }) => <StatusChip value={row.archived ? 'ARCHIVED' : row.status} /> },
    { field: 'availableQuantity', headerName: 'Avail / Total', width: 110, sortable: false, valueGetter: (_, row) => `${row.availableQuantity} / ${row.quantity}` },
    { field: 'purchasePrice', headerName: 'Value', width: 130, align: 'right', headerAlign: 'right', valueFormatter: (value: number | undefined, row) => formatMoney(value, row.currency) },
    {
      field: 'actions', headerName: '', width: 120, sortable: false,
      renderCell: ({ row }) => {
        const canBookThis = canReserve && !row.archived && row.reservable && row.availableQuantity > 0 && !BLOCKED_ASSET_STATUSES.includes(row.status);
        if (!canBookThis) return null;
        return (
          <Button
            size="small"
            variant="outlined"
            startIcon={<EventAvailableOutlinedIcon />}
            onClick={(e) => {
              e.stopPropagation();
              setReserveTargetAsset(row);
            }}
          >
            Reserve
          </Button>
        );
      },
    },
  ];

  // ── Hub view ──────────────────────────────────────────────────────────────────
  if (!activeGroup) {
    return (
      <Box>
        <PageHeader
          eyebrow="INVENTORY"
          title="Assets"
          crumbs={[{ label: 'Assets' }]}
          subtitle="Browse the asset register by category group."
          actions={
            <>
              {hasPermission('REPORT_VIEW') && (
                <Button variant="outlined" startIcon={<FileDownloadOutlinedIcon />} onClick={exportCsv}>Export CSV</Button>
              )}
              {hasPermission('ASSET_CREATE') && (
                <Button component={RouterLink} to="/assets/new" variant="contained" startIcon={<AddIcon />}>Add asset</Button>
              )}
            </>
          }
        />
        {hubQuery.isError && <Alert severity="error" sx={{ mb: 2 }}>Failed to load asset counts.</Alert>}
        <Grid container spacing={2}>
          {ASSET_GROUPS.map((group) => {
            const gc = groupCounts.find((c) => c.key === group.key);
            return (
              <Grid key={group.key} size={{ xs: 12, sm: 6, md: 4 }}>
                <AssetHubCard
                  group={group}
                  count={gc?.count ?? 0}
                  loading={hubQuery.isLoading}
                  onClick={() => { setSearch(''); setStatus(''); setPagination({ page: 0, pageSize: 20 }); setActiveGroup(group); }}
                />
              </Grid>
            );
          })}
        </Grid>
      </Box>
    );
  }

  // ── Drill-down view ───────────────────────────────────────────────────────────
  const { accent, label, sublabel } = activeGroup;

  return (
    <Box>
      <PageHeader
        eyebrow="ASSETS"
        title={label}
        crumbs={[{ label: 'Assets', onClick: () => setActiveGroup(null) }, { label }]}
        subtitle={sublabel}
        actions={
          <Stack direction="row" spacing={1}>
            {hasPermission('REPORT_VIEW') && (
              <Button variant="outlined" startIcon={<FileDownloadOutlinedIcon />} onClick={exportCsv}>Export CSV</Button>
            )}
            {hasPermission('ASSET_CREATE') && (
              <Button component={RouterLink} to="/assets/new" variant="contained" startIcon={<AddIcon />}>Add asset</Button>
            )}
            <Tooltip title="Back to categories">
              <IconButton onClick={() => setActiveGroup(null)} size="small" sx={{ border: '1px solid', borderColor: 'divider' }}>
                <ArrowBackIosNewRoundedIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          </Stack>
        }
      />
      <Card variant="outlined" sx={{ p: 1.5, mb: 2 }}>
        <Stack direction="row" spacing={1.5} alignItems="center" flexWrap="wrap" useFlexGap>
          <TextField
            size="small" placeholder="Search name, code, serial…" value={search}
            sx={{ minWidth: 240 }}
            onChange={(e) => { setSearch(e.target.value); setPagination((p) => ({ ...p, page: 0 })); }}
            InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> }}
          />
          <TextField select size="small" label="Status" value={status} sx={{ minWidth: 170 }}
                     onChange={(e) => setStatus(e.target.value)}>
            <MenuItem value="">All statuses</MenuItem>
            {STATUSES.map((s) => <MenuItem key={s} value={s}>{s.replaceAll('_', ' ')}</MenuItem>)}
          </TextField>
          {(search || status) && <Button size="small" onClick={() => { setSearch(''); setStatus(''); }}>Clear</Button>}
        </Stack>
      </Card>
      <ServerDataGrid<AssetSummary>
        columns={columns}
        page={drillQuery.data}
        loading={drillQuery.isLoading || drillQuery.isFetching}
        paginationModel={pagination}
        onPaginationModelChange={setPagination}
        sortModel={sortModel}
        onSortModelChange={setSortModel}
        onRowClick={(row) => navigate(`/assets/${row.id}`)}
        emptyTitle="No assets found"
        emptyHint="Try a different search or status filter."
      />
      <BookingDialog
        open={!!reserveTargetAsset}
        onClose={() => setReserveTargetAsset(null)}
        initialType="equipment"
        initialAsset={reserveTargetAsset}
      />
    </Box>
  );
}
