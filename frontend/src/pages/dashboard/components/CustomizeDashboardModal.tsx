import {
  Button, Checkbox, Dialog, DialogActions, DialogContent, DialogTitle,
  FormControlLabel, FormGroup, Typography, Divider, Alert
} from '@mui/material';
import TuneIcon from '@mui/icons-material/Tune';
import RestartAltIcon from '@mui/icons-material/RestartAlt';

export interface WidgetConfig {
  showQuickActions: boolean;
  showStatCards: boolean;
  showCategoryChart: boolean;
  showReservationsChart: boolean;
  showPendingApprovals: boolean;
  showOverdueReturns: boolean;
  showLowStock: boolean;
  showMaintenanceQueue: boolean;
  showMyCheckouts: boolean;
  showMyReservations: boolean;
  showConditionSummary: boolean;
}

export const DEFAULT_WIDGET_CONFIG: WidgetConfig = {
  showQuickActions: true,
  showStatCards: true,
  showCategoryChart: true,
  showReservationsChart: true,
  showPendingApprovals: true,
  showOverdueReturns: true,
  showLowStock: true,
  showMaintenanceQueue: true,
  showMyCheckouts: true,
  showMyReservations: true,
  showConditionSummary: true,
};

interface Props {
  open: boolean;
  onClose: () => void;
  config: WidgetConfig;
  onChange: (newConfig: WidgetConfig) => void;
  onReset: () => void;
}

export default function CustomizeDashboardModal({ open, onClose, config, onChange, onReset }: Props) {
  const handleToggle = (key: keyof WidgetConfig) => {
    onChange({
      ...config,
      [key]: !config[key],
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <TuneIcon color="primary" /> Customize Dashboard View
      </DialogTitle>
      <DialogContent dividers>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Tailor your dashboard to show only the widgets and metrics relevant to your daily workflow.
        </Typography>

        <Alert severity="info" sx={{ mb: 2, fontSize: '0.8125rem' }}>
          Preferences are automatically saved for your session.
        </Alert>

        <Typography variant="subtitle2" color="primary" sx={{ mb: 1, fontWeight: 600 }}>
          Global Panels
        </Typography>
        <FormGroup>
          <FormControlLabel
            control={<Checkbox checked={config.showQuickActions} onChange={() => handleToggle('showQuickActions')} />}
            label="Quick Action Shortcuts Bar"
          />
          <FormControlLabel
            control={<Checkbox checked={config.showStatCards} onChange={() => handleToggle('showStatCards')} />}
            label="Key Performance Indicators (KPI Tiles)"
          />
        </FormGroup>

        <Divider sx={{ my: 2 }} />

        <Typography variant="subtitle2" color="primary" sx={{ mb: 1, fontWeight: 600 }}>
          Role & Management Widgets
        </Typography>
        <FormGroup>
          <FormControlLabel
            control={<Checkbox checked={config.showPendingApprovals} onChange={() => handleToggle('showPendingApprovals')} />}
            label="Pending Approval Requests Queue"
          />
          <FormControlLabel
            control={<Checkbox checked={config.showOverdueReturns} onChange={() => handleToggle('showOverdueReturns')} />}
            label="Overdue Returns & Asset Alerts"
          />
          <FormControlLabel
            control={<Checkbox checked={config.showLowStock} onChange={() => handleToggle('showLowStock')} />}
            label="Low-Stock Inventory Warnings"
          />
          <FormControlLabel
            control={<Checkbox checked={config.showMaintenanceQueue} onChange={() => handleToggle('showMaintenanceQueue')} />}
            label="Maintenance & Service Work Orders"
          />
        </FormGroup>

        <Divider sx={{ my: 2 }} />

        <Typography variant="subtitle2" color="primary" sx={{ mb: 1, fontWeight: 600 }}>
          Personal & Operational Lists
        </Typography>
        <FormGroup>
          <FormControlLabel
            control={<Checkbox checked={config.showMyCheckouts} onChange={() => handleToggle('showMyCheckouts')} />}
            label="My Active Loans & Checked-out Items"
          />
          <FormControlLabel
            control={<Checkbox checked={config.showMyReservations} onChange={() => handleToggle('showMyReservations')} />}
            label="My Upcoming Equipment Reservations"
          />
        </FormGroup>

        <Divider sx={{ my: 2 }} />

        <Typography variant="subtitle2" color="primary" sx={{ mb: 1, fontWeight: 600 }}>
          Analytics & Distribution
        </Typography>
        <FormGroup>
          <FormControlLabel
            control={<Checkbox checked={config.showCategoryChart} onChange={() => handleToggle('showCategoryChart')} />}
            label="Assets by Category Chart"
          />
          <FormControlLabel
            control={<Checkbox checked={config.showReservationsChart} onChange={() => handleToggle('showReservationsChart')} />}
            label="Monthly Reservation Trends"
          />
          <FormControlLabel
            control={<Checkbox checked={config.showConditionSummary} onChange={() => handleToggle('showConditionSummary')} />}
            label="Asset Physical Condition Breakdown"
          />
        </FormGroup>
      </DialogContent>
      <DialogActions sx={{ justifyContent: 'space-between', px: 3, py: 2 }}>
        <Button startIcon={<RestartAltIcon />} color="inherit" onClick={onReset}>
          Reset Default
        </Button>
        <Button variant="contained" onClick={onClose}>
          Done
        </Button>
      </DialogActions>
    </Dialog>
  );
}
