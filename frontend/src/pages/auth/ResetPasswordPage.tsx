import { useState } from 'react';
import { Link as RouterLink, useSearchParams } from 'react-router-dom';
import { Alert, Box, Button, Link, Stack, TextField } from '@mui/material';
import { useForm } from 'react-hook-form';
import { api, errorMessage } from '../../api/client';
import AuthShell from './AuthShell';

const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/;

interface ResetForm {
  newPassword: string;
  confirmPassword: string;
}

export default function ResetPasswordPage() {
  const [params] = useSearchParams();
  const token = params.get('token') ?? '';
  const [done, setDone] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { register, handleSubmit, watch, formState: { errors, isSubmitting } } = useForm<ResetForm>();

  const onSubmit = handleSubmit(async ({ newPassword }) => {
    setError(null);
    try {
      await api.post('/auth/reset-password', { token, newPassword });
      setDone(true);
    } catch (e) {
      setError(errorMessage(e));
    }
  });

  return (
    <AuthShell title="Choose a new password">
      <Box component="form" onSubmit={onSubmit} noValidate>
        <Stack spacing={2.5}>
          {!token && <Alert severity="warning">This link is missing its reset token. Request a new one.</Alert>}
          {done && (
            <Alert severity="success">
              Password has been reset. <Link component={RouterLink} to="/login">Sign in</Link>
            </Alert>
          )}
          {error && <Alert severity="error">{error}</Alert>}
          <TextField
            label="New password"
            type="password"
            autoComplete="new-password"
            fullWidth
            error={!!errors.newPassword}
            helperText={errors.newPassword?.message
              ?? 'At least 8 characters with uppercase, lowercase, a number and a special character.'}
            {...register('newPassword', {
              required: 'Password is required',
              pattern: {
                value: PASSWORD_PATTERN,
                message: 'Must contain uppercase, lowercase, a number and a special character (8+ chars)',
              },
            })}
          />
          <TextField
            label="Confirm password"
            type="password"
            autoComplete="new-password"
            fullWidth
            error={!!errors.confirmPassword}
            helperText={errors.confirmPassword?.message}
            {...register('confirmPassword', {
              validate: (value) => value === watch('newPassword') || 'Passwords must match',
            })}
          />
          <Button type="submit" variant="contained" size="large" disabled={isSubmitting || !token} fullWidth>
            Reset password
          </Button>
          <Link component={RouterLink} to="/login" variant="body2" textAlign="center">
            Back to sign in
          </Link>
        </Stack>
      </Box>
    </AuthShell>
  );
}
