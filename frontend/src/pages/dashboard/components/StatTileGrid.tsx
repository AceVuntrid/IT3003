import { useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, CardContent, Grid, Skeleton, Typography } from '@mui/material';
import type { DashboardSummary } from '../../../api/types';
import { useAuth } from '../../../auth/AuthContext';
import { formatMoney, formatNumber } from '../../../utils/format';
import { monoFont } from '../../../theme';
import type { PersonaType } from '../personaUtils';

export interface Tile {
  label: string;
  value: string;
  to?: string;
  tone?: 'default' | 'warning' | 'danger' | 'success';
}

function StatTile({ tile }: { tile: Tile }) {
  const navigate = useNavigate();
  const border =
    tile.tone === 'danger'
      ? '#C4453C'
      : tile.tone === 'warning'
      ? '#C9821A'
      : tile.tone === 'success'
      ? '#0E8C6A'
      : 'transparent';

  return (
    <Card
      variant="outlined"
      onClick={tile.to ? () => navigate(tile.to!) : undefined}
      sx={{
        cursor: tile.to ? 'pointer' : 'default',
        borderTop: `3px solid ${border}`,
        transition: 'all 150ms ease-in-out',
        '&:hover': tile.to
          ? {
              borderColor: 'primary.main',
              boxShadow: '0 4px 12px rgba(0,0,0,0.06)',
              transform: 'translateY(-1px)',
            }
          : undefined,
        height: '100%',
      }}
    >
      <CardContent sx={{ py: 1.75, '&:last-child': { pb: 1.75 } }}>
        <Typography
          sx={{
            fontFamily: monoFont,
            fontSize: '1.45rem',
            fontWeight: 500,
            lineHeight: 1.3,
            color: tile.tone === 'danger' ? '#C4453C' : tile.tone === 'warning' ? '#B45309' : 'text.primary',
          }}
        >
          {tile.value}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {tile.label}
        </Typography>
      </CardContent>
    </Card>
  );
}

interface Props {
  summary: DashboardSummary | undefined;
  isLoading: boolean;
  persona: PersonaType;
  myCheckoutsCount?: number;
  myReservationsCount?: number;
  /** Viewer-scoped PENDING_APPROVAL count from GET /reservations (approver personas). */
  pendingApprovalsCount?: number;
  /** Institution-wide active checkout count (approver personas, CHECKOUT_VIEW only). */
  activeCheckoutsCount?: number;
  /** Own PENDING payment count from GET /payments (student/lecturer persona). */
  paymentsDueCount?: number;
}

