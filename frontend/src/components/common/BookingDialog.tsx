import { useEffect, useState } from 'react';
import {
  Alert, Autocomplete, Button, Dialog, DialogActions, DialogContent, DialogTitle,
  Grid, TextField, ToggleButton, ToggleButtonGroup, Typography,
} from '@mui/material';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Controller, useForm } from 'react-hook-form';
import { useSnackbar } from 'notistack';
import { api, errorMessage } from '../../api/client';
import type { ApiEnvelope, Page } from '../../api/client';
import type {
  AssetDetail, AssetSummary, Availability, ConsumableSummary, Location, Reservation,
} from '../../api/types';
import { useConsumableOptions } from '../../api/referenceData';
import { localInputToIso, localInputValue } from '../../utils/format';

export type BookingType = 'equipment' | 'venue' | 'consumable';

interface BookingValues {
  itemId: string;
  startAt: string;
  endAt: string;
  quantity: string;
  /** Expected attendees — venue bookings only (optional). */
  participantCount: string;
  purpose: string;
}

const defaults = (itemId = ''): BookingValues => ({
  itemId, startAt: localInputValue(24), endAt: localInputValue(28), quantity: '1',
  participantCount: '', purpose: '',
});

/** Asset statuses the reservation service rejects outright — hide them from the picker. */
const BLOCKED_ASSET_STATUSES = ['UNDER_MAINTENANCE', 'DAMAGED', 'LOST', 'DISPOSED'];

/**
 * Unified booking dialog for equipment, venue and consumable requests.
 * Submits POST /reservations with exactly one of assetId / locationId /
 * consumableItemId — every request goes through the normal approval flow.
 *
 * Mounted by the Reservations page "New reservation" button, the Shift+R
 * quick action (RoleQuickActions), the Venues page "Book" button
 * (pre-filled venue) and the consumable detail "Reserve" button
 * (pre-filled item).
 */
