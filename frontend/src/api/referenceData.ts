import { useQuery } from '@tanstack/react-query';
import { api } from './client';
import type { ApiEnvelope, Page } from './client';
import type {
  Category, ConsumableSummary, Department, Faculty, Location, Role, UserRow,
} from './types';

export function useFaculties() {
  return useQuery({
    queryKey: ['faculties'],
    queryFn: async () => (await api.get<ApiEnvelope<Faculty[]>>('/faculties')).data.data,
    staleTime: 5 * 60_000,
  });
}

export function useDepartments(facultyId?: string) {
  return useQuery({
    queryKey: ['departments', facultyId ?? 'all'],
    queryFn: async () =>
      (await api.get<ApiEnvelope<Department[]>>('/departments', {
        params: facultyId ? { facultyId } : undefined,
      })).data.data,
    staleTime: 5 * 60_000,
  });
}

/** Departments the current user may manage settings for (requires SETTINGS_MANAGE). */
export function useManageableDepartments() {
  return useQuery({
    queryKey: ['departments', 'manageable'],
    queryFn: async () =>
      (await api.get<ApiEnvelope<Department[]>>('/departments', {
        params: { scope: 'manageable' },
      })).data.data,
    staleTime: 5 * 60_000,
  });
}

export function useLocations() {
  return useQuery({
    queryKey: ['locations'],
    queryFn: async () => (await api.get<ApiEnvelope<Location[]>>('/locations')).data.data,
    staleTime: 5 * 60_000,
  });
}

export function useCategories(assetType?: string) {
  return useQuery({
    queryKey: ['categories', assetType ?? 'all'],
    queryFn: async () =>
      (await api.get<ApiEnvelope<Category[]>>('/categories', {
        params: assetType ? { assetType } : undefined,
      })).data.data,
    staleTime: 5 * 60_000,
  });
}

export function useRoles(enabled = true) {
  return useQuery({
    queryKey: ['roles'],
    queryFn: async () => (await api.get<ApiEnvelope<Role[]>>('/roles')).data.data,
    staleTime: 5 * 60_000,
    enabled,
  });
}

/** Active consumable items for pickers (e.g. the unified booking dialog). */
export function useConsumableOptions(enabled = true) {
  return useQuery({
    queryKey: ['consumable-options'],
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<ConsumableSummary>>>('/consumables', {
        params: { size: 200, sort: 'name,asc' },
      })).data.data.content.filter((c) => c.active),
    enabled,
    staleTime: 60_000,
  });
}

/** Small user directory for pickers; requires USER_VIEW. */
export function useUserOptions(search: string, enabled = true) {
  return useQuery({
    queryKey: ['user-options', search],
    queryFn: async () =>
      (await api.get<ApiEnvelope<Page<UserRow>>>('/users', {
        params: { search: search || undefined, size: 20 },
      })).data.data.content,
    enabled,
    staleTime: 60_000,
  });
}