export default function StatTileGrid({
  summary: s, isLoading, persona, myCheckoutsCount = 0, myReservationsCount = 0,
  pendingApprovalsCount = 0, activeCheckoutsCount = 0, paymentsDueCount = 0,
}: Props) {
  const { hasPermission } = useAuth();
  const canViewCheckouts = hasPermission('CHECKOUT_VIEW');
  const canViewPayments = hasPermission('PAYMENT_VIEW');

  const tiles = useMemo<Tile[]>(() => {
    if (!s) return [];

    switch (persona) {
      case 'DEPT_ADMIN_USER':
      case 'FACULTY_DEAN_USER':
      case 'CARETAKER_USER':
        return [
          { label: 'Awaiting your approval', value: formatNumber(pendingApprovalsCount), to: '/reservations?status=PENDING_APPROVAL', tone: pendingApprovalsCount > 0 ? 'warning' : 'default' },
          ...(canViewCheckouts
            ? [{ label: 'Active checkouts', value: formatNumber(activeCheckoutsCount), to: '/checkouts' } satisfies Tile]
            : []),
          { label: 'My upcoming reservations', value: formatNumber(myReservationsCount), to: '/reservations' },
        ];
      case 'LAB_MANAGER':
        return [
          // Scoped count from the reservations API (the backend limits the
          // PENDING_APPROVAL list to the manager's unit), not the global summary.
          { label: 'Pending approvals', value: formatNumber(pendingApprovalsCount), to: '/reservations?status=PENDING_APPROVAL', tone: pendingApprovalsCount > 0 ? 'warning' : 'default' },
          { label: 'Overdue returns', value: formatNumber(s.overdueReturns), to: '/checkouts', tone: s.overdueReturns > 0 ? 'danger' : 'default' },
          { label: 'Active checkouts', value: formatNumber(s.checkedOutAssets), to: '/checkouts' },
          { label: 'Currently reserved', value: formatNumber(s.reservedAssets), to: '/reservations' },
          { label: 'Equipment available', value: formatNumber(s.availableAssets), to: '/assets' },
          { label: 'Under maintenance', value: formatNumber(s.underMaintenance), to: '/maintenance', tone: s.underMaintenance > 0 ? 'warning' : 'default' },
        ];
      case 'STOREKEEPER':
        // Note: the old "Consumable items" tile faked its value (totalAssets / 3)
        // and DashboardSummary has no real consumable-count field, so it was
        // dropped. "Damaged / Written-off" (a fixed-asset metric) was also
        // removed as irrelevant to the stores desk.
        return [
          { label: 'Low-stock items', value: formatNumber(s.lowStockConsumables), to: '/consumables', tone: s.lowStockConsumables > 0 ? 'warning' : 'default' },
          { label: 'Batches expiring (<60d)', value: formatNumber(s.expiringConsumables), to: '/consumables', tone: s.expiringConsumables > 0 ? 'warning' : 'default' },
        ];
      case 'MAINTENANCE_OFFICER':
        return [
          { label: 'Open work orders', value: formatNumber(s.maintenanceJobsOpen), to: '/maintenance', tone: s.maintenanceJobsOpen > 0 ? 'warning' : 'default' },
          { label: 'Under maintenance', value: formatNumber(s.underMaintenance), to: '/maintenance', tone: s.underMaintenance > 0 ? 'warning' : 'default' },
          { label: 'Service due (30d)', value: formatNumber(s.maintenanceDueSoon), to: '/maintenance', tone: s.maintenanceDueSoon > 0 ? 'warning' : 'default' },
          { label: 'Damaged assets', value: formatNumber(s.damagedAssets), to: '/assets', tone: s.damagedAssets > 0 ? 'danger' : 'default' },
          { label: 'Lost / Missing', value: formatNumber(s.lostAssets), to: '/assets', tone: s.lostAssets > 0 ? 'danger' : 'default' },
        ];
      case 'FINANCE_OFFICER':
      case 'AUDITOR':
        return [
          { label: 'Total asset valuation', value: formatMoney(s.totalAssetValue) },
          { label: 'Total registered assets', value: formatNumber(s.totalAssets), to: '/assets' },
          // Relabeled: the value is a count of checked-out assets, not a valuation.
          { label: 'Assets checked out', value: formatNumber(s.checkedOutAssets), to: '/checkouts' },
          { label: 'Damaged assets', value: formatNumber(s.damagedAssets), to: '/assets', tone: s.damagedAssets > 0 ? 'danger' : 'default' },
          { label: 'Low-stock warnings', value: formatNumber(s.lowStockConsumables), to: '/consumables', tone: s.lowStockConsumables > 0 ? 'warning' : 'default' },
        ];
      case 'STUDENT_USER':
        // Personal tiles only — the global "Available equipment" stat was
        // removed as an institution-wide number irrelevant to a personal portal.
        return [
          ...(canViewCheckouts
            ? [{ label: 'My active loans', value: formatNumber(myCheckoutsCount), to: '/checkouts' } satisfies Tile]
            : []),
          { label: 'My upcoming reservations', value: formatNumber(myReservationsCount), to: '/reservations' },
          {
            label: 'Payments due',
            value: formatNumber(paymentsDueCount),
            // The /payments route is still gated by PAYMENT_VIEW; only link when navigable.
            to: canViewPayments ? '/payments' : undefined,
            tone: paymentsDueCount > 0 ? 'warning' : 'default',
          },
        ];
      case 'ADMIN':
      default:
        return [
          { label: 'Fixed assets', value: formatNumber(s.totalAssets), to: '/assets' },
          { label: 'Total asset value', value: formatMoney(s.totalAssetValue) },
          { label: 'Available now', value: formatNumber(s.availableAssets), to: '/assets' },
          { label: 'Checked out', value: formatNumber(s.checkedOutAssets), to: '/checkouts' },
          { label: 'Pending approvals', value: formatNumber(s.pendingApprovals), to: '/reservations', tone: s.pendingApprovals > 0 ? 'warning' : 'default' },
          { label: 'Overdue returns', value: formatNumber(s.overdueReturns), to: '/checkouts', tone: s.overdueReturns > 0 ? 'danger' : 'default' },
          { label: 'Low-stock items', value: formatNumber(s.lowStockConsumables), to: '/consumables', tone: s.lowStockConsumables > 0 ? 'warning' : 'default' },
          { label: 'Maintenance due (30d)', value: formatNumber(s.maintenanceDueSoon), to: '/maintenance', tone: s.maintenanceDueSoon > 0 ? 'warning' : 'default' },
        ];
    }
  }, [s, persona, myCheckoutsCount, myReservationsCount, pendingApprovalsCount,
      activeCheckoutsCount, paymentsDueCount, canViewCheckouts, canViewPayments]);

  if (isLoading) {
    return (
      <Grid container spacing={2} sx={{ mb: 2.5 }}>
        {Array.from({ length: 3 }).map((_, i) => (
          <Grid key={i} size={{ xs: 6, sm: 4, md: 3 }}>
            <Skeleton variant="rounded" height={84} />
          </Grid>
        ))}
      </Grid>
    );
  }

  return (
    <Grid container spacing={2} sx={{ mb: 2.5 }}>
      {tiles.map((tile) => (
        <Grid key={tile.label} size={{ xs: 6, sm: 4, md: 3 }}>
          <StatTile tile={tile} />
        </Grid>
      ))}
    </Grid>
  );
}
