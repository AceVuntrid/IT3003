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
      {/* Left Dominant Bright Royal Purple Hero Panel (68% width on desktop) */}
      <Box
        sx={{
          display: { xs: 'none', md: 'flex' },
          flexDirection: 'column',
          justifyContent: 'space-between',
          width: { md: '64%', lg: '68%' },
          backgroundColor: '#23074D',
          color: '#fff',
          p: { md: 6, lg: 7 },
          position: 'relative',
          overflow: 'hidden',
          backgroundImage:
            'radial-gradient(circle at 45% 40%, rgba(147,51,234,0.55), transparent 65%),' +
            'radial-gradient(circle at 15% 15%, rgba(168,85,247,0.40), transparent 50%),' +
            'radial-gradient(circle at 85% 85%, rgba(192,132,252,0.30), transparent 45%)',
        }}
      >
        {/* Top Header with Official Crest & University Name in Top Left */}
        <Stack direction="row" alignItems="center" spacing={2.5} sx={{ zIndex: 2 }}>
          <Box
            component="img"
            src="/uni-logo.png"
            alt="University of Colombo Crest"
            sx={{ width: 56, height: 64, objectFit: 'contain', filter: 'drop-shadow(0 4px 14px rgba(0,0,0,0.3))' }}
          />
          <Box>
            <Typography sx={{ fontFamily: displayFont, fontWeight: 700, fontSize: '1.35rem', letterSpacing: '0.01em', lineHeight: 1.15 }}>
              University of Colombo
            </Typography>
            <Typography sx={{ fontFamily: monoFont, fontSize: '0.72rem', letterSpacing: '0.18em', color: 'rgba(255,255,255,0.92)', mt: 0.4, fontWeight: 600 }}>
              FACULTY OF SCIENCE
            </Typography>
          </Box>
        </Stack>

        {/* Title Banner - Positioned Lower & Continuous Text */}
        <Box sx={{ zIndex: 2, mt: 4, mb: 1, maxWidth: 640 }}>
          <Typography sx={{ fontFamily: displayFont, fontWeight: 700, fontSize: { md: '2.3rem', lg: '2.8rem' }, lineHeight: 1.18, mb: 1.5, letterSpacing: '-0.015em' }}>
            Empowering Research &amp; Academic Excellence
          </Typography>
          <Typography sx={{ color: 'rgba(255,255,255,0.88)', fontSize: { md: '1.02rem', lg: '1.12rem' }, lineHeight: 1.55, maxWidth: 560 }}>
            Centralized management, equipment reservation, and maintenance tracking for the Faculty of Science.
          </Typography>
        </Box>

        {/* Middle/Lower Hero Area: Transparent White Building Sketch */}
        <Box
          sx={{
            zIndex: 2,
            my: 'auto',
            py: 1,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            position: 'relative',
            width: '100%',
          }}
        >
          {/* Ambient Vibrant Purple Glow */}
          <Box
            sx={{
              position: 'absolute',
              width: 680,
              height: 340,
              borderRadius: '50%',
              background: 'radial-gradient(circle, rgba(192,132,252,0.38) 0%, rgba(124,58,237,0) 70%)',
              filter: 'blur(50px)',
              pointerEvents: 'none',
              zIndex: 1,
            }}
          />

          {/* Transparent 4K Ultra-Sharp Building Sketch — Softly Dimmed */}
          <Box
            component="img"
            src="/assets/university_building.png?v=4k_dimmed_v6"
            alt="University Main Building Line Sketch"
            sx={{
              width: '110%',
              maxWidth: 1150,
              maxHeight: 580,
              objectFit: 'contain',
              mixBlendMode: 'screen',
              filter: 'drop-shadow(0 0 16px rgba(255,255,255,0.22)) brightness(0.92) contrast(1.12)',
              opacity: 0.84,
              zIndex: 2,
              transform: 'translateY(12px)',
              transition: 'all 0.4s ease',
              '&:hover': {
                opacity: 0.96,
                transform: 'translateY(12px) scale(1.025)',
                filter: 'drop-shadow(0 0 28px rgba(255,255,255,0.45)) brightness(1.05)',
              },
            }}
          />
        </Box>

        {/* Footer */}
        <Typography sx={{ fontFamily: monoFont, fontSize: '0.68rem', color: 'rgba(255,255,255,0.55)', zIndex: 2, letterSpacing: '0.12em' }}>
          UNIVERSITY OF COLOMBO · FACULTY OF SCIENCE
        </Typography>
      </Box>

      {/* Right Sign-In Form Container - Compact & Sleek */}
      <Box sx={{ flex: 1, display: 'grid', placeItems: 'center', p: { xs: 2.5, sm: 3.5 }, backgroundColor: '#F3EEF9' }}>
        <Card variant="outlined" sx={{ width: '100%', maxWidth: 380, borderRadius: 3, boxShadow: '0 16px 40px rgba(17,5,36,0.08)', borderColor: '#E4DAF2', background: '#FFFFFF' }}>
          <CardContent sx={{ p: { xs: 3, sm: 3.75 } }}>
            {/* Crest Header - Always Visible on Sign-In Card */}
            <Stack direction="row" alignItems="center" spacing={2} sx={{ mb: 3 }}>
              <Box
                component="img"
                src="/uni-logo.png"
                alt="University Crest"
                sx={{ width: 44, height: 52, objectFit: 'contain' }}
              />
              <Box>
                <Typography variant="subtitle1" fontWeight={700} color="primary.main" lineHeight={1.15}>
                  University of Colombo
                </Typography>
                <Typography variant="caption" color="text.secondary" fontWeight={500}>
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
