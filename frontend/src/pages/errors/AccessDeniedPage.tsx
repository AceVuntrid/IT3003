import { Button } from '@mui/material';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import { useNavigate } from 'react-router-dom';
import EmptyState from '../../components/common/EmptyState';

export default function AccessDeniedPage() {
  const navigate = useNavigate();
  return (
    <EmptyState
      icon={<LockOutlinedIcon />}
      title="You don't have access to this page"
      hint="Your role doesn't include permission for this area. If you believe you need it, contact your faculty administrator."
      action={<Button variant="contained" onClick={() => navigate('/')}>Go to dashboard</Button>}
    />
  );
}
