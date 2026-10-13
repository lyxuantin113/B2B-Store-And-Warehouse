export type OrderType = "WHOLESALE" | "SAMPLE";

export type OrderStatus =
  | "PENDING"
  | "CONFIRMED"
  | "PROCESSING"
  | "SHIPPED"
  | "DELIVERED"
  | "COMPLETED"
  | "CANCELLED"
  | "REFUNDED";

export type PaymentMethod = "VNPAY" | "MOMO" | "VIETQR" | "BANK_TRANSFER";

export type PaymentStatus =
  | "UNPAID"
  | "PENDING_VERIFY"
  | "PAID"
  | "FAILED"
  | "REFUNDED";

export type ShippingProvider = "GHN" | "GHTK" | "AHAMOVE" | "SELF_PICKUP";

export interface OrderItem {
  id: number;
  productId: string;
  skuSnapshot: string;
  productNameSnap: string;
  unitLabelSnap: string;
  appliedTierCode?: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface Order {
  id: string;
  orderCode: string;
  userId: string;
  branchId?: string;
  branchName?: string;
  orderType: OrderType;
  orderStatus: OrderStatus;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  shippingProvider: ShippingProvider;
  trackingNumber?: string;
  subtotalAmount: number;
  discountAmount: number;
  shippingFee: number;
  taxAmount: number;
  totalAmount: number;
  recipientName: string;
  recipientPhone: string;
  shippingAddressLine: string;
  shippingAddressDetail?: string;
  shippingPostalCode?: string;
  orderNote?: string;
  items: OrderItem[];
  createdAt: string;
  paidAt?: string;
  shippedAt?: string;
  deliveredAt?: string;
  cancelledAt?: string;
}

export interface CreateOrderRequest {
  branchId: string;
  paymentMethod: PaymentMethod;
  shippingProvider: ShippingProvider;
  recipientName: string;
  recipientPhone: string;
  shippingAddressLine: string;
  shippingAddressDetail?: string;
  shippingPostalCode?: string;
  orderNote?: string;
  items: {
    productId: string;
    quantity: number;
  }[];
}

export interface OrderCheckoutResponse {
  orderCode: string;
  orderStatus: OrderStatus;
  paymentStatus: PaymentStatus;
  totalAmount: number;
  paymentMethod: PaymentMethod;
  paymentUrl?: string | null;
  vietQrUrl?: string | null;
  qrPayload?: string | null;
  trackingNumber?: string | null;
  requiresPaymentRedirect: boolean;
}

export interface PartnerDashboardOverview {
  totalCumulativeSpend: number;
  totalOrdersCount: number;
  activeInTransitOrdersCount: number;
  sampleOrdersCount: number;
  recentOrders: Order[];
}
