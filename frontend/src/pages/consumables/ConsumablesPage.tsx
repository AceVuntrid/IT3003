import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Alert, Box, Button, Card, CardActionArea, CardContent,
  Chip, Grid, IconButton, InputAdornment, MenuItem,
  Skeleton, Stack, TextField, Tooltip, Typography,
} from '@mui/material';
import { alpha } from '@mui/material/styles';
import AddIcon from '@mui/icons-material/Add';
import ArrowBackIosNewRoundedIcon from '@mui/icons-material/ArrowBackIosNewRounded';
import SearchIcon from '@mui/icons-material/Search';
import ScienceOutlinedIcon from '@mui/icons-material/ScienceOutlined';
import LocalPharmacyOutlinedIcon from '@mui/icons-material/LocalPharmacyOutlined';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import HealthAndSafetyOutlinedIcon from '@mui/icons-material/HealthAndSafetyOutlined';
import CleaningServicesOutlinedIcon from '@mui/icons-material/CleaningServicesOutlined';
import InventoryOutlinedIcon from '@mui/icons-material/InventoryOutlined';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import EventAvailableOutlinedIcon from '@mui/icons-material/EventAvailableOutlined';
import { useQuery, keepPreviousData } from '@tanstack/react-query';
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import type { SvgIconComponent } from '@mui/icons-material';
import { api } from '../../api/client';
import type { ApiEnvelope, Page } from '../../api/client';
import type { ConsumableSummary } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import { useCategories } from '../../api/referenceData';
import PageHeader from '../../components/common/PageHeader';
import CodeTag from '../../components/common/CodeTag';
import ServerDataGrid from '../../components/tables/ServerDataGrid';
import BookingDialog from '../../components/common/BookingDialog';
import ConsumableFormDialog from './ConsumableFormDialog';
import { formatDate } from '../../utils/format';

// ─── Group config ──────────────────────────────────────────────────────────────

interface ConsumableGroup {
  key: string;
  label: string;
  sublabel: string;
  Icon: SvgIconComponent;
  accent: string;
  categoryNameKeywords: string[];
}

const CONSUMABLE_GROUPS: ConsumableGroup[] = [
  {
    key: 'chemicals',
    label: 'Chemicals',
    sublabel: 'Reagents, solvents, acids, bases and other lab chemicals',
    Icon: ScienceOutlinedIcon,
    accent: '#DC2626',
    categoryNameKeywords: ['chemical', 'reagent', 'solvent', 'acid', 'base', 'naoh', 'hcl'],
  },
  {
    key: 'lab-supplies',
    label: 'Lab Supplies',
    sublabel: 'Glassware, pipette tips, filter papers, sample bottles…',
    Icon: LocalPharmacyOutlinedIcon,
    accent: '#2563EB',
    categoryNameKeywords: ['glassware', 'pipette', 'filter paper', 'sample bottle', 'lab supply', 'lab-supply'],
  },
  {
    key: 'stationery',
    label: 'Stationery & Office',
    sublabel: 'Printer paper, toner, markers, whiteboard erasers…',
    Icon: DescriptionOutlinedIcon,
    accent: '#0891B2',
    categoryNameKeywords: ['stationery', 'paper', 'toner', 'cartridge', 'marker', 'eraser', 'pen'],
  },
  {
    key: 'safety',
    label: 'Safety Equipment',
    sublabel: 'Gloves, face masks, goggles, protective wear…',
    Icon: HealthAndSafetyOutlinedIcon,
    accent: '#D97706',
    categoryNameKeywords: ['safety', 'glove', 'mask', 'goggle', 'protective'],
  },
  {
    key: 'cleaning',
    label: 'Cleaning Materials',
    sublabel: 'Surface cleaners, wipes, disinfectants…',
    Icon: CleaningServicesOutlinedIcon,
    accent: '#059669',
    categoryNameKeywords: ['cleaning', 'cleaner', 'wipe', 'disinfect'],
  },
  {
    key: 'other',
    label: 'Other Supplies',
    sublabel: 'All other tracked consumable stock',
    Icon: InventoryOutlinedIcon,
    accent: '#94A3B8',
    categoryNameKeywords: [],
  },
];

// ─── Hub card ─────────────────────────────────────────────────────────────────

