import { Box } from '@mui/material';
import { monoFont } from '../../theme';

/**
 * Signature element: every asset code, batch number and serial renders as a
 * bordered monospace "specimen tag", the way items are labelled in a lab.
 */
export default function CodeTag({ children, muted = false }: { children?: string | null; muted?: boolean }) {
  if (!children) return null;
  return (
    <Box
      component="span"
      sx={{
        fontFamily: monoFont,
        fontSize: '0.78rem',
        fontWeight: 500,
        px: 0.75,
        py: 0.25,
        borderRadius: '4px',
        border: '1px solid',
        borderColor: muted ? 'divider' : 'rgba(14, 124, 102, 0.35)',
        backgroundColor: muted ? 'transparent' : 'rgba(14, 124, 102, 0.06)',
        color: muted ? 'text.secondary' : '#0A5D4D',
        whiteSpace: 'nowrap',
      }}
    >
      {children}
    </Box>
  );
}
