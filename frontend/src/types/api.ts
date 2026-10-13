export interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
  timestamp: string;
}

export interface CursorPageDto<T> {
  items: T[];
  nextCursorCreatedAt?: string | null;
  nextCursorId?: string | null;
  hasNext: boolean;
}

export interface PageDto<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export type ErrorCode =
  | "ERR_VALIDATION"
  | "ERR_NOT_FOUND"
  | "ERR_CONCURRENT_MODIFICATION"
  | "ERR_INTERNAL"
  | "ERR_UNAUTHORIZED"
  | "ERR_INVALID_CREDENTIALS"
  | "ERR_TOKEN_EXPIRED"
  | "ERR_TOKEN_REUSE_DETECTED"
  | "ERR_FORBIDDEN"
  | "ERR_PARTNER_NOT_ACTIVE"
  | "ERR_DUPLICATE_EMAIL"
  | "ERR_DUPLICATE_TAX_CODE"
  | "ERR_INVALID_ONBOARDING_STATE"
  | "ERR_INVALID_FILE_TYPE"
  | "ERR_INSUFFICIENT_INVENTORY"
  | "ERR_SAMPLE_QUOTA_EXCEEDED"
  | "ERR_PAYMENT_SIGNATURE"
  | "ERR_CRYPTO";
