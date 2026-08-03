// Shared API types mirroring the backend DTOs.

export interface UserProfile {
  id: string;
  universityId: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  facultyName?: string;
  departmentName?: string;
  roles: string[];
  permissions: string[];
  mustChangePassword: boolean;
}

export interface Faculty {
  id: string;
  code: string;
  name: string;
  description?: string;
  active: boolean;
}

export interface Department extends Faculty {
  facultyId: string;
  facultyName: string;
  maintenanceIntervalDays?: number;
}

export interface Location {
  id: string;
  code: string;
  name: string;
  type: string;
  parentId?: string;
  parentName?: string;
  facultyId?: string;
  facultyName?: string;
  departmentId?: string;
  departmentName?: string;
  address?: string;
  capacity?: number;
  /** Price-list flat fee per venue booking (null/0 = free). */
  bookingFee?: number;
  responsibleUserId?: string;
  responsibleUserName?: string;
  description?: string;
  active: boolean;
}

export interface Category {
  id: string;
  code: string;
  name: string;
  assetType: string;
  parentId?: string;
  parentName?: string;
  description?: string;
  active: boolean;
}

export interface AssetSummary {
  id: string;
  assetCode: string;
  name: string;
  assetType: string;
  categoryName: string;
  facultyName: string;
  departmentName?: string;
  locationName: string;
  serialNumber?: string;
  condition: string;
  status: string;
  quantity: number;
  availableQuantity: number;
  reservable: boolean;
  custodianName?: string;
  purchasePrice?: number;
  currentBookValue?: number;
  currency: string;
  nextServiceDate?: string;
  archived: boolean;
}