export default function BookingDialog({ open, onClose, initialType, initialItemId, initialAsset }: {
  open: boolean;
  onClose: () => void;
  /** Pre-select a booking type, e.g. 'venue' from the Venues page. */
  initialType?: BookingType;
  /** Pre-select an item of the initial type (venue or consumable id). */
  initialItemId?: string;
  /** Pre-select an asset summary/detail object when reserving from assets inventory. */
  initialAsset?: AssetSummary | AssetDetail | null;
}) {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();
  const [type, setType] = useState<BookingType>(initialType ?? 'equipment');
  // The asset list is server-searched (inventories can exceed one page), so the
  // selected option is kept here — it may not be in the current search results.
  const [assetOption, setAssetOption] = useState<AssetSummary | null>(null);
  const [assetSearch, setAssetSearch] = useState('');
  const { register, control, handleSubmit, reset, watch, setValue, formState: { errors } } =
    useForm<BookingValues>({ defaultValues: defaults() });

  useEffect(() => {
    if (open) {
      setType(initialType ?? 'equipment');
      if (initialAsset) {
        setAssetOption({
          id: initialAsset.id,
          assetCode: initialAsset.assetCode,
          name: initialAsset.name,
          assetType: initialAsset.assetType ?? '',
          categoryName: initialAsset.categoryName ?? '',
          facultyName: initialAsset.facultyName ?? '',
          locationName: initialAsset.locationName ?? '',
          condition: initialAsset.condition ?? '',
          status: initialAsset.status ?? '',
          quantity: initialAsset.quantity ?? 1,
          availableQuantity: initialAsset.availableQuantity ?? 1,
          reservable: true,
          currency: initialAsset.currency ?? 'LKR',
          archived: false,
        });
      } else {
        setAssetOption(null);
      }
      setAssetSearch('');
      reset(defaults(initialItemId ?? initialAsset?.id ?? ''));
    }
  }, [open, initialType, initialItemId, initialAsset, reset]);

  // Only bookable equipment: reservable (server-filtered) and not in a state
  // the reservation service would reject anyway.
  const assetsQuery = useQuery({
    queryKey: ['reservable-assets', assetSearch],
    enabled: open && type === 'equipment',
    placeholderData: keepPreviousData,
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<AssetSummary>>>('/assets', {
        params: { size: 100, sort: 'name,asc', reservable: true, search: assetSearch || undefined },
      })).data.data.content
        .filter((a) => !a.archived && !BLOCKED_ASSET_STATUSES.includes(a.status)),
  });

  const venuesQuery = useQuery({
    queryKey: ['venue-options'],
    enabled: open && type === 'venue',
    queryFn: async () =>
      (await api.get<ApiEnvelope<Location[]>>('/locations', {
        params: { venuesOnly: true },
      })).data.data,
  });

  const consumablesQuery = useConsumableOptions(open && type === 'consumable');

  const itemId = watch('itemId');
  const startAt = watch('startAt');
  const endAt = watch('endAt');
  const quantity = watch('quantity');

  // Live availability preview exists only for equipment — the endpoint is
  // asset-based. Venue clashes and consumable stock are validated server-side
  // at submit.
  const availabilityQuery = useQuery({
    queryKey: ['availability', itemId, startAt, endAt, quantity],
    enabled: open && type === 'equipment' && !!itemId && !!startAt && !!endAt,
    queryFn: async () =>
      (await api.get<ApiEnvelope<Availability>>('/reservations/availability', {
        params: {
          assetId: itemId,
          startAt: localInputToIso(startAt),
          endAt: localInputToIso(endAt),
          quantity: Number(quantity) || 1,
        },
      })).data.data,
  });

  const mutation = useMutation({
    mutationFn: async (values: BookingValues) =>
      (await api.post<ApiEnvelope<Reservation>>('/reservations', {
        assetId: type === 'equipment' ? values.itemId : undefined,
        locationId: type === 'venue' ? values.itemId : undefined,
        consumableItemId: type === 'consumable' ? values.itemId : undefined,
        purpose: values.purpose,
        startAt: localInputToIso(values.startAt),
        endAt: localInputToIso(values.endAt),
        quantity: type === 'venue' ? 1 : Number(values.quantity) || 1,
        participantCount: type === 'venue' && values.participantCount
          ? Number(values.participantCount) : undefined,
      })).data.data,
    onSuccess: (reservation) => {
      queryClient.invalidateQueries({ queryKey: ['reservations'] });
      enqueueSnackbar(`Reservation ${reservation.reservationNumber} submitted`, { variant: 'success' });
      onClose();
    },
    onError: (error) => enqueueSnackbar(errorMessage(error), { variant: 'error' }),
  });

  const assets = assetsQuery.data ?? [];
  const venues = venuesQuery.data ?? [];
  const consumables = consumablesQuery.data ?? [];
  const availability = availabilityQuery.data;
  const selectedConsumable = type === 'consumable'
    ? consumables.find((c) => c.id === itemId)
    : undefined;
  const selectedVenue = type === 'venue' ? venues.find((v) => v.id === itemId) : undefined;
  // Option loads can fail (e.g. a booker without CONSUMABLE_VIEW gets a 403 on
  // /consumables) — surface it instead of showing a silently empty picker.
  const optionsError = type === 'equipment' ? assetsQuery.error
    : type === 'venue' ? venuesQuery.error
    : consumablesQuery.error;

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>New booking</DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
          Requests go through the normal approval flow. Any applicable fee comes
          from the price list at approval.
        </Typography>
        <ToggleButtonGroup
          exclusive size="small" color="primary" value={type} sx={{ mb: 2 }}
          onChange={(_, next: BookingType | null) => {
            if (next && next !== type) {
              setType(next);
              setAssetOption(null);
              setAssetSearch('');
              setValue('itemId', '');
            }
          }}
        >
          <ToggleButton value="equipment">Equipment</ToggleButton>
          <ToggleButton value="venue">Venue</ToggleButton>
          <ToggleButton value="consumable">Consumable</ToggleButton>
        </ToggleButtonGroup>
        <Grid container spacing={2}>
          <Grid size={{ xs: 12 }}>
            <Controller name="itemId" control={control} rules={{ required: 'Select an item' }}
                        render={({ field, fieldState }) => (
              type === 'equipment' ? (
                <Autocomplete
                  options={assets}
                  loading={assetsQuery.isLoading}
                  value={assetOption ?? assets.find((a) => a.id === field.value) ?? null}
                  onChange={(_, option) => {
                    setAssetOption(option);
                    field.onChange(option?.id ?? '');
                  }}
                  onInputChange={(_, value, reason) =>
                    setAssetSearch(reason === 'input' ? value : '')}
                  // Options are already server-filtered by the search text.
                  filterOptions={(options) => options}
                  getOptionLabel={(a) => `${a.name} — ${a.assetCode} (${a.locationName})`}
                  isOptionEqualToValue={(a, b) => a.id === b.id}
                  renderInput={(params) => (
                    <TextField {...params} label="Asset" required autoFocus
                               error={!!fieldState.error} helperText={fieldState.error?.message} />
                  )}
                />
              ) : type === 'venue' ? (
                <Autocomplete
                  options={venues}
                  loading={venuesQuery.isLoading}
                  value={venues.find((v) => v.id === field.value) ?? null}
                  onChange={(_, option) => field.onChange(option?.id ?? '')}
                  getOptionLabel={(v) =>
                    `${v.name} — ${v.code}${v.capacity ? ` (seats ${v.capacity})` : ''}`}
                  isOptionEqualToValue={(a, b) => a.id === b.id}
                  renderInput={(params) => (
                    <TextField {...params} label="Venue" required autoFocus
                               error={!!fieldState.error} helperText={fieldState.error?.message} />
                  )}
                />
              ) : (
                <Autocomplete
                  options={consumables}
                  loading={consumablesQuery.isLoading}
                  value={consumables.find((c) => c.id === field.value) ?? null}
                  onChange={(_, option) => field.onChange(option?.id ?? '')}
                  getOptionLabel={(c) =>
                    `${c.name} — ${c.itemCode} (${c.availableQuantity} ${c.unitOfMeasure} available)`}
                  isOptionEqualToValue={(a, b) => a.id === b.id}
                  renderInput={(params) => (
                    <TextField {...params} label="Consumable item" required autoFocus
                               error={!!fieldState.error} helperText={fieldState.error?.message} />
                  )}
                />
              )
            )} />
          </Grid>
          {optionsError != null && (
            <Grid size={{ xs: 12 }}>
              <Alert severity="warning">{errorMessage(optionsError)}</Alert>
            </Grid>
          )}
          <Grid size={{ xs: 6 }}>
            <TextField label={type === 'consumable' ? 'Collect from' : 'Start'}
                       type="datetime-local" fullWidth required
                       InputLabelProps={{ shrink: true }}
                       error={!!errors.startAt} helperText={errors.startAt?.message}
                       {...register('startAt', { required: 'Start is required' })} />
          </Grid>
          <Grid size={{ xs: 6 }}>
            <TextField label={type === 'consumable' ? 'Collect by' : 'End'}
                       type="datetime-local" fullWidth required
                       InputLabelProps={{ shrink: true }}
                       error={!!errors.endAt} helperText={errors.endAt?.message}
                       {...register('endAt', {
                         required: 'End is required',
                         validate: (value) => !startAt || value > startAt || 'End must be after start',
                       })} />
          </Grid>
          {/* Per-type fields: quantity applies to equipment and consumables only;
              venues are booked whole, with an optional expected head-count. */}
          {type !== 'venue' && (
            <Grid size={{ xs: 6 }}>
              <TextField label="Quantity" type="number" fullWidth inputProps={{ min: 1 }}
                         helperText={selectedConsumable
                           ? `In ${selectedConsumable.unitOfMeasure}`
                           : undefined}
                         {...register('quantity')} />
            </Grid>
          )}
          {type === 'venue' && (
            <Grid size={{ xs: 6 }}>
              <TextField label="Expected attendees" type="number" fullWidth
                         inputProps={{ min: 1 }}
                         error={!!errors.participantCount}
                         helperText={errors.participantCount?.message
                           ?? (selectedVenue?.capacity
                             ? `Venue seats ${selectedVenue.capacity}`
                             : undefined)}
                         {...register('participantCount', {
                           validate: (value) => {
                             if (!value) return true; // optional
                             const n = Number(value);
                             return (Number.isInteger(n) && n >= 1)
                               || 'Enter a whole number of attendees';
                           },
                         })} />
            </Grid>
          )}
          <Grid size={{ xs: 12 }}>
            <TextField label="Purpose" fullWidth required
                       error={!!errors.purpose} helperText={errors.purpose?.message}
                       {...register('purpose', { required: 'Purpose is required' })} />
          </Grid>
          {type === 'consumable' && (
            <Grid size={{ xs: 12 }}>
              <Alert severity="info">
                Consumables are issued from stock and not returned — the window
                above is when you plan to collect them.
              </Alert>
            </Grid>
          )}
          {type === 'equipment' && availability && (
            <Grid size={{ xs: 12 }}>
              {availability.available ? (
                <Alert severity="success">
                  Available: {availability.availableInWindow} of {availability.totalQuantity} unit(s)
                  free in this window.
                </Alert>
              ) : (
                <Alert severity="warning">
                  {availability.blockers.join('. ') || 'Not available in this window.'}
                </Alert>
              )}
            </Grid>
          )}
        </Grid>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button color="inherit" onClick={onClose}>Cancel</Button>
        <Button variant="contained"
                disabled={mutation.isPending
                  || (type === 'equipment' && !!availability && !availability.available)}
                onClick={handleSubmit((values) => mutation.mutate(values))}>
          Submit request
        </Button>
      </DialogActions>
    </Dialog>
  );
}
