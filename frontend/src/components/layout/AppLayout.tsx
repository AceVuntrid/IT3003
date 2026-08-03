import { useMemo, useState } from 'react';
import type { ReactElement } from 'react';
import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import {
  AppBar, Avatar, Badge, Box, Divider, Drawer, IconButton, List, ListItemButton,
  ListItemIcon, ListItemText, ListSubheader, Menu, MenuItem, Toolbar, Tooltip, Typography,
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import NotificationsNoneIcon from '@mui/icons-material/NotificationsNone';
import LogoutIcon from '@mui/icons-material/Logout';
import PersonOutlineIcon from '@mui/icons-material/PersonOutline';
import DashboardOutlinedIcon from '@mui/icons-material/DashboardOutlined';
import ScienceOutlinedIcon from '@mui/icons-material/ScienceOutlined';
import Inventory2OutlinedIcon from '@mui/icons-material/Inventory2Outlined';
import EventAvailableOutlinedIcon from '@mui/icons-material/EventAvailableOutlined';
import AssignmentReturnOutlinedIcon from '@mui/icons-material/AssignmentReturnOutlined';
import PlaceOutlinedIcon from '@mui/icons-material/PlaceOutlined';
import BuildOutlinedIcon from '@mui/icons-material/BuildOutlined';
import SwapHorizOutlinedIcon from '@mui/icons-material/SwapHorizOutlined';
import PaymentsOutlinedIcon from '@mui/icons-material/PaymentsOutlined';
import AssessmentOutlinedIcon from '@mui/icons-material/AssessmentOutlined';
import GroupOutlinedIcon from '@mui/icons-material/GroupOutlined';
import HistoryOutlinedIcon from '@mui/icons-material/HistoryOutlined';
import MeetingRoomOutlinedIcon from '@mui/icons-material/MeetingRoomOutlined';
import SettingsOutlinedIcon from '@mui/icons-material/SettingsOutlined';
import CategoryOutlinedIcon from '@mui/icons-material/CategoryOutlined';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../../auth/AuthContext';
import { api } from '../../api/client';
import type { ApiEnvelope } from '../../api/client';
import { ink, displayFont, monoFont } from '../../theme';

const DRAWER_WIDTH = 248;

interface NavItem {
  label: string;
  to: string;
  icon: ReactElement;
  permissions?: string[];
  roles?: string[];
}

interface NavGroup {
  header?: string;
  items: NavItem[];
}

const NAV_GROUPS: NavGroup[] = [
  {
    items: [{ label: 'Dashboard', to: '/', icon: <DashboardOutlinedIcon /> }],
  },
  {
    header: 'Inventory',
    items: [
      { label: 'Assets', to: '/assets', icon: <ScienceOutlinedIcon />, permissions: ['ASSET_VIEW'] },
      { label: 'Consumables', to: '/consumables', icon: <Inventory2OutlinedIcon />, permissions: ['CONSUMABLE_VIEW'] },
      { label: 'Venues', to: '/venues', icon: <MeetingRoomOutlinedIcon /> },
      { label: 'Locations', to: '/locations', icon: <PlaceOutlinedIcon />, permissions: ['LOCATION_VIEW'] },
    ],
  },
  {
    header: 'Operations',
    items: [
      { label: 'Reservations', to: '/reservations', icon: <EventAvailableOutlinedIcon />, permissions: ['RESERVATION_VIEW'] },
      { label: 'Check-Out & Returns', to: '/checkouts', icon: <AssignmentReturnOutlinedIcon />, permissions: ['CHECKOUT_VIEW'] },
      { label: 'Maintenance', to: '/maintenance', icon: <BuildOutlinedIcon />, permissions: ['MAINTENANCE_VIEW'] },
      { label: 'Transfers', to: '/transfers', icon: <SwapHorizOutlinedIcon />, permissions: ['TRANSFER_VIEW'] },
    ],
  },
  {
    header: 'Commerce',
    items: [
      { label: 'Payments & Charges', to: '/payments', icon: <PaymentsOutlinedIcon /> },
    ],
  },
  {
    header: 'Administration',
    items: [
      { label: 'Reports', to: '/reports', icon: <AssessmentOutlinedIcon />, permissions: ['REPORT_VIEW'] },
      { label: 'Users & Roles', to: '/users', icon: <GroupOutlinedIcon />, permissions: ['USER_VIEW'] },
      { label: 'Audit Log', to: '/audit', icon: <HistoryOutlinedIcon />, permissions: ['AUDIT_VIEW'] },
      { label: 'Categories', to: '/categories', icon: <CategoryOutlinedIcon />, roles: ['SUPER_ADMIN'] },
      { label: 'Settings', to: '/settings', icon: <SettingsOutlinedIcon />, permissions: ['SETTINGS_MANAGE'] },
    ],
  },
];

function SidebarContent({ onNavigate }: { onNavigate?: () => void }) {
  const { hasPermission, hasRole } = useAuth();
  const groups = useMemo(
    () =>
      NAV_GROUPS.map((group) => ({
        ...group,
        items: group.items.filter(
          (item) => (!item.permissions || hasPermission(...item.permissions)) &&
                    (!item.roles || hasRole(...item.roles))
        ),
      })).filter((group) => group.items.length > 0),
    [hasPermission, hasRole],
  );

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column', backgroundColor: ink }}>
      <Toolbar sx={{ px: 2.5, gap: 1.5 }}>
        <Box component="img" src="/uni-logo.png" alt="University Crest" sx={{ width: 32, height: 38, objectFit: 'contain' }} />
        <Box>
          <Typography sx={{ fontFamily: displayFont, fontWeight: 700, color: '#fff', fontSize: '1.02rem', lineHeight: 1.15 }}>
            Uni Assets
          </Typography>
          <Typography sx={{ fontFamily: monoFont, fontSize: '0.62rem', color: 'rgba(255,255,255,0.7)', letterSpacing: '0.14em' }}>
            FACULTY OF SCIENCE
          </Typography>
        </Box>
      </Toolbar>
      <Divider sx={{ borderColor: 'rgba(255,255,255,0.08)' }} />
      <Box sx={{ overflowY: 'auto', flex: 1, py: 1 }}>
        {groups.map((group, index) => (
          <List
            key={group.header ?? index}
            dense
            subheader={
              group.header ? (
                <ListSubheader
                  disableSticky
                  sx={{
                    backgroundColor: 'transparent',
                    color: 'rgba(255,255,255,0.4)',
                    fontFamily: monoFont,
                    fontSize: '0.62rem',
                    letterSpacing: '0.16em',
                    lineHeight: 2.4,
                  }}
                >
                  {group.header.toUpperCase()}
                </ListSubheader>
              ) : undefined
            }
          >
            {group.items.map((item) => (
              <ListItemButton
                key={item.to}
                component={NavLink}
                to={item.to}
                end={item.to === '/'}
                onClick={onNavigate}
                sx={{
                  mx: 1.25,
                  mb: 0.25,
                  borderRadius: 1.5,
                  color: 'rgba(255,255,255,0.72)',
                  '& .MuiListItemIcon-root': { color: 'rgba(255,255,255,0.5)', minWidth: 36 },
                  '&.active': {
                    backgroundColor: 'rgba(109, 40, 217, 0.32)',
                    color: '#fff',
                    boxShadow: 'inset 3px 0 0 #C084FC',
                    '& .MuiListItemIcon-root': { color: '#E9D5FF' },
                  },
                  '&:hover': { backgroundColor: 'rgba(255,255,255,0.06)' },
                }}
              >
                <ListItemIcon>{item.icon}</ListItemIcon>
                <ListItemText
                  primary={item.label}
                  primaryTypographyProps={{ fontSize: '0.86rem', fontWeight: 500 }}
                />
              </ListItemButton>
            ))}
          </List>
        ))}
      </Box>
    </Box>
  );
}

