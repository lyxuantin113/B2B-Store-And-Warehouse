export type AccountStatus =
  | "PENDING_ADMIN"
  | "ADMIN_APPROVED"
  | "ACTIVE"
  | "REJECTED"
  | "INACTIVE"
  | "SUSPENDED";

export type RoleCode = "ADMIN" | "MANAGER" | "SELLER" | "B2B_PARTNER";

export interface UserSummary {
  id: string;
  email: string;
  fullName: string;
  phone?: string;
  roleCode: RoleCode;
  accountStatus: AccountStatus;
  onboardingStep?: number;
  permissions: string[];
}

export interface B2bPartnerProfile {
  partnerId?: string;
  id?: string;
  userId: string;
  email: string;
  accountStatus: AccountStatus;
  companyName: string;
  taxCode: string;
  businessType: string;
  representativeName: string;
  contactPersonName: string;
  contactPhone: string;
  businessLicenseUrl: string;
  postalCode?: string;
  addressLine: string;
  addressDetail?: string;
  shippingPostalCode?: string;
  shippingAddressLine?: string;
  shippingAddressDetail?: string;
  approvalNotice?: string | null;
  rejectionReason?: string | null;
  reapplyCount?: number;
  approvedAt?: string | null;
  partnerConfirmedAt?: string | null;
  createdAt?: string;
}

export interface B2bOnboardingStatus {
  partnerProfileId: string;
  userId: string;
  email: string;
  accountStatus: AccountStatus;
  currentStep: number;
  companyName: string;
  taxCode: string;
  businessType: string;
  representativeName: string;
  contactPersonName: string;
  contactPhone: string;
  businessLicenseUrl: string;
  postalCode?: string;
  addressLine: string;
  addressDetail?: string;
  approvalNotice?: string | null;
  rejectionReason?: string | null;
  reapplyCount: number;
  approvedAt?: string | null;
  partnerConfirmedAt?: string | null;
  submittedAt?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
  userId: string;
  email: string;
  fullName: string;
  role: RoleCode;
  accountStatus: AccountStatus;
  onboardingStep: number;
  permissions: string[];
}

export interface B2bRegistrationForm {
  email: string;
  password: string;
  confirmPassword?: string;
  companyName: string;
  taxCode: string;
  businessType: string;
  representativeName: string;
  contactPersonName: string;
  contactPhone: string;
  businessLicenseUrl: string;
  postalCode?: string;
  addressLine: string;
  addressDetail?: string;
}
