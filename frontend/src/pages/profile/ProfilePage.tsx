import { Box, Button, Card, CardContent, Chip, Grid, Stack, TextField, Typography } from '@mui/material';
import { useForm } from 'react-hook-form';
import { useMutation } from '@tanstack/react-query';
import { useSnackbar } from 'notistack';
import { api, errorMessage } from '../../api/client';
import { useAuth } from '../../auth/AuthContext';
import PageHeader from '../../components/common/PageHeader';
import CodeTag from '../../components/common/CodeTag';

const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/;

interface PasswordValues {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export default function ProfilePage() {
  const { user } = useAuth();
  const { enqueueSnackbar } = useSnackbar();
  const { register, handleSubmit, watch, reset, formState: { errors } } = useForm<PasswordValues>({
    defaultValues: { currentPassword: '', newPassword: '', confirmPassword: '' },
  });

  const mutation = useMutation({
    mutationFn: async (values: PasswordValues) =>
      api.put('/auth/change-password', {
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
      }),
    onSuccess: () => {
      enqueueSnackbar('Password changed. Other sessions have been signed out.', { variant: 'success' });
      reset();
    },
    onError: (error) => enqueueSnackbar(errorMessage(error), { variant: 'error' }),
  });

  return (
    <Box>
      <PageHeader eyebrow="ACCOUNT" title="My profile" crumbs={[{ label: 'My profile' }]} />
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 6 }}>
          <Card variant="outlined" sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>Details</Typography>
              <Stack spacing={1.5}>
                <Box>
                  <Typography variant="caption" color="text.secondary">Name</Typography>
                  <Typography>{user?.firstName} {user?.lastName}</Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">University ID</Typography>
                  <Box><CodeTag>{user?.universityId}</CodeTag></Box>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">Email</Typography>
                  <Typography>{user?.email}</Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">Faculty / Department</Typography>
                  <Typography>
                    {user?.facultyName ?? '—'}{user?.departmentName ? ` · ${user.departmentName}` : ''}
                  </Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary" display="block">Roles</Typography>
                  <Stack direction="row" spacing={1} flexWrap="wrap">
                    {(user?.roles ?? []).map((role) => <Chip key={role} label={role} size="small" />)}
                  </Stack>
                </Box>
              </Stack>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, md: 6 }}>
          <Card variant="outlined" sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>Change password</Typography>
              <Box component="form" noValidate
                   onSubmit={handleSubmit((values) => mutation.mutate(values))}>
                <Stack spacing={2}>
                  <TextField label="Current password" type="password" autoComplete="current-password"
                             error={!!errors.currentPassword} helperText={errors.currentPassword?.message}
                             {...register('currentPassword', { required: 'Current password is required' })} />
                  <TextField label="New password" type="password" autoComplete="new-password"
                             error={!!errors.newPassword}
                             helperText={errors.newPassword?.message
                               ?? '8+ characters with uppercase, lowercase, number and special character.'}
                             {...register('newPassword', {
                               required: 'New password is required',
                               pattern: {
                                 value: PASSWORD_PATTERN,
                                 message: 'Must contain uppercase, lowercase, a number and a special character',
                               },
                             })} />
                  <TextField label="Confirm new password" type="password" autoComplete="new-password"
                             error={!!errors.confirmPassword} helperText={errors.confirmPassword?.message}
                             {...register('confirmPassword', {
                               validate: (value) => value === watch('newPassword') || 'Passwords must match',
                             })} />
                  <Button type="submit" variant="contained" disabled={mutation.isPending}>
                    Change password
                  </Button>
                </Stack>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
}