export interface AssetDetail extends AssetSummary {
  description?: string;
  categoryId: string;
  brand?: string;
  model?: string;
  manufacturer?: string;
  barcode?: string;
  qrCode?: string;
  tags?: string;
  facultyId: string;
  departmentId?: string;
  locationId: string;
  locationNotes?: string;
  custodianUserId?: string;
  purchaseOrderNumber?: string;
  invoiceNumber?: string;
  fundingSource?: string;
  grantCode?: string;
  purchaseDate?: string;
  depreciationMethod?: string;
  usefulLifeYears?: number;
  salvageValue?: number;
  initialCondition?: string;
  approvalRequired: boolean;
  externalUseAllowed: boolean;
  depositRequired: boolean;
  depositAmount?: number;
  /** Price-list flat fee per reservation (null/0 = free). */
  reservationFee?: number;
  maxReservationHours?: number;
  warrantyStartDate?: string;
  warrantyEndDate?: string;
  warrantyProvider?: string;
  serviceIntervalMonths?: number;
  lastServiceDate?: string;
  calibrationRequired: boolean;
  calibrationIntervalMonths?: number;
  lastCalibrationDate?: string;
  nextCalibrationDate?: string;
  nextAvailableAt?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface ConsumableSummary {
  id: string;
  itemCode: string;
  name: string;
  categoryName: string;
  facultyName: string;
  locationName: string;
  unitOfMeasure: string;
  currentQuantity: number;
  reservedQuantity: number;
  availableQuantity: number;
  reorderLevel: number;
  lowStock: boolean;
  hazardous: boolean;
  active: boolean;
  batchCount: number;
  earliestExpiry?: string;
}

export interface ConsumableDetail extends ConsumableSummary {
  description?: string;
  categoryId: string;
  brand?: string;
  manufacturer?: string;
  facultyId: string;
  departmentId?: string;
  departmentName?: string;
  locationId: string;
  maximumStockLevel?: number;
  unitCost?: number;
  /** Price-list fee per unit issued against a reservation (null/0 = free). */
  unitFee?: number;
  chemicalClassification?: string;
  storageInstructions?: string;
  disposalInstructions?: string;
}

export interface Batch {
  id: string;
  batchNumber: string;
  quantityReceived: number;
  quantityRemaining: number;
  manufactureDate?: string;
  expiryDate?: string;
  unitCost?: number;
  receivedDate: string;
  expired: boolean;
}

export interface StockTransaction {
  id: string;
  transactionType: string;
  quantity: number;
  batchNumber?: string;
  relatedUserName?: string;
  relatedDepartmentName?: string;
  purpose?: string;
  reason?: string;
  referenceNumber?: string;
  chargeable: boolean;
  chargeAmount?: number;
  createdAt: string;
}

export interface Reservation {
  id: string;
  reservationNumber: string;
  assetId?: string;
  assetName?: string;
  assetCode?: string;
  locationId?: string;
  locationName?: string;
  consumableItemId?: string;
  consumableItemName?: string;
  consumableItemCode?: string;
  consumableUnit?: string;
  requestedById: string;
  requestedByName: string;
  facultyName?: string;
  departmentName?: string;
  purpose: string;
  courseOrProject?: string;
  startAt: string;
  endAt: string;
  quantity: number;
  requestedQuantity?: number;
  /** Total quantity ever issued against this reservation (returned slips included). */
  issuedQuantity?: number;
  feeAmount?: number;
  feeWaived?: boolean;
  /**
   * Price-list fee that would apply right now (unit fee x quantity for
   * consumables, flat otherwise). Pre-approval preview; persisted to feeAmount
   * at final approval. Null/absent = free.
   */
  applicableFee?: number;
  participantCount?: number;
  specialRequirements?: string;
  externalUseRequested: boolean;
  status: string;
  approvalStatus: string;
  requiredApprovalTier?: string;
  currentApprovalStep?: string;
  approvedByName?: string;
  approvedAt?: string;
  approvalNotes?: string;
  /** 4-digit handover code. Server includes it ONLY when the viewer is the requester. */
  collectionCode?: string;
  createdAt: string;
}

export interface Availability {
  available: boolean;
  requestedQuantity: number;
  totalQuantity: number;
  reservedInWindow: number;
  availableInWindow: number;
  blockers: string[];
  overlapping: Reservation[];
}

export interface Checkout {
  id: string;
  checkoutNumber: string;
  reservationId?: string;
  reservationNumber?: string;
  assetId: string;
  assetName: string;
  assetCode: string;
  userId: string;
  userName: string;
  userEmail: string;
  quantity: number;
  checkedOutAt: string;
  expectedReturnAt: string;
  returnedAt?: string;
  conditionBefore: string;
  conditionAfter?: string;
  accessories?: string;
  missingAccessories?: string;
  damageDetected: boolean;
  damageDescription?: string;
  depositPaid?: number;
  penaltyAmount?: number;
  issuedByName: string;
  receivedByName?: string;
  status: string;
  notes?: string;
  daysOverdue: number;
}

export interface MaintenanceRequest {
  id: string;
  requestNumber: string;
  assetId: string;
  assetName: string;
  assetCode: string;
  issueType: string;
  description: string;
  priority: string;
  requestedByName: string;
  assignedToName?: string;
  status: string;
  openedAt: string;
  dueAt?: string;
  startedAt?: string;
  completedAt?: string;
  diagnosis?: string;
  workPerformed?: string;
  partsUsed?: string;
  labourCost?: number;
  partsCost?: number;
  externalCost?: number;
  totalCost?: number;
  result?: string;
  newCondition?: string;
  nextServiceDate?: string;
  notes?: string;
}

export interface Transfer {
  id: string;
  transferNumber: string;
  assetId: string;
  assetName: string;
  assetCode: string;
  quantity: number;
  fromLocationName: string;
  toLocationName: string;
  fromCustodianName?: string;
  toCustodianName?: string;
  reason: string;
  status: string;
  requestedByName: string;
  approvedByName?: string;
  receivedByName?: string;
  expectedDate?: string;
  approvedAt?: string;
  completedAt?: string;
  conditionAtDestination?: string;
  notes?: string;
  createdAt: string;
}

export interface Payment {
  id: string;
  transactionNumber: string;
  transactionType: string;
  payerType: string;
  payerDisplayName?: string;
  reservationNumber?: string;
  assetName?: string;
  description?: string;
  amount: number;
  currency: string;
  paymentMethod: string;
  referenceNumber?: string;
  paymentDate: string;
  status: string;
  refundedAmount: number;
  originalTransactionNumber?: string;
  notes?: string;
}

export interface UserRow {
  id: string;
  universityId: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  phone?: string;
  userType?: string;
  facultyId?: string;
  facultyName?: string;
  departmentId?: string;
  departmentName?: string;
  roles: string[];
  accountStatus: string;
  lastLoginAt?: string;
  mustChangePassword: boolean;
  reservationLimit?: number;
  externalBorrowingAllowed: boolean;
  createdAt: string;
}

export interface Role {
  id: string;
  name: string;
  description?: string;
  systemRole: boolean;
  permissions: string[];
}

export interface Permission {
  id: string;
  code: string;
  module: string;
  action: string;
  description?: string;
}

export interface AuditLogRow {
  id: string;
  userId?: string;
  userEmail?: string;
  action: string;
  module: string;
  entityType?: string;
  entityId?: string;
  oldValues?: string;
  newValues?: string;
  ipAddress?: string;
  userAgent?: string;
  success: boolean;
  createdAt: string;
}

export interface NotificationRow {
  id: string;
  type: string;
  title: string;
  message: string;
  entityType?: string;
  entityId?: string;
  readAt?: string;
  createdAt: string;
}

/**
 * A priceable item row from GET /pricing/items — assets, venues (bookable
 * locations) and consumables the current user is allowed to price.
 */
export interface PricingItem {
  type: 'asset' | 'venue' | 'consumable';
  id: string;
  code: string;
  name: string;
  /** Unit of measure; set for consumables only. */
  unit?: string;
  /** Current price-list fee (null/absent = free). */
  currentFee?: number;
}

export interface NameValue {
  name: string;
  value: number;
}

export interface DashboardSummary {
  totalAssets: number;
  totalAssetValue: number;
  availableAssets: number;
  reservedAssets: number;
  checkedOutAssets: number;
  underMaintenance: number;
  damagedAssets: number;
  lostAssets: number;
  lowStockConsumables: number;
  expiringConsumables: number;
  pendingApprovals: number;
  overdueReturns: number;
  maintenanceJobsOpen: number;
  maintenanceDueSoon: number;
}

export interface DashboardCharts {
  assetsByCategory: NameValue[];
  assetsByFaculty: NameValue[];
  assetsByCondition: NameValue[];
  assetsByStatus: NameValue[];
  acquisitionValueByYear: NameValue[];
  monthlyReservations: NameValue[];
}
