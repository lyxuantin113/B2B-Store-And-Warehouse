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
  phone: string;
  roleCode: RoleCode;
  accountStatus: AccountStatus;
  permissions: string[];
}

export interface B2bPartnerProfile {
  id: string;
  userId: string;
  companyName: string;
  businessType: string;
  representativeName: string;
  taxCode: string;
  businessLicenseUrl: string;
  approvalNotice?: string | null;
  rejectionReason?: string | null;
  reviewedBy?: string | null;
  reviewedAt?: string | null;
  confirmedAt?: string | null;
  activatedAt?: string | null;
  creditLimit: number;
  creditBalance: number;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: UserSummary;
}

export interface B2bRegistrationRequest {
  email: string;
  password: string;
  fullName: string;
  phone: string;
  companyName: string;
  businessType: string;
  taxCode: string;
  addressLine: string;
  addressDetail?: string;
  postalCode?: string;
  businessLicenseBase64?: string;
  businessLicenseFileName?: string;
}
