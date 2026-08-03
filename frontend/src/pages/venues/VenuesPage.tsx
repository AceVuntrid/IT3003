import { useMemo, useState } from 'react';
import {
  Alert, Box, Button, Card, CardActionArea, CardActions, CardContent,
  Chip, Grid, IconButton, InputAdornment, MenuItem, Skeleton,
  Stack, TextField, Tooltip, Typography,
} from '@mui/material';
import { alpha } from '@mui/material/styles';
import ArrowBackIosNewRoundedIcon from '@mui/icons-material/ArrowBackIosNewRounded';
import ApartmentOutlinedIcon from '@mui/icons-material/ApartmentOutlined';
import EventAvailableOutlinedIcon from '@mui/icons-material/EventAvailableOutlined';
import EventSeatOutlinedIcon from '@mui/icons-material/EventSeatOutlined';
import SearchIcon from '@mui/icons-material/Search';
import ScienceOutlinedIcon from '@mui/icons-material/ScienceOutlined';
import BiotechOutlinedIcon from '@mui/icons-material/BiotechOutlined';
import BlurOnOutlinedIcon from '@mui/icons-material/BlurOnOutlined';
import ParkOutlinedIcon from '@mui/icons-material/ParkOutlined';
import FunctionsOutlinedIcon from '@mui/icons-material/FunctionsOutlined';
import QueryStatsOutlinedIcon from '@mui/icons-material/QueryStatsOutlined';
import DomainOutlinedIcon from '@mui/icons-material/DomainOutlined';
import AccountBalanceOutlinedIcon from '@mui/icons-material/AccountBalanceOutlined';
import SchoolOutlinedIcon from '@mui/icons-material/SchoolOutlined';
import MeetingRoomOutlinedIcon from '@mui/icons-material/MeetingRoomOutlined';
import { useQuery } from '@tanstack/react-query';
import type { SvgIconComponent } from '@mui/icons-material';
import type { ReactNode } from 'react';
import { api, errorMessage } from '../../api/client';
import type { ApiEnvelope } from '../../api/client';
import type { Location } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import PageHeader from '../../components/common/PageHeader';
import CodeTag from '../../components/common/CodeTag';
import EmptyState from '../../components/common/EmptyState';
import BookingDialog from '../../components/common/BookingDialog';
import { formatNumber } from '../../utils/format';

// ─── Venue type display ────────────────────────────────────────────────────────

const VENUE_TYPES: Record<string, { label: string; bg: string; fg: string }> = {
  LECTURE_ROOM: { label: 'Lecture Room', bg: alpha('#3B82F6', 0.12), fg: '#1D4ED8' },
  AUDITORIUM:   { label: 'Auditorium',   bg: alpha('#A855F7', 0.14), fg: '#7E22CE' },
  LABORATORY:   { label: 'Laboratory',   bg: alpha('#6D28D9', 0.10), fg: '#5B21B6' },
  ROOM:         { label: 'Room',         bg: alpha('#6B7280', 0.12), fg: '#374151' },
};

function VenueTypeChip({ type }: { type: string }) {
  const meta = VENUE_TYPES[type];
  return (
    <Chip
      size="small"
      label={meta?.label ?? type.replaceAll('_', ' ')}
      sx={meta ? { backgroundColor: meta.bg, color: meta.fg, fontWeight: 500, fontSize: '0.7rem' } : undefined}
    />
  );
}

// ─── Department hub configuration ─────────────────────────────────────────────

interface HubGroup {
  key: string;               // matches parentName or departmentCode prefix in venue data
  label: string;
  sublabel: string;
  Icon: SvgIconComponent;
  accent: string;            // CSS color for icon/border highlight
  matchFn: (v: Location) => boolean;
}

