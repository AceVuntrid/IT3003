import { useEffect } from 'react';
import {
  Button, Checkbox, Dialog, DialogActions, DialogContent, DialogTitle,
  FormControlLabel, Grid, MenuItem, TextField,
} from '@mui/material';
import { Controller, useForm } from 'react-hook-form';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useSnackbar } from 'notistack';
import { api, errorMessage } from '../../api/client';
import type { ConsumableDetail } from '../../api/types';
import { useCategories, useDepartments, useFaculties, useLocations } from '../../api/referenceData';

interface FormValues {
  name: string;
  itemCode: string;
  categoryId: string;
  description: string;
  brand: string;
  manufacturer: string;
  unitOfMeasure: string;
  facultyId: string;
  departmentId: string;
  locationId: string;
  reorderLevel: string;
  maximumStockLevel: string;
  unitCost: string;
  hazardous: boolean;
  chemicalClassification: string;
  storageInstructions: string;
  disposalInstructions: string;
}

const EMPTY: FormValues = {
  name: '', itemCode: '', categoryId: '', description: '', brand: '', manufacturer: '',
  unitOfMeasure: '', facultyId: '', departmentId: '', locationId: '',
  reorderLevel: '0', maximumStockLevel: '', unitCost: '', hazardous: false,
  chemicalClassification: '', storageInstructions: '', disposalInstructions: '',
};

