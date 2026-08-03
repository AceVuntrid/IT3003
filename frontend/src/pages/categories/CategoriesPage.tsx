import { useState } from 'react';
import {
  Box, Button, Card, CardContent, Dialog, DialogActions, DialogContent,
  DialogTitle, FormControl, FormControlLabel, IconButton, InputLabel,
  MenuItem, Select, Switch, Table, TableBody, TableCell, TableHead,
  TableRow, TextField, Typography
} from '@mui/material';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import AddIcon from '@mui/icons-material/Add';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSnackbar } from 'notistack';
import { api, errorMessage } from '../../api/client';
import type { Category } from '../../api/types';
import PageHeader from '../../components/common/PageHeader';
import CodeTag from '../../components/common/CodeTag';
import EmptyState from '../../components/common/EmptyState';

function CategoryDialog({ open, onClose, category, onSuccess }: any) {
  const [code, setCode] = useState(category?.code || '');
  const [name, setName] = useState(category?.name || '');
  const [assetType, setAssetType] = useState(category?.assetType || 'ASSET');
  const [description, setDescription] = useState(category?.description || '');
  const [active, setActive] = useState(category ? category.active : true);

  const isEdit = !!category;
  const { enqueueSnackbar } = useSnackbar();

  const mutation = useMutation({
    mutationFn: async () => {
      const payload = { code, name, assetType, description, active };
      if (isEdit) {
        return (await api.put(`/categories/${category.id}`, payload)).data;
      } else {
        return (await api.post('/categories', payload)).data;
      }
    },
    onSuccess: () => {
      enqueueSnackbar(`Category ${isEdit ? 'updated' : 'created'} successfully`, { variant: 'success' });
      onSuccess();
      onClose();
    },
    onError: (err) => enqueueSnackbar(errorMessage(err), { variant: 'error' })
  });

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>{isEdit ? 'Edit Category' : 'New Category'}</DialogTitle>
      <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 1 }}>
        <TextField
          label="Code"
          value={code}
          onChange={(e) => setCode(e.target.value)}
          fullWidth
          required
          autoFocus
          margin="dense"
        />
        <TextField
          label="Name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          fullWidth
          required
        />
        <FormControl fullWidth>
          <InputLabel>Type</InputLabel>
          <Select
            value={assetType}
            label="Type"
            onChange={(e) => setAssetType(e.target.value)}
          >
            <MenuItem value="ASSET">Asset</MenuItem>
            <MenuItem value="CONSUMABLE">Consumable</MenuItem>
          </Select>
        </FormControl>
        <TextField
          label="Description"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          fullWidth
          multiline
          rows={3}
        />
        <FormControlLabel
          control={<Switch checked={active} onChange={(e) => setActive(e.target.checked)} />}
          label="Active"
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button
          onClick={() => mutation.mutate()}
          variant="contained"
          disabled={!code || !name || mutation.isPending}
        >
          Save
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export default function CategoriesPage() {
  const [dialogOpen, setDialogOpen] = useState(false);
  const [selectedCategory, setSelectedCategory] = useState<Category | null>(null);
  
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();

  const { data, isLoading } = useQuery({
    queryKey: ['categories'],
    queryFn: async () => (await api.get('/categories')).data.data as Category[],
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => {
      return (await api.delete(`/categories/${id}`)).data;
    },
    onSuccess: () => {
      enqueueSnackbar('Category deleted successfully', { variant: 'success' });
      queryClient.invalidateQueries({ queryKey: ['categories'] });
    },
    onError: (err) => enqueueSnackbar(errorMessage(err), { variant: 'error' })
  });

  const handleEdit = (category: Category) => {
    setSelectedCategory(category);
    setDialogOpen(true);
  };

  const handleCreate = () => {
    setSelectedCategory(null);
    setDialogOpen(true);
  };

  const handleDelete = (id: string) => {
    if (confirm('Are you sure you want to delete this category?')) {
      deleteMutation.mutate(id);
    }
  };

  return (
    <>
      <PageHeader
        eyebrow="ADMINISTRATION"
        title="Categories"
        crumbs={[{ label: 'Categories' }]}
        subtitle="Manage categories for assets and consumables."
        actions={
          <Button variant="contained" startIcon={<AddIcon />} onClick={handleCreate}>
            New Category
          </Button>
        }
      />
      
      <Card variant="outlined">
        {!isLoading && (!data || data.length === 0) ? (
          <EmptyState
            title="No categories found"
            hint="Create a category to get started."
            action={<Button variant="outlined" onClick={handleCreate}>New Category</Button>}
          />
        ) : (
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Code</TableCell>
                <TableCell>Name</TableCell>
                <TableCell>Type</TableCell>
                <TableCell>Description</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(data || []).map((cat) => (
                <TableRow key={cat.id} hover>
                  <TableCell><CodeTag>{cat.code}</CodeTag></TableCell>
                  <TableCell>
                    <Typography variant="body2" fontWeight={600}>{cat.name}</Typography>
                  </TableCell>
                  <TableCell>{cat.assetType === 'ASSET' ? 'Asset' : 'Consumable'}</TableCell>
                  <TableCell sx={{ maxWidth: 300, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {cat.description || '-'}
                  </TableCell>
                  <TableCell>
                    {cat.active ? 'Active' : 'Inactive'}
                  </TableCell>
                  <TableCell align="right">
                    <IconButton size="small" onClick={() => handleEdit(cat)}>
                      <EditOutlinedIcon fontSize="small" />
                    </IconButton>
                    <IconButton size="small" color="error" onClick={() => handleDelete(cat.id)}>
                      <DeleteOutlineIcon fontSize="small" />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </Card>

      {dialogOpen && (
        <CategoryDialog
          open={dialogOpen}
          category={selectedCategory}
          onClose={() => setDialogOpen(false)}
          onSuccess={() => queryClient.invalidateQueries({ queryKey: ['categories'] })}
        />
      )}
    </>
  );
}
