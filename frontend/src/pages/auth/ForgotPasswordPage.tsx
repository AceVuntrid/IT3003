import { useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { Alert, Box, Button, Link, Stack, TextField } from '@mui/material';
import { useForm } from 'react-hook-form';
import { api, errorMessage } from '../../api/client';
import AuthShell from './AuthShell';

export default function ForgotPasswordPage() {
  const [sent, setSent] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<{ email: string }>();

  const onSubmit = handleSubmit(async ({ email }) => {
    setError(null);
    try {
      await api.post('/auth/forgot-password', { email });
      setSent(true);
    } catch (e) {
      setError(errorMessage(e));
    }
  });

  return (
    <AuthShell
      title="Reset your password"
      subtitle="Enter your email and we'll send a link to choose a new password."
    >
      <Box component="form" onSubmit={onSubmit} noValidate>
        <Stack spacing={2.5}>
          {sent && (
            <Alert severity="success">
              If the email exists, a reset link has been sent. Check your inbox.
            </Alert>
          )}
          {error && <Alert severity="error">{error}</Alert>}
          <TextField
            label="Email address"
            type="email"
            autoComplete="email"
            autoFocus
            fullWidth
            error={!!errors.email}
            helperText={errors.email?.message}
            {...register('email', { required: 'Email is required' })}
          />
          <Button type="submit" variant="contained" size="large" disabled={isSubmitting} fullWidth>
            Send reset link
          </Button>
          <Link component={RouterLink} to="/login" variant="body2" textAlign="center">
            Back to sign in
          </Link>
        </Stack>
      </Box>
    </AuthShell>
  );
}
