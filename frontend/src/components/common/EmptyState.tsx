import { Box, Typography } from '@mui/material';
import Inventory2OutlinedIcon from '@mui/icons-material/Inventory2Outlined';
import type { ReactNode } from 'react';

interface EmptyStateProps {
  title: string;
  hint?: string;
  action?: ReactNode;
  icon?: ReactNode;
}

export default function EmptyState({ title, hint, action, icon }: EmptyStateProps) {
  return (
    <Box sx={{ textAlign: 'center', py: 7, px: 2 }}>
      <Box sx={{ color: 'text.secondary', mb: 1.5, '& svg': { fontSize: 44, opacity: 0.5 } }}>
        {icon ?? <Inventory2OutlinedIcon />}
      </Box>
      <Typography variant="h6" gutterBottom>{title}</Typography>
      {hint && (
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2, maxWidth: 420, mx: 'auto' }}>
          {hint}
        </Typography>
      )}
      {action}
    </Box>
  );
}
