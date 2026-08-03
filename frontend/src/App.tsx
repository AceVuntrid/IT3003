import { Suspense, lazy } from 'react';
import type { ReactNode } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { Box, CircularProgress } from '@mui/material';
import { useAuth } from './auth/AuthContext';
import AppLayout from './components/layout/AppLayout';

const LoginPage = lazy(() => import('./pages/auth/LoginPage'));
const RegisterPage = lazy(() => import('./pages/auth/RegisterPage'));
const ForgotPasswordPage = lazy(() => import('./pages/auth/ForgotPasswordPage'));
const ResetPasswordPage = lazy(() => import('./pages/auth/ResetPasswordPage'));
const DashboardPage = lazy(() => import('./pages/dashboard/DashboardPage'));
const AssetsListPage = lazy(() => import('./pages/assets/AssetsListPage'));
const AssetDetailPage = lazy(() => import('./pages/assets/AssetDetailPage'));
const AssetFormPage = lazy(() => import('./pages/assets/AssetFormPage'));
const ConsumablesPage = lazy(() => import('./pages/consumables/ConsumablesPage'));
const ConsumableDetailPage = lazy(() => import('./pages/consumables/ConsumableDetailPage'));
const ReservationsPage = lazy(() => import('./pages/reservations/ReservationsPage'));
const CheckoutsPage = lazy(() => import('./pages/checkouts/CheckoutsPage'));
const MaintenancePage = lazy(() => import('./pages/maintenance/MaintenancePage'));
const TransfersPage = lazy(() => import('./pages/transfers/TransfersPage'));
const LocationsPage = lazy(() => import('./pages/locations/LocationsPage'));
const PaymentsPage = lazy(() => import('./pages/payments/PaymentsPage'));
const ReportsPage = lazy(() => import('./pages/reports/ReportsPage'));
const UsersPage = lazy(() => import('./pages/users/UsersPage'));
const VenuesPage = lazy(() => import('./pages/venues/VenuesPage'));
const SettingsPage = lazy(() => import('./pages/settings/SettingsPage'));
const CategoriesPage = lazy(() => import('./pages/categories/CategoriesPage'));
const AuditPage = lazy(() => import('./pages/audit/AuditPage'));
const NotificationsPage = lazy(() => import('./pages/notifications/NotificationsPage'));
const ProfilePage = lazy(() => import('./pages/profile/ProfilePage'));
const AccessDeniedPage = lazy(() => import('./pages/errors/AccessDeniedPage'));
const NotFoundPage = lazy(() => import('./pages/errors/NotFoundPage'));

function CenteredSpinner() {
  return (
    <Box sx={{ display: 'grid', placeItems: 'center', minHeight: '50vh' }}>
      <CircularProgress />
    </Box>
  );
}

function RequireAuth({ children }: { children: ReactNode }) {
  const { user, initializing } = useAuth();
  const location = useLocation();
  if (initializing) return <CenteredSpinner />;
  if (!user) return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  return <>{children}</>;
}

function RequirePermission({ anyOf, children }: { anyOf: string[]; children: ReactNode }) {
  const { hasPermission } = useAuth();
  if (!hasPermission(...anyOf)) return <Navigate to="/access-denied" replace />;
  return <>{children}</>;
}

function RequireRole({ anyOf, children }: { anyOf: string[]; children: ReactNode }) {
  const { hasRole } = useAuth();
  if (!hasRole(...anyOf)) return <Navigate to="/access-denied" replace />;
  return <>{children}</>;
}

export default function App() {
  return (
    <Suspense fallback={<CenteredSpinner />}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />

        <Route element={<RequireAuth><AppLayout /></RequireAuth>}>
          <Route index element={<DashboardPage />} />
          <Route path="assets" element={<RequirePermission anyOf={['ASSET_VIEW']}><AssetsListPage /></RequirePermission>} />
          <Route path="assets/new" element={<RequirePermission anyOf={['ASSET_CREATE']}><AssetFormPage /></RequirePermission>} />
          <Route path="assets/:id" element={<RequirePermission anyOf={['ASSET_VIEW']}><AssetDetailPage /></RequirePermission>} />
          <Route path="assets/:id/edit" element={<RequirePermission anyOf={['ASSET_EDIT']}><AssetFormPage /></RequirePermission>} />
          <Route path="consumables" element={<RequirePermission anyOf={['CONSUMABLE_VIEW']}><ConsumablesPage /></RequirePermission>} />
          <Route path="consumables/:id" element={<RequirePermission anyOf={['CONSUMABLE_VIEW']}><ConsumableDetailPage /></RequirePermission>} />
          <Route path="reservations" element={<RequirePermission anyOf={['RESERVATION_VIEW']}><ReservationsPage /></RequirePermission>} />
          <Route path="checkouts" element={<RequirePermission anyOf={['CHECKOUT_VIEW']}><CheckoutsPage /></RequirePermission>} />
          <Route path="maintenance" element={<RequirePermission anyOf={['MAINTENANCE_VIEW', 'MAINTENANCE_CREATE']}><MaintenancePage /></RequirePermission>} />
          <Route path="transfers" element={<RequirePermission anyOf={['TRANSFER_VIEW']}><TransfersPage /></RequirePermission>} />
          <Route path="locations" element={<RequirePermission anyOf={['LOCATION_VIEW']}><LocationsPage /></RequirePermission>} />
          {/* No permission gate: the backend scopes non-PAYMENT_VIEW users to their own payments. */}
          <Route path="payments" element={<PaymentsPage />} />
          <Route path="reports" element={<RequirePermission anyOf={['REPORT_VIEW']}><ReportsPage /></RequirePermission>} />
          <Route path="venues" element={<VenuesPage />} />
          <Route path="settings" element={<RequirePermission anyOf={['SETTINGS_MANAGE']}><SettingsPage /></RequirePermission>} />
          <Route path="categories" element={<RequireRole anyOf={['SUPER_ADMIN']}><CategoriesPage /></RequireRole>} />
          <Route path="users" element={<RequirePermission anyOf={['USER_VIEW']}><UsersPage /></RequirePermission>} />
          <Route path="audit" element={<RequirePermission anyOf={['AUDIT_VIEW']}><AuditPage /></RequirePermission>} />
          <Route path="notifications" element={<NotificationsPage />} />
          <Route path="profile" element={<ProfilePage />} />
          <Route path="access-denied" element={<AccessDeniedPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </Suspense>
  );
}