const HUB_GROUPS: HubGroup[] = [
  {
    key: 'physics',
    label: 'Physics',
    sublabel: 'Department of Physics',
    Icon: ScienceOutlinedIcon,
    accent: '#2563EB',
    matchFn: (v) => !!(v.code?.startsWith('PHY-') || v.departmentName?.toLowerCase().includes('physics')),
  },
  {
    key: 'chemistry',
    label: 'Chemistry',
    sublabel: 'Department of Chemistry',
    Icon: BiotechOutlinedIcon,
    accent: '#059669',
    matchFn: (v) => !!(v.code?.startsWith('CHEM-') || v.departmentName?.toLowerCase().includes('chemistry')),
  },
  {
    key: 'zoology',
    label: 'Zoology',
    sublabel: 'Dept of Zoology & Environmental Sciences',
    Icon: BlurOnOutlinedIcon,
    accent: '#D97706',
    matchFn: (v) => !!(v.code?.startsWith('ZOO-') || v.departmentName?.toLowerCase().includes('zoology')),
  },
  {
    key: 'botany',
    label: 'Plant Sciences',
    sublabel: 'Department of Plant Sciences (Botany)',
    Icon: ParkOutlinedIcon,
    accent: '#16A34A',
    matchFn: (v) => !!(v.code?.startsWith('BOT-') || v.departmentName?.toLowerCase().includes('plant')),
  },
  {
    key: 'mathematics',
    label: 'Mathematics',
    sublabel: 'Department of Mathematics',
    Icon: FunctionsOutlinedIcon,
    accent: '#7C3AED',
    matchFn: (v) => !!(v.code?.startsWith('MATH-') || v.departmentName?.toLowerCase().includes('mathematics')),
  },
  {
    key: 'statistics',
    label: 'Statistics',
    sublabel: 'Department of Statistics',
    Icon: QueryStatsOutlinedIcon,
    accent: '#DB2777',
    matchFn: (v) => !!(v.code?.startsWith('STAT-') || v.departmentName?.toLowerCase().includes('statistics')),
  },
  {
    key: 'ssc',
    label: 'SSC',
    sublabel: "Science Students' Centre",
    Icon: DomainOutlinedIcon,
    accent: '#0891B2',
    matchFn: (v) => !!(v.code?.startsWith('SSC') || v.parentName?.toUpperCase().includes('SSC')),
  },
  {
    key: 'ilc',
    label: 'ILC',
    sublabel: 'Independence Learning Centre',
    Icon: AccountBalanceOutlinedIcon,
    accent: '#9333EA',
    matchFn: (v) => !!(v.code?.startsWith('ILC') || v.parentName?.toUpperCase().includes('ILC')),
  },
  {
    key: 'dean',
    label: "Dean's Office",
    sublabel: 'Faculty Administration',
    Icon: SchoolOutlinedIcon,
    accent: '#DC2626',
    matchFn: (v) => !!(
      v.code === 'KGH' || v.code === 'QUAD' ||
      v.departmentName?.toLowerCase().includes('dean') ||
      v.parentName?.toLowerCase().includes('dean')
    ),
  },
];

// ─── Small helper row ──────────────────────────────────────────────────────────

function DetailRow({ icon, children }: { icon: ReactNode; children: ReactNode }) {
  return (
    <Stack direction="row" spacing={0.75} alignItems="center"
           sx={{ color: 'text.secondary', '& svg': { fontSize: 16 } }}>
      {icon}
      <Typography variant="caption" color="text.secondary" noWrap>{children}</Typography>
    </Stack>
  );
}

// ─── Hub card ─────────────────────────────────────────────────────────────────

