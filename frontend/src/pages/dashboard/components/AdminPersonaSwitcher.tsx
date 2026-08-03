import { Box, Chip, Paper, Stack, Typography, Tooltip } from '@mui/material';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import AdminPanelSettingsOutlinedIcon from '@mui/icons-material/AdminPanelSettingsOutlined';
import { PERSONA_METADATA } from '../personaUtils';
import type { PersonaType } from '../personaUtils';

interface Props {
  currentPersona: PersonaType;
  onSelectPersona: (persona: PersonaType) => void;
}

const AVAILABLE_PERSONAS: { id: PersonaType; label: string }[] = [
  { id: 'ADMIN', label: 'Executive Admin' },
  { id: 'FACULTY_DEAN_USER', label: 'Faculty Dean' },
  { id: 'DEPT_ADMIN_USER', label: 'Dept Admin' },
  { id: 'CARETAKER_USER', label: 'Caretaker' },
  { id: 'LAB_MANAGER', label: 'Lab Manager' },
  { id: 'STOREKEEPER', label: 'Storekeeper' },
  { id: 'MAINTENANCE_OFFICER', label: 'Maintenance Officer' },
  { id: 'FINANCE_OFFICER', label: 'Finance & Audit' },
  { id: 'STUDENT_USER', label: 'Student / User' },
];

export default function AdminPersonaSwitcher({ currentPersona, onSelectPersona }: Props) {
  return (
    <Paper
      variant="outlined"
      sx={{
        p: 1.5,
        mb: 2.5,
        backgroundColor: 'rgba(244, 245, 248, 0.6)',
        borderColor: 'divider',
        borderRadius: 2,
      }}
    >
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.5} alignItems={{ md: 'center' }} justifyContent="space-between">
        <Stack direction="row" spacing={1} alignItems="center">
          <AdminPanelSettingsOutlinedIcon color="primary" fontSize="small" />
          <Box>
            <Typography variant="caption" sx={{ fontWeight: 600, color: 'text.primary', display: 'block' }}>
              ADMINISTRATOR PERSONA PREVIEW
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Switch perspective to view role-specific dashboards across the institution
            </Typography>
          </Box>
        </Stack>

        <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap>
          {AVAILABLE_PERSONAS.map((p) => {
            const isSelected = currentPersona === p.id;
            return (
              <Tooltip key={p.id} title={`Preview ${PERSONA_METADATA[p.id].title}`}>
                <Chip
                  icon={isSelected ? <VisibilityOutlinedIcon fontSize="small" /> : undefined}
                  label={p.label}
                  size="small"
                  color={isSelected ? 'primary' : 'default'}
                  variant={isSelected ? 'filled' : 'outlined'}
                  onClick={() => onSelectPersona(p.id)}
                  sx={{
                    fontWeight: isSelected ? 600 : 400,
                    cursor: 'pointer',
                    borderRadius: 1.5,
                    transition: 'all 150ms',
                    '&:hover': {
                      backgroundColor: isSelected ? 'primary.main' : 'action.hover',
                    },
                  }}
                />
              </Tooltip>
            );
          })}
        </Stack>
      </Stack>
    </Paper>
  );
}
