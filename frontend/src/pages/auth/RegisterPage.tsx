import { useState } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  IconButton,
  InputAdornment,
  Link,
  MenuItem,
  Stack,
  TextField,
} from '@mui/material';
import Visibility from '@mui/icons-material/Visibility';
import VisibilityOff from '@mui/icons-material/VisibilityOff';
import { useForm } from 'react-hook-form';
import { api, errorMessage } from '../../api/client';
import { useAuth } from '../../auth/AuthContext';
import { useSnackbar } from 'notistack';
import AuthShell from './AuthShell';

interface RegisterForm {
  firstName: string;
  lastName: string;
  email: string;
  studentIndex: string;
  course: string;
  password: string;
  confirmPassword: string;
}

const COURSE_OPTIONS = [
  'Physical Science',
  'Biological Science',
  'ISMF',
  'Molecular Biology',
  'MIT',
];

const UNIVERSITY_EMAIL_PATTERN = /^\d{4}s20\d{2,5}@stu\.cmb\.ac\.lk$/i;
const STUDENT_INDEX_PATTERN = /^S\d{5}$/;
const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/;

export default function RegisterPage() {
  const navigate = useNavigate();
  const { login, user } = useAuth();
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { register, handleSubmit, watch, formState: { errors, isSubmitting } } = useForm<RegisterForm>({
    defaultValues: {
      firstName: '',
      lastName: '',
      email: '',
      studentIndex: '',
      course: '',
      password: '',
      confirmPassword: '',
    },
  });

  if (user) {
    navigate('/', { replace: true });
  }

  const { enqueueSnackbar } = useSnackbar();

  const onSubmit = handleSubmit(async (values) => {
    setError(null);
    try {
      await api.post('/auth/register/student', {
        firstName: values.firstName.trim(),
        lastName: values.lastName.trim(),
        email: values.email.trim().toLowerCase(),
        studentIndex: values.studentIndex.trim().toUpperCase(),
        course: values.course.trim(),
        password: values.password,
        confirmPassword: values.confirmPassword,
      });
      enqueueSnackbar('Account created successfully', { variant: 'success' });
      await login(values.email.trim().toLowerCase(), values.password, true);
      navigate('/', { replace: true });
    } catch (e) {
      setError(errorMessage(e));
    }
  });

  return (
    <AuthShell title="Create student account" subtitle="Register with your university email, index number and course to access the portal.">
      <Box component="form" onSubmit={onSubmit} noValidate>
        <Stack spacing={2.5}>
          {error && <Alert severity="error">{error}</Alert>}
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
            <TextField
              label="First name"
              fullWidth
              autoFocus
              error={!!errors.firstName}
              helperText={errors.firstName?.message}
              {...register('firstName', { required: 'First name is required' })}
            />
            <TextField
              label="Last name"
              fullWidth
              error={!!errors.lastName}
              helperText={errors.lastName?.message}
              {...register('lastName', { required: 'Last name is required' })}
            />
          </Stack>
          <TextField
            label="University email"
            type="email"
            fullWidth
            placeholder="2023s20133@stu.cmb.ac.lk"
            error={!!errors.email}
            helperText={errors.email?.message ?? 'Use the university email format shown in the example'}
            {...register('email', {
              required: 'University email is required',
              pattern: {
                value: UNIVERSITY_EMAIL_PATTERN,
                message: 'Use the format 2023s20133@stu.cmb.ac.lk',
              },
            })}
          />
          <TextField
            label="Index number"
            fullWidth
            placeholder="S17312"
            error={!!errors.studentIndex}
            helperText={errors.studentIndex?.message ?? 'Index number must start with S and contain 5 digits'}
            {...register('studentIndex', {
              required: 'Index number is required',
              pattern: {
                value: STUDENT_INDEX_PATTERN,
                message: 'Index number must start with S followed by 5 digits',
              },
            })}
          />
          <TextField
            select
            label="Course"
            fullWidth
            error={!!errors.course}
            helperText={errors.course?.message}
            {...register('course', { required: 'Course is required' })}
          >
            <MenuItem value="">Select a course</MenuItem>
            {COURSE_OPTIONS.map((course) => (
              <MenuItem key={course} value={course}>
                {course}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            label="Password"
            type={showPassword ? 'text' : 'password'}
            fullWidth
            error={!!errors.password}
            helperText={errors.password?.message}
            {...register('password', {
              required: 'Password is required',
              pattern: {
                value: PASSWORD_PATTERN,
                message: 'Use at least 8 characters including uppercase, lowercase, a number and a symbol',
              },
            })}
            InputProps={{
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton aria-label="Toggle password visibility" onClick={() => setShowPassword((v) => !v)} edge="end">
                    {showPassword ? <VisibilityOff /> : <Visibility />}
                  </IconButton>
                </InputAdornment>
              ),
            }}
          />
          <TextField
            label="Confirm password"
            type={showConfirmPassword ? 'text' : 'password'}
            fullWidth
            error={!!errors.confirmPassword}
            helperText={errors.confirmPassword?.message}
            {...register('confirmPassword', {
              required: 'Please confirm your password',
              validate: (value) => value === watch('password') || 'Passwords do not match',
            })}
            InputProps={{
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton aria-label="Toggle confirm password visibility" onClick={() => setShowConfirmPassword((v) => !v)} edge="end">
                    {showConfirmPassword ? <VisibilityOff /> : <Visibility />}
                  </IconButton>
                </InputAdornment>
              ),
            }}
          />
          <Button type="submit" variant="contained" size="large" fullWidth disabled={isSubmitting}>
            {isSubmitting ? 'Creating account…' : 'Create account'}
          </Button>
          <Link component={RouterLink} to="/login" variant="body2" sx={{ textAlign: 'center' }}>
            Already have an account? Sign in
          </Link>
        </Stack>
      </Box>
    </AuthShell>
  );
}
