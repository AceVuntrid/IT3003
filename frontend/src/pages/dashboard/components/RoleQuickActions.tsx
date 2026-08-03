import React, { useEffect, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { Box, Button, Card, CardContent, Stack, Typography, Grid } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EventAvailableOutlinedIcon from '@mui/icons-material/EventAvailableOutlined';
import CheckCircleOutlinedIcon from '@mui/icons-material/CheckCircleOutlined';
import Inventory2OutlinedIcon from '@mui/icons-material/Inventory2Outlined';
import BuildOutlinedIcon from '@mui/icons-material/BuildOutlined';
import ReceiptLongOutlinedIcon from '@mui/icons-material/ReceiptLongOutlined';
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined';
import PeopleOutlinedIcon from '@mui/icons-material/PeopleOutlined';
import HistoryOutlinedIcon from '@mui/icons-material/HistoryOutlined';
import OutputOutlinedIcon from '@mui/icons-material/OutputOutlined';
import MoveToInboxOutlinedIcon from '@mui/icons-material/MoveToInboxOutlined';
import SettingsOutlinedIcon from '@mui/icons-material/SettingsOutlined';
import type { PersonaType } from '../personaUtils';
import { useAuth } from '../../../auth/AuthContext';
import BookingDialog from '../../../components/common/BookingDialog';

interface QuickActionItem {
  label: string;
  to?: string;
  onClick?: () => void;
  icon: React.ReactNode;
  variant?: 'contained' | 'outlined';
  color?: 'primary' | 'secondary' | 'success' | 'warning' | 'info';
  permission?: string;
}

interface Props {
  persona: PersonaType;
}

export default function RoleQuickActions({ persona }: Props) {
  const { hasPermission } = useAuth();
  const [quickRequestOpen, setQuickRequestOpen] = useState(false);
  const canRequest = hasPermission('RESERVATION_CREATE');

  // Global Shift+R shortcut: open the unified booking dialog (equipment /
  // venue / consumable) for anyone who can create reservations, unless they
  // are typing in a field.
  useEffect(() => {
    if (!canRequest) return undefined;
    const onKeyDown = (event: KeyboardEvent) => {
      if (!event.shiftKey || event.ctrlKey || event.metaKey || event.altKey) return;
      if (event.key.toLowerCase() !== 'r') return;
      const target = event.target as HTMLElement | null;
      if (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA'
        || target.tagName === 'SELECT' || target.isContentEditable)) return;
      event.preventDefault();
      setQuickRequestOpen(true);
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [canRequest]);

  const getActionsForPersona = (): QuickActionItem[] => {
    switch (persona) {
      case 'DEPT_ADMIN_USER':
      case 'FACULTY_DEAN_USER':
      case 'CARETAKER_USER':
        return [
          { label: 'Review Approvals', to: '/reservations?status=PENDING_APPROVAL', icon: <CheckCircleOutlinedIcon />, variant: 'contained', color: 'primary', permission: 'RESERVATION_APPROVE' },
          { label: 'Book an Asset / Venue', onClick: () => setQuickRequestOpen(true), icon: <EventAvailableOutlinedIcon />, variant: 'outlined', permission: 'RESERVATION_CREATE' },
          { label: 'Settings', to: '/settings', icon: <SettingsOutlinedIcon />, variant: 'outlined', permission: 'SETTINGS_MANAGE' },
        ];
      case 'LAB_MANAGER':
        return [
          { label: 'Approve Reservations', to: '/reservations?status=PENDING_APPROVAL', icon: <CheckCircleOutlinedIcon />, variant: 'contained', color: 'primary', permission: 'RESERVATION_APPROVE' },
          { label: 'Check Out Asset', to: '/checkouts', icon: <OutputOutlinedIcon />, variant: 'outlined', permission: 'CHECKOUT_CREATE' },
          { label: 'Process Return', to: '/checkouts', icon: <MoveToInboxOutlinedIcon />, variant: 'outlined', permission: 'CHECKOUT_MANAGE' },
          { label: 'Report Equipment Fault', to: '/maintenance', icon: <BuildOutlinedIcon />, variant: 'outlined', permission: 'MAINTENANCE_CREATE' },
        ];
      case 'STOREKEEPER':
        return [
          { label: 'Issue Consumable', to: '/consumables', icon: <OutputOutlinedIcon />, variant: 'contained', color: 'primary', permission: 'CONSUMABLE_ISSUE' },
          { label: 'Receive Inventory Batch', to: '/consumables', icon: <MoveToInboxOutlinedIcon />, variant: 'outlined', permission: 'CONSUMABLE_RECEIVE' },
          { label: 'Add Consumable SKU', to: '/consumables', icon: <AddIcon />, variant: 'outlined', permission: 'CONSUMABLE_CREATE' },
        ];
      case 'MAINTENANCE_OFFICER':
        return [
          { label: 'Report New Fault', to: '/maintenance', icon: <AddIcon />, variant: 'contained', color: 'primary', permission: 'MAINTENANCE_CREATE' },
          { label: 'My Assigned Work Orders', to: '/maintenance', icon: <BuildOutlinedIcon />, variant: 'outlined', permission: 'MAINTENANCE_VIEW' },
          { label: 'Search Asset Register', to: '/assets', icon: <SearchOutlinedIcon />, variant: 'outlined', permission: 'ASSET_VIEW' },
        ];
      case 'FINANCE_OFFICER':
      case 'AUDITOR':
        return [
          { label: 'View Financial Records', to: '/payments', icon: <ReceiptLongOutlinedIcon />, variant: 'contained', color: 'primary', permission: 'PAYMENT_VIEW' },
          { label: 'System Audit Log', to: '/audit', icon: <HistoryOutlinedIcon />, variant: 'outlined', permission: 'AUDIT_VIEW' },
        ];
      case 'STUDENT_USER':
        return [
          { label: 'Request an Asset', onClick: () => setQuickRequestOpen(true), icon: <EventAvailableOutlinedIcon />, variant: 'contained', color: 'primary', permission: 'RESERVATION_CREATE' },
          { label: 'My Reservations', to: '/reservations', icon: <EventAvailableOutlinedIcon />, variant: 'outlined', permission: 'RESERVATION_CREATE' },
          { label: 'Browse Asset Catalog', to: '/assets', icon: <SearchOutlinedIcon />, variant: 'outlined', permission: 'ASSET_VIEW' },
          { label: 'My Checked-out Items', to: '/checkouts', icon: <OutputOutlinedIcon />, variant: 'outlined', permission: 'CHECKOUT_VIEW' },
          { label: 'Report Damaged Item', to: '/maintenance', icon: <BuildOutlinedIcon />, variant: 'outlined', permission: 'MAINTENANCE_CREATE' },
        ];
      case 'ADMIN':
      default:
        return [
          { label: 'Add Fixed Asset', to: '/assets/new', icon: <AddIcon />, variant: 'contained', color: 'primary', permission: 'ASSET_CREATE' },
          { label: 'Create Reservation', to: '/reservations', icon: <EventAvailableOutlinedIcon />, variant: 'outlined', permission: 'RESERVATION_CREATE' },
          { label: 'Consumable Stock', to: '/consumables', icon: <Inventory2OutlinedIcon />, variant: 'outlined', permission: 'CONSUMABLE_VIEW' },
          { label: 'Manage Users', to: '/users', icon: <PeopleOutlinedIcon />, variant: 'outlined', permission: 'USER_VIEW' },
          { label: 'System Audit', to: '/audit', icon: <HistoryOutlinedIcon />, variant: 'outlined', permission: 'AUDIT_VIEW' },
        ];
    }
  };

  const actions = getActionsForPersona().filter(
    (action) => !action.permission || hasPermission(action.permission)
  );

  if (actions.length === 0 && !canRequest) return null;

  const actionButtonSx = { borderRadius: 1.5, textTransform: 'none', px: 1.75, py: 0.75, fontWeight: 500 };

  return (
    <>
      {actions.length > 0 && (
        <Card variant="outlined" sx={{ mb: 2.5, backgroundColor: 'rgba(124, 58, 237, 0.02)', borderColor: 'primary.light' }}>
          <CardContent sx={{ py: 1.75, px: 2, '&:last-child': { pb: 1.75 } }}>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} alignItems={{ sm: 'center' }} justifyContent="space-between">
              <Box>
                <Typography variant="subtitle2" sx={{ fontWeight: 600, color: 'primary.main', display: 'flex', alignItems: 'center', gap: 0.75 }}>
                  ⚡ Frequent & Quick Actions
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  Direct access shortcuts tailored for your role tasks
                  {canRequest ? ' · press Shift+R for a quick booking' : ''}
                </Typography>
              </Box>
              <Grid container spacing={1} sx={{ width: 'auto' }}>
                {actions.map((act) => (
                  <Grid key={act.label}>
                    {act.to ? (
                      <Button
                        component={RouterLink}
                        to={act.to}
                        variant={act.variant || 'outlined'}
                        color={act.color || 'primary'}
                        startIcon={act.icon}
                        size="small"
                        sx={actionButtonSx}
                      >
                        {act.label}
                      </Button>
                    ) : (
                      <Button
                        onClick={act.onClick}
                        variant={act.variant || 'outlined'}
                        color={act.color || 'primary'}
                        startIcon={act.icon}
                        size="small"
                        sx={actionButtonSx}
                      >
                        {act.label}
                      </Button>
                    )}
                  </Grid>
                ))}
              </Grid>
            </Stack>
          </CardContent>
        </Card>
      )}
      {canRequest && (
        <BookingDialog open={quickRequestOpen} onClose={() => setQuickRequestOpen(false)} />
      )}
    </>
  );
}
