import { AccountStatus, RoleCode } from "./auth";
import { OrderStatus, OrderType, PaymentStatus, ShippingProvider } from "./order";

export interface AdminPartnerKpi {
  totalPartners: number;
  pendingReviewCount: number;
  awaitingConfirmationCount: number;
  activeCount: number;
  rejectedCount: number;
  suspendedCount: number;
}

export interface AdminSalesAnalyticsKpi {
  totalRevenue: number;
  totalOrders: number;
  averageOrderValue: number;
  paidOrdersCount: number;
  deliveryCompletionRatePercent: number;
  cancellationRatePercent: number;
}

export interface BranchInventory {
  id: number;
  branchId: string;
  branchCode: string;
  branchName: string;
  productId: string;
  productSku: string;
  productName: string;
  unitLabel: string;
  availableQty: number;
  reservedQty: number;
  safetyStock: number;
  lowStockWarning: boolean;
  updatedAt: string;
}

export interface UpsertBranchInventoryRequest {
  branchId: string;
  productId: string;
  availableQty: number;
  safetyStock: number;
}

export interface SystemConfigItem {
  configKey: string;
  configGroup: string;
  configValue: string;
  isEncrypted: boolean;
  updatedBy?: string;
  updatedAt: string;
}

export interface UpsertSystemConfigRequest {
  configKey: string;
  configGroup: string;
  configValue: string;
  isEncrypted: boolean;
}

export interface RolePermission {
  roleId: number;
  roleCode: string;
  roleName: string;
  description: string;
  permissions: string[];
}

export interface CreateStaffAccountRequest {
  email: string;
  password?: string;
  fullName: string;
  phone?: string;
  roleCode: "ADMIN" | "MANAGER" | "SELLER";
  branchIds?: string[];
}

export interface AdminPartnerStatusUpdateRequest {
  targetStatus: AccountStatus;
  approvalNotice?: string;
  rejectionReason?: string;
}

export interface AdminBulkPartnerStatusRequest {
  partnerIds: string[];
  targetStatus: AccountStatus;
  approvalNotice?: string;
  rejectionReason?: string;
}

export interface AdminUpdateOrderStatusRequest {
  targetStatus: OrderStatus;
  trackingNumber?: string;
  internalNote?: string;
}

export interface AdminBulkWaybillItem {
  orderCode: string;
  shippingProvider: ShippingProvider;
  trackingNumber: string;
}
