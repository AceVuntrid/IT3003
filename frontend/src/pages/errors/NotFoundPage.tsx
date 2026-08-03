import { Button } from '@mui/material';
import SearchOffIcon from '@mui/icons-material/SearchOff';
import { useNavigate } from 'react-router-dom';
import EmptyState from '../../components/common/EmptyState';

export default function NotFoundPage() {
  const navigate = useNavigate();
  return (
    <EmptyState
      icon={<SearchOffIcon />}
      title="Page not found"
      hint="The page you're looking for doesn't exist or has moved."
      action={<Button variant="contained" onClick={() => navigate('/')}>Go to dashboard</Button>}
    />
  );
}
