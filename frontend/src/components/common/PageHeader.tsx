import { Box, Breadcrumbs, Link, Stack, Typography } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import type { ReactNode } from 'react';

interface Crumb {
  label: string;
  to?: string;
  onClick?: () => void;
}

interface PageHeaderProps {
  title: ReactNode;
  eyebrow?: string;
  crumbs?: Crumb[];
  actions?: ReactNode;
  subtitle?: ReactNode;
}

export default function PageHeader({ title, eyebrow, crumbs, actions, subtitle }: PageHeaderProps) {
  return (
    <Box sx={{ mb: 3 }}>
      {crumbs && crumbs.length > 0 && (
        <Breadcrumbs sx={{ mb: 1, fontSize: '0.82rem' }}>
          <Link component={RouterLink} underline="hover" color="inherit" to="/">
            Home
          </Link>
          {crumbs.map((crumb) =>
            crumb.to ? (
              <Link key={crumb.label} component={RouterLink} underline="hover" color="inherit" to={crumb.to}>
                {crumb.label}
              </Link>
            ) : crumb.onClick ? (
              <Link
                key={crumb.label}
                component="button"
                underline="hover"
                color="inherit"
                onClick={crumb.onClick}
                sx={{ cursor: 'pointer', background: 'none', border: 'none', font: 'inherit', p: 0 }}
              >
                {crumb.label}
              </Link>
            ) : (
              <Typography key={crumb.label} color="text.primary" fontSize="inherit">
                {crumb.label}
              </Typography>
            ),
          )}
        </Breadcrumbs>
      )}
      <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between"
             alignItems={{ xs: 'flex-start', sm: 'center' }} spacing={1.5}>
        <Box>
          {eyebrow && (
            <Typography variant="overline" color="primary.dark" sx={{ display: 'block', lineHeight: 1.6 }}>
              {eyebrow}
            </Typography>
          )}
          <Typography variant="h4" component="h1">{title}</Typography>
          {subtitle && (
            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
              {subtitle}
            </Typography>
          )}
        </Box>
        {actions && <Stack direction="row" spacing={1} flexWrap="wrap">{actions}</Stack>}
      </Stack>
    </Box>
  );
}
