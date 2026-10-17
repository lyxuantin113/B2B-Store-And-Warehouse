export interface Category {
  id: string;
  code: string;
  name: string;
  description?: string;
  displayOrder: number;
  isActive: boolean;
  createdAt: string;
}

export interface ProductPriceTier {
  id?: number;
  minQty: number;
  maxQty?: number | null;
  unitPrice: number;
  tierLabel?: string | null;
}

export interface Product {
  id: string;
  sku: string;
  name: string;
  categoryId?: string | number;
  categoryName?: string;
  description?: string;
  subtitle?: string;
  basePrice: number;
  baseWholesalePrice?: number;
  retailPrice?: number;
  unitLabel: string;
  packageSpec?: string;
  unitsPerBox?: number;
  sampleAllowed?: boolean;
  sampleEnabled?: boolean;
  maxSampleQty?: number;
  sampleMaxQty?: number;
  isActive: boolean;
  totalAvailableStock?: number;
  priceTiers: ProductPriceTier[];
  createdAt?: string;
}

export interface CursorPage<T> {
  items: T[];
  nextCursorCreatedAt?: string | null;
  nextCursorId?: string | null;
  hasNext?: boolean;
}
