import { useEffect, useState } from 'react';
import {
  Button, Card, CardContent, Chip, InputAdornment, Skeleton, Table, TableBody, TableCell,
  TableHead, TableRow, TextField, Typography,
} from '@mui/material';
import SettingsOutlinedIcon from '@mui/icons-material/SettingsOutlined';
import SellOutlinedIcon from '@mui/icons-material/SellOutlined';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSnackbar } from 'notistack';
import { api, errorMessage, type ApiEnvelope } from '../../api/client';
import type { Department, PricingItem } from '../../api/types';
import { useManageableDepartments } from '../../api/referenceData';
import PageHeader from '../../components/common/PageHeader';
import CodeTag from '../../components/common/CodeTag';
import EmptyState from '../../components/common/EmptyState';

function DepartmentPolicyRow({ department }: { department: Department }) {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();
  const serverValue = department.maintenanceIntervalDays != null
    ? String(department.maintenanceIntervalDays) : '';
  const [value, setValue] = useState(serverValue);

  // Keep the draft in sync when the server value changes (e.g. after a refetch).
  useEffect(() => { setValue(serverValue); }, [serverValue]);

  const mutation = useMutation({
    mutationFn: async () =>
      (await api.put(`/departments/${department.id}/settings`, {
        maintenanceIntervalDays: value === '' ? null : Number(value),
      })).data,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['departments'] });
      enqueueSnackbar(value === ''
        ? `Compulsory maintenance disabled for ${department.name}`
        : `${department.name}: compulsory maintenance every ${Number(value)} day(s)`,
        { variant: 'success' });
    },
    onError: (error) => enqueueSnackbar(errorMessage(error), { variant: 'error' }),
  });

  const invalid = value !== '' && (!Number.isInteger(Number(value)) || Number(value) < 1);
  const dirty = value !== serverValue;

  return (
    <TableRow hover>
      <TableCell><CodeTag muted>{department.code}</CodeTag></TableCell>
      <TableCell>
        <Typography variant="body2" fontWeight={600}>{department.name}</Typography>
      </TableCell>
      <TableCell>{department.facultyName}</TableCell>
      <TableCell sx={{ width: 260 }}>
        <TextField
          size="small" type="number" fullWidth
          value={value}
          onChange={(e) => setValue(e.target.value)}
          placeholder="Disabled"
          inputProps={{ min: 1, 'aria-label': `Maintenance interval for ${department.name}` }}
          error={invalid}
          helperText={invalid ? 'Must be a whole number of days (1 or more)' : undefined}
          InputProps={{
            startAdornment: <InputAdornment position="start">every</InputAdornment>,
            endAdornment: <InputAdornment position="end">days</InputAdornment>,
          }}
        />
      </TableCell>
      <TableCell align="right" sx={{ width: 100 }}>
        <Button size="small" variant="outlined"
                disabled={!dirty || invalid || mutation.isPending}
                onClick={() => mutation.mutate()}>
          Save
        </Button>
      </TableCell>
    </TableRow>
  );
}

const PRICING_TYPE_LABEL: Record<PricingItem['type'], string> = {
  asset: 'Equipment',
  venue: 'Venue',
  consumable: 'Consumable',
};

const PRICING_TYPE_COLOR: Record<PricingItem['type'], 'info' | 'secondary' | 'warning'> = {
  asset: 'info',
  venue: 'secondary',
  consumable: 'warning',
};