function HubCard({
  group, count, loading, onClick,
}: {
  group: HubGroup; count: number; loading: boolean; onClick: () => void;
}) {
  const { Icon, accent, label, sublabel } = group;
  return (
    <Card
      variant="outlined"
      sx={{
        height: '100%',
        borderRadius: 3,
        transition: 'box-shadow 0.18s, transform 0.18s',
        '&:hover': { boxShadow: 4, transform: 'translateY(-2px)' },
        borderColor: alpha(accent, 0.25),
      }}
    >
      <CardActionArea onClick={onClick} sx={{ height: '100%', p: 2.5 }}>
        <Stack spacing={1.5}>
          <Box
            sx={{
              width: 48, height: 48, borderRadius: 2,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              backgroundColor: alpha(accent, 0.1),
              color: accent,
            }}
          >
            <Icon sx={{ fontSize: 26 }} />
          </Box>
          <Box>
            <Typography variant="subtitle1" fontWeight={700} lineHeight={1.2}>{label}</Typography>
            <Typography variant="caption" color="text.secondary">{sublabel}</Typography>
          </Box>
          {loading ? (
            <Skeleton width={60} height={20} />
          ) : (
            <Chip
              label={`${count} venue${count !== 1 ? 's' : ''}`}
              size="small"
              sx={{ alignSelf: 'flex-start', backgroundColor: alpha(accent, 0.1), color: accent, fontWeight: 600 }}
            />
          )}
        </Stack>
      </CardActionArea>
    </Card>
  );
}

// ─── Main page ────────────────────────────────────────────────────────────────

