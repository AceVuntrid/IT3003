import { Link as RouterLink } from 'react-router-dom';
import {
  Card, CardHeader, CardContent, Divider, List, ListItem, ListItemText,
  Typography, Stack, Button, Chip, Grid
} from '@mui/material';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../../api/client';
import type { ApiEnvelope } from '../../../api/client';
import type { ConsumableSummary } from '../../../api/types';
import CodeTag from '../../../components/common/CodeTag';

export default function StorekeeperWidgets() {
  const lowStockQuery = useQuery({
    queryKey: ['dashboard', 'low-stock-storekeeper'],
    queryFn: async () =>
      (await api.get<ApiEnvelope<ConsumableSummary[]>>('/consumables/low-stock')).data.data,
  });

  const items = lowStockQuery.data ?? [];

  return (
    <Grid container spacing={2}>
      <Grid size={{ xs: 12, md: 8 }}>
        <Card variant="outlined" sx={{ height: '100%', borderColor: items.length > 0 ? 'warning.main' : undefined }}>
          <CardHeader
            title={`Low-Stock Reorder Queue (${items.length})`}
            titleTypographyProps={{ variant: 'h6' }}
            subheader="Consumable inventory items at or below reorder threshold"
            action={
              <Button component={RouterLink} to="/consumables" size="small" endIcon={<ArrowForwardIcon />}>
                Store Inventory
              </Button>
            }
          />
          <Divider />
          {lowStockQuery.isLoading ? (
            <Typography variant="body2" color="text.secondary" sx={{ p: 3 }}>Checking inventory stock levels...</Typography>
          ) : items.length === 0 ? (
            <CardContent sx={{ py: 4, textAlign: 'center' }}>
              <Typography variant="body1" color="text.primary" sx={{ fontWeight: 500 }}>
                Inventory healthy! All consumables above reorder point.
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                Chemicals, glassware, and disposable stock are sufficiently supplied.
              </Typography>
            </CardContent>
          ) : (
            <List dense disablePadding>
              {items.slice(0, 7).map((item) => (
                <ListItem key={item.id} divider sx={{ py: 1.25, px: 2 }}>
                  <ListItemText
                    primary={
                      <Stack direction="row" spacing={1} alignItems="center">
                        <CodeTag>{item.itemCode}</CodeTag>
                        <Typography variant="body2" sx={{ fontWeight: 600 }} noWrap>
                          {item.name}
                        </Typography>
                        <Chip label={item.categoryName} size="small" variant="outlined" />
                      </Stack>
                    }
                    secondary={
                      <Typography variant="caption" color="text.secondary">
                        Stock on hand: <strong>{item.currentQuantity} {item.unitOfMeasure}</strong> (Reorder point: {item.reorderLevel})
                      </Typography>
                    }
                  />
                  <Button
                    component={RouterLink}
                    to="/consumables"
                    size="small"
                    variant="outlined"
                    color="warning"
                    sx={{ textTransform: 'none', borderRadius: 1.5 }}
                  >
                    Receive Stock
                  </Button>
                </ListItem>
              ))}
            </List>
          )}
        </Card>
      </Grid>

      <Grid size={{ xs: 12, md: 4 }}>
        <Card variant="outlined" sx={{ height: '100%', backgroundColor: 'rgba(234, 179, 8, 0.03)' }}>
          <CardHeader
            title="Consumable Stock Guidelines"
            titleTypographyProps={{ variant: 'h6' }}
            avatar={<WarningAmberOutlinedIcon color="warning" />}
          />
          <Divider />
          <CardContent sx={{ py: 2 }}>
            <Typography variant="body2" paragraph color="text.secondary">
              Keep chemical containers, hazardous materials, and lab glassware logged accurately upon arrival.
            </Typography>
            <Stack spacing={1.5} sx={{ mt: 1 }}>
              <Button component={RouterLink} to="/consumables" variant="contained" color="primary" fullWidth>
                Issue Stock to Lab
              </Button>
              <Button component={RouterLink} to="/consumables" variant="outlined" color="primary" fullWidth>
                Receive Inventory Shipment
              </Button>
            </Stack>
          </CardContent>
        </Card>
      </Grid>
    </Grid>
  );
}