function ConsumableHubCard({
  group, count, lowStockCount, loading, onClick,
}: { group: ConsumableGroup; count: number; lowStockCount: number; loading: boolean; onClick: () => void }) {
  const { Icon, accent, label, sublabel } = group;
  return (
    <Card variant="outlined" sx={{ height: '100%', borderRadius: 3, transition: 'box-shadow 0.18s, transform 0.18s', '&:hover': { boxShadow: 4, transform: 'translateY(-2px)' }, borderColor: alpha(accent, 0.25) }}>
      <CardActionArea onClick={onClick} sx={{ height: '100%', p: 2.5 }}>
        <Stack spacing={1.5}>
          <Box sx={{ width: 48, height: 48, borderRadius: 2, display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: alpha(accent, 0.1), color: accent }}>
            <Icon sx={{ fontSize: 26 }} />
          </Box>
          <Box>
            <Typography variant="subtitle1" fontWeight={700} lineHeight={1.2}>{label}</Typography>
            <Typography variant="caption" color="text.secondary">{sublabel}</Typography>
          </Box>
          {loading ? <Skeleton width={80} height={20} /> : (
            <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap>
              <Chip label={`${count} item${count !== 1 ? 's' : ''}`} size="small"
                    sx={{ backgroundColor: alpha(accent, 0.1), color: accent, fontWeight: 600 }} />
              {lowStockCount > 0 && (
                <Chip icon={<WarningAmberOutlinedIcon />} label={`${lowStockCount} low`} size="small" color="warning" variant="outlined" />
              )}
            </Stack>
          )}
        </Stack>
      </CardActionArea>
    </Card>
  );
}

// ─── Main page ────────────────────────────────────────────────────────────────

