import { createTheme, alpha } from '@mui/material/styles';

/**
 * "Royal University Purple" design tokens.
 * Ink:      #1A0B2E  deep velvet purple — sidebar, drawer, dark cards
 * Purple:   #6D28D9  primary royal purple actions & active states
 * Amber:    #D97706  attention, pending states
 * Paper:    #F7F5FB  soft lavender cool paper background
 * Claret:   #DC2626  errors, damage
 * Violet:   #A855F7  vibrant purple accent
 */
export const ink = '#1A0B2E';
export const royalPurple = '#6D28D9';
export const viridian = '#6D28D9'; // Backward compatibility alias
export const amber = '#D97706';
export const paper = '#F7F5FB';
export const claret = '#DC2626';
export const violet = '#A855F7';

export const displayFont = "'Sora', 'Inter', sans-serif";
export const bodyFont = "'Inter', system-ui, sans-serif";
export const monoFont = "'IBM Plex Mono', ui-monospace, monospace";

export const theme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: '#6D28D9', dark: '#4C1D95', light: '#8B5CF6', contrastText: '#fff' },
    secondary: { main: '#A855F7', dark: '#7E22CE', light: '#C084FC' },
    warning: { main: '#D97706', contrastText: '#fff' },
    error: { main: '#DC2626' },
    info: { main: '#3B82F6' },
    success: { main: '#10B981' },
    background: { default: paper, paper: '#FFFFFF' },
    text: { primary: '#1F152E', secondary: '#6B5E7D' },
    divider: '#E7E2F1',
  },
  shape: { borderRadius: 10 },
  typography: {
    fontFamily: bodyFont,
    h1: { fontFamily: displayFont, fontWeight: 600 },
    h2: { fontFamily: displayFont, fontWeight: 600 },
    h3: { fontFamily: displayFont, fontWeight: 600 },
    h4: { fontFamily: displayFont, fontWeight: 600, fontSize: '1.6rem' },
    h5: { fontFamily: displayFont, fontWeight: 600, fontSize: '1.25rem' },
    h6: { fontFamily: displayFont, fontWeight: 600, fontSize: '1.05rem' },
    subtitle2: { fontWeight: 600 },
    button: { textTransform: 'none', fontWeight: 600 },
    overline: {
      fontFamily: monoFont,
      letterSpacing: '0.12em',
      fontWeight: 500,
    },
  },
  components: {
    MuiPaper: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: { backgroundImage: 'none' },
        outlined: { borderColor: '#E7E2F1' },
      },
    },
    MuiCard: {
      defaultProps: { variant: 'outlined' },
      styleOverrides: {
        root: { borderColor: '#E7E2F1' },
      },
    },
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: { borderRadius: 8 },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: { fontWeight: 600 },
      },
    },
    MuiTableHead: {
      styleOverrides: {
        root: {
          '& th': {
            fontWeight: 600,
            color: '#6B5E7D',
            backgroundColor: '#F3F0FA',
          },
        },
      },
    },
    MuiTooltip: {
      styleOverrides: {
        tooltip: { backgroundColor: ink, fontSize: '0.75rem' },
      },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          backgroundColor: '#fff',
        },
      },
    },
  },
});

/** Consistent status → color mapping used across every module. */
export const statusColors: Record<string, { bg: string; fg: string }> = {
  AVAILABLE: { bg: alpha('#6D28D9', 0.12), fg: '#5B21B6' },
  ACTIVE: { bg: alpha('#6D28D9', 0.12), fg: '#5B21B6' },
  APPROVED: { bg: alpha('#6D28D9', 0.12), fg: '#5B21B6' },
  COMPLETED: { bg: alpha('#6D28D9', 0.12), fg: '#5B21B6' },
  RETURNED: { bg: alpha('#6D28D9', 0.12), fg: '#5B21B6' },
  PAID: { bg: alpha('#6D28D9', 0.12), fg: '#5B21B6' },
  RESERVED: { bg: alpha('#A855F7', 0.16), fg: '#7E22CE' },
  PENDING_APPROVAL: { bg: alpha('#D97706', 0.16), fg: '#B45309' },
  PENDING_LEVEL_1: { bg: alpha('#D97706', 0.16), fg: '#B45309' },
  PENDING_LEVEL_2: { bg: alpha('#A855F7', 0.16), fg: '#7E22CE' },
  PENDING: { bg: alpha('#D97706', 0.16), fg: '#B45309' },
  SUBMITTED: { bg: alpha('#D97706', 0.16), fg: '#B45309' },
  CHECKED_OUT: { bg: alpha('#D97706', 0.16), fg: '#B45309' },
  IN_PROGRESS: { bg: alpha('#3B82F6', 0.14), fg: '#1D4ED8' },
  OPEN: { bg: alpha('#3B82F6', 0.14), fg: '#1D4ED8' },
  ASSIGNED: { bg: alpha('#3B82F6', 0.14), fg: '#1D4ED8' },
  UNDER_MAINTENANCE: { bg: alpha('#3B82F6', 0.14), fg: '#1D4ED8' },
  WAITING_FOR_PARTS: { bg: alpha('#D97706', 0.16), fg: '#B45309' },
  WAITING_FOR_VENDOR: { bg: alpha('#D97706', 0.16), fg: '#B45309' },
  OVERDUE: { bg: alpha('#DC2626', 0.14), fg: '#B91C1C' },
  REJECTED: { bg: alpha('#DC2626', 0.14), fg: '#B91C1C' },
  DAMAGED: { bg: alpha('#DC2626', 0.14), fg: '#B91C1C' },
  UNREPAIRABLE: { bg: alpha('#DC2626', 0.14), fg: '#B91C1C' },
  LOST: { bg: alpha('#6B7280', 0.14), fg: '#374151' },
  CANCELLED: { bg: alpha('#6B7280', 0.14), fg: '#374151' },
  ARCHIVED: { bg: alpha('#6B7280', 0.14), fg: '#374151' },
  DISABLED: { bg: alpha('#6B7280', 0.14), fg: '#374151' },
  DISPOSED: { bg: alpha('#6B7280', 0.14), fg: '#374151' },
  LOCKED: { bg: alpha('#DC2626', 0.14), fg: '#B91C1C' },
  DRAFT: { bg: alpha('#6B7280', 0.14), fg: '#374151' },
  READY_FOR_COLLECTION: { bg: alpha('#6D28D9', 0.12), fg: '#5B21B6' },
  NO_SHOW: { bg: alpha('#6B7280', 0.14), fg: '#374151' },
  PARTIALLY_REFUNDED: { bg: alpha('#A855F7', 0.16), fg: '#7E22CE' },
  REFUNDED: { bg: alpha('#A855F7', 0.16), fg: '#7E22CE' },
  UNPAID: { bg: alpha('#D97706', 0.16), fg: '#B45309' },
  PARTIALLY_PAID: { bg: alpha('#D97706', 0.16), fg: '#B45309' },
};
