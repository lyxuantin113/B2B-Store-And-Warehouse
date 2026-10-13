export interface Branch {
  id: string;
  code: string;
  name: string;
  categoryTag?: string;
  phone?: string;
  postalCode?: string;
  addressLine: string;
  addressDetail?: string;
  latitude: number;
  longitude: number;
  isPinned: boolean;
  isActive: boolean;
  operatingHours?: string;
}

export interface NearbyBranch extends Branch {
  distanceKm: number;
  distanceMeters: number;
}

export interface StandardizedAddress {
  rawQuery: string;
  formattedAddress: string;
  latitude: number;
  longitude: number;
  postalCode: string;
  placeId: string;
  provider: string;
}
