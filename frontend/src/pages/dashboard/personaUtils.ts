import type { UserProfile } from '../../api/types';

export type PersonaType =
  | 'ADMIN'
  | 'FACULTY_DEAN_USER'
  | 'DEPT_ADMIN_USER'
  | 'CARETAKER_USER'
  | 'LAB_MANAGER'
  | 'STOREKEEPER'
  | 'MAINTENANCE_OFFICER'
  | 'FINANCE_OFFICER'
  | 'AUDITOR'
  | 'STUDENT_USER';

/** Personas whose primary job on the dashboard is deciding reservation requests. */
export const APPROVER_PERSONAS = ['DEPT_ADMIN_USER', 'FACULTY_DEAN_USER', 'CARETAKER_USER'] as const;
export type ApproverPersonaType = (typeof APPROVER_PERSONAS)[number];

export function isApproverPersona(persona: PersonaType): persona is ApproverPersonaType {
  return (APPROVER_PERSONAS as readonly PersonaType[]).includes(persona);
}

export interface PersonaMetadata {
  id: PersonaType;
  title: string;
  subtitle: string;
  eyebrow: string;
  badgeColor: 'primary' | 'secondary' | 'info' | 'warning' | 'success' | 'default';
}

export const PERSONA_METADATA: Record<PersonaType, PersonaMetadata> = {
  ADMIN: {
    id: 'ADMIN',
    title: 'Executive Administration Command',
    subtitle: 'System-wide oversight of assets, valuations, approvals, and faculty operations.',
    eyebrow: 'EXECUTIVE OVERVIEW',
    badgeColor: 'primary',
  },
  FACULTY_DEAN_USER: {
    id: 'FACULTY_DEAN_USER',
    title: "Dean's Approval Office",
    subtitle: 'Requests for faculty-owned assets and venues awaiting your decision, plus faculty settings.',
    eyebrow: 'FACULTY APPROVALS',
    badgeColor: 'secondary',
  },
  DEPT_ADMIN_USER: {
    id: 'DEPT_ADMIN_USER',
    title: 'Department Administration',
    subtitle: "Approvals for your department's equipment, venues, and supplies, plus department settings.",
    eyebrow: 'DEPARTMENT ADMINISTRATION',
    badgeColor: 'info',
  },
  CARETAKER_USER: {
    id: 'CARETAKER_USER',
    title: 'Caretaker Venue Desk',
    subtitle: 'Booking requests for the buildings and venues in your care, ready for one-click decisions.',
    eyebrow: 'VENUE CARETAKING',
    badgeColor: 'warning',
  },
  LAB_MANAGER: {
    id: 'LAB_MANAGER',
    title: 'Laboratory Operations Desk',
    subtitle: 'Real-time equipment readiness, reservation queue, checkouts, and overdue items.',
    eyebrow: 'LABORATORY MANAGEMENT',
    badgeColor: 'info',
  },
  STOREKEEPER: {
    id: 'STOREKEEPER',
    title: 'Store & Inventory Management Hub',
    subtitle: 'Consumable stock levels, reorder alerts, expiring batches, and inventory dispatches.',
    eyebrow: 'CONSUMABLES & STORES',
    badgeColor: 'warning',
  },
  MAINTENANCE_OFFICER: {
    id: 'MAINTENANCE_OFFICER',
    title: 'Maintenance & Service Control',
    subtitle: 'Active repair work orders, scheduled calibration, and equipment condition tracking.',
    eyebrow: 'MAINTENANCE & CALIBRATION',
    badgeColor: 'secondary',
  },
  FINANCE_OFFICER: {
    id: 'FINANCE_OFFICER',
    title: 'Financial & Asset Valuation Portal',
    subtitle: 'Asset book value, annual expenditure trends, and audit history.',
    eyebrow: 'FINANCIAL OVERSIGHT',
    badgeColor: 'success',
  },
  AUDITOR: {
    id: 'AUDITOR',
    title: 'Compliance & Audit Oversight',
    subtitle: 'Read-only verification of records, asset movements, and historical logs.',
    eyebrow: 'AUDIT & COMPLIANCE',
    badgeColor: 'default',
  },
  STUDENT_USER: {
    id: 'STUDENT_USER',
    title: 'Personal Asset & Reservation Portal',
    subtitle: 'Your active checkouts, return due dates, reservation schedule, and requests.',
    eyebrow: 'MY PORTAL',
    badgeColor: 'primary',
  },
};

/** Determines the primary persona view for a given user profile. */
export function getPrimaryPersona(user: UserProfile | null): PersonaType {
  if (!user) return 'STUDENT_USER';
  const roles = user.roles || [];

  if (roles.includes('SUPER_ADMIN') || roles.includes('FACULTY_ADMIN') || roles.includes('ASSET_ADMIN')) {
    return 'ADMIN';
  }
  if (roles.includes('FACULTY_DEAN')) {
    return 'FACULTY_DEAN_USER';
  }
  if (roles.includes('DEPT_ADMIN')) {
    return 'DEPT_ADMIN_USER';
  }
  if (roles.includes('CARETAKER')) {
    return 'CARETAKER_USER';
  }
  if (roles.includes('LAB_MANAGER')) {
    return 'LAB_MANAGER';
  }
  if (roles.includes('STOREKEEPER')) {
    return 'STOREKEEPER';
  }
  if (roles.includes('MAINTENANCE_OFFICER')) {
    return 'MAINTENANCE_OFFICER';
  }
  if (roles.includes('FINANCE_OFFICER')) {
    return 'FINANCE_OFFICER';
  }
  if (roles.includes('AUDITOR')) {
    return 'AUDITOR';
  }
  return 'STUDENT_USER';
}

/** Checks whether the logged-in user is an admin allowed to switch/preview persona views. */
export function isAdminUser(user: UserProfile | null): boolean {
  if (!user) return false;
  const roles = user.roles || [];
  return roles.includes('SUPER_ADMIN') || roles.includes('FACULTY_ADMIN') || roles.includes('ASSET_ADMIN');
}