export default function ConsumableFormDialog({ open, onClose, existing }: {
  open: boolean;
  onClose: () => void;
  existing?: ConsumableDetail;
}) {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();
  const { register, control, handleSubmit, reset, watch, formState: { errors } } =
    useForm<FormValues>({ defaultValues: EMPTY });

  const facultyId = watch('facultyId');
  const hazardous = watch('hazardous');
  const faculties = useFaculties();
  const departments = useDepartments(facultyId || undefined);
  const locations = useLocations();
  const categories = useCategories('CONSUMABLE');

  useEffect(() => {
    if (open) {
      reset(existing ? {
        ...EMPTY,
        name: existing.name,
        itemCode: existing.itemCode,
        categoryId: existing.categoryId,
        description: existing.description ?? '',
        brand: existing.brand ?? '',
        manufacturer: existing.manufacturer ?? '',
        unitOfMeasure: existing.unitOfMeasure,
        facultyId: existing.facultyId,
        departmentId: existing.departmentId ?? '',
        locationId: existing.locationId,
        reorderLevel: existing.reorderLevel.toString(),
        maximumStockLevel: existing.maximumStockLevel?.toString() ?? '',
        unitCost: existing.unitCost?.toString() ?? '',
        hazardous: existing.hazardous,
        chemicalClassification: existing.chemicalClassification ?? '',
        storageInstructions: existing.storageInstructions ?? '',
        disposalInstructions: existing.disposalInstructions ?? '',
      } : EMPTY);
    }
  }, [open, existing, reset]);

  const mutation = useMutation({
    mutationFn: async (values: FormValues) => {
      const opt = (v: string) => (v === '' ? null : v);
      const num = (v: string) => (v === '' ? null : Number(v));
      const payload = {
        name: values.name,
        itemCode: opt(values.itemCode),
        categoryId: values.categoryId,
        description: opt(values.description),
        brand: opt(values.brand),
        manufacturer: opt(values.manufacturer),
        unitOfMeasure: values.unitOfMeasure,
        facultyId: values.facultyId,
        departmentId: opt(values.departmentId),
        locationId: values.locationId,
        reorderLevel: num(values.reorderLevel),
        maximumStockLevel: num(values.maximumStockLevel),
        unitCost: num(values.unitCost),
        hazardous: values.hazardous,
        chemicalClassification: opt(values.chemicalClassification),
        storageInstructions: opt(values.storageInstructions),
        disposalInstructions: opt(values.disposalInstructions),
      };
      return existing
        ? (await api.put(`/consumables/${existing.id}`, payload)).data
        : (await api.post('/consumables', payload)).data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['consumables'] });
      queryClient.invalidateQueries({ queryKey: ['consumable'] });
      enqueueSnackbar(existing ? 'Consumable updated' : 'Consumable created', { variant: 'success' });
      onClose();
    },
    onError: (error) => enqueueSnackbar(errorMessage(error), { variant: 'error' }),
  });

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>{existing ? `Edit ${existing.itemCode}` : 'Add consumable'}</DialogTitle>
      <DialogContent>
        <Grid container spacing={2} sx={{ mt: 0.25 }}>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField label="Item name" fullWidth required
                       error={!!errors.name} helperText={errors.name?.message}
                       {...register('name', { required: 'Item name is required' })} />
          </Grid>
          <Grid size={{ xs: 6, sm: 3 }}>
            <TextField label="Item code" fullWidth placeholder="Auto" disabled={!!existing}
                       {...register('itemCode')} />
          </Grid>
          <Grid size={{ xs: 6, sm: 3 }}>
            <TextField label="Unit of measure" fullWidth required placeholder="L, box, pcs…"
                       error={!!errors.unitOfMeasure} helperText={errors.unitOfMeasure?.message}
                       {...register('unitOfMeasure', { required: 'Unit is required' })} />
          </Grid>
          <Grid size={{ xs: 12, sm: 4 }}>
            <Controller name="categoryId" control={control} rules={{ required: 'Category is required' }}
                        render={({ field, fieldState }) => (
              <TextField {...field} select label="Category" fullWidth required
                         error={!!fieldState.error} helperText={fieldState.error?.message}>
                {(categories.data ?? []).map((c) => <MenuItem key={c.id} value={c.id}>{c.name}</MenuItem>)}
              </TextField>
            )} />
          </Grid>
          <Grid size={{ xs: 6, sm: 4 }}>
            <Controller name="facultyId" control={control} rules={{ required: 'Faculty is required' }}
                        render={({ field, fieldState }) => (
              <TextField {...field} select label="Faculty" fullWidth required
                         error={!!fieldState.error} helperText={fieldState.error?.message}>
                {(faculties.data ?? []).map((f) => <MenuItem key={f.id} value={f.id}>{f.name}</MenuItem>)}
              </TextField>
            )} />
          </Grid>
          <Grid size={{ xs: 6, sm: 4 }}>
            <Controller name="departmentId" control={control} render={({ field }) => (
              <TextField {...field} select label="Department" fullWidth>
                <MenuItem value="">None</MenuItem>
                {(departments.data ?? []).map((d) => <MenuItem key={d.id} value={d.id}>{d.name}</MenuItem>)}
              </TextField>
            )} />
          </Grid>
          <Grid size={{ xs: 12, sm: 4 }}>
            <Controller name="locationId" control={control} rules={{ required: 'Store location is required' }}
                        render={({ field, fieldState }) => (
              <TextField {...field} select label="Store location" fullWidth required
                         error={!!fieldState.error} helperText={fieldState.error?.message}>
                {(locations.data ?? []).map((l) => <MenuItem key={l.id} value={l.id}>{l.name}</MenuItem>)}
              </TextField>
            )} />
          </Grid>
          <Grid size={{ xs: 4, sm: 2.5 }}>
            <TextField label="Reorder level" type="number" fullWidth inputProps={{ min: 0, step: '0.001' }}
                       {...register('reorderLevel')} />
          </Grid>
          <Grid size={{ xs: 4, sm: 2.5 }}>
            <TextField label="Max stock" type="number" fullWidth inputProps={{ min: 0, step: '0.001' }}
                       {...register('maximumStockLevel')} />
          </Grid>
          <Grid size={{ xs: 4, sm: 3 }}>
            <TextField label="Unit cost" type="number" fullWidth inputProps={{ min: 0, step: '0.01' }}
                       {...register('unitCost')} />
          </Grid>
          <Grid size={{ xs: 12, sm: 4 }}>
            <TextField label="Brand" fullWidth {...register('brand')} />
          </Grid>
          <Grid size={{ xs: 12, sm: 4 }}>
            <TextField label="Manufacturer" fullWidth {...register('manufacturer')} />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <TextField label="Description" fullWidth multiline minRows={2} {...register('description')} />
          </Grid>
          <Grid size={{ xs: 12 }}>
            <FormControlLabel control={
              <Controller name="hazardous" control={control}
                          render={({ field }) => <Checkbox checked={field.value} onChange={field.onChange} />} />}
              label="Hazardous item (chemical or dangerous goods)" />
          </Grid>
          {hazardous && (
            <>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField label="Chemical classification" fullWidth placeholder="e.g. Flammable liquid, Class 3"
                           {...register('chemicalClassification')} />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField label="Storage instructions" fullWidth {...register('storageInstructions')} />
              </Grid>
              <Grid size={{ xs: 12 }}>
                <TextField label="Disposal instructions" fullWidth {...register('disposalInstructions')} />
              </Grid>
            </>
          )}
        </Grid>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button color="inherit" onClick={onClose}>Cancel</Button>
        <Button variant="contained" disabled={mutation.isPending}
                onClick={handleSubmit((values) => mutation.mutate(values))}>
          {existing ? 'Save changes' : 'Create consumable'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
