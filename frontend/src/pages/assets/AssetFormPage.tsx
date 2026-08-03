import { useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Box, Button, Card, CardContent, CardHeader, Checkbox, Divider, FormControlLabel,
  Grid, MenuItem, Skeleton, Stack, TextField, Typography,
} from '@mui/material';
import { useForm, Controller } from 'react-hook-form';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSnackbar } from 'notistack';
import { api, errorMessage } from '../../api/client';
import type { ApiEnvelope } from '../../api/client';
import type { AssetDetail } from '../../api/types';
import { useCategories, useFaculties, useDepartments, useLocations } from '../../api/referenceData';
import PageHeader from '../../components/common/PageHeader';

const TYPES = ['FIXED', 'CONSUMABLE', 'FACILITY', 'ROOM', 'VEHICLE'];
const CONDITIONS = ['NEW', 'EXCELLENT', 'GOOD', 'FAIR', 'POOR', 'DAMAGED', 'UNSERVICEABLE'];

interface AssetFormValues {
  name: string;
  assetCode: string;
  assetType: string;
  categoryId: string;
  description: string;
  brand: string;
  model: string;
  manufacturer: string;
  serialNumber: string;
  barcode: string;
  tags: string;
  facultyId: string;
  departmentId: string;
  locationId: string;
  locationNotes: string;
  purchasePrice: string;
  currency: string;
  purchaseDate: string;
  purchaseOrderNumber: string;
  invoiceNumber: string;
  fundingSource: string;
  grantCode: string;
  depreciationMethod: string;
  usefulLifeYears: string;
  salvageValue: string;
  condition: string;
  quantity: string;
  reservable: boolean;
  approvalRequired: boolean;
  externalUseAllowed: boolean;
  depositRequired: boolean;
  depositAmount: string;
  maxReservationHours: string;
  warrantyStartDate: string;
  warrantyEndDate: string;
  warrantyProvider: string;
  serviceIntervalMonths: string;
  lastServiceDate: string;
  nextServiceDate: string;
  calibrationRequired: boolean;
  calibrationIntervalMonths: string;
  lastCalibrationDate: string;
  nextCalibrationDate: string;
}

const EMPTY: AssetFormValues = {
  name: '', assetCode: '', assetType: 'FIXED', categoryId: '', description: '',
  brand: '', model: '', manufacturer: '', serialNumber: '', barcode: '', tags: '',
  facultyId: '', departmentId: '', locationId: '', locationNotes: '',
  purchasePrice: '', currency: 'LKR', purchaseDate: '',
  purchaseOrderNumber: '', invoiceNumber: '', fundingSource: '', grantCode: '',
  depreciationMethod: '', usefulLifeYears: '', salvageValue: '',
  condition: 'GOOD', quantity: '1', reservable: true, approvalRequired: true,
  externalUseAllowed: false, depositRequired: false, depositAmount: '', maxReservationHours: '',
  warrantyStartDate: '', warrantyEndDate: '', warrantyProvider: '',
  serviceIntervalMonths: '', lastServiceDate: '', nextServiceDate: '',
  calibrationRequired: false, calibrationIntervalMonths: '', lastCalibrationDate: '', nextCalibrationDate: '',
};