function PricingRow({ item }: { item: PricingItem }) {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();
  const serverValue = item.currentFee != null ? String(item.currentFee) : '';
  const [value, setValue] = useState(serverValue);

  // Keep the draft in sync when the server value changes (e.g. after a refetch).
  useEffect(() => { setValue(serverValue); }, [serverValue]);

  const mutation = useMutation({
    mutationFn: async () =>
      (await api.put(`/pricing/${item.type}/${item.id}`, {
        fee: value === '' ? null : Number(value),
      })).data,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pricing'] });
      enqueueSnackbar(value === '' || Number(value) === 0
        ? `${item.name} is now free of charge`
        : `${item.name}: fee set to LKR ${Number(value).toFixed(2)}`,
        { variant: 'success' });
    },
    onError: (error) => enqueueSnackbar(errorMessage(error), { variant: 'error' }),
  });

  const invalid = value !== '' && (Number.isNaN(Number(value)) || Number(value) < 0);
  const dirty = value !== serverValue;

  return (
    <TableRow hover>
      <TableCell sx={{ width: 130 }}>
        <Chip size="small" variant="outlined"
              color={PRICING_TYPE_COLOR[item.type]}
              label={PRICING_TYPE_LABEL[item.type]} />
      </TableCell>
      <TableCell><CodeTag muted>{item.code}</CodeTag></TableCell>
      <TableCell>
        <Typography variant="body2" fontWeight={600}>{item.name}</Typography>
      </TableCell>
      <TableCell sx={{ width: 260 }}>
        <TextField
          size="small" type="number" fullWidth
          value={value}
          onChange={(e) => setValue(e.target.value)}
          placeholder="Free"
          inputProps={{ min: 0, step: '0.01', 'aria-label': `Fee for ${item.name}` }}
          error={invalid}
          helperText={invalid ? 'Must be zero or a positive amount' : undefined}
          InputProps={{
            startAdornment: <InputAdornment position="start">LKR</InputAdornment>,
            endAdornment: item.type === 'consumable' && item.unit
              ? <InputAdornment position="end">per {item.unit}</InputAdornment>
              : undefined,
          }}
        />
      </TableCell>
      <TableCell align="right" sx={{ width: 100 }}>
        <Button size="small" variant="outlined"
                disabled={!dirty || invalid || mutation.isPending}
                onClick={() => mutation.mutate()}>
          Save
        </Button>
      </TableCell>
    </TableRow>
  );
}

function PricingSection() {
  // Server-side scoped: only the items this user may price are returned.
  const pricing = useQuery({
    queryKey: ['pricing', 'items'],
    queryFn: async () =>
      (await api.get<ApiEnvelope<PricingItem[]>>('/pricing/items')).data.data,
  });

  return (
    <Card variant="outlined" sx={{ mt: 3 }}>
      <CardContent sx={{ pb: 1.5 }}>
        <Typography variant="h6" gutterBottom>Pricing</Typography>
        <Typography variant="body2" color="text.secondary">
          Price list applied automatically when a reservation receives final approval:
          equipment and venues are charged a flat fee per booking, consumables per unit
          issued. Leave a fee empty for no charge. Only items within your custodianship
          scope are listed.
        </Typography>
      </CardContent>
      {pricing.isLoading ? (
        <CardContent><Skeleton variant="rounded" height={160} /></CardContent>
      ) : (pricing.data ?? []).length === 0 ? (
        <EmptyState
          title="No priceable items in your scope"
          hint="Equipment, venues and consumables you are allowed to price will appear here."
          icon={<SellOutlinedIcon />}
        />
      ) : (
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Type</TableCell>
              <TableCell>Code</TableCell>
              <TableCell>Name</TableCell>
              <TableCell>Fee</TableCell>
              <TableCell />
            </TableRow>
          </TableHead>
          <TableBody>
            {(pricing.data ?? []).map((item) => (
              <PricingRow key={`${item.type}-${item.id}`} item={item} />
            ))}
          </TableBody>
        </Table>
      )}
    </Card>
  );
}

export default function SettingsPage() {
  // Server-side scoped: only the departments this user may manage are returned.
  const departments = useManageableDepartments();

  return (
    <>
      <PageHeader
        eyebrow="ADMINISTRATION"
        title="Settings"
        crumbs={[{ label: 'Settings' }]}
        subtitle="Departmental policies and system configuration."
      />
      <Card variant="outlined">
        <CardContent sx={{ pb: 1.5 }}>
          <Typography variant="h6" gutterBottom>Departmental maintenance policy</Typography>
          <Typography variant="body2" color="text.secondary">
            When a department has a policy, its assets are automatically scheduled for compulsory
            preventive maintenance once the interval since their last service elapses. Leave the
            interval empty to disable the policy. Only departments within your own department or
            faculty scope are listed.
          </Typography>
        </CardContent>
        {departments.isLoading ? (
          <CardContent><Skeleton variant="rounded" height={160} /></CardContent>
        ) : (departments.data ?? []).length === 0 ? (
          <EmptyState
            title="No departments in your scope"
            hint="Departments you are allowed to manage will appear here."
            icon={<SettingsOutlinedIcon />}
          />
        ) : (
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Code</TableCell>
                <TableCell>Department</TableCell>
                <TableCell>Faculty</TableCell>
                <TableCell>Compulsory maintenance</TableCell>
                <TableCell />
              </TableRow>
            </TableHead>
            <TableBody>
              {(departments.data ?? []).map((department) => (
                <DepartmentPolicyRow key={department.id} department={department} />
              ))}
            </TableBody>
          </Table>
        )}
      </Card>
      <PricingSection />
    </>
  );
}