export default function AppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [menuAnchor, setMenuAnchor] = useState<null | HTMLElement>(null);

  const { data: unread } = useQuery({
    queryKey: ['notifications', 'unread'],
    queryFn: async () =>
      (await api.get<ApiEnvelope<{ count: number }>>('/notifications/unread-count')).data.data.count,
    refetchInterval: 60_000,
  });

  const initials = user ? `${user.firstName[0] ?? ''}${user.lastName[0] ?? ''}` : '?';

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <AppBar
        position="fixed"
        color="inherit"
        elevation={0}
        sx={{
          zIndex: (t) => t.zIndex.drawer + 1,
          backgroundColor: 'rgba(255,255,255,0.92)',
          backdropFilter: 'blur(8px)',
          borderBottom: '1px solid',
          borderColor: 'divider',
          width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
          ml: { md: `${DRAWER_WIDTH}px` },
        }}
      >
        <Toolbar sx={{ gap: 1 }}>
          <IconButton edge="start" sx={{ display: { md: 'none' } }} onClick={() => setMobileOpen(true)} aria-label="Open navigation">
            <MenuIcon />
          </IconButton>
          <Box sx={{ flex: 1 }} />
          <Tooltip title="Notifications">
            <IconButton onClick={() => navigate('/notifications')} aria-label="Notifications">
              <Badge badgeContent={unread ?? 0} color="error">
                <NotificationsNoneIcon />
              </Badge>
            </IconButton>
          </Tooltip>
          <Tooltip title={user?.email ?? ''}>
            <IconButton onClick={(e) => setMenuAnchor(e.currentTarget)} aria-label="Account menu">
              <Avatar sx={{ width: 34, height: 34, bgcolor: 'primary.main', fontSize: '0.85rem', fontWeight: 600 }}>
                {initials}
              </Avatar>
            </IconButton>
          </Tooltip>
          <Menu anchorEl={menuAnchor} open={!!menuAnchor} onClose={() => setMenuAnchor(null)}>
            <Box sx={{ px: 2, py: 1 }}>
              <Typography variant="subtitle2">{user?.firstName} {user?.lastName}</Typography>
              <Typography variant="caption" color="text.secondary">
                {user?.roles.join(', ')}
              </Typography>
            </Box>
            <Divider />
            <MenuItem onClick={() => { setMenuAnchor(null); navigate('/profile'); }}>
              <ListItemIcon><PersonOutlineIcon fontSize="small" /></ListItemIcon>
              My profile
            </MenuItem>
            <MenuItem onClick={async () => { setMenuAnchor(null); await logout(); navigate('/login'); }}>
              <ListItemIcon><LogoutIcon fontSize="small" /></ListItemIcon>
              Sign out
            </MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>

      <Box component="nav" sx={{ width: { md: DRAWER_WIDTH }, flexShrink: { md: 0 } }}>
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={() => setMobileOpen(false)}
          ModalProps={{ keepMounted: true }}
          sx={{
            display: { xs: 'block', md: 'none' },
            '& .MuiDrawer-paper': { width: DRAWER_WIDTH, border: 0 },
          }}
        >
          <SidebarContent onNavigate={() => setMobileOpen(false)} />
        </Drawer>
        <Drawer
          variant="permanent"
          open
          sx={{
            display: { xs: 'none', md: 'block' },
            '& .MuiDrawer-paper': { width: DRAWER_WIDTH, border: 0 },
          }}
        >
          <SidebarContent />
        </Drawer>
      </Box>

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
          px: { xs: 2, sm: 3, md: 4 },
          pb: 6,
        }}
      >
        <Toolbar />
        <Box sx={{ pt: 3, maxWidth: 1400, mx: 'auto' }}>
          <Outlet />
        </Box>
      </Box>
    </Box>
  );
}