function toPayload(values: AssetFormValues) {
  const opt = (v: string) => (v === '' ? null : v);
  const num = (v: string) => (v === '' ? null : Number(v));
  return {
    name: values.name,
    assetCode: opt(values.assetCode),
    assetType: values.assetType,
    categoryId: values.categoryId,
    description: opt(values.description),
    brand: opt(values.brand),
    model: opt(values.model),
    manufacturer: opt(values.manufacturer),
    serialNumber: opt(values.serialNumber),
    barcode: opt(values.barcode),
    tags: opt(values.tags),
    facultyId: values.facultyId,
    departmentId: opt(values.departmentId),
    locationId: values.locationId,
    locationNotes: opt(values.locationNotes),
    purchasePrice: num(values.purchasePrice),
    currency: opt(values.currency),
    purchaseDate: opt(values.purchaseDate),
    purchaseOrderNumber: opt(values.purchaseOrderNumber),
    invoiceNumber: opt(values.invoiceNumber),
    fundingSource: opt(values.fundingSource),
    grantCode: opt(values.grantCode),
    depreciationMethod: opt(values.depreciationMethod),
    usefulLifeYears: num(values.usefulLifeYears),
    salvageValue: num(values.salvageValue),
    condition: values.condition,
    quantity: num(values.quantity),
    reservable: values.reservable,
    approvalRequired: values.approvalRequired,
    externalUseAllowed: values.externalUseAllowed,
    depositRequired: values.depositRequired,
    depositAmount: num(values.depositAmount),
    maxReservationHours: num(values.maxReservationHours),
    warrantyStartDate: opt(values.warrantyStartDate),
    warrantyEndDate: opt(values.warrantyEndDate),
    warrantyProvider: opt(values.warrantyProvider),
    serviceIntervalMonths: num(values.serviceIntervalMonths),
    lastServiceDate: opt(values.lastServiceDate),
    nextServiceDate: opt(values.nextServiceDate),
    calibrationRequired: values.calibrationRequired,
    calibrationIntervalMonths: num(values.calibrationIntervalMonths),
    lastCalibrationDate: opt(values.lastCalibrationDate),
    nextCalibrationDate: opt(values.nextCalibrationDate),
  };
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <Card variant="outlined" sx={{ mb: 2 }}>
      <CardHeader title={title} titleTypographyProps={{ variant: 'h6' }} sx={{ pb: 0.5 }} />
      <Divider sx={{ mx: 2 }} />
      <CardContent>
        <Grid container spacing={2}>{children}</Grid>
      </CardContent>
    </Card>
  );
}

