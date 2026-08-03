import { Box, Card, CardContent, Typography, Stack } from '@mui/material';
import type { ReactNode } from 'react';
import { displayFont, monoFont } from '../../theme';

/** Split-panel shell shared by the sign-in and password pages. */
export default function AuthShell({ title, subtitle, children }: {
  title: string;
  subtitle?: string;
  children: ReactNode;
}) {
  return (
    <Box sx={{ minHeight: '100vh', display: 'flex' }}>
      {/* Left Premium Dark Hero Panel (58% width on desktop) */}
      <Box
        sx={{
          display: { xs: 'none', md: 'flex' },
          flexDirection: 'column',
          justifyContent: 'space-between',
          width: { md: '54%', lg: '58%' },
          backgroundColor: '#0E051C',
          color: '#fff',
          p: { md: 5, lg: 6 },
          position: 'relative',
          overflow: 'hidden',
          backgroundImage:
            'radial-gradient(circle at 50% 45%, rgba(109,40,217,0.40), transparent 65%),' +
            'radial-gradient(circle at 15% 15%, rgba(124,58,237,0.25), transparent 50%),' +
            'radial-gradient(circle at 85% 85%, rgba(168,85,247,0.20), transparent 45%)',
        }}
      >
        {/* Top Header with Official Crest & University Name */}
        <Stack direction="row" alignItems="center" spacing={2.5} sx={{ zIndex: 2 }}>
          <Box
            component="img"
            src="/uni-logo.png"
            alt="University of Colombo Crest"
            sx={{ width: 48, height: 56, objectFit: 'contain', filter: 'drop-shadow(0 4px 12px rgba(0,0,0,0.4))' }}
          />
          <Box>
            <Typography sx={{ fontFamily: displayFont, fontWeight: 700, fontSize: '1.25rem', letterSpacing: '0.01em', lineHeight: 1.15 }}>
              University of Colombo
            </Typography>
            <Typography sx={{ fontFamily: monoFont, fontSize: '0.68rem', letterSpacing: '0.18em', color: 'rgba(255,255,255,0.85)', mt: 0.35, fontWeight: 600 }}>
              FACULTY OF SCIENCE
            </Typography>
          </Box>
        </Stack>

        {/* Title Banner */}
        <Box sx={{ zIndex: 2, my: 'auto', maxWidth: 540 }}>
          <Typography sx={{ fontFamily: displayFont, fontWeight: 600, fontSize: { md: '2.1rem', lg: '2.5rem' }, lineHeight: 1.25, mb: 1.5, letterSpacing: '-0.01em' }}>
            Empowering Research &amp; Academic Excellence
          </Typography>
          <Typography sx={{ color: 'rgba(255,255,255,0.85)', fontSize: '0.96rem', lineHeight: 1.6 }}>
            Centralized management, equipment reservation, and maintenance tracking for the Faculty of Science.
          </Typography>
        </Box>

        {/* Footer */}
        <Typography sx={{ fontFamily: monoFont, fontSize: '0.66rem', color: 'rgba(255,255,255,0.55)', zIndex: 2, letterSpacing: '0.12em' }}>
          UNIVERSITY OF COLOMBO · FACULTY OF SCIENCE
        </Typography>
      </Box>

      {/* Right Sign-In Form Container - Enterprise Grade */}
      <Box sx={{ flex: 1, display: 'grid', placeItems: 'center', p: { xs: 3, sm: 4 }, backgroundColor: '#F4F0FA' }}>
        <Card variant="outlined" sx={{ width: '100%', maxWidth: 420, borderRadius: 3.5, boxShadow: '0 20px 50px rgba(19,6,36,0.07)', borderColor: '#E5DEF0', background: '#FFFFFF' }}>
          <CardContent sx={{ p: { xs: 3.5, sm: 4.5 } }}>
            <Stack direction="row" alignItems="center" spacing={1.75} sx={{ mb: 3, display: { xs: 'flex', md: 'none' } }}>
              <Box component="img" src="/uni-logo.png" alt="University Crest" sx={{ width: 38, height: 44 }} />
              <Box>
                <Typography variant="subtitle1" fontWeight={700} color="primary.main" lineHeight={1.1}>
                  University of Colombo
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  Faculty of Science (FOS)
                </Typography>
              </Box>
            </Stack>

            <Typography variant="h5" fontWeight={600} letterSpacing="-0.01em" gutterBottom>{title}</Typography>
            {subtitle && (
              <Typography variant="body2" color="text.secondary" sx={{ mb: 3.5, fontSize: '0.9rem' }}>
                {subtitle}
              </Typography>
            )}
            {children}
          </CardContent>
        </Card>
      </Box>
    </Box>
  );
}
