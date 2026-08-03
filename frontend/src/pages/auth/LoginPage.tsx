import { useState } from 'react';
import { useLocation, useNavigate, Link as RouterLink } from 'react-router-dom';
import {
  Alert, Box, Button, Checkbox, FormControlLabel, IconButton, InputAdornment,
  Link, Stack, TextField,
} from '@mui/material';
import Visibility from '@mui/icons-material/Visibility';
import VisibilityOff from '@mui/icons-material/VisibilityOff';
import { useForm } from 'react-hook-form';
import { useAuth } from '../../auth/AuthContext';
import { errorMessage } from '../../api/client';
import AuthShell from './AuthShell';

interface LoginForm {
  email: string;
  password: string;
  rememberMe: boolean;
}

export default function LoginPage() {
  const { login, user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<LoginForm>({
    defaultValues: { email: '', password: '', rememberMe: true },
  });

  if (user) {
    navigate('/', { replace: true });
  }

  const onSubmit = handleSubmit(async (values) => {
    setError(null);
    try {
      await login(values.email, values.password, values.rememberMe);
      const from = (location.state as { from?: string } | null)?.from;
      navigate(from && from !== '/login' ? from : '/', { replace: true });
    } catch (e) {
      setError(errorMessage(e));
    }
  });

  return (
    <AuthShell title="Sign in" subtitle="Use your university account to continue.">
      <Box component="form" onSubmit={onSubmit} noValidate>
        <Stack spacing={2.5}>
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
          <TextField
            label="Password"
            type={showPassword ? 'text' : 'password'}
            autoComplete="current-password"
            fullWidth
            error={!!errors.password}
            helperText={errors.password?.message}
            {...register('password', { required: 'Password is required' })}
            InputProps={{
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                    onClick={() => setShowPassword((v) => !v)}
                    edge="end"
                  >
                    {showPassword ? <VisibilityOff /> : <Visibility />}
                  </IconButton>
                </InputAdornment>
              ),
            }}
          />
          <Stack direction="row" justifyContent="space-between" alignItems="center">
            <FormControlLabel
              control={<Checkbox defaultChecked {...register('rememberMe')} />}
              label="Remember me"
            />
            <Link component={RouterLink} to="/forgot-password" variant="body2">
              Forgot password?
            </Link>
          </Stack>
          <Button type="submit" variant="contained" size="large" disabled={isSubmitting} fullWidth>
            {isSubmitting ? 'Signing in…' : 'Sign in'}
          </Button>
          <Link component={RouterLink} to="/register" variant="body2" sx={{ textAlign: 'center' }}>
            New student? Create an account
          </Link>
        </Stack>
      </Box>
    </AuthShell>
  );
}