export default function ConsumablesPage() {
  const navigate = useNavigate();
  const { hasPermission } = useAuth();

  const [activeGroup, setActiveGroup] = useState<ConsumableGroup | null>(null);
  const [search, setSearch] = useState('');
  const [lowStockOnly, setLowStockOnly] = useState(false);
  const [pagination, setPagination] = useState<GridPaginationModel>({ page: 0, pageSize: 20 });
  const [createOpen, setCreateOpen] = useState(false);
  const [reserveTargetConsumable, setReserveTargetConsumable] = useState<ConsumableSummary | null>(null);

  const canReserve = hasPermission('RESERVATION_CREATE');
  const categories = useCategories('CONSUMABLE');

  // Resolve category IDs for the active group (for the API call)
  const activeCategoryId = useMemo(() => {
    if (!activeGroup || activeGroup.key === 'other') return undefined;
    const keywords = activeGroup.categoryNameKeywords;
    const matched = (categories.data ?? []).find((c) =>
      keywords.some((kw) => c.name.toLowerCase().includes(kw)),
    );
    return matched?.id;
  }, [activeGroup, categories.data]);

  // Hub pre-fetch
  const hubQuery = useQuery({
    queryKey: ['consumables-hub'],
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<ConsumableSummary>>>('/consumables', { params: { size: 300 } })).data.data.content,
    staleTime: 2 * 60_000,
  });

  const hubItems = hubQuery.data ?? [];

  const groupStats = useMemo(
    () => CONSUMABLE_GROUPS.map((g) => {
      const matchItem = (item: ConsumableSummary) => {
        const cn = (item.categoryName ?? '').toLowerCase();
        if (g.key === 'other') {
          return !CONSUMABLE_GROUPS.filter((x) => x.key !== 'other')
            .some((x) => x.categoryNameKeywords.some((kw) => cn.includes(kw)));
        }
        return g.categoryNameKeywords.some((kw) => cn.includes(kw));
      };
      const items = hubItems.filter(matchItem);
      return { key: g.key, count: items.length, lowStock: items.filter((i) => i.lowStock).length };
    }),
    [hubItems],
  );

  const drillParams = activeGroup
    ? { search: search || undefined, categoryId: activeCategoryId, lowStock: lowStockOnly || undefined, page: pagination.page, size: pagination.pageSize }
    : null;

  const drillQuery = useQuery({
    queryKey: ['consumables', drillParams],
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<ConsumableSummary>>>('/consumables', { params: drillParams! })).data.data,
    enabled: !!drillParams,
    placeholderData: keepPreviousData,
  });

  const columns: GridColDef<ConsumableSummary>[] = [
    { field: 'itemCode', headerName: 'Code', width: 140, sortable: false, renderCell: ({ row }) => <CodeTag>{row.itemCode}</CodeTag> },
    {
      field: 'name', headerName: 'Item', flex: 1.4, minWidth: 180,
      renderCell: ({ row }) => (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          {row.name}
          {row.hazardous && <Chip icon={<WarningAmberOutlinedIcon />} label="Hazardous" size="small" color="warning" variant="outlined" />}
        </Box>
      ),
    },
    { field: 'categoryName', headerName: 'Category', flex: 1, minWidth: 130, sortable: false },
    { field: 'locationName', headerName: 'Store', flex: 1, minWidth: 140, sortable: false },
    { field: 'currentQuantity', headerName: 'On hand', width: 120, sortable: false, valueGetter: (_, row) => `${row.currentQuantity} ${row.unitOfMeasure}` },
    {
      field: 'lowStock', headerName: 'Stock', width: 115, sortable: false,
      renderCell: ({ row }) => row.lowStock
        ? <Chip label="Low stock" size="small" color="error" />
        : <Chip label="OK" size="small" sx={{ backgroundColor: 'rgba(14,124,102,0.12)', color: '#0A5D4D' }} />,
    },
    { field: 'earliestExpiry', headerName: 'Expires', width: 120, sortable: false, valueFormatter: (value: string | undefined) => formatDate(value) },
    {
      field: 'actions', headerName: '', width: 120, sortable: false,
      renderCell: ({ row }) => {
        if (!canReserve || row.currentQuantity <= 0) return null;
        return (
          <Button
            size="small"
            variant="outlined"
            startIcon={<EventAvailableOutlinedIcon />}
            onClick={(e) => {
              e.stopPropagation();
              setReserveTargetConsumable(row);
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
          title="Consumables"
          crumbs={[{ label: 'Consumables' }]}
          subtitle="Track stock levels by category — chemicals, lab supplies, stationery and safety gear."
          actions={hasPermission('CONSUMABLE_CREATE') && (
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>Add consumable</Button>
          )}
        />
        {hubQuery.isError && <Alert severity="error" sx={{ mb: 2 }}>Failed to load consumable counts.</Alert>}
        <Grid container spacing={2}>
          {CONSUMABLE_GROUPS.map((group) => {
            const gs = groupStats.find((s) => s.key === group.key);
            return (
              <Grid key={group.key} size={{ xs: 12, sm: 6, md: 4 }}>
                <ConsumableHubCard
                  group={group}
                  count={gs?.count ?? 0}
                  lowStockCount={gs?.lowStock ?? 0}
                  loading={hubQuery.isLoading}
                  onClick={() => { setSearch(''); setLowStockOnly(false); setPagination({ page: 0, pageSize: 20 }); setActiveGroup(group); }}
                />
              </Grid>
            );
          })}
        </Grid>
        <ConsumableFormDialog open={createOpen} onClose={() => setCreateOpen(false)} />
      </Box>
    );
  }

  // ── Drill-down view ───────────────────────────────────────────────────────────
  const { accent, label, sublabel } = activeGroup;

  return (
    <Box>
      <PageHeader
        eyebrow="CONSUMABLES"
        title={label}
        crumbs={[{ label: 'Consumables', onClick: () => setActiveGroup(null) }, { label }]}
        subtitle={sublabel}
        actions={
          <Stack direction="row" spacing={1}>
            {hasPermission('CONSUMABLE_CREATE') && (
              <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>Add consumable</Button>
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
            size="small" placeholder="Search name or code…" value={search} sx={{ minWidth: 240 }}
            onChange={(e) => { setSearch(e.target.value); setPagination((p) => ({ ...p, page: 0 })); }}
            InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> }}
          />
          <TextField select size="small" label="Stock" value={lowStockOnly ? 'low' : ''} sx={{ minWidth: 150 }}
                     onChange={(e) => setLowStockOnly(e.target.value === 'low')}>
            <MenuItem value="">All stock levels</MenuItem>
            <MenuItem value="low">Low stock only</MenuItem>
          </TextField>
          {(search || lowStockOnly) && <Button size="small" onClick={() => { setSearch(''); setLowStockOnly(false); }}>Clear</Button>}
        </Stack>
      </Card>
      <ServerDataGrid<ConsumableSummary>
        columns={columns}
        page={drillQuery.data}
        loading={drillQuery.isLoading || drillQuery.isFetching}
        paginationModel={pagination}
        onPaginationModelChange={setPagination}
        onRowClick={(row) => navigate(`/consumables/${row.id}`)}
        emptyTitle="No consumables found"
        emptyHint="Try a different search or clear the low-stock filter."
      />
      <ConsumableFormDialog open={createOpen} onClose={() => setCreateOpen(false)} />
      <BookingDialog
        open={!!reserveTargetConsumable}
        onClose={() => setReserveTargetConsumable(null)}
        initialType="consumable"
        initialItemId={reserveTargetConsumable?.id}
      />
    </Box>
  );
}