export default function AssetFormPage() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();

  const { register, control, handleSubmit, reset, watch, formState: { errors, isSubmitting } } =
    useForm<AssetFormValues>({ defaultValues: EMPTY });

  const facultyId = watch('facultyId');
  const depositRequired = watch('depositRequired');
  const calibrationRequired = watch('calibrationRequired');

  const faculties = useFaculties();
  const departments = useDepartments(facultyId || undefined);
  const locations = useLocations();
  const categories = useCategories();

  const existing = useQuery({
    queryKey: ['asset', id],
    enabled: isEdit,
    queryFn: async () => (await api.get<ApiEnvelope<AssetDetail>>(`/assets/${id}`)).data.data,
  });

  useEffect(() => {
    if (existing.data) {
      const a = existing.data;
      reset({
        ...EMPTY,
        name: a.name,
        assetCode: a.assetCode,
        assetType: a.assetType,
        categoryId: a.categoryId,
        description: a.description ?? '',
        brand: a.brand ?? '',
        model: a.model ?? '',
        manufacturer: a.manufacturer ?? '',
        serialNumber: a.serialNumber ?? '',
        barcode: a.barcode ?? '',
        tags: a.tags ?? '',
        facultyId: a.facultyId,
        departmentId: a.departmentId ?? '',
        locationId: a.locationId,
        locationNotes: a.locationNotes ?? '',
        purchasePrice: a.purchasePrice?.toString() ?? '',
        currency: a.currency,
        purchaseDate: a.purchaseDate ?? '',
        purchaseOrderNumber: a.purchaseOrderNumber ?? '',
        invoiceNumber: a.invoiceNumber ?? '',
        fundingSource: a.fundingSource ?? '',
        grantCode: a.grantCode ?? '',
        depreciationMethod: a.depreciationMethod ?? '',
        usefulLifeYears: a.usefulLifeYears?.toString() ?? '',
        salvageValue: a.salvageValue?.toString() ?? '',
        condition: a.condition,
        quantity: a.quantity.toString(),
        reservable: a.reservable,
        approvalRequired: a.approvalRequired,
        externalUseAllowed: a.externalUseAllowed,
        depositRequired: a.depositRequired,
        depositAmount: a.depositAmount?.toString() ?? '',
        maxReservationHours: a.maxReservationHours?.toString() ?? '',
        warrantyStartDate: a.warrantyStartDate ?? '',
        warrantyEndDate: a.warrantyEndDate ?? '',
        warrantyProvider: a.warrantyProvider ?? '',
        serviceIntervalMonths: a.serviceIntervalMonths?.toString() ?? '',
        lastServiceDate: a.lastServiceDate ?? '',
        nextServiceDate: a.nextServiceDate ?? '',
        calibrationRequired: a.calibrationRequired,
        calibrationIntervalMonths: a.calibrationIntervalMonths?.toString() ?? '',
        lastCalibrationDate: a.lastCalibrationDate ?? '',
        nextCalibrationDate: a.nextCalibrationDate ?? '',
      });
    }
  }, [existing.data, reset]);

  const mutation = useMutation({
    mutationFn: async (values: AssetFormValues) => {
      const payload = toPayload(values);
      const response = isEdit
        ? await api.put<ApiEnvelope<AssetDetail>>(`/assets/${id}`, payload)
        : await api.post<ApiEnvelope<AssetDetail>>('/assets', payload);
      return response.data.data;
    },
    onSuccess: (asset) => {
      queryClient.invalidateQueries({ queryKey: ['assets'] });
      queryClient.invalidateQueries({ queryKey: ['asset', asset.id] });
      enqueueSnackbar(isEdit ? 'Asset updated' : `Asset ${asset.assetCode} created`, { variant: 'success' });
      navigate(`/assets/${asset.id}`);
    },
    onError: (error) => enqueueSnackbar(errorMessage(error), { variant: 'error' }),
  });

  if (isEdit && existing.isLoading) {
    return <Skeleton variant="rounded" height={480} />;
  }

  const field = (size: { xs?: number; sm?: number; md?: number } = { xs: 12, sm: 6, md: 4 }) => ({ size });

  return (
    <Box component="form" onSubmit={handleSubmit((values) => mutation.mutate(values))} noValidate>
      <PageHeader
        eyebrow="INVENTORY"
        title={isEdit ? `Edit ${existing.data?.assetCode ?? 'asset'}` : 'Add asset'}
        crumbs={[{ label: 'Assets', to: '/assets' }, { label: isEdit ? 'Edit' : 'New' }]}
        actions={
          <>
            <Button color="inherit" onClick={() => navigate(-1)}>Cancel</Button>
            <Button type="submit" variant="contained" disabled={isSubmitting || mutation.isPending}>
              {isEdit ? 'Save changes' : 'Save asset'}
            </Button>
          </>
        }
      />

      <Section title="Basic information">
        <Grid {...field({ xs: 12, sm: 6 })}>
          <TextField label="Asset name" fullWidth required
                     error={!!errors.name} helperText={errors.name?.message}
                     {...register('name', { required: 'Asset name is required' })} />
        </Grid>
        <Grid {...field({ xs: 6, sm: 3 })}>
          <TextField label="Asset code" fullWidth placeholder="Auto-generated"
                     disabled={isEdit}
                     {...register('assetCode')} />
        </Grid>
        <Grid {...field({ xs: 6, sm: 3 })}>
          <Controller name="assetType" control={control} render={({ field: f }) => (
            <TextField {...f} select label="Asset type" fullWidth required>
              {TYPES.map((t) => <MenuItem key={t} value={t}>{t}</MenuItem>)}
            </TextField>
          )} />
        </Grid>
        <Grid {...field()}>
          <Controller name="categoryId" control={control}
                      rules={{ required: 'Category is required' }}
                      render={({ field: f, fieldState }) => (
            <TextField {...f} select label="Category" fullWidth required
                       error={!!fieldState.error} helperText={fieldState.error?.message}>
              {(categories.data ?? []).map((c) => <MenuItem key={c.id} value={c.id}>{c.name}</MenuItem>)}
            </TextField>
          )} />
        </Grid>
        <Grid {...field()}>
          <TextField label="Brand" fullWidth {...register('brand')} />
        </Grid>
        <Grid {...field()}>
          <TextField label="Model" fullWidth {...register('model')} />
        </Grid>
        <Grid {...field()}>
          <TextField label="Manufacturer" fullWidth {...register('manufacturer')} />
        </Grid>
        <Grid {...field()}>
          <TextField label="Serial number" fullWidth {...register('serialNumber')} />
        </Grid>
        <Grid {...field()}>
          <TextField label="Barcode" fullWidth {...register('barcode')} />
        </Grid>
        <Grid {...field({ xs: 12 })}>
          <TextField label="Description" fullWidth multiline minRows={2} {...register('description')} />
        </Grid>
        <Grid {...field({ xs: 12 })}>
          <TextField label="Tags (comma separated)" fullWidth {...register('tags')} />
        </Grid>
      </Section>

      <Section title="Ownership and location">
        <Grid {...field()}>
          <Controller name="facultyId" control={control}
                      rules={{ required: 'Faculty is required' }}
                      render={({ field: f, fieldState }) => (
            <TextField {...f} select label="Faculty" fullWidth required
                       error={!!fieldState.error} helperText={fieldState.error?.message}>
              {(faculties.data ?? []).map((x) => <MenuItem key={x.id} value={x.id}>{x.name}</MenuItem>)}
            </TextField>
          )} />
        </Grid>
        <Grid {...field()}>
          <Controller name="departmentId" control={control} render={({ field: f }) => (
            <TextField {...f} select label="Department" fullWidth>
              <MenuItem value="">None</MenuItem>
              {(departments.data ?? []).map((d) => <MenuItem key={d.id} value={d.id}>{d.name}</MenuItem>)}
            </TextField>
          )} />
        </Grid>
        <Grid {...field()}>
          <Controller name="locationId" control={control}
                      rules={{ required: 'Location is required' }}
                      render={({ field: f, fieldState }) => (
            <TextField {...f} select label="Location" fullWidth required
                       error={!!fieldState.error} helperText={fieldState.error?.message}>
              {(locations.data ?? []).map((l) => (
                <MenuItem key={l.id} value={l.id}>{l.name} ({l.code})</MenuItem>
              ))}
            </TextField>
          )} />
        </Grid>
        <Grid {...field({ xs: 12 })}>
          <TextField label="Exact location notes" fullWidth {...register('locationNotes')} />
        </Grid>
      </Section>

      <Section title="Financial information">
        <Grid {...field()}>
          <TextField label="Purchase price" type="number" fullWidth
                     inputProps={{ min: 0, step: '0.01' }} {...register('purchasePrice')} />
        </Grid>
        <Grid {...field({ xs: 6, sm: 3, md: 2 })}>
          <TextField label="Currency" fullWidth {...register('currency')} />
        </Grid>
        <Grid {...field()}>
          <TextField label="Purchase date" type="date" fullWidth InputLabelProps={{ shrink: true }}
                     {...register('purchaseDate')} />
        </Grid>
        <Grid {...field()}>
          <TextField label="Purchase order number" fullWidth {...register('purchaseOrderNumber')} />
        </Grid>
        <Grid {...field()}>
          <TextField label="Invoice number" fullWidth {...register('invoiceNumber')} />
        </Grid>
        <Grid {...field()}>
          <TextField label="Funding source" fullWidth {...register('fundingSource')} />
        </Grid>
        <Grid {...field()}>
          <TextField label="Grant or project code" fullWidth {...register('grantCode')} />
        </Grid>
        <Grid {...field()}>
          <TextField label="Depreciation method" fullWidth placeholder="e.g. Straight line"
                     {...register('depreciationMethod')} />
        </Grid>
        <Grid {...field({ xs: 6, sm: 3 })}>
          <TextField label="Useful life (years)" type="number" fullWidth inputProps={{ min: 0 }}
                     {...register('usefulLifeYears')} />
        </Grid>
        <Grid {...field({ xs: 6, sm: 3 })}>
          <TextField label="Salvage value" type="number" fullWidth inputProps={{ min: 0, step: '0.01' }}
                     {...register('salvageValue')} />
        </Grid>
      </Section>

      <Section title="Condition and availability">
        <Grid {...field({ xs: 6, sm: 3 })}>
          <Controller name="condition" control={control} render={({ field: f }) => (
            <TextField {...f} select label="Condition" fullWidth>
              {CONDITIONS.map((c) => <MenuItem key={c} value={c}>{c}</MenuItem>)}
            </TextField>
          )} />
        </Grid>
        <Grid {...field({ xs: 6, sm: 3 })}>
          <TextField label="Quantity" type="number" fullWidth required inputProps={{ min: 1 }}
                     error={!!errors.quantity} helperText={errors.quantity?.message}
                     {...register('quantity', {
                       required: 'Quantity is required',
                       min: { value: 1, message: 'Quantity must be greater than zero' },
                     })} />
        </Grid>
        <Grid {...field({ xs: 6, sm: 3 })}>
          <TextField label="Max reservation (hours)" type="number" fullWidth inputProps={{ min: 1 }}
                     {...register('maxReservationHours')} />
        </Grid>
        <Grid {...field({ xs: 12 })}>
          <Stack direction="row" flexWrap="wrap" spacing={2}>
            <FormControlLabel control={
              <Controller name="reservable" control={control}
                          render={({ field: f }) => <Checkbox checked={f.value} onChange={f.onChange} />} />}
              label="Can be reserved" />
            <FormControlLabel control={
              <Controller name="approvalRequired" control={control}
                          render={({ field: f }) => <Checkbox checked={f.value} onChange={f.onChange} />} />}
              label="Reservation requires approval" />
            <FormControlLabel control={
              <Controller name="externalUseAllowed" control={control}
                          render={({ field: f }) => <Checkbox checked={f.value} onChange={f.onChange} />} />}
              label="Can be taken outside campus" />
            <FormControlLabel control={
              <Controller name="depositRequired" control={control}
                          render={({ field: f }) => <Checkbox checked={f.value} onChange={f.onChange} />} />}
              label="Deposit required" />
          </Stack>
        </Grid>
        {depositRequired && (
          <Grid {...field({ xs: 6, sm: 3 })}>
            <TextField label="Deposit amount" type="number" fullWidth required
                       inputProps={{ min: 0.01, step: '0.01' }}
                       error={!!errors.depositAmount} helperText={errors.depositAmount?.message}
                       {...register('depositAmount', {
                         validate: (v) => !depositRequired || v !== '' || 'Deposit amount is required',
                       })} />
          </Grid>
        )}
      </Section>

      <Section title="Warranty and maintenance">
        <Grid {...field()}>
          <TextField label="Warranty start" type="date" fullWidth InputLabelProps={{ shrink: true }}
                     {...register('warrantyStartDate')} />
        </Grid>
        <Grid {...field()}>
          <TextField label="Warranty end" type="date" fullWidth InputLabelProps={{ shrink: true }}
                     {...register('warrantyEndDate')} />
        </Grid>
        <Grid {...field()}>
          <TextField label="Warranty provider" fullWidth {...register('warrantyProvider')} />
        </Grid>
        <Grid {...field({ xs: 6, sm: 3 })}>
          <TextField label="Service interval (months)" type="number" fullWidth inputProps={{ min: 0 }}
                     {...register('serviceIntervalMonths')} />
        </Grid>
        <Grid {...field()}>
          <TextField label="Last service date" type="date" fullWidth InputLabelProps={{ shrink: true }}
                     {...register('lastServiceDate')} />
        </Grid>
        <Grid {...field()}>
          <TextField label="Next service date" type="date" fullWidth InputLabelProps={{ shrink: true }}
                     {...register('nextServiceDate')} />
        </Grid>
        <Grid {...field({ xs: 12 })}>
          <FormControlLabel control={
            <Controller name="calibrationRequired" control={control}
                        render={({ field: f }) => <Checkbox checked={f.value} onChange={f.onChange} />} />}
            label="Calibration required" />
        </Grid>
        {calibrationRequired && (
          <>
            <Grid {...field({ xs: 6, sm: 3 })}>
              <TextField label="Calibration interval (months)" type="number" fullWidth inputProps={{ min: 0 }}
                         {...register('calibrationIntervalMonths')} />
            </Grid>
            <Grid {...field()}>
              <TextField label="Last calibration" type="date" fullWidth InputLabelProps={{ shrink: true }}
                         {...register('lastCalibrationDate')} />
            </Grid>
            <Grid {...field()}>
              <TextField label="Next calibration" type="date" fullWidth InputLabelProps={{ shrink: true }}
                         {...register('nextCalibrationDate')} />
            </Grid>
          </>
        )}
      </Section>

      <Stack direction="row" spacing={1.5} justifyContent="flex-end">
        <Button color="inherit" onClick={() => navigate(-1)}>Cancel</Button>
        <Button type="submit" variant="contained" size="large"
                disabled={isSubmitting || mutation.isPending}>
          {isEdit ? 'Save changes' : 'Save asset'}
        </Button>
      </Stack>
      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1, textAlign: 'right' }}>
        Documents (invoices, manuals, certificates) can be attached from the asset page after saving.
      </Typography>
    </Box>
  );
}
