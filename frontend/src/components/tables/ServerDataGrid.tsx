import { DataGrid } from '@mui/x-data-grid';
import type { GridColDef, GridPaginationModel, GridSortModel, GridValidRowModel } from '@mui/x-data-grid';
import { Box } from '@mui/material';
import type { Page } from '../../api/client';
import EmptyState from '../common/EmptyState';

interface ServerDataGridProps<T extends GridValidRowModel> {
  columns: GridColDef<T>[];
  page?: Page<T>;
  loading: boolean;
  paginationModel: GridPaginationModel;
  onPaginationModelChange: (model: GridPaginationModel) => void;
  sortModel?: GridSortModel;
  onSortModelChange?: (model: GridSortModel) => void;
  onRowClick?: (row: T) => void;
  emptyTitle?: string;
  emptyHint?: string;
}

/** Server-paginated table with consistent styling, loading and empty states. */
export default function ServerDataGrid<T extends GridValidRowModel>({
  columns, page, loading, paginationModel, onPaginationModelChange,
  sortModel, onSortModelChange, onRowClick, emptyTitle = 'Nothing here yet', emptyHint,
}: ServerDataGridProps<T>) {
  return (
    <Box sx={{ width: '100%' }}>
      <DataGrid<T>
        autoHeight
        rows={page?.content ?? []}
        columns={columns}
        rowCount={page?.totalElements ?? 0}
        loading={loading}
        paginationMode="server"
        sortingMode="server"
        paginationModel={paginationModel}
        onPaginationModelChange={onPaginationModelChange}
        sortModel={sortModel}
        onSortModelChange={onSortModelChange}
        pageSizeOptions={[10, 20, 50]}
        disableRowSelectionOnClick
        disableColumnMenu
        onRowClick={onRowClick ? (params) => onRowClick(params.row) : undefined}
        slots={{
          noRowsOverlay: () => <EmptyState title={emptyTitle} hint={emptyHint} />,
        }}
        sx={{
          border: '1px solid',
          borderColor: 'divider',
          borderRadius: 2.5,
          backgroundColor: 'background.paper',
          '& .MuiDataGrid-columnHeaders': {
            backgroundColor: '#FAFCFB',
          },
          '& .MuiDataGrid-columnHeaderTitle': {
            fontWeight: 600,
            color: 'text.secondary',
            fontSize: '0.8rem',
          },
          '& .MuiDataGrid-row': onRowClick ? { cursor: 'pointer' } : undefined,
          '& .MuiDataGrid-cell:focus, & .MuiDataGrid-columnHeader:focus': {
            outline: 'none',
          },
          '--DataGrid-overlayHeight': '260px',
        }}
      />
    </Box>
  );
}
