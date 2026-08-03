import { useEffect, useState } from 'react';
import {
  Box, Chip, Stack, Typography, Alert, AlertTitle
} from '@mui/material';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../api/client';
import type { ApiEnvelope, Page } from '../../api/client';
import type { Checkout, DashboardSummary, Payment, Reservation } from '../../api/types';
import { useAuth } from '../../auth/AuthContext';
import PageHeader from '../../components/common/PageHeader';
import { getPrimaryPersona, isAdminUser, isApproverPersona, PERSONA_METADATA } from './personaUtils';
import type { PersonaType } from './personaUtils';
import RoleQuickActions from './components/RoleQuickActions';
import AdminPersonaSwitcher from './components/AdminPersonaSwitcher';
import StatTileGrid from './components/StatTileGrid';

import LabManagerWidgets from './widgets/LabManagerWidgets';
import StorekeeperWidgets from './widgets/StorekeeperWidgets';
import MaintenanceWidgets from './widgets/MaintenanceWidgets';
import UserStudentWidgets from './widgets/UserStudentWidgets';
import AdminExecutiveWidgets from './widgets/AdminExecutiveWidgets';
import ApproverWidgets from './widgets/ApproverWidgets';

export default function DashboardPage() {
  const { user, hasPermission } = useAuth();
  const primaryPersona = getPrimaryPersona(user);
  const isAdmin = isAdminUser(user);

  const [activePersona, setActivePersona] = useState<PersonaType>(primaryPersona);

  useEffect(() => {
    setActivePersona(primaryPersona);
  }, [primaryPersona]);

  const isApprover = isApproverPersona(activePersona);

  const summaryQuery = useQuery({
    queryKey: ['dashboard', 'summary'],
    queryFn: async () => (await api.get<ApiEnvelope<DashboardSummary>>('/dashboard/summary')).data.data,
  });

  // Own active loans. The checkouts API returns a page of ALL checkouts to any
  // CHECKOUT_VIEW holder, so filter by the viewer's userId explicitly.
  const myCheckoutsQuery = useQuery({
    queryKey: ['dashboard', 'my-checkouts-count', user?.id],
    enabled: activePersona === 'STUDENT_USER' && !!user && hasPermission('CHECKOUT_VIEW'),
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<Checkout>>>('/checkouts', {
        params: { status: 'CHECKED_OUT', userId: user!.id, size: 1 },
      })).data.data.totalElements,
  });

  const myReservationsQuery = useQuery({
    queryKey: ['dashboard', 'my-reservations-count'],
    enabled: activePersona === 'STUDENT_USER' || isApprover,
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<Reservation>>>('/reservations', {
        params: { mineOnly: true, size: 1 },
      })).data.data.totalElements,
  });

  // Viewer-scoped approval inbox count: the backend limits PENDING_APPROVAL
  // rows to the approver's own unit, unlike the global summary figure.
  const pendingApprovalsQuery = useQuery({
    queryKey: ['dashboard', 'pending-approvals-count'],
    enabled: (isApprover || activePersona === 'LAB_MANAGER') && hasPermission('RESERVATION_APPROVE'),
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<Reservation>>>('/reservations', {
        params: { status: 'PENDING_APPROVAL', size: 1 },
      })).data.data.totalElements,
  });

  const activeCheckoutsQuery = useQuery({
    queryKey: ['dashboard', 'active-checkouts-count'],
    enabled: isApprover && hasPermission('CHECKOUT_VIEW'),
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<Checkout>>>('/checkouts', {
        params: { status: 'CHECKED_OUT', size: 1 },
      })).data.data.totalElements,
  });

  // Own unpaid fees. GET /payments returns only the viewer's payments for
  // users without PAYMENT_VIEW.
  const paymentsDueQuery = useQuery({
    queryKey: ['dashboard', 'payments-due-count'],
    enabled: activePersona === 'STUDENT_USER',
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<Payment>>>('/payments', {
        params: { status: 'PENDING', size: 1 },
      })).data.data.totalElements,
  });

  const summary = summaryQuery.data;
  const currentMeta = PERSONA_METADATA[activePersona];

  const showAlerts = activePersona !== 'STUDENT_USER' && summary && (
    ((activePersona === 'ADMIN' || activePersona === 'LAB_MANAGER') && summary.overdueReturns > 0) ||
    ((activePersona === 'ADMIN' || activePersona === 'STOREKEEPER') && summary.lowStockConsumables > 0)
  );

  return (
    <Box>
      <PageHeader
        eyebrow={currentMeta.eyebrow}
        title={`Welcome back, ${user?.firstName ?? 'User'}`}
        subtitle={currentMeta.subtitle}
        actions={
          <Chip
            label={currentMeta.title}
            color={currentMeta.badgeColor}
            variant="outlined"
            size="small"
            sx={{ fontWeight: 600, display: { xs: 'none', sm: 'inline-flex' } }}
          />
        }
      />

      {/* Admin Persona Switcher - Only visible to Super/Faculty/Asset Administrators */}
      {isAdmin && (
        <AdminPersonaSwitcher
          currentPersona={activePersona}
          onSelectPersona={(persona) => setActivePersona(persona)}
        />
      )}

      {/* Dynamic Urgent Priority Alert Banner - Strictly hidden for Students */}
      {showAlerts && summary && (
        <Alert
          severity={summary.overdueReturns > 0 ? 'error' : 'warning'}
          icon={<WarningAmberOutlinedIcon />}
          sx={{ mb: 2.5, borderRadius: 2 }}
        >
          <AlertTitle sx={{ fontWeight: 600 }}>Priority Operational Alerts</AlertTitle>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} flexWrap="wrap">
            {(activePersona === 'ADMIN' || activePersona === 'LAB_MANAGER') && summary.overdueReturns > 0 && (
              <Typography variant="body2">
                ⚠️ <strong>{summary.overdueReturns}</strong> asset(s) are overdue for return.
              </Typography>
            )}
            {(activePersona === 'ADMIN' || activePersona === 'STOREKEEPER') && summary.lowStockConsumables > 0 && (
              <Typography variant="body2">
                📦 <strong>{summary.lowStockConsumables}</strong> consumable item(s) are below reorder thresholds.
              </Typography>
            )}
          </Stack>
        </Alert>
      )}

      {/* Quick Action Shortcuts Bar */}
      <RoleQuickActions persona={activePersona} />

      {/* Key Performance Indicators (Stat Tile Grid) */}
      <StatTileGrid
        summary={summary}
        isLoading={summaryQuery.isLoading}
        persona={activePersona}
        myCheckoutsCount={myCheckoutsQuery.data ?? 0}
        myReservationsCount={myReservationsQuery.data ?? 0}
        pendingApprovalsCount={pendingApprovalsQuery.data ?? 0}
        activeCheckoutsCount={activeCheckoutsQuery.data ?? 0}
        paymentsDueCount={paymentsDueQuery.data ?? 0}
      />

      {/* Role-Specific Main Widgets */}
      {isApproverPersona(activePersona) && <ApproverWidgets persona={activePersona} />}
      {activePersona === 'LAB_MANAGER' && <LabManagerWidgets />}
      {activePersona === 'STOREKEEPER' && <StorekeeperWidgets />}
      {activePersona === 'MAINTENANCE_OFFICER' && <MaintenanceWidgets />}
      {activePersona === 'STUDENT_USER' && <UserStudentWidgets />}
      {(activePersona === 'ADMIN' || activePersona === 'FINANCE_OFFICER' || activePersona === 'AUDITOR') && (
        <AdminExecutiveWidgets />
      )}
    </Box>
  );
}
