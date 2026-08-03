import { Chip } from '@mui/material';
import { statusColors } from '../../theme';

/** Consistent status pill used in every table and detail view. */
export default function StatusChip({ value, size = 'small' }: { value?: string | null; size?: 'small' | 'medium' }) {
  if (!value) return null;
  const colors = statusColors[value] ?? { bg: '#EEF1F0', fg: '#3E4C52' };
  const label = value.replaceAll('_', ' ').toLowerCase()
    .replace(/^\w/, (c) => c.toUpperCase());
  return (
    <Chip
      label={label}
      size={size}
      sx={{ backgroundColor: colors.bg, color: colors.fg, fontWeight: 600 }}
    />
  );
}