export default function VenuesPage() {
  const { hasPermission } = useAuth();
  const [activeGroup, setActiveGroup] = useState<HubGroup | null>(null);
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [bookTarget, setBookTarget] = useState<Location | null>(null);

  const canBook = hasPermission('RESERVATION_CREATE');

  const query = useQuery({
    queryKey: ['locations', 'venues'],
    queryFn: async () =>
      (await api.get<ApiEnvelope<Location[]>>('/locations', {
        params: { venuesOnly: true },
      })).data.data,
    staleTime: 5 * 60_000,
  });

  const venues = query.data ?? [];

  // Count per hub group
  const groupCounts = useMemo(
    () => HUB_GROUPS.map((g) => ({ key: g.key, count: venues.filter(g.matchFn).length })),
    [venues],
  );

  // Venues shown in drill-down view
  const drillVenues = useMemo(() => {
    if (!activeGroup) return [];
    return venues
      .filter(activeGroup.matchFn)
      .filter((v) => {
        const term = search.trim().toLowerCase();
        if (term && !`${v.name} ${v.code}`.toLowerCase().includes(term)) return false;
        if (typeFilter && v.type !== typeFilter) return false;
        return true;
      });
  }, [venues, activeGroup, search, typeFilter]);

  const uniqueTypes = useMemo(
    () => activeGroup ? [...new Set(venues.filter(activeGroup.matchFn).map((v) => v.type))] : [],
    [venues, activeGroup],
  );

  // ── Hub view ────────────────────────────────────────────────────────────────
  if (!activeGroup) {
    return (
      <Box>
        <PageHeader
          eyebrow="OPERATIONS"
          title="Venues"
          crumbs={[{ label: 'Venues' }]}
          subtitle="Select a department or building to browse and book available spaces."
        />

        {query.isError && <Alert severity="error" sx={{ mb: 2 }}>{errorMessage(query.error)}</Alert>}

        <Grid container spacing={2}>
          {HUB_GROUPS.map((group) => {
            const gc = groupCounts.find((c) => c.key === group.key);
            return (
              <Grid key={group.key} size={{ xs: 12, sm: 6, md: 4 }}>
                <HubCard
                  group={group}
                  count={gc?.count ?? 0}
                  loading={query.isLoading}
                  onClick={() => { setSearch(''); setTypeFilter(''); setActiveGroup(group); }}
                />
              </Grid>
            );
          })}
        </Grid>
      </Box>
    );
  }

  // ── Drill-down view ─────────────────────────────────────────────────────────
  const { Icon, accent, label, sublabel } = activeGroup;

  return (
    <Box>
      <PageHeader
        eyebrow="VENUES"
        title={label}
        crumbs={[
          { label: 'Venues', onClick: () => setActiveGroup(null) },
          { label },
        ]}
        subtitle={sublabel}
        actions={
          <Tooltip title="Back to departments">
            <IconButton onClick={() => setActiveGroup(null)} size="small" sx={{ border: '1px solid', borderColor: 'divider' }}>
              <ArrowBackIosNewRoundedIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        }
      />

      {/* Filters */}
      <Card variant="outlined" sx={{ p: 1.5, mb: 2 }}>
        <Stack direction="row" spacing={1.5} alignItems="center" flexWrap="wrap" useFlexGap>
          <TextField
            size="small" placeholder="Search name or code…" value={search}
            sx={{ minWidth: 220 }}
            onChange={(e) => setSearch(e.target.value)}
            InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> }}
          />
          <TextField select size="small" label="Type" value={typeFilter} sx={{ minWidth: 160 }}
                     onChange={(e) => setTypeFilter(e.target.value)}>
            <MenuItem value="">All types</MenuItem>
            {uniqueTypes.map((t) => {
              const meta = VENUE_TYPES[t];
              return <MenuItem key={t} value={t}>{meta?.label ?? t}</MenuItem>;
            })}
          </TextField>
          {(search || typeFilter) && (
            <Button size="small" onClick={() => { setSearch(''); setTypeFilter(''); }}>Clear</Button>
          )}
        </Stack>
      </Card>

      {query.isLoading ? (
        <Grid container spacing={2}>
          {[...Array(4)].map((_, i) => <Grid key={i} size={{ xs: 12, sm: 6, md: 4 }}><Skeleton variant="rounded" height={180} /></Grid>)}
        </Grid>
      ) : drillVenues.length === 0 ? (
        <Card variant="outlined">
          <EmptyState
            title="No venues found"
            hint="Try a different search or filter."
            icon={<MeetingRoomOutlinedIcon />}
          />
        </Card>
      ) : (
        <Grid container spacing={2}>
          {drillVenues.map((venue) => (
            <Grid key={venue.id} size={{ xs: 12, sm: 6, md: 4 }}>
              <Card
                variant="outlined"
                sx={{
                  height: '100%', display: 'flex', flexDirection: 'column',
                  borderRadius: 3,
                  borderColor: alpha(accent, 0.2),
                  transition: 'box-shadow 0.18s',
                  '&:hover': { boxShadow: 3 },
                }}
              >
                <CardContent sx={{ flexGrow: 1, pb: 1 }}>
                  <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={1} mb={0.5}>
                    <Typography variant="subtitle1" fontWeight={700} lineHeight={1.3}>{venue.name}</Typography>
                    <VenueTypeChip type={venue.type} />
                  </Stack>
                  <Box mb={1.25}>
                    <CodeTag>{venue.code}</CodeTag>
                  </Box>
                  <Stack spacing={0.6}>
                    {venue.parentName && (
                      <DetailRow icon={<ApartmentOutlinedIcon />}>{venue.parentName}</DetailRow>
                    )}
                    {/* Capacity shown only when set — small and subtle */}
                    {venue.capacity && (
                      <DetailRow icon={<EventSeatOutlinedIcon />}>
                        {formatNumber(venue.capacity)} seats
                      </DetailRow>
                    )}
                    {venue.bookingFee != null && venue.bookingFee > 0 && (
                      <Typography variant="caption" sx={{ color: accent, fontWeight: 600, mt: 0.25 }}>
                        LKR {formatNumber(venue.bookingFee)} / booking
                      </Typography>
                    )}
                    {venue.bookingFee === 0 && (
                      <Typography variant="caption" sx={{ color: '#16A34A', fontWeight: 600 }}>
                        Free venue
                      </Typography>
                    )}
                  </Stack>
                </CardContent>
                {canBook && (
                  <CardActions sx={{ px: 2, pb: 2, pt: 0 }}>
                    <Button
                      size="small" variant="outlined"
                      startIcon={<EventAvailableOutlinedIcon />}
                      onClick={() => setBookTarget(venue)}
                      sx={{ borderColor: alpha(accent, 0.5), color: accent }}
                    >
                      Book
                    </Button>
                  </CardActions>
                )}
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      <BookingDialog
        open={!!bookTarget}
        onClose={() => setBookTarget(null)}
        initialType="venue"
        initialItemId={bookTarget?.id}
      />
    </Box>
  );
}
