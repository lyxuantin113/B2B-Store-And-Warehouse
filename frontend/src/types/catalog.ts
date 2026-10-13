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
  categoryId: string;
  categoryName?: string;
  description?: string;
  basePrice: number;
  unitLabel: string;
  packageSpec: string;
  sampleAllowed: boolean;
  maxSampleQty: number;
  isActive: boolean;
  totalAvailableStock?: number;
  priceTiers: ProductPriceTier[];
  createdAt: string;
}
